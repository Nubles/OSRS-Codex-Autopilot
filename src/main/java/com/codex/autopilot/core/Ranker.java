package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Puts every candidate step in one order. The bands come first: what is about to go to waste,
 * then ready chores, then the focused goal's next step, then other goals. Inside a band, losses
 * due soonest lead, then the chores that save the most per minute spent (a full set of bird
 * houses and seaweed together beats one grown calquat), then the goals with least left.
 *
 * <p>With "goal first" on, ready chores that lose little by waiting (urgency under
 * {@link #GOAL_FIRST_BELOW}, such as grown trees, dailies with hours left or the kingdom) drop
 * below the focused goal. Skipped steps sink below everything, in the order they were skipped;
 * snoozed steps are left out until their hour is up.</p>
 */
public final class Ranker
{
	/** Chores losing less than this share of a cycle per hour give way to the goal when it comes first. */
	public static final double GOAL_FIRST_BELOW = 1.0;

	private Ranker()
	{
	}

	/** The band a step sorts in: 0 going to waste, 1 ready, 2 focus, 3 goals; chores giving way sit at 2.5. */
	static double band(Candidate c, Settings s)
	{
		switch (c.getTier())
		{
			case WASTING:
				return 0;
			case READY:
				return s.isGoalFirst() && c.getUrgency() < GOAL_FIRST_BELOW ? 2.5 : 1;
			case FOCUS:
				return 2;
			default:
				return 3;
		}
	}

	public static Plan rank(List<Candidate> candidates, Settings s, Session session, long now)
	{
		List<Candidate> keep = new ArrayList<>();
		int snoozed = 0;
		int quiet = 0;
		for (Candidate c : candidates)
		{
			if (c == null)
			{
				continue;
			}
			if (session != null && session.isSnoozed(c.getKey(), now))
			{
				snoozed++;
				continue;
			}
			if (s.isQuiet() && c.getTier() != Tier.WASTING)
			{
				quiet++;
				continue;
			}
			keep.add(c);
		}
		keep.sort(order(s, session));
		return new Plan(keep, snoozed, quiet);
	}

	static Comparator<Candidate> order(Settings s, Session session)
	{
		Comparator<Candidate> skipped = Comparator.comparingInt(c -> session == null ? 0 : session.skipOrder(c.getKey()));
		return skipped
			.thenComparingDouble(c -> band(c, s))
			.thenComparingLong(c -> c.getDeadline() > 0 ? c.getDeadline() : Long.MAX_VALUE)
			.thenComparing(Comparator.comparingDouble(Candidate::density).reversed())
			.thenComparing(Comparator.comparingInt(Candidate::getCount).reversed())
			.thenComparing(Candidate::getKey);
	}
}
