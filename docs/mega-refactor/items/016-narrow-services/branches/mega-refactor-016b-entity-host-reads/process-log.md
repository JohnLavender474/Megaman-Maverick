# Process log: mega-refactor-016b-entity-host-reads

## Status

- State: active
- Goal: The host-only reads in entities get a home (`DrWily` screen switch, `Starkner` `paused`, `selectButtonAction`, debug shapes, `weaponsAttained`)
- Merge target: the parent branch `mega-refactor-016-master` (not `master`)
- Done: branch start; scan (`docs/SCAN.md`); `docs/SCOPE.md` (approved 2026-10-28); `docs/CONTRACT.md` (approved 2026-10-28); `docs/PLAN.md`; revision 2 of `SCOPE.md`,
  `CONTRACT.md` and `PLAN.md` (approved 2026-10-28)
- Done also: plan steps 1 to 7 (2026-10-28); the automated contract checks pass
- Done also: the owner's play test passed (2026-10-29); plan step 8: the temporary suite is deleted, the automated
  checks pass again
- Next: the owner's approval to merge into `mega-refactor-016-master` (squash in the parent's worktree; no merge into
  `master`)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-28

- Branch start on `master`. Worktree `.worktrees/mega-refactor-016b-entity-host-reads`, created from the parent `mega-refactor-016-master`.

## 2026-10-28: scan and scope (gate 1)

- Read the root `CLAUDE.md`, `docs/mega-refactor/CLAUDE.md` (with "Item start: merge target" and "Parent branches"),
  the `016` `ITEM.md` and `NOTES.md`, and the `016a` branch folder as the model.
- Baseline on the branch start `07307f156`: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean
  engine:test core:test lwjgl3:jar` passes; 571 engine and 225 core tests, 0 failures; 81 unique warnings. Same as
  the parent baseline in the brief.
- Scan (`docs/SCAN.md`): the 7 item reads are all still in the code; `WEAPONS_ATTAINED` is done by `016a`
  (`game.weaponsAttained`). Also found entity reads with no holder object yet (clock, scheduler, groups, functions
  that take `game`), which the item `NOTES.md` put in `016b` but `ITEM.md` does not.
- `docs/SCOPE.md` written (draft) with 9 open questions.
- `docs/TODO.md`: one bug candidate (`InsertionOrderPriorityQueue.size` is fixed at `0`; no visible effect).
- Out of scope but approved by the owner (2026-10-28, through the coordinator): the drawables map on the game is
  declared with `InsertionOrderPriorityQueue` values (the field, the return type of `getDrawables()` and the field in
  `MegaLevelScreen`), no cast, no behavior change. Written in `SCOPE.md` "In scope", item 5.

## 2026-10-28: scope approved; contract draft (gate 2)

- Owner, through the coordinator (2026-10-28, verbatim): "approved". Open questions 1 to 4 and 7 to 9 are accepted
  as recommended. Two changes:
  - Question 6 (owner, verbatim): "I'm not sure I agree with this. Forwarding patterns like this can obfuscate the
    actual source/target of things." No forwarding members: `game.paused` and `game.selectButtonAction` go, and every
    caller reads `game.clock.paused` or `game.players.selectButtonAction`. `pause()` and `resume()` stay as actions,
    because they also log, call the current screen's `pause()`/`resume()` and keep their early return. The `016a`
    forwards stay for `016c`. Recorded on `master` as `016` decision 11.
  - Question 5: the owner uses the pathfinding debug shapes now and then. The debug path stays as it is in this
    branch (out of scope); a separate research agent looks for a better approach. A task in `docs/TODO.md` says that
    its home must be decided before `016c`.
- `SCOPE.md` updated and marked approved; the answers are in `docs/NOTES.md` "Decisions".
- `docs/TODO.md`: the `InsertionOrderPriorityQueue.size` candidate is already filed as
  `docs/issues/2026-10-22-insertion-order-priority-queue-size.md`; the file points to it.
- `docs/CONTRACT.md` written (draft).

## 2026-10-28: contract approved; plan draft (gate 3)

- Owner, through the coordinator (2026-10-28, verbatim): "yes to both". `CONTRACT.md` is marked approved.
- Coordinator notes: (1) the process doc on `master` (`f6e9feb7c`) says that a request to run checks starts with the
  absolute worktree path as a `cd` command; the reports of this branch do that. (2) The pathfinding debug shapes get
  their own `fix/` branch from `master` (`--debugPathfinding`). It touches `Bat.kt`, `SubmarineJoe.kt`,
  `MegaPathfinderResultConsumer.kt`, `AbstractPathfindingSystem` and `createGameEngine()`. `016b` does not change those
  parts, so that the later merge of `master` into the parent and into this branch stays easy. The drawables change in
  `createGameEngine()` touches only the 3 lines of the drawables map.
- `docs/PLAN.md` written (draft).

## 2026-10-28: plan not approved; revision 2 of the `DrWily` design

- Owner, through the coordinator (2026-10-28, verbatim): "It doesn't seem like good design to tightly couple a flag
  for the game's ending screen to the level state. Is there a better approach to this?" A research agent wrote a note
  with 6 options. Owner (verbatim): "I like option c since it reuses the existing global events manager" and "016b
  can remove the dead flag". Recorded on `master` as `016` decision 12, which replaces decision 9.
- New design: `EventType.GAME_COMPLETE`; `DrWily` submits it under the same condition; `MegaLevelScreen` switches to
  `ENDING_SCREEN` in `onEvent`, like `END_LEVEL`. The switch moves from inside `engine.update` to `eventsMan.run()`
  of the same frame: an intended timing change with no visible effect (`CONTRACT.md` "Intended changes").
- Out of scope but approved by the owner (2026-10-28): remove the dead `endGame` read on `VICTORY_EVENT` in
  `MegaLevelScreen` (no sender sets `ConstKeys.END` on it).
- The scope changed, so `SCOPE.md`, `CONTRACT.md` and `PLAN.md` are drafts again (revision 2), for one combined
  re-approval. Changed: `SCOPE.md` "Item", "In scope" items 1 and 1a, the docs line, tests 5 and 6, "New API",
  "Removed API", "Out of scope", risks, and a new section "Revision 2"; `CONTRACT.md` intro, the `grep` check, the
  temporary suite, the permanent tests, manual check 1, a new manual check 7 (a robot master victory), "Behaviors",
  "Intended changes"; `PLAN.md` steps 3, 4 and 6.
- The key facts of the research are in `docs/NOTES.md`, "Research: the `DrWily` ending switch".
- A scratch test (deleted at once, `git status` clean) showed that a `MegaLevelScreen` builds with a relaxed game mock
  in a unit test and that `onEvent` runs. So the `GAME_COMPLETE` branch gets a permanent test.

## 2026-10-28: revision 2 approved; implementation (gate 4)

- Owner, through the coordinator (2026-10-28, verbatim): "approved to all". `SCOPE.md`, `CONTRACT.md` and `PLAN.md`
  (revision 2) are marked approved.
- The coordinator merged the parent into this branch (`7291362c8`). It brings the pathfinding debug fix from
  `master` (`--debugPathfinding`): `Bat` and `SubmarineJoe` no longer read `game.getShapes()`, and
  `createGameEngine()` and `MegamanMaverickGameParams` changed. So the 2 debug reads of the item are gone. On the
  parent after that merge: 575 engine and 225 core tests, 0 failures, 81 warnings. The plan and contract numbers are
  updated to 575 engine tests.
- Baseline on the merged branch (`be8e7302d`): clean build; 575 engine and 225 core tests, 0 failures; 81 unique
  warnings.
- Step 1 (`3ae2ffbad`): `GameClock` with `paused`; `game.clock`; the game member `paused` goes; 17 readers read
  `game.clock.paused` (a `sed` for the same rename in 10 files). Tests: `GameClockTest`, `StarknerPausedTest`. Core 230.
- Step 2 (`9095c46f4`): `MegaPlayers.selectButtonAction`; the game member and the `create()` write go; 11 sites read
  `game.players.selectButtonAction` (a `sed` for the same rename in 4 files). `MegaPlayersTest` gets one test. Core
  231.
- Step 3 (`b1430bce3`): `Temp016bFrozenDrWily` and `Temp016bDrWilyEquivalenceTest` (200 seeded sequences); it passes on
  the old code, with more than 50 sequences that reach the ending request and more than 20 that do not. Core 232.
- Step 4 (`09ec101f0`): `EventType.GAME_COMPLETE`; `DrWily` submits it; `MegaLevelScreen` listens and switches to
  `ENDING_SCREEN`; the dead `endGame` read goes. Tests: `DrWilyTest` (2), `MegaLevelScreenEventsTest` (2). The
  temporary suite passes unchanged. A probe showed that `MegaLevelScreen.init()` needs LibGDX in a unit test
  (`Gdx.files` is `null`), so the `VICTORY_EVENT` unit case is dropped, as the plan allows; manual check 7 covers it.
  Core 236.
- Step 5 (`7cbc89c3b`): the drawables map type. Plan deviation: `InsertionOrderPriorityQueue.poll()` is `T?` in Kotlin,
  so the 3 draw loops in `MegaLevelScreen` take `poll()!!`. Each loop polls only after `!isEmpty()`; a `null` would
  throw a `NullPointerException` at the same line in the old and the new code. The unused `java.util.*` import of
  `MegaLevelScreen` goes. Core 236, 81 warnings.
- Step 6 (`f0c2d7a1d`): root `CLAUDE.md` ("Game object": `players.selectButtonAction`, `clock`; "Events": level exits
  through the level screen, `GAME_COMPLETE`).
- Step 7: all automated contract checks pass (`CONTRACT.md` "Results"). Two smoke launches, no exception.

## 2026-10-29: manual check 5 corrected; parent merged again

- Owner, through the coordinator (2026-10-29, verbatim), about manual check 5 (`--level INTRO_STAGE --room room1`,
  pause, then exit): "This test won't work because the intro stage is not beaten yet, and so the exit button is denied
  (level must be beaten before it can be exited early; this rule is the same in the official MMX games)". The rule is
  in `LevelPauseScreen` (exit only when `game.isInExtraLevel()` or the current level is defeated). Check 5 in
  `CONTRACT.md` now uses the extra level `GAUNTLET_1`, `room1` (it has a player spawn). The play test is not finished.
- The coordinator merged the parent into this branch again (`2ee55ef9d`; it brings the `--heartTanks` and
  `--healthTanks` flags from `master`). After it, a clean build passed: 575 engine and 250 core tests, 0 failures, 81
  warnings.

## 2026-10-29: play test passed; temporary suite deleted (plan step 8)

- Owner, through the coordinator (2026-10-29, verbatim): "All tests for 016b pass". All 8 manual checks pass (the
  final boss run with `--weapons all --heartTanks all --healthTanks all`, and check 5 in `GAUNTLET_1`).
- `1b82fe890`: `Temp016bDrWilyEquivalenceTest` and `Temp016bFrozenDrWily` deleted in their own commit.
- Automated checks again: clean build passes; engine 575, core 249, 0 failures; 81 unique warnings, no new one; the
  `grep` checks give 0 lines; no `Temp016b` name left. No game launch.
- `docs/TODO.md` at the merge: one task (decide the home of the pathfinding debug shapes before `016c`; the
  `--debugPathfinding` fix on `master` has since removed the 2 `game.getShapes()` reads from `Bat` and `SubmarineJoe`),
  no backlog candidates, no new bug candidates (the `InsertionOrderPriorityQueue.size` bug is already filed).
