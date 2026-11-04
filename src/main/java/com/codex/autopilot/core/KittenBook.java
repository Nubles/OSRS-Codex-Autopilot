package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The account's kitten as Autopilot last knew it, kept in its own profile settings
 * ({@code codexautopilot/kittenV1}) so a logout, a world hop or another account does not lose it.
 *
 * <p>Every time here is counted only while the kitten follows the player (the game's own rule:
 * care is needed only while it is out in the world), on a clock of following-seconds. What it
 * reads is the game's own lines (wiki, Transcript:Kitten and Transcript:Cat):</p>
 *
 * <ul>
 * <li>fed: "The kitten gobbles up the fish." or "The kitten laps up the milk.";</li>
 * <li>hungry: "Your kitten is hungry.", then "Your kitten is very hungry.";</li>
 * <li>stroked: "You softly stroke your cat."; played with: the player's "That kitten loves to
 * play with that ball of wool. ...";</li>
 * <li>lonely: "Your kitten wants attention.", then "Your kitten really wants attention.";</li>
 * <li>grown: "Your kitten has grown into a healthy cat that can hunt for itself.", then "Your cat
 * has grown into a mighty feline, but it will no longer be able to chase vermin.";</li>
 * <li>its age: "After taking a good look at your kitten you guess that its age is: ... And
 * approximate time until fully adult: ...", which sets the growth clock right;</li>
 * <li>shooed: "The cat has run away."; and "Your cat has passed away." (wiki, Kitten, trivia).</li>
 * </ul>
 *
 * <p>The stage itself comes from the follower's npc id, so a grown cat is known even when its
 * line was missed. The times start as the wiki's and become the kitten's own: each warning the
 * game gives is the measure of how long the last meal or stroke actually lasted, and once
 * {@link #TRUST} are heard for one kind of care, the middle of the last {@link #KEEP} is used.</p>
 */
public final class KittenBook
{
	public static final String KEY = "kittenV1";
	static final int VERSION = 1;
	public static final int TRUST = 3;
	static final int KEEP = 7;
	/** A second stroke this soon after the first is "stroking more than once". */
	static final int STROKES_WINDOW_SECONDS = 60;
	/** The game warns on its 90 second growth tick, so a deadline is only good to that. */
	static final int TICK_SLACK_SECONDS = Kitten.GROWTH_TICK_SECONDS;
	/** Looks without the kitten following before it counts as put away. */
	static final int ABSENT_LOOKS = 2;
	/** The care kind for hunger, beside {@link Kitten.Care}'s names. */
	static final String FED = "FED";

	public static final String GONE_RAN_OFF = "ran off";
	public static final String GONE_SHOOED = "shooed away";
	public static final String GONE_PASSED = "passed away";

	private static final Pattern TAGS = Pattern.compile("<[^>]*>");
	private static final Pattern ADULT = Pattern.compile("time until fully adult:\\s*(?:(\\d+)\\s*hours?)?\\s*(?:(\\d+)\\s*minutes?)?");
	private static final Pattern AGE = Pattern.compile("its age is:\\s*(?:(\\d+)\\s*hours?)?\\s*(?:(\\d+)\\s*minutes?)?");

	private Kitten.Stage stage;
	private int npcId;
	private boolean out;
	private boolean held;
	/** Seconds it has followed the player since Autopilot first saw it. */
	private long clock;
	private long fedAt = -1;
	private int hungry;
	private long hungryAt;
	private long caredAt = -1;
	private Kitten.Care care;
	private int lonely;
	private long lonelyAt;
	/** Seconds of growth counted, or -1 when not a kitten; {@link #ageKnown} when it is the whole age. */
	private long growth = -1;
	private boolean ageKnown;
	/** Seconds followed as a cat since it was seen to grow, or -1. */
	private long catSeconds = -1;
	private long grownAt;
	private String gone;
	private long goneAt;
	private long seenAt;
	private final Map<String, List<Double>> learned = new LinkedHashMap<>();
	private transient int absent;

	public static KittenBook empty()
	{
		return new KittenBook();
	}

	// ---- What the game says ----

	/** Reads one chat line (a game message, a message box, or "Name|text" dialogue). Answers whether anything changed. */
	public synchronized boolean read(String message, long nowMs)
	{
		if (message == null)
		{
			return false;
		}
		String text = message;
		int bar = text.indexOf('|');
		if (bar >= 0)
		{
			text = text.substring(bar + 1);
		}
		text = TAGS.matcher(text).replaceAll(" ").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
		if (text.isEmpty())
		{
			return false;
		}
		if (text.contains("the kitten gobbles up the fish") || text.contains("the kitten laps up the milk"))
		{
			kittenOut(nowMs);
			fedAt = clock;
			hungry = 0;
			return true;
		}
		if (text.contains("your kitten is very hungry"))
		{
			kittenOut(nowMs);
			if (hungry == 0)
			{
				learnFrom(FED, fedAt, Kitten.HUNGER_WARN_MINUTES + Kitten.HUNGER_STEP_MINUTES);
			}
			hungry = 2;
			hungryAt = clock;
			return true;
		}
		if (text.contains("your kitten is hungry"))
		{
			kittenOut(nowMs);
			if (hungry == 0)
			{
				learnFrom(FED, fedAt, Kitten.HUNGER_WARN_MINUTES);
			}
			hungry = 1;
			hungryAt = clock;
			return true;
		}
		if (text.contains("you softly stroke your cat"))
		{
			// Cats are stroked with the same words: only a kitten known to be one is cared for by it.
			if (stage != Kitten.Stage.KITTEN || gone != null)
			{
				return false;
			}
			kittenOut(nowMs);
			boolean again = (care == Kitten.Care.STROKE || care == Kitten.Care.STROKES) && caredAt >= 0
				&& clock - caredAt <= STROKES_WINDOW_SECONDS;
			care = again ? Kitten.Care.STROKES : Kitten.Care.STROKE;
			caredAt = clock;
			lonely = 0;
			return true;
		}
		if (text.contains("loves to play with that ball of wool"))
		{
			kittenOut(nowMs);
			care = Kitten.Care.WOOL;
			caredAt = clock;
			lonely = 0;
			return true;
		}
		if (text.contains("your kitten really wants attention"))
		{
			kittenOut(nowMs);
			if (lonely == 0 && care != null)
			{
				learnFrom(care.name(), caredAt, care.getMinutes() + Kitten.LONELY_STEP_MINUTES);
			}
			lonely = 2;
			lonelyAt = clock;
			return true;
		}
		if (text.contains("your kitten wants attention"))
		{
			kittenOut(nowMs);
			if (lonely == 0 && care != null)
			{
				learnFrom(care.name(), caredAt, care.getMinutes());
			}
			lonely = 1;
			lonelyAt = clock;
			return true;
		}
		if (text.contains("your kitten has grown into a healthy cat"))
		{
			return grow(Kitten.Stage.CAT, nowMs);
		}
		if (text.contains("grown into a mighty feline"))
		{
			return grow(Kitten.Stage.OVERGROWN, nowMs);
		}
		if (text.contains("after taking a good look at your kitten") || text.contains("approximate time until fully adult"))
		{
			return age(text, nowMs);
		}
		if (text.contains("the cat has run away"))
		{
			return leave(GONE_SHOOED, nowMs);
		}
		if (text.contains("your cat has passed away"))
		{
			return leave(GONE_PASSED, nowMs);
		}
		return false;
	}

	/** A line only a kitten out in the world gives: it is a kitten, and it is out. */
	private void kittenOut(long nowMs)
	{
		if (stage != Kitten.Stage.KITTEN || gone != null)
		{
			start(Kitten.Stage.KITTEN, false);
		}
		out = true;
		held = false;
		absent = 0;
		seenAt = nowMs;
	}

	private boolean age(String text, long nowMs)
	{
		Matcher adult = ADULT.matcher(text);
		long left = -1;
		if (adult.find() && (adult.group(1) != null || adult.group(2) != null))
		{
			left = hours(adult.group(1)) * 3600 + minutes(adult.group(2)) * 60;
		}
		else
		{
			Matcher age = AGE.matcher(text);
			if (age.find() && (age.group(1) != null || age.group(2) != null))
			{
				left = Kitten.GROW_SECONDS - (hours(age.group(1)) * 3600 + minutes(age.group(2)) * 60);
			}
		}
		if (left < 0 && !text.contains("fully adult"))
		{
			return false;
		}
		if (stage != Kitten.Stage.KITTEN || gone != null)
		{
			start(Kitten.Stage.KITTEN, false);
		}
		growth = Math.max(0, Kitten.GROW_SECONDS - Math.max(0, left));
		ageKnown = true;
		seenAt = nowMs;
		return true;
	}

	private static long hours(String g)
	{
		return g == null ? 0 : Long.parseLong(g);
	}

	private static long minutes(String g)
	{
		return g == null ? 0 : Long.parseLong(g);
	}

	private boolean grow(Kitten.Stage to, long nowMs)
	{
		if (stage == to && gone == null)
		{
			return false;
		}
		if (stage == Kitten.Stage.KITTEN && to == Kitten.Stage.CAT)
		{
			grownAt = nowMs;
			catSeconds = 0;
		}
		else if (stage == null || gone != null)
		{
			start(to, false);
		}
		stage = to;
		gone = null;
		growth = -1;
		clearNeeds();
		seenAt = nowMs;
		return true;
	}

	private boolean leave(String why, long nowMs)
	{
		if (stage == null || gone != null)
		{
			return false;
		}
		gone = why;
		goneAt = nowMs;
		out = false;
		seenAt = nowMs;
		return true;
	}

	private void clearNeeds()
	{
		fedAt = -1;
		hungry = 0;
		caredAt = -1;
		care = null;
		lonely = 0;
	}

	/** A kitten or cat not known before. A {@code fresh} kitten is new from Gertrude: fed, and content for 25 minutes. */
	private void start(Kitten.Stage seen, boolean fresh)
	{
		stage = seen;
		clock = 0;
		clearNeeds();
		gone = null;
		goneAt = 0;
		grownAt = 0;
		catSeconds = -1;
		growth = seen == Kitten.Stage.KITTEN ? 0 : -1;
		ageKnown = fresh;
		if (fresh)
		{
			fedAt = 0;
			caredAt = 0;
			care = Kitten.Care.NEW;
		}
	}

	// ---- What the game shows ----

	/**
	 * One look at the player's follower, {@code seconds} after the last while logged in:
	 * {@code followerId} is the follower's npc id or -1, {@code carried} whether a kitten is in the
	 * inventory. Answers whether something worth saving at once changed (a new kitten, grown, put
	 * away, run off); the clock itself moves on quietly.
	 */
	public synchronized boolean look(long seconds, int followerId, boolean carried, long nowMs)
	{
		Kitten.Stage seen = followerId > 0 ? Kitten.stage(followerId) : null;
		boolean wasHeld = held;
		held = carried;
		if (seen != null)
		{
			absent = 0;
			boolean news = !out;
			if (stage == null || gone != null || (seen == Kitten.Stage.KITTEN && stage != Kitten.Stage.KITTEN))
			{
				boolean fresh = seen == Kitten.Stage.KITTEN && stage != null && (gone != null || stage != Kitten.Stage.KITTEN);
				start(seen, fresh);
				news = true;
			}
			else if (seen != stage)
			{
				grow(seen, nowMs);
				news = true;
			}
			npcId = followerId;
			out = true;
			held = false;
			seenAt = nowMs;
			long s = Math.max(0, seconds);
			clock += s;
			if (stage == Kitten.Stage.KITTEN && growth >= 0)
			{
				growth += s;
			}
			else if (stage == Kitten.Stage.CAT && catSeconds >= 0)
			{
				catSeconds += s;
			}
			return news;
		}
		if (!out)
		{
			return wasHeld != held;
		}
		absent++;
		if (stage == Kitten.Stage.KITTEN && !carried && absent >= ABSENT_LOOKS && overdue())
		{
			// Gone out of sight just as its time ran out, and not picked up: it has left.
			return leave(GONE_RAN_OFF, nowMs);
		}
		if (carried || absent >= ABSENT_LOOKS)
		{
			out = false;
			return true;
		}
		return false;
	}

	/** Logged out or hopping: it follows no one until it is seen again. Answers whether it was following. */
	public synchronized boolean pause()
	{
		boolean was = out;
		out = false;
		absent = 0;
		return was;
	}

	/** Its time is up, to the game's 90 second tick. */
	synchronized boolean overdue()
	{
		long h = hungerLeft();
		long l = lonelyLeft();
		return h >= 0 && h <= TICK_SLACK_SECONDS || l >= 0 && l <= TICK_SLACK_SECONDS;
	}

	// ---- What it adds up to ----

	/** Minutes from feeding to the first hunger warning: its own once learned, the wiki's until then. */
	public synchronized double hungerMinutes()
	{
		double own = own(FED);
		return own > 0 ? own : Kitten.HUNGER_WARN_MINUTES;
	}

	/** Minutes a kind of care lasts before it asks again: its own once learned, the wiki's until then. */
	public synchronized double careMinutes(Kitten.Care c)
	{
		double own = own(c.name());
		return own > 0 ? own : c.getMinutes();
	}

	public synchronized boolean isLearned(String kind)
	{
		return own(kind) > 0;
	}

	/** Following-seconds until it runs off for want of food, or -1 when not known. Never below 0. */
	public synchronized long hungerLeft()
	{
		if (stage != Kitten.Stage.KITTEN || gone != null)
		{
			return -1;
		}
		long step = Kitten.HUNGER_STEP_MINUTES * 60L;
		long at;
		if (hungry >= 2)
		{
			at = hungryAt + step;
		}
		else if (hungry == 1)
		{
			at = hungryAt + 2 * step;
		}
		else if (fedAt >= 0)
		{
			at = fedAt + Math.round(hungerMinutes() * 60) + 2 * step;
		}
		else
		{
			return -1;
		}
		return Math.max(0, at - clock);
	}

	/** Following-seconds until it runs off for want of attention, or -1 when not known. Never below 0. */
	public synchronized long lonelyLeft()
	{
		if (stage != Kitten.Stage.KITTEN || gone != null)
		{
			return -1;
		}
		long step = Kitten.LONELY_STEP_MINUTES * 60L;
		long at;
		if (lonely >= 2)
		{
			at = lonelyAt + step;
		}
		else if (lonely == 1)
		{
			at = lonelyAt + 2 * step;
		}
		else if (caredAt >= 0 && care != null)
		{
			at = caredAt + Math.round(careMinutes(care) * 60) + 2 * step;
		}
		else
		{
			return -1;
		}
		return Math.max(0, at - clock);
	}

	/** Following-seconds until it is a cat, or -1. When its age was never read, at most this. */
	public synchronized long growLeft()
	{
		if (stage != Kitten.Stage.KITTEN || growth < 0 || gone != null)
		{
			return -1;
		}
		return Math.max(0, Kitten.GROW_SECONDS - growth);
	}

	// ---- Learning ----

	private void learnFrom(String kind, long since, int wikiMinutes)
	{
		if (since < 0)
		{
			return;
		}
		double minutes = (clock - since) / 60.0;
		// Further off than this, a meal or a stroke was missed: not a measure of anything.
		if (minutes < wikiMinutes * 0.5 || minutes > wikiMinutes * 2.0)
		{
			return;
		}
		// The second warning measured from the care: what counts is the time to the first.
		double first = kind.equals(FED) && wikiMinutes != Kitten.HUNGER_WARN_MINUTES ? minutes - Kitten.HUNGER_STEP_MINUTES
			: !kind.equals(FED) && wikiMinutes != careWiki(kind) ? minutes - Kitten.LONELY_STEP_MINUTES : minutes;
		learn(kind, first);
	}

	private static int careWiki(String kind)
	{
		try
		{
			return Kitten.Care.valueOf(kind).getMinutes();
		}
		catch (IllegalArgumentException e)
		{
			return -1;
		}
	}

	void learn(String kind, double minutes)
	{
		if (minutes <= 0 || Double.isNaN(minutes) || Double.isInfinite(minutes))
		{
			return;
		}
		List<Double> list = learned.computeIfAbsent(kind, k -> new ArrayList<>());
		list.add(Math.round(minutes * 10) / 10.0);
		while (list.size() > KEEP)
		{
			list.remove(0);
		}
	}

	private double own(String kind)
	{
		List<Double> list = learned.get(kind);
		if (list == null || list.size() < TRUST)
		{
			return -1;
		}
		List<Double> sorted = new ArrayList<>(list);
		Collections.sort(sorted);
		int n = sorted.size();
		return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
	}

	/** How many times of a kind are heard so far. */
	public synchronized int heard(String kind)
	{
		List<Double> list = learned.get(kind);
		return list == null ? 0 : list.size();
	}

	// ---- Reading it ----

	public synchronized Kitten.Stage getStage()
	{
		return stage;
	}

	public synchronized int getNpcId()
	{
		return npcId;
	}

	/** Following the player at the last look. */
	public synchronized boolean isOut()
	{
		return out;
	}

	/** Carried in the inventory at the last look. */
	public synchronized boolean isHeld()
	{
		return held;
	}

	public synchronized long getClock()
	{
		return clock;
	}

	/** Following-seconds since it was last fed, or -1 when not seen. */
	public synchronized long sinceFed()
	{
		return fedAt < 0 ? -1 : clock - fedAt;
	}

	/** Following-seconds since it was last given attention, or -1 when not seen. */
	public synchronized long sinceCared()
	{
		return caredAt < 0 ? -1 : clock - caredAt;
	}

	/** 0 content, 1 hungry, 2 very hungry: as the game last said. */
	public synchronized int getHungry()
	{
		return hungry;
	}

	/** 0 content, 1 wants attention, 2 really wants it: as the game last said. */
	public synchronized int getLonely()
	{
		return lonely;
	}

	public synchronized Kitten.Care getCare()
	{
		return care;
	}

	/** Whether its age is known in full (new from Gertrude, or guessed in game), not only since Autopilot saw it. */
	public synchronized boolean isAgeKnown()
	{
		return ageKnown;
	}

	/** Seconds of growth counted while a kitten, or -1; its whole age when {@link #isAgeKnown()}. */
	public synchronized long getGrowth()
	{
		return growth;
	}

	/** Seconds followed as a cat since it grew, or -1 when it was not seen to grow. */
	public synchronized long getCatSeconds()
	{
		return catSeconds;
	}

	/** When it was seen to grow into a cat (ms), or 0. */
	public synchronized long getGrownAt()
	{
		return grownAt;
	}

	/** Why it is gone ({@link #GONE_RAN_OFF}...), or null while it is the player's. */
	public synchronized String getGone()
	{
		return gone;
	}

	public synchronized long getGoneAt()
	{
		return goneAt;
	}

	public synchronized long getSeenAt()
	{
		return seenAt;
	}

	/** True when Autopilot knows of no kitten or cat on this account. */
	public synchronized boolean isBlank()
	{
		return stage == null;
	}

	public synchronized KittenBook copy()
	{
		KittenBook b = new KittenBook();
		b.stage = stage;
		b.npcId = npcId;
		b.out = out;
		b.held = held;
		b.clock = clock;
		b.fedAt = fedAt;
		b.hungry = hungry;
		b.hungryAt = hungryAt;
		b.caredAt = caredAt;
		b.care = care;
		b.lonely = lonely;
		b.lonelyAt = lonelyAt;
		b.growth = growth;
		b.ageKnown = ageKnown;
		b.catSeconds = catSeconds;
		b.grownAt = grownAt;
		b.gone = gone;
		b.goneAt = goneAt;
		b.seenAt = seenAt;
		learned.forEach((k, v) -> b.learned.put(k, new ArrayList<>(v)));
		return b;
	}

	// ---- Saving ----

	public synchronized String toJson(Gson gson)
	{
		JsonObject o = new JsonObject();
		o.addProperty("version", VERSION);
		if (stage != null)
		{
			o.addProperty("stage", stage.name());
		}
		o.addProperty("npcId", npcId);
		o.addProperty("out", out);
		o.addProperty("held", held);
		o.addProperty("clock", clock);
		o.addProperty("fedAt", fedAt);
		o.addProperty("hungry", hungry);
		o.addProperty("hungryAt", hungryAt);
		o.addProperty("caredAt", caredAt);
		if (care != null)
		{
			o.addProperty("care", care.name());
		}
		o.addProperty("lonely", lonely);
		o.addProperty("lonelyAt", lonelyAt);
		o.addProperty("growth", growth);
		o.addProperty("ageKnown", ageKnown);
		o.addProperty("catSeconds", catSeconds);
		o.addProperty("grownAt", grownAt);
		if (gone != null)
		{
			o.addProperty("gone", gone);
		}
		o.addProperty("goneAt", goneAt);
		o.addProperty("seenAt", seenAt);
		JsonObject t = new JsonObject();
		learned.forEach((k, v) ->
		{
			JsonArray a = new JsonArray();
			v.forEach(a::add);
			t.add(k, a);
		});
		o.add("learned", t);
		return gson.toJson(o);
	}

	/** The saved book, or an empty one when there is none or it cannot be read. Never null. */
	public static KittenBook parse(Gson gson, String json)
	{
		KittenBook b = new KittenBook();
		JsonObject o = Json.object(Json.parse(gson, json));
		if (o == null || Json.version(o) != VERSION)
		{
			return b;
		}
		try
		{
			b.stage = enumOf(Kitten.Stage.class, Json.text(o, "stage"));
			b.npcId = (int) Json.whole(o, "npcId", 0);
			b.out = Json.bool(o, "out");
			b.held = Json.bool(o, "held");
			b.clock = Math.max(0, Json.whole(o, "clock", 0));
			b.fedAt = Math.min(b.clock, Math.max(-1, Json.whole(o, "fedAt", -1)));
			b.hungry = (int) Math.max(0, Math.min(2, Json.whole(o, "hungry", 0)));
			b.hungryAt = Math.min(b.clock, Math.max(0, Json.whole(o, "hungryAt", 0)));
			b.caredAt = Math.min(b.clock, Math.max(-1, Json.whole(o, "caredAt", -1)));
			b.care = enumOf(Kitten.Care.class, Json.text(o, "care"));
			b.lonely = (int) Math.max(0, Math.min(2, Json.whole(o, "lonely", 0)));
			b.lonelyAt = Math.min(b.clock, Math.max(0, Json.whole(o, "lonelyAt", 0)));
			b.growth = Math.max(-1, Json.whole(o, "growth", -1));
			b.ageKnown = Json.bool(o, "ageKnown");
			b.catSeconds = Math.max(-1, Json.whole(o, "catSeconds", -1));
			b.grownAt = Math.max(0, Json.whole(o, "grownAt", 0));
			b.gone = Json.text(o, "gone");
			b.goneAt = Math.max(0, Json.whole(o, "goneAt", 0));
			b.seenAt = Math.max(0, Json.whole(o, "seenAt", 0));
			JsonObject t = Json.object(o, "learned");
			if (t != null)
			{
				for (Map.Entry<String, JsonElement> e : t.entrySet())
				{
					if (!e.getValue().isJsonArray() || !(FED.equals(e.getKey()) || careWiki(e.getKey()) > 0))
					{
						continue;
					}
					for (JsonElement x : e.getValue().getAsJsonArray())
					{
						if (x.isJsonPrimitive() && x.getAsJsonPrimitive().isNumber())
						{
							b.learn(e.getKey(), x.getAsDouble());
						}
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			return new KittenBook();
		}
		return b;
	}

	private static <E extends Enum<E>> E enumOf(Class<E> type, String name)
	{
		if (name == null)
		{
			return null;
		}
		try
		{
			return Enum.valueOf(type, name);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
