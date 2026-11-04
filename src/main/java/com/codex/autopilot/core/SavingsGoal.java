package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The upgrade Codex Gear Roadmap is saving toward, read and never written. Gear Roadmap keeps it per
 * RS profile under {@code codexgearroadmap/goal} as {@code {"name":"Twisted bow","ids":[20997]}}: the
 * upgrade's name and the item ids of its pieces. Codex Home reads the same key; it is Gear Roadmap's
 * own save, so its shape is pinned by a test here.
 */
public final class SavingsGoal
{
	public static final String GROUP = "codexgearroadmap";
	public static final String KEY = "goal";

	private final String name;
	private final List<Integer> ids;

	public SavingsGoal(String name, List<Integer> ids)
	{
		this.name = name;
		this.ids = Collections.unmodifiableList(new ArrayList<>(ids));
	}

	public static SavingsGoal read(Profile profile, Gson gson)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY));
	}

	/** The goal, or null without a name and at least one piece. */
	public static SavingsGoal parse(Gson gson, String json)
	{
		JsonObject o = Json.object(Json.parse(gson, json));
		String name = Json.text(o, "name");
		JsonArray list = Json.array(o, "ids");
		if (name == null || list == null)
		{
			return null;
		}
		List<Integer> ids = new ArrayList<>();
		for (JsonElement e : list)
		{
			long id = BankSnapshot.number(e, -1);
			if (id > 0 && id <= Integer.MAX_VALUE && !ids.contains((int) id))
			{
				ids.add((int) id);
			}
		}
		return ids.isEmpty() ? null : new SavingsGoal(name, ids);
	}

	public String getName()
	{
		return name;
	}

	public List<Integer> getIds()
	{
		return ids;
	}
}
