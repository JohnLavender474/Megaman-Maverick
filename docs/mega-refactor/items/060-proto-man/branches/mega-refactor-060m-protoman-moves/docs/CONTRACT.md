# Contract: mega-refactor-060m-protoman-moves

- Status: approved
- Approved: 2026-11-16

## Intended changes

- Proto Man wall slides (with the wall jump), air dashes and swims, with Megaman's rules and numbers. No Rush Jet.
- What `A` does in the air and in water follows Megaman's rule (jump, air dash, swim), through one shared part (D1).
- `B` shoots on a wall (away from it), in a dash and in water. `UP`+`B` is the wall slash on a wall and the air slash
  in a dash and in water (D2).
- `DOWN` guards in all 3 moves: the wall guard; a shielded air dash (the dash goes on, shield in front); the air guard
  in water (D3).
- He turns the camera on a gravity change, as Megaman.
- New frames for these moves, from one Fable 5.2 art round (D4).

## Must not change

- Megaman's wall slide, wall jump, air dash, swim and `A` rules (tests first, on the old code).
- X's wall slide, wall kick and air dash.
- Proto Man's run, jump, slide, ladder, guard, buster and sabre outside the new moves.

## Automated checks

- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`.
- Megaman tests of the swim and the `A` rule, green on the old code before the move.
- `ProtomanTest`: wall slide start, hold and end, the wall jump; air dash start, one per jump, the end; swim start in
  water, not out of it; `A` order (D1); attacks and guard in each move (D2, D3); the camera turn on a gravity change.
- Each new case is seen to fail on a mutation of its rule.

## Manual checks

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-060m-protoman-moves`.

Experimental:

- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room wall`: wall slide on both walls, wall jump up a shaft,
  `B` shoots away from the wall, `UP`+`B` is the wall
  slash, `DOWN` raises the wall guard; the new frames.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room airdash`: one air dash per jump over the gaps; `B` shoots in
  the dash, `UP`+`B` is the air slash; `DOWN` in a dash keeps dashing with the shield in front.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room water`: swim strokes, `B` and `UP`+`B` in water, `DOWN` is the
  air guard; a jump out of the water.

Control:

- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room jump`: jump and fall as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room walls`: X's wall slide, wall kick and dash as before.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room intro_room`: Megaman's wall slide on both sides of the shaft, the
  wall jump up it, one air dash per jump at the top, as before.
- `./run-game-alpha.sh --level GLACIER_MAN --room room11`: Megaman's swim strokes, `A` in water swims (not an air
  dash), an air dash after a jump out of the water, as before.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room2`: Megaman's gravity switches (`GravitySwitcharoo`): a smooth
  camera turn with no move during it; after a switch on the ground `A` jumps, in the air `A` air dashes, as before.
  The room has spikes and death sensors.
