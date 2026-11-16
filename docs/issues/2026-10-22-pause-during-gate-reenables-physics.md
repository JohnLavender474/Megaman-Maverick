---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [012]
---

# A pause during a gate opening turns physics and behaviors on again when the game resumes

## Where

`LevelStateHandler.kt:18`, `:26`; `Gate.kt:64`, `:249`; `MegaLevelScreen.kt:666-678`, `:873`

## What happens

`GATE_INIT_OPENING` turns off World, Motion, Behaviors and both Pathfinding systems. The gate does not lock input, so the player can pause during the 0.5 s opening. `resume()` turns on every system that pause saved, which is all of them. The camera transition then runs with physics on, until `GATE_INIT_CLOSING`.

## What should happen

Resume restores the state from before the pause.

## How to reproduce

Walk into a boss or room gate and press START during the opening. Resume.

## Notes

Item `012` replaces these flags with named freezes.

Line numbers on `master` at 2026-11-16: `LevelStateHandler.kt:13-30`, `Gate.kt:231` and `:261`,
`MegaLevelScreen.kt:701-727`.

## Short contract

Branch: `fix/pause-restores-systems`, off `master`, in `.worktrees/fix/pause-restores-systems`. It also fixes
`2026-10-22-health-fill-end-unpauses-systems.md`.

A read-only scan (2026-11-16) found that a plain save and restore at pause is not enough. The health fill of
`PlayerStatsHandler` runs while paused, and when it ends it turns all systems on. With a plain restore, a fill that
ends during the pause leaves the level frozen for good after resume. The owner chose (2026-11-16): the fill keeps
running under the pause menu, as today, and writes its system changes into the pause snapshot in place of the live
systems.

Intended changes:

1. `LevelStateHandler.pause()` saves the `on` value of each system, and `resume()` restores those values, in place of
   turning all systems on. `resume()` with no saved values changes no system. After `resume()` the saved values are
   cleared.
2. `PlayerStatsHandler` turns systems on and off through `LevelStateHandler`. While paused, the handler writes the
   new values into the saved values, so `resume()` applies them, and the live systems stay as the pause set them.
   This applies to the health fill, the heart tank and the health tank.
3. The level screen `reset()` clears the saved values, so a pause that ends with an exit from the pause menu cannot
   apply old values to the next level.

Play changes, from the 3 changes above:

- A pause during a gate opening no longer turns physics, motion, behaviors and pathfinding on at resume.
- While paused, a fill that ends no longer turns the systems on under the pause menu.
- A pause during a fill no longer turns the world on for the rest of the fill: after resume, the world stays frozen
  until the fill ends, as with no pause.

Not changed:

- The fill timing, its sounds and its health steps; a fill still runs while paused.
- The gate events (`GATE_INIT_OPENING`, `GATE_INIT_CLOSING`), the room transition, the pause menu, the pause input
  rules (`LevelPauseRules.kt`) and the audio at pause and resume.
- Every other writer of `GameSystem.on` (level start, respawn, fight steps, `TURN_CONTROLLER_OFF` and `ON`).

Accepted:

- A pause no longer turns on a system that some other bug left off.

Checks:

- New tests in `core:test` drive the real `LevelStateHandler` and `PlayerStatsHandler` with real engine systems:
  - a system that was off before the pause is off after resume, and a system that was on is on;
  - the gate case: the 5 gate systems off, pause, resume: they stay off;
  - `resume()` with no `pause()` changes no system, and a second `resume()` changes nothing;
  - the health fill, the heart tank and the health tank: a fill that starts before the pause and ends during it
    leaves the live systems off until resume, and all systems on after resume;
  - a fill that is still running at resume: the systems stay off until it ends, and then turn on;
  - a fill that starts during the pause: the live systems do not change until resume;
  - after the level screen reset, `resume()` changes no system.
- A mutation check for each rule (`docs/WAYS_OF_WORKING.md`, "Mutation checks").
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- Play checks: a temporary hook, not committed, pauses the game by itself at `GATE_INIT_OPENING`, `ADD_PLAYER_HEALTH`,
  `ATTAIN_HEART_TANK` and `ATTAIN_HEALTH_TANK` when the environment variable `MAVERICK_AUTO_PAUSE=1` is set. The hook
  is removed before the merge, and the merge diff is checked for it.
  - Experimental: `MAVERICK_AUTO_PAUSE=1 ./run-game-alpha.sh --level DESERT_MAN --room room13`: walk into the gate.
    The game pauses. Wait 2 s, hold right and resume. Mega Man and the enemies do not move or fall during the camera
    move; Mega Man arrives on the far side as without a pause.
  - Experimental: `MAVERICK_AUTO_PAUSE=1 ./run-game-alpha.sh --level REFACTOR_TEST_029 --room pickups`: take the large
    health bulb, then the health tank. The game pauses at each. Wait 2 s, resume. Nothing moves under the pause
    menu; after resume Mega Man can move.
  - Experimental: `MAVERICK_AUTO_PAUSE=1 ./run-game-alpha.sh --level REFACTOR_TEST_029 --room heart`: take the heart
    tank. The game pauses. Wait 2 s, resume. After resume Mega Man can move, and the health bar is longer.
  - Control: `./run-game-alpha.sh --level DESERT_MAN --room room13`: walk into the gate with no pause. The door
    opens, the camera moves and the door closes as before.
  - Control: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room pickups` and `--room heart`: take each item with no
    pause. The world freezes during the fill and moves again after it, as before.
  - Control: `./run-game-alpha.sh --level DESERT_MAN --room room14`: pause with START during play, wait, resume.
    Mega Man, the enemies and the sounds continue as before.
