package com.codex.autopilot.core;

import com.codex.autopilot.core.farming.PatchKind;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Every patch and bird house space Autopilot keeps an eye on: the timed chores Codex Rounds tends,
 * and the herb, allotment, flower and hops patches of a herb run.
 * RuneLite's Time Tracking plugin saves each one in its "timetracking" profile config under
 * {@code <first region>.<varbit>} (patches) or {@code birdhouse.<varp>} (bird house spaces), as
 * {@code value:unixSecond}. The regions follow RuneLite's FarmingWorld; only the first is used
 * for the key, the rest are kept so the list can be checked against Rounds.
 */
public enum Spot
{

	BIRDHOUSE_VALLEY_NE("Verdant Valley, north-east", Group.BIRDHOUSES, VarPlayerID.BIRDHOUSE_TRANSMIT_C),
	BIRDHOUSE_VALLEY_SW("Verdant Valley, south-west", Group.BIRDHOUSES, VarPlayerID.BIRDHOUSE_TRANSMIT_D),
	BIRDHOUSE_MEADOW_N("Mushroom Meadow, north", Group.BIRDHOUSES, VarPlayerID.BIRDHOUSE_TRANSMIT_A),
	BIRDHOUSE_MEADOW_S("Mushroom Meadow, south", Group.BIRDHOUSES, VarPlayerID.BIRDHOUSE_TRANSMIT_B),

	HARDWOOD_FOSSIL_E("Fossil Island, east", Group.HARDWOODS, PatchKind.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_A, 14651, 14907, 14908, 15164, 14652, 14906, 14650, 15162, 15163),
	HARDWOOD_FOSSIL_M("Fossil Island, middle", Group.HARDWOODS, PatchKind.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_B, 14651, 14907, 14908, 15164, 14652, 14906, 14650, 15162, 15163),
	HARDWOOD_FOSSIL_W("Fossil Island, west", Group.HARDWOODS, PatchKind.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_C, 14651, 14907, 14908, 15164, 14652, 14906, 14650, 15162, 15163),
	SEAWEED_N("Seabed, north", Group.SEAWEED, PatchKind.SEAWEED, VarbitID.FARMING_TRANSMIT_A, 15008),
	SEAWEED_S("Seabed, south", Group.SEAWEED, PatchKind.SEAWEED, VarbitID.FARMING_TRANSMIT_B, 15008),

	HARDWOOD_ANGLERS("Anglers' Retreat", Group.HARDWOODS, PatchKind.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_A, 9770),
	HARDWOOD_SAVANNAH("Avium Savannah", Group.HARDWOODS, PatchKind.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_A, 6702, 6446),

	CALQUAT_TAI_BWO("Tai Bwo Wannai", Group.TREES, PatchKind.CALQUAT, VarbitID.FARMING_TRANSMIT_A, 11056),
	CALQUAT_KASTORI("Kastori", Group.TREES, PatchKind.CALQUAT, VarbitID.FARMING_TRANSMIT_A, 5423, 5167, 5424),
	CALQUAT_CONCH("The Great Conch", Group.TREES, PatchKind.CALQUAT, VarbitID.FARMING_TRANSMIT_C,
		12581, 12325, 12326, 12327, 12580, 12582, 12583, 12836, 12837, 12838, 12839, 13092, 13093, 13194),
	CELASTRUS_GUILD("Farming Guild", Group.TREES, PatchKind.CELASTRUS, VarbitID.FARMING_TRANSMIT_L, -1),
	REDWOOD_GUILD("Farming Guild", Group.TREES, PatchKind.REDWOOD, VarbitID.FARMING_TRANSMIT_I, -1),
	CRYSTAL_PRIF("Prifddinas", Group.TREES, PatchKind.CRYSTAL_TREE, VarbitID.FARMING_TRANSMIT_E, 13151, 12895, 12894, 13150, 12994, 12993, 12737, 12738, 12126, 12127, 13250),

	SPIRIT_PORT_SARIM("Port Sarim", Group.SPIRIT_TREES, PatchKind.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_A, 12082, 12083),
	SPIRIT_ETCETERIA("Etceteria", Group.SPIRIT_TREES, PatchKind.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_B, 10300),
	SPIRIT_BRIMHAVEN("Brimhaven", Group.SPIRIT_TREES, PatchKind.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_B, 11058, 11057),
	SPIRIT_HOSIDIUS("Hosidius", Group.SPIRIT_TREES, PatchKind.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_F, 6967, 6711),
	SPIRIT_GUILD("Farming Guild", Group.SPIRIT_TREES, PatchKind.SPIRIT_TREE, VarbitID.FARMING_TRANSMIT_A, -1),

	CACTUS_AL_KHARID("Al Kharid", Group.PATCHES, PatchKind.CACTUS, VarbitID.FARMING_TRANSMIT_A, 13106, 13362, 13105),
	CACTUS_GUILD("Farming Guild", Group.PATCHES, PatchKind.CACTUS, VarbitID.FARMING_TRANSMIT_F, -1),
	BUSH_CHAMPIONS("Champions' Guild", Group.PATCHES, PatchKind.BUSH, VarbitID.FARMING_TRANSMIT_A, 12596),
	BUSH_RIMMINGTON("Rimmington", Group.PATCHES, PatchKind.BUSH, VarbitID.FARMING_TRANSMIT_A, 11570, 11826),
	BUSH_ARDOUGNE("Ardougne Monastery", Group.PATCHES, PatchKind.BUSH, VarbitID.FARMING_TRANSMIT_A, 10290, 10546),
	BUSH_ETCETERIA("Etceteria", Group.PATCHES, PatchKind.BUSH, VarbitID.FARMING_TRANSMIT_A, 10300),
	BUSH_GUILD("Farming Guild", Group.PATCHES, PatchKind.BUSH, VarbitID.FARMING_TRANSMIT_B, -1),
	MUSHROOM_CANIFIS("Canifis", Group.PATCHES, PatchKind.MUSHROOM, VarbitID.FARMING_TRANSMIT_A, 13622, 13878),
	BELLADONNA_DRAYNOR("Draynor Manor", Group.PATCHES, PatchKind.BELLADONNA, VarbitID.FARMING_TRANSMIT_A, 12340),
	BELLADONNA_AUBURNVALE("Auburnvale", Group.PATCHES, PatchKind.BELLADONNA, VarbitID.FARMING_TRANSMIT_B, 5427, 5428, 5684),
	ANIMA_GUILD("Farming Guild", Group.PATCHES, PatchKind.ANIMA, VarbitID.FARMING_TRANSMIT_M, -1),
	HESPORI_GUILD("Farming Guild", Group.PATCHES, PatchKind.HESPORI, VarbitID.FARMING_TRANSMIT_J, 5021),
	HERB_ARDOUGNE("Ardougne", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_D, 10548),
	HERB_CATHERBY("Catherby", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_D, 11062),
	HERB_FALADOR("Falador", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_D, 12083),
	HERB_HOSIDIUS("Hosidius", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_D, 6967),
	HERB_PORT_PHASMATYS("Port Phasmatys", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_D, 14391),
	HERB_CIVITAS("Civitas illa Fortis", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_D, 6192),
	HERB_GUILD("Farming Guild", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_E, -1),
	HERB_TROLL_STRONGHOLD("Troll Stronghold", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_A, 11321),
	HERB_WEISS("Weiss", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_A, 11325),
	HERB_HARMONY("Harmony Island", Group.HERBS, PatchKind.HERB, VarbitID.FARMING_TRANSMIT_B, 15148),
	ALLOTMENT_ARDOUGNE_N("Ardougne, north", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 10548),
	ALLOTMENT_ARDOUGNE_S("Ardougne, south", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 10548),
	ALLOTMENT_CATHERBY_N("Catherby, north", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 11062),
	ALLOTMENT_CATHERBY_S("Catherby, south", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 11062),
	ALLOTMENT_FALADOR_NW("Falador, north-west", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 12083),
	ALLOTMENT_FALADOR_SE("Falador, south-east", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 12083),
	ALLOTMENT_HOSIDIUS_NE("Hosidius, north-east", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 6967),
	ALLOTMENT_HOSIDIUS_SW("Hosidius, south-west", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 6967),
	ALLOTMENT_PORT_PHASMATYS_NW("Port Phasmatys, north-west", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 14391),
	ALLOTMENT_PORT_PHASMATYS_SE("Port Phasmatys, south-east", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 14391),
	ALLOTMENT_CIVITAS_N("Civitas illa Fortis, north", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 6192),
	ALLOTMENT_CIVITAS_S("Civitas illa Fortis, south", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 6192),
	ALLOTMENT_GUILD_N("Farming Guild, north", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_C, -1),
	ALLOTMENT_GUILD_S("Farming Guild, south", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_D, -1),
	ALLOTMENT_PRIF_N("Prifddinas, north", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 13151),
	ALLOTMENT_PRIF_S("Prifddinas, south", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 13151),
	ALLOTMENT_HARMONY("Harmony Island", Group.ALLOTMENTS, PatchKind.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 15148),
	FLOWER_ARDOUGNE("Ardougne", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_C, 10548),
	FLOWER_CATHERBY("Catherby", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_C, 11062),
	FLOWER_FALADOR("Falador", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_C, 12083),
	FLOWER_HOSIDIUS("Hosidius", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_C, 6967),
	FLOWER_PORT_PHASMATYS("Port Phasmatys", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_C, 14391),
	FLOWER_CIVITAS("Civitas illa Fortis", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_C, 6192),
	FLOWER_GUILD("Farming Guild", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_H, -1),
	FLOWER_PRIF("Prifddinas", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_C, 13151),
	FLOWER_KASTORI("Kastori", Group.FLOWERS, PatchKind.FLOWER, VarbitID.FARMING_TRANSMIT_C, 5423),
	HOPS_LUMBRIDGE("Lumbridge", Group.HOPS, PatchKind.HOPS, VarbitID.FARMING_TRANSMIT_A, 12851),
	HOPS_SEERS("Seers' Village", Group.HOPS, PatchKind.HOPS, VarbitID.FARMING_TRANSMIT_A, 10551),
	HOPS_YANILLE("Yanille", Group.HOPS, PatchKind.HOPS, VarbitID.FARMING_TRANSMIT_A, 10288),
	HOPS_ENTRANA("Entrana", Group.HOPS, PatchKind.HOPS, VarbitID.FARMING_TRANSMIT_A, 11060),
	HOPS_ALDARIN("Aldarin", Group.HOPS, PatchKind.HOPS, VarbitID.FARMING_TRANSMIT_A, 5421);


	/** How chores are grouped. */
	public enum Group
	{
		BIRDHOUSES("Bird houses"),
		SEAWEED("Giant seaweed"),
		HARDWOODS("Hardwood trees"),
		TREES("Special trees"),
		SPIRIT_TREES("Spirit trees"),
		PATCHES("Special patches"),
		HERBS("Herbs"),
		ALLOTMENTS("Allotments"),
		FLOWERS("Flowers"),
		HOPS("Hops");

		private final String label;

		Group(String label)
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}
	}

	private final String label;
	private final Group group;
	private final PatchKind kind;
	private final int varbit;
	private final int varp;
	private final int[] regions;

	Spot(String label, Group group, int varp)
	{
		this(label, group, null, -1, varp, new int[0]);
	}

	Spot(String label, Group group, PatchKind kind, int varbit, int... regions)
	{
		this(label, group, kind, varbit, -1, regions);
	}

	Spot(String label, Group group, PatchKind kind, int varbit, int varp, int[] regions)
	{
		this.label = label;
		this.group = group;
		this.kind = kind;
		this.varbit = varbit;
		this.varp = varp;
		this.regions = regions.length == 1 && regions[0] == -1
			// The Farming Guild's regions, as FarmingWorld lists them.
			? new int[]{4922, 5177, 5178, 5179, 4921, 4923, 4665, 4666, 4667} : regions;
	}

	public String getLabel()
	{
		return label;
	}

	/** "Calquat, Kastori": what the spot is and where. */
	public String getTitle()
	{
		return kindName() + ", " + label;
	}

	public String kindName()
	{
		if (isBirdhouse())
		{
			return "Bird house";
		}
		switch (kind)
		{
			case HARDWOOD_TREE:
				return "Hardwood";
			case SEAWEED:
				return "Seaweed";
			case CALQUAT:
				return "Calquat";
			case CELASTRUS:
				return "Celastrus";
			case REDWOOD:
				return "Redwood";
			case CRYSTAL_TREE:
				return "Crystal tree";
			case SPIRIT_TREE:
				return "Spirit tree";
			case CACTUS:
				return "Cactus";
			case BUSH:
				return "Bush";
			case MUSHROOM:
				return "Mushroom";
			case BELLADONNA:
				return "Belladonna";
			case ANIMA:
				return "Anima";
			case HESPORI:
				return "Hespori";
			case HERB:
				return "Herbs";
			case ALLOTMENT:
				return "Allotment";
			case FLOWER:
				return "Flowers";
			case HOPS:
				return "Hops";
			default:
				return kind.name();
		}
	}

	public Group getGroup()
	{
		return group;
	}

	/** The patch kind, or null for a bird house space. */
	public PatchKind getKind()
	{
		return kind;
	}

	public boolean isBirdhouse()
	{
		return kind == null;
	}

	/** Whether it is on Fossil Island or its seabed, where one trip covers it all. */
	public boolean isFossilIsland()
	{
		return isBirdhouse() || group == Group.SEAWEED || this == HARDWOOD_FOSSIL_E || this == HARDWOOD_FOSSIL_M || this == HARDWOOD_FOSSIL_W;
	}

	public int getVarbit()
	{
		return varbit;
	}

	public int getVarp()
	{
		return varp;
	}

	/** Where RuneLite's Time Tracking plugin keeps this spot, in its "timetracking" profile config. */
	public String timeTrackingKey()
	{
		return isBirdhouse() ? "birdhouse." + varp : regions[0] + "." + varbit;
	}
}
