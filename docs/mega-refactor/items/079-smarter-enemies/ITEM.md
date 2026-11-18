# 079: Smarter behavior for simple enemies

- Status: active
- Type: feature
- Objectives: `O4`
- Depends on: none
- Related: `015`, `074`, `117`
- Branches: `mega-refactor-079a-smarter-met` (the `Met`)

## Goal

Some enemies have very simple behavior. Make them smarter, so that they give more interesting gameplay.

Examples from the owner:

- `Starkner`: he only runs toward Megaman, with a delay between each charge.
- `Met`: he can be smarter, for example he does not run into a bottomless pit.

This item changes gameplay on purpose. It is not a refactor.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A list of the enemies with too simple behavior, starting with `Starkner` and `Met`. The owner chooses which ones
  to change.
- A new behavior for each chosen enemy.
- For `Met`: a ledge check (for example a sensor in front of his feet) so that he stops or turns at the edge of a
  bottomless pit. Other walking enemies can use the same check if two or more need it.

## Owner's input

2026-10-29 (verbatim):

> Certain enemies are overly simplistic in behavior. Starkner is an example: all he does is run toward Mega Man with a
> delay between each charge. Met is another example: he could be a bit smarter by avoiding running into bottomless
> pits.

## Decisions

- 2026-11-18: merge target is `master` directly. Each enemy is its own branch; the first is
  `mega-refactor-079a-smarter-met`.
- 2026-11-18: the `Met` avoids a `DEATH` area and a fall with no ground; it may still drop onto safe ground. It jumps
  a gap toward the player, a step it can clear, or to a higher platform where the player is. A hard-mode `Met` seeks
  the player more and dodges some shots.
- 2026-11-18: a charged hit on a `Met` with its helmet does no damage. It stops the `Met` and pops the helmet up (more
  for a full charge); the 3rd charged hit breaks the helmet (`FallingPiece` debris) and starts a desperate phase.
- 2026-11-18: the ground check is one shared world-probe helper, not fixture sensors.

## Notes for active branches

- `079a` (2026-11-18, after the `078a` merge): `078a` repacked `Enemies1` (new `*/wily/*` regions, `GreenUziJoe`
  renamed `UziJoe`) and changed `entity-tags.txt` and `README.md`. Take `master` and repack `Enemies1` before the merge.

## Open questions

- Which other enemies are on the list?
- Does a `Met` stop at every ledge, or only at a bottomless pit (a `Death` object below)?
- A change to `Met` affects every level that has a `Met`. Do some levels depend on a `Met` that falls?
- Do the new `Starkner` sections of `074` come after this item?

## Sources

- `NOTES.md` in this folder.
