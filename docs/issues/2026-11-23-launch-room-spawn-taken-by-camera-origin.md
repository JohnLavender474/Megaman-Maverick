---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor-182a-polygon-ceilings (2026-11-23)
related-items: [036]
---

# `--room` can start in another room when a spawn is near the world origin

## Where

- `core/.../screens/levels/spawns/PlayerCheckpointManager.kt`: `startAt` (removes only the spawns whose names sort
  before the start spawn) and `run` / `popIfInCamera` (makes any remaining spawn that overlaps the camera current).
- `core/.../screens/levels/MegaLevelScreen.kt`: `startAtLaunchRoom`.

## What happens

`--level X --room R` sets the current spawn to the first spawn of room `R`. The spawns whose names sort after it stay
in the list. Before the camera moves to the player, its bounds are at the start position near the world origin. If a
later spawn overlaps those bounds, `run()` makes it the current spawn, and the player starts in that room.

## What should happen

The player starts at the first spawn of room `R`, whatever other spawns are near the origin.

## How to reproduce

- In a test map, put the room with the highest spawn name at the bottom of the map, with its spawn less than half a
  view height (7 tiles) above the map bottom. `RefactorTest182.tmx` at `mega-refactor-182a-polygon-ceilings` commit
  `3bd834731` has this layout: room `rect_ceiling`, spawn `5`.
- `./run-game-alpha.sh --level REFACTOR_TEST_182 --room v_jump`: the player starts in `rect_ceiling` (owner's
  screenshots, 2026-11-23). Checks 1 to 5 of branch `182a` all started there.

## Notes

- Workaround in `182a`: the map has 16 empty rows at its bottom, so no spawn is near the origin.
- Item `036` plans checkpoint ranks; its `TODO` at `startAt` is about the order of the names, not this case.
