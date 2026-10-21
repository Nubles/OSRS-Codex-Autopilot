# Codex Autopilot

**One next step for your whole account.** A single plaque in the game view, and a side panel, that always shows the one best thing to do right now, ranked across everything your account has going on, with a one-line reason. Full bird houses, herb runs, grown trees and special patches, diary dailies before the reset, the Kingdom of Miscellania and the goal you pinned in Codex Navigator all go into one order: what is about to go to waste first, then the chores that are ready, then your goal.

It is part of the **Codex** suite, after [Codex Navigator](https://github.com/Nubles/OSRS-Codex-Navigator), [Codex Rounds](https://github.com/Nubles/OSRS-Codex-Rounds), [Codex Bank Steward](https://github.com/Nubles/OSRS-Codex-Bank-Steward), [Codex Minigame Pilot](https://github.com/Nubles/OSRS-Codex-Minigame-Pilot) and the rest, and shares their look: warm ink, aged gold and the game's own fonts.

<p align="center">
  <img src="docs/screenshots/panel.png" width="225" alt="The next step and the five after it">
  <img src="docs/screenshots/going-to-waste.png" width="225" alt="Two hours before the reset">
  <img src="docs/screenshots/stepping-aside.png" width="225" alt="Stepping aside in the Motherlode Mine">
</p>
<p align="center">
  <img src="docs/screenshots/herb-run.png" width="225" alt="A herb run with a diseased patch">
  <img src="docs/screenshots/budget.png" width="225" alt="What fits in 30 minutes">
</p>

## What it does

- **One step, always.** The plaque shows the best next step in gilt letters, what kind of step it is, about how long it takes, why it is first, and what to bring. Its edge takes the step's colour: ember when something is about to be lost, green for a ready chore, teal for your focused goal.
- **Time-aware.** Steps are ordered by what waiting costs. A daily you have not collected is lost at midnight UTC, so in the last hours before the reset it jumps to the front. A diseased patch dies at its next growth tick. The kingdom's approval and coffer drop at every reset. A full bird house idles a whole run every 50 minutes, a grown calquat only a sliver of its day-long growth, so the Fossil Island trip comes before the tree run.
- **Chores that fit together.** Ready things are grouped into the trips you would actually make: "Birdhouse run: 4 ready, 2 seaweed ready" (with Fossil Island's hardwoods), a tree run, and the special patches. If more join within a quarter of an hour the step says so.
- **Herb and hops runs.** Your herb patches become one step, "Herb run: 5 ready, 1 diseased, 4 allotments", with the allotments and flower patches beside them riding along, and the hops patches have a run of their own. A diseased patch dies at its next growth tick and a dead herb patch grows nothing, so either puts the run among the things going to waste. Only patches you actually farm count: weeds, bare soil, scarecrows and patches dead for days are left alone, and a lone flower patch is not worth a trip on its own. The bring line counts herb seeds (any kind) and compost (or your bottomless bucket) against the bank.
- **Heads-up notifications.** Through RuneLite's own notifier, once when something starts going to waste and once when a chore trip becomes ready ("Bird houses are full"). Each step is told about once; it is told again only when it changes, such as falling diseased after it was ready, or coming ready again after you did it. Logging in does not replay what was already waiting. Each kind has its own switch.
- **Your own pace.** Times are estimates until Autopilot has seen you do a kind of run three times; then they are yours: "~9 min (your pace)" instead of "~11 min (estimate)". A run is timed from the first stop you finish (a house reset, a patch replanted) to the last, so a run left waiting for an hour does not count as an hour's work, and it is only learned from when it was the step at the top. The middle of your last seven runs is used, and a run interrupted by logging out is dropped.
- **Time budget.** "I have 15 / 30 / 60 min / any" at the top of the panel. With a limit, the panel shows the best set of steps that fits, with the total time: anything going to waste first, then what saves most per minute. The plaque shows the first of them.
- **Hand-off to Codex Rounds.** With Codex Rounds on, bird house, tree and special patch steps say "Codex Rounds has the route", and Rounds walks you round. Herb and hops runs are not on its round.
- **Your goal, next.** The focused goal on your Codex Navigator board becomes a step ("Toward Quest cape: Start Monkey Madness II"), followed by your other pinned goals, those with least left first. **Goal first** in the settings lets your focused goal pass slow chores such as trees, dailies with hours to spare and the kingdom; full bird houses and anything about to go to waste still come first.
- **What to bring.** If Codex Bank Steward has seen your bank, the step checks it: "Bring 4 yew logs + 40 hop seeds + 2 seaweed spores", or "You're out of flax" before you set off.
- **The next five.** The panel lists the five steps after the first, each with why it is there and how long it takes. **Skip** moves a step to the end for this session; **Snooze 1h** hides it for an hour. **Bring them back** undoes both.
- **Steps aside.** In the Motherlode Mine, at the Blast Furnace or on a Mahogany Homes contract, where Codex Minigame Pilot guides, the plaque goes away and the panel says so. It comes back when you leave.
- **Quiet mode.** Only show what is about to go to waste, and nothing else.
- **Nothing pinned, nothing ready.** The panel says what comes ready next, such as "Bird houses full in 23 min".

<p align="center">
  <img src="docs/screenshots/plaque.png" alt="Plaque">
  <br>
  <img src="docs/screenshots/plaque-waste.png" alt="Plaque, going to waste">
  <br>
  <img src="docs/screenshots/plaque-herbs.png" alt="Plaque, a herb run with a diseased patch">
  <br>
  <img src="docs/screenshots/plaque-paced.png" alt="Plaque, timed at your own pace">
</p>

## Where it reads from

Everything is read on your computer, from the client and from RuneLite's saved profile data, and nothing is changed:

- **RuneLite's Time Tracking plugin**: the bird houses, seaweed, hardwood, special trees, spirit trees, special patches, herbs, allotments, flowers and hops it has seen, with growth predicted the same way as Codex Rounds (including your farming tick offset). Patches left to weeds and empty bird house spaces are not nagged about.
- **RuneLite's Kingdom of Miscellania plugin**: approval and coffer as last seen, carried forward a day at a time.
- **The game**: diary dailies and the farming contract, read live while you are logged in.
- **Codex Navigator**: the goal board it saves for companion plugins.
- **Codex Bank Steward**: the bank it last saw, only to check a chore's supplies.

Every source is optional. Bird houses and the special patches, herb runs and hops runs each have their own switch. Without Navigator there are no goal steps; without Bank Steward the bring line is a plain list. Each can be switched off in the settings.

## Rules posture

Codex Autopilot only reads what the game and RuneLite already know and draws a plaque and a panel. It does not click, type, change menus or menu entries, give boss or combat cues, read or write files, or send anything over the network. Its notifications go through RuneLite's own notifier, on your computer. The only thing it saves is its own settings, including your learned pace (a few numbers per kind of run, kept with the rest of the plugin's settings for that account). It never writes another plugin's data.

## Development

```bash
./gradlew build   # compile and run tests
./gradlew run     # launch RuneLite with the plugin loaded
```

The readers and the ranking (`core/`) are plain Java with unit tests; the panel and plaque are in `ui/` and `overlay/`.

## Licence

BSD 2-Clause, see [LICENSE](LICENSE).
