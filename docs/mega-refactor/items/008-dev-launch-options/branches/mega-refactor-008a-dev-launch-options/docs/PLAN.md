# Plan: mega-refactor-008a-dev-launch-options

How to do the work in `SCOPE.md`. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-22

All steps run in the worktree `.worktrees/mega-refactor-008a-dev-launch-options/`.

## Steps

### 1. Baseline

- Changes: none.
- Check: run the contract command with Corretto JDK 17. Write the test counts in `NOTES.md`.
- Done: yes (2026-10-22)

### 2. `LaunchOptions` and its checks, with unit tests

- Changes: add a `LaunchOptions` data class in `core` (package `com.megaman.maverick.game`, next
  to `MegamanMaverickGameParams`) that holds the raw option values, and a separate pure function
  `resolveLaunchOptions` (in `LaunchOptionsResolver.kt`) that checks them and returns the resolved start (screen, level to start or stage, room) or a list of
  errors. Enum lookup is case-insensitive and suggests the nearest name. No `Gdx` call, so it
  can be tested without a window. Add `launchOptions` to `MegamanMaverickGameParams`, with
  defaults that mean "no option given".
- Check: new Kotest specs in `core/src/test/` cover every case in the contract's "Automated
  checks". `core:test` passes.
- Commit: "Add LaunchOptions with checks and tests".
- Done: yes (2026-10-22)

### 3. Desktop launcher options

- Changes: in `DesktopLauncher.DesktopAppArgs`, add `--screen`, `--level`, `--room`, `--logTags`
  and `--autoPerf`. Fill `LaunchOptions` from them. Map `--runType test` to `--level TEST_1` and
  mark it deprecated in its description. On a check error, print the errors and the usage, and
  exit with code 2. Fix the `--logLevels` whitespace parse.
- Check: `lwjgl3:jar` builds; `--level MOONMAN` exits with code 2 and the suggestion;
  `--help`-style usage lists the new options.
- Commit: "Add dev launch options to the desktop launcher".
- Done: yes (2026-10-22)

### 4. Apply the options at boot

- Changes: in `MegamanMaverickGame`, replace the `RunType` switch at the end of `postCreate()`
  with the resolved start: start a level, or stage a level and open a screen, or open a screen.
  With no option, it opens `SIMPLE_INIT_GAME_SCREEN` as today. Add the `--logTags` tags to
  `GameLogger.tagsToLog`, or set `filterByTag = false` for `all`. Skip the auto-perf block when
  `--autoPerf false`.
- Check: the runs in the contract's "Manual playthrough" for no option, `--runType test`,
  `--level`, `--screen` with `--level`, `--logTags all` and `--autoPerf false`.
- Commit: "Apply launch options at boot".
- Done: yes (2026-10-22)

### 5. Screen prerequisites

- Changes: find which screens need state that a previous screen sets (for example a staged level
  for `ROBOT_MASTER_INTRO_SCREEN` and `LEVEL_SCREEN`). Add these rules to the `LaunchOptions`
  checks, so that such a screen without its prerequisite fails fast with an error that names it.
  Unregistered screens (`WILY_CASTLE_SCREEN`) are an error.
- Check: unit tests for each rule; the `WILY_CASTLE_SCREEN` run in the contract.
- Commit: "Fail fast on screen prerequisites".
- Done: yes (2026-10-22)

### 6. `--room`

- Changes: keep the room name as a one-time value in the game. When the level screen sets its
  player spawns, if the value is set: pick the first player spawn (by the current name order)
  whose rectangle is inside the named room, make it the current spawn, then clear the value. If
  no player spawn is inside that room, stop with an error that lists the rooms that have player
  spawns. Without the option, the start spawn rule does not change.
- Check: the two `--room` runs in the contract; a normal start of the same level still uses the
  first spawn by name.
- Commit: "Add the --room start option".
- Done: yes (2026-10-22)

### 7. Docs

- Changes: list the new options in the launcher line of the root `CLAUDE.md`.
- Check: the line names every new option.
- Commit with step 6, or alone.
- Done: yes (2026-10-22)

### 8. Contract checks

- Changes: none.
- Check: all "Automated checks" in `CONTRACT.md`, and the runs in "Manual playthrough" that I can
  do without playing (boot, the error runs, the screen that opens). Write the results in the
  process log.
- Done: yes (2026-10-22)

### 9. Play test (owner)

- Changes: none.
- Check: the owner plays the runs in "Manual playthrough" and "Behaviors to check".
- Done: yes (2026-10-22)

### 10. Merge

- Changes: follow "Branch merge" in `docs/mega-refactor/CLAUDE.md`, including the owner's
  approval and, if `master` changed, a merge of `master` into the branch and the checks again.
  On `master`, update `docs/issues/2026-10-22-literal-whitespace-regex.md` (the launcher part is
  fixed; `GameState.kt:293` is still open).
- Check: `master` builds with the contract command.
- Done: yes (2026-10-22)
