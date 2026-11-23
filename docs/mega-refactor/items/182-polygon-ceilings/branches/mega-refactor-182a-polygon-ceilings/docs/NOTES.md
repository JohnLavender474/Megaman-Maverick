# Notes: mega-refactor-182a-polygon-ceilings

## Decisions

All on 2026-11-23, owner accepted the recommendation (brief D1 to D4; the item questions Q1 to Q5 are in `ITEM.md`):

- D1: the head-contact record is a typed map on `CeilingPolygonBlock`, from each touched `HEAD` fixture to the step
  serial of its last touch. The block's `postProcess` checks each head that the handler did not touch in this step
  again, and ends the contact when the head no longer reaches the face. Not a property key on the body (`O1-R4`).
  This is how `182a` reads "a typed per-body step field clears it" (Q2 b).
- D2: `O5-R2`: the owner runs one `--diagnostics` pair (the same room on `master` and on the branch) at the play test;
  the numbers go in the process log.
- D3: the 2 level rules (enough clearance under each polygon for a grounded body; no polygon ceilings in levels where
  gravity turns) go in `docs/ways-of-working/levels.md`, in the merge commits on `master` (`TODO.md`).
- D4: a body is grounded for the x push when it has the `FEET_ON_GROUND` sense.

## Overlap with other branches

- `compactor-level`: adds `LevelDefinition` entries (list append).
- `078e`: uncommitted work in `PreciousWoman_v2.tmx` and `BreakableBlock.kt`; `182a` does not touch them.
