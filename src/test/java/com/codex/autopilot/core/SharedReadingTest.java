package com.codex.autopilot.core;

import com.codex.autopilot.core.farming.Action;
import com.codex.autopilot.core.farming.Produce;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Codex Rounds' tests for the parsing Autopilot shares with it, kept so the two stay in step. */
public class SharedReadingTest
{
	static final long T0 = 1_760_000_000L;

	static Vars vars(Map<Integer, Integer> bits, Map<Integer, Integer> ps)
	{
		return new Vars()
		{
			@Override
			public int varbit(int id)
			{
				return bits.getOrDefault(id, 0);
			}

			@Override
			public int varp(int id)
			{
				return ps.getOrDefault(id, 0);
			}
		};
	}

	@Test
	public void birdhouseVarps()
	{
		assertEquals(Birdhouse.State.EMPTY, Birdhouse.state(0));
		assertEquals(Birdhouse.State.BUILT, Birdhouse.state(1));
		assertEquals(Birdhouse.State.SEEDED, Birdhouse.state(3));
		assertEquals(Birdhouse.Type.NORMAL, Birdhouse.type(3));
		assertEquals(Birdhouse.Type.REDWOOD, Birdhouse.type(27));
		assertEquals(Birdhouse.State.SEEDED, Birdhouse.state(27));
		assertEquals(Birdhouse.State.UNKNOWN, Birdhouse.state(28));
		assertEquals(Birdhouse.Type.MAGIC, Birdhouse.Type.bestFor(80));
	}

	@Test
	public void birdhouseFillsAfterFiftyMinutes()
	{
		Seen seeded = new Seen(24, T0);
		Reading early = Growth.read(Spot.BIRDHOUSE_MEADOW_N, seeded, T0 + 49 * 60, Growth.Offset.NONE);
		assertEquals(Status.GROWING, early.getStatus());
		assertEquals(60, early.secondsLeft(T0 + 49 * 60));
		Reading full = Growth.read(Spot.BIRDHOUSE_MEADOW_N, seeded, T0 + 50 * 60, Growth.Offset.NONE);
		assertEquals(Status.READY, full.getStatus());
	}

	@Test
	public void teakGrowsToHealthCheck()
	{
		// Teak saplings are varbit 8 when planted and take seven 640-minute ticks.
		long planted = T0 - (T0 % (640 * 60)) + 60;
		Seen seen = new Seen(8, planted);
		Reading growing = Growth.read(Spot.HARDWOOD_FOSSIL_E, seen, planted + 3 * 640 * 60, Growth.Offset.NONE);
		assertEquals(Status.GROWING, growing.getStatus());
		assertEquals(Produce.TEAK, growing.getProduce());
		Reading ready = Growth.read(Spot.HARDWOOD_FOSSIL_E, seen, planted + 7 * 640 * 60, Growth.Offset.NONE);
		assertEquals(Status.READY, ready.getStatus());
		assertEquals(Action.CHECK_HEALTH, ready.getAction());
	}

	@Test
	public void bushesRegrowThenReady()
	{
		// Redberry bush picked down to stage 0 (value 10) regrows a berry every 20 minutes.
		long seen = T0 - (T0 % 1200) + 30;
		Reading r = Growth.read(Spot.BUSH_RIMMINGTON, new Seen(10, seen), seen + 20 * 60, Growth.Offset.NONE);
		assertEquals(Status.GROWING, r.getStatus());
		Reading full = Growth.read(Spot.BUSH_RIMMINGTON, new Seen(10, seen), seen + 4 * 20 * 60, Growth.Offset.NONE);
		assertEquals(Status.READY, full.getStatus());
		assertEquals(Action.HARVEST, full.getAction());
	}

	@Test
	public void weedsAndClearSoil()
	{
		assertEquals(Action.PLANT, Growth.read(Spot.CALQUAT_KASTORI, new Seen(3, T0), T0, Growth.Offset.NONE).getAction());
		assertEquals(Action.RAKE, Growth.read(Spot.CALQUAT_KASTORI, new Seen(0, T0), T0, Growth.Offset.NONE).getAction());
		// A calquat with every fruit picked is dug up and replanted.
		assertEquals(Action.CLEAR, Growth.read(Spot.CALQUAT_KASTORI, new Seen(12, T0), T0, Growth.Offset.NONE).getAction());
	}

	@Test
	public void spiritTreeGrownIsDone()
	{
		assertEquals(Status.DONE, Growth.read(Spot.SPIRIT_PORT_SARIM, new Seen(20, T0), T0, Growth.Offset.NONE).getStatus());
	}

	@Test
	public void offsetShiftsTicks()
	{
		long t = 1_000_000_200L;
		long plain = Growth.tickTime(5, 1, t, Growth.Offset.NONE);
		long shifted = Growth.tickTime(5, 1, t, new Growth.Offset(2, 640));
		assertEquals(0, plain % 300);
		assertEquals(120, (300 + plain - shifted) % 300);
	}

	@Test
	public void kingdomDecays()
	{
		long day = 86400;
		Kingdom k = Kingdom.estimate(127, 1_000_000, T0, T0 + 5 * day, false);
		// 50k a day while over 500k, then a tenth.
		assertEquals(750_000, k.getCoffer());
		assertEquals(127 - (int) (5 * 0.025f * 127), k.getApproval());
		assertTrue(k.isEstimated());
		Kingdom rt = Kingdom.estimate(127, 1_000_000, T0, T0 + 2 * day, true);
		assertEquals(850_000, rt.getCoffer());
		assertEquals(0, k.topUp(500_000));
		assertEquals(Kingdom.MAX_COFFER - 750_000, k.topUp(99_000_000));
		assertTrue(k.needsVisit(100, 750_000, 100_000));
	}

	@Test
	public void dailiesFollowDiaries()
	{
		Map<Integer, Integer> bits = new HashMap<>();
		bits.put(VarbitID.VARROCK_DIARY_EASY_COMPLETE, 1);
		bits.put(VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, 1);
		bits.put(VarbitID.VARROCK_DIARY_HARD_COMPLETE, 1);
		bits.put(VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, 1);
		bits.put(VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, 1);
		bits.put(VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, 1);
		bits.put(VarbitID.MORYTANIA_SLIME_CLAIMED, 10);
		Vars v = vars(bits, new HashMap<>());
		assertTrue(Daily.ZAFF.claimable(v));
		assertEquals(60, Daily.ZAFF.amount(v));
		assertEquals(60 * Daily.ZAFF_PRICE, Daily.ZAFF.need(v).getAmount());
		assertEquals(26, Daily.ROBIN.amount(v));
		assertEquals(16, Daily.ROBIN.remaining(v));
		assertFalse(Daily.CROMPERTY.unlocked(v));
		bits.put(VarbitID.ZAFF_LAST_CLAIMED, 1);
		assertFalse(Daily.ZAFF.claimable(v));
	}

	@Test
	public void handlesFarmingContracts()
	{
		Map<Integer, Integer> bits = new HashMap<>();
		Vars none = vars(bits, new HashMap<>());
		assertFalse(Daily.CONTRACT.unlocked(none));

		bits.put(VarbitID.FARMGUILD_CONTRACT_TYPE, 6);
		Vars ivy = vars(bits, new HashMap<>());
		assertTrue(Daily.CONTRACT.unlocked(ivy));
		assertFalse(Daily.CONTRACT.claimable(ivy));
		assertEquals("Poison ivy in the guild's bush patch", Daily.CONTRACT.reward(ivy));

		bits.put(VarbitID.FARMGUILD_CONTRACT_COMPLETE, 1);
		assertTrue(Daily.CONTRACT.claimable(vars(bits, new HashMap<>())));

		bits.put(VarbitID.FARMGUILD_CONTRACT_TYPE, 24);
		bits.put(VarbitID.FARMGUILD_CONTRACT_COMPLETE, 0);
		assertEquals("Ranarr in the guild's herb patch", Daily.CONTRACT.reward(vars(bits, new HashMap<>())));
		assertNull(Contract.produce(24));
	}
}
