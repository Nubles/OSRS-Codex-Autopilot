package com.codex.autopilot.core;

/** What the player chose in the config, as the core needs it. */
public final class Settings
{
	private boolean chores = true;
	private boolean herbs = true;
	private boolean hops = true;
	private boolean rounds;
	private boolean dailies = true;
	private boolean kingdom = true;
	private boolean rumours = true;
	private boolean kitten = true;
	private boolean portTasks = true;
	private boolean slayer = true;
	private boolean gemstoneCrab = true;
	private boolean navigator = true;
	private boolean bankSteward = true;
	private boolean wayfinder = true;
	private boolean moneyPilot = true;
	private boolean gearRoadmap = true;
	private boolean ledger = true;
	private boolean quartermaster = true;
	private boolean bossPilot = true;
	private boolean roundsDailies = true;
	private boolean logInsight = true;
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

	public Settings herbs(boolean v)
	{
		herbs = v;
		return this;
	}

	public Settings hops(boolean v)
	{
		hops = v;
		return this;
	}

	/** Codex Rounds is switched on, so chore steps can hand the route over to it. */
	public Settings rounds(boolean v)
	{
		rounds = v;
		return this;
	}

	/** The pet kitten's food and attention as a step. */
	public Settings kitten(boolean v)
	{
		kitten = v;
		return this;
	}

	/** The Hunter Guild rumour as a step. */
	public Settings rumours(boolean v)
	{
		rumours = v;
		return this;
	}

	/** The Slayer task as a step. */
	public Settings slayer(boolean v)
	{
		slayer = v;
		return this;
	}

	public boolean isSlayer()
	{
		return slayer;
	}

	/** The Gemstone Crab as a step while at one of its mines. */
	public Settings gemstoneCrab(boolean v)
	{
		gemstoneCrab = v;
		return this;
	}

	public boolean isGemstoneCrab()
	{
		return gemstoneCrab;
	}

	/** The Sailing port tasks as steps. */
	public Settings portTasks(boolean v)
	{
		portTasks = v;
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

	public Settings wayfinder(boolean v)
	{
		wayfinder = v;
		return this;
	}

	public Settings moneyPilot(boolean v)
	{
		moneyPilot = v;
		return this;
	}

	public Settings gearRoadmap(boolean v)
	{
		gearRoadmap = v;
		return this;
	}

	public Settings ledger(boolean v)
	{
		ledger = v;
		return this;
	}

	public Settings quartermaster(boolean v)
	{
		quartermaster = v;
		return this;
	}

	public Settings bossPilot(boolean v)
	{
		bossPilot = v;
		return this;
	}

	public Settings roundsDailies(boolean v)
	{
		roundsDailies = v;
		return this;
	}

	public Settings logInsight(boolean v)
	{
		logInsight = v;
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

	/** Herb runs, with the allotments and flowers beside them. */
	public boolean isHerbs()
	{
		return herbs;
	}

	public boolean isHops()
	{
		return hops;
	}

	public boolean isRounds()
	{
		return rounds;
	}

	public boolean isKitten()
	{
		return kitten;
	}

	public boolean isRumours()
	{
		return rumours;
	}

	public boolean isPortTasks()
	{
		return portTasks;
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

	public boolean isWayfinder()
	{
		return wayfinder;
	}

	public boolean isMoneyPilot()
	{
		return moneyPilot;
	}

	public boolean isGearRoadmap()
	{
		return gearRoadmap;
	}

	public boolean isLedger()
	{
		return ledger;
	}

	public boolean isQuartermaster()
	{
		return quartermaster;
	}

	public boolean isBossPilot()
	{
		return bossPilot;
	}

	public boolean isRoundsDailies()
	{
		return roundsDailies;
	}

	public boolean isLogInsight()
	{
		return logInsight;
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
