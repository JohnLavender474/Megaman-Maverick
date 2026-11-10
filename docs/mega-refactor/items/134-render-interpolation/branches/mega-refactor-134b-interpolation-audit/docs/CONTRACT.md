# Contract: mega-refactor-134b-interpolation-audit

The behavior that this branch changes on purpose, the behavior that it must not change, and how to check them. The
owner approves the brief, not this file. Do the checks before the branch merges, and write the result in the branch
process log.

- Status: approved
- Approved: 2026-11-09

## Automated checks

- [ ] `./gradlew engine:test` passes, with new tests: a sprite of an entity with a link gets the linked body's offset;
  a link that gives `null` falls back to the entity's own body; an entity with no body and a link gets the linked
  offset; `restartStep()` outside a step gives a step move of 0; inside a step the move after it counts and the move
  before it does not.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.
- [ ] The review finds no gameplay code that reads the draw link or the step state (`O2-R7`).

## Manual playthrough

Worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-134b-interpolation-audit`

The test map rooms are added to `RefactorTest134.tmx` in the first plan step (names can change there). Experimental
checks first:

- `./run-game-alpha.sh --level WILY_STAGE_2 --room room9`: the room before the `GutsTank` boss. The fist stays fixed
  on the tank while the tank rolls; it hitched before.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room attach --interpolation true`: the `FireMet` flame on its
  head, the `PetitDevil` children, the `Electrocutie` parts and a `Spike` on a moving platform stay fixed on their
  parents. Press `I` to compare with the stepped draw.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room pipe`: enter a `PipePortal`. No back-jump of Megaman at the
  pipe mouth.
- `./run-game-alpha.sh --level INTRO_STAGE --room room1 --weapons PRECIOUS_GUARD`: the gem ring stays even around
  Megaman in a walk, a slide and a fast fall.
- `./run-game-alpha.sh --level X6_INTRO --room room1`: X's Z-sabre slash (`UP` + `B`) while X walks, jumps and
  falls: the arc stays on X.
- `./run-game-alpha.sh --level REACTOR_MAN --room room14`: the room before the boss. The 2 projectiles that
  `ReactorMan` holds stay in his hands while he moves, and fly free when he throws them.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room7`: the room before the boss. The shield gems orbit her
  evenly.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: the room before the `BigAssMaverickRobot` boss. Go
  through the gate: the robot falls in from above. The hands and the robot body stay on the robot in the fall (look
  for a hitch, not an exact fit).
- `./run-game-debug.sh --level REFACTOR_TEST_134 --room open --debugShapes`: walk across the room. The block
  outlines stay on the tiles while the view moves.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room open --interpolation true`: pause, press `I`, resume. Nothing
  changes during the pause.

Control checks:

- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room open --interpolation false`: the draw is the same as
  `master` with the flag off.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room3`: the `CapsuleTeleporter` and the `GravitySwitcharoo`s (no
  change).
- `./run-game-debug.sh --level REFACTOR_TEST_134 --room open --interpolation true`: walk for about 30 s; Claude reads
  the `*-diagnostics.txt` file and compares it with the `134a` run (`O5-R2`).

## Behaviors to check

- A child that detaches (the `GutsTank` fist at launch, a thrown `ReactorMan` projectile, a released gem) moves on
  its own at once; it does not keep the parent's offset.
- Death and respawn (`REFACTOR_TEST_134 --room open`, the spike): no change from `134a`.

## Intended changes

- Draw only. A body with a draw parent is drawn with the parent's draw offset; a snapped body is drawn at its new
  position in the frame of the snap; the shapes draw with the camera draw offset; the `I` key does nothing while
  paused.
- No gameplay change: positions, collisions, carries and the gameplay camera stay the same.
- Known and not fixed (backlog, per D3): blocks that a player rides and that move outside the steps or by a catch-up
  velocity; bodies that move once per frame outside the steps (for example `D1000`); the `BigAssMaverickRobotHand`
  orbs.
