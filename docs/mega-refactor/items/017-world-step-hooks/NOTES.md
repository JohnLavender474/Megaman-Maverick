# Notes: 017 Shared entity logic as systems and world-step hooks

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 16. Move shared per-entity lambdas into systems".

````text
(1) Current state
- The body has `preProcess` and `postProcess` maps of lambdas (`Body.kt:31-32`, run at `:101` and `:120`).
- `Body.process(dt)` (`:103-118`) applies friction (`exp(-f*dt)`), gravity (`velocity += gravity*dt` when `gravityOn`), the velocity clamp, and the translation.
- `WorldSystem.cycle` runs, for each sub-step: preProcess (all bodies), process, container rebuild, contacts, collision resolution, postProcess.
- The per-entity lambdas choose the gravity value. Typical case: `Met.kt:270-280` sets the velocity clamp and gravity from `isSensing(FEET_ON_GROUND)`.
- About 125 lines use the "feet on ground → gravity A, else B" pattern. `preProcess.put` appears 224 times in 201 files. `BodyComponentCreator.kt:47` adds a `PRIOR` lambda to every body.
- The body senses come from the contact listener in the previous sub-step. So gravity selection must stay in each sub-step. The owner is right.
- Culling: 112 uses of `getGameCameraCullingLogic` and similar. These are per frame, not physics.
- Damage blink: 169 references. Per frame, render side.

(2) Options
- (a) Culling and damage blink: move them into systems that read data components (for example `CullRule{camera, timeToCull, events}` and `DamageBlink{duration, rate}`). They run per frame and are outside the fixed step, so this is safe now.
- (b) Gravity and clamp: add world-step hooks. `WorldSystem` gets ordered `IWorldStepHook`s registered at engine construction, with `preStep(body, dt)` and `postStep(body, dt)`. Call them inside the existing per-body loop: `bodies.forEach { hooks.pre(it, dt); it.preProcess() }`. Then the order between bodies stays exactly as today. A `GravityRule` component (ground and air values, direction-aware, scalar) is read by a `GravityHook`. `dt` is passed in, so the six `getPerformance().fixedStep` sites (item 9) go away.
- (c) Move gravity into `Body.process` directly with a rule object. Simpler, but it puts game policy into the engine body.

(3) Recommendation
- Do (a) early.
- Do (b) for gravity and the velocity clamp. Keep the per-entity `preProcess` as an escape hatch for unusual bodies.
- Rule: anything that reads body senses or writes velocity goes in a step hook, never in a per-frame system.
- Keep `PRIOR` as a built-in hook.

(4) Open questions for the owner
- Do some entities change gravity rules in the middle of a state (for example bosses)? Those entities stay on custom lambdas at first.
- Must water, gravity-change zones and `GravityChange` entities drive the `GravityRule` through the hook (one place)?

(5) Proposed backlog wording

Title: "Shared entity logic as systems and world-step hooks"
- Move culling and damage blink to systems that read data components.
- Add ordered world-step hooks to `WorldSystem`, run inside each physics sub-step with an explicit `dt`.
- Replace the "feet on ground → gravity" lambdas with a `GravityRule` component and a hook.
- Keep `body.preProcess` for special cases. Remove the reads of `getPerformance().fixedStep`.

(6) Dependencies
- Item 9 (step and `dt`).
- Item 17 (more small components, which makes membership cost more visible).
- The CullablesSystem bug (non-local return).
- Item 14 (state machines).
````
