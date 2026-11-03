# Plan: mega-refactor-019c-merge-spawn-queues

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-03 (owner: "yes")

New API in this branch, with its first caller:

- `SpawnsManager(spawn: (MegaGameEntity, Properties) -> Boolean)`, in place of `SpawnsManager(spawns: Array<Spawn>)`.
  Production caller: `MegaLevelScreen.init`, which passes a lambda that calls `engine.spawn`.

## Steps

### 1. Baseline

- Changes: none. Clean build in the worktree.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. Write the test counts and the sorted set
  of unique compiler warnings to the scratchpad (`019c-warnings-baseline.txt`) and the counts to the process log.
- Done: yes

### 2. `SpawnsManager` gives each spawn to the engine

- Changes:
  - `SpawnsManager`: the constructor takes the spawn function. In `update`, when a spawner fires, call the function
    with the spawn's entity and properties; if it returns `false`, call `spawner.onSpawnRejected()`. The removal of a
    non-respawnable spawner stays as now. `reset()` no longer clears an array.
  - `Spawn`: remove `onRejected`.
  - `MegaLevelScreen`: remove the `spawns` field, the two drain loops in `render` and `spawns.clear()` in `hide()`.
    `init` builds `SpawnsManager { entity, props -> engine.spawn(entity, props) }`. Nothing takes the place of the
    second drain.
  - `SpawnerReleaseTest`: build `SpawnsManager` with the test engine's `spawn`; remove its `spawns` array and drain.
  - New `core/src/test/.../spawns/SpawnsManagerTest.kt`: with stub spawners and a recording spawn function, check
    that one `update` gives each fired spawn in spawner order; a `false` from the function calls `onSpawnRejected` on
    that spawner only; a non-respawnable spawner is removed after its spawn and is not tested again.
- Check: `./gradlew core:test` passes, with all `SpawnerReleaseTest` cases unchanged and the new test.
- Done: yes

### 3. Remove the unread layer-builder param

- Changes: remove `spawnsManager` from `MegaMapLayerBuildersParams` and from its construction in
  `MegaLevelScreen.getLayerBuilders`.
- Check: `./gradlew core:compileKotlin core:compileTestKotlin core:compileSmokeTestKotlin` passes.
- Done: yes

### 4. Rename to `PlayerCheckpointManager`

- Changes: `git mv` `PlayerSpawnsManager.kt` to `PlayerCheckpointManager.kt` and `PlayerSpawnsManagerTest.kt` to
  `PlayerCheckpointManagerTest.kt`; rename the class, the test class, and the field `playerSpawnsMan` to
  `playerCheckpointMan` in `MegaLevelScreen` (including its log text). The package and the class body stay.
- Check: `./gradlew core:test` passes; the test cases of the renamed test are unchanged (`git diff -M` shows only the
  name lines).
- Done: yes

### 5. Contract automated checks and short launches

- Changes: none.
- Check:
  - `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
  - The sorted set of unique warnings is not larger than the baseline.
  - `git diff master -- core/src/test/resources/contact-rule-selection.txt` is empty.
  - `git diff master --stat -- engine/` is empty (no `GameEngine` change).
  - A short launch (`timeout 12`) of each room in the contract's manual list loads with no exception.
- Done: yes

### 6. Owner's play test

- Changes: none.
- Check: the owner plays the four manual checks in `CONTRACT.md`. Write the result in the process log.
- Done: yes
