# Contract: mega-refactor-031f-menu-input

The gameplay that this branch must not change, the gameplay that it changes on purpose, and how to check both. This
was written ahead of time on `master` (`72be803bd`), before the branch existed. It follows the approved
`docs/SCOPE.md` (2026-11-01), with the owner's change of that day to section 4 and intended change 6.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "yes")

The menus, the skip screens and `ScreenController` read frames, not `game.controllerPoller`. This part changes no
result in any frame. `Esc` is a system key: back in menus and pause in gameplay. A `START` release at the start of a
lock no longer opens pause. Everything else about input must feel the same: every menu, every skip screen, the keys, the
pads and the bindings screens.

The largest risk is the lock path, so it has its own check group (L1 to L6).

The code that this contract cites is the same at `a41a82ceb` and at `72be803bd`: the 3 commits between them change
only docs. `E` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`C` = `core/src/main/kotlin/com/megaman/maverick/game/`, `G` = `C/MegamanMaverickGame.kt`, `MLS` =
`C/screens/levels/MegaLevelScreen.kt`, `LPS` = `C/screens/menus/level/LevelPauseScreen.kt`, `CSS` =
`C/screens/menus/ControllerSettingsScreen.kt`, `GSP` = `C/screens/utils/GameSettingsPanel.kt`.

Line numbers moved after this contract was written. In `docs/PLAN.md`, the section "Line numbers and contract text" lists
each one. For example, `MLS:876` and `:878` are now `:874` and `:876`, and `G:574` is `:573`.

## Where a real library differs from a mock

In `031d` a mock hid a real bug. These are the cases for `031f`. Each case names its check. The source is `gdx`
`1.14.2` and `gdx-backend-lwjgl3` `1.14.2`.

- **`isKeyPressed` reads `pressedKeys`, which the GLFW callback sets, and not the input processor.** So `Esc` shows as
  held during a keyboard capture, also while `keyboardListener` refuses it. The fake in `KeyboardInputTest` reads a
  set of held keys, as `pressedKeys` does, and has no processor. It cannot show a capture. So the menu guard has a unit
  test on a real class, and the capture has manual checks. [T3, C2, C5]
- **`keyDown` runs before the frame builds.** The refusal runs before `Esc` shows in the frame of that pass. No test
  calls the 2 in the other order, because that state cannot happen. [C2]
- **`GLFW_REPEAT` does not change `pressedKeys`.** A held `Esc` gives 1 edge. The test fake holds a set and never
  repeats a press. [T14, E1]
- **`Input.Keys.ESCAPE` is `111`, and `Input.Keys.BACK` is `4`, the Android key.** The tests use the real constants and
  assert both values. [T12]
- **`isKeyJustPressed` clears in `prepareNext()`, and some passes skip that call.** A mock clears it on each call. The
  branch does not read it. [A12]
- **A press and a release between 2 samples give no held bit.** `START` has this limit also, and it is more likely at a
  low FPS. This branch accepts it. No check depends on it.
- **The key states do not reset on a focus loss in this code.** L6 uses a focus loss, but it presses `Esc` only after
  the window has the focus again, so it does not depend on this. [L6]
- **`game.render` runs `screen.draw` on the old screen after a change.** A unit test of a screen does not show it. `Esc`
  and the back row take the same path. [M6, P7]
- **The order of `MLS:876` and `events.run()` is only real in the running game.** A test that sets `locked` on a real
  builder proves the decision, not the order. So T32 steps the frames by hand in the order of `render()`, and the manual
  checks prove the order in the game. [T19, T32, L1, L2]
- **`InputFrame` is a value class.** A `mockk` stub of a function that returns it is a known weak point, and a relaxed
  mock gives a default value in place of a real frame. So the tests build frames with the real `InputFrameBuilder`,
  and `MegaMenuScreenTest` reads them through a real `MegaPlayers`. No test stubs a function that returns an
  `InputFrame`. So `game.systemInput` has no unit test in a menu. [T14, T15, T19, T27, T32, R2, P1, P3]
- **Window focus comes from GLFW.** Only a run of the game shows it. [L6]
- **A real pad list.** This branch changes no pad code, and a capture still needs a real pad object. [C5, V2, V3]

## Automated checks

Run from the worktree, on a clean build:

```
JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar 2>&1 | tee build.log
```

Read the test counts from `engine/build/test-results/test/` and `core/build/test-results/test/`. Baseline: the `031e`
entry in `docs/mega-refactor/process-log.md`, on `d28c60893`: the build passes; engine 600 tests, core 528 tests, 0
failures; 76 unique warning lines with the line numbers removed. `master` has only docs commits since then, at
`72be803bd`. Plan step 1 measures these numbers again on the base commit of the branch and writes the sorted warning
list in the worktree. If `mega-refactor-051-master` reaches `master` first, the base numbers change, and the check
uses the numbers of plan step 1.

Kotest here does not accept the Gradle `--tests` filter, so each run of one test is a full `core:test` run.

### Build and counts

- [ ] A1. The command above passes.
- [ ] A2. Engine: `Eb` plus the 10 new tests, 0 failures. `Eb` is the count that plan step 1 measures.
- [ ] A3. Core: the minimum is `Cb + 20`, 0 failures: `Cb` is the count that plan step 1 measures, minus the 3 cases of
      `FrameControllerPollerTest`, plus T11 to T33 (23 tests). No other old case is lost: the 13 `KeyboardInputTest` cases
      stay, also "gives no bit for Esc".
- [ ] A4. Warnings: no new line against the sorted list of plan step 1. Count with
      `grep '^w:' build.log | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u | wc -l`. Compare the sorted lists, not only the
      counts. A line can leave the list.
- [ ] A5. `utils/diagnostics/run_tests.sh` passes. The parser does not name the old label.

### Scope greps (from the worktree)

The commands use `master...HEAD`, because `031d` found that `git diff master` also shows the reverse of new `master`
changes.

- [ ] A6. `git diff master...HEAD --name-status -- engine/src/main` lists 2 lines: `D` for
      `controller/polling/IControllerPoller.kt` and `M` for `screens/menus/AbstractMenuScreen.kt`. This proves that
      `InputFrameBuilder`, `InputFrame`, `ControllerSystem` and the rest of the engine do not change.
- [ ] A7. `git diff master...HEAD --name-status -- core/src/main lwjgl3/` lists only these 20 paths:
  - Added: `C/input/SystemButton.kt`.
  - Deleted: `C/input/FrameControllerPoller.kt`.
  - Changed for frames: `C/screens/menus/MegaMenuScreen.kt`, `C/screens/other/LogoScreen.kt`,
    `C/screens/other/AbstractStoryScreen.kt`, `C/screens/other/GameOverScreen.kt`,
    `C/screens/other/GetWeaponScreen.kt`, `C/screens/other/SimpleInitGameScreen.kt`,
    `C/screens/other/CreditsScreen.kt`, `C/controllers/ScreenController.kt`.
  - Changed for `Esc`: `G`, `MLS`, `C/input/KeyboardInput.kt`, `C/players/MegaPlayers.kt`, `LPS`,
    `C/screens/menus/MainMenuScreen.kt`, `C/screens/menus/SelectDifficultyScreen.kt`,
    `C/screens/menus/ExtrasScreen.kt`, `C/screens/menus/level/LevelSelectScreenV2.kt`, `CSS`.
  - So `SaveGameScreen.kt`, `GSP`, `C/input/GamepadInput.kt`, `C/players/PlayerSettings.kt`,
    `C/players/PlayerSettingsStore.kt`, `C/controllers/MegaControllerButton.kt`, `C/MegaServices.kt`, every file in
    `C/entities/` and all of `lwjgl3/` do not change. The new file of open question 3 is the only allowed addition.
- [ ] A8. `git diff master...HEAD --name-status -- core/src/test engine/src/test` lists only: `MenuScreenTest.kt`,
      `KeyboardInputTest.kt`, `MegaPlayersTest.kt` (changed), `FrameControllerPollerTest.kt` (deleted), and the 2 new
      test files of open question 3.
- [ ] A9. `git diff master...HEAD --stat -- assets/ gradle.properties` gives no output. This branch adds no test map.
- [ ] A10. `grep -rnE 'controllerPoller|IControllerPoller|FrameControllerPoller' core/src engine/src lwjgl3/src` gives
      no output, and `ls engine/src/main/kotlin/com/maverick/game/engine/controller/polling` fails.
- [ ] A11. `grep -rnE 'selectionButtons|START_BUTTONS|END_BUTTONS|EARLY_EXIT_BUTTONS' core/src` gives no output, and
      `grep -rn 'isAnyJust' core/src engine/src lwjgl3/src` gives no output.
- [ ] A12. `grep -rnE 'isKeyPressed|isKeyJustPressed' core/src/main lwjgl3/src/main` adds only 1 line against `master`:
      the `Esc` read in `C/input/KeyboardInput.kt`. `grep -rn 'ESCAPE' core/src/main lwjgl3/src/main` lists 2 lines,
      both in `C/input/KeyboardInput.kt`.
- [ ] A13. In `G`: `grep -c 'slot0FrameBuilder.build'` gives `1`; `grep -c 'beginEntry("inputFrames")'` gives `1`;
      `grep -c 'InputFrameBuilder(SystemButton.entries)'` gives `1`. In `MLS`, the grep of `inputLocked` gives 2 lines:
      the test at `:876` and the test in the queued pause at `:878`. If the plan moves the second test into the function
      of open question 3, the grep gives 1 line in `MLS`, and T32 and R6 hold the rule.
- [ ] A14. `grep -n 'locked' core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt` lists only
      `slot0FrameBuilder.locked` and `inputLocked` lines. The system builder is never locked.
- [ ] A15. `grep -rl 'systemInput' core/src/main` lists `MegamanMaverickGame.kt`, `MegaMenuScreen.kt` and
      `MegaLevelScreen.kt`. `grep -rl 'inputLocked' core/src/main` lists `MegamanMaverickGame.kt` and
      `MegaLevelScreen.kt`. No entity reads either, and no skip screen reads `systemInput`.
- [ ] A16. `grep -rl 'SystemButton' core/src/main` lists `SystemButton.kt`, `KeyboardInput.kt`,
      `MegamanMaverickGame.kt`, `MegaMenuScreen.kt` and `MegaLevelScreen.kt`, plus the file of open question 3 if the
      plan puts the function there. So it does not reach the bindings screen or a player frame.
- [ ] A17. `grep -rl 'menuInput' core/src/main` lists 9 files: `MegaPlayers.kt`, `MegaMenuScreen.kt`, the 6 skip screen
      files of A7, and `ScreenController.kt`.
- [ ] A18. `grep -n 'TODO' core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt` lists 1 new
      line at the `Esc` read that says to consider another action for `Esc` in gameplay later (`2026-10-25` decision).

### Named tests

The test fake of `Gdx.input` reads a set of held keys. See "Where a real library differs from a mock".

`MenuScreenTest` (engine; on a real `StandardMenuScreen`, as today; the 5 cases stay):

- [ ] T1. A back request runs `onBack` once, with the delta of the frame. No `onSelect` runs, and the key does not
      change.
- [ ] T2. No back request: `onBack` does not run.
- [ ] T3. `selectionMade` is `true`: `onBack` does not run.
- [ ] T4. `isInteractionAllowed()` is `false` and `selectionMade` is `false`: `onBack` does not run.
- [ ] T5. The current key is `null`: `onBack` does not run.
- [ ] T6. The current key has no button: `onBack` does not run.
- [ ] T7. A back request and a confirm in one frame, and `onSelect` gives `true`: the confirm runs, and `onBack` does
      not (open question 2).
- [ ] T8. A back request and a confirm in one frame, and `onSelect` gives `false`: the confirm runs, `onBack` does not,
      and `selectionMade` stays `false`.
- [ ] T9. A back request and a navigation in one frame: the navigation runs first, then `onBack`.
- [ ] T10. A subclass with no override: `backRequested()` gives `false`, and the screen acts as today.

`KeyboardInputTest` (core; the 13 cases stay):

- [ ] T11. `systemHeld()` with `Input.Keys.ESCAPE` down gives the bit of `SystemButton.BACK`.
- [ ] T12. `Input.Keys.ESCAPE` is `111` and `Input.Keys.BACK` is `4`. Each other code in `0..255`, also
      `Input.Keys.BACK` and `Input.Keys.ENTER`, gives `0`.
- [ ] T13. With `Esc` and the key of `A` down: `held()` gives only the `A` bit, and `systemHeld()` gives only the `BACK`
      bit.
- [ ] T14. Real `InputFrameBuilder(SystemButton.entries)` and real `KeyboardInput`: `Esc` held for 3 builds gives
      `JUST_PRESSED`, `PRESSED`, `PRESSED`. Then `Esc` up gives `JUST_RELEASED`, `RELEASED`.
- [ ] T15. Two real builders. `locked` on the slot-0 builder makes the slot frame go to `RELEASED`, and the system frame
      still gives `JUST_PRESSED` for a new `Esc` press.
- [ ] T16. `SystemButton.entries` has 1 entry, `BACK`, and `checkInputButtons(SystemButton.entries)` passes.

`MegaPlayersTest` (core; the cases stay):

- [ ] T17. With capacity 2, `menuInput` gives the frame of slot 0, not of slot 1.
- [ ] T18. `menuInput` is `InputFrame.RELEASED` before any write, also with no registered player.

The pause decision (new test; open question 3; it uses real builders and real frames):

- [ ] T19. `START` held, then the first build with `locked` `true`: the frame has `JUST_RELEASED`, and the decision with
      the lock on is "no pause".
- [ ] T20. A real `START` release with no lock: "pause".
- [ ] T21. `Esc` `JUST_PRESSED` with no lock: "pause". `PRESSED`: "no pause".
- [ ] T22. `Esc` `JUST_PRESSED` with the lock on: "no pause".
- [ ] T23. `SELECT` `JUST_PRESSED`: "pause" only with `SelectButtonAction.START`, not with `NEXT_WEAPON` or `AIR_DASH`.
      `SELECT` `JUST_RELEASED`: "no pause".
- [ ] T24. `SELECT` with `SelectButtonAction.START` and the lock on: "no pause".
- [ ] T25. Each of paused, not alive and not in play gives "no pause" for a `START` release and for `Esc`.
- [ ] T26. `START` held across the end of a lock: the next builds give `JUST_PRESSED` and `PRESSED`, "no pause". The
      release gives "pause".
- [ ] T32. Frame sequence of trigger 2, with a real `InputFrameBuilder` and a real run queue, in the order of `render()`.
      Frame `N`: `START` up after a hold gives a real `JUST_RELEASED`, the lock is off, so the pause request is queued.
      Then `locked` becomes `true`, as `events.run()` does. Frame `N+1`: the queue drains before the build. The queued
      pause does not call pause, and the paused flag stays `false`.
- [ ] T33. The same sequence with no lock in frame `N`: the queued pause calls pause 1 time. So the test in the runnable
      drops only a pause under a lock.

`MegaMenuScreenTest` (new; open question 3; a small subclass with a relaxed mock of the game, which gives a real
`MegaPlayers`):

- [ ] T27. `JUST_RELEASED` of each d-pad button gives its `Direction`.
- [ ] T28. 2 d-pad releases in one frame give the first of `UP`, `DOWN`, `LEFT`, `RIGHT`.
- [ ] T29. `JUST_PRESSED` and `PRESSED` of a d-pad button give `null`.
- [ ] T30. `selectionRequested()` is `true` on `JUST_RELEASED` of `START`, and of `A`.
- [ ] T31. `selectionRequested()` is `false` for `JUST_RELEASED` of `B` and `SELECT`, and for `JUST_PRESSED` of `START`.

No temporary contract suite: the poller and the frame are the same value (`docs/SCOPE.md`, "Frame order"). T27 to T31
and R3 hold the menu rules, and they stay on `master`. Each row of the `Esc` table needs a running game, so the manual
checks cover it.

### Review (read the diff, no play needed)

- [ ] R1. Each `onBack` with a back row (`SelectDifficultyScreen`, `ExtrasScreen`, `CSS`, the `MainMenuScreen` panel, the
      `LPS` panel) calls the `IMenuButton.onSelect` of that row, then the same `selectionMade` and `onAnySelection` path
      as a confirm. No `onBack` holds a copy of the row body.
- [ ] R2. `MegaMenuScreen.backRequested()` reads only `game.systemInput.isJustPressed(SystemButton.BACK)`.
      `MainMenuScreen` and `LPS` add their slide test.
- [ ] R3. The 9 reads keep their buttons and edges: `MegaMenuScreen` navigation on release, in the order `UP`, `DOWN`,
      `LEFT`, `RIGHT`; confirm on `START` or `A` release; `LogoScreen`, `AbstractStoryScreen` and `GameOverScreen` on
      `START` release; `GetWeaponScreen` on `A`, `B` or `START` release; `SimpleInitGameScreen` on `START`, `A` or `B`
      release; `CreditsScreen` on `START` or `SELECT` press; `ScreenController` on `isPressed`.
- [ ] R4. `LPS.onBack` on the grid does not write `currentWeapon`, does not call `setCurrentButtonKey`, and uses the guard
      `!exiting && settingsScreenSlide.finished`.
- [ ] R5. `MainMenuScreen` root and `SaveGameScreen` have no back action. `SaveGameScreen.kt` does not change (A7).
- [ ] R6. `isPauseRequested` keeps the order of tests of `MLS:874` and adds only `!locked`. The queued pause at `:876` is
      `game.runQueue.addLast { if (!game.inputLocked) game.pause() }`, or the function of open question 3 with the same
      effect. The pause stays in the queued runnable. The test reads the lock when the runnable runs, not when it is
      added.
- [ ] R7. `game.inputLocked` reads `slot0FrameBuilder.locked` and has no setter.
- [ ] R8. The system builder builds next to slot 0, in the same `else` block of `render()`, before `screen.render`.
- [ ] R9. Each new API has a caller from the table in `docs/SCOPE.md`. The only additions are the ones that the open
      questions approve.
- [ ] R10. `MegaPlayers.capacity` stays `1`. `PlayerSlot(0)` in a menu read is only in `menuInput`.
- [ ] R11. `WilyFinalBoss.kt:311` needs no manual check: its lock goes through `TURN_CONTROLLER_OFF`, so the same guard
      acts on it. T19 and W1 cover the rule.

## Performance

No performance captures: the scope calls for none. The new per-render work is 1 `isKeyPressed` array read and 1
`InputFrameBuilder.build` of 1 button. `held()` already does 14 such reads per render. `InputFrame` is a value class. The
menu reads lose `FrameControllerPoller.getStatus`, the `is` test and the `Array<Any>` walk of `isAnyJustReleased`. The
diagnostics entry with the new label (A13, D1) holds the new work, so a later capture shows its cost.

## Intended changes

Each entry is a gameplay change from `docs/SCOPE.md` "Intended changes". The checks that prove it are in brackets.

1. `Esc` opens pause in gameplay, on the same tests as `START`: not paused, alive, in play, not locked. Today it does
   nothing. It acts on the press. [E1, E2, E3, L3, T21, T22, T25]
2. `Esc` on the pause grid resumes the game, and the weapon stays the same. [P1, P2, P5, R4]
3. `Esc` on a settings panel, `SelectDifficultyScreen`, `ExtrasScreen` and both bindings screens runs the back row, with
   its sound and side effects. From the bindings screen over pause, it goes back to the panel, in the same level state.
   [P3, P4, P7, M2, M3, M4, C1, C2, C3, C4, T1 to T9, R1]
4. `Esc` on `LevelSelectScreenV2` goes to `SAVE_GAME_SCREEN`. Today this screen has no way back. [M6, M7, M8, W2]
5. A pause under a lock can be left with `Esc`. [L6, T15]
6. A `START` release no longer opens pause in 2 cases: a release that the lock makes (trigger 1), and a real release in
   the frame where the lock starts (trigger 2). Today each gives a pause menu that reads no input, and the game cannot
   go on. By reading, this is older than `031` (`docs/SCOPE.md`, bug candidate 1). An issue file names this change
   (open question 6). A real `START` release after the lock ends still opens pause. [L1, L2, L4, L5, W1, T19, T26,
   T32, T33, R6, A13]

   One case stays, and no check requires it to change: a lock that a listener submits, for example `MLS:755`, starts 1
   frame later, so a real release at that moment can still pause. This is an inference, from reading. `Esc` leaves that
   pause, as it leaves each pause under a lock [L6, T15]. A pad-only player has no way out. The issue file names this
   case.
7. The diagnostics label `"controllerPoller"` is now `"inputFrames"`. Old capture files keep the old label. [D1, A13]

## Must stay the same

- **Every menu:** the same buttons on the same edges, in the same order. Navigation acts on release, and confirm acts
  on `START` or `A` release. [V1, V2, V3, T27 to T31, R3]
- **Every skip screen:** the same buttons on the same edge. `CreditsScreen` stays on press. [K1 to K6, W1, R3]
- **`Esc` on a skip screen and on other non-menu screens does nothing.** [K1 to K6, W1, A15]
- **The main menu root and `SaveGameScreen`: `Esc` does nothing.** The root does not quit, and the save screen does not
  leave. [M1, M5, R5]
- **`START` and the `SELECT` pause action**, and the `isJustPressed(SELECT)` rule of `MLS:863-864`. A menu does not
  confirm on the release that opened pause, and a resume does not pause again. [V4, T20, T23, R6]
- **A real pause:** not paused, alive, in play. The queued `game.pause()`, in step 3 of the next frame, if no lock has
  started. [E3, T25, T33, R6]
- **`Esc` gives no bit in a player frame**, so `Megaman`, `ControllerSystem` and the behaviors never see it. [G1, T13, A7]
- **The slot-0 frame** builds once per `render()`, at the same point. The lock, `ControllerSystem` and
  `TURN_CONTROLLER_*` do not change. [A6, A13, R8]
- **The bindings:** capture, the refusal of `Esc`, back to pause, and level music on the bindings screen. [C2, C5, P7]
- **The default gamepad mapping, the aliases, the bindings, the replug decision, `--loadKeyboardPrefs`,
  `--resetBindings`.** This branch does not touch their files. [A7, V1, V2]
- **An unplugged gamepad does not pause the game.** [G2]
- **Both pads on the owner's USB adapter read 1 code set from the primary pad.** [A7, V3]
- **The saved settings file and the save file.** [S1, X1]

These points have no manual check, and this is why:

- **Trigger 2 of intended change 6.** One exact frame cannot be hit by intent. T32, T33 and R6 cover it. A manual
  check cannot show it, also if a try lands in that frame by chance.
- **The listener lock of intended change 6.** It also needs one exact frame. L6 reaches the same state, a pause under a
  lock, and shows that `Esc` leaves it.
- **The `SELECT` pause action.** A manual check needs a change of `SELECT ACTION`, and that row saves at once to the
  settings file. T23, T24 and R6 cover it.
- **`--pauseOnMinimized`.** It calls `game.pause()`, as `--pauseOnFocusLost` does, so L6 covers the same path.
- **A pause during a gate.** Issue `docs/issues/2026-10-22-pause-during-gate-reenables-physics.md` stays open, and `Esc`
  reaches it on the same test as `START`. The scope leaves it to `012`.
- **Each of the 3 buttons of `GetWeaponScreen` and of `SimpleInitGameScreen`.** Each screen has 1 manual check with 1
  button. R3 covers the others.

## Manual play test

The owner plays these once, after the implementation, from the worktree. The owner knows today's behavior, so there is
no baseline play-through. Experimental checks come first. A problem in an experimental check leads to a fix, and the
fix means that the control checks run again.

No new test room is needed. The rooms are in the live TMX files, read at `72be803bd`:

- `REFACTOR_TEST_029` (`RefactorTest029.tmx`): `movement`, `enemy_aim`, `player_death`. The level gives `FRIGID_SHOT` and
  `MOON_SCYTHES`.
- `REFACTOR_TEST_051` (`RefactorTest051.tmx`): `basic`. The spawn is 12 tiles left of the open right edge, with only a
  ladder between them and no enemies. `basic_right` is the next room. `REFACTOR_TEST_031` and `REFACTOR_TEST_029` have no
  transition near a spawn: their rooms stand apart.
- `INTRO_STAGE` (`IntroStage_v3.tmx`): `room3`. The spawn is 2 tiles right of the open left edge, and `room2` is the next
  room. `room12` has the boss `Gate`.
- `TIMBER_WOMAN` (`TimberWoman_16x14_v4.tmx`): `room14` has the boss `Gate`.
- `GAUNTLET_1` (`Gauntlet1_v2.tmx`): `room1`.

Sound: the sound steps need the `run-game-alpha.sh` of the branch, with volumes `0.5`. The mute in the main checkout is
a local edit, and it is not in the worktree.

Pads: the keyboard is the main device. Only the checks marked `[pad]` need a pad. Connect the PS3 pad and the NES-style
pad on the USB adapter before the launch of such a check. Unplug the adapter for the other checks, if you prefer.

### Prefs safety

No check needs a known prefs state, so there is no backup and no restore. The menu steps use `Enter` and the arrow
keys, which work with any saved bindings. No step saves: a capture ends with the key that the row already has, which
is a keep and writes nothing, or with the loss of the pad. No step selects `SAVE`. These 2 steps prove it. They only
read `~/.prefs`, and they run with the game closed.

- [ ] S1. Before the first check: `find ~/.prefs -type f -exec sha1sum {} + 2>/dev/null | sort > /tmp/031f-prefs.sha1`

X1 at the end compares the files.

The checks use these words:

- "Press": press and release the key, unless the step says "hold".
- "Select" a row: move the cursor to the row with the arrow keys, then press and release `Enter`. The menus act on the
  release.
- "Wait for control": wait until the `READY` text ends and Mega stands on the floor.
- "Pause": press and release `Enter` in the level. "The grid" is the pause screen. "The gear" is its settings entry. "The
  panel" is the settings panel behind the gear.
- "Close the game": close the window.

All commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-031f-menu-input`

### Experimental: `Esc` in gameplay

Keyboard only.

- [ ] E1. Press, not release. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Wait for control. Hold `Esc`. See: while the key is down, the pause sound plays 1 time and the grid opens.
  2. Keep `Esc` down for 3 seconds. See: the grid stays, with no flicker. Release `Esc`. See: no change.
  3. Hold `Esc`. See: while the key is down, the grid closes with the pause sound, and the level runs.
  4. Keep `Esc` down for 3 seconds. See: the level runs, and the grid does not open. Release `Esc`. See: no change.
- [ ] E2. Same state as `START`. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Wait for control. Let a `SniperJoe` shot or a `Met` shot hit Mega once.
  2. Press `Esc` while a shot is in flight. Note Mega's position, the health bar, the shot and the enemies.
  3. Press `Esc`. See: all are where they were at step 2. The shot goes on from that place, the enemies go on with their
     pattern, and the controls work at once.
- [ ] E3. Not in play. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`
  1. During the `READY` text, press `Esc` 3 times. See: no pause sound and no grid.
  2. Wait for control. Hold the right arrow and run into the pit. While Mega dies and during the next `READY` text, press
     `Esc` 3 times. See: no pause sound and no grid. Mega respawns in this room.
  3. Wait for control. Press `Esc`. See: the grid opens. Press `Esc`. See: the grid closes.

### Experimental: the lock

L1 to L3, L5 and L6 are keyboard only. L4 is `[pad]`. On the old code, L1 gives the dead pause menu. Trigger 2 has no
manual check: see "Must stay the same", the list of points with no manual check.

- [ ] L1. Held `START` across the start of a transition (trigger 1). `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic`
  1. Wait for control.
  2. Hold `Enter`. See: nothing. A pause acts on the release.
  3. With `Enter` still down, hold the right arrow. Mega runs right, past the ladder, to the right edge of the room.
  4. Mega stops, and after a short delay the camera moves to `basic_right`. While the camera moves, release `Enter`. Then
     release the right arrow.
  5. See: at no time a pause sound or the grid. The camera finishes its move, and Mega is in `basic_right`.
  6. Hold the left arrow for a moment. See: Mega runs. Press the jump key (default `K`). See: Mega jumps.
  7. Pause. See: the grid opens, 1 time. Press `Esc`. See: the grid closes.
- [ ] L2. `START` released after the transition. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic`
  1. Do L1 step 1 to step 3.
  2. Keep `Enter` and the right arrow down while the camera moves, until it stops and Mega runs in `basic_right`. See: no
     pause sound and no grid.
  3. Release the right arrow. See: no pause sound and no grid.
  4. Release `Enter`. See: the pause sound, 1 time, and the grid. This is a real release, after the lock.
  5. Press `Esc`. See: the grid closes. The controls work.
- [ ] L3. `Esc` during the transition. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic`
  1. Wait for control. Hold the right arrow until the camera moves to `basic_right`. Press `Esc` 1 time while it moves.
     Release the right arrow.
  2. See: no pause sound and no grid, also after the camera stops. The controls work.
  3. Hold the left arrow until the camera moves to `basic`. While it moves, hold `Esc`. Release the left arrow, and keep
     `Esc` down until the camera stops and for 2 more seconds. See: no grid.
  4. Release `Esc`. See: no change.
  5. Press `Esc`. See: the grid opens. Press `Esc`. See: the grid closes.
- [ ] L4. `[pad]` The same with the PS3 pad. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic`
  1. Do L1 step 1 to step 5, with the pad's `START` button in place of `Enter` and the d-pad in place of the right arrow.
     See: the same result.
  2. In `basic_right`, hold `START`, then hold left on the d-pad. Keep both down while the camera moves to `basic` and
     until Mega runs. Release left. Release `START`. See: the grid opens on the release of `START`, 1 time.
  3. Press the pad's `A` button on a weapon. See: the grid closes.
- [ ] L5. Real stage. `./run-game-alpha.sh --level INTRO_STAGE --room room3`
  1. Wait for control. Hold `Enter`.
  2. With `Enter` still down, hold the left arrow. Mega runs off the left edge, and the camera moves to `room2`. While it
     moves, release `Enter`. Then release the left arrow.
  3. See: no pause sound and no grid. Mega is in `room2`, and the stage music plays on.
  4. Hold the right arrow until the camera moves back to `room3`. Pause. See: the grid opens. Press `Esc`. See: it closes.
- [ ] L6. A pause under a lock. Intended change 5. Do not add `--resumeOnFocusGained`.
      `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic --pauseOnFocusLost`
  1. Wait for control. Keep another window open on the same desktop.
  2. Hold the right arrow. When the camera starts its move to `basic_right`, press `Alt`+`Tab`. See: the pause sound. The
     transition lasts about 1.35 seconds, so this can take more than one try.
  3. Press `Alt`+`Tab` to go back to the game. See: the grid, and the game does not resume.
  4. Press `Enter`. If the grid closes, the focus left outside the lock: hold the arrow key toward the other room and do
     step 2 again from there.
  5. If the grid stays: press each arrow key. See: the cursor does not move. This is the state of the check.
  6. Press `Esc`. See: the pause sound, the grid closes, and the camera finishes its move.
  7. See: Mega is in the next room. Hold an arrow key and press the jump key (default `K`). See: Mega runs and jumps.

### Experimental: the pause screen

Keyboard only. P1 to P4 use the same command, and each check starts with a new launch.

- [ ] P1. Weapon stays the same. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Wait for control. Pause. Press an arrow key to move the cursor to `FRIGID_SHOT` or `MOON_SCYTHES`.
  2. Press `Esc`. See: the pause sound, the grid closes, and the level runs.
  3. Press the shoot key (default `J`). See: the normal buster shot.
  4. Pause. Move the cursor to the same weapon. Press `Enter`. See: the grid closes. Press the shoot key. See: that
     weapon fires. This row acts as today.
- [ ] P2. Other cursor rows. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Wait for control. Pause. Move the cursor to the gear. Press `Esc`. See: the grid closes, and the panel does not open.
  2. Pause. Move the cursor to `EXIT`. Press `Esc`. See: the grid closes, and the level runs. No fade, and no save screen.
- [ ] P3. The panel. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Wait for control. Pause. Select the gear. See: the panel. Select `BACK`. See: the grid. Note the sound of this row.
  2. Select the gear. Move the cursor down to `SFX VOLUME`. Press `Esc`. See: the grid, with the same sound as in step 1.
     The level does not resume.
  3. Press `Esc`. See: the grid closes, and the level runs.
- [ ] P4. The slide. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Wait for control. Pause. Select the gear, and press `Esc` at once, while the panel slides in. The slide lasts 0.5
     seconds. See: the panel slides in and stays.
  2. Press `Esc`, and press it again at once. See: the first press slides the panel out. The second press does not close
     the grid: the grid stays.
  3. Press `Esc`. See: the grid closes.
- [ ] P5. The health fill. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim --healthTanks all`
  1. Wait for control. Let shots hit Mega 4 times, so that the health bar is well below full.
  2. Pause. Move the cursor to a health tank. Press `Enter`. See: the health bar fills.
  3. Press `Esc` 3 times during the fill. See: the grid stays, and the fill goes on.
  4. When the fill ends, press `Esc`. See: the grid closes.
- [ ] P6. The exit fade. `./run-game-alpha.sh --level GAUNTLET_1 --room room1`
  1. Wait for control. Pause. Select `EXIT`. See: the screen fades out.
  2. Press `Esc` 3 times during the fade. The fade lasts 1 second. See: no resume. The save screen opens, with the rows
     `CONTINUE`, `EXTRAS MENU` and `MAIN MENU`.
  3. Press `Esc`. See: the save screen stays. Select `MAIN MENU`. See: the main menu.
- [ ] P7. Back through the bindings screen. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Wait for control. Press `Esc` while a shot is in flight. Note Mega's position, the health bar, the shot and the
     enemies.
  2. Select the gear. Select `KEYBOARD SETTINGS`. See: the level music plays with no break.
  3. Move the cursor to a row that is not `BACK`. Press `Esc`. See: the panel, in the right place. No `READY`, and no level
     start.
  4. Press `Esc`. See: the grid, with the same bars.
  5. Press `Esc`. See: the pause sound, 1 time. All are where they were at step 1. The shot goes on from that place, and
     the music did not restart.

### Experimental: the menus

Keyboard only.

- [ ] M1. Main menu root. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`
  1. On `START NEW GAME`, press `Esc`. See: no change, and no sound.
  2. Move the cursor up to `EXIT`. Press `Esc`. See: no change. The game does not quit.
- [ ] M2. Main menu panel. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`
  1. Select `SETTINGS`. See: the panel. Select `BACK`. See: the main menu, with the cursor on `SETTINGS`. Note the sound.
  2. Select `SETTINGS`. Move the cursor down to `MUSIC VOLUME`. Press `Esc`. See: the same result and the same sound as
     step 1.
  3. Press `Esc`. See: no change, as in M1.
- [ ] M3. Difficulty. `./run-game-alpha.sh --screen SELECT_DIFFICULTY_SCREEN`
  - The cursor is on the first row. Press `Esc`. See: the main menu, and no level starts.
- [ ] M4. Extras. `./run-game-alpha.sh --screen EXTRAS_SCREEN`
  - Move the cursor down to a level row. Press `Esc`. See: the select sound and the main menu. No level starts.
- [ ] M5. Save screen. `./run-game-alpha.sh --screen SAVE_GAME_SCREEN`
  1. On `SAVE`, press `Esc`. See: no change. Do not select `SAVE`.
  2. Move the cursor to `CONTINUE`. Press `Esc`. See: no change. Move it to `MAIN MENU`. Press `Esc`. See: no change.
  3. Select `MAIN MENU`. See: the main menu.
- [ ] M6. Level select, new route. `./run-game-alpha.sh --screen LEVEL_SELECT_SCREEN`
  1. Press `Esc`. See: the save screen, with the rows `SAVE`, `CONTINUE` and `MAIN MENU`, and its music. No crash.
  2. Select `MAIN MENU`. See: the main menu.
- [ ] M7. Level select, during the outro. `./run-game-alpha.sh --screen LEVEL_SELECT_SCREEN`
  - Move the cursor to a boss face. Press `Enter`. During the 1 second outro, press `Esc` 3 times. See: no save screen. The
    boss intro screen opens. Close the game.
- [ ] M8. Level select with an extra level as the last level (open question 4).
      `./run-game-alpha.sh --level GAUNTLET_1 --screen LEVEL_SELECT_SCREEN`
  - Press `Esc`. See: the save screen, with the rows `CONTINUE`, `EXTRAS MENU` and `MAIN MENU`. No crash. Close the game.

### Experimental: the bindings screens

C1 to C2 are keyboard only. C3 to C5 are `[pad]`.

- [ ] C1. From the main menu path. `./run-game-alpha.sh --screen KEYBOARD_SETTINGS_SCREEN`
  - Move the cursor down to a row that is not `BACK`. Press `Esc`. See: the main menu, and it works.
- [ ] C2. `Esc` in a keyboard capture. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Wait for control. Pause. Select the gear. Select `KEYBOARD SETTINGS`. Note the key that row `A` shows. Select row `A`.
     See: the hint.
  2. Press `Esc`. See: the error sound, "Refused: reserved key" in the hint, and the hint stays. The screen does not leave.
  3. Hold `Esc`, and release it after 2 seconds. See: the hint stays on the press and on the release.
  4. Press the key that you noted. See: the hint closes, and the row shows the same key.
  5. Wait 1 second. Press `Esc`. See: the panel. Press `Esc`. See: the grid. Press `Esc`. See: the level runs.
- [ ] C3. `[pad]` Gamepad screen, main menu path. `./run-game-alpha.sh --screen CONTROLLER_SETTINGS_SCREEN`
  - Move the cursor down to a row that is not `BACK`. Press `Esc`. See: the main menu.
- [ ] C4. `[pad]` Gamepad screen over pause. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Wait for control. Pause. Select the gear. Select `CONTROLLER SETTINGS`. See: the level music plays with no break.
  2. Press `Esc`. See: the panel. Press `Esc`. See: the grid. Press `Esc`. See: the level is the same, and it runs.
- [ ] C5. `[pad]` `Esc` in a gamepad capture. `./run-game-alpha.sh --screen CONTROLLER_SETTINGS_SCREEN`
  1. Select row `A`. See: the hint.
  2. Press `Esc` 3 times. See: the hint stays, with no error sound and no reason line, because only pad input ends this
     capture. The screen does not leave.
  3. Do not press a pad button: a pad capture saves. Unplug the USB adapter. See: the error sound and the main menu.
  4. Plug in the adapter. Close the game.

### Experimental: the diagnostics label

- [ ] D1. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement --diagnostics`
  1. Wait for control. Run for 5 seconds. Close the game.
  2. `grep -c 'inputFrames' assets/*-diagnostics.txt` prints a number above `0`.
  3. `grep -c 'controllerPoller' assets/*-diagnostics.txt` prints `0`.
  4. `utils/diagnostics/run.sh assets/*-diagnostics.txt` runs with no error.

### Control: menus and pause

V1 and V4 are keyboard only. V2 and V3 are `[pad]`.

- [ ] V1. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`
  1. Hold the down arrow. See: the cursor does not move. Release it. See: the cursor moves 1 row, with the cursor sound.
  2. Press the down key of the bindings (default `S`). See: the same, on the release. The up arrow and the up key (default
     `W`) move it up.
  3. Move the cursor to `SETTINGS`. Hold `Enter`. See: nothing. Release it. See: the panel. Select `BACK`.
  4. Move the cursor to `SETTINGS`. Press the key of `A` (default `K`). See: the panel opens on the release. Select `BACK`.
  5. On `SETTINGS`, press the key of `B` (default `J`). See: nothing.
- [ ] V2. `[pad]` `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`
  - Use the PS3 pad. Do V1 with the d-pad, `START`, `A` and `B`. See: the same results. `B` does nothing, and no pad button
    is a back action.
- [ ] V3. `[pad]` `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`
  - Use the NES-style pad. Move the cursor with the d-pad to `SETTINGS`. Press `A`. See: the panel, on the release. Move
    the cursor to `BACK`. Press `START`. See: the main menu. The pad gives the same input as it does today.
- [ ] V4. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Wait for control. Hold `Enter`. See: nothing. Release it. See: the grid, and no weapon row acts.
  2. Select a weapon. See: the grid closes, and it does not open again.
  3. Press numpad `Enter`. See: the grid opens, and it stays. Select a weapon.

### Control: skip screens

Keyboard only, except that W1 and W2 in the next group also pass through skip screens.

- [ ] K1. `./run-game-alpha.sh --screen SIMPLE_INIT_GAME_SCREEN`
  1. Press `Esc` 3 times. See: no change. Hold `Enter`. See: nothing. Release it. See: the logo screen.
  2. Press `Esc` 3 times. See: the logo stays. Hold `Enter`. See: nothing. Release it. See: the opening screen.
- [ ] K2. `./run-game-alpha.sh --screen OPENING_SCREEN`
  - Press `Esc` 3 times. See: no change. Hold `Enter`. See: nothing. Release it. See: the select sound, the music fades, and
    the main menu opens.
- [ ] K3. `./run-game-alpha.sh --screen ENDING_SCREEN`
  - Press `Esc` 3 times. See: no change. Press `Enter`. See: the credits open.
- [ ] K4. `./run-game-alpha.sh --screen CREDITS_SCREEN`
  - Press `Esc` 3 times. See: no change. Press `Enter`. See: the fade starts on the press, and the main menu opens. Keep the
    press short: a long hold has a known open case (`docs/SCOPE.md`, bug candidate 3).
- [ ] K5. `./run-game-alpha.sh --screen CREDITS_SCREEN`
  - Press the key of `SELECT` (default `L`). See: the same as K4.
- [ ] K6. `./run-game-alpha.sh --screen GAME_OVER_SCREEN`
  - Press `Esc` 3 times. See: no change. Press `Enter`. See: the save screen. Do not select `SAVE`. Close the game.
- [ ] K7. `./run-game-alpha.sh --level TIMBER_WOMAN --screen ROBOT_MASTER_INTRO_SCREEN`
  - Press `Esc` 3 times during the intro. See: the intro goes on, and the level starts as today. Close the game.

### Control: gameplay and a real stage

G1 is keyboard only. G2 is `[pad]`. W1 and W2 are keyboard only.

- [ ] G1. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  - Run, stop, jump (short and full), wall slide, air dash, slide, climb the ladder, swim, shoot, charge and fire. During
    these moves, press `Esc` two times. See: all moves are as today, and the two presses only pause and resume.
- [ ] G2. `[pad]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Wait for control. Hold right on the d-pad and unplug the USB adapter. See: Mega stops, the game does not pause, and the
     keyboard works.
  2. Plug in the adapter. See: the pads give input.
- [ ] W1. Boss, get-weapon screen. The command starts in the room with the boss `Gate`.
      `./run-game-alpha.sh --level TIMBER_WOMAN --room room14 --weapons all`
  1. Wait for control. Go right through the `Gate` and fight.
  2. When the boss has little health left, hold `Enter`. Keep it down through the last hit and until the boss has gone. See:
     no pause sound and no grid.
  3. Release `Enter`. See: no pause sound and no grid. Press `Esc` 2 times. See: no grid. The end of the level goes on.
  4. On the get-weapon screen, wait for the blinking text. Press `Esc` 3 times. See: no change.
  5. Press the key of `B` (default `J`). See: the save screen. Do not select `SAVE`. Close the game.
- [ ] W2. Level select, real route. The command starts in the room with the boss `Gate`.
      `./run-game-alpha.sh --level INTRO_STAGE --room room12`
  1. Wait for control. Go right through the `Gate`, beat the boss, and wait for the save screen.
  2. Select `CONTINUE`. See: the level select screen.
  3. Press `Esc`. See: the save screen, with the rows `SAVE`, `CONTINUE` and `MAIN MENU`.
  4. Select `CONTINUE`. See: the level select screen.
  5. Press `Esc`. Select `MAIN MENU`. See: the main menu. Do not select `SAVE`. Close the game.

### Last step

- [ ] X1. Close the game. `find ~/.prefs -type f -exec sha1sum {} + 2>/dev/null | sort | diff - /tmp/031f-prefs.sha1`
      prints nothing. Then `rm /tmp/031f-prefs.sha1`.

## Open questions

Answered (owner, 2026-11-01, verbatim): "yes", as recommended: questions 2 to 6 take their recommendations.

1. **The 1 frame that the guard at `MLS:876` does not cover.** Answered (owner, 2026-11-01, verbatim): "Go ahead and fold
   the bug fix in like you recommended". The queued pause tests the lock again (`docs/SCOPE.md`, section 4). T32, T33, R6
   and A13 check it. The number stays, so that the check text does not change.
2. **A back and a confirm in one frame.** `docs/SCOPE.md` says that this contract fixes the order.
   - a) The confirm wins: if `selectionRequested()` is `true`, `onBack` does not run in that frame, also if `onSelect` gives
     `false`. One frame gives one action. The cost is a lost `Esc` press in that 1 frame. A navigation in that frame still
     runs first (T7, T8, T9).
   - b) The back wins. A confirm release is then lost, and today's confirm path changes.
   - c) The loop order with both: the confirm, then the back if the confirm gave `false`. `SETTINGS` then opens and closes in
     one frame, except where a slide test stops it.
   - Recommendation: a).
3. **2 test seams.** `docs/SCOPE.md` leaves the pause seam to the plan and has no test of `MegaMenuScreen`.
   `MegaLevelScreenEventsTest` shows that a screen can take a relaxed mock of the game in a test. The frame test of
   trigger 2 needs the pause seam in all cases: T19 to T26, T32 and T33 call one pure function for the decision at `:876`,
   and one small function for the queued pause, which takes the queue, the lock read and the pause call. `MLS` calls both.
   This adds 2 lines to the table of new APIs. The question is the second seam.
   - a) Also one new test file `MegaMenuScreenTest`, with T27 to T31. No new production code.
   - b) No `MegaMenuScreenTest`. T27 to T31 go, and only V1 to V3 and R3 cover the menu rules. The minimum of A3 falls by 5.
   - Recommendation: a).
4. **`LevelSelectScreenV2` back after an extra level.** `SAVE_GAME_SCREEN` reads `isInExtraLevel` in `show()` and in the
   `MAIN MENU` row, not only on `CONTINUE` as `docs/SCOPE.md` says. `currentLevel` stays on the extra level after the player
   leaves it, and `loadSavedState()` does not clear it. So this route is possible: an extra level, `MAIN MENU`, `LOAD SAVE
   FILE`, level select, `Esc`. The save screen then has the extras rows. `CONTINUE` restarts the extra level, and `MAIN MENU`
   calls `resetPlayerProgress()`. That removes the loaded progress from memory. The save file does not change. M8 shows
   this state.
   - a) Accept it, with a `TODO` at `LevelSelectScreenV2.onBack` that names this edge case, and a bug candidate in
     `docs/TODO.md`.
   - b) Clear `currentLevel` in `loadSavedState()`. This is a change outside the scope, and `LPS` and `SaveGameScreen` read
     that value.
   - Recommendation: a).
5. **Text fixes in `docs/SCOPE.md`.** No fix changes an owner's answer. This session did not change the file.
   - "Affected areas and risks" still says "`Esc` during a lock: [...] The guard prevents this." and "only
     `--pauseOnFocusLost` or `--pauseOnMinimized`". The listener lock of intended change 6 is one more way to that state.
   - "`SAVE_GAME_SCREEN` reads `isInExtraLevel` and `currentLevel` only on `CONTINUE`": see question 4.
   - "Overlap with other branches": `051e` has merged into `mega-refactor-051-master` (`d8f2a4ec0`), and `084g` is now active
     in its worktree. Both facts came after the scan. The parent also changes `LPS` at the `EXIT` row: `EXIT` then works in a
     `TEST_LEVEL`. This contract does not depend on that: P2 presses `Esc` on `EXIT`, and P6 uses an extra level.
   - "the contract chooses the room for the lock case": `REFACTOR_TEST_051 --room basic`, not a `REFACTOR_TEST_031` room.
   - Recommendation: correct these lines when this contract is approved, and write it in the process log.
6. **The issue file of intended change 6.** The rules allow an issue file only on `master`.
   - a) File `docs/issues/2026-11-01-start-release-at-lock-start-pauses.md` on `master` before the branch starts, with the 2
     triggers, the origin from `docs/SCOPE.md` and the listener case.
   - b) File it at the merge. Then the contract names a file that does not exist during the branch.
   - Recommendation: a).
