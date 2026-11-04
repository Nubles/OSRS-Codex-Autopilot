package com.codex.autopilot.ui;

import com.codex.autopilot.core.Format;
import com.codex.autopilot.core.Kitten;
import com.codex.autopilot.core.KittenBook;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * The kitten at a glance: how long before it runs off for food and for attention, each a bar that
 * drains toward the game's warning and burns ember once the game has given it, and how far it is
 * from growing into a cat (or, grown, from turning overgrown). Under them, whose times these are:
 * the wiki's, or this kitten's own once its warnings have been heard.
 */
public final class KittenCard
{
	private KittenCard()
	{
	}

	/** "Following", "Carried", "Put away", "Ran off": for the section heading. */
	public static String summary(KittenBook k)
	{
		if (k.getGone() != null)
		{
			return capital(k.getGone());
		}
		if (k.isOut())
		{
			return "Following";
		}
		return k.isHeld() ? "Carried" : "Put away";
	}

	public static JComponent card(KittenBook k, long nowMs, int width)
	{
		Kitten.Stage stage = k.getStage();
		boolean warned = k.getHungry() > 0 || k.getLonely() > 0;
		Card card = new Card(new BorderLayout()).accent(k.getGone() != null ? Theme.BLOCKED
			: warned && k.isOut() ? Theme.EMBER : stage == Kitten.Stage.KITTEN ? Theme.GOLD : Theme.GREEN);
		StackPanel body = new StackPanel(5, 0);
		body.add(new Heading(stage == null ? "Kitten" : stage.getLabel(), right(k)));
		if (k.getGone() != null)
		{
			body.add(Ui.wrapped("It " + k.getGone() + ". Gertrude has another kitten for 100 coins.", Theme.small(), Theme.TEXT_MUTED,
				width));
		}
		else if (stage == Kitten.Stage.KITTEN)
		{
			body.add(hunger(k));
			body.add(attention(k));
			body.add(growth(k));
			body.add(Ui.wrapped(footnote(k), Theme.small(), Theme.TEXT_FAINT, width));
		}
		else
		{
			body.add(grown(k, nowMs));
			body.add(Ui.wrapped(catNote(k), Theme.small(), Theme.TEXT_FAINT, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** Its age when known; otherwise that its needs are waiting while it is put away. */
	private static String right(KittenBook k)
	{
		if (k.getGone() != null)
		{
			return null;
		}
		if (!k.isOut())
		{
			return "needs on hold";
		}
		if (k.getStage() == Kitten.Stage.KITTEN && k.isAgeKnown() && k.getGrowth() >= 0)
		{
			return "age " + Format.shortDuration(k.getGrowth());
		}
		return null;
	}

	private static JComponent hunger(KittenBook k)
	{
		long left = k.hungerLeft();
		if (left < 0)
		{
			return new Gauge("Food", "Not fed while watched: feed it to start", 0, Theme.TEXT_FAINT);
		}
		long whole = Math.round(k.hungerMinutes() * 60) + 2L * Kitten.HUNGER_STEP_MINUTES * 60;
		String text;
		Color c;
		if (k.getHungry() > 0)
		{
			text = (k.getHungry() >= 2 ? "Very hungry" : "Hungry") + ": runs off in ~" + Format.duration(left);
			c = k.getHungry() >= 2 ? Theme.BLOOD : Theme.EMBER;
		}
		else
		{
			long warn = left - 2L * Kitten.HUNGER_STEP_MINUTES * 60;
			text = "Fed " + Format.duration(k.sinceFed()) + " ago; hungry " + (warn > 0 ? "in ~" + Format.duration(warn) : "any moment");
			c = null;
		}
		return new Gauge("Food", text, (double) left / whole, c);
	}

	private static JComponent attention(KittenBook k)
	{
		long left = k.lonelyLeft();
		Kitten.Care care = k.getCare();
		if (left < 0 || care == null)
		{
			return new Gauge("Love", "Not stroked while watched: stroke it to start", 0, Theme.TEXT_FAINT);
		}
		long whole = Math.round(k.careMinutes(care) * 60) + 2L * Kitten.LONELY_STEP_MINUTES * 60;
		String text;
		Color c;
		if (k.getLonely() > 0)
		{
			text = (k.getLonely() >= 2 ? "Lonely" : "Wants attention") + ": runs off in ~" + Format.duration(left);
			c = k.getLonely() >= 2 ? Theme.BLOOD : Theme.EMBER;
		}
		else
		{
			long warn = left - 2L * Kitten.LONELY_STEP_MINUTES * 60;
			String did = care == Kitten.Care.NEW ? "New" : care == Kitten.Care.WOOL ? "Played" : "Stroked";
			text = did + " " + Format.duration(k.sinceCared()) + " ago; asks " + (warn > 0 ? "in ~" + Format.duration(warn) : "any moment");
			c = null;
		}
		return new Gauge("Love", text, (double) left / whole, c);
	}

	private static JComponent growth(KittenBook k)
	{
		long left = k.growLeft();
		if (left < 0)
		{
			return new Gauge("Grows", "Guess its age to see when it grows", 0, Theme.TEXT_FAINT);
		}
		double done = 1 - (double) left / Kitten.GROW_SECONDS;
		String text = left == 0 ? "A cat any moment now"
			: (k.isAgeKnown() ? "A cat in ~" : "A cat within ") + Format.duration(left) + " of following";
		return new Gauge("Grows", text, done, Theme.mix(Theme.GOLD, Theme.TEAL, done));
	}

	private static JComponent grown(KittenBook k, long nowMs)
	{
		if (k.getStage() == Kitten.Stage.OVERGROWN)
		{
			return new Gauge("Grown", "Overgrown: it no longer chases vermin", 1, Theme.GREEN);
		}
		long cat = k.getCatSeconds();
		String ago = k.getGrownAt() > 0 ? "Grew up " + Format.duration((nowMs - k.getGrownAt()) / 1000) + " ago" : "A grown cat";
		if (cat < 0)
		{
			return new Gauge("Grown", ago, 1, Theme.GREEN);
		}
		return new Gauge("Grown", ago, (double) cat / (Kitten.OVERGROWN_MAX_HOURS * 3600L), Theme.GREEN);
	}

	/** When a cat turns overgrown (wiki: another five to six hours of following), and that it needs nothing. */
	static String catNote(KittenBook k)
	{
		long cat = k.getCatSeconds();
		if (k.getStage() != Kitten.Stage.CAT || cat < 0)
		{
			return "A cat needs neither food nor attention.";
		}
		long min = Math.max(0, Kitten.OVERGROWN_MIN_HOURS * 3600L - cat);
		long max = Math.max(0, Kitten.OVERGROWN_MAX_HOURS * 3600L - cat);
		String when = max == 0 ? "Overgrown any moment" : "Overgrown after " + Format.shortDuration(min) + " to " + Format.shortDuration(max)
			+ " more following";
		return when + ". A cat needs neither food nor attention.";
	}

	/** Whose times these are, and that they count only while it follows. */
	static String footnote(KittenBook k)
	{
		String meals = k.isLearned("FED") ? "Meals last " + trim(k.hungerMinutes()) + " min for this kitten"
			: "Meals: the wiki's " + Kitten.HUNGER_WARN_MINUTES + " min until " + KittenBook.TRUST + " warnings are heard ("
			+ k.heard("FED") + " so far)";
		return meals + ". Time counts only while it follows you.";
	}

	private static String trim(double m)
	{
		double r = Math.round(m * 2) / 2.0;
		return r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r);
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/** The stage's name with where it is at the right. */
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
				Ui.shadowed(g, right, getWidth() - rw + 6, 12, Theme.TEXT_MUTED);
			}
			g.setFont(Theme.body());
			Ui.shadowed(g, Ui.clip(text, g.getFontMetrics(), getWidth() - rw), 0, 12, Theme.GOLD_BRIGHT);
			g.dispose();
		}
	}

	/** What a need says across the card, then its name in small caps beside a bar of what is left. */
	private static final class Gauge extends JComponent
	{
		private static final int LABEL = 40;
		private final String label;
		private final String text;
		private final double fill;
		private final Color color;

		Gauge(String label, String text, double fill, Color color)
		{
			this.label = label.toUpperCase();
			this.text = text;
			this.fill = Math.max(0, Math.min(1, fill));
			this.color = color;
			setPreferredSize(new Dimension(10, 27));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			Color c = color != null ? color : Theme.progressColor(fill);
			g.setFont(Theme.small());
			Ui.shadowed(g, Ui.clip(text, g.getFontMetrics(), getWidth()), 0, 10, color == Theme.TEXT_FAINT ? Theme.TEXT_MUTED : c);
			Ui.shadowed(g, label, 0, 24, Theme.GOLD_DIM);
			int y = 18;
			int x = LABEL;
			int w = getWidth() - LABEL - 1;
			g.setColor(Theme.INK_DEEP);
			g.fill(new RoundRectangle2D.Float(x, y, w, 5, 5, 5));
			if (fill > 0)
			{
				g.setPaint(new GradientPaint(0, y, Theme.mix(c, Color.WHITE, 0.2), 0, y + 5, Theme.mix(c, Theme.INK_DEEP, 0.3)));
				g.fill(new RoundRectangle2D.Float(x, y, Math.max(5, (float) (w * fill)), 5, 5, 5));
			}
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(x, y, w, 5, 5, 5));
			g.dispose();
		}
	}
}
