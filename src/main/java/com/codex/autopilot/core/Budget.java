package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * "I have 30 minutes": the best set of steps that fits. Anything going to waste is taken first,
 * in the plan's order; then the rest by what they save per minute spent, the way the plan
 * weighs chores, skipped steps last. Each step that still fits is taken and one that does not is
 * passed over, so a long run does not keep two short ones out.
 */
public enum Budget
{
	NONE("No limit", 0),
	MIN_15("15 min", 15),
	MIN_30("30 min", 30),
	MIN_60("60 min", 60);

	private final String label;
	private final int minutes;

	Budget(String label, int minutes)
	{
		this.label = label;
		this.minutes = minutes;
	}

	/** Minutes to fill, or 0 for no limit. */
	public int getMinutes()
	{
		return minutes;
	}

	/** "15", "30", "60" or "Any", for the selector. */
	public String getShort()
	{
		return minutes == 0 ? "Any" : String.valueOf(minutes);
	}

	@Override
	public String toString()
	{
		return label;
	}

	/** What fits the budget. */
	public static final class Fit
	{
		public static final Fit NONE = new Fit(Collections.emptyList(), 0, 0, 0);

		private final List<Candidate> steps;
		private final int total;
		private final int left;
		private final int shortest;

		Fit(List<Candidate> steps, int total, int left, int shortest)
		{
			this.steps = Collections.unmodifiableList(steps);
			this.total = total;
			this.left = left;
			this.shortest = shortest;
		}

		/** The steps chosen, in the order to do them. */
		public List<Candidate> getSteps()
		{
			return steps;
		}

		/** Minutes they take together. */
		public int getTotal()
		{
			return total;
		}

		/** Steps left for another time. */
		public int getLeft()
		{
			return left;
		}

		/** Minutes of the quickest step left out, or 0 when none was. */
		public int getShortest()
		{
			return shortest;
		}

		public Candidate first()
		{
			return steps.isEmpty() ? null : steps.get(0);
		}

		public boolean isEmpty()
		{
			return steps.isEmpty();
		}
	}

	/** The steps of a ranked plan that fit in {@code minutes}; everything when there is no limit. */
	public static Fit fit(Plan plan, int minutes, Session session)
	{
		if (plan == null || plan.isEmpty())
		{
			return Fit.NONE;
		}
		List<Candidate> ranked = plan.getSteps();
		if (minutes <= 0)
		{
			int total = 0;
			for (Candidate c : ranked)
			{
				total += c.getMinutes();
			}
			return new Fit(new ArrayList<>(ranked), total, 0, 0);
		}
		List<Candidate> order = new ArrayList<>(ranked);
		order.sort(Comparator
			.comparingInt((Candidate c) -> session != null && session.skipOrder(c.getKey()) > 0 ? 1 : 0)
			.thenComparingInt(c -> c.getTier() == Tier.WASTING ? 0 : 1)
			.thenComparingDouble(c -> c.getTier() == Tier.WASTING ? ranked.indexOf(c) : -c.density())
			.thenComparingInt(ranked::indexOf));
		List<Candidate> chosen = new ArrayList<>();
		int total = 0;
		int shortest = 0;
		for (Candidate c : order)
		{
			if (total + c.getMinutes() <= minutes)
			{
				chosen.add(c);
				total += c.getMinutes();
			}
			else if (shortest == 0 || c.getMinutes() < shortest)
			{
				shortest = c.getMinutes();
			}
		}
		return new Fit(chosen, total, ranked.size() - chosen.size(), shortest);
	}
}
