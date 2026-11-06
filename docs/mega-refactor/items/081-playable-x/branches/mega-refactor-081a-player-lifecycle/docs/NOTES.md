# Notes: mega-refactor-081a-player-lifecycle

## Decisions

### `invincible` and `canMove` (2026-11-05, draft, `master` at `444681c2d`)

Recommendation from a scan: 2 free functions that compute the value at each read. No lock change, no component, no
stored value.

Readers:

- `canMove`: 9 production readers in 9 files, all `Megaman` code (`ControllerComponent.kt:186` and 8 behaviors). No
  reader writes it. Other classes have unrelated `canMove` members (for example `AbstractProjectile.kt:18`), so the
  shared function must not be named `canMove`.
- `invincible`: overrides `IDamageableEntity.invincible`. 5 production readers, 4 outside `Megaman` code:
  `AbstractHealthEntity.kt:76`, `DeathRule.kt:28`, `PlayerDeath.kt:47`, `TimberWoman.kt:729`, `SmallIceCube.kt:225`.
  `EntityQueries.kt:19` already has an `IGameEntity.invincible`, so the shared function is `playerInvincible`.

Forms that were not chosen:

| Form | Reason not chosen |
|---|---|
| `canMove` as `!isLocked(MOVEMENT)`, with stun, freeze and damage as lock reasons | 2 sources of truth: the states stay, and many readers read them directly. `StunComponent` has no end callback, and `setToEnd` or `reset` ends a state with no write, so a lock can stay on. `onSpawn` would get a new order rule (`O1-R3`). The rule would spread into shared writers |
| `invincible` as a `DAMAGE` lock only | Cannot express `!liveAfterUpdate` or `!inPlay`. Changes `SmallIceCube.kt:228`, which shatters on a `DAMAGE` lock. The `048` owner decision: invincibility is never stored |
| A component that holds the rule | `084g` kept `canMove` as a member and rejected a component (no rule differs between characters). The invincibility component for all entities belongs to `048` |
| A field on each player class | X would copy it (`O8-R1`) |
| A plain `var` or a callback | Goes stale, because timers end with no write; adds an order rule |

Lock facts from the scan: `LocksComponent.reset()` does not run the `onChange` hook, and `isLocked` returns `false`
for an entity with no `LocksComponent` (`engine/.../locks/Locks.kt:12`). The `DAMAGE` lock reasons are `TELEPORT`,
`SPAWN`, `LEVEL_END` (no unlock found), `BOSS_DEFEATED` and `WILY_FINAL_BOSS` (no unlock). `onSpawn` clears both
groups.

### Locks for stun, freeze and damage: not in `081a` (owner, 2026-11-05)

If stun, freeze and damage get start and end hooks later, `LocksComponent` is the right home for the reasons that block
movement: the lock becomes the source of truth (`getLockGroup(MOVEMENT).isLockedBy(STUN)`, the API from
`fix/lock-group-api`), and `playerCanMove` becomes `!isLocked(MOVEMENT)`. `081a` does not do this: it is a pure move.
The free function keeps the later change to its body only. Before the change: end hooks on every end path of
`StunComponent` and `FreezeComponent`, the damage timer (`048`), `LocksComponent.reset()` and the change hook, the
`SmallIceCube.kt:228` reader, and a review of the `048` decision that invincibility is never stored.

## Overlap with other branches
