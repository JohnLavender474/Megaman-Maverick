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

## Trade-offs of doing 010 (2026-10-25)

Based on `master` at `9cd13b5a5`. Read-only scan. Paths: `E` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`C` = `core/src/main/kotlin/com/megaman/maverick/game/`. Marked "(inference)" where the text is not read from code.

What the review above already covers, so this section does not repeat it: the meaning of a fixed tick, options
(a), (b) and (c) with their pros and cons, the risks to game feel (input latency, animation speed, semi-implicit
Euler, repeated frames at 144 Hz), the record format for replay, and the dependency list. This section adds the
cost on weak hardware, numbers per tier, the effect on other items, the size of the work, and options with a
recommendation.

### Line numbers today

Some review line numbers moved:

- `C/MegamanMaverickGame.kt`: `Performance` `:149-154`; `render()` `:452`; `delta = Gdx.graphics.deltaTime`
  `:460`; `runQueue` drain `:465-468`; auto-perf `:498-526`; `controllerPoller.run()` `:531`;
  `screen.render(delta)` `:543`; `eventsMan.run()` `:576`; `WorldSystem` setup `:880-891`; `setPerformance`
  `:1063-1071`.
- `C/screens/levels/MegaLevelScreen.kt`: `render` `:911`; `engine.update(delta)` `:959`; camera lerp `:272`.
- `E/world/WorldSystem.kt`: `process` `:100-148`; `cycle` `:164-206`.

### What the tiers and auto-perf do today

- `Performance` (`MegamanMaverickGame.kt:149-154`): VERY_LOW 30 fps, 1/75 s; LOW 45, 1/112.5; MEDIUM 60, 1/150;
  HIGH 90, 1/225. In each tier `fixedStep * fps = 0.4`, so a frame at the target FPS has 2 or 3 sub-steps (2.5 on
  average).
- The default tier is MEDIUM (`DesktopLauncher.kt:27`). Auto-perf is on by default (`DesktopLauncher.kt:51`).
- `maxIterations = 2 * ceil(fixedStepScalar / (fixedStep * fps)) = 6` (`MegamanMaverickGame.kt:889-891`). It is
  computed once at startup. Because the product is 0.4 in every tier, it is 6 in every tier.
- Auto-perf (`:498-526`) only goes down, one tier at a time. It never goes up. It acts when
  `Gdx.graphics.framesPerSecond` stays below the tier FPS for about 1.5 to 10 s (the timer speed depends on the
  deficit), with a 5 s grace period after each change.
- `setPerformance` (`:1063-1071`) sets the FPS cap, sets `WorldSystem.fixedStep` (`:1067`), and sends
  `CHANGE_PERFORMANCE_MODE`. Only `Bat` (`Bat.kt:209-213`) and `FloatingCan` (`FloatingCan.kt:174-178`) listen.
  The settings panel can also change the tier (`GameSettingsPanel.kt:299`, `:306`).
- So the physics step size can change in the middle of a level, without a player action.
- `isPerformanceSubpar()` (`:1073`) has no caller. It returns `true` for MEDIUM and HIGH, which looks inverted.
  (Side finding.)

### What uses delta today

- `GameEngine.update` (`E/GameEngine.kt:106-126`) passes the same variable frame delta to all 13 systems once per
  frame. Controller (`onPressContinued(poller, delta)`), Behaviors, Cullables, Motion, Pathfinding (interval
  timer), Updatables, Animations, Sprites and Audio all use it once per frame.
- `MegaLevelScreen.render` (`:911-985`) also passes it to the stats handler, `spawnsMan`, the four event handlers,
  backgrounds, `cameraManagerForRooms`, the game camera and the camera shaker.
- `MegamanMaverickGame.render` passes it to auto-perf, the screen controller, `audioMan` and the game
  `updatables`.
- Size in `C`: 914 `Timer(` sites, 754 `.update(delta)` calls, 364 files that name `delta`, 103 `* delta` or
  `delta *` expressions.
- Only `WorldSystem.cycle` runs per sub-step: all `preProcess` hooks, `Body.process` (gravity, friction, clamp,
  move), contacts, collisions and all `postProcess` hooks.
- The frame delta is not clamped. When a frame is slow, physics stops after 6 sub-steps and drops the rest
  (`WorldSystem.kt:130-133`), but every other system gets the full delta. So after a hitch, timers, animations
  and behaviors move ahead of physics.

### Direct time reads (sizes step 1)

`Gdx.graphics.deltaTime` outside the loop:

- `BikerKibbo.kt:141`, `GameEntityUtils.kt:169`, `SubmarineJoe.kt:277`, `MegaLevelScreen.kt:272` (camera lerp).

`game.getPerformance().fixedStep`. All 7 sites are in `body.preProcess`, so they run once per sub-step and are
correct today. They need the step `dt` from the hook (`017`), or a constant after step 2:

- `SigmaRatClaw.kt:311`, `BigAssMaverickRobotHand.kt:382`, `RotationAnchor.kt:124`, `SpiderWeb.kt:168`,
  `DragonFly.kt:162`, `AngryFlameBall.kt:100`.
- `SwingingPlatform.kt:116`. The review did not list this site.

Rules that depend on FPS or the tier:

- `DrillHead.kt:159` and `Laser.kt:286`: spawn only if `Gdx.graphics.framesPerSecond >= 50` (`FPS_GUARD`,
  `DrillHead.kt:87`, `Laser.kt:102`). At VERY_LOW (30) and LOW (45), these never spawn. They are effects (drill
  debris, laser), but the rule reads the measured FPS, so it can flip during play.
- `FallingLeaf.kt:165`: the cap is `fps / 3`.
- `Bat.kt:213`, `FloatingCan.kt:178`: the pathfinding interval depends on the tier.

Wall-clock reads that affect gameplay order: `MegaGameEntity.kt:54` (`timeSpawned`, owned by `011`) and
`AsyncPathfindingSystem.kt:46`, `:65` (the timeout). `DarknessV2` and `DarknessV4` use `currentTimeMillis` only
for timing logs.

### 1. Cost per frame on weak hardware

The owner set a goal on 2026-10-25 (`docs/mega-refactor/CLAUDE.md`, "Performance is also a goal"): the game must
play well on weak hardware. This is the most important trade-off of `010`.

Today a lower tier lowers the FPS cap **and** makes the step longer, because `fixedStep` scales with the FPS. So
the physics work per frame stays the same (2.5 sub-steps), and the work per second goes down:

| Tier | FPS | Step today | Sub-steps per frame | Physics steps per s | Container rebuilds per s | System passes per s |
|---|---|---|---|---|---|---|
| VERY_LOW | 30 | 1/75 | 2.5 | 75 | 105 | 30 |
| LOW | 45 | 1/112.5 | 2.5 | 112.5 | 157.5 | 45 |
| MEDIUM | 60 | 1/150 | 2.5 | 150 | 210 | 60 |
| HIGH | 90 | 1/225 | 2.5 | 225 | 315 | 90 |

(Rebuilds per s = steps per s + one post-loop rebuild per frame. "System passes" = how often the 13 systems and
the level-screen handlers run.)

**Step 1 (one clock, clamp the delta).** No change to the cost. A clamp changes only what happens after a hitch.

**Step 2 (one physics step for all tiers).** The tier then changes only the FPS cap. Physics per second becomes
the same in every tier, so a weaker tier does more physics per frame:

| Shared step | VERY_LOW (30) | LOW (45) | MEDIUM (60) | HIGH (90) |
|---|---|---|---|---|
| 1/150 (MEDIUM) | 5 per frame | 3.33 | 2.5 | 1.67 |
| 1/120 | 4 | 2.67 | 2 | 1.33 |
| 1/60 | 2 | 1.33 | 1 | 0.67 |

- With 1/150, VERY_LOW goes from 75 to 150 physics steps per s and from 105 to 180 rebuilds per s. The machine
  that is already too slow gets twice the physics work. Auto-perf then no longer reduces the physics cost; it
  reduces only render and the once-per-frame systems.
- With 1/150 and `maxIterations = 6`, the physics cap of a frame is 6/150 s = 40 ms. Below 25 fps, physics slows
  down (time is dropped). Today the VERY_LOW cap is 6/75 s = 80 ms, so the slow-down starts only below 12.5 fps.
  So weak machines reach the cap sooner. The cap protects against a spiral of death, but the price is physics
  that runs slower than the timers (see "What uses delta today").
- HIGH becomes cheaper (225 to 150 steps per s), but it alternates 1 and 2 sub-steps (pattern 2, 2, 1). The motion
  per frame alternates between about 1.2x and 0.6x of the average, so the judder is larger than today
  (inference).
- With 1/120, MEDIUM at a stable 60 fps gets exactly 2 sub-steps per frame, so its judder goes away (inference).

**Step 3 (fixed gameplay tick, for example 60 Hz).** All simulation runs at the tick rate, on every machine. The
FPS cap then changes only render.

| Tick and physics | Per frame at 30 fps | Per s on every tier |
|---|---|---|
| 60 Hz tick, 1 step of 1/60 | 2 ticks, 2 steps, 2 post-loop rebuilds | 60 system passes, 60 steps, 120 rebuilds |
| 60 Hz tick, 2 steps of 1/120 | 2 ticks, 4 steps, 2 post-loop rebuilds | 60 system passes, 120 steps, 180 rebuilds |

- Compared with VERY_LOW today (30 system passes and 75 steps per s), a 60 Hz tick doubles the system passes. The
  physics cost goes down (1/60) or up (1/120).
- The split of the frame cost between simulation and render was never measured (`020` NOTES, "Summary": there is
  no real `--diagnostics` capture). If render is the main cost on weak machines, step 3 is cheaper or the same. If
  the simulation is the main cost, step 3 is more expensive than VERY_LOW today (inference).
- A fixed tick needs its own cap on ticks per frame, as a guard against a spiral of death. When a machine cannot
  run 60 ticks per s, the game must either slow down (X6: one update per VSync, no skip) or drop time. This is
  open question "slow down or skip". With a slow-down, a weak machine plays the same game, but slower. With a
  skip, it plays at the right speed with larger jumps. Unlike today, all systems slow down together, so timers and
  physics stay in step.
- Tick rate (Decision D): 60 Hz with 1 physics step per tick is the cheapest and matches X6 (one update per frame,
  timers count frames). Its step (1/60) is 2.5 times the MEDIUM step and close to the VERY_LOW step (1/75), so it
  changes the feel most for MEDIUM players (see 2.). 60 Hz with 2 steps of 1/120 keeps the feel close to MEDIUM
  and keeps a fixed integer step count per tick. 60 Hz with 1/150 gives 2.5 steps per tick (2 and 3 alternate);
  that is deterministic but keeps the alternation inside each tick.
- On a 90 Hz (HIGH) or 144 Hz screen, a 60 Hz tick without interpolation repeats frames. HIGH then gives no
  smoother motion, only lower input-to-render latency (inference). Interpolation removes this, but it adds a copy
  of the previous state for every drawn body.

Levers to keep weak hardware playable under steps 2 and 3: the cosmetic budgets (`FallingLeaf`, `DrillHead`
debris, `Laser`) stay tied to the tier; `020` lowers the cost per step; the tick cap and the slow-down rule.

### 2. Physics feel and the gameplay contract

- `Body.process` (`E/world/body/Body.kt:103-118`): friction is `velocity *= exp(-friction * dt)`. This is exact
  for any step, so friction does not depend on the step. Gravity is semi-implicit Euler (`v += g*dt`, then
  `x += v*dt`). The result depends on the step.
- Example (inference, constant gravity assumed during the rise): the apex of a jump from `v0` under gravity `g`
  is `v0^2/(2g) - v0*dt/2`. For `JUMP_VEL = 25` (`MegamanValues.kt:35`) the loss is `12.5 * dt` tiles: 1/225 =
  1.8 px, 1/150 = 2.7 px, 1/120 = 3.3 px, 1/75 = 5.3 px, 1/60 = 6.7 px. So VERY_LOW and HIGH already differ by
  about 3.5 px in jump height today. A change from 1/150 to 1/60 lowers the apex by about 4 px.
- Velocity clamps are applied per step (`Body.kt:112-115`), so the clamp itself does not change.
- Tunneling: a body moves `v * dt` per step. At Mega's `CLAMP_Y = 40` tiles/s (`MegamanValues.kt:14`) that is
  8.5 px per step at 1/150, 17 px at 1/75 and 21 px at 1/60. Small fast projectiles and thin blocks or sensors
  are most at risk (inference; no list of thin fixtures exists).
- Contact BEGIN and END are found per step. A longer step finds fewer short contacts and changes the step in which
  a sense turns on. Behaviors read the senses once per frame, after the last sub-step.
- **The contract is already tier-dependent.** Each tier uses a different step today, and auto-perf can change the
  step during play. There is no single "current behavior" for physics. Today MEDIUM is the default tier, so it is
  the likely reference (inference; the owner must confirm which tier they play and test on). If MEDIUM is the
  reference, step 2 with 1/150 changes nothing for MEDIUM and makes the other tiers match it. A step of 1/120 or
  1/60 changes MEDIUM too and needs a retune check (open question "small retune").
- Step 3 also changes the once-per-frame code at MEDIUM. At a stable 60 fps it is close to today, because the tick
  equals the frame. The differences: input is latched per tick, events run per tick, spawns happen per tick, and
  the frame delta becomes a constant 1/60.

### 3. Determinism, replay, tests and order

- Today a run depends on the wall-clock delta sequence: the sub-step count per frame, the timer values and the
  FPS guards all change from run to run. Steps 1 and 2 do not fix this; they only make physics equal across
  tiers. Only step 3 (with `011` for randomness) makes a run a function of the seed and the inputs.
- `055` (record and replay) depends on `010`. Without step 3, a replay must record and feed back the raw delta,
  the sub-step count, the tier and the FPS (see the review record list). With step 3, it records the input per
  tick.
- `006` (headless boot and run): a headless run can pass a constant delta today, so it does not need step 3 to
  boot. It needs step 3 (or a pinned delta and step) to compare two runs.
- Event and spawn timing: `eventsMan.run()` runs once per frame, after `screen.render` (`MegamanMaverickGame.kt:576`).
  With 2 ticks in one frame, events must run in each tick, or the second tick sees stale state. The spawn pass in
  `MegaLevelScreen.render` (`:937-944`, and the second spawn drain at `:962-969`; owned by `019`) also moves into the tick.
- Input: `ControllerPoller.run()` (`E/controller/polling/ControllerPoller.kt:20`) samples `isPressed()` and sets
  `JUST_PRESSED` for one poll. With 2 ticks in one frame, a poll per frame makes "just pressed" true in both
  ticks. With 0 ticks in a frame (90 or 144 fps), a press can be lost. The poll must move to the tick, and a press
  that comes and goes between two ticks must be latched (`031`).
- `runQueue` (LIFO, `:465-468`) runs once per frame. Pause, resume and screen changes go through it. A decision is
  needed whether it runs per frame or per tick.

### 4. Effect on other items

- `020` (broad phase): with 1 physics step per tick and 1 tick per frame (60 Hz at 60 fps), the rebuilds go from
  about 3.5 to 2 per frame (one in the cycle, one after the loop). With 2 steps per tick it is 3. With step 2 alone
  at 1/150, the rebuilds per frame go **up** on weak tiers (6 at VERY_LOW). Options A, B and C of `020` do not
  depend on the step count, so they stay valid and become more useful if steps go up. Option D saves more when
  there are more steps per frame, and less with 1 step per tick. Option E ("wait for `010`") gains little: `010`
  is far away and may not lower the step count at all.
- `017` (world-step hooks): the hooks pass `dt` to the 7 `getPerformance().fixedStep` sites. After step 2 the step
  is a constant, so those sites can read a constant without `017`. The step-1 dependency on `017` is soft
  (inference).
- `012` (freezes): a freeze then stops ticks for a system group. A freeze timer counts ticks, not wall time.
- `019` (spawn phase): its single spawn pass per frame becomes one pass per tick. `019b` (destroy notifications)
  does not depend on `010`.
- `031` (input frames): the `InputFrame` is per tick. Step 3 defines the tick that `031` samples on.
- `011` (random): needed for the determinism of step 3, not for steps 1 and 2.

### 5. Size and risk per step

| Step | Work (from the scan) | Depends on | Gameplay risk |
|---|---|---|---|
| 1. One clock, clamp delta | A clock service; 4 direct `deltaTime` reads; 7 `fixedStep` reads (through `017` hooks or a constant); a clamp in `render()`; maybe the FPS guards read the tier instead of the measured FPS. | soft `017` | Low. Only hitches change. The FPS guards change if they move from measured FPS to the tier. |
| 2. One physics step | `Performance` loses `fixedStep`; `setPerformance` stops changing the step; `maxIterations` set for the worst tier. | step 1 | Low at MEDIUM if the step is 1/150; medium for other tiers (they start to play like MEDIUM). Cost goes up on weak tiers. |
| 3. Fixed tick | Split `MegamanMaverickGame.render` and `MegaLevelScreen.render` into simulate and present; move input, events, spawns, camera and culling into the tick; decide for each of the 13 systems and the handlers if they are simulation or present; a tick cap and a slow-down rule. The 754 `.update(delta)` calls need no change if the tick passes a constant `dt` to `engine.update` (inference). | steps 1 and 2, `011`, `019`, `031` | High. Input latency, event order, animation speed, camera, and the cost on weak machines. Needs `006` or `055` to check. |

### 6. Options

**A. All three steps.**
- Pros: one clock; full determinism with `011`; replay (`055`) and run comparison (`006`) become simple; timers
  and physics stay in step at low FPS; the X6 model.
- Cons: the largest work; high gameplay risk; it removes the tier lever for simulation cost, so weak machines
  run 60 ticks per s or slow down; needs a decision on tick rate, retune and slow-down first; HIGH loses its
  smoothness without interpolation.

**B. Steps 1 and 2 only.**
- Pros: physics is the same on all tiers and does not change with auto-perf; the `fixedStep` reads become a
  constant; small work.
- Cons: no determinism (frame delta still drives timers); with 1/150, weak tiers get 2x the physics work per
  second and reach the `maxIterations` cap at 25 fps instead of 12.5; HIGH judder gets worse. This option works
  against the weak-hardware goal unless `020` makes the steps cheaper first.

**C. Step 1 now, steps 2 and 3 later.**
- Pros: low risk; no cost change; fixes the hitch problem (timers that jump ahead of physics); prepares the clock
  for `055`; can be done without `011` and without a full `017`.
- Cons: small gain alone; physics stays tier-dependent.

**D. Do nothing.**
- Pros: no risk; no work; weak tiers keep their cheap physics.
- Cons: no replay with real sessions; tier-dependent physics and FPS-dependent spawns stay; `055` stays blocked.

**Recommendation.** Option C first, with a measurement. The order:

1. Measure the frame cost split (simulation vs render) at VERY_LOW and MEDIUM with `--diagnostics` on a weak
   machine, in the same block-dense rooms as `020` Option 0. Steps 2 and 3 depend on this number for the
   weak-hardware goal.
2. Do step 1 alone. It is the step with the least risk, and it is useful on its own.
3. Do `020` (Options A, B, C) before step 2, so that the extra physics steps on weak tiers cost less.
4. Decide step 2 and step 3 together, not step 2 alone. Step 2 alone raises the cost on weak tiers with no
   determinism gain. If step 3 is done, a 60 Hz tick with 2 physics steps of 1/120 is a candidate: it keeps the
   feel close to MEDIUM, and the step count per tick is fixed. The owner must answer first: the reference tier,
   the retune question, the tick rate and "slow down or skip".

New open questions for `ITEM.md`:

- Which tier is the physics reference: MEDIUM (the default), or another? Answered by the owner (2026-10-25): 60 FPS
  (MEDIUM) is the source of truth for how the game looks and feels. See `ITEM.md`, "Decisions".
- Is it acceptable that a weak machine runs more simulation work per second than VERY_LOW does today, or must the
  tick rate or the step count go down on weak machines?
