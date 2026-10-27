# 062: One engine-owned entity existence state

- Status: active
- Type: refactor
- Depends on: none
- Related: `019`, `021`, `038`, `047`, `048`, `061`
- Branches: `mega-refactor-062a-entity-state` (merged), `mega-refactor-062b-spawn-order-guards` (merged), `mega-refactor-062c-existence-readers` (merged), `mega-refactor-062d-queued-spawn-query` (active)

## Goal

Remove the confusion between two existence flags and health death. Today `IGameEntity.spawned` (engine, set when the
engine applies its queues) and `MegaGameEntity.dead` (core, set by engine callbacks when a change is queued) describe
one fact, existence, are written by different code at different moments, and disagree on some paths (for example
`engine.reset()`/`dispose()` leave `dead = false` with `spawned = false`). `dead` also reads like health death. The
engine should be the only source of truth, and callers should ask a named question.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. From the review in `NOTES.md` (option (b)):

- `062a`: an engine `EntityState { UNSPAWNED, SPAWN_QUEUED, LIVE, DESTROY_QUEUED }` on each entity, written only by
  the engine (`internal set`) in every path (`spawn`, `spawnNow`, `destroy`, `destroyNow`, `reset`, `dispose`).
  `spawned` and `dead` become getters derived from it, with today's values on every path except the
  `reset`/`dispose` fix. The `dead` writes in `MegamanMaverickGame` go. Odd transitions keep today's behavior and are
  logged. Engine tests for each transition.
- `062b` (added 2026-10-26): engine guards for the odd paths, with the `062a` log as evidence (see "Decisions").
- `062c`: move the readers in batches (bosses, enemies, special and blocks, Megaman, screens) to named read-only
  helpers (for example `inEngine` = today's `spawned`, `exists` = today's `!dead`), each reader mapped by the question
  it asks. Keep a player-level `dead` derived from the state where it means "the player died".
- `062d`: remove `engine.contains(..., containedIfQueuedToSpawn)` if the state replaces it, and the pool return of a
  rejected spawn (`MegamanMaverickGame`) that calls it. A plain state check there is not equal: an entity whose spawn
  is queued and then destroyed is `UNSPAWNED` but still in the spawn queue. (`062c` already removed `spawned` and
  `dead`; `PlayerView` no longer exists.)
- Later, only with evidence: decide the odd transitions; a spawn generation (`EntityRef`) if the pooled-reuse (ABA)
  risk is confirmed.

## Owner's input

2026-10-26 (verbatim): "I've actually been thinking about removing the `spawned` and `dead` flags and instead forcing
callers to delegate to the engine (or a global helper func) to remove this confusion. Dispatch an agent to review
this idea, and if it seems like a solid idea, then make a new item for it."

## Decisions

- 2026-10-26: the review found the goal solid, so this item exists (owner's condition). The review recommends a
  different mechanism from the owner's first idea (one engine-owned state field instead of no state and engine
  queries or a global helper); the owner decides (open question 1).
- 2026-10-26 (owner, "engine-owned state field is fine"): the mechanism is the review's option (b): one
  engine-owned `EntityState` field on each entity, written only by the engine in every path, read through read-only
  helpers. Open question 1 is closed.
- 2026-10-26 (owner): the state type is `EntityExistenceState` (property `existenceState`); on a spawn call while a
  destroy is queued, `dead` gives the true value (`true`); the `reset`/`dispose` fix is an intended change of `062a`
  (question 3); the pooled-reuse check waits for `019b`, and `062a` goes before `019b` (questions 6 and 7).
- 2026-10-26 (owner, "Guards in new branch"): `062a` keeps the engine's actions on the odd paths and logs them. A new
  branch `062b` adds the guards with that log as evidence: reject a spawn of a live entity, apply a same-tick respawn
  after the destroy (the last call wins), reject a destroy of an unspawned entity (questions 4 and 5). The readers
  move to `062c` and the removals to `062d`.
- 2026-10-26 (owner): the engine's real behavior is proved by tests (temporary old-versus-new suites and permanent
  tests), not by guesses or by play only. Asked for `062b` (the guards on `onSpawn`/`onDestroy` order); `062a` applies
  it too.
- 2026-10-27 (owner, in `062c`): the questions are properties of `EntityExistenceState` (`liveNow`,
  `liveAfterUpdate`, one constructor value per state), not members of the entity or queries on the engine; no
  player-level `dead`; `062c` removes `spawned` and `dead`. Reasons in the `062c` `docs/NOTES.md`.

## Open questions

1. Closed (2026-10-26): the engine-owned state field (option (b)).
2. Closed (2026-10-27): `liveNow` / `liveAfterUpdate` on `EntityExistenceState`; no player-level `dead`.
3. Closed (2026-10-26): an intended change in `062a`.
4. Closed (2026-10-26): `062b` applies it after the destroy (the last call wins); `062a` logs it.
5. Closed (2026-10-26): `062b` rejects it; `062a` logs it.
6. Closed (2026-10-26): wait for `019b`.
7. Closed (2026-10-26): `062a` goes before `019b`.

## Sources

- `NOTES.md` in this folder.
- Item `061` `NOTES.md`, "'Lifecycle' versus spawning, spawned and dead" (the three axes: existence, play phase,
  health death).
