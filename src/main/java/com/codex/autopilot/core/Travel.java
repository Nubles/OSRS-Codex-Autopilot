package com.codex.autopilot.core;

import java.util.List;

/**
 * Prices the journey to a step's start from Codex Wayfinder's atlas: each teleport the account
 * has, plus the walk from where it lands, and the walk from where the player stands. The walk is
 * the straight-line tile count with a detour allowance, since the atlas carries no paths; a
 * teleport left in the bank costs a bank visit first. Places farther than {@link #REACH} tiles from
 * every landing and from the player are left unpriced rather than guessed.
 */
public final class Travel
{
	/** The farthest walk, in tiles, worth pricing from a landing or from where the player stands. */
	public static final int REACH = 120;
	/** Paths bend round walls and water: a straight line of tiles is walked as this much more. */
	public static final double DETOUR = 1.3;
	/** A teleport in the bank costs a trip to fetch it. */
	public static final int BANK_SECONDS = 60;
	/** Within this many tiles the player is already there. */
	public static final int THERE = 6;

	private Travel()
	{
	}

	/** The cheapest way to the nearest of the targets, or null with no atlas, no targets or nothing in reach. */
	public static Trip best(Atlas atlas, Tile here, List<Tile> targets)
	{
		if (atlas == null || targets == null || targets.isEmpty())
		{
			return null;
		}
		Trip best = null;
		for (Tile t : targets)
		{
			Trip trip = to(atlas, here, t);
			if (trip != null && (best == null || trip.getSeconds() < best.getSeconds()))
			{
				best = trip;
			}
		}
		return best;
	}

	static Trip to(Atlas atlas, Tile here, Tile target)
	{
		double perTile = atlas.getWalkSecondsPerTile();
		Trip best = null;
		if (here != null)
		{
			int d = here.distance(target);
			if (d <= REACH)
			{
				best = Trip.walk(d <= THERE ? 0 : walk(d, perTile));
			}
		}
		for (Atlas.Teleport tp : atlas.getTeleports())
		{
			int d = tp.getLanding().distance(target);
			if (d > REACH)
			{
				continue;
			}
			int seconds = tp.getSeconds() + walk(d, perTile) + (tp.isCarried() ? 0 : BANK_SECONDS);
			if (best == null || seconds < best.getSeconds()
				|| seconds == best.getSeconds() && !best.isWalk() && tp.isCarried() && best.isBanked())
			{
				best = Trip.via(tp.getSource(), seconds, !tp.isCarried());
			}
		}
		return best;
	}

	static int walk(int tiles, double perTile)
	{
		return tiles <= THERE ? 0 : (int) Math.round(tiles * DETOUR * perTile);
	}
}
