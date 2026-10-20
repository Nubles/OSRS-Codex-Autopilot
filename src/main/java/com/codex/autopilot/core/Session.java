package com.codex.autopilot.core;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the player set aside this session: skipped steps sink below the rest, in the order they
 * were skipped; snoozed steps stay out of sight until their hour is up. Nothing here is saved.
 */
public final class Session
{
	public static final long SNOOZE = 3600;

	private final Map<String, Integer> skipped = new LinkedHashMap<>();
	private final Map<String, Long> snoozed = new HashMap<>();
	private int skips;

	public synchronized void skip(String key)
	{
		skipped.put(key, ++skips);
	}

	public synchronized void snooze(String key, long now)
	{
		snoozed.put(key, now + SNOOZE);
	}

	/** Brings everything set aside back. */
	public synchronized void clear()
	{
		skipped.clear();
		snoozed.clear();
	}

	/** 0 when not skipped, otherwise the order it was skipped in, from 1. */
	public synchronized int skipOrder(String key)
	{
		return skipped.getOrDefault(key, 0);
	}

	public synchronized boolean isSnoozed(String key, long now)
	{
		Long until = snoozed.get(key);
		if (until == null)
		{
			return false;
		}
		if (until <= now)
		{
			snoozed.remove(key);
			return false;
		}
		return true;
	}

	/** Unix second the snooze ends, or 0. */
	public synchronized long snoozedUntil(String key)
	{
		return snoozed.getOrDefault(key, 0L);
	}

	public synchronized boolean isEmpty()
	{
		return skipped.isEmpty() && snoozed.isEmpty();
	}
}
