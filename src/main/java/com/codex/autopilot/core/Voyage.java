package com.codex.autopilot.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The courier tasks in hand as one voyage: the fewest port stops that collect every crate still at a
 * ledger table and deliver every crate, with each crate collected before it is delivered.
 *
 * <p>A stop does everything it can at its port: deliver what is aboard for it and collect what waits
 * there, so two tasks bound for the same port share a stop, and a task whose cargo waits where another
 * is delivered is picked up on the same call ("Aldarin, then Deepfin Point, then Rellekka" rather than
 * two voyages to Rellekka). Autopilot does not know the sea's distances, so it counts stops rather than
 * miles; among routes with as few stops, the one that does most at its first stop wins, then the one
 * that unloads most there (crates aboard are lost if the boat is), then the one whose ports come first by name, so the plan holds still while nothing changes.</p>
 */
public final class Voyage
{
	/** What is done at one port. */
	public static final class Stop
	{
		private final int dock;
		private final String port;
		private final List<PortTask> collect;
		private final List<PortTask> deliver;

		Stop(int dock, String port, List<PortTask> collect, List<PortTask> deliver)
		{
			this.dock = dock;
			this.port = port;
			this.collect = Collections.unmodifiableList(collect);
			this.deliver = Collections.unmodifiableList(deliver);
		}

		public int getDock()
		{
			return dock;
		}

		public String getPort()
		{
			return port;
		}

		/** Tasks whose crates are collected here. */
		public List<PortTask> getCollect()
		{
			return collect;
		}

		/** Tasks whose crates are delivered here. */
		public List<PortTask> getDeliver()
		{
			return deliver;
		}

		/** "Deliver dye, collect 2 crates of logs": what to do here, deliveries first, as the ledger table takes them. */
		public String actions()
		{
			List<String> out = new ArrayList<>();
			if (!deliver.isEmpty())
			{
				out.add("deliver " + cargoes(deliver, false));
			}
			if (!collect.isEmpty())
			{
				out.add("collect " + cargoes(collect, true));
			}
			String s = String.join(", ", out);
			return Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}

		static String cargoes(List<PortTask> tasks, boolean collecting)
		{
			List<String> names = new ArrayList<>();
			for (PortTask t : tasks)
			{
				int crates = collecting ? t.toCollect() : t.getAmount() - t.getDelivered();
				String what = t.getCargo().toLowerCase(java.util.Locale.ROOT);
				names.add(crates > 1 ? crates + " crates of " + what : what);
			}
			return Format.list(names);
		}

		@Override
		public String toString()
		{
			return port + ": " + actions();
		}
	}

	public static final Voyage NONE = new Voyage(Collections.emptyList(), Collections.emptyList());
	/** More courier tasks than slots exist is a damaged reading; it is not planned. */
	static final int MAX_TASKS = 5;
	/** The deliver events' bits: every odd bit. */
	private static final int DELIVERIES = 0xAAAAAAAA;

	private final List<Stop> stops;
	private final List<PortTask> tasks;

	private Voyage(List<Stop> stops, List<PortTask> tasks)
	{
		this.stops = Collections.unmodifiableList(stops);
		this.tasks = Collections.unmodifiableList(tasks);
	}

	public List<Stop> getStops()
	{
		return stops;
	}

	/** The courier tasks the voyage carries. */
	public List<PortTask> getTasks()
	{
		return tasks;
	}

	public boolean isEmpty()
	{
		return stops.isEmpty();
	}

	/** Crates taken but not yet delivered: lost if the player dies, capsizes or teleports off the boat at sea. */
	public int aboard()
	{
		int n = 0;
		for (PortTask t : tasks)
		{
			n += t.aboard();
		}
		return n;
	}

	/** "Aldarin → Deepfin Point → Rellekka". */
	public String route()
	{
		List<String> ports = new ArrayList<>();
		for (Stop s : stops)
		{
			ports.add(s.getPort());
		}
		return String.join(" → ", ports);
	}

	/** Plans the voyage for the courier tasks in hand; bounties and finished tasks are left out. */
	public static Voyage plan(List<PortTask> all)
	{
		List<PortTask> tasks = new ArrayList<>();
		if (all != null)
		{
			for (PortTask t : all)
			{
				if (t != null && t.isCourier() && t.getDelivered() < t.getAmount() && t.getFrom() != null && t.getTo() != null)
				{
					tasks.add(t);
				}
			}
		}
		if (tasks.isEmpty() || tasks.size() > MAX_TASKS)
		{
			return NONE;
		}
		tasks.sort(Comparator.comparingInt(PortTask::getSlot));

		// Two events a task: collect (bit 2i), deliver (bit 2i+1). Crates already all taken start collected.
		int n = tasks.size();
		int start = 0;
		for (int i = 0; i < n; i++)
		{
			if (tasks.get(i).toCollect() == 0)
			{
				start |= 1 << (2 * i);
			}
		}
		int goal = (1 << (2 * n)) - 1;

		// The ports in play, in name order, so ties break the same way every time.
		Map<String, Integer> docks = new LinkedHashMap<>();
		List<String> ports = new ArrayList<>();
		for (PortTask t : tasks)
		{
			docks.putIfAbsent(t.getFrom(), t.getFromDock());
			docks.putIfAbsent(t.getTo(), t.getToDock());
		}
		ports.addAll(docks.keySet());
		ports.sort(Comparator.naturalOrder());

		// Breadth first over what is done: the first route to finish has the fewest stops.
		Map<Integer, int[]> came = new HashMap<>();
		Deque<Integer> queue = new ArrayDeque<>();
		came.put(start, null);
		queue.add(start);
		while (!queue.isEmpty())
		{
			int done = queue.poll();
			if (done == goal)
			{
				break;
			}
			List<int[]> moves = new ArrayList<>();
			for (int p = 0; p < ports.size(); p++)
			{
				int after = visit(tasks, ports.get(p), done);
				if (after != done)
				{
					int gained = after & ~done;
					moves.add(new int[]{p, after, Integer.bitCount(gained), Integer.bitCount(gained & DELIVERIES)});
				}
			}
			// The port that does most first, then the one that unloads most (crates aboard are at risk), then by name.
			moves.sort((a, b) -> a[2] != b[2] ? b[2] - a[2] : a[3] != b[3] ? b[3] - a[3] : a[0] - b[0]);
			for (int[] m : moves)
			{
				if (!came.containsKey(m[1]))
				{
					came.put(m[1], new int[]{done, m[0]});
					queue.add(m[1]);
				}
			}
		}
		if (!came.containsKey(goal))
		{
			return NONE;
		}
		List<Stop> stops = new ArrayList<>();
		for (int at = goal; came.get(at) != null; at = came.get(at)[0])
		{
			int before = came.get(at)[0];
			String port = ports.get(came.get(at)[1]);
			List<PortTask> collect = new ArrayList<>();
			List<PortTask> deliver = new ArrayList<>();
			for (int i = 0; i < n; i++)
			{
				if ((at & ~before & (1 << (2 * i))) != 0)
				{
					collect.add(tasks.get(i));
				}
				if ((at & ~before & (1 << (2 * i + 1))) != 0)
				{
					deliver.add(tasks.get(i));
				}
			}
			stops.add(0, new Stop(docks.get(port), port, collect, deliver));
		}
		return new Voyage(stops, tasks);
	}

	/** Everything a stop at this port does: deliver what is aboard for it, then collect what waits there. */
	private static int visit(List<PortTask> tasks, String port, int done)
	{
		int after = done;
		for (int i = 0; i < tasks.size(); i++)
		{
			PortTask t = tasks.get(i);
			int collect = 1 << (2 * i);
			int deliver = 1 << (2 * i + 1);
			if ((done & collect) != 0 && (done & deliver) == 0 && port.equals(t.getTo()))
			{
				after |= deliver;
			}
		}
		for (int i = 0; i < tasks.size(); i++)
		{
			int collect = 1 << (2 * i);
			if ((done & collect) == 0 && port.equals(tasks.get(i).getFrom()))
			{
				after |= collect;
			}
		}
		return after;
	}
}
