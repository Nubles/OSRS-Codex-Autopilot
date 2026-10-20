package com.codex.autopilot.core;

/** How much a step matters right now, most pressing first. */
public enum Tier
{
	/** Loses something for good soon: a daily before the reset, a diseased patch, the kingdom's next drop. */
	WASTING("Going to waste"),
	/** Ready now and losing a little while it waits: full bird houses, grown trees, dailies to collect. */
	READY("Ready"),
	/** The next step toward the goal pinned in focus in Codex Navigator. */
	FOCUS("Your focus"),
	/** The next step toward another pinned goal. */
	GOAL("Goal");

	private final String label;

	Tier(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
