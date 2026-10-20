package com.codex.autopilot.core;

import com.codex.autopilot.core.farming.Action;
import com.codex.autopilot.core.farming.PatchState;
import com.codex.autopilot.core.farming.Produce;

/** What a spot is doing now, predicted from the last time it was seen. */
public final class Reading
{
	private final Spot spot;
	private final Seen seen;
	private final Status status;
	private final PatchState patch;
	private final Birdhouse.State house;
	private final Action action;
	private final long readyAt;
	private final String summary;

	private Reading(Spot spot, Seen seen, Status status, PatchState patch, Birdhouse.State house, Action action, long readyAt, String summary)
	{
		this.spot = spot;
		this.seen = seen;
		this.status = status;
		this.patch = patch;
		this.house = house;
		this.action = action;
		this.readyAt = readyAt;
		this.summary = summary;
	}

	static Reading unseen(Spot spot)
	{
		return new Reading(spot, null, Status.UNSEEN, null, null, null, 0, "Not seen yet");
	}

	static Reading birdhouse(Spot spot, Seen seen, Status status, Birdhouse.State house, long readyAt, String summary)
	{
		return new Reading(spot, seen, status, null, house, null, readyAt, summary);
	}

	static Reading patch(Spot spot, Seen seen, Status status, PatchState patch, Action action, long readyAt, String summary)
	{
		return new Reading(spot, seen, status, patch, null, action, readyAt, summary);
	}

	/** The same reading under another status, such as switched off or locked. */
	public Reading as(Status s, String why)
	{
		return new Reading(spot, seen, s, patch, house, action, readyAt, why);
	}

	public Spot getSpot()
	{
		return spot;
	}

	public Seen getSeen()
	{
		return seen;
	}

	public Status getStatus()
	{
		return status;
	}

	/** The patch as last seen, or null for bird houses and unseen patches. */
	public PatchState getPatch()
	{
		return patch;
	}

	public Birdhouse.State getHouse()
	{
		return house;
	}

	/** The next thing to do there, for patches. */
	public Action getAction()
	{
		return action;
	}

	/** Unix second it is (or was) ready, or 0 when not known. */
	public long getReadyAt()
	{
		return readyAt;
	}

	public String getSummary()
	{
		return summary;
	}

	/** What is growing, or null. */
	public Produce getProduce()
	{
		return patch == null || patch.isEmpty() ? null : patch.getProduce();
	}

	public boolean isEmpty()
	{
		return patch != null && patch.isEmpty();
	}

	/** Seconds until ready, or 0 when ready or unknown. */
	public long secondsLeft(long now)
	{
		return status == Status.GROWING && readyAt > now ? readyAt - now : 0;
	}

	/** Whether a round should stop for it within the lookahead. */
	public boolean dueWithin(long now, long lookahead)
	{
		return status == Status.READY || status == Status.UNSEEN
			|| (status == Status.GROWING && readyAt > 0 && readyAt <= now + lookahead);
	}
}
