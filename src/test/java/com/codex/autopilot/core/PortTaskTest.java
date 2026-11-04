package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.DBTableID.SailingDock;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Sailing port tasks as Autopilot reads and plans them. The tasks here are real ones, as the OSRS
 * wiki lists them (Courier tasks: "1, Port Sarim board, Port Sarim to The Pandemonium, Platebodies,
 * 1"; "1, Port Sarim board, The Pandemonium to Port Sarim, Spices, 1"; "10, Musa Point board, Musa
 * Point to The Pandemonium, Bananas, 1"; "10, Port Sarim board, Port Sarim to Musa Point, Logs, 1";
 * Bounty tasks: "50, Catherby board, Albatross, beak, 5"). Slots by level and the bounty level are the
 * wiki's (Port tasks; Bounty tasks), the notice boards are the wiki's Mooring point table, and the
 * varbits and dock rows are RuneLite's gameval names.
 */
public class PortTaskTest
{
	static final Gson GSON = new Gson();
	static final int SARIM = SailingDock.Row.SAILING_DOCK_PORT_SARIM;
	static final int PANDEMONIUM = SailingDock.Row.SAILING_DOCK_THE_PANDEMONIUM;
	static final int MUSA = SailingDock.Row.SAILING_DOCK_MUSA_POINT;
	static final int CATHERBY = SailingDock.Row.SAILING_DOCK_CATHERBY;
	static final int HOSIDIUS = SailingDock.Row.SAILING_DOCK_HOSIDIUS;
	static final int LANDS_END = SailingDock.Row.SAILING_DOCK_LANDS_END;

	static PortTask platebodies()
	{
		return PortTask.courier(11, "Port Sarim Platebody Delivery", 1, SARIM, "Port Sarim", SARIM, "Port Sarim",
			PANDEMONIUM, "The Pandemonium", 501, "Platebodies", 1);
	}

	static PortTask spices()
	{
		return PortTask.courier(12, "Port Sarim Spice Delivery", 1, SARIM, "Port Sarim", PANDEMONIUM, "The Pandemonium",
			SARIM, "Port Sarim", 502, "Spices", 1);
	}

	static PortTask bananas()
	{
		return PortTask.courier(13, "Musa Point Banana Delivery", 10, MUSA, "Musa Point", MUSA, "Musa Point",
			PANDEMONIUM, "The Pandemonium", 503, "Bananas", 1);
	}

	static PortTask logs()
	{
		return PortTask.courier(14, "Port Sarim Log Delivery", 10, SARIM, "Port Sarim", SARIM, "Port Sarim",
			MUSA, "Musa Point", 504, "Logs", 1);
	}

	static PortTask rope(int crates)
	{
		// Land's End to Hosidius: the wiki's row is one crate; two are used here to test a multi-crate task.
		return PortTask.courier(15, "Land's End Rope Delivery", 5, LANDS_END, "Land's End", LANDS_END, "Land's End",
			HOSIDIUS, "Hosidius", 505, "Rope", crates);
	}

	static PortTask albatross()
	{
		return PortTask.bounty(40, "Catherby Albatross Bounty", 50, CATHERBY, "Catherby", "Albatross", 601, "Albatross beak", 5);
	}

	static PortLog log(int sailing, int completed, PortTask... tasks)
	{
		PortLog l = PortLog.empty();
		l.update(Arrays.asList(tasks), completed, sailing, 1);
		return l;
	}

	static Candidate find(List<Candidate> list, String key)
	{
		for (Candidate c : list)
		{
			if (c.getKey().equals(key))
			{
				return c;
			}
		}
		return null;
	}

	// ---- The facts ----

	@Test
	public void slotsOpenAtTheWikisLevels()
	{
		assertEquals(0, PortLog.slots(0));
		assertEquals(1, PortLog.slots(1));
		assertEquals(1, PortLog.slots(6));
		assertEquals(2, PortLog.slots(7));
		assertEquals(2, PortLog.slots(27));
		assertEquals(3, PortLog.slots(28));
		assertEquals(4, PortLog.slots(56));
		assertEquals(4, PortLog.slots(83));
		assertEquals(5, PortLog.slots(84));
		assertEquals(5, PortLog.slots(99));
		assertEquals(28, PortLog.nextSlotLevel(27));
		assertEquals(0, PortLog.nextSlotLevel(84));
		assertEquals(30, PortLog.BOUNTY_LEVEL);
	}

	@Test
	public void theSlotVarbitsAreRuneLitesGamevals()
	{
		assertEquals(19574, VarbitID.PORT_TASK_SLOT_0_ID);
		assertEquals(VarbitID.PORT_TASK_SLOT_4_ID, PortLog.SLOT_ID[4]);
		assertEquals(VarbitID.PORT_TASK_SLOT_2_CARGO_TAKEN, PortLog.SLOT_TAKEN[2]);
		assertEquals(VarbitID.PORT_TASK_SLOT_3_CARGO_DELIVERED, PortLog.SLOT_DELIVERED[3]);
		assertEquals(VarbitID.BOUNTY_TASK_ITEM_COUNTER_1, PortLog.SLOT_BOUNTY[0]);
		assertEquals(VarbitID.BOUNTY_TASK_ITEM_COUNTER_5, PortLog.SLOT_BOUNTY[4]);
		assertEquals(5207, PortLog.COMPLETED);
		assertEquals(VarPlayerID.PORT_TASKS_COMPLETED, PortLog.COMPLETED);
	}

	@Test
	public void noticeBoardsAreTheMooringPointTables()
	{
		// Notice boards at Port Sarim, Rellekka and Deepfin Point; none at Hosidius, Rimmington or Entrana.
		assertTrue(PortTasks.NOTICE_BOARDS.contains(SARIM));
		assertTrue(PortTasks.NOTICE_BOARDS.contains(SailingDock.Row.SAILING_DOCK_RELLEKKA));
		assertTrue(PortTasks.NOTICE_BOARDS.contains(SailingDock.Row.SAILING_DOCK_DEEPFIN_POINT));
		assertFalse(PortTasks.NOTICE_BOARDS.contains(HOSIDIUS));
		assertFalse(PortTasks.NOTICE_BOARDS.contains(SailingDock.Row.SAILING_DOCK_RIMMINGTON));
		assertFalse(PortTasks.NOTICE_BOARDS.contains(SailingDock.Row.SAILING_DOCK_ENTRANA));
		assertEquals(23, PortTasks.NOTICE_BOARDS.size());
	}

	// ---- The voyage ----

	@Test
	public void oneTaskIsCollectThenDeliver()
	{
		Voyage v = Voyage.plan(Arrays.asList(platebodies().inSlot(0, 0, 0, 0)));
		assertEquals("Port Sarim → The Pandemonium", v.route());
		assertEquals("Collect platebodies", v.getStops().get(0).actions());
		assertEquals("Deliver platebodies", v.getStops().get(1).actions());
	}

	@Test
	public void tasksBoundForOnePortShareTheStop()
	{
		// Platebodies from Port Sarim and bananas from Musa Point both go to The Pandemonium: three stops, not four.
		Voyage v = Voyage.plan(Arrays.asList(platebodies().inSlot(0, 0, 0, 0), bananas().inSlot(1, 0, 0, 0)));
		assertEquals(3, v.getStops().size());
		assertEquals("The Pandemonium", v.getStops().get(2).getPort());
		assertEquals("Deliver platebodies and bananas", v.getStops().get(2).actions());
	}

	@Test
	public void cargoWaitingWhereAnotherIsDeliveredIsPickedUpOnTheSameCall()
	{
		// Platebodies out to The Pandemonium, spices back from it: three stops, the middle one doing both.
		Voyage v = Voyage.plan(Arrays.asList(platebodies().inSlot(0, 0, 0, 0), spices().inSlot(1, 0, 0, 0)));
		assertEquals("Port Sarim → The Pandemonium → Port Sarim", v.route());
		assertEquals("Deliver platebodies, collect spices", v.getStops().get(1).actions());
		assertEquals("Deliver spices", v.getStops().get(2).actions());
	}

	@Test
	public void aChainOfThreeIsFourStops()
	{
		// Logs Port Sarim to Musa Point, bananas Musa Point to The Pandemonium, spices The Pandemonium to Port Sarim.
		Voyage v = Voyage.plan(Arrays.asList(logs().inSlot(0, 0, 0, 0), bananas().inSlot(1, 0, 0, 0), spices().inSlot(2, 0, 0, 0)));
		assertEquals(4, v.getStops().size());
		for (int i = 1; i < 3; i++)
		{
			Voyage.Stop s = v.getStops().get(i);
			assertEquals(s.toString(), 1, s.getCollect().size());
			assertEquals(s.toString(), 1, s.getDeliver().size());
		}
	}

	@Test
	public void cratesAlreadyTakenOnlyNeedDelivering()
	{
		Voyage v = Voyage.plan(Arrays.asList(platebodies().inSlot(0, 1, 0, 0), logs().inSlot(1, 0, 0, 0)));
		// The platebodies are aboard: with as few stops either way, they are unloaded first.
		assertEquals(1, v.aboard());
		assertEquals("The Pandemonium → Port Sarim → Musa Point", v.route());
	}

	@Test
	public void aMultiCrateTaskCountsWhatIsLeft()
	{
		Voyage v = Voyage.plan(Arrays.asList(rope(2).inSlot(0, 1, 0, 0)));
		assertEquals("Collect rope", v.getStops().get(0).actions());
		assertEquals("Deliver 2 crates of rope", v.getStops().get(1).actions());
		Voyage none = Voyage.plan(Arrays.asList(rope(2).inSlot(0, 0, 0, 0)));
		assertEquals("Collect 2 crates of rope", none.getStops().get(0).actions());
		// One of two delivered, the second still to fetch.
		Voyage half = Voyage.plan(Arrays.asList(rope(2).inSlot(0, 1, 1, 0)));
		assertEquals("Collect rope", half.getStops().get(0).actions());
		assertEquals("Deliver rope", half.getStops().get(1).actions());
	}

	@Test
	public void finishedAndBountyTasksAreNotOnTheVoyage()
	{
		assertTrue(Voyage.plan(Arrays.asList(platebodies().inSlot(0, 1, 1, 0))).isEmpty());
		assertTrue(Voyage.plan(Arrays.asList(albatross().inSlot(0, 0, 0, 5))).isEmpty());
		assertTrue(Voyage.plan(null).isEmpty());
	}

	// ---- The steps ----

	@Test
	public void theVoyageIsAGoalUntilCratesAreAboard()
	{
		Candidate c = find(PortTasks.candidates(log(10, 0, platebodies().inSlot(0, 0, 0, 0), bananas().inSlot(1, 0, 0, 0))),
			PortTasks.VOYAGE);
		assertEquals(Tier.GOAL, c.getTier());
		assertEquals("Port tasks", c.getOver());
		assertEquals("Collect bananas at Musa Point", c.getTitle());
		assertEquals("Musa Point → Port Sarim → The Pandemonium: 2 tasks, 3 stops", c.getWhy());
		assertEquals(3 * PortTasks.STOP_MINUTES, c.getMinutes());
		assertEquals(503, c.getItemId());

		Candidate aboard = find(PortTasks.candidates(log(6, 0, platebodies().inSlot(0, 1, 0, 0))), PortTasks.VOYAGE);
		assertEquals(Tier.READY, aboard.getTier());
		assertEquals("Deliver platebodies at The Pandemonium", aboard.getTitle());
		assertEquals("1 crate aboard, lost if you die, capsize or teleport off at sea; One stop finishes it", aboard.getWhy());
	}

	@Test
	public void theXpATaskPaidBeforeIsShownOnlyWhenEveryTaskHasPaid()
	{
		PortLog l = log(10, 3, platebodies().inSlot(0, 0, 0, 0), bananas().inSlot(1, 0, 0, 0));
		assertTrue(l.paid(11, 78));
		assertFalse(find(PortTasks.candidates(l), PortTasks.VOYAGE).getWhy().contains("xp"));
		assertTrue(l.paid(13, 89));
		assertFalse(l.paid(13, 89));
		assertTrue(find(PortTasks.candidates(l), PortTasks.VOYAGE).getWhy().endsWith("they paid you 167 Sailing xp last time"));
		assertEquals(0, l.xp(14));
		assertFalse(l.paid(14, 0));
	}

	@Test
	public void aBountyCountsItsPartsAndIsClaimedAtAnyPortMaster()
	{
		// The counter holds the parts still wanted: 3 of 5 still wanted is 2 in hand.
		Candidate hunt = find(PortTasks.candidates(log(50, 9, albatross().inSlot(0, 0, 0, 3))), PortTasks.BOUNTY + 0);
		assertEquals(Tier.GOAL, hunt.getTier());
		assertEquals("Albatross beak, 2 of 5", hunt.getTitle());
		assertTrue(hunt.getWhy().startsWith("Hunt albatross at sea until 5 drop"));

		Candidate claim = find(PortTasks.candidates(log(50, 9, albatross().inSlot(0, 0, 0, 0))), PortTasks.BOUNTY + 0);
		assertEquals(Tier.READY, claim.getTier());
		assertEquals("Claim the albatross beak bounty", claim.getTitle());
		assertTrue(claim.getWhy().startsWith("Any port master pays it"));
	}

	@Test
	public void openSlotsRideAlongFromWhereTheVoyageEnds()
	{
		// 28 Sailing holds three; one held. The Pandemonium has a notice board: a note on the voyage, not a step.
		List<Candidate> all = PortTasks.candidates(log(28, 5, platebodies().inSlot(0, 0, 0, 0)));
		assertNull(find(all, PortTasks.TAKE));
		assertTrue(find(all, PortTasks.VOYAGE).getWhy().endsWith("then take 2 more tasks at The Pandemonium's notice board, where it ends"));

		// Hosidius has none: nothing said.
		assertFalse(find(PortTasks.candidates(log(7, 5, rope(1).inSlot(0, 0, 0, 0))), PortTasks.VOYAGE).getWhy().contains("take"));

		// Only a bounty in hand, no voyage: a step of its own.
		Candidate take = find(PortTasks.candidates(log(50, 5, albatross().inSlot(0, 0, 0, 5))), PortTasks.TAKE);
		assertEquals("Take 2 more port tasks", take.getTitle());
		assertEquals("From any port's notice board; 1 of 3 slots in use", take.getWhy());
		assertEquals(Tier.GOAL, take.getTier());

		// Every slot full, or an account that has never done one: nothing to offer.
		assertNull(find(PortTasks.candidates(log(6, 5, albatross().inSlot(0, 0, 0, 5))), PortTasks.TAKE));
		assertTrue(PortTasks.candidates(log(50, 0)).isEmpty());
		Candidate fresh = find(PortTasks.candidates(log(50, 1)), PortTasks.TAKE);
		assertEquals("Take 3 more port tasks", fresh.getTitle());
		assertEquals("From any port's notice board: 50 Sailing holds 3", fresh.getWhy());
	}

	@Test
	public void theAdvisorPlansPortTasksAndTheSwitchTurnsThemOff()
	{
		Advisor.Inputs in = new Advisor.Inputs();
		in.ports = log(28, 5, platebodies().inSlot(0, 1, 0, 0));
		List<Candidate> all = Advisor.candidates(in, Settings.defaults(), AutopilotCoreTest.T0);
		assertEquals(Candidate.Kind.PORT_TASK, find(all, PortTasks.VOYAGE).getKind());
		assertNull(find(Advisor.candidates(in, Settings.defaults().portTasks(false), AutopilotCoreTest.T0), PortTasks.VOYAGE));
		Plan plan = Ranker.rank(all, Settings.defaults(), new Session(), AutopilotCoreTest.T0);
		assertEquals(PortTasks.VOYAGE, plan.top().getKey());
	}

	// ---- Reading and saving ----

	static Vars vars(Map<Integer, Integer> bits)
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
				return 0;
			}
		};
	}

	@Test
	public void theSlotsAreReadThroughTheGamesTable()
	{
		Map<Integer, PortTask> table = new HashMap<>();
		table.put(11, platebodies());
		table.put(40, albatross());
		Map<Integer, Integer> bits = new HashMap<>();
		bits.put(VarbitID.PORT_TASK_SLOT_0_ID, 11);
		bits.put(VarbitID.PORT_TASK_SLOT_0_CARGO_TAKEN, 1);
		bits.put(VarbitID.PORT_TASK_SLOT_2_ID, 40);
		bits.put(VarbitID.BOUNTY_TASK_ITEM_COUNTER_3, 4);
		// A task the table does not know (a quest's own) is left out.
		bits.put(VarbitID.PORT_TASK_SLOT_3_ID, 999);
		List<PortTask> read = PortLog.empty().read(vars(bits), table::get);
		assertEquals(2, read.size());
		assertEquals(0, read.get(0).getSlot());
		assertEquals(1, read.get(0).getTaken());
		assertEquals(1, read.get(0).aboard());
		assertEquals(2, read.get(1).getSlot());
		assertEquals(1, read.get(1).getHave());
	}

	@Test
	public void aTaskTheTableCannotGiveIsTakenFromTheSave()
	{
		PortLog saved = log(10, 2, platebodies().inSlot(0, 0, 0, 0));
		PortLog back = PortLog.parse(GSON, saved.toJson(GSON));
		Map<Integer, Integer> bits = new HashMap<>();
		bits.put(VarbitID.PORT_TASK_SLOT_1_ID, 11);
		bits.put(VarbitID.PORT_TASK_SLOT_1_CARGO_TAKEN, 1);
		List<PortTask> read = back.read(vars(bits), id ->
		{
			throw new IllegalStateException("cache not ready");
		});
		assertEquals(1, read.size());
		assertEquals("The Pandemonium", read.get(0).getTo());
		assertEquals(1, read.get(0).getSlot());
		assertEquals(1, read.get(0).getTaken());
	}

	@Test
	public void theSaveRoundTripsAndOnlyChangesWhenTheTasksDo()
	{
		PortLog l = log(62, 140, platebodies().inSlot(0, 1, 0, 0), albatross().inSlot(1, 0, 0, 2));
		l.paid(11, 78);
		String json = l.toJson(GSON);
		assertTrue(json.contains("\"type\":\"COURIER\""));
		assertTrue(json.contains("\"xp\":{\"11\":78}"));
		PortLog back = PortLog.parse(GSON, json);
		assertEquals(l.getTasks(), back.getTasks());
		assertEquals(140, back.getCompleted());
		assertEquals(62, back.getSailing());
		assertEquals(78, back.xp(11));
		assertFalse(back.update(new ArrayList<>(back.getTasks()), 140, 62, 2));
		assertTrue(back.update(Arrays.asList(platebodies().inSlot(0, 1, 1, 0)), 141, 62, 3));
	}

	@Test
	public void aDamagedSaveIsAnEmptyLog()
	{
		assertTrue(PortLog.parse(GSON, "{not json").getTasks().isEmpty());
		assertTrue(PortLog.parse(GSON, null).getTasks().isEmpty());
		assertEquals(-1, PortLog.parse(GSON, "").getCompleted());
		// A task missing its ports, or in a slot that does not exist, is dropped; the rest stays.
		PortLog l = PortLog.parse(GSON, "{\"tasks\":[{\"slot\":0,\"id\":5,\"type\":\"COURIER\",\"amount\":1},"
			+ "{\"slot\":9,\"id\":6,\"type\":\"BOUNTY\",\"need\":5},{\"slot\":1,\"id\":7,\"type\":\"BOUNTY\",\"need\":5}],"
			+ "\"xp\":{\"7\":-3,\"8\":100}}");
		assertEquals(1, l.getTasks().size());
		assertEquals(7, l.getTasks().get(0).getId());
		assertEquals(0, l.xp(7));
		assertEquals(100, l.xp(8));
	}
}
