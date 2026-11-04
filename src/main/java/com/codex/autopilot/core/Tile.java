package com.codex.autopilot.core;

/** A game tile: world x, y and plane. */
public final class Tile
{
	private final int x;
	private final int y;
	private final int plane;

	public Tile(int x, int y, int plane)
	{
		this.x = x;
		this.y = y;
		this.plane = plane;
	}

	public static Tile of(int x, int y)
	{
		return new Tile(x, y, 0);
	}

	public static Tile of(int x, int y, int plane)
	{
		return new Tile(x, y, plane);
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getPlane()
	{
		return plane;
	}

	/** Tiles apart, the way the game counts a diagonal step as one: the larger of the two gaps. Planes are not counted. */
	public int distance(Tile o)
	{
		return Math.max(Math.abs(x - o.x), Math.abs(y - o.y));
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Tile))
		{
			return false;
		}
		Tile t = (Tile) o;
		return x == t.x && y == t.y && plane == t.plane;
	}

	@Override
	public int hashCode()
	{
		return (x * 31 + y) * 4 + plane;
	}

	@Override
	public String toString()
	{
		return x + " " + y + " " + plane;
	}
}
