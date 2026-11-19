# 174: Move the D-1000 frames out of `Bosses3` so that it packs with `Basic`

- Status: idea
- Type: tooling
- Objectives: `O6`
- Depends on: none
- Related: `040` (atlas checks and one-command repack), `081f` (D-1000 art), `077c` (found it)
- Branches: none yet

## Goal

`Bosses3` cannot be repacked with the `Basic` packer today, so a frame change in it is written into the sheet in place
by a script. Move the D-1000 frames to a new atlas, so that both atlases pack from their frames folders again.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A new frames folder, `.tps` file and `TextureAsset` entry for the D-1000 frames (per `docs/ways-of-working/assets.md`);
  the D-1000 classes read the new atlas.
- Repack `Bosses3` with `Basic`. This drops the old ElecDevil regions that `077c` left in the sheet (unused since
  `077c` moved ElecDevil to `Bosses4`).
- Script check: every region keeps its name and pixels.

## Owner's input

## Open questions

## Sources

- `NOTES.md` in this folder.
