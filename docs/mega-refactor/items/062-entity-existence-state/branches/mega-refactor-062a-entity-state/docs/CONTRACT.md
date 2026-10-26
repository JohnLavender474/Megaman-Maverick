# Contract: mega-refactor-062a-entity-state

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-26

What must stay the same: everything the engine does (which `init`, `onSpawn`, `onDestroy`, component resets and
system changes run, in which order, and what `spawn`/`destroy` return), and the value of `spawned` on every path.
`dead` stays the same on every path except the three intended changes below, and on every path it is the true value:
`dead` is `false` exactly when the entity is in the engine after the next update.

## Automated checks

All on a clean build (`./gradlew clean engine:test core:test lwjgl3:jar`).

- [ ] The build passes and the jar builds.
- [ ] All engine and core tests pass, 0 failures. The counts go up only by the new tests.
- [ ] The compiler warnings are the same set as the baseline (counted at plan step 1).
- [ ] **Temporary contract suite** `TempOldEngineContractTest` with the frozen copy `TempOldGameEngine` (engine test
  sources) passes: on seeded random call sequences over several entities (`spawn`, `destroy`, `update`, `reset`,
  `dispose`, a refused `canSpawn`, and calls from inside `onSpawn`/`onDestroy`), the old and the new engine give the
  same ordered action log, the same return values and the same `spawned` after each call; at each update, for every
  entity that no call touched since the last call, the `spawned` after the update equals "not `dead`" before it (the
  new `dead` is the true value). The suite runs at least 10,000 sequences, and it reports how many times each odd path and each
  intended difference occurred (each must occur at least once, so the suite is shown to reach them). The suite and
  the frozen copy are deleted in the last commit before the merge; that commit runs the other checks again.
- [ ] **Permanent engine tests** pass: one per transition (scope item 2), the odd paths (the same actions as today,
  the true state, the log line), `reset()`/`dispose()` with live and queued entities, and the invariant test on
  seeded random sequences (`spawned` = in the engine; `dead` = not in the engine after the next update).
- [ ] **Core tests for the intended changes** (`dead` after `engine.reset()`; the ice shield after an unfreeze and a
  freeze in one frame) fail on the baseline before the change (the result goes in the process log) and pass after it.
- [ ] Greps: no write to `dead` or `spawned` outside the engine; no `onQueueToSpawn` or `onQueueToDestroy`; the
  state has no public setter (a core write does not compile).
- [ ] `core/src/test/resources/contact-rule-selection.txt` is unchanged.

## Manual playthrough

The owner plays these once, after the implementation, from the worktree. Add `--logTags GameEngine` to each command;
the alpha script writes the log to `logs.txt`. The log lines of the odd paths are the evidence for `062b`; keep the
file (or tell me where it is) after the play test.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim --logTags GameEngine`: freeze the `Met` with Frigid
  Shot: the ice shield shows; the `Met` thaws; freeze it again: the shield shows again. Kill it; it comes back when
  the camera leaves and returns.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room pickups --logTags GameEngine`: each pickup works; a killed
  `Met` can drop an item, and the item goes away when taken.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death --logTags GameEngine`: the death and the respawn
  (the level does not restart; the same level screen).
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death --logTags GameEngine`: lose all lives: the game
  over screen, then the save screen; then start any level from the menu. This is a second level start in one run:
  it must look and act as the first start of the run (the beam-in, when the pause button starts to work, no death
  fade).
- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --logTags GameEngine`: the boss intro, the fight, the
  defeat and the beam-out; then start the next level from the menu: the second level start looks the same as the
  first.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement --logTags GameEngine`: shoot (the shot limit works:
  shots that leave the screen free a slot), charge, switch weapons, pause.

## Behaviors to check by review

- The pool return of a rejected spawn (`MegamanMaverickGame.kt:933`) keeps its code; `spawned` gives the same value.
- `FreezableEntityHandler`, `QuestionBlock`, `WilyCapsuleTentacle`, `ReactorMan` and `WilyFinalBoss` read `spawned`
  and `dead` as before; the derived values are the same on their paths, except the ice shield case below.

## Intended changes

1. After `engine.reset()` and `dispose()`, every entity that was live or queued to spawn is `dead` (today it keeps
   `dead = false`). Megaman is `dead` from a level end until his next spawn is queued, at every level start, as at
   the first level start today. Checked by the core test and the second level start above.
2. A spawn call while a destroy is queued leaves the entity `dead` (today `false`, although the entity ends out of the
   engine). An enemy that is unfrozen and frozen again in one frame gets its ice shield at the next freeze (today no
   later freeze shows a shield). Checked by the core test; the frame timing is too short to play reliably.
3. If, in the destroy phase of an update, an entity gets a spawn call and then a destroy call, it is not `dead` (today
   `true`), because it spawns at the next update. Checked by the temporary suite (added 2026-10-26, owner approval).
