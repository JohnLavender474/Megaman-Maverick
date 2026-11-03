# 019: One spawn phase per tick with destroy notifications

- Status: done
- Type: refactor
- Objectives: `O1`, `O2`
- Depends on: none
- Related: `010`, `013`, `038`, `041`, `045`
- Branches: `mega-refactor-019a-remove-second-spawn-pass` (merged 2026-10-22, squash `21df34661`), `mega-refactor-019b-destroy-notification` (merged 2026-10-27, squash `7a30c8982`), `mega-refactor-019c-merge-spawn-queues` (merged 2026-11-03, squash `5e4483993`)

## Goal

Replace the two `spawnsMan.update(delta / 2f)` passes and the four spawn queues with one explicit spawn phase.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- One spawn phase at a fixed point in the tick, instead of two passes.
- Tell a spawner at once when its entity is destroyed, instead of polling `dead`. Do not drop events while a spawn is held. (This is what the second pass fixes today.)
- Merge the spawn queues. Compute the camera bounds once per phase (a quick win against pooled-rectangle churn).
- Optional: a spatial or room index for spawners, if profiling shows a cost.
- Check and fix the possible missed respawn after a room transition (`BEGIN_ROOM_TRANS` dropped).

## Owner's input

Answer to review item 18 (2026-10-22, verbatim):

> let's discuss this further; the `spawnMan` obj runs twice per frame since certain entities won't be spawnable until after the engine has run, but they need to spawn within the same frame instead of waiting until the next frame

## Decisions

- K (2026-10-22): research found no entity that needs the second `spawnsMan.update` pass. The pass after `engine.update` is the original; commit `aea69ae3a` (2024-09-12) added the pass before it. No version of the pass ever gave a same-frame spawn. It only refreshes spawner state one frame early. See `NOTES.md`.
- (2026-10-22) The owner agrees with the destroy-notification design.
- (2026-10-22) Remove the second call first, in its own small branch `mega-refactor-019a-remove-second-spawn-pass`.
- (2026-11-03, `019c` scan) Only two of the four "spawn queues" were queues: `spawns` (the screen array between `SpawnsManager` and the engine) and the engine queue. `playerSpawnsMan` picks the current checkpoint and spawns nothing, and `SPAWN_NOW` already spawns into the engine queue at level build. `019c` removed `spawns`, so the engine queue is the only one. The owner renamed `PlayerSpawnsManager` to `PlayerCheckpointManager`.
- (2026-11-03) The optional spatial or room index for spawners moved out of this item to `125`, with a measure first. The item is done after `019c`.
- (2026-11-03) The `BEGIN_ROOM_TRANS` drop was fixed by `019b` (its intended change 4 and a `SpawnerReleaseTest` case); the issue closed at the `019c` merge.
- (2026-11-03) A destroyed entity does not respawn in the tick of its `destroy()` call. It respawns in the next tick at the earliest. This is the behavior on `master` after `019b`: `spawnsMan.update` runs once per frame, before `engine.update`, so a spawner that is released during frame N can spawn only in frame N+1. The engine needs no spawn flush at the end of the update. `019c` must keep this rule and must not add a spawner pass after `engine.update`.

## Open questions

None.

## Sources

- `NOTES.md` in this folder.
