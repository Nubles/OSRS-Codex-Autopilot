package com.codex.autopilot.core;

import com.codex.autopilot.core.farming.Produce;
import net.runelite.api.gameval.VarbitID;

/**
 * Guildmaster Jane's farming contracts. The contract's crop is a varbit whose values follow RuneLite's Time Tracking
 * plugin; the guild patches Rounds tends (bush, cactus, celastrus, redwood) are planted with the contract crop.
 */
public final class Contract
{
	private static final String[] NAMES = new String[52];

	static
	{
		String[] list = {
			"", "Redberry", "Cadavaberry", "Dwellberry", "Jangerberry", "Whiteberry", "Poison ivy",
			"Oak", "Willow", "Maple", "Yew", "Magic",
			"Apple", "Banana", "Orange", "Curry", "Pineapple", "Papaya", "Palm", "Dragonfruit",
			"Guam", "Marrentill", "Tarromin", "Harralander", "Ranarr", "Toadflax", "Irit", "Avantoe", "Kwuarm",
			"Snapdragon", "Cadantine", "Lantadyme", "Dwarf weed", "Torstol",
			"Marigold", "Rosemary", "Nasturtium", "Woad", "Limpwurt", "White lily",
			"Potato", "Onion", "Cabbage", "Tomato", "Sweetcorn", "Strawberry", "Watermelon", "Snape grass",
			"Cactus", "Potato cactus", "Redwood", "Celastrus",
		};
		System.arraycopy(list, 0, NAMES, 0, list.length);
	}

	private Contract()
	{
	}

	public static int type(Vars v)
	{
		return v.varbit(VarbitID.FARMGUILD_CONTRACT_TYPE);
	}

	public static boolean complete(Vars v)
	{
		return v.varbit(VarbitID.FARMGUILD_CONTRACT_COMPLETE) > 0;
	}

	/** Whether the player has ever been offered contracts. */
	public static boolean known(Vars v)
	{
		return v.varbit(VarbitID.FARMGUILD_CONTRACT_DISCUSSED) > 0 || type(v) > 0 || v.varbit(VarbitID.FARMGUILD_CONTRACT_COUNT) > 0;
	}

	/** The crop's name, or null for no contract or an unknown one. */
	public static String cropName(int type)
	{
		return type > 0 && type < NAMES.length ? NAMES[type] : null;
	}

	/** Which guild patch the crop grows in, for the board. */
	public static String patch(int type)
	{
		if (type >= 1 && type <= 6)
		{
			return "bush patch";
		}
		if (type <= 11)
		{
			return "tree patch";
		}
		if (type <= 19)
		{
			return "fruit tree patch";
		}
		if (type <= 33)
		{
			return "herb patch";
		}
		if (type <= 39)
		{
			return "flower patch";
		}
		if (type <= 47)
		{
			return "allotment";
		}
		if (type <= 49)
		{
			return "cactus patch";
		}
		return type == 50 ? "redwood patch" : "celastrus patch";
	}

	/** The crop when it grows in a guild patch Rounds tends, else null. */
	public static Produce produce(int type)
	{
		switch (type)
		{
			case 1:
				return Produce.REDBERRIES;
			case 2:
				return Produce.CADAVABERRIES;
			case 3:
				return Produce.DWELLBERRIES;
			case 4:
				return Produce.JANGERBERRIES;
			case 5:
				return Produce.WHITEBERRIES;
			case 6:
				return Produce.POISON_IVY;
			case 48:
				return Produce.CACTUS;
			case 49:
				return Produce.POTATO_CACTUS;
			case 50:
				return Produce.REDWOOD;
			case 51:
				return Produce.CELASTRUS;
			default:
				return null;
		}
	}
}
