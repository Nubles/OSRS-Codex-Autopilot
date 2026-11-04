package com.codex.autopilot.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * The "Fed by" row: a small gilt pill for each Codex app whose data is in the plan, wrapping onto
 * a second line when they do not fit, so the player can see at a glance what the next step knows.
 */
public final class FedBy extends JComponent
{
	private static final int PILL_H = 15;
	private static final int GAP = 3;
	private static final int LABEL_W = 40;

	private final List<String> apps;
	private final int width;

	public FedBy(List<String> apps, int width)
	{
		this.apps = new ArrayList<>(apps);
		this.width = width;
		setOpaque(false);
		setToolTipText("The Codex apps whose shared data went into this plan");
		Dimension d = new Dimension(width, layout(null));
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		layout(g);
		g.dispose();
	}

	/** Lays the pills out, painting them when given a graphics, and returns the height used. */
	private int layout(Graphics2D g)
	{
		FontMetrics fm = getFontMetrics(Theme.small());
		if (g != null)
		{
			g.setFont(Theme.small());
			Ui.shadowed(g, "FED BY", 0, 11, Theme.GOLD_DIM);
		}
		int x = LABEL_W;
		int y = 0;
		for (String app : apps)
		{
			int w = fm.stringWidth(app) + 8;
			if (x + w > width && x > LABEL_W)
			{
				x = LABEL_W;
				y += PILL_H + GAP;
			}
			if (g != null)
			{
				RoundRectangle2D pill = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, PILL_H - 1, PILL_H - 1, PILL_H - 1);
				g.setColor(Theme.alpha(Theme.GOLD, 22));
				g.fill(pill);
				g.setColor(Theme.alpha(Theme.GOLD, 110));
				g.draw(pill);
				Ui.shadowed(g, app, x + 4, y + 11, Theme.GOLD);
			}
			x += w + GAP;
		}
		return y + PILL_H;
	}
}
