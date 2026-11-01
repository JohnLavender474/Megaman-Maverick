# Scope: mega-refactor-031f-menu-input

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "scope approved")

## Goal

- The menus, the skip screens and `ScreenController` read frames, not `game.controllerPoller`. This part changes no
  behavior: the poller is a live read of the same frame (see "Frame order").
- `Esc` is a system key. It is back in menus and pause in gameplay.
- A menu has a back action. `Esc` runs it.
- `game.controllerPoller`, `FrameControllerPoller`, `FrameControllerPollerTest`, the engine `IControllerPoller` and the
  diagnostics label `"controllerPoller"` go.

The scan supports this goal, with 1 addition: `Esc` must not pass the input lock in gameplay, and the scan found a
soft-lock at that place (bug candidate 1, question 3).

## Item

`031`: Input frames per player (`InputFrame`), devices and bindings. This branch takes these decisions, copied from
`ITEM.md`:

- 2026-10-25: "keep the button names (`A`, `B`, `START`, `SELECT`, the d-pad). [...] The frame is keyed by button, not by
  intent, so the controller is not coupled to intents. What a button does stays the job of the code that reads it."
- 2026-10-25 (bindings): "`Esc` is a reserved system key and can never be bound."
- 2026-10-25 (bindings): "reserved keyboard keys: `Esc`, `Enter`, numpad `Enter` and the four arrow keys. [...] `Esc` is a
  system key: back in menus, pause in gameplay (with a `TODO` to consider another action later)."
- 2026-10-25 (bindings): "the touch overlay (`ScreenController`, the on-screen controller meant for mobile) keeps a fixed
  layout, with no rebinding. No mobile build exists yet, so it is out of scope [...]."
- 2026-10-26: "A later branch moves the menus and skip screens to frames, with `Esc` and the back action."
- 2026-10-26 (for `031c`): "the menus and skip screens read a thin `IControllerPoller` facade over the slot-0 frame until
  the menu-input branch."
- 2026-10-30 (on the `031e` scope): "`BACK` holds the level screen, and the level music keeps playing." `031e` scope
  section 6: "`031f` adds `Esc` as another input for that action and needs no new path."

From the item `NOTES.md`, not decisions:

- "`Esc` is a system key, not a game button. In menus it means back. In gameplay it opens the pause screen. The code that
  adds this gets a `TODO` to consider another action for `Esc` in gameplay later. In co-op it pauses for both players
  (shared screen)."
- Escape hatch layer 3: "`Esc` and the mouse always reach pause, the input menu and the bindings screen". The mouse has no
  decision. See "Out of scope".
- "What `UiInput` must cover: navigate, adjust left/right, confirm, back (new), skip/advance, and the pause entry." `UiInput`
  is in the unapproved proposed scope only. See question 7.

## Current state

Based on `master` at `c6b8cecf7`. `E` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C` =
`core/src/main/kotlin/com/megaman/maverick/game/`, `G` = `C/MegamanMaverickGame.kt`, `MLS` =
`C/screens/levels/MegaLevelScreen.kt`, `LPS` = `C/screens/menus/level/LevelPauseScreen.kt`, `CSS` =
`C/screens/menus/ControllerSettingsScreen.kt`, `GSP` = `C/screens/utils/GameSettingsPanel.kt`.

### Every use of the poller

Search: `grep -rn "controllerPoller\|IControllerPoller\|FrameControllerPoller" core/src engine/src lwjgl3/src`. 9 files
contain `controllerPoller`. 12 files contain one of the 3 names.

| File | Lines | Use |
|---|---|---|
| `C/screens/menus/MegaMenuScreen.kt` | `:21`, `:31` | 2 reads (4 direction tests, confirm on `START` or `A`). All menus read through these 2 lines. |
| `C/screens/other/LogoScreen.kt` | `:129` | 1 read, `START` just released |
| `C/screens/other/AbstractStoryScreen.kt` | `:110` | 1 read, `START` just released (`OpeningScreen`, `EndingScreen`) |
| `C/screens/other/GameOverScreen.kt` | `:85` | 1 read, `START` just released |
| `C/screens/other/GetWeaponScreen.kt` | `:243` | 1 read, `A`, `B` or `START` just released |
| `C/screens/other/SimpleInitGameScreen.kt` | `:59` | 1 read, `START`, `A` or `B` just released |
| `C/screens/other/CreditsScreen.kt` | `:89` | 1 read, `START` or `SELECT` **just pressed**. The only press read. |
| `C/controllers/ScreenController.kt` | `:193` | 1 read, in dead code (see below) |
| `G` | `:42`, `:91`, `:223`, `:317`, `:573` | 2 imports, the field, the assignment, the diagnostics label |
| `C/input/FrameControllerPoller.kt` | `:4`, `:10` | The class |
| `E/controller/polling/IControllerPoller.kt` | `:9` | The interface, the only file in the folder. 0 other engine uses. |
| `core/src/test/.../input/FrameControllerPollerTest.kt` | 5 lines | 3 cases |

Total: 9 read sites in 8 files. No other class implements or reads `IControllerPoller`. `run()`, `init()` and `on` have no
caller. Of its helpers, readers call only `isPressed`, `isJustReleased`, `isAnyJustReleased` and `isAnyJustPressed`. `SimpleEndLevelScreen`, `RobotMasterIntroScreen` and
`temp_BossIntroScreen` read no input.

Outside the code: `.claude/skills/diagnostics-analyzer/SKILL.md:23` names the label. `utils/` does not name it, so the
parser does not depend on it. `docs/mega-refactor/items/082-dialogue-and-cutscenes/ITEM.md:74` and `NOTES.md:42` name the
poller.

### The pause entry is not a poller read

`MLS:862-864` (`isPauseButtonRequested`) reads `player.input` (`E/entities/EntityQueries.kt:37`). That is the source that
`MegaPlayers.register` binds, `inputs[slot]` (`C/players/MegaPlayers.kt:25`). So it reads the same value as the menus.

### Direct device reads

- `C/input/KeyboardInput.kt:35`, `:38`: the only `isKeyPressed` reads in the frame path.
- `CSS:284`, `:289`: the capture swaps `Gdx.input.inputProcessor` and adds a pad listener. It needs raw events. It stays.
- `lwjgl3/.../DesktopLauncher.kt:154`: the `P` screenshot trigger.
- `C/controllers/ScreenController.kt:96`: sets an input processor, in dead code.

### How `Esc` reaches the game now

It does not. `ESCAPE` has 1 production use: `KeyboardInput.isReservedKey` (`C/input/KeyboardInput.kt:28`), for the capture
refusal. `held()` gives no bit for it (`KeyboardInputTest`, "gives no bit for Esc"). The LWJGL3 backend maps the key
(`DefaultLwjgl3Input.java:467`) and gives it no default action. So `Esc` does nothing in menus and in gameplay.

### Menus

`MegaMenuScreen` has 8 subclasses, and 7 of them are built. `LevelSelectScreen` is not: `G:463` registers
`LevelSelectScreenV2`, and nothing calls the old constructor. So 8 instances are live (`CSS` has 2). The item `NOTES.md` says 9.

`AbstractMenuScreen.render` (`E/screens/menus/AbstractMenuScreen.kt:42-53`) is the one input loop. It runs only if
`isInteractionAllowed()` is true and the key is not `null`. It reads navigation first, then confirm. A confirm calls
`button.onSelect(delta)`, sets `selectionMade` to the result, and calls `onAnySelection()` if it is `true`.

| Menu | Way back today | `isInteractionAllowed` |
|---|---|---|
| `MainMenuScreen` | Root. `EXIT` row calls `Gdx.app.exit()` (`:221`). | `!selectionMade`; `screenSlide.finished` gates the 2 reads (`:286-289`) |
| `MainMenuScreen` panel | `GSP.BACK_KEY` row, `onBack` (`GSP:152-157`, `MainMenuScreen.kt:70`) | as above |
| `SelectDifficultyScreen` | `BACK` row, to the main menu (`:120-131`) | `!selectionMade` |
| `ExtrasScreen` | `BACK` row: `resetPlayerProgress()`, to the main menu (`:73-75`) | `!selectionMade` |
| `SaveGameScreen` | No back row. `MAIN_MENU` row, with `resetPlayerProgress()` in an extra level (`:115`). `SAVE` is a separate row, so a leave can lose an unsaved state. | `!selectionMade` |
| `LevelSelectScreenV2` (live) | None (the `NOTES.md` agrees). The dead `LevelSelectScreen` has a `BACK` row to `SAVE_GAME_SCREEN` (`:171`). | `!selectionMade` |
| `LPS` grid | None. A confirm on a weapon sets the weapon and resumes (`:560`, `:538`). | `LPS:436-441`: also the health fill and `!exiting` |
| `LPS` panel | `GSP.BACK_KEY` row, `onBack` (`LPS:139-142`); `settingsScreenSlide.finished` gates the reads (`:542-544`) | as above |
| `CSS` (2 instances) | `BACK` row, `backAction`: `closeOverPause()` in a level, or the main menu (`:169`, `:38`) | `!selectionMade` |

During a capture, `selectionMade` is `true` until `delayOnChangeTimer` ends (`CSS:293`, `:402-403`). So the menu loop is
blocked, and a back action in that loop is blocked too.

### Dead code in this path

`screenController` (`G:241`) has no assignment. `ScreenController(` has no caller. `--showScreenController` sets
`params.showScreenController` (`DesktopLauncher.kt:155`), and nothing reads it. So `ScreenController.kt:193` never runs.

### Frame order

This table is one pass of `G.render()`, on a frame `N`. The backend order comes from the `1.14.2` sources:
`Lwjgl3Application.loop` calls `window.update()` and then `glfwPollEvents()` (`:181`, `:187`).
`Lwjgl3Window.update` calls `input.update()`, `listener.render()`, `input.prepareNext()` (`:414`, `:435`, `:439`).

| Step | Place | Input effect |
|---|---|---|
| 1 | `glfwPollEvents` at the end of frame `N-1` | `pressedKeys` changes. `keyDown` goes in the event queue. |
| 2 | `input.update()` | The event queue drains. A capture's `keyDown` runs here, before step 5. |
| 3 | `G:508`, `runQueue` | `game.pause()`, `game.resume()`, and some `setCurrentScreen` calls. These use frame `N-1` decisions. |
| 4 | `G:514-540` | While assets load, the frame does not build, and `render()` stops here. |
| 5 | `G:574` | The slot-0 frame builds, once, for every screen and also while paused. Each edge lasts for one `render()`. |
| 6 | `G:578` | `screenController` (`null`). |
| 7 | `G:586`, `:590` | `screen.render`, then `screen.draw` on the same local `screen`, also if the screen changed in step 7. |
| 8 | `MLS:876` in step 7 | The pause request. It queues `game.pause()`. So the pause starts in step 3 of `N+1`. |
| 9 | `MLS:910` in step 7 | `engine.update`: `ControllerSystem` reads frame `N`. |
| 10 | `MLS:941` in step 7 | `pauseScreen.render` while paused. |
| 11 | `G:616` | `events.run()`: `TURN_CONTROLLER_*` sets `locked` here (`G:680-696`). The lock acts on the build of `N+1`. |
| 12 | `input.prepareNext()` | `isKeyJustPressed` clears. The frame path does not use it. |

**Result: the poller and the frame do not advance at different points.** The poller has no state. `getStatus` calls
`source.read()` at each read (`C/input/FrameControllerPoller.kt:14-15`), and `run()` is empty. The source reads
`players.inputs[PlayerSlot(0)]`, which only `G:574` writes. All 9 read sites run in step 6 or 7, after the write, and none
runs in step 3 or in a `show()`. `InputFrame.isPressed`, `isJustPressed` and `isJustReleased` (`E/input/InputFrame.kt:28-35`)
give the same result as the poller helpers. The only other case is a key that is not a `MegaControllerButton`, which gives
`null` in the poller. All 4 button arrays hold only `MegaControllerButton`s. So moving the 9 reads changes no result in any
frame.

**Where a one-frame difference exists today, and stays:**

- **A lock:** the event in step 11 acts on `N+1`. In the first locked build, a held button gives `JUST_RELEASED`, so a
  reader sees a release that the player did not make (`E/input/InputFrameBuilder.kt:18-20`). See bug candidate 1.
- **A change in `runQueue`** (step 3): the new screen reads frame `N+1` in the same `render()`. **A change in a screen
  `render`** (step 7): the old screen still draws in `N`, and the new screen first reads `N+1`. **A change in `events.run`**
  (step 11): the new screen first reads `N+1`.
- **Over a pause:** a pause entry on `START` has `JUST_RELEASED` in `N` and `RELEASED` in `N+1`, so the pause menu does not
  confirm on it. A resume from `LPS:538` runs in step 3 of `N+1`. `MLS:876` then reads `RELEASED`, so it does not pause again.
- **An edge never crosses a screen change, but a held button does.** A button that is held across the change gives
  `JUST_RELEASED` on the new screen, and all menus confirm on release. Today this matters for the 2 press reads: `CreditsScreen:89`
  (bug candidate 3) and the `SELECT` pause entry (`MLS:864`, harmless: `SELECT` confirms nothing). A new `Esc` read on press has
  this case also, but `Esc` confirms nothing. See question 2.

### The lock and pause

`TURN_CONTROLLER_OFF` sets `slot0FrameBuilder.locked` (`G:680-681`). `game.pause()` freezes the level flow: `MLS:886` and
`:912` stop the handlers and `cameraController.update`. `LevelStateHandler.resume` turns on every system (`:25-26`). A pause
during a lock is a state that the game cannot reach with `START` by intent, but see bug candidate 1.

### Issues

| Issue | Result | Branch |
|---|---|---|
| `2026-10-22-pause-during-gate-reenables-physics.md` (open) | Confirmed by reading. A gate does not lock input, so `Esc` also reaches it, as `START` does. | Not `031f`. `012`. No new risk class: one more key at the same test. |
| `2026-10-22-health-fill-end-unpauses-systems.md` (open) | Not changed. | Not `031f`. |
| `2026-10-22-run-queue-lifo.md` (open) | The `Esc` pause uses the same `runQueue` line. | Not `031f`. |
| `2026-10-25-p-key-binding-takes-screenshots.md` (open) | `P` is not read here. | Not `031f`. |

## In scope

### 1. Menus and skip screens on frames

- `MegaMenuScreen:21` and `:31` read a frame. `selectionButtons` goes (0 other readers), and the confirm is
  `isJustReleased(START) || isJustReleased(A)`.
- The 6 skip screens read a frame at their 1 line each. The arrays `START_BUTTONS`, `END_BUTTONS` and `EARLY_EXIT_BUTTONS` go,
  in place of `||` tests. No new `isAny...` helper: each of the 4 tests has 2 or 3 buttons.
- `CreditsScreen:89` stays a press read. The others stay release reads.
- `ScreenController.kt:193` reads a frame (question 9).
- The reader shape is question 7. The proposal is 1 property, `MegaPlayers.menuInput`.

### 2. The system frame

- `SystemButton`, an `InputButton` enum in `C/input/`, with 1 entry, `BACK`. `Esc` is a device key, not a player button, so it
  is not in `MegaControllerButton`. It also must not show on the bindings screen.
- `KeyboardInput.systemHeld()` gives the `Esc` bit from `Gdx.input.isKeyPressed`. `held()` still gives no bit for `Esc`.
- `G` has a second private `InputFrameBuilder(SystemButton.entries)`. It builds at `:574`, next to slot 0. It is never locked.
  `game.systemInput` reads its `frame`.
- See question 1 for the options.

### 3. The back action

- `AbstractMenuScreen` gets `protected open fun backRequested() = false` and `protected open fun onBack(delta: Float) {}`. Its
  `render` calls `onBack` under the same `isInteractionAllowed()` test as the confirm (question 8).
- `MegaMenuScreen.backRequested()` reads `game.systemInput.isJustPressed(SystemButton.BACK)`.
- `MainMenuScreen` and `LPS` add their slide test, as they do for the confirm.
- Each menu with a back target overrides `onBack`. It runs the same `IMenuButton.onSelect` as the row, then
  the same `selectionMade` and `onAnySelection` path, so the sound and side effects match the row. Targets:

| Menu | `Esc` |
|---|---|
| `MainMenuScreen` root | Nothing (question 5) |
| `MainMenuScreen` panel | `GSP.BACK_KEY` row |
| `SelectDifficultyScreen` | `BACK` row |
| `ExtrasScreen` | `BACK` row, with `resetPlayerProgress()` |
| `SaveGameScreen` | Nothing (question 5) |
| `LevelSelectScreenV2` | To `SAVE_GAME_SCREEN` (question 5) |
| `LPS` grid | Resume, through the `onAnySelection` runnable. The weapon stays the same (question 4). |
| `LPS` panel | `GSP.BACK_KEY` row |
| `CSS` | `BACK` row, so `backAction`. No new path, as the `031e` scope says. |

### 4. `Esc` in gameplay

- `MLS.isPauseButtonRequested` adds `|| game.systemInput.isJustPressed(SystemButton.BACK)`, with the `TODO` of the decision.
- `MLS:876` adds `!game.inputLocked`, so a lock stops both keys (question 3). `game.inputLocked` reads `slot0FrameBuilder.locked`.
- The queued pause at `MLS:878` tests the lock again: `game.runQueue.addLast { if (!game.inputLocked) game.pause() }`. A release in the
  frame where a lock starts passes `MLS:876`, because `events.run()` sets `locked` after the screen render. The queued pause runs at the start of
  the next frame, when `locked` is set, so it is dropped (question 3, trigger 2 of bug candidate 1).
- `Esc` stays reserved. The capture refusal does not change.

### 5. Removals

See "Removed".

### New APIs and their first callers

| API | First production callers |
|---|---|
| `SystemButton` (`BACK`) | `G.render` (builder), `MegaMenuScreen`, `MLS` |
| `KeyboardInput.systemHeld()` | `G.render` |
| `game.systemInput` | `MegaMenuScreen.backRequested`, `MLS.isPauseButtonRequested` |
| `game.inputLocked` | `MLS:876`, `MLS:878` |
| A pure function for the pause decision at `MLS:876` (contract question 3) | `MLS.render` |
| A small function for the queued pause, which takes the queue, the lock read and the pause call (contract question 3) | `MLS.render` |
| `MegaPlayers.menuInput` (question 7) | `MegaMenuScreen`, 6 skip screens, `ScreenController` |
| `AbstractMenuScreen.backRequested()`, `onBack(delta)` | `MegaMenuScreen`, `MainMenuScreen`, `LPS`, `SelectDifficultyScreen`, `ExtrasScreen`, `LevelSelectScreenV2`, `CSS`; called by `AbstractMenuScreen.render` |

Nothing here has no caller. The second builder is an instance of an existing class.

### Removed

`game.controllerPoller`, `C/input/FrameControllerPoller.kt`, `FrameControllerPollerTest.kt`,
`E/controller/polling/IControllerPoller.kt` and its folder, the 2 imports in `G`, `selectionButtons`, the 3 skip-screen arrays.
The label at `G:573` changes to `"inputFrames"`.

## Out of scope

- The capture in `CSS` (raw events). `Esc` in a capture stays refused, with no cancel (`031e` decision).
- `Esc` on skip screens, `RobotMasterIntroScreen` and other non-menu screens. The decision says "menus" and "gameplay". `Esc` does
  nothing there.
- The mouse in menus. It has no decision. A new input path.
- A menu frame that ignores the lock (question 6).
- `012`: lock reasons and `TURN_CONTROLLER_*`. `013`: the pause flow and `showOverPause`. `047`: which slot drives menus, the join
  flow. `055`: recording.
- A touch device adapter. The `TODO` at `ScreenController.kt:145` stays.
- `LevelSelectScreen` (dead), the `--showScreenController` flag, and the dead `ControllerUtils` functions. See "Backlog candidates".
- Issue `2026-10-22-pause-during-gate-reenables-physics.md`.

## Intended changes

Each item below is a change that `CONTRACT.md` names under "Intended changes":

1. `Esc` opens pause in gameplay, on the same tests as `START`: not paused, alive, in play, not locked. Today it does nothing.
2. `Esc` on the pause grid resumes the game, and the weapon stays the same (question 4).
3. `Esc` on a settings panel, `SelectDifficultyScreen`, `ExtrasScreen` and both bindings screens runs the back row, with its sound
   and side effects. From the bindings screen over pause, it goes back to the panel, in the same level state.
4. `Esc` on `LevelSelectScreenV2` goes to `SAVE_GAME_SCREEN` (question 5). Today this screen has no way back.
5. A pause under a lock can be left with `Esc`. Today this state has no way out (bug candidates 1 and 2).
6. A `START` release no longer opens pause in 2 cases: a release that the lock makes, and a real release in the frame where the lock
   starts (question 3, bug candidate 1). A new issue file names this change. One case stays: a lock that a listener submits, for example
   `MLS:755`, starts 1 frame later, so a real release at that moment can still pause. This is an inference, from reading. `Esc` leaves that pause;
   a pad-only player has no way out. The new issue file names this case.
7. The diagnostics label `"controllerPoller"` is now `"inputFrames"`. Old capture files keep the old label.

## What must stay the same

- Every menu: the same buttons on the same edges (navigation on release, confirm on `START` or `A` release), in the same order.
- Every skip screen: the same buttons on the same edge. `CreditsScreen` stays on press.
- The default gamepad mapping, the aliases, the bindings, the replug decision, `--loadKeyboardPrefs`, `--resetBindings`.
- `START` and the `SELECT` pause action. The `isJustPressed(SELECT)` rule of `MLS:863-864`.
- A real pause: not paused, alive, in play. The queued `game.pause()`, in step 3 of the next frame.
- `Esc` gives no bit in a player frame, so `Megaman`, `ControllerSystem` and the behaviors never see it.
- The slot-0 frame builds once per `render()`, at the same point. The lock, `ControllerSystem` and `TURN_CONTROLLER_*`.
- Bindings capture, the refusal of `Esc`, and back to pause. Level music on the bindings screen.
- An unplugged gamepad does not pause the game.
- Both pads on the owner's USB adapter read 1 code set from the primary pad. This branch changes no pad code.
- The saved settings file. The save file.

## Tests

No menu screen has a test today, and `MegaMenuScreen` needs a game. So:

- `MenuScreenTest` (engine, on a real `StandardMenuScreen`): `onBack` runs on a back request; not when `selectionMade`; not
  when `isInteractionAllowed()` is `false`; a back request and a confirm in one frame, with the order that the contract fixes.
- `KeyboardInputTest`: `systemHeld()` with the real `Input.Keys.ESCAPE` gives the bit; other keys, also `Input.Keys.BACK`, give
  none; the case "gives no bit for Esc" stays.
- With the real `InputFrameBuilder` and `KeyboardInput`: `Esc` held gives 1 `JUST_PRESSED` and then `PRESSED`; a lock on the slot-0
  builder does not change the system frame.
- `MegaPlayersTest`: `menuInput` is the frame of slot 0.
- 2 seams for the pause, because `MLS` has no test (see "New APIs"), and a new test file `MegaMenuScreenTest` (contract question 3).
- `FrameControllerPollerTest` goes: 3 cases, so `core:test` falls by 3 before the new cases.
- The other checks need a running game: each row of the `Esc` table, the lock case, and the screen changes. They are manual checks
  in `CONTRACT.md`. `REFACTOR_TEST_031` exists, but its rooms (`spring_bouncer`, `pipe_portal`, `ground_slide_blocker`) were made for
  other checks. The contract uses `REFACTOR_TEST_051 --room basic` for the lock case.
- No temporary contract suite is needed for part 1, because the frame and the poller are the same value.

## Where a real library differs from a mock

`031d` showed that a mock can hide a bug. The source is `gdx` `1.14.2` and `gdx-backend-lwjgl3` `1.14.2`. `KeyboardInputTest`
uses a fake `Gdx.input`.

- **`isKeyPressed` reads `pressedKeys`, which the GLFW callback sets, and not the input processor** (`DefaultLwjgl3Input.java:142`). So
  `Esc` is seen as held during a capture, also while `keyboardListener` refuses it. A fake that goes through the processor hides this.
  The menu guard is what stops it. Check: a manual check of `Esc` in a capture.
- **`keyDown` runs before the frame builds** (table step 2). A refusal runs before `Esc` shows in the frame of that pass. A
  test that calls both in another order shows a state that cannot happen.
- **`GLFW_REPEAT` does not change `pressedKeys`** (`:156-161`). A held `Esc` gives 1 edge. A fake that repeats presses
  shows more.
- **`Input.Keys.ESCAPE` is `111`, and `Input.Keys.BACK` is `4`, the Android key.** Tests use the real constants.
- **`isKeyJustPressed` clears in `prepareNext()`, which the `asyncResized` return skips and which an iconified window skips**
  (`Lwjgl3Window.java:422-431`, `:439`). A mock clears it on each call. This is one reason against option b of question 1.
- **A press and a release between 2 samples give no held bit.** `START` has this limit too. It is more likely at a low FPS. Option b of
  question 1 does not have it.
- **The key states do not reset on a focus loss in this code.** `resetPollingStates` runs only in `windowHandleChanged`
  (`:180`). Inference, from memory of GLFW and not from a source here: GLFW sends a release for held keys when the window
  loses focus. No check depends on it, because the read is on press.
- **`game.render` runs `screen.draw` on the old screen after a change.** A unit test of a screen does not show it. Esc and the
  back row take the same path, so nothing new.

## Measured size

| | Files | Detail |
|---|---|---|
| Engine, deleted | 1 | `IControllerPoller.kt`, 54 lines, 0 engine readers |
| Engine, changed | 1 | `AbstractMenuScreen.kt` |
| Core, new | 1 | `SystemButton.kt` |
| Core, deleted | 1 | `FrameControllerPoller.kt`, 18 lines |
| Core, changed for frames | 8 | `MegaMenuScreen`, 6 skip screens, `ScreenController` |
| Core, changed for `Esc` | 10 | `G`, `MLS`, `KeyboardInput`, `MegaPlayers`, `LPS`, `MainMenuScreen`, `SelectDifficultyScreen`, `ExtrasScreen`, `LevelSelectScreenV2`, `CSS` (`MegaMenuScreen` is in the row above) |
| Tests | 4 | `FrameControllerPollerTest` deleted; `MenuScreenTest`, `KeyboardInputTest`, `MegaPlayersTest` changed |

18 changed core files in all, 1 new, 1 deleted; 9 read sites change. No change to `lwjgl3/`. `SaveGameScreen` does not change.
Docs to change at the merge: the skill line, the `082` lines, and the item records.

## Affected areas and risks

- **`Esc` on `ExtrasScreen` runs `resetPlayerProgress()`.** The `BACK` row does this today, and it is the first row. So `Esc` adds
  no new state, but it is 1 key. In-memory progress of an extra run goes. The save file does not change.
- **`LevelSelectScreenV2` back opens a new route.** `SAVE_GAME_SCREEN` reads `isInExtraLevel` in `show()`, on `CONTINUE` and in the
  `MAIN MENU` row. After an extra level, `currentLevel` stays set, so a save load then `Esc` opens the screen with the extras rows, and `MAIN MENU`
  there clears the loaded progress from memory. The save file does not change. Accepted, with a `TODO` at `LevelSelectScreenV2.onBack` and a bug
  candidate (contract question 4). M8 shows this state.
- **`Esc` during a lock:** without `!game.inputLocked`, `Esc` pauses in a room transition. The transition then stops, and a resume turns
  on all systems (`LevelStateHandler.kt:25-26`). The 2 lock tests prevent this, except for the listener lock of intended change 6.
- **`Esc` from a pause under a lock** (`--pauseOnFocusLost` or `--pauseOnMinimized`, both off by default and in no run script, or the listener lock of intended change 6): the
  resume turns on all systems during the lock sequence. A window focus event with `--resumeOnFocusGained` does this today.
- **`onBack` on the `LPS` grid must use the same guard as `onAnySelection`** (`!exiting && settingsScreenSlide.finished`), and the
  health fill blocks it through `isInteractionAllowed`.
- **`LPS.setCurrentButtonKey` ignores the key** (`:526`). So `onBack` must call the button, and must not move the cursor.
- **A confirm and a back in one frame:** the confirm runs first if the plan keeps the loop order. If a confirm returns `false` (for example
  `SETTINGS`), the back runs also. The contract fixes the order.
- **`CSS.render` keeps running after `closeOverPause()`** (the rest of `:400-429`). The `BACK` row does the same.
- **No gamepad back:** a pad-only player has no `Esc` (question 6). The owner's PS3 and NES-style pads get no new input.
- **`MLS` line numbers:** see the overlap with `051e`.

## Overlap with other branches

Scan on `c6b8cecf7`. `git worktree list`: the main checkout, `mega-refactor-051-master` (`d28c60893`) and
`mega-refactor-051e-camera-rotation` (`b79c6de35`). The other folders in `.worktrees/` are not registered worktrees. In `ITEMS.md`,
only those 2 branches are active.

Update at the contract approval: `051e` and its parent merged into `master` (`e12c880f5`), so the 2 items below on `051` are history. That merge also
changes `LPS` at the `EXIT` row, and `EXIT` then works in a `TEST_LEVEL`; no check depends on it. `084g` is active in its own worktree. Read the
line numbers of this file again on the current `master`.

- **`mega-refactor-051e-camera-rotation` (active; next step is the owner's play test).** It changes `G` at the imports, `:189-201`, and the
  `services` build at `:423-434`. In `MLS`, its hunks start at `:93`, `:214`, `:302`, `:314`, `:323`, `:370`, `:847`, `:884`, `:1035` and
  `:1076`. This branch changes `MLS:862-864` and `:876`. The `:847` hunk ends before `:862`, and the `:884` hunk starts 8 lines after
  `:876`. Inference: no conflict. Both branches also change `G`, in other lines. It changes no menu, no skip screen and no input
  file. Whichever merges second takes `master` first and runs its checks again. The line numbers here move after it merges.
- **`mega-refactor-051-master` (active, parent of `051e`).** It has the same base. No code of its own beyond `master`.
- **`084g-movement-state` (planned; no branch, no folder).** Its plan names `canMove`, `RunComponent`, the crouch timers and
  `entities/megaman/components/ControllerComponent.kt`. It reads the slot frame through `input`. This branch changes no entity file and no player frame.
  Inference from its plan text only: no shared file. `084h` is conditional and has no shared file.
- **`013` (planned).** It owns the pause flow and replaces `showOverPause` and `closeOverPause`. Its stack takes over the `Esc` pause
  entry and the `LPS` back. This branch adds 1 test at `MLS:876` and 1 `onBack`.
- **`012` (planned).** It replaces `TURN_CONTROLLER_*` and the lock. `game.inputLocked` becomes its query. `NOTES.md` says to run `012`
  and `031b`-like work apart: it edits `G.onEvent`. This branch does not.
- **`082` (planned).** Its `ITEM.md:74` says that "`031` must split 'gameplay input locked' from 'UI input' before this item". See
  question 6. The system frame already gives `082` the pattern: a builder that the lock does not touch.
- **`047` (planned).** "`Esc` pauses for both players" is true with 1 keyboard frame. `MegaPlayers.menuInput` is the one place that
  chooses the slot. Its `capacity` rises in `047`.
- **`055`, `010` (planned).** A replay must record the system frame too, or `Esc` is lost. It is 1 more `Long` per frame. A note for
  those items, no code here.
- **`034`, `069`, `070` (planned or idea).** `034` has no overlap. `069` adds a button to `MegaControllerButton`, and no menu reads it.
  `070` is the home of a pad back button, if the owner wants it later.
- **`016`.** Its note says "expose input as a service, not as more `game` fields". `game.systemInput` and `game.inputLocked` are 2 fields, but
  screens keep the game, and entities do not read these 2 values. No `MegaServices` change.

## Bug candidates

For `docs/TODO.md`. Files go on `master`. None is run in the game.

1. **A held `START` at the start of a lock pauses the game, and the pause cannot be left.** Reading of the code, not run. A room
   transition starts in `cameraController.update` and submits `TURN_CONTROLLER_OFF` (`LevelRoomHandler.kt:54`). `events.run()` sets
   `locked` in frame `N`. In `N+1`, the builder gives `JUST_RELEASED` for a held `START` (`InputFrameBuilder.kt:18-19`). `MLS:862` reads it, and
   `player.inPlay` is still `true`, because a transition does not change the phase. So `MLS:878` queues `game.pause()`. In `N+2`, the game
   pauses. All later frames are `RELEASED`, so the pause menu reads nothing. `MLS:912` stops `cameraController.update`, so the transition
   does not end, and `TURN_CONTROLLER_ON` (`LevelRoomHandler.kt:135`) never comes. The only way out is a flag that is off by default.
   The same applies at `MLS:755` (boss defeated) and `WilyFinalBoss.kt:311`, in play. `MLS:755` submits from inside `events.run`, so its lock
   starts 1 frame later.

   Trigger 2 (research of 2026-11-01): a real `START` release in frame `N`. `MLS:876` queues the pause before `events.run()` sets `locked`, so the
   pause runs at the start of `N+1` with the same dead menu.

   Origin: `6aebae6c2` (2025-02-15) changed pause from press to release through `runQueue`, and added the dead menu. `39f4d3629` (2025-03-01)
   removed the last default way out. `903c5d145` (2025-03-12) closed the way out that `c5339961f` had opened. `031a` and `031c` moved this logic
   and changed no behavior. The older `ControllerPoller` has the same false release. Question 3 fixes it here.
2. **Candidate 5 of the item `NOTES.md` is confirmed by reading:** a focus-loss or minimize pause in a lock gives the same dead menu. `Esc`
   gives a way out with this branch. The flags are off by default.
3. **A press read carries a held button to the next screen.** Inference: `CreditsScreen:89` skips on press. `fullEndTimer` is 1.5 s (`:51`). If
   `START` is held past that, its release on `MainMenuScreen` confirms `START NEW GAME`, which calls `gameState.reset()` (`:134`). This needs a
   check in the game. Not in this scope.
4. **Candidate 4 of the item `NOTES.md` is still open:** a rebind of `START` or `A` with the key held past `0.25` s confirms the row again. Not in
   this scope.
5. **Docs:** the item `NOTES.md` says 9 live menu instances. The count at this commit is 8.

## Backlog candidates

- Remove the dead `ScreenController` path or give it a real use: `screenController`, `showScreenController`, `--showScreenController`
  (question 9). The `TODO` for mobile is the link.
- Remove the dead `LevelSelectScreen`.
- A gamepad back button (question 6), in `070`.

## Open questions

Answered (owner, 2026-11-01, verbatim): "scope approved", as recommended: each question below takes its recommendation.

1. **How does `Esc` reach the readers?**
   - a) A second frame: `SystemButton.BACK`, `KeyboardInput.systemHeld()`, a second `InputFrameBuilder` that is never locked. It uses the same edge
     rules and the same sample point as every button, which is the goal of this branch. A test uses the real builder. A later replay can record it.
   - b) `Gdx.input.isKeyJustPressed(ESCAPE)` at each reader. No new type. It also catches a press and release between 2 samples. But it is a
     second edge rule, it depends on `prepareNext()` (see the mock section), and it needs a mocked library in each test.
   - c) A 9th bit in the slot frame. Against the decision "a system key, not a game button". The lock then hides it, also in menus, and each
     slot needs an owner of the key.
   - Recommendation: a).

2. **Does `Esc` act on press or on release?** Menus confirm on release, and `START` pauses on release.
   - a) Press. A held `Esc` gives 1 edge, and nothing fires when it is let go. So a press that opens pause does not also close it, and a release
     after a refused capture does nothing. It carries to the next screen as a held key, with no effect, because `Esc` reads only on press.
   - b) Release, as the menus. A press refused in a capture and released after the capture ends can fire a back action. A held `Esc` that crosses a
     screen also fires on the new screen.
   - Recommendation: a).

3. **The lock test at `MLS:876`, and bug candidate 1.**
   - a) One test, `!game.inputLocked`, on the whole line. `Esc` and `START` both obey it. It fixes the soft-lock. It is an intended change, with a new
     issue file. It adds 1 condition, on the line that `Esc` changes anyway. Owner decision of 2026-11-01 (verbatim: "Go ahead and fold the bug
     fix in like you recommended"): the queued pause also tests the lock, for trigger 2 of bug candidate 1.
   - b) The test on `Esc` only. `START` plays as today, and the soft-lock stays, with a `fix/` branch later. This follows the rule that bugs are
     not refactors most closely.
   - c) No test. `Esc` pauses in a transition. This adds new states, and a resume turns on all systems.
   - Recommendation: a). The issue rule allows it through "Intended changes". It needs your word, because it changes `START` in a rare case.

4. **`Esc` on the pause grid.**
   - a) Resume, with the weapon unchanged. `Esc` opens pause, so `Esc` closes it. `LPS` has no other "close" input today.
   - b) Nothing. `Esc` then has no meaning on the first screen that it opens.
   - c) Move the cursor to `EXIT` or `SETTINGS`. A new rule.
   - Recommendation: a).

5. **Menus with no back row.**
   - a) `LevelSelectScreenV2` goes to `SAVE_GAME_SCREEN`, the target of the old screen's `BACK` row. `SaveGameScreen` and the main menu root do
     nothing. The save screen has no back row, and its `MAIN_MENU` row can lose an unsaved state, so a stray key must not run it. The root must
     not quit.
   - b) `LevelSelectScreenV2` also does nothing. No new route, and `Esc` is back only where a back row exists. The screen stays with no way back.
   - c) `SaveGameScreen` also goes to the main menu. A stray `Esc` can then lose an unsaved state.
   - Recommendation: a).

6. **The split of "gameplay input locked" from "UI input" (`082`).** The `082` text says that `031` must do it. Menus read locked frames today, and
   the only case with a live menu under a lock is the flag path of bug candidate 2.
   - a) Menus keep reading the slot frame. `082` builds an unlocked frame when it has a caller, with the system frame as the pattern. Update the
     `082` text at the merge.
   - b) Menus read a new unlocked frame. A third builder, for a state that `Esc` already handles. Behavior in the lock path changes.
   - Recommendation: a), by the rule "write code only when production code calls it".

7. **How do menus read the frame?**
   - a) `MegaPlayers.menuInput`, 1 property: `inputs[PlayerSlot(0)]`. 8 files read it. `047` changes 1 line.
   - b) `game.players.inputs[PlayerSlot(0)]` in each of the 8 files. No new API. `PlayerSlot(0)` is then in 8 more places.
   - c) A `UiInput` class with navigate, confirm, back and skip. The `031` decision keys frames by button, and the code that reads a
     button gives it its meaning. No case here needs another class.
   - Recommendation: a).

8. **Where does the back action live?**
   - a) In the engine loop: 2 protected functions on `AbstractMenuScreen`, under `isInteractionAllowed()`. `MenuScreenTest` covers it on a real
     class. "Back" is a genre concept, so the engine rule allows it.
   - b) In `MegaMenuScreen.render`, core only. No engine change, but no test, because the class needs a game. It must copy the guard.
   - Recommendation: a).

9. **`ScreenController` (dead code).**
   - a) Change the 1 read to a frame, and leave the rest. The `2026-10-25` decision says that the overlay is out of scope.
   - b) Delete the class, the field, the flag and its parameter. This removes the `TODO` for mobile, and this branch is not about it.
   - Recommendation: a), with a backlog candidate.

10. **`B` as back on a gamepad.** Today `B` does nothing in menus. The decision names only `Esc`.
    - a) `Esc` only. Pad players keep the back rows. On `LevelSelectScreenV2` a pad-only player still has no back.
    - b) `B` is back also, on release, as the other menu reads. A pad-only player gets a back action, and on the NES-style pad `B` is the only free
      button. But each menu then has a new meaning for a button that does nothing today, and the 2 decisions name only `Esc`.
    - Recommendation: a), with a backlog candidate in `070`.

11. **One branch? And the item status?**
    - a) One branch. Frames first (no behavior change, proved by the tests), then the system frame, the back action, `Esc` in gameplay, and the
      removals last. It merges directly into `master`, as `031a` to `031e` did. At the merge, `031` becomes `done`, because it has no more branches.
    - b) Two branches: frames and removals, then `Esc`. The removals need only the first. A clean split, but one more merge and play test.
    - Recommendation: a).
