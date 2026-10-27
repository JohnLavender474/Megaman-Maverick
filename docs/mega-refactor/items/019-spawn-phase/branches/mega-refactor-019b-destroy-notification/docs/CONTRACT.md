# Contract: mega-refactor-019b-destroy-notification

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-27

What must stay the same: the engine does exactly what it does today (the hooks only add calls); a spawner makes
the same spawns at the same pass as today, except in the window between its entity's destroy call and the next spawn
pass (the intended changes below).

## Automated checks

All on a clean build (`./gradlew clean engine:test core:test lwjgl3:jar`).

- [ ] The build passes and the jar builds.
- [ ] All engine and core tests pass, 0 failures.
- [ ] The compiler warnings are the same set as the baseline (counted at plan step 1).
- [ ] **Temporary contract suite** `TempOldEngineContractTest` with the frozen copy `TempOldGameEngine` (today's
  engine, after `062b`): on at least 10,000 seeded random call sequences with nested calls, the old and the new
  engine give the same ordered action log, return values and states after every call. Deleted in the last commit
  before the merge; that commit runs the other checks again.
- [ ] **Permanent engine tests for the hooks:** each accepting case of `onDestroyQueued` and `onSpawnQueued` calls the
  hook once with the new state; each refused, repeated or reset case does not call it; on seeded random sequences a
  hook runs exactly when a call changes "live after the next update" (scope item 9).
- [ ] **Core spawner tests**, written first and run on the baseline:
  - an event spawner whose entity is destroyed after the spawn pass accepts the events of that frame (fails on the
    baseline: the `TODO` case 1);
  - a bounds spawner whose entity dies off screen just before its spawn area enters the camera spawns it when the
    area enters (fails on the baseline: the `TODO` case 2);
  - a spawner whose pooled entity is destroyed and then fetched and spawned by another caller before the next spawn
    pass does not keep holding it (fails on the baseline: the pooled-reuse risk);
  - a spawner still frees its spawn on a rejected spawn, and on `reset()`; a culled or removed spawner leaves no
    runnable on its entity.
  The baseline results go in the process log.
- [ ] Greps: no `entity.dead` poll left in `Spawner`; no `camera.getRotatedBounds()` per bounds spawner; the `TODO` at
  `MegaLevelScreen.kt:938-942` is gone.
- [ ] `contact-rule-selection.txt` is unchanged.

## Manual playthrough

The owner plays these once, after the implementation, from the worktree.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`: kill the `Met` and the `SniperJoe`; walk to the
  right end of the room and back: both respawn when their spawn points enter the screen again, and not before.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room5`: the camera spawners of the room (the `CannoHoney` hives):
  kill them, scroll away and back: they respawn as before.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room8`: the room-entry spawns (`BulbBlaster`, `LaserBeamer` and
  the room's other `spawn_room` entities) appear on entry; kill the `BulbBlaster`s, leave the room and come back: they
  appear again.
- `./run-game-alpha.sh --level DESERT_MAN --room room9`: the `Lantern`s of the room appear on entry; leave to
  `room10` and come back to `room9`: they appear again in each room.
- `./run-game-alpha.sh --level DESERT_MAN --room room14`: the `TorikoPlundge` (a `spawn_room` enemy) appears on entry;
  kill it, leave and come back: it appears again.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die and respawn: the room's spawns come back as
  before.

## Behaviors to check by review

- `GameEngine` change is only the two hook calls (the temporary suite proves no other change).
- A respawned entity (`062b`) is not taken back by its spawner (scope decision); only the ice shield respawns, and
  no spawner makes it.

## Intended changes

1. An event spawner whose entity is destroyed after the spawn pass of a frame accepts that frame's events (today it
   drops them and its entity comes back only at the next room entry or respawn). This is `TODO` case 1.
2. A bounds spawner whose entity dies off screen just before its spawn area enters the camera spawns the entity when
   the area enters (today only after the camera leaves the area and comes back). This is `TODO` case 2.
3. A spawner no longer keeps a pooled entity that another caller fetched and spawned before the spawner polled
   (today it can hold it forever, so its own entity never comes back).
4. A `spawn_room` spawner whose entity is destroyed in `beginTransition` accepts that frame's `BEGIN_ROOM_TRANS`. Each
   `spawn_room` entity in the live maps lists one room, so the predicate still says no: no visible change expected
   (inferred).
