package com.codex.autopilot;

import com.codex.autopilot.core.Pace;
import com.codex.autopilot.core.SharedPlan;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** Saved settings keep their keys: a renamed key would quietly reset a player's choice. Add new keys here; never rename old ones. */
public class ConfigKeysTest
{
	@Test
	public void savedSettingsKeepTheirKeys()
	{
		assertEquals("codexautopilot", CodexAutopilotConfig.class.getAnnotation(ConfigGroup.class).value());
		Set<String> keys = new HashSet<>();
		for (Method m : CodexAutopilotConfig.class.getDeclaredMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				keys.add(item.keyName());
			}
		}
		assertEquals(new HashSet<>(Arrays.asList(
			"plaque", "quiet", "stepAside", "timeBudget",
			"chores", "herbs", "hops", "dailies", "kingdom", "rumours", "navigator", "bankSteward",
			"goalFirst", "wasteHours", "approvalTarget", "cofferSlack", "learnPace",
			"notify", "notifyWaste", "notifyReady",
			// The Codex suite.
			"wayfinder", "moneyPilot", "gearRoadmap", "ledger", "quartermaster", "bossPilot", "roundsDailies", "logInsight", "sharePlan",
			// The optional local record.
			"fieldNotes")),
			keys);
		// Saved per account beside the settings.
		assertEquals("paceV1", Pace.KEY);
		assertEquals("planV1", SharedPlan.KEY);
		assertEquals("rumourV1", com.codex.autopilot.core.RumourBook.KEY);
	}
}
