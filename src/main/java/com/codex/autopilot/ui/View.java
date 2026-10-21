package com.codex.autopilot.ui;

import com.codex.autopilot.core.Activity;
import com.codex.autopilot.core.Budget;
import com.codex.autopilot.core.Plan;
import java.util.Collections;
import java.util.List;

/** Everything the panel shows, built off the Swing thread and handed over whole. */
public final class View
{
	/** One source Autopilot reads, for the tiles at the foot of the panel. */
	public static final class Source
	{
		public final String label;
		public final String value;
		/** True when the source was read and gave something. */
		public final boolean live;

		public Source(String label, String value, boolean live)
		{
			this.label = label;
			this.value = value;
			this.live = live;
		}
	}

	public final boolean loggedIn;
	/** Whether there is an account profile to read at all. */
	public final boolean known;
	public final Plan plan;
	public final Activity activity;
	/** True when Codex Minigame Pilot is switched on, so it can be named as the one guiding. */
	public final boolean minigamePilot;
	/** What comes ready next when nothing is due, such as "Bird houses full in 23 min", or null. */
	public final String nextUp;
	public final boolean quiet;
	public final boolean skipped;
	public final List<Source> sources;
	/** The time the player said they have. */
	public final Budget budget;
	/** What fits it; every step when there is no limit. */
	public final Budget.Fit fit;

	public View(boolean loggedIn, boolean known, Plan plan, Activity activity, boolean minigamePilot, String nextUp,
		boolean quiet, boolean skipped, List<Source> sources)
	{
		this(loggedIn, known, plan, activity, minigamePilot, nextUp, quiet, skipped, sources, Budget.NONE, null);
	}

	public View(boolean loggedIn, boolean known, Plan plan, Activity activity, boolean minigamePilot, String nextUp,
		boolean quiet, boolean skipped, List<Source> sources, Budget budget, Budget.Fit fit)
	{
		this.loggedIn = loggedIn;
		this.known = known;
		this.plan = plan == null ? Plan.EMPTY : plan;
		this.activity = activity == null ? Activity.NONE : activity;
		this.minigamePilot = minigamePilot;
		this.nextUp = nextUp;
		this.quiet = quiet;
		this.skipped = skipped;
		this.sources = sources == null ? Collections.emptyList() : sources;
		this.budget = budget == null ? Budget.NONE : budget;
		this.fit = fit == null ? Budget.Fit.NONE : fit;
	}

	/** Whether a time budget is set, so the panel shows what fits rather than everything. */
	public boolean budgeted()
	{
		return budget != Budget.NONE;
	}

	public static View empty()
	{
		return new View(false, false, Plan.EMPTY, Activity.NONE, false, null, false, false, null);
	}

	public boolean steppingAside()
	{
		return activity != Activity.NONE;
	}
}
