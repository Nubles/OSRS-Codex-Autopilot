package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The travel atlas Codex Wayfinder shares, read and never written. Wayfinder saves it per RS
 * profile under {@code codexwayfinder/atlasV1}:
 * {@code {"version":1,"savedAt":ms,"walkSecondsPerTile":0.3,"teleports":[{"name":"Ardougne cloak: Ardougne Farm",
 * "source":"Ardougne cloak","kind":"ITEM","x":2665,"y":3374,"plane":0,"seconds":4,"carried":true,"timed":false}],
 * "networks":{"FAIRY_RING":true},"timedTrips":0}}. Every teleport listed is one this account can use with what it
 * owns; {@code seconds} is the cast itself, timed from the player's own trips once Wayfinder has seen them. A
 * missing, damaged or other-version atlas reads as none.
 */
public final class Atlas
{
	public static final String GROUP = "codexwayfinder";
	public static final String KEY = "atlasV1";
	/**
	 * Seconds a tile when running: two tiles a 0.6 second game tick (OSRS wiki, "Energy": running moves
	 * the player two squares a tick). Used when the atlas gives no pace of its own.
	 */
	public static final double RUN_SECONDS_PER_TILE = 0.3;

	/** One teleport the account has. */
	public static final class Teleport
	{
		private final String name;
		private final String source;
		private final Tile landing;
		private final int seconds;
		private final boolean carried;

		public Teleport(String name, String source, Tile landing, int seconds, boolean carried)
		{
			this.name = name;
			this.source = source;
			this.landing = landing;
			this.seconds = Math.max(0, seconds);
			this.carried = carried;
		}

		/** Wayfinder's label, such as "Ardougne cloak: Ardougne Farm". */
		public String getName()
		{
			return name;
		}

		/** What casts it, such as "Ardougne cloak"; the name when Wayfinder gave none. */
		public String getSource()
		{
			return source != null ? source : name;
		}

		public Tile getLanding()
		{
			return landing;
		}

		public int getSeconds()
		{
			return seconds;
		}

		public boolean isCarried()
		{
			return carried;
		}
	}

	private final long savedAt;
	private final double walkSecondsPerTile;
	private final List<Teleport> teleports;
	private final Map<String, Boolean> networks;

	Atlas(long savedAt, double walkSecondsPerTile, List<Teleport> teleports, Map<String, Boolean> networks)
	{
		this.savedAt = savedAt;
		this.walkSecondsPerTile = walkSecondsPerTile;
		this.teleports = Collections.unmodifiableList(teleports);
		this.networks = Collections.unmodifiableMap(networks);
	}

	/** An atlas holding these, for tests and previews. */
	public static Atlas of(double walkSecondsPerTile, List<Teleport> teleports)
	{
		return new Atlas(0, walkSecondsPerTile, new ArrayList<>(teleports), new LinkedHashMap<>());
	}

	public static Atlas read(Profile profile, Gson gson)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY));
	}

	public static Atlas parse(Gson gson, String json)
	{
		JsonObject o = Json.object(Json.parse(gson, json));
		if (o == null || Json.version(o) != 1)
		{
			return null;
		}
		try
		{
			JsonArray list = Json.array(o, "teleports");
			if (list == null)
			{
				return null;
			}
			List<Teleport> out = new ArrayList<>();
			for (JsonElement e : list)
			{
				JsonObject t = Json.object(e);
				String name = Json.text(t, "name");
				long x = Json.whole(t, "x", -1);
				long y = Json.whole(t, "y", -1);
				long plane = Json.whole(t, "plane", 0);
				long seconds = Json.whole(t, "seconds", -1);
				if (name == null || x <= 0 || y <= 0 || x > 20_000 || y > 20_000 || plane < 0 || plane > 3 || seconds < 0 || seconds > 3600)
				{
					continue;
				}
				out.add(new Teleport(name, Json.text(t, "source"), Tile.of((int) x, (int) y, (int) plane), (int) seconds,
					Json.bool(t, "carried")));
			}
			Map<String, Boolean> networks = new LinkedHashMap<>();
			JsonObject n = Json.object(o, "networks");
			if (n != null)
			{
				for (Map.Entry<String, JsonElement> e : n.entrySet())
				{
					JsonElement v = e.getValue();
					networks.put(e.getKey(), v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() && v.getAsBoolean());
				}
			}
			double walk = Json.real(o, "walkSecondsPerTile", 0);
			return new Atlas(Json.whole(o, "savedAt", 0), walk, out, networks);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	public long getSavedAt()
	{
		return savedAt;
	}

	/** Seconds a tile of this player's walking, as Wayfinder learned it; the running pace when it gave none that makes sense. */
	public double getWalkSecondsPerTile()
	{
		return walkSecondsPerTile >= 0.1 && walkSecondsPerTile <= 2 ? walkSecondsPerTile : RUN_SECONDS_PER_TILE;
	}

	public List<Teleport> getTeleports()
	{
		return teleports;
	}

	/** How many travel networks (fairy rings, spirit trees...) are open. */
	public int openNetworks()
	{
		int n = 0;
		for (Boolean b : networks.values())
		{
			if (Boolean.TRUE.equals(b))
			{
				n++;
			}
		}
		return n;
	}
}
