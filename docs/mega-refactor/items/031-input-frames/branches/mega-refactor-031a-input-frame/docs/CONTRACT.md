# Contract: mega-refactor-031a-input-frame

The gameplay that this branch must not change, and how to check it. This was written ahead of time on `master`,
before the branch existed.

- Status: approved
- Approved: 2026-10-25

This branch adds one `InputFrame` per player slot, copied from the poller right after it polls, and moves every
gameplay input read to it, with the same method and the same button. Every control must act as before: Megaman's
movements, shots and animations, the special entities that read a player's buttons, and pause.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 470 tests on `master`.
- [ ] `./gradlew core:test` passes: the 144 tests on `master`, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on a clean build of `master`).
- [ ] `contact-rule-selection.txt` is unchanged.
- [ ] `InputFrame` tests: each status of each button reads back the same; each query method gives the poller's
      answer for each status; the default frame gives `false` for every query.
- [ ] Equivalence suite: a real `ControllerPoller` with fake buttons and seeded scripts (presses, `on` toggles,
      `enabled` toggles). On every tick, every query on the built frame equals the same query on the poller, for
      every button. Named cases: hold then lock, unlock while held, press and lock in the same tick, the
      disabled-button flip.
- [ ] A player's `input` returns the frame of its slot.
- [ ] No gameplay code reads the poller: no `controllerPoller` under `entities/`, `world/` and `players/`, none in
      `MegaLevelScreen`'s pause check, and `ControllerComponent` reads no button through the actuator's `poller`
      parameter. The remaining readers are the frame builder and `ControllerSystem` in `MegamanMaverickGame`, the
      menus and screens, the `controllers` package, and the dead `ScreenController`.
- [ ] One for one: a list of every changed read (file, line, method, button) before and after shows the same method
      and the same button for each. `JetpackBehavior`'s `allMatch` becomes `getStatus(UP) == PRESSED` and
      `getStatus(A) == JUST_PRESSED`.
- [ ] The frame is built only at the poll point (`MegamanMaverickGame.render`, right after `controllerPoller.run()`).

## Manual playthrough

The owner plays these once, after the implementation, from the worktree (no baseline play-through; see "Test maps"
in `docs/mega-refactor/CLAUDE.md`). The plan adds `RefactorTest031.tmx` and the `LevelDefinition` entry
`REFACTOR_TEST_031`, built by a subagent with the `level-builder` skill, with these rooms (each with a player spawn):

- `spring_bouncer`: four `SpringBouncer`s, one for each direction (`up`, `down`, `left`, `right`).
- `pipe_portal`: two linked `PipePortal` pairs (an `up` pipe on the floor that leads to a `down` pipe, and the way
  back), as in `SMB3_1`.
- `ground_slide_blocker`: a `GroundSlideBlocker` across a passage.

A short launch of each room on the baseline code checks that it loads with no exception.

| Level and room | What to check |
|-------|---------------|
| `REFACTOR_TEST_029`, `movement` | Run, jump (short and full height). Wall slide and wall jump. Air dash on the ground and in the air. Crouch, and ground slide under the low gap. Climb the ladder up and down, jump off it, and get on it from above. In the water: swim up with A, and jump out. Shoot, hold B to charge and release, and the shooting animation while B is held. |
| `REFACTOR_TEST_031`, `spring_bouncer` | Touch each bouncer without holding a direction, then while holding the bouncer's direction: holding the direction gives the stronger bounce, as before. |
| `REFACTOR_TEST_031`, `pipe_portal` | Stand on the `up` pipe: nothing happens until DOWN is held; with DOWN held, Mega enters and comes out of the linked pipe. The same for the way back with its direction. |
| `REFACTOR_TEST_031`, `ground_slide_blocker` | The blocker stops Mega when he walks or jumps into it. It is off (Mega passes through it) while he ground slides, while he crouches, or while DOWN is held (`GroundSlideBlocker.kt:43-44`). |
| `INTRO_STAGE`, `room1` | Stand on a lamp post top and press UP: Mega falls through it. Without UP, he stands on it. Also a short pass of a real stage: run, jump, slide, shoot, and one room transition with a direction held. |
| Any room, pause | START opens pause. In the keyboard settings, set "SELECT ACTION" to `START`: SELECT opens pause. Set it to `AIR DASH`: SELECT air dashes. Set it to `NEXT WEAPON`: SELECT changes the weapon (if the loadout has more than one). (Leaving the settings screen in a level restarts the level; that is the known issue `2026-10-25-bindings-screen-restarts-level.md`, not part of this branch.) |

## Behaviors to check by review

- The jetpack and the Rush Jet air dash: the test levels do not give `RUSH_JET`, and the game gives it only after
  `MOON_MAN`. The one-for-one list covers them.
- `SpiderWeb`: nothing spawns it. The one-for-one list covers its read.
- Megaman's unfreeze on a B release (`Megaman.kt:833`) and shooting down (`MegamanWeaponsHandler.kt:674`): covered by
  the one-for-one list; the owner may check them if a room with a freezing enemy or a down-shooting weapon is at
  hand.
- The control lock and `ControllerSystem`'s dispatch do not change; the equivalence suite covers the frame through the
  lock cases.

## Intended changes

None.
