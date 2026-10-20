package com.codex.autopilot.core.farming;

/** What a patch wants doing next, read from the options its object offers in game. */
public enum Action
{
	/** Weeds to rake, or an empty patch once {@link PatchState#isClear()}. */
	RAKE,
	/** Growing: nothing to do yet. */
	WAIT,
	/** Fully grown; checking its health gives the experience and readies the harvest. */
	CHECK_HEALTH,
	/** Produce to pick or harvest. */
	HARVEST,
	/** A grown tree to chop down. */
	CHOP,
	/** Diseased: cure with a plant cure. */
	CURE,
	/** Diseased: prune with secateurs. */
	PRUNE,
	/** A stump or a dead plant to clear with a spade. */
	CLEAR,
	/** Clear soil, ready for a seed or sapling. */
	PLANT,
	/** Grown and healthy, and nothing is gained by touching it (a spirit tree). */
	GROWN
}
