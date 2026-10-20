package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AutopilotCoreTest
{
	/** 2025-10-09 08:53:20 UTC: fifteen hours before the daily reset. */
	static final long T0 = 1_760_000_000L;
	static final long RESET = KingdomWatch.nextReset(T0);
	static final Gson GSON = new Gson();

	static final class MapProfile implements Profile
	{
		final Map<String, String> values = new HashMap<>();

		MapProfile put(String group, String key, String value)
		{
			values.put(group + "." + key, value);
			return this;
		}

		@Override
		public String get(String group, String key)
		{
			return values.get(group + "." + key);
		}
	}

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

	static String tt(int value, long time)
	{
		return value + ":" + time;
	}

	/** Four oak houses seeded an hour before T0, so full for ten minutes. */
	static MapProfile fullHouses(MapProfile p)
	{
		for (Spot s : new Spot[]{Spot.BIRDHOUSE_VALLEY_NE, Spot.BIRDHOUSE_VALLEY_SW, Spot.BIRDHOUSE_MEADOW_N, Spot.BIRDHOUSE_MEADOW_S})
		{
			p.put(Chores.GROUP, s.timeTrackingKey(), tt(6, T0 - 3600));
		}
		return p;
	}

	static MapProfile seaweedReady(MapProfile p)
	{
		p.put(Chores.GROUP, Spot.SEAWEED_N.timeTrackingKey(), tt(8, T0 - 600));
		p.put(Chores.GROUP, Spot.SEAWEED_S.timeTrackingKey(), tt(8, T0 - 600));
		return p;
	}

	static List<Candidate> chores(MapProfile p, BankSnapshot bank, long now)
	{
		Map<Spot, Seen> seen = Chores.readSeen(p);
		return Chores.candidates(Chores.read(seen, now, Growth.Offset.NONE), bank, now, Growth.Offset.NONE);
	}

	static Candidate c(String key, Tier tier, double urgency, int minutes, long deadline)
	{
		return new Candidate(key, Candidate.Kind.TREE_RUN, tier, key, key, "why", null, minutes, urgency, deadline, 1, -1);
	}

	static List<String> keys(Plan p)
	{
		List<String> out = new ArrayList<>();
		for (Candidate c : p.getSteps())
		{
			out.add(c.getKey());
		}
		return out;
	}

	// ---- Format ----

	@Test
	public void numbersFollowTheSuiteRule()
	{
		assertEquals("0", Format.number(0));
		assertEquals("40", Format.number(40));
		assertEquals("9,999", Format.number(9_999));
		assertEquals("10k", Format.number(10_000));
		assertEquals("250k", Format.number(250_000));
		assertEquals("999k", Format.number(999_999));
		assertEquals("1.0M", Format.number(1_000_000));
		assertEquals("1.2M", Format.number(1_299_999));
		assertEquals("999.9M", Format.number(999_999_999));
		assertEquals("2.14B", Format.number(Integer.MAX_VALUE));
		assertEquals("-12k", Format.number(-12_345));
		assertEquals("250k coins", Format.coins(250_000));
		assertEquals("1 hr 5 min", Format.duration(3900));
		assertEquals("now", Format.duration(0));
	}

	// ---- Time Tracking ----

	@Test
	public void timeTrackingKeysMatchCore()
	{
		assertEquals("birdhouse." + net.runelite.api.gameval.VarPlayerID.BIRDHOUSE_TRANSMIT_A, Spot.BIRDHOUSE_MEADOW_N.timeTrackingKey());
		assertEquals("15008." + VarbitID.FARMING_TRANSMIT_A, Spot.SEAWEED_N.timeTrackingKey());
		assertEquals("4922." + VarbitID.FARMING_TRANSMIT_I, Spot.REDWOOD_GUILD.timeTrackingKey());
		assertEquals("5423." + VarbitID.FARMING_TRANSMIT_A, Spot.CALQUAT_KASTORI.timeTrackingKey());
		assertEquals(34, Spot.values().length);
	}

	@Test
	public void malformedTimeTrackingValuesAreSkipped()
	{
		MapProfile p = new MapProfile();
		p.put(Chores.GROUP, Spot.BIRDHOUSE_VALLEY_NE.timeTrackingKey(), "abc");
		p.put(Chores.GROUP, Spot.BIRDHOUSE_VALLEY_SW.timeTrackingKey(), "6:");
		p.put(Chores.GROUP, Spot.BIRDHOUSE_MEADOW_N.timeTrackingKey(), ":" + T0);
		p.put(Chores.GROUP, Spot.BIRDHOUSE_MEADOW_S.timeTrackingKey(), "6:-5");
		p.put(Chores.GROUP, Spot.SEAWEED_N.timeTrackingKey(), "99999999999:" + T0);
		p.put(Chores.GROUP, Spot.SEAWEED_S.timeTrackingKey(), "-3:" + T0);
		p.put(Chores.GROUP, Spot.CALQUAT_KASTORI.timeTrackingKey(), "1:2:3");
		p.put(Chores.GROUP, Spot.REDWOOD_GUILD.timeTrackingKey(), "999:" + T0);
		Map<Spot, Seen> seen = Chores.readSeen(p);
		assertEquals(1, seen.size());
		// Value 999 is outside every table: it reads as unseen and adds no step.
		assertTrue(chores(p, null, T0).isEmpty());
	}

	@Test
	public void storeThatThrowsReadsAsEmpty()
	{
		Profile broken = (g, k) ->
		{
			throw new IllegalStateException("closed");
		};
		assertTrue(Chores.readSeen(broken).isEmpty());
		Advisor.Inputs in = Advisor.Inputs.read(broken, GSON, T0, false);
		assertTrue(Advisor.plan(in, Settings.defaults(), new Session(), T0).isEmpty());
	}

	@Test
	public void farmTickOffsetIsReadSafely()
	{
		MapProfile p = new MapProfile().put(Chores.GROUP, Chores.OFFSET_KEY, "3").put(Chores.GROUP, Chores.PRECISION_KEY, "oops");
		assertNotNull(Chores.readOffset(p));
		assertEquals(Integer.valueOf(3), Chores.integer(" 3 "));
		assertNull(Chores.integer("3.5"));
	}

	// ---- Grouping ----

	@Test
	public void birdhousesAndSeaweedGroupIntoOneRun()
	{
		MapProfile p = seaweedReady(fullHouses(new MapProfile()));
		List<Candidate> list = chores(p, null, T0);
		assertEquals(1, list.size());
		Candidate run = list.get(0);
		assertEquals(Candidate.Kind.BIRDHOUSE_RUN, run.getKind());
		assertEquals("Birdhouse run: 4 ready, 2 seaweed ready", run.headline());
		assertEquals(Tier.READY, run.getTier());
		assertEquals(6, run.getCount());
		assertTrue(run.getWhy(), run.getWhy().startsWith("Full for 10 min"));
		assertEquals("Bring 4 oak logs + 40 hop seeds + 2 seaweed spores", run.getBring());
		// Four houses idle 1.2 runs an hour each; two seaweed patches 2 an hour each.
		assertEquals(4 * 1.2 + 2 * 2.0, run.getUrgency(), 0.01);
	}

	@Test
	public void seaweedAloneIsAFossilIslandRun()
	{
		List<Candidate> list = chores(seaweedReady(new MapProfile()), null, T0);
		assertEquals("Fossil Island run: 2 seaweed ready", list.get(0).headline());
		assertEquals("Bring 2 seaweed spores", list.get(0).getBring());
	}

	@Test
	public void housesStillFillingAreNotReadyButAreMentioned()
	{
		MapProfile p = seaweedReady(new MapProfile());
		// Seeded 45 minutes ago: full in 5.
		p.put(Chores.GROUP, Spot.BIRDHOUSE_MEADOW_N.timeTrackingKey(), tt(6, T0 - 45 * 60));
		Candidate run = chores(p, null, T0).get(0);
		assertEquals("2 seaweed ready", run.getTitle());
		assertTrue(run.getWhy(), run.getWhy().endsWith("1 more ready in 5 min"));
	}

	@Test
	public void treesAndPatchesAreSeparateRuns()
	{
		MapProfile p = new MapProfile();
		// Planted calquat seen two days ago: grown and waiting for its health check.
		p.put(Chores.GROUP, Spot.CALQUAT_KASTORI.timeTrackingKey(), tt(4, T0 - 2 * 86_400));
		p.put(Chores.GROUP, Spot.CALQUAT_TAI_BWO.timeTrackingKey(), tt(4, T0 - 2 * 86_400));
		// Weeds in a seaweed patch and an empty bird house space: left fallow, not nagged.
		p.put(Chores.GROUP, Spot.SEAWEED_N.timeTrackingKey(), tt(3, T0 - 600));
		p.put(Chores.GROUP, Spot.BIRDHOUSE_VALLEY_NE.timeTrackingKey(), tt(0, T0 - 600));
		List<Candidate> list = chores(p, null, T0);
		assertEquals(1, list.size());
		assertEquals("Tree run: 2 ready: calquat", list.get(0).headline());
		assertNull(list.get(0).getBring());
		assertTrue(list.get(0).getUrgency() < 0.2);
	}

	@Test
	public void diseasedPatchIsGoingToWaste()
	{
		MapProfile p = new MapProfile();
		// Diseased seaweed seen two minutes ago: it dies at the next ten-minute tick.
		long seenAt = T0 - 120;
		p.put(Chores.GROUP, Spot.SEAWEED_N.timeTrackingKey(), tt(11, seenAt));
		Candidate run = chores(p, null, T0).get(0);
		assertEquals(Tier.WASTING, run.getTier());
		assertEquals(Growth.tickTime(10, 1, seenAt, Growth.Offset.NONE), run.getDeadline());
		assertTrue(run.getWhy(), run.getWhy().startsWith("Seaweed at Seabed, north is diseased"));

		// Seen diseased an hour ago: past its tick, so it has died; ready to clear but nothing left to save.
		p.put(Chores.GROUP, Spot.SEAWEED_N.timeTrackingKey(), tt(11, T0 - 3600));
		run = chores(p, null, T0).get(0);
		assertEquals(Tier.READY, run.getTier());
		assertTrue(run.getWhy(), run.getWhy().contains("likely died"));

		// Seen diseased three days ago: abandoned.
		p.put(Chores.GROUP, Spot.SEAWEED_N.timeTrackingKey(), tt(11, T0 - 3 * 86_400));
		assertTrue(chores(p, null, T0).isEmpty());
	}

	@Test
	public void saysWhatComesReadyNext()
	{
		MapProfile p = new MapProfile();
		p.put(Chores.GROUP, Spot.BIRDHOUSE_MEADOW_N.timeTrackingKey(), tt(6, T0 - 27 * 60));
		p.put(Chores.GROUP, Spot.SEAWEED_S.timeTrackingKey(), tt(4, T0));
		Advisor.Inputs in = Advisor.Inputs.read(p, GSON, T0, false);
		assertEquals("Bird houses full in 23 min", Advisor.nextUp(in, Settings.defaults(), T0));
		assertNull(Advisor.nextUp(in, Settings.defaults().chores(false), T0));
		assertNull(Advisor.nextUp(new Advisor.Inputs(), Settings.defaults(), T0));
	}

	// ---- Bank Steward ----

	@Test
	public void bankSaysWhatIsMissing()
	{
		MapProfile p = seaweedReady(fullHouses(new MapProfile()));
		Map<Integer, Long> items = new HashMap<>();
		items.put(ItemID.OAK_LOGS, 50L);
		items.put(ItemID.SEAWEED_SEED, 30L);
		BankSnapshot noSeeds = BankSnapshot.of(items, T0 * 1000);
		assertEquals("You're out of hop seeds", chores(p, noSeeds, T0).get(0).getBring());

		items.put(ItemID.BARLEY_SEED, 12L);
		BankSnapshot few = BankSnapshot.of(items, T0 * 1000);
		assertEquals("Only 12 hop seeds banked, 40 needed", chores(p, few, T0).get(0).getBring());
		assertTrue(Bring.isShort(chores(p, few, T0).get(0).getBring()));

		items.put(ItemID.JUTE_SEED, 300L);
		assertEquals("Bring 4 oak logs + 40 hop seeds + 2 seaweed spores", chores(p, BankSnapshot.of(items, 0), T0).get(0).getBring());
	}

	@Test
	public void prebuiltHousesCountAsLogs()
	{
		Map<Integer, Long> items = new HashMap<>();
		items.put(ItemID.BIRDHOUSE_OAK, 4L);
		items.put(ItemID.HAMMERSTONE_HOP_SEED, 100L);
		String line = chores(fullHouses(new MapProfile()), BankSnapshot.of(items, 0), T0).get(0).getBring();
		assertEquals("Bring 4 oak logs + 40 hop seeds", line);
	}

	@Test
	public void bankSnapshotParsesStewardsShape()
	{
		BankSnapshot b = BankSnapshot.parse(GSON, "{\"version\":1,\"savedAt\":1760000000000,\"items\":{\"995\":1234567,\"1511\":40},\"truncated\":false}");
		assertNotNull(b);
		assertEquals(1_234_567, b.count(ItemID.COINS));
		assertEquals(40, b.count(ItemID.LOGS));
		assertEquals(1_760_000_000_000L, b.getSavedAt());
		assertFalse(b.isTruncated());
	}

	@Test
	public void bankSnapshotIsFailSafe()
	{
		String[] bad = {null, "", "  ", "null", "[]", "{", "{\"items\":[1,2]}", "{\"version\":2,\"items\":{\"995\":1}}",
			"{\"version\":\"one\",\"items\":{}}", "\"text\"", "42", "{\"items\":null}", "{\"version\":1}"};
		for (String s : bad)
		{
			assertNull(s, BankSnapshot.parse(GSON, s));
		}
		assertNull(BankSnapshot.parse(null, "{\"items\":{}}"));
		BankSnapshot odd = BankSnapshot.parse(GSON, "{\"items\":{\"995\":-5,\"abc\":3,\"1511\":1e30,\"314\":2.7,\"-4\":9,\"440\":\"x\",\"453\":null},"
			+ "\"truncated\":\"yes\",\"savedAt\":-1}");
		assertNotNull(odd);
		assertEquals(0, odd.count(ItemID.COINS));
		assertEquals(Integer.MAX_VALUE, odd.count(ItemID.LOGS));
		assertEquals(2, odd.count(314));
		assertEquals(2, odd.size());
		assertFalse(odd.isTruncated());
		assertEquals(0, odd.getSavedAt());
	}

	@Test
	public void truncatedBankNeverClaimsSomethingIsMissing()
	{
		BankSnapshot t = BankSnapshot.parse(GSON, "{\"version\":1,\"items\":{\"1521\":4},\"truncated\":true}");
		assertTrue(t.isTruncated());
		String line = chores(fullHouses(new MapProfile()), t, T0).get(0).getBring();
		assertEquals("Bring 4 oak logs + 40 hop seeds", line);
	}

	// ---- Navigator ----

	/** Written by Navigator's own GoalStore and GoalBoard.of (the Minigame Pilot keeps the same bytes). */
	static final String WRITTEN = "{\"version\":1,\"focus\":\"quest:Dragon Slayer II\",\"goals\":[{\"key\":\"quest:Dragon Slayer II\",\"type\":\"quest\","
		+ "\"title\":\"Dragon Slayer II\",\"complete\":false,\"progress\":0.0,\"next\":\"Start Shield of Arrav\",\"hoursLeft\":109.5,"
		+ "\"skills\":{\"Slayer\":18,\"Magic\":75},\"quests\":[\"Family Crest\",\"Shield of Arrav\"]},"
		+ "{\"key\":\"diary:Varrock Hard\",\"type\":\"diary\",\"title\":\"Varrock Hard Diary\",\"complete\":false,\"progress\":0.05,"
		+ "\"next\":\"Start Death Plateau\",\"hoursLeft\":46.5,\"skills\":{\"Farming\":68},\"quests\":[\"Death Plateau\"]},"
		+ "{\"key\":\"skill:Agility 70\",\"type\":\"skill\",\"title\":\"70 Agility\",\"complete\":false,\"progress\":0.8,"
		+ "\"next\":\"Train Agility to 70\",\"hoursLeft\":3.0,\"skills\":{\"Agility\":70},\"quests\":[]},"
		+ "{\"key\":\"quest:Done Deal\",\"type\":\"quest\",\"title\":\"Done Deal\",\"complete\":true,\"progress\":1.0,"
		+ "\"hoursLeft\":0.0,\"skills\":{},\"quests\":[],\"somethingNew\":[1,2]}]}";

	@Test
	public void goalBoardGivesFocusAndOtherGoals()
	{
		GoalBoard b = GoalBoard.parse(GSON, WRITTEN);
		assertNotNull(b);
		assertEquals(4, b.getGoals().size());
		assertEquals(3, b.open());
		List<Candidate> list = b.candidates();
		assertEquals(3, list.size());
		Candidate focus = list.get(0);
		assertEquals(Tier.FOCUS, focus.getTier());
		assertEquals("Toward Dragon Slayer II: Start Shield of Arrav", focus.headline());
		assertEquals("Your focus in Codex Navigator, 0% done, about 110 hr to go", focus.getWhy());
		assertEquals(60, focus.getMinutes());
		assertEquals(Tier.GOAL, list.get(1).getTier());

		Plan plan = Ranker.rank(list, Settings.defaults(), new Session(), T0);
		assertEquals(Arrays.asList("goal:quest:Dragon Slayer II", "goal:skill:Agility 70", "goal:diary:Varrock Hard"), keys(plan));
	}

	@Test
	public void goalBoardIsFailSafe()
	{
		String[] none = {null, "", "null", "[]", "{", "\"x\"", "{\"version\":2,\"goals\":[]}", "{\"version\":7,\"focus\":\"a\"}",
			"{\"version\":1,\"goals\":{\"a\":1}}", "{\"version\":1,\"goals\":\"many\"}", "{\"version\":\"2\",\"goals\":[]}"};
		for (String s : none)
		{
			assertNull(s, GoalBoard.parse(GSON, s));
		}
		GoalBoard empty = GoalBoard.parse(GSON, "{\"version\":1,\"goals\":[]}");
		assertNotNull(empty);
		assertTrue(empty.candidates().isEmpty());
		GoalBoard odd = GoalBoard.parse(GSON, "{\"goals\":[null,1,\"x\",{\"title\":\"No key\",\"next\":\"a\"},"
			+ "{\"key\":\"k\",\"title\":\"T\",\"next\":\"  \"},"
			+ "{\"key\":\"q\",\"title\":\"Quest cape\",\"next\":\"start Monkey Madness II\",\"progress\":7,\"hoursLeft\":-4,\"complete\":\"no\"}]}");
		assertNotNull(odd);
		assertEquals(2, odd.getGoals().size());
		List<Candidate> list = odd.candidates();
		assertEquals(1, list.size());
		assertEquals("Toward Quest cape: Start Monkey Madness II", list.get(0).headline());
		assertEquals(Tier.GOAL, list.get(0).getTier());
		assertEquals("Pinned in Codex Navigator, 100% done", list.get(0).getWhy());
	}

	// ---- Kingdom ----

	static MapProfile kingdom(MapProfile p, Instant changed, int approval, int coffer)
	{
		return p.put(KingdomWatch.GROUP, "lastChanged", changed.toString())
			.put(KingdomWatch.GROUP, "approval", String.valueOf(approval))
			.put(KingdomWatch.GROUP, "coffer", String.valueOf(coffer));
	}

	@Test
	public void kingdomBelowTargetIsAStepAndWastesNearTheReset()
	{
		// Seen today at 60% approval with 200k in the coffer.
		MapProfile p = kingdom(new MapProfile(), Instant.ofEpochSecond(T0 - 3600), 76, 200_000);
		Kingdom k = KingdomWatch.read(p, T0, false);
		assertNotNull(k);
		assertEquals(59, k.approvalPercent());
		Candidate c = KingdomWatch.candidate(k, Settings.defaults(), null, T0);
		assertEquals(Tier.READY, c.getTier());
		assertEquals("59% approval, top up 300k", c.getTitle());
		assertEquals("Bring 300k coins", c.getBring());

		long late = RESET - 2 * 3600;
		Candidate soon = KingdomWatch.candidate(KingdomWatch.read(p, late, false), Settings.defaults(), null, late);
		assertEquals(Tier.WASTING, soon.getTier());
		assertEquals(RESET, soon.getDeadline());
		assertTrue(soon.getWhy(), soon.getWhy().contains("next in 2 hr"));

		BankSnapshot broke = BankSnapshot.of(Collections.singletonMap(ItemID.COINS, 1000L), 0);
		assertEquals("Only 1,000 coins banked, 300k needed", KingdomWatch.candidate(k, Settings.defaults(), broke, T0).getBring());
	}

	@Test
	public void kingdomCarriesForwardAndStaysQuietWhenFine()
	{
		MapProfile fine = kingdom(new MapProfile(), Instant.ofEpochSecond(T0 - 60), 127, 7_500_000);
		assertNull(KingdomWatch.candidate(KingdomWatch.read(fine, T0, true), Settings.defaults(), null, T0));
		// Ten days later approval has fallen 1% a day after Royal Trouble.
		Kingdom later = KingdomWatch.read(fine, T0 + 10 * 86_400, true);
		assertTrue(later.isEstimated());
		assertEquals(90, later.approvalPercent());
	}

	@Test
	public void kingdomIsFailSafe()
	{
		Instant at = Instant.ofEpochSecond(T0 - 60);
		assertNull(KingdomWatch.read(new MapProfile(), T0, false));
		assertNull(KingdomWatch.read(kingdom(new MapProfile(), at, 100, 0).put(KingdomWatch.GROUP, "lastChanged", "yesterday"), T0, false));
		assertNull(KingdomWatch.read(kingdom(new MapProfile(), at, 100, 0).put(KingdomWatch.GROUP, "approval", "lots"), T0, false));
		assertNull(KingdomWatch.read(kingdom(new MapProfile(), Instant.ofEpochSecond(T0 + 5 * 86_400), 100, 0), T0, false));
		Kingdom wild = KingdomWatch.read(kingdom(new MapProfile(), at, 9000, -50), T0, false);
		assertEquals(100, wild.approvalPercent());
		assertEquals(0, wild.getCoffer());
		assertNull(KingdomWatch.candidate(null, Settings.defaults(), null, T0));
	}

	// ---- Dailies ----

	static Map<Integer, Integer> diaries()
	{
		Map<Integer, Integer> bits = new HashMap<>();
		bits.put(VarbitID.VARROCK_DIARY_EASY_COMPLETE, 1);
		bits.put(VarbitID.KANDARIN_DIARY_EASY_COMPLETE, 1);
		bits.put(VarbitID.WESTERN_DIARY_EASY_COMPLETE, 1);
		return bits;
	}

	@Test
	public void dailiesGroupAndWasteBeforeTheReset()
	{
		List<DailyState> states = Dailies.read(vars(diaries()));
		List<Candidate> list = Dailies.candidates(states, Settings.defaults(), null, T0);
		assertEquals(1, list.size());
		Candidate d = list.get(0);
		assertEquals("Diary dailies: 3 to collect", d.headline());
		assertEquals(Tier.READY, d.getTier());
		assertEquals("Bring 105k coins + 30 flax", d.getBring());
		assertTrue(d.getWhy(), d.getWhy().startsWith("Not collected yet; the day resets in 15 hr"));
		assertTrue(d.getWhy(), d.getWhy().endsWith("Zaff's battlestaves, Flax to bowstrings, Rantz's ogre arrows"));

		long late = RESET - 40 * 60;
		Candidate soon = Dailies.candidates(states, Settings.defaults(), null, late).get(0);
		assertEquals(Tier.WASTING, soon.getTier());
		assertTrue(soon.getWhy(), soon.getWhy().startsWith("Lost at the reset in 40 min"));

		BankSnapshot noFlax = BankSnapshot.of(Collections.singletonMap(ItemID.COINS, 5_000_000L), 0);
		assertEquals("You're out of flax", Dailies.candidates(states, Settings.defaults(), noFlax, T0).get(0).getBring());
	}

	@Test
	public void claimedDailiesAndLockedOnesAddNothing()
	{
		Map<Integer, Integer> bits = diaries();
		bits.put(VarbitID.ZAFF_LAST_CLAIMED, 1);
		bits.put(VarbitID.SEERS_FREE_FLAX, 1);
		bits.put(VarbitID.WESTERN_RANTZ_ARROWS, 1);
		assertTrue(Dailies.candidates(Dailies.read(vars(bits)), Settings.defaults(), null, T0).isEmpty());
		assertTrue(Dailies.candidates(Dailies.read(vars(new HashMap<>())), Settings.defaults(), null, T0).isEmpty());
		assertTrue(Dailies.candidates(null, Settings.defaults(), null, T0).isEmpty());
	}

	@Test
	public void finishedFarmingContractIsItsOwnStep()
	{
		Map<Integer, Integer> bits = new HashMap<>();
		bits.put(VarbitID.FARMGUILD_CONTRACT_TYPE, 24);
		bits.put(VarbitID.FARMGUILD_CONTRACT_COMPLETE, 1);
		List<Candidate> list = Dailies.candidates(Dailies.read(vars(bits)), Settings.defaults(), null, T0);
		assertEquals(1, list.size());
		assertEquals(Candidate.Kind.CONTRACT, list.get(0).getKind());
		assertEquals(Tier.READY, list.get(0).getTier());
		assertEquals(0, list.get(0).getDeadline());
	}

	// ---- Ranking ----

	@Test
	public void bandsComeInOrder()
	{
		List<Candidate> list = Arrays.asList(
			c("goal", Tier.GOAL, 0.5, 30, 0),
			c("focus", Tier.FOCUS, 0.01, 60, 0),
			c("ready", Tier.READY, 0.1, 9, 0),
			c("waste", Tier.WASTING, 0.1, 5, T0 + 600));
		assertEquals(Arrays.asList("waste", "ready", "focus", "goal"), keys(Ranker.rank(list, Settings.defaults(), new Session(), T0)));
	}

	@Test
	public void soonestLossFirstThenBestValuePerMinute()
	{
		List<Candidate> list = Arrays.asList(
			c("later", Tier.WASTING, 9, 5, T0 + 7200),
			c("sooner", Tier.WASTING, 0.1, 5, T0 + 600),
			c("slowTrees", Tier.READY, 0.3, 9, 0),
			c("birdhouses", Tier.READY, 7.8, 11, 0),
			c("kingdom", Tier.READY, 0.4, 6, 0));
		assertEquals(Arrays.asList("sooner", "later", "birdhouses", "kingdom", "slowTrees"),
			keys(Ranker.rank(list, Settings.defaults(), new Session(), T0)));
	}

	@Test
	public void goalFirstLetsTheFocusPassSlowChores()
	{
		List<Candidate> list = Arrays.asList(
			c("trees", Tier.READY, 0.2, 9, 0),
			c("birdhouses", Tier.READY, 4.8, 6, 0),
			c("focus", Tier.FOCUS, 0.01, 60, 0),
			c("waste", Tier.WASTING, 0.1, 5, T0 + 600));
		Settings s = Settings.defaults().goalFirst(true);
		assertEquals(Arrays.asList("waste", "birdhouses", "focus", "trees"), keys(Ranker.rank(list, s, new Session(), T0)));
	}

	@Test
	public void quietModeKeepsOnlyWhatIsGoingToWaste()
	{
		List<Candidate> list = Arrays.asList(c("ready", Tier.READY, 3, 9, 0), c("waste", Tier.WASTING, 0.1, 5, T0 + 600),
			c("focus", Tier.FOCUS, 0.1, 30, 0));
		Plan p = Ranker.rank(list, Settings.defaults().quiet(true), new Session(), T0);
		assertEquals(Collections.singletonList("waste"), keys(p));
		assertEquals(2, p.getQuiet());
	}

	@Test
	public void skipSinksAndSnoozeHidesForAnHour()
	{
		List<Candidate> list = Arrays.asList(c("a", Tier.WASTING, 1, 5, T0 + 600), c("b", Tier.READY, 3, 9, 0),
			c("c", Tier.FOCUS, 0.1, 30, 0), c("d", Tier.GOAL, 0.1, 30, 0));
		Session session = new Session();
		session.skip("a");
		session.skip("c");
		assertEquals(Arrays.asList("b", "d", "a", "c"), keys(Ranker.rank(list, Settings.defaults(), session, T0)));

		session.snooze("b", T0);
		Plan p = Ranker.rank(list, Settings.defaults(), session, T0 + 3599);
		assertEquals(Arrays.asList("d", "a", "c"), keys(p));
		assertEquals(1, p.getSnoozed());
		assertEquals("b", Ranker.rank(list, Settings.defaults(), session, T0 + 3600).top().getKey());

		session.clear();
		assertTrue(session.isEmpty());
		assertEquals("a", Ranker.rank(list, Settings.defaults(), session, T0).top().getKey());
	}

	@Test
	public void planGivesTopAndNextFive()
	{
		List<Candidate> list = new ArrayList<>();
		for (int i = 0; i < 9; i++)
		{
			list.add(c("s" + i, Tier.READY, 9 - i, 5, 0));
		}
		Plan p = Ranker.rank(list, Settings.defaults(), null, T0);
		assertEquals("s0", p.top().getKey());
		assertEquals(5, p.after(5).size());
		assertEquals("s1", p.after(5).get(0).getKey());
		assertNull(Plan.EMPTY.top());
		assertTrue(Plan.EMPTY.after(5).isEmpty());
	}

	// ---- Live context ----

	@Test
	public void minigamesOtherPilotsCoverAreDetected()
	{
		assertEquals(Activity.MOTHERLODE, Activity.of(14936, null));
		assertEquals(Activity.BLAST_FURNACE, Activity.of(7757, Collections.emptyList()));
		assertEquals(Activity.MAHOGANY_HOMES, Activity.of(11828, Arrays.asList("Guard", null, "Amy")));
		assertEquals(Activity.NONE, Activity.of(12850, Arrays.asList("Hans")));
		assertEquals(Activity.NONE, Activity.of(-1, null));
	}

	// ---- End to end ----

	@Test
	public void wholeAccountInOneOrder()
	{
		MapProfile p = seaweedReady(fullHouses(new MapProfile()));
		p.put(Chores.GROUP, Spot.CALQUAT_KASTORI.timeTrackingKey(), tt(4, T0 - 2 * 86_400));
		kingdom(p, Instant.ofEpochSecond(T0 - 3600), 76, 200_000);
		p.put(GoalBoard.GROUP, GoalBoard.KEY, WRITTEN);
		p.put(BankSnapshot.GROUP, BankSnapshot.KEY, "{\"version\":1,\"savedAt\":1,\"items\":{\"1521\":20,\"5305\":200,\"21490\":5},\"truncated\":false}");
		long late = RESET - 2 * 3600;
		Advisor.Inputs in = Advisor.Inputs.read(p, GSON, late, false);
		in.dailies = Dailies.read(vars(diaries()));
		Plan plan = Advisor.plan(in, Settings.defaults(), new Session(), late);
		List<String> order = keys(plan);
		// Two hours before the reset the dailies and the kingdom are about to lose value; the
		// bird houses and seaweed are the best ready trip; the calquat waits; then the goals.
		assertEquals(Arrays.asList("dailies", "kingdom", "chores:birdhouse_run", "chores:tree_run",
			"goal:quest:Dragon Slayer II", "goal:skill:Agility 70", "goal:diary:Varrock Hard"), order);
		assertEquals("Bring 4 oak logs + 40 hop seeds + 2 seaweed spores", plan.getSteps().get(2).getBring());

		// Sources switched off add nothing.
		Settings off = Settings.defaults().chores(false).kingdom(false).dailies(false).navigator(false);
		assertTrue(Advisor.plan(in, off, new Session(), late).isEmpty());
		// Without Bank Steward the line is a plain list.
		Settings noBank = Settings.defaults().bankSteward(false);
		assertEquals("Bring 105k coins + 30 flax", Advisor.plan(in, noBank, new Session(), late).top().getBring());
	}

	@Test
	public void nothingKnownIsAnEmptyPlan()
	{
		Advisor.Inputs in = Advisor.Inputs.read(new MapProfile(), GSON, T0, false);
		assertTrue(Advisor.plan(in, Settings.defaults(), new Session(), T0).isEmpty());
		Advisor.Inputs nulls = new Advisor.Inputs();
		nulls.seen = null;
		nulls.offset = null;
		assertTrue(Advisor.plan(nulls, Settings.defaults(), null, T0).isEmpty());
	}
}
