package com.codex.autopilot.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Where each step is done, for pricing the journey there. Patch tiles are RuneLite's own world
 * map markers (worldmap FarmingPatchLocation); bird houses, the Fossil Island hardwoods and the
 * seabed, which those markers group or leave out, are Codex Rounds' tiles, read from the game
 * cache, as is Etceteria's spirit tree; the dailies, the kingdom and the farming contract are Codex Rounds' places.
 */
public final class Where
{
	/** Advisor Ghrim, on the first floor of Miscellania's castle. */
	public static final Tile KINGDOM = Tile.of(2499, 3857, 1);
	/** Guildmaster Jane, in the Farming Guild. */
	public static final Tile FARMING_GUILD = Tile.of(1248, 3735);

	private static final Map<Spot, Tile> SPOTS = new EnumMap<>(Spot.class);
	private static final Map<Daily, Tile> DAILIES = new EnumMap<>(Daily.class);

	static
	{
		spot(Spot.BIRDHOUSE_VALLEY_NE, 3768, 3761);
		spot(Spot.BIRDHOUSE_VALLEY_SW, 3763, 3755);
		spot(Spot.BIRDHOUSE_MEADOW_N, 3677, 3882);
		spot(Spot.BIRDHOUSE_MEADOW_S, 3679, 3815);
		spot(Spot.HARDWOOD_FOSSIL_E, 3715, 3835);
		spot(Spot.HARDWOOD_FOSSIL_M, 3708, 3833);
		spot(Spot.HARDWOOD_FOSSIL_W, 3702, 3837);
		spot(Spot.SEAWEED_N, 3734, 10274);
		spot(Spot.SEAWEED_S, 3734, 10268);
		spot(Spot.HARDWOOD_ANGLERS, 2472, 2705);
		spot(Spot.HARDWOOD_SAVANNAH, 1683, 2971);
		spot(Spot.CALQUAT_TAI_BWO, 2793, 3099);
		spot(Spot.CALQUAT_KASTORI, 1365, 3035);
		spot(Spot.CALQUAT_CONCH, 3125, 2403);
		spot(Spot.CELASTRUS_GUILD, 1242, 3755);
		spot(Spot.REDWOOD_GUILD, 1233, 3754);
		spot(Spot.CRYSTAL_PRIF, 3292, 6120);
		spot(Spot.SPIRIT_PORT_SARIM, 3056, 3259);
		spot(Spot.SPIRIT_ETCETERIA, 2613, 3858);
		spot(Spot.SPIRIT_BRIMHAVEN, 2799, 3205);
		spot(Spot.SPIRIT_HOSIDIUS, 1690, 3540);
		spot(Spot.SPIRIT_GUILD, 1254, 3753);
		spot(Spot.CACTUS_AL_KHARID, 3313, 3201);
		spot(Spot.CACTUS_GUILD, 1264, 3745);
		spot(Spot.BUSH_CHAMPIONS, 3182, 3356);
		spot(Spot.BUSH_RIMMINGTON, 2938, 3223);
		spot(Spot.BUSH_ARDOUGNE, 2615, 3224);
		spot(Spot.BUSH_ETCETERIA, 2589, 3862);
		spot(Spot.BUSH_GUILD, 1259, 3729);
		spot(Spot.MUSHROOM_CANIFIS, 3449, 3471);
		spot(Spot.BELLADONNA_DRAYNOR, 3084, 3356);
		spot(Spot.BELLADONNA_AUBURNVALE, 1449, 3354);
		spot(Spot.ANIMA_GUILD, 1235, 3724);
		spot(Spot.HESPORI_GUILD, 1182, 10068);

		// Herb, allotment and flower patches share one marker per town.
		town(2663, 3375, Spot.HERB_ARDOUGNE, Spot.ALLOTMENT_ARDOUGNE_N, Spot.ALLOTMENT_ARDOUGNE_S, Spot.FLOWER_ARDOUGNE);
		town(2810, 3462, Spot.HERB_CATHERBY, Spot.ALLOTMENT_CATHERBY_N, Spot.ALLOTMENT_CATHERBY_S, Spot.FLOWER_CATHERBY);
		town(3052, 3309, Spot.HERB_FALADOR, Spot.ALLOTMENT_FALADOR_NW, Spot.ALLOTMENT_FALADOR_SE, Spot.FLOWER_FALADOR);
		town(1729, 3558, Spot.HERB_HOSIDIUS, Spot.ALLOTMENT_HOSIDIUS_NE, Spot.ALLOTMENT_HOSIDIUS_SW, Spot.FLOWER_HOSIDIUS);
		town(3598, 3524, Spot.HERB_PORT_PHASMATYS, Spot.ALLOTMENT_PORT_PHASMATYS_NW, Spot.ALLOTMENT_PORT_PHASMATYS_SE,
			Spot.FLOWER_PORT_PHASMATYS);
		town(1587, 3101, Spot.HERB_CIVITAS, Spot.ALLOTMENT_CIVITAS_N, Spot.ALLOTMENT_CIVITAS_S, Spot.FLOWER_CIVITAS);
		town(1235, 3724, Spot.HERB_GUILD);
		town(1269, 3730, Spot.ALLOTMENT_GUILD_N, Spot.ALLOTMENT_GUILD_S);
		town(1259, 3729, Spot.FLOWER_GUILD);
		town(3289, 6100, Spot.ALLOTMENT_PRIF_N, Spot.ALLOTMENT_PRIF_S, Spot.FLOWER_PRIF);
		town(2828, 3696, Spot.HERB_TROLL_STRONGHOLD);
		town(2847, 3933, Spot.HERB_WEISS);
		town(3789, 2840, Spot.HERB_HARMONY);
		town(3793, 2836, Spot.ALLOTMENT_HARMONY);
		town(1352, 3025, Spot.FLOWER_KASTORI);
		town(3224, 3313, Spot.HOPS_LUMBRIDGE);
		town(2661, 3523, Spot.HOPS_SEERS);
		town(2572, 3102, Spot.HOPS_YANILLE);
		town(2812, 3334, Spot.HOPS_ENTRANA);
		town(1367, 2938, Spot.HOPS_ALDARIN);

		daily(Daily.ZAFF, 3202, 3434);
		daily(Daily.CROMPERTY, 2684, 3323);
		daily(Daily.FLAX, 2744, 3444);
		daily(Daily.RANTZ, 2631, 2982);
		daily(Daily.ROBIN, 3675, 3495);
		daily(Daily.THIRUS, 1489, 3862);
		daily(Daily.BERT, 2550, 3100);
		daily(Daily.HERB_BOXES, 2609, 3115);
		daily(Daily.LUNDAIL, 2535, 4714);
		DAILIES.put(Daily.CONTRACT, FARMING_GUILD);
	}

	private Where()
	{
	}

	private static void spot(Spot s, int x, int y)
	{
		SPOTS.put(s, Tile.of(x, y));
	}

	private static void town(int x, int y, Spot... spots)
	{
		for (Spot s : spots)
		{
			spot(s, x, y);
		}
	}

	private static void daily(Daily d, int x, int y)
	{
		DAILIES.put(d, Tile.of(x, y));
	}

	/** Where the spots of these readings are, each once. */
	static java.util.List<Tile> tiles(java.util.List<Reading> readings)
	{
		java.util.List<Tile> out = new java.util.ArrayList<>();
		for (Reading r : readings)
		{
			Tile t = r == null ? null : of(r.getSpot());
			if (t != null && !out.contains(t))
			{
				out.add(t);
			}
		}
		return out;
	}

	/** Where a patch or bird house space is, or null. */
	public static Tile of(Spot s)
	{
		return SPOTS.get(s);
	}

	/** Where a daily is collected, or null. */
	public static Tile of(Daily d)
	{
		return DAILIES.get(d);
	}
}
