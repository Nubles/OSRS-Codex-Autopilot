package com.codex.autopilot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.DBTableID.SailingDock;

/**
 * The account's Sailing port tasks as steps. A port task never expires, so the voyage ranks with
 * the goals, except while crates are aboard: those are lost if the player dies, capsizes or teleports
 * off the boat at sea (OSRS wiki, Port tasks), so finishing the voyage becomes a ready errand. A
 * bounty whose parts are all in hand is a ready errand too, since any port master pays it.
 *
 * <ul>
 * <li>"Port tasks: Collect dye at Prifddinas", with the whole voyage ("Prifddinas → Civitas illa
 * Fortis → Aldarin: 2 tasks, 3 stops") and the xp the tasks paid this account before;</li>
 * <li>"Bounty: Albatross beak, 2 of 5", or "Claim the albatross beak bounty";</li>
 * <li>"Take 2 more port tasks", for an account that does them, has open slots and no voyage under way;
 * with a voyage, the open slots are a note on it, at its last port when that has a notice board.</li>
 * </ul>
 */
public final class PortTasks
{
	/** A stop's time before anything is learned: an estimate, and shown as one. */
	public static final int STOP_MINUTES = 5;
	static final int CLAIM_MINUTES = 2;
	static final int HUNT_MINUTES = 15;
	static final int TAKE_MINUTES = 2;
	static final double VOYAGE_URGENCY = 0.15;
	static final double ABOARD_URGENCY = 0.4;
	static final double CLAIM_URGENCY = 0.4;
	static final double HUNT_URGENCY = 0.12;
	/** Low enough that a task in hand comes before taking more. */
	static final double TAKE_URGENCY = 0.01;
	/** The steps' keys. */
	public static final String VOYAGE = "port:voyage";
	public static final String BOUNTY = "port:bounty:";
	public static final String TAKE = "port:take";
	public static final String OVER = "Port tasks";

	/**
	 * Docking points with a notice board, by their row in the game's docking point table (OSRS
	 * wiki, Mooring point: the notice board column).
	 */
	static final Set<Integer> NOTICE_BOARDS = new HashSet<>(Arrays.asList(
		SailingDock.Row.SAILING_DOCK_PORT_SARIM,
		SailingDock.Row.SAILING_DOCK_THE_PANDEMONIUM,
		SailingDock.Row.SAILING_DOCK_LANDS_END,
		SailingDock.Row.SAILING_DOCK_MUSA_POINT,
		SailingDock.Row.SAILING_DOCK_PORT_PISCARILIUS,
		SailingDock.Row.SAILING_DOCK_CATHERBY,
		SailingDock.Row.SAILING_DOCK_BRIMHAVEN,
		SailingDock.Row.SAILING_DOCK_ARDOUGNE,
		SailingDock.Row.SAILING_DOCK_PORT_KHAZARD,
		SailingDock.Row.SAILING_DOCK_CIVITAS_ILLA_FORTIS,
		SailingDock.Row.SAILING_DOCK_CORSAIR_COVE,
		SailingDock.Row.SAILING_DOCK_THE_SUMMER_SHORE,
		SailingDock.Row.SAILING_DOCK_ALDARIN,
		SailingDock.Row.SAILING_DOCK_RUINS_OF_UNKAH,
		SailingDock.Row.SAILING_DOCK_VOID_KNIGHTS_OUTPOST,
		SailingDock.Row.SAILING_DOCK_PORT_ROBERTS,
		SailingDock.Row.SAILING_DOCK_RED_ROCK,
		SailingDock.Row.SAILING_DOCK_RELLEKKA,
		SailingDock.Row.SAILING_DOCK_ETCETERIA,
		SailingDock.Row.SAILING_DOCK_PORT_TYRAS,
		SailingDock.Row.SAILING_DOCK_DEEPFIN_POINT,
		SailingDock.Row.SAILING_DOCK_PRIFDDINAS,
		SailingDock.Row.SAILING_DOCK_LUNAR_ISLE));

	private PortTasks()
	{
	}

	/** Every port task step the log calls for. */
	public static List<Candidate> candidates(PortLog log)
	{
		if (log == null)
		{
			return Collections.emptyList();
		}
		List<Candidate> out = new ArrayList<>();
		Voyage voyage = Voyage.plan(log.getTasks());
		if (!voyage.isEmpty())
		{
			out.add(voyage(voyage, log));
		}
		for (PortTask t : log.getTasks())
		{
			if (!t.isCourier())
			{
				out.add(bounty(t, log));
			}
		}
		// With a voyage under way, more tasks are a note on it rather than a step that would jump ahead of it.
		Candidate take = voyage.isEmpty() ? take(log) : null;
		if (take != null)
		{
			out.add(take);
		}
		return out;
	}

	static Candidate voyage(Voyage v, PortLog log)
	{
		Voyage.Stop first = v.getStops().get(0);
		String title = first.actions() + " at " + first.getPort();
		int n = v.getTasks().size();
		int stops = v.getStops().size();
		List<String> why = new ArrayList<>();
		if (stops > 1)
		{
			why.add(v.route() + ": " + Format.plural(n, "task", "tasks") + ", " + Format.plural(stops, "stop", "stops"));
		}
		else
		{
			why.add(n == 1 ? "One stop finishes it" : "One stop finishes " + n + " tasks");
		}
		String paid = paid(v.getTasks(), log);
		if (paid != null)
		{
			why.add(paid);
		}
		String more = more(log, v);
		if (more != null)
		{
			why.add(more);
		}
		int aboard = v.aboard();
		Tier tier = Tier.GOAL;
		double urgency = VOYAGE_URGENCY;
		if (aboard > 0)
		{
			why.add(0, Format.plural(aboard, "crate", "crates") + " aboard, lost if you die, capsize or teleport off at sea");
			tier = Tier.READY;
			urgency = ABOARD_URGENCY;
		}
		PortTask lead = !first.getDeliver().isEmpty() ? first.getDeliver().get(0) : first.getCollect().get(0);
		return new Candidate(VOYAGE, Candidate.Kind.PORT_TASK, tier, OVER, title, String.join("; ", why), null,
			stops * STOP_MINUTES, urgency, 0, n, lead.getCargoItem());
	}

	/** "Paid 11,238 Sailing xp last time", only when every task on the voyage has paid this account before. */
	static String paid(List<PortTask> tasks, PortLog log)
	{
		long sum = 0;
		for (PortTask t : tasks)
		{
			int xp = log.xp(t.getId());
			if (xp <= 0)
			{
				return null;
			}
			sum += xp;
		}
		return sum <= 0 ? null : (tasks.size() == 1 ? "it paid you " : "they paid you ") + Format.number(sum) + " Sailing xp last time";
	}

	static Candidate bounty(PortTask t, PortLog log)
	{
		String monster = t.getMonster() == null ? "the creature" : t.getMonster();
		String item = t.getItem().toLowerCase(java.util.Locale.ROOT);
		int xp = log.xp(t.getId());
		String pays = xp > 0 ? "; it paid you " + Format.number(xp) + " Sailing xp last time" : "";
		if (t.isBountyDone())
		{
			return new Candidate(BOUNTY + t.getSlot(), Candidate.Kind.PORT_TASK, Tier.READY, "Bounty",
				"Claim the " + item + " bounty",
				"Any port master pays it; parts in the hold are lost if the boat is recovered or teleported" + pays, null,
				CLAIM_MINUTES, CLAIM_URGENCY, 0, 1, t.getItemId());
		}
		String title = t.getItem() + ", " + t.getHave() + " of " + t.getNeed();
		String why = "Hunt " + monster.toLowerCase(java.util.Locale.ROOT) + " at sea until " + t.getNeed() + " drop, then any port master pays"
			+ pays;
		return new Candidate(BOUNTY + t.getSlot(), Candidate.Kind.PORT_TASK, Tier.GOAL, "Bounty", title, why, null,
			HUNT_MINUTES, HUNT_URGENCY, 0, 1, t.getItemId());
	}

	/** Slots the account could fill now, or 0. */
	static int open(PortLog log)
	{
		int slots = PortLog.slots(log.getSailing());
		return slots == 0 || !log.doesPortTasks() ? 0 : Math.max(0, slots - log.getTasks().size());
	}

	/** "then take 2 more at The Pandemonium's notice board", when the voyage ends at one and slots are open. */
	static String more(PortLog log, Voyage v)
	{
		int open = open(log);
		Voyage.Stop last = v.getStops().get(v.getStops().size() - 1);
		if (open <= 0 || !NOTICE_BOARDS.contains(last.getDock()))
		{
			return null;
		}
		return (open == 1 ? "then take 1 more task" : "then take " + open + " more tasks") + " at " + last.getPort()
			+ "'s notice board, where it ends";
	}

	/** Open slots on an account that does port tasks and has no voyage under way. */
	static Candidate take(PortLog log)
	{
		int slots = PortLog.slots(log.getSailing());
		int open = open(log);
		if (open <= 0)
		{
			return null;
		}
		String title = open == 1 ? "Take another port task" : "Take " + open + " more port tasks";
		String why = log.getTasks().isEmpty()
			? "From any port's notice board: " + log.getSailing() + " Sailing holds " + slots
			: "From any port's notice board; " + log.getTasks().size() + " of " + slots + " slots in use";
		return new Candidate(TAKE, Candidate.Kind.PORT_TASK, Tier.GOAL, OVER, title, why, null,
			TAKE_MINUTES, TAKE_URGENCY, 0, open, -1);
	}
}
