# Contract: mega-refactor-031b-engine-input

The gameplay that this branch must not change, and how to check it. This was written ahead of time on `master`,
before the branch existed.

- Status: draft
- Approved: no

This branch moves `InputFrame` and `InputSource` to the engine (with `InputButton`), gives each controlled entity its
own input source on `ControllerComponent`, and makes `ControllerSystem` dispatch from that source in button-index
order. The poller stays the only source of input. Every control must act as before: Megaman's run, shots, weapon
switch and every other movement, the special entities that read a player's buttons, pause, and the behavior of the
controls through the control lock (room transitions, spawn, boss intro, boss defeat). The one intended change is the
fixed actuator order (see "Intended changes").

## Automated checks

- [ ] `./gradlew engine:test` passes: the 470 tests on `master`, plus the new and changed tests.
- [ ] `./gradlew core:test` passes: the 157 tests on `master`, plus the new and changed tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (the count on a clean build of `master`, measured at the baseline step).
- [ ] `contact-rule-selection.txt` is unchanged.
- [ ] Engine `InputFrame` tests: each status reads back the same at each index from 0 to 31 (index 31 uses the high
      bits of the `Long`), with the other buttons set to other statuses; the default frame gives `RELEASED` and
      `false` for every query at every index; the four queries keep their meaning (`isPressed` includes
      `JUST_PRESSED`).
- [ ] Button-set check tests: a duplicate `index`, an `index` of `-1` and an `index` of `32` are refused; a valid set
      passes.
- [ ] `MegaControllerButton`: each `index` equals its `ordinal` (UP 0 … SELECT 7), so the bit positions are the same
      as on `master`; the set passes the button-set check.
- [ ] `ControllerSystemTest`: each status calls the matching callback (with `delta` for the two "continued" ones), and
      only that one; an entity with no source is skipped; the actuators are called in index order; the source is read
      once per entity per update.
- [ ] The core equivalence suite (`InputFrameTest`, adapted) passes: on every tick, every query on the frame built
      from the poller equals the same query on the poller, for every button, with the named lock cases.
- [ ] `MegaPlayersTest`: building the view sets the entity's source to the slot source; `MegaPlayer.input` and the
      entity's source give the same frame.
- [ ] Code checks (grep): `ControllerSystem`, `ControllerComponent`, `IButtonActuator` and `ButtonActuator` have no
      `IControllerPoller`; `putActuator` and `removeActuator` do not exist; `MegamanInput.kt` does not use
      `asPlayer`; `InputFrame` and `InputSource` are not in core `input/`.
- [ ] No boxing: `javap` shows no `InputFrame.box-impl` in `ControllerSystem`, `MegaPlayer.getInput` and
      `Megaman`'s input accessor.
- [ ] The slot-0 frame is built only at the poll point (`MegamanMaverickGame.render`, right after
      `controllerPoller.run()`).

## Manual playthrough

The owner plays these once, after the implementation, from the worktree (no baseline play-through; see "Test maps" in
`docs/mega-refactor/CLAUDE.md`). No new test rooms: the rooms from `029` and `031a` cover the checks.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: run left and right, stop (momentum as before), hold
  LEFT and RIGHT together (Mega stops). Jump, wall slide and wall jump, air dash, crouch and ground slide, climb the
  ladder, swim. Shoot; hold B to charge and release; turn while B is held and release. With more than one weapon,
  SELECT (set to `NEXT WEAPON`) changes the weapon. START opens pause.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room spring_bouncer`: holding the bouncer's direction gives the
  stronger bounce, as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room pipe_portal`: with DOWN held on the `up` pipe, Mega enters and
  comes out of the linked pipe; the way back with its direction.
- `./run-game-alpha.sh --level INTRO_STAGE --room room1`: hold RIGHT through a room transition: Mega does not run
  during the transition and runs again after it, as before. Press UP on a lamp post top: Mega falls through it.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: go through the boss gate while holding B to charge: the
  controls lock during the boss intro and come back after it, as before. Hold B to charge when the boss is defeated:
  the charged shot fires as the controls lock (the lock keeps `ControllerSystem` on for this event), as before.
- Die once in any room while holding a direction: on respawn, the controls come back after the spawn, as before.

## Behaviors to check by review

- The actuator callbacks (B, LEFT, RIGHT, SELECT) keep their bodies; only the `poller` parameter goes. A review of the
  diff checks this.
- The status-to-callback rule in `ControllerSystem` is the same: `JUST_PRESSED` → `onJustPressed`, `PRESSED` →
  `onPressContinued`, `JUST_RELEASED` → `onJustReleased`, `RELEASED` → `onReleaseContinued`.
- `ControllerSystem.on` and `controllerPoller.on` are switched as before (`MegamanMaverickGame.onEvent`, the level
  screen); this branch does not change them.

## Intended changes

- The actuator order is fixed: LEFT, RIGHT, B, SELECT (index order). On `master` it is the hash order of an
  `ObjectMap` with enum keys, which can differ between runs. Each run on `master` already uses one of the possible
  orders, so no new behavior appears. It can matter only when other code changed `facing` after the controller ran in
  the previous frame (see `docs/SCOPE.md`, "Where the order can matter"). Not seen in the game.
