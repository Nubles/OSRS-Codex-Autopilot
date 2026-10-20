package com.codex.autopilot.core;

import net.runelite.api.gameval.ItemID;

/**
 * Bird house spaces on Fossil Island. The space's player variable is 0 when empty, and otherwise
 * encodes the house type and whether it has seeds: (type * 3) + 1 built, + 3 seeded (RuneLite's
 * BirdHouseState). Seeded houses fill up 50 minutes after the seeds go in.
 */
public final class Birdhouse
{
	public static final int FILL_SECONDS = 50 * 60;
	public static final int SEEDS_PER_HOUSE = 10;

	/** Seeds a bird house takes, any one kind, ten at a time. */
	public static final int[] SEEDS = {
		ItemID.BARLEY_SEED, ItemID.HAMMERSTONE_HOP_SEED, ItemID.ASGARNIAN_HOP_SEED, ItemID.JUTE_SEED,
		ItemID.YANILLIAN_HOP_SEED, ItemID.KRANDORIAN_HOP_SEED, ItemID.WILDBLOOD_HOP_SEED,
	};

	public enum State
	{
		EMPTY,
		BUILT,
		SEEDED,
		UNKNOWN
	}

	public enum Type
	{
		NORMAL("Bird house", ItemID.BIRDHOUSE_NORMAL, ItemID.LOGS, 5, 5),
		OAK("Oak bird house", ItemID.BIRDHOUSE_OAK, ItemID.OAK_LOGS, 14, 15),
		WILLOW("Willow bird house", ItemID.BIRDHOUSE_WILLOW, ItemID.WILLOW_LOGS, 24, 25),
		TEAK("Teak bird house", ItemID.BIRDHOUSE_TEAK, ItemID.TEAK_LOGS, 34, 35),
		MAPLE("Maple bird house", ItemID.BIRDHOUSE_MAPLE, ItemID.MAPLE_LOGS, 44, 45),
		MAHOGANY("Mahogany bird house", ItemID.BIRDHOUSE_MAHOGANY, ItemID.MAHOGANY_LOGS, 49, 50),
		YEW("Yew bird house", ItemID.BIRDHOUSE_YEW, ItemID.YEW_LOGS, 59, 60),
		MAGIC("Magic bird house", ItemID.BIRDHOUSE_MAGIC, ItemID.MAGIC_LOGS, 74, 75),
		REDWOOD("Redwood bird house", ItemID.BIRDHOUSE_REDWOOD, ItemID.REDWOOD_LOGS, 89, 90);

		private final String name;
		private final int itemId;
		private final int logId;
		private final int hunterLevel;
		private final int craftingLevel;

		Type(String name, int itemId, int logId, int hunterLevel, int craftingLevel)
		{
			this.name = name;
			this.itemId = itemId;
			this.logId = logId;
			this.hunterLevel = hunterLevel;
			this.craftingLevel = craftingLevel;
		}

		public String getName()
		{
			return name;
		}

		public int getItemId()
		{
			return itemId;
		}

		public int getLogId()
		{
			return logId;
		}

		public int getHunterLevel()
		{
			return hunterLevel;
		}

		public int getCraftingLevel()
		{
			return craftingLevel;
		}

		/** The best house a Hunter level can place. */
		public static Type bestFor(int hunter)
		{
			Type best = NORMAL;
			for (Type t : values())
			{
				if (t.hunterLevel <= hunter)
				{
					best = t;
				}
			}
			return best;
		}

		public static Type forItem(int itemId)
		{
			for (Type t : values())
			{
				if (t.itemId == itemId)
				{
					return t;
				}
			}
			return null;
		}
	}

	private Birdhouse()
	{
	}

	public static State state(int varp)
	{
		if (varp == 0)
		{
			return State.EMPTY;
		}
		if (varp < 0 || varp > Type.values().length * 3)
		{
			return State.UNKNOWN;
		}
		return varp % 3 == 0 ? State.SEEDED : State.BUILT;
	}

	public static Type type(int varp)
	{
		if (varp <= 0 || varp > Type.values().length * 3)
		{
			return null;
		}
		return Type.values()[(varp - 1) / 3];
	}
}
