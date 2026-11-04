package com.codex.autopilot.ui;

import com.codex.autopilot.core.Trip;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/**
 * How to get to a step, as Codex Wayfinder's atlas prices it: a small waypoint mark and "via
 * Ardougne cloak, ~40s". Teal when the teleport is carried, muted when it waits in the bank.
 */
public final class TravelLine extends JComponent
{
	private final Trip trip;

	public TravelLine(Trip trip)
	{
		this.trip = trip;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 15));
		setToolTipText("Priced from Codex Wayfinder's atlas: the teleports this account has, and the walk from where they land");
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		Color tint = trip.isBanked() ? Theme.TEXT_MUTED : Theme.TEAL;
		paintMark(g, 0, 2, tint);
		Ui.shadowed(g, Ui.clip(trip.text(), fm, getWidth() - 14), 14, 12, tint);
		g.dispose();
	}

	/** A waypoint: a ringed dot with a short trail, 11 by 11. */
	public static void paintMark(Graphics2D g, int x, int y, Color tint)
	{
		Graphics2D gg = (Graphics2D) g.create();
		Theme.smooth(gg);
		gg.translate(x, y);
		gg.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Path2D trail = new Path2D.Float();
		trail.moveTo(0.8, 10);
		trail.quadTo(2.5, 6.5, 5.2, 6.8);
		gg.setColor(Theme.alpha(tint, 150));
		gg.draw(trail);
		gg.setColor(tint);
		gg.draw(new Ellipse2D.Float(4.6f, 0.8f, 5.8f, 5.8f));
		gg.fill(new Ellipse2D.Float(6.3f, 2.5f, 2.4f, 2.4f));
		gg.dispose();
	}
}
