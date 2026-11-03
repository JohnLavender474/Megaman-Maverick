# Contract: mega-refactor-019c-merge-spawn-queues

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-03 (owner: "yes")

## Intended changes

None in gameplay. The branch changes only how the map spawns reach the engine (one queue less) and two names.

## Behavior that must not change

1. A map spawn reaches the engine in the same frame as its spawner test, before `fights.update` and `engine.update`,
   and the entity is added at the start of that `engine.update`.
2. The spawns reach the engine in spawner order.
3. When the engine refuses a map spawn, the spawner is free again (`onSpawnRejected`), and a rejected pooled entity
   goes back to its pool (the engine `onSpawnRejected` hook).
4. A destroyed entity respawns in the next tick at the earliest (item `019` decision of 2026-11-03). No spawner pass
   runs after `engine.update`.
5. The spawner triggers and the destroy notification of `019b`: camera spawners, room-entry (`spawn_room`) spawners,
   event spawners, non-respawnable spawners.
6. `SPAWN_NOW` entities spawn at level build, as now.
7. Checkpoints: the current checkpoint changes when a player spawn area enters the camera, the "CHECKPOINT ..." text
   shows, and a death respawns the player at the current checkpoint.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with every `SpawnerReleaseTest` case on the new hand-off (rules 1, 3, 4, 5).
- [ ] New `SpawnsManagerTest` (permanent): one `update` gives each fired spawn to the spawn function in spawner order,
      in the same call (rules 1 and 2); a refused spawn frees its spawner (rule 3); a non-respawnable spawner is
      removed after its spawn, as now.
- [ ] `PlayerCheckpointManagerTest` (the renamed `PlayerSpawnsManagerTest`) passes with no change to its cases
      (rule 7).
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warning count does not grow (baseline taken in the first plan step).
- [ ] `contact-rule-selection.txt` is unchanged.

## Manual playthrough

The owner plays these once, after the implementation. All are control checks: the branch has no intended gameplay
change.

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-019c-merge-spawn-queues`

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`: the `Met` and the `SniperJoe` appear when the room
  starts. Kill both, walk to the right end of the room and back: both respawn when their spawn points enter the screen
  again, and not before (rules 1, 4, 5).
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room8`: the room-entry spawns (`BulbBlaster`, `LaserBeamer` and the
  other `spawn_room` entities) appear on entry. Kill the `BulbBlaster`s, leave the room and come back: they appear
  again (rule 5).
- `./run-game-alpha.sh --level DESERT_MAN --room room2`: walk into `room3`: "CHECKPOINT 2" shows. Die in `room3`: you
  respawn at the `room3` spawn (rule 7).
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die and respawn: the room's spawns come back as
  before (rules 4, 5).

## Behaviors to check by review

- `GameEngine` does not change.
- `MegaLevelScreen.render` keeps the order: spawn camera rectangle, `spawnsMan.update` (now with the engine
  hand-off), `fights.update`, the handlers, `engine.update`, `playerCheckpointMan.run()`. The second drain is gone and
  nothing takes its place.
- Order within one pass: before, every spawner test ran before the first `engine.spawn` of the pass; now each spawn
  goes to the engine right after its test. A spawner test reads only its own state, the camera rectangle, the events
  it got and `levelState`, and `engine.spawn` changes none of these. One difference: a pooled entity that the engine
  refuses goes back to its pool at once, so a later spawner of the same class in the same pass can fetch that same
  object (before, it got another pooled object). The entity is unspawned and has no spawner runnable, so this gives
  no visible change (inferred).
- The rename changes names only: no change to the body of the class or its test cases.
