package com.codex.autopilot.core;

/** What the player chose in the config, as the core needs it. */
public final class Settings
{
	private boolean chores = true;
	private boolean dailies = true;
	private boolean kingdom = true;
	private boolean navigator = true;
	private boolean bankSteward = true;
	private boolean quiet;
	private boolean goalFirst;
	private int approvalTarget = 90;
	private int cofferSlack = 150_000;
	private long wasteHorizon = 3 * 3600;

	public static Settings defaults()
	{
		return new Settings();
	}

	public Settings chores(boolean v)
	{
		chores = v;
		return this;
	}

	public Settings dailies(boolean v)
	{
		dailies = v;
		return this;
	}

	public Settings kingdom(boolean v)
	{
		kingdom = v;
		return this;
	}

	public Settings navigator(boolean v)
	{
		navigator = v;
		return this;
	}

	public Settings bankSteward(boolean v)
	{
		bankSteward = v;
		return this;
	}

	public Settings quiet(boolean v)
	{
		quiet = v;
		return this;
	}

	public Settings goalFirst(boolean v)
	{
		goalFirst = v;
		return this;
	}

	public Settings approvalTarget(int v)
	{
		approvalTarget = Math.max(0, Math.min(100, v));
		return this;
	}

	public Settings cofferSlack(int v)
	{
		cofferSlack = Math.max(0, v);
		return this;
	}

	public Settings wasteHorizon(long seconds)
	{
		wasteHorizon = Math.max(0, seconds);
		return this;
	}

	public boolean isChores()
	{
		return chores;
	}

	public boolean isDailies()
	{
		return dailies;
	}

	public boolean isKingdom()
	{
		return kingdom;
	}

	public boolean isNavigator()
	{
		return navigator;
	}

	public boolean isBankSteward()
	{
		return bankSteward;
	}

	/** Only show what is about to go to waste. */
	public boolean isQuiet()
	{
		return quiet;
	}

	/** Put the focused goal ahead of ready chores that lose little by waiting. */
	public boolean isGoalFirst()
	{
		return goalFirst;
	}

	public int getApprovalTarget()
	{
		return approvalTarget;
	}

	public int getCofferSlack()
	{
		return cofferSlack;
	}

	/** How close a loss has to be to count as going to waste, in seconds. */
	public long getWasteHorizon()
	{
		return wasteHorizon;
	}
}
