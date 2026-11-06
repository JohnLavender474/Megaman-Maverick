# 038: One ordered respawn routine instead of an event chain

- Status: active
- Type: refactor
- Objectives: `O1`, `O2`
- Depends on: none
- Related: `012`, `013`, `029`, `047`, `057`, `036`, `037`, `051`, `053`, `118`, `119`
- Branches: see "Branches"

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
- 2026-11-02 (owner, from `119`): the entity keeps the entity fact (`PLAYER_DESTROYED`), and the routine sends the level fact (`LEVEL_FAILED`). If the routine took over the entity submit, slot 1 and fight deaths would lose their event.
- 2026-11-02 (from `118`): the routine can reuse the `Step` and `Routine` type of `118a`. It need not.
- 2026-11-06 (owner, `038a` brief): merge target is directly into `master`. Two branches: `038a` makes the routine with the same public events; `038b` splits `PLAYER_SPAWN` into a spawn event for each slot and `LEVEL_RESET`, and moves the 24 listeners.
- 2026-11-06 (owner, `038a` brief): no interface for the routine yet. `047` designs the co-op shape when it has a second caller.
- 2026-11-06 (owner, `038a` brief): the routine owns the death half and the respawn half. The level start enters at the respawn half.

## Open questions

- Is an interface for the routine enough when players can join and leave during play? Deferred to `047` (decision 2026-11-06).

## Branches

- `mega-refactor-038a-respawn-routine`: active (2026-11-06).
- `mega-refactor-038b-level-reset`: proposed. No folder or `ITEMS.md` row yet.

## Sources

- `NOTES.md` in this folder.
- `items/036-checkpoints-as-data/NOTES.md`: the full spawn and checkpoint review (Q1, Q3 and candidate 3).
