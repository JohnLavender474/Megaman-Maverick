# Contract: mega-refactor-081g-x-crouch-ladder

Status: approved (2026-11-13)

## Intended changes

- X crouches on `DOWN` on the ground, and can shoot, turn, jump and dash from the crouch (D3).
- X grabs a ladder (`UP` at the head, `DOWN` on the top, `UP` in the air), climbs, shoots and turns on it, and leaves
  it at the top, at the bottom, with a jump, or past its end (D1, D2).
- X's one-way pass rule changes (D4).

## Must not change

- Megaman's climb: grab, speed, the camera smooth follow, shoot stop, `A` to let go, the 4 gravity directions, the
  `NEEDLE_SPIN` rule, the `A` button task, the land sound.
- Megaman's crouch.
- X's other moves (run, jump, wall slide, dashes, sabre, buster).

## Automated checks

- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` (JDK 17).
- `MegamanClimbTest` (Megaman's climb rules, written on the old code before the move and unchanged after it),
  `MegamanXClimbCrouchTest` (X's grabs, climb, exits, crouch), and the 2 rewritten one-way tests in `MegamanXTest`.

## Manual checks

Experimental (`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-081g-x-crouch-ladder`):

- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room ladder`: X grabs the right ladder (it stands on the floor) from the floor (`UP`),
  climbs, turns and shoots on it, climbs over the top (a short kneel), grabs it again from the top (`DOWN`, a short
  turn pose), climbs down and stands on the floor (`DOWN`); `DOWN` held at the ladder foot crouches. Jump at the left ladder (it hangs 3 tiles over the floor) and hold `UP` to grab it in
  the air (from the top of the jump); `A` lets go with no jump.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room crouch`: X crouches at once on `DOWN`, turns, shoots low; the
  `SniperJoe` shot passes over the crouch and hits a standing X; jump and dash (`SELECT`) leave the crouch.
- `./run-game-alpha.sh --level X6_INTRO --room room1`: X climbs the stage ladder; `DOWN` no longer drops him through
  one-way blocks.

Control:

- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room flat`: X runs, jumps, dashes and slashes as before.
- `./run-game-alpha.sh --level INTRO_STAGE --room room3`: Megaman climbs, stops while he shoots, lets go with `A`,
  climbs over the top, and crouches as before.
