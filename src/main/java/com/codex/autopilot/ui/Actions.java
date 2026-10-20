package com.codex.autopilot.ui;

/** What the panel's buttons ask the plugin to do. */
public interface Actions
{
	/** Move a step below the rest for this session. */
	void skip(String key);

	/** Hide a step for an hour. */
	void snooze(String key);

	/** Bring back everything skipped or snoozed. */
	void restore();
}
