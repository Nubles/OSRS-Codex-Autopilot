package com.codex.autopilot.core;

import java.util.Arrays;

/** Something a chore takes along: a count of any one of a few items, such as 40 hop seeds. */
public final class Need
{
	private final String name;
	private final int amount;
	private final int[] ids;

	public Need(String name, int amount, int... ids)
	{
		this.name = name;
		this.amount = Math.max(0, amount);
		this.ids = ids.clone();
	}

	/** What it is in a sentence, such as "seeds" or "oak logs". */
	public String getName()
	{
		return name;
	}

	public int getAmount()
	{
		return amount;
	}

	public int[] getIds()
	{
		return ids.clone();
	}

	public boolean accepts(int id)
	{
		for (int i : ids)
		{
			if (i == id)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		return amount + " " + name + " " + Arrays.toString(ids);
	}
}
