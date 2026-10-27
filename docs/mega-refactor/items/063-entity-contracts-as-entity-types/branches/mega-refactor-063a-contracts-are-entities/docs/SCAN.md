# Scan draft: `063a` (contracts are entity types, type-only)

Draft for the scan step of `mega-refactor-063a-<slug>`. Base: `master` at `b5935b029` (after `062d`). Read with the
item `NOTES.md` section "2026-10-27: research for 063a". `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

The draft was written before the owner's decisions. Where it differs, `SCOPE.md` wins: `IGameEntity : IDestroyable`
(option a, so `IDestroyable` stays), no narrowing of `GameEngine.spawn`/`destroy` (the throw goes instead), and
`existenceState` becomes a public `var`.

## 1. Engine changes

| File | Before | After |
|---|---|---|
| `E/entities/IGameEntity.kt` | no `destroy()` | `fun destroy(): Boolean` (option a: `interface IGameEntity : IComponentBucket, IPropertizable, Initializable, IDestroyable`) |
| `E/entities/GameEntity.kt:16` | `IGameEntity, ISpawnable, IDestroyable` | `IGameEntity, ISpawnable` (`override fun destroy()` at `:30` now overrides `IGameEntity.destroy`) |
| `E/entities/contracts/IDestroyable.kt` | `interface IDestroyable` | deleted (option b only; users are `GameEntity.kt:16` and `IBossListener.kt:5`) |
| `E/damage/IDamageable.kt:3` | `interface IDamageable` | `interface IDamageable : IGameEntity` |
| `E/damage/IDamager.kt:3` | `interface IDamager` | `interface IDamager : IGameEntity` |

Optional, decide in `SCOPE.md`: `E/entities/contracts/IChildEntity.kt:5` and `IParentEntity.kt:5` extend
`IGameEntity` (7 and 17 implementers, all entities). No cast depends on them.

## 2. Core contracts to change

All implementers are `MegaGameEntity` (production and tests), so every core contract can extend `IMegaGameEntity`.
Two need it (`game`): `IFreezableEntity`, `ILightSource`. Use `IMegaGameEntity` for all, for one rule.

| File:line | Before | After | Implementers |
|---|---|---|---|
| `C/entities/contracts/IFreezableEntity.kt:3` | `interface IFreezableEntity` | `: IMegaGameEntity` | 65 |
| `C/entities/contracts/IFreezerEntity.kt:3` | `interface IFreezerEntity` | `: IMegaGameEntity` | 3 |
| `C/entities/contracts/IFireEntity.kt:3` | `interface IFireEntity` (marker) | `: IMegaGameEntity` | 4 |
| `C/entities/contracts/IFireableEntity.kt:3` | `interface IFireableEntity` | `: IMegaGameEntity` | 4 |
| `C/entities/contracts/IScalableGravityEntity.kt:8` | `interface IScalableGravityEntity` | `: IMegaGameEntity` | 16 |
| `C/entities/contracts/ILightSource.kt:6` | `interface ILightSource  ` (two spaces) | `: IMegaGameEntity` | 15 |
| `C/entities/contracts/IBossListener.kt:5` | `: IDestroyable` | `: IMegaGameEntity` (option a: either; `IDestroyable` becomes redundant) | 123 |
| `C/entities/contracts/IHazard.kt:3` | `: IBossListener` | no change (gets it through `IBossListener`) | 49 |
| `C/entities/contracts/IOwnable.kt:3` | `interface IOwnable<T>` | `: IMegaGameEntity` | 108 |
| `C/entities/contracts/ITeleporterEntity.kt:5` | `interface ITeleporterEntity` | `: IMegaGameEntity` | 3 |
| `C/entities/contracts/ItemEntity.kt:4` | `interface ItemEntity` | `: IMegaGameEntity` | 9 |
| `C/entities/contracts/IGateTrigger.kt:3` | `interface IGateTrigger` (marker) | `: IMegaGameEntity` | 1 (`Megaman`) |
| `C/entities/contracts/IUpsideDownable.kt` | `interface IUpsideDownable` | deleted (0 refs outside its file) | 0 |

Already entity types (no change): `IMegaGameEntity`, `IProjectileEntity` (`: IMegaGameEntity`), `ILaserEntity`
(`: IGameEntity`), `ICartRider` and `IWater` (`: IBodyEntity`), `IHealthEntity` (`: IPointsEntity`).

Note for `IFreezerEntity`/`IFireEntity`: `023` plans to replace these markers with damage tags. The supertype now
does not block that.

## 3. Casts to remove

### 3a. Need `destroy()` on `IGameEntity` (static type `IGameEntity`)

| File:line | Before | After |
|---|---|---|
| `C/MegamanMaverickGame.kt:900` | `(entity as GameEntity).destroy()` | `entity.destroy()` |
| `C/entities/special/RotationAnchor.kt:102` | `children.forEach { (it as GameEntity).destroy() }` | `children.forEach { it.destroy() }` |
| `C/entities/special/Togglee.kt:193` | `children.forEach { (it as GameEntity).destroy() }` | `children.forEach { it.destroy() }` |
| `C/entities/special/Togglee.kt:215` | `else (it as GameEntity).destroy()` | `else it.destroy()` |
| `C/entities/contracts/IProjectileEntity.kt:69-70` | comment "can't access "destroy" ..." + `game.engine.destroy(this)` | `destroy()` (comment removed) |

Check after the edit: the `GameEntity` import in `MegamanMaverickGame.kt`, `RotationAnchor.kt` (still used at `:48`,
`:122`) and `Togglee.kt` may become unused.

### 3b. Need a contract to extend `IMegaGameEntity`

| File:line | Before | After | Needs |
|---|---|---|---|
| `C/entities/utils/FreezableEntityHandler.kt:53` | `ShieldEntity((entity as MegaGameEntity).game)` | `ShieldEntity(entity.game)` | `IFreezableEntity : IMegaGameEntity` |
| `C/entities/utils/FreezableEntityHandler.kt:93` | `!(entity as MegaGameEntity).existenceState.liveAfterUpdate` | `!entity.existenceState.liveAfterUpdate` | same |
| `C/entities/blocks/ShieldEntity.kt:68` | `!(owner as MegaGameEntity).existenceState.liveAfterUpdate` | `!owner!!.existenceState...` or `owner?.let { !it.existenceState.liveAfterUpdate } == true` (see risk R3) | same |
| `C/utils/misc/LightSourceUtils.kt:43` | `lightSource as MegaGameEntity,` | `lightSource,` | `ILightSource : IMegaGameEntity` and `LightSourceUtils.kt:51` `entity: MegaGameEntity` to `entity: IMegaGameEntity` |

### 3c. Already redundant today (no type change needed; same pass)

`destroy()` casts on a typed `children` (11):

| File:line | Static type of `it` | Before | After |
|---|---|---|---|
| `C/entities/bosses/PenguinMiniBoss.kt:106` | `BabyPenguin` | `(it as GameEntity).destroy()` | `it.destroy()` |
| `C/entities/bosses/PenguinMiniBoss.kt:112` | `BabyPenguin` | same | same |
| `C/entities/bosses/Bospider.kt:229` | `BabySpider` | same | same |
| `C/entities/bosses/Bospider.kt:369` | `BabySpider` | same | same |
| `C/entities/bosses/RodentMan.kt:352` | `RatRobot` | `(it as MegaGameEntity).destroy()` | `it.destroy()` |
| `C/entities/special/DisappearingBlocks.kt:104` | `Block` | `(it as MegaGameEntity).destroy()` | `it.destroy()` |
| `C/entities/enemies/PetitDevil.kt:148` | `PetitDevilChild` | `else (it as GameEntity).destroy()` | `else it.destroy()` |
| `C/entities/enemies/FireMetSpawner.kt:103` | `FireMet` | `(it as GameEntity).destroy()` | `it.destroy()` |
| `C/entities/enemies/TorikoPlundge.kt:191` | `Block` | `(it as MegaGameEntity).destroy()` | `it.destroy()` |
| `C/entities/blocks/RocketPlatform.kt:123` | `MegaGameEntity` | `(it as GameEntity).destroy()` | `it.destroy()` |
| `C/entities/blocks/SwingingPlatform.kt:128` | `MegaGameEntity` | `(it as GameEntity).destroy()` | `it.destroy()` |

Other redundant casts (read only `IGameEntity` or `IMegaGameEntity` members). Optional; include them if `SCOPE.md`
says "every cast that a contract or `IGameEntity` already makes redundant":

| File:line | Before | After |
|---|---|---|
| `C/entities/bosses/TimberWoman.kt:931` | `(projectile as MegaGameEntity).getTag()` | `projectile.getTag()` |
| `C/entities/enemies/PetitDevil.kt:147` | `(it as PetitDevilChild).disintegrateAndDie()` | `it.disintegrateAndDie()` |
| `C/entities/bosses/Bospider.kt:260` | `val child = childIter.next() as MegaGameEntity` | `val child = childIter.next()` |
| `C/entities/bosses/RodentMan.kt:361` | `val next = childIter.next() as MegaGameEntity` | `val next = childIter.next()` |
| `C/entities/bosses/PenguinMiniBoss.kt:127` | `val child = iter.next() as MegaGameEntity` | `val child = iter.next()` |
| `C/entities/enemies/FloatingCanHole.kt:94` | same form (`Array<FloatingCan>`) | same |
| `C/entities/enemies/PetitDevil.kt:180` | same form | same |
| `C/entities/enemies/CactusLauncher.kt:109` | same form (`Array<CactusMissile>`) | same |
| `C/entities/enemies/FireMetSpawner.kt:116` | same form | same |
| `C/entities/blocks/SwingingPlatform.kt:212` | same form | same |
| `C/entities/special/RotationAnchor.kt:109` | `child as MegaGameEntity` (statement) | line removed (uses `existenceState`, `getProperty`) |

Count: 5 (3a) + 4 (3b) + 11 (3c, `destroy()`) = 20 casts plus the `IProjectileEntity` workaround; with the optional
list, 31. The item said "about 25": the 4 `IDamageable`/`IDamager` casts it counted do not go (see 4).

## 4. What stays out, and why

- **`as IBodyEntity` casts on a damager or damageable** (`ToxicGoopShot.kt:71`, `ReactorManProjectile.kt:186`,
  `DrippingToxicGoop.kt:95`, `PreciousGem.kt:223`; also `FreezableEntityHandler.kt:41`, `RotationAnchor.kt:121`):
  `body` is not on `IGameEntity`. A contract that extends `IBodyEntity` is a different design (not every damager has
  a body in the type system).
- **`MegamanDamageNegotiator.kt:36`** (`damager as MegaGameEntity`): needs `IDamager : IMegaGameEntity`, only possible
  if the types move to `core` (question 2, `063b` or `023`).
- **`DamageRule.kt:22-23,40-41`** (`getEntity() as IDamager`/`as IDamageable`): a narrowing cast from
  `MegaGameEntity`; it stays.
- **`(x as MegaGameEntity).id`** (`Rain.kt:200`, `TubeBeamerV2.kt:208`, `Laser.kt:677`, `SmallIceCube.kt:195`) and
  **`GameEntityUtils.kt:56,62,73,91`**, **`IMegaGameEntity.kt:31`** (`IBodyEntity` receiver): `id` and `group` are
  only on `MegaGameEntity`; the `IBodyEntity` parameters come from the engine. Moving `id`/`group` to `IMegaGameEntity`
  is a separate change.
- **`MegamanMaverickGame.kt:930`** (`onSpawnRejected`: `it as MegaGameEntity` for `runnablesOnDestroy`) and
  **`RotationAnchor.kt:122`** (`childTargets: ObjectMap<GameEntity, Vector2>`): not caused by a contract.
- **`spawn()` on `IGameEntity`**: no caller needs it (393 member `.spawn(` calls in `core` main, all on concrete types).
- **The rename** `IDamageable`/`IDamager` to `...Entity`: `063b` (480 refs in 169 files; one mechanical commit).
- **Move of `IDamageable`/`IDamager` to `core`**: not in `063a` (question 2).
- **Contracts as components** (`IFreezableEntity` and others): item `048`. Markers as damage tags: item `023`.
- **Not candidates**: `IDirectional` (`IBody` implements it), `IActivatable`, `IEventListener` (non-entity
  implementers), `IDamageNegotiator` (a strategy object), `IFaceable`, `ISizable` (generic).
- **`GameEngine.spawn`/`destroy` narrowing**: optional last commit (question 3). If it is in scope:
  `E/GameEngine.kt` public `spawn(entity: GameEntity, ...)`, `destroy(entity: GameEntity)`, the internal queues
  (`entities`, `EntitiesToSpawn`, `entitiesToKill`, `entitiesToRespawn`, `destroysWaitingForSpawn`, `respawnsNow`),
  `spawnNow`, `destroyNow` and `setExistenceState` take `GameEntity`; the `as? GameEntity ?: throw` at `:212-213`
  goes. `hasClaimOn`, `onSpawnRejected` and the systems keep `IGameEntity`. Callers need no change once
  `IProjectileEntity.kt:70` uses `destroy()`.

## 5. Overlap

- `.worktrees/fix-starkner-alive-cleanup` (branch `fix/starkner-alive-cleanup`, at `b5935b029`) changes
  `Starkner.kt`, which has the only `destroy()` override (`:192-196`). `063a` does not touch `Starkner.kt`. If the fix
  removes the override, the facts "an override exists" in the notes change, but no `063a` edit conflicts. Merge
  order does not matter; check `Starkner.kt` compiles against the new `IGameEntity.destroy()` (an `override fun
  destroy(): Boolean` still overrides it).
- No other active `mega-refactor-` branch (`git worktree list` on 2026-10-27).

## 6. Risks

- **R1: a contract implemented by a non-entity.** None in production or tests (checked through the full supertype
  chain). Two strict interface mocks (`MegaPlayersTest.kt:105` `mockk<IDamager>()`, `TeleportsTest.kt:31`
  `mockk<ITeleporterEntity>()`) still compile. A strict mock throws if the code under test calls an `IGameEntity`
  member on it; the code under test does not change, so it makes no new calls.
- **R2: member conflicts.** No contract declares a member with the name of an `IGameEntity`/`IMegaGameEntity` member
  (`components`, `existenceState`, `initialized`, `properties`, `game`, `getTag`, `getType`, `destroy`). The default
  methods of `IMegaGameEntity` reach a class through several paths, but from one declaration, so Kotlin needs no
  override.
- **R3: `ShieldEntity.kt:68` smart cast.** `owner` is a `var` property (`:31`), so Kotlin does not smart-cast it after
  `owner != null`. The cast today also does the non-null step. The new form must keep the same logic
  (`owner != null && !owner!!.existenceState.liveAfterUpdate`, or a local `val`).
- **R4: `IProjectileEntity.explodeAndDie` dispatch.** `destroy()` is virtual; `engine.destroy(this)` is not. Today no
  `IProjectileEntity` overrides `destroy()`, so the behavior is the same. After the change, a future override runs
  (this is the wanted behavior; see the `Starkner` issue).
- **R5: `is` checks.** About 12 `is <contract>` checks keep their meaning. Some `x is IGameEntity && x is <contract>`
  forms may get a "useless check" warning; warnings are not errors in this build.
- **R6: Tiled and reflection.** Tiled resolves class names; `MegaEntityFactory` uses class constructors. No code reads
  interface names by reflection.
- **R7: option (b) deletes `IDestroyable`.** No code outside `GameEntity` and `IBossListener` names it; `IBossListener`
  must extend `IMegaGameEntity` in the same commit, or `onBossDefeated` loses `destroy()`.
- Gameplay risk: none expected. The only runtime path change is R4, which is the same today.

## 7. Tests the branch needs

### Temporary (old versus new; delete before the merge)

The branch changes types, not logic, except `IProjectileEntity.explodeAndDie` (R4). A frozen-copy suite is small:

- `Temp063aExplodeAndDieEquivalenceTest` (core): a frozen copy of the old `explodeAndDie` destroy step
  (`game.engine.destroy(this)`) and the new one (`destroy()`), run on two fresh instances of the same projectile in
  the same state (`LIVE`, `SPAWN_QUEUED`, `UNSPAWNED`, `DESTROY_QUEUED`). Assert equal return values and equal
  `existenceState` after the call and after one `engine.update()`.
- If the narrowing is in scope: no temporary suite is needed. The signature change is checked by the compiler, and
  the existing `GameEngineTest`, `EntityExistenceStateTest` and `EntityExistenceHooksTest` cover the logic.

### Permanent

- Engine: `IGameEntity.destroy()` on a `GameEntity` held as `IGameEntity` returns the `062b` results (`true` for
  `LIVE`, `false` for `UNSPAWNED` and `DESTROY_QUEUED`) and calls a subclass override (the `Starkner` hook). Can be added
  to `EntityExistenceHooksTest`.
- Core: `EntityContractsTest`: each core contract (`IFreezableEntity`, `IFreezerEntity`, `IFireEntity`,
  `IFireableEntity`, `IScalableGravityEntity`, `ILightSource`, `IBossListener`, `IHazard`, `IOwnable`,
  `ITeleporterEntity`, `ItemEntity`, `IGateTrigger`) is a subtype of `IMegaGameEntity`, and `IDamageable`/`IDamager`
  are subtypes of `IGameEntity` (`IMegaGameEntity::class.java.isAssignableFrom(X::class.java)`). This guards against
  a later edit that drops a supertype and brings the casts back.
- Core: `IBossListener.onBossDefeated` default destroys the entity (a `MegaGameEntity` test class that implements
  `IBossListener`; after the call, `existenceState` is `DESTROY_QUEUED`).
- Existing suites: `./gradlew engine:test core:test lwjgl3:jar` (CI job), with no test change other than the above.

### Manual

- Type-only, so the contract can be one short sanity pass: a room with a `Togglee`, a `RotationAnchor`, a frozen
  enemy with `ShieldEntity`, a light source, and a projectile that uses `explodeAndDie` (for example
  `ToxicGoopShot`). Find rooms in the live TMX files at the `CONTRACT.md` step.
