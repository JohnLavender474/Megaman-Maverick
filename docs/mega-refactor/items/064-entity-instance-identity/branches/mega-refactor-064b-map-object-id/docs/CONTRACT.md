# Contract: mega-refactor-064b-map-object-id

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-28

What must stay the same: everything, except the intended change at the end. The branch moves the map object id from
the field `MegaGameEntity.id` (a map id, or a random negative id) to a `MapObjectComponent` that only map-spawned
entities have. These must give the same results at the same moments as on `master`:

- the registry lookups `MegaGameEntities.getOfId`/`existsAnyOfId` for every map id (`InfernoMan` meteor floor,
  `TorikoPlundge`, `MockingByte` nest, `BreakableBlock` connected blocks);
- each class 1 reader that keys one map object by its own id (`AstroAssAssaulter` flags, `BikerKibbo`/`Jetto` respawn
  delay, koopa shells, `Starkner` `ALIVE` and successors, `SteamRollerMan` and its roller, `MockingByte` nest);
- each class 2 reader that checks a hit entity against a Tiled id list (`RainDrop`, `Bat`, `BulletBill`,
  `FallingIcicle`, `FireWall`, `Fireball`, `SpitFireball`, `SmallIceCube`, `TubeBeamerV2`, `Laser`);
- `Laser`: the owner and the reflector are ignored as today (by reference, plus the owner's and the reflector's map
  ids; the reflector's map id accepted by the owner, 2026-10-28).

`DarknessV3` is deleted: no map, factory or other code uses it, so no check plays it.

## Automated checks

All on a clean build: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
lwjgl3:jar`.

- [x] The build passes and the jar builds.
- [x] All engine and core tests pass, 0 failures. The counts go up only by the new tests (baseline counted at plan
  step 1, and again after `master` is merged into the branch).
- [x] The compiler warnings are the same set as the baseline (unique `^w:` lines without line and column). Each new
  warning is listed in the process log with a reason, or fixed.
- [x] **Temporary contract suite** `Temp064bMapIdEquivalenceTest` (core test sources), with the frozen copy
  `Temp064bFrozenIdRegistry` of the old id logic of `MegaGameEntity` and `MegaGameEntities` (`RANDOM_ID_POOL` with the
  eager fetch, the free in `onDestroy`, the old `idToEntities`; the test entity keeps an old-style id field and feeds
  the frozen copy at the old points). On seeded random sequences (at least 200 seeds, each with at least 200
  operations) on a real `GameEngine`: spawn with a map id and with none, several entities with one map id (the
  `Starkner` successor and `SteamRollerMan` child pattern), destroy, a destroy and a spawn in one frame, a respawn of a
  pooled object with another map id or with none, `engine.reset()`. For every map id used: the ordered set of
  `getOfId` and the result of `existsAnyOfId` are equal to the frozen copy, (a) from a probe during an update and
  (b) after each operation, between updates. Also: no entity spawned without a map id is in the new index.
- [x] **Temporary contract suite** `Temp064bLaserIgnoreEquivalenceTest` (core test sources), with the frozen copy
  `Temp064bFrozenLaserIgnore` of the old `Laser` ignore rule (`ignoring: ObjectSet<Int>` with owner id, reflector id and Tiled ids; frozen random ids).
  Seeded sequences of an owner (map-spawned, as every live `LaserBeamer`, and runtime), reflectors (map, `Megaman`-like
  with a player spawn id, runtime), and other entities: spawn, destroy, pooled respawn, reuse of a freed random value,
  shared map ids, reflector changes and resets. The ignore result for every candidate entity is equal to the frozen
  rule whenever the owner and the current reflector are live in the spawn that was added. The window cases (owner or
  reflector destroyed while the laser still holds it) are counted, and every difference is in a window case.
- [x] **Permanent core tests** (for example `MapObjectComponentTest`):
  - a spawn with `ConstKeys.ID` adds a `MapObjectComponent` with that id; a spawn without it adds none; a pooled
    object that spawns again without `ConstKeys.ID` has no component; with another id, the component has the new id;
  - the id is still readable in `onDestroy` (after the engine `reset()` of the components);
  - `getOfId` returns the map entities of an id in spawn order (shared ids give a set), drops them at destroy, and never
    returns a runtime entity;
  - `Laser.isLaserIgnoring`: true for the owner, for the reflector, for an entity with the owner's or the reflector's
    map id, for an entity whose map id is in a Tiled `ignore*` list; false for a runtime entity that is neither owner nor reflector.
- [x] Existing tests stay green with no change: `StarknerAliveTest`; after `master` is merged in,
  `SpiderWebDestroyTest` and `OnDestroySuperCallTest`.
- [x] Greps: no `RANDOM_ID_POOL`, `CURRENT_NEW_ID` or `var id` in `MegaGameEntity`; no `as MegaGameEntity).id` in
  `core/`; no `.id` read of a `MegaGameEntity` (the compiler proves this: the field does not exist); no
  `DarknessV3` in `core/` or `assets/`.
- [x] `core/src/test/resources/contact-rule-selection.txt` is unchanged.

## Manual playthrough

Passed (owner, 2026-10-28: "All tests pass"; the `RODENT_MAN` `room1` check was run with `--weapons MOON_SCYTHES`).

The owner plays these once, after the implementation, from the worktree. Each room has a player spawn.

Registry lookups by map id:

- `./run-game-alpha.sh --level DESERT_MAN --room room3`: the `TorikoPlundge` enemies in `room3` and `room4` spawn only
  while their blocks exist, fall when Megaman is near, and break their crumbling blocks; when their blocks are gone they
  die (explode or vanish) as on `master`. Leave the screen and come back: they spawn again only if a block is left.
- `./run-game-alpha.sh --level RODENT_MAN --room room1`: with the Moon Scythe weapon (only moon scythes break these
  blocks), break one of the three connected `BreakableBlock`s in `room1`: the blocks connected to it break at the same
  time (a chain).
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room13`: the three `MockingByte`s wake, fly out of their nests,
  attack, and go back to a nest (their own, or the nearest free one if theirs is gone) and sleep again.
- `./run-game-alpha.sh --level INFERNO_MAN --room room11`: go on to the boss and fight it. The meteors of the boss fall
  from the top and stop on the floor of the room (they do not fall through it).

Own-id keys of one map object:

- `./run-game-alpha.sh --level MOON_MAN --room room12`: the `Starkner` stars: hit one; it splits into the next star
  (blue, yellow, and so on) at the same place; while a star of a map object (or one of its successors) lives, no new
  star of that map object spawns when the screen moves away and back. When all stars of an object are dead, it spawns
  again after the screen leaves and comes back.
- `./run-game-alpha.sh --level MOON_MAN --room room5`: each `AstroAssAssaulter` throws one flag; it throws no second
  flag while its flag lives, also after the assaulter dies and spawns again; after the flag is gone it can throw again.
- `./run-game-alpha.sh --level INTRO_STAGE --room room6`: `SteamRollerMan` with its `SteamRoller`; it does not spawn a
  second time while its roller exists (scroll away and back during the fight).
- `./run-game-alpha.sh --level INTRO_STAGE --room room2`: kill the `BikerKibbo`, scroll it off screen and back at once:
  it does not spawn again for the respawn delay, then spawns again.
- `./run-game-alpha.sh --level DESERT_MAN --room room2`: the same respawn delay for the `Jetto` enemies, and only one
  `Jetto` at a time.
- `./run-game-alpha.sh --level SMB3_1 --room room2` (the only player spawn of the level): kill a `GreenKoopa` and a
  `RedKoopa` so that each leaves a shell; while the shell lives, the koopa does not spawn again when the screen moves
  away and back; after the shell is gone it spawns again. The `BulletBillLauncher`s shoot bullets that pass their
  ignored block and explode on other blocks. The `FireFlower` shoots fireballs as on `master` (see "Intended changes").

Tiled ignore lists (a hit entity against map ids):

- `./run-game-alpha.sh --level INTRO_STAGE --room room3`: go on to `room5` (the room with the second `RainFall`,
  which has two ignored objects). Rain drops splash on blocks and bodies, but not on the two ignored objects.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room1`: the `Bat`s in `room2` and `room3` hang on ceilings, but do
  not hang on their ignored blocks.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: the `FallingIcicle`s in `room1` fall, pass their ignored
  block, and shatter on the next block or on Megaman.
- `./run-game-alpha.sh --level GLACIER_MAN --room room6`: the two `IceCubeMaker`s make ice cubes that are not broken by
  their ignored blocks, and break on other blocks and on Megaman.
- `./run-game-alpha.sh --level INFERNO_MAN --room room1`: the `FireDispensenator`s throw fire walls that pass their
  ignored block and stop at other blocks.
- `./run-game-alpha.sh --level INFERNO_MAN --room room9`: the `MechaDragon` spits fireballs that burst only on its
  listed blocks.
- `./run-game-alpha.sh --level REACTOR_MAN --room room7`: go on through `room8` and `room9`. Each `TubeBeamerV2` beam
  passes through its ignored object and stops at other blocks as on `master`.
- `./run-game-alpha.sh --level REACTOR_MAN --room room10`: the same in `room10` and `room11` (5 beamers each).

`Laser` owner and reflector:

- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room2`: the two `LaserBeamer`s rotate their beams; a beam does not
  stop at its own beamer, passes its ignored object, and stops at other blocks. If Megaman has a shield weapon (the axe
  shield), hold it against a beam: the beam reflects off the shield and the reflected beam does not stop at Megaman.
  Go on to `room3`, `room5` and `room6` (more beamers).
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room3`: the same for the `LaserBeamer`s in `room3` and `room4`.

Sanity pass:

- `./run-game-alpha.sh --level MOON_MAN --room room6`: play from `room6` to `room10` with enemies, shots, item drops,
  enemy deaths, portals and room transitions. Die once and respawn. Then leave the level from the pause menu and start
  `MOON_MAN` again (`engine.reset()`). Nothing is missing, frozen, duplicated, or invisible; no crash.

## Behaviors to check by review

- `MegaGameEntity.onSpawn` writes or removes the component before `MegaGameEntities.add`, at the place where `id` was
  set; `MapObjectComponent.reset()` keeps the id.
- `MegaGameEntities.add` and `remove` read the same component value (only `onSpawn` writes it).
- Each class 2 reader checks "has a map id and the list holds it"; each class 1 own-id reader uses `mapObjectId!!` and
  can only spawn with `ConstKeys.ID`.
- `Laser` copies both ignore sets to its reflecting laser and clears both where `ignoring` was cleared.
- `MegaGameEntity.onDestroy` still ends with the same calls (minus the pool free), and every changed `onDestroy()` keeps
  `super.onDestroy()`.

## Intended changes

- `FireFlower` with no `block` property (`SMB3_1` object `117`): on `master` its fireballs ignore any entity that holds
  the random id `-1` at that moment (the sentinel value `-1` is in the random id space). Now `-1` matches no entity, so
  the fireballs ignore nothing. In practice no live entity was found that holds `-1` (the value is taken by about the
  10th spawn of the session and most likely leaks to a map entity), so no visible change is expected. Accepted by the
  owner (2026-10-28).
- `Laser`: in the short window after its owner or its reflector is destroyed and before the laser refreshes its
  reflector or dies, the ignore result can differ (old: by the freed random id; new: by the object). For a
  `LaserBeamer` owner the window ends at the next `engine.update()`. Accepted with the exact option and as an intended change (owner, 2026-10-28: "064b contract approved");
  the temporary suite counts these cases.
- Log text: `toString` and debug lines print the map id (or `null`) in place of a random negative id.
- `DarknessV3` is deleted (dead code, no level uses it).
