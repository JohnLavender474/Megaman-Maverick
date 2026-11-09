# Contract: mega-refactor-134a-draw-interpolation

- Status: approved
- Approved: 2026-11-09 (owner, at the brief)

## Automated checks

- [ ] `./gradlew engine:test` passes, with the new tests: `Body` step state and draw offset (serial, guard, reset),
  `WorldSystem` alpha (0..1, 1 at the cap, frozen with the world off), the evenness test (constant velocity, step
  1/150, frames of 1/60 s, and frames with ±1 ms jitter), and `GameSprite` (offset applied only during the draw; no
  drift over 100 frames).
- [ ] `./gradlew core:test` passes, with the draw camera tests: offset = focus offset in an open room, 0 at a room
  edge, 0 in a transition, 0 on the axis of a running scroll, 0 in smooth follow.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.
- [ ] The review finds no gameplay read of a drawn position (`O2-R7`) and no per-frame allocation in the new code
  (`O5-R1`).

## Manual playthrough

Worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-134a-draw-interpolation`

The rooms are in `RefactorTest134.tmx` (built in the first plan step). Experimental checks first:

- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room open --interpolation true`: walk, slide and jump across the
  room. The level scrolls evenly, and Megaman does not shake against the view. Toggle the A/B switch (D3) and compare
  it with the stepped draw.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room shaft --interpolation true`: fall down the shaft at full
  speed. The view moves evenly; Megaman stays still on the screen.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room edge --interpolation true`: walk into the walls of a room of
  one view. The view stays still; Megaman moves evenly up to the wall.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room scroll --interpolation true`: a camera scroll section. The
  scroll axis is as smooth as today; climb and drop on the platforms: the other axis follows evenly; the edge blocks
  hold.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room transition --interpolation true`: go through the room exit
  and back. No jump of Megaman or the view at the start or the end of the pan.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room open --interpolation true --performance low`: the same as the
  first check at 45 fps.
- `./run-game-test-coop.sh --level REFACTOR_TEST_134 --room open --interpolation true`: 2 players; the shared view
  moves evenly (rule of D2).
- `./run-game-debug.sh --level REFACTOR_TEST_134 --room open --interpolation false`, then the same with `true`: walk
  for about 30 s in each run. Claude reads the 2 `*-diagnostics.txt` files (`O5-R2`).

Control checks:

- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room open`: with the default (flag off) the draw is the same as
  today.
- `./run-game-alpha.sh --level X6_INTRO --room room1 --interpolation true`: X walks, dashes and jumps past the 2
  `Junkroid`s and the `HoverGunner`; a short sanity pass of a real level with enemies and projectiles.

Real levels (interpolation is on by default; found by a subagent on 2026-11-09):

- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: the room before the `BigAssMaverickRobot` boss. The arm
  orbs against the hand; stand on the hand's platform while it turns: does Megaman shake against it? (`134b` case)
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room9`: the room before the `GutsTank` boss. The fist against the
  tank while it rolls; stand on the tank. (`134b` case)
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room10`: the room before the `ElecDevil` boss. A 1-frame hop where a
  flying piece joins the other half?
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room8`: the nearest room with a spawn before the Wily room. The
  lasers under the plane; the tentacle roots against the capsule.
- `./run-game-alpha.sh --level INTRO_STAGE --room room1 --weapons PRECIOUS_GUARD`: the gem ring against Megaman in a
  walk, a slide and a fast fall. Also: the 2 `HeliMet`s stop with no pop; the slide and air-dash after-images; the
  beam-in at the start and after a death in a pit.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room2`: ride the `PropellerPlatform`s (control: smooth).
- `./run-game-alpha.sh --level MOON_MAN --room room4`: ride a `RocketPlatform` (control: smooth).
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room3`: the `CapsuleTeleporter` (a pop on arrival?), and the
  `GravitySwitcharoo`s: walk and fall in a turned room.
- `./run-game-alpha.sh --level MOON_MAN --room room6`: drop into `room7`, take the `PortalHopper` to `room8` (turned
  180 degrees). A streak at the exit?
- `./run-game-alpha.sh --level SMB3_1 --room room2`: walk left to the `PipePortal`s in `room1`. A pop at the pipe exit?
  (Not sure that `room2` connects to `room1`.)
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room14`: the room before the boss. Win, and watch the beam-out.
- `./run-game-alpha.sh --level X6_INTRO --room room1`: walk from `floor` into `corridor`, `s_section` and
  `top_corridor` (camera zones with the default ease).

## Behaviors to check

- Pause and resume with the flag on: no jump of the sprites or the view.
- Death and respawn with the flag on (`--room open`, walk into the spike in the right third of the room): the
  respawn shows Megaman at the spawn at once (a one-cycle offset for 1 frame at most).

## Intended changes

- By default (owner, 2026-11-09; `--interpolation false` turns it off): sprites of bodies and the game camera are
  drawn between the last 2 world cycles. The drawn positions are up to one cycle (6.7 ms at `MEDIUM`) behind the
  simulation.
- With `--interpolation false`: no change from `master`.
- Known with the flag on, for `134b`: bodies that follow another body outside the world cycles (riders, boss
  parts) can jitter against their parent; a teleport inside a cycle below the guard can streak for 1 frame; an
  entity with sprites and no body gets offset 0, so a sprite that it places on another entity's body can be up to 1
  step away from that body; the shape renderer draws with the stepped camera, so `--debugShapes` outlines and the
  `WhiteArrowPool` outline can be up to 1 step away from the sprites.
- Accepted for this merge (owner, 2026-11-09): the hitches of these known cases in normal play, for example the
  `GutsTank` fist against the tank. `134b` fixes them.
