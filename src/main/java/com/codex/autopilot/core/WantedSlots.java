package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The collection log slots marked wanted in Codex Log Insight, and the log's counters, read and never
 * written. Log Insight keeps its progress per RS profile under {@code codexloginsight/progress}; Autopilot
 * reads only {@code wanted} (item ids, in the order marked), {@code owned} (item ids) and {@code counters}
 * ({@code {"Vorkath kills":312}}). It is Log Insight's own save, so its shape is pinned by a test here.
 */
public final class WantedSlots
{
	public static final String GROUP = "codexloginsight";
	public static final String KEY = "progress";

	private final List<Integer> wanted;
	private final Map<String, Integer> counters;

	WantedSlots(List<Integer> wanted, Map<String, Integer> counters)
	{
		this.wanted = Collections.unmodifiableList(wanted);
		this.counters = Collections.unmodifiableMap(counters);
	}

	public static WantedSlots read(Profile profile, Gson gson)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY));
	}

	public static WantedSlots parse(Gson gson, String json)
	{
		JsonObject o = Json.object(Json.parse(gson, json));
		if (o == null)
		{
			return null;
		}
		Set<Integer> owned = new HashSet<>(ids(Json.array(o, "owned")));
		List<Integer> wanted = new ArrayList<>();
		for (int id : ids(Json.array(o, "wanted")))
		{
			if (!owned.contains(id) && !wanted.contains(id))
			{
				wanted.add(id);
			}
		}
		Map<String, Integer> counters = new HashMap<>();
		JsonObject c = Json.object(o, "counters");
		if (c != null)
		{
			for (Map.Entry<String, JsonElement> e : c.entrySet())
			{
				long n = BankSnapshot.number(e.getValue(), -1);
				if (n >= 0 && n <= Integer.MAX_VALUE)
				{
					counters.put(e.getKey(), (int) n);
				}
			}
		}
		if (wanted.isEmpty() && counters.isEmpty())
		{
			return null;
		}
		return new WantedSlots(wanted, counters);
	}

	private static List<Integer> ids(JsonArray a)
	{
		List<Integer> out = new ArrayList<>();
		if (a == null)
		{
			return out;
		}
		for (JsonElement e : a)
		{
			long id = BankSnapshot.number(e, -1);
			if (id > 0 && id <= Integer.MAX_VALUE)
			{
				out.add((int) id);
			}
		}
		return out;
	}

	/** Wanted slots not yet owned, first marked first. */
	public List<Integer> getWanted()
	{
		return wanted;
	}

	/** A counter of the log, such as "Vorkath kills", or -1. */
	public int counter(String label)
	{
		return counters.getOrDefault(label, -1);
	}
}
