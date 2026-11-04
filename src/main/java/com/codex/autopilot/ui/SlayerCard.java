package com.codex.autopilot.ui;

import com.codex.autopilot.core.Format;
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
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The Slayer task at a glance: the monster and its master, a tally of how many are left, where
 * they live and what the fight needs (for the tasks whose facts are checked), the time left at the
 * player's own pace, and the points and streak with which master to ask next.
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
		Slayer.Master next = Slayers.next(log);
		if (next != null)
		{
			String pts = Slayers.points(log, next);
			body.add(field("Next", next.nameFor(a) + (pts == null ? "" : ": " + pts), log.hasTask() ? Theme.TEXT_MUTED : Theme.GREEN,
				width));
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
}
