# Scope: mega-refactor-008a-dev-launch-options

What this branch changes and why. The user approves this file before `CONTRACT.md` is written.

- Status: approved
- Approved: 2026-10-22

## Goal

Start the game from the command line on a chosen screen, level or room, with clear errors for
bad values. This makes the play tests of later branches faster: start at the room to check,
without playing to it. This branch is the first part of `008`.

## Item

`008` (dev launch options and level staging). This branch takes the options that need no other
item first. The owner's staging rule (2026-10-22, from the review answer): "If the screen in the
arg is NOT the level screen but a level arg is supplied, then the level should be 'staged', i.e.
next load of level screen loads that level (assuming no other process has since overwritten the
staged level)".

## Current state

Scan of 2026-10-22 on `master` at `612edecf8`:

- `DesktopLauncher.DesktopAppArgs` (JCommander) has 25 options. None selects a screen, level or
  room. `--runType test` makes the game call `startLevel(LevelDefinition.TEST_1)`
  (`MegamanMaverickGame.kt:400-403`); otherwise the game opens `SIMPLE_INIT_GAME_SCREEN`.
- The staged level already exists: `setCurrentLevel` / `getCurrentLevel`
  (`MegamanMaverickGame.kt:708-710`, stored in the game property bag; `getCurrentLevel` throws if
  no level is set). `startLevel(null)` uses it (`:896-908`). Level select, the main menu and
  `startLevel(x)` write it.
- Log tags are fixed in code: `TAGS_TO_LOG` is an empty set (`:166`) and `filterByTag = true`
  (`:252-253`).
- Auto-perf always runs (`:452-480`). No option turns it off.
- `DesktopLauncher.kt` parses `--logLevels` with `replace("\\s+", "")`, which treats the pattern
  as literal text (`docs/issues/2026-10-22-literal-whitespace-regex.md`).
- Unknown enum values in `--performance` and `--runType` throw from `valueOf` with no usage text.
- `WILY_CASTLE_SCREEN` is in `ScreenEnum` but not registered
  (`docs/issues/2026-10-22-wily-castle-screen-not-registered.md`).
- The start spawn of a level is the player spawn whose name sorts first
  (`PlayerSpawnsManager.kt:30-43`).

## In scope

- A platform-neutral `LaunchOptions` class in `core` (inside or next to
  `MegamanMaverickGameParams`). `DesktopLauncher` fills it from new options.
- New options:
  - `--screen <ScreenEnum>`, case-insensitive;
  - `--level <LevelDefinition>`, case-insensitive;
  - `--room <name>`: on the first level start after boot, start at the first player spawn (by
    the current name order) that is inside that room; then forget the option;
  - `--logTags a,b,c` adds tags to `GameLogger.tagsToLog`; `--logTags all` sets
    `filterByTag = false`;
  - `--autoPerf true|false` (default `true`, as today).
- Staging rules:
  - `--level L` with no `--screen`: start level L at boot.
  - `--screen S` (not the level screen) with `--level L`: stage L, then open S. The next level
    start with no level argument loads L, unless something stages another level first.
  - `--screen` alone: open that screen.
  - `--screen LEVEL_SCREEN` with no level, or `--room` with no level: error.
- Checks, with exit code 2 and the usage text: an unknown enum value names the value, lists the
  allowed values and suggests the nearest one; an unregistered screen (for example
  `WILY_CASTLE_SCREEN`) is an error; a screen whose prerequisites are not met (for example a
  screen that needs a staged level when none is given) fails fast with an error that names the
  missing prerequisite; `--room` that matches no room with a player spawn fails fast with an
  error that lists the room names that have spawns. This matches the game today: a level with
  no player spawn fails at load (`PlayerSpawnsManager.popFirstSpawn()` reads index 0 of an empty
  array), on purpose.
- Keep `--runType test` working as an alias for `--level TEST_1`, and mark it deprecated in its
  description.
- Fix the `--logLevels` whitespace parse.
- Document the new options in the root `CLAUDE.md` (the launcher flags line).

## Out of scope

These need other items first, and come in later `008` branches:

- `--tmx` (needs a staged level that can hold a map override, and a loader for maps outside
  `assets/`).
- `--checkpoint` (needs named checkpoints, `036`).
- `--slot` / `--state` (needs the save slots, `007`).
- `--seed` (needs the random source, `011`).
- `--strictErrors` (needs the validator, `003`).
- TMX hot reload (needs a clean level teardown, `013`, `037`, `038`).
- Persisted settings (`034`).
- The `GameState.kt:293` whitespace bug (the same pattern, but in the save code, `007`).

## Affected areas and risks

- Files: `DesktopLauncher.kt`, `MegamanMaverickGame.kt` (params and the boot code at
  `:395-403`), a new `LaunchOptions` file, maybe `PlayerSpawnsManager.kt` or `MegaLevelScreen.kt`
  for `--room`, the root `CLAUDE.md`.
- With no new option given, the boot must stay exactly as today. That is the main contract
  check.
- `--room` touches the level start. A mistake there could change the normal start spawn. The
  option is only read when it is given.
- Some screens need state from the screen before them (for example the robot master intro reads
  the current level). Starting on such a screen without a level can fail.

## Overlap with other branches

- `019a` changes `MegaLevelScreen.render()`. If `--room` needs `MegaLevelScreen.kt`, it changes
  the level start code, not `render()`. The second branch to merge checks the merge.
- `002a` changes only build and CI files.

## Open questions

None open.

- (2026-10-22) `--screen` accepts any registered screen. If the screen's prerequisites are not
  met, the game fails fast with an error.
- (2026-10-22) `--room` needs a player spawn inside the room. A room without one is an error. The
  owner confirmed that failing at runtime without a player spawn is on purpose; the code confirms
  it (`MegaLevelScreen.kt:512-514`, `PlayerSpawnsManager.kt:35-43`).
