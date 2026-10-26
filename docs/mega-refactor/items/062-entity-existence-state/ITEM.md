# 062: One engine-owned entity existence state

- Status: planned
- Type: refactor
- Depends on: none
- Related: `019`, `021`, `038`, `047`, `048`, `061`
- Branches: none yet

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
- `062b`: move the readers in batches (bosses, enemies, special and blocks, Megaman, screens) to named read-only
  helpers (for example `inEngine` = today's `spawned`, `exists` = today's `!dead`), each reader mapped by the question
  it asks. Keep a player-level `dead` derived from the state where it means "the player died".
- `062c`: remove the `spawned` `var`, `MegaGameEntity.dead`, and `engine.contains(..., containedIfQueuedToSpawn)` if
  the state replaces it; `PlayerView.present` reads the state.
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

## Open questions

1. Closed (2026-10-26): the engine-owned state field (option (b)).
2. The helper names: `inEngine` / `exists`, or others? Keep `dead` only on the player?
3. The `reset`/`dispose` fix: an intended change in `062a`, or a separate bug in `000`?
4. A spawn while `DESTROY_QUEUED`: reject it, or apply it after the destroy? (Today it fails without a sign;
   `FreezableEntityHandler` can hit it.)
5. A `destroy()` of an `UNSPAWNED` entity: should the engine ignore it? (Today it runs `onDestroy` and the pool free
   again.)
6. Investigate the bullet-pool reuse risk now (a debug check in `cullAllDead`), or wait for `019b`?
7. The order with `019b` (destroy notification), which also touches the spawn and destroy paths.

## Sources

- `NOTES.md` in this folder.
- Item `061` `NOTES.md`, "'Lifecycle' versus spawning, spawned and dead" (the three axes: existence, play phase,
  health death).
