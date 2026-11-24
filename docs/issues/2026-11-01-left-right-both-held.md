---
status: closed
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-084g-movement-state play test (2026-11-01)
related-items: [031, 084]
---

# With left and right both held, Mega runs in place and can face a wall during a wall slide

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/components/ControllerComponent.kt`: the guard in `run(delta, side)`, and the left and right actuators
- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/behaviors/WallSlideBehavior.kt:26-30`: the raw button reads; the behavior never writes `facing`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/sprites/MegamanAnimationKeySupplier.kt:95`: the run animation

The owner saw both problems in play. The causes are from a read of the code at `87d512886`. The old `runLeft` and `runRight`, before `084g`, have the same logic, so `084g` did not cause them.

## What happens

**Run in place.** Hold `RIGHT`, then add `LEFT`. Both sides take the guard. `running` stays true, and no impulse applies, so the run animation plays with no movement.

**Facing during a wall slide.** In play, only `run` writes `facing`. At a right wall, hold `RIGHT`, add `LEFT`, and jump, with both held. The wall slide starts on the raw read, and the guard skips the facing write, so Mega faces the wall until a release. A wall jump then pushes into the wall (`JumpBehavior.kt:68`). The result depends on the earlier press, so it happens only sometimes.

2 cases last 1 frame:

- The first frame of each wall slide: the Controller system runs before the Behaviors system.
- A release of the wall button with both held: the wall slide is still active, so the facing flips toward the wall.

## What should happen

Only the last pressed of `LEFT` and `RIGHT` counts for the run, the wall slide and the facing.

## How to reproduce

`./run-mega-man-maverick-alpha.sh --level REFACTOR_TEST_029 --room movement`: run, press the other button too, and keep both held. For the wall case, do the sequence above at a wall.

## Notes

Owner decisions (2026-11-01). Fix branch: `fix/left-right-last-pressed`, off `master`.

- The rule is a tie value in `RunComponent`, set in the 2 `onJustPressed` callbacks. It applies only when both buttons are pressed, so a button held through a respawn still works.
- `InputFrame` and `PlayerInputs` do not change: menus read the same frame, and a masked button gives `SpiderWeb.kt` a false release. `PipePortal`, `SpringBouncer`, `SpiderWeb` and `MegaMenuScreen` keep their raw reads.
- A tap away from a wall leaves the wall. This is necessary: otherwise `run` turns Mega toward the wall.
- For a press of both buttons in the same frame, `RIGHT` wins, as the actuator order gives.
- The fix sets `facing` in the wall slide `init`, for the first frame.
- A tap of the other button during a run reverses Mega. Air dash and ground slide facing follow a tap.
- The fix also closes `2026-11-01-rodent-claws-run-facing.md`.

Accepted edge case (`TODO` in `RunComponent`): a press in a frame when the `ControllerSystem` does not process Mega (a pause, or Mega not spawned) does not set the tie value. With both held, the older press wins until the next press.

`MegamanRunTest` pins the old rule in 3 blocks, which the fix changes. New tests: the tie, the release handover, a tap away, a held spawn, and the web presses.

Play checks: `--level REFACTOR_TEST_029 --room movement`, also with `--weapons RODENT_CLAWS`, and `--level REFACTOR_TEST_029 --room gravity_switch`.

## Resolution

Fixed by `fix/left-right-last-pressed` (squash `b094f8423`). `RunComponent.lastPressedSide` holds the tie value, and the run and the wall slide read `isRunSidePressed`. `RunComponent.reset()` sets the tie to `RIGHT`, so with both held through a respawn, `RIGHT` wins. A tap does not stop Mega at once: friction and the run impulse turn him, so he skids. With `LEFT` held and `RIGHT` just pressed, `LEFT` still acts for that one frame. The owner's play test passes (2026-11-01).
