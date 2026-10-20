package com.codex.autopilot.core;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * The once-a-day buys and claims an account can pick up, as Codex Rounds reads them. Unlock and claimed checks follow RuneLite's
 * Daily Task Indicator plugin; amounts follow the diary reward tables.
 */
public enum Daily
{
	ZAFF("Zaff's battlestaves", "Varrock", ItemID.BATTLESTAFF, "Buy battlestaves from Zaff", NpcID.ZAFF),
	CROMPERTY("Cromperty's essence", "East Ardougne", ItemID.BLANKRUNE_HIGH, "Claim noted essence from Wizard Cromperty",
		NpcID.CROMPERTY_POST_DIARY, NpcID.CROMPERTY_PRE_DIARY),
	FLAX("Flax to bowstrings", "Seers' Village", ItemID.BOW_STRING, "Hand your flax to the Flax keeper", NpcID.DIARY_FLAX_KEEPER),
	RANTZ("Rantz's ogre arrows", "Feldip Hills", ItemID.OGRE_ARROW, "Claim ogre arrows from Rantz",
		NpcID.RANTZ, NpcID._100_RANTZ, NpcID._100_RANTZ1, NpcID._100_RANTZ2),
	ROBIN("Robin's bonemeal and slime", "Port Phasmatys", ItemID.POT_BONEMEAL, "Hand your bones to Robin", NpcID.AHOY_ROBIN),
	THIRUS("Thirus's dynamite", "Lovakengj", ItemID.LOVAKENGJ_DYNAMITE_FUSED, "Claim dynamite from Thirus",
		NpcID.LOVAKENGJ_THIRUS_POST_DIARY, NpcID.LOVAKENGJ_THIRUS, NpcID.LOVAKENGJ_THIRUS_PRE_DIARY),
	BERT("Bert's sand", "Yanille", ItemID.BUCKET_SAND, "Ask Bert for your sand", NpcID.HANDSAND_BERT_2OPS, NpcID.HANDSAND_BERT_1OP, NpcID.HANDSAND_BERT),
	HERB_BOXES("Herb boxes", "Nightmare Zone", ItemID.NZONE_HERBBOX, "Buy herb boxes from Dominic Onion", NpcID.NZONE_HOST),
	LUNDAIL("Lundail's runes", "Mage Arena", ItemID.DEATHRUNE, "Claim runes from Lundail", NpcID.MAGEARENA_RUNESHOP),
	/** Not daily, but a chore of the same shape: a talk at a place, ready again whenever a contract is done. */
	CONTRACT("Farming contract", "Farming Guild", ItemID.SEEDBOX, "Talk to Guildmaster Jane about your contract",
		NpcID.FARMING_GUILD_MASTER, NpcID.FARMING_GUILD_MASTER_1OP, NpcID.FARMING_GUILD_MASTER_2OP);

	public static final int ZAFF_PRICE = 7_000;
	public static final int HERB_BOX_POINTS = 9_500;
	public static final int HERB_BOX_MAX = 15;
	public static final int SAND_QUEST_COMPLETE = 160;
	public static final int ROBIN_PER_TIER = 13;
	/** Robin takes unnoted bones of any kind; the common ones count as packed. */
	static final int[] ROBIN_BONES = {ItemID.BONES, ItemID.BAT_BONES, ItemID.BIG_BONES, ItemID.BABYDRAGON_BONES, ItemID.DRAGON_BONES,
		ItemID.WOLF_BONES, ItemID.ZOGRE_BONES, ItemID.WYVERN_BONES, ItemID.LAVA_DRAGON_BONES};
	public static final int BERT_SAND = 84;

	private final String label;
	private final String place;
	private final int itemId;
	private final String action;
	private final int[] npcs;

	Daily(String label, String place, int itemId, String action, int... npcs)
	{
		this.label = label;
		this.place = place;
		this.itemId = itemId;
		this.action = action;
		this.npcs = npcs;
	}

	public String getLabel()
	{
		return label;
	}

	/** Where it is collected, such as "Varrock". */
	public String getPlace()
	{
		return place;
	}

	/** The item shown on the board for this daily. */
	public int getItemId()
	{
		return itemId;
	}

	/** What to do at the stop, as a step title. */
	public String getAction()
	{
		return action;
	}

	public int[] getNpcs()
	{
		return npcs.clone();
	}

	public boolean isNpc(int id)
	{
		for (int n : npcs)
		{
			if (n == id)
			{
				return true;
			}
		}
		return false;
	}

	/** Whether it comes back at the daily reset; a contract only comes back when it is done. */
	public boolean resetsDaily()
	{
		return this != CONTRACT;
	}

	/** Whether the account has earned this daily at all. */
	public boolean unlocked(Vars v)
	{
		switch (this)
		{
			case ZAFF:
				return done(v, VarbitID.VARROCK_DIARY_EASY_COMPLETE);
			case CROMPERTY:
				return done(v, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE);
			case FLAX:
				return done(v, VarbitID.KANDARIN_DIARY_EASY_COMPLETE);
			case RANTZ:
				return done(v, VarbitID.WESTERN_DIARY_EASY_COMPLETE);
			case ROBIN:
				return done(v, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE);
			case THIRUS:
				return done(v, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE);
			case BERT:
				return v.varbit(VarbitID.IRONMAN) != 2 && v.varbit(VarbitID.HANDSAND_QUEST) >= SAND_QUEST_COMPLETE;
			case HERB_BOXES:
				return v.varbit(VarbitID.IRONMAN) == 0 && v.varp(VarPlayerID.NZONE_REWARDPOINTS) >= HERB_BOX_POINTS;
			case LUNDAIL:
				return done(v, VarbitID.WILDERNESS_DIARY_EASY_COMPLETE);
			case CONTRACT:
				return Contract.known(v);
			default:
				return false;
		}
	}

	/** Whether today's claim is still waiting. */
	public boolean claimable(Vars v)
	{
		if (!unlocked(v))
		{
			return false;
		}
		switch (this)
		{
			case ZAFF:
				return v.varbit(VarbitID.ZAFF_LAST_CLAIMED) == 0;
			case CROMPERTY:
				return v.varbit(VarbitID.ARDOUGNE_FREE_ESSENCE) == 0;
			case FLAX:
				return v.varbit(VarbitID.SEERS_FREE_FLAX) == 0;
			case RANTZ:
				return v.varbit(VarbitID.WESTERN_RANTZ_ARROWS) == 0;
			case ROBIN:
				return v.varbit(VarbitID.MORYTANIA_SLIME_CLAIMED) < amount(v);
			case THIRUS:
				return v.varbit(VarbitID.KOUREND_FREE_DYNAMITE) == 0;
			case BERT:
				return v.varbit(VarbitID.YANILLE_SAND_CLAIMED) == 0;
			case HERB_BOXES:
				return v.varbit(VarbitID.NZONE_HERBBOXES_PURCHASED) < HERB_BOX_MAX;
			case LUNDAIL:
				return v.varbit(VarbitID.LUNDAIL_LAST_CLAIMED) == 0;
			case CONTRACT:
				return Contract.complete(v) || Contract.type(v) == 0;
			default:
				return false;
		}
	}

	/** How many come today, or 0 when the amount is random. */
	public int amount(Vars v)
	{
		int t;
		switch (this)
		{
			case ZAFF:
				return pick(tier(v, VarbitID.VARROCK_DIARY_EASY_COMPLETE), 15, 30, 60, 120);
			case CROMPERTY:
				t = tier(v, VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE);
				return pick(t, 0, 100, 150, 250);
			case FLAX:
				return pick(tier(v, VarbitID.KANDARIN_DIARY_EASY_COMPLETE), 30, 60, 120, 250);
			case RANTZ:
				return pick(tier(v, VarbitID.WESTERN_DIARY_EASY_COMPLETE), 25, 50, 100, 150);
			case ROBIN:
				t = tier(v, VarbitID.MORYTANIA_DIARY_EASY_COMPLETE);
				return Math.max(0, t - 1) * ROBIN_PER_TIER;
			case THIRUS:
				return pick(tier(v, VarbitID.KOUREND_DIARY_EASY_COMPLETE), 0, 20, 40, 80);
			case BERT:
				return BERT_SAND;
			case HERB_BOXES:
				int left = HERB_BOX_MAX - v.varbit(VarbitID.NZONE_HERBBOXES_PURCHASED);
				return Math.max(0, Math.min(left, v.varp(VarPlayerID.NZONE_REWARDPOINTS) / HERB_BOX_POINTS));
			default:
				return 0;
		}
	}

	/** Robin hands out a share at a time; how many are left today. */
	public int remaining(Vars v)
	{
		if (this == ROBIN)
		{
			return Math.max(0, amount(v) - v.varbit(VarbitID.MORYTANIA_SLIME_CLAIMED));
		}
		return amount(v);
	}

	/** What the board says comes back, such as "120 battlestaves". */
	public String reward(Vars v)
	{
		int n = remaining(v);
		switch (this)
		{
			case ZAFF:
				return n + " battlestaves for " + Format.coins((long) n * ZAFF_PRICE);
			case CROMPERTY:
				return n + " noted pure essence";
			case FLAX:
				return n + " flax spun into bowstrings";
			case RANTZ:
				return n + " ogre arrows";
			case ROBIN:
				return n + " bonemeal and buckets of slime";
			case THIRUS:
				return n + " dynamite";
			case BERT:
				return n + " buckets of sand";
			case HERB_BOXES:
				return n + " herb boxes for " + Format.number((long) n * HERB_BOX_POINTS) + " points";
			case LUNDAIL:
				return "A random handful of runes";
			case CONTRACT:
				int type = Contract.type(v);
				if (Contract.complete(v))
				{
					return "Done: hand it in for a seed pack and take the next";
				}
				if (type == 0 || Contract.cropName(type) == null)
				{
					return "No contract running: take one";
				}
				return Contract.cropName(type) + " in the guild's " + Contract.patch(type);
			default:
				return "";
		}
	}

	/** What the player needs to bring for it, or null. */
	public Need need(Vars v)
	{
		int n = remaining(v);
		switch (this)
		{
			case ZAFF:
				return new Need("coins", n * ZAFF_PRICE, ItemID.COINS);
			case FLAX:
				return new Need("flax", n, ItemID.FLAX, ItemID.FLAX + 1);
			case ROBIN:
				return new Need("bones", n, ROBIN_BONES);
			default:
				return null;
		}
	}

	/** Diary tiers done from easy upwards: easy is 1, elite 4. Each tier's varbit follows the last. */
	static int tier(Vars v, int easyVarbit)
	{
		int t = 0;
		for (int i = 0; i < 4; i++)
		{
			if (v.varbit(easyVarbit + i) == 1)
			{
				t = i + 1;
			}
		}
		return t;
	}

	private static int pick(int tier, int... byTier)
	{
		return tier <= 0 ? 0 : byTier[Math.min(tier, byTier.length) - 1];
	}

	private static boolean done(Vars v, int varbit)
	{
		return v.varbit(varbit) == 1;
	}
}
