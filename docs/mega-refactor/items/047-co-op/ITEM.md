# 047: Co-op with multiple controllers

- Status: planned
- Type: feature
- Depends on: `029`, `031`
- Related: `008`, `030`, `051`
- Branches: none yet

## Goal

Support more than one controller and a co-op mode.

## Owner's input

From the first request (summary): all controller and behavior logic assumes one player. Add support for more than one controller and a co-op mode.

## Decisions

- H (2026-10-22): lives are a shared pool or per player, by game mode. Use an interface (for example `LivesPolicy` with `SharedPool` and `PerPlayer`) so the rule is easy to swap. There will be a maximum number of players.
- H (2026-10-22): split co-op into sequential items, easiest first: (1) shared screen (M–L: camera rule, off-screen rule, room transitions and respawn with 2 players); (2) split screen (L–XL: two cameras and viewports; spawn, cull and checkpoint rules must handle 2 cameras); (3) online (XL: lockstep on top of `010`, `011` and `031`, after `055`).

## Open questions

- Maximum number of players (2 or 4)? (Decision H.)
- Camera: follow one player, or a group bounds?

## Sources

- `NOTES.md` in this folder.
