# 165: Tiled automapping rules for the Desert Man tileset

- Status: dropped (2026-11-17): merged into `166`
- Type: tooling
- Objectives: `O9`
- Depends on: none
- Related: `076`, `166`
- Branches: none yet

> **Dropped, 2026-11-17.** The owner merged the Desert Man items into `166` (Desert Man's stage rework, an epic for
> all of Desert Man's stage). The automapping rules are part 4 of `166`. The text below is kept as history.

## Goal

`desert_man_tileset` (from `076a`) chooses some tiles by position and by neighbor: the rock and soil fill use a 2x2
block by the cell's x/y parity, the edge and corner tiles also by parity, and the rope and fence ends by the tile next
to them. A hand edit of `DesertMan_16x14_v4.tmx` in Tiled must follow these rules, or a seam or a wrong edge shows.
Tiled automapping rules can apply them while the owner paints.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Tiled automapping rules (rule maps and `rules.txt`) for the rock, the soil, the ropes and the fence of
  `desert_man_tileset`.
- Check: a rule run on the current map changes no cell.

## Owner's input

2026-11-17: the owner accepted the recommendation (decision D3 of `076a`): the remap script stays a temporary file,
and automapping is a backlog candidate.

## Open questions

- Should later tilesets of `076` (other levels) get the same kind of rules?

## Sources

- `NOTES.md` in this folder.
- The rules as code: `temp/076-desert-tiles/tools/make_desert_man_tileset.py` and its `README.md` in the main
  checkout (not in git).
