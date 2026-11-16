# Notes: mega-refactor-060d-protoman-slide-ladder

## Decisions

The owner decisions of 2026-11-14 are in `SCOPE.md`, "Decisions".

- `crouchHeight` for Proto Man changes from `BODY_HEIGHT` (`060c` `NOTES.md`) to the slide height 0.9. He has no
  `CROUCHING` behavior, so the value acts only for the slide.
- 2026-11-14 (owner, art round 1): in `groundslide_shoot` Proto Man fires with his right (front) hand, raised at
  chin height (muzzle 37, 34), and Megaman fires with his left. The slide shot leaves from Proto Man's own muzzle,
  so it is a little higher than Megaman's (Megaman: 0.15 tile over the slide body center, minus the 0.1 slide sprite
  offset). Accepted ("not necessarily an issue"). Step 5 records the height difference in tiles here.
- Step 4 (2026-11-14): the art is the owner's final set, round 1 of the art agent
  (`temp/mega-refactor-060d-protoman-slide-ladder/sprites/round1/`, `FINAL.md`), not `groundslide_shoot_v3arm.png`.
  The 6 strips are in `assets/sprites/frames/Protoman_v2/`, and the atlas is repacked with the `060c` packer (33
  regions; each region is byte-equal to its frame file). Muzzle points (x, y from the top left of the 48x48 frame):
  `groundslide_shoot` (37, 34), `climb_still_shoot` (39, 29).

- Steps 5-7 (2026-11-14), Claude's calls:
  - One break for both states: `crouchBlocked()` reads `GROUND_SLIDING` and `CLIMBING`. So `guardBroken()` (no shield,
    no pull-out, no put-away), `guardWanted()` and `crouchAttackWanted()` all see the slide and the ladder from one
    place. The slide break moved into step 5: with `DOWN` held at the slide start, the guard pull-out set the side
    speed to 0 on the first frame, so the slide did not move.
  - The crouch shot pose ends in a slide and on a ladder: `crouchShooting` is false in either state, and
    `updateGuard` clears `crouchShot`. The getter covers the first frame, where the crouch box pre-step runs before
    the updatables.
  - No `GroundSlideParams.onStart`: the guard break covers the shield drop and the crouch shot end.
  - No climb rule in `ProtomanSabre.canStart`: the `B` actuator takes the ladder branch before every sabre path, and
    `B` is the only path that starts the sabre, so the rule would be unreachable and no test could see it fail.
    `ClimbParams.blocked = { sabre.active }` keeps a slash from grabbing a ladder.
  - `B` in a slide (`slideAttack`): buster slot, `B` alone is the slide shot (also under a low ceiling); buster slot,
    `UP`+`B` ends the slide with the single slash (`chains = false`); sabre slot, `B` with or without `UP` ends the
    slide with the floor combo (`chains = true`), as the sabre slot on foot. A slash ends the slide through
    `GroundSlideParams.blocked = { sabre.active }`. Under a low ceiling (`HEAD_TOUCHING_BLOCK`, the rule that keeps
    the slide on) a slash does not start.
  - With `DOWN` held at the slide end the full ground pull-out starts, and the pull-out stops a run, so on ice the
    slide momentum stays only when `DOWN` is released at the end.
  - `SHOOT_KEYS` maps `climb`, `climb_still` and `climb_finish` to `climb_still_shoot`, so a shot pose on a ladder
    never throws (`climb_finish` shows with a shot only in a room transition frame, where the pose key keeps the
    current key).
  - Frame times: `groundslide` and `climb` 0.125 s per frame, loop (`GROUND_SLIDE_FRAME_TIME`, `CLIMB_FRAME_TIME`).
- Shot offsets (tiles, from the body center, by the `060c` rule: the `stand_shoot` muzzle (43, 35) is at
  `SHOT_OFFSET` (1, 0); 1 art px is 1/16 tile; the values are in `ProtomanValues`):
  - slide: x 1 + (37 - 43) / 16 = 0.625; y 0 - (34 - 35) / 16 + (1.5 - 0.9) / 2 = 0.3625 (the slide body is 0.9 high
    and keeps its feet, so its center is 0.3 tile lower);
  - ladder: x 1 + (39 - 43) / 16 = 0.75; y 0 - (29 - 35) / 16 - 0.25 = 0.125 (on a ladder the sprite has the air drop
    `AIR_SPRITE_OFFSET_Y`, as Megaman's). At a ladder foot with the feet over the floor the drop is off, so the shot
    leaves 0.25 tile under the muzzle there (`TODO` in `ProtomanBuster.kt`; Megaman has the same case).
- Slide shot height (gravity up, from the floor): Proto Man 0.45 + 0.3625 = 0.8125 tile; Megaman 0.45 + 0.15 - 0.1 =
  0.5 tile (`MegamanWeaponsHandler`: 0.15 over the slide body center, minus `GROUND_SLIDE_SPRITE_OFFSET_Y`). Proto
  Man's slide shot is 0.3125 tile (10 px at `PPM` 32, 5 art px) higher, and 0.125 tile further forward (x 0.625
  against 0.5).
- Test heap: the full `core:test` ran out of its 1 GB heap at the end of `ProtomanTest` after the new cases. A heap
  dump of `ProtomanTest` alone with a 300 MB heap held about 49 000 MockK invocations, each with a stack trace:
  a mock held in a static field keeps every call (inference: the mocked texture in the static region map of
  `PlayerSabreSlash`). `ProtomanTest` now calls `clearAllMocks(answers = false)` in `afterEach`; it then passes with
  300 MB alone. A `GameObjectPools.performNextReclaim()` per test step was tried and dropped: the full suite leaves a
  large backlog of pooled objects, and the first reclaim stalled the run.
- 2026-11-14 (owner, play test): `jump_shoot` (`060c` art) fired from the right (near) hand, every other shoot frame
  from the left (far) hand. Redrawn as version A of the art agent
  (`temp/mega-refactor-060d-protoman-slide-ladder/jump-shoot/`): `jump.png` with the `stand_shoot` buster arm, moved
  by (-1, -4). Muzzle (41, 31) to (43, 31): the x now equals the `stand_shoot` muzzle x (43) that `SHOT_OFFSET_X`
  uses, and the row is the same, so no offset changes. The upper body is 1 px lower than the old frame, the same as
  `jump`. Atlas repacked (33 regions, each byte-equal to its frame file).
- 2026-11-14 (owner, scope change 1): the jump family is rebuilt on version D of the art agent
  (`temp/mega-refactor-060d-protoman-slide-ladder/jump-resize/`, `jump-family/`): the Megaman v3 jump torso, belt and
  legs, with lean arms (the old `jump` arm thickness, owner: A's arms "too fat and chubby") and a clean armpit. Strips:
  `jump`, `jump_shoot`, `guard_jump_start`, `guard_jump`, `guard_jump_end`, `sabre_air`, `sabre_air_land` (on the
  `stand` legs). Kept by a verify script: the strip sizes and frame counts, the `jump_shoot` muzzle (43, 31), the
  shield pixels of the 3 guard strips, the sabre fist, arm and blade pixels (the arcs). So no code value changes. One
  foot pixel of `guard_jump` is inside the shield box rectangle (32, 39); the box is a code constant, not read from
  the pixels. Atlas repacked (33 regions, each byte-equal to its frame file).
- 2026-11-15 (owner, play test, bug): with `DOWN` released during a crouch shot, each new `B` fired another crouch
  shot (the `060c` rule "`B` fires another crouch shot, also after the release", `060c` `NOTES.md`, scope change 5),
  so a mashed `B` kept him crouched until the mashing stopped. Fix: `crouchAttackWanted()` needs `DOWN` held. A `B`
  after the release is now the standing shot, which ends the crouch shot pose. With no new `B`, the rest of the 0.3 s
  pose still holds him crouched (the `060c` rule, unchanged).
- 2026-11-15 (owner, scope change 2): Megaman's slide-to-crouch delay. A `Timer` of
  `GROUND_SLIDE_TO_CROUCH_DELAY` (0.15 s, Megaman's value) resets in each slide frame and runs in `updateGuard`; while
  it runs, `crouchBlocked()` is true (no crouch guard, air guard or crouch attack). It is set to its end at spawn, so a
  new life can guard at once (Megaman's timer starts unfinished). On ice with `DOWN` held, he now keeps the run speed
  for the delay before the pull-out stops him.

## Overlap with other branches

- `mega-refactor-077a-intro-boss` (active, 2026-11-14): changes `players/PlayerDamageNegotiator.kt`, the launch
  options and boss files. No file in common with this scope.
- `mega-refactor-024a-weapon-defs` (active, found at the merge, 2026-11-15): changes the slide block line in
  `megaman/behaviors/GroundSlideBehavior.kt` to `currentWeaponPrevents(BehaviorType.GROUND_SLIDING)`. `060d` moved that
  line into `GroundSlideParams.blocked`. Owner: merge `060d` first; when `024a` takes `master`, the resolution is
  `blocked = { currentWeaponPrevents(BehaviorType.GROUND_SLIDING) }` (same behavior).
