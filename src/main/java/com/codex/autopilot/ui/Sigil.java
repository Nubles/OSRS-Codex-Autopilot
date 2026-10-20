package com.codex.autopilot.ui;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The Autopilot mark: a compass needle inside a gilt ring, its lit half pointing on to the next
 * step, with a teal pin at its heart.
 */
public final class Sigil
{
	/** Where the needle points: a little east of north, onward. */
	private static final double HEADING = Math.toRadians(38);

	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		float cx = x + 8 * s;
		float cy = y + 8 * s;
		float r = 6.6f * s;

		// The ring: dark under, gilt over, with the four quarters marked.
		g.setStroke(new BasicStroke(Math.max(2.2f, 2.4f * s)));
		g.setColor(Theme.INK_DEEP);
		g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setStroke(new BasicStroke(Math.max(1.1f, 1.2f * s)));
		g.setPaint(new GradientPaint(x, y, Theme.GOLD_BRIGHT, x, y + size, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setStroke(new BasicStroke(Math.max(0.8f, 0.8f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int i = 0; i < 4; i++)
		{
			double a = i * Math.PI / 2;
			float ox = (float) Math.sin(a);
			float oy = (float) -Math.cos(a);
			g.setColor(Theme.alpha(Theme.GOLD, 200));
			g.draw(new Line2D.Float(cx + ox * (r - 2.0f * s), cy + oy * (r - 2.0f * s), cx + ox * (r - 0.9f * s), cy + oy * (r - 0.9f * s)));
		}

		// The needle: a slim diamond, its forward half lit gold and its back half in shadow.
		float len = 5.6f * s;
		float half = 1.7f * s;
		float dx = (float) Math.sin(HEADING);
		float dy = (float) -Math.cos(HEADING);
		float nx = -dy;
		float ny = dx;
		Path2D front = new Path2D.Float();
		front.moveTo(cx + dx * len, cy + dy * len);
		front.lineTo(cx + nx * half, cy + ny * half);
		front.lineTo(cx - nx * half, cy - ny * half);
		front.closePath();
		Path2D back = new Path2D.Float();
		back.moveTo(cx - dx * len * 0.82f, cy - dy * len * 0.82f);
		back.lineTo(cx + nx * half, cy + ny * half);
		back.lineTo(cx - nx * half, cy - ny * half);
		back.closePath();
		g.setPaint(new GradientPaint(cx + dx * len, cy + dy * len, Theme.mix(Theme.GOLD_BRIGHT, java.awt.Color.WHITE, 0.25), cx, cy, Theme.GOLD));
		g.fill(front);
		g.setPaint(new GradientPaint(cx, cy, Theme.GOLD_DIM, cx - dx * len, cy - dy * len, Theme.mix(Theme.GOLD_DIM, Theme.INK_DEEP, 0.5)));
		g.fill(back);
		g.setStroke(new BasicStroke(Math.max(0.5f, 0.45f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.INK_DEEP, 220));
		g.draw(front);
		g.draw(back);

		// The pin: a teal stone, lit from above.
		float pr = Math.max(1.0f, 1.15f * s);
		g.setPaint(new RadialGradientPaint(cx - pr * 0.3f, cy - pr * 0.4f, pr * 1.6f, new float[]{0, 1},
			new java.awt.Color[]{Theme.mix(Theme.TEAL, java.awt.Color.WHITE, 0.45), Theme.mix(Theme.TEAL, Theme.INK_DEEP, 0.35)}));
		g.fill(new Ellipse2D.Float(cx - pr, cy - pr, pr * 2, pr * 2));
		g.setColor(Theme.INK_DEEP);
		g.setStroke(new BasicStroke(Math.max(0.4f, 0.35f * s)));
		g.draw(new Ellipse2D.Float(cx - pr, cy - pr, pr * 2, pr * 2));
	}
}
