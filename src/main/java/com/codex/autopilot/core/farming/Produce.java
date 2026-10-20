/*
 * Copyright (c) 2019 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.codex.autopilot.core.farming;

import net.runelite.api.gameval.ItemID;

/**
 * What can grow in the patches a round visits. Growth timings are RuneLite's (Time Tracking
 * plugin); the seed, level and gardener's payment are added so a round knows what to plant and
 * what to bring.
 */
public enum Produce
{
	WEEDS("Weeds", null, ItemID.WEEDS, 5, 4, 0, 1, -1, 1),

	// Bushes
	REDBERRIES("Redberry", PatchKind.BUSH, ItemID.REDBERRIES, 20, 6, 20, 5, ItemID.REDBERRY_BUSH_SEED, 10, ItemID.SACK_CABBAGE_10, 4),
	CADAVABERRIES("Cadavaberry", PatchKind.BUSH, ItemID.CADAVABERRIES, 20, 7, 20, 5, ItemID.CADAVABERRY_BUSH_SEED, 22, ItemID.BASKET_TOMATO_5, 3),
	DWELLBERRIES("Dwellberry", PatchKind.BUSH, ItemID.DWELLBERRIES, 20, 8, 20, 5, ItemID.DWELLBERRY_BUSH_SEED, 36, ItemID.BASKET_STRAWBERRY_5, 3),
	JANGERBERRIES("Jangerberry", PatchKind.BUSH, ItemID.JANGERBERRIES, 20, 9, 20, 5, ItemID.JANGERBERRY_BUSH_SEED, 48, ItemID.WATERMELON, 6),
	WHITEBERRIES("Whiteberry", PatchKind.BUSH, ItemID.WHITE_BERRIES, 20, 9, 20, 5, ItemID.WHITEBERRY_BUSH_SEED, 59, ItemID.BITTERCAP_MUSHROOM, 8),
	POISON_IVY("Poison ivy", PatchKind.BUSH, ItemID.POISONIVY_BERRIES, 20, 9, 20, 5, ItemID.POISONIVY_BUSH_SEED, 70),

	// Cactus
	CACTUS("Cactus", PatchKind.CACTUS, ItemID.CACTUS_SPINE, 80, 8, 20, 4, ItemID.CACTUS_SEED, 55, ItemID.CADAVABERRIES, 6),
	POTATO_CACTUS("Potato cactus", PatchKind.CACTUS, ItemID.CACTUS_POTATO, 10, 8, 5, 7, ItemID.POTATO_CACTUS_SEED, 64, ItemID.SNAPE_GRASS, 8),

	// Hardwood
	TEAK("Teak", PatchKind.HARDWOOD_TREE, ItemID.TEAK_LOGS, 640, 8, 0, 1, ItemID.PLANTPOT_TEAK_SAPLING, 35, ItemID.LIMPWURT_ROOT, 15),
	MAHOGANY("Mahogany", PatchKind.HARDWOOD_TREE, ItemID.MAHOGANY_LOGS, 640, 9, 0, 1, ItemID.PLANTPOT_MAHOGANY_SAPLING, 55, ItemID.YANILLIAN_HOPS, 25),
	CAMPHOR("Camphor", PatchKind.HARDWOOD_TREE, ItemID.CAMPHOR_LOGS, 640, 9, 0, 1, ItemID.PLANTPOT_CAMPHOR_SAPLING, 66, ItemID.WHITE_BERRIES, 10),
	IRONWOOD("Ironwood", PatchKind.HARDWOOD_TREE, ItemID.IRONWOOD_LOGS, 640, 9, 0, 1, ItemID.PLANTPOT_IRONWOOD_SAPLING, 80, ItemID.CURRY_LEAF, 10),
	ROSEWOOD("Rosewood", PatchKind.HARDWOOD_TREE, ItemID.ROSEWOOD_LOGS, 640, 10, 0, 1, ItemID.PLANTPOT_ROSEWOOD_SAPLING, 92, ItemID.DRAGONFRUIT, 8),

	// Anima
	ATTAS("Attas", PatchKind.ANIMA, ItemID.ANIMA_ATTAS, 640, 9, 0, 1, ItemID.ATTAS_SEED, 76),
	IASOR("Iasor", PatchKind.ANIMA, ItemID.ANIMA_IASOR, 640, 9, 0, 1, ItemID.IASOR_SEED, 76),
	KRONOS("Kronos", PatchKind.ANIMA, ItemID.ANIMA_KRONOS, 640, 9, 0, 1, ItemID.KRONOS_SEED, 76),

	// Special
	SEAWEED("Giant seaweed", PatchKind.SEAWEED, ItemID.GIANT_SEAWEED, 10, 5, 0, 4, ItemID.SEAWEED_SEED, 23),
	MUSHROOM("Bittercap", PatchKind.MUSHROOM, ItemID.BITTERCAP_MUSHROOM, 40, 7, 0, 7, ItemID.MUSHROOM_SEED, 53),
	BELLADONNA("Belladonna", PatchKind.BELLADONNA, ItemID.NIGHTSHADE, 80, 5, 0, 1, ItemID.BELLADONNA_SEED, 63),
	CALQUAT("Calquat", PatchKind.CALQUAT, ItemID.CALQUAT_FRUIT, 160, 9, 0, 7, ItemID.PLANTPOT_CALQUAT_SAPLING, 72, ItemID.POISONIVY_BERRIES, 8),
	SPIRIT_TREE("Spirit tree", PatchKind.SPIRIT_TREE, ItemID.PLANTPOT_SPIRIT_TREE_SAPLING, 320, 13, 0, 1, ItemID.PLANTPOT_SPIRIT_TREE_SAPLING, 83,
		ItemID.MM_MONKEY_NUTS, 5, ItemID.MM_MONKEY_BAR, 1, ItemID.LUNAR_GROUNDTOOTH, 1),
	CELASTRUS("Celastrus", PatchKind.CELASTRUS, ItemID.BATTLESTAFF, 160, 6, 0, 4, ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING, 85, ItemID.CACTUS_POTATO, 8),
	REDWOOD("Redwood", PatchKind.REDWOOD, ItemID.REDWOOD_LOGS, 640, 11, 0, 1, ItemID.PLANTPOT_REDWOOD_TREE_SAPLING, 90, ItemID.DRAGONFRUIT, 6),
	HESPORI("Hespori", PatchKind.HESPORI, ItemID.HESPORI_SEED, 640, 4, 0, 2, ItemID.HESPORI_SEED, 65),
	CRYSTAL_TREE("Crystal tree", PatchKind.CRYSTAL_TREE, ItemID.PLANTPOT_CRYSTAL_TREE_SAPLING, 80, 7, 0, 1, ItemID.PLANTPOT_CRYSTAL_TREE_SAPLING, 74);

	private final String name;
	private final PatchKind kind;
	private final int itemId;
	private final int tickrate;
	private final int stages;
	private final int regrowTickrate;
	private final int harvestStages;
	private final int seedId;
	private final int level;
	/** Item, amount pairs the gardener takes to protect the crop, empty when it cannot be protected. */
	private final int[] payment;

	Produce(String name, PatchKind kind, int itemId, int tickrate, int stages, int regrowTickrate, int harvestStages,
		int seedId, int level, int... payment)
	{
		this.name = name;
		this.kind = kind;
		this.itemId = itemId;
		this.tickrate = tickrate;
		this.stages = stages;
		this.regrowTickrate = regrowTickrate;
		this.harvestStages = harvestStages;
		this.seedId = seedId;
		this.level = level;
		this.payment = payment;
	}

	public String getName()
	{
		return name;
	}

	public PatchKind getKind()
	{
		return kind;
	}

	/** The item the crop is shown with. */
	public int getItemId()
	{
		return itemId;
	}

	/** Minutes per growth tick. */
	public int getTickrate()
	{
		return tickrate;
	}

	/** States while growing, typically growth ticks plus one. */
	public int getStages()
	{
		return stages;
	}

	/** Minutes for the produce to regrow once picked, or zero when it does not. */
	public int getRegrowTickrate()
	{
		return regrowTickrate;
	}

	/** States while harvestable: the lives of the patch. */
	public int getHarvestStages()
	{
		return harvestStages;
	}

	/** What is planted: a seed, spore or sapling. */
	public int getSeedId()
	{
		return seedId;
	}

	/** Farming level needed to plant it. */
	public int getLevel()
	{
		return level;
	}

	public int[] getPayment()
	{
		return payment.clone();
	}

	public boolean isProtectable()
	{
		return payment.length > 0;
	}

	/** Minutes from planting to fully grown. */
	public int growMinutes()
	{
		return tickrate * Math.max(0, stages - 1);
	}

	/** Every crop that grows in a kind of patch, lowest level first. */
	public static java.util.List<Produce> forKind(PatchKind kind)
	{
		java.util.List<Produce> list = new java.util.ArrayList<>();
		for (Produce p : values())
		{
			if (p.kind == kind)
			{
				list.add(p);
			}
		}
		list.sort(java.util.Comparator.comparingInt(Produce::getLevel));
		return list;
	}
}
