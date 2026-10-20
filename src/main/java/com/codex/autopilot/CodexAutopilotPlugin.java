package com.codex.autopilot;

import com.codex.autopilot.core.Activity;
import com.codex.autopilot.core.Advisor;
import com.codex.autopilot.core.BankSnapshot;
import com.codex.autopilot.core.Candidate;
import com.codex.autopilot.core.Dailies;
import com.codex.autopilot.core.DailyState;
import com.codex.autopilot.core.Format;
import com.codex.autopilot.core.GoalBoard;
import com.codex.autopilot.core.Plan;
import com.codex.autopilot.core.Profile;
import com.codex.autopilot.core.Session;
import com.codex.autopilot.core.Settings;
import com.codex.autopilot.core.Tier;
import com.codex.autopilot.core.Vars;
import com.codex.autopilot.overlay.StepPlaque;
import com.codex.autopilot.ui.Actions;
import com.codex.autopilot.ui.AutopilotPanel;
import com.codex.autopilot.ui.Sigil;
import com.codex.autopilot.ui.View;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * One next step for the whole account. Autopilot reads what RuneLite and the other Codex plugins
 * already know (Time Tracking's patches and bird houses, the Kingdom of Miscellania, the diary
 * dailies, Codex Navigator's goals and Codex Bank Steward's bank), ranks it all by what is lost
 * by waiting, and keeps the best step on a plaque and in its panel. It only reads: it never
 * clicks, changes menus, writes another plugin's data or sends anything anywhere.
 */
@PluginDescriptor(
	name = "Codex Autopilot",
	description = "One next step for your whole account: what is ready, what is about to go to waste and your pinned goal, in one order",
	tags = {"next", "step", "todo", "birdhouse", "farming", "daily", "kingdom", "miscellania", "goal", "navigator", "codex"}
)
public class CodexAutopilotPlugin extends Plugin
{
	/** Every few ticks the plan is worked out again, so countdowns move and new readings land. */
	private static final int EVALUATE_TICKS = 5;
	private static final int QUEST_TICKS = 100;
	/** How close a Mahogany Homes contractor has to be to count as being on a contract. */
	private static final int CONTRACTOR_RANGE = 12;
	/** The RuneLite config key that says whether Codex Minigame Pilot is switched on. */
	private static final String MINIGAME_PILOT_KEY = "codexminigamepilotplugin";
	private static final String TIME_TRACKING = "timetracking";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private CodexAutopilotConfig config;

	private final StepPlaque plaque = new StepPlaque();
	private final Session session = new Session();

	private AutopilotPanel panel;
	private NavigationButton navButton;

	private int ticks;
	private volatile boolean skipped;
	private List<DailyState> dailies;
	private boolean royalTrouble;
	private Activity activity = Activity.NONE;

	// Parsed only when the saved text changes.
	private String boardJson;
	private GoalBoard board;
	private String bankJson;
	private BankSnapshot bank;

	@Provides
	CodexAutopilotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexAutopilotConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new AutopilotPanel(new PanelActions());
		navButton = NavigationButton.builder()
			.tooltip("Codex Autopilot")
			.icon(Sigil.icon(16))
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(plaque);
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				readLive(true);
			}
			evaluate();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(plaque);
		clientToolbar.removeNavigation(navButton);
		plaque.show(null);
		panel = null;
		session.clear();
		skipped = false;
		dailies = null;
		activity = Activity.NONE;
		boardJson = null;
		board = null;
		bankJson = null;
		bank = null;
	}

	// ---- Events ----

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		ticks++;
		if (ticks % EVALUATE_TICKS != 0)
		{
			return;
		}
		readLive(ticks % QUEST_TICKS == 0);
		evaluate();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			dailies = null;
			activity = Activity.NONE;
			evaluate();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			ticks = 0;
			clientThread.invokeLater(() ->
			{
				readLive(true);
				evaluate();
			});
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// Another account: what was set aside belonged to the last one.
		session.clear();
		skipped = false;
		dailies = null;
		clientThread.invokeLater(this::evaluate);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		String group = event.getGroup();
		if (CodexAutopilotConfig.GROUP.equals(group) || TIME_TRACKING.equals(group) || GoalBoard.GROUP.equals(group)
			|| BankSnapshot.GROUP.equals(group) || "kingdomofmiscellania".equals(group))
		{
			clientThread.invokeLater(this::evaluate);
		}
	}

	// ---- Reading ----

	/** Reads what only the live game knows: the dailies, Royal Trouble and where the player is. On the client thread. */
	private void readLive(boolean quests)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		Vars vars = new Vars()
		{
			@Override
			public int varbit(int id)
			{
				return client.getVarbitValue(id);
			}

			@Override
			public int varp(int id)
			{
				return client.getVarpValue(id);
			}
		};
		dailies = Dailies.read(vars);
		if (quests)
		{
			try
			{
				royalTrouble = Quest.ROYAL_TROUBLE.getState(client) == QuestState.FINISHED;
			}
			catch (RuntimeException e)
			{
				royalTrouble = false;
			}
		}
		activity = readActivity();
	}

	private Activity readActivity()
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return Activity.NONE;
		}
		WorldPoint at = me.getWorldLocation();
		List<String> near = new ArrayList<>();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && npc.getName() != null && npc.getWorldLocation().distanceTo(at) <= CONTRACTOR_RANGE)
			{
				near.add(npc.getName());
			}
		}
		return Activity.of(at.getRegionID(), near);
	}

	private Settings settings()
	{
		return Settings.defaults()
			.chores(config.chores())
			.dailies(config.dailies())
			.kingdom(config.kingdom())
			.navigator(config.navigator())
			.bankSteward(config.bankSteward())
			.quiet(config.quiet())
			.goalFirst(config.goalFirst())
			.approvalTarget(config.approvalTarget())
			.cofferSlack(config.cofferSlack())
			.wasteHorizon(config.wasteHours() * 3600L);
	}

	/** Works the plan out again from everything read and hands it to the plaque and the panel. */
	private void evaluate()
	{
		long now = Instant.now().getEpochSecond();
		boolean known = configManager.getRSProfileKey() != null;
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		Settings s = settings();
		Profile profile = (group, key) -> configManager.getRSProfileConfiguration(group, key);

		Advisor.Inputs in = new Advisor.Inputs();
		Plan plan = Plan.EMPTY;
		String nextUp = null;
		if (known)
		{
			in = Advisor.Inputs.read(new OnlyCore(profile), gson, now, royalTrouble);
			in.board = board(profile);
			in.bank = bank(profile);
			in.dailies = loggedIn ? dailies : null;
			plan = Advisor.plan(in, s, session, now);
			nextUp = Advisor.nextUp(in, s, now);
		}

		boolean aside = config.stepAside() && activity != Activity.NONE;
		Candidate top = plan.top();
		plaque.show(config.plaque() && loggedIn && !aside ? top : null);

		AutopilotPanel p = panel;
		if (p == null)
		{
			return;
		}
		View view = new View(loggedIn, known, plan, config.stepAside() ? activity : Activity.NONE,
			"true".equals(configManager.getConfiguration("runelite", MINIGAME_PILOT_KEY)), nextUp, s.isQuiet(), skipped,
			sources(in, s, plan, loggedIn, now));
		SwingUtilities.invokeLater(() -> p.update(view));
	}

	private GoalBoard board(Profile profile)
	{
		String json = Profile.safe(profile, GoalBoard.GROUP, GoalBoard.KEY);
		if (!Objects.equals(json, boardJson))
		{
			boardJson = json;
			board = GoalBoard.parse(gson, json);
		}
		return board;
	}

	private BankSnapshot bank(Profile profile)
	{
		String json = Profile.safe(profile, BankSnapshot.GROUP, BankSnapshot.KEY);
		if (!Objects.equals(json, bankJson))
		{
			bankJson = json;
			bank = BankSnapshot.parse(gson, json);
		}
		return bank;
	}

	private List<View.Source> sources(Advisor.Inputs in, Settings s, Plan plan, boolean loggedIn, long now)
	{
		List<View.Source> out = new ArrayList<>();
		int spots = in.seen == null ? 0 : in.seen.size();
		out.add(!s.isChores() ? off("Time Tracking")
			: new View.Source("Time Tracking", spots == 0 ? "Nothing seen" : spots + " tracked", spots > 0));
		long open = in.dailies == null ? 0 : in.dailies.stream().filter(DailyState::isClaimable).count();
		out.add(!s.isDailies() ? off("Dailies")
			: !loggedIn ? new View.Source("Dailies", "Log in", false)
			: new View.Source("Dailies", open == 0 ? "All claimed" : open + " open", true));
		out.add(!s.isKingdom() ? off("Kingdom")
			: in.kingdom == null ? new View.Source("Kingdom", "Not seen", false)
			: new View.Source("Kingdom", in.kingdom.approvalPercent() + "% approval", true));
		out.add(!s.isNavigator() ? off("Navigator")
			: in.board == null ? new View.Source("Navigator", "No goals", false)
			: new View.Source("Navigator", in.board.open() == 1 ? "1 goal" : in.board.open() + " goals", true));
		out.add(!s.isBankSteward() ? off("Bank Steward")
			: in.bank == null ? new View.Source("Bank Steward", "No bank", false)
			: new View.Source("Bank Steward", in.bank.getSavedAt() <= 0 ? "Bank known"
				: Format.shortDuration(now - in.bank.getSavedAt() / 1000) + " ago", true));
		long wasting = plan.count(Tier.WASTING);
		out.add(new View.Source("At risk", wasting == 0 ? "Nothing" : String.valueOf(wasting), wasting > 0));
		return out;
	}

	private static View.Source off(String label)
	{
		return new View.Source(label, "Off", false);
	}

	/**
	 * Reads only RuneLite core's own groups (Time Tracking, Kingdom of Miscellania), so the Codex
	 * readers above can cache their parsed JSON separately.
	 */
	private static final class OnlyCore implements Profile
	{
		private final Profile inner;

		OnlyCore(Profile inner)
		{
			this.inner = inner;
		}

		@Override
		public String get(String group, String key)
		{
			return GoalBoard.GROUP.equals(group) || BankSnapshot.GROUP.equals(group) ? null : inner.get(group, key);
		}
	}

	private final class PanelActions implements Actions
	{
		@Override
		public void skip(String key)
		{
			session.skip(key);
			skipped = true;
			clientThread.invokeLater(CodexAutopilotPlugin.this::evaluate);
		}

		@Override
		public void snooze(String key)
		{
			session.snooze(key, Instant.now().getEpochSecond());
			clientThread.invokeLater(CodexAutopilotPlugin.this::evaluate);
		}

		@Override
		public void restore()
		{
			session.clear();
			skipped = false;
			clientThread.invokeLater(CodexAutopilotPlugin.this::evaluate);
		}
	}
}
