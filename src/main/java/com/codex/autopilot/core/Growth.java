package com.codex.autopilot.core;

import com.codex.autopilot.core.farming.Action;
import com.codex.autopilot.core.farming.CropState;
import com.codex.autopilot.core.farming.PatchKind;
import com.codex.autopilot.core.farming.PatchState;

/**
 * Predicts a patch or bird house from the last time it was seen. Farming ticks land on fixed
 * five-minute marks shifted by an account offset; the maths follows RuneLite's FarmingTracker.
 */
public final class Growth
{
	private Growth()
	{
	}

	/** The account's farming tick offset as RuneLite's Time Tracking learned it, or none. */
	public static final class Offset
	{
		public static final Offset NONE = new Offset(null, null);

		private final Integer minutes;
		private final Integer precision;

		public Offset(Integer minutes, Integer precision)
		{
			this.minutes = minutes;
			this.precision = precision;
		}

		long seconds(int tickRate)
		{
			if (minutes != null && precision != null && (precision >= tickRate || precision >= 40))
			{
				return (long) (minutes % tickRate) * 60;
			}
			return 0;
		}
	}

	private static final java.util.Map<PatchKind, Boolean> HEALTH_CHECK = new java.util.EnumMap<>(PatchKind.class);

	/** Whether a kind of patch has a check-health state once grown; Hespori, for one, goes straight to harvest. */
	static synchronized boolean hasHealthCheck(PatchKind kind)
	{
		return HEALTH_CHECK.computeIfAbsent(kind, k ->
		{
			for (int v = 0; v < 256; v++)
			{
				PatchState s = k.forVarbitValue(v);
				if (s != null && s.getAction() == Action.CHECK_HEALTH)
				{
					return true;
				}
			}
			return false;
		});
	}

	/** The unix second of the tick {@code ticks} after the one {@code time} falls in. */
	public static long tickTime(int tickRate, int ticks, long time, Offset offset)
	{
		long shift = offset.seconds(tickRate);
		long t = time + shift;
		long period = tickRate * 60L;
		long current = t - (t % period);
		return current + ticks * period - shift;
	}

	/** Reads a spot from what was last seen. */
	public static Reading read(Spot spot, Seen seen, long now, Offset offset)
	{
		if (seen == null)
		{
			return Reading.unseen(spot);
		}
		return spot.isBirdhouse() ? readBirdhouse(spot, seen, now) : readPatch(spot, seen, now, offset);
	}

	private static Reading readBirdhouse(Spot spot, Seen seen, long now)
	{
		Birdhouse.State state = Birdhouse.state(seen.getValue());
		switch (state)
		{
			case EMPTY:
				return Reading.birdhouse(spot, seen, Status.READY, state, 0, "Empty space: build a bird house");
			case BUILT:
				return Reading.birdhouse(spot, seen, Status.READY, state, 0, "Built: add seeds");
			case SEEDED:
				long full = seen.getTime() + Birdhouse.FILL_SECONDS;
				return now >= full
					? Reading.birdhouse(spot, seen, Status.READY, state, full, "Full: collect and reset")
					: Reading.birdhouse(spot, seen, Status.GROWING, state, full, "Filling");
			default:
				return Reading.unseen(spot);
		}
	}

	private static Reading readPatch(Spot spot, Seen seen, long now, Offset offset)
	{
		PatchState state = spot.getKind().forVarbitValue(seen.getValue());
		if (state == null)
		{
			return Reading.unseen(spot);
		}

		int stage = state.getStage();
		int stages = state.getStages();
		int tickRate = state.getProduce().getTickrate();
		if (state.getCropState() == CropState.HARVESTABLE)
		{
			tickRate = state.getProduce().getRegrowTickrate();
		}

		if (state.isEmpty())
		{
			// Weeds grow back on their own but nothing is lost: the patch is ready to plant.
			return Reading.patch(spot, seen, Status.READY, state, state.isClear() ? Action.PLANT : Action.RAKE, 0,
				state.isClear() ? "Clear soil: plant it" : "Weeds: rake and plant");
		}

		switch (state.getCropState())
		{
			case DISEASED:
				return Reading.patch(spot, seen, Status.READY, state, state.getAction(), 0, "Diseased: cure it");
			case DEAD:
				return Reading.patch(spot, seen, Status.READY, state, Action.CLEAR, 0,
					state.getStage() == 0 && state.getAction() == Action.CLEAR ? "Stump: clear and replant" : "Dead: clear and replant");
			case HARVESTABLE:
				if (state.getAction() == Action.CHOP)
				{
					return Reading.patch(spot, seen, Status.READY, state, Action.CHOP, 0, "Grown: clear and replant");
				}
				if (state.getAction() == Action.CLEAR)
				{
					return Reading.patch(spot, seen, Status.READY, state, Action.CLEAR, 0, "Stump: clear and replant");
				}
				if (spot.getKind() == PatchKind.CALQUAT && stage == 0)
				{
					return Reading.patch(spot, seen, Status.READY, state, Action.CLEAR, 0, "Picked: dig it up and replant");
				}
				if (tickRate > 0)
				{
					// Berries and spines grow back a stage at a time; stop when the bush is full.
					long seenTick = tickTime(tickRate, 0, seen.getTime(), offset);
					long nowTick = tickTime(tickRate, 0, now, offset);
					int delta = (int) ((nowTick - seenTick) / (tickRate * 60L));
					long full = tickTime(tickRate, stages - 1 - stage, seenTick, offset);
					return stage + delta >= stages - 1
						? Reading.patch(spot, seen, Status.READY, state, Action.HARVEST, full, "Full: ready to pick")
						: Reading.patch(spot, seen, Status.GROWING, state, Action.WAIT, full, "Regrowing");
				}
				return Reading.patch(spot, seen, Status.READY, state, Action.HARVEST, 0, "Ready to harvest");
			case GROWING:
			default:
				break;
		}

		if (state.getAction() == Action.GROWN)
		{
			return Reading.patch(spot, seen, Status.DONE, state, Action.GROWN, 0, "Fully grown");
		}
		if (state.getAction() == Action.CHECK_HEALTH)
		{
			return Reading.patch(spot, seen, Status.READY, state, Action.CHECK_HEALTH, 0, "Ready: check its health");
		}
		if (tickRate <= 0)
		{
			return Reading.patch(spot, seen, Status.GROWING, state, Action.WAIT, 0, "Growing");
		}

		long seenTick = tickTime(tickRate, 0, seen.getTime(), offset);
		long nowTick = tickTime(tickRate, 0, now, offset);
		int delta = (int) ((nowTick - seenTick) / (tickRate * 60L));
		long done = tickTime(tickRate, stages - 1 - stage, seenTick, offset);
		boolean anima = spot.getKind() == PatchKind.ANIMA;
		if (stage + delta >= stages - 1)
		{
			if (anima)
			{
				return Reading.patch(spot, seen, Status.READY, state, Action.CLEAR, done, "Withered: clear and replant");
			}
			Action next = hasHealthCheck(spot.getKind()) ? Action.CHECK_HEALTH : Action.HARVEST;
			return Reading.patch(spot, seen, Status.READY, state, next, done,
				next == Action.CHECK_HEALTH ? "Ready: check its health" : "Ready to harvest");
		}
		return Reading.patch(spot, seen, Status.GROWING, state, Action.WAIT, done, anima ? "Active" : "Growing");
	}
}
