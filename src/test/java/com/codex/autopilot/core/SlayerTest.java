package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.util.List;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The Slayer task as Autopilot reads and plans it. The facts are pinned to their sources: the OSRS
 * wiki ("Slayer master": requirements and replacements; "Slayer points": the points table, the
 * first four tasks and the 5x/15x/25x/35x/50x bonuses; each monster's page for where it lives and
 * what the fight needs) and RuneLite (gameval VarPlayerID/VarbitID/DBTableID/ItemID; the Slayer
 * plugin's master ids 7 and 10 and boss task 98).
 */
public class SlayerTest
{
	static final Gson GSON = new Gson();
	static final long MS = AutopilotCoreTest.T0 * 1000;
	static final int KURASK_ID = 41;
	static final int DURADEL_ID = 8;

	// ---- The facts ----

	@Test
	public void varsAreRuneLites()
	{
		assertEquals(394, VarPlayerID.SLAYER_COUNT);
		assertEquals(395, VarPlayerID.SLAYER_TARGET);
		assertEquals(2096, VarPlayerID.SLAYER_AREA);
		assertEquals(4258, VarPlayerID.SLAYER_COUNT_ORIGINAL);
		assertEquals(4067, VarbitID.SLAYER_MASTER);
		assertEquals(4068, VarbitID.SLAYER_POINTS);
		assertEquals(4069, VarbitID.SLAYER_TASKS_COMPLETED);
		assertEquals(5617, VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED);
		assertEquals(7, Slayer.Master.KRYSTILIA_ID);
		assertEquals(10, Slayer.Master.MORTIMER_ID);
		assertEquals(98, Slayer.BOSS_TASK);
		assertEquals(6258, Slayer.Task.KURASK.getRow());
		assertEquals(DBTableID.SlayerTask.Row.SLAYER_TARGET_GARGOYLES, Slayer.Task.GARGOYLES.getRow());
		assertEquals(DBTableID.SlayerMasterTask.Row.DURADEL_BOSS, Slayer.Master.DURADEL.getSampleRow());
		assertEquals(ItemID.SLAYER_ROCK_HAMMER, Slayer.Task.GARGOYLES.getItemId());
		assertEquals(ItemID.SLAYER_BAG_OF_SALT, Slayer.Task.ROCKSLUGS.getItemId());
	}

	@Test
	public void bringLinesNeverReadAsShortSupplies()
	{
		for (Slayer.Task t : Slayer.Task.values())
		{
			assertFalse(t.name(), Bring.isShort(t.getBring()));
			assertFalse(t.name(), t.getPlaces().isEmpty());
		}
	}

	@Test
	public void mastersAreTheWikis()
	{
		// "Slayer master": combat level, Slayer level and quest.
		assertEquals(0, Slayer.Master.TURAEL.getCombat());
		assertEquals(20, Slayer.Master.MAZCHNA.getCombat());
		assertEquals(Slayer.Quest.PRIEST_IN_PERIL, Slayer.Master.MAZCHNA.getQuest());
		assertEquals(40, Slayer.Master.VANNAKA.getCombat());
		assertEquals(70, Slayer.Master.CHAELDAR.getCombat());
		assertEquals(Slayer.Quest.LOST_CITY, Slayer.Master.CHAELDAR.getQuest());
		assertEquals(75, Slayer.Master.KONAR.getCombat());
		assertEquals(85, Slayer.Master.NIEVE.getCombat());
		assertEquals(100, Slayer.Master.DURADEL.getCombat());
		assertEquals(50, Slayer.Master.DURADEL.getSlayer());
		assertEquals(Slayer.Quest.SHILO_VILLAGE, Slayer.Master.DURADEL.getQuest());
		assertEquals(100, Slayer.Master.MORTIMER.getCombat());
		assertEquals(70, Slayer.Master.MORTIMER.getSlayer());
	}

	@Test
	public void pointsAreTheWikis()
	{
		// "Slayer points": per task, then 10th / 50th / 100th / 250th / 1,000th.
		int[] tasks = {5, 10, 50, 100, 250, 1000};
		int[][] table = {
			{0, 0, 0, 0, 0, 0},          // Turael
			{0, 0, 0, 0, 0, 0},          // Spria
			{6, 30, 90, 150, 210, 300},  // Mazchna
			{8, 40, 120, 200, 280, 400}, // Vannaka
			{10, 50, 150, 250, 350, 500}, // Chaeldar
			{18, 90, 270, 450, 630, 900}, // Konar
			{12, 60, 180, 300, 420, 600}, // Nieve
			{15, 75, 225, 375, 525, 750}, // Duradel
			{25, 125, 375, 625, 875, 1250}, // Krystilia
		};
		Slayer.Master[] masters = {Slayer.Master.TURAEL, Slayer.Master.SPRIA, Slayer.Master.MAZCHNA, Slayer.Master.VANNAKA,
			Slayer.Master.CHAELDAR, Slayer.Master.KONAR, Slayer.Master.NIEVE, Slayer.Master.DURADEL, Slayer.Master.KRYSTILIA};
		Slayer.Account plain = new Slayer.Account();
		for (int i = 0; i < masters.length; i++)
		{
			for (int j = 0; j < tasks.length; j++)
			{
				assertEquals(masters[i] + " task " + tasks[j], table[i][j], masters[i].points(tasks[j], plain));
			}
		}
		// The elite diaries: Nieve to Duradel's 15, Konar to 20.
		Slayer.Account diaries = new Slayer.Account();
		diaries.westernElite = true;
		diaries.kourendElite = true;
		assertEquals(15, Slayer.Master.NIEVE.points(11, diaries));
		assertEquals(75, Slayer.Master.NIEVE.points(10, diaries));
		assertEquals(100, Slayer.Master.KONAR.points(10, diaries));
		assertEquals(1000, Slayer.Master.KONAR.points(1000, diaries));
		// Not for the first four tasks.
		assertEquals(0, Slayer.Master.DURADEL.points(4, plain));
		assertEquals(5, Slayer.POINTS_FROM);
	}

	@Test
	public void theLadderFollowsLevelsAndQuests()
	{
		Slayer.Account a = account(104, 87);
		a.shilo = true;
		a.lostCity = true;
		a.priestInPeril = true;
		assertEquals(Slayer.Master.DURADEL, Slayer.Master.ladder(a).get(0));
		a.shilo = false;
		assertEquals(Slayer.Master.NIEVE, Slayer.Master.ladder(a).get(0));
		Slayer.Account b = account(72, 30);
		assertEquals(Slayer.Master.VANNAKA, Slayer.Master.ladder(b).get(0));
		b.lostCity = true;
		assertEquals(Slayer.Master.CHAELDAR, Slayer.Master.ladder(b).get(0));
		Slayer.Account c = account(30, 5);
		assertEquals(Slayer.Master.TURAEL, Slayer.Master.ladder(c).get(0));
		c.priestInPeril = true;
		assertEquals(Slayer.Master.MAZCHNA, Slayer.Master.ladder(c).get(0));
		// Duradel asks for 50 Slayer too.
		Slayer.Account d = account(110, 40);
		d.shilo = true;
		assertEquals(Slayer.Master.NIEVE, Slayer.Master.ladder(d).get(0));
		// Spria, Krystilia and Mortimer are never the ladder.
		Slayer.Account e = account(126, 99);
		e.shilo = true;
		assertFalse(Slayer.Master.ladder(e).contains(Slayer.Master.KRYSTILIA));
		assertFalse(Slayer.Master.ladder(e).contains(Slayer.Master.MORTIMER));
		assertFalse(Slayer.Master.ladder(e).contains(Slayer.Master.SPRIA));
		// Who stands in after a quest.
		e.wgs = true;
		e.mm2 = true;
		assertEquals("Kuradal", Slayer.Master.DURADEL.nameFor(e));
		assertEquals("Aya", Slayer.Master.TURAEL.nameFor(e));
		assertEquals("Steve", Slayer.Master.NIEVE.nameFor(e));
		assertEquals("Mazchna", Slayer.Master.MAZCHNA.nameFor(e));
		assertEquals("Duradel", Slayer.Master.DURADEL.nameFor(account(126, 99)));
	}

	// ---- Reading the game ----

	@Test
	public void killsAreTimedFromOneToTheNext()
	{
		SlayerLog log = SlayerLog.empty();
		assertTrue(log.update(kurask(150), MS));
		assertTrue(log.hasTask());
		assertEquals(150, log.getInitial());
		assertEquals(Slayer.Master.DURADEL, log.getMaster());
		// 30 kills, one every 30 seconds: the first kill only starts the clock.
		long t = MS;
		for (int left = 149; left >= 120; left--)
		{
			t += 30_000;
			log.update(kurask(left), t);
		}
		assertEquals(29, log.timed());
		assertEquals(120.0, log.killsPerHour(), 0.01);
		assertEquals(60, log.minutesLeft());
		// A bank trip of ten minutes is not counted.
		t += 600_000;
		log.update(kurask(119), t);
		assertEquals(29, log.timed());
		// Two at once (a cannon, say) over 60 seconds.
		t += 60_000;
		log.update(kurask(117), t);
		assertEquals(31, log.timed());
		assertEquals(120.0, log.killsPerHour(), 0.01);
	}

	@Test
	public void aLogoutStopsTheClock()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(kurask(100), MS);
		log.update(kurask(99), MS + 20_000);
		log.pause();
		// Killed while not watched: never counted.
		log.update(kurask(90), MS + 40_000);
		log.update(kurask(89), MS + 60_000);
		assertEquals(0, log.timed());
		log.update(kurask(88), MS + 80_000);
		assertEquals(1, log.timed());
	}

	@Test
	public void thePaceIsTrustedAfterTwentyKills()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(kurask(100), MS);
		long t = MS;
		for (int left = 99; left >= 81; left--)
		{
			t += 30_000;
			log.update(kurask(left), t);
		}
		assertEquals(18, log.timed());
		assertEquals(0, log.minutesLeft());
		t += 30_000;
		log.update(kurask(80), t);
		t += 30_000;
		log.update(kurask(79), t);
		assertEquals(20, log.timed());
		assertTrue(log.minutesLeft() > 0);
	}

	@Test
	public void aDoneTaskOffersTheNextMaster()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(kurask(3), MS);
		SlayerLog.Reading done = kurask(0);
		done.streak = 9;
		assertTrue(log.update(done, MS + 1000));
		assertFalse(log.hasTask());
		assertEquals("Kurask", log.getLastTask());
		List<Candidate> c = Slayers.candidates(log);
		assertEquals(1, c.size());
		assertEquals(Slayers.NEW, c.get(0).getKey());
		assertEquals("New task from Duradel", c.get(0).getTitle());
		assertTrue(c.get(0).getWhy(), c.get(0).getWhy().contains("The highest master your 104 combat and 87 Slayer reach"));
		assertTrue(c.get(0).getWhy(), c.get(0).getWhy().contains("your 10th in a row pays 75 points (5x); Konar pays 90"));
		assertEquals(Tier.GOAL, c.get(0).getTier());
	}

	@Test
	public void earlyTasksSayWhenPointsStart()
	{
		SlayerLog log = SlayerLog.empty();
		SlayerLog.Reading r = kurask(0);
		r.streak = 2;
		log.update(r, MS);
		assertEquals("points start after 2 more tasks", Slayers.points(log, Slayer.Master.DURADEL));
		r.streak = 3;
		log.update(r, MS);
		assertEquals("points start after 1 more task", Slayers.points(log, Slayer.Master.DURADEL));
		r.streak = 4;
		log.update(r, MS);
		assertEquals("your 5th in a row pays 15 points; Konar pays 18", Slayers.points(log, Slayer.Master.DURADEL));
	}

	@Test
	public void anAccountThatNeverSlewIsNotNagged()
	{
		SlayerLog log = SlayerLog.empty();
		SlayerLog.Reading r = new SlayerLog.Reading();
		r.account = account(90, 1);
		r.points = 0;
		r.streak = 0;
		log.update(r, MS);
		assertFalse(log.doesSlayer());
		assertTrue(Slayers.candidates(log).isEmpty());
	}

	// ---- The step ----

	@Test
	public void theTaskStepSaysWhereAndWhatToBring()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(kurask(87), MS);
		Candidate c = Slayers.candidates(log).get(0);
		assertEquals(Slayers.TASK, c.getKey());
		assertEquals(Candidate.Kind.SLAYER, c.getKind());
		assertEquals("Slayer task", c.getOver());
		assertEquals("Kurask, 87 left", c.getTitle());
		assertEquals("Fremennik Slayer Dungeon, Iorwerth Dungeon or Kurask Lair; for Duradel", c.getWhy());
		assertEquals("Leaf-bladed weapons, broad ammunition or Magic Dart: nothing else hurts them", c.getBring());
		assertEquals(Slayers.ESTIMATE_MINUTES, c.getMinutes());
		assertFalse(c.isPaced());
		assertEquals(2, c.getTargets().size());
		assertEquals(ItemID.LEAFBLADED_SWORD, c.getItemId());
	}

	@Test
	public void konarsAreaNarrowsTheJourney()
	{
		SlayerLog log = SlayerLog.empty();
		SlayerLog.Reading r = kurask(40);
		r.areaId = 3;
		r.area = "Iorwerth Dungeon";
		r.masterId = 2;
		r.master = Slayer.Master.KONAR;
		log.update(r, MS);
		Candidate c = Slayers.candidates(log).get(0);
		assertEquals("Konar sends you to Iorwerth Dungeon", c.getWhy());
		assertEquals(1, c.getTargets().size());
		assertEquals(Tile.of(3224, 6044), c.getTargets().get(0));
	}

	@Test
	public void aTaskWithoutCheckedFactsHasNoWhereOrBring()
	{
		SlayerLog log = SlayerLog.empty();
		SlayerLog.Reading r = kurask(55);
		r.row = DBTableID.SlayerTask.Row.SLAYER_TARGET_BLOODVELD;
		r.taskId = 12;
		r.name = "Bloodveld";
		log.update(r, MS);
		Candidate c = Slayers.candidates(log).get(0);
		assertEquals("Bloodveld, 55 left", c.getTitle());
		assertEquals("for Duradel", c.getWhy());
		assertNull(c.getBring());
		assertTrue(c.getTargets().isEmpty());
		assertEquals(ItemID.SLAYER_GEM, c.getItemId());
	}

	@Test
	public void theOwnPaceTimesTheStep()
	{
		SlayerLog log = SlayerLog.empty();
		log.learn("kurask", 100, 3000);
		log.update(kurask(60), MS);
		Candidate c = Slayers.candidates(log).get(0);
		assertTrue(c.isPaced());
		assertEquals(30, c.getMinutes());
		assertTrue(c.getWhy(), c.getWhy().contains("at your 120 kills/hr"));
	}

	// ---- Saving ----

	@Test
	public void itKeepsAcrossASave()
	{
		SlayerLog log = SlayerLog.empty();
		log.learn("kurask", 50, 1500);
		SlayerLog.Reading r = kurask(87);
		r.points = 345;
		r.streak = 9;
		log.update(r, MS);
		SlayerLog back = SlayerLog.parse(GSON, log.toJson(GSON));
		assertEquals(87, back.getAmount());
		assertEquals(150, back.getInitial());
		assertEquals("Kurask", back.getName());
		assertEquals(Slayer.Task.KURASK.getRow(), back.getRow());
		assertEquals(Slayer.Master.DURADEL, back.getMaster());
		assertEquals(345, back.getPoints());
		assertEquals(9, back.getStreak());
		assertEquals(104, back.getAccount().combat);
		assertTrue(back.getAccount().shilo);
		assertEquals(120.0, back.killsPerHour(), 0.01);
		// The master id learned is kept for when the game's tables are not loaded.
		SlayerLog.Reading bare = kurask(86);
		bare.master = null;
		bare.masters = null;
		bare.row = -1;
		bare.name = null;
		back.update(bare, MS + 5000);
		assertEquals(Slayer.Master.DURADEL, back.getMaster());
		assertEquals("Kurask", back.getName());
		assertEquals(Slayer.Task.KURASK.getRow(), back.getRow());
	}

	@Test
	public void aDamagedSaveIsAFreshStart()
	{
		assertFalse(SlayerLog.parse(GSON, "{nope").doesSlayer());
		assertFalse(SlayerLog.parse(GSON, null).doesSlayer());
		assertFalse(SlayerLog.parse(GSON, "{'version':2,'amount':40,'streak':3}").doesSlayer());
		SlayerLog odd = SlayerLog.parse(GSON, "{'version':1,'amount':-5,'master':'NOBODY','pace':{'x':[1]}}");
		assertFalse(odd.hasTask());
		assertNull(odd.getMaster());
	}

	@Test
	public void krystiliaIsKnownWithoutTheTables()
	{
		SlayerLog log = SlayerLog.empty();
		SlayerLog.Reading r = kurask(20);
		r.master = null;
		r.masters = null;
		r.masterId = Slayer.Master.KRYSTILIA_ID;
		r.wildStreak = 4;
		log.update(r, MS);
		assertEquals(Slayer.Master.KRYSTILIA, log.getMaster());
		assertEquals(4, log.streakFor(Slayer.Master.KRYSTILIA));
		assertNotNull(Slayers.candidates(log).get(0));
	}

	static Slayer.Account account(int combat, int slayer)
	{
		Slayer.Account a = new Slayer.Account();
		a.combat = combat;
		a.slayer = slayer;
		return a;
	}

	static SlayerLog.Reading kurask(int left)
	{
		SlayerLog.Reading r = new SlayerLog.Reading();
		r.taskId = KURASK_ID;
		r.row = Slayer.Task.KURASK.getRow();
		r.name = "Kurask";
		r.amount = left;
		r.initial = 150;
		r.masterId = DURADEL_ID;
		r.master = Slayer.Master.DURADEL;
		r.masters = java.util.Collections.singletonMap(DURADEL_ID, Slayer.Master.DURADEL);
		Slayer.Account a = account(104, 87);
		a.shilo = true;
		a.lostCity = true;
		a.priestInPeril = true;
		r.account = a;
		return r;
	}
}
