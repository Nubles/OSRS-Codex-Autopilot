package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The account's Hunter Guild rumour as Autopilot last read it, kept in its own profile settings
 * ({@code codexautopilot/rumourV1}). There is no varbit for the rumour in RuneLite's gameval, so
 * it is read from what the game says, the way the Hunter Rumours plugin reads it:
 *
 * <ul>
 * <li>a guild hunter naming a creature ("There's been rumours of a rare grey chinchompa coming
 * in.", "You're hunting for me at the moment. This rumour was about a grey chinchompa."; wiki,
 * the guild hunters' transcripts), or naming another hunter's ("I hear you're hunting for
 * Teco." then "Aren't you looking for a red salamander at the moment?");</li>
 * <li>the quetzal whistle's Rumour option: "Your current rumour target is a grey chinchompa. ...
 * Cervus was the source of the rumour." (wiki, "Basic quetzal whistle");</li>
 * <li>the rare part: "You find a rare piece of the creature! You should take it back to the
 * Hunter Guild." (wiki, "Hunters' Rumours");</li>
 * <li>the hand-in: "Excellent! Thanks for getting that done. Here's your reward." or "Thanks for
 * that. I'll mark off that report for you." (wiki transcripts);</li>
 * <li>the count: "You have completed 12 rumours for the Hunter Guild." (RuneLite's chat
 * commands plugin reads the same line into {@code killcount/hunter rumours}).</li>
 * </ul>
 *
 * <p>It also learns how long the player's own rumours take, per hunting method: the time logged
 * in from the rumour being given to the rare part, the middle of the last seven, once three are
 * timed.</p>
 */
public final class RumourBook
{
	public static final String KEY = "rumourV1";
	static final int VERSION = 1;
	/** Rumours timed before the player's own time replaces the estimate. */
	public static final int TRUST = 3;
	static final int KEEP = 7;
	/** A rumour timed outside this many minutes was left and come back to, or misread. */
	static final double MIN_MINUTES = 2;
	static final double MAX_MINUTES = 240;

	private static final Pattern COUNT = Pattern.compile("you have completed ([0-9,]+) rumours? for the hunter guild");
	private static final Pattern TAGS = Pattern.compile("<[^>]*>|@[a-z_]+@");

	private Rumour.Master master;
	private Rumour.Creature creature;
	private boolean part;
	private long givenAt;
	private long huntSeconds;
	private int done = -1;
	private int level;
	private boolean atFirstLight;
	private long seenAt;
	private final Map<Rumour.Method, List<Double>> times = new EnumMap<>(Rumour.Method.class);
	/** The hunter another guild hunter just said the player is hunting for; only for the next line. */
	private transient Rumour.Master other;

	public static RumourBook empty()
	{
		return new RumourBook();
	}

	// ---- What the game says ----

	/**
	 * Reads one chat line: {@code dialog} for an NPC's dialogue ("Name|text"), otherwise a game
	 * message. Answers whether what is known changed.
	 */
	public synchronized boolean read(boolean dialog, String message, long nowMs)
	{
		if (message == null)
		{
			return false;
		}
		return dialog ? dialogue(message, nowMs) : game(clean(message), nowMs);
	}

	private boolean game(String text, long nowMs)
	{
		if (text.contains("your current rumour target is"))
		{
			Rumour.Creature c = Rumour.Creature.named(text);
			Rumour.Master m = Rumour.Master.named(text.substring(text.indexOf("your current rumour target is")));
			return c != null && give(m, c, nowMs);
		}
		if (text.contains("you find a rare piece of the creature"))
		{
			if (part)
			{
				return false;
			}
			part = true;
			if (creature != null && givenAt > 0)
			{
				learn(creature.getMethod(), huntSeconds / 60.0);
			}
			seenAt = nowMs;
			return true;
		}
		Matcher m = COUNT.matcher(text);
		if (m.find())
		{
			try
			{
				int n = Integer.parseInt(m.group(1).replace(",", ""));
				if (n != done)
				{
					done = n;
					seenAt = nowMs;
					return true;
				}
			}
			catch (NumberFormatException e)
			{
				return false;
			}
		}
		return false;
	}

	private boolean dialogue(String message, long nowMs)
	{
		int bar = message.indexOf('|');
		if (bar < 0)
		{
			return false;
		}
		String speaker = clean(message.substring(0, bar));
		if (!speaker.contains("guild hunter") && !speaker.contains("huntmaster"))
		{
			return false;
		}
		Rumour.Master who = Rumour.Master.named(speaker);
		if (who == null)
		{
			return false;
		}
		String text = clean(message.substring(bar + 1));
		Rumour.Master named = Rumour.Master.named(text);
		Rumour.Master before = other;
		other = null;
		if (text.contains("here's your reward") || text.contains("mark off that report"))
		{
			return handedIn(nowMs);
		}
		// At First Light: Wolf talks of a pyre fox that is no rumour (Hunter Rumours issue #80).
		if (text.contains("stopped off for a bit of hunting first"))
		{
			return false;
		}
		Rumour.Creature c = Rumour.Creature.named(text);
		if (c == null)
		{
			if (named != null && named != who && text.contains("hunting for"))
			{
				other = named;
			}
			return false;
		}
		// Gilman offering to swap in a fresh rumour does not give it yet (Hunter Rumours plugin).
		if (text.contains("would you prefer that one, or a new one entirely"))
		{
			return false;
		}
		Rumour.Master m = named != null && named != who ? named : before != null && text.contains("looking for") ? before : who;
		return give(m, c, nowMs);
	}

	private boolean give(Rumour.Master m, Rumour.Creature c, long nowMs)
	{
		if (c == creature && (m == null || m == master))
		{
			return false;
		}
		if (c == creature && master == null)
		{
			// The same rumour, now with who gave it.
			master = m;
			seenAt = nowMs;
			return true;
		}
		master = m;
		creature = c;
		part = false;
		givenAt = nowMs;
		huntSeconds = 0;
		seenAt = nowMs;
		return true;
	}

	private boolean handedIn(long nowMs)
	{
		if (creature == null && master == null && !part)
		{
			// Nothing held as far as this book knows, but now it knows that for itself.
			boolean first = seenAt <= 0;
			seenAt = nowMs;
			return first;
		}
		master = null;
		creature = null;
		part = false;
		givenAt = 0;
		huntSeconds = 0;
		seenAt = nowMs;
		return true;
	}

	/** Lower case, without colour tags or non-breaking spaces. */
	static String clean(String s)
	{
		return TAGS.matcher(s).replaceAll("").replace(' ', ' ').toLowerCase(Locale.ROOT).trim();
	}

	/** Time logged in passes; it counts toward the rumour while it is being hunted. */
	public synchronized void hunted(long seconds)
	{
		if (creature != null && !part && seconds > 0)
		{
			huntSeconds += seconds;
		}
	}

	/** The Hunter level and At First Light, as the game shows them; answers whether either changed. */
	public synchronized boolean account(int hunterLevel, boolean firstLight)
	{
		boolean changed = hunterLevel > 0 && hunterLevel != level || firstLight != atFirstLight;
		if (hunterLevel > 0)
		{
			level = hunterLevel;
		}
		atFirstLight = firstLight;
		return changed;
	}

	synchronized void learn(Rumour.Method method, double minutes)
	{
		if (method == null || Double.isNaN(minutes) || minutes < MIN_MINUTES || minutes > MAX_MINUTES)
		{
			return;
		}
		List<Double> list = times.computeIfAbsent(method, k -> new ArrayList<>());
		list.add(Math.round(minutes * 10) / 10.0);
		while (list.size() > KEEP)
		{
			list.remove(0);
		}
	}

	// ---- What is known ----

	public synchronized Rumour.Master getMaster()
	{
		return master;
	}

	public synchronized Rumour.Creature getCreature()
	{
		return creature;
	}

	/** True once the rare part is found and waits to be taken back. */
	public synchronized boolean hasPart()
	{
		return part;
	}

	/** Rumours finished, or -1 when the game has not said. */
	public synchronized int getDone()
	{
		return done;
	}

	public synchronized int getLevel()
	{
		return level;
	}

	public synchronized boolean isAtFirstLight()
	{
		return atFirstLight;
	}

	/** When anything here was last read from the game (epoch ms), or 0 when never. */
	public synchronized long getSeenAt()
	{
		return seenAt;
	}

	/** The player's own minutes for a rumour hunted this way, or 0 until {@link #TRUST} are timed. */
	public synchronized int minutes(Rumour.Method method)
	{
		List<Double> list = times.get(method);
		if (list == null || list.size() < TRUST)
		{
			return 0;
		}
		List<Double> sorted = new ArrayList<>(list);
		Collections.sort(sorted);
		return (int) Math.max(1, Math.round(sorted.get(sorted.size() / 2)));
	}

	/** This book with the count filled from elsewhere when it has none of its own. */
	public synchronized RumourBook withDone(int count)
	{
		RumourBook b = copy();
		if (b.done < 0 && count >= 0)
		{
			b.done = count;
		}
		return b;
	}

	/**
	 * The rumour to plan with: this book once it has read anything itself, else the Hunter Rumours
	 * plugin's rumour with this book's level, quest and learned times. Null when the plugin's is
	 * wanted but there is none.
	 */
	public synchronized RumourBook orElse(RumourBook plugin)
	{
		if (seenAt > 0 || plugin == null)
		{
			return copy();
		}
		RumourBook b = copy();
		b.master = plugin.master;
		b.creature = plugin.creature;
		b.part = plugin.part;
		return b;
	}

	/** True when this book has read nothing itself and would take another's rumour. */
	public synchronized boolean isBlank()
	{
		return seenAt <= 0;
	}

	private RumourBook copy()
	{
		RumourBook b = new RumourBook();
		b.master = master;
		b.creature = creature;
		b.part = part;
		b.givenAt = givenAt;
		b.huntSeconds = huntSeconds;
		b.done = done;
		b.level = level;
		b.atFirstLight = atFirstLight;
		b.seenAt = seenAt;
		times.forEach((k, v) -> b.times.put(k, new ArrayList<>(v)));
		return b;
	}

	// ---- Another plugin's view ----

	/**
	 * The current rumour as the Hunter Rumours plugin saved it ({@code hunterrumours/current.hunter},
	 * {@code hunter.<npc id>} and {@code current.rumour.finished}, its enum names), for when
	 * Autopilot has not read one itself. Null when it has saved nothing usable.
	 */
	public static RumourBook fromHunterRumours(Profile profile)
	{
		String hunter = Profile.safe(profile, "hunterrumours", "current.hunter");
		if (hunter == null)
		{
			return null;
		}
		Rumour.Master m = null;
		String h = hunter.trim();
		int us = h.indexOf('_');
		if (us > 0)
		{
			try
			{
				m = Rumour.Master.valueOf(h.substring(us + 1));
			}
			catch (IllegalArgumentException e)
			{
				m = null;
			}
		}
		if (m == null)
		{
			return null;
		}
		String rumour = Profile.safe(profile, "hunterrumours", "hunter." + m.getNpcId());
		Rumour.Creature c = null;
		if (rumour != null)
		{
			String r = rumour.trim().replaceAll("_\\d+$", "");
			try
			{
				c = Rumour.Creature.valueOf(r);
			}
			catch (IllegalArgumentException e)
			{
				c = null;
			}
		}
		RumourBook b = new RumourBook();
		if (c == null)
		{
			// Their record of a hunter with no rumour: nothing assigned.
			return "NONE".equals(rumour == null ? null : rumour.trim()) ? b : null;
		}
		b.master = m;
		b.creature = c;
		b.part = "true".equalsIgnoreCase(String.valueOf(Profile.safe(profile, "hunterrumours", "current.rumour.finished")).trim());
		return b;
	}

	// ---- Saving ----

	public synchronized String toJson(Gson gson)
	{
		JsonObject o = new JsonObject();
		o.addProperty("version", VERSION);
		if (master != null)
		{
			o.addProperty("master", master.name());
		}
		if (creature != null)
		{
			o.addProperty("creature", creature.name());
		}
		o.addProperty("part", part);
		o.addProperty("givenAt", givenAt);
		o.addProperty("huntSeconds", huntSeconds);
		o.addProperty("done", done);
		o.addProperty("level", level);
		o.addProperty("atFirstLight", atFirstLight);
		o.addProperty("seenAt", seenAt);
		JsonObject t = new JsonObject();
		times.forEach((k, v) ->
		{
			JsonArray a = new JsonArray();
			v.forEach(a::add);
			t.add(k.name(), a);
		});
		o.add("times", t);
		return gson.toJson(o);
	}

	/** The saved book, or an empty one when there is none or it cannot be read. Never null. */
	public static RumourBook parse(Gson gson, String json)
	{
		RumourBook b = new RumourBook();
		JsonObject o = Json.object(Json.parse(gson, json));
		if (o == null)
		{
			return b;
		}
		try
		{
			JsonElement v = o.get("version");
			if (v == null || !v.isJsonPrimitive() || v.getAsInt() != VERSION)
			{
				return b;
			}
			b.master = enumOf(Rumour.Master.class, Json.text(o, "master"));
			b.creature = enumOf(Rumour.Creature.class, Json.text(o, "creature"));
			b.part = b.creature != null && bool(o, "part");
			b.givenAt = Math.max(0, number(o, "givenAt"));
			b.huntSeconds = Math.max(0, number(o, "huntSeconds"));
			b.done = (int) Math.max(-1, number(o, "done", -1));
			b.level = (int) Math.max(0, Math.min(99, number(o, "level")));
			b.atFirstLight = bool(o, "atFirstLight");
			b.seenAt = Math.max(0, number(o, "seenAt"));
			JsonObject t = Json.object(o, "times");
			if (t != null)
			{
				for (Map.Entry<String, JsonElement> e : t.entrySet())
				{
					Rumour.Method m = enumOf(Rumour.Method.class, e.getKey());
					if (m == null || !e.getValue().isJsonArray())
					{
						continue;
					}
					for (JsonElement x : e.getValue().getAsJsonArray())
					{
						if (x.isJsonPrimitive() && x.getAsJsonPrimitive().isNumber())
						{
							b.learn(m, x.getAsDouble());
						}
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			return new RumourBook();
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

	private static boolean bool(JsonObject o, String key)
	{
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();
	}

	private static long number(JsonObject o, String key)
	{
		return number(o, key, 0);
	}

	private static long number(JsonObject o, String key, long fallback)
	{
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsLong() : fallback;
	}
}
