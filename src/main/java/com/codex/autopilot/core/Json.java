package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Careful reading of another app's saved JSON: every helper answers null or a fallback rather than throwing. */
final class Json
{
	private Json()
	{
	}

	static JsonElement parse(Gson gson, String json)
	{
		if (gson == null || json == null || json.trim().isEmpty())
		{
			return null;
		}
		try
		{
			return gson.fromJson(json, JsonElement.class);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	static JsonObject object(JsonElement e)
	{
		return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
	}

	static JsonObject object(JsonObject o, String key)
	{
		return o == null ? null : object(o.get(key));
	}

	static JsonArray array(JsonObject o, String key)
	{
		JsonElement e = o == null ? null : o.get(key);
		return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
	}

	/** A non-empty, trimmed string, or null. */
	static String text(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString())
		{
			return null;
		}
		String s = e.getAsString().trim();
		return s.isEmpty() ? null : s;
	}

	static String text(JsonObject o, String key)
	{
		return o == null ? null : text(o.get(key));
	}

	/** A finite number, or the fallback. */
	static double real(JsonElement e, double fallback)
	{
		if (e == null || !e.isJsonPrimitive())
		{
			return fallback;
		}
		JsonPrimitive p = e.getAsJsonPrimitive();
		if (!p.isNumber())
		{
			return fallback;
		}
		double d = p.getAsDouble();
		return Double.isNaN(d) || Double.isInfinite(d) ? fallback : d;
	}

	static double real(JsonObject o, String key, double fallback)
	{
		return o == null ? fallback : real(o.get(key), fallback);
	}

	static long whole(JsonObject o, String key, long fallback)
	{
		return o == null ? fallback : BankSnapshot.number(o.get(key), fallback);
	}

	static boolean bool(JsonObject o, String key)
	{
		JsonElement e = o == null ? null : o.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();
	}

	/** The saved version: 1 when it is missing, -1 when it is not a number. */
	static long version(JsonObject o)
	{
		return BankSnapshot.version(o);
	}
}
