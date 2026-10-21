package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The learned pace, the heads-up notices and the time budget. */
public class V2PlanningTest
{
	static final long T0 = AutopilotCoreTest.T0;
	static final Gson GSON = new Gson();

	static Candidate chore(String key, Candidate.Kind kind, Tier tier, int count, int minutes, double urgency)
	{
		return new Candidate(key, kind, tier, kind.getLabel(), count + " ready", "why", null, minutes, urgency, 0, count, -1);
	}

	static Candidate houses(int count)
	{
		return chore("chores:birdhouse_run", Candidate.Kind.BIRDHOUSE_RUN, Tier.READY, count, 6, 4.8);
	}

	static List<Candidate> list(Candidate... cs)
	{
		return new ArrayList<>(Arrays.asList(cs));
	}

	static Set<String> keys(Candidate... cs)
	{
		Set<String> out = new HashSet<>();
		for (Candidate c : cs)
		{
			out.add(c.getKey());
		}
		return out;
	}

	// ---- Pace ----

	@Test
	public void paceTrustedAfterThreeRunsAsMedian()
	{
		Pace p = Pace.empty();
		Candidate c = chore("chores:herb_run", Candidate.Kind.HERB_RUN, Tier.READY, 6, 18, 5);
		assertTrue(p.add(Candidate.Kind.HERB_RUN, 2.0));
		assertTrue(p.add(Candidate.Kind.HERB_RUN, 1.5));
		assertSame(c, p.apply(c));
		assertFalse(c.isPaced());
		assertTrue(p.add(Candidate.Kind.HERB_RUN, 9.0));
		assertEquals(2.0, p.perStop(Candidate.Kind.HERB_RUN), 1e-9);
		Candidate paced = p.apply(c);
		assertTrue(paced.isPaced());
		assertEquals(12, paced.getMinutes());
		assertEquals(c.getKey(), paced.getKey());
		assertEquals(c.getWhy(), paced.getWhy());
		// Another kind is untouched, and goals are never paced.
		Candidate trees = chore("chores:tree_run", Candidate.Kind.TREE_RUN, Tier.READY, 2, 6, 1);
		assertSame(trees, p.apply(trees));
	}

	@Test
	public void paceKeepsTheLastSevenAndRefusesNonsense()
	{
		Pace p = Pace.empty();
		for (int i = 0; i < 10; i++)
		{
			p.add(Candidate.Kind.BIRDHOUSE_RUN, 1 + i);
		}
		assertEquals(Pace.KEEP, p.samples(Candidate.Kind.BIRDHOUSE_RUN));
		// 4..10 kept: median 7.
		assertEquals(7.0, p.perStop(Candidate.Kind.BIRDHOUSE_RUN), 1e-9);
		assertFalse(p.add(Candidate.Kind.BIRDHOUSE_RUN, 0.01));
		assertFalse(p.add(Candidate.Kind.BIRDHOUSE_RUN, 90));
		assertFalse(p.add(Candidate.Kind.BIRDHOUSE_RUN, Double.NaN));
		assertFalse(p.add(Candidate.Kind.FOCUS_GOAL, 2));
		assertFalse(p.add(null, 2));
	}

	@Test
	public void paceRoundTripsThroughJson()
	{
		Pace p = Pace.empty();
		p.add(Candidate.Kind.HERB_RUN, 1.234);
		p.add(Candidate.Kind.HERB_RUN, 2.5);
		p.add(Candidate.Kind.HERB_RUN, 3);
		p.add(Candidate.Kind.HOPS_RUN, 4);
		String json = p.toJson(GSON);
		Pace back = Pace.parse(GSON, json);
		assertEquals(3, back.samples(Candidate.Kind.HERB_RUN));
		assertEquals(1, back.samples(Candidate.Kind.HOPS_RUN));
		assertEquals(2.5, back.perStop(Candidate.Kind.HERB_RUN), 1e-9);
		assertEquals(json, back.toJson(GSON));
	}

	@Test
	public void malformedSavedPaceReadsAsNothingLearned()
	{
		for (String bad : new String[]{null, "", "   ", "not json", "[1,2]", "{}", "42", "\"text\"", "{\"version\":2,\"kinds\":{\"HERB_RUN\":[1,2,3]}}",
			"{\"version\":\"1\",\"kinds\":{\"HERB_RUN\":[1,2,3]}}", "{\"version\":1}", "{\"version\":1,\"kinds\":[1,2]}",
			"{\"version\":1,\"kinds\":{\"HERB_RUN\":\"fast\"}}", "{\"version\":1,\"kinds\":{\"HERB_RUN\":[1,2,", "{\"version\":1,\"kinds\":null}"})
		{
			Pace p = Pace.parse(GSON, bad);
			assertTrue(String.valueOf(bad), p.isEmpty());
		}
		assertTrue(Pace.parse(null, "{\"version\":1,\"kinds\":{\"HERB_RUN\":[1,2,3]}}").isEmpty());
		// Bad entries are dropped, good ones beside them kept.
		Pace mixed = Pace.parse(GSON, "{\"version\":1,\"kinds\":{\"HERB_RUN\":[1.5,\"x\",null,-4,1e9,{},2,2.5],"
			+ "\"NOT_A_KIND\":[1,2,3],\"KINGDOM\":[1,2,3],\"TREE_RUN\":{}}}");
		assertEquals(3, mixed.samples(Candidate.Kind.HERB_RUN));
		assertEquals(2.0, mixed.perStop(Candidate.Kind.HERB_RUN), 1e-9);
		assertEquals(0, mixed.samples(Candidate.Kind.KINGDOM));
		assertEquals(0, mixed.samples(Candidate.Kind.TREE_RUN));
	}

	// ---- PaceWatch ----

	@Test
	public void watchTimesARunFromFirstStopToLast()
	{
		PaceWatch w = new PaceWatch();
		long t = T0;
		// Four houses ready and leading, the player walks over for a while: nothing done yet.
		assertTrue(w.observe(list(houses(4)), "chores:birdhouse_run", t).isEmpty());
		assertTrue(w.observe(list(houses(4)), "chores:birdhouse_run", t += 60).isEmpty());
		// First house collected: the clock starts here.
		long first = t += 60;
		assertTrue(w.observe(list(houses(3)), "chores:birdhouse_run", first).isEmpty());
		// Rebuilt (back to 4) then seeded, and so on.
		assertTrue(w.observe(list(houses(4)), "chores:birdhouse_run", t += 30).isEmpty());
		assertTrue(w.observe(list(houses(2)), "chores:birdhouse_run", t += 60).isEmpty());
		assertTrue(w.observe(list(houses(1)), "chores:birdhouse_run", t += 60).isEmpty());
		List<PaceWatch.Sample> done = w.observe(list(), null, t += 60);
		assertEquals(1, done.size());
		assertEquals(Candidate.Kind.BIRDHOUSE_RUN, done.get(0).kind);
		// 210 s over the three stops after the first.
		assertEquals((t - first) / 60.0 / 3, done.get(0).minutesPerStop, 1e-9);
		assertEquals(0, w.tracking());
	}

	@Test
	public void watchIgnoresRunsNotLeadingOrLostFromSight()
	{
		Candidate dailies = new Candidate("dailies", Candidate.Kind.DAILIES, Tier.WASTING, "Diary dailies", "x", "y", null, 6, 3, T0 + 3600, 2, -1);
		PaceWatch w = new PaceWatch();
		long t = T0;
		// Something else leads the whole time: not learned.
		w.observe(list(dailies, houses(4)), "dailies", t);
		w.observe(list(dailies, houses(3)), "dailies", t += 60);
		assertTrue(w.observe(list(dailies), "dailies", t += 120).isEmpty());

		// Leading, but a gap longer than GAP (logged out) drops the run.
		w.observe(list(houses(4)), "chores:birdhouse_run", t += 10);
		w.observe(list(houses(3)), "chores:birdhouse_run", t += 60);
		assertTrue(w.observe(list(), null, t += PaceWatch.GAP + 1).isEmpty());

		// Abandoned by the caller (a setting changed): dropped.
		w.observe(list(houses(4)), "chores:birdhouse_run", t += 10);
		w.observe(list(houses(3)), "chores:birdhouse_run", t += 60);
		w.abandon();
		assertTrue(w.observe(list(), null, t += 10).isEmpty());

		// Gone with no stop seen done first (snapped out of view, one-stop run): not learned.
		w.observe(list(houses(1)), "chores:birdhouse_run", t += 10);
		assertTrue(w.observe(list(), null, t += 60).isEmpty());

		// Done far too fast to be real (a config edit, a misread): refused.
		w.observe(list(houses(4)), "chores:birdhouse_run", t += 10);
		w.observe(list(houses(3)), "chores:birdhouse_run", t += 1);
		assertTrue(w.observe(list(), null, t += 1).isEmpty());

		// Non-chores are never timed.
		w.observe(list(dailies), "dailies", t += 10);
		w.observe(list(), null, t += 10);
		assertEquals(0, w.tracking());
	}

	@Test
	public void watchCountsARunThatTookTheLeadOnceStarted()
	{
		Candidate goal = new Candidate("goal:x", Candidate.Kind.FOCUS_GOAL, Tier.FOCUS, "Toward X", "x", "y", null, 30, 0.1, 0, 1, -1);
		PaceWatch w = new PaceWatch();
		long t = T0;
		w.observe(list(goal, houses(4)), "goal:x", t);
		w.observe(list(goal, houses(3)), "goal:x", t += 60);
		// Skipped the goal: the houses lead while being worked.
		w.observe(list(goal, houses(2)), "chores:birdhouse_run", t += 60);
		assertEquals(1, w.observe(list(goal), "goal:x", t += 60).size());
	}

	// ---- Heads ----

	@Test
	public void headsPrimeQuietlyThenTellOncePerChange()
	{
		Heads h = new Heads();
		Candidate ready = houses(4);
		Candidate herbs = chore("chores:herb_run", Candidate.Kind.HERB_RUN, Tier.READY, 7, 20, 5);
		Candidate herbsSick = new Candidate("chores:herb_run", Candidate.Kind.HERB_RUN, Tier.WASTING, "Herb run", "7 ready, 2 diseased",
			"why", null, 20, 6, T0 + 900, 9, -1);
		long t = T0;
		// The first look after login only takes note.
		assertTrue(h.observe(list(ready), keys(ready), t, true, true).isEmpty());
		assertTrue(h.observe(list(ready), keys(ready), t += 3, true, true).isEmpty());
		// The herbs come ready: told once.
		List<Heads.Notice> n = h.observe(list(ready, herbs), keys(ready, herbs), t += 3, true, true);
		assertEquals(1, n.size());
		assertEquals("Ready now: Herb run (7 ready)", n.get(0).text);
		assertTrue(h.observe(list(ready, herbs), keys(ready, herbs), t += 3, true, true).isEmpty());
		// Then a patch falls diseased: going to waste is news.
		n = h.observe(list(ready, herbsSick), keys(ready, herbsSick), t += 3, true, true);
		assertEquals(1, n.size());
		assertEquals(Tier.WASTING, n.get(0).tier);
		assertEquals("Going to waste: Herb run: 7 ready, 2 diseased", n.get(0).text);
		assertTrue(h.observe(list(ready, herbsSick), keys(ready, herbsSick), t += 3, true, true).isEmpty());
		// Cured, still ready: nothing. Diseased again later: told again.
		assertTrue(h.observe(list(ready, herbs), keys(ready, herbs), t += 3, true, true).isEmpty());
		assertEquals(1, h.observe(list(ready, herbsSick), keys(ready, herbsSick), t += 3, true, true).size());
	}

	@Test
	public void headsDoNotRepeatAFlickerButDoAfterARealGap()
	{
		Heads h = new Heads();
		Candidate ready = houses(4);
		long t = T0;
		h.observe(list(), keys(), t, true, true);
		assertEquals("Bird houses are full (4 ready)", h.observe(list(ready), keys(ready), t += 3, true, true).get(0).text);
		// Gone for a reading or two (a logout, a misread): back without a word.
		h.observe(list(), keys(), t += 3, true, true);
		assertTrue(h.observe(list(ready), keys(ready), t += 60, true, true).isEmpty());
		// Collected and gone for good, full again fifty minutes later: told.
		h.observe(list(), keys(), t += 3, true, true);
		h.observe(list(), keys(), t += Heads.REARM, true, true);
		assertEquals(1, h.observe(list(ready), keys(ready), t += 50 * 60, true, true).size());
	}

	@Test
	public void headsRespectTogglesHiddenStepsAndResets()
	{
		Heads h = new Heads();
		Candidate ready = houses(4);
		Candidate waste = new Candidate("kingdom", Candidate.Kind.KINGDOM, Tier.WASTING, "Miscellania", "x", "y", null, 6, 1, T0 + 600, 1, -1);
		Candidate goal = new Candidate("goal:x", Candidate.Kind.FOCUS_GOAL, Tier.FOCUS, "Toward X", "x", "y", null, 30, 0.1, 0, 1, -1);
		Candidate dailies = new Candidate("dailies", Candidate.Kind.DAILIES, Tier.READY, "Diary dailies", "x", "y", null, 6, 1, 0, 2, -1);
		long t = T0;
		h.observe(list(), keys(), t, true, true);
		// Ready notices off, a goal and a ready daily are never chore news: only the waste is told.
		List<Heads.Notice> n = h.observe(list(ready, waste, goal, dailies), keys(ready, waste, goal, dailies), t += 3, true, false);
		assertEquals(1, n.size());
		assertEquals("kingdom", n.get(0).key);
		// What was seen with its toggle off is not told when it is switched back on.
		assertTrue(h.observe(list(ready, waste), keys(ready, waste), t += 3, true, true).isEmpty());

		// Snoozed or held back by quiet mode: noted, not told.
		Heads h2 = new Heads();
		h2.observe(list(), keys(), t, true, true);
		assertTrue(h2.observe(list(ready), Collections.emptySet(), t += 3, true, true).isEmpty());
		assertTrue(h2.observe(list(ready), keys(ready), t += 3, true, true).isEmpty());

		// After a reset (login, another account) the next look is quiet again.
		h2.reset();
		Candidate herbs = chore("chores:herb_run", Candidate.Kind.HERB_RUN, Tier.READY, 7, 20, 5);
		assertTrue(h2.observe(list(ready, herbs), keys(ready, herbs), t += 3, true, true).isEmpty());
		assertTrue(h2.observe(null, null, t += 3, true, true).isEmpty());
	}

	// ---- Budget ----

	static Plan plan(Candidate... cs)
	{
		return Ranker.rank(Arrays.asList(cs), Settings.defaults(), new Session(), T0);
	}

	@Test
	public void budgetTakesWasteFirstThenValuePerMinute()
	{
		Candidate waste = new Candidate("dailies", Candidate.Kind.DAILIES, Tier.WASTING, "Diary dailies", "3 to collect", "y", null, 9, 3, T0 + 3600, 3, -1);
		Candidate birds = chore("chores:birdhouse_run", Candidate.Kind.BIRDHOUSE_RUN, Tier.READY, 4, 6, 4.8);
		Candidate herbs = chore("chores:herb_run", Candidate.Kind.HERB_RUN, Tier.READY, 9, 20, 7);
		Candidate trees = chore("chores:tree_run", Candidate.Kind.TREE_RUN, Tier.READY, 2, 6, 0.1);
		Candidate goal = new Candidate("goal:x", Candidate.Kind.FOCUS_GOAL, Tier.FOCUS, "Toward X", "x", "y", null, 15, 0.1, 0, 1, -1);
		Plan p = plan(goal, trees, herbs, birds, waste);

		Budget.Fit f15 = Budget.fit(p, 15, null);
		assertEquals(Arrays.asList(waste, birds), f15.getSteps());
		assertEquals(15, f15.getTotal());
		assertEquals(3, f15.getLeft());
		assertEquals(6, f15.getShortest());

		// Thirty: the herb run does not fit after waste and houses, the trees do.
		Budget.Fit f30 = Budget.fit(p, 30, null);
		assertEquals(Arrays.asList(waste, birds, trees), f30.getSteps());
		assertEquals(21, f30.getTotal());

		Budget.Fit f60 = Budget.fit(p, 60, null);
		assertEquals(Arrays.asList(waste, birds, herbs, trees, goal), f60.getSteps());
		assertEquals(56, f60.getTotal());
		assertEquals(0, f60.getLeft());
		assertSame(waste, f60.first());

		Budget.Fit all = Budget.fit(p, 0, null);
		assertEquals(p.getSteps(), all.getSteps());
	}

	@Test
	public void budgetPutsSkippedLastAndCanFitNothing()
	{
		Candidate birds = chore("chores:birdhouse_run", Candidate.Kind.BIRDHOUSE_RUN, Tier.READY, 4, 6, 4.8);
		Candidate trees = chore("chores:tree_run", Candidate.Kind.TREE_RUN, Tier.READY, 2, 6, 0.1);
		Session s = new Session();
		s.skip(birds.getKey());
		Plan p = Ranker.rank(Arrays.asList(birds, trees), Settings.defaults(), s, T0);
		Budget.Fit f = Budget.fit(p, 15, s);
		assertEquals(Arrays.asList(trees, birds), f.getSteps());

		Candidate herbs = chore("chores:herb_run", Candidate.Kind.HERB_RUN, Tier.READY, 9, 20, 7);
		Budget.Fit none = Budget.fit(plan(herbs), 15, null);
		assertTrue(none.isEmpty());
		assertNull(none.first());
		assertEquals(20, none.getShortest());
		assertTrue(Budget.fit(Plan.EMPTY, 30, null).isEmpty());
		assertTrue(Budget.fit(null, 30, null).isEmpty());
	}

	@Test
	public void budgetLabels()
	{
		assertEquals("30 min", Budget.MIN_30.toString());
		assertEquals("No limit", Budget.NONE.toString());
		assertEquals(0, Budget.NONE.getMinutes());
		assertEquals("Any", Budget.NONE.getShort());
		assertEquals("15", Budget.MIN_15.getShort());
	}

	// ---- Learned pace in the plan ----

	@Test
	public void advisorAppliesTheLearnedPaceToChoresOnly()
	{
		AutopilotCoreTest.MapProfile p = AutopilotCoreTest.fullHouses(new AutopilotCoreTest.MapProfile());
		Advisor.Inputs in = Advisor.Inputs.read(p, GSON, T0, false);
		Candidate before = Advisor.candidates(in, Settings.defaults(), T0).get(0);
		assertFalse(before.isPaced());
		in.pace = Pace.parse(GSON, "{\"version\":1,\"kinds\":{\"BIRDHOUSE_RUN\":[1.5,1.5,2.5]}}");
		Candidate after = Advisor.candidates(in, Settings.defaults(), T0).get(0);
		assertTrue(after.isPaced());
		assertEquals(6, after.getMinutes());
	}
}
