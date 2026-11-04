package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.gameval.ItemID;

/**
 * The Gemstone Crab as a step, only while the player is at one of its mines. It is information, the
 * crab's own clock and where it goes, never what to do in a fight:
 *
 * <ul>
 * <li>"North mine, ~6 min left": how long until it burrows, read from its bar (which player damage
 * does not move), with where it goes next once the player's own sightings agree on it, and the
 * player's own xp and gems an hour there;</li>
 * <li>"Mine its shell": when the game said the shell is the player's to mine, with the seconds of
 * its 90 left; it goes to waste after them;</li>
 * <li>"Follow it": once it has burrowed, the cave at every mine leads to it, and the mine it most
 * likely went to is where Wayfinder prices the way.</li>
 * </ul>
 */
public final class Crabs
{
	public static final String FIGHT = "crab:fight";
	public static final String SHELL = "crab:shell";
	public static final String FOLLOW = "crab:follow";
	static final String OVER = "Gemstone Crab";
	/** An hour away from the crab loses an hour of training: a whole cycle of it. */
	static final double URGENCY = 1.5;
	/** The shell's gems are lost for good after its 90 seconds. */
	static final double SHELL_URGENCY = 4;

	private Crabs()
	{
	}

	public static List<Candidate> candidates(CrabLog log, long now)
	{
		if (log == null || log.getNear() == null)
		{
			return Collections.emptyList();
		}
		Candidate c;
		if (log.shellSecondsLeft() > 0)
		{
			c = shell(log, now);
		}
		else if (log.getMine() != null)
		{
			c = fight(log);
		}
		else
		{
			c = follow(log);
		}
		return Collections.singletonList(c);
	}

	static Candidate fight(CrabLog log)
	{
		GemstoneCrab.Mine m = log.getMine();
		int left = log.secondsLeft();
		String title = m.getLabel() + (left >= 0 ? ", " + leftText(left, log.leftFrom()) : "");
		List<String> why = new ArrayList<>();
		why.add("Then it burrows " + next(log, m));
		String pace = pace(log);
		if (pace != null)
		{
			why.add(pace);
		}
		return new Candidate(FIGHT, Candidate.Kind.GEMSTONE_CRAB, Tier.READY, OVER, title, String.join("; ", why),
			"A pickaxe, to mine its shell", left > 0 ? Math.max(1, (left + 59) / 60) : GemstoneCrab.STAY_SECONDS / 60,
			URGENCY, 0, 1, ItemID.UNCUT_RED_TOPAZ).at(Collections.singletonList(m.getTile()));
	}

	static Candidate shell(CrabLog log, long now)
	{
		int left = log.shellSecondsLeft();
		int gems = log.shellGemsLeft();
		String title = "Mine its shell, " + Format.plural(gems, "gem", "gems") + (gems < GemstoneCrab.GEMS_PER_SHELL ? " left" : "");
		String why = "It sinks in " + left + "s; the game says it is yours to mine";
		GemstoneCrab.Mine at = log.getBurrowedFrom() != null ? log.getBurrowedFrom() : log.getNear();
		return new Candidate(SHELL, Candidate.Kind.GEMSTONE_CRAB, Tier.WASTING, OVER, title, why, "A pickaxe", 1,
			SHELL_URGENCY, now + left, gems, ItemID.UNCUT_RED_TOPAZ).at(Collections.singletonList(at.getTile()));
	}

	static Candidate follow(CrabLog log)
	{
		GemstoneCrab.Mine from = log.getBurrowedFrom() != null ? log.getBurrowedFrom() : log.getNear();
		GemstoneCrab.Mine to = log.next(from);
		List<String> why = new ArrayList<>();
		why.add("The cave at every mine leads to where it is now");
		if (to != null)
		{
			why.add(seen(log, from, to));
		}
		else
		{
			why.add(log.getBurrowedFrom() == null ? "the wiki gives no order to its moves"
				: "it left " + from.getPhrase() + "; the wiki gives no order to its moves");
		}
		String pace = pace(log);
		if (pace != null)
		{
			why.add(pace);
		}
		String title = to != null ? "Follow it to " + to.getPhrase() : "Follow it through the cave";
		List<Tile> tiles = new ArrayList<>();
		for (GemstoneCrab.Mine m : spawnsAfter(log, from))
		{
			tiles.add(m.getTile());
		}
		return new Candidate(FOLLOW, Candidate.Kind.GEMSTONE_CRAB, Tier.READY, OVER, title, String.join("; ", why), null, 1,
			URGENCY, 0, 1, ItemID.UNCUT_RED_TOPAZ).at(tiles);
	}

	/** Where it may spawn after leaving a mine: the one the player's sightings name, else the other two. */
	public static List<GemstoneCrab.Mine> spawnsAfter(CrabLog log, GemstoneCrab.Mine from)
	{
		List<GemstoneCrab.Mine> out = new ArrayList<>();
		GemstoneCrab.Mine to = log.next(from);
		if (to != null)
		{
			out.add(to);
			return out;
		}
		for (GemstoneCrab.Mine m : GemstoneCrab.Mine.values())
		{
			if (m != from)
			{
				out.add(m);
			}
		}
		return out;
	}

	/** "to the east mine (4 of 4 moves you saw from here)", or "to the east or south-west mine". */
	public static String next(CrabLog log, GemstoneCrab.Mine from)
	{
		GemstoneCrab.Mine to = log.next(from);
		if (to != null)
		{
			return "to " + to.getPhrase() + " (" + log.moves(from, to) + " of " + log.movesFrom(from) + " moves you saw from here)";
		}
		List<GemstoneCrab.Mine> others = spawnsAfter(log, from);
		return "to " + others.get(0).getPhrase().replace(" mine", "") + " or " + others.get(1).getPhrase().substring(4);
	}

	/** "4 of 4 moves you saw from the north mine went there". */
	static String seen(CrabLog log, GemstoneCrab.Mine from, GemstoneCrab.Mine to)
	{
		return log.moves(from, to) + " of " + log.movesFrom(from) + " moves you saw from " + from.getPhrase() + " went there";
	}

	/** "~6 min left", or "~6 min left (est.)" when the bar's pace is not yet measured. */
	public static String leftText(int seconds, CrabLog.Left from)
	{
		String t = seconds < 60 ? "<1 min left" : "~" + Format.duration(seconds) + " left";
		return from == CrabLog.Left.WIKI ? t + " (est.)" : t;
	}

	/** "your 48k xp/hr, 19.2 gems/hr here", or null until enough is timed. */
	public static String pace(CrabLog log)
	{
		long xp = log.xpPerHour();
		double gems = log.gemsPerHour();
		if (xp < 0)
		{
			return null;
		}
		return "your " + Format.number(xp) + " xp/hr" + (gems >= 0 ? ", " + gemsText(gems) + " gems/hr" : "") + " here";
	}

	static String gemsText(double gems)
	{
		return gems >= 10 || gems == Math.floor(gems) ? String.valueOf(Math.round(gems)) : String.valueOf(gems);
	}
}
