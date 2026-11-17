# Scope: mega-refactor-045b-spawn-allocations

- Status: draft; the decisions are Claude's choice, pending owner confirmation
- Approved: no (the owner asked for the branch end to end by a subagent, with no stops for questions, 2026-11-16)

## Goal

A pooled entity (from `MegaEntityFactory`) makes its objects once: in field initializers or `init()`, or the first
time a spawn needs one more, and then keeps them. `onSpawn`, `onDestroy` and event handlers only set, reset, clear,
hide or show. Also remove the per-frame `new` that `045a` left. Model: `SpikeBall` and `SpikeBallTest` (`045a`).

## Item

`045` (`ITEM.md`), the parts for this branch:

- (2026-11-16, owner, `045a` review) A pooled entity makes its objects once; the per-spawn and per-event `new` sweep
  is `045b`.
- The `045a` `docs/TODO.md` leftovers: `ShapeUtils.intersectRectangleAndLine` (a `Vector2` per hit, each frame for
  `Laser` and `TubeBeamerV2`, and a `GamePair` per hit in the callers), the `--debugShapes` suppliers of `Saw`,
  `SwingingAxe` and `SwingingPlatform` (a `GameLine` per frame), `Darkness.handleLightEvent` (a `GameCircle` per light
  event), `MegaUtilMethods.delayRun` (a `UUID` string and a `Timer` per call).
- Consider a source-scan guard (as `PoolOwnershipGuardTest`), or a note for item `161` (detekt).

## Current state (scan on `b35f8e9a5`)

Scan: `temp/mega-refactor-045b-spawn-allocations/scan.py` (main checkout). It finds the bodies of `onSpawn`,
`onDestroy`, `onEvent` and `canSpawn` in each entity file, follows the calls to functions of the same file, and lists
each constructor call (a capital name before `(`) and each collection builder, copy and kept lambda. Output:
`scan.tsv` (all rows) and `ctor.tsv` (constructor rows, exceptions removed).

- 319 constructor rows in 102 files. By type: `Timer` 59, `Vector2` 38, `GameRectangle` 25, `GameSprite` 22,
  `TimeMarkedRunnable` 16, `DrawingPriority` 15, `Array` 14, `Animation` 14, `Animator` 11, `GameCircle` 9,
  `OrderedMap` 8, `MotionDefinition` 8, `SineWave` 7, `Regex` 7, `GameLine` 7, others fewer.
- Shared spawn paths that make objects on every spawn of a whole family:
  - `AbstractEnemy.onSpawn` (`entities/contracts/AbstractEnemy.kt:213-248`): a `CullableOnUncontained` (2 lambdas), a
    default bounds lambda, an `ObjectSet`, a `CullableOnEvent` (1 lambda) and a `runnablesOnDestroy` lambda, for each
    spawn of each of about 125 enemy classes.
  - `AbstractProjectile.onSpawn` (`AbstractProjectile.kt:38-62`): through `IProjectileEntity.getCullOnOutOfGameCam`
    and `getCullOnEventsCullable` (`IProjectileEntity.kt:55-64`): the same set of objects for each shot.
  - `AbstractItem.onSpawn` (`items/AbstractItem.kt:107`), `Block.onSpawn` (`blocks/Block.kt:79-100`, also a `Regex`
    per label list and a `Fixture` and `GameRectangle` per extra fixture), `Spike.onSpawn` (`hazards/Spike.kt:141-161`,
    also an `Animation`).
- Projectiles, explosions and decorations that spawn many times a room: `ChargedShot` (`.cpy()`), `CaveRock`,
  `Asteroid`, `TubeBeam`, `D1000Bomb`, `PreciousGem`, `SphinxBall`, `ChargedShotExplosion`, `ExplosionField`,
  `ExplosionOrb`, `MagmaGoopExplosion`, `SnowballExplosion`, `Flame`, `LavaBeam`, `Bolt`, `SmallIceCube`, `Splash`
  (a `DrawingPriority` default each spawn), `Smoke` (sprites, animation and components made each spawn), `Snow`,
  `FloatingEmber`, `FallingLeaves`, `LightSourceEntity`.
- Enemies (each room entry respawns them): `Adamski`, `Bat`, `BulbBlaster`, `CarriCarry`, `DeathBat`, `DemonMet`,
  `DrillHead`, `Eyee`, `HoverGunner`, `IceFox`, `JetpackBlaster`, `Junkroid`, `MetaWheelF`, `PetitDevil`, `Popoheli`,
  `PreciousTron`, `SubmarineJoe`, `TellySaucer`, `YellowTiggerSquirt`.
- Hazards with motion: `Saw`, `SwingingAxe`, `SwingingPlatform` (a new `Pendulum`, `RotatingLine` or `Trajectory`,
  `MotionDefinition`, debug lines, circles and lambdas each spawn), `FireballBar` and `LaserBeamer` (a new
  `RotatingLine`), `Laser` (`onDestroy` makes 2 `GameLine`s and an `OrderedMap`; `buildLaserSprites` makes sprites).
- Per frame: `ShapeUtils.intersectRectangleAndLine` (`engine/.../common/shapes/ShapeUtils.kt:45`); the callers make a
  `GamePair` per hit (`LaserRules.kt:46,69,112`, `Laser.kt:392`, `TubeBeamerV2.kt:319`).
- `MegaUtilMethods.delayRun` (`utils/MegaUtilMethods.kt:15-31`): a `UUID` string, a `Timer`, a wrapper lambda and a
  removal lambda per call; 7 callers, one of them each buster shot (`MegamanWeaponFire.kt:73`).
- Engine fact: `CullableOnUncontained.reset()` does not clear `timeUncontained`
  (`engine/.../cullables/CullableOnUncontained.kt`). A new instance starts at 0; a reused one keeps the time of its
  last spawn if that spawn ended while the entity was out of view.
- Not counted as violations: `MegaGameEntity.onSpawn` adds its `MapObjectComponent` and `GroupsComponent` once and
  reuses them; exceptions on the fail-fast path.
- Out of the count: 402 `GameLogger` lines in these bodies build a message string even when the tag is filtered out,
  and `getOrDefault` with a `Float` default boxes the value.

## In scope

1. **Shared spawn paths:** `AbstractEnemy`, `AbstractProjectile` (and `IProjectileEntity`), `AbstractItem`, `Block`,
   `Spike`. Each cullable, its lambdas, its event set and its `runnablesOnDestroy` entry are made once (fields, or the
   first spawn that needs them); `onSpawn` resets and puts them. `Regex` patterns become companion constants.
   (2026-11-16, after the review: the extra fixtures of `Block` and the `Animation` of an animated `Spike` stay as they
   are and move to `045c`; both are rare spawn cases.)
2. **Engine:** `CullableOnUncontained.reset()` also clears `timeUncontained`, so a reused cullable starts as a new one.
   `SineWave.set(origin, speed, amplitude, frequency)`, `Trajectory.set(definitions)` (parses again only when the
   text changes) and `RotatingLine.set(origin, radius, speed, degreesOnReset)` let an entity reuse its motion.
3. **Projectiles, explosions, decorations, enemies and hazards** in the lists above: owned fields, timers made in
   `init()` and set with `resetDuration`, motions made once and set, sprites made once and hidden or shown.
4. **Child spawn props:** a value that only goes into the spawn props of a child (a position, a bounds rectangle) is a
   pool scratch, not a `new` (`045a` rule 3: the child copies it). Each child that this branch feeds that way is read
   to confirm that it copies.
5. **Per frame:** `ShapeUtils.intersectRectangleAndLine` writes into a caller-owned array and returns the hit count;
   a new `LaserContacts` (core) owns the contact points of `Laser` and `TubeBeamerV2`; `LaserRules` add to it. The
   debug suppliers of `Saw`, `SwingingAxe`, `SwingingPlatform` use owned lines. `Darkness.handleLightEvent` uses an
   owned circle.
6. **`delayRun`:** pooled run objects, each with a fixed id made once; a finished run goes back to the pool after its
   removal. `MegaUtilMethods.cancelDelayRun` for the one caller that cancels (`MegamanWeaponFire`).
7. **Guard:** `SpawnAllocationGuardTest` (core test): a source scan of the `onSpawn`, `onDestroy` and `onEvent` bodies
   of the entity files for constructor calls of a fixed list of types, against a baseline file of the known rows (the
   backlog). A new row fails the test; a fixed row must leave the baseline.
8. **Docs:** `docs/CODE_STYLE.md` (the table row and the rule name the guard), `core/.../utils/README.md` (rule 2 no
   longer allows a plain `new` per spawn).
9. **Tests:** spawn-twice tests for the shared paths (`AbstractEnemy` through a real enemy, `AbstractProjectile`
   through a real shot), for `Saw` and for a motion entity, unit tests for the engine changes, `LaserContacts`,
   `intersectRectangleAndLine` and `delayRun`.

## Out of scope (backlog candidate `045c`, counts in `TODO.md`)

- Bosses (spawn once per fight): `ElecDevil`, `GlacierMan`, `InfernoMan`, `MechaDragon`, `MoonMan`, `ReactorMonkey`,
  `RodentMan`, `SigmaRat`, `SigmaRatClaw`, `GutsTank`, `GutsTankFist`, `TimberWoman`, `DesertMan`, `WilyFinalBoss`,
  `BigAssMaverickRobotHand`.
- Level objects that build a sprite grid or parse map data per spawn (spawn once per level load or room): `Water`,
  `Lava`, `ToxicWater`, `ToxicWaterfall`, `DrippingToxicGoop`, `PolygonWater`, `QuickSand`, `GrowingVine`,
  `PreciousSpike`, `SpringBouncer`, `AirConditioner`, `RailTrack`, `Darkness` (grid), `DarknessV4`,
  `DisappearingBlocks`, `GauntletMaster`, `Force`, `GravityChange`, `ForceDecoration`, `DecoratedGravityForce`,
  `EventTrigger`, `Death`, `Rain`, `WhiteArrowPool`, `AsteroidsSpawner`, `InfernoMeteorShower`, `CapsuleTeleporter`,
  `PipePortal`, `Togglee`, `RoomShaker`, `RotationAnchor`, `RocketPlatform`, `SteamRoller`, `CeilingCrusher` (also
  changed by the `compactor-level` branch), `IceCubeMaker`, `CameraEdgeBlock`, `PushableBlock`, `Electrocutie`,
  `GlacierMan`-style child constructors (`GutsTank`, `SigmaRat`, `Electrocutie`, `PushableBlock` make child entities
  with a constructor, not from the factory).
- Dead or test classes: `SniperJoe_OLD`, `TestEnemy`, `TestTentacle`.
- Messages: an `Event` and the `props(...)` that a sender passes to `spawn` or `submitEvent`. They are not kept by
  the sender; a message pool is an engine design of its own.
- Log strings and boxed defaults (see "Current state").
- The slot-owned players (`Megaman`, X): not pooled.

## Decisions (Claude's choice, pending owner confirmation)

| ID | Question | Options | Choice |
|---|---|---|---|
| D1 | All 102 files in this branch? | a) the shared spawn paths, the often-spawned classes and the per-frame leftovers here; bosses and level objects in `045c`; b) all here | a: the shared paths cover the most spawns; bosses and level objects spawn once per fight or room, and a sprite grid rebuild needs its own design (grow-only grids); a smaller diff is safer to play-test |
| D2 | Messages (`Event`, spawn `props`)? | a) leave them; values inside child props become pool scratch; b) pool the messages | a: the sender does not keep them; a message pool changes the engine event and spawn flow |
| D3 | Guard? | a) a source-scan test with a baseline that only shrinks; b) only a note for item `161` | a: the rule can be found by text inside these bodies; a `TODO` moves it to detekt with `161` |
| D4 | `delayRun`? | a) pooled runs with fixed ids and `cancelDelayRun`; b) only a counter id in place of `UUID` | a: no allocation per call apart from the caller's own lambda; the canceller clears its id when the run ends, so a reused id is never cancelled by mistake |
| D5 | Laser hit points? | a) `intersectRectangleAndLine` fills a caller-owned array and returns a count; `LaserContacts` owns the pairs; b) a callback per hit | a: no lambda and no new object per hit; the dedup of a corner hit stays |

## New API and first callers

| API | First production caller |
|---|---|
| `SineWave.set(origin, speed, amplitude, frequency)` | `Snow`, `FloatingEmber`, `TellySaucer`, `YellowTiggerSquirt`, `CarriCarry`, `Adamski` |
| `Trajectory.set(definitions: String)` | `BulbBlaster`, `Saw` |
| `RotatingLine.set(origin, radius, speed, degreesOnReset)` | `PetitDevil`, `FireballBar`, `LaserBeamer`, `Saw` |
| `ShapeUtils.intersectRectangleAndLine(rectangle, line, out: Array<Vector2>): Int` (changed) | `Laser`, `TubeBeamerV2`, `LaserRules` |
| `LaserContacts` (`add`, `clear`, `isEmpty`, `peek`, `poll`) | `Laser`, `TubeBeamerV2`, `LaserRules` |
| `MegaUtilMethods.cancelDelayRun(services, id)` | `MegamanWeaponFire` |

The final list is in the process log after the work.

## Affected areas and risks

- About 55 entity files change their spawn code. Risk: a per-spawn object that held state that a new one cleared
  (a timer's runnables, a cullable's time, a sprite's flip or alpha). Each reused object is reset to the values its
  constructor gave. The spawn-twice tests and the play checks cover the shared paths and the live entities.
- Random draws: the order of `services.random` calls per spawn stays the same.
- `--poolChecks` shows a kept scratch from item 4 at once.

## Overlap with other branches

See `NOTES.md`.

## Objective check

| Rule | Effect | Note |
|---|---|---|
| `O1-R1` No global session state | waiting | `delayRun`'s run pool is a static like `GameObjectPools`; no new kind |
| `O1-R3` No new fragile order | no effect | listener add and remove keep their place in `onSpawn` and `onDestroy` |
| `O1-R4` Typed state | no effect | |
| `O1-R5` Code only with a caller | helps | each new API has a caller (table above) |
| `O2-R2` Random only from the seeded source | no effect | same draws in the same order |
| `O2-R4` Stable iteration order | no effect | `LaserContacts` keeps the distance order |
| `O5-R1` No per-frame allocation in common paths | helps | laser hits, debug lines |
| `O5-R2` Measure hot paths | partly | allocation counted by the spawn-twice tests (same instances); no `--diagnostics` run (no game launch on the branch) |
| `O6-R1` CI passes | helps | new tests and guard |
| `O6-R2` Named checks | helps | `CONTRACT.md` |

## Open questions

- D1 to D5 wait for the owner's confirmation.
