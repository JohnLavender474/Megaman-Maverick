# Process log: mega-refactor-019c-merge-spawn-queues

## Status

- State: merged
- Goal: one spawn queue (the engine queue); map spawns go to the engine in the spawn phase
- Done: planning (scope, contract, plan approved); steps 1 to 6
- Next: none (merged 2026-11-03)
- Blocked: none

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-03

- Branch start on `master`. Merge target: `master` directly, as for `019a` and `019b`.
- Decision on `master` before the start (item `019`, 2026-11-03): a destroyed entity respawns in the next tick at the
  earliest. This branch must not add a spawner pass after `engine.update`.
- Scan: only `spawns` and the engine queue are spawn queues. The second drain of `spawns` after `engine.update` always
  finds it empty. `playerSpawnsMan` picks checkpoints and spawns nothing. `SPAWN_NOW` already uses the engine queue.
  `MegaMapLayerBuildersParams.spawnsManager` has no reader. The `BEGIN_ROOM_TRANS` issue looks fixed by `019b`
  (inferred; needs a game check).
- Wrote the draft `docs/SCOPE.md` and the overlap in `docs/NOTES.md`.
- Owner's idea: rename `playerSpawnsMan` to `playerCheckpointMan`; added to the scope as open question 3.
- Scope approved (owner, 2026-11-03: "1. Yes. 2. Sure, go for it. 3. Yes, go for it."). Decisions in `docs/NOTES.md`;
  spatial index moved to backlog candidates. The `BEGIN_ROOM_TRANS` issue: `019b` intended change 4 and its
  `SpawnerReleaseTest` case already cover it, so the branch closes it at merge with no game check.
- Wrote the draft `docs/CONTRACT.md`.
- Contract approved (owner, 2026-11-03: "yes"). Wrote the draft `docs/PLAN.md` (6 steps).
- Plan approved (owner, 2026-11-03: "yes").
- Step 1 (baseline): clean build passes; 636 engine, 794 core and 436 smoke tests, 0 failures; 76 unique compiler
  warnings (scratchpad `019c-warnings-baseline.txt`). The `019b` count (110) used another method; this branch compares
  with its own baseline.
- Step 2: `SpawnsManager` takes a spawn function and calls it at once; a refused spawn calls `onSpawnRejected`.
  `Spawn.onRejected`, the screen `spawns` array, its two drains and its clear went. `SpawnerReleaseTest` uses the new
  hand-off (9 cases pass, unchanged). New `SpawnsManagerTest` (3 cases). With the `onSpawnRejected` call removed, one
  case of each test fails, so the tests can fail.
- Step 3: removed `MegaMapLayerBuildersParams.spawnsManager`; the three source sets compile.
- Step 4: `PlayerSpawnsManager` is now `PlayerCheckpointManager` (`git mv` of the class and its test),
  `playerSpawnsMan` is now `playerCheckpointMan`. One `sed` over the 3 files (25 references). The test diff shows only
  name lines; 13 cases pass.
- Step 5: `engine:test core:test core:smokeTest lwjgl3:jar` pass: 636 engine, 797 core, 436 smoke tests, 0 failures;
  76 unique warnings, none new; no diff in `engine/` or `contact-rule-selection.txt`. A `timeout 12` launch of each of
  the four contract rooms ran until the timeout with no exception. The only errors are the known map layer errors
  (`DuoBallCanon` in `WILY_STAGE_1`, `Cactus` and `Bat` in `DESERT_MAN`), which have issue files.
- Step 6: the owner's play test passes, all four manual checks (owner, 2026-11-03: "All tests pass").
- `master` changed `MegaLevelScreen.kt` and its tests (`118b`, `068a`) after the branch start, so `master` was merged
  into the branch (`fde663321`, no conflicts). Automated checks again: `engine:test core:test core:smokeTest
  lwjgl3:jar` pass: 636 engine, 851 core, 437 smoke tests, 0 failures; the same 76 unique warnings; no diff in
  `engine/` or `contact-rule-selection.txt`.
- Merge approved (owner, 2026-11-03: "yes"). Squash-merged into `master`.
