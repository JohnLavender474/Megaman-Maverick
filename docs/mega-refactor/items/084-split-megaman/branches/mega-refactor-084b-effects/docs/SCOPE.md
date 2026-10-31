# Scope: mega-refactor-084b-effects

- Status: approved
- Approved: 2026-10-30

## Goal

Move the trail sprites, the underwater bubbles, the death explosion orbs and the out-of-bounds kill out of `Megaman.kt`. Mega plays
the same.

## Item

From the 084 `ITEM.md`, "Proposed scope":

> **Effects as components:** trail sprites, underwater bubbles, the death explosion orbs, and the out-of-bounds kill.
>
> **Resets:** each new component resets its own state in `reset()`, so its lines leave `onSpawn`.

The form from `084d` and `084c` applies: a component holds the data with no entity reference, effects and values are constructor
arguments set in `define<X>Component()` functions, and the lambda stays as the schedule.

## Current state

Scan on 2026-10-30, `master` at `876b90a6e`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`,
`S/` = `C/entities/state/`. Line numbers are in `M/Megaman.kt` unless the line names a file. The file has 789 lines.

**Out-of-bounds kill.** `:631-637`, first in the lambda. It compares the body with the map size plus `DEATH_X_OFFSET` and
`DEATH_Y_OFFSET` (`:119-120`, both 1.5 tiles), then calls `destroy()`. The left and bottom checks run first, so the map is read only
if they are false. This path does not go through `onHealthDepleted`, so it spawns no orbs. No other entity uses this rule.

**Bubbles.** State: `underWaterBubbleTimer` (`:156`), delay `UNDER_WATER_BUBBLE_DELAY` (`:117`, 2 seconds). Lambda: `:657-669`. The timer
updates only if the head is in water and no force is applied. `spawnBubble()`: `:716-731`.

**Trail.** State: `trailSpriteTimer` (`:155`), delay `TRAIL_SPRITE_DELAY` (`:122`, 0.1 seconds). Lambda: `:671-675`. The timer updates in each
frame, and it resets only if a sprite spawned. `tryToSpawnTrailSprite()`: `:687-714`. It reads `currentWeapon`, `shooting` and 2
behaviors. `MegamanTrailSpriteV2` reads the owner as a `Megaman` (`:48` of that file).

**Respawn.** No code resets the 2 timers, so they survive a death. `IGameComponent.reset()` does nothing by default.

**Death orbs.** Table: `explosionOrbTrajectories` (`:106-115`). Spawn: `:516-525` in `onHealthDepleted`. `C/entities/contracts/AbstractBoss.kt` has the
same table (`EXPLOSION_ORB_TRAJS`, `:62-71`) and the same spawn (`:173-181`), and it imports `EXPLOSION_ORB_SPEED` from `MegamanValues`. No other file
reads the 2 tables. `BigAssMaverickRobot` and `ElecDevil` override `spawnExplosionOrbs` and call `super`. The `DEFEAT_SOUND` in that function and the
`MARIO_LEVEL` sound at `:527-528` stay where they are.

### Frame-order constraints

- The out-of-bounds check stays first in the lambda, because `destroy()` changes `existenceState` at once. The lambda goes on after it.
- The bubble block, then the trail block, at the same position: after `updatePostActionMomentum` and before `if (inPlay) spawningTimer.update(delta)`.

## In scope

The components go in `S/`, and they do not import `Megaman`, `MegamanValues` or a `SoundAsset`.

1. **`BubbleEmitterComponent(delay, canEmit, emit)`**: the timer as it is now, and `update(delta)` with the logic of `:657-669`. No `reset()`.
2. **`TrailSpriteComponent(delay, trySpawn)`**: the timer as it is now, and `update(delta)` with the logic of `:671-675`. `trySpawn` returns whether a sprite
   spawned. No `reset()`.
3. **Wiring:** `defineBubbleEmitterComponent()` and `defineTrailSpriteComponent()` in a new `M/components/EffectComponents.kt`. `spawnBubble()`, `tryToSpawnTrailSprite()` and
   the water condition move there as private functions, with no change. The 2 delays move to `MegamanValues`, with no value change. The lambda calls `update(delta)`
   on each component at the same positions.
4. **`IBodyEntity.isOutsideLevel(levelState, offsetX, offsetY)`** in a new `C/entities/utils/LevelBounds.kt`, with no component: this is a level rule, not an effect. The
   checks keep their order. `Megaman` keeps the log line, the `destroy()` and the 2 offsets.
5. **`ExplosionOrb.spawnRing(center, speed = DEFAULT_EXPLOSION_ORB_SPEED)`** in the `ExplosionOrb` companion, with one private table of 8 directions. `Megaman.onHealthDepleted` and
   `AbstractBoss.spawnExplosionOrbs` call it with no speed. Both old tables go. `EXPLOSION_ORB_SPEED` moves from `MegamanValues` to the `ExplosionOrb` companion as
   `DEFAULT_EXPLOSION_ORB_SPEED`, with no value change. `ConstKeys.SPEED` becomes an optional spawn prop of the orb, in tiles per second: `onSpawn` sets the velocity to
   `ConstKeys.TRAJECTORY` (now a direction) times the speed times `ConstVals.PPM`, and it uses the default if the prop is absent. The 2 callers are the only code that
   spawns orbs. The values stay the same: for example `-1 * 3.5 * 32` and `-3.5 * 32` are both `-112`.
6. **Tests:**
   - `BubbleEmitterComponent`: no timer progress when `canEmit` is false; one `emit` at the delay; the time carries across frames with `canEmit` false; the timer resets
     after `emit`; `reset()` keeps the time.
   - `TrailSpriteComponent`: the timer always runs; `trySpawn` runs only when the timer is finished; a `false` keeps the timer finished, so it tries again in the next frame;
     a `true` resets it; `reset()` keeps the time.
   - `isOutsideLevel`: each of the 4 sides, on the limit and past it; a left or bottom result does not read the map.
   - `ExplosionOrb`: with no `ConstKeys.SPEED`, the velocity uses the default; with the prop, it uses the override.
7. **Test room:** a new room `out_of_bounds` in `RefactorTest084.tmx`, with a pit that has no `Death` block, so that Mega can fall past the map. No current test room
   reaches this kill. `RefactorTest029` already has rooms for the rest: `trail`, `movement` (with water) and `player_death`.

No temporary suite. The 2 blocks are short, the spawn functions move with no change, and the permanent tests above state each rule of the timers. Review the moved lines against the old
lines in `git diff`.

### New API and its first caller

| API | First caller |
|---|---|
| `BubbleEmitterComponent`, `Megaman.defineBubbleEmitterComponent` | `Megaman.init()` |
| `TrailSpriteComponent`, `Megaman.defineTrailSpriteComponent` | `Megaman.init()` |
| `IBodyEntity.isOutsideLevel` | The lambda |
| `ExplosionOrb.spawnRing` | `Megaman.onHealthDepleted`, `AbstractBoss.spawnExplosionOrbs` |
| The `speed` argument of `spawnRing`, the optional `ConstKeys.SPEED` prop of the orb | No caller passes another speed yet. The owner asked for this override (see `docs/NOTES.md`, "Decisions") |

## Out of scope

- The trail code in `Starkner`, `GreenKoopaShell` and `RedKoopaShell`.
- `MegamanTrailSpriteV2` and its `Megaman` owner.
- The weapon checks in `tryToSpawnTrailSprite` (`024`).
- The `MARIO_LEVEL` sound (`025`) and `DEFEAT_SOUND`.
- A change of the log text at `:636`, which says "below game bounds" for each side.
- The interfaces on `Megaman`: these moves free none of them.
- The other bag keys in `M/`.

## Affected areas and risks

Files: `M/Megaman.kt`, `M/constants/MegamanValues.kt`, `C/entities/explosions/ExplosionOrb.kt`, `C/entities/contracts/AbstractBoss.kt`, 2 new files in `S/`, `M/components/EffectComponents.kt`,
`C/entities/utils/LevelBounds.kt`, new tests, and one room in the test map.

Risks:

- **Boss defeats.** `AbstractBoss` changes, so all bosses use the new ring. The table values are the same in both places.
- **Shared center vector.** `Megaman` fetched one pooled vector for each orb; `spawnRing(center)` passes one vector to all 8, as `AbstractBoss` does today. `ExplosionOrb.onSpawn` only
  reads it, so this is safe.
- **Timer start state.** Both timers start with no time passed, not at the end. Do not use `setToEnd()`.
- **`reset()` must stay empty.** If a component resets the timers, the first bubble and trail after a respawn move in time.
- **Hook order.** `canEmit` must run in each frame, before the timer. `trySpawn` must run only when the timer is finished, because it fetches an entity from a pool.
- **Performance:** 2 more map lookups in each frame, for one player. The lookups replace no others.

## Overlap with other branches

`mega-refactor-031e-bindings` and `mega-refactor-051c-camera-zones` (active) change none of the code files above. See `docs/NOTES.md`.

## Open questions

None. The 3 questions of the draft are answered in `docs/NOTES.md`, "Decisions".
