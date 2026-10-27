# Scope: mega-refactor-062d-queued-spawn-query

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-27

## Goal

The pool return of a rejected spawn asks one named engine question, `engine.hasClaimOn(entity)`, in place of a
flag on `engine.contains` plus a state check. `engine.contains(..., containedIfQueuedToSpawn)` goes. This is the last
branch of item `062`.

## Item

From item `062` `ITEM.md`, "Proposed scope" (as updated at the `062c` merge):

> - `062d`: remove `engine.contains(..., containedIfQueuedToSpawn)` if the state replaces it, and the pool return of a
>   rejected spawn (`MegamanMaverickGame`) that calls it. A plain state check there is not equal: an entity whose spawn
>   is queued and then destroyed is `UNSPAWNED` but still in the spawn queue. (`062c` already removed `spawned` and
>   `dead`; `PlayerView` no longer exists.)

## Current state

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

### The only caller

`C/MegamanMaverickGame.kt:927-933`, the engine's `onSpawnRejected` callback. `GameEngine.spawn` calls it when
`canSpawn` returns `false` (`E/GameEngine.kt:82-84`), before any other check:

```kotlin
val entity = it as MegaGameEntity
if (!entity.existenceState.liveNow && !entity.engine.contains(entity, containedIfQueuedToSpawn = true))
    entity.runnablesOnDestroy[ConstKeys.POOL]?.invoke()
```

It returns a rejected pooled entity to its pool, but only when the engine has no claim on it: not in the engine's
entity set, not in the spawn queue, and not live now. Freeing an entity that the engine still holds would put it in
the pool twice (its `onDestroy` frees it again later), and a later `fetch` could give one object to two owners.

`engine.contains(entity, containedIfQueuedToSpawn)` (`E/GameEngine.kt:78-79`) has no other production caller. One
engine test uses `engine.contains(e)` (without the flag) as an invariant: `liveNow` equals membership in the engine
(`EntityExistenceStateTest.kt:465`).

### Where the state and the claim disagree

For each state, does the engine hold a claim (in `entities`, the spawn queue or the respawn queue)?

| State | Claim? |
|---|---|
| `SPAWN_QUEUED`, `LIVE`, `DESTROY_QUEUED`, `RESPAWN_QUEUED` | always yes |
| `UNSPAWNED` | no, except in the case below |

The exception: a spawn call is queued, and then a destroy call comes before the next update. `destroy()` sets
`UNSPAWNED` (`E/GameEngine.kt:162-167`; the entity is not live after the update), but the spawn stays in the queue,
so the next update runs `onSpawn` and then `onDestroy`. The same holds in `destroyNow` when the entity is in both the
spawn and the kill queue (`E/GameEngine.kt:197-205`). While the entity's `onSpawn` runs in that update, it is in
`entities` and its state is still `UNSPAWNED`.

So today "`UNSPAWNED`" does not mean "the engine has no claim". That is why the pool return needs the queue query.

### Readers of `UNSPAWNED`

No reader in `core` compares with `UNSPAWNED` or `SPAWN_QUEUED`. The questions `liveNow` and `liveAfterUpdate` are
`false` in that case, and they stay `false` with either option below. The engine tests check `UNSPAWNED` in that case
(for example `EntityExistenceHooksTest`, "runs once with UNSPAWNED for a destroy of an entity whose spawn is queued").

## In scope

Option (b), owner decision (2026-10-27): one call site, so a named engine function, not a new state.

1. `GameEngine.hasClaimOn(entity: IGameEntity): Boolean` (first and only caller: the pool return): `true` when the
   engine still holds the entity, that is, when it is live now (`existenceState.liveNow`), in the engine's entity set,
   or in the spawn queue. This is exactly the negation of today's condition
   (`!liveNow && !engine.contains(entity, containedIfQueuedToSpawn = true)`). The `liveNow` part stays: inside an
   entity's own `onDestroy`, the entity is no longer in the entity set or a queue, but its state is
   `DESTROY_QUEUED`, and its `onDestroy` frees it to the pool itself.
2. The pool return becomes `if (!entity.engine.hasClaimOn(entity)) entity.runnablesOnDestroy[ConstKeys.POOL]?.invoke()`.
   It frees in exactly the same cases as today.
3. Remove the `containedIfQueuedToSpawn` parameter. `engine.contains(entity)` then has no production caller (see open
   question 1).
4. Tests: permanent engine tests for `hasClaimOn` on each path (never spawned, spawn queued, live, destroy queued,
   respawn queued, inside the entity's own `onDestroy`, a spawn and then a destroy before the update, after the
   destroy); a temporary suite on seeded random sequences with nested calls that checks, before every call, that
   `hasClaimOn` equals the old condition (run while the old flag still exists, deleted before the merge).

## Out of scope

- Any change of engine actions, of the states, or of the `062b` rules. A sixth state for "a spawn queued, then a
  destroy" (option (a)) is not done: one call site does not need it (owner decision).
- The ABA risk of pooled objects (a spawn generation); item `062`, "Later, only with evidence".
- `Spawner.spawned` and other meanings of "spawned".
- Item `063` (entity contracts, `destroy()` on `IGameEntity`) and the `Starkner` issue.

## Affected areas and risks

- `E/GameEngine.kt` (`contains`, the new `hasClaimOn`), `C/MegamanMaverickGame.kt:927-933`, and the engine tests.
- Risk: a `hasClaimOn` that differs from the old condition on a rare path, so a pooled entity is freed twice or not
  at all. Mitigation: the temporary suite compares both on random sequences, including nested calls inside `onSpawn`
  and `onDestroy`; the permanent tests cover each path.
- Gameplay: none expected.

## Overlap with other branches

No other branch is active. Item `019c` (merge the four spawn queues) comes later and will read the state.

## Open questions

1. Closed (owner, 2026-10-27): option (b), a named engine query, because only one place asks it.
2. Dropped with option (a): the name of a new state.
3. Closed (owner, 2026-10-27): `engine.contains(entity)` becomes `internal`. With `existenceState` and `hasClaimOn`,
   a public `contains` would only add a third existence answer that differs from the state inside the engine's own
   callbacks; the engine tests keep it to check that `liveNow` equals membership.
4. Closed (owner, 2026-10-27): the name is `hasClaimOn`.
