---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: owner (2026-10-29)
related-items: []
---

# Needle Spin does not bounce Megaman up while he rises from a jump and JUMP is held

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/handlers/MegamanWeaponsHandler.kt:1101-1112`:
  `spinNeedles()` sets Megaman's velocity to the bounce impulse (`vel.y = impulse` when `direction` is `UP`).
- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/behaviors/JumpBehavior.kt:32`: `evaluate`
  returns `false` when `currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting`.
- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/behaviors/JumpBehavior.kt:122-126`: `end` sets
  `body.physics.velocity.y = 0f` (or `x` when gravity is sideways).
- `engine/src/main/kotlin/com/maverick/game/engine/behaviors/AbstractBehaviorImpl.kt:57-64`: `update` calls `end()`
  in the frame where `evaluate` changes from `true` to `false`.
- `core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt:910-912`: the system order is
  `ControllerSystem`, then `BehaviorsSystem`, then `WorldSystem`.

## What happens

1. Megaman jumps and the player holds JUMP (`MegaControllerButton.A`). `JumpBehavior` stays active while A is held
   and Megaman moves up (`JumpBehavior.kt:35-43`).
2. The player fires Needle Spin. In `ControllerSystem`, `ControllerComponent.kt:93` calls `shoot()`.
   `fireWeapon` calls `spinNeedles()`, which sets `vel.y = impulse`. Then `Megaman.shoot()` resets
   `shootAnimTimer`, so `shooting` is `true` (`Megaman.kt:187-191`, `Megaman.kt:952-956`).
3. In the same frame, `BehaviorsSystem` evaluates `JumpBehavior`. Line 32 makes `evaluate` return `false`. The
   behavior was active, so `AbstractBehaviorImpl.update` calls `end()`, and `end()` sets `velocity.y = 0f`.
4. `WorldSystem` then moves the body with `velocity.y == 0`. The bounce impulse is lost, and Megaman starts to
   fall from the spin height.

When JUMP is not held, `JumpBehavior` is already inactive (it ended when A was released), so `end()` does not run
at the spin and the bounce works. The bounce also works while JUMP is held if Megaman already falls, because the
jump ends when the velocity is no longer upward.

## What should happen

Needle Spin bounces Megaman up by the same impulse whether JUMP is held or not.

## How to reproduce

```
./run-game-debug.sh --level INTRO_STAGE --room room1 --weapons NEEDLE_SPIN
```

1. Select Needle Spin.
2. Jump and keep JUMP held.
3. While Megaman still rises, fire Needle Spin. No bounce.
4. Do it again, but release JUMP before you fire. Megaman bounces up.

`room1` is in the live map `IntroStage_v3.tmx`, with player spawn `0`.

## Notes

- Likely cause, high confidence from reading the code: the order in "What happens" (impulse in
  `ControllerSystem`, then `JumpBehavior.end()` zeroes the velocity in `BehaviorsSystem` in the same frame). Not
  proved by a test.
- The parent branch `mega-refactor-016-master` (where the owner saw the bug) did not cause it. The same code is on
  `master`. On `mega-refactor-016-master` the lines that matter are the same: `JumpBehavior.kt:32` and `:124`,
  `MegamanWeaponsHandler.kt:1105`, and the system order (`MegamanMaverickGame.kt:916-918` on that branch). The diff
  in these files only changes `game` to `services` names.
- The code path is older than the refactor. Before `mega-refactor-029g` (commit `037a5f1a0`), the same check and
  the same `velocity.y = 0f` in `end` were in `entities/megaman/components/BehaviorsComponent.kt`. The impulse
  code comes from commit `ece3690c6` ("Add needle spin weapon").
- Possible fixes, for the owner to choose:
  - Do not zero the velocity in `JumpBehavior.end` when the jump ends because of the Needle Spin.
  - Apply the spin impulse after `BehaviorsSystem` has run, or end the jump behavior before the impulse is set.
