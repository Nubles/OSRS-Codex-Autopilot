package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.Map;

/**
 * The account's time at the Gemstone Crab, learned from its own play and saved per RS profile
 * under {@code codexautopilot/gemstoneCrabV1}:
 *
 * <pre>{"version":1,"timedMs":3600000,"xp":52000,"crabs":6,"shells":5,"missed":1,
 *  "gems":{"OPAL":4,"JADE":5},"moves":{"NORTH":{"EAST":3}},"stays":2,"stayMs":1200000}</pre>
 *
 * <ul>
 * <li>{@code timedMs}: game time spent at a mine with the crab there (or just gone), the hours
 * the xp and gems an hour are worked out over; {@code xp}: combat xp gained in that time;</li>
 * <li>{@code gems}: the uncut gems mined from its shell, by the game's own "You mine an uncut ..."
 * line; {@code shells}: shells mined, {@code missed}: shells the game said were not yours;</li>
 * <li>{@code moves}: from which mine the crab burrowed to which it was next seen at. The wiki gives
 * no order, so this is the only place an order comes from;</li>
 * <li>{@code stays}/{@code stayMs}: crabs timed from a full health bar to burrowing.</li>
 * </ul>
 *
 * What the crab is doing right now (where it is, its bar, the shell) is kept beside it but never
 * saved. Times are game time, 600 ms a tick, as the plugin counts it.
 */
public final class CrabLog
{
	public static final String KEY = "gemstoneCrabV1";
	static final int VERSION = 1;

	/** The xp an hour is shown once this much time at the crab is timed. */
	public static final long TRUST_XP_MS = 5 * 60_000L;
	/** Gems come one shell at a time, so their hourly rate waits for longer. */
	public static final long TRUST_GEMS_MS = 20 * 60_000L;
	/** A move is learned only when the crab is seen again this soon after burrowing, so no move between is missed. */
	static final long MOVE_WINDOW_MS = 8 * 60_000L;
	/** The clock keeps running this long after a burrow, while the player follows it. */
	static final long FOLLOW_MS = 3 * 60_000L;
	/** A gap between two looks longer than this (a hop, a stall) is not counted. */
	static final long MAX_STEP_MS = 5_000L;
	/** A crab gone from sight this recently is the one a burrow line is about. */
	static final long LOST_MS = 3_000L;
	/** The bar has to fall this far before its pace is trusted. */
	static final double DRAIN_FROM = 0.05;
	/** A next spawn is named only once this many moves from a mine agree. */
	public static final int TRUST_MOVES = 3;
	static final double TRUST_SHARE = 0.8;
	/** A timed stay outside this is a misreading (a bar seen part way down, a hop), not kept. */
	static final long MIN_STAY_MS = 3 * 60_000L;
	static final long MAX_STAY_MS = 20 * 60_000L;

	// ---- saved -----------------------------------------------------------------------------------
	private long timedMs;
	private long xp;
	private int crabs;
	private int shells;
	private int missed;
	private final Map<GemstoneCrab.Gem, Integer> gems = new EnumMap<>(GemstoneCrab.Gem.class);
	private final int[][] moves = new int[3][3];
	private int stays;
	private long stayMs;

	// ---- live, never saved -----------------------------------------------------------------------
	/** The game time of the last look. */
	private long gameMs = -1;
	/** The mine the player is at, or null. */
	private GemstoneCrab.Mine near;
	/** The mine the crab is at, while it is in sight. */
	private GemstoneCrab.Mine mine;
	/** The bar, 0 to 1, or -1 when it has not been shown. */
	private double ratio = -1;
	/** The first reading of this crab's bar and when, to measure how fast it falls. */
	private double anchorRatio = -1;
	private long anchorAt;
	/** When this crab was first seen at full health, or 0. */
	private long spawnAt;
	/** The crab last out of sight, when and its spawn time: its burrow line can come just after it goes. */
	private GemstoneCrab.Mine lostMine;
	private long lostAt;
	private long lostSpawnAt;
	/** When it last burrowed and where from, or 0 and null. */
	private long burrowedAt;
	private GemstoneCrab.Mine burrowedFrom;
	/** Where the next sighting counts a move from; cleared once one is counted. */
	private GemstoneCrab.Mine moveFrom;
	/** The game's word on this shell: 1 yours to mine, -1 not, 0 not said. */
	private int eligible;
	private int shellGems;

	/** Nothing ever seen: the account has not been to the crab with Autopilot. */
	public synchronized boolean isBlank()
	{
		return timedMs == 0 && crabs == 0 && totalGems() == 0 && near == null;
	}

	/**
	 * One look at the game, every tick.
	 *
	 * @param now game time
	 * @param crabAt the mine the crab stands at, or null when it is not in sight
	 * @param bar its health bar, 0 to 1, or -1 when not shown
	 * @param playerAt the mine the player is at, or null
	 * @return true when something worth saving changed
	 */
	public synchronized boolean look(long now, GemstoneCrab.Mine crabAt, double bar, GemstoneCrab.Mine playerAt)
	{
		long dt = gameMs < 0 ? 0 : now - gameMs;
		gameMs = now;
		boolean changed = false;
		if (playerAt != null && (crabAt != null || following()) && dt > 0 && dt <= MAX_STEP_MS)
		{
			timedMs += dt;
		}
		near = playerAt;
		if (crabAt != null && crabAt == burrowedFrom && burrowedAt > 0 && now - burrowedAt <= LOST_MS)
		{
			// The crab that just burrowed, still in the scene for a moment.
			crabAt = null;
		}
		if (crabAt != null && crabAt != mine)
		{
			// A crab comes into sight: a new one, or the same one walked up to.
			mine = crabAt;
			ratio = -1;
			anchorRatio = -1;
			anchorAt = 0;
			spawnAt = 0;
			if (moveFrom != null && crabAt != moveFrom && burrowedAt > 0 && now - burrowedAt <= MOVE_WINDOW_MS)
			{
				moves[moveFrom.ordinal()][crabAt.ordinal()]++;
				changed = true;
			}
			moveFrom = null;
		}
		else if (crabAt == null && mine != null)
		{
			// Walked away from it, or it burrowed and its line is still to come: the bar is no longer known.
			lostMine = mine;
			lostAt = now;
			lostSpawnAt = spawnAt;
			mine = null;
			ratio = -1;
			anchorRatio = -1;
			spawnAt = 0;
		}
		if (crabAt != null && bar >= 0 && bar <= 1)
		{
			if (anchorRatio < 0)
			{
				anchorRatio = bar;
				anchorAt = now;
				if (bar >= 0.995)
				{
					spawnAt = now;
				}
			}
			ratio = bar;
		}
		return changed;
	}

	/** Combat xp gained this tick; counted while at the crab. */
	public synchronized boolean xp(long gained)
	{
		if (gained <= 0 || gained > 100_000 || near == null || mine == null && !following())
		{
			return false;
		}
		xp += gained;
		return true;
	}

	/** A game message; true when it was about the crab. */
	public synchronized boolean chat(String raw)
	{
		if (raw == null)
		{
			return false;
		}
		String message = raw.replaceAll("<[^>]*>", "").trim();
		if (GemstoneCrab.BURROWS.equalsIgnoreCase(message))
		{
			long now = Math.max(0, gameMs);
			crabs++;
			boolean justLost = mine == null && lostMine != null && now - lostAt <= LOST_MS;
			GemstoneCrab.Mine from = mine != null ? mine : justLost ? lostMine : near;
			long spawned = mine != null ? spawnAt : justLost ? lostSpawnAt : 0;
			if (spawned > 0)
			{
				long stay = now - spawned;
				if (stay >= MIN_STAY_MS && stay <= MAX_STAY_MS)
				{
					stays++;
					stayMs += stay;
				}
			}
			burrowedAt = now;
			burrowedFrom = from;
			moveFrom = burrowedFrom;
			eligible = 0;
			shellGems = 0;
			mine = null;
			ratio = -1;
			anchorRatio = -1;
			spawnAt = 0;
			return true;
		}
		if (GemstoneCrab.ELIGIBLE.equalsIgnoreCase(message))
		{
			eligible = 1;
			return true;
		}
		if (GemstoneCrab.NOT_ELIGIBLE.equalsIgnoreCase(message))
		{
			if (eligible != -1)
			{
				missed++;
			}
			eligible = -1;
			return true;
		}
		GemstoneCrab.Gem gem = GemstoneCrab.Gem.mined(message);
		if (gem != null)
		{
			gems.merge(gem, 1, Integer::sum);
			if (shellGems == 0)
			{
				shells++;
			}
			shellGems++;
			return true;
		}
		return false;
	}

	/** Logged out or hopped: what was in sight is gone, and no time runs across the gap. */
	public synchronized void pause()
	{
		gameMs = -1;
		near = null;
		mine = null;
		ratio = -1;
		anchorRatio = -1;
		spawnAt = 0;
	}

	private boolean following()
	{
		return burrowedAt > 0 && gameMs - burrowedAt <= FOLLOW_MS;
	}

	// ---- what it adds up to ----------------------------------------------------------------------

	public synchronized GemstoneCrab.Mine getNear()
	{
		return near;
	}

	/** The mine the crab is at while in sight, or null. */
	public synchronized GemstoneCrab.Mine getMine()
	{
		return mine;
	}

	/** Where it last burrowed from, or null. */
	public synchronized GemstoneCrab.Mine getBurrowedFrom()
	{
		return burrowedFrom;
	}

	public synchronized double getRatio()
	{
		return ratio;
	}

	/** Seconds of the shell left to mine, or 0 when there is none of it left for this player. */
	public synchronized int shellSecondsLeft()
	{
		if (burrowedAt <= 0 || eligible != 1 || shellGems >= GemstoneCrab.GEMS_PER_SHELL)
		{
			return 0;
		}
		long left = GemstoneCrab.SHELL_SECONDS * 1000L - (gameMs - burrowedAt);
		return left <= 0 ? 0 : (int) ((left + 999) / 1000);
	}

	/** Gems still to mine from this shell. */
	public synchronized int shellGemsLeft()
	{
		return Math.max(0, GemstoneCrab.GEMS_PER_SHELL - shellGems);
	}

	/** True when the game said this shell was not this player's to mine. */
	public synchronized boolean shellRefused()
	{
		return burrowedAt > 0 && eligible == -1 && gameMs - burrowedAt <= GemstoneCrab.SHELL_SECONDS * 1000L;
	}

	/** How fast its bar falls, a share a second, from this crab's own bar; 0 until it has fallen enough to tell. */
	synchronized double drain()
	{
		if (anchorRatio < 0 || ratio < 0 || anchorRatio - ratio < DRAIN_FROM || gameMs <= anchorAt)
		{
			return 0;
		}
		return (anchorRatio - ratio) * 1000 / (gameMs - anchorAt);
	}

	/** This account's crabs, timed from a full bar to burrowing, on average; 0 until one is timed. */
	public synchronized long averageStayMs()
	{
		return stays == 0 ? 0 : stayMs / stays;
	}

	/** How the time left was found. */
	public enum Left
	{
		/** Its bar has not been shown. */
		UNKNOWN,
		/** The bar's own fall, measured on this crab. */
		MEASURED,
		/** The bar against this account's timed crabs. */
		LEARNED,
		/** The bar against the wiki's "approximately every 10 minutes". */
		WIKI
	}

	public synchronized Left leftFrom()
	{
		if (mine == null || ratio < 0)
		{
			return Left.UNKNOWN;
		}
		if (drain() > 0)
		{
			return Left.MEASURED;
		}
		return stays > 0 ? Left.LEARNED : Left.WIKI;
	}

	/** Seconds until it burrows, from its bar, or -1 when not known. */
	public synchronized int secondsLeft()
	{
		switch (leftFrom())
		{
			case MEASURED:
				return (int) Math.round(ratio / drain());
			case LEARNED:
				return (int) Math.round(ratio * averageStayMs() / 1000);
			case WIKI:
				return (int) Math.round(ratio * GemstoneCrab.STAY_SECONDS);
			default:
				return -1;
		}
	}

	/** The mine the crab goes to after this one, when the player's own sightings agree on it; else null. */
	public synchronized GemstoneCrab.Mine next(GemstoneCrab.Mine from)
	{
		if (from == null)
		{
			return null;
		}
		int[] row = moves[from.ordinal()];
		int total = movesFrom(from);
		for (GemstoneCrab.Mine to : GemstoneCrab.Mine.values())
		{
			int n = row[to.ordinal()];
			if (n >= TRUST_MOVES && n >= TRUST_SHARE * total)
			{
				return to;
			}
		}
		return null;
	}

	/** How many moves away from a mine have been seen. */
	public synchronized int movesFrom(GemstoneCrab.Mine from)
	{
		int total = 0;
		for (int n : moves[from.ordinal()])
		{
			total += n;
		}
		return total;
	}

	/** How many of them went to that mine. */
	public synchronized int moves(GemstoneCrab.Mine from, GemstoneCrab.Mine to)
	{
		return moves[from.ordinal()][to.ordinal()];
	}

	/** Combat xp an hour at the crab, or -1 until enough is timed. */
	public synchronized long xpPerHour()
	{
		return timedMs < TRUST_XP_MS ? -1 : Math.round(xp * 3_600_000.0 / timedMs);
	}

	/** Uncut gems an hour at the crab, to one decimal, or -1 until enough is timed. */
	public synchronized double gemsPerHour()
	{
		return timedMs < TRUST_GEMS_MS ? -1 : Math.round(totalGems() * 36_000_000.0 / timedMs) / 10.0;
	}

	public synchronized long getTimedMs()
	{
		return timedMs;
	}

	public synchronized long getXp()
	{
		return xp;
	}

	public synchronized int getCrabs()
	{
		return crabs;
	}

	public synchronized int getShells()
	{
		return shells;
	}

	public synchronized int getMissed()
	{
		return missed;
	}

	public synchronized int gems(GemstoneCrab.Gem g)
	{
		return gems.getOrDefault(g, 0);
	}

	public synchronized int totalGems()
	{
		int n = 0;
		for (int v : gems.values())
		{
			n += v;
		}
		return n;
	}

	public synchronized CrabLog copy()
	{
		CrabLog c = new CrabLog();
		c.timedMs = timedMs;
		c.xp = xp;
		c.crabs = crabs;
		c.shells = shells;
		c.missed = missed;
		c.gems.putAll(gems);
		for (int i = 0; i < 3; i++)
		{
			System.arraycopy(moves[i], 0, c.moves[i], 0, 3);
		}
		c.stays = stays;
		c.stayMs = stayMs;
		c.gameMs = gameMs;
		c.near = near;
		c.mine = mine;
		c.ratio = ratio;
		c.anchorRatio = anchorRatio;
		c.anchorAt = anchorAt;
		c.spawnAt = spawnAt;
		c.lostMine = lostMine;
		c.lostAt = lostAt;
		c.lostSpawnAt = lostSpawnAt;
		c.burrowedAt = burrowedAt;
		c.burrowedFrom = burrowedFrom;
		c.moveFrom = moveFrom;
		c.eligible = eligible;
		c.shellGems = shellGems;
		return c;
	}

	public synchronized String toJson(Gson gson)
	{
		JsonObject o = new JsonObject();
		o.addProperty("version", VERSION);
		o.addProperty("timedMs", timedMs);
		o.addProperty("xp", xp);
		o.addProperty("crabs", crabs);
		o.addProperty("shells", shells);
		o.addProperty("missed", missed);
		JsonObject g = new JsonObject();
		gems.forEach((k, v) -> g.addProperty(k.name(), v));
		o.add("gems", g);
		JsonObject m = new JsonObject();
		for (GemstoneCrab.Mine from : GemstoneCrab.Mine.values())
		{
			JsonObject row = new JsonObject();
			for (GemstoneCrab.Mine to : GemstoneCrab.Mine.values())
			{
				int n = moves[from.ordinal()][to.ordinal()];
				if (n > 0)
				{
					row.addProperty(to.name(), n);
				}
			}
			if (row.size() > 0)
			{
				m.add(from.name(), row);
			}
		}
		o.add("moves", m);
		o.addProperty("stays", stays);
		o.addProperty("stayMs", stayMs);
		return gson.toJson(o);
	}

	/** Reads a save; anything missing or damaged starts blank rather than failing. */
	public static CrabLog parse(Gson gson, String json)
	{
		CrabLog b = new CrabLog();
		JsonObject o = Json.object(Json.parse(gson, json));
		if (o == null || Json.version(o) != VERSION)
		{
			return b;
		}
		try
		{
			b.timedMs = Math.max(0, Json.whole(o, "timedMs", 0));
			b.xp = Math.max(0, Json.whole(o, "xp", 0));
			b.crabs = (int) Math.max(0, Math.min(Integer.MAX_VALUE, Json.whole(o, "crabs", 0)));
			b.shells = (int) Math.max(0, Math.min(Integer.MAX_VALUE, Json.whole(o, "shells", 0)));
			b.missed = (int) Math.max(0, Math.min(Integer.MAX_VALUE, Json.whole(o, "missed", 0)));
			JsonObject g = Json.object(o, "gems");
			if (g != null)
			{
				for (Map.Entry<String, JsonElement> e : g.entrySet())
				{
					GemstoneCrab.Gem gem = gem(e.getKey());
					long n = Math.round(Json.real(e.getValue(), 0));
					if (gem != null && n > 0)
					{
						b.gems.put(gem, (int) Math.min(Integer.MAX_VALUE, n));
					}
				}
			}
			JsonObject m = Json.object(o, "moves");
			if (m != null)
			{
				for (Map.Entry<String, JsonElement> e : m.entrySet())
				{
					GemstoneCrab.Mine from = GemstoneCrab.Mine.named(e.getKey());
					JsonObject row = Json.object(e.getValue());
					if (from == null || row == null)
					{
						continue;
					}
					for (Map.Entry<String, JsonElement> t : row.entrySet())
					{
						GemstoneCrab.Mine to = GemstoneCrab.Mine.named(t.getKey());
						long n = Math.round(Json.real(t.getValue(), 0));
						if (to != null && to != from && n > 0)
						{
							b.moves[from.ordinal()][to.ordinal()] = (int) Math.min(Integer.MAX_VALUE, n);
						}
					}
				}
			}
			b.stays = (int) Math.max(0, Math.min(Integer.MAX_VALUE, Json.whole(o, "stays", 0)));
			b.stayMs = Math.max(0, Json.whole(o, "stayMs", 0));
			if (b.stays == 0)
			{
				b.stayMs = 0;
			}
		}
		catch (RuntimeException e)
		{
			return new CrabLog();
		}
		return b;
	}

	private static GemstoneCrab.Gem gem(String name)
	{
		try
		{
			return GemstoneCrab.Gem.valueOf(name);
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			return null;
		}
	}
}
