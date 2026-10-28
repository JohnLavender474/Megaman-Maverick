# Contract: mega-refactor-064a-instance-identity

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-27

What must stay the same: everything. The branch moves the group index from `MegaGameEntities` (updated in
`MegaGameEntity.onSpawn`/`onDestroy`) to a `GroupsSystem` (updated when an entity joins or leaves the system, in
`GameEngine.spawnNow`/`destroyNow`). Group lookups must give the same entities in the same order at every moment when
a lookup can run. The only reader is the `doUpdate` predicate of grouped `Block`s and `GravityBlock`s, which decides
whether their bodies update. `GameSystem` gets join/leave hooks; every existing system must keep the same members in the
same order.

## Automated checks

All on a clean build: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
lwjgl3:jar`.

- [x] The build passes and the jar builds.
- [x] All engine and core tests pass, 0 failures. The counts go up only by the new tests (baseline counted at plan
  step 1).
- [x] The compiler warnings are the same set as the baseline (unique `^w:` lines without line and column). Each new
  warning is listed in the process log with a reason, or fixed.
- [x] **Temporary contract suite** `Temp064aGroupIndexEquivalenceTest` (core test sources), with the frozen copy
  `Temp064aFrozenGroupRegistry` of the old group logic of `MegaGameEntities` and `MegaGameEntity` (the test entity keeps
  an old-style group field and feeds the frozen copy in `onSpawn`, and in `onDestroy` only when it calls
  `super.onDestroy()`). On seeded random sequences (at least 200 seeds, each with at least 200 operations) of spawn,
  destroy, a destroy and a spawn in one frame, a respawn with another group or with no group, pooled and non-pooled
  entities, entities in more than one sequence step per frame, `engine.reset()`, and systems turned off (`on = false`):
  the ordered member list of every group used is equal in `GroupsSystem.getEntitiesInGroup` and in the frozen copy,
  (a) from a probe system before and a probe system after `GroupsSystem` in the system list, during their `update`, and
  (b) after each operation, between updates. An entity that skips `super.onDestroy()` is in the sequences without a
  group. Deleted before the merge, in its own commit.
- [x] **Permanent engine test** `GameSystemHooksTest`: each join and leave path fires the hook once and in order
  (`add` and `remove` when not updating; deferred `add` and `remove` during the system's own `update`; the drop of a
  member that no longer qualifies; `reset()`); an `add` of a member and a `remove` of a non-member fire nothing; the
  members and their order are the same as on `master` for the same calls.
- [x] **Permanent engine test** `GroupsSystemTest` (through a real `GameEngine`): an entity is in its groups from the
  end of `onSpawn` until the start of the component `reset()` at destroy; a destroy and a spawn in one frame; a
  respawn with another group or with none; the member order is the join order; an unknown group gives an empty result;
  an empty group set is dropped; `reset()` and `dispose()` leave the index empty; a lookup from inside another system's
  `update` sees the spawns of the same frame; `on = false` does not change the index.
- [x] **Permanent core test**: `MegaGameEntity.onSpawn` puts the `ConstKeys.GROUP` value into a `GroupsComponent` and
  adds no component when the spawn has no group; a pooled entity that spawns again with no group has an empty
  `GroupsComponent`; a `MegaGameEntity` that skips `super.onDestroy()` and has a group leaves its group at destroy (the
  intended change below).
- [x] Greps: no `group` member in `MegaGameEntity`; no `groupToEntities` or `getOfGroup` in `core/`;
  `GroupsSystem` is the last system in `createGameEngine()`.
- [x] `core/src/test/resources/contact-rule-selection.txt` is unchanged.

## Manual playthrough

Passed (owner, 2026-10-28: "064a: tests pass").

The owner plays these once, after the implementation, from the worktree.

- `./run-game-alpha.sh --level MOON_MAN --room rest_room_1`: `room10` (the room with the grouped moon crates) has no
  player spawn; `rest_room_1` is the room before it. Take the right portal in `rest_room_1` (the one at the right side)
  to the left end of `room10`. Cross `room10` from left to right, with the `GravitySwitcharoo`s, to the portal at its
  right end. Look at all 6 groups of `GravityBlock` crates and their frame blocks: each crate stays still while its
  group is off screen and starts to fall when its group comes near the screen; crates land on the blocks of their
  group and do not fall through them or out of the room; after a gravity flip, the crates near the screen fall the
  other way together; the pound sound plays when a crate lands on screen; Megaman can stand on the crates and the
  frame blocks.
- `./run-game-alpha.sh --level MOON_MAN --room rest_room_1`: as above, go to `room10`, let Megaman die in `room10`
  (enemies or a pit), go back to `room10` after the respawn, and play it again. The crates are back in their start places and act as
  in the check above.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: climb the tall `room5` from the bottom to the top. Two groups
  (`room5_group1` near the bottom, `room5_group2` near the top) of four `GravityBlock`s each, with frame blocks. Same
  checks as in `room10`: the crates of a group start to move together when the group comes near the screen, they land
  on their frame blocks, and Megaman can stand on them.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: in `room5`, pause and unpause while a group is on screen
  (the pause turns all systems off and on). The crates do not jump, fall through, or stop.
- `./run-game-alpha.sh --level MOON_MAN --room room6`: a short sanity pass of a real stage, because `GameSystem`
  changes for every system: play from `room6` through `room7`, `room8`, `room9` and `rest_room_1` to `room10`, with
  enemies, shots, item drops, enemy deaths, portals and room transitions. Then leave the level from the pause menu and
  start `MOON_MAN` again (leaving the level runs `engine.reset()`). Nothing is missing, frozen, duplicated, or invisible; no crash.

## Behaviors to check by review

- Each `GameSystem` path that changes `entities` fires the right hook, and the replaced `addAll`/`removeAll` loops give
  the same members in the same order.
- `GroupsSystem.onEntityLeft` uses the names stored at the join, not the component (which `reset()` empties after the
  leave).
- `GroupsSystem.getEntitiesInGroup` allocates nothing per call.
- `getStandardBodyDoUpdatePredicate` takes `GroupsSystem` once, when the predicate is built, and with one group name
  gives the same predicate as on `master`.

## Intended changes

- An entity that skips `super.onDestroy()` and has a group now leaves its group at destroy. On `master` it stayed in its
  group forever. No current entity is affected: the only entity that skips `super.onDestroy()` is `SpiderWeb`, which
  never has a group (accepted by the owner, 2026-10-27). The `SpiderWeb` bug itself is not fixed here.
