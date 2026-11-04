package com.codex.autopilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The money making runs Codex Money Pilot has clocked for this account, read and never written.
 * Money Pilot keeps them per RS profile under {@code codexmoneypilot/runs} as a list of
 * {@code {"m":"vorkath","t":startedAtMs,"ms":playedMs,"gp":profit}}. That list is Money Pilot's own
 * save, not a shared contract, so its shape is pinned by a test here; anything else reads as no runs.
 * The rate is worked out as Money Pilot does: the runs of the last {@link #DAYS} days, once a method
 * has {@link #ENOUGH_MS} of play, each run at least {@link #MIN_RUN_MS} and at most 30 days long.
 */
public final class MoneyRuns
{
	public static final String GROUP = "codexmoneypilot";
	public static final String KEY = "runs";
	static final int DAYS = 60;
	static final long ENOUGH_MS = 15 * 60_000L;
	static final long MIN_RUN_MS = 5 * 60_000L;
	static final long MAX_RUN_MS = 30L * 86_400_000L;

	/** Money Pilot's method ids and names, from its Methods table, so a step can say what to do. */
	private static final Map<String, String> NAMES = new HashMap<>();

	static
	{
		name("chambers-of-xeric", "Chambers of Xeric (Normal Mode)");
		name("vardorvis", "Killing Vardorvis");
		name("muspah-tbow", "Killing Phantom Muspah");
		name("kreearra", "Killing Kree'arra");
		name("demonic-gorillas", "Killing demonic gorillas");
		name("corrupted-gauntlet", "Completing The Corrupted Gauntlet");
		name("hydra", "Killing the Alchemical Hydra");
		name("vorkath", "Killing Vorkath");
		name("tombs-expert", "Tombs of Amascut (Expert)");
		name("zilyana", "Killing Commander Zilyana");
		name("graardor", "Killing General Graardor");
		name("corporeal-beast", "Killing the Corporeal Beast");
		name("revenants-msb", "Killing revenants");
		name("cerberus", "Killing Cerberus");
		name("zulrah", "Killing Zulrah");
		name("dagannoth-kings", "Killing Dagannoth Kings (solo tribrid)");
		name("scorpia", "Killing Scorpia");
		name("gauntlet", "Completing The Gauntlet");
		name("zombie-pirates", "Killing zombie pirates");
		name("brutal-black-dragons", "Killing brutal black dragons");
		name("rune-dragons", "Killing rune dragons");
		name("moons-of-peril", "Moons of Peril");
		name("kril", "Killing K'ril Tsutsaroth");
		name("lizardman-shamans", "Killing Lizardman shamans");
		name("green-dragons", "Killing green dragons");
		name("gargoyles", "Killing gargoyles");
		name("kraken", "Killing the Kraken");
		name("barrows", "Barrows");
		name("sarachnis", "Killing Sarachnis");
		name("king-black-dragon", "Killing the King Black Dragon");
		name("elves", "Pickpocketing elves");
		name("blood-runes", "Crafting blood runes");
		name("vyres", "Pickpocketing vyres");
		name("wrath-runes", "Crafting wrath runes");
		name("blood-runes-abyss", "Crafting blood runes through the Abyss");
		name("runite-bars-bf", "Smelting runite bars at Blast Furnace");
		name("rogues-castle", "Stealing from Rogues' Castle chests");
		name("adamantite-bars-bf", "Smelting adamantite bars at Blast Furnace");
		name("tan-leather", "Casting Tan Leather");
		name("mastering-mixology", "Mastering Mixology");
		name("death-runes-abyss", "Crafting death runes through the Abyss");
		name("astral-runes", "Crafting astral runes");
		name("runite-ore-f2p", "Mining runite ore (free-to-play)");
		name("sanfew-serum", "Making sanfew serum(4)");
		name("crystal-keys", "Creating crystal keys");
		name("unicorn-horns", "Grinding unicorn horns");
		name("ranarr-potions", "Making ranarr potions");
		name("black-dragonhide", "Tanning black dragonhide");
		name("snapdragon-potions", "Making snapdragon potions");
		name("birdhouse-traps", "Making birdhouse traps");
		name("mahogany-planks", "Making mahogany planks");
		name("bow-strings", "Spinning flax to bow strings");
		name("pie-shells", "Making pie shells");
		name("dyes", "Making dyes");
		name("cowhide", "Tanning cowhide");
		name("snape-grass", "Collecting snape grass");
		name("anti-dragon-shields-f2p", "Collecting anti-dragon shields (free-to-play)");
		name("cosmic-runes-f2p", "Collecting cosmic runes");
		name("wine-of-zamorak-f2p", "Collecting wine of zamorak (free-to-play)");
		name("bird-houses", "Bird house runs");
		name("herbs-ranarr", "Herb runs (ranarr weed)");
		name("herbs-snapdragon", "Herb runs (snapdragon)");
		name("herbs-torstol", "Herb runs (torstol)");
		name("miscellania", "Managing Miscellania");
	}

	private MoneyRuns()
	{
	}

	private static void name(String id, String name)
	{
		NAMES.put(id, name);
	}

	/** Money Pilot's name for a method, or the id written out ("rune-dragons" reads "Rune dragons"). */
	public static String name(String id)
	{
		String known = NAMES.get(id);
		if (known != null)
		{
			return known;
		}
		String words = id.replace('-', ' ').trim();
		return words.isEmpty() ? id : words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
	}

	public static List<Earner> read(Profile profile, Gson gson, long nowMs)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY), nowMs);
	}

	/** Each method's own rate, best first; empty when there are no runs or they cannot be read. */
	public static List<Earner> parse(Gson gson, String json, long nowMs)
	{
		List<Earner> out = new ArrayList<>();
		JsonElement root = Json.parse(gson, json);
		if (root == null || !root.isJsonArray())
		{
			return out;
		}
		long from = nowMs - DAYS * 86_400_000L;
		Map<String, long[]> sums = new LinkedHashMap<>();
		for (JsonElement e : (JsonArray) root)
		{
			JsonObject o = Json.object(e);
			String m = Json.text(o, "m");
			long t = Json.whole(o, "t", 0);
			long ms = Json.whole(o, "ms", 0);
			long gp = Json.whole(o, "gp", 0);
			if (m == null || t <= 0 || t < from || ms < MIN_RUN_MS || ms > MAX_RUN_MS)
			{
				continue;
			}
			long[] sum = sums.computeIfAbsent(m, k -> new long[2]);
			sum[0] += gp;
			sum[1] += ms;
		}
		for (Map.Entry<String, long[]> e : sums.entrySet())
		{
			long[] sum = e.getValue();
			if (sum[1] >= ENOUGH_MS && sum[0] > 0)
			{
				out.add(new Earner("money:" + e.getKey(), name(e.getKey()), perHour(sum[0], sum[1]), sum[1], "Money Pilot"));
			}
		}
		out.sort((a, b) -> Long.compare(b.getPerHour(), a.getPerHour()));
		return out;
	}

	static long perHour(long gp, long ms)
	{
		return ms <= 0 ? 0 : (long) (gp * (3_600_000.0 / ms));
	}
}
