package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.runelite.api.gameval.ItemID;

/**
 * The goals pinned in Codex Navigator, read from the board it saves for companion plugins
 * ({@code codexnavigator/goalBoard}, per RS profile, fields only ever added):
 * {@code {"version":1,"focus":"quest:Dragon Slayer II","goals":[{"key","type","title","complete",
 * "progress","next","hoursLeft","skills","quests"}]}}. A board with a version above 1 has broken
 * its shape and is read as no board. Each unfinished goal with a next step becomes a step here.
 */
public final class GoalBoard
{
	public static final String GROUP = "codexnavigator";
	public static final String KEY = "goalBoard";

	/** One pinned goal, as far as Autopilot reads it. */
	public static final class Goal
	{
		private final String key;
		private final String type;
		private final String title;
		private final boolean complete;
		private final double progress;
		private final String next;
		private final double hoursLeft;

		public Goal(String key, String type, String title, boolean complete, double progress, String next, double hoursLeft)
		{
			this.key = key;
			this.type = type;
			this.title = title;
			this.complete = complete;
			this.progress = progress;
			this.next = next;
			this.hoursLeft = hoursLeft;
		}

		public String getKey()
		{
			return key;
		}

		public String getType()
		{
			return type;
		}

		public String getTitle()
		{
			return title;
		}

		public boolean isComplete()
		{
			return complete;
		}

		/** 0 to 1. */
		public double getProgress()
		{
			return progress;
		}

		/** Navigator's next step for it, such as "Start Monkey Madness II", or null. */
		public String getNext()
		{
			return next;
		}

		/** Navigator's estimate of the hours to finish, or 0 when it has none. */
		public double getHoursLeft()
		{
			return hoursLeft;
		}
	}

	private final String focus;
	private final List<Goal> goals;

	GoalBoard(String focus, List<Goal> goals)
	{
		this.focus = focus;
		this.goals = Collections.unmodifiableList(goals);
	}

	public static GoalBoard read(Profile profile, Gson gson)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY));
	}

	/** The board, or null when there is none, it is damaged, or it is a version this reader does not know. */
	public static GoalBoard parse(Gson gson, String json)
	{
		if (gson == null || json == null || json.trim().isEmpty())
		{
			return null;
		}
		try
		{
			JsonElement root = gson.fromJson(json, JsonElement.class);
			if (root == null || !root.isJsonObject())
			{
				return null;
			}
			JsonObject o = root.getAsJsonObject();
			long version = BankSnapshot.version(o);
			if (version > 1 || version < 0)
			{
				return null;
			}
			JsonElement list = o.get("goals");
			List<Goal> goals = new ArrayList<>();
			if (list != null && list.isJsonArray())
			{
				JsonArray a = list.getAsJsonArray();
				for (JsonElement e : a)
				{
					Goal g = goal(e);
					if (g != null)
					{
						goals.add(g);
					}
				}
			}
			else if (list != null && !list.isJsonNull())
			{
				return null;
			}
			return new GoalBoard(text(o.get("focus")), goals);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private static Goal goal(JsonElement e)
	{
		if (e == null || !e.isJsonObject())
		{
			return null;
		}
		JsonObject o = e.getAsJsonObject();
		String key = text(o.get("key"));
		String title = text(o.get("title"));
		if (key == null || title == null)
		{
			return null;
		}
		JsonElement c = o.get("complete");
		boolean complete = c != null && c.isJsonPrimitive() && c.getAsJsonPrimitive().isBoolean() && c.getAsBoolean();
		return new Goal(key, text(o.get("type")), title, complete, clamp(real(o.get("progress")), 0, 1), text(o.get("next")),
			clamp(real(o.get("hoursLeft")), 0, 100_000));
	}

	private static String text(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString())
		{
			return null;
		}
		String s = e.getAsString().trim();
		return s.isEmpty() ? null : s;
	}

	private static double real(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return 0;
		}
		double d = e.getAsDouble();
		return Double.isNaN(d) || Double.isInfinite(d) ? 0 : d;
	}

	private static double clamp(double d, double lo, double hi)
	{
		return Math.max(lo, Math.min(hi, d));
	}

	public String getFocus()
	{
		return focus;
	}

	public List<Goal> getGoals()
	{
		return goals;
	}

	/** Unfinished goals with a next step. */
	public int open()
	{
		int n = 0;
		for (Goal g : goals)
		{
			if (!g.isComplete() && g.getNext() != null)
			{
				n++;
			}
		}
		return n;
	}

	/** A step for each unfinished goal with a next step; the focused one in its own tier. */
	public List<Candidate> candidates()
	{
		List<Candidate> out = new ArrayList<>();
		for (Goal g : goals)
		{
			if (g.isComplete() || g.getNext() == null)
			{
				continue;
			}
			boolean focused = g.getKey().equals(focus);
			int minutes = (int) Math.max(15, Math.min(60, Math.round(g.getHoursLeft() * 60)));
			String pct = Math.round(g.getProgress() * 100) + "% done";
			String left = g.getHoursLeft() > 0 ? ", about " + hours(g.getHoursLeft()) + " to go" : "";
			String why = (focused ? "Your focus in Codex Navigator, " : "Pinned in Codex Navigator, ") + pct + left;
			// Sooner goals first: the less there is left, the more a step moves it.
			double urgency = 1.0 / (1.0 + g.getHoursLeft());
			out.add(new Candidate("goal:" + g.getKey(), focused ? Candidate.Kind.FOCUS_GOAL : Candidate.Kind.GOAL,
				focused ? Tier.FOCUS : Tier.GOAL, "Toward " + g.getTitle(), sentence(g.getNext()), why, null, minutes, urgency, 0, 1,
				icon(g.getType())));
		}
		return out;
	}

	static String hours(double h)
	{
		if (h < 1)
		{
			return Math.max(1, Math.round(h * 60)) + " min";
		}
		if (h < 10)
		{
			double r = Math.round(h * 2) / 2.0;
			return (r == Math.floor(r) ? String.valueOf((long) r) : String.format(Locale.ROOT, "%.1f", r)) + " hr";
		}
		return Math.round(h) + " hr";
	}

	/** Navigator writes steps as titles ("Start Shield of Arrav"); keep them as they are, first letter up. */
	static String sentence(String next)
	{
		return Character.toUpperCase(next.charAt(0)) + next.substring(1);
	}

	private static int icon(String type)
	{
		if (type == null)
		{
			return ItemID.SKILLCAPE_QP;
		}
		switch (type)
		{
			case "diary":
				return ItemID.ARDY_CAPE_ELITE;
			case "skill":
				return ItemID.SKILLCAPE_MAX;
			default:
				return ItemID.SKILLCAPE_QP;
		}
	}
}
