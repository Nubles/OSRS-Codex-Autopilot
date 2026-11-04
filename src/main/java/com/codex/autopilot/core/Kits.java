package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The supply kits Codex Quartermaster shares, read and never written. Quartermaster saves them per
 * RS profile under {@code codexquartermaster/kitsV1}, a documented shape that only grows:
 * {@code {"version":1,"active":"vorkath","target":5,"kits":[{"id","name","preset","tripsLeft","limiting",
 * "needs":[{"supply","name","unit","perTrip","items"}]}]}}. {@code tripsLeft} is -1 when the bank was
 * never seen; {@code active} is absent until the player chooses a kit.
 */
public final class Kits
{
	public static final String GROUP = "codexquartermaster";
	public static final String KEY = "kitsV1";

	/** One kit, as far as Autopilot reads it. */
	public static final class Kit
	{
		private final String id;
		private final String name;
		private final int tripsLeft;
		private final String limiting;

		public Kit(String id, String name, int tripsLeft, String limiting)
		{
			this.id = id;
			this.name = name;
			this.tripsLeft = tripsLeft;
			this.limiting = limiting;
		}

		public String getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		/** Trips the bank supports, or -1 when not known. */
		public int getTripsLeft()
		{
			return tripsLeft;
		}

		/** The name of the supply that runs out first, such as "Sharks", or null. */
		public String getLimiting()
		{
			return limiting;
		}
	}

	private final String active;
	private final int target;
	private final List<Kit> kits;

	Kits(String active, int target, List<Kit> kits)
	{
		this.active = active;
		this.target = target;
		this.kits = Collections.unmodifiableList(kits);
	}

	public static Kits read(Profile profile, Gson gson)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY));
	}

	public static Kits parse(Gson gson, String json)
	{
		JsonObject o = Json.object(Json.parse(gson, json));
		JsonArray list = Json.array(o, "kits");
		if (o == null || Json.version(o) != 1 || list == null)
		{
			return null;
		}
		List<Kit> kits = new ArrayList<>();
		for (JsonElement e : list)
		{
			JsonObject k = Json.object(e);
			String id = Json.text(k, "id");
			String name = Json.text(k, "name");
			if (id == null || name == null)
			{
				continue;
			}
			String limitingKey = Json.text(k, "limiting");
			String limiting = null;
			JsonArray needs = Json.array(k, "needs");
			if (limitingKey != null && needs != null)
			{
				for (JsonElement n : needs)
				{
					JsonObject line = Json.object(n);
					if (limitingKey.equals(Json.text(line, "supply")))
					{
						limiting = Json.text(line, "name");
						break;
					}
				}
			}
			long trips = Json.whole(k, "tripsLeft", -1);
			kits.add(new Kit(id, name, (int) Math.max(-1, Math.min(10_000, trips)), limiting));
		}
		return new Kits(Json.text(o, "active"), (int) Math.max(0, Math.min(1000, Json.whole(o, "target", 0))), kits);
	}

	/** The kit the player chose, or null. */
	public Kit active()
	{
		return active == null ? null : byId(active);
	}

	public Kit byId(String id)
	{
		for (Kit k : kits)
		{
			if (k.id.equals(id))
			{
				return k;
			}
		}
		return null;
	}

	/** The kit for a boss: the chosen one if its name or id says the boss, else the first that does. */
	public Kit forBoss(String key, String name)
	{
		Kit a = active();
		if (a != null && names(a, key, name))
		{
			return a;
		}
		for (Kit k : kits)
		{
			if (names(k, key, name))
			{
				return k;
			}
		}
		return null;
	}

	private static boolean names(Kit k, String key, String name)
	{
		String n = k.name.toLowerCase(Locale.ROOT);
		return (key != null && k.id.equalsIgnoreCase(key)) || (name != null && n.contains(name.toLowerCase(Locale.ROOT)));
	}

	public int getTarget()
	{
		return target;
	}

	public List<Kit> getKits()
	{
		return kits;
	}
}
