# 019: One spawn phase per tick with destroy notifications

- Status: active
- Type: refactor
- Depends on: none
- Related: `010`, `013`, `038`, `041`, `045`
- Branches: `mega-refactor-019a-remove-second-spawn-pass` (merged 2026-10-22, squash `21df34661`), `mega-refactor-019b-destroy-notification` (active)

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

## Open questions

- Must a destroyed entity be able to respawn in the same tick?

## Sources

- `NOTES.md` in this folder.
