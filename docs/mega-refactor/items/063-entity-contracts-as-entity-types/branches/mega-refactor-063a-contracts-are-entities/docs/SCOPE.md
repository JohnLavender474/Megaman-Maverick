# Scope: mega-refactor-063a-contracts-are-entities

- Status: approved
- Approved: 2026-10-27

## Goal

The entity contract interfaces say that they are entities. Code that holds a contract type reaches entity members
(`existenceState`, `destroy()`, `game`) with no cast. The engine accepts any `IGameEntity` and does not throw for an
entity that is not a `GameEntity`. No gameplay change.

## Item

From `063` `ITEM.md`, "Proposed scope", first branch (type-only): every core entity contract extends `IGameEntity`
(or `IMegaGameEntity` where it needs `game`); `IDamageable`/`IDamager` extend `IGameEntity`; `IGameEntity` gets
`destroy()` (not `spawn`); the casts that existed only for these members go; delete the unused `IUpsideDownable`;
drop the redundant cast at `TimberWoman.kt:931`.

Owner decisions (2026-10-27, in `ITEM.md`):

- `IGameEntity : IDestroyable`, with the existing `IDestroyable`. Not LibGDX `Disposable`.
- `IDamageable`/`IDamager` stay in `engine` and get `: IGameEntity`. `063b` decides the module, together with the
  rename.
- "The game engine should always accept any `IGameEntity`. Otherwise, the interface is a lie." `GameEngine.spawn`/
  `destroy` keep `IGameEntity` parameters.
- "Let's just make `existenceState` be a var. Callers will need to be smart and not accidentally overwrite it."

## Current state

Base: `master` at `9e9c6f0c3`. Full research: item `NOTES.md`, section "2026-10-27: research for 063a" (all counts
checked against `master` after `062d`). `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

- `E/entities/IGameEntity.kt` has `val existenceState` and no `destroy()`. `E/entities/GameEntity.kt:16` implements
  `IGameEntity, ISpawnable, IDestroyable`; `:23` holds `existenceState` with an `internal set`.
- `E/GameEngine.kt:211-214` (`setExistenceState`) casts to `GameEntity` and throws for anything else. The throw runs
  after the queue write in `spawn`/`destroy`, so a bad call leaves a queue entry behind. No caller passes a
  non-`GameEntity` today, and no test checks the throw.
- `IDestroyable` (`fun destroy(): Boolean`) has two users: `GameEntity.kt:16` and `C/entities/contracts/IBossListener.kt:5`.
- The core contracts do not extend an entity type. All their implementers are `MegaGameEntity` subclasses
  (production and tests). Two strict test mocks (`mockk<IDamager>()` in `MegaPlayersTest`, `mockk<ITeleporterEntity>()`
  in `TeleportsTest`) implement a contract directly.
- Casts that exist only because of this (see "In scope" for the list): 5 that need `destroy()` on `IGameEntity`
  (including the `IProjectileEntity.kt:69-70` workaround `game.engine.destroy(this)`), 4 that need a contract to
  extend `IMegaGameEntity`, and 22 that are already redundant today (the static type already has the member).
- `IUpsideDownable` has 0 references outside its own file.

## In scope

Engine:

1. `IGameEntity : IComponentBucket, IPropertizable, Initializable, IDestroyable`. `GameEntity` drops the now
   redundant `IDestroyable` from its list.
2. `IGameEntity.existenceState` becomes `var`. `GameEntity.existenceState` loses its `internal set`.
   `GameEngine.setExistenceState` is deleted (with its cast and throw); each of its call sites writes
   `entity.existenceState = ...` directly (owner, 2026-10-27: "There shouldn't be a `setExistenceState` func").
3. `IDamageable : IGameEntity`, `IDamager : IGameEntity`.

Core contracts (each gets `: IMegaGameEntity`):

4. `IFreezableEntity`, `IFreezerEntity`, `IFireEntity`, `IFireableEntity`, `IScalableGravityEntity`, `ILightSource`,
   `IOwnable`, `ITeleporterEntity`, `ItemEntity`, `IGateTrigger`. `IBossListener` changes `: IDestroyable` to
   `: IMegaGameEntity`. `IHazard` gets it through `IBossListener` (no change).
5. Delete `IUpsideDownable`.

Casts (file:line on `master`; before and after in the item `NOTES.md` and the scan):

6. Need `destroy()` on `IGameEntity`: `MegamanMaverickGame.kt:900`, `RotationAnchor.kt:102`, `Togglee.kt:193`,
   `Togglee.kt:215`; `IProjectileEntity.kt:69-70` becomes `destroy()`.
7. Need a contract to extend `IMegaGameEntity`: `FreezableEntityHandler.kt:53`, `:93`; `ShieldEntity.kt:68`;
   `LightSourceUtils.kt:43` (with the parameter at `:51` from `MegaGameEntity` to `IMegaGameEntity`).
8. Already redundant (same pass, same kind of edit): 11 `destroy()` casts on typed `children` (`PenguinMiniBoss`,
   `Bospider`, `RodentMan`, `DisappearingBlocks`, `PetitDevil`, `FireMetSpawner`, `TorikoPlundge`, `RocketPlatform`,
   `SwingingPlatform`), `TimberWoman.kt:931`, `PetitDevil.kt:147`, and 9 iterator casts `next() as MegaGameEntity`
   that read only `existenceState`.
9. Root `CLAUDE.md`: "only the engine writes it" changes to say that only the engine should write it.

## Out of scope

- `spawn()` on `IGameEntity`: no caller needs it.
- The rename `IDamageable`/`IDamager` to `IDamageableEntity`/`IDamagerEntity`, and their module: `063b`.
- `MegamanDamageNegotiator.kt:36` (`damager as MegaGameEntity`): needs `IDamager : IMegaGameEntity`, which needs the
  move to `core` (`063b`).
- `as IBodyEntity` casts on a damager or damageable (`ToxicGoopShot.kt:71`, `ReactorManProjectile.kt:186`,
  `DrippingToxicGoop.kt:95`, `PreciousGem.kt:223`, `FreezableEntityHandler.kt:41`, `RotationAnchor.kt:121`): `body`
  is not on `IGameEntity`.
- `(x as MegaGameEntity).id` (4 sites) and `GameEntityUtils.kt` casts: `id` and `group` are only on `MegaGameEntity`.
- `MegamanMaverickGame.kt:930` (`runnablesOnDestroy`) and `RotationAnchor.kt:122` (`ObjectMap<GameEntity, ...>`):
  not caused by a contract.
- `IChildEntity`/`IParentEntity : IGameEntity`: no cast depends on it, so no caller. Backlog candidate.
- Contracts as components (`048`); marker contracts as damage tags (`023`).
- A guard against writes to `existenceState` from outside the engine (owner decision).

## Affected areas and risks

- About 25 core files and 5 engine files. Types only, except two runtime paths:
  - `IProjectileEntity.explodeAndDie` calls the virtual `destroy()` in place of `engine.destroy(this)`. No projectile
    overrides `destroy()`, so the behavior is the same today. A temporary old-versus-new suite proves it.
  - `GameEngine.setExistenceState` is deleted and its call sites write the state directly. For a `GameEntity` the
    write is the same.
- `ShieldEntity.kt:68`: `owner` is a `var`, so Kotlin does not smart-cast it. The new form keeps the null check.
- A public `existenceState` setter: core code could write it by mistake. Accepted by the owner. The `062` tests that
  check the engine writes stay.
- Strict mocks of `IDamager`/`ITeleporterEntity`: they still compile; the code under test makes no new calls on them.
- Possible new "useless `is` check" warnings where code checks `x is IGameEntity && x is <contract>`. The warning
  count is compared with `master`.

## Overlap with other branches

- `fix/starkner-alive-cleanup` (`.worktrees/fix-starkner-alive-cleanup`) changes `Starkner.kt` and removes its
  `destroy()` override. `063a` does not edit `Starkner.kt`. Either merge order works.
- No other active `mega-refactor-` branch.

## Open questions

None.
