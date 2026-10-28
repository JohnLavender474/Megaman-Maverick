# Plan: mega-refactor-064a-instance-identity

- Status: approved
- Approved: 2026-10-27

JDK: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13` (the default JDK 25 does not run Gradle 8.10.1). Scratch
files: `/tmp/claude-1000/-home-johnlavender-IdeaProjects-Megaman-Maverick/a600e326-cd21-4d95-a3bf-326f727a8247/scratchpad/064a/`.
One commit per step. `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

## New API and first production caller

| API | Module | First production caller (step) |
|---|---|---|
| `GameSystem.onEntityJoined(entity)` / `onEntityLeft(entity)` (`protected open`, empty) | engine | `GroupsSystem` overrides both (step 3) |
| `GroupsComponent` (a set of group names; `reset()` clears it) | engine | `MegaGameEntity.onSpawn` writes it; `GroupsSystem.onEntityJoined` and `getStandardBodyDoUpdatePredicate` read it (steps 3, 5) |
| `GroupsSystem : GameSystem(GroupsComponent::class)` | engine | `MegamanMaverickGame.createGameEngine()` (step 5) |
| `GroupsSystem.getEntitiesInGroup(group): Collection<IGameEntity>` | engine | `getUpdateWhenGroupNearCameraPredicate` (step 5) |

Removed: `MegaGameEntity.group`, `MegaGameEntities.groupToEntities`, `MegaGameEntities.getOfGroup`.

Steps 2 and 3 add engine code whose production callers come in step 5. The branch does not merge between them.

## Temporary test code

- `Temp064aGroupIndexEquivalenceTest` and `Temp064aFrozenGroupRegistry` (core test sources). Added in step 4, switched
  to the new code in step 5, deleted in step 8 in its own commit.

## Steps

### 1. Baseline

- Changes: none. On the branch before any code change, run
  `./gradlew clean engine:test core:test lwjgl3:jar`. Write the engine and core test counts in the process log. Save the
  unique warning lines (`grep '^w:' | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u`) to
  `064a-warnings-baseline.txt` in the scratchpad.
- Check: the build passes, 0 failures; the counts and the warning file exist.
- Done: yes

### 2. `GameSystem` join/leave hooks

- Changes: `E/systems/GameSystem.kt`: add `protected open fun onEntityJoined(entity: IGameEntity) {}` and
  `protected open fun onEntityLeft(entity: IGameEntity) {}`. Fire them only on a real change of `entities`:
  - `add` when not updating: `if (entities.add(e)) onEntityJoined(e)`;
  - `remove` when not updating: after `entities.remove(e)` (the entity was a member);
  - `update`: the deferred adds and removes become per-element loops in the same order as `addAll`/`removeAll`, with a
    hook for each element that really joins or leaves; `removeAll { !qualifies(it) }` fires `onEntityLeft` for each
    member it drops;
  - `reset()`: `onEntityLeft` for each member, in member order, before `entities.clear()`.

  The return values of `add`/`remove` (`0`, `1`, `2`) do not change. New engine test `GameSystemHooksTest` (a small test
  system that records the hook calls): each path fires once and in order; an `add` of a member and a `remove` of a
  non-member fire nothing; the members and their order after each call are the same as the expected `master` result.
- Check: `engine:test core:test` pass; the existing system tests are unchanged and pass.
- Done: yes

### 3. `GroupsComponent` and `GroupsSystem`

- Changes: new `E/groups/GroupsComponent.kt` and `E/groups/GroupsSystem.kt`:
  - `GroupsComponent`: `val groups` (a LibGDX set of `String`); `reset()` clears it.
  - `GroupsSystem`: `OrderedMap<String, MutableOrderedSet<IGameEntity>>` (group to members, join order) and
    `ObjectMap<IGameEntity, Array<String>>` (the names each member joined with). `onEntityJoined` reads the
    component's names and adds the entity to each group; `onEntityLeft` removes it from the stored names and drops an
    empty group set; `process` does nothing; `getEntitiesInGroup(group)` returns the set as `Collection<IGameEntity>`,
    or one shared empty set.

  New engine test `GroupsSystemTest` (real `GameEngine`, test entities that write the component in `onSpawn`): the
  cases in `CONTRACT.md` (join after `onSpawn`, leave before the component `reset()`, destroy and spawn in one frame,
  respawn with another group or none, join order, unknown group, empty set dropped, `reset()` and `dispose()` empty the
  index, a lookup from another system's `update`, `on = false`).
- Check: `engine:test core:test` pass.
- Done: yes

### 4. Temporary suite on the old code

- Changes: core test sources: `Temp064aFrozenGroupRegistry` (a frozen copy of the old group logic: the `add`/`remove`
  group lines of `MegaGameEntities` and the group field of `MegaGameEntity`) and `Temp064aGroupIndexEquivalenceTest`.
  The test entity is a `MegaGameEntity` (relaxed `MegamanMaverickGame` mock with a real `GameEngine`) that keeps an
  old-style group field and feeds the frozen copy in `onSpawn` and, when it calls `super.onDestroy()`, in `onDestroy`.
  A seeded generator (at least 200 seeds, at least 200 operations each) draws spawn (with a group, another group, or
  none), destroy, a destroy and a spawn in one frame, `engine.update`, `engine.reset()`, and `on = false`/`true`, over
  pooled (reused) and new entities; one entity kind skips `super.onDestroy()` and never has a group. Group names are
  unique to the suite (the old registry is a global object). After each operation, and from probe systems before and
  after the lookup point during `update`, the suite compares the ordered members of every group used. The "current
  lookup" is one function in the suite: in this step it is `MegaGameEntities.getOfGroup` (the old production code), so
  the suite first proves that the frozen copy and the generator match the real old behavior.
- Check: `core:test` passes, including the new suite, on the old production code.
- Done: yes

### 5. Core uses `GroupsSystem`

- Changes:
  - `C/MegamanMaverickGame.kt` `createGameEngine()`: add `GroupsSystem()` as the last system, after `AudioSystem`.
  - `C/entities/contracts/MegaGameEntity.kt`: remove `group`; in `onSpawn`, at the old place, read `ConstKeys.GROUP`
    and, if not `null`, get or add the `GroupsComponent` and add the name; remove `group = null` from `onDestroy`.
  - `C/entities/MegaGameEntities.kt`: remove `groupToEntities`, the group lines in `add`/`remove`, and `getOfGroup`.
  - `C/entities/utils/GameEntityUtils.kt`: `getStandardBodyDoUpdatePredicate` reads the names from the entity's
    `GroupsComponent` (no cast for the group); `getUpdateWhenGroupNearCameraPredicate` takes the names and the
    `GroupsSystem` (from `game.getSystem(GroupsSystem::class)`, once, when the predicate is built) and returns `true`
    if any member of any of the groups is an `IBodyEntity` near the camera.
  - The temporary suite: its "current lookup" becomes `GroupsSystem.getEntitiesInGroup`, with a `GroupsSystem` in the
    test engine between the two probe systems.
  - New permanent core test (for example `EntityGroupsTest`): the component is written from `ConstKeys.GROUP`; no
    component with no group; a pooled entity that spawns again with no group has an empty component; a
    `MegaGameEntity` that skips `super.onDestroy()` and has a group leaves its group at destroy.
- Check: `engine:test core:test` pass, including the temporary suite; greps: no `group` member in `MegaGameEntity`, no
  `groupToEntities`/`getOfGroup` in `core/`.
- Done: yes

### 6. Docs

- Changes: root `CLAUDE.md`: the `MegaGameEntity` line drops `group`; the system order line ends with `Audio, Groups`;
  the engine section says that entity groups are a `GroupsComponent` and a `GroupsSystem` (`getEntitiesInGroup`), and
  that `GameSystem` has `onEntityJoined`/`onEntityLeft`.
- Check: the lines read correctly.
- Done: yes

### 7. Contract checks

- Changes: none (fixes only if a check fails).
- Run the clean build and all tests; compare the warnings with step 1 (list each new warning in the process log); the
  greps in `CONTRACT.md`; `contact-rule-selection.txt` unchanged; the review items in `CONTRACT.md`. Then short smoke
  launches (about 15 to 20 seconds each) of `--level MOON_MAN --room rest_room_1` and
  `--level WILY_STAGE_3 --room room5`, to check that the levels load with no exception. Then STOP and ask for the
  owner's play test with the manual checks in `CONTRACT.md`.
- Check: every automated box in `CONTRACT.md` ticked and logged.
- Done: yes

### 8. After the play test

- Changes: delete `Temp064aGroupIndexEquivalenceTest` and `Temp064aFrozenGroupRegistry` in their own commit. Run the
  automated checks again. Update the process log and the status block, re-read the `063b` folder for overlap, and
  update `docs/NOTES.md`.
- Check: build and tests pass; the test counts drop only by the temporary suite; the warnings are the same as in
  step 7.
- Done: yes
