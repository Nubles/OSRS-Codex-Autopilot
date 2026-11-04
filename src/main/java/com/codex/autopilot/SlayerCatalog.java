package com.codex.autopilot;

import com.codex.autopilot.core.Slayer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;

/**
 * What the Slayer task is, read from the game's own tables on the client thread, the way
 * RuneLite's Slayer plugin reads it: the task table ({@code DBTableID.SlayerTask}, or for a boss
 * task the sublist {@code SlayerTaskSublist}) gives the task's row and name, the area table
 * ({@code SlayerArea}) Konar's area. Which master the {@code SLAYER_MASTER} varbit names is read
 * from the master task table ({@code SlayerMasterTask}): its rows are named by master in
 * RuneLite's gameval, so one row of each gives that master's id. Nothing is written in.
 */
final class SlayerCatalog
{
	private final Client client;
	private final Map<String, Integer> rows = new HashMap<>();
	private final Map<Integer, String> names = new HashMap<>();
	private final Map<Integer, String> areas = new HashMap<>();
	private Map<Integer, Slayer.Master> masters;

	SlayerCatalog(Client client)
	{
		this.client = client;
	}

	/** The task table row for this task, or -1 when the tables are not loaded or have no such task. */
	int row(int taskId, int bossId)
	{
		String key = taskId + ":" + bossId;
		Integer known = rows.get(key);
		if (known != null)
		{
			return known;
		}
		int row = -1;
		try
		{
			if (taskId == Slayer.BOSS_TASK)
			{
				List<Integer> boss = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID, DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
					0, bossId);
				Integer r = boss == null || boss.isEmpty() ? null : integer(boss.get(0), DBTableID.SlayerTaskSublist.COL_TASK);
				row = r == null ? -1 : r;
			}
			else
			{
				List<Integer> task = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
				row = task == null || task.isEmpty() || task.get(0) == null ? -1 : task.get(0);
			}
		}
		catch (RuntimeException e)
		{
			row = -1;
		}
		if (row > 0)
		{
			rows.put(key, row);
		}
		return row;
	}

	/** The task's name, such as "Kurask", or null. */
	String name(int row)
	{
		if (row <= 0)
		{
			return null;
		}
		if (names.containsKey(row))
		{
			return names.get(row);
		}
		String n = string(row, DBTableID.SlayerTask.COL_NAME_UPPERCASE);
		if (n != null)
		{
			names.put(row, n);
		}
		return n;
	}

	/** Konar's area's name, or null. */
	String area(int areaId)
	{
		if (areaId <= 0)
		{
			return null;
		}
		if (areas.containsKey(areaId))
		{
			return areas.get(areaId);
		}
		String n = null;
		try
		{
			List<Integer> found = client.getDBRowsByValue(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, 0, areaId);
			if (found != null && !found.isEmpty() && found.get(0) != null)
			{
				n = string(found.get(0), DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER);
			}
		}
		catch (RuntimeException e)
		{
			n = null;
		}
		if (n != null)
		{
			areas.put(areaId, n);
		}
		return n;
	}

	/** Master id to master, as far as the tables say; empty while they are not loaded. */
	Map<Integer, Slayer.Master> masters()
	{
		if (masters != null && !masters.isEmpty())
		{
			return masters;
		}
		Map<Integer, Slayer.Master> map = new HashMap<>();
		Map<Slayer.Master, Integer> seen = new EnumMap<>(Slayer.Master.class);
		for (Slayer.Master m : Slayer.Master.values())
		{
			Integer id = integer(m.getSampleRow(), DBTableID.SlayerMasterTask.COL_MASTER_ID);
			if (id != null && id > 0)
			{
				seen.put(m, id);
				map.put(id, m);
			}
		}
		// Two masters on one id would mean the table is not what it was: trust none of it.
		masters = map.size() == seen.size() ? map : Collections.emptyMap();
		return masters;
	}

	private Object[] field(int row, int column)
	{
		try
		{
			return client.getDBTableField(row, column, 0);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private Integer integer(int row, int column)
	{
		Object[] f = field(row, column);
		return f == null || f.length == 0 || !(f[0] instanceof Integer) ? null : (Integer) f[0];
	}

	private String string(int row, int column)
	{
		Object[] f = field(row, column);
		return f == null || f.length == 0 || !(f[0] instanceof String) ? null : (String) f[0];
	}
}
