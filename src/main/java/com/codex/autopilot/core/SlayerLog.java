package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The account's Slayer task as Autopilot last read it from the game, kept in its own profile
 * settings ({@code codexautopilot/slayerV1}), and how fast this player kills each kind of task.
 *
 * <p>What the game says is read the way RuneLite's own Slayer plugin reads it: the amount left
 * ({@code VarPlayerID.SLAYER_COUNT}), the task ({@code SLAYER_TARGET}, or the boss in
 * {@code SLAYER_TARGET_BOSSID}), Konar's area ({@code SLAYER_AREA}), the amount given
 * ({@code SLAYER_COUNT_ORIGINAL}), the master ({@code VarbitID.SLAYER_MASTER}), the points
 * ({@code SLAYER_POINTS}) and the tasks in a row ({@code SLAYER_TASKS_COMPLETED}, Krystilia's
 * {@code SLAYER_WILDERNESS_TASKS_COMPLETED}, Mortimer's {@code SLAYER_MORTIMER_TASKS_COMPLETED}).
 * The task's and area's names come from the game's own tables, and are kept here for when those
 * are not loaded.</p>
 *
 * <p>The pace is learned from the count going down: each kill is timed from the one before it,
 * and a gap longer than five minutes (a bank trip, a break, a logout) is not counted, so the
 * rate is the player's own kills an hour while on task. It is kept per task, for this account
 * only, and trusted once twenty kills are timed.</p>
 */
public final class SlayerLog
{
	public static final String KEY = "slayerV1";
	static final int VERSION = 1;
	/** Kills timed before the player's own pace replaces the estimate. */
	public static final int TRUST_KILLS = 20;
	static final double TRUST_SECONDS = 120;
	/** Past this many kills the record is halved, so recent play weighs more. */
	static final double KEEP_KILLS = 400;
	/** A gap between kills longer than this was spent off the task, and is not timed. */
	static final long MAX_GAP_SECONDS = 300;
	/** More kills than this between two looks is not a kill but a misreading. */
	static final int MAX_STEP = 10;

	/** One look at the game. */
	public static final class Reading
	{
		public int taskId;
		public int bossId;
		/** The task table row, or -1 when the game's tables are not loaded. */
		public int row = -1;
		public String name;
		public int areaId;
		public String area;
		public int amount;
		public int initial;
		public int masterId;
		/** The master, when the game's tables named it; else what was learned before is used. */
		public Slayer.Master master;
		public int points = -1;
		public int streak = -1;
		public int wildStreak = -1;
		public int mortimerStreak = -1;
		public Slayer.Account account;
		/** Master ids learned from the game's master task table. */
		public Map<Integer, Slayer.Master> masters;
		/** Game time in ms (600 a tick), to time kills by; 0 to time them by the wall clock. */
		public long gameMs;
	}

	private int taskId;
	private int bossId;
	private int row = -1;
	private String name;
	private int areaId;
	private String area;
	private int amount;
	private int initial;
	private int masterId;
	private Slayer.Master master;
	private int points = -1;
	private int streak = -1;
	private int wildStreak = -1;
	private int mortimerStreak = -1;
	private String lastTask;
	private long seenAt;
	private Slayer.Account account = new Slayer.Account();
	private final Map<Integer, Slayer.Master> masters = new HashMap<>();
	/** Task key to {kills timed, seconds}. */
	private final Map<String, double[]> pace = new LinkedHashMap<>();

	/** The count as last seen this session, or -1 before the first look; a kill is only counted between two looks. */
	private transient int live = -1;
	private transient long lastKillMs;

	public static SlayerLog empty()
	{
		return new SlayerLog();
	}

	// ---- Reading the game ----

	/** Takes one look at the game; answers whether anything worth saving changed. */
	public synchronized boolean update(Reading r, long nowMs)
	{
		if (r == null)
		{
			return false;
		}
		String before = signature();
		long clock = r.gameMs > 0 ? r.gameMs : nowMs;
		if (r.masters != null)
		{
			masters.putAll(r.masters);
		}
		if (r.amount > 0)
		{
			boolean same = r.taskId == taskId && r.bossId == bossId && r.areaId == areaId && amount > 0;
			if (!same || r.amount > amount && r.amount > live)
			{
				// A new task (or a new one of the same kind): its count starts here.
				taskId = r.taskId;
				bossId = r.bossId;
				areaId = r.areaId;
				row = r.row;
				name = r.name;
				area = r.area;
				initial = Math.max(r.initial, r.amount);
				masterId = r.masterId;
				master = resolve(r);
				lastKillMs = 0;
			}
			else
			{
				if (live > 0 && r.amount < live)
				{
					kill(live - r.amount, clock);
				}
				if (r.row > 0)
				{
					row = r.row;
				}
				if (r.name != null)
				{
					name = r.name;
				}
				if (r.area != null)
				{
					area = r.area;
				}
				if (r.initial >= r.amount && r.initial > 0)
				{
					initial = r.initial;
				}
				if (r.masterId > 0 && (r.masterId != masterId || master == null))
				{
					masterId = r.masterId;
					master = resolve(r);
				}
			}
			amount = r.amount;
			live = r.amount;
			seenAt = nowMs;
		}
		else
		{
			if (amount > 0)
			{
				// Finished (or cancelled): the next step is a new one.
				lastTask = name;
			}
			clearTask();
			live = 0;
			if (r.masterId > 0)
			{
				masterId = r.masterId;
				master = resolve(r);
			}
		}
		if (r.points >= 0)
		{
			points = r.points;
		}
		if (r.streak >= 0)
		{
			streak = r.streak;
		}
		if (r.wildStreak >= 0)
		{
			wildStreak = r.wildStreak;
		}
		if (r.mortimerStreak >= 0)
		{
			mortimerStreak = r.mortimerStreak;
		}
		if (r.account != null && r.account.combat > 0)
		{
			account = r.account.copy();
		}
		if (streak > 0 || points > 0)
		{
			seenAt = Math.max(seenAt, 1);
		}
		return !before.equals(signature());
	}

	private void clearTask()
	{
		taskId = 0;
		bossId = 0;
		row = -1;
		name = null;
		areaId = 0;
		area = null;
		amount = 0;
		initial = 0;
		lastKillMs = 0;
	}

	private Slayer.Master resolve(Reading r)
	{
		if (r.master != null)
		{
			masters.put(r.masterId, r.master);
			return r.master;
		}
		Slayer.Master m = masters.get(r.masterId);
		if (m != null)
		{
			return m;
		}
		return r.masterId == Slayer.Master.KRYSTILIA_ID ? Slayer.Master.KRYSTILIA
			: r.masterId == Slayer.Master.MORTIMER_ID ? Slayer.Master.MORTIMER : null;
	}

	private void kill(int kills, long nowMs)
	{
		if (kills > MAX_STEP)
		{
			lastKillMs = 0;
			return;
		}
		if (lastKillMs > 0)
		{
			long gap = (nowMs - lastKillMs) / 1000;
			if (gap > 0 && gap <= MAX_GAP_SECONDS)
			{
				learn(key(), kills, gap);
			}
		}
		lastKillMs = nowMs;
	}

	synchronized void learn(String key, double kills, double seconds)
	{
		if (key == null || kills <= 0 || seconds <= 0)
		{
			return;
		}
		double[] p = pace.computeIfAbsent(key, k -> new double[2]);
		p[0] += kills;
		p[1] += seconds;
		if (p[0] > KEEP_KILLS)
		{
			p[0] /= 2;
			p[1] /= 2;
		}
	}

	/** Logged out or switched off: the next kill cannot be timed from the last. */
	public synchronized void pause()
	{
		live = -1;
		lastKillMs = 0;
	}

	/** What the pace of the task in hand is kept under: its name, else its id. */
	synchronized String key()
	{
		if (name != null)
		{
			return name.trim().toLowerCase(Locale.ROOT);
		}
		return taskId > 0 ? "task:" + taskId + (bossId > 0 ? ":" + bossId : "") : null;
	}

	private String signature()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(taskId).append('|').append(bossId).append('|').append(row).append('|').append(name).append('|').append(areaId)
			.append('|').append(area).append('|').append(amount).append('|').append(initial).append('|').append(masterId).append('|')
			.append(master).append('|').append(points).append('|').append(streak).append('|').append(wildStreak).append('|')
			.append(mortimerStreak).append('|').append(lastTask).append('|').append(account.combat).append('|').append(account.slayer)
			.append('|').append(account.hashCode()).append(account.priestInPeril).append(account.lostCity).append(account.shilo)
			.append(account.wgs).append(account.mm2).append(account.westernElite).append(account.kourendElite).append('|')
			.append(masters).append('|').append(pace.size());
		for (double[] p : pace.values())
		{
			sb.append(',').append(p[0]);
		}
		return sb.toString();
	}

	// ---- What is known ----

	/** True while a task is held. */
	public synchronized boolean hasTask()
	{
		return amount > 0;
	}

	/** True for an account that does Slayer: a task, a streak or points have been seen. */
	public synchronized boolean doesSlayer()
	{
		return amount > 0 || streak > 0 || points > 0 || lastTask != null || !pace.isEmpty();
	}

	public synchronized int getTaskId()
	{
		return taskId;
	}

	public synchronized int getRow()
	{
		return row;
	}

	/** The task's name as the game's table gives it, such as "Kurask", or null. */
	public synchronized String getName()
	{
		return name;
	}

	/** Konar's area for this task, such as "Fremennik Slayer Dungeon", or null. */
	public synchronized String getArea()
	{
		return area;
	}

	public synchronized int getAmount()
	{
		return amount;
	}

	public synchronized int getInitial()
	{
		return initial;
	}

	public synchronized Slayer.Master getMaster()
	{
		return master;
	}

	/** Slayer points, or -1 when not read. */
	public synchronized int getPoints()
	{
		return points;
	}

	/** Tasks in a row with the usual masters, or -1 when not read. */
	public synchronized int getStreak()
	{
		return streak;
	}

	/** The streak the current master counts: Krystilia and Mortimer keep their own. */
	public synchronized int streakFor(Slayer.Master m)
	{
		return m == Slayer.Master.KRYSTILIA ? wildStreak : m == Slayer.Master.MORTIMER ? mortimerStreak : streak;
	}

	/** The last task finished, or null. */
	public synchronized String getLastTask()
	{
		return lastTask;
	}

	public synchronized Slayer.Account getAccount()
	{
		return account.copy();
	}

	/** When anything was last read from the game (epoch ms), or 0 when never. */
	public synchronized long getSeenAt()
	{
		return seenAt;
	}

	/** This player's kills an hour on the task in hand, or 0 until {@link #TRUST_KILLS} are timed. */
	public synchronized double killsPerHour()
	{
		String k = key();
		double[] p = k == null ? null : pace.get(k);
		if (p == null || p[0] < TRUST_KILLS || p[1] < TRUST_SECONDS)
		{
			return 0;
		}
		return p[0] * 3600 / p[1];
	}

	/** Kills timed on the task in hand so far. */
	public synchronized int timed()
	{
		String k = key();
		double[] p = k == null ? null : pace.get(k);
		return p == null ? 0 : (int) Math.floor(p[0]);
	}

	/** Minutes left at this player's own pace, or 0 when the pace is not known yet. */
	public synchronized int minutesLeft()
	{
		double rate = killsPerHour();
		if (rate <= 0 || amount <= 0)
		{
			return 0;
		}
		return (int) Math.max(1, Math.round(amount * 60 / rate));
	}

	public synchronized SlayerLog copy()
	{
		SlayerLog b = new SlayerLog();
		b.taskId = taskId;
		b.bossId = bossId;
		b.row = row;
		b.name = name;
		b.areaId = areaId;
		b.area = area;
		b.amount = amount;
		b.initial = initial;
		b.masterId = masterId;
		b.master = master;
		b.points = points;
		b.streak = streak;
		b.wildStreak = wildStreak;
		b.mortimerStreak = mortimerStreak;
		b.lastTask = lastTask;
		b.seenAt = seenAt;
		b.account = account.copy();
		b.masters.putAll(masters);
		pace.forEach((k, v) -> b.pace.put(k, v.clone()));
		b.live = live;
		b.lastKillMs = lastKillMs;
		return b;
	}

	// ---- Saving ----

	public synchronized String toJson(Gson gson)
	{
		JsonObject o = new JsonObject();
		o.addProperty("version", VERSION);
		o.addProperty("taskId", taskId);
		o.addProperty("bossId", bossId);
		o.addProperty("row", row);
		if (name != null)
		{
			o.addProperty("name", name);
		}
		o.addProperty("areaId", areaId);
		if (area != null)
		{
			o.addProperty("area", area);
		}
		o.addProperty("amount", amount);
		o.addProperty("initial", initial);
		o.addProperty("masterId", masterId);
		if (master != null)
		{
			o.addProperty("master", master.name());
		}
		o.addProperty("points", points);
		o.addProperty("streak", streak);
		o.addProperty("wildStreak", wildStreak);
		o.addProperty("mortimerStreak", mortimerStreak);
		if (lastTask != null)
		{
			o.addProperty("lastTask", lastTask);
		}
		o.addProperty("seenAt", seenAt);
		JsonObject a = new JsonObject();
		a.addProperty("combat", account.combat);
		a.addProperty("slayer", account.slayer);
		a.addProperty("priestInPeril", account.priestInPeril);
		a.addProperty("lostCity", account.lostCity);
		a.addProperty("shilo", account.shilo);
		a.addProperty("wgs", account.wgs);
		a.addProperty("mm2", account.mm2);
		a.addProperty("westernElite", account.westernElite);
		a.addProperty("kourendElite", account.kourendElite);
		o.add("account", a);
		JsonObject m = new JsonObject();
		masters.forEach((k, v) -> m.addProperty(String.valueOf(k), v.name()));
		o.add("masters", m);
		JsonObject p = new JsonObject();
		pace.forEach((k, v) ->
		{
			JsonArray arr = new JsonArray();
			arr.add(Math.round(v[0] * 10) / 10.0);
			arr.add(Math.round(v[1] * 10) / 10.0);
			p.add(k, arr);
		});
		o.add("pace", p);
		return gson.toJson(o);
	}

	/** The saved log, or an empty one when there is none or it cannot be read. Never null. */
	public static SlayerLog parse(Gson gson, String json)
	{
		SlayerLog b = new SlayerLog();
		JsonObject o = Json.object(Json.parse(gson, json));
		if (o == null)
		{
			return b;
		}
		try
		{
			if (Json.version(o) != VERSION)
			{
				return b;
			}
			b.taskId = (int) Math.max(0, Json.whole(o, "taskId", 0));
			b.bossId = (int) Math.max(0, Json.whole(o, "bossId", 0));
			b.row = (int) Json.whole(o, "row", -1);
			b.name = Json.text(o, "name");
			b.areaId = (int) Math.max(0, Json.whole(o, "areaId", 0));
			b.area = Json.text(o, "area");
			b.amount = (int) Math.max(0, Math.min(100_000, Json.whole(o, "amount", 0)));
			b.initial = (int) Math.max(b.amount, Math.min(100_000, Json.whole(o, "initial", 0)));
			b.masterId = (int) Math.max(0, Json.whole(o, "masterId", 0));
			b.master = master(Json.text(o, "master"));
			b.points = (int) Math.max(-1, Json.whole(o, "points", -1));
			b.streak = (int) Math.max(-1, Json.whole(o, "streak", -1));
			b.wildStreak = (int) Math.max(-1, Json.whole(o, "wildStreak", -1));
			b.mortimerStreak = (int) Math.max(-1, Json.whole(o, "mortimerStreak", -1));
			b.lastTask = Json.text(o, "lastTask");
			b.seenAt = Math.max(0, Json.whole(o, "seenAt", 0));
			if (b.amount == 0)
			{
				b.taskId = 0;
				b.name = null;
				b.area = null;
				b.row = -1;
			}
			JsonObject a = Json.object(o, "account");
			if (a != null)
			{
				b.account.combat = (int) Math.max(0, Math.min(126, Json.whole(a, "combat", 0)));
				b.account.slayer = (int) Math.max(0, Math.min(99, Json.whole(a, "slayer", 0)));
				b.account.priestInPeril = Json.bool(a, "priestInPeril");
				b.account.lostCity = Json.bool(a, "lostCity");
				b.account.shilo = Json.bool(a, "shilo");
				b.account.wgs = Json.bool(a, "wgs");
				b.account.mm2 = Json.bool(a, "mm2");
				b.account.westernElite = Json.bool(a, "westernElite");
				b.account.kourendElite = Json.bool(a, "kourendElite");
			}
			JsonObject m = Json.object(o, "masters");
			if (m != null)
			{
				for (Map.Entry<String, JsonElement> e : m.entrySet())
				{
					Slayer.Master v = master(Json.text(e.getValue()));
					try
					{
						if (v != null)
						{
							b.masters.put(Integer.parseInt(e.getKey()), v);
						}
					}
					catch (NumberFormatException ex)
					{
						// Skip it.
					}
				}
			}
			JsonObject p = Json.object(o, "pace");
			if (p != null)
			{
				for (Map.Entry<String, JsonElement> e : p.entrySet())
				{
					if (!e.getValue().isJsonArray() || e.getValue().getAsJsonArray().size() != 2)
					{
						continue;
					}
					JsonArray arr = e.getValue().getAsJsonArray();
					double kills = Json.real(arr.get(0), 0);
					double seconds = Json.real(arr.get(1), 0);
					if (kills > 0 && seconds > 0)
					{
						b.pace.put(e.getKey(), new double[]{Math.min(kills, KEEP_KILLS * 2), seconds});
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			return new SlayerLog();
		}
		return b;
	}

	private static Slayer.Master master(String name)
	{
		if (name == null)
		{
			return null;
		}
		try
		{
			return Slayer.Master.valueOf(name);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
