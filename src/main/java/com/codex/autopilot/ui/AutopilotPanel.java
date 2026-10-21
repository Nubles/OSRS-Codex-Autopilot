package com.codex.autopilot.ui;

import com.codex.autopilot.core.Budget;
import com.codex.autopilot.core.Candidate;
import com.codex.autopilot.core.Plan;
import com.codex.autopilot.core.Tier;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import net.runelite.client.ui.PluginPanel;

/**
 * The Autopilot side panel: the crest with how many steps are waiting, the one best thing to do
 * now, the five after it with why each is there, and the sources it read them from.
 */
public class AutopilotPanel extends PluginPanel
{
	static final int WIDTH = 200;
	static final int AFTER = 5;

	private final Actions actions;
	private final Crest crest = new Crest();
	private final JScrollPane scroll = new JScrollPane();
	private View view = View.empty();

	public AutopilotPanel(Actions actions)
	{
		super(false);
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);
		add(crest, BorderLayout.NORTH);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		add(scroll, BorderLayout.CENTER);
		rebuild();
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		this.view = view;
		rebuild();
	}

	private void rebuild()
	{
		updateCrest();
		int y = scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(build());
		scroll.getVerticalScrollBar().setValue(y);
		revalidate();
		repaint();
	}

	private void updateCrest()
	{
		if (!view.known)
		{
			crest.update("ON YOUR ACCOUNT", null, "step", "steps", "Log in to read your account", null, Theme.TEXT_MUTED, Theme.GOLD);
			return;
		}
		Plan p = view.plan;
		int n = p.getSteps().size();
		if (view.steppingAside())
		{
			crest.update("STEPPING ASIDE", n, "step", "steps", view.activity.getWhere(), null, Theme.TEAL, Theme.GOLD_DIM);
			return;
		}
		long wasting = p.count(Tier.WASTING);
		long ready = p.count(Tier.READY);
		String status;
		Color color;
		if (wasting > 0)
		{
			status = wasting + " going to waste soon";
			color = Theme.EMBER;
		}
		else if (ready > 0)
		{
			status = ready + (ready == 1 ? " chore ready now" : " chores ready now");
			color = Theme.GREEN;
		}
		else if (p.count(Tier.FOCUS) > 0)
		{
			status = "Clear for your goal";
			color = Theme.TEAL;
		}
		else
		{
			status = n == 0 ? "All caught up" : "Goals waiting";
			color = Theme.TEXT_MUTED;
		}
		crest.update(view.quiet ? "QUIET MODE" : "ON YOUR ACCOUNT", n, "step", "steps", status, null, color,
			wasting > 0 ? Theme.EMBER : Theme.GOLD);
	}

	private StackPanel build()
	{
		StackPanel list = new StackPanel(6, 8);
		if (!view.known)
		{
			list.add(note("Log in and Autopilot reads your bird houses, patches, dailies and kingdom, and the goals you pinned in "
				+ "Codex Navigator, then keeps the one best next step in view."));
			sources(list);
			return list;
		}

		Plan p = view.plan;
		Candidate top = p.top();
		if (!view.steppingAside())
		{
			list.add(budgetBar());
		}
		if (view.steppingAside())
		{
			list.add(new SectionHeader("Stepping aside"));
			list.add(aside());
			if (top != null)
			{
				list.space(2);
				list.add(new SectionHeader("When you're done", p.getSteps().size() == 1 ? "1 step" : p.getSteps().size() + " steps"));
				list.add(StepCard.lead(top, null, WIDTH - 30, true));
			}
		}
		else if (top == null)
		{
			list.add(new SectionHeader("Do this now"));
			list.add(caughtUp());
		}
		else if (view.budgeted() && !view.fit.isEmpty())
		{
			budgeted(list);
		}
		else
		{
			if (view.budgeted())
			{
				list.add(note("Nothing fits in " + view.budget + ": the quickest step takes ~" + view.fit.getShortest()
					+ " min. Here is the best one anyway."));
			}
			list.add(new SectionHeader("Do this now", top.getTier() == Tier.WASTING && top.getDeadline() > 0 ? "before it's lost" : null));
			list.add(StepCard.lead(top, actions, WIDTH - 30, false));
			List<Candidate> after = p.after(AFTER);
			if (!after.isEmpty())
			{
				list.space(2);
				int more = p.getSteps().size() - 1;
				list.add(new SectionHeader("Then", more == 1 ? "1 more" : more + " more"));
				int i = 2;
				for (Candidate c : after)
				{
					list.add(StepCard.row(i++, c, actions, WIDTH - 30));
				}
			}
		}

		if (view.skipped || p.getSnoozed() > 0 || p.getQuiet() > 0)
		{
			list.space(2);
			list.add(heldBack(p));
		}
		sources(list);
		return list;
	}

	/** The steps that fit the time the player has, in the order to do them. */
	private void budgeted(StackPanel list)
	{
		Budget.Fit fit = view.fit;
		List<Candidate> steps = fit.getSteps();
		Candidate first = steps.get(0);
		list.add(new SectionHeader("In your " + view.budget, "~" + fit.getTotal() + " min"));
		list.add(StepCard.lead(first, actions, WIDTH - 30, false));
		if (steps.size() > 1)
		{
			list.space(2);
			list.add(new SectionHeader("Then", steps.size() - 1 == 1 ? "1 more" : steps.size() - 1 + " more"));
			for (int i = 1; i < steps.size(); i++)
			{
				list.add(StepCard.row(i + 1, steps.get(i), actions, WIDTH - 30));
			}
		}
		if (fit.getLeft() > 0)
		{
			list.space(2);
			list.add(note((fit.getLeft() == 1 ? "1 more step waits" : fit.getLeft() + " more steps wait") + " for when you have longer."));
		}
	}

	/** "I have 15 / 30 / 60 / Any": what the plan has to fit in. */
	private JComponent budgetBar()
	{
		JPanel bar = new JPanel(new BorderLayout(6, 0));
		bar.setOpaque(false);
		javax.swing.JLabel label = new javax.swing.JLabel("I have");
		label.setFont(Theme.small());
		label.setForeground(Theme.GOLD);
		bar.add(label, BorderLayout.WEST);
		JPanel buttons = new JPanel(new GridLayout(1, 0, 4, 0));
		buttons.setOpaque(false);
		for (Budget b : new Budget[]{Budget.MIN_15, Budget.MIN_30, Budget.MIN_60, Budget.NONE})
		{
			String text = b == Budget.NONE ? "Any" : b.getMinutes() + "m";
			buttons.add(new CodexButton(text, b == view.budget, 18, () -> actions.budget(b)));
		}
		bar.add(buttons, BorderLayout.CENTER);
		return bar;
	}

	private JComponent aside()
	{
		Card card = new Card(new BorderLayout(0, 4)).accent(Theme.TEAL);
		StackPanel body = new StackPanel(3, 0);
		String who = view.minigamePilot ? "Minigame Pilot has this" : view.activity.getLabel() + " has your attention";
		body.add(Ui.wrapped(who, Theme.title(), Theme.GOLD_BRIGHT, WIDTH - 30));
		body.add(Ui.wrapped(view.activity.getWhere() + ". Autopilot stays quiet here and picks up again when you leave.",
			Theme.small(), Theme.TEXT, WIDTH - 30));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	private JComponent caughtUp()
	{
		Card card = new Card(new BorderLayout(0, 4)).accent(Theme.GOLD_DIM);
		StackPanel body = new StackPanel(3, 0);
		body.add(Ui.wrapped(view.quiet ? "Nothing going to waste" : "All caught up", Theme.title(), Theme.GOLD_BRIGHT, WIDTH - 30));
		String detail = view.nextUp != null ? view.nextUp + "." : "Nothing is ready and no goal has a next step.";
		if (!view.quiet && view.plan.getSnoozed() == 0)
		{
			detail += " Pin a goal in Codex Navigator and its next step shows here.";
		}
		body.add(Ui.wrapped(detail, Theme.small(), Theme.TEXT, WIDTH - 30));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	private JComponent heldBack(Plan p)
	{
		StringBuilder sb = new StringBuilder();
		if (p.getQuiet() > 0)
		{
			sb.append("Quiet mode holds back ").append(p.getQuiet()).append(p.getQuiet() == 1 ? " step" : " steps").append(". ");
		}
		if (p.getSnoozed() > 0)
		{
			sb.append(p.getSnoozed()).append(" snoozed. ");
		}
		if (view.skipped)
		{
			sb.append("Skipped steps wait at the end. ");
		}
		Card card = new Card(new BorderLayout(0, 6));
		card.add(Ui.wrapped(sb.toString().trim(), Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		if (view.skipped || p.getSnoozed() > 0)
		{
			JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			bar.setOpaque(false);
			bar.add(new CodexButton("Bring them back", false, 20, actions::restore));
			card.add(bar, BorderLayout.SOUTH);
		}
		return card;
	}

	private void sources(StackPanel list)
	{
		if (view.sources.isEmpty())
		{
			return;
		}
		list.space(2);
		list.add(new SectionHeader("Reading from"));
		JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
		grid.setOpaque(false);
		for (View.Source s : view.sources)
		{
			grid.add(new StatTile(s.label, s.value, s.live));
		}
		list.add(grid);
	}

	private static JComponent note(String text)
	{
		Card card = new Card(new BorderLayout());
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		return card;
	}
}
