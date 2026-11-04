package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToLongFunction;
import net.runelite.api.gameval.ItemID;

/**
 * Steps that come from the rest of the Codex suite: coins toward Gear Roadmap's saving goal at the
 * best rate Money Pilot or Boss Pilot has clocked, a trip to the boss pinned in Boss Pilot with its
 * Quartermaster kit, a restock when the chosen kit has run out, and a collection log slot marked
 * wanted in Log Insight. Each reads only what its app shared and adds nothing when it is missing.
 */
public final class Suite
{
	/** Steps toward coins come in sittings of at most an hour. */
	static final int EARN_MAX_MINUTES = 60;
	static final int EARN_MIN_MINUTES = 15;
	/** A boss trip before any is logged. */
	static final int BOSS_MINUTES = 30;
	static final int RESTOCK_MINUTES = 5;
	static final int LOG_MINUTES = 30;

	private Suite()
	{
	}

	/** What is short of the goal: its pieces not yet held, at today's price, less the coins held. Null when it cannot be priced. */
	static Long shortfall(SavingsGoal goal, Wealth wealth, BankSnapshot bank, IntToLongFunction prices)
	{
		long cost = 0;
		boolean priced = true;
		for (int id : goal.getIds())
		{
			if ((wealth != null && wealth.holds(id)) || (bank != null && bank.count(id) > 0))
			{
				continue;
			}
			long p = prices == null ? 0 : safePrice(prices, id);
			if (p <= 0 && wealth != null)
			{
				p = wealth.price(id);
			}
			if (p <= 0)
			{
				priced = false;
				continue;
			}
			cost += p;
		}
		if (!priced && cost == 0)
		{
			return null;
		}
		long coins = wealth != null ? wealth.coins()
			: bank != null ? bank.count(ItemID.COINS) + bank.count(ItemID.PLATINUM) * Wealth.PLATINUM_VALUE : 0;
		return Math.max(0, cost - coins);
	}

	private static long safePrice(IntToLongFunction prices, int id)
	{
		try
		{
			return prices.applyAsLong(id);
		}
		catch (RuntimeException e)
		{
			return 0;
		}
	}

	/** "Earn X toward your goal", when the Gear Roadmap goal is short of coins. */
	static Candidate earn(Advisor.Inputs in, BankSnapshot bank, List<Earner> earners)
	{
		SavingsGoal goal = in.goal;
		if (goal == null || in.ironman)
		{
			return null;
		}
		Long shortBy = shortfall(goal, in.wealth, bank, in.prices);
		if (shortBy == null || shortBy <= 0)
		{
			return null;
		}
		long gap = shortBy;
		Earner best = Earner.best(earners);
		int item = goal.getIds().get(0);
		String over = "Toward " + goal.getName();
		if (best == null)
		{
			return new Candidate("earn:gear", Candidate.Kind.EARN, Tier.GOAL, over, "Earn " + Format.number(gap),
				Format.number(gap) + " short in Codex Gear Roadmap; clock a money maker in Codex Money Pilot and the best one shows here",
				null, EARN_MAX_MINUTES, 0.01, 0, 1, item);
		}
		double hours = gap / (double) best.getPerHour();
		int minutes = (int) Math.max(EARN_MIN_MINUTES, Math.min(EARN_MAX_MINUTES, Math.ceil(hours * 60)));
		long earn = Math.min(gap, best.getPerHour() * minutes / 60);
		String why = Format.number(gap) + " short; you make " + Format.number(best.getPerHour()) + "/hr at it in Codex "
			+ best.getFrom() + (hours > 1 ? ", about " + GoalBoard.hours(hours) + " to go" : "");
		return new Candidate("earn:gear", Candidate.Kind.EARN, Tier.GOAL, over, "Earn " + Format.number(earn) + ": " + best.getDoing(),
			why, null, minutes, 1.0 / (1.0 + hours), 0, 1, item);
	}

	/** A trip to the boss pinned in Boss Pilot, with its kit. */
	static Candidate boss(Advisor.Inputs in, Settings s)
	{
		BossLog log = in.bossLog;
		String key = log == null ? null : log.getPinned();
		if (key == null)
		{
			return null;
		}
		String name = BossLog.name(key);
		BossLog.Record r = log.get(key);
		List<String> parts = new ArrayList<>();
		int minutes = BOSS_MINUTES;
		if (r != null && r.getTrips() > 0)
		{
			minutes = (int) Math.max(10, Math.min(120, Math.round(r.tripMinutes())));
			if (r.lootPerHour() > 0)
			{
				parts.add(Format.number(r.lootPerHour()) + "/hr");
			}
			parts.add(Math.max(1, Math.round(r.getKills() / (double) r.getTrips())) + " kills a trip");
		}
		int kc = in.wanted != null && s.isLogInsight() ? in.wanted.counter(name + " kills") : -1;
		if (kc < 0 && r != null)
		{
			kc = r.getKc();
		}
		if (kc >= 0)
		{
			parts.add(Format.number(kc) + " kc");
		}
		String why = "Pinned in Codex Boss Pilot" + (parts.isEmpty() ? "; no trips logged yet" : "; your trips: " + String.join(", ", parts));
		String bring = null;
		Kits.Kit kit = in.kits != null && s.isQuartermaster() ? in.kits.forBoss(key, name) : null;
		if (kit != null && kit.getTripsLeft() == 0)
		{
			bring = "You're out of " + (kit.getLimiting() == null ? "supplies" : kit.getLimiting().toLowerCase())
				+ " for your " + kit.getName() + " kit";
		}
		else if (kit != null && kit.getTripsLeft() > 0)
		{
			bring = "Take your " + kit.getName() + " kit: " + Format.plural(kit.getTripsLeft(), "trip", "trips") + " in the bank";
		}
		return new Candidate("boss:" + key, Candidate.Kind.BOSS, Tier.GOAL, "Boss Pilot", name + " trip", why, bring, minutes,
			0.2, 0, 1, ItemID.SKULL);
	}

	/** The chosen kit has nothing left in the bank, and no boss step already says so. */
	static Candidate restock(Advisor.Inputs in, Candidate boss)
	{
		Kits.Kit kit = in.kits == null ? null : in.kits.active();
		if (kit == null || kit.getTripsLeft() != 0 || (boss != null && boss.getBring() != null && boss.getBring().contains(kit.getName() + " kit")))
		{
			return null;
		}
		String why = (kit.getLimiting() == null ? "Your bank" : kit.getLimiting() + " ran out first: your bank")
			+ " covers no trips of the kit chosen in Codex Quartermaster";
		return new Candidate("restock:" + kit.getId(), Candidate.Kind.RESTOCK, Tier.GOAL, "Quartermaster", "Restock " + kit.getName(),
			why, null, RESTOCK_MINUTES, 0.3, 0, 1, ItemID.SHARK);
	}

	/** The first slot marked wanted in Log Insight. */
	static Candidate log(Advisor.Inputs in)
	{
		WantedSlots w = in.wanted;
		if (w == null || w.getWanted().isEmpty())
		{
			return null;
		}
		int id = w.getWanted().get(0);
		String name = null;
		if (in.names != null)
		{
			try
			{
				name = in.names.apply(id);
			}
			catch (RuntimeException e)
			{
				name = null;
			}
		}
		int more = w.getWanted().size() - 1;
		String why = "Marked wanted in Codex Log Insight" + (more > 0 ? "; " + Format.plural(more, "more wanted slot", "more wanted slots") : "");
		return new Candidate("log:" + id, Candidate.Kind.LOG, Tier.GOAL, "Collection log",
			"Chase " + (name == null || name.isEmpty() ? "your wanted slot" : name), why, null, LOG_MINUTES, 0.1, 0, 1, id);
	}

	/** Every suite step the settings allow. */
	static List<Candidate> candidates(Advisor.Inputs in, Settings s, BankSnapshot bank, List<Earner> earners)
	{
		List<Candidate> out = new ArrayList<>();
		if (s.isGearRoadmap())
		{
			add(out, () -> earn(in, bank, earners));
		}
		Candidate boss = null;
		if (s.isBossPilot())
		{
			try
			{
				boss = boss(in, s);
			}
			catch (RuntimeException e)
			{
				boss = null;
			}
			if (boss != null)
			{
				out.add(boss);
			}
		}
		Candidate b = boss;
		if (s.isQuartermaster())
		{
			add(out, () -> restock(in, b));
		}
		if (s.isLogInsight())
		{
			add(out, () -> log(in));
		}
		return out;
	}

	private static void add(List<Candidate> out, java.util.function.Supplier<Candidate> step)
	{
		try
		{
			Candidate c = step.get();
			if (c != null)
			{
				out.add(c);
			}
		}
		catch (RuntimeException e)
		{
			// One app's odd data never stops the plan.
		}
	}
}
