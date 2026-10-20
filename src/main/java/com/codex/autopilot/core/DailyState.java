package com.codex.autopilot.core;

/** A daily's standing today: unlocked, claimable, what it gives and needs. */
public final class DailyState
{
	private final Daily daily;
	private final boolean unlocked;
	private final boolean claimable;
	private final String reward;
	private final Need need;

	public DailyState(Daily daily, boolean unlocked, boolean claimable, String reward, Need need)
	{
		this.daily = daily;
		this.unlocked = unlocked;
		this.claimable = claimable;
		this.reward = reward;
		this.need = need;
	}

	public static DailyState of(Daily d, Vars v)
	{
		boolean unlocked = d.unlocked(v);
		boolean claimable = unlocked && d.claimable(v);
		return new DailyState(d, unlocked, claimable, unlocked ? d.reward(v) : "", claimable ? d.need(v) : null);
	}

	public Daily getDaily()
	{
		return daily;
	}

	public boolean isUnlocked()
	{
		return unlocked;
	}

	public boolean isClaimable()
	{
		return claimable;
	}

	public String getReward()
	{
		return reward;
	}

	/** What to bring, or null. */
	public Need getNeed()
	{
		return need;
	}

	/** The state after the daily reset: claimable again if it was ever unlocked. */
	public DailyState afterReset()
	{
		return new DailyState(daily, unlocked, unlocked, reward, need);
	}
}
