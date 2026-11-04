package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The Hunter Guild's rumours as Autopilot reads and plans them. The facts are pinned to their
 * sources: the OSRS wiki ("Hunters' Rumours": tiers, levels, who gives what, drop rates and pity,
 * the reward and milestones; the guild hunters' transcripts and "Basic quetzal whistle" for the
 * lines read) and RuneLite (gameval NpcID.HG_*, the world map's hunter areas).
 */
public class RumourTest
{
	static final Gson GSON = new Gson();
	static final long NOW_MS = AutopilotCoreTest.T0 * 1000;
	static final String CERVUS = "Guild Hunter Cervus (Adept)|";
	static final String TECO = "Guild Hunter Teco (Expert)|";

	// ---- The facts ----

	@Test
	public void guildHuntersAndTiersAreTheWikis()
	{
		assertEquals(46, Rumour.Tier.NOVICE.getLevel());
		assertEquals(57, Rumour.Tier.ADEPT.getLevel());
		assertEquals(72, Rumour.Tier.EXPERT.getLevel());
		assertEquals(91, Rumour.Tier.MASTER.getLevel());
		assertEquals(Rumour.Tier.NOVICE, Rumour.Master.GILMAN.getTier());
		assertEquals(Rumour.Tier.ADEPT, Rumour.Master.ORNUS.getTier());
		assertEquals(Rumour.Tier.ADEPT, Rumour.Master.CERVUS.getTier());
		assertEquals(Rumour.Tier.EXPERT, Rumour.Master.ACO.getTier());
		assertEquals(Rumour.Tier.EXPERT, Rumour.Master.TECO.getTier());
		assertEquals(Rumour.Tier.MASTER, Rumour.Master.WOLF.getTier());
		// RuneLite gameval NpcID: HG_GILMAN 13121 ... HG_WOLF 13126.
		assertEquals(13121, Rumour.Master.GILMAN.getNpcId());
		assertEquals(NpcID.HG_WOLF, Rumour.Master.WOLF.getNpcId());
		assertEquals(13126, Rumour.Master.WOLF.getNpcId());
		// Experience = (Hunter level + 5) x 50, 50, 55 or 60.
		assertEquals(4_345, Rumour.Tier.EXPERT.experience(74));
		assertEquals(5_100, Rumour.Tier.MASTER.experience(80));
		assertEquals(3_100, Rumour.Tier.NOVICE.experience(57));
	}

	@Test
	public void eachGuildHunterGivesTheWikisCreatures()
	{
		assertEquals(29, Rumour.Creature.values().length);
		for (Rumour.Creature c : Rumour.Creature.values())
		{
			assertTrue("Gilman gives every rumour: " + c, c.getMasters().contains(Rumour.Master.GILMAN));
		}
		assertEquals(names("SWAMP_LIZARD HORNED_GRAAHK SPOTTED_KEBBIT BLACK_WARLOCK ORANGE_SALAMANDER RAZOR_BACKED_KEBBIT "
			+ "SABRE_TOOTHED_KEBBIT GREY_CHINCHOMPA DARK_KEBBIT PYRE_FOX WYRMSCRAIG_GOAT RED_CHINCHOMPA SUNLIGHT_MOTH"), given(Rumour.Master.CERVUS));
		assertEquals(names("SPINED_LARUPIA SNOWY_KNIGHT EMBERTAILED_JERBOA SPOTTED_KEBBIT ORANGE_SALAMANDER SABRE_TOOTHED_KEBBIT "
			+ "SABRE_TOOTHED_KYATT PYRE_FOX RED_SALAMANDER RED_CHINCHOMPA"), given(Rumour.Master.ORNUS));
		assertEquals(names("ORANGE_SALAMANDER SABRE_TOOTHED_KEBBIT GREY_CHINCHOMPA SABRE_TOOTHED_KYATT DARK_KEBBIT RED_SALAMANDER "
			+ "RED_CHINCHOMPA DASHING_KEBBIT SUNLIGHT_ANTELOPE TECU_SALAMANDER MOONLIGHT_MOTH"), given(Rumour.Master.ACO));
		assertEquals(names("SABRE_TOOTHED_KEBBIT GREY_CHINCHOMPA SABRE_TOOTHED_KYATT DARK_KEBBIT RED_SALAMANDER WYRMSCRAIG_GOAT "
			+ "RED_CHINCHOMPA DASHING_KEBBIT SUNLIGHT_ANTELOPE SUNLIGHT_MOTH HERBIBOAR"), given(Rumour.Master.TECO));
		assertEquals(names("RED_SALAMANDER RED_CHINCHOMPA DASHING_KEBBIT SUNLIGHT_ANTELOPE TECU_SALAMANDER HERBIBOAR "
			+ "MOONLIGHT_MOTH MOONLIGHT_ANTELOPE"), given(Rumour.Master.WOLF));
		// Levels the rumours are given at, where Hunter Rumours' own table differs (kyatt 53, moonlight moth 75).
		assertEquals(55, Rumour.Creature.SABRE_TOOTHED_KYATT.getLevel());
		assertEquals(85, Rumour.Creature.MOONLIGHT_MOTH.getLevel());
		assertEquals(75, Rumour.Creature.SUNLIGHT_MOTH.getLevel());
		assertEquals(91, Rumour.Creature.MOONLIGHT_ANTELOPE.getLevel());
	}

	@Test
	public void rarePartOddsAreTheWikis()
	{
		assertEquals(50, Rumour.Method.BOX_TRAP.getOneIn());
		assertEquals(100, Rumour.Method.BOX_TRAP.getPity());
		assertEquals(94, Rumour.Method.BOX_TRAP.getPityOutfit());
		assertEquals(20, Rumour.Method.FALCONRY.getPity());
		assertEquals(96, Rumour.Method.GOAT_PIT.getPity());
		assertEquals(14, Rumour.Method.HERBIBOAR.getPity());
		assertEquals(30, Rumour.Method.SPIKED_PIT.getPity());
		assertEquals(50, Rumour.Method.NET_TRAP.getPity());
	}

	/** Every hunting place is one of RuneLite's own world map hunter areas, for that creature. */
	@Test
	public void placesAreRuneLitesHunterAreas() throws Exception
	{
		Class<?> areas = Class.forName("net.runelite.client.plugins.worldmap.HunterAreaLocation");
		Method location = areas.getDeclaredMethod("getLocation");
		Method tooltip = areas.getDeclaredMethod("getTooltip");
		location.setAccessible(true);
		tooltip.setAccessible(true);
		Map<WorldPoint, String> byPoint = new HashMap<>();
		for (Object a : areas.getEnumConstants())
		{
			byPoint.put((WorldPoint) location.invoke(a), ((String) tooltip.invoke(a)).toLowerCase());
		}
		for (Rumour.Creature c : Rumour.Creature.values())
		{
			for (Tile t : c.getTiles())
			{
				if (t.equals(Rumour.CAVERNS))
				{
					// Below the guild: reached by the Hunter Guild Caverns entrance (worldmap/DungeonLocation).
					continue;
				}
				String there = byPoint.get(new WorldPoint(t.getX(), t.getY(), t.getPlane()));
				assertNotNull(c + " at " + t + " is a RuneLite hunter area", there);
				String name = c == Rumour.Creature.RED_CHINCHOMPA ? "carnivorous chinchompa"
					: c == Rumour.Creature.GREY_CHINCHOMPA ? "chinchompa" : c.getName().toLowerCase();
				assertTrue(c + " is hunted at " + t + ": " + there, there.contains(name));
			}
		}
	}

	// ---- Reading what the game says ----

	@Test
	public void aGuildHunterGivesARumour()
	{
		RumourBook b = RumourBook.empty();
		assertTrue(b.read(true, CERVUS + "There's been rumours of a rare grey chinchompa coming in.", NOW_MS));
		assertEquals(Rumour.Master.CERVUS, b.getMaster());
		assertEquals(Rumour.Creature.GREY_CHINCHOMPA, b.getCreature());
		assertFalse(b.hasPart());
		// Asked again: the same rumour, nothing new.
		assertFalse(b.read(true, CERVUS + "You're hunting for me at the moment. This rumour was about a grey chinchompa.", NOW_MS + 1000));
	}

	@Test
	public void anotherHuntersRumourIsTheirs()
	{
		RumourBook b = RumourBook.empty();
		b.read(true, CERVUS + "I hear you're hunting for Teco.", NOW_MS);
		assertNull(b.getCreature());
		assertTrue(b.read(true, CERVUS + "Aren't you looking for a red salamander at the moment?", NOW_MS));
		assertEquals(Rumour.Master.TECO, b.getMaster());
		assertEquals(Rumour.Creature.RED_SALAMANDER, b.getCreature());
	}

	@Test
	public void theWhistleTellsTheRumourAndItsSource()
	{
		RumourBook b = RumourBook.empty();
		assertTrue(b.read(false, "Your current rumour target is a grey chinchompa. You'll need to bring back some of its fur."
			+ " Cervus was the source of the rumour.", NOW_MS));
		assertEquals(Rumour.Master.CERVUS, b.getMaster());
		assertEquals(Rumour.Creature.GREY_CHINCHOMPA, b.getCreature());
	}

	@Test
	public void longestNameWinsAndAliasesRead()
	{
		assertEquals(Rumour.Creature.SABRE_TOOTHED_KYATT, Rumour.Creature.named("a strange sabre-toothed kyatt"));
		assertEquals(Rumour.Creature.SABRE_TOOTHED_KEBBIT, Rumour.Creature.named("a rare sabre-toothed kebbit"));
		assertEquals(Rumour.Creature.RED_CHINCHOMPA, Rumour.Creature.named("rumours of a rare carnivorous chinchompa"));
		assertEquals(Rumour.Creature.MOONLIGHT_ANTELOPE, Rumour.Creature.named("the local moonlight antelope population"));
		// "Aco" is a word of its own, never the inside of another.
		assertNull(Rumour.Master.named("taco tuesday"));
		assertEquals(Rumour.Master.ACO, Rumour.Master.named("aco was the source"));
	}

	@Test
	public void theRarePartThenTheHandIn()
	{
		RumourBook b = RumourBook.empty();
		b.read(true, TECO + "I've heard rumours of a strange herbiboar that you could check out for me.", NOW_MS);
		b.hunted(25 * 60);
		assertTrue(b.read(false, "<col=ef1020>You find a rare piece of the creature! You should take it back to the Hunter Guild.</col>", NOW_MS));
		assertTrue(b.hasPart());
		// Hunting time stops with the part.
		b.hunted(600);
		assertTrue(b.read(true, TECO + "Excellent! Thanks for getting that done. Here's your reward.", NOW_MS));
		assertNull(b.getCreature());
		assertTrue(b.read(false, "You have completed <col=ff0000>12</col> rumours for the Hunter Guild.", NOW_MS));
		assertEquals(12, b.getDone());
		assertFalse("one timed rumour is not yet a pace", b.minutes(Rumour.Method.HERBIBOAR) > 0);
	}

	@Test
	public void atFirstLightIsNoRumour()
	{
		RumourBook b = RumourBook.empty();
		assertFalse(b.read(true, "Guild Hunter Wolf (Master)|I stopped off for a bit of hunting first, a pyre fox.", NOW_MS));
		assertNull(b.getCreature());
		// Only the guild hunters are read.
		assertFalse(b.read(true, "Wolf|A grey chinchompa ran past.", NOW_MS));
	}

	@Test
	public void ownPaceAfterThreeRumours()
	{
		RumourBook b = RumourBook.empty();
		for (int m : new int[]{20, 40, 26})
		{
			b.read(true, CERVUS + "There's been rumours of a rare grey chinchompa coming in.", NOW_MS);
			b.hunted(m * 60L);
			b.read(false, "You find a rare piece of the creature! You should take it back to the Hunter Guild.", NOW_MS);
			b.read(true, CERVUS + "Thanks for that. I'll mark off that report for you. Would you like another rumour?", NOW_MS);
		}
		assertEquals(26, b.minutes(Rumour.Method.BOX_TRAP));
		b.read(true, CERVUS + "There's been rumours of a rare grey chinchompa coming in.", NOW_MS);
		Candidate c = Rumours.candidates(b).get(0);
		assertTrue(c.isPaced());
		assertEquals(26, c.getMinutes());
	}

	@Test
	public void savedAndReadBack()
	{
		RumourBook b = RumourBook.empty();
		b.read(true, CERVUS + "There's been rumours of a rare grey chinchompa coming in.", NOW_MS);
		b.read(false, "You have completed 49 rumours for the Hunter Guild.", NOW_MS);
		b.account(74, false);
		b.learn(Rumour.Method.BOX_TRAP, 22);
		RumourBook back = RumourBook.parse(GSON, b.toJson(GSON));
		assertEquals(Rumour.Master.CERVUS, back.getMaster());
		assertEquals(Rumour.Creature.GREY_CHINCHOMPA, back.getCreature());
		assertEquals(49, back.getDone());
		assertEquals(74, back.getLevel());
		assertEquals(b.toJson(GSON), back.toJson(GSON));
		// Damaged or from a newer version: an empty book, never a throw.
		assertNull(RumourBook.parse(GSON, "{\"version\":1,\"creature\":").getCreature());
		assertNull(RumourBook.parse(GSON, "{\"version\":9,\"creature\":\"PYRE_FOX\"}").getCreature());
		assertNull(RumourBook.parse(GSON, "{\"version\":1,\"creature\":\"DODO\"}").getCreature());
	}

	@Test
	public void hunterRumoursPluginsSaveFillsIn()
	{
		Map<String, String> saved = new HashMap<>();
		Profile p = (g, k) -> saved.get(g + "." + k);
		assertNull(RumourBook.fromHunterRumours(p));
		// Hunter Rumours saves its enums by name: current.hunter, hunter.<npc id>, current.rumour.finished.
		saved.put("hunterrumours.current.hunter", "EXPERT_TECO");
		saved.put("hunterrumours.hunter.13125", "RED_CHINCHOMPA_2");
		saved.put("hunterrumours.current.rumour.finished", "true");
		RumourBook theirs = RumourBook.fromHunterRumours(p);
		assertEquals(Rumour.Master.TECO, theirs.getMaster());
		assertEquals(Rumour.Creature.RED_CHINCHOMPA, theirs.getCreature());
		assertTrue(theirs.hasPart());

		RumourBook own = RumourBook.empty();
		own.account(80, false);
		RumourBook plan = own.orElse(theirs);
		assertEquals(Rumour.Creature.RED_CHINCHOMPA, plan.getCreature());
		assertEquals(80, plan.getLevel());
		// Once Autopilot has read the game itself, its own reading stands.
		own.read(true, CERVUS + "There's been rumours of a rare pyre fox coming in.", NOW_MS);
		assertEquals(Rumour.Creature.PYRE_FOX, own.orElse(theirs).getCreature());

		saved.put("hunterrumours.hunter.13125", "NONE");
		assertNull(RumourBook.fromHunterRumours(p).getCreature());
	}

	// ---- The steps ----

	@Test
	public void finishTheRumourWhereItIsHunted()
	{
		RumourBook b = RumourBook.empty();
		b.read(true, CERVUS + "There's been rumours of a rare grey chinchompa coming in.", NOW_MS);
		Candidate c = Rumours.candidates(b).get(0);
		assertEquals("Hunter rumour: Grey chinchompa for Cervus", c.headline());
		assertEquals(Tier.GOAL, c.getTier());
		assertEquals("Box traps at Piscatoris, Kourend Woodland or Isle of Souls; the part is 1 in 50 a catch, sure by 100", c.getWhy());
		assertEquals("Bring box traps", c.getBring());
		assertEquals(Rumours.ESTIMATE_MINUTES, c.getMinutes());
		assertFalse(c.isPaced());
		assertEquals(3, c.getTargets().size());
	}

	@Test
	public void handInTheRarePart()
	{
		RumourBook b = RumourBook.empty();
		b.account(74, false);
		b.read(false, "You have completed 49 rumours for the Hunter Guild.", NOW_MS);
		b.read(true, TECO + "There's been rumours of a rare dashing kebbit coming in.", NOW_MS);
		b.read(false, "You find a rare piece of the creature! You should take it back to the Hunter Guild.", NOW_MS);
		Candidate c = Rumours.candidates(b).get(0);
		assertEquals("Rumour done: Take the part back to Teco", c.headline());
		assertEquals(Tier.READY, c.getTier());
		assertEquals("Teco pays 4,345 Hunter xp and an expert loot sack; your 50th unlocks cooking the antelopes and dashing kebbit", c.getWhy());
		assertEquals(Arrays.asList(Rumour.GUILD), c.getTargets());
	}

	@Test
	public void aNewRumourOnlyForThoseWhoDoThem()
	{
		RumourBook b = RumourBook.empty();
		b.account(74, false);
		assertTrue("never seen doing rumours", Rumours.candidates(b).isEmpty());
		b.read(false, "You have completed 7 rumours for the Hunter Guild.", NOW_MS);
		Candidate c = Rumours.candidates(b).get(0);
		assertEquals("New rumour from Aco or Teco", c.getTitle());
		assertEquals("At 74 Hunter, expert rumours pay 4,345 Hunter xp and an expert loot sack; 3 more to 10:"
			+ " a quetzal whistle blueprint from Soar Leader Pitri", c.getWhy());
		// Master rumours need At First Light as well as 91 Hunter.
		b.account(92, false);
		assertEquals("New rumour from Aco or Teco", Rumours.candidates(b).get(0).getTitle());
		b.account(92, true);
		assertEquals("New rumour from Wolf", Rumours.candidates(b).get(0).getTitle());
		b.account(50, false);
		assertEquals("New rumour from Huntmaster Gilman", Rumours.candidates(b).get(0).getTitle());
		b.account(45, false);
		assertTrue("below 46 the guild gives none", Rumours.candidates(b).isEmpty());
	}

	@Test
	public void rankedWithTheGoalsAndPricedByWayfinder()
	{
		RumourBook b = RumourBook.empty();
		b.read(true, CERVUS + "There's been rumours of a rare grey chinchompa coming in.", NOW_MS);
		Advisor.Inputs in = new Advisor.Inputs();
		in.rumour = b;
		// A made-up teleport landing 30 tiles from the Kourend Woodland's chinchompas: the hunt is priced to the nearest area.
		in.atlas = Atlas.parse(GSON, "{\"version\":1,\"walkSecondsPerTile\":0.3,\"teleports\":[{\"name\":\"Test landing\",\"source\":"
			+ "\"Test teleport\",\"kind\":\"ITEM\",\"x\":1481,\"y\":3474,\"plane\":0,\"seconds\":4,\"carried\":true,\"timed\":false}]}");
		List<Candidate> all = Advisor.candidates(in, Settings.defaults(), AutopilotCoreTest.T0);
		assertEquals(1, all.size());
		Candidate c = all.get(0);
		assertNotNull(c.getTrip());
		assertEquals("Test teleport", c.getTrip().getVia());
		// Off: no step.
		assertTrue(Advisor.candidates(in, Settings.defaults().rumours(false), AutopilotCoreTest.T0).isEmpty());
		// A ready daily still comes first; the rumour sits with the goals.
		Candidate daily = new Candidate("dailies", Candidate.Kind.DAILIES, Tier.READY, "Diary dailies", "Zaff", "", null, 5, 0.2, 0, 1, -1);
		List<Candidate> both = new ArrayList<>(all);
		both.add(daily);
		Plan plan = Ranker.rank(both, Settings.defaults(), null, AutopilotCoreTest.T0);
		assertEquals("dailies", plan.top().getKey());
		assertEquals(Rumours.HUNT, plan.getSteps().get(1).getKey());
	}

	@Test
	public void fromThePluginIsSaidSo()
	{
		Advisor.Inputs in = new Advisor.Inputs();
		Map<String, String> saved = new HashMap<>();
		saved.put("hunterrumours.current.hunter", "ADEPT_ORNUS");
		saved.put("hunterrumours.hunter.13122", "PYRE_FOX");
		in.rumour = RumourBook.empty().orElse(RumourBook.fromHunterRumours((g, k) -> saved.get(g + "." + k)));
		in.rumourFromPlugin = true;
		List<Candidate> all = Advisor.candidates(in, Settings.defaults(), AutopilotCoreTest.T0);
		assertTrue(all.get(0).getWhy().endsWith(Advisor.SEEN_BY_HUNTER_RUMOURS));
		assertTrue(Advisor.fedBy(in, Settings.defaults(), all).contains("Hunter Rumours"));
	}

	private static EnumSet<Rumour.Creature> names(String list)
	{
		EnumSet<Rumour.Creature> out = EnumSet.noneOf(Rumour.Creature.class);
		for (String n : list.trim().split("\\s+"))
		{
			out.add(Rumour.Creature.valueOf(n));
		}
		return out;
	}

	private static EnumSet<Rumour.Creature> given(Rumour.Master m)
	{
		EnumSet<Rumour.Creature> out = EnumSet.noneOf(Rumour.Creature.class);
		for (Rumour.Creature c : Rumour.Creature.values())
		{
			if (c.getMasters().contains(m))
			{
				out.add(c);
			}
		}
		return out;
	}
}
