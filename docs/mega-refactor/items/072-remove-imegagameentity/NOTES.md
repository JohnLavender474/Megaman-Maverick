# Notes: 072 Remove `IMegaGameEntity`

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-28: research (read-only agent, `master` at `97771d127`)


Read-only research, 2026-10-28, `master` at `97771d127` (after `064b`). Short forms: `E/` =
`engine/src/main/kotlin/com/maverick/game/engine/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. Tags:
[code] read in the repo, [inferred] reasoned, [training] general knowledge, not checked.

Owner's question (2026-10-28, verbatim): "see if it's advisable to get rid of `IMegaGameEntity` and instead to tie
the funcs/fields to `IGameEntity` or a system/service/func/etc. This way, we can get rid of maintaining multiple
interfaces to describe an entity."

### Short answer

Not now. `IMegaGameEntity` is small (31 lines) and has a real job: it is the one type that says "a game entity has
`game`, a tag and an `EntityType`". The engine cannot hold two of the three (`MegamanMaverickGame`, `EntityType`), and
it has no caller for the third (the tag). A removal now trades one interface for casts to `MegaGameEntity` at the
places that hold a contract (the cast class that `063a` removed), or for a hidden cast inside an extension. The
interface becomes cheap to remove after `016` (narrow services), when entities no longer read `game` through it. So:
keep it as the game-layer entity type (option a), shrink it where a change has a caller, and decide again at the end
of `016`.

### 1. What `IMegaGameEntity` declares and provides [code]

File `C/entities/contracts/IMegaGameEntity.kt` (31 lines):

| Line | Member | Kind | Game content or generic |
|---|---|---|---|
| `:11` | `IGameEntity` | supertype (engine) | generic |
| `:11` | `ITaggable` (`fun getTag(): String`, `E/common/interfaces/ITaggable.kt`) | supertype (engine) | generic. The engine has no caller of `getTag()` on an entity |
| `:11` | `ITypable<EntityType>` (`E/common/interfaces/ITypable.kt`) | supertype, type argument from core | `ITypable` is generic; `EntityType` (`C/entities/EntityType.kt`, 14 values, each a core package suffix for Tiled class lookup) is game content |
| `:13` | `val game: MegamanMaverickGame` | abstract property | game content |
| `:15` | `fun getGameCamera() = game.getGameCamera()` | default method | game (`RotatableCamera`) |
| `:17` | `fun playSoundNow(soundKey, loop) = game.audioMan.playSound(...)` | default method | game (`MegaAudioManager`) |
| `:19` | `fun stopSoundNow(soundKey) = game.audioMan.stopSound(...)` | default method | game |
| `:22-23` | `val IMegaGameEntity.players get() = game.players` | extension | game (`MegaPlayers`) |
| `:25-28` | `val IMegaGameEntity.targetPlayer: MegaGameEntity` (`game.players.targetFor(this)`), with a `TODO` (item `047`) | extension | game; `029` decided it must not stay on every entity |
| `:30-31` | `fun IBodyEntity.overlapsGameCamera() = (this as MegaGameEntity).game...` | extension with an `IBodyEntity` receiver and a hidden cast | game |

`getTag()` has no default on the interface. `MegaGameEntity.kt:75` gives `this::class.simpleName ?: "?"`. 147 classes
override it with `= TAG`, and in 9 of them `TAG` is not the class name (for example `LightSourceEntity` has
`"LightSource"`, `MegaPreciousCluster` has `"PreciousCluster"`, `BigAssMaverickRobotHand` has
`"BigAssMaverickRobot/Hand"`). So the tag is per-class data, not a derived value. `getType()` is abstract: 176
overrides in `core` main (including `IProjectileEntity.kt:37`, `AbstractEnemy.kt:374`), 8 in tests.

Only `IGameEntity`, `ITaggable` and `ITypable` are generic. Everything that the interface adds by itself is game
content.

### 2. Uses of `IMegaGameEntity` [code]

Direct name references (not imports), `core` main: 18 lines. `engine` and `lwjgl3`: 0.

| Kind | Count | Where | What it needs |
|---|---|---|---|
| Supertype of a contract | 13 | `IBossListener.kt:3`, `IFireableEntity.kt:3`, `IFireEntity.kt:3`, `IFreezableEntity.kt:3`, `IFreezerEntity.kt:3`, `IGateTrigger.kt:3`, `ILightSource.kt:6`, `IOwnable.kt:3`, `IScalableGravityEntity.kt:8`, `ITeleporterEntity.kt:5`, `ItemEntity.kt:4`, `IProjectileEntity.kt:34` (and `IHazard : IBossListener`, `IHazard.kt:3`) | see below |
| Supertype of a class | 1 | `MegaGameEntity.kt:16` | — |
| Parameter type | 2 | `MegaPlayers.kt:30` (`targetFor(seeker)`, parameter unused, `@Suppress`), `LightSourceUtils.kt:51` (`sendLightSourceEvent(entity, ...)`, reads `entity.game` at `:56`) | `game` (1) |
| Extension receiver | 2 | `IMegaGameEntity.kt:22,27` | `game` |
| Field, return type, `is`/`as` | 0 | — | — |
| Tests | 1 | `EntityContractsTest.kt:26-27` (each of the 12 contracts is a subtype of `IMegaGameEntity`) | — |

Import lines of the type: 2 (`MegaPlayers.kt:10`, `LightSourceUtils.kt:15`).

What the contracts need from `IMegaGameEntity` (as opposed to `IGameEntity`):

- `063a` `SCAN.md:26-27`: "Two need it (`game`): `IFreezableEntity`, `ILightSource`. Use `IMegaGameEntity` for all,
  for one rule." Confirmed: `FreezableEntityHandler.kt:53` (`ShieldEntity(entity.game)`, `entity: IFreezableEntity`,
  `:19`), and `LightSourceUtils.kt:42-56` (`ILightSource` passed to the `IMegaGameEntity` overload).
- `IProjectileEntity.kt:37` overrides `getType()` (so it needs `ITypable<EntityType>` in its supertypes). The value
  `EntityType.PROJECTILE` reaches 5 classes that do not extend `AbstractProjectile` (`GroundPebble`, `DarkSerketClaw`,
  `SealionBall`, `PreciousGem`; `BulletBill` overrides it back to `ENEMY` at `BulletBill.kt:178`). The type feeds
  `MegaGameEntities` (`:21,27`), `MegamanDamageNegotiator.kt:48`, `Water.kt:151`, `PolygonWater.kt:100`. Gameplay
  depends on it.
- `TimberWoman.kt:931` (`projectile.getTag()`, `projectile: IProjectileEntity`) needs `ITaggable`.
- No contract default method reads `game`, `getGameCamera()`, `playSoundNow()`, `players` or `targetPlayer`.
  `IBossListener.kt:6` calls `destroy()`, which is on `IGameEntity`.

Use counts of the members and extensions (`core` main; tests use none of them except `getTag`/`getType`):

| Member | Uses | Receivers |
|---|---|---|
| `game` (any `game.` that is not a package name) | 1359 in 467 files; 887 in 419 files under `entities/` | Almost all implicit `this` in a `MegaGameEntity` subclass (the constructor property `MegaGameEntity(override val game)`). Reads through another entity reference: about 10 sites: `IBodyExtensions.kt:19,33` (`getEntity()`, a `MegaGameEntity`), `AbstractEnemy.kt:104` (`it: AbstractEnemy`), `AnimatedBlock.kt:119`, `GameEntityUtils.kt:51,79`, `FreezableEntityHandler.kt:53`, `LightSourceUtils.kt:56`, `IMegaGameEntity.kt:31`. Only the last 3 go through an interface type |
| `targetPlayer` | about 483 uses in 147 files (144 import lines) | all implicit `this` in a `MegaGameEntity` subclass, plus `AbstractEnemy.kt:104,110` (`it: AbstractEnemy`) and `FacingUtils.kt:23` (`entity: MegaGameEntity`) |
| `players` (extension) | 64 bare uses in 46 files (41 import lines); `game.players` has 96 more | implicit `this` |
| `getGameCamera()` | 6 bare calls + 2 through a cast (`GameEntityUtils.kt:58,64`); `game.getGameCamera()` has 74 | `MegaGameEntity` |
| `playSoundNow` / `stopSoundNow` | 42 calls in 36 files | implicit `this` |
| `overlapsGameCamera()` | about 92 calls in 78 files | `IBodyEntity`; 2 with a cast to a concrete class (`PetitDevil.kt:122,285`) |
| `getTag()` (not overrides) | about 40 non-log reads | mostly `this` (atlas keys in `HealthBulb`, `Screw`, `WeaponEnergyBulb`; `MegaGameEntities.getOfTag(getTag())`), and through `getEntity()` (`Force.kt:68`, `MoonScythe.kt:174`) |
| `getType()` (entity, not fixture) | about 12 reads | `MegaGameEntity` (via `getEntity()`) except `TimberWoman.kt:931`-style contract reads; `MegamanDamageNegotiator.kt:36` still casts `damager as MegaGameEntity` for `getTag()`/`getType()` |

Conclusion: the interface type is used directly in only 4 places outside its own file. Most code uses
`MegaGameEntity` (308 lines in 221 files) or `IGameEntity` (125 lines in `core` main). The members are used a lot,
but almost always on `this` inside a `MegaGameEntity` subclass, where the class would supply them anyway.

### 3. What `MegaGameEntity` adds; implementers outside it [code]

`C/entities/contracts/MegaGameEntity.kt` (79 lines) adds state and lifecycle, which an interface cannot hold:

- `game` as a constructor property (`:16`), and `GameEntity(game.engine)`.
- `runnablesOnSpawn`, `runnablesOnDestroy`, `runnablesOnDestroyQueued` (`:22-25`), `timeSpawned` (`:27-28`).
- `canSpawn` rules: the level override and the difficulty flags (`:30-43`).
- `onSpawn`: `MapObjectComponent` from `ConstKeys.ID`, `GroupsComponent` from `ConstKeys.GROUP`, `MegaGameEntities.add`,
  the spawn runnables (`:45-61`). `onDestroy`: `MegaGameEntities.remove`, the destroy runnables (`:69-73`).
- The default `getTag()` (`:75`) and `toString()` (`:77-78`).

Implementers of `IMegaGameEntity` that do not extend `MegaGameEntity`: none in production (165 direct subclasses,
about 452 entity classes take `game: MegamanMaverickGame`). Tests: none. Test entities either extend `MegaGameEntity`
(`MegaPlayersTest.kt:27`, `LaserIgnoreTest.kt:23`, `SpawnerReleaseTest.kt:20`, `EntityQueriesTest.kt:33`,
`MapObjectComponentTest.kt:19`, `EntityExistenceTest.kt:16`) or extend the engine `GameEntity` with a relaxed mock
engine (`PlayPhasesTest.kt:11`, `TeleportsTest.kt:14`, `MegaLocksTest.kt:16`, `EntityQueriesTest.kt:28`). Two
strict interface mocks implement it indirectly: `mockk<ITeleporterEntity>()` (`TeleportsTest.kt:31`) and
`mockk<IDamagerEntity>()` (`MegaPlayersTest.kt:105`, engine type, not `IMegaGameEntity`).

So `IMegaGameEntity` and `MegaGameEntity` always go together. The interface exists so that interfaces (the
contracts) can say "game entity"; a Kotlin interface cannot extend a class.

Note: the core tests that use `GameEntity(mockk(relaxed = true))` show the direction that `064` and `029` took: game
state as a component plus an extension on `IGameEntity` (`IGameEntity.mapObjectId` in `C/entities/state/MapObjects.kt:5`,
`playPhase`, `teleporting`, `focus`, `healthRatio` in `C/entities/EntityQueries.kt`). These need no
`MegamanMaverickGame` mock.

### 4. `game` through the interface, and item `016` [code]

- Through the interface type, `game` is read at 3 places: `FreezableEntityHandler.kt:53`, `LightSourceUtils.kt:56`,
  `IMegaGameEntity.kt:23,28` (the two extensions). `IMegaGameEntity.kt:31` reads it after a cast.
- What those reads want: `ShieldEntity(game)` (a constructor), `game.eventsMan` (events), `game.players` (players
  service), `game.getGameCamera()`, `game.audioMan`. All are services.
- `016` (planned, no branch): "Entities and systems take small services (for example `LevelContext`) instead of the
  whole `MegamanMaverickGame`"; `LevelContext` with level, rooms, world container, players, random, texture cache;
  "Entities never take the host contract (`001b`)". `001` `NOTES.md:122-124`: entities and systems must not depend
  on the host contract; "no game feature adds a member to the host contract. A new need becomes a service." Open
  question in `016`: `LevelContext<out P : IPlayers<out PlayerView>>` with `typealias MegaLevelContext`.
- `029` decision (2026-10-23): `targetFor` and `targetPlayer` must not stay global; targeting belongs to the entities
  that need it (`TargetingComponent` or policy), item `047`. So `IMegaGameEntity.targetPlayer` (`:27`) goes in any
  case, with or without the interface.

Consequence: after `016`, `val game` either leaves the entity, or it stays as a constructor value on
`MegaGameEntity` only (each entity class still needs its services at construction; `MegaEntityFactory` calls the
`(game)` constructor through reflection). In both cases no contract needs `game` through an interface type, and
`IMegaGameEntity` holds only `getTag()` and `getType()`.

### 5. Options

### (a) Keep `IMegaGameEntity` as the game-layer entity type

Contracts stay `: IMegaGameEntity` (`063a`). Optional trims, each with a caller:

- Move `getGameCamera`, `playSoundNow`, `stopSoundNow` (`:15-19`) from the interface to `MegaGameEntity`, or leave
  them to `016`, which replaces them with the camera and audio services. All 50 callers are `MegaGameEntity`
  subclasses or cast to it, so the move does not change a call site (except `GameEntityUtils.kt:58,64`, which
  already cast to `MegaGameEntity`).
- Remove the hidden cast in `overlapsGameCamera` (`:30-31`):

  ```kotlin
  // before
  fun IBodyEntity.overlapsGameCamera() =
      (this as MegaGameEntity).game.getGameCamera().getRotatedBounds().overlaps(body.getBounds())
  // after (callers do not change: every receiver is an IMegaGameEntity and an IBodyEntity)
  fun <T> T.overlapsGameCamera() where T : IMegaGameEntity, T : IBodyEntity =
      game.getGameCamera().getRotatedBounds().overlaps(body.getBounds())
  ```

  [inferred] Must be checked by compile: a receiver typed only as `IBodyEntity` would stop compiling. The 2
  `PetitDevil` receivers are concrete classes.

- Pros: no churn; contracts keep compile-time access to `game`, tag and type; the engine/core split stays clean
  (the interface is in `core`, the engine sees only `IGameEntity`); matches the `063a` rule "one rule for all core
  contracts"; `EntityContractsTest` stays as it is.
- Cons: two core entity types to keep in step (`IMegaGameEntity` + `MegaGameEntity`), and the interface carries
  `game`, which `016` wants out of entities.
- Size: 0; trims 1-2 files, under 15 lines.

### (b) Generic parts to `IGameEntity`; the rest as extensions, components or services

What can go to `E/entities/IGameEntity.kt`:

- `ITaggable`: genre-neutral, but the engine has no caller of `getTag()` on an entity (`ITaggable` has 1 user,
  `IMegaGameEntity.kt:11`). The rule "write code only when production code calls it" (`docs/mega-refactor/CLAUDE.md`)
  argues against it. A default `getTag() = this::class.simpleName` would also change nothing for the 147 overrides.
- `ITypable<EntityType>`: no. `EntityType` is core content. `ITypable<Any>` would lose the type, and a type
  parameter on `IGameEntity` is option (d1).
- `game`: no. The engine cannot name `MegamanMaverickGame`; `001b` plans `IMaverickGame`, but `001` and `016` say that
  entities must not take the host contract.

What the rest becomes:

| Member | As | Caller writes | Cost |
|---|---|---|---|
| `game` | a core component (`GameRefComponent(game)`) + `val IGameEntity.game` | `entity.game` (same) | a map lookup per read on about 1359 reads (most on `this`, many per frame), or callers keep the class property and only contract holders use the extension; a component that holds the whole game is the god-object that `016` removes |
| `game` | `val IGameEntity.game get() = (this as MegaGameEntity).game` | same | a hidden cast; `071` wants to remove this pattern (`IFixtureExtensions.kt:17`, `IBodyExtensions.kt:65`) |
| `game` | a global (`MegamanMaverickGame.instance`) | `Game.instance` | global state, which the mega refactor removes (`game.megaman` removal in `029`) |
| tag | a `TagComponent(TAG)` or a property | `entity.tag` | 147 classes add a component; reads on hot paths get a lookup (`MegaGameEntities.add/remove`, `MegamanDamageNegotiator`, block filters) |
| type | an `EntityTypeComponent` | `entity.entityType` | 176 overrides change; `048` already plans to replace type checks with capabilities ("Code that checks `EntityType` ... checks a capability instead") |
| `players`, `targetPlayer` | `game.players` / a targeting component (`029`, `047`) | `game.players.targetFor(this)` | already planned |

- Pros: one entity interface in name.
- Cons: none of the parts has an engine caller; the `game` part has no good form before `016`; tag and type as
  components cost a lookup each and touch 300+ classes for no behavior gain; `048` will change the type checks anyway.
- Size: largest of all options (hundreds of files). Risk: tag and type values feed lookups and damage numbers, so
  every value must stay identical.

### (c) Contracts extend `IGameEntity` directly; remove `IMegaGameEntity`

`MegaGameEntity` takes over the supertypes: `abstract class MegaGameEntity(val game: MegamanMaverickGame) :
GameEntity(game.engine), ITaggable, ITypable<EntityType>`. The 12 contracts change `: IMegaGameEntity` to
`: IGameEntity`.

Changes needed (each site from section 2):

```kotlin
// IProjectileEntity.kt:34-37: keeps its getType() override, so it names ITypable itself
interface IProjectileEntity : IGameEntity, ITypable<EntityType>, ITaggable, IBodyEntity, ...

// IMegaGameEntity.kt:22-28: receiver becomes the class (all receivers are MegaGameEntity today)
val MegaGameEntity.players get() = game.players
val MegaGameEntity.targetPlayer: MegaGameEntity get() = game.players.targetFor(this)

// MegaPlayers.kt:30
fun targetFor(seeker: IGameEntity): MegaGameEntity

// FreezableEntityHandler.kt:53, before / after (one of)
private val shieldEntity = ShieldEntity(entity.game)
private val shieldEntity = ShieldEntity((entity as MegaGameEntity).game)      // cast comes back
class FreezableEntityHandler(private val entity: IFreezableEntity, game: MegamanMaverickGame, ...)  // 61 callers add `game`

// LightSourceUtils.kt:50-56: parameter becomes MegaGameEntity; the ILightSource overload (:42) needs a cast,
// or the function takes the events manager: sendLightSourceEvent(eventsMan, owner, keys, center, radiance, radius)
```

- `TimberWoman.kt:931`: `fixture.getEntity().getTag()` (already a `MegaGameEntity`), so no cast.
- `EntityContractsTest.kt:26-27`: asserts `IGameEntity` in place of `IMegaGameEntity` (it then tests the same thing as
  the `IDamageableEntity` part of the same file).
- Pros: one interface less in `core`; contracts say only "engine entity", which is the minimum they need.
- Cons: the 2 contracts that need `game` (and the `ILightSource` overload) get a cast back or a wider signature; a
  contract holder that wants the tag or type must cast to `MegaGameEntity` (as `MegamanDamageNegotiator.kt:36` does
  today); `MegaGameEntity` becomes the only "game entity" type, and `071` (typed `getEntity()`) has only the class to
  return. It removes an interface but not a concept: "game entity" still exists, as a class.
- Size: about 20 files, about 40 lines; type-only, no gameplay change if `getType()` values stay the same (check
  with `MegaGameEntities` lookups and the damage table). Engine/core split: clean.

### (d) Other options

- **(d1) Type parameter on `IGameEntity`** (`IGameEntity<G>` with `val game: G`, or `<T>` for the type): no. It
  spreads to every holder: 125 `IGameEntity` lines in `core` main and every engine system, query and collection
  (`GameEngine` queues, `GameSystem` members). The engine would carry a host type that `001`/`016` say entities must
  not hold. Kotlin has no default type arguments, so a `typealias` would be needed in every module.
- **(d2) `typealias MegaEntity = MegaGameEntity` or `= IGameEntity`**: a name, not a type. It cannot add `game` to
  `IGameEntity`. It does not remove a type to maintain. Not useful.
- **(d3) Merge in the other direction: drop `MegaGameEntity`, keep `IMegaGameEntity`**: no. `MegaGameEntity` holds
  state (runnables, `timeSpawned`) and the lifecycle (`canSpawn`, `onSpawn`, `onDestroy`), which an interface cannot
  hold. Its spawn/destroy work (registry, map id, groups, runnables) could move to systems and components (`048`), but
  that is a separate, large item.
- **(d4) Do (a) now, (c) after `016`, and let `048` decide tag and type.** After `016`, `game` is no longer read
  through a contract, so (c) loses its only cost (the 2-3 casts). If `048` turns the type checks into capability
  checks, `getType()` also loses its readers, and (c) shrinks to tag only. This is the cheapest path to the owner's
  goal.

### 6. Other engines [training]

- **Unity.** `GameObject` is sealed. Game code adds `MonoBehaviour` components. Many projects add their own base
  `MonoBehaviour` (for example `Entity : MonoBehaviour`) with project state; services come from singletons, a
  locator, or dependency injection. Interfaces (for example `IDamageable`) are on components and found with
  `GetComponent<IDamageable>()`; they do not extend `GameObject`.
- **Unreal.** Games subclass `AActor` (`AMyGameActor`, `AMyCharacter`). The game is reached through the world, with a
  cast at the point of use (`GetWorld()->GetGameInstance<UMyGameInstance>()`, `GetGameMode()`). `UInterface`s cannot
  extend `AActor`; code tests `Implements<UMyInterface>()` and casts.
- **Godot.** Scripts extend a node class (`class_name Enemy extends CharacterBody2D`). Global services are autoload
  singletons or reached through `get_tree()`. "Type" is the script class or a group.
- **Bevy / EnTT / flecs.** No entity base type. Game data (the "type", a name) are marker or data components; game
  services are resources (`Res<T>`), passed to systems, not stored on entities.

What helps the decision: object-style engines keep a game-layer base class (the role of `MegaGameEntity`), and their
interfaces do not carry the host. None puts the whole game on every entity through an interface; they reach services
through the world or a context. So the problem is `val game` on the interface (solved by `016`), not the existence of
a game-layer entity type. ECS engines would make tag and type components, which is the `048` direction.

### 7. Recommendation

Keep `IMegaGameEntity` now (option a). Do not do (b). Do (c) only after `016` (option d4).

Reasons:

1. The interface is the cheapest form of "game entity" for interfaces: 31 lines, 18 direct references, 0 casts. The
   engine cannot take its game parts, and has no caller for its generic part.
2. A removal now brings back casts or hidden casts (the patterns that `063a` and `071` remove), or adds component
   lookups on hot paths, or adds a global.
3. The member that causes the problem, `game`, is the scope of `016`. `targetPlayer` and `players` are already
   planned to move (`029`, `047`). `getType()` checks are in `048`. When these are done, `IMegaGameEntity` holds
   about 2 members, and (c) is a type-only change of about 20 files.

Order of work, as branches (proposal):

| Branch | Item | Content | Depends on |
|---|---|---|---|
| none now | — | Optionally record the trims of option (a) as backlog: `overlapsGameCamera` with no hidden cast; `getGameCamera`/`playSoundNow`/`stopSoundNow` off the interface (or let `016` do it) | — |
| `016x` | `016` | Narrow services; `FreezableEntityHandler`, `LightSourceUtils` and the extensions take services, not `game` | — |
| `047x` / first targeting entity | `029`/`047` | `targetPlayer` and `players` leave `IMegaGameEntity.kt` | — |
| new item (for example "remove `IMegaGameEntity`"), one branch | new | Option (c): 12 contracts to `: IGameEntity`; `MegaGameEntity : ITaggable, ITypable<EntityType>`; `IProjectileEntity` names `ITypable<EntityType>`, `ITaggable`; delete `IMegaGameEntity.kt`; update `EntityContractsTest` | after `016` |

What must stay true in every branch:

- Gameplay unchanged. In particular every `getType()` and `getTag()` value stays the same, because they key
  `MegaGameEntities` (`MegaGameEntities.kt:20-27`), `MegamanDamageNegotiator.kt:36-48` (damage numbers),
  `Water.kt:151`/`PolygonWater.kt:100` (splash), `MegaLevelScreen.kt:825` (boss defeat), atlas keys
  (`HealthBulb`, `Screw`, `WeaponEnergyBulb`) and filters (`Force.kt:68`, `MoonScythe.kt:174`). `BulletBill` must keep
  `ENEMY` over the `IProjectileEntity` default `PROJECTILE`.
- `EntityContractsTest` (`core/src/test/.../entities/contracts/EntityContractsTest.kt`) and
  `EngineEntityContractsTest` (`engine/src/test/.../entities/EngineEntityContractsTest.kt`) stay green. If (c) is done,
  `EntityContractsTest:26-27` changes its assertion to `IGameEntity`, and a check that `MegaGameEntity` is
  `ITaggable` and `ITypable` can replace it.
- The engine gets no Megaman Maverick content (`EntityType`, `MegamanMaverickGame`), and no API without an engine
  caller.
- `./gradlew engine:test core:test lwjgl3:jar` passes; the `TeleportsTest` strict `mockk<ITeleporterEntity>()` still
  compiles.

### Open questions for the owner

- Is the goal "fewer interface files" or "no `game` on entities"? The first is (c), and it costs casts now. The second
  is `016`, and it makes (c) free later.
- Do the 3 audio/camera default methods go now (small trim) or wait for `016`?
