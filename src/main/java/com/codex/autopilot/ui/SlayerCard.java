package com.codex.autopilot.ui;

import com.codex.autopilot.core.Format;
import com.codex.autopilot.core.SkipPlan;
import com.codex.autopilot.core.Slayer;
import com.codex.autopilot.core.SlayerLog;
import com.codex.autopilot.core.Slayers;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The Slayer task at a glance: the monster and its master, a tally of how many are left, where
 * they live and what the fight needs (for the tasks whose facts are checked), the time left at the
 * player's own pace, and the points and streak with which master to ask next. Below it the
 * streak on a track to its next big milestone, what the next task pays with each master open, the
 * player's own points an hour from their finished tasks, and, with the skip plan on, what to do
 * and how skipping compares with full tasks at the player's own times.
 */
public final class SlayerCard
{
	private static final int LABEL = 44;

	private SlayerCard()
	{
	}

	/** "87 left", or "No task". */
	public static String summary(SlayerLog log)
	{
		return log.hasTask() ? Format.number(log.getAmount()) + " left" : "No task";
	}

	public static JComponent card(SlayerLog log, int width)
	{
		Slayer.Account a = log.getAccount();
		Slayer.Master m = log.getMaster();
		Card card = new Card(new BorderLayout()).accent(log.hasTask() ? Theme.GOLD : Theme.GREEN);
		StackPanel body = new StackPanel(5, 0);
		if (log.hasTask())
		{
			String name = log.getName() == null ? "Your task" : log.getName();
			body.add(new Heading(name, m == null ? null : "for " + m.nameFor(a)));
			body.add(new Tally(log.getAmount(), log.getInitial()));
			Slayer.Task facts = Slayer.Task.of(log.getRow());
			String where = Slayers.where(log, facts);
			if (where != null)
			{
				body.add(field("Where", where, Theme.TEXT, width));
			}
			if (facts != null)
			{
				body.add(field("Bring", facts.getBring(), Theme.TEXT, width));
			}
			body.add(field("Pace", pace(log), log.minutesLeft() > 0 ? Theme.TEAL : Theme.TEXT_MUTED, width));
		}
		else
		{
			String last = log.getLastTask();
			body.add(new Heading("Between tasks", last == null ? null : "last: " + last));
		}
		SkipPlan plan = log.getPlan();
		SkipPlan.Advice advice = plan.advise(log);
		Slayer.Master next = Slayers.next(log);
		if (advice != null)
		{
			body.add(field("Plan", advice.text, advice.master == null ? Theme.TEXT_MUTED : Theme.GREEN, width));
		}
		else if (next != null)
		{
			String pts = Slayers.points(log, next);
			body.add(field("Next", next.nameFor(a) + (pts == null ? "" : ": " + pts), log.hasTask() ? Theme.TEXT_MUTED : Theme.GREEN,
				width));
		}
		boolean standard = m != Slayer.Master.KRYSTILIA && m != Slayer.Master.MORTIMER;
		if (standard && log.getStreak() >= 0)
		{
			body.add(new StreakTrack(log.getStreak(), plan));
			List<SkipPlan.Pays> pays = SkipPlan.pays(log);
			if (!pays.isEmpty())
			{
				body.add(new PaysChart(log.getStreak() + 1, pays, advice == null ? null : plan.milestoneMaster(a, log.getStreak() + 1)));
			}
		}
		String own = own(log);
		if (own != null)
		{
			body.add(field("Yours", own, log.pointsPerHour() > 0 ? Theme.TEAL : Theme.TEXT_MUTED, width));
		}
		boolean quickHeld = !log.hasTask() || m == Slayer.Master.TURAEL || m == Slayer.Master.SPRIA;
		SkipPlan.Compare cmp = plan.isOn() && quickHeld ? plan.compare(log) : null;
		if (cmp != null)
		{
			body.add(field("Skip", compare(cmp, a), cmp.skipPerHour() >= cmp.fullPerHour() ? Theme.GREEN : Theme.EMBER, width));
		}
		String tally = standing(log, m);
		if (tally != null)
		{
			body.add(Ui.wrapped(tally, Theme.small(), Theme.TEXT_FAINT, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** "~47 min left at your 112 kills/hr", or how far the learning is. */
	static String pace(SlayerLog log)
	{
		int left = log.minutesLeft();
		if (left > 0)
		{
			return "~" + Format.duration(left * 60L) + " left at your " + Format.number(Math.round(log.killsPerHour())) + " kills/hr";
		}
		return "Your own pace shows after " + SlayerLog.TRUST_KILLS + " kills (" + log.timed() + " timed)";
	}

	/**
	 * "41 points/hr over your last 12 tasks; Turael 6 min a task, Konar 34", or how far the
	 * learning is; null for an account with no finished task seen yet.
	 */
	static String own(SlayerLog log)
	{
		int n = log.timedTasks();
		if (n == 0)
		{
			return null;
		}
		double ph = log.pointsPerHour();
		if (ph <= 0)
		{
			return "Your points an hour shows after " + SlayerLog.TRUST_TASKS + " finished tasks (" + n + " so far)";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(Format.number(Math.round(ph))).append(" points/hr over your last ").append(Format.plural(n, "task", "tasks"));
		Slayer.Account a = log.getAccount();
		List<String> per = new ArrayList<>();
		double quick = log.minutesPerQuickTask();
		if (quick > 0)
		{
			per.add(Slayer.Master.TURAEL.nameFor(a) + " " + Math.round(quick) + " min");
		}
		for (Slayer.Master m : Slayer.Master.values())
		{
			if (m == Slayer.Master.TURAEL || m == Slayer.Master.SPRIA)
			{
				continue;
			}
			double mins = log.minutesPerTask(m);
			if (mins > 0)
			{
				per.add(m.nameFor(a) + " " + Math.round(mins) + " min");
			}
		}
		if (!per.isEmpty())
		{
			sb.append("; a task: ").append(String.join(", ", per));
		}
		return sb.toString();
	}

	/** "Next 3 tasks: skipping 90 pts in 0.4h (225/hr); all Konar 126 in 1.7h (74/hr)". */
	static String compare(SkipPlan.Compare c, Slayer.Account a)
	{
		return "Next " + Format.plural(c.tasks, "task", "tasks") + ": skipping " + Format.number(c.skipPoints) + " pts in "
			+ hours(c.skipHours) + " (" + Format.number(Math.round(c.skipPerHour())) + "/hr); all " + c.master.nameFor(a) + " "
			+ Format.number(c.fullPoints) + " in " + hours(c.fullHours) + " (" + Format.number(Math.round(c.fullPerHour())) + "/hr)";
	}

	private static String hours(double h)
	{
		return Format.duration(Math.round(h * 3600));
	}

	/** "345 points · 9 tasks in a row". */
	static String standing(SlayerLog log, Slayer.Master m)
	{
		int streak = log.streakFor(m);
		if (log.getPoints() < 0 && streak < 0)
		{
			return null;
		}
		StringBuilder sb = new StringBuilder();
		if (log.getPoints() >= 0)
		{
			sb.append(Format.plural(log.getPoints(), "point", "points"));
		}
		if (streak >= 0)
		{
			if (sb.length() > 0)
			{
				sb.append(" · ");
			}
			sb.append(Format.plural(streak, "task", "tasks")).append(" in a row");
			if (m == Slayer.Master.KRYSTILIA)
			{
				sb.append(" (Wilderness)");
			}
		}
		return sb.toString();
	}

	private static JComponent field(String label, String text, Color color, int width)
	{
		JPanel row = new JPanel(new BorderLayout(0, 0));
		row.setOpaque(false);
		JComponent l = new Label(label);
		row.add(l, BorderLayout.WEST);
		row.add(Ui.wrapped(text, Theme.small(), color, width - LABEL), BorderLayout.CENTER);
		return row;
	}

	/** A field's name in small gold caps. */
	private static final class Label extends JComponent
	{
		private final String text;

		Label(String text)
		{
			this.text = text.toUpperCase();
			setPreferredSize(new Dimension(LABEL, 14));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(Theme.small());
			Ui.shadowed(g, text, 0, 11, Theme.GOLD_DIM);
			g.dispose();
		}
	}

	/** The monster's name with its master at the right. */
	private static final class Heading extends JComponent
	{
		private final String text;
		private final String right;

		Heading(String text, String right)
		{
			this.text = text;
			this.right = right;
			setPreferredSize(new Dimension(10, 16));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			int rw = right == null ? 0 : fm.stringWidth(right) + 6;
			if (right != null)
			{
				Ui.shadowed(g, right, getWidth() - rw + 6, 13, Theme.TEXT_MUTED);
			}
			g.setFont(Theme.body());
			Ui.shadowed(g, Ui.clip(text, g.getFontMetrics(), getWidth() - rw), 0, 13, Theme.GOLD_BRIGHT);
			g.dispose();
		}
	}

	/** "87 of 150 left" over a bar of how many are slain. */
	private static final class Tally extends JComponent
	{
		private final int left;
		private final int initial;

		Tally(int left, int initial)
		{
			this.left = left;
			this.initial = Math.max(left, initial);
			setPreferredSize(new Dimension(10, 24));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			double done = initial <= 0 ? 0 : 1 - (double) left / initial;
			Color c = Theme.progressColor(done);
			g.setFont(Theme.small());
			String text = initial > left ? Format.number(left) + " of " + Format.number(initial) + " left" : Format.number(left) + " left";
			Ui.shadowed(g, text, 0, 10, Theme.TEXT);
			String pct = Math.round(done * 100) + "% slain";
			int pw = g.getFontMetrics().stringWidth(pct);
			Ui.shadowed(g, pct, getWidth() - pw, 10, Theme.TEXT_MUTED);
			int y = 16;
			int w = getWidth() - 1;
			g.setColor(Theme.INK_DEEP);
			g.fill(new RoundRectangle2D.Float(0, y, w, 6, 6, 6));
			if (done > 0)
			{
				g.setPaint(new GradientPaint(0, y, Theme.mix(c, Color.WHITE, 0.2), 0, y + 6, Theme.mix(c, Theme.INK_DEEP, 0.3)));
				g.fill(new RoundRectangle2D.Float(0, y, Math.max(6, (float) (w * done)), 6, 6, 6));
			}
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(0, y, w, 6, 6, 6));
			g.dispose();
		}
	}

	/**
	 * The streak on a track from the last 50th to the next: a tick at every 10th with its
	 * multiplier, the tasks done filled in, the next task marked. With the skip plan on, the
	 * milestones it takes from the high-points master are gold and the rest faint.
	 */
	static final class StreakTrack extends JComponent
	{
		private final int streak;
		private final SkipPlan plan;

		StreakTrack(int streak, SkipPlan plan)
		{
			this.streak = Math.max(0, streak);
			this.plan = plan;
			setPreferredSize(new Dimension(10, 47));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int end = Slayer.nextMilestone(streak, 15);
			int start = end - 50;
			int next = streak + 1;
			g.setFont(Theme.small());
			Ui.shadowed(g, "STREAK", 0, 13, Theme.GOLD_DIM);
			FontMetrics fm = g.getFontMetrics();
			int lx = fm.stringWidth("STREAK") + 6;
			g.setFont(Theme.body());
			String num = Format.number(streak);
			Ui.shadowed(g, num, lx, 14, Theme.GOLD_BRIGHT);
			int nx = lx + g.getFontMetrics().stringWidth(num) + 4;
			g.setFont(Theme.small());
			Ui.shadowed(g, "in a row", nx, 13, Theme.TEXT_MUTED);
			String right = Slayer.ordinal(end) + " in " + (end - streak);
			int rw = g.getFontMetrics().stringWidth(right);
			Ui.shadowed(g, right, getWidth() - rw, 13, Theme.TEXT_MUTED);

			int x0 = 4;
			int w = getWidth() - 9;
			int y = 25;
			g.setColor(Theme.INK_DEEP);
			g.fill(new RoundRectangle2D.Float(x0, y - 2, w, 5, 5, 5));
			double done = (streak - start) / 50.0;
			if (done > 0)
			{
				g.setPaint(new GradientPaint(x0, y, Theme.GOLD_DIM, x0 + w, y, Theme.GOLD_BRIGHT));
				g.fill(new RoundRectangle2D.Float(x0, y - 2, (float) (w * Math.min(1, done)), 5, 5, 5));
			}
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(x0, y - 2, w, 5, 5, 5));
			for (int t = start + 10; t <= end; t += 10)
			{
				float x = x0 + (float) (w * (t - start) / 50.0);
				boolean reached = t <= streak;
				boolean taken = plan == null || !plan.isOn() || plan.isMilestone(t);
				Color c = reached ? Theme.GOLD_BRIGHT : taken ? Theme.GOLD : Theme.TEXT_FAINT;
				float r = t == end ? 4.5f : 3f;
				g.setColor(Theme.INK_DEEP);
				g.fill(new Ellipse2D.Float(x - r - 1, y + 0.5f - r - 1, 2 * r + 2, 2 * r + 2));
				g.setColor(reached ? c : Theme.CARD);
				g.fill(new Ellipse2D.Float(x - r, y + 0.5f - r, 2 * r, 2 * r));
				g.setColor(c);
				g.draw(new Ellipse2D.Float(x - r, y + 0.5f - r, 2 * r, 2 * r));
				String label = Slayer.multiplier(t) + "x";
				int tw = g.getFontMetrics().stringWidth(label);
				int tx = Math.max(0, Math.min(getWidth() - tw, Math.round(x - tw / 2f)));
				Ui.shadowed(g, label, tx, y + 18, t == end ? Theme.GOLD_BRIGHT : taken ? Theme.TEXT_MUTED : Theme.TEXT_FAINT);
			}
			if (next <= end)
			{
				float x = x0 + (float) (w * (next - start) / 50.0);
				g.setColor(Theme.alpha(Theme.TEAL, 70));
				g.fill(new Ellipse2D.Float(x - 5, y - 4.5f, 10, 10));
				g.setColor(Theme.TEAL);
				g.fill(new Ellipse2D.Float(x - 2.5f, y - 2, 5, 5));
			}
			g.dispose();
		}
	}

	/** What the next task pays with each master open, as bars; the plan's master outlined. */
	static final class PaysChart extends JComponent
	{
		private static final int ROW = 13;
		private final int task;
		private final List<SkipPlan.Pays> pays;
		private final Slayer.Master pick;

		PaysChart(int task, List<SkipPlan.Pays> pays, Slayer.Master pick)
		{
			this.task = task;
			this.pays = pays;
			this.pick = pick;
			setPreferredSize(new Dimension(10, 14 + ROW * pays.size()));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String head = "YOUR " + Slayer.ordinal(task).toUpperCase() + " TASK PAYS";
			Ui.shadowed(g, head, 0, 10, Theme.GOLD_DIM);
			int nameW = 0;
			int max = 1;
			for (SkipPlan.Pays p : pays)
			{
				nameW = Math.max(nameW, fm.stringWidth(p.name));
				max = Math.max(max, p.points);
			}
			int numW = fm.stringWidth("0000");
			int bx = nameW + 6;
			int bw = getWidth() - bx - numW - 4;
			int y = 14;
			boolean first = true;
			for (SkipPlan.Pays p : pays)
			{
				boolean best = first && p.points > 0;
				boolean picked = p.master == pick;
				Ui.shadowed(g, p.name, 0, y + 9, picked ? Theme.GOLD_BRIGHT : Theme.TEXT);
				g.setColor(Theme.INK_DEEP);
				g.fill(new RoundRectangle2D.Float(bx, y + 3, bw, 6, 6, 6));
				if (p.points > 0)
				{
					float len = Math.max(6, bw * p.points / (float) max);
					Color c = best ? Theme.GOLD : Theme.GOLD_DIM;
					g.setPaint(new GradientPaint(0, y + 3, Theme.mix(c, Color.WHITE, 0.2f), 0, y + 9, Theme.mix(c, Theme.INK_DEEP, 0.3f)));
					g.fill(new RoundRectangle2D.Float(bx, y + 3, len, 6, 6, 6));
				}
				g.setColor(picked ? Theme.GOLD_BRIGHT : Theme.STROKE_SOFT);
				g.draw(new RoundRectangle2D.Float(bx, y + 3, bw, 6, 6, 6));
				String n = Format.number(p.points);
				Ui.shadowed(g, n, getWidth() - fm.stringWidth(n), y + 9, p.points == 0 ? Theme.TEXT_FAINT : best ? Theme.GOLD_BRIGHT : Theme.TEXT);
				y += ROW;
				first = false;
			}
			g.dispose();
		}
	}
}
