# Replays

Each `.replay` file here is a scripted stretch of play, tick by tick, played against the
whole plugin in CI by `ReplayTest`: started as RuneLite starts it, and sent the game's events.
The game client is a stand-in (`com.codex.replay.FakeGame`) that the script drives: where the player stands, the ground, skills, varbits, what is carried,
and the objects that spawn and despawn. The plugin reads it through RuneLite's own `Client`
interface, so its code runs unchanged.

A script says what the plugin must show (`expect`), must never show (`never`) and must always
show (`always`). A failure names the file, line, tick and tile, and lists every reading that tick.

```
include /replays/scenes/varrock.scene      # a shared start, which logs in and watches the plaque
varbit VARROCK_DIARY_EASY_COMPLETE 1       # the account has earned Zaff's battlestaves
varbit ZAFF_LAST_CLAIMED 0                 # and has not bought today's
tick 5
expect title = "Zaff's battlestaves"
```

The full command list is at the top of `src/test/java/com/codex/replay/Replay.java`. The plugin
runs whole under `PluginHost.java`, which adds `config`, `saved`, `watch` (what to read out of the
plugin) and `call`. The `com.codex.replay` files are shared by the Codex plugins; keep them identical.

Rules for new scripts:

- Write the game facts a script relies on (levels, unlocks, ids) from the OSRS wiki or RuneLite's
  source, and cite the page in a comment. Ids are written by their RuneLite names.
- Scenes are schematic (the shape that matters, not the real map) and say so.
- Every player-reported bug gets a replay named after what went wrong, with the issue number in
  its first comment, before the fix is merged.
