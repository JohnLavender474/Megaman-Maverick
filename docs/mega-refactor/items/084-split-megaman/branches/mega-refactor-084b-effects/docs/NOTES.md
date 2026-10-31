# Notes: mega-refactor-084b-effects

## Decisions

- (2026-10-30, owner) `docs/SCOPE.md` approved:
  - The trail and bubble timers survive a respawn, with no `reset()` on the 2 components.
  - The effects stay in this item, not in `042`.
  - `EXPLOSION_ORB_SPEED` moves to `ExplosionOrb` as `DEFAULT_EXPLOSION_ORB_SPEED`, and the spawn props allow an optional override (verbatim: "prefix the name with `DEFAULT_` and make sure
    that the spawn props allows for an optional override"). No caller passes another speed yet; the owner asked for this, so it is an exception to the rule on code with no caller.

## Overlap with other branches

Checked at branch start (2026-10-30, `master` at `876b90a6e`). Neither active branch changes `Megaman.kt`, `AbstractBoss.kt`, `ExplosionOrb.kt`, `UnderWaterBubble.kt`, `MegamanTrailSpriteV2.kt` or
`MegamanValues.kt`:

- `mega-refactor-031e-bindings`: no shared code file.
- `mega-refactor-051c-camera-zones`: no shared code file.

Checked again before the merge (2026-10-31, `master` at `6cf00df2c`):

- `master` has only docs changes since this branch took it, so no merge of `master` into this branch.
- `mega-refactor-031e-bindings`, `mega-refactor-051-master` and `mega-refactor-051d-auto-scroll` (with uncommitted files): no shared code file. `051c` merged into `mega-refactor-051-master`.
