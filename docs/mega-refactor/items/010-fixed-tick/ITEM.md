# 010: One simulation clock: fixed tick for gameplay, separate present phase

- Status: idea
- Type: refactor
- Objectives: `O2`, `O6`, `O5`
- Depends on: `011`, `017`
- Related: `006`, `008`, `012`, `019`, `031`, `055`
- Branches: `010a` planned (frame clock and the simulation/present split, before `006`)

## Goal

Make gameplay time independent of the frame rate and the performance tier, so that runs are repeatable and replays work.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Step 1: route all time reads through one clock. Remove the direct `Gdx.graphics.deltaTime` and `getPerformance().fixedStep` reads. Clamp the frame delta (at most `maxIterations × step`). Auto-perf must not count a clamped hitch frame (see `docs/issues/2026-11-23-auto-perf-drops-tier-after-freeze.md`; that bug may be fixed first in a `fix/` branch).
- Step 2: one physics step for all performance tiers. Tiers change only the FPS cap and cosmetic budgets.
- Step 3: a fixed gameplay tick (for example 60 Hz): latch input once per tick; run events, spawns, systems and physics each tick; render once per frame.
- Remove the FPS-dependent gameplay rules (`DrillHead`, `Laser`, `FallingLeaf`, `Bat`, `FloatingCan`).
- Record the timing data for each frame (raw delta, sub-steps, step, tier, FPS, seed, input) for replay and divergence checks.

## Bugs folded in

- `Bat` (`Bat.kt:223-230`) and `FloatingCan` (`FloatingCan.kt:175-182`) set the re-path interval from the
  `CHANGE_PERFORMANCE_MODE` event: a live enemy goes to 0.25 s at a low tier and 0.5 s at MEDIUM/HIGH, while the
  spawn default is 0.1 s (`PathfindingComponent.kt:14`). The mapping looks inverted. The scope line on FPS rules
  removes it. Found by the gameplay-need scan (2026-11-23).
- `WorldSystem` loops on `accumulator >= fixedStep` but subtracts `fixedStep / fixedStepScalar`
  (`engine/.../world/WorldSystem.kt:188-190`). With `--fixedStepScalar` other than 1, the number of sub-steps is wrong. It is a dev
  flag only. Found by the same scan (2026-11-23).

## Owner's input

Answer to review item 9 (2026-10-22, verbatim):

> Please expand on this. What does "fixed simulation step" mean exactly, here? Should we record physics step, FPS, and other info when recording?

## Decisions

- 2026-10-25: 60 FPS is the source of truth for how the game is meant to look and feel. The physics reference is
  the `MEDIUM` tier (60 fps, step 1/150 s). A unified step or tick must match the feel at 60 FPS; the other tiers
  may change to match it.

## Objectives audit (2026-11-01)

- Decision (owner, 2026-11-01): one fixed tick in all play (`O2-R3`). Tiers and auto-performance change only the frame rate and cosmetic budgets. Measure the cost split of simulation and render on a weak machine before the tick is decided.
- This item owns the camera as part of the simulation: the camera moves on the tick, not in the render path (`MegaLevelScreen.kt:286,921`).
- Input frames are built once per render (`MegamanMaverickGame.kt:573`); they must be built once per tick.
- `timeSpawned` is wall-clock time, and it sorts weapons (`MegaGameEntity.kt:57`, `MegamanWeaponsHandler.kt:338`).
- Async paths must apply on a set tick (`AsyncPathfindingSystem.kt`).

- 2026-11-23 (owner accepted the recommendation): branch `010a` comes before `006`. It holds step 1 (one frame clock,
  clamp) and the split of the frame into a simulation call and a present call: the camera update and the viewport size
  move to the simulation side, and the producer frame clears the drawables and shapes queues. The draw side owns the renderers.
  Reason: the owner rule "no stopgaps" (`docs/ways-of-working/planning.md`); `006` uses the real seams, not a
  test fake. Cost: until `006` exists, only the existing tests and a play test check `010a`. Full plan:
  `006` scan, `temp/006-headless-scan/findings.md` ("Real-fix plan").
- 2026-11-23: gameplay need per step is in `NOTES.md` ("Gameplay need per step").

## Open questions

- Target tick: 60 Hz like X6, or another value? (Decision D.)
- May a weak machine run more simulation work per second than `VERY_LOW` does today (for example 5 physics
  sub-steps per frame at 30 fps with one shared step of 1/150 s)? Or must the tick rate or the step count go down
  on weak machines? See `NOTES.md`, "Trade-offs of doing 010 (2026-10-25)".
- Is a small physics retune acceptable when the step is unified?
- Must replays reproduce real player sessions, or only scripted inputs?
- On slow PCs, slow down like X6, or skip time?
- `ICameraView.overlaps` tests the libGDX frustum, which changes only at `viewport.apply()` in the draw (`051` decision, 2026-10-30). Gameplay uses it, for example `Beezee.kt:167`, `DragonFly.kt:215`, `CannoHoney.kt:226` and `PreciousGemCluster.kt:141`. With ticks that run with no draw, or with 2 views (`094`), the frustum is stale or belongs to the wrong view. Is a change to a rectangle test accepted here? `051` calls that a behavior change that needs its own contract check.
- `O2-R3` applies an async path on a set tick. What happens if the path is not ready on that tick? Options: wait and stop the simulation, use the old path, or use sync pathfinding in a net session. Today a wall-clock timeout decides (`AsyncPathfindingSystem.kt:66`).

## Sources

- `NOTES.md` in this folder.
