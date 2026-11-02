# Notes: 048 Replace entity inheritance with components

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## From the old `BACKLOG.md` (2026-10-22)

Entities get health by extending `AbstractHealthEntity`. Enemies must extend `AbstractEnemy` and
bosses must extend `AbstractBoss`. Logic in the game does type checks on these classes. Replace
the inheritance layers with components (for example a health component) and remove the type
checks.

## Scoping scan (2026-11-02, `master` at `71761c21c`)

Counts are `grep` results, so they can include a few comments. Check them again before the work starts. The subclass counts for `AbstractEnemy`, `AbstractBoss` and `AbstractProjectile` come from the `O1` audit. Some enemy and boss files are fully commented out, for example `ReactorMan_OLD.kt` (see `092`).

### Enemy, boss and health layers

| Area | Today | Measured size | Destination |
|---|---|---|---|
| Health | `AbstractHealthEntity`, `IHealthEntity` | 128 `AbstractEnemy` subclasses (17 of them through `AbstractBoss`), `Megaman`, 6 other classes | Health component |
| Taking damage | `canBeDamagedBy`, `takeDamageFrom`, `editDamageFrom`, `invincible` | 36, 25, 4 and 17 overrides | Component with gates and reactions; `023` owns the amounts |
| Dealing damage | `canDamage`, `onDamageInflictedTo` | 18 and 51 overrides; 272 `IDamagerEntity` implementers | Same pattern |
| Hit feedback | `damageBlink`, spawn blink | 155 lines in 148 files; `spawn_blink` 314 times in TMX | Data component and a system (`017`) |
| Culling | Set in `AbstractEnemy.onSpawn` and in `AbstractProjectile.onSpawn` | `cull_out_of_bounds` 975 times in TMX | One component, then `041` |
| Death | Drops, `disintegrate`, `explode`, points | 24 `onHealthDepleted` overrides; `explode(` in 39 lines | `ItemDropComponent` and its system for drops (`ITEM.md`, "Decisions"); plain functions for the rest |
| Helpers | `spawnWhackForOverlap`, `spawnFloatingPoints`, `isMegamanShootingAtMe` | 0 overrides | Plain functions |
| Boss flow | `ready`, `defeated`, `isReady`, `onReady`, `triggerDefeat`, `explodeOnDefeat` | 21, 9 and 8 overrides of `isReady`, `onReady`, `triggerDefeat`; `defeated` in 101 lines in 28 files | `BossComponent`, with states from `015` |
| Boss readers | `MegaLevelScreen`, `Gate`, `IBossListener` | 5 typed reads, 123 listeners | Read the component, not `AbstractBoss` |
| Identity | `is AbstractEnemy`, `getType() ==` | 12 lines in 8 files; 39 lines in 30 files | Capability checks |
| Template methods | `defineBodyComponent`, `defineSpritesComponent`, `defineUpdatablesComponent` | 150, 150 and 145 overrides in enemies and bosses | Each `init()` adds the components |
| State contracts | `IFreezableEntity`, `IFaceable`, others | 65 `IFreezableEntity` implementers, 64 with the same delegate | Components |

### Projectile layer

| Part | Today | Measured size | Destination |
|---|---|---|---|
| Base layer | `AbstractProjectile`, `IProjectileEntity` | 69 subclasses, 73 and 98 lines | Removed; the classes extend `MegaGameEntity` |
| Hit hooks | `hitBlock`, `hitShield`, `hitProjectile`, `hitBody`, `hitSand`, `hitWater` | 133 overrides: 57, 34, 14, 11, 9, 8 (`hitExplosion` not counted) | Handlers in a `ProjectileComponent`, called by `ProjectileRules` |
| Owner | `IOwnable<IGameEntity>` | 108 implementers | Component, with the design from `030` |
| Type checks | `is` or `as` on `IProjectileEntity` or `AbstractProjectile` | 24 lines in 20 files | Capability checks |
| Helper | `explodeAndDie` | On the interface | Plain function |

`AbstractProjectile` and `AbstractEnemy` each have their own copy of the `onDamageInflictedTo` callback, `movementScalar` and the cull setup.

### Risks

- **OR gates:** at least 13 `canBeDamagedBy` overrides have the form `damager is Axe || super.canBeDamagedBy(damager)`, so that damager skips the invincibility check. A plain list of AND gates cannot express this. The count comes from a listing of 30 of the 36 overrides.
- **Full replacements:** `Petal`, `Needle`, `CactusMissile`, `HomingMissile`, `PinkBlock` and `Wanaan` replace the damage logic and do not call `super`.
- **Boss death:** `AbstractBoss.definePointsComponent` replaces the base listener, so `onHealthDepleted` and `onHealthDepletedCallbacks` do not run for a boss. One shared death path would change this behavior.
- **Hidden order:** `AbstractBoss.onSpawn` writes `DROP_ITEM_ON_DEATH` and `CULL_OUT_OF_BOUNDS` to `spawnProps` before `super.onSpawn`.
- **Reset order:** `reset()` runs before `onDestroy`, but drops and defeat orbs read health at destroy. A new component must not clear that value (`087`, `112`).
- **Exact class keys:** `addComponent` keys on the exact class, so a boss component cannot be a subclass of an enemy component.
- **Performance:** each damage contact changes from 2 casts to 2 map lookups (`063` `NOTES.md`). This needs a measurement for `O5`.
- **Hook order:** hit hooks run during contact processing, so a handler must see the same state in the same contact pass.
- **`030` overlap:** `030` asks whether one `owner` is enough, so a move of `owner` before `030` can touch the same field twice.

### `065`

Only 1 enemy or boss file with an `init()` override has no `super.init()`, so `065` is a small part of this item's risk. This is a per-file check, not a check of each override.
