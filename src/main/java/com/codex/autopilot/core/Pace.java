package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The player's own pace on each kind of chore, learned from runs they finished, so a step says
 * "~9 min (your pace)" instead of a guess. What is kept is minutes per stop (a house, a patch),
 * the last {@link #KEEP} runs of each kind; the median of them is trusted once there are
 * {@link #TRUST}. A step's time is then that median times the stops it covers.
 *
 * <p>Saved in Autopilot's own RS-profile config as
 * {@code {"version":1,"kinds":{"HERB_RUN":[1.9,2.1,1.7]}}}. A missing, damaged or newer value
 * reads as no pace learned, and any figure outside the sane range is dropped.</p>
 */
public final class Pace
{
	public static final String KEY = "paceV1";
	static final int VERSION = 1;
	/** Runs remembered per kind. */
	public static final int KEEP = 7;
	/** Runs needed before the learned time replaces the estimate. */
	public static final int TRUST = 3;
	/** Minutes per stop outside this range are a run interrupted or misread, not a pace. */
	public static final double MIN_STOP = 0.2;
	public static final double MAX_STOP = 15;

	private final Map<Candidate.Kind, List<Double>> samples = new EnumMap<>(Candidate.Kind.class);

	public static Pace empty()
	{
		return new Pace();
	}

	/** The saved pace, or an empty one when there is none or it cannot be read. Never null. */
	public static Pace parse(Gson gson, String json)
	{
		Pace p = new Pace();
		if (gson == null || json == null || json.trim().isEmpty())
		{
			return p;
		}
		try
		{
			JsonElement root = gson.fromJson(json, JsonElement.class);
			if (root == null || !root.isJsonObject())
			{
				return p;
			}
			JsonObject o = root.getAsJsonObject();
			JsonElement v = o.get("version");
			if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber() || v.getAsInt() != VERSION)
			{
				return p;
			}
			JsonElement kinds = o.get("kinds");
			if (kinds == null || !kinds.isJsonObject())
			{
				return p;
			}
			for (Map.Entry<String, JsonElement> e : kinds.getAsJsonObject().entrySet())
			{
				Candidate.Kind kind;
				try
				{
					kind = Candidate.Kind.valueOf(e.getKey());
				}
				catch (IllegalArgumentException ex)
				{
					continue;
				}
				if (!kind.isChore() || !e.getValue().isJsonArray())
				{
					continue;
				}
				for (JsonElement x : e.getValue().getAsJsonArray())
				{
					if (x != null && x.isJsonPrimitive() && x.getAsJsonPrimitive().isNumber())
					{
						p.add(kind, x.getAsDouble());
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			return new Pace();
		}
		return p;
	}

	public synchronized String toJson(Gson gson)
	{
		JsonObject kinds = new JsonObject();
		for (Map.Entry<Candidate.Kind, List<Double>> e : samples.entrySet())
		{
			JsonArray a = new JsonArray();
			for (double d : e.getValue())
			{
				a.add(new JsonPrimitive(Math.round(d * 100) / 100.0));
			}
			kinds.add(e.getKey().name(), a);
		}
		JsonObject root = new JsonObject();
		root.addProperty("version", VERSION);
		root.add("kinds", kinds);
		return gson.toJson(root);
	}

	/** Records one finished run's minutes per stop; nonsense is ignored. Returns whether it was kept. */
	public synchronized boolean add(Candidate.Kind kind, double minutesPerStop)
	{
		if (kind == null || !kind.isChore() || Double.isNaN(minutesPerStop) || minutesPerStop < MIN_STOP || minutesPerStop > MAX_STOP)
		{
			return false;
		}
		List<Double> list = samples.computeIfAbsent(kind, k -> new ArrayList<>());
		list.add(minutesPerStop);
		while (list.size() > KEEP)
		{
			list.remove(0);
		}
		return true;
	}

	public synchronized int samples(Candidate.Kind kind)
	{
		List<Double> list = samples.get(kind);
		return list == null ? 0 : list.size();
	}

	/** Median minutes per stop once trusted, otherwise -1. */
	public synchronized double perStop(Candidate.Kind kind)
	{
		List<Double> list = samples.get(kind);
		if (list == null || list.size() < TRUST)
		{
			return -1;
		}
		List<Double> sorted = new ArrayList<>(list);
		Collections.sort(sorted);
		int n = sorted.size();
		return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
	}

	/** The step with the learned time in place of the estimate, when the kind is trusted. */
	public Candidate apply(Candidate c)
	{
		if (c == null || !c.getKind().isChore())
		{
			return c;
		}
		double per = perStop(c.getKind());
		if (per <= 0)
		{
			return c;
		}
		return c.withPace((int) Math.max(1, Math.round(per * Math.max(1, c.getCount()))));
	}

	public synchronized boolean isEmpty()
	{
		return samples.isEmpty();
	}
}
