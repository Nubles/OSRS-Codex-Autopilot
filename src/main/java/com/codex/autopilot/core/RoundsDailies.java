package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The dailies as Codex Rounds last saw them, for while the player is logged out and the game cannot be
 * read. Rounds keeps its memory per RS profile under {@code codexrounds/memory}; Autopilot reads only
 * {@code dailies}: {@code {"ZAFF":{"unlocked":true,"claimable":true,"seenAt":unixSecond,"reward":"..."}}},
 * keyed by the same daily names as Autopilot's own list. Only what was seen since the last reset at
 * midnight UTC counts. It is Rounds' own save, so its shape is pinned by a test here.
 */
public final class RoundsDailies
{
	public static final String GROUP = "codexrounds";
	public static final String KEY = "memory";

	private RoundsDailies()
	{
	}

	public static List<DailyState> read(Profile profile, Gson gson, long now)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY), now);
	}

	/** The dailies seen today, or null when there is no memory or nothing from today. */
	public static List<DailyState> parse(Gson gson, String json, long now)
	{
		JsonObject dailies = Json.object(Json.object(Json.parse(gson, json)), "dailies");
		if (dailies == null)
		{
			return null;
		}
		List<DailyState> out = new ArrayList<>();
		long today = now / 86_400;
		for (Map.Entry<String, JsonElement> e : dailies.entrySet())
		{
			Daily d;
			try
			{
				d = Daily.valueOf(e.getKey());
			}
			catch (IllegalArgumentException ex)
			{
				continue;
			}
			JsonObject snap = Json.object(e.getValue());
			long seen = Json.whole(snap, "seenAt", 0);
			// The contract does not reset at midnight: a finished one waits however long ago it was seen.
			if (snap == null || seen <= 0 || seen > now + 60 || (d.resetsDaily() && seen / 86_400 != today))
			{
				continue;
			}
			boolean unlocked = Json.bool(snap, "unlocked");
			boolean claimable = unlocked && Json.bool(snap, "claimable");
			String reward = Json.text(snap, "reward");
			out.add(new DailyState(d, unlocked, claimable, reward == null ? "" : reward, null));
		}
		return out.isEmpty() ? null : out;
	}
}
