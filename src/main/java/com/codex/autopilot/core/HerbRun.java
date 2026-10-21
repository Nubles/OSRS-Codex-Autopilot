package com.codex.autopilot.core;

import com.codex.autopilot.core.farming.CropState;
import com.codex.autopilot.core.farming.PatchState;
import com.codex.autopilot.core.farming.Produce;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * The herb run and the hops run, as one step each. Herb patches lead the herb run; the allotments
 * and flower patches beside them ride along ("Herb run: 7 ready, 2 diseased, 4 allotments"). A
 * diseased patch dies at its next growth tick, and a dead herb patch grows nothing until it is
 * cleared, so either puts the run among the steps going to waste.
 *
 * <p>Each crop weighs what it is worth to the run: an hour of a herb patch idle costs most of a
 * harvest, an hour of a flower patch costs three cheap ones, so flowers are weighted down and a
 * lone ready flower patch makes no step of its own.</p>
 */
final class HerbRun
{
	/** Every herb seed, so "herb seeds" counts any of them in the bank. */
	static final int[] HERB_SEEDS = {
		ItemID.GUAM_SEED, ItemID.MARRENTILL_SEED, ItemID.TARROMIN_SEED, ItemID.HARRALANDER_SEED, ItemID.RANARR_SEED,
		ItemID.TOADFLAX_SEED, ItemID.IRIT_SEED, ItemID.AVANTOE_SEED, ItemID.KWUARM_SEED, ItemID.HUASCA_SEED,
		ItemID.SNAPDRAGON_SEED, ItemID.CADANTINE_SEED, ItemID.LANTADYME_SEED, ItemID.DWARF_WEED_SEED, ItemID.TORSTOL_SEED,
	};
	static final int[] COMPOST = {ItemID.BUCKET_COMPOST, ItemID.BUCKET_SUPERCOMPOST, ItemID.BUCKET_ULTRACOMPOST};

	private HerbRun()
	{
	}

	/** What an hour idle costs, relative to a herb patch losing the same share of its cycle. */
	static double weight(Spot.Group g)
	{
		switch (g)
		{
			case HERBS:
				return 1.0;
			case HOPS:
				return 0.4;
			case ALLOTMENTS:
				return 0.3;
			default:
				return 0.15;
		}
	}

	/** "herb patch", "allotment", for sentences about one patch. */
	static String noun(Spot.Group g)
	{
		switch (g)
		{
			case HERBS:
				return "herb patch";
			case ALLOTMENTS:
				return "allotment";
			case FLOWERS:
				return "flower patch";
			default:
				return "hops patch";
		}
	}

	/** The town a spot is in: "Catherby" for "Catherby, north". */
	static String place(Spot s)
	{
		String l = s.getLabel();
		int comma = l.indexOf(',');
		return comma < 0 ? l : l.substring(0, comma);
	}

	/** The step for the herb or hops patches that are ready, or null when only flowers are. */
	static Candidate step(Candidate.Kind kind, List<Reading> list, List<Reading> soon, BankSnapshot bank, long now, Growth.Offset offset)
	{
		Spot.Group lead = kind == Candidate.Kind.HOPS_RUN ? Spot.Group.HOPS : null;
		if (lead == null)
		{
			for (Spot.Group g : Arrays.asList(Spot.Group.HERBS, Spot.Group.ALLOTMENTS, Spot.Group.FLOWERS))
			{
				if (lead == null && list.stream().anyMatch(r -> r.getSpot().getGroup() == g))
				{
					lead = g;
				}
			}
		}
		if (lead == null)
		{
			return null;
		}

		int ready = 0;
		int sick = 0;
		int dead = 0;
		int deadHerbs = 0;
		int allotments = 0;
		int flowers = 0;
		int replant = 0;
		int herbSeeds = 0;
		double urgency = 0;
		long idleSince = Long.MAX_VALUE;
		long dies = Long.MAX_VALUE;
		Reading first = null;
		Set<String> places = new LinkedHashSet<>();
		for (Reading r : list)
		{
			Spot s = r.getSpot();
			Spot.Group g = s.getGroup();
			places.add(place(s));
			double w = weight(g);
			urgency += w * 60.0 / Chores.cycleMinutes(s, r);
			PatchState p = r.getPatch();
			boolean diseased = Chores.diseased(r);
			boolean gone = p != null && p.getCropState() == CropState.DEAD;
			if (diseased)
			{
				long d = Chores.dies(r, offset);
				if (d > now)
				{
					sick++;
					if (d < dies)
					{
						dies = d;
						first = r;
					}
				}
				else
				{
					// Seen diseased a growth tick or more ago: dead by now unless it was cured.
					gone = true;
				}
			}
			if (gone)
			{
				dead++;
				if (g == Spot.Group.HERBS)
				{
					deadHerbs++;
				}
			}
			if (!diseased || gone)
			{
				replant++;
				if (g == Spot.Group.HERBS)
				{
					herbSeeds++;
				}
			}
			if (g == lead)
			{
				if (!diseased && !gone)
				{
					ready++;
				}
			}
			else if (g == Spot.Group.ALLOTMENTS)
			{
				allotments++;
			}
			else if (g == Spot.Group.FLOWERS)
			{
				flowers++;
			}
			if (!diseased && !gone && r.getReadyAt() > 0 && r.getReadyAt() <= now)
			{
				idleSince = Math.min(idleSince, r.getReadyAt());
			}
		}
		if (lead == Spot.Group.FLOWERS && sick == 0)
		{
			// Flowers alone are not worth a trip; they come along when the herbs are ready.
			return null;
		}

		String over = kind == Candidate.Kind.HOPS_RUN ? "Hops run"
			: lead == Spot.Group.HERBS ? "Herb run" : lead == Spot.Group.ALLOTMENTS ? "Allotment run" : "Flower patches";
		List<String> parts = new ArrayList<>();
		if (ready > 0)
		{
			parts.add(ready + " ready");
		}
		if (sick > 0)
		{
			parts.add(sick + " diseased");
		}
		if (dead > 0)
		{
			parts.add(dead + " dead");
		}
		if (allotments > 0)
		{
			parts.add(allotments == 1 ? "1 allotment" : allotments + " allotments");
		}
		if (flowers > 0)
		{
			parts.add(flowers == 1 ? "1 flower patch" : flowers + " flower patches");
		}
		String title = String.join(", ", parts);

		Tier tier = Tier.READY;
		long deadline = 0;
		String why;
		if (first != null)
		{
			tier = Tier.WASTING;
			deadline = dies;
			String in = Format.duration(dies - now);
			why = sick == 1
				? capital(noun(first.getSpot().getGroup())) + " at " + first.getSpot().getLabel() + " is diseased: cure it within " + in + " or it dies"
				: sick + " patches are diseased: the first dies in " + in + " unless cured";
			urgency += 60.0 / Math.max(5, (dies - now) / 60.0);
		}
		else if (deadHerbs > 0)
		{
			tier = Tier.WASTING;
			why = (deadHerbs == 1 ? "A herb patch has died" : deadHerbs + " herb patches have died")
				+ ": clear and replant, a dead patch grows nothing";
		}
		else if (dead > 0)
		{
			why = (dead == 1 ? "A patch has died" : dead + " patches have died") + ": clear and replant";
		}
		else if (idleSince != Long.MAX_VALUE && now - idleSince >= 60)
		{
			String idle = Format.duration(now - idleSince);
			why = lead == Spot.Group.HERBS ? "Grown for " + idle + ": every 80 min idle is a harvest lost"
				: "Grown for " + idle + ": pick and replant to keep them growing";
		}
		else
		{
			why = "Ready now: nothing grows there until you see to it";
		}
		if (!soon.isEmpty())
		{
			long next = Long.MAX_VALUE;
			for (Reading r : soon)
			{
				next = Math.min(next, r.getReadyAt());
			}
			why += "; " + soon.size() + " more ready in " + Format.duration(next - now);
		}

		// A stop per town, and a little longer for each extra patch there.
		int patches = list.size();
		int stops = places.size();
		int minutes = (int) Math.round(1 + stops * 2.5 + (patches - stops) * 0.75);

		String bring = Bring.line(needs(kind == Candidate.Kind.HERB_RUN ? herbSeeds : 0, replant, bank), bank);
		int item = produceItem(list, lead, kind == Candidate.Kind.HOPS_RUN ? ItemID.HAMMERSTONE_HOPS : ItemID.RANARR_WEED);
		return new Candidate("chores:" + kind.name().toLowerCase(), kind, tier, over, title, why, bring,
			minutes, urgency, deadline, patches, item);
	}

	/** Seeds for the herb patches and compost for every patch replanted; a filled bottomless bucket covers the compost. */
	static List<Need> needs(int seeds, int compost, BankSnapshot bank)
	{
		List<Need> out = new ArrayList<>();
		if (seeds > 0)
		{
			out.add(new Need(seeds == 1 ? "herb seed" : "herb seeds", seeds, HERB_SEEDS));
		}
		if (compost > 0)
		{
			if (bank != null && bank.count(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED) > 0)
			{
				out.add(new Need("bottomless compost bucket", 1, ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED));
			}
			else
			{
				out.add(new Need("compost", compost, COMPOST));
			}
		}
		return out;
	}

	private static int produceItem(List<Reading> list, Spot.Group lead, int fallback)
	{
		for (Reading r : list)
		{
			Produce p = r.getProduce();
			if (r.getSpot().getGroup() == lead && p != null && p != Produce.ANYHERB && p.getItemId() > 0)
			{
				return p.getItemId();
			}
		}
		return fallback;
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
