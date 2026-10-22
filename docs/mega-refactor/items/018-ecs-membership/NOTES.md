# Notes: 018 Event-driven system membership; component IDs when needed

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 17. ECS internals: component IDs and membership".

````text
(1) Current state
- `GameEntity.components` is an `OrderedMap<KClass, IGameComponent>` (`GameEntity.kt:14`). `getComponent` and `hasComponent` are hash lookups.
- `GameSystem.update` (`GameSystem.kt:68-81`) applies the queued adds and removes, then runs `entities.removeAll { !qualifies(it) }` on every frame. `qualifies` is `componentMask.all { e.hasComponent(it) }`. Then it allocates `ImmutableCollection(entities)` (one small object for each system for each frame).
- Membership is already updated at spawn: `GameEngine.spawnNow` calls `updateSystemMembershipsFor` after `onSpawn` (`GameEngine.kt:72-80`). `destroyNow` removes the entity from all 13 systems (`:93-101`).
- I scanned all `addComponent`/`removeComponent` calls in core. 920 are in `init`, 275 in `defineBodyComponent`, and the rest are in `onSpawn` or in define helpers that `onSpawn` calls (`Water`, `Force`, `GravityChange`, `ForceDecoration`, `ConveyorBelt` …).
- I found no component removal while an entity is alive. So the per-frame `removeAll` never removes anything today. It is pure overhead.

Scale:
- Live levels have 285-572 TMX objects (for example `InfernoMan_16x14_v3` 572, `GlacierMan_16x14_v2` 570, `IntroStage_v3` 285). Only the objects near the camera are alive.
- Estimate: a few hundred live entities and about 1-2k mask hash lookups for each frame over all 13 systems. That is about tens of microseconds per frame, or about 0.1-0.3% of a 16.6 ms frame.
- `getComponent` in the systems (for example `WorldSystem.kt:113`) is a cost of the same size.

(2) Options
- (a) Leave the storage as it is. Remove the per-frame `removeAll` and the `ImmutableCollection` allocation. Refresh membership when `addComponent`/`removeComponent` runs while the entity is spawned. Pros: about 30 lines; clear semantics. Cons: no speed-up for `getComponent`.
- (b) Integer component IDs plus `Bits` masks. Systems get event-driven membership, and `getComponent` becomes an array index. Pros: a measurable win once item 16 and the component refactor add many small components. Cons: you need a registry of component types, and all `getComponent(X::class)` calls change (they can stay as an inline wrapper).
- (c) Archetype storage (struct of arrays). Pros: the best cache behavior. Cons: a large rewrite, and it does not fit OO entities and pooled objects. Not worth it at this scale.

(3) Recommendation
- Do (a) early. It is cheap, it fixes the semantics, and it makes the later component work safe.
- Do (b) only when the component refactor starts or when profiling shows it. Note that the `removeAll` cost is not visible in `RuntimeDiagnostics` today, because each system measures only inside its own `process()`. Add one diagnostics entry around `GameSystem.update` first.
- The honest benefit: a small CPU and GC win today; mostly cleaner semantics, plus room for many more components.
- Fix `MutableOrderedSet.retainAll` (inverted) in the same pass.

(4) Open questions for the owner
- Do you plan entities that add or remove components in the middle of their life (for example a "frozen" component)? If yes, (a) must come before that work.

(5) Proposed backlog wording

Title: "Event-driven system membership; component IDs when needed"
- Remove the per-frame `removeAll { !qualifies }` and the per-frame `ImmutableCollection` allocation.
- Refresh membership when a component is added or removed on a spawned entity.
- Add a diagnostics entry for the cost of system bookkeeping.
- Later, if profiling or the component refactor needs it: integer component IDs, `Bits` masks, array lookup.

(6) Dependencies
- Item 16 and the backlog item "Replace entity inheritance with components" (they increase the number of components).
- Item 8 (profiling baseline).
- Bug: `MutableOrderedSet.retainAll`.
````
