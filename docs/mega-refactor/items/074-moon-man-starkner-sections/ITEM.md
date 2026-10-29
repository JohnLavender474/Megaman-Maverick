# 074: More Starkner sections in Moon Man's stage

- Status: idea
- Type: feature
- Depends on: none
- Related: `073`, `079`
- Branches: none yet

## Goal

Moon Man's stage (the owner calls it "Gravity Man's stage") introduces the `Starkner` enemy late in the stage, so
the enemy gets little use. Add new sections that give more `Starkner` encounters.

This item changes gameplay on purpose. It is level content, not a refactor.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Add new sections with `Starkner` to `MoonMan_16x14_v5.tmx` (use the `level-designer` and `level-builder`
  skills).
- Introduce `Starkner` earlier, in a safe room, and then use him in harder layouts, for example together with the
  gravity changes of the stage.

## Owner's input

2026-10-29 (verbatim):

> In the same Gravity Man stage, there is the Starkner enemy which is introduced late in the stage, but he doesn't
> get much use or time to shine. New sections should be created so that more Starkner interactions occur.

## Open questions

- Do the new sections come before or after the smarter `Starkner` behavior of `079`? A section designed for the
  current behavior can play differently after `079`.

## Sources

- `NOTES.md` in this folder.
- Item `079` (smarter `Starkner` behavior).
