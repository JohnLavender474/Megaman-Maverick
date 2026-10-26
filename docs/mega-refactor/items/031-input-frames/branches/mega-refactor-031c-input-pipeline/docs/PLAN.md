# Plan: mega-refactor-031c-input-pipeline

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch existed.

- Status: draft
- Approved: no

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from `build/test-results`.
Smoke launches are short (about 20 seconds); a clean exit, or the timeout with no exception, is a pass. The owner
plays the manual checks once, at the end.

Each step is its own commit. After each code step, the build and the tests pass, and the game plays the same.

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

New APIs and their first callers in this branch:

| API | First callers |
|---|---|
| Engine edge builder (held mask → frame, lock) | slot 0 in `render`, the lock in `onEvent` (step 4) |
| Keyboard adapter (held mask; reads keys through a key reader that production sets to `Gdx.input::isKeyPressed`) | slot 0 in `render` (step 4) |
| Gamepad-0 adapter (held mask, connect logic, `loadPrefsOnConnect`; reads the gamepad through a provider that production sets to gamepad 0) | slot 0 in `render`, `ControllerSettingsScreen` (step 4) |
| `IControllerPoller` facade over the slot-0 frame | the menus, the skip screens, `ScreenController` (step 4) |

The key reader and the gamepad provider are the seams that let the temporary suite and the adapter tests drive the
adapters with fake devices. Production passes the real devices.

## Steps

### 1. Branch start

- Changes: on `master`, in a docs-only commit: set the `031c` row in `ITEMS.md` to `active` with the start date; mark
  `031c` active in the `ITEM.md` branches line; set the branch `process-log.md` state to `active`. Then
  `git worktree add -b mega-refactor-031c-input-pipeline .worktrees/mega-refactor-031c-input-pipeline master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: no

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`. Launch
  `--screen OPENING_SCREEN`, `--screen CREDITS_SCREEN` and `--screen GAME_OVER_SCREEN` once each.
- Check: the build passes. Write the test counts (expected 481 engine, 161 core) and the warning count in the process
  log. Each screen opens with no exception (if one does not open directly, the contract gets another way to reach it,
  and the process log says so).
- Done: no

### 3. Temporary suite harness

- Changes: in the core test sources, a clearly temporary package (for example `input/frozen031c/`) with frozen copies
  of `ControllerPoller`, `MegaControllerPoller` and the device read of `ControllerButton.isPressed`, made to read fake
  devices (a set of keys down; a fake gamepad with a connected flag, a name, a mapping and buttons down; prefs). A
  seeded script driver that changes the fake devices, `loadPrefsOnConnect` and the lock each tick. A first test runs
  the frozen copies against the live poller on the same fake devices and checks equal statuses, to prove the harness.
- Check: build and tests pass. The harness test passes on every script.
- Done: no

### 4. The new pipeline, the lock and the facade

- Changes:
  - `E/input/`: the edge builder (the rule in `SCOPE.md`, in scope 1; no `enabled`; no allocation per tick).
  - Core: the keyboard adapter and the gamepad-0 adapter (the connect, disconnect and prefs logic of
    `MegaControllerPoller`, in the same order). Both read the codes from `ControllerButtons`.
  - `MegamanMaverickGame`: create the adapters and slot 0's edge builder in `create()`; in `render`, at the old poll
    point, build slot 0's frame from the keyboard mask OR the gamepad mask; `onEvent` sets slot 0's lock in place of
    `controllerPoller.on`; `controllerPoller` becomes the facade over the slot-0 frame (with a `TODO` for the
    menu-input branch). `ControllerSettingsScreen:224` sets `loadPrefsOnConnect` on the gamepad adapter.
  - Tests: the temporary suite compares the new pipeline with the frozen copies on every tick of the seeded scripts,
    with the named cases from `CONTRACT.md`. Edge builder tests. Facade tests.
- Check: build and tests pass; the temporary suite passes. Short launches of `REFACTOR_TEST_029 --room movement` and
  of `./run-game-alpha.sh` (start screens to the main menu) show no exception.
- Done: no

### 5. Removals and moves

- Changes: remove `ControllerPoller`, `MegaControllerPoller`, `IControllerButton`, the device reads and `enabled` in
  `ControllerButton`, `alternateActuators` (with a `TODO` in `ScreenController` for a touch adapter in the mobile
  build), and `MegaLevelScreen.controllerPoller`. Move `ControllerUtils` to core, unchanged. Move the cases of
  `MegaControllerPollerTest` to gamepad adapter tests; add keyboard adapter tests. `InputFrameTest` loses its
  comparison with the live poller; the harness test of step 3 goes (the live poller is gone).
- Check: build and tests pass; the temporary suite passes. Greps: no `Gdx.input` and no `Controllers` in
  `engine/src/main`; none of the removed names left. Short launches as in step 4.
- Done: no

### 6. Contract checks

- Changes: none (fixes only if a check fails).
- Check: every automated check in `docs/CONTRACT.md` on a clean build. Write the results in the process log. Then give
  the owner the manual checks as a list of commands, run from the worktree.
- Done: no

### 7. Owner's play test

- Changes: none, unless the play test finds a problem.
- Check: the owner plays the manual checks in `docs/CONTRACT.md` and approves the merge.
- Done: no

### 8. Remove the temporary suite and merge

- Changes: the last commit before the merge deletes the frozen copies and the temporary suite.
- Check: build and tests pass without them. Then follow "Branch merge" in `docs/mega-refactor/CLAUDE.md`.
- Done: no
