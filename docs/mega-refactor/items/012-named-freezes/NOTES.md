# Notes: 012 Named, composable freezes with a reason table

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 11. Freezes owned by named reasons".

````text
(1) Current state

The owner's reading is correct. These code paths write `system.on` and the controller flags directly (verified):
- Pause: `LevelStateHandler.kt:18` turns off all systems except `SpritesSystem`. On resume (`:26`) it turns on all of the systems that it saved, and it saved all engine systems.
- Room transition: `MegaLevelScreen.kt:302` (behaviors off) and `:376` (behaviors on). It also sends `TURN_CONTROLLER_OFF` and `TURN_CONTROLLER_ON` (`:278`, `:359`).
- Gate: `GATE_INIT_OPENING` (`:666-678`) turns off World, Motion, Behaviors and both Pathfinding systems. `GATE_INIT_CLOSING` (`:680-691`) turns them on.
- Boss intro: `BOSS_READY` (`:746`) turns off all systems except Sprites and Animations. `END_BOSS_SPAWN` (`:752-756`) turns all on.
- Player spawn: `PLAYER_SPAWN_INIT` (`:556`) turns all on. `MegaLevelScreen.kt:420` turns all on at show.
- Stats fill: `PlayerStatsHandler.kt:128`, `:137`, `:156`, `:161`, `:300`, `:317` turn all off or all on.
- Controller: `MegamanMaverickGame.kt:594-610` handles `TURN_CONTROLLER_ON`/`OFF`. It sets `controllerPoller.on` and `ControllerSystem.on`.
- In total: 18 lines write `system.on`. There are about 15 `TURN_CONTROLLER_*` submits in 6 files.
- Each writer sets absolute values ("all on"). No writer knows about the others. The last writer wins.

X6 (`engine.md` "Stop flags", lines 284-289, and the table at 588-589):
- There are 10 stop bytes at `game+0x10…+0x19`, one for each object class (player, weapon, main, visual, effect, stage logic, stage objects, misc, ray, kind-7).
- While a byte is set, only the objects with flag bit `0x08` update. The frozen objects are still drawn (`Display`).
- Writers: pause objects, the boss gate and the health capsule (`SetStopFlags(1)`) write 1. The teleporter writes 2. Player death sets all of them for 8 updates.
- There is a separate input lock, `game+0x1C`.
- So X6 already has granular switches for each class plus a per-object exemption bit. There is no reference count.

Suspected bug: verified by reading for the gate, refuted for the boss fill, and a new related bug found.
- Gate: confirmed reachable. During the 0.5 s gate opening (`Gate.kt:64`, `:249`), the gate does not turn off the controller poller. `isPauseButtonRequested()` (`MegaLevelScreen.kt:873`) reads the poller directly, so a pause is accepted. `resume()` then turns on World, Motion, Behaviors and Pathfinding. The following camera transition runs with physics on, until `GATE_INIT_CLOSING`.
- Boss health fill: I did not find a normal path. `beginTransition` sends `TURN_CONTROLLER_OFF`, and `endTransition` does not turn the controller on for a boss room. With the poller off, buttons go to RELEASED (`ControllerPoller.run`), so START cannot be "just released". (An edge case remains: START is held at the exact frame the poller turns off.)
- New bug, found by reading; needs a runtime check: `playerStatsHandler.update(delta)` runs every frame, also while paused (`MegaLevelScreen.kt:894` is outside the `!game.paused` check). A pickup health fill (HealthBulb) does not lock input, so the player can pause during the fill. When the fill queue empties, `PlayerStatsHandler.kt:317` turns all systems on while the game is still paused. `engine.update` also runs while paused (`:919`). So the simulation and the controller system run under the pause menu.

(2) Options
- (a) Keep the flags, but save and restore the previous values (a stack). Pros: tiny change. Cons: overlapping freezes that do not nest (gate, then pause, then fill end) still break.
- (b) Named freeze reasons with reference counting. An enum or registry of `FreezeReason` (PAUSE, ROOM_TRANSITION, GATE, BOSS_INTRO, STATS_FILL, PLAYER_DEATH, CUTSCENE…). Each reason declares in one table which groups it stops (input, behaviors, physics, motion, pathfinding, animations, updatables, audio…) and which entities are exempt (like the X6 `0x08` bit, for example the gate itself or the boss during its intro). `FreezeController.push(reason)` and `pop(reason)`. A group runs only when no active reason stops it. Pros: overlaps compose, and the table makes freezes easy to configure. Cons: you must migrate all 18 writers and the controller events.
- (c) Freeze per entity class instead of per system, like the X6 classes. You can combine this with (b): the reason table names system groups and entity categories.

Add a debug overlay that lists the active reasons and the stopped groups, and log push/pop with the source.

(3) Recommendation
- Use (b), with exemptions as an entity tag (c-lite).
- Put the input lock in the same mechanism (group `INPUT`), so `TURN_CONTROLLER_ON`/`OFF` become `push`/`pop(CUTSCENE_INPUT)`.
- Make `push`/`pop` idempotent for each owner (a set of reasons, or a count for each owner token), so that a double ON cannot unfreeze another reason.
- Fix the two bugs first as separate issues:
  - The pause must save and restore the state, not "all on".
  - The stats handler must not run, or must not write flags, while paused.

(4) Open questions for the owner
- The list of reasons and their groups. (For example, must animations run during a boss intro? Today: yes, Sprites and Animations stay on.)
- Must the pause stop audio, as today, and cosmetic animations?
- Do you want the X6 "exempt" bit as a property in Tiled for each entity?

(5) Proposed backlog wording

Title: "Named, composable freezes with a reason table"
- Add `FreezeController` with named reasons and a reference count. Each reason stops a declared set of system groups and the input.
- Replace all direct `system.on` writes and the `TURN_CONTROLLER_*` events with push/pop.
- Support exempt entities (like the X6 `0x08` flag).
- Add a debug overlay and log that show the active reasons.
- First fix the pause/resume restore bug and the stats-fill-while-paused bug.

(6) Dependencies
- Item 12 (level flow FSM, pause as overlay): the FSM states push and pop the reasons.
- Item 9: a freeze stops ticks for each group.
- Item 37 (respawn routine).
- Item 30 (input intents).
````
