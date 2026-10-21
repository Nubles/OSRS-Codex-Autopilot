package com.codex.autopilot.ui;

import com.codex.autopilot.core.Candidate;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A small engraved mark for each kind of step: a bird house, a tree, a sprout, a herb sprig, a hop
 * cone, an hourglass, a scroll, a crown, a star and a flag. Drawn rather than taken from item sprites, so it reads the
 * same at any size and in the game view.
 */
public final class Glyph
{
	private Glyph()
	{
	}

	/** Draws the mark for a kind in a size by size box, tinted. */
	public static void paint(Graphics2D g, Candidate.Kind kind, float x, float y, float size, Color tint)
	{
		Graphics2D gg = (Graphics2D) g.create();
		Theme.smooth(gg);
		gg.translate(x, y);
		gg.scale(size / 16f, size / 16f);
		Shape shape = shape(kind);
		Color light = Theme.mix(tint, Color.WHITE, 0.3);
		Color dark = Theme.mix(tint, Theme.INK_DEEP, 0.35);
		gg.setPaint(new GradientPaint(0, 1, light, 0, 15, dark));
		gg.fill(shape);
		gg.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gg.setColor(Theme.alpha(Theme.INK_DEEP, 230));
		gg.draw(shape);
		detail(gg, kind, tint);
		gg.dispose();
	}

	private static Shape shape(Candidate.Kind kind)
	{
		switch (kind)
		{
			case BIRDHOUSE_RUN:
			{
				Path2D p = new Path2D.Float();
				p.moveTo(8, 1.5);
				p.lineTo(14.5, 7);
				p.lineTo(12.6, 7);
				p.lineTo(12.6, 14.5);
				p.lineTo(3.4, 14.5);
				p.lineTo(3.4, 7);
				p.lineTo(1.5, 7);
				p.closePath();
				Area a = new Area(p);
				a.subtract(new Area(new Ellipse2D.Float(6.1f, 7.6f, 3.8f, 3.8f)));
				return a;
			}
			case TREE_RUN:
			{
				Area a = new Area(new Ellipse2D.Float(2.5f, 1.5f, 11, 9.5f));
				a.add(new Area(new Ellipse2D.Float(1.2f, 5f, 6.5f, 6f)));
				a.add(new Area(new Ellipse2D.Float(8.3f, 5f, 6.5f, 6f)));
				a.add(new Area(new Rectangle2D.Float(6.9f, 9.5f, 2.2f, 5.5f)));
				return a;
			}
			case PATCH_RUN:
			{
				Area a = new Area(leaf(8, 9, -1));
				a.add(new Area(leaf(8, 7.5f, 1)));
				a.add(new Area(new Rectangle2D.Float(7.3f, 7f, 1.4f, 6f)));
				a.add(new Area(new Ellipse2D.Float(3.5f, 12.2f, 9, 3)));
				return a;
			}
			case HERB_RUN:
			{
				// A sprig: three leaves on a stem.
				Area a = new Area(new Rectangle2D.Float(7.4f, 6f, 1.2f, 9f));
				a.add(new Area(leaf(8, 7.5f, -1)));
				a.add(new Area(leaf(8, 7.5f, 1)));
				Path2D top = new Path2D.Float();
				top.moveTo(8, 7);
				top.quadTo(4.6, 3.6, 8, 0.8);
				top.quadTo(11.4, 3.6, 8, 7);
				top.closePath();
				a.add(new Area(top));
				a.add(new Area(leaf(8, 12.5f, -1)));
				return a;
			}
			case HOPS_RUN:
			{
				// A hop cone hanging from its bine.
				Area a = new Area(new Ellipse2D.Float(4f, 3.5f, 8f, 11.5f));
				a.add(new Area(new Rectangle2D.Float(7.4f, 0.8f, 1.2f, 3.5f)));
				a.add(new Area(leaf(8.6f, 3f, 1)));
				return a;
			}
			case DAILIES:
			{
				Path2D p = new Path2D.Float();
				p.moveTo(3.5, 1.5);
				p.lineTo(12.5, 1.5);
				p.lineTo(12.5, 3);
				p.curveTo(12.5, 6, 9, 7, 9, 8);
				p.curveTo(9, 9, 12.5, 10, 12.5, 13);
				p.lineTo(12.5, 14.5);
				p.lineTo(3.5, 14.5);
				p.lineTo(3.5, 13);
				p.curveTo(3.5, 10, 7, 9, 7, 8);
				p.curveTo(7, 7, 3.5, 6, 3.5, 3);
				p.closePath();
				return p;
			}
			case CONTRACT:
			{
				Area a = new Area(new RoundRectangle2D.Float(3.5f, 2, 9, 12, 2, 2));
				a.add(new Area(new Ellipse2D.Float(2, 1, 3.5f, 3.5f)));
				a.add(new Area(new Ellipse2D.Float(10.5f, 11.5f, 3.5f, 3.5f)));
				return a;
			}
			case KINGDOM:
			{
				Path2D p = new Path2D.Float();
				p.moveTo(1.5, 5);
				p.lineTo(5, 8.5);
				p.lineTo(8, 2.5);
				p.lineTo(11, 8.5);
				p.lineTo(14.5, 5);
				p.lineTo(13, 13);
				p.lineTo(3, 13);
				p.closePath();
				Area a = new Area(p);
				a.add(new Area(new RoundRectangle2D.Float(3, 12.2f, 10, 2.4f, 1, 1)));
				return a;
			}
			case FOCUS_GOAL:
				return star(8, 8.4f, 7, 2.9f, 5);
			default:
			{
				Area a = new Area(new Rectangle2D.Float(3, 1.5f, 1.6f, 13.5f));
				Path2D flag = new Path2D.Float();
				flag.moveTo(4.4, 2);
				flag.curveTo(7, 1, 9, 3.5, 13.5, 2.5);
				flag.lineTo(13.5, 8.5);
				flag.curveTo(9, 9.5, 7, 7, 4.4, 8);
				flag.closePath();
				a.add(new Area(flag));
				return a;
			}
		}
	}

	/** Inked details that a fill alone would lose. */
	private static void detail(Graphics2D g, Candidate.Kind kind, Color tint)
	{
		g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
		switch (kind)
		{
			case DAILIES:
				g.setColor(Theme.mix(Theme.GOLD_BRIGHT, tint, 0.3));
				Path2D sand = new Path2D.Float();
				sand.moveTo(5.5, 13.6);
				sand.lineTo(10.5, 13.6);
				sand.lineTo(8, 11);
				sand.closePath();
				g.fill(sand);
				break;
			case CONTRACT:
				g.draw(new Line2D.Float(5.5f, 6, 10.5f, 6));
				g.draw(new Line2D.Float(5.5f, 8.5f, 10.5f, 8.5f));
				g.draw(new Line2D.Float(5.5f, 11, 9, 11));
				break;
			case KINGDOM:
				g.setColor(Theme.mix(Theme.TEAL, Color.WHITE, 0.3));
				g.fill(new Ellipse2D.Float(7, 9.2f, 2, 2));
				break;
			case HOPS_RUN:
				// The overlapping bracts.
				Path2D bracts = new Path2D.Float();
				bracts.moveTo(5, 7.5);
				bracts.quadTo(8, 9.5, 11, 7.5);
				bracts.moveTo(4.6, 10.5);
				bracts.quadTo(8, 12.5, 11.4, 10.5);
				bracts.moveTo(6, 13.4);
				bracts.quadTo(8, 14.6, 10, 13.4);
				g.draw(bracts);
				break;
			default:
				break;
		}
	}

	private static Shape leaf(float bx, float by, int side)
	{
		float tipX = bx + side * 5.5f;
		float tipY = by - 4.5f;
		Path2D p = new Path2D.Float();
		p.moveTo(bx, by);
		p.quadTo(bx + side * 0.8f, tipY - 0.8f, tipX, tipY);
		p.quadTo(bx + side * 5f, by + 0.6f, bx, by);
		p.closePath();
		return p;
	}

	private static Shape star(float cx, float cy, float outer, float inner, int points)
	{
		Path2D p = new Path2D.Float();
		for (int i = 0; i < points * 2; i++)
		{
			double a = Math.PI * i / points - Math.PI / 2;
			float r = i % 2 == 0 ? outer : inner;
			float px = (float) (cx + Math.cos(a) * r);
			float py = (float) (cy + Math.sin(a) * r);
			if (i == 0)
			{
				p.moveTo(px, py);
			}
			else
			{
				p.lineTo(px, py);
			}
		}
		p.closePath();
		return p;
	}
}
