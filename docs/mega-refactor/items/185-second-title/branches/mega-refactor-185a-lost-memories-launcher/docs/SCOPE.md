# Scope: mega-refactor-185a-lost-memories-launcher

- Status: approved (merged 2026-11-23)
- Approved: 2026-11-23 (owner asked for a full first pass and accepted the recommendations with
  `/go-with-recs`; no HTML brief, see the process log)

## Goal

Two playable games from one repo: Megaman Maverick as today, and "Mega Man X: Lost Memories", a
splash screen that starts the X6 intro level. The run scripts take the game as a parameter.

## Item

Item `185`, step 1 of "Proposed scope" (the new game module, a shared `desktop-common` launcher
module, one small launcher module per game, run scripts that take the game), and the folded bug
`build.gradle:85` (`version = '$projectVersion'`). Builds per game (`build-game*.sh`, packr,
`construo`) are out of scope: see `TODO.md`.

## Current state

- One launcher: `lwjgl3/.../DesktopLauncher.kt` (title, icon, 45 flags, `MegamanMaverickGame`).
- `MegamanMaverickGame.postCreate()` registers every screen and opens `SIMPLE_INIT_GAME_SCREEN`
  (or `TEST_1`) on a default start (`MegamanMaverickGame.kt:524-528`).
- The save file and the player settings file have fixed names (`Constants.kt:5-8`,
  `LaunchOptionsResolver.kt:39,187`, `PlayerSettingsStore`).
- `X6_INTRO` ends on `EXTRAS_SCREEN`; a game over goes `GAME_OVER_SCREEN` → `SAVE_GAME_SCREEN`.
  Both are Maverick menus.
- The run scripts call `./gradlew lwjgl3:run`.

## In scope

1. `core`: a game title hook (`IGameTitle`) that `MegamanMaverickGameParams.title` holds. The
   title gives the save file name, the player settings file name, its own screens, the default
   start, and screen redirects (a Maverick screen key that the title replaces with its own).
   `MaverickTitle` keeps today's values.
2. `desktop-common` module: the shared launcher part (JVM restart, flag parsing, window config,
   window listener, dispose on crash). `lwjgl3` keeps the Maverick launcher, now thin.
3. `games/mega-man-x-lost-memories/`: Gradle projects `lost-memories-core` (the title, the splash
   screen) and `lost-memories-desktop` (its launcher).
4. Run scripts: one set per game, `run-<game>-<mode>.sh` (D9).
5. CI builds the Lost Memories jar too.
6. Folded bug: `version = projectVersion`.

## Out of scope

- Builds and bundles per game (TODO).
- Steps 2 to 6 of the item: spawn-name registry, scan roots, asset list per game, content
  extraction, the move of Maverick into `games/`.
- An assets folder of the new game: it runs from the root `assets/` (open question 2).

## Affected areas and risks

- Launcher refactor: a flag can get lost in the move. Mitigation: the flag class moves whole.
- Screen redirects in `setCurrentScreen`: a stopgap until the host contract (`001b`) and a level
  registry. Each redirect has a `TODO`.
- Save and settings files of Lost Memories are new files under `~/.prefs/`; Maverick's files do
  not change name.

## Overlap with other branches

See `NOTES.md`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` (new API needs a production caller) | no effect | each new type has a caller in this branch |
| `O7`, `O8` | helps | second title on the shared code |
| `O2-R2` (random calls) | no effect | |

## Open questions

None for the owner before the play test.

# Contract

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar lost-memories-desktop:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check runs as written. No game launch.

## Manual playthrough

1. `./run-mega-man-x-lost-memories-debug.sh`: window title "Mega Man X: Lost Memories"; splash screen;
   START opens the X6 intro level with X; the level end returns to the splash.
2. `./run-mega-man-x-lost-memories-debug.sh`: die until game over; the game over screen returns to the
   splash, not to the Maverick save screen; no music plays on the splash.
3. `./run-mega-man-maverick-debug.sh`: Maverick starts as before (title "Megaman Maverick", init screen).
4. `./run-mega-man-maverick-test.sh` and `./run-mega-man-maverick-test-coop.sh`: as before.
5. `./run-mega-man-maverick-alpha.sh --level DESERT_MAN`: the pass-through works. `./run-mega-man-x-lost-memories-alpha.sh`: the splash shows.

## Behaviors to check

- Maverick save and settings files keep their names and content.

## Intended changes

- New game "Mega Man X: Lost Memories"; the run scripts are renamed to one set per game (D9).
