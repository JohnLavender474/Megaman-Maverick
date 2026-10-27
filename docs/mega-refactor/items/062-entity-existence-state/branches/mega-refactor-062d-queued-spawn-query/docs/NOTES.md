# Notes: mega-refactor-062d-queued-spawn-query

## Decisions

- 2026-10-27 (owner, "I think I like option b: a specific query. Since it's only one spot in the code, I think this
  is a case where the func option is better"): a named engine query, not a sixth state. The owner found the state
  name `SPAWN_THEN_DESTROY_QUEUED` confusing; the state would mean "the engine runs `onSpawn` and then `onDestroy`
  before the entity is free".
- 2026-10-27 (owner, "Yes"): `engine.contains(entity)` becomes `internal` (no public caller; a public one would
  differ from the state inside `onSpawn`/`onDestroy`); the query is named `hasClaimOn`. Scope approved.

## Overlap with other branches
