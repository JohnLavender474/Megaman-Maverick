# 181: Entity pools and factories owned by the session

- Status: idea
- Type: refactor
- Objectives: `O1`, `O6`
- Depends on: none
- Related: `006`, `045`, `021`, `016`
- Branches: none yet

## Goal

Each game session owns its entity pools, factories and log setup. A second game in the same JVM (a headless test of
`006`, a later restart or mode change) starts clean and never gets entities that hold the services of an earlier game.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Move the pools of `MegaEntityFactory` to an instance that the session owns (for example reached through
  `MegaServices`). Decide what happens to the 345 `fetch` call sites.
- The same for the deprecated `EntityFactories`, or remove its last callers first.
- Add the `GameLogger` log receiver and tags once per process, not once per `create()`.
- Static gameplay state such as `ToxicLiquidClock` goes into the session or resets at level start.
- `GameObjectPools` scratch: decide with `045` if it stays global (no session state) or moves.

## Owner's input

- 2026-11-23 (owner accepted the recommendation): this is its own item, not part of the `006` scope, because it
  changes how every entity is pooled. It comes before `006`, which needs a clean boot per level with no reset
  workaround (`docs/ways-of-working/planning.md`).

## Open questions

- Is the factory reached through `MegaServices`, or through a factory service that the entity gets in its
  constructor?
- One factory per session, or one per level?

## Sources

- `NOTES.md` in this folder.
