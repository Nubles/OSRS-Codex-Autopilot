package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntToLongFunction;

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

	/** Codex Rounds routes bird houses, trees and special patches; herb and hops runs are not on its round. */
	static boolean routedByRounds(Candidate.Kind kind)
	{
		return kind == Candidate.Kind.BIRDHOUSE_RUN || kind == Candidate.Kind.TREE_RUN || kind == Candidate.Kind.PATCH_RUN;
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

		// The rest of the Codex suite; each may be null when its app is missing or has shared nothing.
		/** Codex Wayfinder's travel atlas, to price the journey to each step. */
		public Atlas atlas;
		/** Where the player stands, or null while logged out. */
		public Tile here;
		/** Money makers clocked by Codex Money Pilot, best paying first. */
		public List<Earner> money = Collections.emptyList();
		/** Codex Boss Pilot's pinned boss and trips. */
		public BossLog bossLog;
		/** What Codex Gear Roadmap is saving toward. */
		public SavingsGoal goal;
		/** What Codex Ledger counts the account holding. */
		public Wealth wealth;
		/** Codex Quartermaster's kits. */
		public Kits kits;
		/** The dailies as Codex Rounds last saw them, for while logged out. */
		public List<DailyState> roundsDailies;
		/** Codex Log Insight's wanted slots and counters. */
		public WantedSlots wanted;
		/** Today's price of an item, or 0 when not known; null to use Ledger's prices only. */
		public IntToLongFunction prices;
		/** An item's name, or null; for the collection log step. */
		public IntFunction<String> names;
		/** The Hunter Guild rumour as Autopilot read it, or as the Hunter Rumours plugin saved it; null when neither knows. */
		public RumourBook rumour;
		/** True when the rumour is the Hunter Rumours plugin's, not read by Autopilot itself. */
		public boolean rumourFromPlugin;
		/** Ironmen cannot buy their upgrades, so no coins step is offered. */
		public boolean ironman;

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
					out.add(s.isRounds() && routedByRounds(c.getKind()) ? c.withNote(ROUNDS) : c);
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
			if (in.dailies == null && s.isRoundsDailies() && in.roundsDailies != null)
			{
				// Logged out: the dailies as Codex Rounds last saw them today.
				for (Candidate c : Dailies.candidates(in.roundsDailies, s, bank, now))
				{
					out.add(c.withNote(SEEN_BY_ROUNDS));
				}
			}
			else
			{
				out.addAll(Dailies.candidates(in.dailies, s, bank, now));
			}
		}
		if (s.isRumours() && in.rumour != null)
		{
			try
			{
				for (Candidate c : Rumours.candidates(in.rumour))
				{
					out.add(in.rumourFromPlugin ? c.withNote(SEEN_BY_HUNTER_RUMOURS) : c);
				}
			}
			catch (RuntimeException e)
			{
				// Leave the rumour out this time round.
			}
		}
		if (s.isNavigator() && in.board != null)
		{
			out.addAll(in.board.candidates());
		}
		Inputs suite = s.isLedger() ? in : withoutLedger(in);
		out.addAll(Suite.candidates(suite, s, bank, earners(in, s)));
		return travel(out, in, s);
	}

	/** Shown on dailies read from Codex Rounds' memory while logged out. */
	public static final String SEEN_BY_ROUNDS = "as Codex Rounds last saw them";
	/** Shown on a rumour read from the Hunter Rumours plugin's save. */
	public static final String SEEN_BY_HUNTER_RUMOURS = "as Hunter Rumours saved it";

	private static Inputs withoutLedger(Inputs in)
	{
		Inputs copy = new Inputs();
		copy.goal = in.goal;
		copy.bossLog = in.bossLog;
		copy.kits = in.kits;
		copy.wanted = in.wanted;
		copy.prices = in.prices;
		copy.names = in.names;
		copy.ironman = in.ironman;
		return copy;
	}

	/** Every money maker the settings allow: Money Pilot's clocked methods and Boss Pilot's bosses. */
	public static List<Earner> earners(Inputs in, Settings s)
	{
		List<Earner> out = new ArrayList<>();
		if (s.isMoneyPilot() && in.money != null)
		{
			out.addAll(in.money);
		}
		if (s.isBossPilot() && in.bossLog != null)
		{
			out.addAll(in.bossLog.earners());
		}
		return out;
	}

	/** Prices the journey to each step that has a place, when Codex Wayfinder shares its atlas. */
	static List<Candidate> travel(List<Candidate> steps, Inputs in, Settings s)
	{
		if (!s.isWayfinder() || in.atlas == null)
		{
			return steps;
		}
		List<Candidate> out = new ArrayList<>(steps.size());
		for (Candidate c : steps)
		{
			Trip trip;
			try
			{
				trip = Travel.best(in.atlas, in.here, c.getTargets());
			}
			catch (RuntimeException e)
			{
				trip = null;
			}
			out.add(trip == null ? c : c.withTrip(trip));
		}
		return out;
	}

	/** The Codex apps whose data is in this plan, for the "Fed by" row, in suite order. */
	public static List<String> fedBy(Inputs in, Settings s, List<Candidate> steps)
	{
		List<String> out = new ArrayList<>();
		boolean travelled = false;
		boolean roundsDailies = false;
		boolean hunterRumours = false;
		for (Candidate c : steps)
		{
			hunterRumours |= c.getWhy() != null && c.getWhy().endsWith(SEEN_BY_HUNTER_RUMOURS);
			travelled |= c.getTrip() != null;
			roundsDailies |= c.getWhy() != null && c.getWhy().endsWith(SEEN_BY_ROUNDS);
		}
		if (s.isNavigator() && in.board != null)
		{
			out.add("Navigator");
		}
		if (s.isWayfinder() && in.atlas != null && (travelled || !in.atlas.getTeleports().isEmpty()))
		{
			out.add("Wayfinder");
		}
		if (s.isGearRoadmap() && in.goal != null)
		{
			out.add("Gear Roadmap");
		}
		if (s.isMoneyPilot() && in.money != null && !in.money.isEmpty())
		{
			out.add("Money Pilot");
		}
		if (s.isBossPilot() && in.bossLog != null)
		{
			out.add("Boss Pilot");
		}
		if (s.isQuartermaster() && in.kits != null)
		{
			out.add("Quartermaster");
		}
		if (s.isLogInsight() && in.wanted != null)
		{
			out.add("Log Insight");
		}
		if (s.isLedger() && in.wealth != null)
		{
			out.add("Ledger");
		}
		if (s.isBankSteward() && in.bank != null)
		{
			out.add("Bank Steward");
		}
		if (s.isRounds() || roundsDailies)
		{
			out.add("Rounds");
		}
		if (hunterRumours)
		{
			out.add("Hunter Rumours");
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
