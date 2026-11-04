package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * The account's Sailing port tasks: the ones in its slots as the game last showed them, the Sailing
 * xp each task paid when this account handed it in, and how many it has done.
 *
 * <p>Saved per RS profile under {@code codexautopilot/portTasksV1}, so the voyage still shows while
 * logged out and other Codex apps can read it:
 * {@code {"version":1,"savedAt":ms,"sailing":62,"completed":140,"tasks":[{"slot":0,"id":101,"name":"...",
 * "type":"COURIER","level":46,"boardDock":8605,"board":"Aldarin","fromDock":8616,"from":"Prifddinas",
 * "toDock":8605,"to":"Aldarin","cargoItem":1234,"cargo":"Dye","amount":1,"taken":0,"delivered":0}],
 * "xp":{"101":7082}}}. Fields only grow; a breaking change would get a new key.</p>
 *
 * <p>The xp is learned, never written in: a slot that empties in the same moment Sailing xp arrives
 * was handed in and paid that much (a cancelled task pays nothing and teaches nothing). The wiki's
 * tables list the reward for every task, but they move with game updates, and a wrong figure is worse
 * than none, so a task this account has not handed in shows no xp.</p>
 */
public final class PortLog
{
	public static final String KEY = "portTasksV1";
	public static final int VERSION = 1;
	/** Slots the game has, 0 to 4. */
	public static final int SLOTS = 5;

	/** {@code PORT_TASK_SLOT_n_ID}: the task in each slot, 0 when empty. */
	public static final int[] SLOT_ID = {VarbitID.PORT_TASK_SLOT_0_ID, VarbitID.PORT_TASK_SLOT_1_ID, VarbitID.PORT_TASK_SLOT_2_ID,
		VarbitID.PORT_TASK_SLOT_3_ID, VarbitID.PORT_TASK_SLOT_4_ID};
	/** {@code PORT_TASK_SLOT_n_CARGO_TAKEN}: crates collected from the ledger table. */
	static final int[] SLOT_TAKEN = {VarbitID.PORT_TASK_SLOT_0_CARGO_TAKEN, VarbitID.PORT_TASK_SLOT_1_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_2_CARGO_TAKEN, VarbitID.PORT_TASK_SLOT_3_CARGO_TAKEN, VarbitID.PORT_TASK_SLOT_4_CARGO_TAKEN};
	/** {@code PORT_TASK_SLOT_n_CARGO_DELIVERED}: crates handed in at the destination. */
	static final int[] SLOT_DELIVERED = {VarbitID.PORT_TASK_SLOT_0_CARGO_DELIVERED, VarbitID.PORT_TASK_SLOT_1_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_2_CARGO_DELIVERED, VarbitID.PORT_TASK_SLOT_3_CARGO_DELIVERED, VarbitID.PORT_TASK_SLOT_4_CARGO_DELIVERED};
	/** {@code BOUNTY_TASK_ITEM_COUNTER_1..5}: a bounty's parts still wanted, slot by slot. */
	static final int[] SLOT_BOUNTY = {VarbitID.BOUNTY_TASK_ITEM_COUNTER_1, VarbitID.BOUNTY_TASK_ITEM_COUNTER_2,
		VarbitID.BOUNTY_TASK_ITEM_COUNTER_3, VarbitID.BOUNTY_TASK_ITEM_COUNTER_4, VarbitID.BOUNTY_TASK_ITEM_COUNTER_5};
	/** The account's port tasks done, all time. */
	static final int COMPLETED = VarPlayerID.PORT_TASKS_COMPLETED;

	/**
	 * Tasks held at once by Sailing level (OSRS wiki, Port tasks: 1 at level 1, 2 at 7, 3 at 28, 4 at
	 * 56 and 5 at 84).
	 */
	static final int[] SLOT_LEVELS = {1, 7, 28, 56, 84};
	/** Bounty tasks open at 30 Sailing (OSRS wiki, Bounty tasks). */
	public static final int BOUNTY_LEVEL = 30;

	/** What a task is, from the game's port task table; null when the game cannot say. */
	public interface Catalog
	{
		PortTask lookup(int id);
	}

	int version = VERSION;
	long savedAt;
	int sailing;
	int completed = -1;
	List<PortTask> tasks = new ArrayList<>();
	Map<String, Integer> xp = new LinkedHashMap<>();

	public static PortLog empty()
	{
		return new PortLog();
	}

	public static PortLog parse(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return empty();
		}
		try
		{
			PortLog log = gson.fromJson(json, PortLog.class);
			if (log == null)
			{
				return empty();
			}
			List<PortTask> ok = new ArrayList<>();
			if (log.tasks != null)
			{
				for (PortTask t : log.tasks)
				{
					if (t != null && t.usable() && t.slot >= 0 && t.slot < SLOTS)
					{
						ok.add(t);
					}
				}
			}
			log.tasks = ok;
			if (log.xp == null)
			{
				log.xp = new LinkedHashMap<>();
			}
			log.xp.values().removeIf(v -> v == null || v <= 0);
			return log;
		}
		catch (JsonParseException | IllegalStateException | ClassCastException | NumberFormatException e)
		{
			return empty();
		}
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	/**
	 * The tasks in the account's slots now. What each is comes from the catalog (the game's own
	 * table), or, when it cannot say, from this log's save of the same task; a task neither knows is
	 * left out rather than guessed.
	 */
	public List<PortTask> read(Vars vars, Catalog catalog)
	{
		List<PortTask> out = new ArrayList<>();
		for (int slot = 0; slot < SLOTS; slot++)
		{
			int id = vars.varbit(SLOT_ID[slot]);
			if (id <= 0)
			{
				continue;
			}
			PortTask what = catalog == null ? null : safeLookup(catalog, id);
			if (what == null || !what.usable())
			{
				what = saved(id);
			}
			if (what == null)
			{
				continue;
			}
			out.add(what.inSlot(slot, vars.varbit(SLOT_TAKEN[slot]), vars.varbit(SLOT_DELIVERED[slot]), vars.varbit(SLOT_BOUNTY[slot])));
		}
		return out;
	}

	private static PortTask safeLookup(Catalog catalog, int id)
	{
		try
		{
			return catalog.lookup(id);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private PortTask saved(int id)
	{
		for (PortTask t : tasks)
		{
			if (t.id == id)
			{
				return t;
			}
		}
		return null;
	}

	/**
	 * Copies each slot's progress varbits (crates taken, crates delivered, parts still wanted) into
	 * {@code into} from index {@code at}, so a change in any of them is noticed the tick it lands.
	 */
	public static void progress(java.util.function.IntUnaryOperator varbit, int[] into, int at)
	{
		for (int i = 0; i < SLOTS; i++)
		{
			into[at + i * 3] = varbit.applyAsInt(SLOT_TAKEN[i]);
			into[at + i * 3 + 1] = varbit.applyAsInt(SLOT_DELIVERED[i]);
			into[at + i * 3 + 2] = varbit.applyAsInt(SLOT_BOUNTY[i]);
		}
	}

	/** Takes in what the game shows now; true when anything worth saving changed. */
	public boolean update(List<PortTask> now, int completed, int sailing, long nowMs)
	{
		List<PortTask> sorted = new ArrayList<>(now == null ? Collections.emptyList() : now);
		sorted.sort(Comparator.comparingInt(PortTask::getSlot));
		boolean changed = !sorted.equals(tasks) || completed != this.completed || sailing > 0 && sailing != this.sailing;
		if (changed)
		{
			tasks = sorted;
			this.completed = completed;
			if (sailing > 0)
			{
				this.sailing = sailing;
			}
			savedAt = nowMs;
		}
		return changed;
	}

	/** Notes that a task paid this much Sailing xp when it was handed in; true when it is news. */
	public boolean paid(int id, int amount)
	{
		if (id <= 0 || amount <= 0)
		{
			return false;
		}
		Integer old = xp.put(String.valueOf(id), amount);
		return old == null || old != amount;
	}

	/** The Sailing xp this task paid the last time this account handed it in, or 0 when it has not. */
	public int xp(int id)
	{
		Integer v = xp.get(String.valueOf(id));
		return v == null ? 0 : v;
	}

	public List<PortTask> getTasks()
	{
		return Collections.unmodifiableList(tasks);
	}

	/** Port tasks done, all time, or -1 when not read yet. */
	public int getCompleted()
	{
		return completed;
	}

	public int getSailing()
	{
		return sailing;
	}

	public long getSavedAt()
	{
		return savedAt;
	}

	/** A copy holding these tasks, safe to hand to the panel while the log itself moves on. */
	public PortLog with(List<PortTask> now)
	{
		PortLog l = new PortLog();
		l.savedAt = savedAt;
		l.sailing = sailing;
		l.completed = completed;
		l.tasks = new ArrayList<>(now);
		l.tasks.sort(Comparator.comparingInt(PortTask::getSlot));
		l.xp = new LinkedHashMap<>(xp);
		return l;
	}

	/** Whether the account does port tasks at all: one is held or some are done. */
	public boolean doesPortTasks()
	{
		return !tasks.isEmpty() || completed > 0;
	}

	/** Tasks the account can hold at its Sailing level, or 0 when the level is not known. */
	public static int slots(int sailing)
	{
		int n = 0;
		for (int at : SLOT_LEVELS)
		{
			if (sailing >= at)
			{
				n++;
			}
		}
		return n;
	}

	/** The Sailing level at which the next slot opens, or 0 when every slot is open. */
	public static int nextSlotLevel(int sailing)
	{
		for (int at : SLOT_LEVELS)
		{
			if (sailing < at)
			{
				return at;
			}
		}
		return 0;
	}
}
