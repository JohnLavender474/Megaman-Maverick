# 067: PPM 16 with 1x pixel art

- Status: idea
- Type: refactor
- Depends on: none
- Related: `028`, `033`, `040`, `046`, `054`
- Branches: none yet

## Goal

`ConstVals.PPM` changes from 32 to 16, and all pixel art is scaled by 0.5 (from 2x art to 1x art) with no loss. The game
must look and play the same.

## Proposed scope

Not approved. Research first (see `NOTES.md`). Facts on `master` at `3f7329489`: `PPM` has about 3,600 references in
`core/src/main` and `engine/src/main`; `assets/` holds about 3,500 PNG files; the Tiled maps use 32-pixel tiles.

## Owner's input

2026-10-28 (verbatim): "Right now, PPM is set to 32px. All artwork is scaled 2x. I'd like to change this so that the PPM
is 16px. This will require scaling all pixel art by 0.5x. The scaling cannot be lossy."

## Open questions

## Sources

- `NOTES.md` in this folder.
