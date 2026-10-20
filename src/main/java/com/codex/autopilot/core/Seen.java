package com.codex.autopilot.core;

import java.util.Objects;

/** A variable's value and the unix second it was first seen holding it. */
public final class Seen
{
	private final int value;
	private final long time;

	public Seen(int value, long time)
	{
		this.value = value;
		this.time = time;
	}

	public int getValue()
	{
		return value;
	}

	public long getTime()
	{
		return time;
	}

	/** Reads RuneLite's Time Tracking form, "value:unixtime". */
	public static Seen parse(String stored)
	{
		if (stored == null)
		{
			return null;
		}
		String[] parts = stored.split(":");
		if (parts.length != 2)
		{
			return null;
		}
		try
		{
			long time = Long.parseLong(parts[1]);
			return time > 0 ? new Seen(Integer.parseInt(parts[0]), time) : null;
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Seen))
		{
			return false;
		}
		Seen s = (Seen) o;
		return value == s.value && time == s.time;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(value, time);
	}

	@Override
	public String toString()
	{
		return value + ":" + time;
	}
}
