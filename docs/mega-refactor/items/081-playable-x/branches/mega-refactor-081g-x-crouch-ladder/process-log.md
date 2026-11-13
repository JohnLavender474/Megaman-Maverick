# Process log: mega-refactor-081g-x-crouch-ladder

## Status

- State: merged (2026-11-13)
- Goal: X's crouch, ladder and climb, from `mmx-x6` (`081` open question 10)
- Brief: https://claude.ai/artifact/FNw9guyTgzTWAgMbBAxYR6
- Done: plan steps 1 to 7 (test rooms, Megaman climb test, shared climb, X climb, X crouch, docs); checks green
- Done: play test passed (2026-11-13), Diffity review with no threads
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-12

- Branch start (owner chose this thread after the `081f` merge). `060c` is active in another session and changes
  `core/.../players/`; this branch keeps to X-only code where it can, and `CharacterDef` waits for `060c`.

## 2026-11-13

- Scan, `SCOPE.md`, `CONTRACT.md`, brief. The owner chose all 4 recommendations (D1 shared climb, D2 X6 ladder ends,
  D3 X6 crouch, D4 one-way pass only while climbing). Scope and contract approved.
- Plan step 1: an agent added rooms `ladder` and `crouch` to `RefactorTest081.tmx` (a new `specials` layer for the
  ladders; `SniperJoe` on a step in `crouch`). I raised the step from 16 to 20 px, so the shot clears a 1-tile crouch.
- Plan step 2: `MegamanClimbTest` (12 cases) passes on the old climb. A break of the ladder margin fails it. A break of
  the init snap does not, because the climb also snaps on each frame (the result stays covered).
- Plan step 3: `players/PlayerClimb.kt` (`playerClimbBehavior`, `ClimbParams`, `ClimbEntry`, `ClimbExit`); Megaman's
  `ClimbBehavior.kt` is now a call with his hooks. `MegamanClimbTest` passes unchanged; `core` 1686, 0 failures.
- Plan steps 4 and 5: `megamanx/MegamanXClimbCrouch.kt` (X's climb params, ladder poses, `Crouch`); new lock reason
  `MegaLockReason.LADDER`; X's crouch body height, one-way rule (D4), animations, keys, turn-only run, ladder shot
  height, no sabre on the ladder. `MegamanXClimbCrouchTest` (14 cases); 2 old one-way tests in `MegamanXTest` now
  check the D4 rule. A mutation run with 6 breaks (climb speed, air-dash reset, floor exit, crouch height, turn-only
  run, finish lock) fails 8 tests; all restored.
- Plan step 7: `players/README.md` (shared climb), branch `NOTES.md` (decisions made in the work, X6 sources, `060c`
  overlap), `CONTRACT.md` checks (real rooms: `X6_INTRO` `room1`, `INTRO_STAGE` `room3`).
- Checks: `engine` 675, `core` 1700, smoke 457, JAR built.
- Review before the play test, 2 agents (code diff; manual checks and map). Findings and what was done:
  - Bug (both agents): at a ladder foot with `DOWN` held, X left the ladder (floor exit) and grabbed it again with
    the feet on alternate frames, and could not crouch there. Fixed: `ClimbParams.feetGrabAllowed`; X grabs with the
    feet only on a ladder top (over ground, head off the ladder), and the crouch uses the same test. The same gate
    stops a `DOWN` grab in the air (X6). 2 new tests; a break of the gate fails both.
  - Wrong exit label in `DOWN` and `LEFT` gravity (a bottom exit could give X the climb-over pose). Fixed: the exit
    reports `overTop` by the player's up direction.
  - Megaman: `-0f` velocity at rest in `DOWN`/`LEFT` gravity (old code gave `+0f`). Fixed (`setZero`).
  - Not changed: a float rounding difference in the last bit with a `movementScalar` other than 1; the debug log tag
    of the climb is now `PlayerClimb`.
  - Style: `ClimbExit` with no readers removed (a boolean `overTop`); `LADDER_MOVE_OFFSET` private; the `init` vararg
    renamed (no alias); blank lines removed in new code; `crouch` is `lateinit`.
  - Test gaps closed in `MegamanClimbTest`: both keys held in `UP` and `DOWN` gravity, `RIGHT`/`LEFT` gravity speed,
    `+0` at rest.
  - Contract: "left" and "right" ladders were swapped in the `ladder` check. Fixed.
  - Kept as is, for the play test: X cannot grab with `UP` while the jump still rises (the shared gate blocks the
    grab during `JUMPING`, as for Megaman); the grab works from the top of the jump. X climbs while he shoots (X6).
  - Manual checks: all 5 load and hold their setups (`crouch`: the shot spans 35 to 46 px over the floor; it misses
    the 32 px crouch and hits the 48 px standing X). `flat` has a `LadderTop` with no ladder; X now stands on it (D4).

## 2026-11-13 (play test)

- Owner play test, round 1. The climb works well. 2 fixes:
  - X sat 1 to 2 px right of the ladder while he climbed. A code shift (`translateX` in the sprite pre-process) was
    replaced at the owner's request with an art fix: the 6 `climb` frames and the `climb_shoot` frame moved 2 px left
    in the frame strips and in the atlas (patched in place at the same bounds, in the original palette format; no
    other pixel changed). The slicer data file gives `climb_shoot` the offset `[1, 0]` (was `[3, 0]`) and notes the
    hand-made `climb.png`. The owner made this a rule in the new `docs/WAYS_OF_WORKING.md` on `master`.
  - The crouch shot started too far back: the `crouch_shoot` arm tip is 2 art px further out than the standing shot
    pose. New `SHOT_OFFSET_X_CROUCH` (0.125 tile).
- Owner: "Play checks pass." Diffity review: no threads. Checks with the fixes: `engine` 675, `core` 1706, smoke 457,
  JAR built.
