# TODO: mega-refactor-134b-interpolation-audit

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- D3 (owner, 2026-11-09): one new item for the 2 cases in `SCOPE.md` tables B and C. (1) Blocks that a player rides
  and that move outside the world steps or by the catch-up velocity `(target - position) / fixedStep` with a target
  that changes once per frame (`GutsTank` block, `GravityBlock`, `SphinxBall`, `SteamRoller`, `PushableBlock`,
  `SigmaRatClaw`, `BigAssMaverickRobotHand` in `ROTATE`, `RotationAnchor`, `SwingingPlatform`): the rider's carry
  happens in the first step of a frame only, and a parent snap in `preProcess` can give no carry at all. Gameplay.
  (2) Bodies that move once per frame outside the steps (for example `D1000`): drawn at the frame time, up to
  `alpha` steps ahead of the camera. A fix needs a second offset rule (a full step back) and a way to tell such a
  body from a child of a stepped parent. Also the `BigAssMaverickRobotHand` orbs (`i/n` of the hand move).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
