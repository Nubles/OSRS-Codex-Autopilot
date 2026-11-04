package com.codex.autopilot;

import com.codex.autopilot.notes.FieldNotes;
import com.codex.autopilot.core.Activity;
import com.codex.autopilot.core.Advisor;
import com.codex.autopilot.core.Atlas;
import com.codex.autopilot.core.BankSnapshot;
import com.codex.autopilot.core.BossLog;
import com.codex.autopilot.core.Budget;
import com.codex.autopilot.core.Candidate;
import com.codex.autopilot.core.Dailies;
import com.codex.autopilot.core.DailyState;
import com.codex.autopilot.core.Format;
import com.codex.autopilot.core.GoalBoard;
import com.codex.autopilot.core.Earner;
import com.codex.autopilot.core.Heads;
import com.codex.autopilot.core.Kits;
import com.codex.autopilot.core.MoneyRuns;
import com.codex.autopilot.core.Pace;
import com.codex.autopilot.core.PaceWatch;
import com.codex.autopilot.core.Plan;
import com.codex.autopilot.core.PortLog;
import com.codex.autopilot.core.Profile;
import com.codex.autopilot.core.Ranker;
import com.codex.autopilot.core.RoundsDailies;
import com.codex.autopilot.core.RumourBook;
import com.codex.autopilot.core.SavingsGoal;
import com.codex.autopilot.core.Session;
import com.codex.autopilot.core.Settings;
import com.codex.autopilot.core.SharedPlan;
import com.codex.autopilot.core.Tier;
import com.codex.autopilot.core.Tile;
import com.codex.autopilot.core.Vars;
import com.codex.autopilot.core.WantedSlots;
import com.codex.autopilot.core.Wealth;
import com.codex.autopilot.overlay.StepPlaque;
import com.codex.autopilot.ui.Actions;
import com.codex.autopilot.ui.AutopilotPanel;
import com.codex.autopilot.ui.Sigil;
import com.codex.autopilot.ui.View;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
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
 * dailies, Codex Navigator's goals and Codex Bank Steward's bank, and what the rest of the suite
 * shares: Wayfinder's teleports, Money Pilot's and Boss Pilot's rates, Gear Roadmap's saving goal,
 * Ledger's coins, Quartermaster's kits, Rounds' dailies and Log Insight's wanted slots), ranks it
 * all by what is lost by waiting and the time it takes to get there, and keeps the best step on a
 * plaque and in its panel. It never clicks, changes menus, writes another plugin's data or sends
 * anything off the computer; the only things it keeps are its own settings, among them the
 * player's learned pace and its top steps shared for Codex Home, and the only thing it sends is a
 * notification through RuneLite's own notifier.
 */
@PluginDescriptor(
	name = "Codex Autopilot",
	description = "One next step for your whole account: what is ready, what is about to go to waste and your pinned goal, in one order",
	tags = {"next", "step", "todo", "birdhouse", "farming", "daily", "kingdom", "miscellania", "goal", "navigator", "sailing", "port", "codex"}
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
	/** The RuneLite config key that says whether Codex Rounds is switched on. */
	private static final String ROUNDS_KEY = "codexroundsplugin";
	/** Settings that make chore steps come or go: a run in progress is dropped rather than timed when one changes. */
	private static final Set<String> CHORE_KEYS = new HashSet<>(Arrays.asList("chores", "herbs", "hops", "learnPace"));
	private static final String TIME_TRACKING = "timetracking";
	/** Codex apps whose shared saves change the plan as soon as they change. Ledger is read on a slower beat. */
	private static final Set<String> SUITE_GROUPS = new HashSet<>(Arrays.asList(Atlas.GROUP, MoneyRuns.GROUP, BossLog.GROUP,
		SavingsGoal.GROUP, Kits.GROUP, RoundsDailies.GROUP, WantedSlots.GROUP));
	/** The Hunter Rumours plugin's saved rumour, read when Autopilot has read none itself. */
	private static final String HUNTER_RUMOURS = "hunterrumours";
	/** RuneLite's chat commands plugin keeps the rumour count here, as it does boss kill counts. */
	private static final String KILLCOUNT = "killcount";
	private static final String RUMOUR_COUNT = "hunter rumours";
	/** The rumour's hunting time is saved this often while it runs; what the game says is saved at once. */
	private static final int RUMOUR_SAVE_TICKS = 500;
	/** A task's pay is the Sailing xp that arrives within this many ticks of its slot emptying. */
	private static final int PAID_TICKS = 2;
	/** Codex Ledger saves often and its ledger is large: it is read again at most this often. */
	private static final long LEDGER_SECONDS = 60;

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

	/** The optional local record of what the plugin read and decided. */
	private FieldNotes notes;

	@Inject
	private Notifier notifier;

	@Inject
	private ItemManager itemManager;

	private final StepPlaque plaque = new StepPlaque();
	private final Session session = new Session();
	private final Heads heads = new Heads();
	private final PaceWatch watch = new PaceWatch();

	private AutopilotPanel panel;
	private NavigationButton navButton;

	private int ticks;
	private volatile boolean skipped;
	private List<DailyState> dailies;
	private boolean royalTrouble;
	private Activity activity = Activity.NONE;
	/** The account's Hunter Guild rumour, read from the game; loaded from its save on first use. */
	private RumourBook rumour;
	private boolean rumourDirty;
	private boolean atFirstLight;
	private int hunterLevel;
	/** The account's port tasks, read from the game each tick they change; loaded from its save on first use. */
	private PortLog portLog;
	private PortCatalog portCatalog;
	/** The port task varbits last seen, so a change is noticed the tick it lands. */
	private int[] portSeen;
	private long sailingXp = -1;
	/** A task whose slot just emptied, the xp before it, and the ticks left to see its pay arrive. */
	private int paidTask;
	private long paidFrom;
	private int paidTicks;

	// Parsed only when the saved text changes.
	private String boardJson;
	private GoalBoard board;
	private String bankJson;
	private BankSnapshot bank;
	private String paceJson;
	private Pace pace;
	private Tile here;
	private boolean ironman;
	private final Cached<Atlas> atlas = new Cached<>();
	private final Cached<List<Earner>> money = new Cached<>();
	private final Cached<BossLog> bossLog = new Cached<>();
	private final Cached<SavingsGoal> goal = new Cached<>();
	private final Cached<Wealth> wealth = new Cached<>();
	private final Cached<Kits> kits = new Cached<>();
	private final Cached<List<DailyState>> roundsDailies = new Cached<>();
	private final Cached<WantedSlots> wanted = new Cached<>();
	private long ledgerReadAt;
	private final SharedPlan.Saver planSaver = new SharedPlan.Saver();
	/** The plan as last shared, for the replays and tests to read. */
	private SharedPlan shared;

	/** One shared save, parsed again only when its text changes. */
	private static final class Cached<T>
	{
		private String text;
		private boolean read;
		private T value;

		T get(String json, Function<String, T> parse)
		{
			if (!read || !Objects.equals(json, text))
			{
				text = json;
				read = true;
				try
				{
					value = json == null ? null : parse.apply(json);
				}
				catch (RuntimeException e)
				{
					value = null;
				}
			}
			return value;
		}

		void clear()
		{
			text = null;
			read = false;
			value = null;
		}
	}

	@Provides
	CodexAutopilotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexAutopilotConfig.class);
	}

	@Override
	protected void startUp()
	{
		notes = new FieldNotes(this::getPluginDirectory, gson);
		if (config.fieldNotes())
		{
			notes.open();
		}
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
		if (notes != null)
		{
			notes.shutdown();
			notes = null;
		}
		overlayManager.remove(plaque);
		clientToolbar.removeNavigation(navButton);
		plaque.show(null);
		panel = null;
		session.clear();
		heads.reset();
		watch.abandon();
		skipped = false;
		dailies = null;
		activity = Activity.NONE;
		rumour = null;
		rumourDirty = false;
		atFirstLight = false;
		hunterLevel = 0;
		forgetPorts();
		boardJson = null;
		board = null;
		bankJson = null;
		bank = null;
		paceJson = null;
		pace = null;
		here = null;
		ironman = false;
		clearSuite();
	}

	private void clearSuite()
	{
		atlas.clear();
		money.clear();
		bossLog.clear();
		goal.clear();
		wealth.clear();
		kits.clear();
		roundsDailies.clear();
		wanted.clear();
		ledgerReadAt = 0;
		planSaver.reset();
		shared = null;
	}

	// ---- Events ----

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		ticks++;
		if (watchPorts() && ticks % EVALUATE_TICKS != 0)
		{
			evaluate();
			return;
		}
		if (ticks % EVALUATE_TICKS != 0)
		{
			return;
		}
		readLive(ticks % QUEST_TICKS == 0);
		huntRumour(ticks % RUMOUR_SAVE_TICKS == 0);
		evaluate();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.rumours() || configManager.getRSProfileKey() == null)
		{
			return;
		}
		ChatMessageType type = event.getType();
		boolean dialog = type == ChatMessageType.DIALOG;
		if (!dialog && type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.MESBOX)
		{
			return;
		}
		RumourBook book = rumourBook();
		if (book.read(dialog, event.getMessage(), System.currentTimeMillis()))
		{
			rumourDirty = true;
			saveRumour();
			evaluate();
		}
	}

	/** The rumour's hunting time runs while logged in; the level and quest are noted, and saved now and then. */
	private void huntRumour(boolean save)
	{
		if (!config.rumours() || configManager.getRSProfileKey() == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		RumourBook book = rumourBook();
		book.hunted(EVALUATE_TICKS * 600L / 1000);
		if (book.getCreature() != null && !book.hasPart())
		{
			rumourDirty = true;
		}
		if (hunterLevel > 0 && book.account(hunterLevel, atFirstLight))
		{
			rumourDirty = true;
			save = true;
		}
		if (save)
		{
			saveRumour();
		}
	}

	private RumourBook rumourBook()
	{
		if (rumour == null)
		{
			rumour = RumourBook.parse(gson, Profile.safe((g, k) -> configManager.getRSProfileConfiguration(g, k),
				CodexAutopilotConfig.GROUP, RumourBook.KEY));
		}
		return rumour;
	}

	private void saveRumour()
	{
		if (rumourDirty && rumour != null && configManager.getRSProfileKey() != null)
		{
			rumourDirty = false;
			configManager.setRSProfileConfiguration(CodexAutopilotConfig.GROUP, RumourBook.KEY, rumour.toJson(gson));
		}
	}

	/**
	 * Reads the port task slots each tick, cheaply, and the whole of each task only when a slot
	 * changes. A slot that empties as Sailing xp arrives was handed in: that xp is what the task pays.
	 * True when the tasks changed.
	 */
	private boolean watchPorts()
	{
		if (!config.portTasks() || configManager.getRSProfileKey() == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}
		long xp;
		int[] seen = new int[PortLog.SLOTS * 4 + 1];
		try
		{
			xp = client.getSkillExperience(Skill.SAILING);
			for (int i = 0; i < PortLog.SLOTS; i++)
			{
				seen[i] = client.getVarbitValue(PortLog.SLOT_ID[i]);
			}
			PortLog.progress(client::getVarbitValue, seen, PortLog.SLOTS);
			seen[seen.length - 1] = client.getVarpValue(net.runelite.api.gameval.VarPlayerID.PORT_TASKS_COMPLETED);
		}
		catch (RuntimeException e)
		{
			return false;
		}
		if (xp <= 0)
		{
			// The account's stats are not in yet; neither are its slots, likely.
			return false;
		}
		PortLog log = portLog();
		boolean dirty = false;
		if (paidTicks > 0 && --paidTicks == 0)
		{
			dirty = log.paid(paidTask, (int) Math.min(Integer.MAX_VALUE, xp - paidFrom));
		}
		boolean changed = portSeen == null || !Arrays.equals(seen, portSeen);
		if (portSeen != null && changed && paidTicks == 0)
		{
			int emptied = 0;
			int task = 0;
			for (int i = 0; i < PortLog.SLOTS; i++)
			{
				if (portSeen[i] > 0 && seen[i] != portSeen[i])
				{
					emptied++;
					task = portSeen[i];
				}
			}
			if (emptied == 1)
			{
				// The xp may land this tick or the next: measure from before the slot emptied.
				paidTask = task;
				paidFrom = sailingXp >= 0 ? sailingXp : xp;
				paidTicks = PAID_TICKS;
			}
		}
		sailingXp = xp;
		if (changed)
		{
			portSeen = seen;
			if (portCatalog == null)
			{
				portCatalog = new PortCatalog(client);
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
			dirty |= log.update(log.read(vars, portCatalog), seen[seen.length - 1], client.getRealSkillLevel(Skill.SAILING),
				System.currentTimeMillis());
		}
		if (dirty)
		{
			configManager.setRSProfileConfiguration(CodexAutopilotConfig.GROUP, PortLog.KEY, log.toJson(gson));
		}
		return changed;
	}

	private PortLog portLog()
	{
		if (portLog == null)
		{
			portLog = PortLog.parse(gson, Profile.safe((g, k) -> configManager.getRSProfileConfiguration(g, k),
				CodexAutopilotConfig.GROUP, PortLog.KEY));
		}
		return portLog;
	}

	/** Another account, or a fresh start: nothing carries over but what is saved. */
	private void forgetPorts()
	{
		portLog = null;
		portSeen = null;
		sailingXp = -1;
		paidTicks = 0;
		paidTask = 0;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			dailies = null;
			activity = Activity.NONE;
			here = null;
			saveRumour();
			// The slots are read afresh on the way back in; a hand-in is not measured across it.
			portSeen = null;
			sailingXp = -1;
			paidTicks = 0;
			// Out of sight: a run in progress cannot be timed across it.
			watch.abandon();
			evaluate();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			ticks = 0;
			// What is already ready at login is old news: the first look after it only takes note.
			heads.reset();
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
		// Another account: what was set aside, told or timed belonged to the last one.
		saveRumour();
		rumour = null;
		rumourDirty = false;
		hunterLevel = 0;
		atFirstLight = false;
		forgetPorts();
		session.clear();
		heads.reset();
		watch.abandon();
		skipped = false;
		dailies = null;
		clientThread.invokeLater(() ->
		{
			// Its learned pace is read afresh, never carried over from the last account.
			pace = null;
			paceJson = null;
			clearSuite();
			evaluate();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CodexAutopilotConfig.GROUP.equals(event.getGroup()) && "fieldNotes".equals(event.getKey()) && notes != null)
		{
			if (config.fieldNotes())
			{
				notes.open();
			}
			else
			{
				notes.close();
			}
			return;
		}
		String group = event.getGroup();
		if (CodexAutopilotConfig.GROUP.equals(group) && CHORE_KEYS.contains(event.getKey()))
		{
			watch.abandon();
		}
		if (CodexAutopilotConfig.GROUP.equals(group) && (Pace.KEY.equals(event.getKey()) || SharedPlan.KEY.equals(event.getKey())
			|| RumourBook.KEY.equals(event.getKey()) || PortLog.KEY.equals(event.getKey())))
		{
			// Autopilot's own save of the pace or of the shared plan; nothing new to show.
			return;
		}
		if (CodexAutopilotConfig.GROUP.equals(group) || TIME_TRACKING.equals(group) || GoalBoard.GROUP.equals(group)
			|| BankSnapshot.GROUP.equals(group) || "kingdomofmiscellania".equals(group) || SUITE_GROUPS.contains(group)
			|| HUNTER_RUMOURS.equals(group))
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
			try
			{
				atFirstLight = Quest.AT_FIRST_LIGHT.getState(client) == QuestState.FINISHED;
			}
			catch (RuntimeException e)
			{
				atFirstLight = false;
			}
		}
		activity = readActivity();
		try
		{
			hunterLevel = client.getRealSkillLevel(Skill.HUNTER);
		}
		catch (RuntimeException e)
		{
			hunterLevel = 0;
		}
		Player me = client.getLocalPlayer();
		WorldPoint at = me == null ? null : me.getWorldLocation();
		here = at == null ? null : Tile.of(at.getX(), at.getY(), at.getPlane());
		try
		{
			ironman = client.getVarbitValue(VarbitID.IRONMAN) > 0;
		}
		catch (RuntimeException e)
		{
			ironman = false;
		}
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
			.herbs(config.herbs())
			.hops(config.hops())
			.rounds("true".equals(configManager.getConfiguration("runelite", ROUNDS_KEY)))
			.dailies(config.dailies())
			.kingdom(config.kingdom())
			.rumours(config.rumours())
			.portTasks(config.portTasks())
			.navigator(config.navigator())
			.bankSteward(config.bankSteward())
			.wayfinder(config.wayfinder())
			.moneyPilot(config.moneyPilot())
			.gearRoadmap(config.gearRoadmap())
			.ledger(config.ledger())
			.quartermaster(config.quartermaster())
			.bossPilot(config.bossPilot())
			.roundsDailies(config.roundsDailies())
			.logInsight(config.logInsight())
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
		List<Candidate> all = new ArrayList<>();
		String nextUp = null;
		boolean learn = config.learnPace();
		if (known)
		{
			in = Advisor.Inputs.read(new OnlyCore(profile), gson, now, royalTrouble);
			in.board = board(profile);
			in.bank = bank(profile);
			in.pace = learn ? pace(profile) : null;
			in.dailies = loggedIn ? dailies : null;
			readSuite(in, profile, s, now, loggedIn);
			all = Advisor.candidates(in, s, now);
			plan = Ranker.rank(all, s, session, now);
			nextUp = Advisor.nextUp(in, s, now);
		}

		Budget budget = config.timeBudget() == null ? Budget.NONE : config.timeBudget();
		Budget.Fit fit = Budget.fit(plan, budget.getMinutes(), session);
		Candidate top = budget == Budget.NONE || fit.isEmpty() ? plan.top() : fit.first();

		List<String> fedBy = known ? Advisor.fedBy(in, s, plan.getSteps()) : new ArrayList<>();
		if (known && loggedIn)
		{
			learn(all, top, now, learn);
			announce(all, plan, now);
			share(budget == Budget.NONE || fit.isEmpty() ? plan.getSteps() : fit.getSteps(), fedBy);
		}

		if (notes.isOpen() && known)
		{
			// The ranking and what it was read from, so a strange next step can be traced.
			com.google.gson.JsonObject n = new com.google.gson.JsonObject();
			n.add("top", notes.safe(top));
			n.addProperty("nextUp", nextUp);
			n.add("ranked", notes.safe(all.subList(0, Math.min(8, all.size()))));
			n.addProperty("budget", String.valueOf(budget));
			n.addProperty("activity", String.valueOf(activity));
			n.add("fedBy", notes.safe(fedBy));
			notes.noteChanged("plan", n);
		}
		boolean aside = config.stepAside() && activity != Activity.NONE;
		plaque.show(config.plaque() && loggedIn && !aside ? top : null);

		AutopilotPanel p = panel;
		if (p == null)
		{
			return;
		}
		View view = new View(loggedIn, known, plan, config.stepAside() ? activity : Activity.NONE,
			"true".equals(configManager.getConfiguration("runelite", MINIGAME_PILOT_KEY)), nextUp, s.isQuiet(), skipped,
			sources(in, s, plan, loggedIn, now), budget, fit, fedBy,
			s.isPortTasks() && in.ports != null ? in.ports.with(in.ports.getTasks()) : null);
		SwingUtilities.invokeLater(() -> p.update(view));
	}

	/** Reads what the rest of the Codex suite shares, each parsed again only when its save changes. On the client thread. */
	private void readSuite(Advisor.Inputs in, Profile profile, Settings s, long now, boolean loggedIn)
	{
		long nowMs = now * 1000;
		in.here = loggedIn ? here : null;
		in.ironman = loggedIn && ironman;
		if (s.isWayfinder())
		{
			in.atlas = atlas.get(Profile.safe(profile, Atlas.GROUP, Atlas.KEY), json -> Atlas.parse(gson, json));
		}
		if (s.isMoneyPilot())
		{
			// The rate window moves on with the day, so the runs are worked out again each new day.
			String json = Profile.safe(profile, MoneyRuns.GROUP, MoneyRuns.KEY);
			in.money = money.get(json == null ? null : (now / 86_400) + "|" + json,
				t -> MoneyRuns.parse(gson, t.substring(t.indexOf('|') + 1), nowMs));
			if (in.money == null)
			{
				in.money = new ArrayList<>();
			}
		}
		if (s.isBossPilot())
		{
			String pin = Profile.safe(profile, BossLog.GROUP, BossLog.PINNED_KEY);
			String book = Profile.safe(profile, BossLog.GROUP, BossLog.LOGBOOK_KEY);
			in.bossLog = bossLog.get(pin == null && book == null ? null : pin + "\u0000" + book, t -> BossLog.parse(gson, pin, book));
		}
		if (s.isGearRoadmap())
		{
			in.goal = goal.get(Profile.safe(profile, SavingsGoal.GROUP, SavingsGoal.KEY), json -> SavingsGoal.parse(gson, json));
		}
		if (s.isLedger())
		{
			if (now - ledgerReadAt >= LEDGER_SECONDS)
			{
				ledgerReadAt = now;
				wealth.get(Profile.safe(profile, Wealth.GROUP, Wealth.KEY), json -> Wealth.parse(gson, json));
			}
			in.wealth = wealth.value;
		}
		if (s.isQuartermaster())
		{
			in.kits = kits.get(Profile.safe(profile, Kits.GROUP, Kits.KEY), json -> Kits.parse(gson, json));
		}
		if (s.isRoundsDailies() && !loggedIn)
		{
			String json = Profile.safe(profile, RoundsDailies.GROUP, RoundsDailies.KEY);
			in.roundsDailies = roundsDailies.get(json == null ? null : (now / 86_400) + "|" + json,
				t -> RoundsDailies.parse(gson, t.substring(t.indexOf('|') + 1), now));
		}
		if (s.isLogInsight())
		{
			in.wanted = wanted.get(Profile.safe(profile, WantedSlots.GROUP, WantedSlots.KEY), json -> WantedSlots.parse(gson, json));
		}
		if (s.isRumours())
		{
			RumourBook own = rumourBook();
			RumourBook plugin = own.isBlank() ? RumourBook.fromHunterRumours(profile) : null;
			in.rumour = own.orElse(plugin).withDone(count(Profile.safe(profile, KILLCOUNT, RUMOUR_COUNT)));
			in.rumourFromPlugin = plugin != null && plugin.getCreature() != null;
		}
		if (s.isPortTasks())
		{
			PortLog log = portLog();
			in.ports = log.doesPortTasks() || !log.getTasks().isEmpty() ? log : null;
		}
		in.prices = id -> itemManager.getItemPrice(id);
		in.names = id -> itemManager.getItemComposition(id).getName();
	}

	/** Saves the top steps for Codex Home and the rest of the suite, when they change. On the client thread. */
	private void share(List<Candidate> steps, List<String> fedBy)
	{
		if (!config.sharePlan())
		{
			return;
		}
		long nowMs = System.currentTimeMillis();
		SharedPlan plan = SharedPlan.of(steps, fedBy, nowMs);
		if (planSaver.due(plan, gson, nowMs))
		{
			shared = plan;
			configManager.setRSProfileConfiguration(CodexAutopilotConfig.GROUP, SharedPlan.KEY, plan.toJson(gson));
		}
	}

	/** Times the run being worked and keeps what it learns in Autopilot's own profile settings. On the client thread. */
	private void learn(List<Candidate> all, Candidate top, long now, boolean on)
	{
		if (!on || pace == null)
		{
			watch.abandon();
			return;
		}
		List<PaceWatch.Sample> done = watch.observe(all, top == null ? null : top.getKey(), now);
		boolean changed = false;
		for (PaceWatch.Sample sample : done)
		{
			changed |= pace.add(sample.kind, sample.minutesPerStop);
		}
		if (changed)
		{
			String json = pace.toJson(gson);
			paceJson = json;
			configManager.setRSProfileConfiguration(CodexAutopilotConfig.GROUP, Pace.KEY, json);
		}
	}

	/** Sends what is new through RuneLite's notifier, once per change. On the client thread. */
	private void announce(List<Candidate> all, Plan plan, long now)
	{
		Set<String> visible = new HashSet<>();
		for (Candidate c : plan.getSteps())
		{
			visible.add(c.getKey());
		}
		boolean on = config.notifications();
		// Watched even with notifications off, so switching them on later does not announce old news.
		for (Heads.Notice n : heads.observe(all, visible, now, on && config.notifyWaste(), on && config.notifyReady()))
		{
			notifier.notify(n.text);
		}
	}

	private Pace pace(Profile profile)
	{
		String json = Profile.safe(profile, CodexAutopilotConfig.GROUP, Pace.KEY);
		if (pace == null || !Objects.equals(json, paceJson))
		{
			paceJson = json;
			pace = Pace.parse(gson, json);
		}
		return pace;
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
		out.add(rumourSource(in, s));
		out.add(portSource(in, s));
		out.add(!s.isNavigator() ? off("Navigator")
			: in.board == null ? new View.Source("Navigator", "No goals", false)
			: new View.Source("Navigator", in.board.open() == 1 ? "1 goal" : in.board.open() + " goals", true));
		out.add(!s.isBankSteward() ? off("Bank Steward")
			: in.bank == null ? new View.Source("Bank Steward", "No bank", false)
			: new View.Source("Bank Steward", in.bank.getSavedAt() <= 0 ? "Bank known"
				: Format.shortDuration(now - in.bank.getSavedAt() / 1000) + " ago", true));
		out.add(!s.isWayfinder() ? off("Wayfinder")
			: in.atlas == null ? new View.Source("Wayfinder", "No atlas", false)
			: new View.Source("Wayfinder", Format.plural(in.atlas.getTeleports().size(), "teleport", "teleports"), true));
		Earner best = Earner.best(Advisor.earners(in, s));
		out.add(!s.isMoneyPilot() && !s.isBossPilot() ? off("Earning")
			: best == null ? new View.Source("Earning", "No runs", false)
			: new View.Source("Earning", "Best " + Format.number(best.getPerHour()) + "/hr", true));
		long wasting = plan.count(Tier.WASTING);
		out.add(new View.Source("At risk", wasting == 0 ? "Nothing" : String.valueOf(wasting), wasting > 0));
		return out;
	}

	private static int count(String text)
	{
		try
		{
			return text == null ? -1 : Integer.parseInt(text.trim());
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	private static View.Source rumourSource(Advisor.Inputs in, Settings s)
	{
		RumourBook r = in.rumour;
		if (!s.isRumours())
		{
			return off("Rumour");
		}
		if (r == null || r.getCreature() == null)
		{
			boolean known = r != null && (r.getSeenAt() > 0 || r.getDone() >= 0);
			return new View.Source("Rumour", known ? "None held" : "Not seen", false);
		}
		return new View.Source("Rumour", r.hasPart() ? "Part found" : r.getCreature().getName(), true);
	}

	private static View.Source portSource(Advisor.Inputs in, Settings s)
	{
		if (!s.isPortTasks())
		{
			return off("Port tasks");
		}
		PortLog log = in.ports;
		if (log == null || log.getTasks().isEmpty())
		{
			return new View.Source("Port tasks", log != null && log.doesPortTasks() ? "None held" : "Not seen", false);
		}
		return new View.Source("Port tasks", Format.plural(log.getTasks().size(), "task", "tasks"), true);
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
			return GoalBoard.GROUP.equals(group) || BankSnapshot.GROUP.equals(group) || CodexAutopilotConfig.GROUP.equals(group)
				? null : inner.get(group, key);
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
		public void budget(Budget budget)
		{
			configManager.setConfiguration(CodexAutopilotConfig.GROUP, CodexAutopilotConfig.BUDGET_KEY, budget);
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
