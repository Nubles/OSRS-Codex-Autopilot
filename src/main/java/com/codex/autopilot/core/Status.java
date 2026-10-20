package com.codex.autopilot.core;

/** Where a chore stands on the board. */
public enum Status
{
	/** Something to do there now. */
	READY,
	/** Growing, filling or waiting, with a time it will be ready. */
	GROWING,
	/** Nothing to do: a grown spirit tree, a claimed daily. */
	DONE,
	/** Never seen, so the round should look. */
	UNSEEN,
	/** The account can't use it yet. */
	LOCKED,
	/** The player left it out. */
	OFF
}
