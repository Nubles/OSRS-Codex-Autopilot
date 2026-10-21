package com.codex.autopilot.core;

import java.util.Objects;

/**
 * One thing the account could do next, with what is lost by waiting and how long it takes.
 *
 * <p>Urgency is what an hour of delay costs, as a share of the chore's own cycle: a full bird
 * house idles a whole run every 50 minutes, so four of them cost 4.8 an hour; a grown calquat
 * idles a fraction of its 19 hour growth. A deadline is when something is lost for good.</p>
 */
public final class Candidate
{
	/** What a step is, for its icon, colour and stable key. */
	public enum Kind
	{
		BIRDHOUSE_RUN("Birdhouse run"),
		TREE_RUN("Tree run"),
		PATCH_RUN("Special patches"),
		HERB_RUN("Herb run"),
		HOPS_RUN("Hops run"),
		DAILIES("Diary dailies"),
		CONTRACT("Farming contract"),
		KINGDOM("Kingdom of Miscellania"),
		FOCUS_GOAL("Focused goal"),
		GOAL("Goal");

		private final String label;

		Kind(String label)
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}

		/** A timed farming or Fossil Island trip, the kind Codex Rounds routes. */
		public boolean isChore()
		{
			return this == BIRDHOUSE_RUN || this == TREE_RUN || this == PATCH_RUN || this == HERB_RUN || this == HOPS_RUN;
		}
	}

	private final String key;
	private final Kind kind;
	private final Tier tier;
	private final String over;
	private final String title;
	private final String why;
	private final String bring;
	private final int minutes;
	private final double urgency;
	private final long deadline;
	private final int count;
	private final int itemId;
	private final boolean paced;

	public Candidate(String key, Kind kind, Tier tier, String over, String title, String why, String bring,
		int minutes, double urgency, long deadline, int count, int itemId)
	{
		this(key, kind, tier, over, title, why, bring, minutes, urgency, deadline, count, itemId, false);
	}

	private Candidate(String key, Kind kind, Tier tier, String over, String title, String why, String bring,
		int minutes, double urgency, long deadline, int count, int itemId, boolean paced)
	{
		this.key = key;
		this.kind = kind;
		this.tier = tier;
		this.over = over;
		this.title = title;
		this.why = why;
		this.bring = bring;
		this.minutes = Math.max(1, minutes);
		this.urgency = Double.isNaN(urgency) || urgency < 0 ? 0 : urgency;
		this.deadline = Math.max(0, deadline);
		this.count = count;
		this.itemId = itemId;
		this.paced = paced;
	}

	/** The same step taking the player's own learned time instead of the estimate. */
	public Candidate withPace(int minutes)
	{
		return new Candidate(key, kind, tier, over, title, why, bring, minutes, urgency, deadline, count, itemId, true);
	}

	/** The same step with a note added to its why line. */
	public Candidate withNote(String note)
	{
		String w = why == null || why.isEmpty() ? note : why + "; " + note;
		return new Candidate(key, kind, tier, over, title, w, bring, minutes, urgency, deadline, count, itemId, paced);
	}

	/** Stable for the session, so Skip and Snooze stick to the same chore or goal. */
	public String getKey()
	{
		return key;
	}

	public Kind getKind()
	{
		return kind;
	}

	public Tier getTier()
	{
		return tier;
	}

	/** The small line above the title, such as "Birdhouse run" or "Toward Quest cape". */
	public String getOver()
	{
		return over;
	}

	/** The step itself in a few words, such as "4 ready, 2 seaweed ready". */
	public String getTitle()
	{
		return title;
	}

	/** One line on why it is here and why now. */
	public String getWhy()
	{
		return why;
	}

	/** What to take along, or null. */
	public String getBring()
	{
		return bring;
	}

	/** About how many minutes it takes. */
	public int getMinutes()
	{
		return minutes;
	}

	/** True when the minutes are the player's own pace, learned from their runs, rather than an estimate. */
	public boolean isPaced()
	{
		return paced;
	}

	public double getUrgency()
	{
		return urgency;
	}

	/** Unix second it goes to waste, or 0 when nothing is lost for good. */
	public long getDeadline()
	{
		return deadline;
	}

	/** How many chores it covers, or 1. */
	public int getCount()
	{
		return count;
	}

	/** An item to show beside it, or -1. */
	public int getItemId()
	{
		return itemId;
	}

	/** The whole step as one line, such as "Birdhouse run: 4 ready, 2 seaweed ready". */
	public String headline()
	{
		return over == null || over.isEmpty() ? title : over + ": " + title;
	}

	/** Urgency per minute spent, so quick, valuable chores come first. */
	public double density()
	{
		return urgency / minutes;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Candidate))
		{
			return false;
		}
		Candidate c = (Candidate) o;
		return key.equals(c.key) && tier == c.tier && minutes == c.minutes && paced == c.paced && deadline == c.deadline && count == c.count
			&& Objects.equals(title, c.title) && Objects.equals(why, c.why) && Objects.equals(bring, c.bring);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key, tier, title, why, minutes, deadline);
	}

	@Override
	public String toString()
	{
		return tier + " " + headline() + " (" + minutes + " min)";
	}
}
