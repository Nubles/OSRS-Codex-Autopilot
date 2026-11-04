package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Codex Boss Pilot's pinned boss and logbook of trips, read and never written. Boss Pilot keeps per RS
 * profile {@code codexbosspilot/pinned} (the boss's key as plain text, such as {@code vorkath}) and
 * {@code codexbosspilot/logbookV1}:
 * {@code {"version":1,"bosses":{"vorkath":{"kc":312,"best":58.2,"trips":[{"start":ms,"end":ms,"kills":4,"loot":1200000,...}]}}}}.
 * Both are Boss Pilot's own saves, not shared contracts, so their shape is pinned by a test here.
 */
public final class BossLog
{
	public static final String GROUP = "codexbosspilot";
	public static final String PINNED_KEY = "pinned";
	public static final String LOGBOOK_KEY = "logbookV1";
	/** Trips need this much time logged before their loot is a rate worth quoting. */
	static final double ENOUGH_MINUTES = 15;

	/** Boss Pilot's boss keys and names, from its Bosses table. */
	private static final Map<String, String> NAMES = new HashMap<>();

	static
	{
		String[][] list = {
			{"obor", "Obor"}, {"bryophyta", "Bryophyta"}, {"scurrius", "Scurrius"}, {"mole", "Giant Mole"}, {"barrows", "Barrows"},
			{"sarachnis", "Sarachnis"}, {"kq", "Kalphite Queen"}, {"kbd", "King Black Dragon"}, {"dks", "Dagannoth Kings"},
			{"kraken", "Kraken"}, {"thermy", "Thermonuclear smoke devil"}, {"gg", "Grotesque Guardians"}, {"zulrah", "Zulrah"},
			{"vorkath", "Vorkath"}, {"graardor", "General Graardor"}, {"zilyana", "Commander Zilyana"}, {"kreearra", "Kree'arra"},
			{"kril", "K'ril Tsutsaroth"},
		};
		for (String[] e : list)
		{
			NAMES.put(e[0], e[1]);
		}
	}

	/** What the logbook shows for one boss. */
	public static final class Record
	{
		private final int kc;
		private final int trips;
		private final int kills;
		private final double minutes;
		private final long loot;

		Record(int kc, int trips, int kills, double minutes, long loot)
		{
			this.kc = kc;
			this.trips = trips;
			this.kills = kills;
			this.minutes = minutes;
			this.loot = loot;
		}

		/** The game's kill count as Boss Pilot last read it, or -1. */
		public int getKc()
		{
			return kc;
		}

		/** Trips with at least one kill. */
		public int getTrips()
		{
			return trips;
		}

		public int getKills()
		{
			return kills;
		}

		public double getMinutes()
		{
			return minutes;
		}

		public long getLoot()
		{
			return loot;
		}

		/** The average trip in minutes, or 0 with none logged. */
		public double tripMinutes()
		{
			return trips == 0 ? 0 : minutes / trips;
		}

		/** Loot an hour, or 0 until enough time is logged. */
		public long lootPerHour()
		{
			return minutes < ENOUGH_MINUTES ? 0 : (long) (loot * 60.0 / minutes);
		}
	}

	private final String pinned;
	private final Map<String, Record> records;

	BossLog(String pinned, Map<String, Record> records)
	{
		this.pinned = pinned;
		this.records = Collections.unmodifiableMap(records);
	}

	public static BossLog read(Profile profile, Gson gson)
	{
		return parse(gson, Profile.safe(profile, GROUP, PINNED_KEY), Profile.safe(profile, GROUP, LOGBOOK_KEY));
	}

	/** The pin and the logbook; null when there is neither. A damaged or other-version logbook reads as empty. */
	public static BossLog parse(Gson gson, String pinned, String logbook)
	{
		String pin = pinned == null || pinned.trim().isEmpty() ? null : pinned.trim();
		Map<String, Record> records = new LinkedHashMap<>();
		JsonObject o = Json.object(Json.parse(gson, logbook));
		if (o != null && Json.version(o) == 1)
		{
			JsonObject bosses = Json.object(o, "bosses");
			if (bosses != null)
			{
				for (Map.Entry<String, JsonElement> e : bosses.entrySet())
				{
					Record r = record(Json.object(e.getValue()));
					if (r != null)
					{
						records.put(e.getKey(), r);
					}
				}
			}
		}
		if (pin == null && records.isEmpty())
		{
			return null;
		}
		return new BossLog(pin, records);
	}

	private static Record record(JsonObject o)
	{
		if (o == null)
		{
			return null;
		}
		int trips = 0;
		int kills = 0;
		double minutes = 0;
		long loot = 0;
		JsonArray list = Json.array(o, "trips");
		if (list != null)
		{
			for (JsonElement e : list)
			{
				JsonObject t = Json.object(e);
				long k = Json.whole(t, "kills", 0);
				long start = Json.whole(t, "start", 0);
				long end = Json.whole(t, "end", 0);
				if (k <= 0 || start <= 0 || end <= start || end - start > 86_400_000L)
				{
					continue;
				}
				trips++;
				kills += (int) Math.min(10_000, k);
				minutes += (end - start) / 60_000.0;
				loot += Math.max(0, Json.whole(t, "loot", 0));
			}
		}
		return new Record((int) Math.max(-1, Math.min(Integer.MAX_VALUE, Json.whole(o, "kc", -1))), trips, kills, minutes, loot);
	}

	/** Boss Pilot's name for a boss, or the key with its first letter up. */
	public static String name(String key)
	{
		String known = NAMES.get(key);
		if (known != null || key == null || key.isEmpty())
		{
			return known;
		}
		return Character.toUpperCase(key.charAt(0)) + key.substring(1);
	}

	/** The pinned boss's key, or null. */
	public String getPinned()
	{
		return pinned;
	}

	/** What is logged for a boss, or null. */
	public Record get(String key)
	{
		return key == null ? null : records.get(key);
	}

	/** Each boss whose trips have logged enough time, at its loot an hour. */
	public List<Earner> earners()
	{
		List<Earner> out = new ArrayList<>();
		for (Map.Entry<String, Record> e : records.entrySet())
		{
			long rate = e.getValue().lootPerHour();
			if (rate > 0)
			{
				out.add(new Earner("boss:" + e.getKey(), name(e.getKey()) + " trips", rate,
					(long) (e.getValue().getMinutes() * 60_000), "Boss Pilot"));
			}
		}
		return out;
	}
}
