# Plan: mega-refactor-031f-menu-input

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch existed. It follows the
approved `docs/SCOPE.md` and `docs/CONTRACT.md` (both 2026-11-01). All line numbers in this file are from `master` at
`5c02dc8aa`. `master` moved after the contract: the `051` parent merged (`e12c880f5`), and `084g` is active. So if a
number does not fit, find the place by its content.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "yes")

## Rules for every step

- Build and test command, from the worktree, on a clean build:
  `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar 2>&1 | tee build.log`.
  Read the test counts from `engine/build/test-results/test/` and `core/build/test-results/test/`.
- Warnings: after each build, this command gives no output:
  `grep '^w:' build.log | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u | comm -13 build-baseline-warnings.txt -`. It shows only
  new lines. A line can leave the list.
- Each code step is one commit. After each code step, the build passes and all tests pass. Each commit message ends with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. The same commit adds a dated entry to the branch
  `process-log.md` and updates its status block.
- One test case per `T` ID of the contract. Each step gives the count that it must reach as `Eb + n` and `Cb + n`, where
  `Eb` and `Cb` are the engine and core counts of step 1. Do not use the numbers `600`, `610`, `528` or `548` of the
  contract (see "Line numbers and contract text").
- Test-driven order. Kotest here does not accept the Gradle `--tests` filter, so each test-first run is a full
  `engine:test` or `core:test`. A test does not compile against an API that does not exist. So first add the signature
  with an empty body or today's rule, run the tests, and see the listed tests fail. Then write the code. Do not commit
  that first body. Write in the process log which tests failed before the code, which passed, and each prediction that
  was wrong (`031e` step 9 had 1 wrong prediction; a difference is a log line, not a failure).
- A test run of a mutation is not committed. It proves that a test that passes on an empty body can fail.
- No code comments. The only exception is a `TODO` that names an accepted edge case: when it happens, and where the
  follow-up is tracked. Steps 8 and 10 add the 2 `TODO`s that `SCOPE.md` and the contract name. Open question 3 asks about a
  third.
- Write code only when production code calls it. A test alone is not a caller. See the API table.
- Smoke launches come after a compile, and each one uses `timeout 25`. From the worktree:
  - `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew lwjgl3:classes`
  - `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 timeout 25 ./run-game-alpha.sh <arguments>`

  A clean exit is a pass; do not investigate a clean exit. An exit by `timeout` with no exception in the console or in
  `assets/logs.txt` is also a pass. The agent cannot press keys in the game, so a smoke launch never drives a menu and
  never presses `Esc`. **No launch passes `--resetBindings`.**
- Never run a bare `git stash`. Stage only named paths (`git add <path> ...`), never `git add -A` or `git add .`, in the
  main checkout and in the worktree. `build.log` and `build-baseline-warnings.txt` are not git-ignored (`git check-ignore`
  gives no output for them), and they are not committed. `assets/*-diagnostics.txt` is ignored (`.gitignore:183`).
- On the branch, write docs only inside the branch folder.
- Only the paths in contract checks A6, A7 and A8 change.

### `~/.prefs`

The agent must never read, write or remove anything in `~/.prefs` or `~/.prefs.bak-*`. Only the owner runs S1 and X1.

The game process itself reads `~/.prefs` in a launch. These facts were checked on `5c02dc8aa`:

- **Reads:** `run-game-alpha.sh` passes `--loadKeyboardPrefs`, and `applyBootSettings` opens the slot file at each boot. This
  is the game's own read. No agent command opens the folder.
- **Writes:** a `grep` for `.flush()` gives only `MegamanMaverickGame.kt:853` (`saveState`) and 6 lines in
  `PlayerSettingsStore.kt`, all from a menu action, plus the `RuntimeDiagnostics` file writer. A launch with no input writes
  nothing. `clear()` runs at boot only with `--resetBindings`.
- **`Esc` adds no write:** `resetPlayerProgress()` (`:807-814`) changes memory only, and no `onBack` calls a save.
- **Tests:** `core:test` uses `FakePreferences` behind a mock `Gdx.app`, so it does not touch `~/.prefs`.

### Paths

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`,
`ET/` = `engine/src/test/kotlin/com/maverick/game/engine/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`,
`G` = `C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`, `LPS` =
`C/screens/menus/level/LevelPauseScreen.kt`, `CSS` = `C/screens/menus/ControllerSettingsScreen.kt`, `GSP` =
`C/screens/utils/GameSettingsPanel.kt`, `LPR` = `C/screens/levels/LevelPauseRules.kt` (new).

### New APIs and their first production callers

| API | Step | First production callers |
|---|---|---|
| `MegaPlayers.menuInput` | 2 | `MegaMenuScreen` (2 reads); step 3 adds the 6 skip screens and `ScreenController` |
| `SystemButton` (`BACK`) in `C/input/SystemButton.kt` | 4 | `G` (builder), `KeyboardInput.systemHeld()` |
| `KeyboardInput.systemHeld()` | 4 | `G.render` |
| Private `systemFrameBuilder` in `G` | 4 | `G.render` |
| `AbstractMenuScreen.backRequested()`, `onBack(delta)` | 5 | `AbstractMenuScreen.render`; overrides: `MegaMenuScreen` in step 5, the menus in steps 6 to 8 |
| `game.systemInput` | 5 | `MegaMenuScreen.backRequested` |
| `isPauseRequested(...)` in `LPR` | 9 | `MLS.render` |
| `queuePause(queue, locked, pause)` in `LPR` | 10 | `MLS.render` |
| `game.inputLocked` | 10 | `MLS.render` (2 lines) |

The check that no step adds code with no caller:

- Every line of the table has its production caller in the same step.
- 1 result has no reader for 1 commit: step 4 builds the system frame in `G.render`, and step 5 adds its first reader. The
  order of `SCOPE.md` question 11 causes this. `game.systemInput` is not added until step 5, so no unread API exists.
- `onBack` has an empty default, so step 5 changes no behavior. Steps 6 to 8 give it its bodies.
- `T/screens/levels/LevelPauseRulesTest.kt` and `T/screens/menus/MegaMenuScreenTest.kt` are test code. They are the 2 seams
  of contract question 3.
- `LPR` is a new production file. Contract A7 allows it ("the new file of open question 3").

Removed: `game.controllerPoller`, `C/input/FrameControllerPoller.kt`, `T/input/FrameControllerPollerTest.kt`,
`E/controller/polling/IControllerPoller.kt` and its folder, the 2 imports in `G`, `selectionButtons`, `START_BUTTONS`,
`END_BUTTONS`, `EARLY_EXIT_BUTTONS`, and `MLS.isPauseButtonRequested()`, which `isPauseRequested` replaces.

### Line numbers and contract text

The citations in `SCOPE.md` and `CONTRACT.md` that moved. The other citations that a step uses are the same at
`5c02dc8aa`.

| File | Old | Now | What |
|---|---|---|---|
| `G` | `:42`, `:91` | `:44`, `:93` | The 2 poller imports |
| `G` | `:573` | `:572` | The label |
| `G` | `:574` | `:573` | The slot-0 build |
| `G` | `:578` | `:577` | `screenController` |
| `G` | `:586`, `:590` | `:585`, `:589` | `screen.render`, `screen.draw` |
| `G` | `:616` | `:615` | `events.run()` |
| `G` | `:680-681` | `:679-680` | `TURN_CONTROLLER_OFF` |
| `G` | `:463` | `:462` | `LevelSelectScreenV2` registered |
| `MLS` | `:755` | `:754` | Boss-defeat lock, also in the issue file |
| `MLS` | `:862-864` | `:860-862` | `isPauseButtonRequested` |
| `MLS` | `:876` | `:874` | The pause test |
| `MLS` | `:878` | `:876` | The queued pause |
| `MLS` | `:886`, `:912` | `:884`, `:910` | The paused blocks |
| `MLS` | `:910`, `:941` | `:908`, `:939` | `engine.update`, `pauseScreen.render` |
| `LPS` | `:436-441` | `:438-443` | `isInteractionAllowed` |
| `LPS` | `:526` | `:528` | `setCurrentButtonKey` |
| `LPS` | `:538` | `:540` | The resume in `onAnySelection` |
| `LPS` | `:542-544` | `:544-546` | The slide tests |

Contract text that no longer fits is in open question 1. In short:

- **A2, A3, A4:** the base numbers are out of date, and the contract says so ("If `mega-refactor-051-master` reaches
  `master` first"). `docs/mega-refactor/process-log.md` gives engine 621, core 521, 95 unique warnings on the parent before
  `e12c880f5`. Step 1 measures the real base. Then A2 is `Eb + 10`, A3 is `Cb + 20`, and T27 to T31 stay (the minimum of
  `Cb + 15` was for option b, which the owner did not take).
- **A11, second half:** `grep -rn 'isAnyJust'` does not match `isAnyKeyJustPressed`. At `5c02dc8aa` the only matches are the
  poller interface, its test and its 4 reads. So after step 11 the correct result is no output, not the `InputExtensions.kt` line.
- **R6, first sentence:** the order of tests moves from `MLS:874` into `isPauseRequested`, as contract question 3 says. The
  function keeps the order and adds only `!locked`. R6 is read on the function body.

## Steps

### 0. Branch start

- Files: in the main checkout, on `master`: this `docs/PLAN.md`, `docs/mega-refactor/ITEMS.md`,
  `docs/mega-refactor/items/031-input-frames/ITEM.md`, the branch `process-log.md`.
- Change: one docs-only commit. Stage only these 4 paths:
  - this file: `- Status: approved` and `- Approved: <date>`;
  - `ITEMS.md`: the `mega-refactor-031f-menu-input` row to `active`, with the start date;
  - `ITEM.md`: in the "Branches" line, `031f` (planned) becomes (active);
  - the branch `process-log.md`: state `active`, a dated entry for the start.

  Item `031` merges each branch directly into `master` (squash), as `031a` to `031e` did (`SCOPE.md` question 11). Then, from
  the main checkout: `git worktree add -b mega-refactor-031f-menu-input .worktrees/mega-refactor-031f-menu-input master`.
  Write the base commit in the log.
- Tests first: none.
- Check: `git worktree list` shows the worktree on the branch. `git status` in the main checkout shows no staged change,
  and the owner's 2 local edits stay unstaged.
- Contract checks: none.
- Commit, on `master`: `031f start: mega-refactor-031f-menu-input`
- Done: no

### 1. Baseline

- Files: the branch `docs/NOTES.md`, `docs/TODO.md`, `process-log.md`. No code.
- Change:
  - In the worktree, run the build command.
  - `grep '^w:' build.log | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u > build-baseline-warnings.txt`.
  - Write in the process log: `Eb`, `Cb`, the failures, the number of unique warning lines, and the base commit. These
    are the numbers from `master` at branch start. Do not assume `600` and `528`.
  - Copy "Overlap with other branches" of `docs/SCOPE.md` to `docs/NOTES.md`. Mark the 2 `051` lines as history. Read the
    folders of the other active branches again (`git worktree list`) and add the result. `084g` at this commit: see
    "Overlap at this commit" below.
  - Copy the bug candidates and backlog candidates of `docs/SCOPE.md` to `docs/TODO.md`, and add the bug candidate of contract
    question 4. Step 14 turns each one into a file or an item.
- Tests first: none.
- Check: the build passes, with 0 failures. `git status --short` in the worktree shows only the 3 docs paths and the 2
  untracked files `build.log` and `build-baseline-warnings.txt`. If the paths in the warning list are for the worktree, keep
  them as they are: step 14.9 normalizes them.
- Contract checks: the baseline for A1 to A4.
- Commit, on the branch: `031f: baseline, overlaps and candidates`
- Done: yes

### 2. `MegaMenuScreen` reads a frame

- Files: `C/players/MegaPlayers.kt`, `C/screens/menus/MegaMenuScreen.kt`, `T/players/MegaPlayersTest.kt`, new
  `T/screens/menus/MegaMenuScreenTest.kt`.
- Change:
  - `MegaPlayers`: `val menuInput get() = inputs[PlayerSlot(0)]`.
  - `MegaMenuScreen:21`: `game.players.menuInput.let { ... }`. The 4 `when` lines do not change.
  - `MegaMenuScreen:31`: `game.players.menuInput.let { it.isJustReleased(MegaControllerButton.START) ||
    it.isJustReleased(MegaControllerButton.A) }`.
  - Remove `selectionButtons` (`:19`) and the `gdxArrayOf` import (`:5`). A `grep` gives 0 other readers.
- Tests first:
  - `MegaPlayersTest`: T17, T18. `MegaMenuScreenTest`: T27 to T31, on a test subclass, with
    `mockk<MegamanMaverickGame>(relaxed = true)` and `every { game.players } returns players`, where `players` is a real
    `MegaPlayers`. The frames are real. No test stubs a function that gives an `InputFrame`.
  - Run 1: `menuInput` gives `InputFrame.RELEASED`, and the mock has no poller stub. Prediction: T17, T27, T28 and T30 fail,
    because the relaxed mock poller gives `false`; T18, T29 and T31 pass.
  - Run 2: add `every { game.controllerPoller } returns FrameControllerPoller { players.inputs[PlayerSlot(0)] }`. This is
    the production poller. Prediction: T27 to T31 pass on the old reads; only T17 fails. This run proves that the menu
    rules are the same before the code. Write both lists in the log.
  - Then the code, and remove the stub line. Prediction: all 7 pass with no other change to the test.
- Check: the build command passes. Engine `Eb`. Core `Cb + 7`. No new warning line. Smoke launch: `--screen MAIN_MENU_SCREEN`.
- Contract checks: T17, T18, T27 to T31; R3 and R10 for this read; the `selectionButtons` part of A11; the `menuInput` part of
  A17. Manual proof later: V1 to V4.
- Commit: `031f: MegaMenuScreen reads a frame through menuInput`
- Done: yes

### 3. The skip screens and `ScreenController` read a frame

No test can read these screens without a game. `SCOPE.md` "Frame order" shows that the reads are the same value, and
R3 checks the buttons and edges.

- Files and the exact change:
  - `C/screens/other/LogoScreen.kt:129`: `game.players.menuInput.isJustReleased(MegaControllerButton.START)`.
  - `C/screens/other/AbstractStoryScreen.kt:110`: the same read, after `!done &&`.
  - `C/screens/other/GameOverScreen.kt:85`: the same read.
  - `C/screens/other/GetWeaponScreen.kt:243`: `game.players.menuInput.let { it.isJustReleased(MegaControllerButton.A) ||
    it.isJustReleased(MegaControllerButton.B) || it.isJustReleased(MegaControllerButton.START) }`. Remove `END_BUTTONS`
    (`:58-62`). The `gdxArrayOf` import stays: `:50` uses it.
  - `C/screens/other/SimpleInitGameScreen.kt:59`: `START`, `A` or `B` released, in the same form. Remove `START_BUTTONS` (`:19-20`)
    and the `gdxArrayOf` import (`:4`).
  - `C/screens/other/CreditsScreen.kt:89`: `game.players.menuInput.let { it.isJustPressed(MegaControllerButton.START) ||
    it.isJustPressed(MegaControllerButton.SELECT) }`. This stays on press. Remove `EARLY_EXIT_BUTTONS` (`:52-55`) and the
    `gdxArrayOf` import (`:10`).
  - `C/controllers/ScreenController.kt:193`: `game.players.menuInput.isPressed(it)`. The `TODO` at `:145` stays.
- Tests first: none. Each read keeps its test, and the file has no test seam (`SCOPE.md`, "Tests"). No new helper:
  each test has 2 or 3 buttons.
- Check: the build command passes. Engine `Eb`. Core `Cb + 7`. No new warning line.
  `grep -rn 'controllerPoller' core/src/main/kotlin/com/megaman/maverick/game/screens core/src/main/kotlin/com/megaman/maverick/game/controllers`
  gives no output. Smoke launches: `--screen SIMPLE_INIT_GAME_SCREEN`, `--screen CREDITS_SCREEN`.
- Contract checks: R3 (all 9 reads); A11 (first half); A17. Manual proof later: K1 to K6, W1.
- Commit: `031f: the skip screens and ScreenController read a frame`
- Done: yes

### 4. The system frame

- Files: new `C/input/SystemButton.kt`, `C/input/KeyboardInput.kt`, `G`, `T/input/KeyboardInputTest.kt`.
- Change:
  - `SystemButton`: `enum class SystemButton(override val index: Int) : InputButton { BACK(0) }`.
  - `KeyboardInput`: `fun systemHeld() = if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) 1L shl SystemButton.BACK.index else 0L`.
    `held()` does not change.
  - `G:226`: add `private val systemFrameBuilder = InputFrameBuilder(SystemButton.entries)` next to `slot0FrameBuilder`.
  - `G:573`: add `systemFrameBuilder.build(keyboardInput.systemHeld())` on the next line, in the same diagnostics entry and
    the same `else` block. It has no `locked` write, now or later.
- Tests first: T11 to T16 in `KeyboardInputTest`, with the real `Input.Keys` constants and the real `InputFrameBuilder`. The 13
  cases stay. Prediction with `systemHeld() = 0L`: T11, T13, T14 and T15 fail; T12 and T16 pass.
- Check: the build command passes. Engine `Eb`. Core `Cb + 13`. No new warning line. `grep -c 'ESCAPE' C/input/KeyboardInput.kt`
  gives `2`. Smoke launch: `--level REFACTOR_TEST_029 --room movement`.
- Contract checks: T11 to T16, R8, A12, the `InputFrameBuilder(SystemButton.entries)` part of A13, A14. Manual proof
  later: G1.
- Commit: `031f: Esc has its own system frame, which the lock does not touch`
- Done: yes

### 5. The back action in the menu loop

This step changes no behavior: every `onBack` is empty until step 6.

- Files: `E/screens/menus/AbstractMenuScreen.kt`, `C/screens/menus/MegaMenuScreen.kt`, `G`,
  `ET/screens/menus/MenuScreenTest.kt`.
- Change:
  - `AbstractMenuScreen`: `protected open fun backRequested() = false` and `protected open fun onBack(delta: Float) {}`.
  - `AbstractMenuScreen.render:48`: the confirm test gets an `else` branch: `if (selectionRequested()) { ... } else if
    (backRequested()) onBack(delta)`. So the confirm wins (contract question 2), and a navigation in that frame runs first.
    All of it stays under `isInteractionAllowed() && key != null` and `buttons[key]?.let`.
  - `MegaMenuScreen`: `override fun backRequested() = game.systemInput.isJustPressed(SystemButton.BACK)`.
  - `G`: `val systemInput get() = systemFrameBuilder.frame`.
- Tests first: T1 to T10 in `MenuScreenTest`. The 5 cases stay. The fixture gets a `backRequested` flag and an `onBack` record
  that keeps the order of calls. T10 uses its own subclass with no override.
  - Run 1, with the 2 functions added and no call in `render`: T1 and T9 fail; T2 to T8 and T10 pass.
  - Run 2, a mutation: the call goes after the `let`, outside the guard, with no `else`. Prediction: T3, T4, T5, T6, T7 and T8
    fail. This proves that the 6 guard tests can fail. Do not commit it.
- Check: the build command passes. Engine `Eb + 10`. Core `Cb + 13`. No new warning line. Smoke launch: `--screen MAIN_MENU_SCREEN`.
- Contract checks: T1 to T10; R2 (first half); the `M` line of A6; A2. Manual proof later: M1 to M8.
- Commit: `031f: the menu loop has a back action, under the same guard as the confirm`
- Done: yes

### 6. `Esc` runs the back rows

- Files: `C/screens/menus/SelectDifficultyScreen.kt`, `C/screens/menus/ExtrasScreen.kt`, `CSS`, `C/screens/menus/MainMenuScreen.kt`.
- Change: each file overrides `onBack`. The body runs the row through the same 2 lines as the confirm (open question 2):

  ```kotlin
  override fun onBack(delta: Float) {
      selectionMade = buttons[BACK].onSelect(delta)
      if (selectionMade) onAnySelection()
  }
  ```

  - `SelectDifficultyScreen`: as above. The row is `:120-131`.
  - `ExtrasScreen`: as above. The row is `:73-75`, so `resetPlayerProgress()` and the select sound run.
  - `CSS`: as above. The row is `:169`, so `backAction` runs, and `onAnySelection` (`:371`) plays the select sound. A capture
    sets `selectionMade`, so the loop guard blocks it (T3).
  - `MainMenuScreen`: `override fun backRequested() = screenSlide.finished && super.backRequested()`. `onBack` returns at once
    if `GameSettingsPanel.ALL_KEYS` does not contain `buttonKey`. So the root does nothing. Else it uses
    `buttons[GameSettingsPanel.BACK_KEY]`. The `GSP` row (`:152-157`) returns `false`, so `onAnySelection` does not run, as for a
    confirm on that row.
  - `SaveGameScreen.kt` does not change.
- Tests first: none. `T1` to `T9` hold the loop. Each row needs a running game, so the manual checks cover it (contract, "No
  temporary contract suite").
- Check: the build command passes. Engine `Eb + 10`. Core `Cb + 13`. No new warning line. R1 and R5 by reading the diff: no `onBack`
  holds a copy of a row body. Smoke launches: `--screen EXTRAS_SCREEN`, `--screen KEYBOARD_SETTINGS_SCREEN`.
- Contract checks: R1 (4 of 5 rows), R2 (`MainMenuScreen`), R5; intended change 3. Manual proof later: M1 to M5, C1 to C5.
- Commit: `031f: Esc runs the back row of the main panel, difficulty, extras and the bindings screens`
- Done: yes

### 7. `Esc` on the pause screen

- Files: `LPS`.
- Change:
  - `override fun backRequested() = settingsScreenSlide.finished && super.backRequested()`, next to `:546`.
  - `onBack`:

    ```kotlin
    override fun onBack(delta: Float) {
        selectionMade = if (showingSettings) buttons[GameSettingsPanel.BACK_KEY].onSelect(delta) else true
        if (selectionMade) onAnySelection()
    }
    ```

  - On the panel, this is the `GSP` row (`LPS:137-144`), so `showingSettings` goes `false`, and the row gives `false`.
  - On the grid, this is the same `selectionMade` and `onAnySelection` path as a weapon confirm. `onAnySelection` (`:531-542`)
    holds the guard `!exiting && settingsScreenSlide.finished` and queues the resume at `:540`. `backRequested` and
    `isInteractionAllowed` (`:438-443`) make that guard true on each call, so `selectionMade` cannot stay `true` with no
    resume. `resume()` calls `pauseScreen.reset()` (`MLS:1105`), which clears it.
  - It does not write `currentWeapon` and does not call `setCurrentButtonKey`, which ignores the key (`:528`).
- Tests first: none. `LPS` needs a running game. T3, T4 and the slide manual checks hold the guard.
- Check: the build command passes. Engine `Eb + 10`. Core `Cb + 13`. No new warning line. R4 by reading the diff. Smoke launch:
  `--level REFACTOR_TEST_029 --room movement` (it does not reach this path).
- Contract checks: R1 (the fifth row), R2 (`LPS`), R4; intended changes 2, 3. Manual proof later: P1 to P7.
- Commit: `031f: Esc closes the pause grid and leaves the pause panel`
- Done: yes

### 8. `Esc` on the level select screen

- Files: `C/screens/menus/level/LevelSelectScreenV2.kt`.
- Change:

  ```kotlin
  // TODO: after an extra level, currentLevel stays on that level, so a save load and then Esc opens SAVE_GAME_SCREEN with the extras rows, and its MAIN MENU row clears the loaded progress from memory; the save file does not change; follow-up in docs/issues/2026-11-01-level-select-back-after-extra-level.md (see 031f CONTRACT.md, question 4)
  override fun onBack(delta: Float) {
      game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
  }
  ```

  Wrap the `TODO` to the file's line length. Facts that this change depends on:
  - It does not set `selectionMade`. This screen has no back row, and a `true` value selects the `SELECTED` background
    (`:61`) for the 1 draw of the old screen after the change. `setCurrentScreen` runs `reset()`, which clears the value.
  - It plays no sound: the dead `LevelSelectScreen` `BACK` row (`:169-173`) plays none, and the contract asks for none.
  - During the outro, `onSelect` gave `true`, so the loop guard blocks it (M7).
- Tests first: none. It needs a running game (M6 to M8, W2).
- Check: the build command passes. Engine `Eb + 10`. Core `Cb + 13`. No new warning line. Smoke launch: `--screen LEVEL_SELECT_SCREEN`.
- Contract checks: intended change 4. Manual proof later: M6, M7, M8, W2.
- Commit: `031f: Esc on the level select screen goes to the save screen`
- Done: yes

### 9. The pause decision is a pure function

This step changes no behavior. `Esc` and the lock tests come in step 10.

- Files: new `C/screens/levels/LevelPauseRules.kt`, `MLS`, new `T/screens/levels/LevelPauseRulesTest.kt`.
- Change:
  - `LPR`: `fun isPauseRequested(paused: Boolean, alive: Boolean, inPlay: Boolean, playerFrame: InputFrame, selectAction:
    SelectButtonAction): Boolean`. The body is `!paused && alive && inPlay` and then the rule of `MLS:860-862`, in the same
    order.
  - `MLS:874` calls it, with `game.clock.paused`, `player.existenceState.liveAfterUpdate`, `player.inPlay`, `player.input` and
    `game.players.settings(player.playerSlot!!).selectButtonAction`. Named arguments.
  - Remove `isPauseButtonRequested()` (`:860-862`) and the 2 imports that only it uses (`:67`, `:68`). A `grep` shows no other use.
  - The reads are now not lazy. By reading: `input`, `inPlay` and `playerSlot` have no side effects, and `Megaman` keeps
    its `PlayerComponent`, so `playerSlot!!` cannot throw for a player that is dead. In play, the old chain already does
    these reads on each frame, because the right side of `||` runs unless `START` is released. So the new work is 3
    component lookups, only while paused, dead or not in play.
- Tests first: `LevelPauseRulesTest` with T20, T23, T25, with real frames from the real `InputFrameBuilder`. A local helper in
  the test gives the defaults, so that step 10 changes one place. Prediction with a body of `false`: T20 and T23 fail; T25
  passes. Then the code. T25 has the `START` half here, and step 10 adds the `Esc` half.
- Check: the build command passes. Engine `Eb + 10`. Core `Cb + 16`. No new warning line. Smoke launch:
  `--level REFACTOR_TEST_051 --room basic`.
- Contract checks: T20, T23, T25 (`START` half). Manual proof later: V4.
- Commit: `031f: the pause decision is a pure function, with no change in behavior`
- Done: yes

### 10. `Esc` in gameplay and the lock tests

`Esc` and the 2 lock tests are one commit. Without the lock tests, `Esc` pauses in a room transition (`SCOPE.md`, "Affected
areas and risks"), so no commit may hold one with no other.

- Files: `LPR`, `MLS`, `G`, `T/screens/levels/LevelPauseRulesTest.kt`.
- Change:
  - `G`: `val inputLocked get() = slot0FrameBuilder.locked`. It has no setter.
  - `isPauseRequested` gets `locked: Boolean` after `inPlay`, and `backStatus: ButtonStatus` after `playerFrame`:

    ```kotlin
    !paused && alive && inPlay && !locked &&
        (playerFrame.isJustReleased(MegaControllerButton.START) ||
            backStatus == ButtonStatus.JUST_PRESSED ||
            (selectAction == SelectButtonAction.START && playerFrame.isJustPressed(MegaControllerButton.SELECT)))
    ```

    It takes a `ButtonStatus`, not the system frame. So `MLS` keeps the `Esc` read, with the `TODO` of A18 at that read, and
    `MLS` stays in the list of A15 and A16. T21 and T22 still use a real frame, through `getStatus`.
  - `LPR`: `fun queuePause(queue: Queue<() -> Unit>, locked: () -> Boolean, pause: () -> Unit) { queue.addLast { if (!locked())
    pause() } }`. The lock read runs when the runnable runs.
  - `MLS:874`: `locked = game.inputLocked` and `backStatus = game.systemInput.getStatus(SystemButton.BACK)`.
  - `MLS:876`: `queuePause(game.runQueue, { game.inputLocked }, { game.pause() })`.
  - The `TODO`, on the line before the `backStatus` argument, wrapped to the file's line length:
    `// TODO: Esc is pause in gameplay for now; consider another action for Esc in gameplay later (031 decision of 2026-10-25)`.
  - `LPR` must not contain the words `systemInput` or `inputLocked` (A15). It names `SystemButton` nowhere (A16).
- Tests first: T19, T21, T22, T24, T26, T32, T33 in `LevelPauseRulesTest`, and the `Esc` half of T25. T32 and T33 use a real
  `InputFrameBuilder` and a real `Queue<() -> Unit>`. They drain with `removeLast()`, as `G.render` does, before the build.
  - Run 1: the 2 parameters are there and the body ignores them; `queuePause` has the body `queue.addLast { pause() }`.
    Prediction: T19, T21, T24 and T32 fail; T22, T26 and T33 pass.
  - Run 2, a mutation: `queuePause` reads the lock when it adds the runnable. Prediction: T32 fails, and T33 passes. This proves
    that T32 checks the last sentence of R6. Do not commit it.
- Check: the build command passes. Engine `Eb + 10`. Core `Cb + 23`. No new warning line. `grep -c 'inputLocked' MLS` gives `2`. A15, A16,
  A18 pass as written. Smoke launches: `--level REFACTOR_TEST_051 --room basic`, `--level REFACTOR_TEST_029 --room player_death`.
- Contract checks: T19, T21, T22, T24, T26, T32, T33, T25 (`Esc` half); R6, R7, R11; A13 (`inputLocked`), A14, A15, A16, A18; intended
  changes 1, 5, 6. Manual proof later: E1 to E3, L1 to L6, W1.
- Commit: `031f: Esc pauses in gameplay; a lock stops every pause request and a queued pause`
- Done: yes

### 11. Removals

- Files: delete `C/input/FrameControllerPoller.kt`, `E/controller/polling/IControllerPoller.kt`, `T/input/FrameControllerPollerTest.kt`.
  Change `G`.
- Change:
  - `git rm` the 3 files. The `polling` folder goes with the last file.
  - `G`: remove the imports at `:44` and `:93`, the field at `:223` and the assignment at `:317`.
  - `G:572`: the label is `"inputFrames"`.
- Tests first: none. A removal. `FrameControllerPollerTest` has 3 `it` cases (the status loop is in 1 case), so core
  falls by 3.
- Check: the build command passes. Engine `Eb + 10`. Core `Cb + 20`. No new warning line. A6, A10 and A11 pass. For A11, the
  `isAnyJust` grep gives no output (see "Line numbers and contract text"). Smoke launch: `--level REFACTOR_TEST_029 --room movement`.
- Contract checks: A3, A6, A10, A11, A13 (label); intended change 7. Manual proof later: D1.
- Commit: `031f: remove the controllerPoller facade; the diagnostics label is inputFrames`
- Done: yes

### 12. Contract checks

- Files: the branch `process-log.md`, `docs/NOTES.md`. A fix for a failed check is its own commit.
- Change: no code. In the worktree, on a clean build with the build command, do each check and write each result in the
  process log:
  - A1 to A5: the build passes; engine `Eb + 10`; core at least `Cb + 20`, 0 failures; `comm` gives no new warning line;
    `utils/diagnostics/run_tests.sh` passes.
  - A6 to A18: the commands in `docs/CONTRACT.md`, as written there, with the 2 notes in "Line numbers and contract text"
    for A11 and with `C/screens/levels/LevelPauseRules.kt` as the one allowed addition in A7.
  - T1 to T33: each case is present by name and passes (read the test results). Write the 33 names in the log.
  - R1 to R11: read `git diff master...HEAD -- core/src/main engine/src/main`. R9: compare each new API with the table
    in this plan and in `docs/SCOPE.md`.
  - Comments: the only new comments in `core/src/main` and `engine/src/main` are the 2 `TODO`s of steps 8 and 10, and the `TODO`
    of open question 3 if the owner takes it.
  - Overlap: run `git worktree list`. Read the folder of each other active branch. For each one, run
    `git merge-tree <merge-base> HEAD <branch>`, the 3-argument form, because the local git is `2.34.1`. Write the
    result in `docs/NOTES.md`.
- Tests first: none.
- Check: all results pass, or a failure has a fix commit and the affected checks run again.
- Contract checks: A1 to A18, T1 to T33, R1 to R11.
- Commit: `031f: contract checks`
- Done: yes

### 13. The owner's play test

**The agent that runs these steps does not run these checks.** Only the owner plays them, and only the owner runs S1 and X1.
The full text of each check is in `docs/CONTRACT.md`, "Manual play test". Give it to the owner as a list, in the contract's order,
with the `cd` line first:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-031f-menu-input`

1. S1: the hash of `~/.prefs`, with the game closed.
2. E1 to E3: `Esc` in gameplay.
3. L1 to L6: the lock. L4 is `[pad]`.
4. P1 to P7: the pause screen.
5. M1 to M8: the menus.
6. C1 to C5: the bindings screens. C3 to C5 are `[pad]`.
7. D1: the diagnostics label.
8. V1 to V4: control, menus and pause. V2 and V3 are `[pad]`.
9. K1 to K7: control, skip screens.
10. G1, G2, W1, W2: control, gameplay and a real stage. G2 is `[pad]`.
11. X1: compare the hash of `~/.prefs`.

- Files: the branch `process-log.md`. No code, unless the play test finds a problem. A fix is its own commit, and the affected
  automated checks run again. A fix to an experimental check means that the control checks run again.
- Change: write the result of each check in the process log, and which checks were skipped and why.
- Tests first: none.
- Check: the owner gives the result.
- Contract checks: S1, E1 to E3, L1 to L6, P1 to P7, M1 to M8, C1 to C5, D1, V1 to V4, K1 to K7, G1, G2, W1, W2, X1.
- Commit: `031f: play test results`
- Done: no

### 14. Merge

Follow "Branch merge" in `docs/mega-refactor/CLAUDE.md`. Do these in this order:

1. The results of steps 12 and 13 are in the branch process log.
2. Overlap: run `git worktree list`, and read the folder of each other active branch again. Run
   `git diff --stat mega-refactor-031f-menu-input...master`. **A merge of `master` into the branch is necessary if `master`'s new
   changes touch any of these paths.** This list is a rule, not a guide (lesson from `031d`):
   - The production files of A6 and A7, and the new `C/screens/levels/LevelPauseRules.kt`.
   - The code that the checks depend on: `GSP`, `C/screens/menus/SaveGameScreen.kt`, `C/screens/utils/ScreenSlide.kt`,
     `C/screens/levels/LevelStateHandler.kt`, `C/screens/levels/LevelRoomHandler.kt`, `C/audio/MegaAudioManager.kt`, `E/screens/menus/`,
     `E/input/`, `C/input/`, `C/controllers/`, `C/players/`.
   - The tests of A8, and `T/TestServices.kt`.
   - The build files: `build.gradle` (each module), `settings.gradle`, `gradle.properties`, `.github/workflows/ci.yml`, the 3
     `run-game-*.sh` scripts, and `utils/diagnostics/`.

   `084g` changes `LevelRoomHandler.kt`, so a merge of `084g` forces this merge. For a forced merge: merge `master` into the
   branch in its worktree, run all automated checks of step 12 again, and tell the owner the result. A new warning line that
   comes from `master` goes in the log with its source; the list of step 1 stays. If a conflict or a change touches pause,
   the lock or a menu, ask the owner which manual checks to run again. If no path matches, skip the merge. Write the result
   in `docs/NOTES.md`.
3. Get the owner's explicit approval to merge `mega-refactor-031f-menu-input`. Approval for another branch does not count.
   Merge one branch at a time. Do the forced merge before the approval, so that the approval covers the combined code.
4. From the main checkout: `git merge --squash mega-refactor-031f-menu-input`, then one commit:
   `Menus and skip screens read frames; Esc is back and pause; the controllerPoller facade goes (mega-refactor-031f)`. The message
   lists the main changes (the 9 reads, the system frame, the back action, `Esc` in gameplay, the 2 lock tests and the intended
   change to `START`, the removals, the diagnostics label) and ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
   The owner's local edits to `CameraEdgeBlocks.kt` and `run-game-alpha.sh` are not in the branch: do not stage them. If git
   refuses the merge because of a local edit, stop and tell the owner. No bare `git stash`.
5. No commit of this branch is in `.git-blame-ignore-revs`, so that file does not change.
6. On `master`, in one docs-only commit, staging only named paths:
   - `docs/mega-refactor/process-log.md`: an entry, in the format of that file.
   - `docs/mega-refactor/ITEMS.md`: the `031f` row to `merged` with the finish date; item `031` to `done` with the finish date.
   - `docs/mega-refactor/items/031-input-frames/ITEM.md`: `- Status: done`; the "Branches" line with `031f` (merged) and no
     next branch. Add the owner's decisions of this branch.
   - `docs/mega-refactor/items/031-input-frames/NOTES.md:744`: "all 9 live menu instances" becomes 8 (bug candidate 5). No issue
     file for this candidate.
   - `docs/mega-refactor/items/082-dialogue-and-cutscenes/ITEM.md:74` and `NOTES.md:42`: replace the text. `game.controllerPoller`
     is gone. Menus read `players.menuInput`, which the lock still blinds (`SCOPE.md` question 6). `082` builds an unlocked frame
     when it has a caller, with the system frame as the pattern. `031` does not do this split.
   - `.claude/skills/diagnostics-analyzer/SKILL.md:23`: `controllerPoller` becomes `inputFrames`. Add that old capture files keep the
     old label.
   - Root `CLAUDE.md`: at `5c02dc8aa`, `grep -in 'poller' CLAUDE.md` gives no output, so it does not name the poller, and it needs no
     change. Run the `grep` again at the merge. If it gives a line, replace the line.
   - `docs/issues/2026-11-01-start-release-at-lock-start-pauses.md`: `status: closed`, and a `## Resolution` section with the
     squash commit. It says that both triggers are fixed, and it keeps the listener case by name: a lock that a listener
     submits, for example `MegaLevelScreen.kt:754`, starts 1 frame later, so a real release at that moment can still pause; this
     is an inference from reading; `Esc` leaves that pause; a pad-only player has no way out. `SCOPE.md` does not give a rule
     for the close, so see open question 3. Do not change `verified`: no run on the old code exists. Update the row in
     `docs/issues/README.md`.
   - Each bug candidate of the branch `docs/TODO.md` becomes a file in `docs/issues/`, with a row in `README.md`:
     - candidate 2, a focus-loss or minimize pause under a lock: with `031f`, `Esc` leaves it, and a pad-only player has no way out;
     - candidate 3, a press read carries a held button to the next screen (`CreditsScreen.kt:89`);
     - candidate 4, a rebind of `START` or `A` with the key held past `0.25` s confirms the row again;
     - contract question 4, `LevelSelectScreenV2` back after an extra level:
       `2026-11-01-level-select-back-after-extra-level.md`. The `TODO` of step 8 names this file.

     Candidate 1 is the issue above, and candidate 5 is the `NOTES.md` line above.
   - Each backlog candidate becomes an item, with the next free ID (the highest ID at `5c02dc8aa` is `093`):
     - remove the dead `ScreenController` path or give it a real use (`screenController`, `showScreenController`,
       `--showScreenController`);
     - remove the dead `LevelSelectScreen`.

     The third candidate, a gamepad back button, has a home: add it to `docs/mega-refactor/items/070-flexible-controls/ITEM.md`.
     No new ID.
7. Note for items `013`, `012`, `055`, `047`: `SCOPE.md` "Overlap with other branches" names what each one takes over. These
   lines are in the branch `docs/NOTES.md` after step 1. No item text changes, unless the owner asks.
8. Copy `build-baseline-warnings.txt` from the worktree to the scratchpad, for step 14.9. In the worktree, `git status --short`
   must show only `build.log` and `build-baseline-warnings.txt`. Remove them with `rm`, because they make a plain
   `git worktree remove` refuse. Then run `git worktree remove .worktrees/mega-refactor-031f-menu-input` and
   `git branch -D mega-refactor-031f-menu-input`. If an empty folder stays:
   `rmdir .worktrees/mega-refactor-031f-menu-input/.gradle .worktrees/mega-refactor-031f-menu-input`.
9. On `master`, run the build command. It passes, with the test counts of step 12. The compare runs from another folder, so
   normalize the paths first, or each line shows as new:
   `sed 's#/\.worktrees/mega-refactor-031f-menu-input/#/#' <scratchpad copy> | sort -u > <normalized list>`, and then the `comm`
   command of "Rules for every step" against `<normalized list>`. It gives no output. If the owner's local edit to
   `CameraEdgeBlocks.kt` gives a line, write it in the log. If the build fails, fix `master` at once, before any other merge.
   `build.log` is untracked on `master` too: do not stage it, and remove it after the check.

- Files: the list in 14.6, and the squash commit.
- Tests first: none.
- Check: `git worktree list` no longer shows the branch. The build on `master` passes. `ITEMS.md` shows `031f` as `merged` and `031`
  as `done`. The issue shows `closed` in `docs/issues/README.md`.
- Contract checks: all, as recorded in steps 12 and 13; the automated checks again on `master`.
- Commit: the squash commit of 14.4, then on `master`: `031f merged: process log, item record, 082 text, skill line, new issues and items`
- Done: no

## Contract check map

Each check, the step that makes it true, and the step that records it. "Records" is step 12 for each automated check
and step 13 for each manual check.

| Check | Made true in step | Recorded in step |
|---|---|---|
| A1 Build | each code step | 12, 14.9 |
| A2 Engine count | 5 | 12, 14.9 |
| A3 Core count | 11 | 12, 14.9 |
| A4 Warnings | each code step | 12, 14.9 |
| A5 Diagnostics tests | 11 | 12 |
| A6 Engine paths | 5, 11 | 12 |
| A7 Core and launcher paths | 2 to 11 | 12 |
| A8 Test paths | 2, 4, 5, 9, 11 | 12 |
| A9 No assets or `gradle.properties` | all (no step touches them) | 12 |
| A10 No poller names | 11 | 12 |
| A11 No arrays, no `isAnyJust` | 2, 3, 11 | 12 |
| A12 1 new key read | 4 | 12 |
| A13 Single build, label, builder, `inputLocked` | 4, 10, 11 | 12 |
| A14 `locked` lines in `G` | 4, 10 | 12 |
| A15 `systemInput` and `inputLocked` readers | 5, 10 | 12 |
| A16 `SystemButton` readers | 4, 5, 10 | 12 |
| A17 `menuInput` readers | 2, 3 | 12 |
| A18 `Esc` `TODO` | 10 | 12 |
| T1 to T10 | 5 | 12 |
| T11 to T16 | 4 | 12 |
| T17, T18 | 2 | 12 |
| T19 | 10 | 12 |
| T20 | 9 | 12 |
| T21, T22 | 10 | 12 |
| T23 | 9 | 12 |
| T24 | 10 | 12 |
| T25 | 9, 10 | 12 |
| T26 | 10 | 12 |
| T27 to T31 | 2 | 12 |
| T32, T33 | 10 | 12 |
| R1 Rows | 6, 7 | 12 |
| R2 `backRequested` | 5, 6, 7 | 12 |
| R3 The 9 reads | 2, 3 | 12 |
| R4 `LPS` grid | 7 | 12 |
| R5 No back on root and save | 6 | 12 |
| R6 Pause test and queue | 10 | 12 |
| R7 `inputLocked` | 10 | 12 |
| R8 System build place | 4 | 12 |
| R9 APIs have callers | 2 to 10 | 12 |
| R10 `capacity`, `PlayerSlot(0)` | 2 | 12 |
| R11 `WilyFinalBoss` | 10 | 12 |
| S1, X1 | none (read only) | 13 |
| E1 to E3 | 10 | 13 |
| L1 to L6 | 10 | 13 |
| P1 to P7 | 7 (the grid opens with `Esc` from 10) | 13 |
| M1 to M5 | 6 | 13 |
| M6 to M8 | 8 | 13 |
| C1 to C5 | 6 | 13 |
| D1 | 11 | 13 |
| V1 to V3 | 2 | 13 |
| V4 | 2, 9 | 13 |
| K1 to K7 | 3 (K7 is `Esc` with no read) | 13 |
| G1 | 4, 10 | 13 |
| G2 | no step (no pad code changes) | 13 |
| W1 | 3, 10 | 13 |
| W2 | 3, 8 | 13 |

Count: 18 A, 33 T, 11 R, 45 manual, S1 and X1, so 109 checks. Each one has a line. 0 checks have no step.

Facts for the owner, from reading only:

- P3 and M2 compare the sound of the panel `BACK` row with `Esc`. The row gives `false`, so `onAnySelection` does not run, and
  neither path plays a select sound.
- M8 is reachable: `G:488` sets `currentLevel` for a staged level, and `isInExtraLevel` reads it.
- All rooms and screen names of the contract exist at `5c02dc8aa`. The live TMX files of the rooms have no change since `c6b8cecf7`.
- D1 reads `assets/*-diagnostics.txt`: `lwjgl3/build.gradle:53` sets the working folder to `assets`.

Intended change 6 has a part with no manual check, as the contract says. T32, T33 and R6 cover trigger 2. L6 covers the listener
state.

## Overlap at this commit

- **`mega-refactor-084g-movement-state` (active, read at `fd8e5e85a` and its docs in the worktree).** Its code diff against
  `master` has 10 files: `Megaman.kt`, 3 files in `entities/megaman/`, `RunComponent.kt`, `Runs.kt`, `LevelRoomHandler.kt` and 3 test
  files. No file is in A6, A7 or A8. `LevelRoomHandler.kt` submits the `TURN_CONTROLLER_*` events that L1 depends on; its change
  there is the end of the transition, at `:155`, not the 2 submits (`:54`, `:135`). It took in `051e` at `f16f1aaa3`, so the
  two branches have the same camera code. If it merges first, step 14.2 forces the merge of `master`, and the log gives the new
  test counts next to the step 1 numbers.
- **Other folders in `.worktrees/`:** only `084g` is a registered worktree. Step 1 reads the list again.

## Open questions

Answered (owner, 2026-11-01, verbatim): "yes", as recommended: each question takes a). The corrections of question 1 are done in `CONTRACT.md` and `SCOPE.md`.

1. **Contract and scope text that no longer fits.** This session changed neither file.
   - A2, A3, A4: the numbers `600`, `610`, `528`, `548`, `543` and `76`. Plan step 1 replaces them.
   - A3: "If open question 3 takes option b": the owner took a), so this line has no use.
   - A11: the `isAnyKeyJustPressed` lines do not match the pattern `isAnyJust`. The correct result is no output.
   - A13, R6, "Must stay the same" and the library section: `MLS:876` and `:878` are now `:874` and `:876`, and `G:574` is `:573`.
   - R6, first sentence: the order of tests is in `isPauseRequested`, not at `MLS:874`.
   - `docs/SCOPE.md` section 4: "`MLS.isPauseButtonRequested` adds" and the `SystemButton` line of the API table: the function goes in
     step 9, and `MLS` reads `getStatus(SystemButton.BACK)` for the seam of contract question 3.
   - `docs/SCOPE.md` "Measured size": 1 new core file is now 2, and 4 test files are now 6, as the contract allows.
   - `docs/SCOPE.md` "Backlog candidates": the gamepad back button says "question 6"; the correct number is 10.
   - a) Correct these lines when this plan is approved, as contract question 5 did, and write it in the process log.
   - b) Leave the files. Steps 11 and 12 read the checks through "Line numbers and contract text".
   - Recommendation: a). No line changes an owner's answer.

2. **The 2 lines that run a row from `onBack`.** R1 asks for the same `selectionMade` and `onAnySelection` path as a confirm. R9
   allows only the APIs in the `SCOPE.md` table.
   - a) Inline: 5 sites of 2 lines (`LPS` has 1 form of them). No new API, and no change to the scope.
   - b) A protected `runSelection(button, delta)` on `AbstractMenuScreen`, which `render` also calls. No repeated lines, but a new API
     that the scope does not list, so the scope and R9 need a change and a new approval.
   - Recommendation: a).

3. **The listener case after the issue closes.** `SCOPE.md` says that the issue file names this case. It gives no rule for the
   close. The global rule says that a knowingly accepted edge case gets a `TODO` at the code that causes it, and the contract
   names only 2 `TODO`s. A18 limits only `MLS`, so a `TODO` in `LPR` does not break a check.
   - a) Close the issue as step 14.6 says, with the case named in the Resolution. File 1 new issue on `master` at the merge for
     the case. Step 10 adds this `TODO` at `queuePause`, wrapped to the file's line length:
     `// TODO: a lock that a listener submits, for example MegaLevelScreen:754, starts 1 frame later, so a real START release at that moment still pauses, and only Esc leaves that pause; follow-up in item 012 (see 031f CONTRACT.md, intended change 6)`.
   - b) Close the issue with the case named in the Resolution, and add no `TODO` and no new issue. The case then has no open
     tracker.
   - c) Leave the issue open, with both triggers marked as fixed. The index then shows a fixed bug as open.
   - Recommendation: a).
