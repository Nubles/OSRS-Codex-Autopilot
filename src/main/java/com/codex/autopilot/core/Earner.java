package com.codex.autopilot.core;

/** A way this player has made money, at the rate their own play has shown. */
public final class Earner
{
	private final String key;
	private final String doing;
	private final long perHour;
	private final long playedMs;
	private final String from;

	public Earner(String key, String doing, long perHour, long playedMs, String from)
	{
		this.key = key;
		this.doing = doing;
		this.perHour = perHour;
		this.playedMs = playedMs;
		this.from = from;
	}

	public String getKey()
	{
		return key;
	}

	/** What it is, such as "Killing Vorkath" or "Vorkath trips". */
	public String getDoing()
	{
		return doing;
	}

	/** Coins an hour. */
	public long getPerHour()
	{
		return perHour;
	}

	public long getPlayedMs()
	{
		return playedMs;
	}

	/** The app that clocked it, such as "Money Pilot". */
	public String getFrom()
	{
		return from;
	}

	/** The best paying of these, or null. */
	public static Earner best(Iterable<Earner> all)
	{
		Earner best = null;
		if (all == null)
		{
			return null;
		}
		for (Earner e : all)
		{
			if (e != null && e.perHour > 0 && (best == null || e.perHour > best.perHour))
			{
				best = e;
			}
		}
		return best;
	}

	@Override
	public String toString()
	{
		return doing + " " + perHour + "/hr (" + from + ")";
	}
}
