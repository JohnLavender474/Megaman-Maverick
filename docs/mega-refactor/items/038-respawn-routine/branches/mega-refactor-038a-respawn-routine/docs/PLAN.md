# Plan: mega-refactor-038a-respawn-routine

How to do the work in `SCOPE.md`. Write this file after the owner approves the brief. It needs
no approval. When the plan changes, write the reason in the process log.

## Frame map

`EventsManager.submitEvent` during `run()` delivers in the next frame. A screen `onEvent` runs in `events.run()` at the end of a frame. Each hand-off of the old chain therefore did its next part in the **next `events.run()`**. The routine keeps that rule directly: `AwaitEventsRun` and `BeamUntilEventsRun` finish when `EventsManager.runCount` has gone up since their mark. The routine updates at the top of the unpaused part of `render`, so the work lands where the old `run()` work did (only the `runQueue` drain and the pause checks run in between, and pause is blocked while the lead is dead or not in play).

| Today | Routine |
|---|---|
| `JUST_DIED` handler (destroy others, music off, life, death init, resume), run N | `startAtDeath()` calls `defeat()` at once, run N |
| death timer and fade out, `DONE_DYIN` submit, render N+1 .. M | `WaitAndFadeOut`, render N+1 .. M |
| `DONE_DYIN` handler: level music, lives check, spawn init, run M | `CheckLives` at render M; game over (music, switch, lives reset) goes through `runQueue`, which runs at the start of M+1; then `AwaitEventsRun` |
| spawn init: locks, `TURN_CONTROLLER_OFF`, beam flag, run M | `BeginRespawn` at the top of render M+1 |
| fade-in updates from render M+1 | `BeamUntilEventsRun` updates the beam until the next run |
| `PLAYER_SPAWN_INIT` handler: the level reset, run M+1 | `ResetLevel` at the top of render M+2 |
| `PLAYER_READY`, render R | `BeamIn`, then `Ready`, render R |

Level start: `startAtRespawn()` calls `beginRespawn()` at once, as the old `show()` did. From a menu, `startLevel` runs inside another screen's `render`, so an `events.run()` follows at once, and the reset comes at the top of the first level `render`. From a `--level` launch (`postCreate`), no run comes before the first frame, so the reset comes at the top of the second `render`. Both match the old code.

A death during the beam-in (found by the review; no known path today): the old `when` finished the beam-in to `PLAYER_READY` before the death fade ran. The routine does `defeat()` at once and starts the death steps after `Ready`.

## Steps

### 1. Test map and baseline event log

- Changes: a subagent builds `RefactorTest038.tmx` with the `level-builder` skill, and a `LevelDefinition` entry `REFACTOR_TEST_038` of type `TEST_LEVEL`. Room `death_pit`: the player spawns above a pit and dies with no input; after the respawn it falls again, until game over. Room `respawn_room` (a second checkpoint room with enemies) if the builder can make the pit room lead to it. Launch on the baseline code: `timeout 20 ./run-game-alpha.sh --level REFACTOR_TEST_038 --room death_pit --logTags MegaLevelScreen,PlayerSpawnEventHandler,PlayerDeathEventHandler --logLevels debug,log,error`. Keep the log in `temp/038a-frames/baseline.log`.
- Check: the log shows the order of the "Today" column of the frame map for each death, then game over.
- Done: yes

### 2. `RespawnContext` and the steps

- Changes: new package `screens/levels/respawn/`. `RespawnContext` holds the game, the lead slot, the death fade, the beam-in, the delta, and the screen callbacks (`resetLevel`, `levelMusic`). Steps as `Step<RespawnContext>` objects: `Defeat`, `WaitAndFadeOut`, `CheckLives`, `NextFrame`, `BeginRespawn`, `ResetLevel`, `BeamIn`, `Ready`. `PlayerDeathEventHandler` becomes `DeathFadeOut` and `PlayerSpawnEventHandler` becomes `BeamInView`: timers, fades and draws only, with no event submits and no `init` side effects on players. The `TODO` comments about slot 0 (`047`) move with the code.
- Check: `./gradlew core:compileKotlin`.
- Done: yes

### 3. `RespawnRoutine`

- Changes: `RespawnRoutine` with `startAtDeath()`, `startAtRespawn()`, `update(delta)`, `cancel()`, `draw(batch)`, `finished`. A new engine `Routine` per start (1 allocation per death, not per frame).
- Check: compiles.
- Done: yes

### 4. Screen wiring

- Changes in `MegaLevelScreen`: the `PLAYER_SPAWN_INIT` body becomes the private `resetLevelForRespawn()`, without the room-music block. `onEvent(PLAYER_JUST_DIED)` calls `respawn.startAtDeath()` for slot 0. The `PLAYER_DONE_DYIN`, `PLAYER_SPAWN_INIT` and `PLAYER_READY` branches and mask keys go. `render`: `respawn.update(delta)` at the top of the unpaused block when `endLevelEventHandler.finished`; the `when` keeps only the end-level handler. `show()` calls `respawn.startAtRespawn()`. `draw` and `reset` call the routine. `EventType.PLAYER_SPAWN_INIT` goes. Update `MegaLevelScreenEventsTest` for the new mask.
- Check: `./gradlew core:test`; `grep` finds no `PLAYER_SPAWN_INIT`, `PlayerSpawnEventHandler` or `PlayerDeathEventHandler` in `core/src/main`.
- Done: yes

### 5. Routine tests

- Changes: `RespawnRoutineTest` in `core/src/test/.../screens/levels/respawn/`. Drive `update` frame by frame with a mocked context, and assert: the step order; the frame of `PLAYER_DONE_DYIN`, of `TURN_CONTROLLER_OFF`, of `resetLevel` and of `PLAYER_READY` relative to the start; no respawn step after a failed lives check; `cancel` submits nothing. Break the code for each test and confirm that it fails.
- Check: `./gradlew core:test`.
- Done: yes

### 6. Event order against step 1

- Changes: none. No launch (owner rule of 2026-11-06, "Do not launch the game"). The step 1 log gives the order of today; `RespawnRoutineTest` pins the same order and frames on the branch.
- Check: each event of `temp/038a-frames/baseline.log` (per death: `PLAYER_JUST_DIED`, `PLAYER_DONE_DYIN`, spawn init, beam end; game over at the third death) has a step and a test assertion in the same order.
- Done: yes

### 7. Package README and docs

- Changes: `screens/levels/respawn/README.md` (purpose, the steps, the frame map as a `sequenceDiagram`, the rules a change must keep). Fix the references in `fights/README.md` and `EndLevelEventHandler.kt` comments if they name the old handlers.
- Check: read through.
- Done: yes

### 8. Full checks, review and play test

- Changes: none.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`; then the review before the play test (code-review subagents, no launch); then the owner's play checks in `CONTRACT.md`.
- Done: yes
