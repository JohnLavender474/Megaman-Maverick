# Plan: mega-refactor-061a-thin-player

How to do the work in `SCOPE.md`. Written ahead of time on `master`.

- Status: approved
- Approved: 2026-10-26

Build with `JAVA_HOME=~/.jdks/corretto-17.0.13`. Each step is its own commit; after each code step, the build and
the tests pass.

New APIs and their first callers: see `SCOPE.md`.

## Steps

### 1. Branch start

- Changes: on `master`, docs only: the `061a` row and item `061` to `active` with the start date; `ITEM.md` branches
  line; the branch `process-log.md` to `active`. Then
  `git worktree add -b mega-refactor-061a-thin-player .worktrees/mega-refactor-061a-thin-player master`, and in it
  `git merge --no-ff temp-thin-player`.
- Check: `git worktree list`; the merge has no conflict.
- Done: yes

### 2. Baseline

- Changes: none. A clean build in the worktree.
- Check: the test counts (expected 493 engine, 163 core, the temp branch's numbers) and 114 warnings, in the process
  log.
- Done: yes

### 3. Review notes into the branch folder

- Changes: move `docs/temp-thin-player-review.md` to this folder's `docs/NOTES.md` (under "Decisions" and a section
  for the temp branch's notes).
- Check: no review file left at the root of `docs/`.
- Done: yes

### 4. Locks to the engine

- Changes:
  - Engine `locks/`: `LockGroup` and `LockReason` interfaces with `index`; `LocksComponent(groupCount)` (one `Int` bit
    set per group; refuses a reason index outside `0 until 32` and a group index outside the count); a change hook on
    the component (called on each lock with the group, and when a group becomes free); the free functions `lock`,
    `unlock`, `clearLocks`, `isLocked` on `IGameEntity`.
  - Core: `MegaLockGroup` and `MegaLockReason` enums (explicit indexes, the same order as today); `Megaman` adds the
    component with the hook that calls `setAllBehaviorsAllowed(false)` on each `BEHAVIORS` lock and
    `setAllBehaviorsAllowed(true)` when `BEHAVIORS` becomes free; the call sites change their imports and enum names.
  - Tests: the generic lock tests move to the engine; the behaviors test stays in core and goes through `Megaman`'s
    hook (or a test entity with the same hook).
- Check: build and tests pass; `core/.../locks/` holds only the two enums; a short launch of
  `REFACTOR_TEST_029 --room movement` and `INTRO_STAGE --room room12` shows no exception.
- Done: yes

### 5. Test leftovers and import order

- Changes: `TestPlayerEntity` loses its unused `spawn` override and `spawnedWith`; imports in sorted order where the
  temp branch's script put them out of order.
- Check: build and tests pass; warnings stay at 114.
- Done: yes

### 6. Contract checks

- Changes: none (fixes only if a check fails).
- Check: every automated check in `docs/CONTRACT.md` on a clean build; results in the process log; then the manual
  checks for the owner.
- Done: yes

### 7. Owner's play test and merge

- Check: the owner plays the manual checks and approves the merge; then "Branch merge" in
  `docs/mega-refactor/CLAUDE.md`. After the merge, delete `temp-thin-player` and its worktree.
- Done: yes
