# Notes: 063 Entity contracts are entity types

Two read-only review agents in the `062c` session (2026-10-27), on the `062c` worktree. `E/` =
`engine/src/main/kotlin/com/maverick/game/engine/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. Tags:
[code] read in the repo, [inferred] reasoned, [training] unchecked general knowledge.

## Review 1: contracts extend `IGameEntity`, rename, or components

### Inventory [code]

| Interface | Module | Extends `IGameEntity`? | Implementers | Non-entity implementer? |
|---|---|---|---|---|
| `IDamageable` | engine `damage/` | no | 159 | none |
| `IDamager` | engine `damage/` | no | 269 | none (one `mockk` in `MegaPlayersTest.kt:105`) |
| `IFreezableEntity` | core | no | 65 (64 write the same `override var frozen` delegate) | none |
| `IFreezerEntity`, `IFireEntity` | core | no | 3, 4 (markers) | none |
| `IFireableEntity`, `IScalableGravityEntity`, `ILightSource` | core | no | 4, 14, 14 | none |
| `IHazard`, `IBossListener`, `IOwnable<T>` | core | no | 49, 122, 107 | none |
| `ITeleporterEntity`, `ItemEntity`, `IGateTrigger` | core | no | 3, 7, 1 | none |
| `IUpsideDownable` | core | no | 0, no uses (dead code) | none |
| `IChildEntity`, `IParentEntity<T>` | engine | no | 7, 17 | none |
| `ISpawnable`, `IDestroyable` | engine | no | all, through `GameEntity` | none |
| `IDirectional` | engine | no | 88 | yes: `IBody` |
| `IActivatable`, `IEventListener` | engine | no | 15, 55 | yes |
| `IDamageNegotiator` | core | no | — | all (a strategy object) |
| `ICartRider`, `IWater`, `ILaserEntity`, `IHealthEntity`, `IProjectileEntity`, the component accessors | | yes | | |

Casts only because a contract is not an entity type (about 8): `FreezableEntityHandler.kt:53,93`,
`ShieldEntity.kt:68`, `LightSourceUtils.kt:43`, `ToxicGoopShot.kt:71`, `ReactorManProjectile.kt:186`,
`DrippingToxicGoop.kt:95`, `PreciousGem.kt:223`; `TimberWoman.kt:931` is already redundant. Other causes: about 15
`(it as GameEntity).destroy()` (no `destroy()` on `IGameEntity`), about 5 `(x as MegaGameEntity).id`, 4 in
`GameEntityUtils.kt` for `game`/`group`.

### Lift [code]

| Change | Size | Casts removed |
|---|---|---|
| Rename `IDamageable` → `IDamageableEntity` | 183 refs in 79 files + 3 engine; tests 2 | — |
| Rename `IDamager` → `IDamagerEntity` | 287 refs in 114 files + 3 engine; tests 2 | — |
| `IDamageable`/`IDamager` extend `IGameEntity` | 2 files + 4 sites | 4 |
| `IFreezableEntity` extends `IMegaGameEntity` | 1 file + 3 sites | 3 |
| `ILightSource` extends `IMegaGameEntity` | 2 files | 1 |
| Other core contracts | about 12 one-line edits | 0 now |
| `destroy()` on `IGameEntity` | 1 engine file + about 15 sites | about 15 |

Risks: Tiled resolves class names, not interface names; no reflection on these names; about 12 `is` checks keep
their meaning; no gameplay risk (type-only).

### Interfaces or components

State-only contracts fit components: `IFreezableEntity` (with `FreezableEntityHandler`, already component logic),
`ILightSource`, `IScalableGravityEntity`, `IFireableEntity` (item `048`). Markers `IFreezerEntity`/`IFireEntity` fit
damage tags (item `023`, "reactions key on tags, not classes"). Callback contracts stay interfaces until `023`/`048`
replace `AbstractHealthEntity`; as components, each damage contact would need 2 map lookups in place of 2 casts.

### Other engines [training]

Unity: components; `GetComponent<IDamageable>()` (interfaces on components). Unreal: actor components hold state;
`UInterface`s are thin accessors (for example `IAbilitySystemInterface` returns `UAbilitySystemComponent`). Godot:
child nodes, groups, `has_method`. Bevy/EnTT/flecs: pure and marker components read by queries; behavior in systems.
Most engines mix: data in components, a thin interface or event for per-type behavior.

## Review 2: `spawn`/`destroy` on `IGameEntity`

- `GameEntity` implements `ISpawnable`/`IDestroyable` with one-line delegates to `engine.spawn(this, p)` and
  `engine.destroy(this)` (`E/entities/GameEntity.kt`). `GameEngine.spawn`/`destroy` take `IGameEntity`, but
  `setExistenceState` throws for anything that is not a `GameEntity`. No implementer of `IGameEntity` is not a
  `GameEntity`, also in tests [code].
- Callers: 5 production `engine.spawn`/`engine.destroy` calls; about 390 member `.spawn(` calls, all on a concrete type
  (the factory returns it), so no cast exists only for `spawn`; exactly 15 casts exist only for `destroy()`:
  `MegamanMaverickGame.kt:900`, `PenguinMiniBoss.kt:106,112`, `Bospider.kt:229,369`, `RodentMan.kt:352`,
  `RotationAnchor.kt:102`, `DisappearingBlocks.kt:104`, `Togglee.kt:193,215`, `PetitDevil.kt:148`,
  `FireMetSpawner.kt:103`, `TorikoPlundge.kt:191`, `RocketPlatform.kt:123`, `SwingingPlatform.kt:128` [code].
- `Starkner` overrides `destroy()` (`Starkner.kt:192-196`), so a member `destroy()` is a virtual hook that the game
  uses; a change to `engine.destroy(x)` skips it (bug filed: `docs/issues/2026-10-27-starkner-alive-cleanup-only-in-destroy.md`).
  `IBossListener : IDestroyable` already calls `destroy()` [code].
- Other engines [training]: Unity `Instantiate`/`Destroy(obj)` (static, deferred); Unreal `UWorld::SpawnActor` and
  member `AActor::Destroy()` (deferred); Godot `add_child` and member `queue_free()` (deferred); Bevy
  `commands.spawn`/`despawn` (IDs only); EnTT `registry.create`/`destroy`; flecs `world.entity()` and member
  `e.destruct()` on a handle. The world or factory creates; object-style engines put the removal request on the
  entity, and the world defers it. None makes an entity spawn itself.
- Recommendation: `destroy()` only, as `IGameEntity : IDestroyable`; not `spawn` (no caller needs it, and creation
  stays with the factory and the engine). Optionally narrow `GameEngine.spawn`/`destroy` to `GameEntity`.

## 2026-10-27: research for 063a

One read-only research agent on `master` at `b5935b029` (after `062d`). Same tags and path short forms as above.
The scan draft for `063a` is outside the repo; the three answers and the corrected counts are here.

### Corrections to the reviews above [code]

- **The 15 `destroy()` casts.** All 15 file:line values are still correct. But 11 of them are already redundant
  today: the static type is a concrete `GameEntity` subclass (`children` is `Array<BabyPenguin>`, `Array<FireMet>`,
  `Array<Block>`, `Array<MegaGameEntity>`, ...), so `destroy()` is reachable with no cast. Only 4 casts need
  `destroy()` on `IGameEntity`, because the static type is `IGameEntity`: `MegamanMaverickGame.kt:900` (the
  `GameEntityCuller.cull(entity: IGameEntity)` parameter), `RotationAnchor.kt:102`, `Togglee.kt:193`, `Togglee.kt:215`
  (`children` is `Array<IGameEntity>`). The redundant 11: `PenguinMiniBoss.kt:106,112`, `Bospider.kt:229,369`,
  `RodentMan.kt:352`, `DisappearingBlocks.kt:104`, `PetitDevil.kt:148`, `FireMetSpawner.kt:103`, `TorikoPlundge.kt:191`,
  `RocketPlatform.kt:123`, `SwingingPlatform.kt:128`.
- **A 16th `destroy()` site.** `IProjectileEntity.kt:69-70` calls `game.engine.destroy(this)` with the comment "can't
  access "destroy" since it's defined in the abstract class". With `destroy()` on `IGameEntity`, it becomes `destroy()`.
  Today this is the same behavior: no `IProjectileEntity` overrides `destroy()`. After the change, an override is no
  longer skipped (the `Starkner` bug class).
- **`IDamageable`/`IDamager` extend `IGameEntity`: "4 sites, 4 casts" is wrong.** The 4 sites
  (`ToxicGoopShot.kt:71`, `ReactorManProjectile.kt:186`, `DrippingToxicGoop.kt:95`, `PreciousGem.kt:223`) cast to
  `IBodyEntity`, not to an entity type. `IGameEntity` does not give `body`, so these 4 casts stay. In production,
  `: IGameEntity` removes 0 casts. `: IMegaGameEntity` (possible only in `core`) would remove 1:
  `MegamanDamageNegotiator.kt:36` (`damager as MegaGameEntity`, for `getTag()` and `getType()`).
- **`ILightSource` (1 cast, `LightSourceUtils.kt:43`).** The cast goes only if the 5-parameter
  `sendLightSourceEvent(entity: MegaGameEntity, ...)` (`LightSourceUtils.kt:50-51`) takes `IMegaGameEntity` (it reads
  only `entity.game`). Its other callers pass a `MegaGameEntity`, so they still compile.
- **`IFreezableEntity` (3 casts)** is correct: `FreezableEntityHandler.kt:53` (`.game`, so it needs
  `IMegaGameEntity`), `FreezableEntityHandler.kt:93` and `ShieldEntity.kt:68` (`existenceState`).
- **`TimberWoman.kt:931`** is redundant, as noted: `IProjectileEntity : IMegaGameEntity` already gives `getTag()`.
- **9 more redundant casts** read only `IGameEntity` members (`existenceState`, `getProperty`) on a typed `children`
  value: `Bospider.kt:260`, `RodentMan.kt:361`, `PenguinMiniBoss.kt:127`, `FloatingCanHole.kt:94`,
  `PetitDevil.kt:180`, `CactusLauncher.kt:109`, `FireMetSpawner.kt:116`, `SwingingPlatform.kt:212`,
  `RotationAnchor.kt:109`. They are not caused by the contracts; they can go in the same pass.
- **`(x as MegaGameEntity).id`:** 4 sites, not about 5: `Rain.kt:200`, `TubeBeamerV2.kt:208`, `Laser.kt:677`,
  `SmallIceCube.kt:195`. `id` and `group` are on `MegaGameEntity` only, not on `IMegaGameEntity`, so these and the 4
  in `GameEntityUtils.kt:56,62,73,91` (parameter type `IBodyEntity`) stay. Not in `063`.
- **Rename counts.** `IDamageable`: 183 refs in 79 files (`core` main), 2 in 1 test file, 3 in 2 engine files = 188.
  `IDamager`: 287 in 114, 2 in 1, 3 in 2 = 292. Total 480 refs (the item says about 470) in 169 files that name one or
  both (the item says about 190; the two per-type file counts overlap). `core` has 193 import lines of
  `com.maverick.game.engine.damage.*` in 165 files (78 `IDamageable`, 115 `IDamager`).
- **Implementer counts** (all classes, also abstract and test classes, through the full supertype chain) match the
  inventory within a few: `IDamageable` 162, `IDamager` 272, `IFreezableEntity` 65, `IBossListener` 123, `IOwnable`
  108, `IHazard` 49, `ILightSource` 15, `IScalableGravityEntity` 16, `ItemEntity` 9, `IFireableEntity` 4,
  `IFireEntity` 4, `IFreezerEntity` 3, `ITeleporterEntity` 3, `IGateTrigger` 1, `IUpsideDownable` 0, `IChildEntity` 7,
  `IParentEntity` 17. No production implementer is outside `MegaGameEntity`. Tests: `PartsEntity`
  (`EntityQueriesTest.kt:33`) and `TestExistenceEntity` (`EntityExistenceTest.kt:16`) are `MegaGameEntity`; two strict
  interface mocks, `mockk<IDamager>()` (`MegaPlayersTest.kt:105`) and `mockk<ITeleporterEntity>()`
  (`TeleportsTest.kt:31`), still compile.
- **`engine.spawn`/`engine.destroy` callers** (5 in `core` production) are correct: `IProjectileEntity.kt:70`,
  `WilyPlaneBomb.kt:143`, `MegaLevelScreen.kt:610,947,972`.
- **`Starkner.kt:192-196`** still overrides `destroy()` on `master`. The fix in
  `.worktrees/fix-starkner-alive-cleanup` can remove that override; then no class overrides `destroy()`.

### Question 1: how `destroy()` reaches `IGameEntity`

Facts [code]:

- `E/entities/contracts/IDestroyable.kt` declares `fun destroy(): Boolean`. Its only users: `GameEntity.kt:16`
  (`IGameEntity, ISpawnable, IDestroyable`) and `IBossListener.kt:5` (`IBossListener : IDestroyable`, so the default
  `onBossDefeated` can call `destroy()`). `ISpawnable` has one user, `GameEntity.kt:16`.
- The `Boolean` is the `062b` accept/refuse result ("the last accepted call wins"). Production code does not read it,
  except `Starkner.kt:195` (`return super.destroy()`). Engine tests assert it 9 times (for example
  `EntityExistenceHooksTest.kt:131,148,152`, `EntityExistenceStateTest.kt:183,221`).
- `core` main has 320 `destroy()` calls.
- LibGDX `Disposable` (gdx 1.14.2 source): "Interface for disposable resources." `void dispose()`: "Releases all
  resources of this object." It returns `Unit`.
- In this repo, `dispose()` means "shut down for good": `DesktopLauncher.kt:181` calls `game.dispose()`;
  `MegamanMaverickGame.kt:721-754` disposes the batch, the shape renderer, the engine, the screens, the debug window,
  the log writer and the diagnostics; `GameEngine.dispose()` (`GameEngine.kt:279-288`) destroys all entities, disposes
  the systems and sets `disposed`, after which `update()` throws (`GameEngine.kt:231`). `GameSystem.kt:88` disposes its
  `disposables`. `MegaLevelScreen.kt:219,541-542,1140` keeps an `Array<Disposable>` (listener removals from
  `SpawnersLayerBuilder.kt:151,174`) and disposes it at screen dispose. Implementers: `GameEngine`, `GameSystem`,
  `RuntimeDiagnostics`, `MegamanMaverickGame`, `DebugWindow`, `MegaLevelScreen`, `AsyncFileWriter`, the screens. No
  entity implements `Disposable`.
- Entities are pooled. After `destroy()`, the pool runnable returns the entity, and the next `spawn()` reuses it.

Options:

| | (a) `IGameEntity : IDestroyable` | (b) `fun destroy(): Boolean` on `IGameEntity` | (c) `IGameEntity : Disposable` |
|---|---|---|---|
| Change | 1 line in `IGameEntity.kt` | 1 function in `IGameEntity.kt`; delete `IDestroyable.kt`; `GameEntity.kt:16` and `IBossListener.kt:5` drop it | Rename `destroy()` to `dispose()` (320 calls, 2 declarations, 9 test asserts), or keep both names |
| Return | `Boolean` | `Boolean` | `Unit`: the `062b` result is lost |
| Meaning | "remove from the world, may come back" | same | LibGDX: "release resources, object is dead"; in this repo: shutdown. A pooled entity is spawned again after it |
| Leftover | `IDestroyable` has one user (`IGameEntity`) once `IBossListener` extends `IMegaGameEntity` | none | an entity could be added by mistake to an `Array<Disposable>` (`MegaLevelScreen`, `GameSystem`) and be "disposed" at screen or engine shutdown |
| `032` | neutral | neutral | neutral: `Disposable` is in gdx core, so it works on all platforms; but it ties the entity contract to a LibGDX type that the item has no need for |

- (a) Pros: no new code; keeps the `ISpawnable`/`IDestroyable` pair. Cons: after `063a` the interface has no user of
  its own; the rule "write code only when production code calls it" argues against a type that only `IGameEntity`
  uses.
- (b) Pros: one place; deletes one engine file; `IBossListener` gets `destroy()` from `IMegaGameEntity` (which `063a`
  adds anyway). Cons: `ISpawnable` stays alone on `GameEntity` (no caller needs `spawn` on `IGameEntity`, so this is
  correct, not a gap).
- (c) Pros: an existing library type, as the owner said. Cons: wrong meaning for a pooled entity, `Unit` return, a
  rename of about 330 sites or two names for one action, and a real risk of shutdown code calling it on a live
  entity. [inferred] Unity and Godot also keep "destroy/free" separate from "release native resources".

Recommendation: (b). The owner does not want `IGameEntity : IDestroyable`, and (b) gives the same result with one
interface less. Do not use `Disposable`.

### Question 2: `IDamageable`/`IDamager` in `engine` or `core`

Facts [code]:

- Declared in `E/damage/IDamageable.kt` (9 lines) and `E/damage/IDamager.kt` (7 lines); the package has no other
  file. Related types are all in `core`: `C/damage/IDamageNegotiator.kt`, `DamageNegotiation.kt`,
  `StandardDamageNegotiator.kt`, `CustomMapDamageNegotiator.kt`, `SelfSizeDamageNegotiator.kt`, the contact rule
  `C/world/contacts/rules/DamageRule.kt` (the only caller of `canBeDamagedBy`, `canDamage`, `takeDamageFrom`,
  `onDamageInflictedTo`), and `AbstractHealthEntity`.
- In `engine/` the names occur only in the two files (3 refs each, from the cross references). No engine system, no
  other engine code and no engine test uses them.
- `core`: 193 import lines in 165 files; 480 refs in total (see the corrections).
- Engine scope (memory note, 2026-10-22): `engine/` is a Mega Man-like engine; genre concepts such as damage tables
  may live there.
- `023` (planned, depends on `005`) replaces the negotiators with `DamageSource(tag, elements, magnitude, owner)` and
  named profiles; `023/NOTES.md:13` already notes "All of the logic is in `core`". `030` (planned) adds `Faction` and
  root owner. Both can change the method signatures of these two types.

Options:

- **Keep in `engine` (063a adds `: IGameEntity`).** Pros: no move; fits the engine scope (damage is a genre concept);
  if `023` puts a damage pipeline in the engine, the types are already there. Cons: an engine type with no engine
  caller; it can only extend `IGameEntity`, so `MegamanDamageNegotiator.kt:36` keeps its cast.
- **Move to `core` (`C/damage/`).** Pros: next to every related type and its only caller; it can extend
  `IMegaGameEntity` (1 cast less); the engine API gets smaller. Cons: about 170 files of churn; if `023` moves damage
  into the engine, a second move follows.
- The rule "write code only when production code calls it" does not decide this: production code (`DamageRule`)
  calls the types. The rule is about whether code exists, not about its module.

One commit with the rename: yes. Both the move and the rename rewrite the same import lines, so one scripted pass
(`import com.maverick.game.engine.damage.IDamageable` to `import com.megaman.maverick.game.damage.IDamageableEntity`,
then the word rename) makes one mechanical commit and one `.git-blame-ignore-revs` entry. Keep the supertype change
(`IGameEntity` to `IMegaGameEntity`) out of that commit, because it is not mechanical. [inferred] Git may not detect
the two moved files as renames (they are small and the package line and names change), so their history shows as
delete plus add; this is acceptable for 16 lines.

Recommendation: in `063a`, keep them in `engine` and add `: IGameEntity` (type-only, 2 lines). Decide the module at
`063b`: if the owner wants `core`, do the move in the same mechanical commit as the rename. Otherwise leave the
question to `023`, which decides where the damage pipeline lives. Do not move them twice.

### Question 3: narrow `GameEngine.spawn`/`destroy` to `GameEntity`

Facts [code]:

- The throw: `GameEngine.kt:211-214`, `setExistenceState(entity: IGameEntity, ...)` does `entity as? GameEntity ?:
  throw IllegalArgumentException("The engine can only hold a GameEntity: $entity")`. The reason: `existenceState` has
  an `internal set` on `GameEntity` (`GameEntity.kt:23-24`), and an interface property cannot have an internal setter.
- The throw comes after a queue write: `spawn()` adds to `entitiesToSpawn` (`GameEngine.kt:106`) before
  `setExistenceState` (`:107`); `destroy()` adds to `entitiesToKill` (`:159`) before `:160`. So a non-`GameEntity`
  leaves a queue entry behind after the exception. No such caller exists, and no test checks the throw.
- Callers and the static type they hold: `GameEntity.kt:28,30` (`this: GameEntity`); `IProjectileEntity.kt:70`
  (`this: IProjectileEntity`, an `IGameEntity`); `WilyPlaneBomb.kt:143` (`this: WilyPlaneBomb`);
  `MegaLevelScreen.kt:610` (`player: MegaGameEntity`, `:169`); `MegaLevelScreen.kt:947,972` (`spawn.entity:
  MegaGameEntity`, `Spawn.kt:7`). Tests: `GameEngineTest.kt:34,41,48,49`, `UpdatablesSystemTest.kt:36,37,53`,
  `SpriteSystemTest.kt:51`, `SpawnerReleaseTest.kt:50`; all hold `GameEntity` subclasses (`MockGameEntity`,
  `FilteredEntity`) or `MegaGameEntity`.
- Other `IGameEntity` parameters: `hasClaimOn` (`:80`, read-only, called from `MegamanMaverickGame.kt:931` with a
  `MegaGameEntity`), `onSpawnRejected: ((IGameEntity) -> Unit)?` (`:18`), and the internal queues.

Goal and cost: the goal is a compile error in place of a runtime throw. The cost the owner asked about (code that
holds a contract type needs a cast again to call the engine) is zero today: after `063a` changes
`IProjectileEntity.kt:70` to `destroy()`, no production code holds an `IGameEntity` and calls `engine.spawn` or
`engine.destroy`. Code that holds an `IGameEntity` calls the member `destroy()`. Code that wants `spawn` on an
`IGameEntity` already needs a cast today, because `spawn` is on `GameEntity` only (no such caller; the 393 member
`.spawn(` calls in `core` main are on concrete types).

To remove the throw, not only the public signature but also the internal queues (`entities`, `EntitiesToSpawn`,
`entitiesToKill`, `entitiesToRespawn`, `destroysWaitingForSpawn`, `respawnsNow`) and `spawnNow`, `destroyNow`,
`setExistenceState` change to `GameEntity`. All edits are in `GameEngine.kt`. `hasClaimOn` and `onSpawnRejected` can
stay `IGameEntity`. Systems keep `ImmutableCollection<IGameEntity>`.

Sealed interface: it does not work. Kotlin requires that every direct subtype of a sealed interface (also a
sub-interface) is in the same module and the same package. Direct subtypes of `IGameEntity` today: `GameEntity`
(package `com.maverick.game.engine.entities`, the only one that would pass), the component accessor interfaces in
package `com.maverick.game.engine.entities.contracts` (`IBodyEntity`, `ISpritesEntity`, ...; another package), and in
`core` `IMegaGameEntity` and `ILaserEntity` (another module). After `063a`, `IDamageable`/`IDamager` (package
`engine.damage`) and every core contract are added. All but `GameEntity` fail to compile. Also, `sealed`
does not let a value typed `IGameEntity` pass where `GameEntity` is required; it only gives exhaustive `when`.

Recommendation: narrow `spawn`/`destroy` and the internal queues to `GameEntity` in `063a`, as one separate commit
after the contract changes (type-only, one file, no caller needs a cast). If the owner wants `063a` to stay
contracts-only, put it in `063a`'s `docs/TODO.md` as a backlog candidate.
