# Notes: mega-refactor-060m-protoman-moves

## Decisions

### The `A` task part (D1, step 3, 2026-11-16)

- **A component, not a field on `MegaPlayer`.** `PlayerAButtonComponent` (`players/PlayerAButton.kt`) holds the
  task (`AButtonTask`: `JUMP`, `SWIM`, `AIR_DASH`) and `airDashConsumed` (one air dash for each jump). A class that
  has the moves adds it in `init()`. X has no `A` task (his dash is on `SELECT`), so the state does not go on
  `MegaPlayer` (`players/README.md`: the base holds only what every class shares; `O1-R2`: components, not new
  inheritance). The extension `aButtonTask` reads it with `require`, so a class with no component fails fast.
- **Named rule functions, called by the class.** Each rule that sets the task in more than a plain value is a
  function in `PlayerAButton.kt`: `resetAButtonTask` (spawn), `updateAButtonTask` (each frame, in the updatables),
  `aButtonTaskOnAirDashStart`, `aButtonTaskOnWater(state)` (body water hook), `aButtonTaskOnGravitySwitch`,
  `offGroundAButtonTask()` (swim in water, else air dash: feet leave a block in the air, ladder grab and end) and
  `setAirDashTaskOutOfWater` (feet bounce, wall slide end). The plain sets stay plain (`aButtonTask = JUMP` at a
  feet landing and in the wall slide; `AIR_DASH` at a teleport end). The class keeps the call points, because the
  hooks (body params, move params, teleport) are its own; `playerBody` does not call them, so a class with no `A`
  task (X) has no hidden rule.
- **The move params keep the task check.** `JumpParams.canStart`, `AirDashParams.canStart` and the new
  `SwimParams.canStart` read the task in the class (Megaman passes `aButtonTask == ...`), as before. The shared moves
  do not read the component, so X's jump and dash do not need it.
- **`AButtonTask` moved** from `entities/megaman/constants/` to `players/`; `MegamanAButtonTask.kt` is gone (its body
  is `updateAButtonTask`).
- **Every new API has Megaman as its caller now.** Proto Man (step 5) will call: `PlayerAButtonComponent` (in his
  `init()`), `resetAButtonTask` (his spawn hook), `updateAButtonTask` (his updatables), `aButtonTask` in his jump,
  air dash and swim `canStart`, `aButtonTaskOnAirDashStart`, `aButtonTaskOnWater` (his body water hook),
  `offGroundAButtonTask` (his feet block end and ladder), `setAirDashTaskOutOfWater` (feet bounce, wall slide end),
  `aButtonTaskOnGravitySwitch` (if he gets a `GravityDeviceComponent`), and `playerSwimBehavior` with his
  `SwimParams`.

### The swim (step 3)

- `players/PlayerSwim.kt`: `SwimParams(time, speed, blocked, canStart)` and `playerSwimBehavior`. Megaman's
  `swimBehavior()` is a thin call: `SWIM_TIMER`, `swimVel` with his `JUMP_BOOST` scale, his weapon block
  (`currentWeaponPrevents(SWIMMING)`), his task check.
- One difference in the arithmetic: the old code scaled the stroke by `PPM` and then by the boost; the new code
  scales the speed by the boost and then by `PPM`. The result is the same up to float rounding.
- The stroke keeps the old signs in `DOWN` and `RIGHT` gravity (bug candidate in `TODO.md`), with a `TODO` at the
  code.

### The tests (step 2)

- `MegamanSwimTest` (19) and `MegamanAButtonTaskTest` (31) drive a fully spawned Megaman (`MegamanSpawnedRig`:
  `init()`, `onSpawn()`, the real body, behaviors and receivers; mocked atlas, weapons handler and engine). The rig
  calls the frame rule `updateAButtonTask()` after the behaviors, as the updatables do; the full updatables list
  needs level bounds that the test services do not have.
- Step 3 changed the 2 step 2 specs only in imports. Older Megaman test rigs changed in their setup: the rule
  components helper adds `PlayerAButtonComponent`, and 3 rigs set the task after it (`MegamanJumpTest`,
  `MegamanGroundSlideTest`, `MegamanMovesRig`); `MegamanBodyFixturesTest` and `MegamanAirDashDirectionTest` add the
  component.

### Mutation results

Runner and lists: `temp/mega-refactor-060m-protoman-moves/` (`mutate.py`, `mutations-*.json`, `mutations-*.txt`).

- Old code (step 2): 54 mutations of the swim, the frame rule, the body hooks, the spawn, the teleport, the gravity
  switch and the jump, air dash, wall slide and climb task lines. 51 killed. 2 of them were first missed and killed after a
  new case each (`J1`: the jump's task check; `C3`: the task at the ladder end, hidden by the frame rule while the
  dash is unused). 3 survive as equivalent mutants: the swim's `damaged` check (`playerCanMove` already has
  `!damaged`), and the swim timer reset in `init()` alone or in `end()` alone (each covers the other; both removed is
  killed).
- New code (step 3): a run was started before the owner's rule below and stopped after 30 of 33 mutations: 29
  killed; 1 survived (`n-M1`: Megaman's own call of `updateAButtonTask` in his updatables, because the rig calls the
  frame rule itself; the call is the old line moved, not a new rule). The stopped run left 1 mutation in
  `AirDashBehavior.kt` (the `aButtonTaskOnAirDashStart()` call removed); it was restored by hand and checked with
  the diff and the full checks.
- Owner rule (2026-11-16): a move "rule for rule" needs no new mutation runs; the step 2 tests on the old code are
  the proof. Later loops batch 3-6 independent mutations per run and run only the target specs.

### Proto Man's moves (steps 5-7, 2026-11-16)

Claude's calls where the scope did not fix the rule:

- **The camera turn is one shared function.** `turnBodyAndCamera(value, onCameraTurnStart)` in
  `players/PlayerGravity.kt` holds Megaman's setter body (smooth follow, the `START_GAME_CAM_ROTATION` event, the
  `MOVEMENT` lock, gravity off, velocity zero). Megaman passes his jetpack reset as `onCameraTurnStart`; Proto Man
  passes nothing. Megaman's setter changed rule for rule (`players/README.md`, "One copy"). The `TODO` (item `058`)
  moved with it.
- **A slash and the wall slide:** a floor or air slash blocks the wall slide (`blocked`), so a slash never turns into
  a wall slash; the wall slide starts after the slash. The wall slash (`UP`+`B` on the wall) keeps him on the wall
  with the side released, as X (`holdsWithoutInput`). Proto Man keeps `endOnStateChange = true`: when the wall slide
  ends (off the wall, a wall jump, a landing), the wall slash ends.
- **A slash and the air dash:** a slash blocks the start of a dash (`canStart`, as X). A dash does not end a slash:
  `UP`+`B` in a dash starts the air slash, and the dash goes on (D2, and the same as the shielded dash, D3).
- **The swim and a slash:** `SwimParams.blocked = { false }`: a stroke in a slash is allowed, as the jump.
- **Facing on the wall:** the shared rules: the wall slide faces him away from the wall, and the run button keeps him
  facing away while he holds the side to the wall (`PlayerRun`). So `B` fires away from the wall, as Megaman.
- **The shield in the moves:** the box is in front of him (his facing), so on the wall it is away from the wall. Wall
  slide and water: the jump box (`wallslide_guard` has the `guard_jump` shield pixels unchanged). Air dash: a new box
  `GUARD_AIR_DASH_SHIELD_BOX` (35, 27, 41, 44), read from the `airdash_guard` pixels. The air sprite drop applies (all
  3 moves are in the air).
- **No put-away on the wall and in a dash:** the art has no put-away strip there, so the shield goes away at once
  (`putAwayCut` reads the wall slide and the air dash; `TODO` for `060k`). In water the `guard_jump_end` put-away
  plays.
- **Guard breaks:** the slide and the ladder still break the guard (`crouchBlocked()`); the wall slide, the air dash
  and the swim do not.
- **The air sprite drop stays** for the 3 moves (Megaman draws his wall slide and air dash with no drop). The new
  strips have the feet on the frame bottom, as `jump`; touch-ups go in `060k`.
- **The `A` task calls:** all the calls of the step 3 list, and `GravityDeviceComponent` (a gravity switch device sets
  his task, as Megaman's). No teleport line: Megaman's teleport sets `AIR_DASH` at the end and resets a dash at the
  start; Proto Man has the default teleport component, and the air dash already stops while teleporting (follow-up in
  `TODO.md`).
- **`BusterParams`:** 3 new offsets (`wallSlideOffset`, `airDashOffset`, `swimOffset`), each with its exception; X
  passes `null`. A shot in a swim stroke uses the swim offset; out of a stroke in water the jump shot.
- **Not in `ProtomanTest`:** the `wallJumpTimer` delay after a spawn (Megaman's rule, copied) and the calls in the
  updatables list (`updateAButtonTask`, `updateWallTimers`), because the test rig calls them itself.

### Shot offsets of the new poses (step 7)

The `060c` rule: the `stand_shoot` muzzle (43, 35) is at `SHOT_OFFSET` (1, 0); 1 art px is 1/16 tile (3 tiles / 48
px); the 3 poses are in the air, so the sprite drop `AIR_SPRITE_OFFSET_Y` (-0.25) is added to y (as the ladder shot).

| Pose | Muzzle | x | y |
|---|---|---|---|
| `wallslide_shoot` | (42, 31) | 1 + (42 - 43) / 16 = 0.9375 | -(31 - 35) / 16 - 0.25 = 0 |
| `airdash_shoot` | (45, 36) | 1 + (45 - 43) / 16 = 1.125 | -(36 - 35) / 16 - 0.25 = -0.3125 |
| `swim_shoot` | (42, 31) | 0.9375 | 0 |

The wall slash: `sabre_wall_arc` is `sabre_air_arc` 1 px lower, so the hit boxes are the air ones with the y from the
arc bottom 1 px less (from the bounding box of each arc frame); arc size 102 x 82 and `arcCenterAboveBody` -5 as the
air slash (art notes).

### Mutation results (steps 5-7)

Runner and lists: `temp/mega-refactor-060m-protoman-moves/mutations-567/` (main checkout). 12 runs (7 batches of 5
or 6, and 5 single reruns to split cases that 2 mutations failed). 41 mutations of new rules; 40 killed. 1 survived
(`E3`: no put-away start in the wall slide and the dash): the put-away cut already ends it at once (the side is held
on the wall; `putAwayCut` reads the dash), so the condition was redundant and was removed.

## Overlap with other branches
