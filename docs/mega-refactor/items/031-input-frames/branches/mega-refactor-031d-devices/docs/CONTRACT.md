# Contract: mega-refactor-031d-devices

The gameplay that this branch must not change, the gameplay that it changes on purpose, and how to check both. This
was written ahead of time on `master` (`742abf63f`), before the branch existed. It follows the approved
`docs/SCOPE.md` (2026-10-30).

- Status: approved
- Approved: 2026-10-30

This branch changes the two device adapters of slot 0 (`KeyboardInput`, `GamepadInput`), one prefs function
(`getPreferredControllerCode`) and one capture listener on the gamepad bindings screen. The keyboard gets fixed
aliases, the gamepad adapter reads every connected pad, the left stick and the triggers. Everything else about input
must feel the same: the bound keys and buttons, the press and release edges, the lock, the menus and skip screens,
the bindings screens, and gamepad connect and disconnect in single player.

## Automated checks

Run from the worktree, on a clean build:

```
JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar 2>&1 | tee build.log
```

Read the test counts from `engine/build/test-results/test/` and `core/build/test-results/test/`. Baseline on
`master` `742abf63f`: the build passes; engine 575 tests, core 272 tests, 0 failures; 81 unique warnings.

### Build and counts

- [ ] A1. The command above passes.
- [ ] A2. Engine: 575 tests, 0 failures. The engine does not change, so the count does not change.
- [ ] A3. Core: 272 tests plus the new tests, 0 failures. No existing test is removed. The 3 `KeyboardInputTest`
      cases and the 8 `GamepadInputTest` cases stay, with the same meaning. The `GamepadInputTest` cases move to the
      pad-list seam.
- [ ] A4. Warnings: the same set of 81 unique lines as on `master`, no new line. Count with:
      `grep '^w:' build.log | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u | wc -l`. Compare the sorted lists, not only
      the counts.

### Code checks (from the worktree)

- [ ] A5. `git diff master --stat -- engine/ core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt`
      gives no output. This also proves: `InputFrameBuilder` (the edge rule and the lock) does not change, the slot-0
      frame is still built only in `MegamanMaverickGame.render`, the `"controllerPoller"` diagnostics entry keeps its
      name, and the `TURN_CONTROLLER_OFF/ON` handling in `onEvent` does not change.
- [ ] A6. `git diff master --stat -- core/src/test/resources/contact-rule-selection.txt` gives no output.
- [ ] A7. `git diff master --name-only -- core/src/main lwjgl3/` lists only these 5 files:
      `input/KeyboardInput.kt`, `input/GamepadInput.kt`, `controllers/ControllerUtils.kt`,
      `controllers/ControllerUtilsExtensions.kt`, `screens/menus/ControllerSettingsScreen.kt`.
- [ ] A8. `grep -rn "fun isControllerKeyPressed(key: Int)" core/src/main` gives no output (the index-0 overload is
      gone).
- [ ] A9. `grep -rn "isControllerKeyPressed(" core/src/main` gives no call with one argument.
- [ ] A10. `grep -n "TODO" core/src/main/kotlin/com/megaman/maverick/game/input/KeyboardInput.kt` shows one `TODO`
      at the alias table. It names the edge case (a key that is bound and is also an alias gives both buttons) and
      `031e` (the refusal of reserved keys on the bindings screen).
- [ ] A11. No `TODO` is added to `ScreenController.kt`, and the file does not change (covered by A7).

### New tests (core, with mocks of `Gdx.input`, `Gdx.app`, `Controller`, `ControllerMapping` and the pad list)

`KeyboardInputTest`:

- [ ] T1. `Enter` gives `START`.
- [ ] T2. Numpad `Enter` gives `START`.
- [ ] T3. `START` rebound to another key: `Enter` still gives `START`, and the new key also gives `START`.
- [ ] T4. Each of the 4 arrow keys gives its d-pad button (`UP`, `DOWN`, `LEFT`, `RIGHT`).
- [ ] T5. `W` and the up arrow held together give one `UP` bit.
- [ ] T6. `Esc` gives no bit.

`KeyboardInput` into a real `InputFrameBuilder`:

- [ ] T7. Hold `W` for one tick, then also press the up arrow: `UP` stays `PRESSED`, with no second `JUST_PRESSED`.

`GamepadInputTest`:

- [ ] T8. Two pads: a bound button on the second pad sets its bit.
- [ ] T9. Two pads: buttons held on both pads OR into one mask; the same button on both pads gives one bit.
- [ ] T10. Index shift with `loadPrefsOnConnect` true: list `[A, B]`, then `[B]`: the codes reload from B's prefs.
- [ ] T11. Index shift with `loadPrefsOnConnect` false: list `[A, B]`, then `[B]`: the codes reload from the mapping.
- [ ] T12. Reconnect: list `[A]`, then `[]`, then `[A']` (a new object): the codes go to `null`, then reload.
- [ ] T13. The primary stays the same object over many ticks: no reload (today's "does not re-apply the mapping"
      case).
- [ ] T14. Stick X: `0.49` gives no bit, `0.5` gives `RIGHT`, `-0.49` gives no bit, `-0.5` gives `LEFT`.
- [ ] T15. Stick Y: `-0.5` gives `UP`, `0.5` gives `DOWN`, `0.49` and `-0.49` give no bit.
- [ ] T16. A diagonal (X `0.5`, Y `-0.5`) gives `RIGHT` and `UP`.
- [ ] T17. The stick ORs with a bound d-pad button (stick `RIGHT` and the d-pad `RIGHT` give one bit).
- [ ] T18. Triggers: a button bound to the left or the right trigger code is held when that axis is at `0.5`, and not
      held at `0.49`.
- [ ] T19. A trigger code never reaches `Controller.getButton`.
- [ ] T20. A pad with no bound codes (all `null`) still gives the stick bits.

`getPreferredControllerCode` (new test file or new cases, with a mock `Gdx.app` and `Preferences`):

- [ ] T21. A missing key gives `null`.
- [ ] T22. A saved key gives its value (also the value `0`).
- [ ] T23. `GamepadInput` with `loadPrefsOnConnect` true and a prefs file with no keys: the codes come from the
      mapping, not `0`.

The bindings screen has no tests. The trigger capture is checked by hand (G7).

## Performance check

Each `getButton` and `getAxis` call is a native call. Today: up to 8 per tick, on one pad. After: per connected pad,
up to 8 buttons, 2 stick axes and up to 2 trigger axes. Measure the cost of the `"controllerPoller"` root entry.

- [ ] P1. Make 4 captures in the same room, each with about the same play (stand, run, jump, open and close the pause
      menu once), and quit the game cleanly so that the file is flushed. `run-game-debug.sh` adds `--diagnostics`.
      The file is `assets/<epoch-millis>-diagnostics.txt` in the checkout that runs the game.
  1. `master`, no gamepad. From `cd /home/johnlavender/IdeaProjects/Megaman-Maverick`:
     `./run-game-debug.sh --level REFACTOR_TEST_029 --room movement`
  2. `master`, one gamepad: the same command.
  3. Branch, no gamepad. From `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-031d-devices`:
     `./run-game-debug.sh --level REFACTOR_TEST_029 --room movement`
  4. Branch, one gamepad: the same command.
  - If the owner has two pads, add a fifth capture on the branch with two pads.
  - The main checkout can have uncommitted owner edits (for example `Met.kt`). The `movement` room has no enemy, so
    they do not change the `"controllerPoller"` entry.

How to read the result:

- Run `utils/diagnostics/run.sh <capture file>` for each capture (the `diagnostics-analyzer` skill in
  `.claude/skills/` describes the tool). Open `summary.txt` in the output folder.
- In "Process Summary", read the `controllerPoller` row: mean, p50, p95, p99 and `avg%`. Write the 4 rows in the
  branch process log, side by side.
- No gamepad: the branch row must be at the `master` level. With no pad, neither version makes a native call.
- One gamepad: the branch row can be higher, because each tick adds up to 4 native calls (2 stick axes, and the
  trigger axes only for a button bound to a trigger). Write the difference in the log. The owner decides if it is
  acceptable.
- Check the end of each raw capture for a `=== Dropped N frames ===` line. A capture with dropped frames is still
  valid, but write the count.

## Intended changes

Each entry is a gameplay change from `docs/SCOPE.md` "Intended changes". The manual check that proves it is named in
brackets. Because the menus and skip screens read slot 0's frame, changes 1 to 4, 7 and 8 also work there.

1. `Enter` gives `START` on the keyboard, whatever key `START` is bound to. Today `Enter` gives `START` only while
   `START` is bound to `Enter`. [K2, K3]
2. Numpad `Enter` gives `START`. Today it gives nothing unless it is bound. [K2, K4, K5]
3. The 4 arrow keys give `UP`, `DOWN`, `LEFT`, `RIGHT`. Today they give nothing unless they are bound. [K2, K4]
4. Every connected gamepad gives input to slot 0. Today only the gamepad at index 0 does. [D1]
5. Index shift. Fixes issue `docs/issues/2026-10-25-gamepad-index-shift-keeps-old-codes.md`. When the first pad is
   unplugged and another pad stays, the remaining pad gets its own codes: its saved prefs if `loadPrefsOnConnect`,
   else the mapping. Today it keeps the codes of the old pad. [D2; T10, T11 if the owner has one pad]
6. Code 0. Fixes issue `docs/issues/2026-10-25-preferred-controller-code-zero.md`. With `loadPrefsOnConnect`, a pad
   model with no saved file gets the mapping. Today it gets code `0` (the bottom face button) on all 8 buttons. [G5]
7. The left stick of each pad gives the d-pad at `0.5` per axis. Today the stick does nothing. [G2, G3, G4]
8. A trigger can be bound on the gamepad bindings screen. It then acts as a button through the threshold `0.5`, in
   play and in the menus. Today no trigger can be bound. [G7]

## Must stay the same

- The keyboard with the default bindings (`WASD`, `K`, `J`, `Enter`, `L`): every move, the weapon change and the
  pause menu. [K1]
- One gamepad, with the default mapping and with saved bindings ("LOAD SAVED SETTINGS"): the same codes on connect,
  `null` codes on disconnect, the same reads of the bound buttons. [G1, G6]
- The keyboard and a gamepad together: each device works while the other holds a button. A button held on two devices
  gives one press. [G10, K2]
- The lock sequence (`TURN_CONTROLLER_OFF/ON`): a room transition, the player spawn, the boss intro, the boss defeat
  and the respawn lock and unlock the controls as today, also for input from an alias, the stick or a trigger. [K6,
  G9]
- The menus and the skip screens: `WASD`, `Enter`, the gamepad d-pad and the bound buttons work as today. [K4, K5]
- The keyboard bindings screen: a capture takes the next key, also an arrow key or `Enter` (the refusal is `031e`).
  "LOAD SAVED SETTINGS" and the select button action work as today. [K7]
- The gamepad bindings screen: the button capture, "LOAD SAVED SETTINGS" and the select button action work as today.
  The screen edits one code set, for the first pad in the list. [G6, D4]
- An unplugged gamepad in single player: no pause, and the keyboard (and any other pad) keeps working. A replug gives
  input again. [G8]
- `Esc` gives no button in play (as today, no code reads it). [K7]

Checked by review (no play needed):

- [ ] R1. Of the 13 index-0 call sites in the scope, only the 5 in `ControllerUtils.kt:11` and `GamepadInput` change.
      The 8 primary-pad call sites in `ControllerUtilsExtensions`, `ControllerSettingsScreen` and `GameSettingsPanel`
      do not change.
- [ ] R2. In `ControllerUtilsExtensions.kt`, only `getPreferredControllerCode` changes.
- [ ] R3. In `ControllerSettingsScreen.kt`, only the capture listener changes (an `axisMoved` override next to
      `buttonDown`).
- [ ] R4. The connect check stays before the read, in the same tick, as today.

## Manual play test

The owner plays these once, after the implementation, from the worktree. The owner knows today's behavior, so there
is no baseline play-through (except the diagnostics captures in P1).

Before the bindings checks (K3, K7, G5, G6, G7, D2, D4), back up the prefs files, and restore them after the checks:

- `cp -a ~/.prefs ~/.prefs.bak-031d` (before)
- `rm -rf ~/.prefs && mv ~/.prefs.bak-031d ~/.prefs` (after, with the game closed)

The bindings checks open the bindings screens from the pause menu of a level ("pause menu, settings, keyboard
settings" or "gamepad settings"). The new codes live in memory, so they reach play only in the same session. "BACK"
then starts the level again from its first room (known issue `2026-10-25-bindings-screen-restarts-level.md`, fixed in
`031e`). That is expected, and the check continues in that room.

All commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-031d-devices`

### Keyboard only (no gamepad connected)

- [ ] K1. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: with `WASD`, `K`, `J`, `Enter`, `L`: run,
      stop, hold `A` and `D` together, jump (short and full), wall slide and wall jump, air dash, crouch and ground
      slide, climb the ladder, swim; shoot, charge and release `J`; `L` changes the weapon; `Enter` opens the pause
      menu and the menu works. All as today.
- [ ] K2. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: the arrow keys move Mega, climb the ladder
      up and down, and crouch. `Enter` and numpad `Enter` each open and close the pause menu. One press of `Enter`
      opens the menu and it stays open (no double toggle). Hold `D`, then also press the right arrow, then release
      `D`: Mega keeps running with no stop. Hold `W` on the ladder, then also press the up arrow: Mega keeps climbing
      with no hitch.
- [ ] K3. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: in the keyboard settings (from the pause
      menu), bind `START` to `I`, then "BACK". In the level: `I` and `Enter` both open the pause menu. Restore the
      prefs after the check.
- [ ] K4. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`: the arrow keys and `WASD` move the cursor; `Enter`,
      numpad `Enter` and `K` confirm, as `Enter` and `K` do today.
- [ ] K5. Skip screens, with numpad `Enter`, then again with `Enter`:
  - `./run-game-alpha.sh --screen OPENING_SCREEN`: `START` skips the story.
  - `./run-game-alpha.sh --screen CREDITS_SCREEN`: a press of an exit button ends the credits early, as today.
  - `./run-game-alpha.sh --screen GAME_OVER_SCREEN`: `START` continues.
- [ ] K6. Lock, with the arrow keys:
  - `./run-game-alpha.sh --level INTRO_STAGE --room room1`: at the level start, hold the right arrow through the
    player spawn: Mega does not move until the spawn ends. Hold the right arrow through a room transition: Mega does
    not run during the transition and runs again after it.
  - `./run-game-alpha.sh --level INTRO_STAGE --room room12`: go through the boss gate while you hold the right arrow
    and `J`: the controls lock during the boss intro and come back after it. Hold `J` to charge when the boss is
    defeated: the charged shot fires as the controls lock, as today.
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: fall into the pit while you hold an arrow
    key: after the respawn the controls come back.
- [ ] K7. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: press `Esc` in play: nothing happens. Then
      in the keyboard settings (from the pause menu), select a button and press the up arrow: it is captured as the
      new key, as today. Restore the prefs after the check.

### One gamepad

Use a pad with a left stick and triggers for G2, G3, G4, G7 (see open question 1). Connect the pad before the launch
unless the check says otherwise.

- [ ] G1. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: with the default mapping (no "LOAD SAVED
      SETTINGS" in this session): the K1 moves with the d-pad and the bound buttons, as today.
- [ ] G2. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: push the left stick fully left and right:
      Mega runs. Push it up at the ladder: Mega climbs up (not down). Push it down: Mega crouches, and climbs down the
      ladder. A small push (less than half) does nothing. A diagonal (down and right) on the ground gives a ground
      slide, as the d-pad diagonal does. The right stick does nothing.
- [ ] G3. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: let the stick rest for some seconds, also
      after a hard push: Mega does not move (no drift).
- [ ] G4. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`: each push of the stick moves the cursor one step, as one
      d-pad press does. A held push behaves as a held d-pad. The cursor does not move with the stick at rest.
- [ ] G5. Code 0. Before the launch, move the prefs file of this pad model out of `~/.prefs/` (for example
      `mv ~/.prefs/"Megaman Maverick - Controller Preferences - PS3 Controller" ~/`). Then
      `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: in the gamepad settings (from the pause menu),
      choose "LOAD SAVED SETTINGS", then "BACK". Unplug the pad and plug it in again. Jump, shoot, `START` and the
      d-pad are on the mapping buttons, as in G1. Today all 8 buttons are on the bottom face button. Put the file back
      after the check.
- [ ] G6. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: in the gamepad settings (from the pause
      menu), rebind one button, choose "LOAD SAVED SETTINGS", then "BACK". Unplug and plug in the pad: the saved code
      is used, as today. Restore the prefs after the check.
- [ ] G7. Triggers. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: in the gamepad settings (from the
      pause menu), select `B` and pull the right trigger halfway: no capture. Pull it fully: `B` is bound to the right
      trigger. Do the same for `A` and the left trigger, then "BACK". In the level: the right trigger shoots, a held
      right trigger charges and a release fires the charged shot; the left trigger jumps (short and full). In the
      pause menu, the left trigger confirms as `A` does. Then "LOAD SAVED SETTINGS", unplug and plug in the pad: the
      triggers still work. Restore the prefs after the check.
- [ ] G8. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: hold a direction on the stick and unplug
      the pad: Mega stops, the game does not pause, and the keyboard keeps working. Do it again while you hold the
      d-pad. Plug the pad in again: the d-pad, the buttons and the stick work again.
- [ ] G9. `./run-game-alpha.sh --level INTRO_STAGE --room room12`: go through the boss gate while you hold the stick
      right and the shoot button: the controls lock during the boss intro and come back after it. If G7 bound a
      trigger, repeat with the held trigger.
- [ ] G10. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: hold the right arrow on the keyboard and
      press jump on the pad, then hold the stick right and press `K`: both work together. Hold the d-pad up on the
      ladder, then also push the stick up: Mega keeps climbing with no hitch.

### Two gamepads (only if the owner has them)

- [ ] D1. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: connect pad A, then pad B. The d-pad, the
      stick and the buttons of each pad move Mega. Hold right on A and jump on B: both work together. Hold the same
      button on both pads and release it on one: Mega still sees it held.
- [ ] D2. Index shift. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement` (no "LOAD SAVED SETTINGS" in
      this session): connect pad A only. In the gamepad settings (from the pause menu), rebind jump on A to another
      button, then "BACK". Connect pad B. Unplug A. On B, jump is on the mapping button again, not on the rebound
      button. Today B keeps A's codes. Restore the prefs after the check.
- [ ] D3. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: with A and B connected, unplug B: A keeps
      its codes and works. Unplug A: the keyboard keeps working, no pause. Plug in B: B works.
- [ ] D4. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: with A and B connected, open the gamepad
      settings (from the pause menu): it shows and edits the codes of A (the first pad). Rebind one button, then
      "BACK": the new code works on A and on B (one code set). Restore the prefs after the check.

## Open questions

1. **Which gamepads, and how many?** The prefs folder has saved files for a "PS3 Controller" and a "NEXT SNES
   Controller". A SNES-style pad has no stick and no analog triggers, so G2, G3, G4 and G7 need the PS3 pad (or
   another pad with a left stick and triggers). D1 to D4 need two pads at the same time.
   - Recommendation: play K1 to K7 and G1 to G10 with the PS3 pad. If the SNES pad also works, use it as pad B for D1
     to D4: two different models also show the known limit (one code set for every pad, `docs/SCOPE.md` risks). If
     the owner has only one pad, skip D1 to D4 and accept the unit tests T8 to T12 for changes 4 and 5. Write in the
     process log which checks were skipped and why.
   - Answer (owner, 2026-10-30, verbatim): "yes" (as recommended: the PS3 pad for K and G, the SNES pad as pad B).
2. **Pass rule for P1.** The scope asks to measure the `"controllerPoller"` entry, but gives no limit.
   - Recommendation: no fixed limit. The no-pad rows must be at the `master` level; the one-pad difference goes in the
     process log, and the owner accepts or rejects it at the merge review.
   - Answer (owner, 2026-10-30, verbatim): "yes" (as recommended).
