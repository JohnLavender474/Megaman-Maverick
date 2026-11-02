# Notes: 117 Crush with a falling pushable block

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-11-02, branch `fix/quicksand-two-players`

From a code read. Not run.

- `PushableBlock` has no `DAMAGER` or `DEATH` fixture and no `CrusherComponent`, so it hurts nothing today.
- Its collision comes from a static `InnerBlock`. Its own `DYNAMIC` body does not collide with an enemy, so an enemy cannot stop or hold up a falling block.
- `StandardCollisionHandler` moves a dynamic body out along the smaller overlap. An enemy under a falling block is probably pushed into the ground, and it can jitter or pop out sideways. This was not seen in play.
- `GravityBlock` and `CameraEdgeBlock` use `CrusherComponent`.
