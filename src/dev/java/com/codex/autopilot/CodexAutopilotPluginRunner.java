package com.codex.autopilot;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexAutopilotPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexAutopilotPlugin.class);
		RuneLite.main(args);
	}
}
