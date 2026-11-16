# Plan: mega-refactor-060d-protoman-slide-ladder

Scope and contract: approved 2026-11-14. Each step ends with its check. Steps 1 to 3 do not need the art; steps 5
and 6 wait for step 4 (the art agent's strips).

New APIs and their first callers: see `SCOPE.md`, "New APIs and their first callers".

## Steps

### 1. Megaman slide tests on the old code

- Changes: `core/src/test/.../megaman/MegamanGroundSlideTest.kt`, one case per rule of the old
  `GroundSlideBehavior` (the list in `CONTRACT.md`). Drive the public flow: a real `Megaman`, input through
  `InputFrame`, reads through public state (`docs/CODE_STYLE.md`, "Tests").
- Check: `./gradlew core:test` green on the old code; each case seen to fail once with its rule removed (then the
  rule restored). Commit the tests before step 2.
- Done: yes (2026-11-14)

### 2. Move the slide to `players/`, Megaman plays the same

- Changes: `players/PlayerGroundSlide.kt` with `GroundSlideParams` (`minTime`, `maxTime`, `cooldown`,
  `speed: () -> Float`, `blocked: () -> Boolean`) and `playerGroundSlideBehavior(player: MegaPlayer, params)`; the
  rules move unchanged. `Megaman.groundSlideBehavior()` becomes a thin call: his `MegamanValues` timers, Needle Spin
  in `blocked`, and a speed lambda with water, `movementScalar` and the boost. `TODO` at the boost read for the
  loadout (item `024`). `players/README.md` gets the shared slide.
- Check: `MegamanGroundSlideTest` passes unchanged; `MegamanBehaviorsTest` and the other Megaman tests pass;
  `./gradlew core:test`. No change to `MegamanGroundSlideTest` in this step.
- Done: yes (2026-11-14)

### 3. Test map rooms

- Changes: a subagent adds 2 rooms to `assets/tiled_maps/tmx/RefactorTest060.tmx` past x 240, with the
  `level-builder` skill (brief: `docs/WAYS_OF_WORKING.md`, "Levels"; the test-map rules; no game launch):
  - `slide`: a player spawn, a flat run, a low ceiling with a 1-tile gap over at least 8 tiles (longer than one
    slide of 5 tiles), a `GroundSlideBlocker` with the shape of the `REFACTOR_TEST_031` one, an ice patch, a sand
    patch;
  - `ladder`: a player spawn, a `Ladder` from the floor to a ledge with a `LadderTop`, a `Ladder` that hangs over the
    floor (jump reach), a wall for ladder shots. No enemies.
  - Painted `tiles` on every block cell; the map width grows to fit.
- Check: the `level-analyzer` reads both rooms, each with a player spawn and the listed objects; the spawner names
  match classes in the packages of their layers.
- Done: yes (2026-11-14)

### 4. Art install (after the art agent)

- Changes: the art agent's 6 strips (`groundslide` 2, `groundslide_shoot` 1, `climb` 2, `climb_still` 1,
  `climb_still_shoot` 1, `climb_finish` 1), owner-approved, go into the Proto Man frame strips and
  `assets/sprites/sprite_sheets/Protoman_v2.txt`/`.png`. Read the slide and climb muzzle points from
  `groundslide_shoot` and `climb_still_shoot`. Feet on the floor row of the frame; no code offset.
- Check: the atlas lists each region with its frame count; the muzzle points are written in `docs/NOTES.md`.
- Done: yes (2026-11-14)

### 5. Proto Man's slide

- Changes: `ProtomanValues` slide values (Megaman's numbers) and `SLIDE_HEIGHT` 0.9; `crouchHeight = SLIDE_HEIGHT`
  (record the change of the `060c` decision in `docs/NOTES.md`); `GROUND_SLIDING` through
  `playerGroundSlideBehavior` with `blocked = { sabre.active }`, after `JUMPING`; `groundSliding` from the behavior;
  remove the `TODO` at `downBlocksGroundJump`. `BusterParams.slideOffset` and `PlayerBuster.shoot(..., sliding = true)`;
  `PROTOMAN_BUSTER.slideOffset` from step 4; X passes `null`. Animation keys `groundslide` and `groundslide_shoot`
  (`SHOOT_KEYS`, `BASE_POSES`).
- Check: the slide cases of `ProtomanTest` that do not need the guard or `B` (start, numbers, low ceiling, ice,
  sand, momentum, behavior order); `./gradlew core:test core:smokeTest`.
- Done: yes (2026-11-14)

### 6. Proto Man's ladder

- Changes: `ProtomanValues.CLIMB_SPEED` 5; `CLIMBING` through `playerClimbBehavior` with `ClimbParams`
  (`blocked = { sabre.active }`, `feetGrabAllowed = { true }`, `holdStill = { buster.shooting }`,
  `leavesOnFloor = false`, `onStart` sets `canMakeLandSound = false`); `PROTOMAN_BUSTER.climbOffset` from step 4;
  `sabre.canStart` false while climbing; animation keys `climb`, `climb_still`, `climb_still_shoot`, `climb_finish`
  (Megaman's key rule).
- Check: the ladder cases of `ProtomanTest` for grab, climb, ends, `A`, poses and the room transition;
  `./gradlew core:test core:smokeTest`.
- Done: yes (2026-11-14)

### 7. The `DOWN` reader changes

- Changes in `Protoman.kt`:
  - `guardBroken()` reads `GROUND_SLIDING` and `CLIMBING`: no guard, no shield and no put-away in a slide or on a
    ladder; remove the guard `TODO`;
  - `crouchAttackWanted()` is false in a slide and on a ladder;
  - the crouch box stays off in a slide;
  - the `B` actuator order: chain, ladder shot (`climbing = true`), slide (`sliding = true` shot in the buster slot;
    in the sabre slot the slash, unless `HEAD_TOUCHING_BLOCK`), crouch attack, guard, sabre slot, `UP` slash,
    standing shot; remove the `B` `TODO`;
  - the crouch-shot pose ends at a slide start (`GroundSlideParams.onStart` only if `guardBroken` does not cover it;
    else no `onStart`).
- Check: the guard, `B` and crouch cases of `ProtomanTest` (slide and ladder); the existing guard and crouch attack
  cases pass unchanged; `grep -n "060d" Protoman.kt` finds no `TODO`.
- Done: yes (2026-11-14)

### 8. Tests and docs

- Changes: the rest of the `ProtomanTest` cases in `CONTRACT.md`; the smoke test frame counts for the 6 strips; each
  new rule seen to fail once; `players/README.md` (the slide, Proto Man as the third climb caller); branch
  `docs/NOTES.md` (decisions, muzzle points, the `crouchHeight` change).
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`.
- Done: yes (2026-11-14)

### 9. Review before the play test

- Changes: the automated checks first; then review subagents with the branch diff, `CONTRACT.md`,
  `docs/CODE_STYLE.md` and the rule "do not launch the game": one for the diff, one for the `REFACTOR_TEST_060`
  checks (`slide`, `ladder`, `shield`), one for the control checks (`REFACTOR_TEST_031`, `REFACTOR_TEST_068`,
  `INTRO_STAGE`, `REFACTOR_TEST_081`).
- Check: each finding fixed and reviewed again; the result in the process log; the play checks given to the owner
  as a list; Diffity open.
- Done: yes (2026-11-14)
