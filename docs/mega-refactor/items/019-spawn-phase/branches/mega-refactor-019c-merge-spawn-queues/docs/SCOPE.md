# Scope: mega-refactor-019c-merge-spawn-queues

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-11-03 (owner: "1. Yes. 2. Sure, go for it. 3. Yes, go for it.")

## Goal

Have one spawn queue: the engine queue. Map spawners give their spawns to the engine in the spawn phase. The level
screen keeps no queue of its own.

## Item

Item `019` ("One spawn phase per tick with destroy notifications"). From `ITEM.md`:

- Goal: "Replace the two `spawnsMan.update(delta / 2f)` passes and the four spawn queues with one explicit spawn
  phase." (The two passes went in `019a`.)
- Proposed scope: "Merge the spawn queues. Compute the camera bounds once per phase (a quick win against
  pooled-rectangle churn)." (The camera bounds went in `019b`.)
- Proposed scope: "Optional: a spatial or room index for spawners, if profiling shows a cost."
- Proposed scope: "Check and fix the possible missed respawn after a room transition (`BEGIN_ROOM_TRANS` dropped)."
- Decision (2026-11-03): a destroyed entity respawns in the next tick at the earliest. `019c` must not add a spawner
  pass after `engine.update`.

## Current state

The item names four queues: `spawns`, the engine queue, `playerSpawnsMan` and `SPAWN_NOW`. The scan found that only
two of them are spawn queues.

1. **`spawns`** (`MegaLevelScreen.kt:234`): an `Array<Spawn>` that the screen gives to `SpawnsManager`
   (`MegaLevelScreen.kt:265`). `SpawnsManager.update` adds one `Spawn` for each spawner that fires
   (`SpawnsManager.kt:31-34`), with `onRejected = spawner::onSpawnRejected`. The screen drains it two times per frame:
   - `MegaLevelScreen.kt:926-931`, right after `spawnsMan.update`, before `fights.update` and `engine.update`;
   - `MegaLevelScreen.kt:953-958`, after `engine.update`. **This drain always finds the array empty.**
     `SpawnsManager` is the only writer of `spawns`, and `spawnsMan.update` runs only before the first drain
     (since `019a`). `spawns` is `private` and has no other reader or writer.
   - `hide()` clears it (`MegaLevelScreen.kt:1106`), and `SpawnsManager.reset()` clears it again.
   - `Spawn.onRejected` (`Spawn.kt:9`) exists only for this hand-off.
2. **The engine queue** (`GameEngine.spawn`, `GameEngine.kt:82`): every other spawn goes here directly. The engine
   adds the queued entities at the start of `engine.update`. This is the queue that stays.
3. **`playerSpawnsMan`** (`PlayerSpawnsManager.kt`) is not a spawn queue. It holds the player spawn map objects and
   picks the current checkpoint when a spawn area enters the camera (`run()`, called after `engine.update` at
   `MegaLevelScreen.kt:951`). It spawns no entity. The player spawns through `playerSpawnEventHandler` with
   `playerSpawnsMan.currentSpawnProps` (`MegaLevelScreen.kt:552`).
4. **`SPAWN_NOW`** (`SpawnersLayerBuilder.kt:85-94`) is not a separate queue. At level build it fetches the entity and
   calls `entity.spawn(spawnProps)`, which goes into the engine queue. The first `engine.update` of the level adds it.
   Ten live or old TMX files use it (for example `ReactorMan_v2.tmx`, `TimberWoman_v2.tmx`).

Other findings:

- `MegaMapLayerBuildersParams.spawnsManager` (`MegaMapLayerBuilders.kt:19`) has no reader. Only `spawnCameraBounds`
  of the params is read (`SpawnersLayerBuilder.kt:179`).
- `MegaEntityFactory.fetch` takes the entity out of its pool (`Pool.fetch`), so two spawners of the same class in one
  pass get different entities, whether the engine gets each spawn at once or after the pass.
- The test `SpawnerReleaseTest` (`core/src/test/.../spawns/SpawnerReleaseTest.kt:34-50`) copies the screen's
  hand-off: its own `spawns` array and a drain that calls `engine.spawn` and `onRejected`.
- Issue `docs/issues/2026-10-22-spawner-drops-room-transition-event.md` (status `open`, not verified): a
  `SpawnerForEvent` drops `BEGIN_ROOM_TRANS` because it still holds the spawn of an enemy that the room transition
  destroyed. Since `019b`, the spawner lets go of its spawn at the `destroy()` call (`runnablesOnDestroyQueued`), so it
  is free when the event is delivered in `events.run()`. By reading, `019b` already fixed it (inferred). It needs a
  check in the game.

## Will change

1. `SpawnsManager` gives each spawn to the engine at once, in `update`: it calls the spawn function, and if the engine
   refuses the spawn, it calls `spawner.onSpawnRejected()`. The order of spawns to the engine stays the spawner order.
   All spawns still reach the engine before `fights.update` and `engine.update` in the same frame.
2. Remove the `spawns` array from `MegaLevelScreen`, its two drains and its clear in `hide()`.
3. Remove `Spawn.onRejected`. `Spawn` keeps `entity` and `properties`.
4. Remove the unused `MegaMapLayerBuildersParams.spawnsManager`.
5. `SpawnerReleaseTest` builds `SpawnsManager` with the new spawn function, and its frame no longer drains a queue.
6. Close the `BEGIN_ROOM_TRANS` issue at the merge. `019b` fixed it: its intended change 4 names this case, and
   `SpawnerReleaseTest` "accepts the events of the frame in which its entity is destroyed after the spawn pass" proves
   it. No live map has a `spawn_room` entity that lists two rooms, so no game check can show it (`019b` contract).
7. Rename `PlayerSpawnsManager` to `PlayerCheckpointManager` (file and class), `PlayerSpawnsManagerTest` to
   `PlayerCheckpointManagerTest`, and the field `playerSpawnsMan` to `playerCheckpointMan`. The package stays. No
   behavior change.
8. Item docs on `master` at merge: correct the item text, because `playerSpawnsMan` and `SPAWN_NOW` are not queues;
   change the old class name in the forward-looking docs (the item docs of `019`, `036`, `037`, `047`, `094`, `109`
   and the two open checkpoint issues). History docs stay as written. Set item `019` to `done`.

New API, with its first caller:

- The `SpawnsManager` constructor takes a spawn function `(MegaGameEntity, Properties) -> Boolean` in place of
  `Array<Spawn>`. Callers: `MegaLevelScreen.init` passes `engine::spawn` (or a lambda that calls it);
  `SpawnerReleaseTest` passes its test engine.

## Will not change

- The checkpoint rule of `PlayerSpawnsManager` (renamed only). It is not a spawn queue. (Its `TODO` waits for item
  `036`.)
- `SPAWN_NOW`: it already uses the engine queue. A change to a spawner would move the spawn from level build to the
  first spawn phase, with no gain.
- The place of the spawn phase in the frame: `spawnsMan.update` stays before `fights.update` and `engine.update`.
  No spawner pass after `engine.update` (decision of 2026-11-03).
- The engine queues (`entitiesToSpawn`, `entitiesToKill`, `entitiesToRespawn`) and `GameEngine.update`.
- The spawner triggers, the destroy notification of `019b`, and the shared spawn camera rectangle.
- The spatial or room index for spawners. The item makes it optional and asks for profiling first. It becomes a
  backlog candidate (`docs/TODO.md`) with a measure as its first step.
- Direct `entity.spawn(...)` calls in entities. They already use the engine queue.

## Objective check

| Rule | Check | Reason |
|------|-------|--------|
| `O1-R1` No global session state | `waiting` | No new global. `SpawnsManager` takes its spawn function from the screen. |
| `O1-R2` Components, not new inheritance | `no effect` | No new class hierarchy. |
| `O1-R3` No new fragile order | `helps` | One hand-off less: the spawns no longer wait in a screen array whose drain must come before `engine.update`. |
| `O1-R4` Typed state | `no effect` | No new key. |
| `O1-R5` Code only with a caller | `helps` | Removes the dead second drain, `Spawn.onRejected` and an unread param. The new constructor parameter has a caller. |
| `O2-R3` One fixed tick | `waiting` | The spawn phase stays one per frame; no change to delta. |
| `O2-R4` Stable order | `no effect` | Spawns reach the engine in spawner order (an `Array`), as now. |
| `O2-R5` No single-player logic | `no effect` | No player logic. |
| `O2-R7` Gameplay apart from rendering | `no effect` | The spawn phase stays in `render`, as now; `010` moves the tick. |
| `O5-R1` No per-frame allocation | `helps` | No `Array` iterator over `spawns` two times per frame. `Spawn` objects are still made per spawn, as now. |
| `O5-R2` Measure hot paths | `no effect` | The spawn pass cost does not grow. No measure planned; the change only removes work. |
| `O6-R1` CI passes | `no effect` | Checked at merge. |
| `O6-R2` Named checks | `helps` | `SpawnerReleaseTest` keeps its cases on the new path; the contract names the room checks. |

The other always-on rules (`O2-R1`, `O2-R2`, `O2-R6`, `O2-R8`, `O2-R9`, `O3-R1`, `O7-R3`, `O8-R2`, `O9-R1`, `O9-R2`):
`no effect`.

## Overlap with other branches

- `mega-refactor-118b-batch-1` (active) changes `MegaLevelScreen.kt` at lines 716-723 (the boss spawn event). This
  branch changes lines 234, 265, 926-958 and 1106. No shared lines, but both branches change the same file, so the
  second merge must take in `master` first.
- Item `018` (status `idea`) notes that a `019c` change to `GameEngine.kt:219-240` would conflict with its branch b2.
  This scope does not change `GameEngine`, so there is no overlap.

## Open questions

Answered by the owner on 2026-11-03: 1 yes, 2 yes, 3 yes. The answers are in the scope above and in `NOTES.md`.

1. Is it correct to drop `playerSpawnsMan` and `SPAWN_NOW` from the work? (Recommended: yes. Neither is a spawn
   queue, and a change would add risk with no gain.)
2. After this branch, is item `019` `done`? The spatial index stays optional and waits for profiling. (Recommended:
   yes. Move the index to the backlog as a new idea if profiling shows a cost.)
3. Rename `PlayerSpawnsManager` to `PlayerCheckpointManager` and the field `playerSpawnsMan` to
   `playerCheckpointMan` in this branch? (Owner's idea, 2026-11-03.) The name makes the class look like a spawn queue.
   Code: 3 files, 25 references (`MegaLevelScreen.kt`, `PlayerSpawnsManager.kt`, `PlayerSpawnsManagerTest.kt`); the
   package stays. Docs on `master` at merge: the item docs of `019`, `036`, `037`, `047`, `094`, `109` and the two
   open checkpoint issues; history docs stay as written. No behavior change. (Recommended: yes, in this branch. The
   other choice is to wait for `036`, which reworks the class.)
