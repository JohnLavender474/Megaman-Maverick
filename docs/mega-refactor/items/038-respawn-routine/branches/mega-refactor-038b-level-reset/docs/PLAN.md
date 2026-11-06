# Plan: mega-refactor-038b-level-reset

Ordered steps. Each step has its own check. No game launch (owner rule, 2026-11-06).

### 1. Frame test first

- Changes: in `RespawnRoutineTest`, record `LEVEL_RESET` per frame. Tests: once in the frame of the level reset at a menu start, a launch start and a respawn; none on game over; none after `cancel`.
- Check: `./gradlew core:test --tests '*RespawnRoutineTest*'` fails (the event does not exist yet: compile error is the expected failure).
- Done: yes

### 2. `LEVEL_RESET` from the routine

- Changes: `EventType.LEVEL_RESET`. `ResetLevel.begin` submits it after `context.resetLevel()`.
- Check: `RespawnRoutineTest` passes.
- Done: yes

### 3. Move the 24 listeners and remove `PLAYER_SPAWN`

- Changes: each listener uses `LEVEL_RESET` at the same place in its key list or `when`. Remove `EventType.PLAYER_SPAWN`, the submit and `TODO` in `playerSpawnEnd()`. Reword the `Togglee` comment. Update `GateTest`, `PlayerLifecycleTest`, `MegaLevelScreenEventsTest`.
- Check: `grep -rn "PLAYER_SPAWN\b" core/src engine/src` gives 0 lines; `./gradlew core:test` passes.
- Done: yes

### 4. READMEs

- Changes: `screens/levels/respawn/README.md` (status, frame diagram), `players/README.md` (spawn sequence).
- Check: `grep -rn "PLAYER_SPAWN" core/src/main` gives 0 lines, docs included.
- Done: yes

### 5. Mutation checks

- Changes: none kept. Move the submit to `BeamIn`, drop it, send it twice; each must fail at least 1 test.
- Check: each mutation fails a test; the tree is clean after.
- Done: yes

### 6. Full checks, review and play test

- Changes: none.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`; the review before the play test (code-review subagents, no launch); the owner's play checks in `CONTRACT.md`.
- Done: yes
