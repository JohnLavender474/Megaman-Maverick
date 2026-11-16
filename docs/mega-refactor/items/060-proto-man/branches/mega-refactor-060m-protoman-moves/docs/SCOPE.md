# Scope: mega-refactor-060m-protoman-moves

This is Claude's record. The owner approves the brief, not this file.

- Status: approved
- Approved: 2026-11-16

## Goal

Proto Man gets Megaman's wall slide (with the wall jump), air dash and swim, with Megaman's rules and the shared
`players/` code. No Rush Jet. He turns the camera on a gravity change, as Megaman does. A Fable 5.2 art agent draws
the new frames.

## Item

From `060` `ITEM.md` (2026-11-16, owner): "`060m`: Megaman's moves for Proto Man: wall slide and air dash through the
shared `PlayerWallSlide` and `PlayerAirDash`, swim (Megaman's swim moves into `players/` first), no Rush Jet; the
camera turn on a gravity change; the frames of these moves." At the branch start: "A Fable 5.2 art agent draws the
new frames of `060m`." Earlier (2026-10-31, from `084`): "`AButtonTask` has no component until this item shows a need."

## Current state

- Shared already: `players/PlayerWallSlide.kt` (`WallSlideParams`; Megaman and X), `players/PlayerAirDash.kt`
  (`AirDashParams`, `airDashInputPressed`; Megaman and X), `players/PlayerJump.kt` (`JumpParams` with the wall jump
  fields; Proto Man passes `wallJumpAllowed = { false }`).
- Megaman only:
  - `megaman/behaviors/SwimBehavior.kt`: an `AbstractBehaviorImpl` with a `Megaman` receiver (`SWIM_TIMER`, swim
    impulse by direction, `JUMP_BOOST` enhancement, weapon block).
  - The A-button task (`AButtonTask` `JUMP`, `SWIM`, `AIR_DASH`): a field on `Megaman`, set in 8 files: `Megaman.kt`
    (spawn, the body's water and feet hooks), `MegamanAButtonTask.kt` (the air and ground rules), the jump, air dash,
    wall slide, swim and climb behaviors. It decides what `A` does in the air and in water.
  - The wall jump state: `wallJumpTimer`, `wallJumpVel`, `wallSlideNotAllowedTimer` on `Megaman`.
  - The camera turn on a gravity change: `Megaman.direction` setter calls `cameraController.startSmoothFollow()`.
- Proto Man (`entities/protoman/Protoman.kt`): behaviors `JUMPING`, `GROUND_SLIDING`, `CLIMBING`; `A` is only the
  jump; `TODO` "no swim; A in mid-water does nothing; decided in 060g" (060m now); `TODO` at the `direction` setter
  (no camera turn). His guard breaks in a slide and on a ladder (`crouchBlocked()`); the air guard is `DOWN` in the
  air.
- Art: `assets/sprites/frames/Protoman_v2/` has no wall slide, air dash or swim strips. Megaman's keys for reference:
  `wallslide`, `wallslide_shoot`, `airdash`, `airdash_shoot`, `swim`, `swim_shoot`, `swim_attack`.
- Test map `RefactorTest060.tmx`: rooms `walk`, `jump`, `water`, `transition`, `buster`, `sabre`, `shield`, `slide`,
  `ladder`.

## In scope

1. Megaman tests first for the swim and for the A-button task rules, green on the old code.
2. The swim moves to `players/` (`SwimParams`, `playerSwimBehavior`); Megaman calls it, rule for rule the old one.
3. The A-button task moves into one `players/` part (D1).
4. Proto Man: `WALL_SLIDING` (with the wall jump), `AIR_DASHING`, `SWIMMING` through the shared code, with his own
   values in `ProtomanValues` (Megaman's numbers). Megaman's enhancements (`JUMP_BOOST`) and weapon blocks stay
   Megaman's.
5. Attacks and the guard in the new moves (D2, D3): shots, the wall slash and the air slash; the wall guard, the
   shielded air dash, the air guard in water.
6. The camera turn on a gravity change, as Megaman.
7. Art: the new strips by a Fable 5.2 art agent (decision D4 for the list); the atlas repacked with the `060c` packer;
   the muzzle points and shot offsets of the new shoot frames.
8. `RefactorTest060`: new rooms `wall` (tall walls on both sides, a wall gap) and `airdash` (gaps that need a dash);
   the `water` room for the swim.

## Out of scope

- Rush Jet (`JETPACKING`), Megaman's crouch (Proto Man's crouch is his guard).
- Proto Man's damage, stun and frozen art (`060e`); the sabre hit box (`060o`).

## Affected areas and risks

- `Megaman.kt`, `MegamanAButtonTask.kt`, `SwimBehavior.kt`, the jump, air dash, wall slide, climb behaviors (D1),
  `players/` (new swim file, maybe an A-task component), `Protoman.kt`, `ProtomanValues.kt`, `ProtomanBuster.kt`,
  `ProtomanSabre.kt`, the `Protoman_v2` atlas, `RefactorTest060.tmx`.
- Risk: the A-button task is spread over the body hooks of `Megaman`; a move changes when `A` jumps, dashes or swims
  for Megaman. Tests first.
- Risk: the atlas. `060k` (active) edits frames in the same atlas. The frame files are separate; the second branch to
  merge repacks from its frames.

## Overlap with other branches

- `060k` (active): `Protoman_v2` frames and atlas.
- `024b` (active): `Protoman.kt` (`character`, `shooting`), `Megaman.kt` weapons; `currentWeaponPrevents` is read by
  the swim, wall slide and air dash blocks.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` | no effect | Each new param has its caller (Megaman and Proto Man). |
| `O1-R3` | no effect | No new order rule; behaviors keep the behaviors list order. |
| `O2-R1` | no effect | Input through the controller frame. |
| `O8-R1` | helps | The swim (and the A-task, D1) move into `players/`; no copy of `Megaman` code. |
| `O8-R2` | no effect | No new `is Megaman` check. |

## Decisions

Owner, 2026-11-16, at the brief:

- D1: the `A` task (what `A` does in the air and in water: jump, air dash, swim) moves from `Megaman` into one
  `players/` part; Megaman and Proto Man use it. Megaman tests first, on the old code.
- D2: `B` shoots in all 3 moves (Megaman's rule): wall slide shot (away from the wall), air dash shot, swim shot.
  `UP`+`B` on a wall is a wall slash (the shared `SabreState.WALL`, as X); in a dash and in water it is the air slash.
- D3: the guard (`DOWN`) works in all 3 moves (owner; Claude agreed: `DOWN` has no other job there, unlike on a
  ladder). Follow-ups: on a wall, a new wall guard strip with its pull-out (he faces away from the wall); in an air
  dash, the dash goes on with the shield up in front (a shielded dash, new strip); in water, the air guard
  (`guard_jump` frames).
- D4: one Fable 5.2 art round for all new strips; the owner picks per strip; later fixes go in `060k`. The list:
  `wallslide`, `wallslide_shoot`, `sabre_wall` (+ arc), `wallslide_guard` (+ pull-out), `airdash`, `airdash_shoot`,
  `airdash_guard` (+ pull-out), `swim`, `swim_shoot`.

## Open questions

None.
