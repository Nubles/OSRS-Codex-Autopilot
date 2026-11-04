package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.gameval.ItemID;

/**
 * The Slayer task as a step. A task never goes to waste, so it is ranked with the goals:
 *
 * <ul>
 * <li>"Slayer task: Kurask, 87 left", with where they live (or where Konar sent you), what the
 * fight needs, and the time left at the player's own kills an hour once twenty kills are timed;</li>
 * <li>"New task from Duradel" once a task is done, for an account that does Slayer: the most
 * demanding master the account's combat, Slayer level and quests open, with the points the next
 * task in a row pays there, and Konar's when she pays more.</li>
 * </ul>
 */
public final class Slayers
{
	/** A task's time before the player's own pace is learned. */
	public static final int ESTIMATE_MINUTES = 30;
	static final int NEW_MINUTES = 3;
	static final double TASK_URGENCY = 0.15;
	static final double NEW_URGENCY = 0.1;
	public static final String TASK = "slayer:task";
	public static final String NEW = "slayer:new";

	private Slayers()
	{
	}

	public static List<Candidate> candidates(SlayerLog log)
	{
		if (log == null)
		{
			return Collections.emptyList();
		}
		Candidate c = log.hasTask() ? task(log) : fresh(log);
		return c == null ? Collections.emptyList() : Collections.singletonList(c);
	}

	static Candidate task(SlayerLog log)
	{
		Slayer.Task facts = Slayer.Task.of(log.getRow());
		String name = log.getName() == null ? "Your task" : log.getName();
		String title = name + ", " + log.getAmount() + " left";
		List<String> why = new ArrayList<>();
		String where = where(log, facts);
		if (where != null)
		{
			why.add(where);
		}
		int own = log.minutesLeft();
		if (own > 0)
		{
			why.add("at your " + Format.number(Math.round(log.killsPerHour())) + " kills/hr");
		}
		Slayer.Master m = log.getMaster();
		if (m != null && log.getArea() == null)
		{
			// Konar's area already names her.
			why.add("for " + m.nameFor(log.getAccount()));
		}
		List<Tile> tiles = tiles(log, facts);
		Candidate c = new Candidate(TASK, Candidate.Kind.SLAYER, Tier.GOAL, "Slayer task", title, String.join("; ", why),
			facts == null ? null : facts.getBring(), ESTIMATE_MINUTES, TASK_URGENCY, 0, log.getAmount(),
			facts == null ? ItemID.SLAYER_GEM : facts.getItemId()).at(tiles);
		return own > 0 ? c.withPace(own) : c;
	}

	/** "Konar sends you to the Smoke Dungeon", or the places the monster lives, or null when unchecked. */
	public static String where(SlayerLog log, Slayer.Task facts)
	{
		if (log.getArea() != null)
		{
			Slayer.Master m = log.getMaster();
			return (m == null ? "Assigned to " : m.nameFor(log.getAccount()) + " sends you to ") + log.getArea();
		}
		if (facts == null)
		{
			return null;
		}
		List<String> names = new ArrayList<>();
		for (Slayer.Place p : facts.getPlaces())
		{
			names.add(p.getName());
		}
		String s = Slayer.either(names);
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static List<Tile> tiles(SlayerLog log, Slayer.Task facts)
	{
		List<Tile> out = new ArrayList<>();
		if (facts == null)
		{
			return out;
		}
		if (log.getArea() != null)
		{
			Slayer.Place p = facts.place(log.getArea());
			if (p != null && p.getTile() != null)
			{
				out.add(p.getTile());
			}
			return out;
		}
		for (Slayer.Place p : facts.getPlaces())
		{
			if (p.getTile() != null)
			{
				out.add(p.getTile());
			}
		}
		return out;
	}

	/** The master to ask next, or null when the account is not known well enough. */
	public static Slayer.Master next(SlayerLog log)
	{
		Slayer.Account a = log.getAccount();
		if (a.combat <= 0)
		{
			return null;
		}
		List<Slayer.Master> ladder = Slayer.Master.ladder(a);
		return ladder.isEmpty() ? null : ladder.get(0);
	}

	/** "your 10th task in a row pays 75 points there; Konar pays 90", or what is known of it. */
	public static String points(SlayerLog log, Slayer.Master m)
	{
		Slayer.Account a = log.getAccount();
		int streak = log.getStreak();
		if (streak < 0)
		{
			return null;
		}
		int task = streak + 1;
		if (task < Slayer.POINTS_FROM)
		{
			int more = Slayer.POINTS_FROM - task;
			return "points start " + (more == 0 ? "with this task" : "after " + Format.plural(more, "more task", "more tasks"));
		}
		int pts = m.points(task, a);
		String s = "your " + Rumours.ordinal(task) + " in a row pays " + Format.number(pts) + " points"
			+ (Slayer.multiplier(task) > 1 ? " (" + Slayer.multiplier(task) + "x)" : "");
		if (m != Slayer.Master.KONAR && Slayer.Master.KONAR.opens(a) && Slayer.Master.KONAR.points(task, a) > pts)
		{
			s += "; Konar pays " + Format.number(Slayer.Master.KONAR.points(task, a));
		}
		return s;
	}

	static Candidate fresh(SlayerLog log)
	{
		if (!log.doesSlayer())
		{
			return null;
		}
		Slayer.Master m = next(log);
		if (m == null)
		{
			return null;
		}
		Slayer.Account a = log.getAccount();
		List<String> why = new ArrayList<>();
		why.add(m == Slayer.Master.TURAEL ? "The master open to you at " + a.combat + " combat"
			: "The highest master your " + a.combat + " combat" + (m.getSlayer() > 0 ? " and " + a.slayer + " Slayer" : "") + " reach");
		String pts = points(log, m);
		if (pts != null)
		{
			why.add(pts);
		}
		return new Candidate(NEW, Candidate.Kind.SLAYER, Tier.GOAL, "Slayer", "New task from " + m.nameFor(a), String.join("; ", why),
			null, NEW_MINUTES, NEW_URGENCY, 0, 1, ItemID.SLAYER_GEM);
	}
}
