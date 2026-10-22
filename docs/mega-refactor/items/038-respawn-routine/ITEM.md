# 038: One ordered respawn routine instead of an event chain

- Status: planned
- Type: refactor
- Depends on: none
- Related: `012`, `013`, `029`, `036`, `037`, `051`, `053`
- Branches: none yet

## Goal

Replace the 5-event respawn chain across frames with one routine that runs fixed steps in a fixed order.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A `RespawnRoutine` with fixed steps: reset world, place player, reset camera and room, apply checkpoint data, music, beam-in, ready.
- Keep the public events for entities, but submit them from one place in a set order.
- Remove the stale room-music block in `PLAYER_SPAWN_INIT` (`MegaLevelScreen.kt:570-579`).

## Owner's input

Answer to review item 37 (2026-10-22, verbatim):

> yes, this is what I meant when I mentioned earlier about tightly coupling to a specific series of events. this is an extreme example. the existing player-spawn handler does offer a somewhat centralized handling of the event order, but taking this even further and cleaning up the tight coupling to event-driven logic would be ideal

## Open questions

None yet. The scan for the first branch will find them.

## Sources

- `NOTES.md` in this folder.
- `items/036-checkpoints-as-data/NOTES.md`: the full spawn and checkpoint review (Q1, Q3 and candidate 3).
