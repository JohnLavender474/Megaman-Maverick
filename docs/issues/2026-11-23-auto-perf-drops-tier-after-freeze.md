---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: item 010 gameplay-need scan (2026-11-23)
related-items: [010]
---

# One freeze of about 1.5 s can make auto-perf drop the tier for the rest of the session

## Where

`core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt:597-623` (auto-perf in `render()`), timer values at
`:182-189`.

## What happens

Auto-perf adds `delta × multiplier` to `autoPerfTimer` (`AUTO_PERF_SUSTAINED_DUR` = 3 s) while `framesPerSecond` is
below the tier FPS. The delta is not clamped (see `2026-10-22-frame-delta-not-clamped.md`). After one long frame (a
minimize and restore, a window drag, a GC pause), `framesPerSecond` counts the frames of a partial second, so it reads far below
the tier FPS. Then the multiplier is `AUTO_PERF_CRISIS_SPEED` 2.0. A single frame of 1.5 s or more fills the 3 s timer
at once, and the tier drops (`setPerformance`). Auto-perf only steps down, so the lower tier (lower FPS cap, longer physics step)
lasts for the rest of the session.

## What should happen

A single hitch does not change the tier. Only a sustained low frame rate does, as the `AUTO_PERF_*` comments describe
("~1.5s" to "~10s" tolerance).

## How to reproduce

1. `./run-game-alpha.sh --level INTRO_STAGE --room room1` (auto-perf is on by default; MEDIUM tier).
2. Stop the game JVM for about 3 s and continue it (`kill -STOP <pid>`, then `kill -CONT <pid>`). On Linux this is the
   reliable way to get one long frame: a minimized window keeps rendering (continuous rendering, `pauseWhenMinimized`
   is off), and a window drag does not block the GLFW loop on most Linux window managers.
3. Look for the "Performance issue detected! Downgrading to 45 FPS." notification.

## Notes

Confirmed by reading the code and the libGDX 1.14.2 source (2026-11-23). Not yet seen in the game.

`Lwjgl3Graphics.update()` (libGDX 1.14.2, `Lwjgl3Graphics.java:157-173`) sets `fps` to the frames of the window that
started at the last window close, at the first frame that is 1 s or more after that close. A long frame of `L` s that
starts `x` s into a window (`x + L ≥ 1`) closes the window with about `x × tier FPS` frames. That low value then stays
for 1 more second. So the timer gets `(L + 1) × multiplier`:

| Reading after the freeze (MEDIUM, 60 fps) | Multiplier | Freeze that drops the tier |
|---|---|---|
| 45 or less (`x ≤ 0.75`) | 2.0 | `L ≥ 0.5` s |
| 46 to 49 | 1.0 | `L ≥ 2` s |
| 50 to 54 | 0.5 | `L ≥ 5` s |
| 55 to 59 | 0.3 | `L ≥ 9` s |

So the "1.5 s" in the title is not the lower limit: a freeze of 0.5 s can drop the tier, and a freeze of 1 s or more
drops it for about 3 of 4 freezes. The same holds at HIGH (90 fps, crisis at 60 or less). The level-load frame is also a
long frame: it adds its full delta to the grace timer, so a load of 5 s or more ends the grace period at once.

## Fix options (2026-11-23)

| Option | What it does | Result |
|---|---|---|
| A: skip the timer on a long frame | No timer update on a frame over a cap; keep `framesPerSecond` | Incomplete: the partial-window reading stays for 1 s after the freeze and adds up to 2 s to the timer |
| B: own frame-rate meter (recommended) | Auto-perf counts its own 1 s frame windows from the raw delta; a hitch frame is not a sample | Fixes both parts; `010a` passes its raw delta to the same unit |
| C: new grace period after a long frame | A long frame restarts the 5 s grace | Works, but GC hitches every few seconds on a slow machine stop auto-perf for ever |
| D: feed the clamped delta of `010a` | Timer gets `min(delta, maxIterations × step)` | Needs `010a`; the partial reading stays; at HIGH the clamp (27 ms) also hides real slow frames |

Decision: option B. Owner accepted the recommendation, 2026-11-23.

## Short contract

Branch: `fix/auto-perf-freeze`, off `master`, in `.worktrees/fix/auto-perf-freeze`.

Intended changes:

- A new class `AutoPerformance` (`core/.../state/AutoPerformance.kt`) holds the auto-perf rule. The game gives it the
  raw frame delta and the current tier each frame (only with auto-perf on), and it gives back the lower tier when the
  tier must drop. The game keeps `setPerformance`, the notification and the log line.
- `AutoPerformance` measures the frame rate itself from the deltas it gets, with the same 1 s window count as libGDX
  `framesPerSecond`. It does not read `Gdx.graphics.framesPerSecond`.
- A frame with a delta over 0.25 s is a hitch, not a sample: it does not go into the frame-rate window, the 3 s timer or
  the 5 s grace timer. So one freeze (a stopped process, a GC pause, a window drag, a slow level load) does not drop the
  tier and does not use up the grace period.
- Before the first full window there is no reading, and the tier does not drop (today the grace period covers this).

Not changed:

- The timer rules: the 3 s sustained time, the 5 s grace period after a tier change and at level start, the deficit
  bands and the multipliers (0.3, 0.5, 1.0, 2.0), one tier down at a time, never up, nothing at `VERY_LOW`.
- At a steady frame rate the reading is the same as `framesPerSecond`, so a machine that is slow all the time drops
  its tier as before.
- `--autoPerf false`, the debug text FPS, and the `DrillHead` and `Laser` FPS guards (they still read
  `framesPerSecond`; item `010` removes them).
- The frame delta that the game passes to the screens: no clamp here (`2026-10-22-frame-delta-not-clamped.md`, item
  `010a`).

Accepted edge case: a machine whose frames are all over 0.25 s (under 4 fps) never drops its tier. `TODO` at the hitch
rule.

`010a`: its frame clock passes the raw delta to `AutoPerformance.update`, in place of `Gdx.graphics.deltaTime`. The
hitch limit stays an auto-perf value. It is not the clamp of `010a`.

Checks:

- New unit tests in `core` (`AutoPerformanceTest`): a steady 60 fps at MEDIUM never drops; a steady 30 fps at MEDIUM
  drops to LOW; one freeze of 0.6, 1, 1.5, 3 or 10 s between 60 fps frames does not drop the tier; a freeze does not use up
  the grace period; a steady 30 fps with a freeze still drops; `VERY_LOW` never drops. A mutation that treats every
  frame as a sample (hitch limit `Float.MAX_VALUE`) makes the freeze and grace tests fail.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule` passes.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/auto-perf-freeze`. The helper
scripts are in `temp/play/` of the worktree: `freeze.sh <seconds>` stops the game JVM of this worktree for that time,
and `throttle.sh <seconds>` stops it for 50 ms in each 70 ms for that time (about 15 fps, each frame under 0.25 s).
Run them from a second terminal while the game window shows the level.

- `./run-game-alpha.sh --level INTRO_STAGE --room room1`: play for 10 s, then run `temp/play/freeze.sh 3` three
  times, with about 5 s of play between them. No "Performance issue detected!" notification, and no "Downgrading"
  line in the log.
- `./run-game-alpha.sh --level INTRO_STAGE --room room1`: play for 10 s, then run `temp/play/freeze.sh 1` five times,
  with about 3 s of play between them. No "Performance issue detected!" notification.
- `./run-game-alpha.sh --level INTRO_STAGE --room room1`: play for 10 s, then run `temp/play/throttle.sh 15`. A
  sustained low frame rate still drops the tier: "Downgrading to 45 FPS." comes after about 3 s, and "Downgrading to
  30 FPS." about 7 s later.
