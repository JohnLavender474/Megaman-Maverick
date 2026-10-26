# Plan: mega-refactor-061c-entity-queries

How to do the work in `SCOPE.md`. Written ahead of time on `master`.

- Status: approved
- Approved: 2026-10-26

Build with `JAVA_HOME=~/.jdks/corretto-17.0.13`. Read the test counts from `build/test-results`. Smoke launches are
short (about 20 seconds); the timeout with no exception is a pass. Each step is its own commit; after each code step,
the build and the tests pass and the game plays the same. Steps 3 and 4 only add functions; step 5 is the switch,
which cannot be split, because the old forwarding functions are extensions on the view and go with it.

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`.
New APIs and their first callers: see `SCOPE.md`.

## Steps

### 1. Branch start

- Changes: on `master`, docs only: the `061c` row to `active` with the start date; the `ITEM.md` branches line; the
  branch `process-log.md` to `active`. Then
  `git worktree add -b mega-refactor-061c-entity-queries .worktrees/mega-refactor-061c-entity-queries master`.
- Check: `git worktree list`.
- Done: yes

### 2. Baseline

- Changes: none. A clean build in the worktree.
- Check: the test counts (expected 501 engine, 167 core) and 110 warnings, in the process log.
- Done: yes

### 3. Engine query functions and the player component

- Changes: `E/entities/EntityQueries.kt` (on `IGameEntity`: `body`, `hasBody`, `facing`, `direction`,
  `isBehaviorActive`, `isAnyBehaviorActive`, `input` with no nullable step); `E/players/PlayerComponent.kt`
  (`PlayerComponent(slot)`) with `playerSlot` and `isPlayer`. Tests for each (needed parts fail, optional parts
  empty).
- Check: build and tests pass; `javap`: no `InputFrame.box-impl` in `input`. The new functions have their callers in
  step 5 (this branch).
- Done: yes

### 4. Core query functions

- Changes: `C/entities/EntityQueries.kt` (on `IGameEntity`: `focus`, `invincible`, `damaged`, the health functions,
  the six fixture getters, `shooting`); `lightOrNull` and `onGravityDeviceSwitch` on `IGameEntity` next to their
  components in `C/players/`. Tests.
- Check: build and tests pass. Callers in step 5.
- Done: yes

### 5. The switch: players are entities

- Changes:
  - Engine: `Players` becomes a slot → `IGameEntity` registry (`register` adds `PlayerComponent(slot)`, `unregister`
    removes it; `get`; the loops over entities in slot order; `registeredSlots`); `IPlayers` without the view type
    parameter; `PlayerView` and `viewOf` go. `PlayersTest` follows.
  - Core: `MegaPlayers` registers entities and binds the input source to the entity's `ControllerComponent`;
    `targetFor` returns the entity; the damage signal's payload carries the entity. `MegaPlayer`,
    `MegaPlayerAccess.kt` and the core `PlayerComponent` go. `asPlayer` returns the entity if it has a
    `PlayerComponent`, else `null`; `IMegaGameEntity.targetPlayer` returns the entity.
  - Callers: a script moves the repeated reads (`player.X` → `player.X` on the entity where the type changes, the
    `MegaPlayer` type names in the 25 files to `IGameEntity`, the imports from `players.` to the new query files),
    and the compiler finds the rest; the view-only uses read `playerSlot`, `input`, `spawned`; the `megaman`
    accessor's uses become `as Megaman` casts; `WorldGravity`, `AbstractBoss.onPlayerDamaged`, the items'
    `contactWithPlayer` and the `HIT_BY_PLAYER` receiver take the entity.
  - Tests: `MegaPlayersTest` (input binding, `targetFor`, the damage signal), and the tests that register `Megaman`.
- Check: build and tests pass; warnings not above 110; the one-for-one read script; greps (no `MegaPlayer`,
  `PlayerView`, `viewOf`, `MegaPlayerAccess`, `megaman` accessor). Short launches of `REFACTOR_TEST_029` `movement`,
  `enemy_aim`, `pickups`, `INTRO_STAGE --room room12` and `REFACTOR_TEST_031 --room pipe_portal` show no exception.
- Done: yes

### 6. Contract checks

- Changes: none (fixes only if a check fails).
- Check: every automated check in `docs/CONTRACT.md` on a clean build; results in the process log; then the manual
  checks for the owner.
- Done: yes

### 7. Owner's play test and merge

- Check: the owner plays the manual checks and approves the merge; then "Branch merge" in
  `docs/mega-refactor/CLAUDE.md`.
- Done: no
