package com.codex.autopilot.ui;

import com.codex.autopilot.core.Bring;
import com.codex.autopilot.core.Candidate;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A step on the panel. The lead step is large: its mark in a gilt medallion, the tier it sits in,
 * the step in gold, the why, what to bring and the two buttons. The steps after it are compact
 * rows with their place in the order.
 */
public final class StepCard
{
	private StepCard()
	{
	}

	/** The one best thing to do now. */
	public static Card lead(Candidate c, Actions actions, int width, boolean dim)
	{
		Color tint = dim ? Theme.TEXT_MUTED : Theme.tier(c.getTier());
		Card card = new Card(new BorderLayout(0, 6)).accent(tint).highlighted(!dim);
		card.add(new LeadHead(c, tint), BorderLayout.NORTH);

		StackPanel body = new StackPanel(3, 0);
		body.add(Ui.wrapped(c.getTitle(), Theme.title(), dim ? Theme.TEXT : Theme.GOLD_BRIGHT, width));
		body.add(Ui.wrapped(c.getWhy(), Theme.small(), Theme.TEXT, width));
		if (c.getBring() != null)
		{
			body.add(Ui.wrapped(c.getBring(), Theme.small(), Bring.isShort(c.getBring()) ? Theme.EMBER : Theme.TEAL, width));
		}
		card.add(body, BorderLayout.CENTER);
		if (actions != null)
		{
			card.add(buttons(c, actions, 22, FlowLayout.LEFT), BorderLayout.SOUTH);
		}
		return card;
	}

	/** A later step, numbered by its place. */
	public static Card row(int place, Candidate c, Actions actions, int width)
	{
		Color tint = Theme.tier(c.getTier());
		Card card = new Card(new BorderLayout(0, 3)).accent(tint);
		card.setBorder(new javax.swing.border.EmptyBorder(6, 10, 6, 8));
		card.add(new RowHead(place, c, tint), BorderLayout.NORTH);
		StackPanel body = new StackPanel(2, 0);
		body.add(Ui.wrapped(c.getTitle(), Theme.body(), Theme.TEXT, width));
		body.add(Ui.wrapped(c.getWhy(), Theme.small(), Theme.TEXT_MUTED, width));
		if (c.getBring() != null)
		{
			body.add(Ui.wrapped(c.getBring(), Theme.small(), Bring.isShort(c.getBring()) ? Theme.EMBER : Theme.alpha(Theme.TEAL, 220), width));
		}
		card.add(body, BorderLayout.CENTER);
		if (actions != null)
		{
			card.add(buttons(c, actions, 18, FlowLayout.RIGHT), BorderLayout.SOUTH);
		}
		return card;
	}

	private static JPanel buttons(Candidate c, Actions actions, int height, int align)
	{
		JPanel bar = new JPanel(new FlowLayout(align, 0, 0));
		bar.setOpaque(false);
		bar.add(new CodexButton("Skip", false, height, () -> actions.skip(c.getKey())));
		JPanel gap = new JPanel();
		gap.setOpaque(false);
		gap.setPreferredSize(new Dimension(5, height));
		bar.add(gap);
		bar.add(new CodexButton("Snooze 1h", false, height, () -> actions.snooze(c.getKey())));
		return bar;
	}

	/** "~11 min", or "1 hr" for the long ones. */
	static String minutes(Candidate c)
	{
		int m = c.getMinutes();
		return m >= 60 ? "~1 hr" : "~" + m + " min";
	}

	/** The lead step's head: the mark in its medallion, what kind of step, its tier and how long. */
	static final class LeadHead extends JComponent
	{
		private static final int HEIGHT = 34;
		private final Candidate c;
		private final Color tint;

		LeadHead(Candidate c, Color tint)
		{
			this.c = c;
			this.tint = tint;
			setOpaque(false);
			setPreferredSize(new Dimension(10, HEIGHT));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			int d = 32;
			int my = 1;
			g.setPaint(new RadialGradientPaint(d / 2f, my + d / 3f, d * 0.75f, new float[]{0, 1},
				new Color[]{Theme.mix(Theme.CARD_TOP, tint, 0.22), Theme.INK_DEEP}));
			g.fill(new Ellipse2D.Float(0, my, d, d));
			g.setStroke(new BasicStroke(1.4f));
			g.setPaint(new GradientPaint(0, my, Theme.mix(tint, Color.WHITE, 0.25), 0, my + d, Theme.mix(tint, Theme.INK_DEEP, 0.4)));
			g.draw(new Ellipse2D.Float(0.7f, my + 0.7f, d - 1.4f, d - 1.4f));
			g.setStroke(new BasicStroke(1f));
			Glyph.paint(g, c.getKind(), 8, my + 8, 16, tint);

			int tx = d + 8;
			g.setFont(Theme.small());
			FontMetrics small = g.getFontMetrics();
			String time = minutes(c);
			int pillW = small.stringWidth(time) + 12;
			int px = w - pillW;
			RoundRectangle2D pill = new RoundRectangle2D.Float(px + 0.5f, 3.5f, pillW - 1, 15, 8, 8);
			g.setColor(Theme.alpha(Theme.GOLD, 26));
			g.fill(pill);
			g.setColor(Theme.alpha(Theme.GOLD, 120));
			g.draw(pill);
			Ui.shadowed(g, time, px + 6, 15, Theme.GOLD);

			int room = px - tx - 6;
			Ui.shadowed(g, Ui.clip(c.getOver().toUpperCase(), small, room), tx, 14, Theme.GOLD);
			Ui.shadowed(g, Ui.clip(c.getTier().getLabel(), small, w - tx), tx, 29, tint);
			g.dispose();
		}
	}

	/** A row's head: its place in a gilt ring, what kind of step and how long. */
	static final class RowHead extends JComponent
	{
		private static final int HEIGHT = 18;
		private final int place;
		private final Candidate c;
		private final Color tint;

		RowHead(int place, Candidate c, Color tint)
		{
			this.place = place;
			this.c = c;
			this.tint = tint;
			setOpaque(false);
			setPreferredSize(new Dimension(10, HEIGHT));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			g.setFont(Theme.small());
			FontMetrics small = g.getFontMetrics();

			g.setColor(Theme.INK_DEEP);
			g.fill(new Ellipse2D.Float(0, 1, 15, 15));
			g.setColor(Theme.alpha(tint, 190));
			g.draw(new Ellipse2D.Float(0.5f, 1.5f, 14, 14));
			String n = String.valueOf(place);
			Ui.shadowed(g, n, 8 - small.stringWidth(n) / 2, 13, Theme.GOLD_BRIGHT);

			Glyph.paint(g, c.getKind(), 20, 2, 13, tint);

			String time = minutes(c);
			int tw = small.stringWidth(time);
			Ui.shadowed(g, time, w - tw, 13, Theme.TEXT_FAINT);
			Ui.shadowed(g, Ui.clip(c.getOver().toUpperCase(), small, w - 38 - tw - 6), 38, 13, Theme.GOLD_DIM);
			g.dispose();
		}
	}
}
