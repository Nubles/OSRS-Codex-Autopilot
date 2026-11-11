package com.codex.autopilot.ui;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * A card narrows the clip for its accent stripe and puts back the clip it was given: it never
 * clears it, so nothing it draws lands outside where Swing asked it to paint.
 */
public class CardClipTest
{
	@Test
	public void theClipIsPutBackNotCleared()
	{
		Card card = new Card(new BorderLayout()).accent(Theme.GOLD).highlighted(true);
		card.setSize(100, 40);
		BufferedImage img = new BufferedImage(100, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle clip = new Rectangle(0, 0, 20, 20);
		g.setClip(clip);
		card.paint(g);
		assertEquals(clip, g.getClipBounds());
		g.dispose();
		// The rim along the bottom and the right lies outside the clip: none of it was drawn.
		for (int x = 20; x < 100; x++)
		{
			assertEquals("x " + x, 0, img.getRGB(x, 39) >>> 24);
		}
		for (int y = 20; y < 40; y++)
		{
			assertEquals("y " + y, 0, img.getRGB(99, y) >>> 24);
		}
	}
}
