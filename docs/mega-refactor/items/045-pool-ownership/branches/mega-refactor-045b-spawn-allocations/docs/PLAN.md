# Plan: mega-refactor-045b-spawn-allocations

The owner asked for the work end to end with no stops, so the plan follows the draft scope at once.

## Steps

### 1. Engine: reusable motions and cullables

- Changes: `CullableOnUncontained.reset()` clears `timeUncontained`; `SineWave.set`, `Trajectory.set`,
  `RotatingLine.set`; `ShapeUtils.intersectRectangleAndLine(rectangle, line, out): Int`. Engine tests for each.
- Check: `engine:test`.
- Done: no

### 2. Shared spawn paths

- Changes: `AbstractEnemy`, `AbstractProjectile`, `IProjectileEntity`, `AbstractItem`, `Block`, `Spike`.
- Check: spawn-twice tests (same cullable instances, same results); `core:test`.
- Done: no

### 3. Per-frame leftovers and `delayRun`

- Changes: `LaserContacts`, `Laser`, `TubeBeamerV2`, `LaserRules`; debug suppliers of `Saw`, `SwingingAxe`,
  `SwingingPlatform`; `Darkness.handleLightEvent`; `delayRun` pool and `cancelDelayRun`, `MegamanWeaponFire`.
- Check: unit tests for `LaserContacts` and `delayRun`; `core:test`.
- Done: no

### 4. Entity sweep

- Changes: the projectiles, explosions, decorations, enemies and hazards in `SCOPE.md` "In scope", item 3 and 4.
  Done in groups by parallel subagents (no Gradle in the subagents), then one build.
- Check: `core:test`, `core:smokeTest`; `SawTest` spawn-twice test.
- Done: no

### 5. Guard and docs

- Changes: `SpawnAllocationGuardTest` with its baseline; `CODE_STYLE.md`; `utils/README.md`; `TODO.md` backlog with
  counts.
- Check: the guard passes; one mutation (a new `Vector2()` in an `onSpawn`) makes it fail.
- Done: no

### 6. Review before the play test

- Changes: fixes from the review.
- Check: all automated checks; the review of each manual check; the process log.
- Done: no
