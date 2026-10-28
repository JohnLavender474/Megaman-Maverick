# Scan: `064b` (the map object id as a `core` component)

Base: `master` at `3f7329489`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `E/` =
`engine/src/main/kotlin/com/maverick/game/engine/`. Line numbers are on the base.

Method: the reader list in sections 3 to 6 comes from the compiler. The field `id` in `MegaGameEntity` was renamed
for one local build (`core:compileKotlin`, reverted at once). Each `Unresolved reference 'id'` is one read or write of
the field. Then `grep` found every `getOfId`, `existsAnyOfId`, `RANDOM_ID_POOL` and `ConstKeys.ID`. No test, `engine`
or `lwjgl3` code reads the field.

## 1. Where `id` lives today

- `C/entities/contracts/MegaGameEntity.kt:32-33`: `var id = 0` with a `private set`.
- `:20-24`: `CURRENT_NEW_ID = -1` and `RANDOM_ID_POOL`, a `Pool<Int>` (engine `Pool`). The pool starts with 10 values
  (`-1` to `-10`); `fetch` takes the last value that was freed (LIFO), or makes the next lower value.
- `onSpawn` (`:53`): `id = spawnProps.getOrDefault(ConstKeys.ID, RANDOM_ID_POOL.fetch(), Int::class)`. Then (as
  before `064a`) groups, `timeSpawned`, `MegaGameEntities.add(this)`, `runnablesOnSpawn`.
- `onDestroy` (`:71-73`): `MegaGameEntities.remove(this)`, `runnablesOnDestroy`, then `if (id < 0)
  RANDOM_ID_POOL.free(id)`.
- `toString` (`:79-80`) prints `id`.

Facts:

- The default argument is evaluated before the call, so **every** spawn fetches a pool value, also a spawn with
  `ConstKeys.ID`. A map entity keeps its map id and never frees the fetched value. So each map spawn leaks one random
  value, and `CURRENT_NEW_ID` goes down for the whole session. The values `-1` to `-10` are taken by the first 10
  spawns of the game (the first is `Megaman`, see section 2), and most of them leak to map entities.
- After `destroy()`, the field keeps its last value. A pooled entity gets a new value at its next spawn (the next pool
  value, often the value that the last destroyed entity freed).
- `SpiderWeb.onDestroy` skipped `super.onDestroy()` on the base, so a destroyed web stayed in the registry and never
  freed its value. Fixed on `master` by `fix/spider-web-super-on-destroy` (squash `9d3f67fc5`), after this base.

## 2. Where `ConstKeys.ID` comes from

LibGDX's TMX loader puts `id` (the Tiled object id, an `Int` > 0, unique in one map file) in the `MapProperties` of
every map object. `ConstKeys.ID = "id"` (`C/Constants.kt:154`). So every `Properties` made from a map object
(`toProps()`, `convertToProps()`, `C/utils/extensions/Extensions.kt:54-110`) holds `ConstKeys.ID`. Spawn paths that
give it:

| Path | Entities |
|---|---|
| `SpawnersLayerBuilder.build` (`C/screens/levels/tiled/layers/SpawnersLayerBuilder.kt:66`) | all spawner layers (blocks, enemies, items, hazards, specials, decorations, projectiles) |
| `SensorsLayerBuilder.build` (`.../SensorsLayerBuilder.kt:24`) | sensors, for example `Gate` |
| `MegaLevelScreen` `ENTER_BOSS_ROOM` (`C/screens/levels/MegaLevelScreen.kt:742`) | bosses |
| `PlayerSpawnsManager.currentSpawnProps` (`C/screens/levels/spawns/PlayerSpawnsManager.kt:19`), spawned at `MegaLevelScreen.kt:610` | `Megaman`: its `id` is the id of the current player spawn object |
| `GauntletMaster.spawnWave` (`C/entities/special/GauntletMaster.kt:213`) | wave enemies (own map objects) |
| `convertObjectPropsToEntitySuppliers` (`C/entities/utils/GameEntityUtils.kt:118`) | children of `DisappearingBlocks`, `Togglee`, `RocketPlatform`, `RotationAnchor`, `FixtureTypeOverlapSpawn` (own map objects) |
| `Bospider.kt:297`, `HeartTank.kt:139`, `ReactorMan.kt:260` | children made from own map objects |

Code that passes an id on to another entity:

| Site | Key | What |
|---|---|---|
| `Starkner.kt:239`, `:258`, `:277` | `ConstKeys.ID` | each successor star gets the id of the star that died. The successors share one map id. |
| `SteamRollerMan.kt:189` | `ConstKeys.ID` | its `SteamRoller` child gets the man's map id. |
| `DecoratedGravityForce.kt:62-68` | `ConstKeys.ID` (a copy of all spawn props) | its `GravityChange` and `Force` children get the parent's map id. |
| `CartV2.kt:94` | `ConstKeys.ID` (a copy) | passes it to its own `super.onSpawn`, same entity. |
| `GreenKoopa.kt:181`, `RedKoopa.kt:179` | `parent_id` | the shell stores the koopa's map id in `koopaId`. |
| `AstroAssAssaulter.kt:193` | `parent_id` | the flag stores the assaulter's map id in `parentId`. |

No code puts a negative or a zero value in `ConstKeys.ID`. So today: `id > 0` exactly when the spawn properties hold
`ConstKeys.ID`, and `id < 0` (a random value) otherwise.

## 3. The registry (`C/entities/MegaGameEntities.kt`)

- `idToEntities: OrderedMap<Int, OrderedSet<MegaGameEntity>>`. `add` (`:18`) puts the entity in the set of
  `entity.id`. `remove` (`:27-31`) removes it and drops an empty set. Every entity is indexed, also under a random id.
- `add` runs in `MegaGameEntity.onSpawn`, `remove` in `MegaGameEntity.onDestroy` (after the engine calls `reset()` on
  every component, `E/GameEngine.kt:191-192`).
- `getOfId(id)` (`:62`) returns the live set or a new empty set; `existsAnyOfId(id)` (`:60`).

Callers of `getOfId`/`existsAnyOfId`, with the source of the id that they ask for:

| Site | Asks for | Source of the id | Class |
|---|---|---|---|
| `InfernoMan.kt:736` | the floor `Block` | `COLLIDE` map object (`:237`) | 1 |
| `TorikoPlundge.kt:109` (`canSpawn`) | any block of the `block*` map objects | map objects | 1 |
| `TorikoPlundge.kt:244`, `:252` | its plundges and crumbling blocks still exist | map objects (`:174-178`) | 1 |
| `MockingByte.kt:575` | its nest | `NEST` map object (`:193`), or `nest.id` of a nest found by tag (`:595`) | 1 |
| `BreakableBlock.kt:126` | connected blocks | `CONNECTED` map objects (`:117`) | 1 |
| `DarknessV3.kt:412` | a light source owner still exists | keys from section 6 | 3 |

So every live query asks for a map id (> 0). Only `DarknessV3` asks for a random id, and `DarknessV3` is not used
(section 6).

Tests that use the registry by id: `StarknerAliveTest` (spawns with `ConstKeys.ID`); on `master` after the base,
`SpiderWebDestroyTest` (spawns a web with `ConstKeys.ID pairTo 201` and asserts `getOfId(201)`).

## 4. Readers, class 0: log text only

`Block.kt:66`, `:240`; `FallingLeaves.kt:63` (exception message); `Gate.kt:171`, `:179`, `:189`, `:203`, `:219`,
`:231`, `:246`, `:257`, `:275`, `:287`; `GauntletMaster.kt:136` (exception message); `MockingByte.kt:560`;
`Starkner.kt:190`, `:209`; `MegaGameEntity.kt:80` (`toString`). All of these entities are map-spawned, except that
`toString` is used for every entity.

## 5. Readers, class 1: the entity at a map object, or state of one map object

The reader uses its own id (or a stored copy) as the key of one map object. Each class here can only spawn with
`ConstKeys.ID` in live content: either `canSpawn` reads `spawnProps.get(ConstKeys.ID, Int::class)!!`, or the only
spawn paths are map paths (section 2).

| Site | Use | Spawned without `ConstKeys.ID`? |
|---|---|---|
| `AstroAssAssaulter.kt:174`, `:193`, `:199` | static `FLAGS: OrderedMap<Int, StagedMoonLandingFlag>` keyed by its id: no new flag while the flag of this map object lives (also after the assaulter died and respawned). The flag removes the key (`StagedMoonLandingFlag.kt:164`, `parentId`, default `-1`). | No caller (map only: `MOON_MAN`, `WILY_STAGE_3`). No `!!` guard. |
| `BikerKibbo.kt:132` | `delayNextPossibleSpawn(game, TAG, id, ...)` in `onDestroy` (key `"$TAG/$id"`, `GameEntityUtils.kt:179-193`); `canSpawn` reads the props id. | No (`canSpawn` `!!`). |
| `Jetto.kt:184` | same as `BikerKibbo`. | No (`!!`). |
| `GreenKoopa.kt:181`, `RedKoopa.kt:179` | passes its id as `parent_id`; `canSpawn` refuses while a shell with `koopaId == id` lives. | No (`!!`). |
| `Starkner.kt:188`, `:207`, `:239`, `:258`, `:277` | static `ALIVE: ObjectSet<Int>`; `canSpawn` reads the props id; `onSpawn` adds `id`; `releaseAliveId` (from `onDestroyQueued` and `onDestroy`) removes it; successors get the same id. `onDestroy` reads `id` after the component `reset()`. | No (`!!`). |
| `SteamRollerMan.kt:139`, `:189` | `canSpawn` refuses while a `SteamRoller` with `it.id == id` lives; the roller gets the man's id. | No (`!!`). |
| `MockingByte.kt:595` | `nestId = sortedNests[0].id`, then `getOfId(nestId)`. | `MockingByteNest` is map only (`Test1`, `TimberWoman_16x14_v4`). |
| Registry callers in section 3 (except `DarknessV3`) | find the entity of a map object. | – |

`BikerKibbo`, `Jetto` and `Starkner` read the id in `onDestroy`, which runs after the engine `reset()` of every
component. So the new component must still hold the id in `onDestroy`.

## 6. Readers, class 2: a hit entity against a Tiled-authored id list

The list holds Tiled ids (> 0) from map object references. A hit entity with a random id (< 0) never matches. A hit
entity with a map id matches only if a map object reference names it (or names the map object of the parent that
passed the id on, section 2).

| Site | List and source |
|---|---|
| `Rain.kt:197` (`RainDrop`, `block.id`), `:200` (`(it as MegaGameEntity).id`, cast 1 of 4) | `ignoreIds` from `RainFall` `ignore*` map objects (`:288-290`, `:330`) |
| `Bat.kt:296` | `blocksToIgnore`, `ignore*` map objects (`:191-193`) |
| `BulletBill.kt:99` | `collisionIdsToIgnore`, from `BulletBillLauncher` `ignore*` map objects (`:86-88`, `:157`) |
| `FallingIcicle.kt:136`, `:148` | `ignore`, `ignore*` map objects (`:108-111`) |
| `FireWall.kt:99` | `ignoreBlockSet`, from `FireDispensenator` string property `ignore_blocks` (`:137-139`, `:267`) |
| `Fireball.kt:166`, `:229` | `collisionsToIgnore`, from `ConstKeys.IGNORE`; only caller `FireFlower.kt:264` with `blockId` = the `BLOCK` map object id, **or `-1` when the object has no `block` property** (`:135-140`) |
| `SpitFireball.kt:89` | `blockIds`, from `MechaDragon` `block*` map objects (`:237`, `:792`) |
| `SmallIceCube.kt:195` (cast 2 of 4) | `ignoreHitsFromIds`, from `IceCubeMaker` `ignore_hit*` map objects (`:87-88`, `:113`) |
| `TubeBeamerV2.kt:208` (cast 3 of 4) | `ignoreIds`, `ignore*` map objects (`:168-170`) |
| `Laser.kt:677` (cast 4 of 4) | `ignoring`: `ignore*` map objects (`:209-211`) **and** the class 3 values below |

The `FireFlower` sentinel `-1` is in the random id space. Live content: `SMB3_1` has one `FireFlower` (object `117`)
with no `block` property. So its fireballs ignore any block, shield or body whose random id is `-1` at that moment.
Section 1 shows that `-1` is fetched by about the 10th spawn of the session and is most likely leaked to a map entity,
so in practice no live entity holds `-1`. This is not proved.

## 7. Readers, class 3: a key for runtime entities

### 7a. `Laser` owner and reflector

- `Laser.kt:202-203`: `owner = spawnProps.get(ConstKeys.OWNER)`; `ignoring.add(owner.id)`. The only caller with an
  owner is `LaserBeamer.kt:150-165` (owner = the beamer). `LaserBeamer` is map only (`PRECIOUS_WOMAN` 8,
  `WILY_STAGE_1` 6), so the owner id is its map id. The beamer passes no `ConstKeys.ID` to anything, so no other entity
  has that id. `LaserBeamer.onDestroy` destroys its laser.
- `Laser.kt:585-590`: each update that reflects, the reflecting laser does `ignoring.clear()`,
  `ignoring.addAll(parent.ignoring)`, `ignoring.add(reflector.id)`, `owner = reflector`. The reflector is the entity
  of the `SHIELD` fixture that the beam hits: `Megaman` (its id is the player spawn object id), a map enemy (map id), or
  a runtime entity with a shield (random id). `resetReflectingLaserIfAny` (`:440-449`) clears `ignoring` when the beam
  hits no shield.
- `Laser.kt:677`: `isLaserIgnoring(entity)` = `ignoring.contains(entity.id)` or a list of classes. Called from
  `LaserRules.kt:38`, `:61`, `:86`, `:104` (each contact) and `Laser.shouldHitShield` (`:423`).

Does an entity reference (`owner` and `reflector` in a set of entities, the Tiled ids in the old `Int` set) give the
same result?

| Case | Old (id) | New (reference) | Same? |
|---|---|---|---|
| Owner or reflector live, in the spawn it had when added | ignores entities with its id: only itself (map ids are unique, the beamer passes none on, a random value is held by one live entity) | ignores itself | yes |
| Pooled owner or reflector destroyed and spawned again (same object) while the laser keeps the old value | ignores entities with the old id; the object has a new id (or by chance the same value) | ignores the object | no, but only while the laser outlives the owner (the beamer destroys its laser in `onDestroy`; the destroy runs at the next `engine.update()`) or until the next reflect update replaces the reflector |
| Owner or reflector destroyed; another entity gets its freed random value | ignores the other entity | does not ignore it | no, same windows as above. Not possible for `LaserBeamer` (map id) |
| Map-spawned owner; another entity at the same map object (another pooled object) | ignores it | does not | no, same window |
| Successors that share an id | none for `LaserBeamer`; for a map reflector (`Starkner` has no shield) none found | – | yes in live content |

How to prove it: a temporary suite with a frozen copy of the old id rules (the `RANDOM_ID_POOL` logic including the
eager fetch, and the old `ignoring` set) and the new rules, fed by the same seeded sequence of spawns, destroys,
respawns and reflector changes. It asserts equal results for every check where the owner and the reflector are live
in the spawn that was added, and counts the window cases separately (expected differences, listed in the contract).

### 7b. `DarknessV3`

- `tilesInLightSourceMap: OrderedMap<Int, DarknessTilesLightener>` and `queuedLightSourcesToAdd` are keyed by
  `entity.id` of projectiles and explosions (`:356`), `player.id` (`:380`), and the event `ConstKeys.ID`
  (`:263`). Each update removes a key when `!MegaGameEntities.existsAnyOfId(key)` (`:412`). Its own `key` (`:203`) is
  its map id (class 1).
- `DarknessV3` is not used: no TMX file names it (`grep` over `assets/`), and `SpecialsFactory.kt:49` has it only in a
  comment. Also, no sender of `ADD_LIGHT_SOURCE` puts `ConstKeys.ID` in the event (`LightSourceUtils.kt:56-64` puts
  `ConstKeys.OWNER`), so its `ADD_LIGHT_SOURCE` path throws a `NullPointerException` when a key matches.
- With references: a pooled projectile that respawns is the same key (old: a new random key, the old light stays until
  no entity holds the old value); a freed value taken by another entity no longer keeps an old light. These change only
  the frames where a light fades, in a class that no level uses.

### 7c. The `FireFlower` sentinel

See section 6: `-1` is compared with entity ids. With no random ids, it matches nothing.

## 8. The 4 casts

`Rain.kt:200`, `TubeBeamerV2.kt:208`, `Laser.kt:677`, `SmallIceCube.kt:195`: each casts an `IGameEntity` to
`MegaGameEntity` only to read `id`. `IFixture.getEntity()` (`C/world/body/IFixtureExtensions.kt:17`) already returns
`MegaGameEntity`; that cast is out of scope.

## 9. Existing patterns for a `core` component

`C/entities/state/PlayPhaseComponent.kt` (`var phase`, empty `reset()`) with the extension
`val IGameEntity.playPhase` in `PlayPhases.kt`; `TeleportableComponent`. `GameEntity.addComponent`,
`getComponent`, `removeComponent` (`E/entities/GameEntity.kt:32-45`). No `GameSystem` needs the new component.

## 10. Overlap with other branches

`fix/spider-web-super-on-destroy` (merged on `master` as `9d3f67fc5`, after this base): `SpiderWeb.onDestroy` calls
`super.onDestroy()`; new `OnDestroySuperCallTest` (exempts `MegaGameEntity.kt`) and `SpiderWebDestroyTest` (uses
`ConstKeys.ID` and `MegaGameEntities.getOfId`). Details in `NOTES.md`.
