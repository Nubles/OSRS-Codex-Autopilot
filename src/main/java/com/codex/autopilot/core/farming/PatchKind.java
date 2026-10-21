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
 * only the patches Autopilot watches and noting, from each object's menu, what the patch wants doing.
 * The herb, allotment, flower and hops tables are RuneLite's as they stand, with the action taken from
 * the crop state: weeds want raking, a crop wants waiting on, picking, curing or clearing.
 */
public enum PatchKind
{
	ALLOTMENT("", false)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8575,8574,8573
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 5)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 6 && value <= 9)
				{
					// Potato seed,Potato plant[Inspect,Guide] 8558,8559,8560,8561
					return new PatchState(Produce.POTATO, CropState.GROWING, value - 6, Action.WAIT);
				}
				if (value >= 10 && value <= 12)
				{
					// Potato[Harvest,Inspect,Guide] 8562,8562,8562
					return new PatchState(Produce.POTATO, CropState.HARVESTABLE, value - 10, Action.HARVEST);
				}
				if (value >= 13 && value <= 16)
				{
					// Onion seeds,Onion plant[Inspect,Guide] 8580,8581,8582,8583
					return new PatchState(Produce.ONION, CropState.GROWING, value - 13, Action.WAIT);
				}
				if (value >= 17 && value <= 19)
				{
					// Onion[Harvest,Inspect,Guide] 8584,8584,8584
					return new PatchState(Produce.ONION, CropState.HARVESTABLE, value - 17, Action.HARVEST);
				}
				if (value >= 20 && value <= 23)
				{
					// Cabbages[Inspect,Guide] 8535,8536,8537,8538
					return new PatchState(Produce.CABBAGE, CropState.GROWING, value - 20, Action.WAIT);
				}
				if (value >= 24 && value <= 26)
				{
					// Cabbages[Harvest,Inspect,Guide] 8539,8539,8539
					return new PatchState(Produce.CABBAGE, CropState.HARVESTABLE, value - 24, Action.HARVEST);
				}
				if (value >= 27 && value <= 30)
				{
					// Tomato plant[Inspect,Guide] 8641,8642,8643,8644
					return new PatchState(Produce.TOMATO, CropState.GROWING, value - 27, Action.WAIT);
				}
				if (value >= 31 && value <= 33)
				{
					// Tomato[Harvest,Inspect,Guide] 8645,8645,8645
					return new PatchState(Produce.TOMATO, CropState.HARVESTABLE, value - 31, Action.HARVEST);
				}
				if (value >= 34 && value <= 39)
				{
					// Sweetcorn seed,Sweetcorn plant[Inspect,Guide] 8618,8619,8620,8621,8622,8623
					return new PatchState(Produce.SWEETCORN, CropState.GROWING, value - 34, Action.WAIT);
				}
				if (value >= 40 && value <= 42)
				{
					// Sweetcorn[Harvest,Inspect,Guide] 8624,8624,8624
					return new PatchState(Produce.SWEETCORN, CropState.HARVESTABLE, value - 40, Action.HARVEST);
				}
				if (value >= 43 && value <= 48)
				{
					// Strawberry seed,Strawberry plant[Inspect,Guide] 8595,8596,8597,8598,8599,8600
					return new PatchState(Produce.STRAWBERRY, CropState.GROWING, value - 43, Action.WAIT);
				}
				if (value >= 49 && value <= 51)
				{
					// Strawberry[Harvest,Inspect,Guide] 8601,8601,8601
					return new PatchState(Produce.STRAWBERRY, CropState.HARVESTABLE, value - 49, Action.HARVEST);
				}
				if (value >= 52 && value <= 59)
				{
					// Watermelon seed,Watermelons[Inspect,Guide] 8656,8657,8658,8659,8660,8661,8662,8663
					return new PatchState(Produce.WATERMELON, CropState.GROWING, value - 52, Action.WAIT);
				}
				if (value >= 60 && value <= 62)
				{
					// Watermelon[Harvest,Inspect,Guide] 8664,8664,8664
					return new PatchState(Produce.WATERMELON, CropState.HARVESTABLE, value - 60, Action.HARVEST);
				}
				if (value >= 63 && value <= 69)
				{
					// Snape grass seedling,Snape grass plant[Inspect,Guide] 33674,33675,33676,33677,33678,33679,33680
					return new PatchState(Produce.SNAPE_GRASS, CropState.GROWING, value - 63, Action.WAIT);
				}
				if (value >= 70 && value <= 73)
				{
					// Potato seed,Potato plant[Inspect,Guide] 8563,8564,8565,8566
					return new PatchState(Produce.POTATO, CropState.GROWING, value - 70, Action.WAIT);
				}
				if (value >= 74 && value <= 76)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 77 && value <= 80)
				{
					// Onion seeds,Onion plant[Inspect,Guide] 8585,8586,8587,8588
					return new PatchState(Produce.ONION, CropState.GROWING, value - 77, Action.WAIT);
				}
				if (value >= 81 && value <= 83)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 84 && value <= 87)
				{
					// Cabbages[Inspect,Guide] 8540,8541,8542,8543
					return new PatchState(Produce.CABBAGE, CropState.GROWING, value - 84, Action.WAIT);
				}
				if (value >= 88 && value <= 90)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 91 && value <= 94)
				{
					// Tomato plant[Inspect,Guide] 8646,8647,8648,8649
					return new PatchState(Produce.TOMATO, CropState.GROWING, value - 91, Action.WAIT);
				}
				if (value >= 95 && value <= 97)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 98 && value <= 103)
				{
					// Sweetcorn seed,Sweetcorn plant[Inspect,Guide] 8625,8626,8627,8628,8629,8630
					return new PatchState(Produce.SWEETCORN, CropState.GROWING, value - 98, Action.WAIT);
				}
				if (value >= 104 && value <= 106)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 107 && value <= 112)
				{
					// Strawberry seed,Strawberry plant[Inspect,Guide] 8602,8603,8604,8605,8606,8607
					return new PatchState(Produce.STRAWBERRY, CropState.GROWING, value - 107, Action.WAIT);
				}
				if (value >= 113 && value <= 115)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 116 && value <= 123)
				{
					// Watermelon seed,Watermelons[Inspect,Guide] 8665,8666,8667,8668,8669,8670,8671,8672
					return new PatchState(Produce.WATERMELON, CropState.GROWING, value - 116, Action.WAIT);
				}
				if (value >= 124 && value <= 127)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 128 && value <= 134)
				{
					// Snape grass seedling,Snape grass plant[Inspect,Guide] 33666,33667,33668,33669,33670,33671,33672
					return new PatchState(Produce.SNAPE_GRASS, CropState.GROWING, value - 128, Action.WAIT);
				}
				if (value >= 135 && value <= 137)
				{
					// Diseased potatoes[Cure,Inspect,Guide] 8567,8568,8569
					return new PatchState(Produce.POTATO, CropState.DISEASED, value - 134, Action.CURE);
				}
				if (value >= 138 && value <= 140)
				{
					// Snape grass plant[Harvest,Inspect,Guide] 33673,33673,33673
					return new PatchState(Produce.SNAPE_GRASS, CropState.HARVESTABLE, value - 138, Action.HARVEST);
				}
				if (value == 141)
				{
					// Allotment[Rake,Inspect,Guide] 8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 142 && value <= 144)
				{
					// Diseased onions[Cure,Inspect,Guide] 8589,8590,8591
					return new PatchState(Produce.ONION, CropState.DISEASED, value - 141, Action.CURE);
				}
				if (value >= 145 && value <= 148)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 149 && value <= 151)
				{
					// Diseased cabbages[Cure,Inspect,Guide] 8544,8545,8546
					return new PatchState(Produce.CABBAGE, CropState.DISEASED, value - 148, Action.CURE);
				}
				if (value >= 152 && value <= 155)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 156 && value <= 158)
				{
					// Diseased tomato plant[Cure,Inspect,Guide] 8650,8651,8652
					return new PatchState(Produce.TOMATO, CropState.DISEASED, value - 155, Action.CURE);
				}
				if (value >= 159 && value <= 162)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 163 && value <= 167)
				{
					// Diseased sweetcorn plant[Cure,Inspect,Guide] 8631,8632,8633,8634,8635
					return new PatchState(Produce.SWEETCORN, CropState.DISEASED, value - 162, Action.CURE);
				}
				if (value >= 168 && value <= 171)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 172 && value <= 176)
				{
					// Diseased strawberry plant[Cure,Inspect,Guide] 8608,8609,8610,8611,8612
					return new PatchState(Produce.STRAWBERRY, CropState.DISEASED, value - 171, Action.CURE);
				}
				if (value >= 177 && value <= 180)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 181 && value <= 187)
				{
					// Diseased watermelons[Cure,Inspect,Guide] 8673,8674,8675,8676,8677,8678,8679
					return new PatchState(Produce.WATERMELON, CropState.DISEASED, value - 180, Action.CURE);
				}
				if (value >= 188 && value <= 192)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 193 && value <= 195)
				{
					// Dead Snape grass[Clear,Inspect,Guide] 33687,33688,33689
					return new PatchState(Produce.SNAPE_GRASS, CropState.DEAD, value - 192, Action.CLEAR);
				}
				if (value >= 196 && value <= 198)
				{
					// Diseased Snape grass[Cure,Inspect,Guide] 33681,33682,33683
					return new PatchState(Produce.SNAPE_GRASS, CropState.DISEASED, value - 195, Action.CURE);
				}
				if (value >= 199 && value <= 201)
				{
					// Dead potatoes[Clear,Inspect,Guide] 8570,8571,8572
					return new PatchState(Produce.POTATO, CropState.DEAD, value - 198, Action.CLEAR);
				}
				if (value >= 202 && value <= 204)
				{
					// Diseased Snape grass[Cure,Inspect,Guide] 33684,33685,33686
					return new PatchState(Produce.SNAPE_GRASS, CropState.DISEASED, 3 + value - 201, Action.CURE);
				}
				if (value == 205)
				{
					// Allotment[Rake,Inspect,Guide] 8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 206 && value <= 208)
				{
					// Dead onions[Clear,Inspect,Guide] 8592,8593,8594
					return new PatchState(Produce.ONION, CropState.DEAD, value - 205, Action.CLEAR);
				}
				if (value >= 209 && value <= 211)
				{
					// Dead Snape grass[Clear,Inspect,Guide] 33690,33691,33692
					return new PatchState(Produce.SNAPE_GRASS, CropState.DEAD, 3 + value - 208, Action.CLEAR);
				}
				if (value == 212)
				{
					// Allotment[Rake,Inspect,Guide] 8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 213 && value <= 215)
				{
					// Dead cabbages[Clear,Inspect,Guide] 8547,8548,8549
					return new PatchState(Produce.CABBAGE, CropState.DEAD, value - 212, Action.CLEAR);
				}
				if (value >= 216 && value <= 219)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 220 && value <= 222)
				{
					// Dead tomato plant[Clear,Inspect,Guide] 8653,8654,8655
					return new PatchState(Produce.TOMATO, CropState.DEAD, value - 219, Action.CLEAR);
				}
				if (value >= 223 && value <= 226)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 227 && value <= 231)
				{
					// Dead sweetcorn plant[Clear,Inspect,Guide] 8636,8637,8638,8639,8640
					return new PatchState(Produce.SWEETCORN, CropState.DEAD, value - 226, Action.CLEAR);
				}
				if (value >= 232 && value <= 235)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 236 && value <= 240)
				{
					// Dead strawberry plant[Clear,Inspect,Guide] 8613,8614,8615,8616,8617
					return new PatchState(Produce.STRAWBERRY, CropState.DEAD, value - 235, Action.CLEAR);
				}
				if (value >= 241 && value <= 244)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 245 && value <= 251)
				{
					// Dead watermelons[Clear,Inspect,Guide] 8680,8681,8682,8683,8684,8685,8686
					return new PatchState(Produce.WATERMELON, CropState.DEAD, value - 244, Action.CLEAR);
				}
				if (value >= 252 && value <= 255)
				{
					// Allotment[Rake,Inspect,Guide] 8576,8576,8576,8576
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	HERB("", false)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Herb patch[Rake,Inspect,Guide] 8135,8134,8133,8132
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Herbs[Inspect,Guide] 14209,26825,26826,26827
					return new PatchState(Produce.GUAM, CropState.GROWING, value - 4, Action.WAIT);
				}
				if (value >= 8 && value <= 10)
				{
					// Herbs[Pick,Inspect,Guide] 26828,26828,26828
					return new PatchState(Produce.GUAM, CropState.HARVESTABLE, 10 - value, Action.HARVEST);
				}
				if (value >= 11 && value <= 14)
				{
					// Herbs[Inspect,Guide] 26829,39748,39749,39750
					return new PatchState(Produce.MARRENTILL, CropState.GROWING, value - 11, Action.WAIT);
				}
				if (value >= 15 && value <= 17)
				{
					// Herbs[Pick,Inspect,Guide] 39751,39751,39751
					return new PatchState(Produce.MARRENTILL, CropState.HARVESTABLE, 17 - value, Action.HARVEST);
				}
				if (value >= 18 && value <= 21)
				{
					// Herbs[Inspect,Guide] 39752,39753,39754,39755
					return new PatchState(Produce.TARROMIN, CropState.GROWING, value - 18, Action.WAIT);
				}
				if (value >= 22 && value <= 24)
				{
					// Herbs[Pick,Inspect,Guide] 39756,39756,39756
					return new PatchState(Produce.TARROMIN, CropState.HARVESTABLE, 24 - value, Action.HARVEST);
				}
				if (value >= 25 && value <= 28)
				{
					// Herbs[Inspect,Guide] 39757,39758,39759,39760
					return new PatchState(Produce.HARRALANDER, CropState.GROWING, value - 25, Action.WAIT);
				}
				if (value >= 29 && value <= 31)
				{
					// Herbs[Pick,Inspect,Guide] 39761,39761,39761
					return new PatchState(Produce.HARRALANDER, CropState.HARVESTABLE, 31 - value, Action.HARVEST);
				}
				if (value >= 32 && value <= 35)
				{
					// Herbs[Inspect,Guide] 39762,39763,39764,39765
					return new PatchState(Produce.RANARR, CropState.GROWING, value - 32, Action.WAIT);
				}
				if (value >= 36 && value <= 38)
				{
					// Herbs[Pick,Inspect,Guide] 39766,39766,39766
					return new PatchState(Produce.RANARR, CropState.HARVESTABLE, 38 - value, Action.HARVEST);
				}
				if (value >= 39 && value <= 42)
				{
					// Herbs[Inspect,Guide] 39777,39778,39779,39780
					return new PatchState(Produce.TOADFLAX, CropState.GROWING, value - 39, Action.WAIT);
				}
				if (value >= 43 && value <= 45)
				{
					// Herbs[Pick,Inspect,Guide] 39781,39781,39781
					return new PatchState(Produce.TOADFLAX, CropState.HARVESTABLE, 45 - value, Action.HARVEST);
				}
				if (value >= 46 && value <= 49)
				{
					// Herbs[Inspect,Guide] 39767,39768,39769,39770
					return new PatchState(Produce.IRIT, CropState.GROWING, value - 46, Action.WAIT);
				}
				if (value >= 50 && value <= 52)
				{
					// Herbs[Pick,Inspect,Guide] 39771,39771,39771
					return new PatchState(Produce.IRIT, CropState.HARVESTABLE, 52 - value, Action.HARVEST);
				}
				if (value >= 53 && value <= 56)
				{
					// Herbs[Inspect,Guide] 39772,39773,39774,39775
					return new PatchState(Produce.AVANTOE, CropState.GROWING, value - 53, Action.WAIT);
				}
				if (value >= 57 && value <= 59)
				{
					// Herbs[Pick,Inspect,Guide] 39776,39776,39776
					return new PatchState(Produce.AVANTOE, CropState.HARVESTABLE, 59 - value, Action.HARVEST);
				}
				if (value >= 60 && value <= 63)
				{
					// Herbs[Inspect,Guide] 55347,55348,55349,55350
					return new PatchState(Produce.HUASCA, CropState.GROWING, value - 60, Action.WAIT);
				}
				if (value >= 64 && value <= 66)
				{
					// Herbs[Pick,Inspect,Guide] 55351,55351,55351
					return new PatchState(Produce.HUASCA, CropState.HARVESTABLE, 66 - value, Action.HARVEST);
				}
				if (value == 67)
				{
					// Herb patch[Rake,Inspect,Guide] 8135
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 68 && value <= 71)
				{
					// Herbs[Inspect,Guide] 39782,39783,39784,39785
					return new PatchState(Produce.KWUARM, CropState.GROWING, value - 68, Action.WAIT);
				}
				if (value >= 72 && value <= 74)
				{
					// Herbs[Pick,Inspect,Guide] 39786,39786,39786
					return new PatchState(Produce.KWUARM, CropState.HARVESTABLE, 74 - value, Action.HARVEST);
				}
				if (value >= 75 && value <= 78)
				{
					// Herbs[Inspect,Guide] 39807,39808,39809,39810
					return new PatchState(Produce.SNAPDRAGON, CropState.GROWING, value - 75, Action.WAIT);
				}
				if (value >= 79 && value <= 81)
				{
					// Herbs[Pick,Inspect,Guide] 39811,39811,39811
					return new PatchState(Produce.SNAPDRAGON, CropState.HARVESTABLE, 81 - value, Action.HARVEST);
				}
				if (value >= 82 && value <= 85)
				{
					// Herbs[Inspect,Guide] 39787,39788,39789,39790
					return new PatchState(Produce.CADANTINE, CropState.GROWING, value - 82, Action.WAIT);
				}
				if (value >= 86 && value <= 88)
				{
					// Herbs[Pick,Inspect,Guide] 39791,39791,39791
					return new PatchState(Produce.CADANTINE, CropState.HARVESTABLE, 88 - value, Action.HARVEST);
				}
				if (value >= 89 && value <= 92)
				{
					// Herbs[Inspect,Guide] 39792,39793,39794,39795
					return new PatchState(Produce.LANTADYME, CropState.GROWING, value - 89, Action.WAIT);
				}
				if (value >= 93 && value <= 95)
				{
					// Herbs[Pick,Inspect,Guide] 39796,39796,39796
					return new PatchState(Produce.LANTADYME, CropState.HARVESTABLE, 95 - value, Action.HARVEST);
				}
				if (value >= 96 && value <= 99)
				{
					// Herbs[Inspect,Guide] 39797,39798,39799,39800
					return new PatchState(Produce.DWARF_WEED, CropState.GROWING, value - 96, Action.WAIT);
				}
				if (value >= 100 && value <= 102)
				{
					// Herbs[Pick,Inspect,Guide] 39801,39801,39801
					return new PatchState(Produce.DWARF_WEED, CropState.HARVESTABLE, 102 - value, Action.HARVEST);
				}
				if (value >= 103 && value <= 106)
				{
					// Herbs[Inspect,Guide] 39802,39803,39804,39805
					return new PatchState(Produce.TORSTOL, CropState.GROWING, value - 103, Action.WAIT);
				}
				if (value >= 107 && value <= 109)
				{
					// Herbs[Pick,Inspect,Guide] 39806,39806,39806
					return new PatchState(Produce.TORSTOL, CropState.HARVESTABLE, 109 - value, Action.HARVEST);
				}
				if (value >= 128 && value <= 130)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.GUAM, CropState.DISEASED, value - 127, Action.CURE);
				}
				if (value >= 131 && value <= 133)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.MARRENTILL, CropState.DISEASED, value - 130, Action.CURE);
				}
				if (value >= 134 && value <= 136)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.TARROMIN, CropState.DISEASED, value - 133, Action.CURE);
				}
				if (value >= 137 && value <= 139)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.HARRALANDER, CropState.DISEASED, value - 136, Action.CURE);
				}
				if (value >= 140 && value <= 142)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.RANARR, CropState.DISEASED, value - 139, Action.CURE);
				}
				if (value >= 143 && value <= 145)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.TOADFLAX, CropState.DISEASED, value - 142, Action.CURE);
				}
				if (value >= 146 && value <= 148)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.IRIT, CropState.DISEASED, value - 145, Action.CURE);
				}
				if (value >= 149 && value <= 151)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.AVANTOE, CropState.DISEASED, value - 148, Action.CURE);
				}
				if (value >= 152 && value <= 154)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.KWUARM, CropState.DISEASED, value - 151, Action.CURE);
				}
				if (value >= 155 && value <= 157)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.SNAPDRAGON, CropState.DISEASED, value - 154, Action.CURE);
				}
				if (value >= 158 && value <= 160)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.CADANTINE, CropState.DISEASED, value - 157, Action.CURE);
				}
				if (value >= 161 && value <= 163)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.LANTADYME, CropState.DISEASED, value - 160, Action.CURE);
				}
				if (value >= 164 && value <= 166)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.DWARF_WEED, CropState.DISEASED, value - 163, Action.CURE);
				}
				if (value >= 167 && value <= 169)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.TORSTOL, CropState.DISEASED, value - 166, Action.CURE);
				}
				if (value >= 170 && value <= 172)
				{
					// Dead herbs[Clear,Inspect,Guide] 8147,8148,8149
					return new PatchState(Produce.ANYHERB, CropState.DEAD, value - 169, Action.CLEAR);
				}
				if (value >= 173 && value <= 175)
				{
					// Diseased herbs[Cure,Inspect,Guide] 8144,8145,8146
					return new PatchState(Produce.HUASCA, CropState.DISEASED, value - 172, Action.CURE);
				}
				if (value >= 176 && value <= 191)
				{
					// Herb patch[Rake,Inspect,Guide] 8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 192 && value <= 195)
				{
					// Goutweed[Inspect,Guide] 9044,9045,9046,9047
					return new PatchState(Produce.GOUTWEED, CropState.GROWING, value - 192, Action.WAIT);
				}
				if (value >= 196 && value <= 197)
				{
					// Goutweed[Pick,Inspect,Guide] 9048,9048
					return new PatchState(Produce.GOUTWEED, CropState.HARVESTABLE, 197 - value, Action.HARVEST);
				}
				if (value >= 198 && value <= 200)
				{
					// Diseased goutweed[Cure,Inspect,Guide] 9049,9050,9051
					return new PatchState(Produce.GOUTWEED, CropState.DISEASED, value - 197, Action.CURE);
				}
				if (value >= 201 && value <= 203)
				{
					// Dead goutweed[Clear,Inspect,Guide] 9052,9053,9054
					return new PatchState(Produce.GOUTWEED, CropState.DEAD, value - 200, Action.CLEAR);
				}
				if (value >= 204 && value <= 219)
				{
					// Herb patch[Rake,Inspect,Guide] 8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 221 && value <= 255)
				{
					// Herb patch[Rake,Inspect,Guide] 8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135,8135
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	FLOWER("", false)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7842,7841,7840
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843,7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 8 && value <= 11)
				{
					// Marigold[Inspect,Guide] 7867,7868,7869,7870
					return new PatchState(Produce.MARIGOLD, CropState.GROWING, value - 8, Action.WAIT);
				}
				if (value == 12)
				{
					// Marigold[Pick,Inspect,Guide] 7871
					return new PatchState(Produce.MARIGOLD, CropState.HARVESTABLE, 0, Action.HARVEST);
				}
				if (value >= 13 && value <= 16)
				{
					// Rosemary[Inspect,Guide] 7899,7900,7901,7902
					return new PatchState(Produce.ROSEMARY, CropState.GROWING, value - 13, Action.WAIT);
				}
				if (value == 17)
				{
					// Rosemary[Pick,Inspect,Guide] 7903
					return new PatchState(Produce.ROSEMARY, CropState.HARVESTABLE, 0, Action.HARVEST);
				}
				if (value >= 18 && value <= 21)
				{
					// Nasturtium[Inspect,Guide] 7883,7884,7885,7886
					return new PatchState(Produce.NASTURTIUM, CropState.GROWING, value - 18, Action.WAIT);
				}
				if (value == 22)
				{
					// Nasturtium[Pick,Inspect,Guide] 7887
					return new PatchState(Produce.NASTURTIUM, CropState.HARVESTABLE, 0, Action.HARVEST);
				}
				if (value >= 23 && value <= 26)
				{
					// Woad plant[Inspect,Guide] 7919,7920,7921,7922
					return new PatchState(Produce.WOAD, CropState.GROWING, value - 23, Action.WAIT);
				}
				if (value == 27)
				{
					// Woad plant[Pick,Inspect,Guide] 7923
					return new PatchState(Produce.WOAD, CropState.HARVESTABLE, 0, Action.HARVEST);
				}
				if (value >= 28 && value <= 31)
				{
					// Limpwurt plant[Inspect,Guide] 7851,7852,7853,7854
					return new PatchState(Produce.LIMPWURT, CropState.GROWING, value - 28, Action.WAIT);
				}
				if (value == 32)
				{
					// Limpwurt plant[Pick,Inspect,Guide] 7855
					return new PatchState(Produce.LIMPWURT, CropState.HARVESTABLE, 0, Action.HARVEST);
				}
				if (value >= 33 && value <= 35)
				{
					// Scarecrow[Rake,Inspect,Guide,Remove] 7918,7917,7916
					return new PatchState(Produce.SCARECROW, CropState.GROWING, 35 - value, Action.GROWN);
				}
				if (value == 36)
				{
					// Scarecrow[Remove,Inspect,Guide] 7915
					return new PatchState(Produce.SCARECROW, CropState.GROWING, 0, Action.GROWN);
				}
				if (value >= 37 && value <= 40)
				{
					// White lily[Inspect,Guide] 33650,33651,33652,33653
					return new PatchState(Produce.WHITE_LILY, CropState.GROWING, value - 37, Action.WAIT);
				}
				if (value == 41)
				{
					// White lily[Pick,Inspect,Guide] 33654
					return new PatchState(Produce.WHITE_LILY, CropState.HARVESTABLE, 0, Action.HARVEST);
				}
				if (value >= 42 && value <= 71)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 72 && value <= 75)
				{
					// Marigold[Inspect,Guide] 7872,7873,7874,7875
					return new PatchState(Produce.MARIGOLD, CropState.GROWING, value - 72, Action.WAIT);
				}
				if (value == 76)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 77 && value <= 80)
				{
					// Rosemary[Inspect,Guide] 7904,7905,7906,7907
					return new PatchState(Produce.ROSEMARY, CropState.GROWING, value - 77, Action.WAIT);
				}
				if (value == 81)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 82 && value <= 85)
				{
					// Nasturtium[Inspect,Guide] 7888,7889,7890,7891
					return new PatchState(Produce.NASTURTIUM, CropState.GROWING, value - 82, Action.WAIT);
				}
				if (value == 86)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 87 && value <= 90)
				{
					// Woad plant[Inspect,Guide] 7924,7925,7926,7927
					return new PatchState(Produce.WOAD, CropState.GROWING, value - 87, Action.WAIT);
				}
				if (value == 91)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 92 && value <= 95)
				{
					// Limpwurt plant[Inspect,Guide] 7856,7857,7858,7859
					return new PatchState(Produce.LIMPWURT, CropState.GROWING, value - 92, Action.WAIT);
				}
				if (value >= 96 && value <= 100)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843,7843,7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 101 && value <= 104)
				{
					// White lily[Inspect,Guide] 33655,33656,33657,33658
					return new PatchState(Produce.WHITE_LILY, CropState.GROWING, value - 101, Action.WAIT);
				}
				if (value >= 105 && value <= 136)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 137 && value <= 139)
				{
					// Diseased marigold[Cure,Inspect,Guide] 7876,7877,7878
					return new PatchState(Produce.MARIGOLD, CropState.DISEASED, value - 136, Action.CURE);
				}
				if (value >= 140 && value <= 141)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 142 && value <= 144)
				{
					// Diseased rosemary[Cure,Inspect,Guide] 7908,7909,7910
					return new PatchState(Produce.ROSEMARY, CropState.DISEASED, value - 141, Action.CURE);
				}
				if (value >= 145 && value <= 146)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 147 && value <= 149)
				{
					// Diseased nasturtium[Cure,Inspect,Guide] 7892,7893,7894
					return new PatchState(Produce.NASTURTIUM, CropState.DISEASED, value - 146, Action.CURE);
				}
				if (value >= 150 && value <= 151)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 152 && value <= 154)
				{
					// Diseased woad plant[Cure,Inspect,Guide] 7928,7929,7930
					return new PatchState(Produce.WOAD, CropState.DISEASED, value - 151, Action.CURE);
				}
				if (value >= 155 && value <= 156)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 157 && value <= 159)
				{
					// Diseased limpwurt plant[Cure,Inspect,Guide] 7860,7861,7862
					return new PatchState(Produce.LIMPWURT, CropState.DISEASED, value - 156, Action.CURE);
				}
				if (value >= 160 && value <= 165)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843,7843,7843,7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 166 && value <= 168)
				{
					// Diseased White lily[Cure,Inspect,Guide] 33659,33660,33661
					return new PatchState(Produce.WHITE_LILY, CropState.DISEASED, value - 165, Action.CURE);
				}
				if (value >= 169 && value <= 200)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 201 && value <= 204)
				{
					// Dead marigold[Clear,Inspect,Guide] 7879,7880,7881,7882
					return new PatchState(Produce.MARIGOLD, CropState.DEAD, value - 200, Action.CLEAR);
				}
				if (value == 205)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 206 && value <= 209)
				{
					// Dead rosemary[Clear,Inspect,Guide] 7911,7912,7913,7914
					return new PatchState(Produce.ROSEMARY, CropState.DEAD, value - 205, Action.CLEAR);
				}
				if (value == 210)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 211 && value <= 214)
				{
					// Dead nasturtium[Clear,Inspect,Guide] 7895,7896,7897,7898
					return new PatchState(Produce.NASTURTIUM, CropState.DEAD, value - 210, Action.CLEAR);
				}
				if (value == 215)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 216 && value <= 219)
				{
					// Dead woad plant[Clear,Inspect,Guide] 7931,7932,7933,7934
					return new PatchState(Produce.WOAD, CropState.DEAD, value - 215, Action.CLEAR);
				}
				if (value == 220)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 221 && value <= 224)
				{
					// Dead limpwurt plant[Clear,Inspect,Guide] 7863,7864,7865,7866
					return new PatchState(Produce.LIMPWURT, CropState.DEAD, value - 220, Action.CLEAR);
				}
				if (value >= 225 && value <= 229)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843,7843,7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 230 && value <= 233)
				{
					// Dead White lily[Clear,Inspect,Guide] 33662,33663,33664,33665
					return new PatchState(Produce.WHITE_LILY, CropState.DEAD, value - 229, Action.CLEAR);
				}
				if (value >= 234 && value <= 255)
				{
					// Flower Patch[Rake,Inspect,Guide] 7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843,7843
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},
	HOPS("", false)
		{
			@Override
			public PatchState forVarbitValue(int value)
			{
				if (value >= 0 && value <= 3)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8209,8208,8207
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3 - value, Action.RAKE);
				}
				if (value >= 4 && value <= 7)
				{
					// Hammerstone Hops[Inspect,Guide] 8177,8178,8179,8180
					return new PatchState(Produce.HAMMERSTONE, CropState.GROWING, value - 4, Action.WAIT);
				}
				if (value >= 8 && value <= 10)
				{
					// Hammerstone Hops[Harvest,Inspect,Guide] 8181,8181,8181
					return new PatchState(Produce.HAMMERSTONE, CropState.HARVESTABLE, 10 - value, Action.HARVEST);
				}
				if (value >= 11 && value <= 13)
				{
					// Diseased Hammerstone Hops[Cure,Inspect,Guide] 8186,8187,8188
					return new PatchState(Produce.HAMMERSTONE, CropState.DISEASED, value - 10, Action.CURE);
				}
				if (value >= 14 && value <= 18)
				{
					// Asgarnian Hops[Inspect,Guide] 8154,8155,8156,8157,8158
					return new PatchState(Produce.ASGARNIAN, CropState.GROWING, value - 14, Action.WAIT);
				}
				if (value >= 19 && value <= 21)
				{
					// Asgarnian Hops[Harvest,Inspect,Guide] 8159,8159,8159
					return new PatchState(Produce.ASGARNIAN, CropState.HARVESTABLE, 21 - value, Action.HARVEST);
				}
				if (value >= 22 && value <= 25)
				{
					// Diseased Asgarnian Hops[Cure,Inspect,Guide] 8165,8166,8167,8168
					return new PatchState(Produce.ASGARNIAN, CropState.DISEASED, value - 21, Action.CURE);
				}
				if (value >= 26 && value <= 31)
				{
					// Yanillian Hops[Inspect,Guide] 8288,8289,8290,8291,8292,8293
					return new PatchState(Produce.YANILLIAN, CropState.GROWING, value - 26, Action.WAIT);
				}
				if (value >= 32 && value <= 34)
				{
					// Yanillian Hops[Harvest,Inspect,Guide] 8294,8294,8294
					return new PatchState(Produce.YANILLIAN, CropState.HARVESTABLE, 34 - value, Action.HARVEST);
				}
				if (value >= 35 && value <= 39)
				{
					// Diseased Yanillian Hops[Cure,Inspect,Guide] 8301,8302,8303,8304,8305
					return new PatchState(Produce.YANILLIAN, CropState.DISEASED, value - 34, Action.CURE);
				}
				if (value >= 40 && value <= 46)
				{
					// Krandorian Hops[Inspect,Guide] 8211,8212,8213,8214,8215,8216,8217
					return new PatchState(Produce.KRANDORIAN, CropState.GROWING, value - 40, Action.WAIT);
				}
				if (value >= 47 && value <= 49)
				{
					// Krandorian Hops[Harvest,Inspect,Guide] 8218,8218,8218
					return new PatchState(Produce.KRANDORIAN, CropState.HARVESTABLE, 49 - value, Action.HARVEST);
				}
				if (value >= 50 && value <= 55)
				{
					// Diseased Krandorian Hops[Cure,Inspect,Guide] 8226,8227,8228,8229,8230,8231
					return new PatchState(Produce.KRANDORIAN, CropState.DISEASED, value - 49, Action.CURE);
				}
				if (value >= 56 && value <= 63)
				{
					// Wildblood Hops[Inspect,Guide] 8257,8258,8259,8260,8261,8262,8263,8264
					return new PatchState(Produce.WILDBLOOD, CropState.GROWING, value - 56, Action.WAIT);
				}
				if (value >= 64 && value <= 66)
				{
					// Wildblood Hops[Harvest,Inspect,Guide] 8265,8265,8265
					return new PatchState(Produce.WILDBLOOD, CropState.HARVESTABLE, 66 - value, Action.HARVEST);
				}
				if (value >= 67 && value <= 73)
				{
					// Diseased Wildblood Hops[Cure,Inspect,Guide] 8274,8275,8276,8277,8278,8279,8280
					return new PatchState(Produce.WILDBLOOD, CropState.DISEASED, value - 66, Action.CURE);
				}
				if (value >= 74 && value <= 77)
				{
					// Barley[Inspect,Guide] 8192,8193,8194,8195
					return new PatchState(Produce.BARLEY, CropState.GROWING, value - 74, Action.WAIT);
				}
				if (value >= 78 && value <= 80)
				{
					// Barley[Harvest,Inspect,Guide] 8196,8196,8196
					return new PatchState(Produce.BARLEY, CropState.HARVESTABLE, 80 - value, Action.HARVEST);
				}
				if (value >= 81 && value <= 83)
				{
					// Diseased Barley[Cure,Inspect,Guide] 8201,8202,8203
					return new PatchState(Produce.BARLEY, CropState.DISEASED, value - 80, Action.CURE);
				}
				if (value >= 84 && value <= 88)
				{
					// Jute[Inspect,Guide] 8238,8239,8240,8241,8242
					return new PatchState(Produce.JUTE, CropState.GROWING, value - 84, Action.WAIT);
				}
				if (value >= 89 && value <= 91)
				{
					// Jute[Harvest,Inspect,Guide] 8243,8243,8243
					return new PatchState(Produce.JUTE, CropState.HARVESTABLE, 91 - value, Action.HARVEST);
				}
				if (value >= 92 && value <= 95)
				{
					// Diseased Jute[Cure,Inspect,Guide] 8249,8250,8251,8252
					return new PatchState(Produce.JUTE, CropState.DISEASED, value - 91, Action.CURE);
				}
				if (value >= 96 && value <= 98)
				{
					// Flax[Inspect,Guide] 58836,58837,58838
					return new PatchState(Produce.FLAX, CropState.GROWING, value - 96, Action.WAIT);
				}
				if (value >= 99 && value <= 101)
				{
					// Flax[Harvest,Inspect,Guide] 58839,58839,58839
					return new PatchState(Produce.FLAX, CropState.HARVESTABLE, 101 - value, Action.HARVEST);
				}
				if (value >= 102 && value <= 103)
				{
					// Diseased Flax[Cure,Inspect,Guide] 58843,58844
					return new PatchState(Produce.FLAX, CropState.DISEASED, value - 101, Action.CURE);
				}
				if (value >= 104 && value <= 107)
				{
					// Hemp[Inspect,Guide] 58847,58848,58849,58850
					return new PatchState(Produce.HEMP, CropState.GROWING, value - 104, Action.WAIT);
				}
				if (value >= 108 && value <= 110)
				{
					// Hemp[Harvest,Inspect,Guide] 58851,58851,58851
					return new PatchState(Produce.HEMP, CropState.HARVESTABLE, 110 - value, Action.HARVEST);
				}
				if (value >= 111 && value <= 113)
				{
					// Diseased Hemp[Cure,Inspect,Guide] 58856,58857,58858
					return new PatchState(Produce.HEMP, CropState.DISEASED, value - 110, Action.CURE);
				}
				if (value >= 114 && value <= 118)
				{
					// Cotton[Inspect,Guide] 58862,58863,58864,58865,58866
					return new PatchState(Produce.COTTON, CropState.GROWING, value - 114, Action.WAIT);
				}
				if (value >= 119 && value <= 121)
				{
					// Cotton[Harvest,Inspect,Guide] 58867,58867,58867
					return new PatchState(Produce.COTTON, CropState.HARVESTABLE, 121 - value, Action.HARVEST);
				}
				if (value >= 122 && value <= 125)
				{
					// Diseased Cotton[Cure,Inspect,Guide] 58873,58874,58875,58876
					return new PatchState(Produce.COTTON, CropState.DISEASED, value - 121, Action.CURE);
				}
				if (value >= 126 && value <= 131)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210,8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 132 && value <= 135)
				{
					// Hammerstone Hops[Inspect,Guide] 8182,8183,8184,8185
					return new PatchState(Produce.HAMMERSTONE, CropState.GROWING, value - 132, Action.WAIT);
				}
				if (value >= 136 && value <= 138)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 139 && value <= 141)
				{
					// Dead Hammerstone Hops[Clear,Inspect,Guide] 8189,8190,8191
					return new PatchState(Produce.HAMMERSTONE, CropState.DEAD, value - 138, Action.CLEAR);
				}
				if (value >= 142 && value <= 146)
				{
					// Asgarnian Hops[Inspect,Guide] 8160,8161,8162,8163,8164
					return new PatchState(Produce.ASGARNIAN, CropState.GROWING, value - 142, Action.WAIT);
				}
				if (value >= 147 && value <= 149)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 150 && value <= 153)
				{
					// Dead Asgarnian Hops[Clear,Inspect,Guide] 8169,8170,8171,8172
					return new PatchState(Produce.ASGARNIAN, CropState.DEAD, value - 149, Action.CLEAR);
				}
				if (value >= 154 && value <= 159)
				{
					// Yanillian Hops[Inspect,Guide] 8295,8296,8297,8298,8299,8300
					return new PatchState(Produce.YANILLIAN, CropState.GROWING, value - 154, Action.WAIT);
				}
				if (value >= 160 && value <= 162)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 163 && value <= 167)
				{
					// Dead Yanillian Hops[Clear,Inspect,Guide] 8306,8307,8308,8309,8310
					return new PatchState(Produce.YANILLIAN, CropState.DEAD, value - 162, Action.CLEAR);
				}
				if (value >= 168 && value <= 174)
				{
					// Krandorian Hops[Inspect,Guide] 8219,8220,8221,8222,8223,8224,8225
					return new PatchState(Produce.KRANDORIAN, CropState.GROWING, value - 168, Action.WAIT);
				}
				if (value >= 175 && value <= 177)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 178 && value <= 183)
				{
					// Dead Krandorian Hops[Clear,Inspect,Guide] 8232,8233,8234,8235,8236,8237
					return new PatchState(Produce.KRANDORIAN, CropState.DEAD, value - 177, Action.CLEAR);
				}
				if (value >= 184 && value <= 191)
				{
					// Wildblood Hops[Inspect,Guide] 8266,8267,8268,8269,8270,8271,8272,8273
					return new PatchState(Produce.WILDBLOOD, CropState.GROWING, value - 184, Action.WAIT);
				}
				if (value >= 192 && value <= 194)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 195 && value <= 201)
				{
					// Dead Wildblood Hops[Clear,Inspect,Guide] 8281,8282,8283,8284,8285,8286,8287
					return new PatchState(Produce.WILDBLOOD, CropState.DEAD, value - 194, Action.CLEAR);
				}
				if (value >= 202 && value <= 205)
				{
					// Barley[Inspect,Guide] 8197,8198,8199,8200
					return new PatchState(Produce.BARLEY, CropState.GROWING, value - 202, Action.WAIT);
				}
				if (value >= 206 && value <= 208)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 209 && value <= 211)
				{
					// Dead Barley[Clear,Inspect,Guide] 8204,8205,8206
					return new PatchState(Produce.BARLEY, CropState.DEAD, value - 208, Action.CLEAR);
				}
				if (value >= 212 && value <= 216)
				{
					// Jute[Inspect,Guide] 8244,8245,8246,8247,8248
					return new PatchState(Produce.JUTE, CropState.GROWING, value - 212, Action.WAIT);
				}
				if (value >= 217 && value <= 219)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 220 && value <= 223)
				{
					// Dead Jute[Clear,Inspect,Guide] 8253,8254,8255,8256
					return new PatchState(Produce.JUTE, CropState.DEAD, value - 219, Action.CLEAR);
				}
				if (value >= 224 && value <= 226)
				{
					// Flax[Inspect,Guide] 58840,58841,58842
					return new PatchState(Produce.FLAX, CropState.GROWING, value - 224, Action.WAIT);
				}
				if (value >= 227 && value <= 229)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 230 && value <= 231)
				{
					// Dead Flax[Clear,Inspect,Guide] 58845,58846
					return new PatchState(Produce.FLAX, CropState.DEAD, value - 229, Action.CLEAR);
				}
				if (value >= 232 && value <= 235)
				{
					// Hemp[Inspect,Guide] 58852,58853,58854,58855
					return new PatchState(Produce.HEMP, CropState.GROWING, value - 232, Action.WAIT);
				}
				if (value >= 236 && value <= 238)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 239 && value <= 240)
				{
					// Dead Hemp[Clear,Inspect,Guide] 58859,58860
					return new PatchState(Produce.HEMP, CropState.DEAD, value - 238, Action.CLEAR);
				}
				if (value == 241)
				{
					// Dead Hemp[Inspect,Guide] 58861
					return new PatchState(Produce.HEMP, CropState.DEAD, 3, Action.CLEAR);
				}
				if (value >= 242 && value <= 246)
				{
					// Cotton[Inspect,Guide] 58868,58869,58870,58871,58872
					return new PatchState(Produce.COTTON, CropState.GROWING, value - 242, Action.WAIT);
				}
				if (value >= 247 && value <= 249)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				if (value >= 250 && value <= 253)
				{
					// Dead Cotton[Clear,Inspect,Guide] 58877,58878,58879,58880
					return new PatchState(Produce.COTTON, CropState.DEAD, value - 249, Action.CLEAR);
				}
				if (value >= 254 && value <= 255)
				{
					// Hops Patch[Rake,Inspect,Guide] 8210,8210
					return new PatchState(Produce.WEEDS, CropState.GROWING, 3, Action.RAKE);
				}
				return null;
			}
		},

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
