package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Hunter Guild's rumours as steps. A rumour never goes to waste, so it is ranked with the
 * goals: finishing the one in hand comes before asking for the next, and a found rare part is a
 * quick ready errand, since the next rumour (and its reward) waits on it.
 *
 * <ul>
 * <li>"Hunter rumour: Grey chinchompa for Cervus", where it is hunted and how likely the rare part
 * is, timed at the player's own pace once three rumours of that method are timed;</li>
 * <li>"Rumour done: Take the part back to Cervus", with the reward;</li>
 * <li>"New rumour from Aco or Teco", only for an account that does rumours (one is known
 * finished or has been seen) and is at least 46 Hunter.</li>
 * </ul>
 */
public final class Rumours
{
	/** A rumour's time before the player's own is learned. */
	public static final int ESTIMATE_MINUTES = 30;
	static final int HAND_IN_MINUTES = 3;
	static final int NEW_MINUTES = 3;
	static final double HUNT_URGENCY = 0.15;
	static final double HAND_IN_URGENCY = 0.4;
	static final double NEW_URGENCY = 0.1;
	/** A milestone further off than this many rumours is not worth a mention on a new rumour. */
	static final int MILESTONE_NEAR = 5;
	/** The steps' keys. */
	public static final String HUNT = "rumour:hunt";
	public static final String HAND_IN = "rumour:hand-in";
	public static final String NEW = "rumour:new";

	private Rumours()
	{
	}

	/** The one rumour step the book calls for, or none. */
	public static List<Candidate> candidates(RumourBook book)
	{
		if (book == null)
		{
			return Collections.emptyList();
		}
		Candidate c;
		if (book.getCreature() != null && book.hasPart())
		{
			c = handIn(book);
		}
		else if (book.getCreature() != null)
		{
			c = hunt(book);
		}
		else
		{
			c = fresh(book);
		}
		return c == null ? Collections.emptyList() : Collections.singletonList(c);
	}

	static Candidate hunt(RumourBook book)
	{
		Rumour.Creature cr = book.getCreature();
		Rumour.Method m = cr.getMethod();
		Rumour.Master master = book.getMaster();
		String title = master == null ? cr.getName() : cr.getName() + " for " + master.getName();
		String why = how(cr) + "; the part is 1 in " + m.getOneIn() + " a catch, sure by " + m.getPity();
		Candidate c = new Candidate(HUNT, Candidate.Kind.RUMOUR, Tier.GOAL, "Hunter rumour", title, why, m.getBring(),
			ESTIMATE_MINUTES, HUNT_URGENCY, 0, 1, m.getItemId()).at(cr.getTiles());
		int own = book.minutes(m);
		return own > 0 ? c.withPace(own) : c;
	}

	/** "Box traps at Piscatoris, Kourend Woodland or Isle of Souls", or "Barehanded at the Hunter Guild". */
	static String how(Rumour.Creature cr)
	{
		List<String> areas = cr.getAreas();
		String where = areas.size() == 1 ? areas.get(0)
			: String.join(", ", areas.subList(0, areas.size() - 1)) + " or " + areas.get(areas.size() - 1);
		String at = " at ";
		switch (cr.getMethod())
		{
			case BOX_TRAP:
				return "Box traps" + at + where;
			case BIRD_SNARE:
				return "Bird snares" + at + where;
			case NET_TRAP:
				return "Net traps" + at + where;
			case DEADFALL:
				return "Deadfalls" + at + where;
			case SPIKED_PIT:
				return "Spiked pits" + at + where;
			case GOAT_PIT:
				return "The goat pit" + at + where;
			case FALCONRY:
				return "Falconry" + at + where;
			case BUTTERFLY_NET:
				return "A butterfly net" + at + where;
			case BAREHANDED:
				return "Barehanded" + at + where;
			default:
				return "Tracking" + at + where;
		}
	}

	static Candidate handIn(RumourBook book)
	{
		Rumour.Master master = book.getMaster();
		String title = master == null ? "Take the part to the guild" : "Take the part back to " + master.getName();
		List<String> why = new ArrayList<>();
		if (master != null && book.getLevel() > 0)
		{
			why.add(master.getName() + " pays " + Format.number(master.getTier().experience(book.getLevel())) + " Hunter xp and "
				+ master.getTier().sack());
		}
		else if (master != null)
		{
			why.add(master.getName() + " pays Hunter xp and " + master.getTier().sack());
		}
		else
		{
			why.add("The guild hunter who gave it pays Hunter xp and a loot sack");
		}
		if (book.getDone() >= 0)
		{
			String milestone = milestone(book.getDone() + 1, true);
			if (milestone != null)
			{
				why.add(milestone);
			}
		}
		return new Candidate(HAND_IN, Candidate.Kind.RUMOUR, Tier.READY, "Rumour done", title, String.join("; ", why), null,
			HAND_IN_MINUTES, HAND_IN_URGENCY, 0, 1, book.getCreature().getMethod().getItemId())
			.at(Collections.singletonList(Rumour.GUILD));
	}

	static Candidate fresh(RumourBook book)
	{
		int level = book.getLevel();
		boolean doesRumours = book.getDone() > 0 || book.getSeenAt() > 0;
		if (level < Rumour.OPENS_AT || !doesRumours)
		{
			return null;
		}
		List<Rumour.Master> best = Rumour.Master.best(level, book.isAtFirstLight());
		if (best.isEmpty())
		{
			return null;
		}
		Rumour.Tier tier = best.get(0).getTier();
		String who = best.size() == 1 ? (best.get(0) == Rumour.Master.GILMAN ? "Huntmaster Gilman" : best.get(0).getName())
			: best.get(0).getName() + " or " + best.get(1).getName();
		String why = "At " + level + " Hunter, " + tier.getLabel().toLowerCase(java.util.Locale.ROOT) + " rumours pay "
			+ Format.number(tier.experience(level)) + " Hunter xp and " + tier.sack();
		if (book.getDone() >= 0)
		{
			String milestone = milestone(book.getDone() + 1, false);
			if (milestone != null)
			{
				why += "; " + milestone;
			}
		}
		return new Candidate(NEW, Candidate.Kind.RUMOUR, Tier.GOAL, "Hunter rumour", "New rumour from " + who, why, null,
			NEW_MINUTES, NEW_URGENCY, 0, 1, -1).at(Collections.singletonList(Rumour.GUILD));
	}

	/**
	 * What the next milestone unlocks, counting the rumour about to be finished as {@code next}:
	 * "your 50th unlocks cooking the antelopes and dashing kebbit" when it is that rumour, otherwise
	 * (when {@code exactOnly} is false) "4 more to 50: ...". Null when there is nothing to say.
	 */
	static String milestone(int next, boolean exactOnly)
	{
		int i = Rumour.nextMilestone(next - 1);
		if (i < 0)
		{
			return null;
		}
		int at = Rumour.MILESTONES[i];
		if (at == next)
		{
			return "your " + ordinal(at) + " unlocks " + Rumour.UNLOCKS[i];
		}
		int more = at - next + 1;
		return exactOnly || more > MILESTONE_NEAR ? null : more + " more to " + at + ": " + Rumour.UNLOCKS[i];
	}

	static String ordinal(int n)
	{
		int tens = n % 100;
		String suffix = tens >= 11 && tens <= 13 ? "th" : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
		return n + suffix;
	}
}
