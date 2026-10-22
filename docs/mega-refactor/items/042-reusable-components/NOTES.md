# Notes: 042 Split god classes and extract reusable motion and attack components

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Players, input and entity design (2026-10-22)

Copied as written from the review section "ITEM 41: REUSABLE COMPONENTS (WilyFinalBoss tentacles and similar logic)".

````text
1. Current state
- The tentacle logic is partly extracted already. `entities/special/WavyTentacleOfJoints.kt` (171 lines) is a reusable joint chain: anchor, target, 4 states, sine wave with an envelope (`:127-167`). `entities/hazards/WilyCapsuleTentacle.kt` (553 lines) wraps it with drift, wander, joint-stutter, lunge/pin and chomp.
- Copies remain:
  - `entities/special/TestTentacle.kt` (451 lines) is a self-contained copy, with its own `TentacleState` (`:200`) and its own `updateJoints` (`:385`).
  - `entities/special/TestWavyTentacleController.kt` (280 lines) repeats drift, lunge and stutter.
- `WilyFinalBoss.kt` is 3376 lines with `Phase1Handler` `:928`, `Phase2Handler` `:2076`, `Phase3Handler` `:2763`, `PhaseTransitionHandler` `:3247`, and phase state machines cast at use (`:406, :547`).

2. Candidates (where, API, users)

(1) Lunge-and-return limb controller. Strongest candidate; the same logic exists 6 times:
- `GutsTankFist.kt:178-239` (ATTACHED / LAUNCHED / RETURNING, delay, `epsilonEquals` arrival)
- `BigAssMaverickRobotHand.kt:312-339` (LAUNCH / RETURN with delays)
- `SigmaRatClaw.kt:231-269` (launch, pause, return)
- `WilyCapsuleTentacle.kt:409-520` (LUNGING / PAUSING / RETURNING)
- `TestTentacle.kt:344-380`
- `TestWavyTentacleController.kt:225-260`
API: `LimbMotion(home: () -> Vector2, launchSpeed, returnSpeed, windup, holdAtTarget, arriveEps)` with `launch(target)`, `update(dt) -> Vector2 or velocity`, `state`, and `onArrive` / `onReturned` callbacks. It is built on the item 14 `Fsm`.
Pitfall: today arrival uses velocity plus `epsilonEquals`, with no clamp of the last step. Default to the same behavior, or timing changes.

(2) Segmented chain / link placement along a path:
- `SpikeBall.kt:81-99` (chain sprites along a pendulum)
- `Saw.kt:131-136` (rings)
- `SwingingPlatform.kt:225-235` (rings)
- `InfernoChainPlatform.kt:166-175` (chain sprites)
- `FireballBar.kt:68-100` (child fireballs along a `RotatingLine`)
- `WilyCapsuleTentacle.kt:345-366` (joint entities and a tip entity)
API: `LinkChain(path: ILinkPath, count or spacing, makeLink: (i) -> GameSprite or entity)`. `ILinkPath` implementations: straight (anchor to tip), `Pendulum.getPointFromAnchor`, `RotatingLine.getScaledPosition`, `WavyTentacleOfJoints` joints. It updates the link positions each frame. An optional stutter interval gives the retro joint update (`WilyCapsuleTentacle` `JOINT_UPDATE_INTERVAL`).

(3) Idle drift motion (sine on two axes plus random wander):
- `WilyCapsuleTentacle.kt:48-56, 380-396`
- `TestTentacle` constants `:176-195`
- `TestWavyTentacleController.kt:48-55`
API: `DriftMotion(radius, speedX, speedY, wanderRadius?, retargetRange?)` as an `IMotion` in `E/motion/`, next to `SineWave`, `Pendulum`, `RotatingLine`. Wander must use the seeded random source (item 10).

(4) Pendulum rig setup:
- `SwingingPlatform.kt:143-175`, `SwingingAxe.kt:125-150`, `Saw.kt:123-160`, `SpikeBall.kt` (pendulum)
- All four repeat "create `Pendulum`, `putMotionDefinition`, debug line plus 2 circles".
API: `addPendulum(length, gravity, anchor, onValue)`, which returns the pendulum and adds the debug shapes. This is small but removes copy-paste.

(5) Boss phase controller and health curves:
- Wily phases (`:147, :183, :340-423`)
- Health thresholds in `MechaDragon.kt:737-738`
- Health-scaled values `interpolate(min, max, getHealthRatio())`: 28 calls in 11 files (`ReactorMan.kt:329, 395, 592`, `GutsTank.kt:370, 454, 527, 542`, `Bospider.kt:322, 460`, `SigmaRat.kt:256`, `ElecDevil.kt:406`)
API: `BossPhases(phases with enter thresholds, per-phase Fsm, transition state)` using item 14 parent states, plus `HealthCurve(min, max, invert = false).at(entity)`.

(6) Projectile pattern emitter (aim, clamp, spread):
- `MechaDragon.kt:759-775` (aim angle, clamp, ±spread)
- `DesertMan.kt:746`, `SphinxMiniBoss.kt:164`, `MoonMan.kt:662`, `InfernoMan.kt:686`, `DemonMet.kt:282-283`, `Cactus.kt:379`, `SpikeBot.kt:375`
API: `ShotPattern.aimed(from, target, speed, minDeg, maxDeg).spread(n, stepDeg)`, which returns trajectories. The spawn stays in the entity. It reads faction and target from items 28/29.

3. Recommendation
- First, move the Wily inner handlers to their own files. This is a mechanical change.
- Then extract (1), (2) and (3), because the tentacle work proves them. Delete `TestTentacle` and `TestWavyTentacleController`, or make them use the components (this overlaps with item 38's "not used yet" review).
- (5) and (6) come after item 14.
- (4) is a quick win.

4. Open questions for the owner
- Are `TestTentacle` and `TestWavyTentacleController` used in any TMX, or are they experiments only?
- Do you want these as engine-level motions (`E/motion`) or as game-level helpers in `core`?

5. Backlog wording
"Split god classes and extract reusable motion and attack components"
- Move the WilyFinalBoss phase handlers into separate files.
- `LimbMotion` (launch, hold, return), used by GutsTankFist, BigAssMaverickRobotHand, SigmaRatClaw and WilyCapsuleTentacle.
- `LinkChain` along a path (straight, pendulum, rotating line, wavy joints), used by SpikeBall, Saw, SwingingPlatform, InfernoChainPlatform, FireballBar and the tentacle.
- `DriftMotion` (an `IMotion`) and an `addPendulum` helper. Remove the tentacle test copies.
- Later: `BossPhases` / `HealthCurve` and `ShotPattern`.

6. Dependencies
Item 14 (`Fsm` under `LimbMotion` and `BossPhases`), 10 (seeded wander), 28/29 (targets and faction), 38 (dead or test classes), 25/45 (link sprites and draw layers), 5 (replay proof that boss timing does not change).
````
