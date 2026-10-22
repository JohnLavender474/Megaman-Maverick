# 042: Split god classes and extract reusable motion and attack components

- Status: planned
- Type: refactor
- Depends on: none
- Related: `006`, `011`, `015`, `026`, `029`, `030`, `039`
- Branches: none yet

## Goal

Split large classes (`WilyFinalBoss` is 3376 lines) and extract logic that repeats in many entities, for example the tentacle and limb logic.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Move the WilyFinalBoss phase handlers into separate files (mechanical).
- `LimbMotion` (launch, hold, return), used by GutsTankFist, BigAssMaverickRobotHand, SigmaRatClaw and WilyCapsuleTentacle.
- `LinkChain` along a path (straight, pendulum, rotating line, wavy joints), used by SpikeBall, Saw, SwingingPlatform, InfernoChainPlatform, FireballBar and the tentacle.
- `DriftMotion` (an `IMotion`) and an `addPendulum` helper. Remove the tentacle test copies.
- Later: `BossPhases` / `HealthCurve` and `ShotPattern`.

## Owner's input

Answer to review item 41 (2026-10-22, verbatim):

> yes, splitting things up would be great, and it would be even better if certain things can be modularized or made into reusable components. we can dig deeper to see if there are any cases where this is appropriate. for example, the wily final boss (to use the example you surfaced) has some logic around its tentacles - maybe some of this can be extracted so that other enemies/objects can also leverage the same logic without repeat?

## Open questions

- Are `TestTentacle` and `TestWavyTentacleController` used in any TMX, or are they experiments?
- Engine-level motions (`engine/.../motion`) or game-level helpers in `core`?

## Sources

- `NOTES.md` in this folder.
