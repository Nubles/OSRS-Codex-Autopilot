package com.codex.autopilot.core;

import java.util.Collections;
import java.util.List;

/** The ranked steps, and what was held back. */
public final class Plan
{
	public static final Plan EMPTY = new Plan(Collections.emptyList(), 0, 0);

	private final List<Candidate> steps;
	private final int snoozed;
	private final int quiet;

	Plan(List<Candidate> steps, int snoozed, int quiet)
	{
		this.steps = Collections.unmodifiableList(steps);
		this.snoozed = snoozed;
		this.quiet = quiet;
	}

	public List<Candidate> getSteps()
	{
		return steps;
	}

	/** The one best thing to do now, or null. */
	public Candidate top()
	{
		return steps.isEmpty() ? null : steps.get(0);
	}

	/** Up to {@code n} steps after the top one. */
	public List<Candidate> after(int n)
	{
		if (steps.size() <= 1)
		{
			return Collections.emptyList();
		}
		return steps.subList(1, Math.min(steps.size(), 1 + n));
	}

	/** Steps snoozed and still asleep. */
	public int getSnoozed()
	{
		return snoozed;
	}

	/** Steps quiet mode is holding back. */
	public int getQuiet()
	{
		return quiet;
	}

	public long count(Tier tier)
	{
		return steps.stream().filter(c -> c.getTier() == tier).count();
	}

	public boolean isEmpty()
	{
		return steps.isEmpty();
	}
}
