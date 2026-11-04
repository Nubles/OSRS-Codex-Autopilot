package com.codex.autopilot.core;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The streak, its milestones and the "Turael skipping" plan. Facts pinned to the OSRS wiki:
 * "Slayer points" ("bonus points are awarded after every 10th, 50th, 100th, 250th, and 1,000th
 * completed task"; 5x/15x/25x/35x/50x; "Tasks assigned by Turael/Spria give no points but do
 * contribute to the four needed to start gaining points"; Konar 18 (20), Duradel 15, Nieve 12 (15))
 * and "Turael" ("completing every 50th task with a Slayer master that gives a lot of points per
 * task", "using Turael for the rest of the tasks"; "After a task change is processed, your task
 * streak will also be reset to zero!").
 */
public class SkipPlanTest
{
	static final long MS = SlayerTest.MS;
	static final int TURAEL_ID = 1;
	static final int KONAR_ID = 9;

	@Test
	public void milestonesAreTheWikis()
	{
		assertEquals(10, Slayer.nextMilestone(0, 5));
		assertEquals(40, Slayer.nextMilestone(37, 5));
		assertEquals(50, Slayer.nextMilestone(37, 15));
		assertEquals(100, Slayer.nextMilestone(50, 25));
		assertEquals(250, Slayer.nextMilestone(101, 35));
		assertEquals(1000, Slayer.nextMilestone(250, 50));
		// The streak itself on a milestone: the next one is the one after.
		assertEquals(60, Slayer.nextMilestone(50, 5));
	}

	@Test
	public void theMilestoneMasterIsTheChosenOneWhenOpen()
	{
		Slayer.Account a = full();
		SkipPlan konar = new SkipPlan(true, SkipPlan.Choice.KONAR, SkipPlan.Every.TENTH);
		assertEquals(Slayer.Master.KONAR, konar.milestoneMaster(a, 50));
		SkipPlan duradel = new SkipPlan(true, SkipPlan.Choice.DURADEL, SkipPlan.Every.TENTH);
		assertEquals(Slayer.Master.DURADEL, duradel.milestoneMaster(a, 50));
		// Konar pays the most of the usual masters (18 a task, 270 on a 50th).
		SkipPlan best = new SkipPlan(true, SkipPlan.Choice.BEST, SkipPlan.Every.TENTH);
		assertEquals(Slayer.Master.KONAR, best.milestoneMaster(a, 50));
		// Duradel asks for Shilo Village: without it the plan falls back to the best open.
		a.shilo = false;
		assertEquals(Slayer.Master.KONAR, duradel.milestoneMaster(a, 50));
		// Under Konar's 75 combat, Chaeldar with Lost City.
		Slayer.Account low = SlayerTest.account(72, 40);
		low.lostCity = true;
		assertEquals(Slayer.Master.CHAELDAR, best.milestoneMaster(low, 10));
	}

	@Test
	public void betweenMilestonesTheQuickTaskIsTuraels()
	{
		SlayerLog log = between(37);
		log.withPlan(new SkipPlan(true, SkipPlan.Choice.KONAR, SkipPlan.Every.TENTH));
		SkipPlan.Advice a = log.getPlan().advise(log);
		assertEquals(Slayer.Master.TURAEL, a.master);
		assertEquals(38, a.task);
		assertFalse(a.milestone);
		assertEquals("A quick task from Turael or Spria: no points, but the task still counts; 1 more after it, then"
			+ " the 40th from Konar pays 90", a.text);
		Candidate c = Slayers.candidates(log).get(0);
		assertEquals("Quick task from Turael or Spria", c.getTitle());
		// After While Guthix Sleeps, Aya.
		Slayer.Account wgs = full();
		wgs.wgs = true;
		assertEquals("Aya or Spria", SkipPlan.quick(wgs));
	}

	@Test
	public void aMilestoneGoesToTheHighPointsMaster()
	{
		SlayerLog log = between(49);
		log.withPlan(new SkipPlan(true, SkipPlan.Choice.KONAR, SkipPlan.Every.TENTH));
		SkipPlan.Advice a = log.getPlan().advise(log);
		assertEquals(Slayer.Master.KONAR, a.master);
		assertTrue(a.milestone);
		assertEquals("Your 50th task in a row is a milestone: take it from Konar for 270 points (15x)", a.text);
		assertEquals("Milestone task from Konar", Slayers.candidates(log).get(0).getTitle());
	}

	@Test
	public void everyFiftiethSkipsTheTens()
	{
		SlayerLog log = between(39);
		SkipPlan fifty = new SkipPlan(true, SkipPlan.Choice.KONAR, SkipPlan.Every.FIFTIETH);
		log.withPlan(fifty);
		SkipPlan.Advice a = fifty.advise(log);
		assertEquals(Slayer.Master.TURAEL, a.master);
		assertTrue(a.text, a.text.endsWith("9 more after it, then the 50th from Konar pays 270"));
		assertFalse(fifty.isMilestone(40));
		assertTrue(fifty.isMilestone(50));
		assertTrue(fifty.isMilestone(100));
	}

	@Test
	public void theFirstFourGoToTuraelToo()
	{
		SlayerLog log = between(2);
		SkipPlan plan = new SkipPlan(true, SkipPlan.Choice.KONAR, SkipPlan.Every.TENTH);
		SkipPlan.Advice a = plan.advise(log);
		assertEquals(Slayer.Master.TURAEL, a.master);
		assertTrue(a.text, a.text.contains("no master pays for the first four"));
		// The 5th is not a milestone; the 10th is.
		assertFalse(plan.isMilestone(5));
		assertEquals(10, plan.nextMilestone(2));
	}

	@Test
	public void aHeldTaskIsNeverChangedAtTurael()
	{
		SlayerLog log = SlayerLog.empty();
		SlayerLog.Reading r = konar(120, 37, 500);
		log.update(r, MS);
		SkipPlan plan = new SkipPlan(true, SkipPlan.Choice.KONAR, SkipPlan.Every.TENTH);
		SkipPlan.Advice a = plan.advise(log);
		assertNull(a.master);
		assertEquals("Finish this task: a task change at Turael would reset your 37-task streak to zero", a.text);
		// A Turael task in hand: what comes after it.
		SlayerLog t = SlayerLog.empty();
		t.update(turael(15, 38, 500), MS);
		assertEquals("Then 40th in a row from Konar for 90 points (5x)", plan.advise(t).text);
	}

	@Test
	public void offMeansNoPlan()
	{
		SlayerLog log = between(37);
		assertFalse(log.getPlan().isOn());
		assertNull(log.getPlan().advise(log));
		assertEquals("New task from Duradel", Slayers.candidates(log).get(0).getTitle());
	}

	@Test
	public void paysListsEveryMasterOpenTheMostFirst()
	{
		List<SkipPlan.Pays> pays = SkipPlan.pays(between(49));
		assertEquals(Slayer.Master.KONAR, pays.get(0).master);
		assertEquals(270, pays.get(0).points);
		assertEquals(Slayer.Master.DURADEL, pays.get(1).master);
		assertEquals(225, pays.get(1).points);
		assertEquals(Slayer.Master.TURAEL, pays.get(pays.size() - 1).master);
		assertEquals(0, pays.get(pays.size() - 1).points);
	}

	// ---- The player's own tasks ----

	@Test
	public void finishedTasksAreTimedAndPaid()
	{
		SlayerLog log = SlayerLog.empty();
		long t = MS;
		// A Konar task over 30 minutes, killed every minute; it pays 18 for the 37th.
		log.update(konar(30, 36, 500), t);
		for (int left = 29; left >= 1; left--)
		{
			t += 60_000;
			log.update(konar(left, 36, 500), t);
		}
		t += 60_000;
		// Done: the count, the streak and the points move on in one tick.
		log.update(none(Slayer.Master.KONAR, KONAR_ID, 37, 518), t);
		List<SlayerLog.Done> done = log.getDone();
		assertEquals(1, done.size());
		assertEquals(Slayer.Master.KONAR, done.get(0).master);
		assertEquals(18, done.get(0).points);
		assertEquals(1800, done.get(0).seconds);
	}

	@Test
	public void pointsArriveAfterTheCount()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(konar(2, 39, 500), MS);
		log.update(konar(1, 39, 500), MS + 300_000);
		// The count first, then the streak, then the points on the next tick.
		log.update(none(Slayer.Master.KONAR, KONAR_ID, 39, 500), MS + 360_000);
		log.update(none(Slayer.Master.KONAR, KONAR_ID, 40, 500), MS + 360_600);
		assertTrue(log.getDone().isEmpty());
		log.update(none(Slayer.Master.KONAR, KONAR_ID, 40, 590), MS + 361_200);
		assertEquals(1, log.getDone().size());
		assertEquals(90, log.getDone().get(0).points);
	}

	@Test
	public void aTuraelTaskIsSettledAtOnceWithNoPoints()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(turael(15, 38, 500), MS);
		log.update(turael(1, 38, 500), MS + 300_000);
		log.update(none(Slayer.Master.TURAEL, TURAEL_ID, 39, 500), MS + 330_000);
		assertEquals(1, log.getDone().size());
		assertEquals(0, log.getDone().get(0).points);
		assertEquals(330, log.getDone().get(0).seconds);
	}

	@Test
	public void aCancelledTaskIsNotRecorded()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(konar(100, 20, 500), MS);
		log.update(konar(90, 20, 500), MS + 300_000);
		// Cancelled for points: the count goes, the streak stays, points drop.
		log.update(none(Slayer.Master.KONAR, KONAR_ID, 20, 470), MS + 360_000);
		log.update(konar(80, 20, 470), MS + 420_000);
		assertTrue(log.getDone().isEmpty());
	}

	@Test
	public void aBreakIsNotTimed()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(turael(15, 38, 500), MS);
		log.update(turael(10, 38, 500), MS + 120_000);
		// An hour away with the task in hand.
		log.update(turael(5, 38, 500), MS + 3_720_000);
		log.update(none(Slayer.Master.TURAEL, TURAEL_ID, 39, 500), MS + 3_840_000);
		assertEquals(240, log.getDone().get(0).seconds);
	}

	@Test
	public void ownPointsAnHourAndTheComparison()
	{
		SlayerLog log = SlayerLog.empty();
		long t = MS;
		int streak = 30;
		int points = 500;
		// Two Konar tasks of 40 minutes, three Turael tasks of 5.
		for (int i = 0; i < 2; i++)
		{
			log.update(konar(50, streak, points), t);
			for (int left = 40; left >= 10; left -= 10)
			{
				t += 10 * 60_000;
				log.update(konar(left, streak, points), t);
			}
			streak++;
			points += Slayer.Master.KONAR.points(streak, full());
			log.update(none(Slayer.Master.KONAR, KONAR_ID, streak, points), t);
			t += 60_000;
		}
		assertEquals(0, log.pointsPerHour(), 0.0);
		for (int i = 0; i < 3; i++)
		{
			log.update(turael(15, streak, points), t);
			t += 5 * 60_000;
			log.update(turael(1, streak, points), t);
			streak++;
			log.update(none(Slayer.Master.TURAEL, TURAEL_ID, streak, points), t);
			t += 60_000;
		}
		assertEquals(35, log.getStreak());
		assertEquals(5, log.timedTasks());
		// 36 points (the 31st and 32nd) in 95 minutes.
		assertEquals(36 * 60.0 / 95, log.pointsPerHour(), 0.01);
		assertEquals(40.0, log.minutesPerTask(Slayer.Master.KONAR), 0.01);
		assertEquals(5.0, log.minutesPerQuickTask(), 0.01);
		SkipPlan plan = new SkipPlan(true, SkipPlan.Choice.KONAR, SkipPlan.Every.TENTH);
		SkipPlan.Compare c = plan.compare(log);
		assertNotNull(c);
		// Tasks 36 to 40: all Konar 18 x 4 + 90 = 162 in 200 min; skipping 90 in 4 x 5 + 40 = 60 min.
		assertEquals(5, c.tasks);
		assertEquals(162, c.fullPoints);
		assertEquals(90, c.skipPoints);
		assertEquals(200 / 60.0, c.fullHours, 0.001);
		assertEquals(1.0, c.skipHours, 0.001);
		assertTrue(c.skipPerHour() > c.fullPerHour());
	}

	@Test
	public void finishedTasksAreSavedAndRead()
	{
		SlayerLog log = SlayerLog.empty();
		log.update(turael(15, 38, 500), MS);
		log.update(turael(1, 38, 500), MS + 300_000);
		log.update(none(Slayer.Master.TURAEL, TURAEL_ID, 39, 500), MS + 330_000);
		log.update(konar(40, 39, 500), MS + 400_000);
		log.update(konar(39, 39, 500), MS + 460_000);
		String json = log.toJson(SlayerTest.GSON);
		SlayerLog back = SlayerLog.parse(SlayerTest.GSON, json);
		assertEquals(1, back.getDone().size());
		assertEquals(Slayer.Master.TURAEL, back.getDone().get(0).master);
		assertEquals(330, back.getDone().get(0).seconds);
		assertEquals(1, back.taskMinutes());
		// A save from before this change still reads.
		SlayerLog old = SlayerLog.parse(SlayerTest.GSON, "{'version':1,'taskId':41,'amount':150,'streak':8,'points':330}");
		assertTrue(old.getDone().isEmpty());
		assertEquals(8, old.getStreak());
		// Bad rows are skipped.
		SlayerLog bad = SlayerLog.parse(SlayerTest.GSON, "{'version':1,'streak':8,'done':[['KONAR',-5,60,0],['NOPE',10,60,0],'x']}");
		assertEquals(1, bad.getDone().size());
		assertNull(bad.getDone().get(0).master);
	}

	// ---- Helpers ----

	static Slayer.Account full()
	{
		Slayer.Account a = SlayerTest.account(126, 99);
		a.shilo = true;
		a.lostCity = true;
		a.priestInPeril = true;
		return a;
	}

	static SlayerLog between(int streak)
	{
		SlayerLog log = SlayerLog.empty();
		log.update(none(Slayer.Master.KONAR, KONAR_ID, streak, 500), MS);
		return log;
	}

	static SlayerLog.Reading konar(int left, int streak, int points)
	{
		SlayerLog.Reading r = task(left, streak, points);
		r.taskId = 41;
		r.name = "Kurask";
		r.masterId = KONAR_ID;
		r.master = Slayer.Master.KONAR;
		return r;
	}

	static SlayerLog.Reading turael(int left, int streak, int points)
	{
		SlayerLog.Reading r = task(left, streak, points);
		r.taskId = 2;
		r.name = "Birds";
		r.masterId = TURAEL_ID;
		r.master = Slayer.Master.TURAEL;
		return r;
	}

	private static SlayerLog.Reading task(int left, int streak, int points)
	{
		SlayerLog.Reading r = new SlayerLog.Reading();
		r.amount = left;
		r.initial = Math.max(left, 15);
		r.streak = streak;
		r.points = points;
		r.account = full();
		return r;
	}

	static SlayerLog.Reading none(Slayer.Master m, int id, int streak, int points)
	{
		SlayerLog.Reading r = new SlayerLog.Reading();
		r.masterId = id;
		r.master = m;
		r.streak = streak;
		r.points = points;
		r.account = full();
		return r;
	}
}
