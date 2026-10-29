# 076: A new tileset for the final stage

- Status: idea
- Type: feature
- Depends on: none
- Related: `067`
- Branches: none yet

## Goal

The final level of the game uses a generic brick-like tileset. It does not look like a final stage. Give the final
stage a tileset that fits it.

This item changes how the stage looks. It must not change the block geometry or the gameplay unless the scope says
so.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Choose or make a tileset for the final stage, and paint the stage tiles with it.
- Keep the block objects as they are, so that the stage plays the same.

## Owner's input

2026-10-29 (verbatim):

> The final level of the game uses a generic brick-like tileset that is lackluster for the final stage.

## Open questions

- Is "the final level" `WILY_STAGE_3` (it holds `WilyFinalBoss`)? The owner confirms.
- Where does the new tileset come from: new art, or an existing tileset in `assets/tiled_maps/tsx/`?
- Does this wait for `067` (PPM 16 with 1x pixel art)? New art made before `067` can need a second change.

## Sources

- `NOTES.md` in this folder.
