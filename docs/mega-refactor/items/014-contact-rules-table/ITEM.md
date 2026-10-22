# 014: Table-driven contact and collision rules

- Status: planned
- Type: refactor
- Depends on: none
- Related: `006`, `020`, `029`, `030`, `031`, `055`
- Branches: none yet

## Goal

Replace the 1609-line `if/else` chain in `MegaContactListener` with a rule table that is easy to read and configure.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- An unordered fixture-pair table with begin, continue and end callbacks, ordered wildcard pre-rules and a build-time duplicate check.
- Replace the 16 `Megaman` references with fixture receivers or capabilities.
- `MegaCollisionHandler` reads body flags only (no `game.megaman`, no raw input).
- A contact trace and diff tool. Prove equality on a replay.
- Do `014` and `020` in separate branches, because `020` changes the contact order.

## Owner's input

Answer to review item 13 (2026-10-22, verbatim):

> Yes, table-driven contact & collision logic would go a long way in cleaning up the code and also make configuration much easier.

## Open questions

- Rules in data (JSON) now, or only the simple friction and sense rules?
- Can some special cases (wall-slide ice friction) become a general body property?

## Sources

- `NOTES.md` in this folder.
