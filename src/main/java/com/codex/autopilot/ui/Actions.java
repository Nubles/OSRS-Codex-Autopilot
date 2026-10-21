package com.codex.autopilot.ui;

import com.codex.autopilot.core.Budget;

/** What the panel's buttons ask the plugin to do. */
public interface Actions
{
	/** Move a step below the rest for this session. */
	void skip(String key);

	/** Hide a step for an hour. */
	void snooze(String key);

	/** Plan for this much time, from the selector at the top of the panel. */
	void budget(Budget budget);

	/** Bring back everything skipped or snoozed. */
	void restore();
}
