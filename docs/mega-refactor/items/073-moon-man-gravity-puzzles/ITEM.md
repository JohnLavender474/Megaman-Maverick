# 073: More gravity-block puzzles in Moon Man's stage

- Status: idea
- Type: feature
- Depends on: none
- Related: `074`, `078`
- Branches: none yet

## Goal

Moon Man's stage (the owner calls it "Gravity Man's stage") has sections with `GravityBlock`s. One larger section
has a puzzle: Megaman uses the gravity switcher to move a block out of his way. The puzzle is simple. Add new areas
that expand on the puzzle idea, so that the gravity blocks and the gravity switcher get more use.

This item changes gameplay on purpose. It is level content, not a refactor, so its branch contract is not "plays
the same as `master`".

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Design new puzzle areas with `GravityBlock` and `GravitySwitcharoo` in `MoonMan_16x14_v5.tmx` (use the
  `level-designer` and `level-builder` skills).
- Each new puzzle adds one new idea on top of the existing puzzle, for example more than one block, a block that must
  land on a switch or a hazard, or a timed gravity change.

## Owner's input

2026-10-29 (verbatim):

> Gravity Man's stage has sections with GravityBlock instances in them. One larger section has a "puzzle" which has
> Mega Man use the gravity switcher in order to move a particular block out of his way. However, this "puzzle" is
> simplistic, and it seems like there should be new areas in the level that expand on the puzzle aspect.

## Open questions

- Where in the stage do the new areas go: before the existing puzzle, after it, or both?
- Is a new puzzle on the main path, or on an alt path (`078`)?
- Do the puzzles need new block types or new switch behavior, or only new layouts?

## Sources

- `NOTES.md` in this folder.
