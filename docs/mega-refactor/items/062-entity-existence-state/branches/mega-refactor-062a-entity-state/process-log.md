# Process log: mega-refactor-062a-entity-state

## Status

- State: merged
- Goal: one engine-owned `EntityState` on each entity, written only by the engine on every path
- Done: planning (scope, contract, plan approved); steps 1 to 7
- Next: none (merged 2026-10-26)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- Branch start on `master` at `cd046dade`. Worktree `.worktrees/mega-refactor-062a-entity-state`.
- Scan: the engine spawn and destroy paths, the `spawned` and `dead` writers and readers (after `061c`), the level
  start window, `FreezableEntityHandler`. Findings: `state` is taken in 55 files; the four states are the four
  combinations of "applied now" and "will be live", so `spawned` and `dead` derive from one state; the derived `dead`
  differs from today on `reset`/`dispose` and on a spawn call while a destroy is queued (both: today `false` for an
  entity out of the engine). Draft `docs/SCOPE.md` with 5 open questions.
- Owner answers on the draft scope: the name `EntityExistenceState` (property `existenceState`); the true `dead` on a
  spawn while a destroy is queued; the `reset`/`dispose` fix is intended (benefits and consequences written down);
  the odd paths are logged here, and a new branch (`062b`) adds the guards; the pooled-reuse check waits for `019b`.
  All scope questions closed.
- Owner: prove the engine's behavior with tests, not guesses or play only. Scope item 6 now has a temporary
  old-versus-new contract suite (`TempOldGameEngine`, `TempOldEngineContractTest`), permanent engine tests and core
  tests for the intended changes that run on the baseline first.
- Scope approved (owner, 2026-10-26: "Yes, scope approved"; the testing request was meant for `062b` and applies to
  both). Draft `docs/CONTRACT.md`: the automated checks (with the temporary suite and the baseline-first core tests),
  six manual checks with `--logTags GameEngine`, the two intended changes.
- Contract approved (owner, 2026-10-26: "yes"). Draft `docs/PLAN.md` (7 steps). A compiler probe showed that an interface member cannot be `internal`, so `GameEntity` holds the `internal set` and the engine writes through one cast.
- Plan approved (owner, 2026-10-26: "yes").
- Step 1 (baseline, `cd046dade` + docs): clean build passes; 506 engine and 160 core tests, 0 failures; 110 unique
  compiler warnings (sorted list in the scratchpad, `062a-warnings-baseline.txt`).
- Step 2 (core tests on the baseline, not committed yet): `CT/entities/EntityExistenceTest.kt`, with a real
  `GameEngine` built with the game's `dead` callbacks. Both tests fail on the baseline as the scope predicts:
  - "engine reset leaves an entity that was live dead": `spawned` is `false`, then `dead` expected `true` but was
    `false` (line 46).
  - the ice shield: the shield is live after the first freeze; after an unfreeze and a freeze in one frame it is out
    of the engine; at the next freeze, `shield.spawned` expected `true` but was `false` (line 70). So the shield bug is
    real on the baseline, not only inferred.
- Step 3 (temporary suite on the baseline): `ET/TempOldGameEngine.kt` (today's `GameEngine`; the only changes: the
  class name, `spawned` kept in an identity map, three read-only queue queries for the counts) and
  `ET/TempOldEngineContractTest.kt` (10,000 seeded sequences, 20 to 60 calls over 4 to 8 entities, nested calls from
  `onSpawn`/`onDestroy`, nested resets during an update). Result: passes in about 2 s with 0 `dead` differences;
  counts: spawn on a live entity 31,437; spawn while a destroy is queued 3,992 (live) and 10,315 (unspawned); destroy
  of an unspawned entity 72,306; deferred reset 3,970. Mutation check: with destroys before spawns in the frozen copy,
  the suite fails at once (sequence 0, step 24, the action logs differ); the change was undone.
- Step 4 (in progress, not committed): the engine state is in (`EntityExistenceState`, `existenceState`, the writes,
  the derived `spawned` and `dead`, the log lines; the old callbacks go). The temporary suite found two things:
  - A bug in my first `destroyNow` rule (it set `SPAWN_QUEUED` when a spawn was queued, also when a destroy was queued
    too). Fixed: `SPAWN_QUEUED` only when a spawn is queued and no destroy is queued.
  - A third kind of `dead` difference that the scope did not list: during the destroy phase of an update, `spawn(X)`
    then `destroy(X)`. The destroy is applied in the same phase, the spawn at the next update, so `X` will be live;
    today's `dead` is `true` (the last call was a destroy), the new `dead` is `false` (the true value). 61 times in
    10,000 sequences. Waiting for the owner's decision (it changes the contract).
  - The suite now checks the truth in both directions: at each update, for every entity that no call touched, the
    `spawned` after the update equals "not `dead`" before it. Result: passes; 611,549 truth checks; differences after
    a reset 42,823, old `false`/new `true` 21,407, old `true`/new `false` 61.
- Owner approved intended change 3 (2026-10-26, "yes"); `docs/SCOPE.md` and `docs/CONTRACT.md` updated: the rule is
  "the new `dead` is always the true value", checked by the temporary suite at every update. The suite now also
  requires the old-`true`/new-`false` difference to occur at least once.
- Step 4 done. `destroyNow` sets `SPAWN_QUEUED` only when a spawn is queued and no destroy is queued (a plan detail;
  the plan's rule list did not cover a destroy queued again during the destroy phase). Permanent tests:
  `ET/entities/EntityExistenceStateTest.kt` (14 tests: the normal life, the state inside `onSpawn`/`onDestroy`, a
  refused spawn, second calls, a spawn and a destroy of an unspawned entity, the four odd paths with their actions,
  state and log line, a spawn and a destroy in the destroy phase, `reset`, a deferred `reset`, `dispose`, and the
  invariant on 2,000 seeded sequences: `spawned` = `engine.contains`, not `dead` = in the engine after the next
  update). The core `EntityExistenceTest` (step 2) passes now; its engine is built with no callbacks (the one line
  that changed). Tests: 521 engine (506 + 1 temporary + 14), 162 core (160 + 2), 0 failures. Temporary suite counts
  as above.
- Step 5 (contract checks, clean build): build and jar pass; 521 engine and 162 core tests, 0 failures; the 110
  warnings are the same set as the baseline (5 lines in `MegamanMaverickGame.kt` moved up by 2 after the two removed
  callback lines); `contact-rule-selection.txt` unchanged; greps: no write to `dead` or `spawned` outside the engine,
  no `onQueueToSpawn`/`onQueueToDestroy` outside the frozen copy; a probe file in `core` that writes
  `existenceState` and `dead` does not compile ("it is internal in `GameEntity`", "'val' cannot be reassigned"),
  removed. Launches with `--logTags GameEngine` of `REFACTOR_TEST_029` `enemy_aim`, `pickups`, `player_death`,
  `movement` and `INTRO_STAGE --room room12`: no exception until the timeout. The log file is written by an async
  writer that the timeout kill stops, and each run deletes `assets/logs.txt` at start, so the owner copies the file
  after each run. Waiting for the owner's play test.
- Test map change (owner's request): in `RefactorTest029.tmx`, room `enemy_aim`, the `Met` (object 78) moves from
  `x=3968` to `x=3840`. The room is 24 tiles wide and the camera 16, so at the room's right end the camera's left edge
  is at `x=3904`; the old position was always in the camera, so a killed `Met` could never respawn (its spawner needs
  the camera to leave its bounds and enter again). The room is also in the `029` contracts (`Met` faces and shoots
  at Megaman); that check still works. Launch of the room: no exception until the timeout.
- Step 6: the owner's play test is done (owner, 2026-10-26: "All tests done"; all six manual checks). The six log
  files have no `GameEngine` line: no odd path happened in these runs. Evidence and its limits in `docs/NOTES.md`.
  The log files moved to the scratchpad (`062a-play-logs/`). Waiting for the owner's merge approval.
- Step 7: the owner approved the merge (2026-10-26, "yes, approved"). The temporary suite (`TempOldGameEngine.kt`,
  `TempOldEngineContractTest.kt`) is deleted in the last commit. Clean build passes; 520 engine (506 + 14) and 162
  core tests, 0 failures.
