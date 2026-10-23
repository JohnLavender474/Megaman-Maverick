# 057: Typed event channels and payloads

- Status: idea
- Type: refactor
- Depends on: none
- Related: `013`, `016`, `029`, `031`, `038`, `047`, `053`
- Branches: none yet

## Goal

Split the one flat `EventType` enum and its untyped property bag into typed channels (for example player events, level events and controller events), each with typed payloads. Listeners then get only the events of the channels they need, and the compiler checks the payload.

This is the first concrete item split out of `053`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Keep `EventsManager` as the central hub. Add typed channels to it, in place of one global set of event types.
- Typed payloads in place of `Properties` lookups by `ConstKeys`.
- Player events carry the player slot (`029` adds the slot prop first).
- Work in small stages, one channel or one group of events per branch, so that each branch stays small. The old and the new paths live side by side during the migration.

## Owner's input

Item `029` research (2026-10-23, verbatim):

> Right now, the game uses a single global event listener with a single set of global event types. Should we create a new item to refactor this so that it's split up into "player events", "level events", "controller events", etc.?

> Should we consider an abstraction or central hub that handles events?

> That item can be split into multiple stages for the sake of keeping feature branches lightweight.

Motivation from `029`: co-op needs events that say which player they are about, and level-wide events (for example the level reset) that are separate from player events.

## Open questions

- Which channels, and in which order? A first guess: player, level (room transitions, boss flow, reset), controller, audio and UI.
- Keep the buffered "run after the screen render" delivery, or let some channels deliver at once?
- How does this fit with `013` (level events become state machine inputs) and `038` (one routine submits the respawn events in a set order)?

## Sources

- `NOTES.md` in this folder.
- `items/029-player-contract/NOTES.md`: "Co-op gaps and ideas" and review 3 (events).
