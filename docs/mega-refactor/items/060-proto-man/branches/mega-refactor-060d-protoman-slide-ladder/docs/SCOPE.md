# Scope: mega-refactor-060d-protoman-slide-ladder

- Status: approved
- Approved: 2026-11-14 (all decisions answered by the owner on 2026-11-14; see "Decisions")

## Goal

Proto Man slides (`DOWN` + `A` on the ground) and climbs ladders, with Megaman's rules. The slide moves from a
`Megaman` receiver into `players/`, so that Megaman and Proto Man call one shared slide, as `PlayerClimb` is shared.
Megaman plays the same. X does not change.

## Item

`060` (Proto Man), `ITEM.md`:

- Phase `060d`: "Slide and ladder, with Mega's rules".
- Decision (2026-11-10, owner): a thin `Protoman` class that calls Megaman's shared behaviors and components, with
  his own values file.
- Decision (2026-11-14, owner, at the `060d` start): Proto Man shares Megaman's slide and ladder rules.
- Notes for `060d` from the `060c` merge (squash `3247c7fbe`): `DOWN` + `A` on the ground is the slide; no guard on a
  ladder; on a ladder `B` is always the buster, for every character; `DOWN` does not drop Proto Man through a one-way
  platform; `SpringBouncer` and `SpiderWeb` also read `DOWN`.

Research (main checkout temp folder, not committed):

- Code scan: `/home/johnlavender/IdeaProjects/Megaman-Maverick/temp/mega-refactor-060d-protoman-slide-ladder/scan-code.md`.
- Art scan: `/home/johnlavender/IdeaProjects/Megaman-Maverick/temp/mega-refactor-060d-protoman-slide-ladder/sprites/notes.md`
  (with `contact_x4.png` and `fan_cells_x4.png`).

## Current state

Base: `master` `a0cf393e7`. Paths are under `core/src/main/kotlin/com/megaman/maverick/game/`. The scan report has
the full tables; this is the summary.

### Megaman's slide

- The slide is a `Megaman` receiver, `Megaman.groundSlideBehavior()` in `entities/megaman/behaviors/GroundSlideBehavior.kt`
  (130 lines), registered at `Megaman.kt:492`. No test covers its rules. `MegamanBehaviorsTest` checks only the
  behavior order.
- Start: `FEET_ON_GROUND`, `DOWN` held, `A` just pressed (`GroundSlideBehavior.kt:58-60`). `JumpParams.downBlocksGroundJump`
  keeps `DOWN` + `A` from a jump.
- Rules (`GroundSlideBehavior.kt:39-128`): not when dead, out of play, unable to move, in a camera turn, on sand,
  jetpacking, in cooldown, or while Needle Spin shoots; stays on under a low ceiling (`HEAD_TOUCHING_BLOCK`) with no
  time limit; ends off the ground, on damage, at the max time, or after the min time when `DOWN` is released; goes on
  after the min time only while `A` is held and the gravity is the start gravity; body moves at the start and the end
  for each gravity; speed 10 tiles/s (6 in water) x `movementScalar`, x1.5 with `GROUND_SLIDE_BOOST`, set along the
  floor by `facing`; ice keeps the momentum; at the end, cooldown and a run-speed clamp.
- Values: `MegamanValues.kt:71-75` (cooldown 0.075 s, min 0.2 s, max 0.5 s, speed 10, water speed 6). Body height
  0.9 (`GROUNDSLIDE_CROUCH_HEIGHT`, `entities/megaman/components/BodyComponent.kt:14`).
- Megaman-only reads in the behavior: `canMove` (equal to `playerCanMove`), Needle Spin, the `MegamanValues` timers
  and speeds, `movementScalar` and the boost. All other reads are `MegaPlayer` members.
- Already shared: `playerBody` shrinks the body to `crouchHeight`, sets gravity 0, turns the damage box and stops
  speed across the floor for `GROUND_SLIDING` (`players/PlayerBody.kt:221-225,262-269,296,319`); `PlayerRun` and
  `PlayerClimb` read `GROUND_SLIDING`; the momentum helpers are `IBodyEntity` extensions
  (`entities/state/PostActionMomentum.kt`).
- Megaman only, and staying so: after-images (`MegamanTrailSpriteV2`), bubbles, the slide sprite offset, the shot
  offsets in `MegamanWeaponsHandler`.

### The ladder

- `players/PlayerClimb.kt` is shared: `ClimbParams` and `playerClimbBehavior(player, params)`. Megaman
  (`entities/megaman/behaviors/ClimbBehavior.kt`) and X (`entities/megamanx/MegamanXClimbCrouch.kt`) call it.
  Proto Man is a third caller; `ClimbParams` needs no new field.
- Megaman's params: speed 5 x `movementScalar`, `feetGrabAllowed = { true }` (`DOWN` grabs in the air when the feet
  touch a ladder), `holdStill = { shooting }`, `leavesOnFloor = false`.
- Megaman's poses (`MegamanAnimationKeySupplier.kt:68-80`): `climb` when moving, `climb_still` when still,
  `climb_finish` when the body center is out of the ladder and he does not shoot; a shot holds him still, so a ladder
  shot shows `climb_still_shoot`.
- `PlayerBuster.shoot(status, crouched, climbing)` reads `BusterParams.climbOffset`; `PROTOMAN_BUSTER.climbOffset` is
  `null` today, so the first ladder shot throws `BusterClimbOffsetMissingException` (`players/PlayerBuster.kt:47-48,98`).
  `BusterParams` has no slide offset.

### Proto Man today

- `Protoman` registers only `JUMPING` (`Protoman.kt:427-449`). No `GROUND_SLIDING`, no `CLIMBING`.
- `crouchHeight = ProtomanValues.BODY_HEIGHT` (`Protoman.kt:329`): the `060c` `NOTES.md` decision "`crouchHeight` stays
  `BODY_HEIGHT`". He has no `CROUCHING` behavior, so `crouchHeight` acts only for the slide. This branch changes the
  decision.
- `groundSliding = { false }` in `defineMomentumComponent` (`Protoman.kt:405`).
- `animationKey` throws for a shot in a pose that has no `SHOOT_KEYS` entry (`Protoman.kt:575`). `SHOOT_KEYS` and
  `BASE_POSES` have no slide or climb key.
- `Protoman_v2.txt` has no `groundslide` or `climb*` region. `findRegion(name)!!` fails the smoke test until they exist.
- `RefactorTest060.tmx` (240 x 18 tiles; rooms `walk`, `jump`, `water`, `transition`, `transition_next`, `buster`,
  `sabre`, `shield`) has 0 ladders, 0 low ceilings and 0 `GroundSlideBlocker`s. Proto Man spawns only in
  `REFACTOR_TEST_060`.

### The `DOWN` readers that must learn the slide and the ladder

None of them reads `GROUND_SLIDING` or `CLIMBING` today.

| Reader | Today | Change |
|---|---|---|
| Guard (`guardBroken`, `guardWanted`, `Protoman.kt:221-225`) | `DOWN` raises the guard in every state | the slide and the ladder break the guard: no shield, no pull-out, no put-away |
| Crouch attack (`crouchAttackWanted`, `:227`) | `DOWN` + `B` over ground is a crouch attack | not in a slide, not on a ladder (`bodyOverGround` can be true at a ladder foot) |
| Crouch box (`defineCrouchBox`, `:361-375`; `crouched`, `:167-168`) | resizes `BODY` to 18 art px while he guards on the ground | off in the slide, so the 0.9 slide body stays |
| Shield (`shieldUp`, preProcess `:384`) | up with `guardUp && guardWanted` | off in the slide and on a ladder (through `guardBroken`) |
| Animation keys (`animationKey`, `poseKey`, `SHOOT_KEYS`, `BASE_POSES`, `:74-92,569-597`) | no slide or climb branch | `groundslide`, `groundslide_shoot`, `climb`, `climb_still`, `climb_still_shoot`, `climb_finish` |
| Momentum (`groundSliding`, `:405`) | `{ false }` | `{ isBehaviorActive(GROUND_SLIDING) }`, as Megaman |
| Slide body (`crouchHeight`, `:329`) | `BODY_HEIGHT` | `ProtomanValues` slide height, Megaman's 0.9 |

Other `DOWN` readers outside `Protoman.kt` (`PipePortal`, `GroundSlideBlocker`, `SpringBouncer`, `SpiderWeb`) need no
change: they read the input or the behavior type, not the character (scan section 4).

### The `TODO`s that name `060d`

| Place | Text | Done by |
|---|---|---|
| `Protoman.kt:224` | no guard on a ladder, where `DOWN` climbs down | step 7 (guard reads `CLIMBING`) |
| `Protoman.kt:441` | `DOWN` + `A` on the ground does nothing until the ground slide | step 5 (the slide) |
| `Protoman.kt:458` | on a ladder `B` is always the buster | step 6 (the `B` actuator) |

Each `TODO` goes when its rule is in.

## In scope

1. **Megaman slide tests on the old code** (`MegamanGroundSlideTest`), before the move. They pass on `master` code and
   pass unchanged after the move.
2. **The shared slide.** `players/PlayerGroundSlide.kt`: `GroundSlideParams` and
   `playerGroundSlideBehavior(player: MegaPlayer, params)`, with the rules of `GroundSlideBehavior.kt` unchanged.
   Megaman's `groundSlideBehavior()` becomes a thin call with his values, his Needle Spin block and a speed lambda
   that keeps water, `movementScalar` and the boost. `GROUND_SLIDE_BOOST` stays in Megaman's lambda, with a `TODO`
   for the loadout (item `024`).
3. **Proto Man's values** in `ProtomanValues`, with Megaman's numbers: slide cooldown, min and max time, speed, water
   speed, slide height 0.9, climb speed 5.
4. **Proto Man's slide.** `GROUND_SLIDING` in his behaviors (after `JUMPING`, before `CLIMBING`, as Megaman's order),
   `crouchHeight` = the slide height, `groundSliding` from the behavior.
   - Start from the crouch guard (`DOWN` held, then `A`): the shield drops at once, with no put-away.
   - `B` in the buster slot: a slide shot (`groundslide_shoot`), from a slide muzzle offset; the slide goes on.
   - `B` in the sabre slot: the slide ends and the normal ground slash starts. Under a low ceiling (he cannot stand),
     `B` does nothing.
   - The slide does not start while a slash is active.
   - If `DOWN` is still held when the slide ends, the crouch guard pulls out again (the full pull-out), after
     Megaman's 0.15 s slide-to-crouch delay (scope change 2).
5. **Proto Man's ladder.** `CLIMBING` through `playerClimbBehavior` with Megaman's rules (`feetGrabAllowed = { true }`,
   `holdStill = { buster.shooting }`, `leavesOnFloor = false`).
   - `DOWN` in the air grabs a ladder that the feet touch; elsewhere `DOWN` in the air is the air guard.
   - No guard on a ladder.
   - `B` on a ladder is always the buster, in both slots and with `UP` held; the `B` actuator takes the
     ladder-shot branch before every sabre path, so no sabre start is possible (step 7: no climb rule in
     `sabre.canStart`, which could never act; see `NOTES.md`). `SELECT` still changes the slot.
   - The ladder shot uses `PROTOMAN_BUSTER.climbOffset`, from the `climb_still_shoot` muzzle point.
6. **The `B` actuator order** (Claude's call, from the scan): `sabre.active` (chain), climbing (buster shot),
   sliding (`UP`+`B` in the buster slot or `B` in the sabre slot: the slide ends and the slash starts, nothing
   under a low ceiling; `B` in the buster slot: slide shot), crouch attack, guard, sabre slot, `UP` slash, standing
   shot.
7. **Art install.** A separate art agent draws the strips; the code waits for that art. Strips and frame counts:
   `groundslide` (2, scarf flutter, fan cells 8-9), `groundslide_shoot` (1), `climb` (2), `climb_still` (1),
   `climb_still_shoot` (1), `climb_finish` (1). On the ladder, a large white shield covers his back (fan cells 10-12).
   Each frame gets the v2 restyle. The feet sit on the floor row of the frame, so no code offset (`docs/WAYS_OF_WORKING.md`,
   "Fix art in the art").
8. **Test map.** 2 new rooms in `RefactorTest060.tmx`, past x 240, with the `level-builder` skill: `slide` and
   `ladder` (see `CONTRACT.md`).
9. **Tests** (`ProtomanTest`, `MegamanGroundSlideTest`, the smoke test for the strips) and `players/README.md` (the
   shared slide).

## Out of scope

- Slide after-images for Proto Man (Q9). `MegamanTrailSpriteV2` stays Megaman only.
- `GROUND_SLIDE_BOOST` for any player other than Megaman (Q4); the loadout is item `024`.
- Stacked rooms for a ladder room transition (Q10). The climb pose in a transition is covered by `poseKey`, which
  keeps the current key in a transition, and by a unit test.
- A sabre slash on a ladder or in a slide (no art; `060g`). Megaman's `climb_still_slash1` and `groundslide_slash1`
  stay his.
- X6 ladder ends (a climb-down pose, a climb while shooting) (Q8): Megaman's rules.
- A one-way platform drop on `DOWN` (Megaman has none).
- The shield, the damage reaction, stun and teleport art (`060e`); swim (`060g`); gravity rooms for Proto Man (none
  in `REFACTOR_TEST_060`; the 4 gravities of the slide and the climb are covered by unit tests).
- X: he has no `GROUND_SLIDING` and his own body, so the shared slide does not reach him. `PlayerClimb.kt` does not
  change.
- The scan's bug candidate (X's landing probe skips one-way blocks while `DOWN` is held,
  `entities/megamanx/MegamanXMoves.kt:110`): `docs/TODO.md`, "Bug candidates".

## New APIs and their first callers

| API | First callers |
|---|---|
| `players/PlayerGroundSlide.kt`: `playerGroundSlideBehavior(player: MegaPlayer, params: GroundSlideParams)` | `Megaman.groundSlideBehavior()` (replaces the receiver body), `Protoman.defineBehaviorsComponent` |
| `GroundSlideParams`: `minTime`, `maxTime`, `cooldown`, `speed: () -> Float`, `blocked: () -> Boolean` | the 2 calls above (Megaman: Needle Spin; Proto Man: `sabre.active`) |
| `GroundSlideParams.onStart` (only if `guardBroken` does not cover the guard drop and the crouch-shot pose end) | Proto Man; Megaman passes nothing. Dropped at step 5 if it has no work |
| `BusterParams.slideOffset` and `PlayerBuster.shoot(..., sliding = true)` | `Protoman` (`B` in the buster slot during a slide); X passes `null` |

No new API for the ladder: `ClimbParams` gets a third caller, and `BusterParams.climbOffset` gets a value.

## Affected areas and risks

Files (from the scan): `players/PlayerGroundSlide.kt` (new), `players/PlayerBuster.kt`, `players/README.md`,
`entities/megaman/behaviors/GroundSlideBehavior.kt`, `entities/megamanx/MegamanXBuster.kt` (one `null` param),
`entities/protoman/Protoman.kt`, `ProtomanValues.kt`, `ProtomanBuster.kt`, `ProtomanSabre.kt` (`canStart`),
`assets/sprites/sprite_sheets/Protoman_v2.*`, the Proto Man frame strips, `assets/tiled_maps/tmx/RefactorTest060.tmx`,
tests.

| Risk | Detail | Check |
|---|---|---|
| No slide tests today | the move has no safety net | `MegamanGroundSlideTest` first, on the old code (step 1) |
| Guard readers | the 5 `DOWN` readers act in the slide and on the ladder unless each learns the 2 states | a `ProtomanTest` case per reader (shield off, no crouch box, no crouch attack) |
| Shoot key throw | a shot in a pose with no `SHOOT_KEYS` entry throws | `ProtomanTest` shot in the slide and on the ladder |
| Climb offset | `climbOffset = null` throws on the first ladder shot | value from the art; ladder shot test |
| Momentum | `groundSliding = { false }` lets the post-action decay act in the slide | test that the slide speed holds |
| Behavior order | `JUMPING` before `GROUND_SLIDING` before `CLIMBING` | a Proto Man behavior order test, as `MegamanBehaviorsTest` |
| Slide-jump cut | inside the min time, a jump after the `DOWN` release loses its speed (inference, Megaman today) | inherited, not changed; Megaman's test records it |
| Body moves | the slide start and end moves fit a 1 x 1.5 body with a 0.9 slide height | Proto Man's body is 1 x 1.5; a test per gravity |
| Art wait | code steps 5 and 6 need the regions; the smoke test fails without them | step 4 before steps 5 and 6 |
| `GroundSlideBlocker` | one player's `DOWN` turns it off for all players | existing `TODO` (item `047`), not changed |
| README | `players/README.md` lists the shared moves | step 8 adds the slide |

## Overlap with other branches

- `mega-refactor-077a-intro-boss` (active): changes `players/PlayerDamageNegotiator.kt`, the launcher options and boss
  files. No file in common with this scope.
- No other active branch touches `players/`, `entities/megaman/`, `entities/protoman/` or `RefactorTest060.tmx`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O8-R1` no copy of `Megaman.kt` | helps | the slide moves to `players/`; Proto Man is a third `PlayerClimb` caller |
| `O8-R2` capabilities, not character checks | no effect | 0 new `is` checks; the blockers read the behavior type |
| `O8-R3` name the modes | no effect | single player; no mode change |
| `O8-R4` one definition per weapon | no effect | the boost stays an enhancement read in Megaman's lambda |
| `O1-R1` no global session state | waiting (`096`) | |
| `O1-R2` components, not inheritance | no effect | a function and a params class |
| `O1-R3` no new fragile order | no effect | 0 new listeners; slide and guard order is inside one frame (Behaviors before Updatables) |
| `O1-R4` typed state, not property keys | no effect | 0 new keys |
| `O1-R5` code only with a production caller | helps | each new API has its callers (table above) |
| `O2-R1` input only through `InputFrame` | no effect | reads `player.input` as today |
| `O2-R4` stable order, slot order | no effect | |
| `O2-R5` no logic assumes one player | no effect | the `GroundSlideBlocker` all-player rule stays (`TODO`, item `047`) |
| `O2-R7` gameplay not on rendering or audio | no effect | |
| `O5-R1` no per-frame allocation | no effect | no pool fetch in the slide; the climb uses `GameObjectPools` as today |
| `O6-R1` CI passes | no effect | |
| `O6-R2` named checks | helps | Megaman slide tests first; named `ProtomanTest` cases |
| `O7-R3` no level-identity checks | no effect | |
| `O9-R1` NES-style pad | no effect | `DOWN` + `A`, `UP`/`DOWN`, `B` only |
| `O2-R2`, `O2-R3`, `O2-R6`, `O3-R1`, `O9-R2` | waiting | |

## Decisions

All answered by the owner on 2026-11-14.

- **Shared rules.** Proto Man shares Megaman's slide and ladder rules (owner, at the branch start).
  - Q1: the slide moves to `players/` (`PlayerGroundSlide` with `GroundSlideParams`), called by Megaman and Proto Man.
    Megaman slide tests are written on the old code first, then Megaman must play the same.
  - Q2: Proto Man has his own constants in `ProtomanValues`, with Megaman's numbers.
  - Q4: `GROUND_SLIDE_BOOST` stays Megaman only.
  - Q8: Megaman's ladder rules (ends, shots on the ladder).
  - Q9: no slide after-images.
  - Q10: no stacked rooms for a ladder room transition.
- **`B` during a slide (Q3).**
  - Buster slot: a slide shot (new `groundslide_shoot` art, a slide muzzle offset), as Megaman.
  - Sabre slot: the slide ends and the normal ground slash starts. Under a low ceiling (he cannot stand), `B` does
    nothing.
- **`DOWN` in the air (Q7).** When he touches a ladder, `DOWN` grabs it (Megaman's rule). Elsewhere, `DOWN` in the
  air is the air guard.
- **Guard and slide (Q5, Q6).**
  - The slide starts from the crouch guard (`DOWN` held, then `A`). The shield drops at once, with no put-away.
  - If `DOWN` is still held when the slide ends, the crouch guard pulls out again: the full pull-out starts at once
    when the slide ends, as after a crouch attack (owner, 2026-11-14; not Megaman's 0.15 s slide-to-crouch delay).
    Replaced by scope change 2 (2026-11-15): the pull-out waits for Megaman's 0.15 s delay.
- **`UP`+`B` in a slide (owner, 2026-11-14).** In the buster slot, `UP`+`B` during a slide ends the slide and does
  the single slash, as on the ground (nothing under a low ceiling). `B` alone in the buster slot is the slide shot.
- **Slash and slide (owner, 2026-11-14).** The slide does not start while a slash is active
  (`blocked = { sabre.active }`).
- **Ladder.**
  - No guard on a ladder.
  - On a ladder, `B` is always the buster: the sabre is blocked while he climbs, as for X.
  - The ladder shot uses a climb offset.
- **Art (Q11).**
  - A large white shield over his back on the ladder (fan cells 10-12).
  - The slide has 2 frames of scarf flutter (fan cells 8-9).
  - Strips: `groundslide` (2), `groundslide_shoot` (1), `climb` (2), `climb_still` (1), `climb_still_shoot` (1),
    `climb_finish` (1).
  - A separate art agent draws them, and the code waits for that art.

Claude's calls (not owner decisions):

- `ClimbParams.blocked = { sabre.active }` (as X), so a slash does not snap to a ladder.

### Scope change 1 (2026-11-14, owner, during the play test)

- `jump_shoot` fired from the right (near) hand; redrawn to fire from the left (far) hand, as every other shoot frame
  (`NOTES.md`).
- Body size: the jump family (`jump`, `jump_shoot`, `guard_jump_start`, `guard_jump`, `guard_jump_end`, `sabre_air`,
  `sabre_air_land`) is thinner than `stand` and `run` (from the fan sheet's `jump.png`; audit in
  `temp/mega-refactor-060d-protoman-slide-ladder/proportions/AUDIT.md`). Owner: rebuild `jump` on the Megaman v3 jump
  body first, then carry it into the other 6 strips; `stand`/`run` are the reference size. Muzzle points, shield
  boxes and sabre arcs stay in place. The slide keeps Megaman's v3 slide body (no trim).

### Scope change 2 (2026-11-15, owner, during the play test)

- The slide end felt stiff: with `DOWN` held, the guard pulled out (and stopped him) on the first frame after the
  slide. Owner: use Megaman's slide-to-crouch delay. For 0.15 s after a slide (`GROUND_SLIDE_TO_CROUCH_DELAY`, the
  value of `MegamanValues`), the crouch guard, the air guard and the crouch attacks are blocked, so he keeps the
  slide-end momentum, and then the full pull-out starts. A `B` in the delay is the standing attack of the slot.

## Open questions

None.
