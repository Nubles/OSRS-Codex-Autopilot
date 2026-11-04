package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

/**
 * What Autopilot knows about the Hunter Guild's rumours, every fact from the OSRS wiki ("Hunters'
 * Rumours", "Hunter Guild", the creature and trap pages) or RuneLite's own source:
 *
 * <ul>
 * <li>The guild hunters and their tiers: Huntmaster Gilman (Novice, 46 Hunter, can give every
 * rumour), Guild Hunters Ornus and Cervus (Adept, 57), Aco and Teco (Expert, 72) and Wolf
 * (Master, 91 and At First Light). NPC ids are RuneLite's gameval NpcID.HG_*.</li>
 * <li>The creatures, the Hunter level each rumour is given at, how it is hunted, and which guild
 * hunters give it: the wiki's rumours table.</li>
 * <li>The rare part: a base chance per method and a pity count after which it is certain (lower in
 * the guild hunter outfit): the wiki's drop rate table.</li>
 * <li>The reward: (Hunter level + 5) times 50 (Novice, Adept), 55 (Expert) or 60 (Master) Hunter
 * experience, and a loot sack of the rumour's tier.</li>
 * <li>Where each creature is hunted: RuneLite's world map hunter areas
 * (worldmap/HunterAreaLocation); the moonlight creatures live below the guild, so they are
 * reached by the Hunter Guild Caverns entrance (worldmap/DungeonLocation).</li>
 * </ul>
 */
public final class Rumour
{
	/** The Hunter level the guild's rumours open at (Huntmaster Gilman). */
	public static final int OPENS_AT = 46;
	/** Where the guild hunters are found: the Hunter Guild, where the Hunter cape teleport lands (worldmap/TeleportLocationData). */
	public static final Tile GUILD = Tile.of(1558, 3046);
	/** The Hunter Guild Caverns entrance (worldmap/DungeonLocation). */
	static final Tile CAVERNS = Tile.of(1557, 3049);

	/** Completed-rumour counts with something to unlock, and what: the wiki's milestones. */
	static final int[] MILESTONES = {10, 25, 50, 100, 150, 250};
	static final String[] UNLOCKS = {
		"a quetzal whistle blueprint from Soar Leader Pitri",
		"cooking graahk, kyatt and pyre fox",
		"cooking the antelopes and dashing kebbit",
		"a torn enhanced whistle blueprint in loot sacks",
		"Guildmaster Apatura's special chest key",
		"a torn perfected whistle blueprint in expert and master sacks",
	};

	private Rumour()
	{
	}

	/** A rumour's tier: who gives it, what it pays. */
	public enum Tier
	{
		NOVICE("Novice", 46, 50),
		ADEPT("Adept", 57, 50),
		EXPERT("Expert", 72, 55),
		MASTER("Master", 91, 60);

		private final String label;
		private final int level;
		private final int modifier;

		Tier(String label, int level, int modifier)
		{
			this.label = label;
			this.level = level;
			this.modifier = modifier;
		}

		public String getLabel()
		{
			return label;
		}

		/** The Hunter level the tier's guild hunters give rumours at. */
		public int getLevel()
		{
			return level;
		}

		/** Hunter experience for finishing a rumour of this tier at this level. */
		public int experience(int hunterLevel)
		{
			return (hunterLevel + 5) * modifier;
		}

		/** "an expert loot sack" (the novice tier's is the basic one). */
		public String sack()
		{
			return this == NOVICE ? "a basic loot sack" : (this == MASTER ? "a " : "an ") + label.toLowerCase(Locale.ROOT) + " loot sack";
		}
	}

	/** The guild hunters who give rumours. */
	public enum Master
	{
		GILMAN("Gilman", Tier.NOVICE, NpcID.HG_GILMAN),
		ORNUS("Ornus", Tier.ADEPT, NpcID.HG_ORNUS),
		CERVUS("Cervus", Tier.ADEPT, NpcID.HG_CERVUS),
		ACO("Aco", Tier.EXPERT, NpcID.HG_ACO),
		TECO("Teco", Tier.EXPERT, NpcID.HG_TECO),
		WOLF("Wolf", Tier.MASTER, NpcID.HG_WOLF);

		private final String name;
		private final Tier tier;
		private final int npcId;

		Master(String name, Tier tier, int npcId)
		{
			this.name = name;
			this.tier = tier;
			this.npcId = npcId;
		}

		public String getName()
		{
			return name;
		}

		public Tier getTier()
		{
			return tier;
		}

		public int getNpcId()
		{
			return npcId;
		}

		/** The master whose name stands as a word in this text, or null. */
		static Master named(String text)
		{
			if (text == null)
			{
				return null;
			}
			for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z]+"))
			{
				for (Master m : values())
				{
					if (m.name.toLowerCase(Locale.ROOT).equals(word))
					{
						return m;
					}
				}
			}
			return null;
		}

		/** The guild hunters of the best tier this Hunter level (and At First Light, for Wolf) opens. */
		public static List<Master> best(int hunterLevel, boolean atFirstLight)
		{
			Tier top = null;
			for (Tier t : Tier.values())
			{
				if (hunterLevel >= t.getLevel() && (t != Tier.MASTER || atFirstLight))
				{
					top = t;
				}
			}
			List<Master> out = new ArrayList<>();
			for (Master m : values())
			{
				if (m.tier == top)
				{
					out.add(m);
				}
			}
			return out;
		}
	}

	/** How a creature is hunted: its rare part's base chance and pity count, and what to bring. */
	public enum Method
	{
		BIRD_SNARE("Bird snare", 20, 40, 38, "Bring bird snares", ItemID.HUNTING_OJIBWAY_BIRD_SNARE),
		BOX_TRAP("Box trap", 50, 100, 94, "Bring box traps", ItemID.HUNTING_BOX_TRAP),
		BUTTERFLY_NET("Butterfly net", 40, 80, 76, "Bring a butterfly net", ItemID.HUNTING_BUTTERFLY_NET),
		/** Moths are only given once they can be caught barehanded (wiki, "Hunters' Rumours"). */
		BAREHANDED("Barehanded", 40, 80, 76, null, ItemID.HUNTING_BUTTERFLY_NET),
		DEADFALL("Deadfall", 15, 30, 28, "Bring logs and a knife", ItemID.KNIFE),
		FALCONRY("Falconry", 10, 20, 18, "Matthias rents a gyr falcon for 500 coins", ItemID.FALCON_GLOVES),
		NET_TRAP("Net trap", 25, 50, 46, "Bring net traps", ItemID.NET_TRAP),
		SPIKED_PIT("Spiked pit", 15, 30, 28, "Bring logs, a knife and a teasing stick", ItemID.HUNTING_TEASING_STICK),
		GOAT_PIT("Goat pit", 48, 96, 90, "Bring a cattleprod to wield", ItemID.CATTLEPROD),
		TRACKING("Tracking", 15, 30, 28, "Bring a noose wand", ItemID.NOOSE_WAND),
		HERBIBOAR("Tracking", 7, 14, 12, null, -1);

		private final String label;
		private final int oneIn;
		private final int pity;
		private final int pityOutfit;
		private final String bring;
		private final int itemId;

		Method(String label, int oneIn, int pity, int pityOutfit, String bring, int itemId)
		{
			this.label = label;
			this.oneIn = oneIn;
			this.pity = pity;
			this.pityOutfit = pityOutfit;
			this.bring = bring;
			this.itemId = itemId;
		}

		public String getLabel()
		{
			return label;
		}

		/** The rare part's base chance, one in this many catches. */
		public int getOneIn()
		{
			return oneIn;
		}

		/** Catches after which the rare part is certain, without the guild hunter outfit. */
		public int getPity()
		{
			return pity;
		}

		/** The same in the guild hunter outfit. */
		public int getPityOutfit()
		{
			return pityOutfit;
		}

		/** What to take along, or null when nothing is needed. */
		public String getBring()
		{
			return bring;
		}

		public int getItemId()
		{
			return itemId;
		}
	}

	/** Every creature a rumour can be about. */
	public enum Creature
	{
		TROPICAL_WAGTAIL("Tropical wagtail", 19, Method.BIRD_SNARE, "G", "Feldip", 2557, 2912, "Tlati Rainforest", 1269, 3102),
		WILD_KEBBIT("Wild kebbit", 23, Method.DEADFALL, "G", "Piscatoris", 2335, 3584, "Auburnvale", 1387, 3392),
		SAPPHIRE_GLACIALIS("Sapphire glacialis", 25, Method.BUTTERFLY_NET, "G", "Rellekka", 2719, 3780, "Mons Gratia", 1443, 3235),
		SWAMP_LIZARD("Swamp lizard", 29, Method.NET_TRAP, "GC", "Canifis", 3553, 3438, "Canifis", 3535, 3445, "Slepe", 3677, 3405),
		SPINED_LARUPIA("Spined larupia", 31, Method.SPIKED_PIT, "GO", "Feldip", 2557, 2912),
		BARB_TAILED_KEBBIT("Barb-tailed kebbit", 33, Method.DEADFALL, "G", "Feldip", 2557, 2912),
		SNOWY_KNIGHT("Snowy knight", 35, Method.BUTTERFLY_NET, "GO", "Rellekka", 2719, 3780, "Mons Gratia", 1443, 3235),
		PRICKLY_KEBBIT("Prickly kebbit", 37, Method.DEADFALL, "G", "Piscatoris", 2335, 3584),
		EMBERTAILED_JERBOA("Embertailed jerboa", 39, Method.BOX_TRAP, "GO", "the Hunter Guild", 1515, 3047, "Locus Oasis", 1671, 3001),
		HORNED_GRAAHK("Horned graahk", 41, Method.SPIKED_PIT, "GC", "Karamja", 2786, 3001),
		SPOTTED_KEBBIT("Spotted kebbit", 43, Method.FALCONRY, "GCO", "Piscatoris", 2379, 3599),
		BLACK_WARLOCK("Black warlock", 45, Method.BUTTERFLY_NET, "GC", "Feldip", 2557, 2912, "Tlati Rainforest", 1282, 3021),
		ORANGE_SALAMANDER("Orange salamander", 47, Method.NET_TRAP, "GCOA", "Necropolis", 3285, 2739, "Uzer", 3401, 3104),
		RAZOR_BACKED_KEBBIT("Razor-backed kebbit", 49, Method.TRACKING, "GC", "Piscatoris", 2335, 3584),
		SABRE_TOOTHED_KEBBIT("Sabre-toothed kebbit", 51, Method.DEADFALL, "GCOAT", "Rellekka", 2719, 3780),
		GREY_CHINCHOMPA("Grey chinchompa", 53, Method.BOX_TRAP, "GCAT", "Piscatoris", 2335, 3584, "Kourend Woodland", 1481, 3504,
			"Isle of Souls", 2127, 2950),
		SABRE_TOOTHED_KYATT("Sabre-toothed kyatt", 55, Method.SPIKED_PIT, "GOAT", "Rellekka", 2719, 3780),
		DARK_KEBBIT("Dark kebbit", 57, Method.FALCONRY, "GCAT", "Piscatoris", 2379, 3599),
		PYRE_FOX("Pyre fox", 57, Method.DEADFALL, "GCO", "Avium Savannah", 1616, 2999),
		RED_SALAMANDER("Red salamander", 59, Method.NET_TRAP, "GOATW", "Ourania", 2447, 3219, "Ourania", 2475, 3240, "Charred Island", 2644, 2401),
		WYRMSCRAIG_GOAT("Wyrmscraig goat", 60, Method.GOAT_PIT, "GCT", "Wyrmscraig", 2571, 2195),
		RED_CHINCHOMPA("Red chinchompa", 63, Method.BOX_TRAP, "GCOATW", "Feldip", 2557, 2912, "Gwenith", 2269, 3408, "Prifddinas", 3293, 6160,
			"Tlati Rainforest", 1315, 3169),
		DASHING_KEBBIT("Dashing kebbit", 69, Method.FALCONRY, "GATW", "Piscatoris", 2379, 3599),
		SUNLIGHT_ANTELOPE("Sunlight antelope", 72, Method.SPIKED_PIT, "GATW", "Avium Savannah", 1745, 3008),
		SUNLIGHT_MOTH("Sunlight moth", 75, Method.BAREHANDED, "GCT", "the Hunter Guild", 1556, 3091, "the Hunter Guild", 1575, 3020),
		TECU_SALAMANDER("Tecu salamander", 79, Method.NET_TRAP, "GAW", "Ralos' Rise", 1475, 3096),
		HERBIBOAR("Herbiboar", 80, Method.HERBIBOAR, "GTW", "Fossil Island", 3693, 3800, "Fossil Island", 3701, 3809, "Fossil Island", 3703, 3829,
			"Fossil Island", 3749, 3850, "Fossil Island", 3684, 3870),
		MOONLIGHT_MOTH("Moonlight moth", 85, Method.BAREHANDED, "GAW", "the Hunter Guild Caverns", 1557, 3049),
		MOONLIGHT_ANTELOPE("Moonlight antelope", 91, Method.SPIKED_PIT, "GW", "the Hunter Guild Caverns", 1557, 3049);

		private final String name;
		private final int level;
		private final Method method;
		private final Set<Master> masters;
		private final List<String> areas;
		private final List<Tile> tiles;

		Creature(String name, int level, Method method, String masters, Object... places)
		{
			this.name = name;
			this.level = level;
			this.method = method;
			Set<Master> m = EnumSet.noneOf(Master.class);
			for (char c : masters.toCharArray())
			{
				m.add(c == 'G' ? Master.GILMAN : c == 'C' ? Master.CERVUS : c == 'O' ? Master.ORNUS
					: c == 'A' ? Master.ACO : c == 'T' ? Master.TECO : Master.WOLF);
			}
			this.masters = Collections.unmodifiableSet(m);
			List<String> a = new ArrayList<>();
			List<Tile> t = new ArrayList<>();
			for (int i = 0; i + 2 < places.length; i += 3)
			{
				String area = (String) places[i];
				if (!a.contains(area))
				{
					a.add(area);
				}
				t.add(Tile.of((Integer) places[i + 1], (Integer) places[i + 2]));
			}
			this.areas = Collections.unmodifiableList(a);
			this.tiles = Collections.unmodifiableList(t);
		}

		/** As the game writes it in a sentence, such as "Grey chinchompa". */
		public String getName()
		{
			return name;
		}

		/** The Hunter level rumours of it are given at. */
		public int getLevel()
		{
			return level;
		}

		public Method getMethod()
		{
			return method;
		}

		/** The guild hunters who give it. */
		public Set<Master> getMasters()
		{
			return masters;
		}

		/** Where it is hunted, by name, each once. */
		public List<String> getAreas()
		{
			return areas;
		}

		/** Where it is hunted, for pricing the journey. */
		public List<Tile> getTiles()
		{
			return tiles;
		}

		/** Other names the game uses for it in a rumour. */
		List<String> aliases()
		{
			return this == RED_CHINCHOMPA ? Arrays.asList("red chinchompa", "carnivorous chinchompa")
				: Collections.singletonList(name.toLowerCase(Locale.ROOT));
		}

		/** The creature a rumour line names, longest name first so no name is read inside another, or null. */
		static Creature named(String text)
		{
			if (text == null)
			{
				return null;
			}
			String t = text.toLowerCase(Locale.ROOT);
			Creature best = null;
			int bestLength = 0;
			for (Creature c : values())
			{
				for (String alias : c.aliases())
				{
					if (alias.length() > bestLength && t.contains(alias))
					{
						best = c;
						bestLength = alias.length();
					}
				}
			}
			return best;
		}
	}

	/** The next milestone after this many finished rumours, as "N: what it unlocks", or null past the last. */
	static int nextMilestone(int done)
	{
		for (int i = 0; i < MILESTONES.length; i++)
		{
			if (done < MILESTONES[i])
			{
				return i;
			}
		}
		return -1;
	}
}
