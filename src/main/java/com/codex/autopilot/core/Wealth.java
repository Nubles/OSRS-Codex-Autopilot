package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * What Codex Ledger knows the account holds, read and never written. Ledger keeps its ledger per RS
 * profile under {@code codexledger/ledger}; Autopilot reads only {@code total} (net worth),
 * {@code stores} ({@code {"BANK":{"seen":ms,"items":{"995":1200000}},"INVENTORY":{...}}}) and
 * {@code prices} ({@code {"20997":1200000000}}). The rest of the ledger is Ledger's own business;
 * the parts read are pinned by a test here.
 */
public final class Wealth
{
	public static final String GROUP = "codexledger";
	public static final String KEY = "ledger";
	/** A platinum token is worth 1,000 coins (OSRS wiki, "Platinum token"). */
	static final int PLATINUM_VALUE = 1000;

	private final long total;
	private final Map<Integer, Long> held;
	private final Map<Integer, Long> prices;

	Wealth(long total, Map<Integer, Long> held, Map<Integer, Long> prices)
	{
		this.total = total;
		this.held = Collections.unmodifiableMap(held);
		this.prices = Collections.unmodifiableMap(prices);
	}

	public static Wealth of(long total, Map<Integer, Long> held, Map<Integer, Long> prices)
	{
		return new Wealth(total, new HashMap<>(held), new HashMap<>(prices));
	}

	public static Wealth read(Profile profile, Gson gson)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY));
	}

	/** The ledger's holdings, or null when it has not begun or cannot be read. */
	public static Wealth parse(Gson gson, String json)
	{
		JsonObject o = Json.object(Json.parse(gson, json));
		if (o == null)
		{
			return null;
		}
		try
		{
			long total = Json.whole(o, "total", 0);
			Map<Integer, Long> held = new HashMap<>();
			JsonObject stores = Json.object(o, "stores");
			if (stores != null)
			{
				for (Map.Entry<String, JsonElement> s : stores.entrySet())
				{
					add(held, Json.object(Json.object(s.getValue()), "items"));
				}
			}
			Map<Integer, Long> prices = new HashMap<>();
			add(prices, Json.object(o, "prices"));
			if (total <= 0 && held.isEmpty())
			{
				return null;
			}
			return new Wealth(Math.max(0, total), held, prices);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private static void add(Map<Integer, Long> into, JsonObject items)
	{
		if (items == null)
		{
			return;
		}
		for (Map.Entry<String, JsonElement> e : items.entrySet())
		{
			int id;
			try
			{
				id = Integer.parseInt(e.getKey().trim());
			}
			catch (NumberFormatException ex)
			{
				continue;
			}
			long n = BankSnapshot.number(e.getValue(), 0);
			if (id > 0 && n > 0)
			{
				into.merge(id, Math.min(n, Integer.MAX_VALUE), (a, b) -> Math.min(Long.MAX_VALUE / 2, a + b));
			}
		}
	}

	/** Net worth as Ledger last counted it, or 0. */
	public long getTotal()
	{
		return total;
	}

	/** Coins and platinum tokens across every place Ledger has seen. */
	public long coins()
	{
		return held.getOrDefault(net.runelite.api.gameval.ItemID.COINS, 0L)
			+ held.getOrDefault(net.runelite.api.gameval.ItemID.PLATINUM, 0L) * PLATINUM_VALUE;
	}

	public boolean holds(int id)
	{
		return held.getOrDefault(id, 0L) > 0;
	}

	/** What Ledger last priced an item at, or 0. */
	public long price(int id)
	{
		return Math.max(0, prices.getOrDefault(id, 0L));
	}
}
