package com.codex.autopilot;

import com.codex.autopilot.core.PortLog;
import com.codex.autopilot.core.PortTask;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPCComposition;
import net.runelite.api.gameval.DBTableID;

/**
 * What each port task is, read from the game's own tables on the client thread: the port task
 * table ({@code DBTableID.PortTask}) gives the task's name, level, notice board, cargo port,
 * destination, cargo and amount, or the bounty's creature, part and count; the docking point table
 * ({@code DBTableID.SailingDock}) gives each port's name; item and NPC definitions give the cargo's,
 * the part's and the creature's names. Nothing is written in, so new and changed tasks read as the game
 * has them, and a row it cannot make sense of (the quests' own tasks, say) gives nothing.
 */
final class PortCatalog implements PortLog.Catalog
{
	private final Client client;
	/** Task id to its row in the port task table, built on first use. */
	private Map<Integer, Integer> rows;
	private final Map<Integer, PortTask> tasks = new HashMap<>();
	private final Map<Integer, String> docks = new HashMap<>();

	PortCatalog(Client client)
	{
		this.client = client;
	}

	/** Forgets what was read, for a new game cache. */
	void clear()
	{
		rows = null;
		tasks.clear();
		docks.clear();
	}

	@Override
	public PortTask lookup(int id)
	{
		if (tasks.containsKey(id))
		{
			return tasks.get(id);
		}
		Integer row = rows().get(id);
		PortTask t = row == null ? null : read(id, row);
		if (t != null || rows != null && !rows.isEmpty())
		{
			// An empty table means the cache was not ready: ask again next time.
			tasks.put(id, t);
		}
		return t;
	}

	private Map<Integer, Integer> rows()
	{
		if (rows == null || rows.isEmpty())
		{
			Map<Integer, Integer> map = new HashMap<>();
			List<Integer> all;
			try
			{
				all = client.getDBTableRows(DBTableID.PortTask.ID);
			}
			catch (RuntimeException e)
			{
				all = null;
			}
			if (all != null)
			{
				for (Integer row : all)
				{
					Integer id = row == null ? null : integer(row, DBTableID.PortTask.COL_TASK_ID, 0);
					if (id != null && id > 0)
					{
						map.putIfAbsent(id, row);
					}
				}
			}
			rows = map;
		}
		return rows;
	}

	private PortTask read(int id, int row)
	{
		String name = string(row, DBTableID.PortTask.COL_NAME, 0);
		int level = orZero(integer(row, DBTableID.PortTask.COL_LEVEL_REQUIRED, 0));
		Integer board = integer(row, DBTableID.PortTask.COL_STARTING_PORT, 0);
		Integer from = integer(row, DBTableID.PortTask.COL_CARGO_PORT, 0);
		Integer to = integer(row, DBTableID.PortTask.COL_ENDING_PORT, 0);
		Integer part = integer(row, DBTableID.PortTask.COL_BOUNTY_OBJECT, 0);
		Integer count = integer(row, DBTableID.PortTask.COL_BOUNTY_OBJECT_AMOUNT, 0);
		String boardName = board == null ? null : dock(board);
		if (part != null && part > 0 && count != null && count > 0)
		{
			Integer npc = integer(row, DBTableID.PortTask.COL_BOUNTY_TARGET_ALIVE, 0);
			return PortTask.bounty(id, name, level, orZero(board), boardName, npc == null ? null : npcName(npc), part,
				itemName(part), count);
		}
		Object[] cargo = field(row, DBTableID.PortTask.COL_CARGO, 0);
		if (from == null || to == null || from.equals(to) || cargo == null || cargo.length < 2
			|| !(cargo[0] instanceof Integer) || !(cargo[1] instanceof Integer))
		{
			return null;
		}
		String fromName = dock(from);
		String toName = dock(to);
		if (fromName == null || toName == null)
		{
			return null;
		}
		int item = (Integer) cargo[0];
		return PortTask.courier(id, name, level, orZero(board), boardName, from, fromName, to, toName, item,
			cargoName(itemName(item)), (Integer) cargo[1]);
	}

	/** "Crate of dye" reads as "Dye": the ledger table hands out crates, so the word is said once. */
	static String cargoName(String item)
	{
		if (item == null)
		{
			return null;
		}
		String s = item.trim();
		for (String lead : new String[]{"Crates of ", "Crate of "})
		{
			if (s.regionMatches(true, 0, lead, 0, lead.length()) && s.length() > lead.length())
			{
				s = s.substring(lead.length());
				return Character.toUpperCase(s.charAt(0)) + s.substring(1);
			}
		}
		return s;
	}

	private String dock(int row)
	{
		if (docks.containsKey(row))
		{
			return docks.get(row);
		}
		String name = string(row, DBTableID.SailingDock.COL_NICE_NAME, 0);
		docks.put(row, name);
		return name;
	}

	private String itemName(int id)
	{
		try
		{
			ItemComposition c = client.getItemDefinition(id);
			return c == null || "null".equals(c.getName()) ? null : c.getName();
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private String npcName(int id)
	{
		try
		{
			NPCComposition c = client.getNpcDefinition(id);
			return c == null || "null".equals(c.getName()) ? null : c.getName();
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private Object[] field(int row, int column, int tuple)
	{
		try
		{
			return client.getDBTableField(row, column, tuple);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private Integer integer(int row, int column, int tuple)
	{
		Object[] f = field(row, column, tuple);
		return f == null || f.length == 0 || !(f[0] instanceof Integer) ? null : (Integer) f[0];
	}

	private String string(int row, int column, int tuple)
	{
		Object[] f = field(row, column, tuple);
		return f == null || f.length == 0 || !(f[0] instanceof String) ? null : (String) f[0];
	}

	private static int orZero(Integer v)
	{
		return v == null ? 0 : v;
	}
}
