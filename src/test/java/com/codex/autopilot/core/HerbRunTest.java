package com.codex.autopilot.core;

import com.codex.autopilot.core.farming.PatchKind;
import com.codex.autopilot.core.farming.PatchState;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HerbRunTest
{
	static final long T0 = AutopilotCoreTest.T0;

	// Herb patch values: ranarr growing 32-35, ranarr to pick 36-38, diseased guam 128, dead herbs 170.
	static final int RANARR_PICK = 36;
	static final int RANARR_LAST_STAGE = 35;
	static final int HERB_DISEASED = 128;
	static final int HERB_DEAD = 170;
	static final int HERB_WEEDS = 0;
	static final int POTATO_PICK = 10;
	static final int MARIGOLD_PICK = 12;
	static final int SCARECROW = 36;
	static final int HAMMERSTONE_PICK = 8;

	static AutopilotCoreTest.MapProfile put(AutopilotCoreTest.MapProfile p, Spot s, int value, long time)
	{
		return p.put(Chores.GROUP, s.timeTrackingKey(), value + ":" + time);
	}

	static List<Candidate> chores(AutopilotCoreTest.MapProfile p, BankSnapshot bank, long now)
	{
		return AutopilotCoreTest.chores(p, bank, now);
	}

	static Candidate find(List<Candidate> list, Candidate.Kind kind)
	{
		for (Candidate c : list)
		{
			if (c.getKind() == kind)
			{
				return c;
			}
		}
		return null;
	}

	// ---- The tables match RuneLite's ----

	@Test
	public void herbAllotmentFlowerHopsTablesMatchRuneLite() throws Exception
	{
		Class<?> impl = Class.forName("net.runelite.client.plugins.timetracking.farming.PatchImplementation");
		Method forValue = impl.getDeclaredMethod("forVarbitValue", int.class);
		forValue.setAccessible(true);
		Class<?> theirState = Class.forName("net.runelite.client.plugins.timetracking.farming.PatchState");
		Method produce = theirState.getDeclaredMethod("getProduce");
		Method crop = theirState.getDeclaredMethod("getCropState");
		Method stage = theirState.getDeclaredMethod("getStage");
		produce.setAccessible(true);
		crop.setAccessible(true);
		stage.setAccessible(true);
		for (PatchKind kind : new PatchKind[]{PatchKind.HERB, PatchKind.ALLOTMENT, PatchKind.FLOWER, PatchKind.HOPS})
		{
			@SuppressWarnings({"unchecked", "rawtypes"})
			Object theirs = Enum.valueOf((Class) impl, kind.name());
			for (int v = 0; v < 256; v++)
			{
				Object t = forValue.invoke(theirs, v);
				PatchState o = kind.forVarbitValue(v);
				String at = kind + " " + v;
				if (t == null)
				{
					assertNull(at, o);
					continue;
				}
				assertNotNull(at, o);
				assertEquals(at, ((Enum<?>) produce.invoke(t)).name(), o.getProduce().name());
				assertEquals(at, ((Enum<?>) crop.invoke(t)).name(), o.getCropState().name());
				assertEquals(at, stage.invoke(t), o.getStage());
				assertEquals(at, ((Enum<?>) produce.invoke(t)).name().equals("WEEDS"), o.isEmpty());
			}
		}
	}

	@Test
	public void everyNewSpotIsAPatchRuneLiteTracks() throws Exception
	{
		Class<?> world = Class.forName("net.runelite.client.plugins.timetracking.farming.FarmingWorld");
		java.lang.reflect.Constructor<?> make = world.getDeclaredConstructor();
		make.setAccessible(true);
		Object w = make.newInstance();
		Method tabs = world.getDeclaredMethod("getTabs");
		tabs.setAccessible(true);
		Class<?> patch = Class.forName("net.runelite.client.plugins.timetracking.farming.FarmingPatch");
		Method region = patch.getDeclaredMethod("getRegion");
		Method varbit = patch.getDeclaredMethod("getVarbit");
		Method implementation = patch.getDeclaredMethod("getImplementation");
		region.setAccessible(true);
		varbit.setAccessible(true);
		implementation.setAccessible(true);
		Set<String> theirs = new HashSet<>();
		Set<String> wanted = new HashSet<>();
		for (Object set : ((Map<?, ?>) tabs.invoke(w)).values())
		{
			for (Object p : (Collection<?>) set)
			{
				Object r = region.invoke(p);
				int id = (Integer) r.getClass().getMethod("getRegionID").invoke(r);
				String impl = ((Enum<?>) implementation.invoke(p)).name();
				String key = id + "." + varbit.invoke(p) + ":" + impl;
				theirs.add(key);
				if (impl.equals("HERB") || impl.equals("ALLOTMENT") || impl.equals("FLOWER") || impl.equals("HOPS"))
				{
					wanted.add(key);
				}
			}
		}
		Set<String> ours = new HashSet<>();
		for (Spot s : Spot.values())
		{
			if (!s.isBirdhouse())
			{
				String key = s.timeTrackingKey() + ":" + s.getKind().name();
				assertTrue(key + " is not a RuneLite patch", theirs.contains(key));
				assertTrue(key + " twice", ours.add(key));
			}
		}
		for (String key : wanted)
		{
			assertTrue("missing " + key, ours.contains(key));
		}
	}

	// ---- Reading ----

	@Test
	public void herbsGrowToReadyAndScarecrowsAreLeftAlone()
	{
		Reading growing = Growth.read(Spot.HERB_CATHERBY, new Seen(RANARR_LAST_STAGE, T0), T0 + 60, Growth.Offset.NONE);
		assertEquals(Status.GROWING, growing.getStatus());
		Reading grown = Growth.read(Spot.HERB_CATHERBY, new Seen(RANARR_LAST_STAGE, T0), T0 + 21 * 60, Growth.Offset.NONE);
		assertEquals(Status.READY, grown.getStatus());
		assertEquals(Status.READY, Growth.read(Spot.HERB_CATHERBY, new Seen(RANARR_PICK, T0), T0, Growth.Offset.NONE).getStatus());
		Reading crow = Growth.read(Spot.FLOWER_FALADOR, new Seen(SCARECROW, T0), T0, Growth.Offset.NONE);
		assertEquals(Status.DONE, crow.getStatus());
	}

	// ---- The herb run ----

	@Test
	public void herbRunGroupsHerbsWithAllotmentsAndFlowers()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		put(p, Spot.HERB_CATHERBY, RANARR_PICK, T0 - 600);
		put(p, Spot.HERB_ARDOUGNE, RANARR_PICK, T0 - 600);
		put(p, Spot.HERB_FALADOR, RANARR_PICK, T0 - 600);
		put(p, Spot.ALLOTMENT_CATHERBY_N, POTATO_PICK, T0 - 600);
		put(p, Spot.ALLOTMENT_CATHERBY_S, POTATO_PICK, T0 - 600);
		put(p, Spot.FLOWER_CATHERBY, MARIGOLD_PICK, T0 - 600);
		Candidate c = find(chores(p, null, T0), Candidate.Kind.HERB_RUN);
		assertNotNull(c);
		assertEquals("Herb run", c.getOver());
		assertEquals("3 ready, 2 allotments, 1 flower patch", c.getTitle());
		assertEquals(Tier.READY, c.getTier());
		assertEquals("chores:herb_run", c.getKey());
		assertEquals(6, c.getCount());
		assertEquals(ItemID.RANARR_WEED, c.getItemId());
		assertEquals("Bring 3 herb seeds + 6 compost", c.getBring());
		// Three towns, three extra patches in one of them.
		assertEquals((int) Math.round(1 + 3 * 2.5 + 3 * 0.75), c.getMinutes());
	}

	@Test
	public void diseasedHerbIsGoingToWaste()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		put(p, Spot.HERB_CATHERBY, RANARR_PICK, T0 - 600);
		put(p, Spot.HERB_FALADOR, HERB_DISEASED, T0 - 60);
		Candidate c = find(chores(p, null, T0), Candidate.Kind.HERB_RUN);
		assertNotNull(c);
		assertEquals(Tier.WASTING, c.getTier());
		assertEquals("1 ready, 1 diseased", c.getTitle());
		assertTrue(c.getDeadline() > T0);
		assertTrue(c.getWhy(), c.getWhy().startsWith("Herb patch at Falador is diseased: cure it within"));
		// The diseased patch is cured, not replanted: one seed.
		assertEquals("Bring 1 herb seed + 1 compost", c.getBring());
	}

	@Test
	public void deadHerbIsGoingToWasteUntilLongAbandoned()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		put(p, Spot.HERB_CATHERBY, HERB_DEAD, T0 - 3600);
		Candidate c = find(chores(p, null, T0), Candidate.Kind.HERB_RUN);
		assertNotNull(c);
		assertEquals(Tier.WASTING, c.getTier());
		assertEquals(0, c.getDeadline());
		assertEquals("1 dead", c.getTitle());
		assertTrue(c.getWhy().startsWith("A herb patch has died"));

		AutopilotCoreTest.MapProfile old = new AutopilotCoreTest.MapProfile();
		put(old, Spot.HERB_CATHERBY, HERB_DEAD, T0 - 3 * 86_400);
		assertNull(find(chores(old, null, T0), Candidate.Kind.HERB_RUN));
	}

	@Test
	public void weedsAndNeverPlantedPatchesAreNotNagged()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		put(p, Spot.HERB_CATHERBY, HERB_WEEDS, T0 - 86_400);
		put(p, Spot.ALLOTMENT_FALADOR_NW, 0, T0 - 86_400);
		assertTrue(chores(p, null, T0).isEmpty());
	}

	@Test
	public void flowersAloneMakeNoStepButAllotmentsDo()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		put(p, Spot.FLOWER_CATHERBY, MARIGOLD_PICK, T0 - 600);
		assertNull(find(chores(p, null, T0), Candidate.Kind.HERB_RUN));
		put(p, Spot.ALLOTMENT_CATHERBY_N, POTATO_PICK, T0 - 600);
		Candidate c = find(chores(p, null, T0), Candidate.Kind.HERB_RUN);
		assertNotNull(c);
		assertEquals("Allotment run", c.getOver());
		assertEquals("1 ready, 1 flower patch", c.getTitle());
	}

	@Test
	public void hopsHaveTheirOwnRun()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		put(p, Spot.HOPS_YANILLE, HAMMERSTONE_PICK, T0 - 600);
		put(p, Spot.HOPS_LUMBRIDGE, HAMMERSTONE_PICK, T0 - 600);
		put(p, Spot.HERB_CATHERBY, RANARR_PICK, T0 - 600);
		List<Candidate> list = chores(p, null, T0);
		Candidate hops = find(list, Candidate.Kind.HOPS_RUN);
		assertNotNull(hops);
		assertEquals("Hops run", hops.getOver());
		assertEquals("2 ready", hops.getTitle());
		assertEquals("Bring 2 compost", hops.getBring());
		assertNotNull(find(list, Candidate.Kind.HERB_RUN));
	}

	@Test
	public void bringChecksSeedsAndCompostInTheBank()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		put(p, Spot.HERB_CATHERBY, RANARR_PICK, T0 - 600);
		put(p, Spot.HERB_ARDOUGNE, RANARR_PICK, T0 - 600);
		Map<Integer, Long> items = new HashMap<>();
		items.put(ItemID.SNAPDRAGON_SEED, 1L);
		items.put(ItemID.TORSTOL_SEED, 5L);
		items.put(ItemID.BUCKET_ULTRACOMPOST, 10L);
		assertEquals("Bring 2 herb seeds + 2 compost", find(chores(p, BankSnapshot.of(items, 1), T0), Candidate.Kind.HERB_RUN).getBring());

		Map<Integer, Long> bottomless = new HashMap<>();
		bottomless.put(ItemID.RANARR_SEED, 20L);
		bottomless.put(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED, 1L);
		assertEquals("Bring 2 herb seeds + 1 bottomless compost bucket",
			find(chores(p, BankSnapshot.of(bottomless, 1), T0), Candidate.Kind.HERB_RUN).getBring());

		Map<Integer, Long> none = new HashMap<>();
		none.put(ItemID.BUCKET_COMPOST, 10L);
		assertEquals("You're out of herb seeds", find(chores(p, BankSnapshot.of(none, 1), T0), Candidate.Kind.HERB_RUN).getBring());
	}

	@Test
	public void herbAndHopsSwitchesLeaveTheOtherChoresAlone()
	{
		AutopilotCoreTest.MapProfile p = AutopilotCoreTest.fullHouses(new AutopilotCoreTest.MapProfile());
		put(p, Spot.HERB_CATHERBY, RANARR_PICK, T0 - 600);
		put(p, Spot.HOPS_YANILLE, HAMMERSTONE_PICK, T0 - 600);
		Advisor.Inputs in = Advisor.Inputs.read(p, AutopilotCoreTest.GSON, T0, false);
		List<Candidate> all = Advisor.candidates(in, Settings.defaults(), T0);
		assertNotNull(find(all, Candidate.Kind.HERB_RUN));
		assertNotNull(find(all, Candidate.Kind.HOPS_RUN));
		assertNotNull(find(all, Candidate.Kind.BIRDHOUSE_RUN));

		List<Candidate> noHerbs = Advisor.candidates(in, Settings.defaults().herbs(false).hops(false), T0);
		assertNull(find(noHerbs, Candidate.Kind.HERB_RUN));
		assertNull(find(noHerbs, Candidate.Kind.HOPS_RUN));
		assertNotNull(find(noHerbs, Candidate.Kind.BIRDHOUSE_RUN));

		// The v1 switch keeps its meaning: bird houses and the special patches, not herbs.
		List<Candidate> noChores = Advisor.candidates(in, Settings.defaults().chores(false), T0);
		assertNull(find(noChores, Candidate.Kind.BIRDHOUSE_RUN));
		assertNotNull(find(noChores, Candidate.Kind.HERB_RUN));
	}

	@Test
	public void roundsHandOffNotesChoresOnly()
	{
		AutopilotCoreTest.MapProfile p = AutopilotCoreTest.fullHouses(new AutopilotCoreTest.MapProfile());
		put(p, Spot.HERB_CATHERBY, RANARR_PICK, T0 - 600);
		Advisor.Inputs in = Advisor.Inputs.read(p, AutopilotCoreTest.GSON, T0, false);
		in.dailies = Dailies.read(AutopilotCoreTest.vars(new HashMap<>()));
		for (Candidate c : Advisor.candidates(in, Settings.defaults().rounds(true), T0))
		{
			assertEquals(c.toString(), c.getKind().isChore(), c.getWhy().endsWith("; " + Advisor.ROUNDS));
		}
		for (Candidate c : Advisor.candidates(in, Settings.defaults(), T0))
		{
			assertFalse(c.getWhy().contains(Advisor.ROUNDS));
		}
	}

	@Test
	public void nextUpSkipsTheRidersButNamesHerbs()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		// Flowers ready in a few minutes, herbs in about twenty.
		put(p, Spot.FLOWER_CATHERBY, 11, T0 - 60);
		put(p, Spot.HERB_CATHERBY, RANARR_LAST_STAGE, T0 - 60);
		Advisor.Inputs in = Advisor.Inputs.read(p, AutopilotCoreTest.GSON, T0, false);
		String next = Advisor.nextUp(in, Settings.defaults(), T0);
		assertNotNull(next);
		assertTrue(next, next.startsWith("Herbs at Catherby ready in"));
	}
}
