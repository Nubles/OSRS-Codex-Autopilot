package com.codex.autopilot.core;

import java.util.Objects;

/**
 * One port task in one of the account's task slots, as the game describes it.
 *
 * <p>What the task is (its name, level, ports, cargo or bounty) comes from the game's own port task
 * table ({@code DBTableID.PortTask}) and its docks' names from the docking point table
 * ({@code DBTableID.SailingDock}); how far along it is comes from the slot's varbits
 * ({@code PORT_TASK_SLOT_n_CARGO_TAKEN}, {@code _CARGO_DELIVERED}, {@code BOUNTY_TASK_ITEM_COUNTER_n}).
 * Nothing about a task is written into Autopilot, so a task Jagex adds or changes is read as it is.</p>
 *
 * <p>Plain fields, so Gson writes the account's save as they are.</p>
 */
public final class PortTask
{
	/** Courier tasks carry crates between two ports; bounty tasks want parts from a sea creature. */
	public enum Type
	{
		COURIER,
		BOUNTY
	}

	/** The task slot, 0 to 4. */
	int slot;
	/** The task's id, as the slot's {@code PORT_TASK_SLOT_n_ID} varbit holds it. */
	int id;
	String name;
	Type type;
	int level;
	/** The notice board the task was taken from: its docking point row and name. */
	int boardDock;
	String board;
	/** Courier: where the crates are collected from the ledger table. */
	int fromDock;
	String from;
	/** Courier: where they are delivered. */
	int toDock;
	String to;
	/** Courier: the cargo item and how many crates. */
	int cargoItem = -1;
	String cargo;
	int amount;
	int taken;
	int delivered;
	/** Bounty: the creature, the part it drops and how many are wanted. */
	String monster;
	int itemId = -1;
	String item;
	int need;
	int have;

	public PortTask()
	{
	}

	public static PortTask courier(int id, String name, int level, int boardDock, String board, int fromDock, String from,
		int toDock, String to, int cargoItem, String cargo, int amount)
	{
		PortTask t = new PortTask();
		t.id = id;
		t.name = name;
		t.type = Type.COURIER;
		t.level = level;
		t.boardDock = boardDock;
		t.board = board;
		t.fromDock = fromDock;
		t.from = from;
		t.toDock = toDock;
		t.to = to;
		t.cargoItem = cargoItem;
		t.cargo = cargo;
		t.amount = Math.max(1, amount);
		return t;
	}

	public static PortTask bounty(int id, String name, int level, int boardDock, String board, String monster, int itemId,
		String item, int need)
	{
		PortTask t = new PortTask();
		t.id = id;
		t.name = name;
		t.type = Type.BOUNTY;
		t.level = level;
		t.boardDock = boardDock;
		t.board = board;
		t.monster = monster;
		t.itemId = itemId;
		t.item = item;
		t.need = Math.max(1, need);
		return t;
	}

	/**
	 * This task in a slot, with how far along it is: crates taken and delivered for a courier task,
	 * and for a bounty the parts still wanted, as the slot's counter holds them.
	 */
	public PortTask inSlot(int slot, int taken, int delivered, int stillWanted)
	{
		PortTask t = copy();
		t.slot = slot;
		if (type == Type.COURIER)
		{
			t.taken = clamp(taken, amount);
			t.delivered = clamp(delivered, amount);
			t.have = 0;
		}
		else
		{
			t.have = need - clamp(stillWanted, need);
		}
		return t;
	}

	private static int clamp(int v, int max)
	{
		return Math.max(0, Math.min(max, v));
	}

	PortTask copy()
	{
		PortTask t = new PortTask();
		t.slot = slot;
		t.id = id;
		t.name = name;
		t.type = type;
		t.level = level;
		t.boardDock = boardDock;
		t.board = board;
		t.fromDock = fromDock;
		t.from = from;
		t.toDock = toDock;
		t.to = to;
		t.cargoItem = cargoItem;
		t.cargo = cargo;
		t.amount = amount;
		t.taken = taken;
		t.delivered = delivered;
		t.monster = monster;
		t.itemId = itemId;
		t.item = item;
		t.need = need;
		t.have = have;
		return t;
	}

	/** Whether a save gave enough to plan with. */
	boolean usable()
	{
		if (type == null || id <= 0)
		{
			return false;
		}
		return type == Type.COURIER ? from != null && to != null && amount > 0 : need > 0;
	}

	public int getSlot()
	{
		return slot;
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public Type getType()
	{
		return type;
	}

	public boolean isCourier()
	{
		return type == Type.COURIER;
	}

	public int getLevel()
	{
		return level;
	}

	public int getBoardDock()
	{
		return boardDock;
	}

	public String getBoard()
	{
		return board;
	}

	public int getFromDock()
	{
		return fromDock;
	}

	public String getFrom()
	{
		return from;
	}

	public int getToDock()
	{
		return toDock;
	}

	public String getTo()
	{
		return to;
	}

	public int getCargoItem()
	{
		return cargoItem;
	}

	/** The cargo in a few words, such as "Dye", or "cargo" when the game gave no name. */
	public String getCargo()
	{
		return cargo == null || cargo.isEmpty() ? "cargo" : cargo;
	}

	public int getAmount()
	{
		return amount;
	}

	public int getTaken()
	{
		return taken;
	}

	public int getDelivered()
	{
		return delivered;
	}

	public String getMonster()
	{
		return monster;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getItem()
	{
		return item == null || item.isEmpty() ? "parts" : item;
	}

	public int getNeed()
	{
		return need;
	}

	public int getHave()
	{
		return have;
	}

	/** Courier: crates still at the ledger table to collect. */
	public int toCollect()
	{
		return isCourier() ? amount - taken : 0;
	}

	/** Courier: crates taken but not yet delivered, so aboard (or in hand). */
	public int aboard()
	{
		return isCourier() ? Math.max(0, taken - delivered) : 0;
	}

	/** A bounty with every part it wants, ready to claim. */
	public boolean isBountyDone()
	{
		return !isCourier() && have >= need;
	}

	/** Where the task's part is: courier progress as crates delivered, bounty as parts in hand. */
	public double progress()
	{
		if (isCourier())
		{
			return (taken + delivered) / (2.0 * amount);
		}
		return need <= 0 ? 0 : Math.min(1, have / (double) need);
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof PortTask))
		{
			return false;
		}
		PortTask t = (PortTask) o;
		return slot == t.slot && id == t.id && taken == t.taken && delivered == t.delivered && have == t.have
			&& amount == t.amount && need == t.need && type == t.type && Objects.equals(name, t.name)
			&& Objects.equals(from, t.from) && Objects.equals(to, t.to) && Objects.equals(cargo, t.cargo)
			&& Objects.equals(monster, t.monster) && Objects.equals(item, t.item);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(slot, id, taken, delivered, have);
	}

	@Override
	public String toString()
	{
		return isCourier()
			? "#" + slot + " " + name + " " + from + "->" + to + " " + taken + "/" + delivered + "/" + amount
			: "#" + slot + " " + name + " " + monster + " " + have + "/" + need;
	}
}
