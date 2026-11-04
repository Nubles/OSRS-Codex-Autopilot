package com.codex.autopilot.core;

import java.util.Objects;

/**
 * How the player gets to where a step starts, priced from what Codex Wayfinder shares: a teleport
 * this account can use and the walk from where it lands, or a walk from where the player stands.
 */
public final class Trip
{
	private final String via;
	private final int seconds;
	private final boolean banked;

	Trip(String via, int seconds, boolean banked)
	{
		this.via = via;
		this.seconds = Math.max(0, seconds);
		this.banked = banked;
	}

	public static Trip walk(int seconds)
	{
		return new Trip(null, seconds, false);
	}

	public static Trip via(String teleport, int seconds, boolean banked)
	{
		return new Trip(teleport, seconds, banked);
	}

	/** The teleport, such as "Ardougne cloak", or null for a walk. */
	public String getVia()
	{
		return via;
	}

	/** About how many seconds the journey takes. */
	public int getSeconds()
	{
		return seconds;
	}

	/** True when the teleport was in the bank, not carried, when Wayfinder last looked. */
	public boolean isBanked()
	{
		return banked;
	}

	public boolean isWalk()
	{
		return via == null;
	}

	/** "via Ardougne cloak, ~40s", "via Varrock Teleport (from the bank), ~2 min" or "walk, ~20s", or "you're there". */
	public String text()
	{
		if (via == null && seconds == 0)
		{
			return "you're there";
		}
		String how = via == null ? "walk" : "via " + via + (banked ? " (from the bank)" : "");
		return how + ", " + time(seconds);
	}

	/** "~40s" under a minute (to the 5 seconds), else "~2 min" (rounded up). */
	public static String time(int seconds)
	{
		if (seconds < 60)
		{
			return "~" + Math.max(5, (seconds + 4) / 5 * 5) + "s";
		}
		return "~" + (seconds + 59) / 60 + " min";
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Trip))
		{
			return false;
		}
		Trip t = (Trip) o;
		return seconds == t.seconds && banked == t.banked && Objects.equals(via, t.via);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(via, seconds, banked);
	}

	@Override
	public String toString()
	{
		return text();
	}
}
