# 053: Clean up event-driven logic

- Status: idea
- Type: refactor
- Depends on: none
- Related: `012`, `013`, `019`, `029`, `038`, `057`
- Branches: none yet

## Goal

Remove fragile event ordering in `MegaLevelScreen`, and replace events that are better as flags or tables. This is an umbrella item: `012`, `013`, `019` and `038` cover the worst cases.

## Owner's input

From the first request (summary): some events must follow others in a fixed order; some events would be better as flags in a table.

## Open questions

- Which events are left after `012`, `013`, `019` and `038`? Split this item into concrete items, or drop it. First split (2026-10-23): `057` (typed event channels).

## Sources

- `NOTES.md` in this folder.
