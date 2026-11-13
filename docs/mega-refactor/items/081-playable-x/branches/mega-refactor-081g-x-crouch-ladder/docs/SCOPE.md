# Scope: mega-refactor-081g-x-crouch-ladder

Status: approved (2026-11-13)

## Goal

X can crouch, grab a ladder, climb it and leave it at the top, the bottom or in the air. This closes `081` open
question 10 for crouch, ladder and climb. The behavior follows `mmx-x6` `Player.java` (facts only, no code copied).

## Scan

### X today

- `megamanx/MegamanX.kt` adds 4 behaviors: `WALL_SLIDING`, `GROUND_DASHING`, `JUMPING`, `AIR_DASHING`. No crouch, no
  climb.
- X's body has `HEAD` and `FEET` fixtures, so the `HEAD_LADDER` and `FEET_LADDER` contact rules already set
  `HEAD_TOUCHING_LADDER`, `FEET_TOUCHING_LADDER` and the `ConstKeys.LADDER` body property for X.
- `MegamanX.kt:166`: a `TODO` says X passes every one-way block (ladder tops and one-way platforms) while `DOWN` is
  held, because X cannot climb yet.
- The atlas `MegamanX` already has the strips `crouch`, `crouch_shoot`, `climb`, `climb_shoot`, `climb_start`,
  `climb_finish` (from the `1LMX` sheet). No art work is needed; the frame counts are read at the plan step.
- X keeps his own body (`defineBodyComponent`), not `players/PlayerBody.kt`.

### Megaman today

- `megaman/behaviors/ClimbBehavior.kt` (230 lines): a `Megaman` receiver. Grab with `UP` at the head or `DOWN` at the
  feet; snap to the ladder center; gravity off; climb speed `MegamanValues.CLIMB_VEL` (5 tiles/s); stop while
  shooting; `A` lets go; leave past the ladder ends with a 0.5-tile margin; all 4 gravity directions. Megaman-only
  parts: `aButtonTask`, `canMakeLandSound`, the `NEEDLE_SPIN` check.
- `megaman/behaviors/CrouchBehavior.kt` (67 lines): a `Megaman` receiver, with a 0.05 s button delay, a 0.15 s delay
  after a ground slide, no crouch on sand, and a body translate per direction.
- The shared code already reads `CLIMBING` and `CROUCHING` (`PlayerBody.kt`, `PlayerJump.kt`, `PlayerRun.kt`,
  `PlayerWallSlide.kt`, `PlayerAirDash.kt`, `MegaPlayer.kt`), so X gets these rules when he has the behaviors.

### `mmx-x6` (`game/player/Player.java`, `PlayerConst.java`)

- Crouch (`toCrouch`, `updateCrouch`): starts on `DOWN` on the ground, X stops, can turn, can shoot (low pose), jump
  and dash leave it. On release, a short rise animation, then stand.
- Ladder (`tryLadderFromGround`, `tryLadderAir`, `updateLadder`, `updateClimb`): grab with `UP` at the head, `DOWN`
  on a ladder top (a climb-down animation), or `UP` in the air (resets the air dash). Snap to the tile center.
  `LADDER` = 1.5 px per frame (5.625 tiles/s at 60 fps). At the top: a finish animation, then stand on the top. At the
  bottom with `DOWN` on a floor: stand. Jump or off the ladder: let go and fall. X can turn and shoot on the ladder.

### Other active branches

- `060c` (other session) changes `MegamanX.kt`, `MegamanXMoves.kt`, `MegamanXController.kt`, `MegamanXSprites.kt` and
  adds files in `players/`. It does not touch climb or crouch. Expect small text conflicts in `MegamanX.kt` (the
  behaviors list and the animations map). See `NOTES.md`.

## Will change

- A shared climb behavior in `players/` with a parameter class and hooks; Megaman and X call it (decision D1).
- X's ladder enter and exit animations (decision D2).
- X's crouch (decision D3).
- X's one-way pass rule (decision D4); the `TODO` at `MegamanX.kt:166` is removed.
- X's animation keys, sprite offsets and buster origin for crouch and climb.
- Test rooms in `RefactorTest081.tmx`: a ladder room and a crouch room.

## Will not change

- Megaman's climb and crouch behavior (the contract checks this).
- Ropes, bars, the falcon dash (`081` scope).
- `CharacterDef` (after `060c`).

## New API and first caller

| API | First caller |
|---|---|
| `players/PlayerClimb.kt` `playerClimbBehavior(params)` | `Megaman.defineBehaviorsComponent`, `MegamanX.defineBehaviorsComponent` |
| `ClimbParams` | the 2 calls above |

## Objective check

| Rule | Effect |
|---|---|
| `O8-R1` no copy of `Megaman.kt` | helps (climb shared, not copied) |
| `O8-R2` capabilities, not character checks | no effect (0 new `is` checks) |
| `O8-R3` name the modes | no effect |
| `O8-R4` one definition per weapon | no effect |
| Always-on: keep untouched behavior | helps (Megaman climb checks in the contract) |

## Decisions

Owner, 2026-11-13 (all recommendations):

- D1: share Megaman's climb in `players/` with a params class and hooks; Megaman and X call it.
- D2: X6 ladder ends for X: the climb-down animation from the top and the climb-over animation at the top, with a
  short input lock.
- D3: X6 crouch rules: crouch at once on `DOWN`, X stops, turns, shoots low, jumps or dashes out; a short rise on
  release; a lower body.
- D4: as Megaman: X passes a one-way block only while he climbs; `DOWN` on a ladder top grabs the ladder.
