# 010: One simulation clock: fixed tick for gameplay, separate present phase

- Status: idea
- Type: refactor
- Depends on: `011`, `017`
- Related: `006`, `008`, `012`, `019`, `031`, `055`
- Branches: none yet

## Goal

Make gameplay time independent of the frame rate and the performance tier, so that runs are repeatable and replays work.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Step 1: route all time reads through one clock. Remove the direct `Gdx.graphics.deltaTime` and `getPerformance().fixedStep` reads. Clamp the frame delta.
- Step 2: one physics step for all performance tiers. Tiers change only the FPS cap and cosmetic budgets.
- Step 3: a fixed gameplay tick (for example 60 Hz): latch input once per tick; run events, spawns, systems and physics each tick; render once per frame.
- Remove the FPS-dependent gameplay rules (`DrillHead`, `Laser`, `FallingLeaf`, `Bat`, `FloatingCan`).
- Record the timing data for each frame (raw delta, sub-steps, step, tier, FPS, seed, input) for replay and divergence checks.

## Owner's input

Answer to review item 9 (2026-10-22, verbatim):

> Please expand on this. What does "fixed simulation step" mean exactly, here? Should we record physics step, FPS, and other info when recording?

## Decisions

- 2026-10-25: 60 FPS is the source of truth for how the game is meant to look and feel. The physics reference is
  the `MEDIUM` tier (60 fps, step 1/150 s). A unified step or tick must match the feel at 60 FPS; the other tiers
  may change to match it.

## Open questions

- Target tick: 60 Hz like X6, or another value? (Decision D.)
- May a weak machine run more simulation work per second than `VERY_LOW` does today (for example 5 physics
  sub-steps per frame at 30 fps with one shared step of 1/150 s)? Or must the tick rate or the step count go down
  on weak machines? See `NOTES.md`, "Trade-offs of doing 010 (2026-10-25)".
- Is a small physics retune acceptable when the step is unified?
- Must replays reproduce real player sessions, or only scripted inputs?
- On slow PCs, slow down like X6, or skip time?

## Sources

- `NOTES.md` in this folder.
