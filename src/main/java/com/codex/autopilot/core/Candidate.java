package com.codex.autopilot.core;

import java.util.Collections;
import java.util.List;
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
		GOAL("Goal"),
		/** Coins toward a Codex Gear Roadmap saving goal, at a Money Pilot or Boss Pilot rate. */
		EARN("Earn"),
		/** A trip to the boss pinned in Codex Boss Pilot. */
		BOSS("Boss trip"),
		/** The kit chosen in Codex Quartermaster has run out. */
		RESTOCK("Restock"),
		/** A collection log slot marked wanted in Codex Log Insight. */
		LOG("Collection log"),
		/** The Hunter Guild rumour: finish it, hand in its rare part, or ask for a new one. */
		RUMOUR("Hunter rumour"),
		/** Sailing port tasks: the voyage that collects and delivers the cargo, a bounty, or more tasks to take. */
		PORT_TASK("Port tasks");

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
	/** Where the step is done; the cheapest to reach is where it starts. */
	private final List<Tile> targets;
	/** How the player gets there from what Codex Wayfinder shares, or null when not costed. */
	private final Trip trip;

	public Candidate(String key, Kind kind, Tier tier, String over, String title, String why, String bring,
		int minutes, double urgency, long deadline, int count, int itemId)
	{
		this(key, kind, tier, over, title, why, bring, minutes, urgency, deadline, count, itemId, false, Collections.emptyList(), null);
	}

	private Candidate(String key, Kind kind, Tier tier, String over, String title, String why, String bring,
		int minutes, double urgency, long deadline, int count, int itemId, boolean paced, List<Tile> targets, Trip trip)
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
		this.targets = targets == null ? Collections.emptyList() : targets;
		this.trip = trip;
	}

	/** The same step taking the player's own learned time instead of the estimate. */
	public Candidate withPace(int minutes)
	{
		return new Candidate(key, kind, tier, over, title, why, bring, minutes, urgency, deadline, count, itemId, true, targets, trip);
	}

	/** The same step, done at these places. */
	public Candidate at(List<Tile> places)
	{
		List<Tile> list = places == null ? Collections.emptyList() : Collections.unmodifiableList(new java.util.ArrayList<>(places));
		return new Candidate(key, kind, tier, over, title, why, bring, minutes, urgency, deadline, count, itemId, paced, list, trip);
	}

	/** The same step with the journey to its start costed in. */
	public Candidate withTrip(Trip t)
	{
		return new Candidate(key, kind, tier, over, title, why, bring, minutes, urgency, deadline, count, itemId, paced, targets, t);
	}

	/** The same step with another bring line. */
	public Candidate withBring(String line)
	{
		return new Candidate(key, kind, tier, over, title, why, line, minutes, urgency, deadline, count, itemId, paced, targets, trip);
	}

	/** The same step with a note added to its why line. */
	public Candidate withNote(String note)
	{
		String w = why == null || why.isEmpty() ? note : why + "; " + note;
		return new Candidate(key, kind, tier, over, title, w, bring, minutes, urgency, deadline, count, itemId, paced, targets, trip);
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

	/** Whole minutes the journey to the start adds, or 0 when it is not costed. */
	public int getTravelMinutes()
	{
		return trip == null ? 0 : (trip.getSeconds() + 59) / 60;
	}

	/** The step's own minutes and the journey to it: what the ranking and the time budget count. */
	public int getTotalMinutes()
	{
		return minutes + getTravelMinutes();
	}

	/** The step's own time and the journey to it, to the second: what ranks two steps otherwise level. */
	public int getTotalSeconds()
	{
		return minutes * 60 + (trip == null ? 0 : trip.getSeconds());
	}

	/** Where the step is done, or empty when it has no place (a goal, say). */
	public List<Tile> getTargets()
	{
		return targets;
	}

	/** How to get there, such as "via Ardougne cloak, ~40s", or null. */
	public Trip getTrip()
	{
		return trip;
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

	/** Urgency per minute spent, journey included, so quick, valuable chores close at hand come first. */
	public double density()
	{
		return urgency * 60 / getTotalSeconds();
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
			&& Objects.equals(title, c.title) && Objects.equals(why, c.why) && Objects.equals(bring, c.bring)
			&& Objects.equals(trip, c.trip);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key, tier, title, why, minutes, deadline);
	}

	@Override
	public String toString()
	{
		return tier + " " + headline() + " (" + getTotalMinutes() + " min" + (trip == null ? "" : ", " + trip.text()) + ")";
	}
}
