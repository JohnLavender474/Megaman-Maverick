# 147: Render interpolation for riders and for bodies that move once per frame

- Status: idea
- Type: refactor
- Objectives: `O9`, `O5`
- Depends on: none
- Related: `134` (render interpolation), `010` (fixed tick)
- Branches: none yet

## Goal

Two cases that `134` left, so that every moving thing on screen moves evenly with the camera:

1. Blocks that a player rides and that move outside the world steps, or by the catch-up velocity
   `(target - position) / fixedStep` with a target that changes once per frame. The rider is carried in the first step
   of a frame only, and a parent that snaps its block in its body `preProcess` can give the rider no carry at all.
   This is gameplay, not only the draw.
2. Bodies that code moves once per frame outside the world steps (for example `D1000`). Their step move is 0, so they
   are drawn at the frame time, up to 1 step ahead of the camera and the stepped bodies; the gap changes each frame.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Riders: move the blocks inside the steps (place them in a body `preProcess` hook on a parent that moves in the
  steps, or compute the catch-up target per step), so that the carry is even. Cases from the `134b` audit:
  `GutsTank` block, `GravityBlock`, `SphinxBall`, `SteamRoller`, `PushableBlock`, `SigmaRatClaw`,
  `BigAssMaverickRobotHand` in `ROTATE`, `RotationAnchor`, `SwingingPlatform`, `DynamicBlock`.
- Bodies that move once per frame: a second draw offset rule (a full step back, not `1 - alpha`), and a way to tell
  such a body from a child of a stepped parent; or move these bodies into the steps. Also the
  `BigAssMaverickRobotHand` orbs (`i/n` of the hand move).
- Play checks and the `--diagnostics` measure (`O5-R2`).

## Owner's input

2026-11-09, `134b` brief, D3: these cases become a new item (the recommended option).

## Open questions

- Riders: which blocks really carry a player in a live level, and which carries are visible.
- Bodies that move once per frame: a draw rule, or a move into the steps, per case.

## Sources

- `NOTES.md` in this folder.
- The `134b` `docs/SCOPE.md` (tables B and C) and the audit files of that branch (in `temp/`, not committed).
