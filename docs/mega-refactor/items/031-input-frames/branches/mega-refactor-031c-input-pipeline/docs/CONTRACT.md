# Contract: mega-refactor-031c-input-pipeline

The gameplay that this branch must not change, and how to check it. This was written ahead of time on `master`,
before the branch existed.

- Status: draft
- Approved: no

This branch replaces the poller with device adapters, held masks and an engine edge builder. Every button must feel
the same on the keyboard and on a gamepad, alone and together: the press and release edges, the lock during room
transitions, spawns, the boss intro and the boss defeat, the menus and skip screens, the bindings screen, and gamepad
connect and disconnect during play.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 481 tests on `master`, plus the new tests.
- [ ] `./gradlew core:test` passes: the 161 tests on `master`, minus the removed poller tests, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (the count on a clean build of `master`, measured at the baseline step).
- [ ] `contact-rule-selection.txt` is unchanged.
- [ ] Temporary contract suite (on the branch only; deleted in the last commit before the merge): on seeded scripts of
      key presses, gamepad button presses, gamepad connect and disconnect, `loadPrefsOnConnect` and lock toggles, the
      slot-0 frame from the new pipeline equals the frame from the frozen `ControllerPoller`, `MegaControllerPoller`
      and `ControllerButton.isPressed` on every tick. Named cases: hold then lock, unlock while held, press and lock in
      one tick, the keyboard holds A and the gamepad then presses A, gamepad connect while a button is held, gamepad
      disconnect while a button is held.
- [ ] Edge builder tests: each status transition, the lock cases, the default frame before the first tick.
- [ ] Adapter tests: the keyboard reads the bound codes; the gamepad adapter's connect, disconnect, prefs and mapping
      cases (from `MegaControllerPollerTest`).
- [ ] Facade tests: each query that the menus and skip screens use gives the slot-0 frame's answer.
- [ ] Code checks (grep): no `Gdx.input` and no `Controllers` in `engine/src/main`; no `ControllerPoller`,
      `MegaControllerPoller`, `IControllerButton`, `alternateActuators` or `enabled` on `ControllerButton`; the frozen
      copies and the temporary suite are gone at the merge.
- [ ] The slot-0 frame is built only in `MegamanMaverickGame.render`, at the old poll point.

## Manual playthrough

The owner plays these once, after the implementation, from the worktree. Play each gameplay check with the keyboard,
then with a gamepad, unless it says otherwise.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: run, stop, hold LEFT and RIGHT together; jump
  (short and full), wall slide and wall jump, air dash, crouch and ground slide, climb, swim; shoot, charge and release
  B; SELECT changes the weapon; START opens pause and the pause menu works. Then hold a direction on the keyboard and
  press jump on the gamepad, and the other way around: both work together, as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement` (gamepad): unplug the gamepad while you hold a
  direction: Mega stops, the keyboard keeps working, and the game does not pause. Plug it back in: it works again.
- `./run-game-alpha.sh --level INTRO_STAGE --room room1`: hold RIGHT through a room transition: Mega does not run during
  the transition and runs again after it. Press UP on a lamp post top: Mega falls through it.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: go through the boss gate while you hold B to charge: the
  controls lock during the boss intro and come back after it. Hold B to charge when the boss is defeated: the charged
  shot fires as the controls lock.
- Die once in any room while you hold a direction: after the respawn the controls come back, as before.
- `./run-game-alpha.sh`: the start screen and the logo skip with START; the main menu moves with the d-pad and confirms
  with START and A, with the keyboard and with a gamepad.
- From the main menu, keyboard settings: rebind one button to a new key: the new key works at once in the menu and in a
  level. Gamepad settings: rebind one button; then "LOAD SAVED SETTINGS", unplug and plug the gamepad: the saved code is
  used, as before. (Reset the bindings after the check.)
- `./run-game-alpha.sh --screen OPENING_SCREEN`: START skips the story. `./run-game-alpha.sh --screen CREDITS_SCREEN`:
  a press of an exit button ends the credits early. `./run-game-alpha.sh --screen GAME_OVER_SCREEN`: START continues.

## Behaviors to check by review

- `ControllerSystem.on` is switched as before; only `controllerPoller.on` moves to the edge builder.
- The `TURN_CONTROLLER_*` submitters do not change.
- The bindings screen changes only where it sets `loadPrefsOnConnect`.

## Intended changes

None. The per-button `enabled` flag goes; no production code sets it to `false`.
