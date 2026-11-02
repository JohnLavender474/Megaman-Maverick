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

## Agent report: Recompute on each component add or remove (2026-11-02)

### Should the engine recompute system membership on each component add or remove?

**Basis:** read and grep only, at `master` `b3e792bc4`. `HEAD` moved during the work, but that commit changed docs only. The working tree has one unrelated edit in `docs/mega-refactor/ROADMAP.md`. I did not run anything.

#### Direct answer

**No.** Do not recompute on each `addComponent` or `removeComponent`, now or as written in `018`.

- No production write changes the membership of a live entity.
- A direct notification is not safe today. It has at least 4 proved failures (Q3).

If a real caller comes, the best form is a dirty queue that the engine drains before each system's update (option b2 in Q3).

**One correction to the brief.** A live add of a masked component does exist, in `WilyFinalBoss`. It replaces a key that is already there, so membership does not change (Q2).

#### 1. What `018` plans

- `018` has status `idea` and is not approved (`018/ITEM.md:3,16`). Its 4 scope bullets are:
  - Remove the sweep and the `ImmutableCollection` allocation.
  - Refresh membership on an add or remove on a spawned entity.
  - Add a diagnostics entry.
  - Later, add integer component IDs.
- **Does its plan delete the sweep?** Yes (`ITEM.md:18`, `NOTES.md:23`).
- **Does this question conflict with it?** This question is the same as bullet 2, which is option (a). So the conflict is real: if the answer is no, bullet 2 must change.
- The owner has not approved it: "let's discuss this further, particularly what benefits we'll see" (`ITEM.md:27`).
- The open question "Will entities add or remove components in the middle of their life?" has no answer (`ITEM.md:32`).
- `NOTES.md` is out of date:
  - It says 13 systems. There are now 14.
  - It says `removeAll`. The code is now an iterator loop at `GameSystem.kt:88-95`.
  - Its cost of 0.1-0.3% of a frame is an estimate, not a measurement (`NOTES.md:19`).
- `029` already rejected this idea for now: "Hooks on component add/remove ... no current caller, so not now" (`029/NOTES.md:1720`).

#### 2. Who writes components, and when

Scan: `grep` and `awk` by enclosing function. There are 1,249 `addComponent(` lines, and 22 are comments. Only 8 code lines (add or remove) are at an indent of 12 or more, and I read each one. So no site hides in an inner-class `init` or in a lambda, unless the lambda is at indent 8 or less (limit: see "Not read").

| Group | Lines | Where | Live? |
|---|---|---|---|
| `init` bodies | 928 | entity `init` | No |
| `defineBodyComponent` | 270 | called from `init` | No |
| `TubeBeamerV2.kt:395-396` | 2 | `init:140` | No |
| `defineDrawables` | 11 | `Water`, `PolygonWater`, `ToxicWater`, `ToxicWaterfall`, `DrippingToxicGoop`, `Lava`, all from `onSpawn` | No |
| `onSpawn` | 7 | `MegaGameEntity:52,56`, `ForceDecoration:56`, `Force:96`, `GravityChange:59`, `ConveyorBelt:172-173` | No |
| `Saw.kt:161,202` | 2 | from `onSpawn:111-112` | No |
| `WilyFinalBoss.kt:1078` | 1 | from `onSpawn:247` | No |
| `Players.kt:17` | 1 | `postCreate`, before spawn | Not today; `047f` makes it live, but `PlayerComponent` is in no mask |
| **`WilyFinalBoss.kt:2305,2941`** | 2 | `startNextPhase:808`, from `:3328`, from the updatable at `:392` | **Yes, during `UpdatablesSystem`** |

**`WilyFinalBoss` detail.** These 2 lines write `DrawableShapesComponent`, which is a masked key. The key is already present from `onSpawn:247`, so `components.put` replaces the instance. Membership does not change.

**`removeComponent`: 7 lines, confirmed.**
- 6 are in `onSpawn`.
- The 7th is `Players.kt:24`. Only a test calls it, and it is in no mask.
- `clearComponents` has no production caller.

**Where `init` runs.** It has 3 entry points. A gate must cover all of them:
- `MegamanMaverickGame.kt:451`: not in the engine.
- `MegaEntityFactory.kt:42`: not in the engine.
- `spawnNow:122`: already in `entities` (`:120`).

**What a notification does in these cases.** The state and the set cannot give a safe gate:

| Moment | `existenceState` | `engine.contains` | Needed |
|---|---|---|---|
| Manual or factory `init` | `UNSPAWNED` | false | nothing |
| `spawnNow`, first spawn | `SPAWN_QUEUED` | **true** | nothing |
| `spawnNow`, respawn path | `RESPAWN_QUEUED`, **`liveNow` true** | true | nothing |
| Live | `LIVE`, `DESTROY_QUEUED` or `RESPAWN_QUEUED` | true | notify |
| `reset()` or `onDestroy` in `destroyNow` | `DESTROY_QUEUED`, **`liveNow` true** | false | nothing, or a ghost re-join |

- `liveNow` alone fires during a respawn `onSpawn`, and it re-adds an entity that is being destroyed.
- `contains` alone fires during `init` and `onSpawn`.
- A correct gate needs a new engine-written boolean, for example "is in systems". It is set at `GameEngine.kt:130` and cleared at `:187`.

**A concrete failure without a gate.** `MegaGameEntity.kt:56` adds `GroupsComponent`, and `:57` fills it. A join at `:56` sees empty groups. `GroupsSystem.kt:25` returns, and the entity is never in the group index.

**Hot path.**
- Every spawn with no `ConstKeys.ID` calls `removeComponent` at `MegaGameEntity.kt:49`, for example each `Bullet`.
- One recompute visits 14 systems, each with a 1-key mask. That is up to 28 component lookups and 14 set operations (`GameEngine.kt:212`, `GameSystem.kt:57,71`).
- A gated write costs 1 branch.
- I did not measure the spawn rate.

#### 3. Options

| | (a) notify at once | (b) dirty flag, drained at the spawn phase | (c) explicit call | (d) today |
|---|---|---|---|---|
| Write during a system's update | Own system queues. Other systems change at once. | Nothing until the next `update`. | Same as (a) if the call is direct. | Own write is plain. Sweep removes at the next system update. |
| Live remove, later systems this frame | Safe | **Member with no component** | Safe | Safe (sweep) |
| Live add of a new key | Joins now | Joins next frame | Joins on the call | **Never, until the next spawn** |
| Hook timing | Inside the writer's stack; varies by system position | Fixed point, one place | Same as (a) | Leave at sweep; join only at spawn |
| Remove then add in one frame | Leave and join; entity moves to the tail. **In its own system: lost** | Net result; no hook; order kept | Same as (a) | Stays a member |
| `GameEntity.engine` | Exists, non-null (`GameEntity.kt:12`) | Same | Not needed in the entity | Not used |
| Cost per write | Gate; recompute on a hit | Gate; 1 queue add | 0 | 0 |
| Cost per frame | 0 | 1 empty check | 0 | 1 lookup per system member |
| Tests | Below | `GameSystemHooksTest:101-114` | `:101-114` only if the sweep goes | None |

**Proved failures of (a), and of (c) if called directly:**

1. **Crash at the Wily phase change, or at any write during a system's update.**
   - `updateSystemMembershipsFor` runs `systems.forEach` (`GameEngine.kt:212`) inside `systems.forEach` (`:240`).
   - LibGDX shares its iterators. The nested call makes the outer iterator invalid, and `hasNext` throws `#iterator() cannot be used nested.` (`Array.java:725-728,774-788`, gdx 1.14.2).
   - No code sets `Collections.allocateIterators`.
   - `destroyNow:189` has the same nesting if a leave hook writes a component.
   - Mitigations: use an index loop, and skip a replace (`put` returns the old value, `OrderedMap.java:72-77`).
2. **Membership lost.** Remove then add, for a member during its own system's update: both calls queue. Adds drain first (`GameSystem.kt:76-79`, no change), then removes (`:82-85`). The entity has the component but is not a member, and the sweep never adds it back.
3. **Stale member.** Add then remove during own update: `remove` returns 0 and cancels nothing (`:46`). `:78` adds with no re-check. Today the sweep fixes this. With no sweep, `getComponent(...)!!` throws. `GameSystemHooksTest.kt:90` pins the 0 result.
4. **`066` becomes live.** Its text says no code reaches this path today (`066/ITEM.md:12`). Option (a) reaches it.

**Facts on plain (b).**
- With the sweep deleted, a remove during a frame leaves a stale member until the next update. 6 systems use `!!` (`WorldSystem:113`, `BehaviorsSystem:20`, `AnimationsSystem:19`, `SpritesSystem:21`, `DrawableShapesSystem:21`, `ControllerSystem:16`) and rethrow.
- So plain (b) must keep the sweep, or use (b2).

**(b2), the best form when a caller exists.**
- `addComponent` and `removeComponent` queue the entity only if the key set really changed and the "in systems" flag is true.
- The engine drains the queue before each system's update, in an index loop.
- This gives the sweep's leave timing in every later system. No `GameSystem.updating` is true during a drain, so failures 2, 3 and 4 cannot occur.
- Hooks fire at a fixed point (`O1-R3`, inference).
- Entities in the queue but gone from the engine are dropped.
- `init` and `onSpawn` writes cost 1 branch, and no hook fires.
- A mock entity never has the flag, so it never calls the strict `mockk()` at `MockGameEntity.kt:18`.
- Limits:
  - A hook that writes a component extends the drain.
  - An instance swap fires no hook.
  - A write in a system's own `process` still takes effect only after that system finishes.

**Iteration safety, all options.**
- A notification does not iterate the component map. `qualifies` uses `containsKey`, so a notification adds no new risk there.
- The risk is in the write. `removeComponent` during `entity.components.forEach` (`GameEngine.kt:191`) shifts `keys`, and the next component is skipped with no error (from `OrderedMap.java:107-108,260-267`, inference).
- A second `components.forEach` inside a `reset()` throws.
- I found no such write today. This belongs with `087`.

**Test impact of (a).**
- The strict mock is in 16 test files (56 lines). An ungated engine call throws in those tests.
- Core tests use a relaxed mock (`TestServices.kt:28`).
- `GameSystemHooksTest.kt:101-114` depends on the sweep. It is the only test that does.
- It also needs new tests for the queue order.

**Sweep cost and allocation.**
- The sweep itself allocates nothing (inference from the shared iterators).
- The allocation is `ImmutableCollection` at `GameSystem.kt:96`, 1 per system per frame, plus 1 `ImmutableIterator` per `forEach`. A field can replace the first with no change to membership.

#### 4. Is there a caller?

| Source | Result |
|---|---|
| Today | **No.** 0 writes that change membership on a live entity. |
| `048` | **No.** Roles are components, so they are set at `init`. `ItemDropComponent` reads in `onEntityLeft` at destroy. `FreezeComponent` is a flag. The `destroyNow` order must stay. |
| `018` | **No.** It is the plan, not a caller. It asks the question. |
| Hostile player role | **No.** No item names this change. `030`: faction is a value, copied at spawn, updated on a change of owner. `109`: teams are an open question. |
| `047f` join and leave | Live `Players.register` and `unregister`, but `PlayerComponent` is in no mask. |
| `031` possession | "No system mask contains `PlayerStateComponent`" (`031/NOTES.md:940`). |
| `017`, `114` | Open question on gravity rule changes (`017/ITEM.md`). A field in the component is enough (inference). |

- By `O1-R5` and `docs/mega-refactor/CLAUDE.md:207`, option (a) would have callers in name but no real effect. No write would reach the recompute.
- The first real caller is a mechanic with a new masked key that an entity must gain or lose while live, and that key must be a system mask key. I found no such mechanic.

#### 5. Flag in a component, or add and remove

**A flag is better when:**
- The state changes during an entity's life. `init()` runs once, so a removed component is gone at the next spawn unless `onSpawn` adds it again. `Force.kt:96` does this in both branches.
- The component holds data that must stay: `FreezeComponent` holds a rule and a callback.
- Other code keeps a reference. `MegaPlayer` reads its component once, so "remove the component only after `unregister`" (`031/NOTES.md:942`).
- Several reasons overlap. `LocksComponent` uses a bit per reason, and `029/NOTES.md:1643` gives this as the reason to avoid tag components.
- A read must be correct at once in the same contact pass (`048` decision, 2026-10-30).
- The system must still see the entity.

**Existing flag patterns:**
- `FreezeComponent.frozen`
- `RisingLavaRiver` `DORMANT`, an entity field
- `doUpdate` in `PathfindingComponent:9` and `MotionComponent:13`
- `doUpdateAndDraw`
- `body.physics` flags: 147 write lines
- A flag must clear in `reset()`, as `LocksComponent` and `FreezeComponent` do.

**Add and remove is better when:**
- Spawn data fixes the fact for the whole life and the system work is wasted. Examples: `Water.kt:126-127` when hidden, and `Force.kt:96` with no cull.
- This is the present use, and it needs no live recompute.

**Cost of a flag:** the system still visits the entity each frame.

#### Recommendation

1. Answer the `018` open question with this rule: an entity's set of masked keys is fixed from the end of `onSpawn` to destroy. Replacing an instance of a present key is allowed. Runtime state is a flag in a component.
2. Change `018` bullet 2 to "add b2 in the branch of the first real caller". Leave a `TODO` at `GameEntity.removeComponent`.
3. In `018`, keep these steps, in this order:
   - Add the diagnostics entry first (`O5-R2`).
   - Delete the sweep.
   - Rewrite `GameSystemHooksTest.kt:101-114`.
   - Make `updateSystemMembershipsFor` private. It has 1 caller, it has no gate, and it crashes if called inside a system update.
   - Share the `ImmutableCollection`.
4. Do not build (a).

If the owner does not want to delete the sweep yet, keep (d) until `009` gives a number. Either result is safe today.

#### Risks

- With no sweep, a later live remove of a masked key throws in 6 systems (`!!`, loud). In the other systems the entity stays a harmless stale member. Rule 1 and the `TODO` cover this.
- A live add of a new masked key stays silent, the same as today.
- `Wily` phase changes pass through the write path. Any future change to `addComponent` must be tested with a phase change.
- `019c` and b2 change the same lines (`GameEngine.kt:219-240`).
- `010` may run `update` more than once per render. The drain is per update, so this should be fine (inference).
- Separate bug candidate (inference): `MegaEntityFactory.kt:42` calls `init()` and never sets `initialized`. The factory has no such write, so 25 `Bullet` and 25 `Block` seem to run `init()` again at first spawn. I did not trace this further.

#### Open questions for the owner

1. Do you accept the rule in recommendation item 1? **I recommend yes.**
2. Delete the sweep in `018` with no replacement? **I recommend yes, after the diagnostics entry.** The gain is not measured yet.
3. Is any planned mechanic a true live gain or loss of a masked key, for example a player role that changes which systems process it? **I recommend a flag unless a named design needs a key.**
4. Must the `018` branch also make `updateSystemMembershipsFor` private? **I recommend yes.**

#### Not read in full

- `019/NOTES.md`, `062/NOTES.md`, `047/ITEM.md`: grep only.
- `114/ITEM.md`: grep only.
- `029`, `030`, `031` `NOTES.md`: sections only.
- `GameSystem` subclasses: grep of `process` lines, except `GroupsSystem`, which I read in full.
- `WilyFinalBoss.kt`: sections only.
- Engine tests: `GameSystemHooksTest` only; others by grep.
- `HeadlessHarness.kt`: grep only.
- The 928 `init` lines: by script, not line by line. A lambda stored at indent 8 or less would not show in this scan.

##### Critical Files for Implementation
- /home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/GameEngine.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/systems/GameSystem.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/entities/GameEntity.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/test/kotlin/com/maverick/game/engine/systems/GameSystemHooksTest.kt
- /home/johnlavender/IdeaProjects/Megaman-Maverick/docs/mega-refactor/items/018-ecs-membership/ITEM.md
