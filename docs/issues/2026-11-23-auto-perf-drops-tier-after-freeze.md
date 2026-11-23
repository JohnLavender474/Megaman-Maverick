---
status: open
verified: no
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

1. `./run-game-alpha.sh --level INTRO_STAGE` (auto-perf is on by default; MEDIUM tier).
2. Minimize the window for about 3 s and restore it, or hold a window drag for about 3 s.
3. Look for the "Performance issue detected! Downgrading to 45 FPS." notification.

## Notes

Found by reading the code. Not checked in the game. A fix can skip auto-perf on a frame whose raw delta is
longer than a cap, or feed it the clamped delta once `010` step 1 adds the clamp.
