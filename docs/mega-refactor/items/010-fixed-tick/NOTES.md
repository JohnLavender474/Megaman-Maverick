# Notes: 010 One simulation clock: fixed tick for gameplay, separate present phase

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 9. One fixed simulation step; separate simulate and present phases".

````text
(1) Current state

What one frame does:
- `MegamanMaverickGame.render()` (`MegamanMaverickGame.kt:406`) reads `delta = Gdx.graphics.deltaTime` (`:414`). The value is not clamped. After a hitch (for example a window drag), every timer gets the full large delta.
- Then it drains `runQueue` (`:419-422`). Note: it uses `removeLast()`, so queued runnables run in the reverse order of submission (LIFO). This is probably not intended.
- Then auto-perf (`:452-482`), `controllerPoller.run()` (input is sampled once per frame), `screen.render(delta)` (`:497`), `screen.draw(batch)`, `eventsMan.run()` (`:530`; all events are delivered once per frame, here), audio, updatables, and `GameObjectPools.performNextReclaim()` (`:586`).
- `MegaLevelScreen.render()` (`MegaLevelScreen.kt:876`) runs: stats handler, spawn pass 1, the event handlers, `engine.update(delta)` (`:919`), spawn pass 2, then backgrounds, camera manager and camera (`:932-936`).
- `GameEngine.update` (`GameEngine.kt:106`) adds the queued spawns, removes the queued kills, then runs the 13 systems once each with the same variable `delta`.

Only physics uses a fixed step:
- `WorldSystem.process` (`WorldSystem.kt:100-148`) adds `delta` to an accumulator. While `accumulator >= fixedStep`, it runs `cycle(bodies, fixedStep)` (`:164-206`: preProcess, process, which integrates gravity and velocity, `collectContacts`, `processContacts`, `resolveCollisions`, postProcess).
- It stops after `maxIterations` sub-steps and drops the remaining time (`:130-133`).
- The world container is rebuilt in each sub-step and one more time after the loop.

The step depends on the performance tier:
- `Performance` (`MegamanMaverickGame.kt:141-146`): VERY_LOW 30 fps / 1/75 s, LOW 45 / 1/112.5, MEDIUM 60 / 1/150, HIGH 90 / 1/225.
- In every tier, `fixedStep * fps = 0.4`. So at the target FPS, physics runs 2.5 sub-steps per frame on average. Frames alternate between 2 and 3 sub-steps. There is no interpolation, so the motion on screen per frame alternates between about 0.8x and 1.2x of the average. This can cause a small visible judder.
- `maxIterations = 2*ceil(1/0.4) = 6` (`:830-832`).
- Auto-perf `setPerformance` (`:1004-1012`) changes the FPS cap and `WorldSystem.fixedStep` at runtime, and sends `CHANGE_PERFORMANCE_MODE`.

Everything else uses the variable frame delta: behaviors, timers (557 hand-run timers), animations, motion, cameras, spawners and event handlers.

Code that depends on frame rate or tier (verified):
- FPS guards change what spawns: `DrillHead.kt:159`, `Laser.kt:285`.
- `FallingLeaf.kt:165` sets its cap from the tier FPS.
- `Bat.kt:210` and `FloatingCan.kt:178` set the pathfinding interval from the tier.
- These sites read `Gdx.graphics.deltaTime` directly and bypass the delta that the loop passes: `BikerKibbo.kt:141`, `GameEntityUtils.kt:169`, `SubmarineJoe.kt:277`, and the camera lerp at `MegaLevelScreen.kt:264`.
- These sites use `game.getPerformance().fixedStep` to convert between per-step and per-second values: `SigmaRatClaw.kt:311`, `BigAssMaverickRobotHand.kt:381`, `RotationAnchor.kt:124`, `SpiderWeb.kt:168`, `DragonFly.kt:162`, `AngryFlameBall.kt:100`.
- `MegaGameEntity.kt:54` stores the wall-clock spawn time. `MegamanWeaponsHandler.kt:341` sorts by it.

X6 for comparison (`mmx-demake-tools/docs/engine.md`, "Frame rate" lines 14-20 and 182-188):
- X6 makes one update per VSync and uses no delta time. Timers count frames.
- When a frame takes too long, the game slows down. It does not skip time.

What "fixed simulation tick" means:
- A real-time accumulator collects the frame time.
- While the accumulator holds at least one tick (for example 1/60 s), the game runs one full simulation tick and subtracts the tick. A full tick is: latch input, deliver events, spawn phase, all gameplay systems, physics, camera.
- Then the game renders once. Interpolation between the last two states is optional; for pixel art, you can leave it out.
- In this model, timers advance by the tick (or count ticks). Animations can run on ticks (gameplay) or on render time (cosmetic only). "Just pressed" must be true for exactly one tick, even when one frame runs 0 or 2 ticks. Spawns become "this tick", not "this frame".

(2) Options

(a) Keep the loop as it is. In replay mode, pin the frame delta and the physics step.
- Pros: small change.
- Cons: replays then do not show how real players run. Tier-dependent physics stays. Recorded real sessions cannot be reproduced unless the real deltas are recorded and fed back.

(b) One physics step for all tiers, for example 1/120 s or 1/150 s. Tiers then only change the FPS cap and cosmetic budgets.
- Pros: jump arcs and collision results no longer depend on the tier. The `getPerformance().fixedStep` sites become constants. The change is small.
- Cons: the number of sub-steps per frame still varies. Behaviors still read body senses once per frame while physics steps 2 or 3 times. On a slow PC, more sub-steps are needed per frame.

(c) A full fixed tick for all gameplay systems (60 Hz, like X6). Physics runs 1 or 2 sub-steps inside each tick. Rendering is separate.
- Pros: full determinism, given a seed and inputs. Timers, FPS guards and spawns become frame-exact. Replay and headless tests (item 5) become simple. This also prepares the engine for other platforms (item 31).
- Cons: the biggest change. It touches every `Gdx.graphics.deltaTime` read and the interface of every system.
- Risks to game feel: input latency changes by up to one tick. Animation speed can change if animations move to ticks. Physics tuning (gravity, jump height) changes slightly if the step changes, because semi-implicit Euler results depend on the step size. On a 144 Hz monitor without interpolation, you see repeated frames.

(3) Recommendation

Do (b) first, then (c), in steps:
- Step 1: route all time through one clock service. Remove the direct `Gdx.graphics.deltaTime` reads and pass `dt` to physics hooks (see item 16). Clamp the frame delta.
- Step 2: use one physics step for all tiers. Tiers keep only the FPS cap and cosmetic limits.
- Step 3: add an outer fixed tick of 60 Hz for the simulation phase. Keep a variable-delta "present" phase for backgrounds and cosmetic animation.

Do not ship (a) alone. Use it only as a test mode.

What the replay recorder must record about timing. Record it per frame, so that a replay can either re-run the same steps or stop at the first divergence:
- Frame index, raw `Gdx.graphics.deltaTime`, and the delta that the simulation actually used (after the clamp).
- Physics: the sub-step count (`WorldSystem.iteration`), `fixedStep`, `fixedStepScalar`, the accumulator remainder, and a flag when the `maxIterations` cap dropped time.
- The performance tier and every tier change. Record the FPS value too (`Gdx.graphics.framesPerSecond`), because the FPS guards read it. In replay, the recorded value must be fed back to the guards.
- The RNG seed for each stream at level start, and the RNG call count for each stream for each frame. The call count is a cheap way to detect divergence (item 10).
- The input frame: the button state after `controllerPoller.run()`, plus the paused flag.
- Cheap state hashes for divergence detection: the active system/freeze mask (item 11), the number of spawns queued and added, the player position and health.
- Async pathfinding results, or force sync pathfinding in replay mode.

With (c), most of these values become constants for each tick, and the record gets much smaller. That is one more reason to go to (c).

(4) Open questions for the owner
- Target tick: 60 Hz like X6, or another value?
- Is it acceptable that physics tuning changes slightly when the step is unified? (A re-tune pass may be needed.)
- Must replays reproduce real player sessions, or only scripted test inputs?
- On slow PCs, slow down like X6, or skip time?

(5) Proposed backlog wording

Title: "One simulation clock: fixed tick for gameplay, separate present phase"
- Route all time reads through one clock. Remove direct `Gdx.graphics.deltaTime` and `getPerformance().fixedStep` reads. Clamp the frame delta.
- Use one physics step for all performance tiers. Tiers change only the FPS cap and cosmetic budgets.
- Add a fixed gameplay tick: latch input once per tick; run events, spawns, systems and physics each tick; render once per frame.
- Remove the FPS-dependent gameplay rules (`DrillHead`, `Laser`, `FallingLeaf`, `Bat`, `FloatingCan`).
- Record the timing data for each frame (raw delta, sub-steps, step, tier, FPS, seed, input) for replay and divergence checks.

(6) Dependencies
- Item 10 (RNG): needed for determinism.
- Item 16 (step hooks): the physics `dt` must come from the hooks.
- Item 18 (spawn phase): the spawn phase goes inside the tick.
- Item 11 (freezes): a freeze stops ticks for each system group.
- Items 5 and 7 (`--seed`, `--autoPerf false`) and item 30 (input frames per tick).
- The "Record and replay" backlog item.
````
