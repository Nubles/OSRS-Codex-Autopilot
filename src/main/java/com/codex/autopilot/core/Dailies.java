package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.ItemID;

/**
 * The diary dailies still to collect today, read live from the game the way Codex Rounds reads
 * them, as one step. What is not collected by midnight UTC is gone, so they count as going to
 * waste once the reset is near. A finished farming contract becomes its own step: it waits
 * for you, so it never goes to waste.
 */
public final class Dailies
{
	private Dailies()
	{
	}

	public static List<DailyState> read(Vars v)
	{
		List<DailyState> out = new ArrayList<>();
		for (Daily d : Daily.values())
		{
			try
			{
				out.add(DailyState.of(d, v));
			}
			catch (RuntimeException e)
			{
				// A varbit the client could not give: leave that daily out.
			}
		}
		return out;
	}

	public static List<Candidate> candidates(List<DailyState> states, Settings s, BankSnapshot bank, long now)
	{
		List<Candidate> out = new ArrayList<>();
		if (states == null)
		{
			return out;
		}
		List<DailyState> open = new ArrayList<>();
		DailyState contract = null;
		for (DailyState d : states)
		{
			if (d == null || !d.isClaimable())
			{
				continue;
			}
			if (d.getDaily().resetsDaily())
			{
				open.add(d);
			}
			else
			{
				contract = d;
			}
		}
		if (!open.isEmpty())
		{
			long reset = KingdomWatch.nextReset(now);
			boolean soon = reset - now <= s.getWasteHorizon();
			List<String> names = new ArrayList<>();
			List<Need> needs = new ArrayList<>();
			for (DailyState d : open)
			{
				names.add(d.getDaily().getLabel());
				if (d.getNeed() != null && d.getNeed().getAmount() > 0)
				{
					needs.add(d.getNeed());
				}
			}
			String title = open.size() == 1 ? names.get(0) : open.size() + " to collect";
			String why = (soon ? "Lost at the reset in " : "Not collected yet; the day resets in ") + Format.duration(reset - now)
				+ (open.size() > 1 ? ": " + String.join(", ", names) : "");
			out.add(new Candidate("dailies", Candidate.Kind.DAILIES, soon ? Tier.WASTING : Tier.READY, "Diary dailies", title, why,
				Bring.line(needs, bank), open.size() * 3, (soon ? 1.0 : 0.15) * open.size(), soon ? reset : 0, open.size(),
				open.size() == 1 ? open.get(0).getDaily().getItemId() : ItemID.BATTLESTAFF));
		}
		if (contract != null)
		{
			out.add(new Candidate("contract", Candidate.Kind.CONTRACT, Tier.READY, "Farming Guild", "See Guildmaster Jane",
				contract.getReward(), null, 3, contract.getReward().startsWith("Done") ? 0.3 : 0.05, 0, 1, Daily.CONTRACT.getItemId()));
		}
		return out;
	}
}
