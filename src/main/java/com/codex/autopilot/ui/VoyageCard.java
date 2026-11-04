package com.codex.autopilot.ui;

import com.codex.autopilot.core.Format;
import com.codex.autopilot.core.PortLog;
import com.codex.autopilot.core.PortTask;
import com.codex.autopilot.core.Voyage;
import java.awt.BasicStroke;
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
import javax.swing.border.EmptyBorder;

/**
 * The port tasks in hand: the voyage as a chart of its stops down a gilt rail, the next stop lit,
 * each with what to deliver and collect there; then every task, where it goes and how far along it
 * is, with the Sailing xp it paid this account the last time it was handed in.
 */
public final class VoyageCard
{
	private static final int RAIL = 18;

	private VoyageCard()
	{
	}

	/** The voyage's stops, or null when no courier task needs a stop. */
	public static JComponent route(Voyage voyage, int width)
	{
		if (voyage == null || voyage.isEmpty())
		{
			return null;
		}
		Card card = new Card(new BorderLayout()).accent(voyage.aboard() > 0 ? Theme.GREEN : Theme.GOLD);
		StackPanel body = new StackPanel(0, 0);
		List<Voyage.Stop> stops = voyage.getStops();
		for (int i = 0; i < stops.size(); i++)
		{
			body.add(new StopRow(stops.get(i), i, stops.size(), width - RAIL));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** Every task in the slots, one row each, with the slots used and the level the next one opens at. */
	public static JComponent tasks(PortLog log, int width)
	{
		Card card = new Card(new BorderLayout());
		StackPanel body = new StackPanel(6, 0);
		for (PortTask t : log.getTasks())
		{
			body.add(task(t, log.xp(t.getId()), width));
		}
		int slots = PortLog.slots(log.getSailing());
		if (slots > 0)
		{
			String foot = log.getTasks().size() + " of " + slots + (slots == 1 ? " slot" : " slots") + " in use";
			int next = PortLog.nextSlotLevel(log.getSailing());
			if (next > 0)
			{
				foot += "; another opens at " + next + " Sailing";
			}
			body.add(Ui.wrapped(foot, Theme.small(), Theme.TEXT_FAINT, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	private static JComponent task(PortTask t, int xp, int width)
	{
		StackPanel row = new StackPanel(1, 0);
		row.add(new Heading(t.getName() == null ? (t.isCourier() ? t.getCargo() + " delivery" : "Bounty") : t.getName(),
			xp > 0 ? Format.number(xp) + " xp" : null));
		String where;
		String count;
		double progress;
		if (t.isCourier())
		{
			where = t.getCargo() + ": " + t.getFrom() + " → " + t.getTo();
			if (t.getTaken() < t.getAmount())
			{
				count = t.getTaken() + " of " + Format.plural(t.getAmount(), "crate", "crates") + " collected";
			}
			else if (t.getDelivered() == 0)
			{
				count = (t.getAmount() == 1 ? "Aboard" : "All " + t.getAmount() + " aboard") + ": deliver at " + t.getTo();
			}
			else
			{
				count = t.getDelivered() + " of " + Format.plural(t.getAmount(), "crate", "crates") + " delivered";
			}
			progress = t.progress();
		}
		else
		{
			String monster = t.getMonster() == null ? "Sea creature" : t.getMonster();
			where = monster + ", at sea; any port master pays";
			count = t.isBountyDone() ? "All " + t.getNeed() + " in hand: claim it" : t.getItem() + ", " + t.getHave() + " of " + t.getNeed();
			progress = t.progress();
		}
		row.add(Ui.wrapped(where, Theme.small(), Theme.TEXT_MUTED, width));
		row.add(new Meter(count, progress));
		return row;
	}

	/** One stop: a node on the rail, the port's name, and what is done there. */
	private static final class StopRow extends JPanel
	{
		private final int index;
		private final int count;

		StopRow(Voyage.Stop stop, int index, int count, int width)
		{
			super(new BorderLayout());
			this.index = index;
			this.count = count;
			setOpaque(false);
			setBorder(new EmptyBorder(index == 0 ? 0 : 3, RAIL, index == count - 1 ? 0 : 5, 0));
			StackPanel text = new StackPanel(1, 0);
			boolean next = index == 0;
			text.add(Ui.label(stop.getPort(), next ? Theme.title() : Theme.body(), next ? Theme.GOLD_BRIGHT : Theme.TEXT));
			text.add(Ui.wrapped(stop.actions(), Theme.small(), next ? Theme.TEXT : Theme.TEXT_MUTED, width));
			add(text, BorderLayout.CENTER);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int cx = 6;
			int cy = (index == 0 ? 0 : 3) + 8;
			// The rail runs on to the next stop and back to the last, dotted like a sea chart's course.
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{2.5f, 3f}, 0));
			g.setColor(Theme.alpha(Theme.GOLD, 120));
			if (index > 0)
			{
				g.drawLine(cx, 0, cx, cy - 6);
			}
			if (index < count - 1)
			{
				g.drawLine(cx, cy + 6, cx, getHeight());
			}
			g.setStroke(new BasicStroke(1.2f));
			if (index == 0)
			{
				g.setColor(Theme.alpha(Theme.GOLD_BRIGHT, 50));
				g.fill(new Ellipse2D.Float(cx - 7, cy - 7, 14, 14));
				g.setPaint(new GradientPaint(0, cy - 5, Theme.GOLD_BRIGHT, 0, cy + 5, Theme.GOLD));
				g.fill(new Ellipse2D.Float(cx - 4.5f, cy - 4.5f, 9, 9));
				g.setColor(Theme.INK_DEEP);
				g.draw(new Ellipse2D.Float(cx - 4.5f, cy - 4.5f, 9, 9));
			}
			else
			{
				g.setColor(Theme.CARD);
				g.fill(new Ellipse2D.Float(cx - 4, cy - 4, 8, 8));
				g.setColor(index == count - 1 ? Theme.GOLD : Theme.GOLD_DIM);
				g.draw(new Ellipse2D.Float(cx - 4, cy - 4, 8, 8));
			}
			g.dispose();
		}
	}

	/** A task's name with its pay at the right. */
	private static final class Heading extends JComponent
	{
		private final String text;
		private final String right;

		Heading(String text, String right)
		{
			this.text = text;
			this.right = right;
			setPreferredSize(new Dimension(10, 15));
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
				Ui.shadowed(g, right, getWidth() - rw + 6, 12, Theme.TEAL);
			}
			g.setFont(Theme.body());
			Ui.shadowed(g, Ui.clip(text, g.getFontMetrics(), getWidth() - rw), 0, 12, Theme.GOLD_BRIGHT);
			g.dispose();
		}
	}

	/** "1 of 2 crates collected" over a thin bar in the progress colours. */
	private static final class Meter extends JComponent
	{
		private final String text;
		private final double progress;

		Meter(String text, double progress)
		{
			this.text = text;
			this.progress = Math.max(0, Math.min(1, progress));
			setPreferredSize(new Dimension(10, 19));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(Theme.small());
			Color c = Theme.progressColor(progress);
			Ui.shadowed(g, Ui.clip(text, g.getFontMetrics(), getWidth()), 0, 10, c);
			int y = 14;
			int w = getWidth() - 1;
			g.setColor(Theme.INK_DEEP);
			g.fill(new RoundRectangle2D.Float(0, y, w, 4, 4, 4));
			if (progress > 0)
			{
				g.setPaint(new GradientPaint(0, y, Theme.mix(c, Color.WHITE, 0.2), 0, y + 4, Theme.mix(c, Theme.INK_DEEP, 0.3)));
				g.fill(new RoundRectangle2D.Float(0, y, Math.max(4, (float) (w * progress)), 4, 4, 4));
			}
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(0, y, w, 4, 4, 4));
			g.dispose();
		}
	}

	/** For the panel's heading: "2 tasks" or "1 task, 1 bounty". */
	public static String summary(PortLog log)
	{
		int couriers = 0;
		int bounties = 0;
		for (PortTask t : log.getTasks())
		{
			if (t.isCourier())
			{
				couriers++;
			}
			else
			{
				bounties++;
			}
		}
		List<String> parts = new ArrayList<>();
		if (couriers > 0)
		{
			parts.add(Format.plural(couriers, "courier", "couriers"));
		}
		if (bounties > 0)
		{
			parts.add(Format.plural(bounties, "bounty", "bounties"));
		}
		return String.join(", ", parts);
	}
}
