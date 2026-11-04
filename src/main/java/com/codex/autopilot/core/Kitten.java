package com.codex.autopilot.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The game's facts about a pet kitten, each from the OSRS wiki or RuneLite's gameval.
 *
 * <ul>
 * <li>Ids: the following kitten is {@code NpcID.KITTENPET1}..{@code KITTENPET_BLUEGREY} and
 * {@code KITTENPET_HELL} (5591-5597); the grown cat {@code GROWNCAT}..{@code GROWNCAT_HELL}
 * (1619-1625); the overgrown cat {@code OVERGROWNCAT}..{@code OVERGROWNCAT_HELL} (5598-5604). The
 * kitten carried is {@code ItemID.KITTENOBJECT}..{@code KITTENOBJECT_BLUEGREY} (1555-1560) and
 * {@code KITTENOBJECT_HELL} (7583). (RuneLite gameval; the wiki's Kitten page lists the same
 * npc and item ids.)</li>
 * <li>Hunger: "the player is warned 24 minutes after the kitten was last fed", a second warning
 * 3 minutes later, and "if it isn't fed within 3 more minutes, it runs away" (wiki, Kitten).</li>
 * <li>Attention: "A fresh kitten can go without attention for 25 minutes before it warns the
 * player", it cries it is lonely 7 minutes later and runs away 7 minutes after that; one stroke
 * covers 18 minutes, stroking more than once 25, a ball of wool 51 (wiki, Kitten).</li>
 * <li>Growth: a counter that rises every 150 ticks (90 seconds) while it follows the player; after
 * 120 rises (three hours) it is a cat (wiki, Kitten). A cat becomes overgrown "after another five
 * to six hours" of following (wiki, Cat). Cats need neither food nor attention (wiki, Cat).</li>
 * <li>Care is needed only "while out in the world (not in item form in the bank or inventory)"
 * (wiki, Kitten), so every time here counts only while it follows.</li>
 * </ul>
 */
public final class Kitten
{
	public static final int HUNGER_WARN_MINUTES = 24;
	/** From the first hunger warning to the second, and from the second to running away. */
	public static final int HUNGER_STEP_MINUTES = 3;
	public static final int NEW_KITTEN_MINUTES = 25;
	public static final int STROKE_MINUTES = 18;
	public static final int STROKES_MINUTES = 25;
	public static final int WOOL_MINUTES = 51;
	/** From the first attention warning to the second, and from the second to running away. */
	public static final int LONELY_STEP_MINUTES = 7;
	public static final int GROWTH_TICK_SECONDS = 90;
	public static final int GROWTH_TICKS = 120;
	public static final int GROW_SECONDS = GROWTH_TICK_SECONDS * GROWTH_TICKS;
	public static final int OVERGROWN_MIN_HOURS = 5;
	public static final int OVERGROWN_MAX_HOURS = 6;
	/** What it eats (wiki, Kitten: raw or cooked fish, a bucket of milk). */
	public static final String FOOD = "Any raw or cooked fish, or a bucket of milk";

	/** How far along it is. */
	public enum Stage
	{
		KITTEN("Kitten"),
		CAT("Cat"),
		OVERGROWN("Overgrown cat");

		private final String label;

		Stage(String label)
		{
			this.label = label;
		}

		/** The start of "one stroke lasts 18 min". */
		public String getLabel()
		{
			return label;
		}
	}

	/** How it was last given attention: each buys its own time before it asks again. */
	public enum Care
	{
		NEW(NEW_KITTEN_MINUTES, "a new kitten is content for"),
		STROKE(STROKE_MINUTES, "one stroke lasts"),
		STROKES(STROKES_MINUTES, "stroking twice lasts"),
		WOOL(WOOL_MINUTES, "a ball of wool lasts");

		private final int minutes;
		private final String label;

		Care(int minutes, String label)
		{
			this.minutes = minutes;
			this.label = label;
		}

		/** The wiki's minutes before it asks for attention again. */
		public int getMinutes()
		{
			return minutes;
		}

		public String getLabel()
		{
			return label;
		}
	}

	// net.runelite.api.gameval.NpcID: KITTENPET1, _LIGHT, _BROWN, _BLACK, _BROWNGREY, _BLUEGREY, _HELL.
	static final int[] KITTEN_NPCS = {5591, 5592, 5593, 5594, 5595, 5596, 5597};
	// GROWNCAT, GROWNCAT_LIGHT, _BROWN, _BLACK, _BROWNGREY, _BLUEGREY, _HELL.
	static final int[] CAT_NPCS = {1619, 1620, 1621, 1622, 1623, 1624, 1625};
	// OVERGROWNCAT, OVERGROWNCAT_LIGHT, _BROWN, _BLACK, _BROWNGREY, _BLUEGREY, _HELL.
	static final int[] OVERGROWN_NPCS = {5598, 5599, 5600, 5601, 5602, 5603, 5604};
	// net.runelite.api.gameval.ItemID: KITTENOBJECT, _LIGHT, _BROWN, _BLACK, _BROWNGREY, _BLUEGREY, _HELL.
	static final int[] KITTEN_ITEMS = {1555, 1556, 1557, 1558, 1559, 1560, 7583};

	private static final Map<Integer, Stage> STAGES = new HashMap<>();
	private static final Map<Integer, Integer> ITEMS = new HashMap<>();

	static
	{
		for (int i = 0; i < KITTEN_NPCS.length; i++)
		{
			STAGES.put(KITTEN_NPCS[i], Stage.KITTEN);
			STAGES.put(CAT_NPCS[i], Stage.CAT);
			STAGES.put(OVERGROWN_NPCS[i], Stage.OVERGROWN);
			ITEMS.put(KITTEN_NPCS[i], KITTEN_ITEMS[i]);
			ITEMS.put(CAT_NPCS[i], KITTEN_ITEMS[i]);
			ITEMS.put(OVERGROWN_NPCS[i], KITTEN_ITEMS[i]);
		}
	}

	private Kitten()
	{
	}

	/** The stage a follower's npc id is, or null when it is no kitten or cat of the player's. */
	public static Stage stage(int npcId)
	{
		return STAGES.get(npcId);
	}

	/** A kitten of the same colour, to picture the step by; the grey and black one when unknown. */
	public static int item(int npcId)
	{
		Integer item = ITEMS.get(npcId);
		return item == null ? KITTEN_ITEMS[0] : item;
	}

	/** Whether an item is a kitten carried, which keeps its needs on hold. */
	public static boolean isKittenItem(int itemId)
	{
		for (int id : KITTEN_ITEMS)
		{
			if (id == itemId)
			{
				return true;
			}
		}
		return false;
	}
}
