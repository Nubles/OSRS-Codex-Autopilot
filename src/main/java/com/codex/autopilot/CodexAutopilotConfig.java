package com.codex.autopilot;

import com.codex.autopilot.core.Budget;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CodexAutopilotConfig.GROUP)
public interface CodexAutopilotConfig extends Config
{
	String GROUP = "codexautopilot";

	@ConfigSection(name = "Display", description = "Where the next step shows", position = 0)
	String DISPLAY = "display";

	@ConfigSection(name = "Sources", description = "What Autopilot reads to find your next step", position = 1)
	String SOURCES = "sources";

	@ConfigSection(name = "Priorities", description = "How steps are put in order", position = 2)
	String PRIORITIES = "priorities";

	@ConfigSection(name = "Notifications", description = "Heads-up notifications through RuneLite's notifier", position = 3)
	String NOTIFICATIONS = "notifications";

	@ConfigSection(name = "Codex suite", description = "What the other Codex apps share that Autopilot plans with", position = 4)
	String SUITE = "suite";

	/** The time budget the panel's selector sets. */
	String BUDGET_KEY = "timeBudget";

	@ConfigItem(
		keyName = "plaque",
		name = "Show the plaque",
		description = "Show the next step on a plaque in the game view",
		section = DISPLAY,
		position = 0
	)
	default boolean plaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "quiet",
		name = "Quiet mode",
		description = "Only show steps about to go to waste: a daily before the reset, a diseased patch, the kingdom before its next drop",
		section = DISPLAY,
		position = 1
	)
	default boolean quiet()
	{
		return false;
	}

	@ConfigItem(
		keyName = "stepAside",
		name = "Step aside in minigames",
		description = "Hide the plaque in the Motherlode Mine, at the Blast Furnace and on Mahogany Homes contracts, where Codex Minigame Pilot guides",
		section = DISPLAY,
		position = 2
	)
	default boolean stepAside()
	{
		return true;
	}

	@ConfigItem(
		keyName = BUDGET_KEY,
		name = "Time budget",
		description = "Plan only what fits in this long: the panel and plaque show the best set of steps for the time, things going to waste first."
			+ " Also set from the top of the panel",
		section = DISPLAY,
		position = 3
	)
	default Budget timeBudget()
	{
		return Budget.NONE;
	}

	@ConfigItem(
		keyName = "chores",
		name = "Bird houses and patches",
		description = "Read bird houses, seaweed, special trees and special patches from RuneLite's Time Tracking plugin",
		section = SOURCES,
		position = 0
	)
	default boolean chores()
	{
		return true;
	}

	@ConfigItem(
		keyName = "herbs",
		name = "Herb runs",
		description = "Read herb patches from RuneLite's Time Tracking plugin, with the allotments and flower patches beside them",
		section = SOURCES,
		position = 1
	)
	default boolean herbs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hops",
		name = "Hops runs",
		description = "Read hops patches from RuneLite's Time Tracking plugin",
		section = SOURCES,
		position = 2
	)
	default boolean hops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dailies",
		name = "Diary dailies",
		description = "Battlestaves, essence, flax, sand and the other diary dailies, and finished farming contracts",
		section = SOURCES,
		position = 3
	)
	default boolean dailies()
	{
		return true;
	}

	@ConfigItem(
		keyName = "kingdom",
		name = "Kingdom of Miscellania",
		description = "Read approval and the coffer from RuneLite's Kingdom of Miscellania plugin",
		section = SOURCES,
		position = 4
	)
	default boolean kingdom()
	{
		return true;
	}

	@ConfigItem(
		keyName = "navigator",
		name = "Codex Navigator goals",
		description = "Add the next step toward the goals you pinned in Codex Navigator",
		section = SOURCES,
		position = 5
	)
	default boolean navigator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankSteward",
		name = "Codex Bank Steward bank",
		description = "Check what a chore needs against the bank Codex Bank Steward last saw",
		section = SOURCES,
		position = 6
	)
	default boolean bankSteward()
	{
		return true;
	}

	@ConfigItem(
		keyName = "goalFirst",
		name = "Goal first",
		description = "Put your focused goal ahead of ready chores that lose little by waiting (trees, dailies with hours left, the kingdom)."
			+ " Anything about to go to waste and full bird houses still come first",
		section = PRIORITIES,
		position = 0
	)
	default boolean goalFirst()
	{
		return false;
	}

	@ConfigItem(
		keyName = "wasteHours",
		name = "Going to waste within (hours)",
		description = "How close a loss has to be before its step jumps to the front",
		section = PRIORITIES,
		position = 1
	)
	@Range(min = 1, max = 12)
	default int wasteHours()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "approvalTarget",
		name = "Kingdom approval target",
		description = "Suggest a visit to Miscellania when approval falls below this",
		section = PRIORITIES,
		position = 2
	)
	@Range(max = 100)
	@Units(Units.PERCENT)
	default int approvalTarget()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "cofferSlack",
		name = "Coffer short by",
		description = "Suggest topping up the coffer once it is this far below a full daily payout",
		section = PRIORITIES,
		position = 3
	)
	@Range(max = 7_500_000)
	default int cofferSlack()
	{
		return 150_000;
	}

	@ConfigItem(
		keyName = "learnPace",
		name = "Learn my pace",
		description = "Time your bird house, herb and other chore runs and show your own pace once three runs of a kind are timed."
			+ " Kept in this plugin's settings for the account; switch off to go back to the estimates",
		section = PRIORITIES,
		position = 4
	)
	default boolean learnPace()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notify",
		name = "Notifications",
		description = "Send heads-up notifications through RuneLite's notifier, which has its own settings for when and how they show",
		section = NOTIFICATIONS,
		position = 0
	)
	default boolean notifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyWaste",
		name = "Going to waste",
		description = "Notify once when something starts going to waste: a diseased patch, dailies before the reset, the kingdom",
		section = NOTIFICATIONS,
		position = 1
	)
	default boolean notifyWaste()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyReady",
		name = "Chores ready",
		description = "Notify once when a chore trip becomes ready, such as full bird houses or a herb run",
		section = NOTIFICATIONS,
		position = 2
	)
	default boolean notifyReady()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wayfinder",
		name = "Codex Wayfinder travel",
		description = "Price the journey to each step with the teleports Codex Wayfinder says this account has, and show the way: \"via Ardougne cloak, ~40s\"",
		section = SUITE,
		position = 0
	)
	default boolean wayfinder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moneyPilot",
		name = "Codex Money Pilot rates",
		description = "Use the money makers Codex Money Pilot has clocked for you, at your own rate",
		section = SUITE,
		position = 1
	)
	default boolean moneyPilot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gearRoadmap",
		name = "Codex Gear Roadmap goal",
		description = "When the upgrade Codex Gear Roadmap is saving toward is short of coins, offer a step to earn them",
		section = SUITE,
		position = 2
	)
	default boolean gearRoadmap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ledger",
		name = "Codex Ledger coins",
		description = "Count your coins and what you hold across every place Codex Ledger has seen",
		section = SUITE,
		position = 3
	)
	default boolean ledger()
	{
		return true;
	}

	@ConfigItem(
		keyName = "quartermaster",
		name = "Codex Quartermaster kits",
		description = "Check the kit chosen in Codex Quartermaster, and say when it needs restocking",
		section = SUITE,
		position = 4
	)
	default boolean quartermaster()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bossPilot",
		name = "Codex Boss Pilot",
		description = "A step for the boss pinned in Codex Boss Pilot, and your loot an hour there as a money maker",
		section = SUITE,
		position = 5
	)
	default boolean bossPilot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "roundsDailies",
		name = "Codex Rounds dailies",
		description = "While logged out, show the dailies as Codex Rounds last saw them today",
		section = SUITE,
		position = 6
	)
	default boolean roundsDailies()
	{
		return true;
	}

	@ConfigItem(
		keyName = "logInsight",
		name = "Codex Log Insight wanted slots",
		description = "A step for the first collection log slot you marked wanted in Codex Log Insight",
		section = SUITE,
		position = 7
	)
	default boolean logInsight()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sharePlan",
		name = "Share the plan",
		description = "Save the top steps for the other Codex apps, so Codex Home can show them on its Today tab",
		section = SUITE,
		position = 8
	)
	default boolean sharePlan()
	{
		return true;
	}
}
