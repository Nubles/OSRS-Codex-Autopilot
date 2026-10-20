package com.codex.autopilot.core;

import com.codex.autopilot.core.farming.CropState;
import com.codex.autopilot.core.farming.PatchState;
import com.codex.autopilot.core.farming.Produce;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * The timed chores RuneLite's Time Tracking plugin already knows about, read the way Codex Rounds
 * reads them, and grouped into the trips that make sense together: one Fossil Island trip for bird
 * houses, seaweed and its hardwoods, one tree run, one for the special patches.
 */
public final class Chores
{
	public static final String GROUP = "timetracking";
	public static final String OFFSET_KEY = "farmTickOffset";
	public static final String PRECISION_KEY = "farmTickOffsetPrecision";

	/** Ready within this long joins the note on the step: "2 more ready in 6 min". */
	static final long SOON = 15 * 60;
	/** A dead patch left this long is taken as abandoned. */
	static final long FALLOW_AFTER = 2 * 86_400;

	private Chores()
	{
	}

	/** Every spot Time Tracking has saved, skipping anything it cannot read. */
	public static Map<Spot, Seen> readSeen(Profile profile)
	{
		Map<Spot, Seen> out = new EnumMap<>(Spot.class);
		for (Spot s : Spot.values())
		{
			Seen seen;
			try
			{
				seen = Seen.parse(Profile.safe(profile, GROUP, s.timeTrackingKey()));
			}
			catch (RuntimeException e)
			{
				seen = null;
			}
			if (seen != null && seen.getValue() >= 0)
			{
				out.put(s, seen);
			}
		}
		return out;
	}

	public static Growth.Offset readOffset(Profile profile)
	{
		return new Growth.Offset(integer(Profile.safe(profile, GROUP, OFFSET_KEY)), integer(Profile.safe(profile, GROUP, PRECISION_KEY)));
	}

	static Integer integer(String s)
	{
		if (s == null)
		{
			return null;
		}
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/** Each saved spot as it stands now. A value the farming tables do not know reads as unseen. */
	public static Map<Spot, Reading> read(Map<Spot, Seen> seen, long now, Growth.Offset offset)
	{
		Map<Spot, Reading> out = new EnumMap<>(Spot.class);
		for (Map.Entry<Spot, Seen> e : seen.entrySet())
		{
			try
			{
				out.put(e.getKey(), Growth.read(e.getKey(), e.getValue(), now, offset));
			}
			catch (RuntimeException ex)
			{
				// A value outside the tables: leave the spot out rather than guess.
			}
		}
		return out;
	}

	/** What comes ready next, such as "Bird houses full in 23 min", or null when nothing is growing. */
	public static String nextUp(Map<Spot, Reading> readings, long now)
	{
		Reading next = null;
		for (Reading r : readings.values())
		{
			if (r.getStatus() == Status.GROWING && r.getReadyAt() > now && (next == null || r.getReadyAt() < next.getReadyAt()))
			{
				next = r;
			}
		}
		if (next == null)
		{
			return null;
		}
		String in = Format.duration(next.getReadyAt() - now);
		Spot s = next.getSpot();
		return s.isBirdhouse() ? "Bird houses full in " + in : s.kindName() + " at " + s.getLabel() + " ready in " + in;
	}

	/** The trip a spot belongs to. */
	static Candidate.Kind trip(Spot s)
	{
		if (s.isFossilIsland())
		{
			return Candidate.Kind.BIRDHOUSE_RUN;
		}
		return s.getGroup() == Spot.Group.PATCHES ? Candidate.Kind.PATCH_RUN : Candidate.Kind.TREE_RUN;
	}

	/** Minutes a chore takes to come round again, so an hour idle can be weighed as a share of it. */
	static double cycleMinutes(Spot s, Reading r)
	{
		if (s.isBirdhouse())
		{
			return Birdhouse.FILL_SECONDS / 60.0;
		}
		PatchState p = r.getPatch();
		if (p != null && !p.isEmpty() && p.getProduce() != null && p.getProduce().getTickrate() > 0)
		{
			return p.getProduce().getTickrate() * (double) Math.max(1, p.getStages() - 1);
		}
		switch (s.getGroup())
		{
			case SEAWEED:
				return 40;
			case HARDWOODS:
				return 640 * 7;
			case SPIRIT_TREES:
				return 320 * 11;
			case TREES:
				return 160 * 7;
			default:
				return 80 * 7;
		}
	}

	/**
	 * Whether a spot looks deliberately left alone: weeds or bare soil, an empty bird house space,
	 * or a patch that died days ago. Those are not nagged about; a full house or a grown tree is.
	 */
	static boolean fallow(Reading r, long now)
	{
		if (r.isEmpty() || r.getHouse() == Birdhouse.State.EMPTY)
		{
			return true;
		}
		PatchState p = r.getPatch();
		return p != null && (p.getCropState() == CropState.DEAD || p.getCropState() == CropState.DISEASED) && r.getSeen() != null && now - r.getSeen().getTime() > FALLOW_AFTER;
	}

	/** Whether the reading is a diseased patch, which dies at its next growth tick unless cured. */
	static boolean diseased(Reading r)
	{
		PatchState p = r.getPatch();
		return p != null && p.getCropState() == CropState.DISEASED;
	}

	/** When a diseased patch dies: its next growth tick after it was seen. */
	static long dies(Reading r, Growth.Offset offset)
	{
		Produce produce = r.getPatch().getProduce();
		int rate = produce == null || produce.getTickrate() <= 0 ? 5 : produce.getTickrate();
		return Growth.tickTime(rate, 1, r.getSeen().getTime(), offset);
	}

	/** The grouped steps for everything ready. */
	public static List<Candidate> candidates(Map<Spot, Reading> readings, BankSnapshot bank, long now, Growth.Offset offset)
	{
		Map<Candidate.Kind, List<Reading>> ready = new EnumMap<>(Candidate.Kind.class);
		Map<Candidate.Kind, List<Reading>> soon = new EnumMap<>(Candidate.Kind.class);
		for (Reading r : readings.values())
		{
			Candidate.Kind k = trip(r.getSpot());
			if (r.getStatus() == Status.READY && !fallow(r, now))
			{
				ready.computeIfAbsent(k, x -> new ArrayList<>()).add(r);
			}
			else if (r.getStatus() == Status.GROWING && r.getReadyAt() > now && r.getReadyAt() - now <= SOON)
			{
				soon.computeIfAbsent(k, x -> new ArrayList<>()).add(r);
			}
		}
		List<Candidate> out = new ArrayList<>();
		for (Map.Entry<Candidate.Kind, List<Reading>> e : ready.entrySet())
		{
			out.add(step(e.getKey(), e.getValue(), soon.getOrDefault(e.getKey(), new ArrayList<>()), bank, now, offset));
		}
		return out;
	}

	private static Candidate step(Candidate.Kind kind, List<Reading> list, List<Reading> soon, BankSnapshot bank, long now, Growth.Offset offset)
	{
		double urgency = 0;
		long idleSince = Long.MAX_VALUE;
		Reading sick = null;
		long dies = Long.MAX_VALUE;
		int houses = 0;
		int seaweed = 0;
		int trees = 0;
		int dead = 0;
		Set<String> names = new LinkedHashSet<>();
		for (Reading r : list)
		{
			Spot s = r.getSpot();
			if (diseased(r))
			{
				long d = dies(r, offset);
				if (d > now)
				{
					if (d < dies)
					{
						dies = d;
						sick = r;
					}
				}
				else
				{
					// Seen diseased a growth tick or more ago: it has died by now unless it was cured.
					dead++;
				}
			}
			urgency += 60.0 / cycleMinutes(s, r);
			if (r.getReadyAt() > 0 && r.getReadyAt() <= now)
			{
				idleSince = Math.min(idleSince, r.getReadyAt());
			}
			if (s.isBirdhouse())
			{
				houses++;
			}
			else if (s.getGroup() == Spot.Group.SEAWEED)
			{
				seaweed++;
			}
			else
			{
				trees++;
				names.add(s.kindName().toLowerCase());
			}
		}

		String over;
		String title;
		int minutes;
		int item;
		switch (kind)
		{
			case BIRDHOUSE_RUN:
			{
				over = houses > 0 ? "Birdhouse run" : "Fossil Island run";
				List<String> parts = new ArrayList<>();
				if (houses > 0)
				{
					parts.add(houses + " ready");
				}
				if (seaweed > 0)
				{
					parts.add(seaweed + " seaweed ready");
				}
				if (trees > 0)
				{
					parts.add(trees + (trees == 1 ? " hardwood" : " hardwoods") + " ready");
				}
				title = String.join(", ", parts);
				minutes = (int) Math.round(2 + houses * 1.0 + (seaweed > 0 ? 1.5 + seaweed * 1.5 : 0) + trees * 1.0);
				item = houses > 0 ? ItemID.BIRDHOUSE_NORMAL : seaweed > 0 ? ItemID.GIANT_SEAWEED : ItemID.TEAK_LOGS;
				break;
			}
			case TREE_RUN:
				over = "Tree run";
				title = trees + " ready: " + String.join(", ", names);
				minutes = (int) Math.round(trees * 3.0);
				item = produceItem(list, ItemID.PLANTPOT_CALQUAT_SAPLING);
				break;
			default:
				over = "Special patches";
				title = trees + " ready: " + String.join(", ", names);
				minutes = (int) Math.round(trees * 2.5);
				item = produceItem(list, ItemID.CACTUS_SPINE);
				break;
		}

		String why;
		Tier tier = Tier.READY;
		long deadline = 0;
		if (sick != null)
		{
			tier = Tier.WASTING;
			deadline = dies;
			why = sick.getSpot().kindName() + " at " + sick.getSpot().getLabel() + " is diseased: cure it within "
				+ Format.duration(dies - now) + " or it dies";
			urgency += 60.0 / Math.max(5, (dies - now) / 60.0);
		}
		else if (dead > 0)
		{
			why = dead == 1 ? "A diseased patch has likely died: clear it and replant" : dead + " diseased patches have likely died: clear and replant";
		}
		else if (idleSince != Long.MAX_VALUE && now - idleSince >= 60)
		{
			String idle = Format.duration(now - idleSince);
			switch (kind)
			{
				case BIRDHOUSE_RUN:
					why = houses > 0 ? "Full for " + idle + ": every 50 min idle is a run lost"
						: "Ready for " + idle + ": it grows nothing until you replant";
					break;
				case TREE_RUN:
					why = "Grown for " + idle + ": the next cycle starts when you replant";
					break;
				default:
					why = "Ready for " + idle + ": pick and replant to keep them growing";
					break;
			}
		}
		else
		{
			why = kind == Candidate.Kind.BIRDHOUSE_RUN && houses > 0 ? "Ready now: a house without seed catches nothing"
				: "Ready now: nothing grows there until you see to it";
		}
		if (!soon.isEmpty())
		{
			long first = Long.MAX_VALUE;
			for (Reading r : soon)
			{
				first = Math.min(first, r.getReadyAt());
			}
			why += "; " + soon.size() + " more ready in " + Format.duration(first - now);
		}

		String bring = kind == Candidate.Kind.BIRDHOUSE_RUN ? Bring.line(fossilNeeds(list), bank) : null;
		return new Candidate("chores:" + kind.name().toLowerCase(), kind, tier, over, title, why, bring,
			minutes, urgency, deadline, list.size(), item);
	}

	private static int produceItem(List<Reading> list, int fallback)
	{
		for (Reading r : list)
		{
			Produce p = r.getProduce();
			if (p != null && p.getItemId() > 0)
			{
				return p.getItemId();
			}
		}
		return fallback;
	}

	/** Logs and seeds to reset the houses, spores for the seabed. */
	static List<Need> fossilNeeds(List<Reading> list)
	{
		int rebuild = 0;
		int seed = 0;
		int spores = 0;
		Map<Birdhouse.Type, Integer> types = new EnumMap<>(Birdhouse.Type.class);
		for (Reading r : list)
		{
			Spot s = r.getSpot();
			if (s.isBirdhouse())
			{
				Birdhouse.State st = r.getHouse();
				Birdhouse.Type t = r.getSeen() == null ? null : Birdhouse.type(r.getSeen().getValue());
				if (t != null)
				{
					types.merge(t, 1, Integer::sum);
				}
				if (st == Birdhouse.State.SEEDED)
				{
					rebuild++;
				}
				if (st != Birdhouse.State.UNKNOWN)
				{
					seed++;
				}
			}
			else if (s.getGroup() == Spot.Group.SEAWEED)
			{
				spores++;
			}
		}
		List<Need> out = new ArrayList<>();
		Birdhouse.Type type = null;
		int best = 0;
		for (Map.Entry<Birdhouse.Type, Integer> e : types.entrySet())
		{
			if (e.getValue() > best || e.getValue() == best && type != null && e.getKey().ordinal() > type.ordinal())
			{
				type = e.getKey();
				best = e.getValue();
			}
		}
		if (rebuild > 0 && type != null)
		{
			out.add(new Need(logName(type), rebuild, type.getLogId(), type.getItemId()));
		}
		if (seed > 0)
		{
			out.add(new Need("hop seeds", seed * Birdhouse.SEEDS_PER_HOUSE, Birdhouse.SEEDS));
		}
		if (spores > 0)
		{
			out.add(new Need(spores == 1 ? "seaweed spore" : "seaweed spores", spores, ItemID.SEAWEED_SEED));
		}
		return out;
	}

	static String logName(Birdhouse.Type t)
	{
		return t == Birdhouse.Type.NORMAL ? "logs" : t.name().toLowerCase() + " logs";
	}
}
