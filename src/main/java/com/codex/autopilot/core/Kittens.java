package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The kitten as a step, only while it follows the player (picked up or banked, its needs wait):
 *
 * <ul>
 * <li>"Feed your kitten" a few minutes before it should get hungry, so a meal fits in the trip
 * already being made; going to waste once the game says it is hungry, with the minutes left
 * before it runs off as the deadline;</li>
 * <li>"Stroke your kitten" the same way for attention;</li>
 * <li>"Feed and stroke your kitten" when both are due.</li>
 * </ul>
 *
 * <p>The minutes come from the game's own warnings when it has given one (3 or 6 minutes after a
 * hunger warning, 7 or 14 after an attention one), otherwise from the last meal or stroke and how
 * long that lasts: this kitten's own time once learned, the wiki's until then.</p>
 */
public final class Kittens
{
	public static final String FEED = "kitten:feed";
	public static final String STROKE = "kitten:stroke";
	public static final String CARE = "kitten:care";
	/** Offered this long before the game's first warning is due. */
	static final int SOON_SECONDS = 5 * 60;
	static final double SOON_URGENCY = 2.0;
	/** A kitten lost is lost for good: worth more than any chore. */
	static final double WARNED_URGENCY = 20.0;
	static final String STROKE_TIP = "Stroke it twice: 25 min of peace rather than 18; a ball of wool gives 51";

	private Kittens()
	{
	}

	/** A need: whether it is pressing (the game warned, or its warning is due), the seconds left, and why. */
	private static final class Need
	{
		final boolean warned;
		final long left;
		final String why;

		Need(boolean warned, long left, String why)
		{
			this.warned = warned;
			this.left = left;
			this.why = why;
		}
	}

	public static List<Candidate> candidates(KittenBook book, long now)
	{
		if (book == null || book.getStage() != Kitten.Stage.KITTEN || book.getGone() != null || !book.isOut())
		{
			return Collections.emptyList();
		}
		Need food = hunger(book);
		Need love = attention(book);
		if (food == null && love == null)
		{
			return Collections.emptyList();
		}
		boolean warned = food != null && food.warned || love != null && love.warned;
		long left = Math.min(food == null ? Long.MAX_VALUE : food.left, love == null ? Long.MAX_VALUE : love.left);
		List<String> why = new ArrayList<>();
		String key;
		String title;
		String bring;
		if (food != null && love != null)
		{
			key = CARE;
			title = "Feed and stroke your kitten";
			bring = Kitten.FOOD;
			why.add(food.why);
			why.add(love.why);
		}
		else if (food != null)
		{
			key = FEED;
			title = "Feed your kitten";
			bring = Kitten.FOOD;
			why.add(food.why);
		}
		else
		{
			key = STROKE;
			title = "Stroke your kitten";
			bring = STROKE_TIP;
			why.add(love.why);
		}
		Tier tier = warned ? Tier.WASTING : Tier.READY;
		long deadline = tier == Tier.WASTING ? now + left : 0;
		return Collections.singletonList(new Candidate(key, Candidate.Kind.KITTEN, tier, "Kitten", title, String.join("; ", why),
			bring, 1, tier == Tier.WASTING ? WARNED_URGENCY : SOON_URGENCY, deadline, 1, Kitten.item(book.getNpcId())));
	}

	static Need hunger(KittenBook book)
	{
		long left = book.hungerLeft();
		if (left < 0)
		{
			return null;
		}
		long step = Kitten.HUNGER_STEP_MINUTES * 60L;
		if (book.getHungry() > 0)
		{
			return new Need(true, left, (book.getHungry() >= 2 ? "It is very hungry" : "It is hungry") + ": it runs off in "
				+ about(left) + " unless fed");
		}
		long untilWarning = left - 2 * step;
		if (untilWarning > SOON_SECONDS)
		{
			return null;
		}
		String whose = book.isLearned(KittenBook.FED) ? "your kitten's own time" : "the wiki's time";
		return new Need(untilWarning <= 0, left, "Fed " + Format.duration(book.sinceFed()) + " ago; it asks for food after "
			+ minutes(book.hungerMinutes()) + " (" + whose + ")");
	}

	static Need attention(KittenBook book)
	{
		long left = book.lonelyLeft();
		if (left < 0)
		{
			return null;
		}
		long step = Kitten.LONELY_STEP_MINUTES * 60L;
		if (book.getLonely() > 0)
		{
			return new Need(true, left, (book.getLonely() >= 2 ? "It feels lonely" : "It wants attention") + ": it runs off in "
				+ about(left) + " unless stroked");
		}
		long untilWarning = left - 2 * step;
		if (untilWarning > SOON_SECONDS)
		{
			return null;
		}
		Kitten.Care care = book.getCare();
		String whose = book.isLearned(care.name()) ? "your kitten's own time" : "the wiki's time";
		String since = care == Kitten.Care.NEW ? "New " + Format.duration(book.sinceCared()) + " ago"
			: "Last " + (care == Kitten.Care.WOOL ? "played with" : "stroked") + " " + Format.duration(book.sinceCared()) + " ago";
		return new Need(untilWarning <= 0, left, since + "; " + care.getLabel() + " " + minutes(book.careMinutes(care)) + " (" + whose + ")");
	}

	/** "about 3 min", or "under a minute". */
	static String about(long seconds)
	{
		return seconds < 60 ? "under a minute" : "about " + Format.duration(seconds);
	}

	/** "24 min", "22.5 min". */
	static String minutes(double m)
	{
		double r = Math.round(m * 2) / 2.0;
		return (r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r)) + " min";
	}
}
