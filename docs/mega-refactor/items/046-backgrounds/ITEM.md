# 046: Backgrounds as pure functions of the camera, with visible-cell drawing

- Status: planned
- Type: refactor
- Depends on: none
- Related: `009`, `026`, `036`, `037`, `038`, `044`
- Branches: none yet

## Goal

Compute background positions from the camera every frame, as X6 does, so that respawn needs no workaround and Wily 3 draws about 4 quads instead of 10,000.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Compute the background position each frame from the game camera: `anchor + offset + parallax × cam`. No accumulated deltas.
- Draw only the visible cells, with wrap in x and/or y. Remove the `rows`/`columns` grid.
- Optional X6-style modes (half, quarter, follow-x, fixed, scripted) as parallax presets.
- Restore background visibility and offsets per checkpoint on respawn (with `037`).
- Remove the per-frame debug string in `Background.update`.

## Owner's input

Answer to review item 46 (2026-10-22, verbatim):

> yes, the 10,000 quads is a workaround because when Mega respawns I can't be sure that the background matches what it was before, so I create a ton of rows/cols to prevent the background from ever moving off screen and disappearing in a reasonable amount of time. I think there's a lot to learn from how X6 handles backgrounds that we can incorporate

## Open questions

- Was the respawn mismatch a wrong position, or a wrong visible/hidden background? The code suggests visibility.
- Do any backgrounds need non-linear motion (capped catch-up after a teleport)?
- Must a background change by checkpoint (X6 stage 7), or only by triggers?

## Sources

- `NOTES.md` in this folder.
