# Scope: mega-refactor-081f-x6-intro-fixes

What this branch changes and why. Write this file after the scan, with `CONTRACT.md` and before
`BRIEF.src.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-09 (brief); scope changes 2026-11-09 (owner answers, see "Scope changes during the work")

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
   Scope change (owner, 2026-11-09, during the work): D-1000 falls until the landing pose's feet touch the floor (the
   44 px), and it rises toward X's height, clamped so that its hit box bottom is at least 0.5 tile over the floor and
   its hit box top is at most 0.5 tile under the ceiling (in place of the fixed 3.5-tile stand height). The ceiling is
   the top of the `D1000` spawn rectangle, which the TMX grows to the room ceiling. The ram wind-up uses the same
   range (was 2 to 3.5 tiles). With the hit box, the lowest hover origin is floor + 44 px, the landing height.
5. `D1000` walk: fix both the range (the X6 56 px, so the box edge reaches the wall) and the facing (turn toward X on
   each frame of the walk).
6. Damage to X: halve the damage of `D1000` to X: body 3, bomb 3, pillar 3, core shot 2 (was 6, 6, 6, 4).

## Scope changes during the work (owner, 2026-11-09)

The owner added these to the branch after the first fixes passed their tests. Each deviation from X6 has a row in
the CUSTOM tables of `NOTES.md`.

1. Pacing: the fight is too slow for the larger room and the faster X. Shorter waits and faster moves (CUSTOM).
2. Music: the D-1000 fight plays its own looping track, `MMX6_D1000.ogg`, cut from the owner's `MMX6_d1000.mp3`
   with the fade-out removed.
3. Eyeball: it spawns 8 tiles over the floor (was 5).
4. Art: the D-1000 body art is 1.25 times larger and hand-cleaned, traced from the X6 sprites (the original red and
   X6's dark eyes on `land` and `stand_up` are kept). The body sizes in `D1000.kt` scale 1.25 times with it
   (`CROUCH_HEIGHT` 1.75 tiles, from the feet of the new `land` art).
5. Attacks: Wall Crash, Dazed Eye, Ricochet Ram, Pillar Fence, Bracket, Sweep, Ceiling Crash, the wall-climb
   Shockwave, Peekaboo, Spotter, Overdrive at half HP and the adaptive director (`D1000Director`).
6. Eyeball attacks: Spread and Dive (Claude's design, from "has a few attacks of its own"), and contact damage 2.
7. Hit boxes: per drawn frame BODY, DAMAGER and DAMAGEABLE boxes traced from the art, on the body and on the
   eyeball (`TickFrameFixtures`).

New APIs and their first callers: `D1000Director.chooseAction` (`D1000` THINK); `D1000Shockwave` (`D1000` slam
landing); `TickFrames.artFrame`, `placeBox`, `maxBoxes` and `TickFrameFixtures` (`D1000` and `D1000Core`
pre-process).

Open after the play test (Claude's defaults are in the code): the aimed bomb lands on X; the closed eye is 45 %
opacity; a combo is ready at the fight start; the DAMAGER uses the full art; the flashing eye is hit on the whole
ball; the box split of `stand` frame 2.

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

## Scan at branch start (2026-11-09, `master` `fd03dfd06`)

- The line references above are true on `master` after the `081e` merge (`D1000.kt:75,78,186-210,401-406,577,582`;
  `PlayerDamageNegotiator.kt:38-39`; `JunkShot.kt:56`; `D1000Pillar.kt:56`, `DAMAGE = 6`, read through
  `IHazard.getDamageToMegaman()`).
- `JunkShot.Type.CORE` has one caller, `D1000Core.kt:152`. So its damage change touches only the core shot.
- `PlayerDamageNegotiator` serves both characters, but only `X6_INTRO` has a `D1000`, and only X plays it. So the
  halved damage does not change Megaman's play.
- The 2 gates of the intro are objects 13 (room1 to room2) and 14 (room2 to `boss_room`), 64 x 96 px at floor level.
  The `081b` log says that the gate trigger of this map is above standing height; a play check looks at it.
- `D1000Test.kt` exists (`core/src/test/.../bosses/`); the body test goes there.
- Small changes in scope (owner may say no in the brief): the 5 code `TODO`s from `081e` point to the new items
  `137` to `141` in place of "(item 081)"; `docs/x6-intro/d-1000-plan.md` and `intro-objects-plan.md` use the new
  names (`DebrisPiece`, `Wreck`, `JunkShot`, `TickFrames`).

## Overlap with other branches

- `127a`, `134a` and `compactor-level` change none of the files of this branch (checked with
  `git diff --name-only master...<branch>`).

## Objective check

To fill in at the scan.

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` Components, not new inheritance | no effect | no new class |
| `O1-R3` No new fragile order | no effect | the body is placed in `onSpawn` and in its pre-process, the same value |
| `O1-R5` Code only with a production caller | no effect | no new API |
| `O2-R5` No logic assumes one player | no effect | `facePlayer()` already picks the target player |
| `O2-R7` Gameplay does not depend on rendering | no effect | the gate fix changes only X's animation key |
| `O5-R1` No per-frame allocation | no effect | `facePlayer()` on each walk frame allocates nothing |
| `O6-R2` Named checks for kept and changed behavior | helps | tests for the body place, the crouch height, the walk, the damage, X's transition key |
| `O8-R1` No copy of `Megaman.kt` | no effect | X gets its own 2 transition cases, as X's key code already is its own; a shared helper is out of scope (it changes Megaman's key code) |
| `O8-R2` Capabilities, not character checks | no effect | no `is Megaman` or `is MegamanX` check is added |

## Brief answers (2026-11-09)

- D1: yes. The 5 code `TODO`s from `081e` point at the items `137` to `141`; `docs/x6-intro/d-1000-plan.md` and
  `intro-objects-plan.md` use the new class names. Comment and docs text only.
- D2: walking into the intro gates works (owner). No map change for the gate triggers.

## Open questions

None.
