# Notes: 091 Check that a test room loads with no visible game window

Scan of 2026-10-31, based on `master` commit `dc6057a99`.

## What the launch checks today

A TMX file is data, so it does not compile. The Gradle build does not read it. The first code that reads it is the level
load, so a launch is the only current check that the room works. No test in `core/src/test/` loads a TMX file.

The launch shows that:

- the map parses and the layer builders run (`MegaLevelScreen.start()`, `buildLevel()`);
- with `--room`, the room exists and has a player spawn (`MegaLevelScreen.kt:447`; else an `IllegalStateException`);
- the player reaches `PLAYER_READY` (`PlayerSpawnEventHandler.kt:214`), with the start room entities spawned.

The launch does not show that:

- a spawner far from the player spawn works, because only entities near the camera spawn;
- an entity name is correct, because `SpawnersLayerBuilder` logs an error and skips a wrong name (root `CLAUDE.md`).

## Problems of the current launch

- The rule is `timeout 12 ./run-game-alpha.sh ...`, one launch for each room. At least 11 branch process logs record
  launches of this type.
- The window opens on the owner's desktop, and it can take the focus. The owner's input can then go to it.
- The level screen starts the music (`MegaLevelScreen.start()`), so a launch plays sound.
- Claude reads the log to find the result, because `timeout` always ends the run and the `run` block in
  `lwjgl3/build.gradle` has `setIgnoreExitValue(true)`. A crash and a pass can look the same from the exit code.
- A failed launch and a pass both need the full limit, or a log search. Timing from earlier launches: Gradle took 8 s to
  a failed room check, and `PLAYER_READY` came 5-6 s after the session start.

## Options

### A: hidden window and self-exit in the real launcher (recommended)

- `Lwjgl3ApplicationConfiguration.setInitialVisible(false)`. The method exists in the current `gdxVersion` (1.14.2).
- The real GL context, renderer, assets and level load run, so the check uses the same code as the game.
- A hidden window takes no focus and gets no keyboard input. Volume 0 stops the sound.
- The game exits when `PLAYER_READY` comes. A failure gives a non-zero exit code.
- The check needs a display, so it works in the owner's session and not in CI. CI is not the goal of this item.
- Risk: the game loop may not run for a hidden window. No launch tested this. A short spike must check it before the
  scope is final.

### B: Xvfb (`xvfb-run`)

- No game code changes. The window goes to a virtual display.
- `xvfb-run` and `Xvfb` are not installed on the owner's machine, and it adds a system dependency.
- It keeps the problems of the kill by `timeout` and of the exit code, unless the self-exit of option A is added too.

### C: static check with `003`

- `003` plans a validator that reuses the `core` code, with no window. It finds wrong names and bad enum values, which a
  launch does not find.
- It does not run the level load. So it adds to option A and does not replace it. It also has status `planned`, and it
  depends on `002`.

### D: headless boot with `006`

- `006` boots each level with no display, and it can run in CI. It is the complete answer.
- It needs an injectable batch, shape renderer and frame delta, and synchronous asset load. It has status `idea`, and it
  depends on `004`.
- If `006` is done, the command of this item can call it, and the hidden window mode can go.

## Change size of option A

`DesktopLauncher.kt`, one exit hook in `core`, one new script, a Gradle task if the script does not use the jar,
`docs/mega-refactor/CLAUDE.md`. The hidden window spike comes first.

## Rule changes after the merge

These still tell Claude to launch the game, and they change when this item merges:

- "Test maps" in `docs/mega-refactor/CLAUDE.md` ("A short launch of each new room").
- The memory `feedback_short_smoke_launches`.
- Any `PLAN.md` of a branch that is active at that time.
