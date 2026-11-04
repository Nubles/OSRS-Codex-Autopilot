package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.List;

/**
 * "Turael skipping", as a plan the player opts into: take the milestone tasks of the streak from a
 * master who pays well, and the tasks between them from Turael (or Spria), whose tasks pay nothing
 * but are quick and still count. Every rule here is the OSRS wiki's:
 *
 * <ul>
 * <li>"Slayer points": "bonus points are awarded after every 10th, 50th, 100th, 250th, and 1,000th
 * completed task"; Turael, Aya and Spria pay 0; "Tasks assigned by Turael/Spria give no points
 * but do contribute to the four needed to start gaining points."</li>
 * <li>"Turael": the method is "completing every 50th task with a Slayer master that gives a lot of
 * points per task", "using Turael for the rest of the tasks to get to every interval of 50 more
 * quickly".</li>
 * <li>"Turael" and "Slayer points": Turael can change a harder master's task for one of his own,
 * but "After a task change is processed, your task streak will also be reset to zero!" So the plan
 * never suggests that while a task from another master is held.</li>
 * </ul>
 */
public final class SkipPlan
{
	/** The master the player wants their milestone tasks from. */
	public enum Choice
	{
		BEST("Best points open to me", null),
		KONAR("Konar", Slayer.Master.KONAR),
		DURADEL("Duradel", Slayer.Master.DURADEL),
		NIEVE("Nieve", Slayer.Master.NIEVE),
		CHAELDAR("Chaeldar", Slayer.Master.CHAELDAR),
		VANNAKA("Vannaka", Slayer.Master.VANNAKA),
		MAZCHNA("Mazchna", Slayer.Master.MAZCHNA);

		private final String label;
		private final Slayer.Master master;

		Choice(String label, Slayer.Master master)
		{
			this.label = label;
			this.master = master;
		}

		/** The master named, or null for the best one open. */
		public Slayer.Master getMaster()
		{
			return master;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** Which tasks of the streak count as milestones to take from the high-points master. */
	public enum Every
	{
		TENTH("Every 10th task (5x and up)", 10),
		FIFTIETH("Every 50th task (15x and up)", 50);

		private final String label;
		private final int tasks;

		Every(String label, int tasks)
		{
			this.label = label;
			this.tasks = tasks;
		}

		public int getTasks()
		{
			return tasks;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** What the plan says to do now. */
	public static final class Advice
	{
		/** The master to ask for the next task, or null while a task is held. */
		public final Slayer.Master master;
		/** The task in a row the next task will be. */
		public final int task;
		/** True when the next task is one of the plan's milestones. */
		public final boolean milestone;
		/** One line for the player. */
		public final String text;

		Advice(Slayer.Master master, int task, boolean milestone, String text)
		{
			this.master = master;
			this.task = task;
			this.milestone = milestone;
			this.text = text;
		}
	}

	/** The time each plan takes over the same run of tasks, from the player's own task times. */
	public static final class Compare
	{
		public final int tasks;
		public final Slayer.Master master;
		public final int fullPoints;
		public final double fullHours;
		public final int skipPoints;
		public final double skipHours;

		Compare(int tasks, Slayer.Master master, int fullPoints, double fullHours, int skipPoints, double skipHours)
		{
			this.tasks = tasks;
			this.master = master;
			this.fullPoints = fullPoints;
			this.fullHours = fullHours;
			this.skipPoints = skipPoints;
			this.skipHours = skipHours;
		}

		public double fullPerHour()
		{
			return fullHours <= 0 ? 0 : fullPoints / fullHours;
		}

		public double skipPerHour()
		{
			return skipHours <= 0 ? 0 : skipPoints / skipHours;
		}
	}

	private final boolean on;
	private final Choice choice;
	private final Every every;

	public SkipPlan(boolean on, Choice choice, Every every)
	{
		this.on = on;
		this.choice = choice == null ? Choice.BEST : choice;
		this.every = every == null ? Every.TENTH : every;
	}

	public static SkipPlan off()
	{
		return new SkipPlan(false, Choice.BEST, Every.TENTH);
	}

	public boolean isOn()
	{
		return on;
	}

	public Choice getChoice()
	{
		return choice;
	}

	public Every getEvery()
	{
		return every;
	}

	/** True when this task in a row is one the plan takes from the high-points master. */
	public boolean isMilestone(int task)
	{
		return task >= Slayer.POINTS_FROM && task % every.getTasks() == 0;
	}

	/** The next task in a row after this streak that the plan counts as a milestone. */
	public int nextMilestone(int streak)
	{
		int t = Math.max(streak, 0) + 1;
		while (!isMilestone(t))
		{
			t++;
		}
		return t;
	}

	/**
	 * The master for the milestone tasks: the one chosen if this account can use them, else the
	 * one open that pays the most for this task. Null when the account is not known.
	 */
	public Slayer.Master milestoneMaster(Slayer.Account a, int task)
	{
		if (a == null || a.combat <= 0)
		{
			return null;
		}
		Slayer.Master chosen = choice.getMaster();
		if (chosen != null && chosen.opens(a))
		{
			return chosen;
		}
		return best(a, task);
	}

	/** The open master of the usual ladder who pays the most for this task in a row, or null. */
	public static Slayer.Master best(Slayer.Account a, int task)
	{
		Slayer.Master best = null;
		for (Slayer.Master m : Slayer.Master.ladder(a))
		{
			if (best == null || m.points(task, a) > best.points(task, a))
			{
				best = m;
			}
		}
		return best;
	}

	/** What to do now, or null when the plan is off or the streak is not known. */
	public Advice advise(SlayerLog log)
	{
		if (!on || log == null)
		{
			return null;
		}
		int streak = log.getStreak();
		Slayer.Account a = log.getAccount();
		if (streak < 0 || a.combat <= 0)
		{
			return null;
		}
		int task = streak + 1;
		int next = nextMilestone(streak);
		Slayer.Master high = milestoneMaster(a, next);
		if (high == null)
		{
			return null;
		}
		String highName = high.nameFor(a);
		String quick = quick(a);
		Slayer.Master held = log.hasTask() ? log.getMaster() : null;
		if (log.hasTask())
		{
			if (held == Slayer.Master.KRYSTILIA || held == Slayer.Master.MORTIMER)
			{
				return null;
			}
			if (held != null && held != Slayer.Master.TURAEL && held != Slayer.Master.SPRIA)
			{
				return new Advice(null, task, isMilestone(task), "Finish this task: a task change at Turael would reset your "
					+ Format.number(streak) + "-task streak to zero");
			}
			return new Advice(null, task, isMilestone(task), "Then " + Rumours.ordinal(next) + " in a row from " + highName + " for "
				+ Format.number(high.points(next, a)) + " points (" + Slayer.multiplier(next) + "x)");
		}
		if (isMilestone(task))
		{
			return new Advice(high, task, true, "Your " + Rumours.ordinal(task) + " task in a row is a milestone: take it from " + highName
				+ " for " + Format.number(high.points(task, a)) + " points (" + Slayer.multiplier(task) + "x)");
		}
		int left = next - task - 1;
		String why = task < Slayer.POINTS_FROM ? "no master pays for the first four" : "no points, but the task still counts";
		return new Advice(Slayer.Master.TURAEL, task, false, "A quick task from " + quick + ": " + why + "; "
			+ (left == 0 ? "then " : Format.plural(left, "more", "more") + " after it, then ") + "the " + Rumours.ordinal(next) + " from "
			+ highName + " pays " + Format.number(high.points(next, a)));
	}

	/** "Turael or Spria", or "Aya or Spria" after While Guthix Sleeps. */
	static String quick(Slayer.Account a)
	{
		return Slayer.Master.TURAEL.nameFor(a) + " or " + Slayer.Master.SPRIA.getName();
	}

	/**
	 * The plan against doing every task with the milestone master, over the next run of tasks up to
	 * and including the next milestone (the one after when the next task is one), timed at this player's own minutes a task with each. Null
	 * until both are known.
	 */
	public Compare compare(SlayerLog log)
	{
		if (log == null || log.getStreak() < 0)
		{
			return null;
		}
		Slayer.Account a = log.getAccount();
		int streak = log.getStreak();
		int next = nextMilestone(streak);
		if (next == streak + 1)
		{
			// The milestone is next: compare through to the one after, or there is nothing to compare.
			next = nextMilestone(next);
		}
		Slayer.Master high = milestoneMaster(a, next);
		if (high == null)
		{
			return null;
		}
		double highMin = log.minutesPerTask(high);
		double quickMin = log.minutesPerQuickTask();
		if (highMin <= 0 || quickMin <= 0)
		{
			return null;
		}
		int full = 0;
		int skip = 0;
		int milestones = 0;
		int tasks = next - streak;
		for (int t = streak + 1; t <= next; t++)
		{
			full += high.points(t, a);
			if (isMilestone(t))
			{
				skip += high.points(t, a);
				milestones++;
			}
		}
		return new Compare(tasks, high, full, tasks * highMin / 60, skip, (milestones * highMin + (tasks - milestones) * quickMin) / 60);
	}

	/** The masters this account can use for its next task in a row, with what each pays, the most first. */
	public static List<Pays> pays(SlayerLog log)
	{
		List<Pays> out = new ArrayList<>();
		if (log == null || log.getStreak() < 0)
		{
			return out;
		}
		Slayer.Account a = log.getAccount();
		if (a.combat <= 0)
		{
			return out;
		}
		int task = log.getStreak() + 1;
		for (Slayer.Master m : Slayer.Master.ladder(a))
		{
			out.add(new Pays(m, m.nameFor(a), m.points(task, a)));
		}
		out.sort((x, y) -> Integer.compare(y.points, x.points));
		return out;
	}

	/** A master and the points their next task pays. */
	public static final class Pays
	{
		public final Slayer.Master master;
		public final String name;
		public final int points;

		Pays(Slayer.Master master, String name, int points)
		{
			this.master = master;
			this.name = name;
			this.points = points;
		}
	}
}
