package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The Gemstone Crab as Autopilot reads and plans it. The facts are pinned to their sources: the OSRS
 * wiki (Gemstone Crab: "approximately every 10 minutes", the shell's 90 seconds, three gems for the
 * top 16, the game's lines; Tlati Rainforest: its three mines) and RuneLite (gameval NpcID and
 * ItemID; the world map's mine markers for the crab's three mines).
 */
public class CrabTest
{
	static final Gson GSON = new Gson();
	static final long T0 = AutopilotCoreTest.T0;
	static final GemstoneCrab.Mine N = GemstoneCrab.Mine.NORTH;
	static final GemstoneCrab.Mine E = GemstoneCrab.Mine.EAST;
	static final GemstoneCrab.Mine SW = GemstoneCrab.Mine.SOUTH_WEST;

	// ---- The facts ----

	@Test
	public void idsAreRuneLites()
	{
		assertEquals(14779, NpcID.GEMSTONE_CRAB);
		assertEquals(14780, NpcID.GEMSTONE_CRAB_REMAINS);
		assertEquals(NpcID.GEMSTONE_CRAB, GemstoneCrab.CRAB);
		assertEquals(NpcID.GEMSTONE_CRAB_REMAINS, GemstoneCrab.SHELL);
		assertEquals(1625, ItemID.UNCUT_OPAL);
		assertEquals(1631, ItemID.UNCUT_DRAGONSTONE);
		assertEquals(8, GemstoneCrab.Gem.values().length);
	}

	/** The three mines are where RuneLite's own world map marks them, each with a Gemstone Crab. */
	@Test
	public void minesAreRuneLitesMarkers() throws Exception
	{
		Class<?> sites = Class.forName("net.runelite.client.plugins.worldmap.MiningSiteLocation");
		Method location = sites.getDeclaredMethod("getLocation");
		location.setAccessible(true);
		String[] names = {"TLATI_RAINFOREST_NORTH", "TLATI_RAINFOREST_EAST", "TLATI_RAINFOREST_SOUTH"};
		GemstoneCrab.Mine[] mines = {N, E, SW};
		for (int i = 0; i < 3; i++)
		{
			WorldPoint wp = (WorldPoint) location.invoke(site(sites, names[i]));
			assertEquals(names[i], Tile.of(wp.getX(), wp.getY(), wp.getPlane()), mines[i].getTile());
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object site(Class<?> sites, String name)
	{
		return Enum.valueOf((Class) sites, name);
	}

	@Test
	public void theGamesLines()
	{
		assertEquals("The gemstone crab burrows away, leaving a piece of its shell behind.", GemstoneCrab.BURROWS);
		assertEquals(GemstoneCrab.Gem.RED_TOPAZ, GemstoneCrab.Gem.mined("You mine an uncut red topaz from the crab shell."));
		assertEquals(GemstoneCrab.Gem.DRAGONSTONE, GemstoneCrab.Gem.mined("You mine an uncut dragonstone from the crab shell."));
		assertNull(GemstoneCrab.Gem.mined("You mine an uncut ruby."));
		assertNull(GemstoneCrab.Gem.mined("You swing your pick at the crab shell."));
		assertEquals(90, GemstoneCrab.SHELL_SECONDS);
		assertEquals(3, GemstoneCrab.GEMS_PER_SHELL);
	}

	@Test
	public void minesAreFoundNearTheirMarkers()
	{
		assertEquals(N, GemstoneCrab.Mine.at(Tile.of(1280, 3170)));
		assertEquals(E, GemstoneCrab.Mine.at(Tile.of(1352 + GemstoneCrab.NEAR, 3112)));
		assertNull(GemstoneCrab.Mine.at(Tile.of(1352 + GemstoneCrab.NEAR + 1, 3112)));
		assertNull(GemstoneCrab.Mine.at(Tile.of(3200, 3200)));
		assertNull(GemstoneCrab.Mine.at(null));
	}

	// ---- Reading the crab ----

	/** Ticks of 600 ms from {@code from}, at a mine, with the crab and its bar. */
	static long play(CrabLog log, long from, int ticks, GemstoneCrab.Mine crab, double bar, GemstoneCrab.Mine player)
	{
		long t = from;
		for (int i = 0; i < ticks; i++)
		{
			t += 600;
			log.look(t, crab, bar, player);
		}
		return t;
	}

	@Test
	public void timeLeftIsTheWikisTenMinutesUntilItsBarIsMeasured()
	{
		CrabLog log = new CrabLog();
		long t = play(log, 0, 1, N, 0.5, N);
		assertEquals(CrabLog.Left.WIKI, log.leftFrom());
		assertEquals(300, log.secondsLeft());
		// The bar falls from 50% to 40% in 30 s: 0.4 left at that pace is 120 s.
		play(log, t, 50, N, 0.4, N);
		assertEquals(CrabLog.Left.MEASURED, log.leftFrom());
		assertEquals(120, log.secondsLeft());
	}

	@Test
	public void noBarNoTime()
	{
		CrabLog log = new CrabLog();
		play(log, 0, 3, N, -1, N);
		assertEquals(CrabLog.Left.UNKNOWN, log.leftFrom());
		assertEquals(-1, log.secondsLeft());
		assertEquals("North mine", Crabs.fight(log).getTitle());
	}

	@Test
	public void aFullStayIsLearnedAndUsedForTheNextCrab()
	{
		CrabLog log = new CrabLog();
		long t = play(log, 0, 1, N, 1.0, N);
		// Eight minutes later it burrows (800 ticks).
		t = play(log, t, 800, N, 0.01, N);
		assertTrue(log.chat(GemstoneCrab.BURROWS));
		assertEquals(1, log.getCrabs());
		assertEquals(t - 600, log.averageStayMs());
		// The next crab, its bar first seen part way, not yet falling: the learned stay is used.
		t = play(log, t, 10, E, 0.5, E);
		assertEquals(CrabLog.Left.LEARNED, log.leftFrom());
		assertEquals(Math.round(0.5 * log.averageStayMs() / 1000), log.secondsLeft());
	}

	@Test
	public void movesAreLearnedFromWhereItIsSeenNext()
	{
		CrabLog log = new CrabLog();
		long t = 0;
		for (int i = 0; i < 3; i++)
		{
			t = play(log, t, 5, N, 0.2, N);
			log.chat(GemstoneCrab.BURROWS);
			// The crab is still in the scene a tick after its line: not a new sighting.
			t = play(log, t, 1, N, 0.2, N);
			assertNull(log.getMine());
			// The player crawls through the cave and finds it at the east mine a minute later.
			t = play(log, t, 100, null, -1, N);
			t = play(log, t, 5, E, 1.0, E);
			assertEquals(E, log.getMine());
			t = play(log, t, 5, E, 0.2, E);
			log.chat(GemstoneCrab.BURROWS);
			t = play(log, t, 100, null, -1, E);
			// Back to the north mine, so each move is seen.
			t = play(log, t, 5, N, 1.0, N);
		}
		assertEquals(3, log.moves(N, E));
		assertEquals(3, log.moves(E, N));
		assertEquals(E, log.next(N));
		assertEquals(N, log.next(E));
		assertNull(log.next(SW));
		assertEquals("to the east mine (3 of 3 moves you saw from here)", Crabs.next(log, N));
		assertEquals("to the north or east mine", Crabs.next(log, SW));
	}

	@Test
	public void aMoveSeenTooLateIsNotLearned()
	{
		CrabLog log = new CrabLog();
		long t = play(log, 0, 5, N, 0.2, N);
		log.chat(GemstoneCrab.BURROWS);
		// Twenty minutes away: it may have moved twice since.
		t = play(log, t, 2000, null, -1, null);
		play(log, t, 5, SW, 1.0, SW);
		assertEquals(0, log.movesFrom(N));
	}

	@Test
	public void theBurrowLineCanComeJustAfterTheCrabGoes()
	{
		CrabLog log = new CrabLog();
		long t = play(log, 0, 5, SW, 0.1, SW);
		// It despawns this tick and the line arrives after.
		play(log, t, 1, null, -1, SW);
		log.chat(GemstoneCrab.BURROWS);
		assertEquals(SW, log.getBurrowedFrom());
	}

	@Test
	public void anUnsureOrderIsNeverNamed()
	{
		CrabLog log = CrabLog.parse(GSON, "{'version':1,'moves':{'NORTH':{'EAST':3,'SOUTH_WEST':2}}}");
		assertNull(log.next(N));
		log = CrabLog.parse(GSON, "{'version':1,'moves':{'NORTH':{'EAST':2}}}");
		assertNull(log.next(N));
		log = CrabLog.parse(GSON, "{'version':1,'moves':{'NORTH':{'EAST':4,'SOUTH_WEST':1}}}");
		assertEquals(E, log.next(N));
	}

	@Test
	public void xpAndGemsAnHourAreTheAccountsOwn()
	{
		CrabLog log = new CrabLog();
		long t = 0;
		log.look(t, N, 0.5, N);
		// Twenty minutes at the crab (2000 ticks), 30 xp a tick.
		for (int i = 0; i < 2000; i++)
		{
			t += 600;
			log.look(t, N, 0.5, N);
			log.xp(30);
		}
		assertEquals(1_200_000, log.getTimedMs());
		assertEquals(180_000, log.xpPerHour());
		log.chat(GemstoneCrab.BURROWS);
		log.chat(GemstoneCrab.ELIGIBLE);
		log.chat("You mine an uncut opal from the crab shell.");
		log.chat("You mine an uncut jade from the crab shell.");
		log.chat("<col=ff0000>You mine an uncut ruby from the crab shell.</col>");
		assertEquals(3, log.totalGems());
		assertEquals(1, log.getShells());
		assertEquals(9.0, log.gemsPerHour(), 0.001);
		assertEquals("your 180k xp/hr, 9 gems/hr here", Crabs.pace(log));
	}

	@Test
	public void ratesWaitUntilEnoughIsTimed()
	{
		CrabLog log = new CrabLog();
		play(log, 0, 100, N, 0.5, N);
		log.xp(1000);
		assertEquals(-1, log.xpPerHour());
		assertEquals(-1, log.gemsPerHour(), 0);
		assertNull(Crabs.pace(log));
	}

	@Test
	public void timeAwayAndXpAwayAreNotCounted()
	{
		CrabLog log = new CrabLog();
		long t = play(log, 0, 10, N, 0.5, N);
		long timed = log.getTimedMs();
		// At the mine but the crab is elsewhere and has not just burrowed: not training.
		t = play(log, t, 10, null, -1, N);
		assertEquals(timed, log.getTimedMs());
		assertFalse(log.xp(500));
		// Logged out: the gap is not counted.
		log.pause();
		play(log, t + 600_000, 2, N, 0.5, N);
		assertEquals(timed + 600, log.getTimedMs());
	}

	// ---- The steps ----

	@Test
	public void nothingAwayFromTheCrab()
	{
		CrabLog log = new CrabLog();
		assertTrue(Crabs.candidates(log, T0).isEmpty());
		play(log, 0, 1, null, -1, null);
		assertTrue(Crabs.candidates(log, T0).isEmpty());
	}

	@Test
	public void atTheCrabTheStepIsItsClockAndWhereItGoes()
	{
		CrabLog log = CrabLog.parse(GSON, "{'version':1,'moves':{'NORTH':{'EAST':4}}}");
		play(log, 0, 1, N, 0.6, N);
		List<Candidate> out = Crabs.candidates(log, T0);
		assertEquals(1, out.size());
		Candidate c = out.get(0);
		assertEquals(Crabs.FIGHT, c.getKey());
		assertEquals(Candidate.Kind.GEMSTONE_CRAB, c.getKind());
		assertEquals(Tier.READY, c.getTier());
		assertEquals("Gemstone Crab", c.getOver());
		assertEquals("North mine, ~6 min left (est.)", c.getTitle());
		assertEquals("Then it burrows to the east mine (4 of 4 moves you saw from here)", c.getWhy());
		assertEquals(Arrays.asList(N.getTile()), c.getTargets());
	}

	@Test
	public void theShellIsAStepThatGoesToWaste()
	{
		CrabLog log = new CrabLog();
		long t = play(log, 0, 5, N, 0.05, N);
		log.chat(GemstoneCrab.BURROWS);
		// Not said yet: no shell step.
		assertEquals(Crabs.FOLLOW, Crabs.candidates(log, T0).get(0).getKey());
		log.chat(GemstoneCrab.ELIGIBLE);
		t = play(log, t, 10, null, -1, N);
		Candidate c = Crabs.candidates(log, T0).get(0);
		assertEquals(Crabs.SHELL, c.getKey());
		assertEquals(Tier.WASTING, c.getTier());
		assertEquals("Mine its shell, 3 gems", c.getTitle());
		assertEquals("It sinks in 84s; the game says it is yours to mine", c.getWhy());
		assertEquals(T0 + 84, c.getDeadline());
		assertEquals("A pickaxe", c.getBring());
		log.chat("You mine an uncut jade from the crab shell.");
		assertEquals("Mine its shell, 2 gems left", Crabs.candidates(log, T0).get(0).getTitle());
		log.chat("You mine an uncut opal from the crab shell.");
		log.chat("You mine an uncut opal from the crab shell.");
		assertEquals(Crabs.FOLLOW, Crabs.candidates(log, T0).get(0).getKey());
		assertEquals(2, log.gems(GemstoneCrab.Gem.OPAL));
	}

	@Test
	public void theShellRunsOutAfterNinetySeconds()
	{
		CrabLog log = new CrabLog();
		long t = play(log, 0, 5, N, 0.05, N);
		log.chat(GemstoneCrab.BURROWS);
		log.chat(GemstoneCrab.ELIGIBLE);
		play(log, t, 150, null, -1, N);
		assertEquals(0, log.shellSecondsLeft());
		assertEquals(Crabs.FOLLOW, Crabs.candidates(log, T0).get(0).getKey());
	}

	@Test
	public void aShellThatIsNotYoursIsCountedNotOffered()
	{
		CrabLog log = new CrabLog();
		play(log, 0, 5, N, 0.05, N);
		log.chat(GemstoneCrab.BURROWS);
		log.chat(GemstoneCrab.NOT_ELIGIBLE);
		log.chat(GemstoneCrab.NOT_ELIGIBLE);
		assertEquals(1, log.getMissed());
		assertTrue(log.shellRefused());
		assertEquals(Crabs.FOLLOW, Crabs.candidates(log, T0).get(0).getKey());
	}

	@Test
	public void afterItMovesTheStepPointsAtWhereItWent()
	{
		CrabLog log = CrabLog.parse(GSON, "{'version':1,'moves':{'EAST':{'SOUTH_WEST':5}}}");
		long t = play(log, 0, 5, E, 0.05, E);
		log.chat(GemstoneCrab.BURROWS);
		play(log, t, 1, null, -1, E);
		Candidate c = Crabs.candidates(log, T0).get(0);
		assertEquals(Crabs.FOLLOW, c.getKey());
		assertEquals("Follow it to the south-west mine", c.getTitle());
		assertEquals("The cave at every mine leads to where it is now; 5 of 5 moves you saw from the east mine went there", c.getWhy());
		assertEquals(Arrays.asList(SW.getTile()), c.getTargets());

		// Unlearned: both other mines, for Wayfinder to price the nearer.
		CrabLog fresh = new CrabLog();
		t = play(fresh, 0, 5, E, 0.05, E);
		fresh.chat(GemstoneCrab.BURROWS);
		play(fresh, t, 1, null, -1, E);
		c = Crabs.candidates(fresh, T0).get(0);
		assertEquals("Follow it through the cave", c.getTitle());
		assertEquals("The cave at every mine leads to where it is now; it left the east mine; the wiki gives no order to its moves",
			c.getWhy());
		assertEquals(Arrays.asList(N.getTile(), SW.getTile()), c.getTargets());
	}

	@Test
	public void theStepIsLeftOutWhenSwitchedOff()
	{
		CrabLog log = new CrabLog();
		play(log, 0, 1, N, 0.5, N);
		Advisor.Inputs in = new Advisor.Inputs();
		in.crab = log;
		assertTrue(Advisor.candidates(in, new Settings(), T0).stream().anyMatch(c -> c.getKind() == Candidate.Kind.GEMSTONE_CRAB));
		assertFalse(Advisor.candidates(in, new Settings().gemstoneCrab(false), T0).stream()
			.anyMatch(c -> c.getKind() == Candidate.Kind.GEMSTONE_CRAB));
	}

	// ---- The save ----

	@Test
	public void roundTrips()
	{
		CrabLog log = new CrabLog();
		long t = play(log, 0, 1, N, 1.0, N);
		for (int i = 0; i < 700; i++)
		{
			t += 600;
			log.look(t, N, 0.5, N);
			log.xp(20);
		}
		log.chat(GemstoneCrab.BURROWS);
		log.chat(GemstoneCrab.ELIGIBLE);
		log.chat("You mine an uncut sapphire from the crab shell.");
		t = play(log, t, 50, null, -1, N);
		play(log, t, 2, SW, 1.0, SW);
		CrabLog back = CrabLog.parse(GSON, log.toJson(GSON));
		assertEquals(log.getTimedMs(), back.getTimedMs());
		assertEquals(log.getXp(), back.getXp());
		assertEquals(1, back.getCrabs());
		assertEquals(1, back.getShells());
		assertEquals(1, back.gems(GemstoneCrab.Gem.SAPPHIRE));
		assertEquals(1, back.moves(N, SW));
		assertEquals(log.averageStayMs(), back.averageStayMs());
		// What is in sight is never saved.
		assertNull(back.getNear());
		assertNull(back.getMine());
	}

	@Test
	public void damagedSavesStartBlank()
	{
		assertTrue(CrabLog.parse(GSON, null).isBlank());
		assertTrue(CrabLog.parse(GSON, "not json").isBlank());
		assertTrue(CrabLog.parse(GSON, "{'version':2,'xp':5}").isBlank());
		CrabLog odd = CrabLog.parse(GSON, "{'version':1,'xp':-5,'gems':{'PEBBLE':3,'RUBY':'x'},'moves':{'NORTH':{'NORTH':4,'MOON':2}}}");
		assertEquals(0, odd.getXp());
		assertEquals(0, odd.totalGems());
		assertEquals(0, odd.movesFrom(N));
	}
}
