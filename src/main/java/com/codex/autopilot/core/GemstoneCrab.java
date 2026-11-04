package com.codex.autopilot.core;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

/**
 * What the game and the OSRS wiki say about the Gemstone Crab, and nothing more. Every fact here
 * is checked:
 *
 * <ul>
 * <li>"The Gemstone Crab is a world boss that moves between three mines in the Tlati Rainforest
 * approximately every 10 minutes." The three are the North, East and South-west Tlati Rainforest
 * mines (wiki, Tlati Rainforest), each "home to one of the three spawns of the Gemstone Crab"
 * (wiki, each mine's page). Their tiles are RuneLite's own world map mine markers
 * (worldmap MiningSiteLocation TLATI_RAINFOREST_NORTH, _EAST and _SOUTH, each with a Gemstone Crab
 * rock).</li>
 * <li>The wiki gives no order for its moves, so none is assumed: the order is learned from the
 * player's own sightings (see {@link CrabLog}).</li>
 * <li>"The health bar of the crab is not affected by player damage", it drains over time to show
 * when it burrows; so the time left is read from the bar and its pace measured, never assumed.</li>
 * <li>"The shell will remain for 90 seconds before disappearing into the ground." The 16 players who
 * dealt the most damage "may mine the shell for three random unnoted uncut gems", opal up to
 * dragonstone, with a pickaxe.</li>
 * <li>The game's words, quoted on the wiki page: {@link #BURROWS}, {@link #ELIGIBLE},
 * {@link #NOT_ELIGIBLE} and {@link #MINED}.</li>
 * <li>Ids are RuneLite's gameval: NpcID.GEMSTONE_CRAB (14779) and GEMSTONE_CRAB_REMAINS (14780,
 * "Gemstone Crab Shell"), the uncut gems' ItemIDs.</li>
 * </ul>
 */
public final class GemstoneCrab
{
	public static final int CRAB = NpcID.GEMSTONE_CRAB;
	public static final int SHELL = NpcID.GEMSTONE_CRAB_REMAINS;

	/** Wiki: "The gemstone crab burrows away, leaving a piece of its shell behind." */
	public static final String BURROWS = "The gemstone crab burrows away, leaving a piece of its shell behind.";
	/** Wiki: said to the 16 players who dealt the most damage. */
	public static final String ELIGIBLE = "You gained enough understanding of the crab to mine from its remains.";
	/** Wiki: said to everyone else who tries to mine it. */
	public static final String NOT_ELIGIBLE = "Your understanding of the gemstone crab is not great enough to mine its shell.";
	/** Wiki: "You mine an uncut [gem] from the crab shell." */
	public static final String MINED = "You mine an uncut ";
	static final String MINED_END = " from the crab shell.";

	/** Wiki: "The shell will remain for 90 seconds before disappearing into the ground." */
	public static final int SHELL_SECONDS = 90;
	/** Wiki: "three random unnoted uncut gems". */
	public static final int GEMS_PER_SHELL = 3;
	/** Wiki: "approximately every 10 minutes"; only used until the player's own crabs are timed. */
	public static final int STAY_SECONDS = 600;
	/** How far from a mine's marker a crab, or the player, still counts as at that mine. */
	public static final int NEAR = 30;

	private GemstoneCrab()
	{
	}

	/** The crab's three spawns, by the wiki's names, at RuneLite's world map markers. */
	public enum Mine
	{
		NORTH("North mine", "the north mine", Tile.of(1272, 3173)),
		EAST("East mine", "the east mine", Tile.of(1352, 3112)),
		SOUTH_WEST("South-west mine", "the south-west mine", Tile.of(1238, 3043));

		private final String label;
		private final String phrase;
		private final Tile tile;

		Mine(String label, String phrase, Tile tile)
		{
			this.label = label;
			this.phrase = phrase;
			this.tile = tile;
		}

		/** "North mine". */
		public String getLabel()
		{
			return label;
		}

		/** "the north mine", for the middle of a sentence. */
		public String getPhrase()
		{
			return phrase;
		}

		public Tile getTile()
		{
			return tile;
		}

		/** The mine a tile is at, or null when it is near none. */
		public static Mine at(Tile t)
		{
			if (t == null)
			{
				return null;
			}
			Mine best = null;
			int bestD = Integer.MAX_VALUE;
			for (Mine m : values())
			{
				int d = m.tile.distance(t);
				if (d <= NEAR && d < bestD)
				{
					best = m;
					bestD = d;
				}
			}
			return best;
		}

		/** A saved name back to its mine, or null. */
		static Mine named(String name)
		{
			if (name == null)
			{
				return null;
			}
			try
			{
				return valueOf(name);
			}
			catch (IllegalArgumentException e)
			{
				return null;
			}
		}
	}

	/** The shell's gems, as the wiki's drop table lists them, commonest first. */
	public enum Gem
	{
		OPAL("opal", "Opal", ItemID.UNCUT_OPAL),
		JADE("jade", "Jade", ItemID.UNCUT_JADE),
		RED_TOPAZ("red topaz", "Red topaz", ItemID.UNCUT_RED_TOPAZ),
		SAPPHIRE("sapphire", "Sapphire", ItemID.UNCUT_SAPPHIRE),
		EMERALD("emerald", "Emerald", ItemID.UNCUT_EMERALD),
		RUBY("ruby", "Ruby", ItemID.UNCUT_RUBY),
		DIAMOND("diamond", "Diamond", ItemID.UNCUT_DIAMOND),
		DRAGONSTONE("dragonstone", "Dragonstone", ItemID.UNCUT_DRAGONSTONE);

		private final String word;
		private final String label;
		private final int itemId;

		Gem(String word, String label, int itemId)
		{
			this.word = word;
			this.label = label;
			this.itemId = itemId;
		}

		public String getLabel()
		{
			return label;
		}

		public int getItemId()
		{
			return itemId;
		}

		/** The gem in "You mine an uncut ruby from the crab shell.", or null when the line is not that. */
		public static Gem mined(String message)
		{
			if (message == null || !message.startsWith(MINED) || !message.endsWith(MINED_END))
			{
				return null;
			}
			String word = message.substring(MINED.length(), message.length() - MINED_END.length()).trim();
			for (Gem g : values())
			{
				if (g.word.equalsIgnoreCase(word))
				{
					return g;
				}
			}
			return null;
		}
	}
}
