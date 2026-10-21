package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Turns everything Autopilot reads into one ranked plan. Every source is optional: a missing or
 * damaged one simply adds no steps, and the reading of each is fenced off so one bad value
 * cannot stop the others.
 */
public final class Advisor
{
	/** Added to a chore's why line when Codex Rounds is on to guide the trip itself. */
	public static final String ROUNDS = "Codex Rounds has the route";

	private Advisor()
	{
	}

	/** What was read for one evaluation. Any field may be empty or null. */
	public static final class Inputs
	{
		public Map<Spot, Seen> seen = Collections.emptyMap();
		public Growth.Offset offset = Growth.Offset.NONE;
		public Kingdom kingdom;
		/** Null while logged out: dailies are only read live. */
		public List<DailyState> dailies;
		public GoalBoard board;
		public BankSnapshot bank;
		/** The player's learned pace, or null to keep every estimate. */
		public Pace pace;

		/** Reads every config-backed source; dailies come from the live game and are added by the caller. */
		public static Inputs read(Profile profile, Gson gson, long now, boolean royalTrouble)
		{
			Inputs in = new Inputs();
			in.seen = Chores.readSeen(profile);
			in.offset = Chores.readOffset(profile);
			try
			{
				in.kingdom = KingdomWatch.read(profile, now, royalTrouble);
			}
			catch (RuntimeException e)
			{
				in.kingdom = null;
			}
			in.board = GoalBoard.read(profile, gson);
			in.bank = BankSnapshot.read(profile, gson);
			return in;
		}
	}

	/** Every step the sources the player has on would offer, unranked. */
	public static List<Candidate> candidates(Inputs in, Settings s, long now)
	{
		List<Candidate> out = new ArrayList<>();
		BankSnapshot bank = s.isBankSteward() ? in.bank : null;
		if (in.seen != null)
		{
			try
			{
				Growth.Offset offset = in.offset == null ? Growth.Offset.NONE : in.offset;
				for (Candidate c : Chores.candidates(Chores.read(watched(in.seen, s), now, offset), bank, now, offset))
				{
					if (in.pace != null)
					{
						c = in.pace.apply(c);
					}
					out.add(s.isRounds() ? c.withNote(ROUNDS) : c);
				}
			}
			catch (RuntimeException e)
			{
				// Leave the chores out this time round.
			}
		}
		if (s.isKingdom())
		{
			Candidate k = KingdomWatch.candidate(in.kingdom, s, bank, now);
			if (k != null)
			{
				out.add(k);
			}
		}
		if (s.isDailies())
		{
			out.addAll(Dailies.candidates(in.dailies, s, bank, now));
		}
		if (s.isNavigator() && in.board != null)
		{
			out.addAll(in.board.candidates());
		}
		return out;
	}

	/** The spots the player's settings say to watch: the timed chores, the herb run and the hops run each have a switch. */
	static Map<Spot, Seen> watched(Map<Spot, Seen> seen, Settings s)
	{
		Map<Spot, Seen> out = new EnumMap<>(Spot.class);
		for (Map.Entry<Spot, Seen> e : seen.entrySet())
		{
			switch (e.getKey().getGroup())
			{
				case HERBS:
				case ALLOTMENTS:
				case FLOWERS:
					if (s.isHerbs())
					{
						out.put(e.getKey(), e.getValue());
					}
					break;
				case HOPS:
					if (s.isHops())
					{
						out.put(e.getKey(), e.getValue());
					}
					break;
				default:
					if (s.isChores())
					{
						out.put(e.getKey(), e.getValue());
					}
					break;
			}
		}
		return out;
	}

	/** What comes ready next among the chores, for when nothing is due. */
	public static String nextUp(Inputs in, Settings s, long now)
	{
		if (in.seen == null)
		{
			return null;
		}
		try
		{
			return Chores.nextUp(Chores.read(watched(in.seen, s), now, in.offset == null ? Growth.Offset.NONE : in.offset), now);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	public static Plan plan(Inputs in, Settings s, Session session, long now)
	{
		return Ranker.rank(candidates(in, s, now), s, session, now);
	}
}
