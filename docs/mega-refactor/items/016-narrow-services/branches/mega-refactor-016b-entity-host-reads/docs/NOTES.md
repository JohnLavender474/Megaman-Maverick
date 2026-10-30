# Notes: mega-refactor-016b-entity-host-reads

## Decisions

- 2026-10-28 (owner, gate 1, "approved"): `SCOPE.md` approved. Accepted as recommended: only the 7 item reads;
  `LevelState.showEndingScreen` set in `MLS.init` and called with `!!`; `GameClock` in `core/.../state/` as
  `game.clock`; `MegaPlayers.selectButtonAction`; a temporary suite for `DrWily` only; the root `CLAUDE.md` lines;
  real rooms for the manual checks. Changed by the owner:
  - No forwarding members (verbatim: "I'm not sure I agree with this. Forwarding patterns like this can obfuscate the
    actual source/target of things."). `game.paused` and `game.selectButtonAction` go; every caller reads the real
    home. `pause()` and `resume()` stay as actions. The `016a` forwards stay for `016c`. Recorded on `master` as
    `016` decision 11.
  - The pathfinding debug shapes stay as they are (the owner uses them now and then); a separate research agent looks
    for a better approach.
- 2026-10-28 (owner, gate 2, "yes to both"): `CONTRACT.md` approved.
- 2026-10-28 (owner, plan review): the plan is not approved as written. The `DrWily` callback on `LevelState` goes.
  Owner (verbatim): "It doesn't seem like good design to tightly couple a flag for the game's ending screen to the
  level state. Is there a better approach to this?"; after the research below: "I like option c since it reuses the
  existing global events manager" and "016b can remove the dead flag". `DrWily` submits a new event `GAME_COMPLETE`;
  `MegaLevelScreen` switches to `ENDING_SCREEN` on it. Recorded on `master` as `016` decision 12, which replaces
  decision 9. `SCOPE.md` "Revision 2".

## Overlap with other branches

- 2026-10-28: no other code branch is active (`git worktree list`: the main checkout, the parent
  `mega-refactor-016-master`, and this branch). The merge target is the parent; the branch start `07307f156` is its
  tip. `031` (active item, no active branch) plans `selectButtonAction` per slot (`031` `ITEM.md` decisions
  2026-10-25); `013` (planned) owns pause later. `016c` builds on the homes of this branch.
- 2026-10-28: a planned `fix/` branch from `master` (pathfinding debug shapes, `--debugPathfinding`) touches
  `Bat.kt`, `SubmarineJoe.kt`, `MegaPathfinderResultConsumer.kt`, `AbstractPathfindingSystem` and
  `createGameEngine()`. `016b` changes none of these, except the 3 drawables lines in `createGameEngine()`.

## Research: the `DrWily` ending switch (2026-10-28)

Key facts from the research note (a separate agent, worktree of this branch, plan not implemented). `C/` =
`core/src/main/kotlin/com/megaman/maverick/game/`, `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`.

- `WilyFinalBoss.onDestroy` (`C/entities/bosses/WilyFinalBoss.kt:291-310`) spawns `DrWily` only if the target player
  lives, the health is depleted and the phase is `PHASE_3`; it damage-locks all players and submits
  `TURN_CONTROLLER_OFF`. `WilyFinalBoss.getEventOnDefeated()` is `null` (`:324-325`), so no `VICTORY_EVENT` and no
  `END_LEVEL`. Only `WilyStage3_v4.tmx` (`WILY_STAGE_3`) places the boss.
- Frame N today (`DrWily` calls `setCurrentScreen` in the `UpdatablesSystem`): `MLS.hide()` does nothing;
  `MLS.reset()` removes the `MLS` listener at once (the event loop is not running), `engine.reset()` only sets a flag
  (`E/GameEngine.kt:247-251`), the handlers and camera reset, `audioMan.unsetMusic()`, `TURN_CONTROLLER_ON` is
  buffered; then `EndingScreen.show` plays the ending music. The other systems run on the still-present entities, the
  engine resets at the end of its update (`E/GameEngine.kt:242-244`), the rest of `MLS.render` runs on the reset
  level, and `screen.draw` (a local, still `MLS`) draws the level once more. Frame N+1 is the first ending frame.
  `EndingScreen` goes to `CREDITS_SCREEN`, the credits to `MAIN_MENU_SCREEN`.
- With an event: the switch runs in `eventsMan.run()` of frame N, after `draw`. Same visible output: the level draws in
  frame N, the ending first in N+1, the same music order. Internal differences: the engine resets at once; the frame
  runs on the live level; `MLS` and the destroyed entities get the frame-N events after the switch (a removal during
  `run()` is deferred, `E/events/EventsManager.kt:49-50`); `TURN_CONTROLLER_ON` arrives in frame N+1. `END_LEVEL`
  (`MegaLevelScreen.kt:870-880`) and game over (`:651-662`) already switch at this point.
- The `VICTORY_EVENT` `endGame` read (`MegaLevelScreen.kt:855-856`) is what remains of a first plan (commit
  `9d689277c`, an empty `EndGameHandler`, later removed; commit `260877972` added the ending screen and the direct
  call in `DrWily`). No sender sets `ConstKeys.END` on `VICTORY_EVENT`.
- The ending path skips the beam-out, `addLevelDefeated(WILY_STAGE_3)` and `screenOnCompletion` (which is
  `SAVE_GAME_SCREEN` for all Wily stages). So `WILY_STAGE_3` stays "not defeated" after the ending. Changing this is a
  profile and gameplay decision for `013` (option d).
- Options studied: (a) the `LevelState` callback; (b) a level-exit object outside `LevelState` (b1 an interface on
  `MLS`, b2 a typed `LevelExit` with a handler); (c) an event that `MLS` handles (chosen); (c') `VICTORY_EVENT` with
  `END = true`; (d) `screenOnCompletion` and `END_LEVEL` (a gameplay change); (e) a navigation service (not
  recommended); (f) a polled outcome value (not recommended). Left for `013`: one owner of level exits and
  outcomes; for `057`: the typed form of the event.
- Checked in this branch (2026-10-28, a scratch test, deleted): a `MegaLevelScreen` builds with a relaxed game mock in
  a unit test, and `onEvent` runs, so the `GAME_COMPLETE` branch can have a permanent test.
