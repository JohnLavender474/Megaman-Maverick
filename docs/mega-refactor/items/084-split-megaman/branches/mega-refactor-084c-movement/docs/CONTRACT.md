# Contract: mega-refactor-084c-movement

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-30

The branch moves post-action momentum, the room-transition carry and the `AButtonTask` transitions out of `Megaman.kt`. Mega must
move at the same speeds in the same frames, and the A button must do the same thing in each state.

`M/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with these tests:
  - One test class for each component: a new instance equals the state after `reset()`, and the hooks stay.
  - Momentum: `shouldPreserveMomentum` on snow, on ice, and with and without the timer; `stopMomentum` on each axis, with and without the
    stop; `killPostActionMomentum` clamps on each axis and clears the pending kill; the pending kill waits for ground and no ice; the
    decay runs only when not running, over ground, with the timer on, not ground sliding and not on ice; `maxSpeed` runs only on a kill.
  - Room carry: the velocity on each axis comes back at the end; a climb or the air gives zero velocity at the start; `reset()` clears the
    pause timer and keeps the carried values, as the old bag keys did; `roomTransitionPaused` follows the timer.
  - Room-carry fix: a direction change during the transition, on each axis, has no exception and restores the old axis; no carry at the start
    and a carry at the end has no exception and the velocity stays zero; the end clears the values when it does not carry; a stale value is
    not restored.
  - The temporary suite `Temp084cMovementEquivalenceTest`, with the frozen copy `Temp084cFrozenMovement`: the old and the new code run on the
    same seeded frames (delta, over ground, ice, snow, running, sliding, facing a block, velocity, and the momentum calls), and each frame has
    the same velocity, timer state and pending kill. The carry comparison uses begin and end pairs with the same `carries` value and the same
    direction, because the fix changes the other cases on purpose. Both files are deleted in the last commit before the merge.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] No old state is left: `grep -nE 'postActionMomentumTimer|pendingPostActionMomentumKill|roomTransPauseTimer|"\$\{ConstKeys.VELOCITY\}' core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt` finds nothing.
- [ ] The parts do not depend on `Megaman`: `grep -lE 'Megaman\b|MegamanValues|MegamanWeapon|SoundAsset' <the 4 new files in entities/state/>` finds nothing.
- [ ] A short launch (about 15-20 s) of each new test room starts with no exception, on the baseline and at the end of the branch.

## Frame order

Each old line runs at this position. Review the final diff against this table. Lines are at `ba4b3ca1c`.

| Old line in `M/Megaman.kt` | New position |
|---|---|
| `:676` pause timer | `RoomTransitionCarryComponent.update`, same position |
| `:678-679` momentum timer, facing a block | First lines of `updatePostActionMomentum` |
| `:681-684` pending kill | Same function, next |
| `:686-697` decay | Same function, last. The call has the `:678` position, right after the pause timer |
| `:532`, `:540-552` start of the carry | `beginRoomTransitionCarry`, same positions in the event arm |
| `:564-576` end of the carry | `endRoomTransitionCarry`, same position in the event arm |
| `:727-744` `AButtonTask` | `updateAButtonTask`, last in the lambda |

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084c-movement`

The plan adds 3 adjoining rooms to `RefactorTest084.tmx`: `carry_left`, `carry_right` and `carry_top`.

Momentum:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: ground slide, then release: Mega slows as before. Ground slide off a ledge
  and land: Mega does not slide past the landing spot. Air dash and land: the same. Run into a wall during momentum: Mega stops. Jump
  during a ground slide: the speed carries as before.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: on ice, Mega keeps sliding after a run, an air dash and a ground slide. On snow,
  momentum stops as before. Land from an air dash on ice: the kill waits until Mega leaves the ice.
- `./run-game-alpha.sh --level MOON_MAN --room rest_room_1`: the kill clamps to the moon run speed. Run, ground slide and air dash feel the same.

Room carry:

- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room carry_left`: run across into `carry_right`: Mega pauses on the room change, shows no walk
  frame change during the pause, and keeps its speed after it. Ground slide and air dash across too: momentum stops at the change.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room carry_right`: jump up into `carry_top`, and fall back. Mega has zero velocity after the
  change. No crash on any of these changes.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room carry_right`: climb the ladder into `carry_top`, and climb back down. Mega stays on the
  ladder and has no gravity during the change.
- `./run-game-alpha.sh --level REFACTOR_TEST_051 --room to_gravity`: go into `gravity_room`, on the ground and in a jump. The direction changes to
  `DOWN` at the end of the transition, and Mega moves as before. This change stays on the vertical axis, so it does not reproduce the crash.

`AButtonTask`:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: walk off a ledge and press A: air dash. Press A again in the same air time:
  nothing happens until Mega lands. On the ground, A jumps. On a ladder and in a wall slide, A does what it did.
- `./run-game-alpha.sh --level GLACIER_MAN --room room6`: after a teleport or a gravity change, A does what it did. In water, A swims.

## Behaviors to check

- Death during a slide: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die during a ground slide or an air dash. After the
  respawn, Mega has no momentum, does not slow or clamp on the first landing, and A jumps. This check covers the reset order found in step 7.
- One real stage: `./run-game-alpha.sh --level GLACIER_MAN --room room1`: play through several rooms, with room changes on ground, in the air and
  on ladders.

## Intended changes

`Megaman.kt:502` is deleted, but it removes a key that no code writes.

Room-carry crash (owner-approved bug fix, from `051c`): `END_ROOM_TRANS` no longer throws when the direction changes to the other axis during a
transition, or when the entity carries only at the end. In these cases the player keeps the old axis velocity, or zero velocity. A stale value
from an earlier transition is no longer restored. With no direction change and the same `carries` value at both ends, the carry does not change.
The proof on this branch is the automated tests; `edge_rotated` on `051c` (check 18) is the play test.
