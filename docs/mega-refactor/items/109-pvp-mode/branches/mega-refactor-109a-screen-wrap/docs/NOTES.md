# Notes: mega-refactor-109a-screen-wrap

## Decisions

- 2026-11-19, D1 (owner): a room with `wrap=true` must be exactly one view wide; the level load throws otherwise.
- 2026-11-19, D2 (owner): the rules that read the real fixture shape stay as they are, with a `TODO` at each.
- 2026-11-19 (Claude): the wrap happens after `postProcess` and `endStep` (changed in step 4, see the process log), so a `postProcess` reader of
  `getPositionDelta()` never sees ±W.
- 2026-11-19 (Claude): the far half is a field on `GameSprite` drawn by `GameSprite.draw`, not a change to the
  draw loops in `MegaLevelScreen`: one entry point, and the priority order stays.
- 2026-11-19 (owner): move the draw offsets out of `WorldSystem` into a draw-side `BodyInterpolation`, as the first code step of this branch (`PLAN.md` step 2b).
- 2026-11-19 (Claude): no transition is guarded at run time in 3 places, not by a map check, so any map stays safe.

## Overlap with other branches

2026-11-19: `060k` and `078d` are art only. No overlap.
