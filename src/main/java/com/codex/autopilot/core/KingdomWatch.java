package com.codex.autopilot.core;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import net.runelite.api.gameval.ItemID;

/**
 * The Kingdom of Miscellania as RuneLite's own Kingdom plugin last saved it
 * ({@code kingdomofmiscellania/lastChanged, approval, coffer}), carried forward to today the way
 * Codex Rounds does. Approval falls and the coffer pays out at each daily reset, so a kingdom
 * below its targets loses a little at every reset until someone visits.
 */
public final class KingdomWatch
{
	public static final String GROUP = "kingdomofmiscellania";
	public static final int DAY = 86_400;

	private KingdomWatch()
	{
	}

	/** Today's kingdom, or null when the Kingdom plugin has not saved one or what it saved is damaged. */
	public static Kingdom read(Profile profile, long now, boolean royalTrouble)
	{
		String changed = Profile.safe(profile, GROUP, "lastChanged");
		Integer approval = Chores.integer(Profile.safe(profile, GROUP, "approval"));
		Integer coffer = Chores.integer(Profile.safe(profile, GROUP, "coffer"));
		if (changed == null || approval == null || coffer == null)
		{
			return null;
		}
		long seenAt;
		try
		{
			seenAt = Instant.parse(changed.trim()).getEpochSecond();
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
		if (seenAt <= 0 || seenAt > now + DAY)
		{
			return null;
		}
		int a = Math.max(0, Math.min(Kingdom.MAX_APPROVAL, approval));
		int c = Math.max(0, Math.min(Kingdom.MAX_COFFER, coffer));
		return Kingdom.estimate(a, c, seenAt, now, royalTrouble);
	}

	/** The next daily reset, midnight UTC. */
	public static long nextReset(long now)
	{
		return (Math.floorDiv(now, DAY) + 1) * DAY;
	}

	/** A visit step when the kingdom is under the player's targets, or null. */
	public static Candidate candidate(Kingdom k, Settings s, BankSnapshot bank, long now)
	{
		if (k == null)
		{
			return null;
		}
		int full = Kingdom.fullPayoutCoffer(k.isRoyalTrouble());
		int topUp = k.topUp(full);
		boolean lowApproval = k.approvalPercent() < s.getApprovalTarget();
		boolean lowCoffer = topUp > s.getCofferSlack();
		if (!lowApproval && !lowCoffer)
		{
			return null;
		}
		long reset = nextReset(now);
		boolean soon = reset - now <= s.getWasteHorizon();
		String drop = k.isRoyalTrouble() ? "1%" : "2.5%";
		String title;
		String why;
		if (lowApproval && lowCoffer)
		{
			title = k.approvalPercent() + "% approval, top up " + Format.number(topUp);
		}
		else if (lowApproval)
		{
			title = "Approval " + k.approvalPercent() + "%: help out";
		}
		else
		{
			title = "Top up the coffer by " + Format.number(topUp);
		}
		if (lowApproval)
		{
			why = "Approval drops " + drop + " at each reset" + (soon ? ", next in " + Format.duration(reset - now) : "")
				+ "; the haul shrinks with it";
		}
		else
		{
			why = "A coffer under " + Format.number(full) + " pays out less each day" + (soon ? "; next payout in " + Format.duration(reset - now) : "");
		}
		if (k.isEstimated())
		{
			why += " (estimated)";
		}
		double urgency = (lowApproval ? (s.getApprovalTarget() - k.approvalPercent()) / 100.0 : 0) + (lowCoffer ? topUp / (double) full : 0);
		// The value is lost at the reset, so it only counts as going to waste when the reset is near.
		return new Candidate("kingdom", Candidate.Kind.KINGDOM, soon ? Tier.WASTING : Tier.READY, "Miscellania", title, why,
			lowCoffer ? Bring.line(java.util.Collections.singletonList(new Need("coins", topUp, ItemID.COINS)), bank) : null, 6, 0.1 + urgency * 0.5, soon ? reset : 0, 1, ItemID.POH_TREASURE_TEAK);
	}
}
