package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Autopilot's top steps, shared for the other Codex apps (Codex Home's Today tab reads them). Saved
 * per RS profile under {@code codexautopilot/planV1}, a contract that only grows:
 * {@code {"version":1,"savedAt":ms,"steps":[{"key":"chores:herb_run","kind":"HERB_RUN","tier":"READY",
 * "over":"Herb run","title":"5 ready","why":"...","minutes":12,"travel":"via Ardougne cloak, ~40s",
 * "itemId":257}],"fedBy":["Wayfinder","Ledger"]}}. {@code minutes} includes the journey; {@code travel}
 * is absent when the journey was not priced; {@code tier} is WASTING, READY, FOCUS or GOAL. A breaking
 * change would get a new key, never a higher version under this one.
 */
public final class SharedPlan
{
	public static final String KEY = "planV1";
	public static final int VERSION = 1;
	/** Steps shared: the next one and the few after it. */
	public static final int STEPS = 5;
	/** A plan whose steps are the same but whose words have moved on (a countdown) is saved at most this often. */
	public static final long REFRESH_MS = 5 * 60_000L;

	/** One shared step; plain fields so Gson writes them as they are. */
	public static final class Step
	{
		String key;
		String kind;
		String tier;
		String over;
		String title;
		String why;
		int minutes;
		String travel;
		int itemId;
	}

	int version = VERSION;
	long savedAt;
	List<Step> steps = new ArrayList<>();
	List<String> fedBy = new ArrayList<>();

	public static SharedPlan of(List<Candidate> ranked, List<String> fedBy, long nowMs)
	{
		SharedPlan p = new SharedPlan();
		p.savedAt = nowMs;
		if (ranked != null)
		{
			for (int i = 0; i < ranked.size() && p.steps.size() < STEPS; i++)
			{
				Candidate c = ranked.get(i);
				Step s = new Step();
				s.key = c.getKey();
				s.kind = c.getKind().name();
				s.tier = c.getTier().name();
				s.over = c.getOver();
				s.title = c.getTitle();
				s.why = c.getWhy();
				s.minutes = c.getTotalMinutes();
				s.travel = c.getTrip() == null ? null : c.getTrip().text();
				s.itemId = c.getItemId();
				p.steps.add(s);
			}
		}
		if (fedBy != null)
		{
			p.fedBy.addAll(fedBy);
		}
		return p;
	}

	/** What the steps are, without the words that drift: when it changes the plan is saved at once. */
	public String identity()
	{
		StringBuilder sb = new StringBuilder();
		for (Step s : steps)
		{
			sb.append(s.key).append('|').append(s.tier).append('|').append(s.title).append('|').append(s.travel).append(';');
		}
		return sb.append(fedBy).toString();
	}

	/** The whole plan as saved, less the time it was saved. */
	public String content(Gson gson)
	{
		long at = savedAt;
		savedAt = 0;
		String json = gson.toJson(this);
		savedAt = at;
		return json;
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	/**
	 * Decides when to save: at once when the steps change, otherwise only when the words have moved on and
	 * {@link #REFRESH_MS} has passed, so a ticking countdown does not rewrite the profile every few seconds.
	 */
	public static final class Saver
	{
		private String identity;
		private String content;
		private long savedAt;

		/** Whether this plan should be written now; remembers it when so. */
		public synchronized boolean due(SharedPlan plan, Gson gson, long nowMs)
		{
			String id = plan.identity();
			String body = plan.content(gson);
			boolean changed = !Objects.equals(id, identity);
			boolean drifted = !Objects.equals(body, content) && nowMs - savedAt >= REFRESH_MS;
			if (!changed && !drifted)
			{
				return false;
			}
			identity = id;
			content = body;
			savedAt = nowMs;
			return true;
		}

		public synchronized void reset()
		{
			identity = null;
			content = null;
			savedAt = 0;
		}
	}
}
