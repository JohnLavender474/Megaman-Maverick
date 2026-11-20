# TODO: mega-refactor-077g-glacier-man

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- Jump-over lands short: `jumpOverTargetPlayer` computes the x speed for an air time of `2 * 15 / 22.5` s with no
  drag, but `Body.process` applies the default y friction (`frictionOnSelf.y = 1`), so he lands about 1 tile past
  the player, not 2 (rig: 287.6 px for 320 px over a 6.75-tile jump). Found in the final-pass tests (2026-11-20).
- A standing player counts as "above": `isMegamanAboveOffsetY` uses `>=` on the player's top against the boss's feet
  + 1.5 tiles; a 1.5-tile player on the same floor is exactly at the limit (no duck, `aim_up` throws, snowballs
  up), unless float rounding puts the top below. Same rule on `master`. Found in the final-pass tests (2026-11-20).
- `GlacierMan.isOwnLiveIcicle` counts any `SPAWN_QUEUED` icicle in `icicles` as own (the owner is set only in
  `onSpawn`). A pooled icicle of the boss that another owner (`IcicleTelly`) re-queues before the next prune is
  destroyed if the boss is defeated or destroyed in that frame. No live map has the setup. `TODO` at the code.
  Found in the review before the play test (2026-11-20).
