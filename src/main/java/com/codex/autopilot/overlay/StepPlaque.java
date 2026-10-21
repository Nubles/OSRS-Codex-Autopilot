package com.codex.autopilot.overlay;

import com.codex.autopilot.core.Bring;
import com.codex.autopilot.core.Candidate;
import com.codex.autopilot.ui.Glyph;
import com.codex.autopilot.ui.Sigil;
import com.codex.autopilot.ui.StepCard;
import com.codex.autopilot.ui.Theme;
import com.codex.autopilot.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The plaque in the game view: the one best next step for the account in gilt letters, what
 * kind of step it is and how long it takes, the reason in a line or two and what to bring. A
 * stripe down its left edge takes the step's colour: ember when something is about to go to
 * waste, green for a ready chore, teal for the focused goal.
 */
public class StepPlaque extends Overlay
{
	private static final int PAD = 8;
	private static final int MIN_WIDTH = 200;
	private static final int MAX_WIDTH = 270;

	private volatile Candidate shown;

	public StepPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_HIGH);
	}

	/** The step to show; null hides the plaque. */
	public void show(Candidate c)
	{
		shown = c;
	}

	public Candidate getShown()
	{
		return shown;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Candidate c = shown;
		if (c == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		Color tint = Theme.tier(c.getTier());
		int icon = 18;
		int textX = PAD + 4 + icon + 8;
		String over = "NEXT: " + c.getOver().toUpperCase();
		String note = StepCard.paceNote(c);
		String time = StepCard.minutes(c) + (note == null ? "" : " (" + note + ")");
		int want = textX + Math.max(bold.stringWidth(c.getTitle()), Math.max(small.stringWidth(c.getWhy()) / 2 + 20,
			small.stringWidth(over) + small.stringWidth(time) + 12)) + PAD;
		int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, want));
		int room = width - textX - PAD;
		String[] titleLines = wrap(c.getTitle(), bold, room, 2);
		String[] why = wrap(c.getWhy(), small, room, 3);
		String[] bring = wrap(c.getBring(), small, room, 2);
		int height = PAD + small.getHeight() + titleLines.length * bold.getHeight() + why.length * small.getHeight()
			+ bring.length * small.getHeight() + 8;

		Shape shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 238), 0, height, Theme.alpha(Theme.INK_DEEP, 238)));
		g.fill(shape);

		// The tier stripe, inside the rounded edge.
		g.setClip(shape);
		g.setPaint(new GradientPaint(0, 0, tint, 0, height, Theme.alpha(tint, 80)));
		g.fillRect(0, 0, 3, height);
		g.setClip(null);

		g.setColor(Theme.alpha(Theme.GOLD, 150));
		g.draw(shape);
		g.setColor(Theme.alpha(Color.WHITE, 14));
		g.drawLine(6, 1, width - 6, 1);

		Sigil.paint(g, PAD + 4, PAD + 1, icon);
		Glyph.paint(g, c.getKind(), PAD + 4 + 3, PAD + icon + 6, icon - 6, tint);

		int y = PAD + small.getAscent();
		g.setFont(Theme.small());
		int tw = small.stringWidth(time);
		Ui.shadowed(g, Ui.clip(over, small, room - tw - 8), textX, y, Theme.TEXT_MUTED);
		Ui.shadowed(g, time, width - PAD - tw, y, Theme.GOLD_DIM);
		g.setFont(Theme.title());
		for (String line : titleLines)
		{
			y += bold.getHeight();
			Ui.shadowed(g, line, textX, y, Theme.GOLD_BRIGHT);
		}
		g.setFont(Theme.small());
		for (String line : why)
		{
			y += small.getHeight();
			Ui.shadowed(g, line, textX, y, Theme.TEXT);
		}
		for (String line : bring)
		{
			y += small.getHeight();
			Ui.shadowed(g, line, textX, y, Bring.isShort(c.getBring()) ? Theme.EMBER : Theme.TEAL);
		}
		return new Dimension(width, height);
	}

	static String[] wrap(String text, FontMetrics fm, int width, int maxLines)
	{
		if (text == null || text.isEmpty())
		{
			return new String[0];
		}
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		String[] words = text.split(" ");
		int i = 0;
		for (; i < words.length; i++)
		{
			String word = words[i];
			String trial = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(trial) > width && line.length() > 0)
			{
				out.add(line.toString());
				line = new StringBuilder(word);
				if (out.size() == maxLines)
				{
					break;
				}
			}
			else
			{
				line = new StringBuilder(trial);
			}
		}
		if (out.size() < maxLines)
		{
			if (line.length() > 0)
			{
				out.add(Ui.clip(line.toString(), fm, width));
			}
		}
		else
		{
			// Ran out of lines with words to spare: end the last one with an ellipsis.
			out.set(maxLines - 1, Ui.clip(out.get(maxLines - 1) + " ...", fm, width));
		}
		return out.toArray(new String[0]);
	}
}
