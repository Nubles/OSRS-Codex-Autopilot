package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.ItemID;

/**
 * What Autopilot knows about Slayer, every fact from the OSRS wiki ("Slayer master", "Slayer
 * points" and each monster's page) or RuneLite's own source:
 *
 * <ul>
 * <li>The masters, what each asks of the player before giving tasks (combat level, Slayer level,
 * a quest), who replaces whom after a quest, and the points each pays a task: the wiki's
 * "Slayer master" and "Slayer points" tables.</li>
 * <li>Points start with the fifth task in a row and are multiplied on every 10th (5x), 50th (15x),
 * 100th (25x), 250th (35x) and 1,000th (50x) task: "Slayer points".</li>
 * <li>Which master the game's {@code VarbitID.SLAYER_MASTER} names: read from the game's own
 * master task table ({@code DBTableID.SlayerMasterTask}), whose rows RuneLite names by master
 * ({@code DURADEL_KURASK}, ...); Krystilia is 7 and Mortimer 10 in RuneLite's Slayer plugin.</li>
 * <li>For the tasks below only, where the monster lives and what the fight needs: each monster's
 * wiki page. Tasks not listed get no where or bring line rather than a guess. Map points for the
 * journey are RuneLite's world map dungeon and teleport locations (worldmap/DungeonLocation,
 * TeleportLocationData).</li>
 * </ul>
 */
public final class Slayer
{
	/** Tasks in a row before any points are paid ("not awarded for the first four tasks"). */
	public static final int POINTS_FROM = 5;
	/** The task id the game gives a boss task; the boss itself is in {@code SLAYER_TARGET_BOSSID} (RuneLite's Slayer plugin). */
	public static final int BOSS_TASK = 98;

	private Slayer()
	{
	}

	/** The points multiplier on this task in a row: the wiki's "5x points every 10th task" and the rest. */
	public static int multiplier(int task)
	{
		if (task <= 0)
		{
			return 1;
		}
		if (task % 1000 == 0)
		{
			return 50;
		}
		if (task % 250 == 0)
		{
			return 35;
		}
		if (task % 100 == 0)
		{
			return 25;
		}
		if (task % 50 == 0)
		{
			return 15;
		}
		return task % 10 == 0 ? 5 : 1;
	}

	/**
	 * The next task in a row after this streak whose multiplier is at least this much: 5 for the
	 * next 10th, 15 for the next 50th and so on.
	 */
	public static int nextMilestone(int streak, int atLeast)
	{
		int t = Math.max(streak, 0) + 1;
		while (multiplier(t) < atLeast)
		{
			t++;
		}
		return t;
	}

	/** "50th". */
	public static String ordinal(int n)
	{
		return Rumours.ordinal(n);
	}

	/** A quest a master waits on. */
	public enum Quest
	{
		NONE, PRIEST_IN_PERIL, LOST_CITY, SHILO_VILLAGE
	}

	/** The Slayer masters, in the order a player outgrows them. */
	public enum Master
	{
		TURAEL("Turael", "Aya", 0, 0, Quest.NONE, 0, 0, true, DBTableID.SlayerMasterTask.Row.TURAEL_LIZARDS),
		SPRIA("Spria", null, 0, 0, Quest.NONE, 0, 0, false, DBTableID.SlayerMasterTask.Row.SPRIA_LIZARDS),
		MAZCHNA("Mazchna", null, 20, 0, Quest.PRIEST_IN_PERIL, 6, 6, true, DBTableID.SlayerMasterTask.Row.MAZCHNA_CAVEBUGS),
		VANNAKA("Vannaka", null, 40, 0, Quest.NONE, 8, 8, true, DBTableID.SlayerMasterTask.Row.VANNAKA_GARGOYLES),
		CHAELDAR("Chaeldar", null, 70, 0, Quest.LOST_CITY, 10, 10, true, DBTableID.SlayerMasterTask.Row.CHAELDAR_GARGOYLES),
		KONAR("Konar", null, 75, 0, Quest.NONE, 18, 20, true, DBTableID.SlayerMasterTask.Row.KONAR_BOSS),
		NIEVE("Nieve", "Steve", 85, 0, Quest.NONE, 12, 15, true, DBTableID.SlayerMasterTask.Row.NIEVE_BOSS),
		DURADEL("Duradel", "Kuradal", 100, 50, Quest.SHILO_VILLAGE, 15, 15, true, DBTableID.SlayerMasterTask.Row.DURADEL_BOSS),
		/** Wilderness tasks, counted apart from the rest. */
		KRYSTILIA("Krystilia", null, 0, 0, Quest.NONE, 25, 25, false, DBTableID.SlayerMasterTask.Row.KRYSTILLIA_BOSS),
		/** Pays 0 to 40 points; counted apart, and asks for part of Fallen From Grace. */
		MORTIMER("Mortimer", null, 100, 70, Quest.NONE, 0, 0, false, DBTableID.SlayerMasterTask.Row.MORTIMER_KURASK);

		/** What RuneLite's Slayer plugin calls Krystilia's and Mortimer's {@code SLAYER_MASTER} values. */
		public static final int KRYSTILIA_ID = 7;
		public static final int MORTIMER_ID = 10;

		private final String name;
		private final String after;
		private final int combat;
		private final int slayer;
		private final Quest quest;
		private final int points;
		private final int diaryPoints;
		private final boolean ladder;
		private final int sampleRow;

		Master(String name, String after, int combat, int slayer, Quest quest, int points, int diaryPoints, boolean ladder, int sampleRow)
		{
			this.name = name;
			this.after = after;
			this.combat = combat;
			this.slayer = slayer;
			this.quest = quest;
			this.points = points;
			this.diaryPoints = diaryPoints;
			this.ladder = ladder;
			this.sampleRow = sampleRow;
		}

		/** The name before any replacement: "Duradel". */
		public String getName()
		{
			return name;
		}

		/** Who stands in their place for this account: Aya and Kuradal after While Guthix Sleeps, Steve after Monkey Madness II. */
		public String nameFor(Account a)
		{
			if (after == null || a == null)
			{
				return name;
			}
			boolean replaced = this == NIEVE ? a.mm2 : a.wgs;
			return replaced ? after : name;
		}

		public int getCombat()
		{
			return combat;
		}

		public int getSlayer()
		{
			return slayer;
		}

		public Quest getQuest()
		{
			return quest;
		}

		/** Points for one task in a row, with the diary that raises them (Western Provinces elite for Nieve, Kourend & Kebos elite for Konar). */
		public int points(int task, Account a)
		{
			if (task > 0 && task < POINTS_FROM)
			{
				return 0;
			}
			boolean diary = a != null && (this == NIEVE && a.westernElite || this == KONAR && a.kourendElite);
			return (diary ? diaryPoints : points) * multiplier(task);
		}

		/** A row of the game's master task table that belongs to this master, to learn its id from. */
		public int getSampleRow()
		{
			return sampleRow;
		}

		/** Whether this account can take tasks from this master, as far as is known. */
		public boolean opens(Account a)
		{
			if (a == null || a.combat < combat || a.slayer < slayer)
			{
				return false;
			}
			switch (quest)
			{
				case PRIEST_IN_PERIL:
					return a.priestInPeril;
				case LOST_CITY:
					return a.lostCity;
				case SHILO_VILLAGE:
					return a.shilo;
				default:
					return true;
			}
		}

		/**
		 * The masters of the usual ladder (not Spria, nor Krystilia's and Mortimer's separate
		 * counts) this account can use, the most demanding first.
		 */
		public static List<Master> ladder(Account a)
		{
			List<Master> out = new ArrayList<>();
			for (Master m : values())
			{
				if (m.ladder && m.opens(a))
				{
					out.add(m);
				}
			}
			out.sort((x, y) -> Integer.compare(y.combat, x.combat));
			return out;
		}
	}

	/** What the account has that the masters ask about. */
	public static final class Account
	{
		public int combat;
		public int slayer;
		public boolean priestInPeril;
		public boolean lostCity;
		public boolean shilo;
		/** While Guthix Sleeps: Aya and Kuradal. */
		public boolean wgs;
		/** Monkey Madness II: Steve. */
		public boolean mm2;
		public boolean westernElite;
		public boolean kourendElite;

		public Account copy()
		{
			Account a = new Account();
			a.combat = combat;
			a.slayer = slayer;
			a.priestInPeril = priestInPeril;
			a.lostCity = lostCity;
			a.shilo = shilo;
			a.wgs = wgs;
			a.mm2 = mm2;
			a.westernElite = westernElite;
			a.kourendElite = kourendElite;
			return a;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Account))
			{
				return false;
			}
			Account b = (Account) o;
			return combat == b.combat && slayer == b.slayer && priestInPeril == b.priestInPeril && lostCity == b.lostCity
				&& shilo == b.shilo && wgs == b.wgs && mm2 == b.mm2 && westernElite == b.westernElite && kourendElite == b.kourendElite;
		}

		@Override
		public int hashCode()
		{
			return combat * 31 + slayer;
		}
	}

	/** A named place a monster lives, with a map point for the journey when RuneLite's world map has one. */
	public static final class Place
	{
		private final String name;
		private final Tile tile;

		Place(String name, Tile tile)
		{
			this.name = name;
			this.tile = tile;
		}

		public String getName()
		{
			return name;
		}

		/** Null when there is no verified map point to price the journey to. */
		public Tile getTile()
		{
			return tile;
		}
	}

	// RuneLite world map points (worldmap/DungeonLocation unless noted).
	private static final Tile FREMENNIK = Tile.of(2796, 3615);
	/** TeleportLocationData.SLAYER_TOWER, the slayer ring's landing at the tower. */
	private static final Tile TOWER = Tile.of(3423, 3536);
	private static final Tile LUMBRIDGE_SWAMP = Tile.of(3168, 3172);
	private static final Tile STRONGHOLD = Tile.of(2427, 3424);
	private static final Tile SMOKE_DUNGEON = Tile.of(3309, 2962);
	private static final Tile CATACOMBS = Tile.of(1636, 3673);
	private static final Tile SMOKE_DEVIL = Tile.of(2411, 3061);
	private static final Tile KARUULM = Tile.of(1308, 3807);
	private static final Tile MOS_LE_HARMLESS = Tile.of(3747, 2973);
	private static final Tile IORWERTH = Tile.of(3224, 6044);
	private static final Tile JORMUNGAND = Tile.of(2464, 4012);

	private static Place at(String name, Tile tile)
	{
		return new Place(name, tile);
	}

	private static Place at(String name)
	{
		return new Place(name, null);
	}

	/**
	 * The tasks whose where and what to bring are checked against the wiki, keyed by the game's
	 * Slayer task table row ({@code DBTableID.SlayerTask.Row}).
	 */
	public enum Task
	{
		ROCKSLUGS(DBTableID.SlayerTask.Row.SLAYER_TARGET_ROCKSLUGS, 20,
			"Bags of salt: one finishes a rockslug at 4 hitpoints or less", ItemID.SLAYER_BAG_OF_SALT,
			at("Lumbridge Swamp Caves", LUMBRIDGE_SWAMP), at("Fremennik Slayer Dungeon", FREMENNIK), at("Dorgesh-Kaan South Dungeon"),
			at("Tonali Cavern"), at("Ynysdail Cavern")),
		LIZARDS(DBTableID.SlayerTask.Row.SLAYER_TARGET_LIZARDS, 22,
			"Ice coolers: one finishes a lizard at low health, and is used up", ItemID.SLAYER_ICY_WATER,
			at("the Kharidian Desert")),
		BANSHEES(DBTableID.SlayerTask.Row.SLAYER_TARGET_BANSHEES, 15,
			"Wear earmuffs or a Slayer helmet", ItemID.SLAYER_EARMUFFS,
			at("the Slayer Tower", TOWER)),
		COCKATRICE(DBTableID.SlayerTask.Row.SLAYER_TARGET_COCKATRICE, 25,
			"A mirror shield or V's shield is highly recommended", ItemID.SLAYER_MIRROR_SHIELD,
			at("Fremennik Slayer Dungeon", FREMENNIK)),
		HARPIE_BUG_SWARMS(DBTableID.SlayerTask.Row.SLAYER_TARGET_HARPIEBUGSWARMS, 33,
			"Equip a lit bug lantern (33 Firemaking to light)", ItemID.SLAYER_BUGLAN_ON,
			at("north-east of Tai Bwo Wannai"), at("east of the Colossal Wyrm Remains")),
		WALL_BEASTS(DBTableID.SlayerTask.Row.SLAYER_TARGET_WALLBEASTS, 35,
			"Wear a spiny helmet or Slayer helmet to start a fight", ItemID.WALLBEAST_SPIKE_HELMET,
			at("Lumbridge Swamp Caves", LUMBRIDGE_SWAMP)),
		KILLERWATTS(DBTableID.SlayerTask.Row.SLAYER_TARGET_KILLERWATTS, 37,
			"Wear insulated boots", ItemID.SLAYER_BOOTS,
			at("the portal atop Draynor Manor (Ernest the Chicken)")),
		MOLANISKS(DBTableID.SlayerTask.Row.SLAYER_TARGET_MOLANISKS, 39,
			"A slayer bell to start each fight", ItemID.MOLANISK_BELL,
			at("Dorgesh-Kaan South Dungeon")),
		BASILISKS(DBTableID.SlayerTask.Row.SLAYER_TARGET_BASILISKS, 40,
			"Equip a mirror shield or V's shield", ItemID.SLAYER_MIRROR_SHIELD,
			at("Fremennik Slayer Dungeon", FREMENNIK), at("Jormungand's Prison", JORMUNGAND)),
		FEVER_SPIDERS(DBTableID.SlayerTask.Row.SLAYER_TARGET_FEVERSPIDERS, 42,
			"Wear slayer gloves", ItemID.DEAL_SLAYER_GLOVES,
			at("the brewery basement on Braindeath Island")),
		TUROTH(DBTableID.SlayerTask.Row.SLAYER_TARGET_TUROTH, 55,
			"Leaf-bladed weapons, broad arrows and bolts, or Magic Dart: nothing else hurts them", ItemID.LEAFBLADED_SWORD,
			at("Fremennik Slayer Dungeon", FREMENNIK)),
		MUTATED_ZYGOMITES(DBTableID.SlayerTask.Row.SLAYER_TARGET_MUTATEDZYGOMITES, 57,
			"Fungicide spray: they die to it under 8 hitpoints", ItemID.SLAYER_SPRAY_PUMP_10,
			at("Zanaris")),
		CAVE_HORRORS(DBTableID.SlayerTask.Row.SLAYER_TARGET_CAVEHORRORS, 58,
			"A light source and a witchwood icon", ItemID.WITCHWOOD_ICON,
			at("Mos Le'Harmless Cave", MOS_LE_HARMLESS)),
		ABERRANT_SPECTRES(DBTableID.SlayerTask.Row.SLAYER_TARGET_ABERRANTSPECTRES, 60,
			"Wear a nose peg or Slayer helmet", ItemID.SLAYER_NOSEPEG,
			at("the Slayer Tower", TOWER), at("Stronghold Slayer Cave", STRONGHOLD), at("Deepfin Mine")),
		WYRMS(DBTableID.SlayerTask.Row.SLAYER_TARGET_WYRMS, 62,
			"In Karuulm, boots of stone, boots of brimstone or granite boots against the hot floor", ItemID.BOOTS_OF_STONE,
			at("Karuulm Slayer Dungeon", KARUULM), at("Wyrmscraig Cavern")),
		DUST_DEVILS(DBTableID.SlayerTask.Row.SLAYER_TARGET_DUSTDEVILS, 65,
			"Wear a facemask or Slayer helmet", ItemID.SLAYER_FACEMASK,
			at("the Smoke Dungeon", SMOKE_DUNGEON), at("the Catacombs of Kourend", CATACOMBS), at("the Wilderness Slayer Cave")),
		KURASK(DBTableID.SlayerTask.Row.SLAYER_TARGET_KURASK, 70,
			"Leaf-bladed weapons, broad ammunition or Magic Dart: nothing else hurts them", ItemID.LEAFBLADED_SWORD,
			at("Fremennik Slayer Dungeon", FREMENNIK), at("Iorwerth Dungeon", IORWERTH), at("Kurask Lair")),
		GARGOYLES(DBTableID.SlayerTask.Row.SLAYER_TARGET_GARGOYLES, 75,
			"A rock hammer, rock thrownhammer or granite hammer to finish them, unless you own Gargoyle Smasher",
			ItemID.SLAYER_ROCK_HAMMER,
			at("the Slayer Tower's top floor", TOWER), at("the Slayer Tower basement", TOWER)),
		DRAKES(DBTableID.SlayerTask.Row.SLAYER_TARGET_DRAKES, 84,
			"Boots of stone, boots of brimstone or granite boots against the hot floor", ItemID.BOOTS_OF_STONE,
			at("Karuulm Slayer Dungeon", KARUULM)),
		SMOKE_DEVILS(DBTableID.SlayerTask.Row.SLAYER_TARGET_SMOKEDEVIL, 93,
			"Wear a facemask or Slayer helmet", ItemID.SLAYER_FACEMASK,
			at("the Smoke Devil Dungeon", SMOKE_DEVIL)),
		HYDRAS(DBTableID.SlayerTask.Row.SLAYER_TARGET_HYDRAS, 95,
			"Boots of stone, boots of brimstone or granite boots against the hot floor", ItemID.BOOTS_OF_STONE,
			at("Karuulm Slayer Dungeon", KARUULM));

		private final int row;
		private final int level;
		private final String bring;
		private final int itemId;
		private final List<Place> places;

		Task(int row, int level, String bring, int itemId, Place... places)
		{
			this.row = row;
			this.level = level;
			this.bring = bring;
			this.itemId = itemId;
			List<Place> p = new ArrayList<>();
			Collections.addAll(p, places);
			this.places = Collections.unmodifiableList(p);
		}

		/** The game's Slayer task table row. */
		public int getRow()
		{
			return row;
		}

		/** The Slayer level the monster needs. */
		public int getLevel()
		{
			return level;
		}

		/** What the fight needs, as one line. */
		public String getBring()
		{
			return bring;
		}

		public int getItemId()
		{
			return itemId;
		}

		public List<Place> getPlaces()
		{
			return places;
		}

		/** The task whose row this is, or null for a task with no checked facts. */
		public static Task of(int row)
		{
			if (row <= 0)
			{
				return null;
			}
			for (Task t : values())
			{
				if (t.row == row)
				{
					return t;
				}
			}
			return null;
		}

		/** The place this area name names, ignoring a leading "the" and case, or null. */
		public Place place(String area)
		{
			if (area == null)
			{
				return null;
			}
			String a = bare(area);
			for (Place p : places)
			{
				if (bare(p.name).equals(a))
				{
					return p;
				}
			}
			return null;
		}

		private static String bare(String s)
		{
			String t = s.trim().toLowerCase(Locale.ROOT);
			return t.startsWith("the ") ? t.substring(4) : t;
		}
	}

	/** "A, B or C". */
	static String either(List<String> names)
	{
		if (names.isEmpty())
		{
			return "";
		}
		if (names.size() == 1)
		{
			return names.get(0);
		}
		return String.join(", ", names.subList(0, names.size() - 1)) + " or " + names.get(names.size() - 1);
	}
}
