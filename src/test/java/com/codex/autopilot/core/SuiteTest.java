package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The whole suite in one next step: each Codex app's shared save read as its owner writes it (the
 * contract tests pin the shapes Autopilot reads, several of which are the owner's own saves rather
 * than published contracts), the journey priced from Wayfinder's atlas, and the order rule kept.
 */
public class SuiteTest
{
	static final long T0 = AutopilotCoreTest.T0;
	static final long NOW_MS = T0 * 1000;
	static final Gson GSON = new Gson();

	/** Codex Wayfinder's {@code Shared} as its Gson writes it (osrs-codex-wayfinder core/Shared.java). */
	static final String ATLAS = "{\"version\":1,\"savedAt\":1760000000000,\"walkSecondsPerTile\":0.3,\"teleports\":["
		+ "{\"name\":\"Ardougne cloak: Ardougne Farm\",\"source\":\"Ardougne cloak\",\"kind\":\"ITEM\",\"x\":2665,\"y\":3374,\"plane\":0,"
		+ "\"seconds\":4,\"carried\":true,\"timed\":false},"
		+ "{\"name\":\"Camelot Teleport\",\"source\":\"Camelot Teleport\",\"kind\":\"SPELL\",\"x\":2757,\"y\":3479,\"plane\":0,"
		+ "\"seconds\":5,\"carried\":true,\"timed\":true},"
		+ "{\"name\":\"Varrock Teleport\",\"source\":\"Varrock Teleport\",\"kind\":\"SPELL\",\"x\":3213,\"y\":3424,\"plane\":0,"
		+ "\"seconds\":5,\"carried\":false,\"timed\":false}],"
		+ "\"networks\":{\"FAIRY_RING\":true,\"SPIRIT_TREE\":false,\"GLIDER\":true},\"timedTrips\":3}";

	/** Codex Money Pilot's {@code runs}: a list of its {@code Clocked} (fields m, t, ms, gp). */
	static String runs(Object[]... list)
	{
		JsonArray a = new JsonArray();
		for (Object[] r : list)
		{
			JsonObject o = new JsonObject();
			o.addProperty("m", (String) r[0]);
			o.addProperty("t", (Long) r[1]);
			o.addProperty("ms", (Long) r[2]);
			o.addProperty("gp", (Long) r[3]);
			a.add(o);
		}
		return a.toString();
	}

	static final long HOUR = 3_600_000L;

	static Advisor.Inputs inputs()
	{
		Advisor.Inputs in = new Advisor.Inputs();
		in.atlas = Atlas.parse(GSON, ATLAS);
		return in;
	}

	static Candidate find(List<Candidate> list, Candidate.Kind kind)
	{
		return HerbRunTest.find(list, kind);
	}

	// ---- Wayfinder ----

	@Test
	public void atlasContractReadsWayfinderShape()
	{
		Atlas a = Atlas.parse(GSON, ATLAS);
		assertNotNull(a);
		assertEquals(3, a.getTeleports().size());
		Atlas.Teleport cloak = a.getTeleports().get(0);
		assertEquals("Ardougne cloak", cloak.getSource());
		assertEquals(Tile.of(2665, 3374), cloak.getLanding());
		assertEquals(4, cloak.getSeconds());
		assertTrue(cloak.isCarried());
		assertFalse(a.getTeleports().get(2).isCarried());
		assertEquals(0.3, a.getWalkSecondsPerTile(), 1e-9);
		assertEquals(2, a.openNetworks());
		assertEquals(1_760_000_000_000L, a.getSavedAt());
	}

	@Test
	public void atlasFailsSafe()
	{
		assertNull(Atlas.parse(GSON, null));
		assertNull(Atlas.parse(GSON, ""));
		assertNull(Atlas.parse(GSON, "{not json"));
		assertNull(Atlas.parse(GSON, "[]"));
		assertNull(Atlas.parse(GSON, "{\"version\":2,\"teleports\":[]}"));
		assertNull(Atlas.parse(GSON, "{\"version\":1}"));
		// A broken teleport is left out on its own; a nonsense walking pace falls back to running.
		Atlas a = Atlas.parse(GSON, "{\"version\":1,\"walkSecondsPerTile\":-4,\"teleports\":[{\"name\":\"x\",\"x\":\"a\"},"
			+ "{\"name\":\"Lumbridge Home Teleport\",\"x\":3222,\"y\":3218,\"plane\":0,\"seconds\":20,\"carried\":true}]}");
		assertEquals(1, a.getTeleports().size());
		assertEquals(Atlas.RUN_SECONDS_PER_TILE, a.getWalkSecondsPerTile(), 1e-9);
		assertEquals("Lumbridge Home Teleport", a.getTeleports().get(0).getSource());
	}

	@Test
	public void travelTakesTheCheapestWayAndSaysIt()
	{
		Atlas a = Atlas.parse(GSON, ATLAS);
		// The cloak lands beside Ardougne's patches: its cast and nothing more.
		Trip ardy = Travel.best(a, null, Collections.singletonList(Where.of(Spot.HERB_ARDOUGNE)));
		assertEquals("Ardougne cloak", ardy.getVia());
		assertEquals(4, ardy.getSeconds());
		assertEquals("via Ardougne cloak, ~5s", ardy.text());
		// Camelot lands 53 tiles from Catherby's patches: 5 s cast and 53 x 1.3 x 0.3 s of running.
		Trip cath = Travel.best(a, null, Collections.singletonList(Where.of(Spot.HERB_CATHERBY)));
		assertEquals("Camelot Teleport", cath.getVia());
		assertEquals(5 + Math.round(53 * Travel.DETOUR * 0.3), cath.getSeconds());
		// Of a run's stops, the one cheapest to reach is where it starts.
		Trip run = Travel.best(a, null, Arrays.asList(Where.of(Spot.HERB_CATHERBY), Where.of(Spot.HERB_ARDOUGNE)));
		assertEquals("Ardougne cloak", run.getVia());
		// A teleport in the bank costs a trip to fetch it, and says so.
		Trip zaff = Travel.best(a, null, Collections.singletonList(Where.of(Daily.ZAFF)));
		assertTrue(zaff.isBanked());
		assertEquals("via Varrock Teleport (from the bank), ~2 min", zaff.text());
		// Standing nearby, walking wins.
		Trip walk = Travel.best(a, Tile.of(3160, 3400), Collections.singletonList(Where.of(Daily.ZAFF)));
		assertTrue(walk.isWalk());
		// 42 tiles: 42 x 1.3 x 0.3 = 16 s, shown to the 5 seconds.
		assertEquals(16, walk.getSeconds());
		assertEquals("walk, ~20s", walk.text());
		assertEquals("you're there", Travel.best(a, Tile.of(3203, 3433), Collections.singletonList(Where.of(Daily.ZAFF))).text());
		// Nowhere near any landing: left unpriced rather than guessed.
		assertNull(Travel.best(a, null, Collections.singletonList(Where.of(Spot.HERB_WEISS))));
		assertNull(Travel.best(null, null, Collections.singletonList(Where.of(Spot.HERB_WEISS))));
		assertNull(Travel.best(a, null, Collections.emptyList()));
	}

	@Test
	public void tripTimesReadTheSuiteWay()
	{
		assertEquals("~5s", Trip.time(1));
		assertEquals("~40s", Trip.time(38));
		assertEquals("~1 min", Trip.time(60));
		assertEquals("~2 min", Trip.time(61));
	}

	/** Every patch tile that RuneLite's world map marks is one of its markers, so none drifts from the game. */
	@Test
	public void patchTilesAreRuneLiteWorldMapMarkers() throws Exception
	{
		Class<?> type = Class.forName("net.runelite.client.plugins.worldmap.FarmingPatchLocation");
		Field locations = type.getDeclaredField("locations");
		locations.setAccessible(true);
		Set<Tile> markers = new HashSet<>();
		for (Object e : type.getEnumConstants())
		{
			for (WorldPoint p : (WorldPoint[]) locations.get(e))
			{
				markers.add(Tile.of(p.getX(), p.getY(), p.getPlane()));
			}
		}
		Set<Spot> fromRounds = new HashSet<>(Arrays.asList(Spot.BIRDHOUSE_VALLEY_NE, Spot.BIRDHOUSE_VALLEY_SW, Spot.BIRDHOUSE_MEADOW_N,
			Spot.BIRDHOUSE_MEADOW_S, Spot.HARDWOOD_FOSSIL_E, Spot.HARDWOOD_FOSSIL_M, Spot.HARDWOOD_FOSSIL_W, Spot.SEAWEED_N, Spot.SEAWEED_S,
			Spot.SPIRIT_ETCETERIA));
		for (Spot s : Spot.values())
		{
			Tile t = Where.of(s);
			assertNotNull("No tile for " + s, t);
			if (!fromRounds.contains(s))
			{
				assertTrue(s + " at " + t + " is not a RuneLite marker", markers.contains(t));
			}
		}
		for (Daily d : Daily.values())
		{
			assertNotNull("No tile for " + d, Where.of(d));
		}
	}

	// ---- The order rule ----

	static Candidate chore(String key, Tier tier, double urgency, int minutes, Tile at)
	{
		return new Candidate(key, Candidate.Kind.TREE_RUN, tier, key, key, "why", null, minutes, urgency, 0, 1, -1)
			.at(Collections.singletonList(at));
	}

	@Test
	public void tiesBreakByMinutesIncludingTravel()
	{
		Advisor.Inputs in = inputs();
		Settings s = Settings.defaults();
		// Two chores alike in every way but where they are: the one by a teleport comes first.
		Candidate far = chore("a-far", Tier.READY, 1.0, 5, Tile.of(2600, 3300));
		Candidate near = chore("b-near", Tier.READY, 1.0, 5, Where.of(Spot.HERB_ARDOUGNE));
		List<Candidate> priced = Advisor.travel(Arrays.asList(far, near), in, s);
		Plan p = Ranker.rank(priced, s, null, T0);
		assertEquals(Arrays.asList("b-near", "a-far"), AutopilotCoreTest.keys(p));
		// 4 s by the cloak against 33 s (the cloak and 74 tiles of running): both round to a minute, the seconds decide.
		assertEquals(5 * 60 + 4, p.getSteps().get(0).getTotalSeconds());
		assertEquals(5 * 60 + 33, p.getSteps().get(1).getTotalSeconds());
		// Without the atlas the key decides, as before.
		in.atlas = null;
		assertEquals(Arrays.asList("a-far", "b-near"), AutopilotCoreTest.keys(Ranker.rank(Advisor.travel(Arrays.asList(far, near), in, s), s,
			null, T0)));
	}

	@Test
	public void travelNeverJumpsTheBands()
	{
		Advisor.Inputs in = inputs();
		Settings s = Settings.defaults();
		// Going to waste far away still beats a ready chore at the cloak, which beats a goal.
		Candidate wasting = chore("wasting", Tier.WASTING, 0.1, 20, Tile.of(2600, 3300));
		Candidate ready = chore("ready", Tier.READY, 5, 2, Where.of(Spot.HERB_ARDOUGNE));
		Candidate focus = new Candidate("goal:x", Candidate.Kind.FOCUS_GOAL, Tier.FOCUS, "Toward x", "x", "why", null, 15, 1, 0, 1, -1);
		Candidate other = new Candidate("goal:y", Candidate.Kind.GOAL, Tier.GOAL, "Toward y", "y", "why", null, 15, 9, 0, 1, -1);
		Plan p = Ranker.rank(Advisor.travel(Arrays.asList(other, focus, ready, wasting), in, s), s, null, T0);
		assertEquals(Arrays.asList("wasting", "ready", "goal:x", "goal:y"), AutopilotCoreTest.keys(p));
	}

	@Test
	public void herbRunShowsTheWayAndCountsIt()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		HerbRunTest.put(p, Spot.HERB_ARDOUGNE, HerbRunTest.RANARR_PICK, T0 - 4000);
		HerbRunTest.put(p, Spot.HERB_CATHERBY, HerbRunTest.RANARR_PICK, T0 - 4000);
		Advisor.Inputs in = Advisor.Inputs.read(p, GSON, T0, false);
		in.atlas = Atlas.parse(GSON, ATLAS);
		List<Candidate> all = Advisor.candidates(in, Settings.defaults(), T0);
		Candidate herbs = find(all, Candidate.Kind.HERB_RUN);
		assertNotNull(herbs);
		assertEquals("via Ardougne cloak, ~5s", herbs.getTrip().text());
		assertEquals(herbs.getMinutes() + 1, herbs.getTotalMinutes());
		// Switched off, the same run is not priced.
		Candidate plain = find(Advisor.candidates(in, Settings.defaults().wayfinder(false), T0), Candidate.Kind.HERB_RUN);
		assertNull(plain.getTrip());
		assertEquals(plain.getMinutes(), plain.getTotalMinutes());
	}

	@Test
	public void budgetCountsTheJourney()
	{
		Advisor.Inputs in = inputs();
		Settings s = Settings.defaults();
		// 14 minutes of work plus a 2 minute journey does not fit in 15.
		Candidate zaff = chore("zaff", Tier.READY, 1, 14, Where.of(Daily.ZAFF));
		Plan p = Ranker.rank(Advisor.travel(Collections.singletonList(zaff), in, s), s, null, T0);
		assertEquals(16, p.top().getTotalMinutes());
		assertTrue(Budget.fit(p, 15, null).isEmpty());
		assertEquals(1, Budget.fit(p, 30, null).getSteps().size());
	}

	// ---- Money Pilot, Gear Roadmap, Ledger ----

	@Test
	public void moneyRunsContractAndRate()
	{
		String json = runs(
			new Object[]{"vorkath", NOW_MS - 86_400_000L, HOUR, 3_000_000L},
			new Object[]{"vorkath", NOW_MS - 2 * 86_400_000L, HOUR / 2, 2_000_000L},
			// Too short to say anything, and too old: left out, as Money Pilot leaves them out.
			new Object[]{"vorkath", NOW_MS - 3_600_000L, 60_000L, 90_000_000L},
			new Object[]{"zulrah", NOW_MS - 61 * 86_400_000L, HOUR, 99_000_000L},
			// Under fifteen minutes of play in all: no rate yet.
			new Object[]{"tan-leather", NOW_MS - 3_600_000L, 10 * 60_000L, 500_000L},
			new Object[]{"rune-dragons", NOW_MS - 3_600_000L, HOUR, 1_000_000L});
		List<Earner> e = MoneyRuns.parse(GSON, json, NOW_MS);
		assertEquals(2, e.size());
		assertEquals("Killing Vorkath", e.get(0).getDoing());
		assertEquals(5_000_000L * 2 / 3, e.get(0).getPerHour(), 1);
		assertEquals("Money Pilot", e.get(0).getFrom());
		assertEquals("Killing rune dragons", e.get(1).getDoing());
		assertEquals("Some new method", MoneyRuns.name("some-new-method"));
		assertTrue(MoneyRuns.parse(GSON, "{\"m\":1}", NOW_MS).isEmpty());
		assertTrue(MoneyRuns.parse(GSON, "garbage[", NOW_MS).isEmpty());
		assertTrue(MoneyRuns.parse(GSON, null, NOW_MS).isEmpty());
	}

	@Test
	public void gearGoalAndLedgerContracts()
	{
		SavingsGoal g = SavingsGoal.parse(GSON, "{\"name\":\"Twisted bow\",\"ids\":[20997]}");
		assertEquals("Twisted bow", g.getName());
		assertEquals(Collections.singletonList(20997), g.getIds());
		assertNull(SavingsGoal.parse(GSON, "{\"name\":\"Twisted bow\",\"ids\":[]}"));
		assertNull(SavingsGoal.parse(GSON, "{\"ids\":[20997]}"));
		assertNull(SavingsGoal.parse(GSON, "[1]"));

		// Codex Ledger's Ledger as its Gson writes it: stores by Store name, items and prices by id.
		String ledger = "{\"stores\":{\"BANK\":{\"seen\":1760000000000,\"items\":{\"995\":40000000,\"13204\":2000,\"4151\":1}},"
			+ "\"INVENTORY\":{\"seen\":1760000000000,\"items\":{\"995\":500000}}},\"prices\":{\"20997\":1500000000,\"4151\":1500000},"
			+ "\"history\":[{\"t\":1760000000000,\"v\":100000000}],\"days\":[],\"total\":100000000,\"since\":1750000000000}";
		Wealth w = Wealth.parse(GSON, ledger);
		assertEquals(100_000_000L, w.getTotal());
		assertEquals(40_500_000L + 2000 * 1000L, w.coins());
		assertTrue(w.holds(4151));
		assertEquals(1_500_000_000L, w.price(20997));
		assertNull(Wealth.parse(GSON, "{\"total\":0}"));
		assertNull(Wealth.parse(GSON, "nonsense"));
	}

	static Advisor.Inputs goalInputs(String runs)
	{
		Advisor.Inputs in = new Advisor.Inputs();
		in.goal = new SavingsGoal("Twisted bow", Collections.singletonList(ItemID.TWISTED_BOW));
		Map<Integer, Long> held = new HashMap<>();
		held.put(ItemID.COINS, 1_200_000_000L);
		in.wealth = Wealth.of(1_300_000_000L, held, Collections.singletonMap(ItemID.TWISTED_BOW, 1_500_000_000L));
		in.money = MoneyRuns.parse(GSON, runs, NOW_MS);
		return in;
	}

	@Test
	public void earnStepWhenTheGearGoalIsShortOfCoins()
	{
		Advisor.Inputs in = goalInputs(runs(new Object[]{"vorkath", NOW_MS - 3_600_000L, 2 * HOUR, 6_800_000L}));
		List<Candidate> all = Advisor.candidates(in, Settings.defaults(), T0);
		Candidate earn = find(all, Candidate.Kind.EARN);
		assertNotNull(earn);
		assertEquals(Tier.GOAL, earn.getTier());
		assertEquals("Toward Twisted bow", earn.getOver());
		// 300M short at 3.4M an hour: an hour's sitting.
		assertEquals("Earn 3.4M: Killing Vorkath", earn.getTitle());
		assertEquals(60, earn.getMinutes());
		assertEquals("300.0M short; you make 3.4M/hr at it in Codex Money Pilot, about 88 hr to go", earn.getWhy());
		assertEquals(ItemID.TWISTED_BOW, earn.getItemId());
		assertTrue(Advisor.fedBy(in, Settings.defaults(), all).containsAll(Arrays.asList("Gear Roadmap", "Money Pilot", "Ledger")));
	}

	@Test
	public void earnStepFitsWhatIsLeftAndPricesFromTheGame()
	{
		Advisor.Inputs in = goalInputs(runs(new Object[]{"vorkath", NOW_MS - 3_600_000L, HOUR, 6_000_000L}));
		// Today's price from the game: 1.2015B, so 1.5M short, a quarter hour at 6M an hour.
		in.prices = id -> id == ItemID.TWISTED_BOW ? 1_201_500_000L : 0;
		Candidate earn = Suite.earn(in, null, in.money);
		assertEquals("Earn 1.5M: Killing Vorkath", earn.getTitle());
		assertEquals(Suite.EARN_MIN_MINUTES, earn.getMinutes());
		// Affordable, owned, an ironman, or nothing to price: no step.
		in.prices = id -> 1_000_000_000L;
		assertNull(Suite.earn(in, null, in.money));
		in.prices = null;
		in.ironman = true;
		assertNull(Suite.earn(in, null, in.money));
		in.ironman = false;
		in.wealth = Wealth.of(1, Collections.singletonMap(ItemID.TWISTED_BOW, 1L), Collections.emptyMap());
		assertNull(Suite.earn(in, null, in.money));
		in.wealth = null;
		assertNull(Suite.earn(in, null, in.money));
	}

	@Test
	public void earnStepWithoutAnyRateSaysHowToGetOne()
	{
		Advisor.Inputs in = goalInputs(null);
		Candidate earn = Suite.earn(in, null, in.money);
		assertEquals("Earn 300.0M", earn.getTitle());
		assertTrue(earn.getWhy().contains("clock a money maker in Codex Money Pilot"));
		// Without Ledger, Bank Steward's bank gives the coins.
		in.wealth = null;
		Map<Integer, Long> bank = new HashMap<>();
		bank.put(ItemID.COINS, 500_000_000L);
		bank.put(ItemID.PLATINUM, 700_000L);
		in.prices = id -> 1_500_000_000L;
		assertEquals("Earn 300.0M", Suite.earn(in, BankSnapshot.of(bank, 0), in.money).getTitle());
	}

	// ---- Boss Pilot, Quartermaster, Log Insight ----

	/** Codex Boss Pilot's Logbook as its Gson writes it. */
	static final String LOGBOOK = "{\"version\":1,\"bosses\":{\"vorkath\":{\"kc\":312,\"best\":58.2,\"trips\":["
		+ "{\"start\":1759990000000,\"end\":1759991200000,\"kills\":5,\"loot\":1500000,\"deaths\":0,\"foodUsed\":40,\"dosesUsed\":8,"
		+ "\"bestKill\":61.0,\"damage\":900,\"biggestHit\":40},"
		+ "{\"start\":1759992000000,\"end\":1759993200000,\"kills\":4,\"loot\":1100000,\"deaths\":0,\"foodUsed\":40,\"dosesUsed\":8,"
		+ "\"bestKill\":58.2,\"damage\":800,\"biggestHit\":38}]},"
		+ "\"zulrah\":{\"kc\":-1,\"best\":0,\"trips\":[]}}}";

	/** Codex Quartermaster's KitBook as its Gson writes it. */
	static String kits(int tripsLeft)
	{
		return "{\"version\":1,\"active\":\"vorkath\",\"target\":5,\"kits\":[{\"id\":\"vorkath\",\"name\":\"Vorkath\",\"preset\":true,"
			+ "\"tripsLeft\":" + tripsLeft + ",\"limiting\":\"shark\",\"needs\":[{\"supply\":\"shark\",\"name\":\"Sharks\",\"unit\":\"sharks\","
			+ "\"perTrip\":15,\"items\":{\"385\":1}}]},{\"id\":\"zulrah\",\"name\":\"Zulrah\",\"preset\":true,\"tripsLeft\":-1,\"needs\":[]}]}";
	}

	@Test
	public void bossPilotContractAndStep()
	{
		BossLog log = BossLog.parse(GSON, "vorkath", LOGBOOK);
		assertEquals("vorkath", log.getPinned());
		BossLog.Record r = log.get("vorkath");
		assertEquals(312, r.getKc());
		assertEquals(2, r.getTrips());
		assertEquals(9, r.getKills());
		assertEquals(40, r.getMinutes(), 1e-9);
		assertEquals(3_900_000L, r.lootPerHour());
		assertEquals("Vorkath trips", log.earners().get(0).getDoing());
		assertNull(BossLog.parse(GSON, null, null));
		assertNull(BossLog.parse(GSON, " ", "{\"version\":2,\"bosses\":{\"vorkath\":{}}}"));

		Advisor.Inputs in = new Advisor.Inputs();
		in.bossLog = log;
		in.kits = Kits.parse(GSON, kits(3));
		Candidate boss = Suite.boss(in, Settings.defaults());
		assertEquals("Vorkath trip", boss.getTitle());
		assertEquals("Boss Pilot", boss.getOver());
		assertEquals(20, boss.getMinutes());
		assertEquals("Pinned in Codex Boss Pilot; your trips: 3.9M/hr, 5 kills a trip, 312 kc", boss.getWhy());
		assertEquals("Take your Vorkath kit: 3 trips in the bank", boss.getBring());
		assertFalse(Bring.isShort(boss.getBring()));

		// The log's own counter is newer than Boss Pilot's count.
		in.wanted = WantedSlots.parse(GSON, "{\"counters\":{\"Vorkath kills\":320}}");
		assertTrue(Suite.boss(in, Settings.defaults()).getWhy().endsWith("320 kc"));

		in.kits = Kits.parse(GSON, kits(0));
		boss = Suite.boss(in, Settings.defaults());
		assertEquals("You're out of sharks for your Vorkath kit", boss.getBring());
		assertTrue(Bring.isShort(boss.getBring()));
		// The boss step already says the kit is out, so no separate restock.
		assertNull(Suite.restock(in, boss));
		assertNotNull(Suite.restock(in, null));

		// A pin with nothing logged yet.
		in.bossLog = BossLog.parse(GSON, "zulrah", null);
		in.kits = null;
		in.wanted = null;
		boss = Suite.boss(in, Settings.defaults());
		assertEquals("Zulrah trip", boss.getTitle());
		assertEquals("Pinned in Codex Boss Pilot; no trips logged yet", boss.getWhy());
		assertEquals(Suite.BOSS_MINUTES, boss.getMinutes());
	}

	@Test
	public void quartermasterContractAndRestock()
	{
		Kits k = Kits.parse(GSON, kits(0));
		assertEquals("vorkath", k.active().getId());
		assertEquals("Sharks", k.active().getLimiting());
		assertEquals(5, k.getTarget());
		assertEquals(-1, k.byId("zulrah").getTripsLeft());
		assertEquals("zulrah", k.forBoss("zulrah", "Zulrah").getId());
		Advisor.Inputs in = new Advisor.Inputs();
		in.kits = k;
		Candidate restock = Suite.restock(in, null);
		assertEquals("Restock Vorkath", restock.getTitle());
		assertEquals("Sharks ran out first: your bank covers no trips of the kit chosen in Codex Quartermaster", restock.getWhy());
		// Trips left, or never counted: nothing to restock.
		in.kits = Kits.parse(GSON, kits(2));
		assertNull(Suite.restock(in, null));
		in.kits = Kits.parse(GSON, kits(-1));
		assertNull(Suite.restock(in, null));
		// No kit chosen yet: Quartermaster leaves "active" out.
		in.kits = Kits.parse(GSON, kits(0).replace("\"active\":\"vorkath\",", ""));
		assertNull(Suite.restock(in, null));
		assertNull(Kits.parse(GSON, "{\"version\":2,\"kits\":[]}"));
	}

	@Test
	public void logInsightContractAndStep()
	{
		// Codex Log Insight's Progress as its Gson writes it (only owned, wanted and counters are read).
		String progress = "{\"owned\":[22106,11286],\"counters\":{\"Vorkath kills\":312},\"finds\":[],\"pace\":{},\"fullReadAt\":0,"
			+ "\"pagesRead\":{},\"pageLabels\":{},\"wanted\":[11286,22006,21992]}";
		WantedSlots w = WantedSlots.parse(GSON, progress);
		assertEquals(Arrays.asList(22006, 21992), w.getWanted());
		assertEquals(312, w.counter("Vorkath kills"));
		assertEquals(-1, w.counter("Zulrah kills"));
		Advisor.Inputs in = new Advisor.Inputs();
		in.wanted = w;
		in.names = id -> id == 22006 ? "Skeletal visage" : null;
		Candidate log = Suite.log(in);
		assertEquals("Chase Skeletal visage", log.getTitle());
		assertEquals("Marked wanted in Codex Log Insight; 1 more wanted slot", log.getWhy());
		assertEquals(22006, log.getItemId());
		in.names = id -> {
			throw new IllegalStateException("not on the client thread");
		};
		assertEquals("Chase your wanted slot", Suite.log(in).getTitle());
		assertNull(WantedSlots.parse(GSON, "{\"wanted\":[1],\"owned\":[1]}"));
	}

	// ---- Rounds ----

	@Test
	public void roundsDailiesWhileLoggedOut()
	{
		long today = T0 - 600;
		long yesterday = T0 - 86_400;
		// Codex Rounds' Memory as its Gson writes it (only "dailies" is read).
		String memory = "{\"seen\":{},\"bank\":{},\"dailies\":{"
			+ "\"ZAFF\":{\"unlocked\":true,\"claimable\":true,\"seenAt\":" + today + ",\"reward\":\"15 battlestaves\",\"needQuantity\":0},"
			+ "\"FLAX\":{\"unlocked\":true,\"claimable\":true,\"seenAt\":" + yesterday + ",\"reward\":\"\",\"needQuantity\":0},"
			+ "\"BERT\":{\"unlocked\":false,\"claimable\":false,\"seenAt\":" + today + ",\"reward\":\"\",\"needQuantity\":0},"
			+ "\"SOMETHING_NEW\":{\"unlocked\":true,\"claimable\":true,\"seenAt\":" + today + "}},\"farming\":40}";
		List<DailyState> seen = RoundsDailies.parse(GSON, memory, T0);
		assertEquals(2, seen.size());
		Advisor.Inputs in = new Advisor.Inputs();
		in.roundsDailies = seen;
		List<Candidate> all = Advisor.candidates(in, Settings.defaults(), T0);
		Candidate d = find(all, Candidate.Kind.DAILIES);
		assertEquals("Zaff's battlestaves", d.getTitle());
		assertTrue(d.getWhy().endsWith(Advisor.SEEN_BY_ROUNDS));
		assertTrue(Advisor.fedBy(in, Settings.defaults(), all).contains("Rounds"));
		// Logged in, the live game wins.
		in.dailies = new ArrayList<>();
		assertNull(find(Advisor.candidates(in, Settings.defaults(), T0), Candidate.Kind.DAILIES));
		in.dailies = null;
		assertNull(find(Advisor.candidates(in, Settings.defaults().roundsDailies(false), T0), Candidate.Kind.DAILIES));
		assertNull(RoundsDailies.parse(GSON, "{\"seen\":{}}", T0));
		assertNull(RoundsDailies.parse(GSON, "][", T0));
	}

	// ---- Fail-safe, fed by, the shared plan ----

	@Test
	public void brokenSuiteDataNeverStopsThePlan()
	{
		AutopilotCoreTest.MapProfile p = new AutopilotCoreTest.MapProfile();
		AutopilotCoreTest.fullHouses(p);
		Advisor.Inputs in = Advisor.Inputs.read(p, GSON, T0, false);
		in.atlas = Atlas.parse(GSON, "{\"version\":1,\"teleports\":[{\"name\":7}]}");
		in.money = MoneyRuns.parse(GSON, "{]", NOW_MS);
		in.bossLog = BossLog.parse(GSON, "nobody", "{oops");
		in.goal = SavingsGoal.parse(GSON, "{\"name\":\"x\",\"ids\":[\"y\"]}");
		in.wealth = Wealth.parse(GSON, "{\"total\":\"lots\"}");
		in.kits = Kits.parse(GSON, "{\"version\":1,\"kits\":[{}]}");
		in.wanted = WantedSlots.parse(GSON, "{\"wanted\":\"all\"}");
		in.prices = id -> {
			throw new IllegalStateException();
		};
		in.names = id -> {
			throw new IllegalStateException();
		};
		Plan plan = Advisor.plan(in, Settings.defaults(), null, T0);
		assertEquals("chores:birdhouse_run", plan.top().getKey());
		Candidate boss = find(plan.getSteps(), Candidate.Kind.BOSS);
		assertEquals("Nobody trip", boss.getTitle());
	}

	@Test
	public void fedByNamesOnlyAppsInThePlan()
	{
		Advisor.Inputs in = new Advisor.Inputs();
		Settings s = Settings.defaults();
		assertTrue(Advisor.fedBy(in, s, Collections.emptyList()).isEmpty());
		in.atlas = Atlas.parse(GSON, ATLAS);
		in.kits = Kits.parse(GSON, kits(2));
		in.bossLog = BossLog.parse(GSON, "vorkath", LOGBOOK);
		assertEquals(Arrays.asList("Wayfinder", "Boss Pilot", "Quartermaster"), Advisor.fedBy(in, s, Collections.emptyList()));
		assertEquals(Arrays.asList("Boss Pilot", "Quartermaster"), Advisor.fedBy(in, s.wayfinder(false), Collections.emptyList()));
		assertEquals(Collections.singletonList("Rounds"), Advisor.fedBy(new Advisor.Inputs(), Settings.defaults().rounds(true),
			Collections.emptyList()));
	}

	/** Pins {@code codexautopilot/planV1}, which Codex Home reads: the same shape is pinned there. */
	@Test
	public void sharedPlanContract()
	{
		Candidate herbs = new Candidate("chores:herb_run", Candidate.Kind.HERB_RUN, Tier.READY, "Herb run", "5 ready", "Grown for 2 hr",
			null, 12, 3, 0, 5, ItemID.RANARR_WEED).withTrip(Trip.via("Ardougne cloak", 38, false));
		Candidate goal = new Candidate("goal:quest:Dragon Slayer II", Candidate.Kind.FOCUS_GOAL, Tier.FOCUS, "Toward Dragon Slayer II",
			"Start Dragon Slayer II", "Your focus", null, 60, 0.1, 0, 1, ItemID.SKILLCAPE_QP);
		SharedPlan plan = SharedPlan.of(Arrays.asList(herbs, goal), Arrays.asList("Wayfinder", "Navigator"), NOW_MS);
		JsonObject o = GSON.fromJson(plan.toJson(GSON), JsonObject.class);
		assertEquals(new HashSet<>(Arrays.asList("version", "savedAt", "steps", "fedBy")), o.keySet());
		assertEquals(1, o.get("version").getAsInt());
		assertEquals(NOW_MS, o.get("savedAt").getAsLong());
		JsonObject first = o.getAsJsonArray("steps").get(0).getAsJsonObject();
		assertEquals(new HashSet<>(Arrays.asList("key", "kind", "tier", "over", "title", "why", "minutes", "travel", "itemId")), first.keySet());
		assertEquals("chores:herb_run", first.get("key").getAsString());
		assertEquals("HERB_RUN", first.get("kind").getAsString());
		assertEquals("READY", first.get("tier").getAsString());
		assertEquals("Herb run", first.get("over").getAsString());
		assertEquals("5 ready", first.get("title").getAsString());
		assertEquals(13, first.get("minutes").getAsInt());
		assertEquals("via Ardougne cloak, ~40s", first.get("travel").getAsString());
		assertEquals(ItemID.RANARR_WEED, first.get("itemId").getAsInt());
		// No journey priced: no travel field.
		assertFalse(o.getAsJsonArray("steps").get(1).getAsJsonObject().has("travel"));
		assertEquals("FOCUS", o.getAsJsonArray("steps").get(1).getAsJsonObject().get("tier").getAsString());
		assertEquals("Wayfinder", o.getAsJsonArray("fedBy").get(0).getAsString());
		assertEquals("codexautopilot", com.codex.autopilot.CodexAutopilotConfig.GROUP);
		assertEquals("planV1", SharedPlan.KEY);
		// At most five steps.
		List<Candidate> many = new ArrayList<>();
		for (int i = 0; i < 9; i++)
		{
			many.add(chore("c" + i, Tier.READY, 1, 5, Tile.of(1, 1)));
		}
		assertEquals(SharedPlan.STEPS, SharedPlan.of(many, null, NOW_MS).steps.size());
	}

	@Test
	public void sharedPlanIsSavedWhenItChangesNotEveryTick()
	{
		SharedPlan.Saver saver = new SharedPlan.Saver();
		Candidate a = chore("a", Tier.READY, 1, 5, Tile.of(1, 1));
		Candidate b = chore("b", Tier.READY, 1, 5, Tile.of(1, 1));
		assertTrue(saver.due(SharedPlan.of(Arrays.asList(a, b), null, NOW_MS), GSON, NOW_MS));
		assertFalse(saver.due(SharedPlan.of(Arrays.asList(a, b), null, NOW_MS + 3000), GSON, NOW_MS + 3000));
		// A countdown in the why moves on: saved again only after the refresh.
		Candidate a2 = a.withNote("1 min more");
		assertFalse(saver.due(SharedPlan.of(Arrays.asList(a2, b), null, NOW_MS + 6000), GSON, NOW_MS + 6000));
		assertTrue(saver.due(SharedPlan.of(Arrays.asList(a2, b), null, NOW_MS + SharedPlan.REFRESH_MS), GSON, NOW_MS + SharedPlan.REFRESH_MS));
		// A new order is saved at once.
		assertTrue(saver.due(SharedPlan.of(Arrays.asList(b, a2), null, NOW_MS + SharedPlan.REFRESH_MS + 600),
			GSON, NOW_MS + SharedPlan.REFRESH_MS + 600));
	}
}
