# Notes: 020 Cheaper broad phase with stable contact order

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 19. Broad phase (sanity check)".

````text
(1) Current state
- In each sub-step, `cycle` clears the container and adds all bodies and active fixtures again (`WorldSystem.kt:169-187`). After the loop it rebuilds the container one more time (`:136-142`). At 2.5 sub-steps per frame, that is the "about 3.5x" rebuild.
- `Fixture.getShape()` (`Fixture.kt:51-65`) recomputes the shape on each call. It is called for each fixture insert, for each query fixture, and for each candidate in `collectContacts`.
- `Contact` equality does not depend on the order of the two fixtures (`Contact.kt:54-58`). So a pair found from both sides is de-duplicated by `currentContactSet.add`, and the first orientation found wins.

The proposal is sound. These things must be preserved:
- The callback order: `processContacts` goes through the `OrderedSet` in insertion order. That order comes from the body order (system insertion order), the fixture order, and the grid candidate order. "Test each pair once" must keep the same first-found orientation and the same order, or `MegaContactListener`'s first-match branches can change behavior. This can be checked with the golden replay.
- Begin, continue and end are computed for each sub-step.
- `BodyType.STATIC` does not mean "does not move". `Block` is STATIC (`Block.kt:255`), and its subclasses move (for example `AnimatedBlock.kt:93` sets the velocity). `Body.process` translates STATIC bodies too. A persistent static layer must use an explicit "immobile" flag or detect movement. It must not use the body type.
- A shape cache must be invalidated when the body moves. That happens in `process`, in `resolveCollisions`, and in the contact listener. The safe first step: cache only inside the `collectContacts` phase.

(2) Options
- (a) A persistent layer for immobile bodies, rebuilt on spawn, destroy or move.
- (b) Pair de-duplication with a stable order.
- (c) Shape cache for each phase.
- Do (c) first (lowest risk), then (a), then (b).

(3) Recommendation: as above. Gate each step on the replay or contract check.

(4) Open questions for the owner
- Is there a list of "moving static" blocks? Or may an `immobile` flag be added in Tiled?

(5) Proposed backlog wording

Title: "Cheaper broad phase with stable contact order"
- Cache fixture shapes inside the contact-collection phase.
- Keep immobile bodies in a persistent layer, decided by an immobile flag, not by `BodyType.STATIC`.
- Test each pair once, but keep the current callback order and orientation.

(6) Dependencies
- Item 9 (the sub-step count).
- Item 13 (the contact table makes the order rules explicit).
- The replay item and item 5 (verification).
````

## Scan: the size of the work (2026-10-25)

Based on `master` at `9cd13b5a5`. Four read-only scans: the frame flow and contact order, fixture shapes, static
bodies that move, and verification. Paths: `E` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`C` = `core/src/main/kotlin/com/megaman/maverick/game/`, `R` = `C/world/contacts/rules/`. Some line numbers in the
review above are stale (for example `Contact.kt:54-58` is now `:12-16`).

### Summary

- The "about 3.5 rebuilds per frame" claim is correct. Every performance tier has `fixedStep * fps = 0.4`, so a
  frame has 2 or 3 sub-steps (2.5 on average). Each sub-step clears the container and adds every body and every
  active fixture again. One more rebuild runs after the loop.
- **Nobody has measured the cost.** There is no real `--diagnostics` capture in the repo. The files in
  `utils/diagnostics/assets/` are synthetic test data. The git history only gives anecdotes (`f958aee3d`,
  `b96412d62`, `9c2bfeb6e`). The item is based on structure, not on a measured hot spot.
- `Fixture.getShape()` does not allocate. It reuses one shape for each fixture, but it recomputes the shape on each
  call: per insert, per querying fixture, and per candidate.
- No test checks which contacts are found, their order, their orientation, or the order of the collision calls.
  The golden replay (`055`) and the headless boot (`006`) are not done. A side-by-side harness in the engine tests
  can be built now (see "Verification").
- The answer to the open question: there is no list of "moving static" blocks, and there is no `immobile` property.
  All 2,336 plain `Block` objects in the 16 live maps are immobile, but about 13 code owners move a plain `Block`
  each frame (`Spike` is one of them, and it is common).
- Blocks spawn when they come into the camera and are culled 1 s after they leave it. So the set of bodies changes
  on any frame, not only on a room change. This makes a persistent layer harder, and it also makes it less useful,
  because few blocks are alive at one time.

### Frame flow (`E/world/WorldSystem.kt`)

```
process (100):
  if accumulator >= fixedStep:
    bodyArray = the system entities in insertion order, if doUpdate()          (111-118)
    while accumulator >= fixedStep: cycle(...)                                  (122-133, max 6 iterations)
    post-loop rebuild: clear; addBody only if collisionOn; add active fixtures  (136-142)
cycle (164-206):
  preProcess (all) -> clear -> process (all) -> addBody (all, also collisionOn = false)
  -> add active fixtures -> collectContacts -> processContacts -> resolveCollisions -> postProcess
```

- The core container is `SimpleGridWorldContainer(ConstVals.PPM)` (`C/screens/levels/MegaLevelScreen.kt:441`):
  `LongMap<OrderedSet>` cells of 32 px. The cell sets are pooled, so a rebuild in steady state does not allocate.
- The post-loop container is not read inside `WorldSystem`. It is read during the next frame by the pathfinding
  snapshot (`MegaPathfinderFactory.kt:24-34`, `container.copy()`), `DynamicBodyHeuristic.kt:23-24`, and container
  queries in `Bat`, `FloatingCan`, `DeathBat` and `SubmarineJoe`. It must stay, and it must stay unchanged on a
  frame with no step.
- Allocations per sub-step: a capturing lambda per querying fixture in `collectContacts` (`:241`), per body in the
  insert (`:183`) and in the post-loop rebuild (`:140`), and per non-static body in `resolveCollisions` (`:268`).
  `tempFixtureSet.clear()` costs O(capacity) per query.
- Redundant work: `collectContacts` checks `candidate.isActive()`, but only active fixtures are inserted. Each pair
  that both sides query (BODY-BODY, BODY-FEET, BODY-SIDE, SIDE-SIDE, HEAD-FEET, LADDER-HEAD/FEET, PLAYER-BODY,
  PROJECTILE-x, EXPLOSION-BODY, LASER-BODY, CONSUMER-x) runs the filter and the narrow phase twice. The second
  result is dropped by `currentContactSet.add`.

### Contact order and orientation

- Discovery order: bodies in `bodyArray` order, then each body's fixtures (the `OrderedMap` type order, then the
  insertion order in each type), then the grid candidates (column outer, row inner, then cell insertion order; the
  first occurrence wins). Cell insertion order is the order of the last rebuild, which is the body order times the
  fixture order. The grid query has no hash-map iteration, so it is deterministic.
- Orientation: `fixture1` is the querying fixture (`:247`). For a pair that both sides query, the fixture of the
  body that comes first in `bodyArray` wins. A later duplicate keeps neither its position nor its orientation.
- `ContactRuleTable` flips different-type pairs into role order, so their orientation does not matter. Same-type
  pairs are not flipped (`ContactRulesBuilder.kt:8-12`). Same-type rules in core:
  - `BodyBodyRule` and `SideSideRule` call `fixture1`'s receiver first (`R/ReceiverRules.kt:20-30`, `:81-84`).
  - `ProjectileRule` for PROJECTILE-PROJECTILE is asymmetric: only `fixture2` gets the `NO_PROJECTILE_COLLISION`
    guard and the `HIT_BY_PROJECTILE` receiver (`R/ProjectileRules.kt:28-33`, `:82-86`).
  - `ConsumerRule` for CONSUMER-CONSUMER calls only `fixture1`'s consumer (`R/ReceiverRules.kt:11-13`).
- `processContacts` runs all BEGIN and CONTINUE calls in insertion order, then all END calls in the order of the
  previous set, with the previous orientation. CONTINUE can have the opposite orientation to BEGIN.

The rules depend on the order inside one sub-step in these ways:

- **END after BEGIN/CONTINUE.** Boolean senses with no count (for example `BlockSideRule`, `HeadBlockRule`,
  `BlockBodyRule`, `FeetIce`, `SideIce`, `FeetSand`, `FeetSnow`, `BodyForce`, the ladder rules, `FeetCartRule`) are
  correct only while this two-pass split stays.
- **The last writer wins:** `frictionOnSelf` (ice against sand or snow), the `LADDER` property with two ladders,
  overlapping gravity zones, and the cart owner.
- **Contacts that move the body in the pass:** `FeetBlockRule` (`:35-36`, `:65-66`), `BlockSideRule` (`:87-88`),
  `TeleporterRule` (`:399`). Later shape checks in the same pass (ladders, gravity point, lasers, the
  `ProjectileRule` shape copies) see the new position.
- **A flag that one contact writes and another reads:** `IN_WATER` read by `STICK_TO_BLOCK`; `FEET_ON_GROUND` read
  by the ice friction filter; `wasInWater` with two waters; `DamageRule`, where the first damager makes the entity
  invincible, so the order decides the damage, `lastDamager` and Mega's knockback direction; `PlayerItemRule` in
  co-op; the laser intersection list (`R/LaserRules.kt:43-46`).

`resolveCollisions` also depends on order. Only non-static bodies query. Candidates come in grid order, and a
DYNAMIC-DYNAMIC pair is called twice. `StandardCollisionHandler` picks the axis from the overlap, so at a corner the
first block changes the result of the next. `MegaCollisionHandler` snaps to absolute positions, so the last block
wins.

Conclusion: any change to the discovery order, the orientation of a same-type pair, or the candidate order in
`resolveCollisions` can change gameplay.

### Fixture shapes and cache invalidation

- `Fixture.getShape()` (`E/world/body/Fixture.kt:51-65`): `setTo(rawShape)`, then `body.getBounds` (with a rotation
  if the direction is not UP), a position point, `setCenter`, `translate`, then a rotation. `GameRectangle.rotate`
  is the expensive part (`GameRectangle.kt:125-165`). It returns early for rotation 0.
- From the insert to the end of `collectContacts`, nothing changes a body or a fixture. The only core code that runs
  in this window is `MegaContactFilter` and its 39 `Receivers.FILTER` lambdas, and all of them are pure. So a cache
  that is filled at the insert and dropped before `processContacts` is safe.
- A cache for the whole sub-step is not safe. The changes come from many places:
  - 145 of the 222 `preProcess` hooks change raw shapes, sizes, offsets, `active` flags or directions directly,
    without a setter.
  - In the contact listener: `FeetBlockRule`, `BlockSideRule`, `PipePortal.teleport`, and gravity changes, which
    rotate every fixture of the body.
  - In collision resolution: `StandardCollisionHandler` and `MegaCollisionHandler`.
  - In `postProcess`: `Spike.kt:267`, `Laser.kt:376-377`.
- A dirty flag set by the `Fixture`/`Body` setters cannot see most of these changes: `rawShape` is changed through
  casts, `offsetFromBodyAttachment` is a mutable `Vector2`, and `direction`, `attachedToBody` and
  `bodyAttachmentPosition` are public `var`s.

### Static bodies that move

- `BodyType.STATIC` changes only collision resolution (`WorldSystem.kt:260`, `StandardCollisionHandler`,
  `MegaCollisionHandler`). `Body.process` moves a STATIC body with a velocity like any other body. The comment at
  `WorldSystem.kt:256-259` ("Static bodies never move") is wrong.
- `Block` subclasses that move: `Lift`, `GearTrolley`, `PropellerPlatform`, `RocketPlatform`, `SwingingPlatform`,
  `FeetRiseSinkBlock` and `InfernoChainPlatform`, `WoodCrate`, `CartV2`, `RailTrackPlatform`. They move only with a
  prop: `AnimatedBlock` and its subclasses (`trajectory`, none in live maps), and `PreciousBlock` (`trajectory_def`,
  17 of 45 live). `ConveyorBelt`, `RocketPlatform` and `GravityBlock` change `direction`, which rotates the bounds.
- A plain `Block` moved by its owner: `PushableBlock`, `DynamicBlock`, `GravityBlock`, `Spike` (each Tiled spike
  has an inner block by default), `SteamRoller`, `CeilingCrusher`, `SigmaRatClaw`, `SphinxBall`, `GutsTank`,
  `BigAssMaverickRobotHand`, `FlipperPlatform`.
- Runtime toggles of `collisionOn` or the block fixture's `active`: `DropperLift`, `SwitchGate`,
  `GroundSlideBlocker`, `RailTrack`, `SteamRoller`, `FlipperPlatform`, `Spike`.
- Body counts in three live maps (objects in the `blocks*` layer): `TimberWoman_16x14_v4` 151, `WilyStage1_v2` 169,
  `Gauntlet1_v2` 119. Many non-STATIC bodies also never move (`Ladder`, `Water`, `QuickSand`, `Death`, `Gate`, most
  ABSTRACT specials and hazards).
- `Body` keeps no previous position and no version number. `bounds` is private, and every position or size change
  goes through `Body` (`Body.kt:117`, `:152-340`), so a version counter there is possible. It would not see
  `direction` changes, `rawShape` or offset changes, or `active` flips.

### Verification

Available today:
- `ContactRuleSelectionTest` checks the rule selection for each (phase, type1, type2). It checks the role order
  only for same-type pairs. It checks nothing in `WorldSystem`.
- The rule unit tests, `MegaCollisionHandlerTest`, and the container tests (insert and query, not order).
- `WorldSystemTest` uses a mocked container and has no contact or order tests.
- `--diagnostics` gives timings per phase (`collectContacts`, `addBody`, `addFixtures`, `updateWorldContainer`),
  but no counts.

What can be built for `020` without `006` or `055`: an engine test harness that runs a frozen copy of today's
`WorldSystem` (in the test sources) and the new one side by side on seeded synthetic worlds, with:
- real `Body`, `Fixture` and `SimpleGridWorldContainer`;
- immobile and moving STATIC blocks, DYNAMIC bodies, same-type pairs, fixtures that toggle `active`;
- a listener that moves bodies (like stick-to-block), and spawns, destroys and `doUpdate` changes during the run;
- a recording listener and collision handler that log (sub-step, phase, type1, type2, body ids) and the collision
  call order, plus the body positions after each sub-step.

The two logs must be equal. `cycle`, `collectContacts`, `processContacts` and `resolveCollisions` are `internal`, so
engine tests can call them. `014` proposed a `TracingContactListener` as the proof tool for `020`. It was not built.

### Related items

- `010` (fixed tick) changes the sub-step count. The saving from `020` scales with it. If `010` goes to one step per
  frame, the rebuild count goes from about 3.5 to 2 without `020`.
- `017` (world-step hooks) changes `WorldSystem.cycle`. It is a direct overlap.
- `018` (ECS membership) and `019` (spawn phase) set the body order, which sets the contact order.
- `001`, `016` (the container moves to `LevelContext`), `012` (freezes), `009` (the allocation baseline, useful to
  measure `020`), `044` (`DamageRule` builds debug strings on each call).

### Side findings (not in scope)

- `GameLine.rotate` sets `this.originY = originX` (`E/common/shapes/GameLine.kt:185-190`). It looks like a bug.
- `LooseQuadtreeWorldContainer` uses `tmr.tileWidth` as its cell size, while `WorldSystem` queries with
  `ConstVals.PPM`. The cells do not match if the tile width is not 32. It is used only with
  `--worldContainer quadtree`. Every map in `assets/tiled_maps/tmx/` has `tilewidth="32"`, so this has no effect
  today. It is not filed as an issue.
- `QuadtreeWorldContainer.kt` is commented out. The core classes `MegaSimpleGridWorldContainer` and
  `MegaLooseQuadtreeWorldContainer` are empty.

## Options (2026-10-25)

All options keep the contact order, the orientation and the collision order, unless an option says otherwise. Each
step is gated on the side-by-side harness.

### Option 0: measure first

Run `--diagnostics` in two or three block-dense rooms on `master`. Add counters (queries, candidates, narrow-phase
tests, contacts) to the WorldSystem diagnostics entries. Then decide which option is worth doing.

- Pros: no gameplay risk; it shows whether the broad phase is a hot spot at all. The counters also measure each
  later step.
- Cons: the counters are new production code (they need a caller, the diagnostics output). It delays the work.

### Option A: a shape cache for the collection phase

Compute each fixture's shape once at the insert, and use it for the query fixture and every candidate until
`collectContacts` ends. Drop it before `processContacts`.

- Pros: the scan shows that nothing changes in this window, so the risk is the lowest. No change to order or
  orientation. Removes about 1 + (candidates per fixture) shape computations per fixture per sub-step. Small:
  engine only, in `WorldSystem` and the container insert.
- Cons: the saving is unknown, and probably small. The shapes are computed again in the post-loop rebuild (a
  separate fill is needed after `postProcess`). The container interface may need an insert that takes a shape.

### Option B: test each pair once, with the same order

When a fixture finds a candidate whose body comes earlier in `bodyArray` and the candidate also queries (it is
active and passes `shouldProceedFiltering`), skip the filter and the narrow phase. The earlier body already tested
this pair and put it in the set first, with its orientation. So the set and its order stay the same.

- Pros: about half of the filter and narrow-phase work for pairs that both sides query. The contact set, its order
  and its orientation do not change. Small: a body index for each cycle, and one check per candidate.
- Cons: it is correct only if the filter and `overlaps` are symmetric, and if the earlier body's query range
  always contains the later body's fixture. The scan found the filter to be symmetric. The range uses floor and
  ceil without an epsilon, so it is a superset. Edge cases need tests (touching edges, `GameLine` and `GameCircle`
  shapes). The `Receivers.FILTER` lambdas are then called once, not twice (they are pure, so no change). It does
  not touch `resolveCollisions`, where a DYNAMIC-DYNAMIC pair is also called twice. The handlers return early for
  those pairs, so the saving there is small.

### Option C: small engine clean-ups

Remove the capturing lambdas in the hot loops (use member callbacks or index loops), remove the redundant
`candidate.isActive()` check, and use a cheaper dedup than `tempFixtureSet.clear()` (for example a stamp counter
per query).

- Pros: no change to order. Less garbage per sub-step (this ties in with `009`). Each change is small.
- Cons: small gains. It is a clean-up, not the broad-phase change that the item names.

### Option D: a persistent layer for immobile bodies

Keep the immobile bodies and fixtures in the container across sub-steps and frames. Re-insert only the bodies that
move.

How to decide that a body is immobile:
- D1, a flag per class in code: wrong for a plain `Block` that an owner moves, unless the owner, `gravity_on`, or
  `custom_pre_process` clears it. It must be kept up to date for new classes.
- D2, a Tiled property: mostly redundant (all live plain blocks are immobile), and it does not cover blocks that
  code spawns or the rotation from gravity changes. A wrong flag gives stale entries: collisions that are missed or
  that do not exist.
- D3, detect movement (compare bounds and direction before and after, or a version counter in `Body`): catches
  every mover. It costs a check per body per sub-step. A counter misses `rawShape` and offset changes.

- Pros: the largest possible saving in rooms with many blocks.
- Cons:
  - **It changes the candidate order.** Today a cell's order is the body order of the last rebuild. If the
    immobile bodies stay in the cell and the moving bodies are re-inserted, the moving bodies come after them. The
    contact order and the `resolveCollisions` order then change. To keep the order, the query must sort or merge
    the candidates by body index, and that removes much of the saving.
  - The add and remove hooks are many: spawn and cull on camera entry and exit (any frame), destroy, `doUpdate`
    (`update_distance`), `collisionOn`, fixture `active`, `direction`, `rawShape` changes, pooled reuse, and
    `reset`.
  - Two containers must still be produced: the in-cycle one (all bodies) and the post-loop one (only
    `collisionOn` bodies) that pathfinding copies.
  - Blocks are culled out of the camera, so few blocks are alive at one time. The saving is smaller than the
    total block count suggests.
  - The largest size and risk of all the options.

### Option E: wait for `010`

Do nothing in the broad phase until `010` sets the sub-step count.

- Pros: `010` alone may cut the rebuilds from about 3.5 to 2 per frame. `020` can then be sized against the new
  loop.
- Cons: `010` is an idea, and it depends on `011` and `017`. Option A and Option B do not depend on the step
  count, so there is no reason for them to wait.

### Recommendation

Updated with the owner's feedback (see "Owner's feedback and follow-up" below).

1. Build the two test suites first: the temporary side-by-side suite and the permanent order suite. Without
   them, no option can be proven.
2. Option 0 is not a gate. The work is necessary (the weak-hardware goal). Take one `--diagnostics` capture in
   block-dense rooms before and after, only to show the gain and to choose between options.
3. Option A, Option B and Option C in one branch. All three keep the order by design, are engine-only and small,
   and the suites check them.
4. Do not do Option D in the first branch. Its best form is D4 (an incremental grid, see below), and it needs its
   own branch after the first one, with the numbers from step 2 to size it.
5. Answer to the open question: no list exists and no Tiled flag is needed. Under the recommendation, nothing
   needs to know which bodies are immobile.

## Owner's feedback and follow-up (2026-10-25)

### Decisions

- Two test suites (owner, 2026-10-25):
  - A **temporary** suite that runs a frozen copy of today's `WorldSystem` side by side with the new one on seeded
    worlds, and asserts the same contacts, order, orientation, collision calls and positions. It is deleted
    before the merge to `master`. (The general rule is now in `docs/mega-refactor/CLAUDE.md`, "During the
    branch".)
  - A **permanent** suite against the new code that checks a stable contact order (the discovery order and the
    orientation rules in "Contact order and orientation" above, and the END pass after BEGIN/CONTINUE).
- Optimization is a goal of the refactor (owner, 2026-10-25): the game must play well on weak hardware. So
  Option 0 is not a gate. (Now in `docs/mega-refactor/CLAUDE.md`, "Goal", and in the root `CLAUDE.md`.)
- Option C: agreed by the owner.

### How the gain of Option A (and B, C) is measured

No new mechanism is needed for timings. `WorldSystem` already has `RuntimeDiagnostics` entries around each phase:
`addBody`, `addFixtures`, `collectContacts`, `processContacts`, `resolveCollisions` and `updateWorldContainer`.
Option A shows up as a smaller `addFixtures` + `collectContacts` time, and Option B as a smaller `collectContacts`
time. A capture with `--diagnostics` on `master` and on the branch, in the same rooms, compared with the
`diagnostics-analyzer` skill, gives the gain.

Limits of this:
- The entries are in milliseconds with 3 decimals, and a phase can take less than 0.1 ms on a fast machine. The
  noise between two runs can be larger than the gain. Long captures and averages help.
- The run is not repeatable: the player's path changes the bodies on screen.

If the timings are not clear, there are two ways to get better data:
- **Counters in `RuntimeDiagnostics`.** Add a counter API next to the timings (for example
  `count(name, value)`), and count shape computations, queries, candidates and narrow-phase tests in
  `WorldSystem`. Counts do not depend on the machine, so they show the saving exactly. This extends the existing
  mechanism; it is not a new one. The production caller is `WorldSystem`, and the output goes to the same file.
- **A benchmark on the seeded worlds of the temporary suite.** Time the old and the new `WorldSystem` on the same
  world. It is repeatable. It is test code, run by hand, not in CI, because timings in CI are not stable.

### How Option B works

Today each pair that both sides can query is tested twice. Body `j` (earlier in `bodyArray`) finds the pair first
and adds it to `currentContactSet` with `j`'s fixture as `fixture1`. Later, body `i` finds the same pair, runs the
filter and `overlaps` again, and `currentContactSet.add` drops the result. Option B skips that second test when the
result is already known.

1. At the start of each `cycle`, give each body its index in `bodyArray` (a field on the body, or an
   `IdentityMap<IBody, Int>`).
2. In `collectContacts`, fixture `F` of body `i` queries the grid and gets candidate `C` of body `j`.
3. Skip `C` (no filter, no `overlaps`) only if all of these are true:
   - `j < i`: body `j` ran its queries earlier in this sub-step;
   - `C` queries: `C.isActive()` and `contactFilter.shouldProceedFiltering(C)`;
   - `F` is inside `C`'s query range: `F`'s grid cells overlap the cell range that `C` queried. This is a check of
     4 integers if Option A also caches each fixture's query range.
4. In all other cases, test `C` as today.

Why the result is the same as today: when body `j` queried from `C`, it visited `F` (the third condition), and it
tested `filter(C, F)` and `C.shape.overlaps(F.shape)`. If that test passed, the pair is already in the set, and
today's second test only finds a duplicate. If it failed, today's second test also fails, because the filter and
`overlaps` give the same answer in both directions. So the set, its insertion order and the orientation of each
contact do not change. Only the wasted second test goes away.

What must be true, and what the scan found (`master` `9cd13b5a5`):
- The filter result is symmetric: `MegaContactFilter.filter` checks the type pair in both directions
  (`MegaContactFilter.kt:143-144`), and the `Receivers.FILTER` lambdas are pure. With Option B they run once per
  pair, not twice. That is not visible, because they are pure.
- `overlaps` is symmetric for the shape pairs that core uses: rectangle, circle and line call the same LibGDX
  function from both sides (`GameRectangle.kt:341-347`, `GameCircle.kt:79-88`, `GameLine.kt:374-388`). Rectangle
  against rectangle is strict, so two rectangles that only touch do not overlap from either side. Polygon pairs
  and `OVERLAP_EXTENSION` are not proven symmetric (for example `GameLine` against `GamePolygon` uses
  `intersectLinePolygon`, an infinite line). No core fixture uses a polygon today. The permanent suite must check
  symmetry for each shape pair, or Option B must test polygon pairs as today.
- The third condition covers the grid edge cases: insert moves an exact-integer edge inward by an epsilon, and the
  query uses `floor` and `ceil` with no epsilon. So a fixture can be in a cell that the other side did not query.
  The check uses the real cells, so it does not depend on the reasoning about epsilons.

Gain: for each pair that both sides query, one filter call and one `overlaps` call fewer. The grid loop still
visits the candidate. The temporary suite proves that nothing changed; the permanent suite keeps the order rules
true later.

### Option D in more depth: how to handle immobile bodies

What a persistent layer saves: only the **insert** of bodies that did not move, that is the cell loop and the
`OrderedSet.add` for each cell, about 3.5 times per frame. It does not make the queries cheaper. With Option A, the
shape computation is done once per sub-step anyway, so the insert is the part that is left.

Four ways to decide what to keep:

**D1: an `immobile` flag on `Body` (engine), set by core.** For example, core sets it for a plain Tiled `Block`
with no owner, no trajectory, no `gravity_on` and no `custom_pre_process`.
- Pros: simple to read. The engine skips the flagged bodies with no checks.
- Cons: the engine trusts core. If a flagged body moves (a later prop, a new owner, a gravity flip that rotates
  it), its grid cells are stale. Contacts and collisions are then missed or wrong, with no error. This couples the
  correctness of the physics to how each entity is set up, which is the coupling the owner asked about. A debug
  check (compare the bounds in debug runs) can catch a mistake, but only in the rooms that someone plays.

**D2: an `immobile` Tiled property.** The same as D1, but the level author sets it.
- Pros: the author decides.
- Cons: the same coupling as D1, now in the map data, which has no checks. It is mostly redundant, because all
  2,336 plain live blocks are immobile. It does not cover blocks that code spawns.

**D3: detect it on the fly for STATIC bodies (compare the bounds).**
- What it gains: no flag, so no coupling to the setup. A mover is caught on the first sub-step that it moves.
- What it costs: to compare, the engine must know the current shape of each fixture, and that is the
  `getShape()` work that Option A already does once per sub-step. So the comparison is cheap with Option A, but
  the saving is only the cell loop.
- Limiting it to STATIC bodies gains nothing. The comparison works the same for any body, and many non-STATIC
  bodies (`Ladder`, `Water`, `Spike`, sensors) also never move. The body type is not the right key.

**D4: an incremental grid (D3 for every body, at the level of cells).** For each fixture and body, keep the cell
range from the last insert. After Option A computes the new shape, compute the new cell range. If it is the same,
do nothing. If it changed, remove the entry from the cells that it left and add it to the cells that it entered.
- Pros: no flag, no Tiled property, no coupling to the setup, and no special case for STATIC. It also helps
  moving bodies: at 150 sub-steps per second, a body at Mega's running speed stays in the same 32 px cell for
  several sub-steps, so most bodies do not change cells on most sub-steps. A new mover or a new entity type needs
  nothing.
- Cons:
  - **The order problem.** Today a cell's order is the body order of the last rebuild. If a body enters a cell
    by an append, it comes after bodies that are earlier than it in `bodyArray`, but also after bodies that are
    later. The order changes. To keep it, each cell must stay sorted by a stable key: the entity's insertion
    sequence in the `WorldSystem`, then the fixture's place in its body, (a number given when the entity joins the system; a pooled entity that spawns
    again gets a new number, like it gets a new place at the end of the system set today). An entry that enters
    a cell is inserted at its sorted place. Cells hold few entries, so this is cheap, but it is new code that the
    suites must cover.
  - Removal and change hooks: destroy, `doUpdate` false (`update_distance`), fixture `active` flips, and
    `collisionOn`. For these the engine can compare the state each sub-step (it builds `bodyArray` each frame
    already), so no core hook is needed.
  - The post-loop container (only `collisionOn` bodies, read by pathfinding) is a second view. It needs either its
    own incremental grid or a `collisionOn` check at read time, and `copy()` must respect that.
  - It is the largest option. It changes `SimpleGridWorldContainer`, and maybe the `IWorldContainer` interface
    (remove, move).

Recommendation for D: D4, in its own branch after A, B and C, with the measurements from that branch to size it. Do
not do D1 or D2. Both make physics correctness depend on setup data, and D4 gives the same saving without that.

### Option E

The trade-offs of item `010` are in item `010` `NOTES.md`, "Trade-offs of doing 010 (2026-10-25)". A correction
to Option E above: `010` cuts the rebuilds from about 3.5 to 2 per frame only with one physics step per tick
(step 3). Step 2 alone, with one shared step of 1/150 s, raises them to 6 per frame on `VERY_LOW` (5 sub-steps plus
the post-loop rebuild). So waiting for `010` does not help weak hardware by itself, and Options A, B and C do not
depend on it.
