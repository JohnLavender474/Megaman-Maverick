# Scope: mega-refactor-084g-movement-state

- Status: approved
- Approved: 2026-11-01

## Goal

Move the run state, the crouch timers and 2 bag values out of the property bag and the `Megaman` lifecycle, merge the 2 run functions, and remove 1 cast to `Megaman`. Mega plays the same. `canMove` stays on `Megaman` (see `docs/NOTES.md`, "Decisions").

## Item

From the 084 `ITEM.md`, "Proposed scope":

> Still in the bag: 15 lines in `entities/megaman/`. 4 go to `084g`, 2 (`ConstKeys.FOCUS`) go to `084f`, and 9 are weapon lines that go to `024`.

From the 084 `NOTES.md`, "Proposed branches":

> `084g-movement-state`: `canMove` and `RunComponent`; the 2 crouch timers move into `crouchBehavior()`; `ANIMATION_KEY` and `DIRECTION_ON_AIR_DASH` become typed; `runLeft` and `runRight` become one function (keep the `RODENT_CLAWS` difference at `M/components/ControllerComponent.kt:163-168,207-210`). Start after `031e` merges.

## Current state

Scan on 2026-11-01, `master` at `c6b8cecf7`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`. Line numbers are in `M/Megaman.kt` unless the line names a file.

**`canMove`.** A class member (`:87-88`): `!isLocked(MegaLockGroup.MOVEMENT) && !stunned && !damaged && !frozen`. It reads no class-only member: `isLocked`, `stunned`, `frozen` and `damaged` are `IGameEntity` queries. 8 behaviors and `runLeft` and `runRight` read it, 10 lines, all in `M/`.

**Run state.** `runTime` (`:100`) and `runningFlag` with `running` (`:223-228`). The getter is `inPlay && runningFlag`.

- Writers: `M/components/ControllerComponent.kt` (5 lines), `onSpawn` (`:361`), and `C/screens/levels/LevelRoomHandler.kt:155`, which casts to `Megaman` for this write only.
- Readers: `MegamanWeaponsHandler.kt:178,208,238,272`, `MegamanAnimationKeySupplier.kt:94` and `:530`.
- `runTime` has 5 uses in the controller file, and `onSpawn` resets it (`:347`).
- No behavior writes either value. `GameEngine.destroyNow` runs each component `reset()` for a player death, a level exit and `GameEngine.reset()`, so a `reset()` can replace the 2 `onSpawn` lines. The controller system does not run between the destroy and the next spawn.

**`runLeft`, `runRight`.** `M/components/ControllerComponent.kt:161-203,205-247`, 86 lines that are almost the same. 3 facts:

- The impulse has the opposite sign, so 1 sign value covers both.
- The factor `if (isBehaviorActive(BehaviorType.WALL_SLIDING)) -1f else 1f` is dead in both. `running` is false during a wall slide, so the function returns before that line.
- The `RODENT_CLAWS` difference is live. When the guard stops the run, `runLeft` sets the facing only if `RIGHT` is not pressed; `runRight` sets the facing even if `LEFT` is pressed. So with both buttons pressed during a shot, the facing goes right, and it does not go left.

**Crouch timers.** `groundSlideToCrouchDelay` and `buttonToCrouchDelay` (`:105-106`). Only `M/behaviors/CrouchBehavior.kt:23-32` uses them. `onSpawn` does not reset them, and `init()` builds the behavior once, so a behavior field has the same life.

**`ANIMATION_KEY`.** `var Megaman.currentAnimKey` (`M/components/AnimationsComponent.kt:19-23`) has 2 bag lines. It has 1 writer (`:38` there) and 2 readers: the key supplier and `MegamanTrailSpriteV2.kt:52`. `ConstKeys.ANIMATION_KEY` has no other use. The bag is not cleared on a destroy, so the value lives through a respawn.

**`DIRECTION_ON_AIR_DASH`.** 2 bag lines: the write at `M/behaviors/AirDashBehavior.kt:95` and the read at `M/components/SpritesComponent.kt:50`, with `!!`. The read is only during an air dash, after the write. `MegamanKeys.kt` holds only this key.

### Frame-order constraints

No line moves in a frame. The merged run function does the same checks in the same order, and each actuator calls it at the same place. The left actuator still runs before the right one.

## In scope

1. **`canMove`:** the member stays. It gets the `TODO` that `invincible` has: a second player entity needs this same rule; extract one function when it exists (item `060`).
2. **`RunComponent`:** new `C/entities/state/RunComponent.kt` holds `running` and `runTime`, with `reset()`. New `C/entities/state/Runs.kt` holds `var IGameEntity.running`, whose getter keeps `inPlay &&`. `Megaman.init()` adds the component. `runningFlag`, `runTime` and the 2 `onSpawn` lines go.
3. **The cast:** `LevelRoomHandler.kt:155` writes the query, so 16 casts become 15.
4. **One run function:** `run(delta, side)` replaces `runLeft` and `runRight`. The `RODENT_CLAWS` difference stays, as a parameter, with a `TODO` that names the issue file. The dead wall-slide factor goes.
5. **Crouch timers:** 2 fields of the object in `crouchBehavior()`.
6. **Typed values:** `currentAnimKey` and `airDashDirection` become fields on `Megaman`, as `facing` and `aButtonTask` did in `084a`, with no reset, so each keeps its value through a respawn as the bag does. Delete `MegamanKeys.kt` and `ConstKeys.ANIMATION_KEY`.
7. **Tests:** the run function first, on the old code: the velocity for each of the 4 directions on both sides, the ramp of `runTime`, the guard, and the `RODENT_CLAWS` difference. Then `RunComponent.reset()`, the `running` query out of play, and the 2 crouch delays.

No new test room. `RefactorTest029` has `movement` and `gravity_switch`.

### New API and its first caller

| API | First caller |
|---|---|
| `RunComponent` | `Megaman.init()`, the run function |
| `IGameEntity.running` | The run function, `LevelRoomHandler.kt:155`, `MegamanWeaponsHandler.kt:178` |
| `Megaman.airDashDirection` | `AirDashBehavior.kt:95`, `SpritesComponent.kt:50` |

## Out of scope

- A fix of the `RODENT_CLAWS` difference. It changes gameplay, so it is a bug candidate (see `docs/TODO.md`).
- `getMaxRunSpeed()`, the run values in `MegamanValues` (`060`), and the weapon checks in the run guard (`024`).
- The 9 weapon bag lines (`024`).
- `airDashConsumed`, `wallJumpTimer` and the other movement fields on `Megaman`. No planned branch took them.
- The charge timer (`084h`).

## Affected areas and risks

Production files that change: `Megaman.kt`, `ControllerComponent.kt`, `MegamanWeaponsHandler.kt`, `MegamanAnimationKeySupplier.kt`, `LevelRoomHandler.kt`, `CrouchBehavior.kt`, `AirDashBehavior.kt`, `AnimationsComponent.kt`, `SpritesComponent.kt`, `MegamanTrailSpriteV2.kt` if its import changes, and `Constants.kt`. 1 file is deleted and 2 are new.

Risks:

- **The merged run function.** This is the only logic change, and a sign error reverses Mega on a wall or a ceiling. The tests come first on the old code, and they must pass with no change to their expected values after the merge. `gravity_switch` covers it in play.
- **`running` in a test.** A `Megaman` with no `init()` has no `RunComponent`. The setter throws, as `setPlayPhase` does; the getter gives `false`.
- **A reset in place of `onSpawn`.** A spawn with no destroy before it would keep the old value. `Megaman` has one path, through `destroyNow`, and `GameEngine.reset()` uses it too.
- **Performance:** each `running` read and write adds 1 or 2 component lookups, in place of a field read: several in each frame for one player. The run function caches the component in a local for `runTime`. `--diagnostics` can measure it.

## Overlap with other branches

See `docs/NOTES.md`. `051e` changes 1 or 2 lines in `CrouchBehavior.kt`, `AirDashBehavior.kt` and `ControllerComponent.kt`.

## Open questions

None.
