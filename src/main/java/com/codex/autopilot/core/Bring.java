package com.codex.autopilot.core;

import java.util.List;

/**
 * Says what to take for a chore, checked against the banked snapshot when there is one: "Bring 4
 * oak logs + 40 seeds", or "You're out of hop seeds" when the bank has none. It stays light: no
 * pull list, just the one line that saves a wasted trip.
 */
public final class Bring
{
	private Bring()
	{
	}

	/** The line to show, or null when there is nothing to bring. */
	public static String line(List<Need> needs, BankSnapshot bank)
	{
		StringBuilder list = new StringBuilder();
		for (Need n : needs)
		{
			if (n.getAmount() <= 0)
			{
				continue;
			}
			if (bank != null)
			{
				long have = bank.count(n);
				if (have <= 0 && !bank.isTruncated())
				{
					return "You're out of " + n.getName();
				}
				if (have > 0 && have < n.getAmount())
				{
					return "Only " + Format.number(have) + " " + n.getName() + " banked, " + Format.number(n.getAmount()) + " needed";
				}
			}
			if (list.length() > 0)
			{
				list.append(" + ");
			}
			list.append(Format.number(n.getAmount())).append(' ').append(n.getName());
		}
		return list.length() == 0 ? null : "Bring " + list;
	}

	/** Whether the line warns of something missing. */
	public static boolean isShort(String line)
	{
		return line != null && (line.startsWith("You're out of") || line.startsWith("Only "));
	}
}
