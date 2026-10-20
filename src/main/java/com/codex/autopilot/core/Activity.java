package com.codex.autopilot.core;

import java.util.Collection;

/**
 * The minigames another Codex Pilot already guides, so Autopilot can step aside there instead of
 * nagging. Detection follows Codex Minigame Pilot: the Motherlode Mine's regions, the Blast
 * Furnace's region, and a Mahogany Homes contractor close by.
 */
public enum Activity
{
	NONE("", ""),
	MOTHERLODE("Motherlode Mine", "In the Motherlode Mine"),
	BLAST_FURNACE("Blast Furnace", "At the Blast Furnace"),
	MAHOGANY_HOMES("Mahogany Homes", "On a Mahogany Homes contract");

	static final int[] MOTHERLODE_REGIONS = {14679, 14680, 14681, 14935, 14936, 14937, 15191, 15192, 15193};
	static final int BLAST_FURNACE_REGION = 7757;
	/** The four Mahogany Homes contractors: Falador, Varrock, East Ardougne and Hosidius. */
	static final String[] CONTRACTORS = {"Amy", "Marlo", "Ellie", "Angelo"};

	private final String label;
	private final String where;

	Activity(String label, String where)
	{
		this.label = label;
		this.where = where;
	}

	public String getLabel()
	{
		return label;
	}

	/** "In the Motherlode Mine". */
	public String getWhere()
	{
		return where;
	}

	/**
	 * @param region the region the player stands in, or -1
	 * @param nearbyNpcs names of the NPCs close by, may be null
	 */
	public static Activity of(int region, Collection<String> nearbyNpcs)
	{
		for (int r : MOTHERLODE_REGIONS)
		{
			if (r == region)
			{
				return MOTHERLODE;
			}
		}
		if (region == BLAST_FURNACE_REGION)
		{
			return BLAST_FURNACE;
		}
		if (nearbyNpcs != null)
		{
			for (String n : nearbyNpcs)
			{
				if (n == null)
				{
					continue;
				}
				for (String c : CONTRACTORS)
				{
					if (c.equalsIgnoreCase(n.trim()))
					{
						return MAHOGANY_HOMES;
					}
				}
			}
		}
		return NONE;
	}
}
