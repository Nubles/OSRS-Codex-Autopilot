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
 * What a patch holds, how far it has grown and what it wants doing. Adapted from RuneLite's Time
 * Tracking plugin, with the next action added.
 */
public final class PatchState
{
	private final Produce produce;
	private final CropState cropState;
	private final int stage;
	private final Action action;

	public PatchState(Produce produce, CropState cropState, int stage, Action action)
	{
		this.produce = produce;
		this.cropState = cropState;
		this.stage = stage;
		this.action = action;
	}

	public Produce getProduce()
	{
		return produce;
	}

	public CropState getCropState()
	{
		return cropState;
	}

	public int getStage()
	{
		return stage;
	}

	public Action getAction()
	{
		return action;
	}

	/** Weeds fully raked away: the patch is ready to plant. */
	public boolean isClear()
	{
		return produce == Produce.WEEDS && stage == 0;
	}

	/** Weeds, raked or not: nothing is planted. */
	public boolean isEmpty()
	{
		return produce == Produce.WEEDS;
	}

	public int getStages()
	{
		return cropState == CropState.HARVESTABLE || cropState == CropState.FILLING ? produce.getHarvestStages() : produce.getStages();
	}

	/** Minutes per growth tick in this state, or 0 when it does not grow. */
	public int getTickRate()
	{
		switch (cropState)
		{
			case HARVESTABLE:
				return produce.getRegrowTickrate();
			case GROWING:
				return produce.getTickrate();
			default:
				return 0;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof PatchState))
		{
			return false;
		}
		PatchState p = (PatchState) o;
		return produce == p.produce && cropState == p.cropState && stage == p.stage && action == p.action;
	}

	@Override
	public int hashCode()
	{
		return ((produce.hashCode() * 31 + cropState.hashCode()) * 31 + stage) * 31 + action.hashCode();
	}

	@Override
	public String toString()
	{
		return produce + " " + cropState + " " + stage + " " + action;
	}
}
