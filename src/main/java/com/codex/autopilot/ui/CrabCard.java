package com.codex.autopilot.ui;

import com.codex.autopilot.core.CrabLog;
import com.codex.autopilot.core.Crabs;
import com.codex.autopilot.core.Format;
import com.codex.autopilot.core.GemstoneCrab;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The Gemstone Crab at a glance, while the player is at one of its mines: which mine, its bar and
 * the time left on it, where it goes next (as far as the player's own sightings tell), the shell
 * while it is theirs, and their own xp, gems and time there.
 */
public final class CrabCard
{
	private static final int LABEL = 44;

	private CrabCard()
	{
	}

	/** "48k xp/hr", or the mine. */
	public static String summary(CrabLog log)
	{
		long xp = log.xpPerHour();
		if (xp >= 0)
		{
			return Format.number(xp) + " xp/hr";
		}
		GemstoneCrab.Mine m = log.getMine() != null ? log.getMine() : log.getNear();
		return m == null ? null : m.getLabel();
	}

	public static JComponent card(CrabLog log, int width)
	{
		GemstoneCrab.Mine m = log.getMine();
		int shell = log.shellSecondsLeft();
		Card card = new Card(new BorderLayout()).accent(shell > 0 ? Theme.EMBER : m != null ? Theme.TEAL : Theme.GOLD);
		StackPanel body = new StackPanel(5, 0);
		if (m != null)
		{
			int left = log.secondsLeft();
			body.add(new Heading(m.getLabel(), left >= 0 ? Crabs.leftText(left, log.leftFrom()) : "bar not shown yet"));
			body.add(new Clock(log.getRatio(), left, log.leftFrom()));
			body.add(field("Next", Crabs.next(log, m).substring(3), log.next(m) != null ? Theme.GREEN : Theme.TEXT_MUTED, width));
		}
		else
		{
			GemstoneCrab.Mine from = log.getBurrowedFrom() != null ? log.getBurrowedFrom() : log.getNear();
			GemstoneCrab.Mine to = log.next(from);
			body.add(new Heading(log.getBurrowedFrom() != null ? "Burrowed" : "Not here", "at " + log.getNear().getPhrase()));
			body.add(field("Next", (to != null ? capital(to.getPhrase()) + ", " + log.moves(from, to) + " of " + log.movesFrom(from)
				+ " moves seen" : capital(Crabs.next(log, from).substring(3)) + "; the cave here leads to it"), to != null ? Theme.GREEN
				: Theme.TEXT_MUTED, width));
		}
		if (shell > 0)
		{
			body.add(field("Shell", "Yours: " + Format.plural(log.shellGemsLeft(), "gem", "gems") + " to mine, " + shell + "s left",
				Theme.EMBER, width));
		}
		else if (log.shellRefused())
		{
			body.add(field("Shell", "Not yours this time: only the top 16 by damage may mine it", Theme.TEXT_MUTED, width));
		}
		String pace = Crabs.pace(log);
		body.add(field("Pace", pace != null ? capital(pace.replace(" here", "")) : "Your own rates show after "
			+ CrabLog.TRUST_XP_MS / 60_000 + " min here (" + Format.duration(log.getTimedMs() / 1000) + " timed)",
			pace != null ? Theme.TEAL : Theme.TEXT_MUTED, width));
		if (log.totalGems() > 0)
		{
			body.add(new Gems(log));
		}
		String tally = tally(log);
		if (tally != null)
		{
			body.add(Ui.wrapped(tally, Theme.small(), Theme.TEXT_FAINT, width));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** "6 crabs · 5 shells · 1 hr 12 min here", leaving out what is still nothing. */
	static String tally(CrabLog log)
	{
		java.util.List<String> parts = new java.util.ArrayList<>();
		if (log.getCrabs() > 0)
		{
			parts.add(Format.plural(log.getCrabs(), "crab", "crabs"));
		}
		if (log.getShells() > 0 || log.getMissed() > 0)
		{
			parts.add(Format.plural(log.getShells(), "shell", "shells") + (log.getMissed() > 0 ? " (" + log.getMissed() + " not yours)" : ""));
		}
		if (log.getTimedMs() >= 60_000)
		{
			parts.add(Format.duration(log.getTimedMs() / 1000) + " here");
		}
		return parts.isEmpty() ? null : String.join(" · ", parts);
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static JComponent field(String label, String text, Color color, int width)
	{
		JPanel row = new JPanel(new BorderLayout(0, 0));
		row.setOpaque(false);
		row.add(new Label(label), BorderLayout.WEST);
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

	/** The mine with the time left at the right. */
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

	/** The crab's own bar, as the game shows it, with how the time left was found. */
	private static final class Clock extends JComponent
	{
		private final double ratio;
		private final int left;
		private final CrabLog.Left from;

		Clock(double ratio, int left, CrabLog.Left from)
		{
			this.ratio = ratio;
			this.left = left;
			this.from = from;
			setPreferredSize(new Dimension(10, 24));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(Theme.small());
			String text = ratio < 0 ? "Its bar shows once it is hit" : Math.round(ratio * 100) + "% of its bar left";
			Ui.shadowed(g, text, 0, 10, Theme.TEXT);
			String how;
			switch (from)
			{
				case MEASURED:
					how = "timed";
					break;
				case LEARNED:
					how = "your crabs";
					break;
				case WIKI:
					how = "wiki ~10 min";
					break;
				default:
					how = null;
			}
			if (how != null)
			{
				int hw = g.getFontMetrics().stringWidth(how);
				Ui.shadowed(g, how, getWidth() - hw, 10, Theme.TEXT_FAINT);
			}
			int y = 16;
			int w = getWidth() - 1;
			g.setColor(Theme.INK_DEEP);
			g.fill(new RoundRectangle2D.Float(0, y, w, 6, 6, 6));
			if (ratio > 0)
			{
				Color c = left >= 0 && left < 60 ? Theme.EMBER : Theme.TEAL;
				g.setPaint(new GradientPaint(0, y, Theme.mix(c, Color.WHITE, 0.2), 0, y + 6, Theme.mix(c, Theme.INK_DEEP, 0.3)));
				g.fill(new RoundRectangle2D.Float(0, y, Math.max(6, (float) (w * ratio)), 6, 6, 6));
			}
			g.setColor(Theme.STROKE_SOFT);
			g.draw(new RoundRectangle2D.Float(0, y, w, 6, 6, 6));
			g.dispose();
		}
	}

	/** Each gem mined so far: a small cut stone in its colour and how many. */
	private static final class Gems extends JComponent
	{
		private static final int CELL = 46;
		private final CrabLog log;

		Gems(CrabLog log)
		{
			this.log = log;
			setPreferredSize(new Dimension(10, 34));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(Theme.small());
			int perRow = Math.max(1, getWidth() / CELL);
			int i = 0;
			for (GemstoneCrab.Gem gem : GemstoneCrab.Gem.values())
			{
				int n = log.gems(gem);
				int x = (i % perRow) * CELL;
				int y = (i / perRow) * 17;
				Color c = colour(gem);
				if (n == 0)
				{
					c = Theme.mix(c, Theme.INK_DEEP, 0.7);
				}
				stone(g, x + 1, y + 2, c);
				Ui.shadowed(g, Format.number(n), x + 15, y + 12, n == 0 ? Theme.TEXT_FAINT : Theme.TEXT);
				i++;
			}
			g.dispose();
		}

		private static void stone(Graphics2D g, float x, float y, Color c)
		{
			Path2D p = new Path2D.Float();
			p.moveTo(x + 3, y);
			p.lineTo(x + 9, y);
			p.lineTo(x + 12, y + 4);
			p.lineTo(x + 6, y + 12);
			p.lineTo(x, y + 4);
			p.closePath();
			g.setPaint(new GradientPaint(x, y, Theme.mix(c, Color.WHITE, 0.35), x, y + 12, Theme.mix(c, Theme.INK_DEEP, 0.35)));
			g.fill(p);
			g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
			g.draw(p);
		}

		static Color colour(GemstoneCrab.Gem gem)
		{
			switch (gem)
			{
				case OPAL:
					return new Color(222, 214, 196);
				case JADE:
					return new Color(120, 176, 104);
				case RED_TOPAZ:
					return new Color(214, 112, 76);
				case SAPPHIRE:
					return new Color(84, 120, 210);
				case EMERALD:
					return new Color(64, 172, 104);
				case RUBY:
					return new Color(196, 52, 60);
				case DIAMOND:
					return new Color(236, 240, 244);
				default:
					return Theme.VIOLET;
			}
		}
	}
}
