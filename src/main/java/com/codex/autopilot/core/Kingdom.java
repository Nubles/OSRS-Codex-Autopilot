package com.codex.autopilot.core;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * The Kingdom of Miscellania. Approval (0 to 127) and the coffer live in varbits; away from the
 * kingdom both are estimated from the last values seen, the way RuneLite's Kingdom plugin does:
 * every day the coffer pays out a tenth of itself, capped at 50k (75k after Royal Trouble), and
 * approval falls by 2.5% (1% after Royal Trouble).
 */
public final class Kingdom
{
	public static final int MAX_APPROVAL = 127;
	public static final int MAX_COFFER = 7_500_000;
	public static final int[] REGIONS = {10044, 10300};

	private final int approval;
	private final int coffer;
	private final boolean royalTrouble;
	private final boolean estimated;

	public Kingdom(int approval, int coffer, boolean royalTrouble, boolean estimated)
	{
		this.approval = approval;
		this.coffer = coffer;
		this.royalTrouble = royalTrouble;
		this.estimated = estimated;
	}

	/** Today's kingdom, from a snapshot taken at {@code seenAt}. */
	public static Kingdom estimate(int approval, int coffer, long seenAt, long now, boolean royalTrouble)
	{
		int days = daysBetween(seenAt, now);
		int withdrawal = withdrawalCap(royalTrouble);
		int threshold = withdrawal * 10;
		int c = coffer;
		for (int i = 0; i < days && c > 0; i++)
		{
			c -= c > threshold ? withdrawal : c / 10;
		}
		float daily = royalTrouble ? 0.010f : 0.025f;
		int a = Math.max(0, approval - (int) (days * daily * MAX_APPROVAL));
		return new Kingdom(a, c, royalTrouble, days > 0);
	}

	public static int withdrawalCap(boolean royalTrouble)
	{
		return royalTrouble ? 75_000 : 50_000;
	}

	/** The coffer that keeps the full daily payout flowing: ten days of the cap. */
	public static int fullPayoutCoffer(boolean royalTrouble)
	{
		return withdrawalCap(royalTrouble) * 10;
	}

	static int daysBetween(long from, long to)
	{
		Instant a = Instant.ofEpochSecond(from).truncatedTo(ChronoUnit.DAYS);
		Instant b = Instant.ofEpochSecond(to).truncatedTo(ChronoUnit.DAYS);
		return (int) Math.max(0, Duration.between(a, b).toDays());
	}

	public int getApproval()
	{
		return approval;
	}

	public int approvalPercent()
	{
		return approval * 100 / MAX_APPROVAL;
	}

	public int getCoffer()
	{
		return coffer;
	}

	public boolean isRoyalTrouble()
	{
		return royalTrouble;
	}

	/** Whether the numbers are a guess carried forward from an earlier visit. */
	public boolean isEstimated()
	{
		return estimated;
	}

	/** Coins to deposit to reach the target, never past the coffer's cap. */
	public int topUp(int target)
	{
		int t = Math.min(MAX_COFFER, Math.max(0, target));
		return Math.max(0, t - coffer);
	}

	/** Whether a visit is due under the player's thresholds. */
	public boolean needsVisit(int approvalTargetPercent, int cofferTarget, int topUpSlack)
	{
		return approvalPercent() < approvalTargetPercent || topUp(cofferTarget) > topUpSlack;
	}

	/** Days until approval falls below a percentage, at the daily drop. */
	public int daysUntilApprovalBelow(int percent)
	{
		float daily = royalTrouble ? 0.010f : 0.025f;
		int perDay = Math.max(1, (int) (daily * MAX_APPROVAL));
		int floor = (percent * MAX_APPROVAL + 99) / 100;
		int above = approval - floor;
		return above < 0 ? 0 : above / perDay + 1;
	}
}
