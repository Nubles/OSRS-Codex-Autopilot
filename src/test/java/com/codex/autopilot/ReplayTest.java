package com.codex.autopilot;

import com.codex.replay.FakeGame;
import com.codex.replay.PluginHost;
import com.codex.replay.Replay;
import com.codex.replay.Rig;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Plays every script in {@code src/test/resources/replays} against the whole plugin, started
 * as RuneLite starts it; see {@link Replay} for the commands and {@link PluginHost} for the rest.
 *
 * <p>Autopilot's scripts add one command of their own, as the shared fake game has no follower:
 * {@code follower ID} makes the NPC with that id (an {@code npc} line put it there; RuneLite's
 * {@code NpcID} name or a number) the player's follower, and {@code follower none} takes it away. Nor has it
 * health bars: {@code bar SHARE} makes every NPC's bar show that share, 0 to 1, or none for -1.</p>
 */
@RunWith(Parameterized.class)
public class ReplayTest
{
	@Parameterized.Parameters(name = "{0}")
	public static List<String> scripts()
	{
		return Replay.scripts("/replays");
	}

	@Parameterized.Parameter
	public String script;

	@Test
	public void plays()
	{
		Replay.play(script, game -> withFollower(game, PluginHost.of(CodexAutopilotPlugin.class).apply(game)));
	}

	private static Rig withFollower(FakeGame game, Rig host)
	{
		return (Rig) Proxy.newProxyInstance(Rig.class.getClassLoader(), new Class<?>[]{Rig.class}, (proxy, m, args) ->
		{
			if ("command".equals(m.getName()) && "bar".equals(args[0]))
			{
				@SuppressWarnings("unchecked")
				List<String> a = (List<String>) args[1];
				bar(((PluginHost) host).plugin(), Double.parseDouble(a.get(0)));
				return true;
			}
			if ("command".equals(m.getName()) && "follower".equals(args[0]))
			{
				@SuppressWarnings("unchecked")
				List<String> a = (List<String>) args[1];
				follow(((PluginHost) host).plugin(), a.get(0));
				return true;
			}
			try
			{
				return m.invoke(host, args);
			}
			catch (InvocationTargetException e)
			{
				throw e.getCause();
			}
		});
	}

	/** Every NPC's health bar shows this share, or none at all for -1. */
	private static void bar(Object plugin, double share) throws ReflectiveOperationException
	{
		Function<NPC, Double> f = n -> share;
		Field field = CodexAutopilotPlugin.class.getDeclaredField("barOf");
		field.setAccessible(true);
		field.set(plugin, f);
	}

	private static void follow(Object plugin, String who) throws ReflectiveOperationException
	{
		Function<Client, NPC> f;
		if ("none".equals(who))
		{
			f = c -> null;
		}
		else
		{
			int id = who.matches("\\d+") ? Integer.parseInt(who) : NpcID.class.getField(who).getInt(null);
			f = c -> c.getNpcs().stream().filter(n -> n.getId() == id).findFirst().orElse(null);
		}
		Field field = CodexAutopilotPlugin.class.getDeclaredField("followerOf");
		field.setAccessible(true);
		field.set(plugin, f);
	}
}
