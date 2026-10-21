package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Times the player's chore runs, to teach {@link Pace}. A run is timed from its first stop done to
 * its last: the moment the step's count of ready stops first drops (a house reset, a patch
 * replanted) to the moment the step is gone because every stop is done. That span covers every
 * stop but the first, so it is divided by the stops less one to give minutes per stop.
 *
 * <p>Timing from the first stop done, rather than from when the step reached the top of the
 * queue, keeps a run the player left waiting for an hour from reading as an hour's work, and
 * needs no guess at where the player is walking. Only steps that were first in the queue while
 * being worked are learned from, and a run is dropped, not learned, when Autopilot loses sight
 * of it: no reading for {@link #GAP} seconds (logged out, plugin off), or the caller abandons it
 * because a setting changed. Runs of one stop cannot be timed this way and are not learned.</p>
 */
public final class PaceWatch
{
	/** Longer than this between readings and every run in progress is dropped. */
	public static final long GAP = 120;

	/** One finished run, ready to add to the pace. */
	public static final class Sample
	{
		public final Candidate.Kind kind;
		public final double minutesPerStop;

		Sample(Candidate.Kind kind, double minutesPerStop)
		{
			this.kind = kind;
			this.minutesPerStop = minutesPerStop;
		}

		@Override
		public String toString()
		{
			return kind + " " + minutesPerStop;
		}
	}

	private static final class Run
	{
		final Candidate.Kind kind;
		int count;
		int peak;
		long firstDone;
		boolean top;

		Run(Candidate.Kind kind, int count)
		{
			this.kind = kind;
			this.count = count;
			this.peak = count;
		}
	}

	private final Map<String, Run> runs = new HashMap<>();
	private long last;

	/**
	 * Looks at every chore step offered now (before snoozing or quiet mode hide any) and the key of
	 * the step shown first. Returns the runs that finished since the last look.
	 */
	public synchronized List<Sample> observe(List<Candidate> all, String topKey, long now)
	{
		List<Sample> out = new ArrayList<>();
		if (last > 0 && (now - last > GAP || now < last))
		{
			runs.clear();
		}
		last = now;
		Map<String, Candidate> chores = new HashMap<>();
		if (all != null)
		{
			for (Candidate c : all)
			{
				if (c != null && c.getKind().isChore())
				{
					chores.put(c.getKey(), c);
				}
			}
		}
		for (Iterator<Map.Entry<String, Run>> it = runs.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<String, Run> e = it.next();
			if (chores.containsKey(e.getKey()))
			{
				continue;
			}
			Run r = e.getValue();
			it.remove();
			if (r.top && r.firstDone > 0 && r.peak >= 2)
			{
				double per = (now - r.firstDone) / 60.0 / (r.peak - 1);
				if (per >= Pace.MIN_STOP && per <= Pace.MAX_STOP)
				{
					out.add(new Sample(r.kind, per));
				}
			}
		}
		for (Candidate c : chores.values())
		{
			Run r = runs.get(c.getKey());
			if (r == null)
			{
				r = new Run(c.getKind(), c.getCount());
				runs.put(c.getKey(), r);
			}
			else
			{
				if (c.getCount() < r.count && r.firstDone == 0)
				{
					r.firstDone = now;
				}
				r.count = c.getCount();
				r.peak = Math.max(r.peak, c.getCount());
			}
			if (c.getKey().equals(topKey))
			{
				r.top = true;
			}
			else if (r.firstDone == 0)
			{
				// Led once but something else is first now, before any of it was done: it has to lead
				// again, or be led while worked, to count as the step the player was on.
				r.top = false;
			}
		}
		return out;
	}

	/** Drops every run in progress, as when the player logs out or a setting hides chores. */
	public synchronized void abandon()
	{
		runs.clear();
		last = 0;
	}

	synchronized int tracking()
	{
		return runs.size();
	}
}
