package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides which heads-up notifications are new: once when a step starts going to waste, and once
 * when a chore trip becomes ready ("Bird houses are full"). The plugin only passes the text on to
 * RuneLite's notifier.
 *
 * <p>A step is told about once per state. It is told again only after it changes: going to waste
 * after it was only ready, or ready again after it was gone (done) for {@link #REARM} seconds, so
 * a step that flickers out for a reading or two is not repeated. The first look after a login or
 * account change only notes what is already there, so logging in does not set off a burst of old
 * news. Steps hidden by a snooze or quiet mode are noted but not announced.</p>
 */
public final class Heads
{
	/** How long a step must be gone before its return counts as new. */
	public static final long REARM = 5 * 60;

	/** One notification to send. */
	public static final class Notice
	{
		public final String key;
		public final Tier tier;
		public final String text;

		Notice(String key, Tier tier, String text)
		{
			this.key = key;
			this.tier = tier;
			this.text = text;
		}

		@Override
		public String toString()
		{
			return text;
		}
	}

	private final Map<String, Tier> told = new HashMap<>();
	private final Map<String, Long> goneSince = new HashMap<>();
	private boolean primed;

	/**
	 * Looks at every step offered now and returns what to announce. {@code visible} holds the keys
	 * the player can see (not snoozed, not held back by quiet mode); {@code waste} and {@code ready}
	 * are the player's toggles for each kind of notice.
	 */
	public synchronized List<Notice> observe(List<Candidate> all, Set<String> visible, long now, boolean waste, boolean ready)
	{
		List<Notice> out = new ArrayList<>();
		Map<String, Candidate> present = new HashMap<>();
		if (all != null)
		{
			for (Candidate c : all)
			{
				if (c != null && (c.getTier() == Tier.WASTING || c.getTier() == Tier.READY && c.getKind().isChore()))
				{
					present.put(c.getKey(), c);
				}
			}
		}
		for (Candidate c : present.values())
		{
			goneSince.remove(c.getKey());
			Tier before = told.get(c.getKey());
			boolean shown = visible == null || visible.contains(c.getKey());
			if (c.getTier() == Tier.WASTING)
			{
				if (before != Tier.WASTING && primed && waste && shown)
				{
					out.add(new Notice(c.getKey(), Tier.WASTING, wasteText(c)));
				}
				told.put(c.getKey(), Tier.WASTING);
			}
			else
			{
				if (before == null && primed && ready && shown)
				{
					out.add(new Notice(c.getKey(), Tier.READY, readyText(c)));
				}
				// Cured, but still ready: a later disease is news again; the trip being ready is not.
				told.put(c.getKey(), Tier.READY);
			}
		}
		for (Iterator<Map.Entry<String, Tier>> it = told.entrySet().iterator(); it.hasNext(); )
		{
			String key = it.next().getKey();
			if (present.containsKey(key))
			{
				continue;
			}
			long since = goneSince.computeIfAbsent(key, k -> now);
			if (now - since >= REARM || now < since)
			{
				it.remove();
				goneSince.remove(key);
			}
		}
		primed = true;
		return out;
	}

	/** Forgets everything told; the next look only takes note. For a login or another account. */
	public synchronized void reset()
	{
		told.clear();
		goneSince.clear();
		primed = false;
	}

	static String wasteText(Candidate c)
	{
		return "Going to waste: " + c.headline();
	}

	static String readyText(Candidate c)
	{
		if (c.getKind() == Candidate.Kind.BIRDHOUSE_RUN && "Birdhouse run".equals(c.getOver()))
		{
			return "Bird houses are full (" + c.getTitle() + ")";
		}
		return "Ready now: " + c.getOver() + " (" + c.getTitle() + ")";
	}
}
