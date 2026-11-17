# TODO: mega-refactor-045b-spawn-allocations

## Tasks

## Backlog candidates

- **`045c`: the rest of the spawn sweep.** `SpawnAllocationGuardTest` lists the files that still make objects in the
  direct body of `onSpawn`, `onDestroy` or `onEvent`: `core/src/test/resources/spawn-allocation-baseline.txt`
  (38 files, 85 calls on this branch; 73 files, 158 calls on `master` at `b35f8e9a5`). The guard does not follow calls
  to other functions; the full scan (`temp/mega-refactor-045b-spawn-allocations/scan.py`, main checkout) also follows
  same-file calls. Left, by group:
  - Bosses (spawn once per fight): `ElecDevil`, `GlacierMan`, `InfernoMan`, `MechaDragon`, `MoonMan`,
    `ReactorMonkey`, `RodentMan`, `SigmaRat`, `SigmaRatClaw`, `GutsTank`, `GutsTankFist`, `TimberWoman`, `DesertMan`,
    `WilyFinalBoss`, `BigAssMaverickRobotHand`.
  - Level objects that build a sprite grid or parse map data per spawn: `Water`, `Lava`, `ToxicWater`,
    `ToxicWaterfall`, `DrippingToxicGoop`, `PolygonWater`, `QuickSand`, `GrowingVine`, `PreciousSpike`,
    `SpringBouncer`, `AirConditioner`, `RailTrack`, `Darkness` (grid, `LightEvent` and `LightEventDef` per event),
    `DisappearingBlocks`, `GauntletMaster`, `Force`, `GravityChange`, `ForceDecoration`, `DecoratedGravityForce`,
    `EventTrigger`, `Death`, `Rain`, `WhiteArrowPool`, `AsteroidsSpawner`, `InfernoMeteorShower`,
    `CapsuleTeleporter`, `PipePortal`, `Togglee`, `RoomShaker`, `RotationAnchor`, `RocketPlatform`, `SteamRoller`,
    `CeilingCrusher`, `IceCubeMaker`, `CameraEdgeBlock`.
  - Child entities made with a constructor in `onSpawn`, not from the factory: `GutsTank`, `SigmaRat`,
    `Electrocutie`, `PushableBlock`.
  - `Block`: a `Fixture` and a `GameRectangle` per extra fixture entry, a block-filter lambda per tag, the label
    string split; `getStandardBodyDoUpdatePredicate` per spawn. `Spike`: an `Animation` per animated spawn.
  - Shared helpers: `IBody.addBodyLabels` and `IFixture.addFixtureLabels` make a default `ObjectSet` on each call;
    `Properties.getAllMatching` makes an `Array` and pairs.
  - Per-event code that the sweep found but left: `PreciousTron.throwGem` (a set and a lambda per throw),
    `JetpackBlaster.shoot` and `calculateAimLine`, `HoverGunner` fan order.
- **Messages:** an `Event` and the `props(...)` for each `spawn` and `submitEvent`. A pool for them is an engine design
  (the receiver must not keep them).
- **Log strings:** 402 `GameLogger` lines in spawn, destroy and event bodies build their message even when the tag is
  filtered out (for example `MegaGameEntity.onSpawn` on every spawn). A lazy overload (`debug(tag) { message }`) would
  remove them.
- **Boxed defaults:** `getOrDefault(key, 1f, Float::class)` boxes the default on each call.
- **Detekt:** move `SpawnAllocationGuardTest` to a detekt rule with item `161` (a `TODO` is at the test).

## Bug candidates

- `Saw` of type `t` (trajectory): `setToTrajectory` sets the body center to the motion value, but
  `Trajectory.getMotionValue` gives a velocity, so the saw jumps to a point near the world origin. Same on `master`;
  no live map uses `type=t`. `BulbBlaster` and `PreciousBlock` set `body.physics.velocity` in place.
- The `Trajectory` `MotionDefinition.onReset` of `Saw` and `BulbBlaster` read a pool scratch on `master` (fixed here
  with an owned vector).

- `LightSourceEntity` (and `Spike` with `cull_events`): `getStandardEventCullingLogic` runs in `onSpawn`, after
  `super.onSpawn` ran the `runnablesOnSpawn` entries. So the cullable of this spawn is never a listener (the room cull
  never acts), and on a reuse the cullable of the last spawn is added and never removed (one leaked listener per
  reuse). A fix turns the cull on, which changes behavior. A `TODO` is at `LightSourceEntity`.
- `Block` with `cull_room` had the same leak (one listener per reuse). This branch fixed it in `Block`
  (`CONTRACT.md`, "Intended changes").
- `PreciousTron.setNextPosition`: the check `it != currentPosition` compared a lambda with a `Vector2`, so it was
  always true and the enemy could pick its current spot. Kept as it was; the dead check is gone.
- `DemonMet`: `target.set(targetsPQ.poll())` throws a null pointer error when the map object has no `target*`
  property.
- `SwingingAxe` and `SwingingPlatform` added 3 more debug shape suppliers on each spawn (a growing list). Fixed here.
