# Process log: mega-refactor-064a-instance-identity

## Status

- State: active
- Goal: entity groups as an engine feature (`GroupsComponent` and a `GroupsSystem` with the index); `id` moves to `064b`
- Done: planning approved; plan steps 1 to 8; play test passed; temporary suite deleted
- Next: owner merge approval
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-27

- Branch start on `master`. Worktree `.worktrees/mega-refactor-064a-instance-identity`.
- Scan (base `ce10fc4b1`), in `docs/SCAN.md`. Findings: the registry adds and removes an entity inside
  `MegaGameEntity.onSpawn`/`onDestroy`, so a group stored at `add` and dropped at `remove` gives the same lookups at
  the same moments; the 4 `id` casts hold `IGameEntity` or `IBodyEntity` values, so `id` on `IMegaGameEntity` alone
  removes none of them; `GameEntityUtils.kt` has no cast only for `id`; `group` is used only in `MOON_MAN` `room10`
  and `WILY_STAGE_3` `room5`; `SpiderWeb.onDestroy` skips `super.onDestroy()` (bug candidate in `docs/TODO.md`).
- Read the `063b` folder: template docs only; file overlap in `docs/NOTES.md`.
- Draft `docs/SCOPE.md` with 4 open questions.
- Scope change (owner, 2026-10-27, recorded on `master` in item `064` `ITEM.md`, commit `df49f9c39`): "Why
  `IMegaGameEntity` and not `IGameEntity`? And could the group-registry logic be moved as a first-class component into
  the engine? Maybe a \"groups component\"? I don't mind the component lookup if it leads to better design"; then
  "Would it still make sense to make core's concept of id be a component or prop as well?"; then "Yes to all". `064a`
  now does groups only (an engine `GroupsComponent` and an engine group index); `id` becomes a `core` component in
  `064b`. The old open questions 1 to 3 (all about `id`) are gone. Question 4 approved: real rooms, no
  `RefactorTest064` map.
- Scan updated in `docs/SCAN.md` (engine lifecycle, update points, `reset()`, component `reset()` at destroy; `id`
  parts removed). Draft `docs/SCOPE.md` rewritten with 6 open questions. Rechecked `063b` (at `f6866be2e`): no shared
  code file; overlap in `docs/NOTES.md`.
- Owner answers to the second gate-1 report (2026-10-27, recorded on `master` in item `064` `ITEM.md`, commit
  `41ec18082`): "I'm not sure the engine should be responsible for handling groups. That seems more like it should be
  a separate system (literally a system)." and "I think having an index like today's `MegaGameEntities.kt` is
  preferable. We can open up `GameSystem`'s funcs and/or add hooks as needed." Approved as recommended: the query name
  `getEntitiesInGroup(group)`, a read-only return with no allocation per call, the component added in
  `MegaGameEntity.onSpawn` only with a group, a set of names, real rooms; accepted: an entity that skips
  `super.onDestroy()` and has a group leaves the index. Not in this branch: `super.init()` (item `065`), the `SpiderWeb`
  fix (a `fix/` branch after `064a`).
- Scan extended (`docs/SCAN.md` section 3a: every `GameSystem` join and leave point, the deferred paths, `reset()`,
  `on`). Draft `docs/SCOPE.md` rewritten: `GameSystem` join/leave hooks, `GroupsComponent`, `GroupsSystem` with the
  index and `getEntitiesInGroup`; 3 open questions. Two backlog candidates in `docs/TODO.md` (`GameSystem.reset()` and
  `entitiesToRemove`; `MutableOrderedSet.retainAll`). Rechecked `063b` (at `02d1a4eb7`, no code): no shared code file.
- Scope approved (owner, 2026-10-27: "Yes to both"), with the recommendations: `GroupsSystem` last in the system
  list; `GameEntityUtils` gets it through `game.getSystem(GroupsSystem::class)` once when the predicate is built; hooks
  `onEntityJoined`/`onEntityLeft`. The `MutableOrderedSet.retainAll` note in `docs/TODO.md` now points to the existing
  issue `docs/issues/2026-10-22-mutable-ordered-set-retain-all.md`.
- Draft `docs/CONTRACT.md`: automated checks (clean build, tests, warnings, the temporary
  `Temp064aGroupIndexEquivalenceTest`, permanent `GameSystemHooksTest`, `GroupsSystemTest` and core tests, greps);
  manual checks in `MOON_MAN` (`room10` through `rest_room_1`, which is the nearest room with a player spawn; a death
  in `room10`; a sanity pass from `room6` and a level restart) and `WILY_STAGE_3` `room5` (climb, pause). Intended
  change: an entity that skips `super.onDestroy()` and has a group leaves its group (no current entity).
- Contract approved (owner, 2026-10-27: "yes").
- Draft `docs/PLAN.md` (8 steps): baseline; `GameSystem` hooks; `GroupsComponent`/`GroupsSystem`; temporary suite
  proved first on the old code; core switch; docs; contract checks and smoke launches; delete the suite after the play
  test.
- Plan approved (owner, 2026-10-27: "064a: approved").
- Step 1 (baseline, clean build, JDK 17 Corretto): build and jar pass; engine 549 tests, core 192 tests, 0 failures;
  88 unique warnings (scratchpad `064a-warnings-baseline.txt`).
- Step 2: `GameSystem` gets `onEntityJoined`/`onEntityLeft` (empty defaults), fired on each real change of the
  members (immediate `add`/`remove`, deferred adds/removes and the drop of non-qualifying members in `update`, each member
  at `reset()`). New `GameSystemHooksTest` (7 cases). Engine 556, core 192, 0 failures.
- Step 3: new `engine/.../groups/GroupsComponent` (an `OrderedSet` of names; `reset()` clears it) and `GroupsSystem`
  (ordered group-to-members index; names stored at the join; empty group sets dropped; `getEntitiesInGroup` returns the
  set or one shared empty set). New `GroupsSystemTest` (13 cases). Engine 569, core 192, 0 failures.
- Step 4: `Temp064aFrozenGroupRegistry` and `Temp064aGroupIndexEquivalenceTest` (core test sources). A `GameSystem`
  subclass cannot be written in `core` (`process` is `internal`), so the probes are two `UpdatablesSystem`s with a probe
  entity (spawned again after each `engine.reset()`); the on/off operation toggles every other system. 200 seeds x 300
  operations, 12 entities (2 skip `super.onDestroy()` and have no group). On the old code the lookup is
  `MegaGameEntities.getOfGroup`, and the suite passes: 168925 non-empty comparisons, 32122 probe comparisons, 5979
  spawns with another group, 2578 destroys without `super`, 1196 resets, 7274 spawn/destroy pairs in one frame. A
  check with a wrong lookup (reversed order) fails at seed 0 in a probe, so the suite can fail. Engine 569, core 193,
  0 failures.
- Step 5: `GroupsSystem` is the last system in `createGameEngine()`; `MegaGameEntity` has no `group` (its `onSpawn`
  gets or adds a `GroupsComponent` and adds the `ConstKeys.GROUP` value); `MegaGameEntities` has no group map and no
  `getOfGroup`; `getStandardBodyDoUpdatePredicate` reads the names from the component (no cast for the group) and copies
  them into an `Array` for the predicate (so a later change of the component cannot change a built predicate, as with
  the old captured `String`); `getUpdateWhenGroupNearCameraPredicate` gets the `GroupsSystem` once through
  `game.getSystem` and checks any member of any of the groups. The temporary suite now looks up through `GroupsSystem`
  (placed between the two probe systems; the on/off operation toggles it) and passes with the same counts as in step 4.
  New `EntityGroupsTest` (4 cases). Engine 569, core 197, 0 failures. Greps: no `group` member, `groupToEntities` or
  `getOfGroup` in `core/` main.
- Step 6: root `CLAUDE.md`: the system order ends with `Groups`; the engine section names the `GameSystem` hooks and
  the groups (`GroupsComponent`, `GroupsSystem.getEntitiesInGroup`); the `MegaGameEntity` line drops `group`.
- Step 7 (contract checks, clean build): build and jar pass; engine 569 (549 + 7 + 13), core 197 (192 + 1 temporary +
  4), 0 failures. Warnings: the same 88 lines as the baseline, no new warning. Greps: no `group` member in
  `MegaGameEntity`; no `groupToEntities`/`getOfGroup` in `core/`; `GroupsSystem` is the last system;
  `contact-rule-selection.txt` unchanged. Review: each `GameSystem` member change fires one hook (`GameSystemHooksTest`
  checks each path and the order); `onEntityLeft` uses the stored names; `getEntitiesInGroup` returns the stored set or
  a shared empty set (no allocation); the predicate takes `GroupsSystem` once and, with one name, checks the same
  members as on `master`. Smoke launches (`--level MOON_MAN --room rest_room_1`, 90 s; `--level WILY_STAGE_3 --room
  room5`, 60 s; stopped by a timeout): the levels load, no exception in the output.
- Read the `063b` folder again (at `fad9a543a`): no shared code file; `064a` code does not name the renamed types.

## 2026-10-28

- Owner play test (2026-10-28: "064a: tests pass"): all manual checks in `docs/CONTRACT.md` pass.
- Step 8: deleted `Temp064aGroupIndexEquivalenceTest` and `Temp064aFrozenGroupRegistry` in their own commit
  (`00388edcd`). Automated checks again (clean build): build and jar pass; engine 569, core 196 (197 - 1 temporary), 0
  failures; warnings the same 88 lines as the baseline; no `Temp064a` left in `core/` or `engine/`. `master` is not
  merged into the branch (the coordinator merges it before the `064a` merge, after `063b`).
