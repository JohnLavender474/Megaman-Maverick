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
