# 038: One ordered respawn routine instead of an event chain

- Status: planned
- Type: refactor
- Depends on: none
- Related: `012`, `013`, `029`, `047`, `057`, `036`, `037`, `051`, `053`
- Branches: none yet

## Goal

Replace the 5-event respawn chain across frames with one routine that runs fixed steps in a fixed order.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A `RespawnRoutine` with fixed steps: reset world, place player, reset camera and room, apply checkpoint data, music, beam-in, ready.
- Keep the public events for entities, but submit them from one place in a set order.
- Remove the stale room-music block in `PLAYER_SPAWN_INIT` (`MegaLevelScreen.kt:570-579`).
- Split `PLAYER_SPAWN` into a spawn event for each player and a level reset that the routine owns. Move the 24 listeners that use `PLAYER_SPAWN` as a level reset (`029` `NOTES.md`, review 2, section 4).
- Define the routine behind an interface. The single-player routine copies today's behavior. `047` adds a co-op routine.

## Owner's input

Answer to review item 37 (2026-10-22, verbatim):

> yes, this is what I meant when I mentioned earlier about tightly coupling to a specific series of events. this is an extreme example. the existing player-spawn handler does offer a somewhat centralized handling of the event order, but taking this even further and cleaning up the tight coupling to event-driven logic would be ideal

Item `029` research (2026-10-23, verbatim):

> This makes the case further that we need a dedicated orchestrator rather than having this logic spread around all over the place.

> Having it behind an interface might not be sufficient if nth players (other than the primary player) can drop in and out of the game on the fly. We can discuss this further when we reach that point.

## Decisions

- 2026-10-23 (from `029`): `PLAYER_SPAWN` must split into a spawn event for each player and a separate level reset. This item does the split. Motivation: with respawn per player (co-op), one player's respawn must not reset the room for everyone.

## Open questions

- Is an interface for the routine enough when players can join and leave during play? Discuss at the scope step, with `047`.

## Sources

- `NOTES.md` in this folder.
- `items/036-checkpoints-as-data/NOTES.md`: the full spawn and checkpoint review (Q1, Q3 and candidate 3).
