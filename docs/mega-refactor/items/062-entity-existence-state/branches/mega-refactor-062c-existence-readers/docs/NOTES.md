# Notes: mega-refactor-062c-existence-readers

## Decisions

- 2026-10-27 (owner, "Go for it"): the existence questions are properties of `EntityExistenceState`, not members of
  the entity and not queries on the engine. The owner did not want more flags on the entity. Reasons for this place:
  - The engine already owns the truth: it is the only writer of `existenceState` (`internal set`). Where the value is
    stored does not change who owns it.
  - A query on the engine (`engine.isLiveNow(entity)`) costs hash lookups in its queues for each read (some readers
    run per contact), cannot give "the last accepted call wins" from the queues alone when an entity is in two queues,
    and needs an engine reference at each reader (tests with a relaxed MockK game would get mock answers). An entity
    getter that calls the engine has the same costs.
  - Engines whose entities are objects keep the state on the object and let the engine write it (Godot
    `is_queued_for_deletion()`, Unreal `IsValid(actor)`); engines whose entities are IDs ask the world (Bevy
    `world.get_entity(e)`). Our entities are objects.
  - On the enum, the entity shows only `existenceState`, and later questions go on the state type.
  - One constructor value per state (a table), not a getter with `||` lists: a new state must give both answers or the
    code does not compile, and a read is a field read.
- 2026-10-27 (owner, "Go with your recommendations"): no player-level `dead` (player readers ask
  `existenceState.liveAfterUpdate`); `062c` removes `spawned` and `dead`. Scope approved.

## Overlap with other branches

No other branch is active.
