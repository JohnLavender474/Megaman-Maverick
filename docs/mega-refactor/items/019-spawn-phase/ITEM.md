# 019: One spawn phase per tick with destroy notifications

- Status: idea
- Type: refactor
- Depends on: none
- Related: `010`, `013`, `038`, `041`, `045`
- Branches: none yet

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

## Open questions

- Which entity needed the second pass? A name lets us confirm the destroy notification covers it. (Decision K.)
- Must a destroyed entity be able to respawn in the same tick?

## Sources

- `NOTES.md` in this folder.
