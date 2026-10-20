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

/**
 * How each kind of patch reports its state through its varbit. Adapted from RuneLite's Time
 * Tracking plugin (net.runelite.client.plugins.timetracking.farming.PatchImplementation), keeping
 * only the patches a round visits and noting, from each object's menu, what the patch wants doing.
 */
public enum PatchKind
{
	MUSHROOM("", false)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Mushroom patch[Rake,Inspect,Guide] 8314,8313,8312,8311
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 9)
				{
					// Bittercap Mushrooms[Inspect,Guide] 8315,8316,8317,8318,8319,8320
					return new PatchState(Produce.MUSHROOM, CropState.GROWING, value - 4, Action.WAIT);
				}
				if (value >= 10 && value <= 15)
				{
					// Bittercap Mushrooms[Pick,Inspect,Guide] 8321,8322,8323,8324,8325,8326
					return new PatchState(Produce.MUSHROOM, CropState.HARVESTABLE, value - 10, Action.HARVEST);
				}
				if (value >= 16 && value <= 20)
				{
					// Diseased Bittercap Mushrooms[Cure,Inspect,Guide] 8327,8328,8329,8330,8331
					return new PatchState(Produce.MUSHROOM, CropState.DISEASED, value - 15, Action.CURE);
				}
				if (value >= 21 && value <= 25)
				{
					// Dead Bittercap Mushrooms[Clear,Inspect,Guide] 8332,8333,8334,8335,8336
					return new PatchState(Produce.MUSHROOM, CropState.DEAD, value - 20, Action.CLEAR);
				}
				if (value >= 26 && value <= 255)
				{
					// Mushroom patch[Rake,Inspect,Guide] 8314,8314,8314,8314,8314,8314,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	HESPORI("", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Hespori patch[Rake,Inspect,Guide] 33722,33723,33724,33725
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 6)
				{
					// Hespori[Inspect,Guide] 33726,33727,33728
					return new PatchState(Produce.HESPORI, CropState.GROWING, value - 4, Action.WAIT);
				}
				if (value >= 7 && value <= 8)
				{
					// Hespori[Harvest,Inspect,Guide,Clear] 33729,33730
					return new PatchState(Produce.HESPORI, CropState.HARVESTABLE, value - 7, Action.HARVEST);
				}
				if (value == 9)
				{
					// Hespori patch[Rake,Inspect,Guide] 33722
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	BUSH("", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7575,7574,7573
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value == 4)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 5 && value <= 9)
				{
					// Redberry bush[Inspect,Guide] 7692,7693,7694,7695,7696
					return new PatchState(Produce.REDBERRIES, CropState.GROWING, value - 5, Action.WAIT);
				}
				if (value >= 10 && value <= 14)
				{
					// Redberry bush[Clear,Inspect,Guide,Pick-from] 7697,7701,7701,7701,7701
					return new PatchState(Produce.REDBERRIES, CropState.HARVESTABLE, value - 10, Action.HARVEST);
				}
				if (value >= 15 && value <= 20)
				{
					// Cadavaberry bush[Inspect,Guide] 7581,7582,7583,7584,7585,7586
					return new PatchState(Produce.CADAVABERRIES, CropState.GROWING, value - 15, Action.WAIT);
				}
				if (value >= 21 && value <= 25)
				{
					// Cadavaberry bush[Clear,Inspect,Guide,Pick-from] 7587,7591,7591,7591,7591
					return new PatchState(Produce.CADAVABERRIES, CropState.HARVESTABLE, value - 21, Action.HARVEST);
				}
				if (value >= 26 && value <= 32)
				{
					// Dwellberry bush[Inspect,Guide] 7605,7606,7607,7608,7609,7610,7611
					return new PatchState(Produce.DWELLBERRIES, CropState.GROWING, value - 26, Action.WAIT);
				}
				if (value >= 33 && value <= 37)
				{
					// Dwellberry bush[Clear,Inspect,Guide,Pick-from] 7612,7616,7616,7616,7616
					return new PatchState(Produce.DWELLBERRIES, CropState.HARVESTABLE, value - 33, Action.HARVEST);
				}
				if (value >= 38 && value <= 45)
				{
					// Jangerberry bush[Inspect,Guide] 7632,7633,7634,7635,7636,7637,...
					return new PatchState(Produce.JANGERBERRIES, CropState.GROWING, value - 38, Action.WAIT);
				}
				if (value >= 46 && value <= 50)
				{
					// Jangerberry bush[Clear,Inspect,Guide,Pick-from] 7640,7644,7644,7644,7644
					return new PatchState(Produce.JANGERBERRIES, CropState.HARVESTABLE, value - 46, Action.HARVEST);
				}
				if (value >= 51 && value <= 58)
				{
					// Whiteberry bush[Inspect,Guide] 7713,7714,7715,7716,7717,7718,...
					return new PatchState(Produce.WHITEBERRIES, CropState.GROWING, value - 51, Action.WAIT);
				}
				if (value >= 59 && value <= 63)
				{
					// Whiteberry bush[Clear,Inspect,Guide,Pick-from] 7721,7725,7725,7725,7725
					return new PatchState(Produce.WHITEBERRIES, CropState.HARVESTABLE, value - 59, Action.HARVEST);
				}
				if (value >= 64 && value <= 69)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 70 && value <= 74)
				{
					// Diseased redberry bush[Prune,Inspect,Guide] 7703,7704,7705,7706,7707
					return new PatchState(Produce.REDBERRIES, CropState.DISEASED, value - 69, Action.PRUNE);
				}
				if (value >= 75 && value <= 79)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 80 && value <= 85)
				{
					// Diseased cadavaberry bush[Prune,Inspect,Guide] 7593,7594,7595,7596,7597,7598
					return new PatchState(Produce.CADAVABERRIES, CropState.DISEASED, value - 79, Action.PRUNE);
				}
				if (value >= 86 && value <= 90)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 91 && value <= 97)
				{
					// Diseased dwellberry bush[Prune,Inspect,Guide] 7618,7619,7620,7621,7622,7623,7624
					return new PatchState(Produce.DWELLBERRIES, CropState.DISEASED, value - 90, Action.PRUNE);
				}
				if (value >= 98 && value <= 102)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 103 && value <= 110)
				{
					// Diseased jangerberry bush[Prune,Inspect,Guide] 7646,7647,7648,7649,7650,7651,...
					return new PatchState(Produce.JANGERBERRIES, CropState.DISEASED, value - 102, Action.PRUNE);
				}
				if (value >= 111 && value <= 115)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 116 && value <= 123)
				{
					// Diseased whiteberry bush[Prune,Inspect,Guide] 7727,7728,7729,7730,7731,7732,...
					return new PatchState(Produce.WHITEBERRIES, CropState.DISEASED, value - 115, Action.PRUNE);
				}
				if (value >= 124 && value <= 133)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576,7576,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 134 && value <= 138)
				{
					// Dead redberry bush[Clear,Inspect,Guide] 7708,7709,7710,7711,7712
					return new PatchState(Produce.REDBERRIES, CropState.DEAD, value - 133, Action.CLEAR);
				}
				if (value >= 139 && value <= 143)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 144 && value <= 149)
				{
					// Dead cadavaberry bush[Clear,Inspect,Guide] 7599,7600,7601,7602,7603,7604
					return new PatchState(Produce.CADAVABERRIES, CropState.DEAD, value - 143, Action.CLEAR);
				}
				if (value >= 150 && value <= 154)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 155 && value <= 161)
				{
					// Dead dwellberry bush[Clear,Inspect,Guide] 7625,7626,7627,7628,7629,7630,7631
					return new PatchState(Produce.DWELLBERRIES, CropState.DEAD, value - 154, Action.CLEAR);
				}
				if (value >= 162 && value <= 166)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 167 && value <= 174)
				{
					// Dead jangerberry bush[Clear,Inspect,Guide] 7654,7655,7656,7657,7658,7659,...
					return new PatchState(Produce.JANGERBERRIES, CropState.DEAD, value - 166, Action.CLEAR);
				}
				if (value >= 175 && value <= 179)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 180 && value <= 187)
				{
					// Dead whiteberry bush[Clear,Inspect,Guide] 7735,7736,7737,7738,7739,7740,...
					return new PatchState(Produce.WHITEBERRIES, CropState.DEAD, value - 179, Action.CLEAR);
				}
				if (value >= 188 && value <= 196)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576,7576,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 197 && value <= 204)
				{
					// Poison Ivy bush[Inspect,Guide] 7662,7663,7664,7665,7666,7667,...
					return new PatchState(Produce.POISON_IVY, CropState.GROWING, value - 197, Action.WAIT);
				}
				if (value >= 205 && value <= 209)
				{
					// Poison Ivy bush[Clear,Inspect,Guide,Pick-from] 7670,7674,7674,7674,7674
					return new PatchState(Produce.POISON_IVY, CropState.HARVESTABLE, value - 205, Action.HARVEST);
				}
				if (value >= 210 && value <= 216)
				{
					// Diseased Poison Ivy bush[Prune,Inspect,Guide] 7676,7677,7678,7679,7680,7681,7682
					return new PatchState(Produce.POISON_IVY, CropState.DISEASED, value - 209, Action.PRUNE);
				}
				if (value >= 217 && value <= 224)
				{
					// Dead Poison Ivy bush[Clear,Inspect,Guide] 7684,7685,7686,7687,7688,7689,...
					return new PatchState(Produce.POISON_IVY, CropState.DEAD, value - 216, Action.CLEAR);
				}
				if (value == 225)
				{
					// Diseased Poison Ivy bush[Prune,Inspect,Guide] 7683
					return new PatchState(Produce.POISON_IVY, CropState.DISEASED, 8, Action.PRUNE);
				}
				if (value >= 226 && value <= 249)
				{
					// Bush Patch[Rake,Inspect,Guide] 7576,7576,7576,7576,7576,7576,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value == 250)
				{
					// Redberry bush[Check-health,Inspect,Guide] 7702
					return new PatchState(Produce.REDBERRIES, CropState.GROWING, Produce.REDBERRIES.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 251)
				{
					// Cadavaberry bush[Check-health,Inspect,Guide] 7592
					return new PatchState(Produce.CADAVABERRIES, CropState.GROWING, Produce.CADAVABERRIES.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 252)
				{
					// Dwellberry bush[Check-health,Inspect,Guide] 7617
					return new PatchState(Produce.DWELLBERRIES, CropState.GROWING, Produce.DWELLBERRIES.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 253)
				{
					// Jangerberry bush[Check-health,Inspect,Guide] 7645
					return new PatchState(Produce.JANGERBERRIES, CropState.GROWING, Produce.JANGERBERRIES.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 254)
				{
					// Whiteberry bush[Check-health,Inspect,Guide] 7726
					return new PatchState(Produce.WHITEBERRIES, CropState.GROWING, Produce.WHITEBERRIES.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 255)
				{
					// Poison Ivy bush[Check-health,Inspect,Guide] 7675
					return new PatchState(Produce.POISON_IVY, CropState.GROWING, Produce.POISON_IVY.getStages() - 1, Action.CHECK_HEALTH);
				}
				return null;
			}
		},
	HARDWOOD_TREE("Hardwood Trees", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Tree patch[Rake,Inspect,Guide] 30479,30478,30477,30476
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Tree patch[Rake,Inspect,Guide] 30479,30479,30479,30479
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 8 && value <= 14)
				{
					// Teak Tree[Inspect,Guide] 30437,30438,30439,30440,30441,30442,30443
					return new PatchState(Produce.TEAK, CropState.GROWING, value - 8, Action.WAIT);
				}
				if (value == 15)
				{
					// Teak Tree[Check-health,Inspect,Guide] 30444
					return new PatchState(Produce.TEAK, CropState.GROWING, Produce.TEAK.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 16)
				{
					// Teak Tree[Chop down,Inspect,Guide] 30445
					return new PatchState(Produce.TEAK, CropState.HARVESTABLE, 0, Action.CHOP);
				}
				if (value == 17)
				{
					// Tree stump[Clear,Inspect,Guide] 30446
					return new PatchState(Produce.TEAK, CropState.HARVESTABLE, 0, Action.CLEAR);
				}
				if (value >= 18 && value <= 23)
				{
					// Diseased Teak[Prune,Inspect,Guide] 30447,30448,30449,30450,30451,30452
					return new PatchState(Produce.TEAK, CropState.DISEASED, value - 17, Action.PRUNE);
				}
				if (value >= 24 && value <= 29)
				{
					// Dead Teak[Clear,Inspect,Guide] 30453,30454,30455,30456,30457,30458
					return new PatchState(Produce.TEAK, CropState.DEAD, value - 23, Action.CLEAR);
				}
				if (value >= 30 && value <= 37)
				{
					// Mahogany sapling,Mahogany tree[Inspect,Guide] 30406,30407,30408,30409,30410,30411,...
					return new PatchState(Produce.MAHOGANY, CropState.GROWING, value - 30, Action.WAIT);
				}
				if (value == 38)
				{
					// Mahogany tree[Check-health,Inspect,Guide] 30416
					return new PatchState(Produce.MAHOGANY, CropState.GROWING, Produce.MAHOGANY.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 39)
				{
					// Mahogany tree[Chop down,Inspect,Guide] 30417
					return new PatchState(Produce.MAHOGANY, CropState.HARVESTABLE, 0, Action.CHOP);
				}
				if (value == 40)
				{
					// Mahogany tree stump[Clear,Inspect,Guide] 30418
					return new PatchState(Produce.MAHOGANY, CropState.HARVESTABLE, 0, Action.CLEAR);
				}
				if (value >= 41 && value <= 47)
				{
					// Diseased Mahogany[Prune,Inspect,Guide] 30419,30420,30421,30422,30423,30424,30425
					return new PatchState(Produce.MAHOGANY, CropState.DISEASED, value - 40, Action.PRUNE);
				}
				if (value >= 48 && value <= 54)
				{
					// Dead Mahogany[Clear,Inspect,Guide] 30428,30429,30430,30431,30432,30433,30434
					return new PatchState(Produce.MAHOGANY, CropState.DEAD, value - 47, Action.CLEAR);
				}
				if (value >= 55 && value <= 62)
				{
					// Camphor sapling,Camphor tree[Inspect,Guide] 58712,58713,58714,58715,58716,58717,...
					return new PatchState(Produce.CAMPHOR, CropState.GROWING, value - 55, Action.WAIT);
				}
				if (value == 63)
				{
					// Camphor tree[Check-health,Inspect,Guide] 58722
					return new PatchState(Produce.CAMPHOR, CropState.GROWING, Produce.CAMPHOR.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 64)
				{
					// Camphor tree[Chop down,Inspect,Guide] 58723
					return new PatchState(Produce.CAMPHOR, CropState.HARVESTABLE, 0, Action.CHOP);
				}
				if (value == 65)
				{
					// Camphor tree stump[Clear,Inspect,Guide] 58724
					return new PatchState(Produce.CAMPHOR, CropState.HARVESTABLE, 0, Action.CLEAR);
				}
				if (value >= 66 && value <= 72)
				{
					// Diseased Camphor[Prune,Inspect,Guide] 58725,58726,58727,58728,58729,58730,58731
					return new PatchState(Produce.CAMPHOR, CropState.DISEASED, value - 65, Action.PRUNE);
				}
				if (value >= 73 && value <= 79)
				{
					// Dead Camphor[Clear,Inspect,Guide] 58734,58735,58736,58737,58738,58739,58740
					return new PatchState(Produce.CAMPHOR, CropState.DEAD, value - 72, Action.CLEAR);
				}
				if (value >= 80 && value <= 87)
				{
					// Ironwood sapling,Ironwood tree[Inspect,Guide] 58743,58744,58745,58746,58747,58748,...
					return new PatchState(Produce.IRONWOOD, CropState.GROWING, value - 80, Action.WAIT);
				}
				if (value == 88)
				{
					// Ironwood tree[Check-health,Inspect,Guide] 58753
					return new PatchState(Produce.IRONWOOD, CropState.GROWING, Produce.IRONWOOD.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 89)
				{
					// Ironwood tree[Chop down,Inspect,Guide] 58754
					return new PatchState(Produce.IRONWOOD, CropState.HARVESTABLE, 0, Action.CHOP);
				}
				if (value == 90)
				{
					// Ironwood tree stump[Clear,Inspect,Guide] 58755
					return new PatchState(Produce.IRONWOOD, CropState.HARVESTABLE, 0, Action.CLEAR);
				}
				if (value >= 91 && value <= 97)
				{
					// Diseased Ironwood[Prune,Inspect,Guide] 58756,58757,58758,58759,58760,58761,58762
					return new PatchState(Produce.IRONWOOD, CropState.DISEASED, value - 90, Action.PRUNE);
				}
				if (value >= 98 && value <= 104)
				{
					// Dead Ironwood[Clear,Inspect,Guide] 58765,58766,58767,58768,58769,58770,58771
					return new PatchState(Produce.IRONWOOD, CropState.DEAD, value - 97, Action.CLEAR);
				}
				if (value >= 105 && value <= 113)
				{
					// Rosewood sapling,Rosewood tree[Inspect,Guide] 58774,58775,58776,58777,58778,58779,...
					return new PatchState(Produce.ROSEWOOD, CropState.GROWING, value - 105, Action.WAIT);
				}
				if (value == 114)
				{
					// Rosewood tree[Check-health,Inspect,Guide] 58784
					return new PatchState(Produce.ROSEWOOD, CropState.GROWING, Produce.ROSEWOOD.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 115)
				{
					// Rosewood tree[Chop down,Inspect,Guide] 58785
					return new PatchState(Produce.ROSEWOOD, CropState.HARVESTABLE, 0, Action.CHOP);
				}
				if (value == 116)
				{
					// Rosewood tree stump[Clear,Inspect,Guide] 58786
					return new PatchState(Produce.ROSEWOOD, CropState.HARVESTABLE, 0, Action.CLEAR);
				}
				if (value >= 117 && value <= 124)
				{
					// Diseased Rosewood[Prune,Inspect,Guide] 58787,58788,58789,58790,58791,58792,...
					return new PatchState(Produce.ROSEWOOD, CropState.DISEASED, value - 116, Action.PRUNE);
				}
				if (value >= 125 && value <= 132)
				{
					// Dead Rosewood[Clear,Inspect,Guide] 58796,58797,58798,58799,58800,58801,...
					return new PatchState(Produce.ROSEWOOD, CropState.DEAD, value - 124, Action.CLEAR);
				}
				if (value >= 133 && value <= 255)
				{
					// Tree patch[Rake,Inspect,Guide] 30479,30479,30479,30479,30479,30479,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	REDWOOD("Redwood Trees", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Redwood tree patch[Rake,Inspect,Guide] 34050,34049,34048,34047
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Redwood tree patch[Rake,Inspect,Guide] 34050,34050,34050,34050
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 8 && value <= 17)
				{
					// Redwood tree[Inspect,Guide] 34205,34206,34207,34208,34209,34210,...
					return new PatchState(Produce.REDWOOD, CropState.GROWING, value - 8, Action.WAIT);
				}
				if (value == 18)
				{
					// Redwood tree[Clear,Inspect,Guide] 34278
					return new PatchState(Produce.REDWOOD, CropState.HARVESTABLE, 0, Action.CLEAR);
				}
				if (value >= 19 && value <= 27)
				{
					// Diseased Redwood tree[Prune,Inspect,Guide] 34130,34131,34132,34133,34134,34139,...
					return new PatchState(Produce.REDWOOD, CropState.DISEASED, value - 18, Action.PRUNE);
				}
				if (value >= 28 && value <= 36)
				{
					// Dead Redwood tree[Clear,Inspect,Guide] 34061,34062,34063,34064,34065,34070,...
					return new PatchState(Produce.REDWOOD, CropState.DEAD, value - 27, Action.CLEAR);
				}
				if (value == 37)
				{
					// Redwood tree[Check-health,Inspect,Guide] 34297
					return new PatchState(Produce.REDWOOD, CropState.GROWING, Produce.REDWOOD.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value >= 41 && value <= 55)
				{
					// Redwood tree[Clear,Inspect,Guide] 34278,34278,34278,34278,34278,34278,...
					return new PatchState(Produce.REDWOOD, CropState.HARVESTABLE, 0, Action.CLEAR);
				}
				return null;
			}
		},
	SPIRIT_TREE("Spirit Trees", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Spirit Tree Patch[Rake,Inspect,Guide] 8342,8341,8340,8339
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Spirit Tree Patch[Rake,Inspect,Guide] 8342,8342,8342,8342
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 8 && value <= 19)
				{
					// Spirit Tree[Inspect,Guide] 8343,8344,8345,8346,8347,8348,...
					return new PatchState(Produce.SPIRIT_TREE, CropState.GROWING, value - 8, Action.WAIT);
				}
				if (value == 20)
				{
					// Spirit Tree[Travel,Talk-to,Inspect,Guide,Clear] 8355
					return new PatchState(Produce.SPIRIT_TREE, CropState.GROWING, 12, Action.GROWN);
				}
				if (value >= 21 && value <= 31)
				{
					// Diseased Spirit Tree[Prune,Inspect,Guide] 8358,8359,8360,8361,8362,8363,...
					return new PatchState(Produce.SPIRIT_TREE, CropState.DISEASED, value - 20, Action.PRUNE);
				}
				if (value >= 32 && value <= 43)
				{
					// Dead Spirit Tree[Clear,Inspect,Guide] 8370,8371,8372,8373,8374,8375,...
					return new PatchState(Produce.SPIRIT_TREE, CropState.DEAD, value - 31, Action.CLEAR);
				}
				if (value == 44)
				{
					// Spirit Tree[Check-health,Inspect,Guide] 8356
					return new PatchState(Produce.SPIRIT_TREE, CropState.GROWING, Produce.SPIRIT_TREE.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value >= 45 && value <= 63)
				{
					// Spirit Tree Patch[Rake,Inspect,Guide] 8342,8342,8342,8342,8342,8342,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	ANIMA("", false)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Anima patch[Rake,Inspect,Guide] 33983,33982,33981,33980
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Anima patch[Rake,Inspect,Guide] 33983,33983,33983,33983
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 8 && value <= 16)
				{
					// Attas plant[Inspect,Guide] 33991,33992,33993,33994,33995
					// Attas plant[Inspect,Guide] 33995,33995
					// Withering Attas plant[Inspect,Guide] 33996
					// Dead Attas plant[Clear,Inspect,Guide] 33997
					return new PatchState(Produce.ATTAS, value - 8 == 8 ? CropState.DEAD : CropState.GROWING, value - 8, value - 8 == 8 ? Action.CLEAR : Action.WAIT);
				}
				if (value >= 17 && value <= 25)
				{
					// Iasor plant[Inspect,Guide] 33984,33985,33986,33987,33988
					// Iasor plant[Inspect,Guide] 33988,33988
					// Withering Iasor plant[Inspect,Guide] 33989
					// Dead Iasor plant[Clear,Inspect,Guide] 33990
					return new PatchState(Produce.IASOR, value - 17 == 8 ? CropState.DEAD : CropState.GROWING, value - 17, value - 17 == 8 ? Action.CLEAR : Action.WAIT);
				}
				if (value >= 26 && value <= 34)
				{
					// Kronos plant[Inspect,Guide] 33999,34000,34001,34002,34003
					// Kronos plant[Inspect,Guide] 34003,34003
					// Withering Kronos plant[Inspect,Guide] 34004
					// Dead Kronos plant[Clear,Inspect,Guide] 34005
					return new PatchState(Produce.KRONOS, value - 26 == 8 ? CropState.DEAD : CropState.GROWING, value - 26, value - 26 == 8 ? Action.CLEAR : Action.WAIT);
				}
				if (value >= 35 && value <= 255)
				{
					// Anima patch[Rake,Inspect,Guide] 33983,33983,33983,33983,33983,33983,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	BELLADONNA("Belladonna", false)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Belladonna patch[Rake,Inspect,Guide] 7560,7559,7558,7557
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Belladonna[Inspect,Guide] 7561,7562,7563,7564
					return new PatchState(Produce.BELLADONNA, CropState.GROWING, value - 4, Action.WAIT);
				}
				if (value == 8)
				{
					// Belladonna[Pick,Inspect,Guide] 7565
					return new PatchState(Produce.BELLADONNA, CropState.HARVESTABLE, 0, Action.HARVEST);
				}
				if (value >= 9 && value <= 11)
				{
					// Diseased Belladonna[Cure,Inspect,Guide] 7566,7567,7568
					return new PatchState(Produce.BELLADONNA, CropState.DISEASED, value - 8, Action.CURE);
				}
				if (value >= 12 && value <= 14)
				{
					// Dead Belladonna[Clear,Inspect,Guide] 7569,7570,7571
					return new PatchState(Produce.BELLADONNA, CropState.DEAD, value - 11, Action.CLEAR);
				}
				if (value >= 15 && value <= 255)
				{
					// Belladonna patch[Rake,Inspect,Guide] 7560,7560,7560,7560,7560,7560,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	CACTUS("Cactus", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Cactus patch[Rake,Inspect,Guide] 7746,7745,7744,7743
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Cactus patch[Rake,Inspect,Guide] 7746,7746,7746,7746
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 8 && value <= 14)
				{
					// Cactus[Inspect,Guide] 7747,7748,7749,7750,7751,7752,7753
					return new PatchState(Produce.CACTUS, CropState.GROWING, value - 8, Action.WAIT);
				}
				if (value >= 15 && value <= 18)
				{
					// Cactus[Clear,Inspect,Guide,Pick-spine] 7754,7757,7757,7757
					return new PatchState(Produce.CACTUS, CropState.HARVESTABLE, value - 15, Action.HARVEST);
				}
				if (value >= 19 && value <= 24)
				{
					// Diseased cactus[Cure,Inspect,Guide] 7759,7760,7761,7762,7763,7764
					return new PatchState(Produce.CACTUS, CropState.DISEASED, value - 18, Action.CURE);
				}
				if (value >= 25 && value <= 30)
				{
					// Dead cactus[Clear,Inspect,Guide] 7765,7766,7767,7768,7769,7770
					return new PatchState(Produce.CACTUS, CropState.DEAD, value - 24, Action.CLEAR);
				}
				if (value == 31)
				{
					// Cactus[Check-health,Inspect,Guide] 7758
					return new PatchState(Produce.CACTUS, CropState.GROWING, Produce.CACTUS.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value >= 32 && value <= 38)
				{
					// Potato cactus[Inspect,Guide] 33734,33735,33736,33737,33738,33739,33740
					return new PatchState(Produce.POTATO_CACTUS, CropState.GROWING, value - 32, Action.WAIT);
				}
				if (value >= 39 && value <= 45)
				{
					// Potato cactus[Clear,Inspect,Guide,Pick] 33741,33742,33743,33744,33745,33746,33747
					return new PatchState(Produce.POTATO_CACTUS, CropState.HARVESTABLE, value - 39, Action.HARVEST);
				}
				if (value >= 46 && value <= 51)
				{
					// Diseased Potato cactus[Cure,Inspect,Guide] 33749,33750,33751,33752,33753,33754
					return new PatchState(Produce.POTATO_CACTUS, CropState.DISEASED, value - 45, Action.CURE);
				}
				if (value >= 52 && value <= 57)
				{
					// Dead Potato cactus[Clear,Inspect,Guide] 33755,33756,33757,33758,33759,33760
					return new PatchState(Produce.POTATO_CACTUS, CropState.DEAD, value - 51, Action.CLEAR);
				}
				if (value == 58)
				{
					// Potato cactus[Check-health,Inspect,Guide] 33748
					return new PatchState(Produce.POTATO_CACTUS, CropState.GROWING, Produce.POTATO_CACTUS.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value >= 59 && value <= 255)
				{
					// Cactus patch[Rake,Inspect,Guide] 7746,7746,7746,7746,7746,7746,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	SEAWEED("Seaweed", false)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Seaweed patch[Rake,Inspect,Guide] 30486,30485,30484,30483
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Seaweed[Inspect,Guide] 30487,30488,30489,30490
					return new PatchState(Produce.SEAWEED, CropState.GROWING, value - 4, Action.WAIT);
				}
				if (value >= 8 && value <= 10)
				{
					// Seaweed[Pick,Inspect,Guide] 30491,30492,30493
					return new PatchState(Produce.SEAWEED, CropState.HARVESTABLE, value - 8, Action.HARVEST);
				}
				if (value >= 11 && value <= 13)
				{
					// Diseased seaweed[Cure,Inspect,Guide] 30494,30495,30496
					return new PatchState(Produce.SEAWEED, CropState.DISEASED, value - 10, Action.CURE);
				}
				if (value >= 14 && value <= 16)
				{
					// Dead seaweed[Clear,Inspect,Guide] 30497,30498,30499
					return new PatchState(Produce.SEAWEED, CropState.DEAD, value - 13, Action.CLEAR);
				}
				if (value >= 17 && value <= 255)
				{
					// Seaweed patch[Rake,Inspect,Guide] 30486,30486,30486,30486,30486,30486,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	CALQUAT("Calquat", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Calquat patch[Rake,Inspect,Guide] 7775,7774,7773,7772
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 11)
				{
					// Calquat Tree[Inspect,Guide] 7776,7777,7778,7779,7780,7781,...
					return new PatchState(Produce.CALQUAT, CropState.GROWING, value - 4, Action.WAIT);
				}
				if (value >= 12 && value <= 18)
				{
					// Calquat Tree[Clear,Inspect,Guide,Pick-fruit] 7784,7785,7786,7787,7788,7789,7790
					return new PatchState(Produce.CALQUAT, CropState.HARVESTABLE, value - 12, Action.HARVEST);
				}
				if (value >= 19 && value <= 25)
				{
					// Diseased Calquat[Prune,Inspect,Guide] 7792,7793,7794,7795,7796,7797,7798
					return new PatchState(Produce.CALQUAT, CropState.DISEASED, value - 18, Action.PRUNE);
				}
				if (value >= 26 && value <= 33)
				{
					// Dead Calquat[Clear,Inspect,Guide] 7799,7800,7801,7802,7803,7804,...
					return new PatchState(Produce.CALQUAT, CropState.DEAD, value - 25, Action.CLEAR);
				}
				if (value == 34)
				{
					// Calquat Tree[Check-health,Inspect,Guide] 7791
					return new PatchState(Produce.CALQUAT, CropState.GROWING, Produce.CALQUAT.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value >= 35 && value <= 255)
				{
					// Calquat patch[Rake,Inspect,Guide] 7775,7775,7775,7775,7775,7775,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	CELASTRUS("Celastrus", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Celastrus patch[Rake,Inspect,Guide] 33698,33697,33696,33695
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Celastrus patch[Rake,Inspect,Guide] 33698,33698,33698,33698
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 8 && value <= 12)
				{
					// Celastrus tree[Inspect,Guide] 33699,33700,33701,33702,33703
					return new PatchState(Produce.CELASTRUS, CropState.GROWING, value - 8, Action.WAIT);
				}
				if (value == 13)
				{
					// Celastrus tree[Check-health,Inspect,Guide] 33704
					return new PatchState(Produce.CELASTRUS, CropState.GROWING, Produce.CELASTRUS.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value >= 14 && value <= 16)
				{
					// Celastrus tree[Harvest,Inspect,Guide] 33719,33718,33717
					return new PatchState(Produce.CELASTRUS, CropState.HARVESTABLE, value - 14, Action.HARVEST);
				}
				if (value == 17)
				{
					// Harvested Celastrus tree[Chop,Inspect,Guide] 33720
					return new PatchState(Produce.CELASTRUS, CropState.HARVESTABLE, 0, Action.CHOP);
				}
				if (value >= 18 && value <= 22)
				{
					// Diseased celastrus tree[Prune,Inspect,Guide] 33705,33706,33707,33708,33709
					return new PatchState(Produce.CELASTRUS, CropState.DISEASED, value - 17, Action.PRUNE);
				}
				if (value >= 23 && value <= 27)
				{
					// Dead celastrus tree[Clear,Inspect,Guide] 33711,33712,33713,33714,33715
					return new PatchState(Produce.CELASTRUS, CropState.DEAD, value - 22, Action.CLEAR);
				}
				if (value == 28)
				{
					// Celastrus tree stump[Clear,Inspect,Guide] 33721
					return new PatchState(Produce.CELASTRUS, CropState.HARVESTABLE, 0, Action.CLEAR);
				}
				if (value >= 29 && value <= 255)
				{
					// Celastrus patch[Rake,Inspect,Guide] 33698,33698,33698,33698,33698,33698,...
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	CRYSTAL_TREE("Crystal Tree", true)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Crystal tree patch[Rake,Inspect,Guide] 34910,34909,34908,34907
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 8 && value <= 13)
				{
					// Crystal tree[Inspect,Guide] 34911,34912,34913,34914,34915,34916
					return new PatchState(Produce.CRYSTAL_TREE, CropState.GROWING, value - 8, Action.WAIT);
				}
				if (value == 14)
				{
					// Crystal tree[Check-health,Inspect,Guide] 34917
					return new PatchState(Produce.CRYSTAL_TREE, CropState.GROWING, Produce.CRYSTAL_TREE.getStages() - 1, Action.CHECK_HEALTH);
				}
				if (value == 15)
				{
					// Crystal tree[Chop-down,Inspect,Guide] 34918
					return new PatchState(Produce.CRYSTAL_TREE, CropState.HARVESTABLE, 0, Action.CHOP);
				}
				return null;
			}
		};

	/** The patch's state for a varbit value, or null for a value the game never sends. */
	public abstract PatchState forVarbitValue(int value);

	private final String name;

	private final boolean healthCheckRequired;

	PatchKind(String name, boolean healthCheckRequired)
	{
		this.name = name;
		this.healthCheckRequired = healthCheckRequired;
	}

	public String getName()
	{
		return name;
	}

	public boolean isHealthCheckRequired()
	{
		return healthCheckRequired;
	}
}
