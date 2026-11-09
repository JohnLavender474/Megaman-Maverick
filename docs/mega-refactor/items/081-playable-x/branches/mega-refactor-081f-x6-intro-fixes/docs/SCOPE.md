# Scope: mega-refactor-081f-x6-intro-fixes

What this branch changes and why. Write this file after the scan, with `CONTRACT.md` and before
`BRIEF.src.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: draft (written at the `081e` merge, before the branch exists; it still goes through the scan and the brief)
- Approved: no

## Goal

Fix the X6 intro stage (`X6_INTRO`) and `D1000` problems that the `081e` play test found: the boss music, X's
animation in a room transition, the `D1000` body, the `D1000` landing height and walk, and the damage of `D1000` to X.

## Item

`081` (playable X). This branch takes the stage and boss fixes that the `081e` play test found. They are not a phase
of the move set.

Base: the owner chose to start `081f` from `master` after the `081e` merge (2026-11-09).

## Current state

Research of 2 read-only agents during the `081e` play test (2026-11-09, `master` before `00796be27`). The scan at
branch start checks each line reference again.

### Music

- The `boss_room` object in `assets/tiled_maps/tmx/X6Intro_16x14_v1.tmx` (lines 59 to 64) has `event=boss` and
  `object`, but no `fade_out_music=true`.
- `LevelRoomHandler.kt:57` fades the music only when the entered room has `fade_out_music`. So the stage music does not
  fade out at the boss room.

### Animation in a room transition (the gate)

- `MegamanAnimationKeySupplier.kt:32-40` has 2 room transition cases: while `roomTransitionPaused`, the key is `null`
  (the frame holds); while `roomTransition`, the prior key stays, with `stand` (and `slip`) changed to `run`.
- X's `animationKey()` (`MegamanXSprites.kt:76`) has neither case, and X's animator ignores the prior key. So X's pose
  does not follow the transition as Megaman's does.

### `D1000` body

- `D1000.kt:577` makes an `ABSTRACT` body. Only its pre-process (`D1000.kt:582`) places it at the origin.
- `IBodyExtensions.kt:29` processes an `ABSTRACT` body only if it overlaps the camera or has velocity. The body is at
  (0, 0) with no velocity, so it is never processed and stays at (0, 0). So the body of `D1000` cannot damage X, and X
  cannot hit the body.
- Pattern: `BigAssMaverickRobot.kt:215` places the body in `onSpawn`.

### `D1000` landing height

- `CROUCH_HEIGHT = 2f * PPM` (`D1000.kt:78`), 64 px. The landing frames reach 44 px below the origin, so `D1000`
  lands 20 px too high.

### `D1000` walk

- Range: `HALF_WIDTH = 2f * PPM` (`D1000.kt:75`), 64 px. In X6 it is 56 px, so the box edge reaches the wall.
- Facing: X6 turns toward X on each frame of the walk. Maverick turns once, at the start of the walk
  (`D1000.kt:404-406`, `facePlayer()` in step 0).

### Damage of `D1000` to X

- Body 6 (`PlayerDamageNegotiator.kt:38`), bomb 6 (`PlayerDamageNegotiator.kt:39`), pillar 6 (`D1000Pillar.kt:56`),
  core shot 4 (`JunkShot.kt:56`, `Type.CORE`; read through `PlayerDamageNegotiator.kt:39`).

## In scope

Owner's answers (2026-11-09, at the `081e` play test):

1. Music: add `fade_out_music=true` to the `boss_room` object.
2. Gate animation: copy Megaman's 2 room transition cases into X's `animationKey()`. During the transition, `stand`,
   `land_shoot`, `run_trans`, `run_trans_shoot`, `dash_end` and `dash_end_shoot` become `run`; in the pause, the frame
   holds. An air pose (`jump`, `fall`) keeps its frame, as Megaman's does.
3. `D1000` body: place the body in `onSpawn` too, as `BigAssMaverickRobot` does, with a test. Side effect: the body
   hits land for the first time (a full charge shot 5, the sabre 2), and the defeat explosions spawn at the boss, not
   at (0, 0).
4. `D1000` landing: `CROUCH_HEIGHT` 44 px (`1.375f * PPM`).
5. `D1000` walk: fix both the range (the X6 56 px, so the box edge reaches the wall) and the facing (turn toward X on
   each frame of the walk).
6. Damage to X: halve the damage of `D1000` to X: body 3, bomb 3, pillar 3, core shot 2 (was 6, 6, 6, 4).

## Out of scope

Left for later (owner, 2026-11-09):

- The crouch shake as a body move.
- The stun start timing.
- The side that the core returns to.
- The wall dent, the rock fall, the `WARNING` and the death scene.
- The buster bounce.
- The X6 sabre combo damage window (keep 0.5 s).

## Affected areas and risks

- `X6Intro_16x14_v1.tmx`, `MegamanXSprites.kt`, `D1000.kt`, `PlayerDamageNegotiator.kt`, `D1000Pillar.kt`,
  `JunkShot.kt`.
- The `D1000` body fix makes the body hits real for the first time, so the fight gets harder from the body and easier
  from the halved damage. The play test checks the balance.
- `JunkShot.Type.CORE` damage: check that no other caller uses the `CORE` type.

## Overlap with other branches

To check at branch start (see "Parallel branches").

## Objective check

To fill in at the scan.

| Rule | Effect | Note |
|------|--------|------|

## Open questions
