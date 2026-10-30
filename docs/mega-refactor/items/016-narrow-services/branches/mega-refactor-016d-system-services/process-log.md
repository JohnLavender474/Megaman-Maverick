# Process log: mega-refactor-016d-system-services

## Status

- State: active
- Goal: Contact rules and the pathfinder factory take narrow services in place of `MegamanMaverickGame`; the creation order in `create()` allows it
- Done: scan; `docs/SCOPE.md`, `docs/CONTRACT.md`, `docs/PLAN.md` approved 2026-10-29; plan steps 1 to 8 done; the owner's play test passed 2026-10-29
- Next: the owner's approval to merge into `mega-refactor-016-master`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-29

- Branch start on `master`. Worktree `.worktrees/mega-refactor-016d-system-services`, created from the parent `mega-refactor-016-master`.

## 2026-10-29: scan and draft scope (gate 1)

- Baseline at `48f7e72f5`: `./gradlew clean engine:test core:test lwjgl3:jar` passes; engine 575, core 259, 0
  failures; 81 unique warnings. Same as the parent baseline.
- `docs/SCAN.md`: the contact rules read `players` and `gravity`; the pathfinder factory reads `levelState`;
  `players` and `gravity` are built in `postCreate()`, after the engine; their constructors have no side effect; no
  real level has a `GravityChange` in a room with a player spawn; the list of what still names the host (input for
  `001b`).
- `docs/SCOPE.md` (draft): 7 open questions, each with a recommendation.
- Owner input through the coordinator (2026-10-29, verbatim), about `screenOnCompletion`: "I prefer the latter
  because it scopes the available data to only what's needed. Agent can weigh pros and cons of this choice".
  `SCOPE.md` now plans a narrow parameter; open question 4 compares `LevelState`, the weapons attained, a `Boolean`
  and a new value type, and recommends the weapons attained (`OrderedSet<MegamanWeapon>?`). New planned test
  `LevelCompletionScreensTest`.

## 2026-10-29: scope approved; draft contract (gate 2)

- Owner, through the coordinator (2026-10-29, verbatim): "On 2nd thought, I see no harm in continuing with the
  original recommendation of passing in `LevelContext`. I approve the scope". The coordinator: there is no
  `LevelContext` class; the owner means option (a), `screenOnCompletion: (LevelState) -> ScreenEnum`, with the level
  screen passing `game.levelState`. Questions 1 to 3 and 5 to 7 as recommended.
- `docs/SCOPE.md`: status approved; question 4 resolved as (a) with the owner's words, options (b) to (d) listed as not
  chosen; the goal, in-scope item 4, the tests and the API table changed to match. Decisions in `docs/NOTES.md`.
- `docs/CONTRACT.md` drafted.

## 2026-10-29: contract approved; draft plan (gate 3)

- Owner, through the coordinator (2026-10-29, verbatim): "contract approved". The approved version is the one
  committed in `eb6818355` (the worktree was clean).
- `docs/PLAN.md` drafted.

## 2026-10-29: plan approved

- Owner, through the coordinator (2026-10-29, verbatim): "plan approved", including the step 5 proof with the
  uncommitted test adapter.

## 2026-10-29: implementation, plan steps 1 to 7 (gate 4)

- Baseline at `48f7e72f5`: engine 575, core 259, 81 warnings.
- Step 1 (`74f07d031`): a `level-builder` subagent built `RefactorTest016.tmx`: room `gravity_change` (x 0, y 64,
  1536 x 448 px, 48 x 14 tiles), floor, ceiling and 2 walls, a player spawn at tile column 3, `GravityChange` areas A
  (`down`, columns 10-15), B (`up`, columns 16-21) and the `Met` area (`down`, columns 41-46) with a `Met` on the floor.
  `tiles` layer: 358 block cells, 0 missing, 0 painted outside blocks (its check script `S/check_tiles.py`; the CLI
  plus `S/add_tiles_and_specials.py`). No hand fix. `LevelDefinition` entry `REFACTOR_TEST_016`. Baseline smoke launch
  of the room: 20 seconds, no exception. Core 259.
  - Smoke launch method: the run scripts start Gradle first, which uses up the 20 seconds. The launches run the built
    jar from `assets/` with the same options as `run-game-alpha.sh` (`S/smoke.sh`).
- Step 2 (`5324b3616`): `players` and `gravity` are `val` fields; `GamePlayersFieldsTest`. Core 261. Smoke launch
  `DESERT_MAN room1`: no exception (the known `Cactus`/`Bat` spawner errors of the issue
  `2026-10-27-desert-man-enemies-on-wrong-layers.md` show, as on the base).
- Step 3 (`f48444862`): `megaContactRules(players, gravity, debugFilter)`; the 2 tests use real objects;
  `MegaContactRulesTest` (5 cases, through `table.dispatch`). Reference file unchanged. Core 266. 2 smoke launches.
- Step 4 (`d51426093`): `MegaPathfinderFactory(levelState)`; `MegaPathfinderFactoryTest` (3 cases). Plan deviation:
  the test checks the end of the path, not `targetReached`, because the engine `Pathfinder` returns `false` for a found
  path (backlog candidate in `docs/TODO.md`). Core 269. Smoke launches `DESERT_MAN room10`, async and sync.
- Step 5 (`5285f0bf9`): `LevelCompletionScreensTest` ran first on the old code with the adapter (a relaxed game mock
  that returns the test `LevelState`): 3 cases passed. Then `screenOnCompletion: (LevelState) -> ScreenEnum`,
  `RobotMasterLevelOnCompletion` takes `LevelState`, the 9 entries pass the object, `MLS` type and call (and its
  `LevelState` import); the adapter was removed before the commit. Core 272. Smoke launch `DESERT_MAN room16`.
- Step 6 (`3b0512c8c`): root `CLAUDE.md` ("Game object" 2 lines, "Levels and Tiled pipeline" 1 line).
- Step 7: all automated contract checks pass (`docs/CONTRACT.md`, "Results"). 4 smoke launches, no exception.

## 2026-10-29: owner-requested debug colors for `GravityChange` (outside the scope)

- Owner, through the coordinator (2026-10-29, verbatim), about manual check 2: "For this room, draw shapes/boundaries
  depicting where the different gravity settings should be felt. The run command can use `--debugShapes`".
- `5908d50dd`: a small change outside the scope, debug only, no gameplay change. `GravityChange` sets `body.drawingColor` in
  `onSpawn` from its direction (through `setGravityDirection` and `removeGravityDirection`, so a pooled area gets the
  right color on each spawn): `UP` green, `DOWN` red, `LEFT` yellow, `RIGHT` cyan, no direction white (before: red for
  all, the `Body` default). The body bounds copy the body color (`Body.getBounds(out)`), and the existing debug shape
  supplier draws them. `DrawableShapesSystem` draws debug suppliers only when `--debugShapes` is on; that code is not
  changed, so with the flag off nothing is drawn, as before. The body color has no other reader.
- Clean build passes; engine 575, core 272, 0 failures; 81 unique warnings, no new one. Smoke launches of
  `REFACTOR_TEST_016 gravity_change` with and without `--debugShapes`: 20 seconds each, no exception.
- Note: the first build attempt of this change ran while the owner ran the game from this worktree, and both Gradle
  runs used the same build folders; that build failed (missing classes and resources). The build after the owner's
  run passed. Builds and smoke launches in this worktree wait while the owner plays.
- `docs/CONTRACT.md` manual check 2: the command has `--debugShapes`, with the color legend.

## 2026-10-29: manual check 2 replaced

- Owner, through the coordinator (2026-10-29, verbatim): "For 016d, we'll test via GravitySwitcharoo instead".
  `docs/CONTRACT.md` manual check 2 (`REFACTOR_TEST_016 gravity_change`) is replaced by checks 3 (`REFACTOR_TEST_029
  gravity_switch`) and 4 (`WILY_STAGE_3 room5`). `MegaContactRulesTest` still covers the gravity-change rule for a
  player and a non-player. The test map `RefactorTest016.tmx` and its room stay.
- The play test found Megaman at the top of the screen with the camera oriented down in that room. A separate read-only
  agent checks whether that is a bug and, if so, files an issue on `master`. No change on this branch for it.

## 2026-10-29: play test passed (plan step 8)

- Owner, through the coordinator (2026-10-29, verbatim): "All tests pass". All manual checks in `docs/CONTRACT.md`
  pass; check 2 is replaced by the `GravitySwitcharoo` checks 3 and 4 (entry above).
- No temporary suite to delete (scope, open question 5).
- Automated checks again at `fb5c9f7b2`: clean build passes; engine 575, core 272, 0 failures; 81 unique warnings, no
  new one. No game launch.
- `docs/TODO.md` at the merge: 1 backlog candidate (`PathfinderResult.targetReached` has a misleading value; no
  production reader); no bug candidates. The camera question from the `REFACTOR_TEST_016 gravity_change` room is with a
  separate read-only agent (an issue on `master` if it is a bug).
