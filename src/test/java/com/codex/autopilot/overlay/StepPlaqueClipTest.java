package com.codex.autopilot.overlay;

import com.codex.autopilot.core.Candidate;
import com.codex.autopilot.core.Tier;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * RuneLite's OverlayRenderer compares the clip after every overlay with
 * getClip().equals(...). An overlay that leaves the clip null makes that throw
 * for every overlay drawn after it, so the plaque must hand the clip back as it found it.
 */
public class StepPlaqueClipTest
{
	@Test
	public void leavesTheCallersClipAlone()
	{
		StepPlaque plaque = new StepPlaque();
		plaque.show(new Candidate("dailies", Candidate.Kind.DAILIES, Tier.READY, "Diary dailies", "Zaff",
			"Battlestaves are ready", "Coins", 5, 0.2, 0, 1, -1));

		BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Rectangle layer = new Rectangle(0, 0, 400, 300);
		g.setClip(layer);
		plaque.render(g);
		assertEquals(layer, g.getClip());
		g.dispose();
	}
}
