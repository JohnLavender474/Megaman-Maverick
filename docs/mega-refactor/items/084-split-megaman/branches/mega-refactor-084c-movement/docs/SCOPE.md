# Scope: mega-refactor-084c-movement

- Status: approved
- Approved: 2026-10-30 (the reset-order change and the room-carry fix were approved later, the same day)

## Goal

Move post-action momentum and the room-transition velocity carry out of `Megaman.kt` into components, and move the
`AButtonTask` transitions out of the updatables lambda into one function. Mega plays the same, except for the room-carry crash fix
(see "In scope", item 7).

## Item

From the 084 `ITEM.md`, "Proposed scope":

> **Movement support as components:** post-action momentum (`preserveMomentum`, `stopMomentum`,
> `killPostActionMomentum`, the decay in the updatables), and the room-transition velocity carry (`onEvent`,
> `BEGIN/CONTINUE/END_ROOM_TRANS`).
>
> **The `AButtonTask` transitions** that run in the updatables, next to the ones in the behaviors.
>
> **Resets:** each new component resets its own state in `reset()`, so its lines leave `onSpawn`.

Owner decisions that apply (084 `ITEM.md`, "Decisions"): a type with `fun reset()` extends `Resettable`. The form from `084d`
applies: a component holds the data with no entity reference, free functions touch the body, effects and values are
constructor arguments set in `define<X>Component()` functions, and the lambda stays as the schedule.

## Current state

Scan on 2026-10-30, `master` at `ba4b3ca1c`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`,
`M/` = `C/entities/megaman/`. Line numbers are in `M/Megaman.kt` unless the line names a file.

**Momentum.** State: `postActionMomentumTimer`, `pendingPostActionMomentumKill` (`:144-145`). Lambda: `:678-697`. Functions:
`shouldPreserveMomentum`, `preserveMomentum`, `stopMomentum`, `killPostActionMomentum` (`:847-874`). `onSpawn`: `:488-489`.
Outside callers, 9 lines in 5 files: `BodyComponent.kt:441`, `ControllerComponent.kt:243`, `AirDashBehavior.kt:107,136,140`,
`GroundSlideBehavior.kt:105,117,121`, `JumpBehavior.kt:118`. Two of them write `pendingPostActionMomentumKill` directly
(`AirDashBehavior.kt:140`, `GroundSlideBehavior.kt:121`). `onEvent` calls `stopMomentum(true)` at the room-transition events.

**Room carry.** State: `roomTransPauseTimer` (`:314`) and 2 bag keys on the body, `velocity_y` and `velocity_x`. Events:
`:531-559` (begin, continue), `:561-581` (end). The lambda updates the timer at `:676`. `onSpawn`: `:485`. Reader:
`MegamanAnimationKeySupplier.kt:28`. `:502` removes the key `ConstKeys.VELOCITY` from the body, but nothing writes that key to
this body, so the line does nothing.

**`AButtonTask`.** `:727-744` in the lambda, last in the order. `airDashConsumed` is at `:147`, written at `:490`, `:727` and
`AirDashBehavior.kt:75`. The behaviors and `BodyComponent` also write `aButtonTask`; they do not change.

**Facts for the design.** `bodyOverGround` is a lambda in a bag key on the body (`BodyComponent.kt:78,184`), and the key is set only
by `Megaman`. `getMaxRunSpeed()` is a `Megaman` extension and reads the level. `FacingUtils.isFacingBlock` needs an `IFaceable`.

### Frame-order constraints

- `:676`, then `:678`, then the kill at `:681-684`, then the decay at `:686-697`, as now.
- `:678-697` run after `:672` and before the water and trail blocks.
- `:727` runs before `:729`, and the `AButtonTask` block stays last in the lambda.
- The `stopMomentum` and `stopSound` calls in the event arms keep their order.

## In scope

All new types go in `C/entities/state/`, and they do not import `Megaman`, `MegamanValues` or a `SoundAsset`.

1. **`PostActionMomentumComponent(duration, resistance, maxSpeed, overGround, facingBlock, groundSliding)`**: the timer, the pending
   kill, and `reset()` equal to `:488-489`; `onSpawn` also calls `reset()` (see "Reset order"). The last 4 arguments are hooks. A hook runs only when the old code reads the value.
   `PostActionMomentum.kt`, functions on `IBodyEntity` with the same names, so most callers change only an import:
   `shouldPreserveMomentum`, `preserveMomentum`, `stopMomentum`, `killPostActionMomentum`, and new `requestMomentumKill()` in place
   of the 2 direct writes. `updatePostActionMomentum(delta, running)` holds `:678-697` as one block.
2. **`RoomTransitionCarryComponent(pauseDuration, carries)`**: the pause timer and 2 typed fields `carriedX` and `carriedY` in place
   of the bag keys. `carries` is a hook for "not climbing and over ground". `RoomTransitionCarries.kt`: `IGameEntity.roomTransitionPaused`,
   `IBodyEntity.beginRoomTransitionCarry()`, `IBodyEntity.endRoomTransitionCarry()`. `Megaman.onEvent` keeps the event arms, `setCenter`,
   `gravityOn`, `stopMomentum` and `stopSound`, and calls these functions. The lambda calls `update(delta)` at the `:676` position.
3. **`updateAButtonTask()`**: `internal fun Megaman.updateAButtonTask()` in a new `M/MegamanAButtonTask.kt`, with `:727-744` unchanged. No
   component. The lambda calls it last. `airDashConsumed` stays a field on `Megaman`.
4. **Wiring:** `defineMomentumComponent()` and `defineRoomTransitionCarryComponent()` in a new `M/components/MovementComponents.kt`.
5. **Clean-up:** delete `:502`, because no code writes that key.
6. **Tests:**
   - One test class per component: a new instance equals the state after `reset()`, and the hooks stay.
   - Momentum: `shouldPreserveMomentum` for snow, ice and the timer; `stopMomentum` on each axis; `killPostActionMomentum` clamps;
     the pending kill waits for ground and no ice; the decay runs only in its conditions; `maxSpeed` runs only on a kill.
   - Room carry: the carry on each axis, a climb, and in the air.
   - A temporary suite with a frozen copy compares the old and new code frame by frame. `CONTRACT.md` lists it.
7. **Room-carry crash fix (added 2026-10-30 at the request of `051c`; a bug fix that the owner approved).** The cause: `LevelRoomHandler`
   `transition.onEnd` submits `END_ROOM_TRANS` (buffered) and then calls `services.gravity.set(...)` at once, so the event can arrive with a
   direction on the other axis, and `carriedX!!` throws. The `051c` branch reproduced this at `Megaman.kt:572` in the old code. The fix:
   - `beginRoomTransitionCarry` clears `carriedX` and `carriedY`, then stores the value as before.
   - `endRoomTransitionCarry`: if `carries()` is true, it restores each value that is not null to its own axis, with no `!!` and with no
     axis selected from the current direction. If `carries()` is false, it sets the velocity to zero as before. It always clears both values.
   - If the direction changes, the player keeps the old axis velocity, which is about 0 on the ground. This is accepted.

### New API and its first caller

| API | First caller |
|---|---|
| `PostActionMomentumComponent`, `Megaman.defineMomentumComponent` | `Megaman.init()` |
| `shouldPreserveMomentum`, `preserveMomentum`, `stopMomentum`, `killPostActionMomentum` | The 9 lines above; they replace members |
| `requestMomentumKill` | `AirDashBehavior:140`, `GroundSlideBehavior:121` |
| `updatePostActionMomentum` | The lambda |
| `RoomTransitionCarryComponent`, `Megaman.defineRoomTransitionCarryComponent` | `Megaman.init()` |
| `roomTransitionPaused` | `MegamanAnimationKeySupplier:28` |
| `beginRoomTransitionCarry`, `endRoomTransitionCarry` | `Megaman.onEvent` |
| `Megaman.updateAButtonTask` | The lambda |

## Out of scope

- The `aButtonTask` writes in the behaviors and `BodyComponent`, and a component for `AButtonTask` (see `docs/NOTES.md`).
- `bodyOverGround` and its bag key, `getMaxRunSpeed()`, `MegamanValues` numbers.
- The trail, bubbles, orbs and the out-of-bounds check (`084b`).
- The other bag keys in `M/`, for example `MegamanKeys.DIRECTION_ON_AIR_DASH`.
- The room events themselves: `LevelRoomHandler` and the camera parts from `051b`.

## Affected areas and risks

Files: `M/Megaman.kt`, `BodyComponent.kt`, `ControllerComponent.kt`, `AirDashBehavior.kt`, `GroundSlideBehavior.kt`, `JumpBehavior.kt`,
`MegamanAnimationKeySupplier.kt`, 4 new files in `C/entities/state/`, 2 new files in `M/`, new tests, and rooms in the test map.

Risks:

- **Reset order (found in plan step 7).** `GameEngine.destroyNow` resets components in insertion order. `PostActionMomentumComponent` is added
  before `BehaviorsComponent`, and `AirDashBehavior.end` and `GroundSlideBehavior.end` write momentum when `BehaviorsComponent.reset()` ends them.
  So `:488-489` cannot simply leave `onSpawn`: `onSpawn` calls `PostActionMomentumComponent.reset()` at that position. This is a scope change from
  the first approved text ("its lines leave `onSpawn`"); the owner approved it.
- **Stale carried values.** Today the bag keys survive a death, because `:502` removes another key. `reset()` still clears only the pause
  timer, not `carriedX` or `carriedY`, and there are 2 fields, not 1. The crash fix clears both values at the start of each transition, so
  a stale value cannot reach a later transition.
- **Room-carry fix.** The fix changes play only in 2 cases: the direction changes to the other axis during the transition, and a stale
  value that the old code restored. `051c` owns the room that reproduces the crash (`edge_rotated`); this branch has the automated tests as
  the proof. `WilyStage3_v4.tmx` (`room7`, `room9`) also sets `megaman_direction`; the same crash may be possible there, not tested.
- **Hooks must stay lazy.** `maxSpeed` must run only on a kill, because it reads `currentLevel!!`. `overGround` must not run before the
  body is built.
- **`Timer.setToEnd()` against `reset()`:** keep the same calls. The pause timer starts at the end through `onSpawn` today.
- **Order in `onEvent`:** `stopMomentum(true)` runs in `BEGIN`, `CONTINUE` and `END`, so it now calls `require` in each `CONTINUE` frame.
- **Performance:** each momentum call adds a map lookup. `CONTINUE_ROOM_TRANS` and the lambda run each frame for one player. Measure
  with `--diagnostics` if you want the numbers.
- **Proof of order is partly manual,** as in `084d`.

## Overlap with other branches

`mega-refactor-031e-bindings` (active) changes `AirDashBehavior.kt` (`:33`, `:38`) and `ControllerComponent.kt`, and it changes `Constants.kt`.
This branch changes `AirDashBehavior.kt:140`, imports in both files, and `ControllerComponent.kt:243` only through an import. See
`docs/NOTES.md`.

`mega-refactor-051c-camera-zones` (active): this branch must merge before it. Check 18 of the `051c` play test (`edge_rotated`) needs the
room-carry fix. Tell the owner when this branch has merged.

## Open questions

None. The 2 questions of the draft are answered in `docs/NOTES.md`, "Decisions".
