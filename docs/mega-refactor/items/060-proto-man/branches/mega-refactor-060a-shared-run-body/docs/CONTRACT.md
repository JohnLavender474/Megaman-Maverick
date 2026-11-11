# Contract: mega-refactor-060a-shared-run-body

- Status: approved
- Approved: 2026-11-10 (brief)

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with `MegamanRunTest`, `MegamanJumpTest`, `MegamanBehaviorsTest`,
  `MegamanWallSlideTest` and `MegamanAirDashTest` unchanged, except 2 call lines in `MegamanRunTest`
  (`megaman.run(...)` becomes `megaman.playerRun(..., megaman.runParams)`; step 3, 2026-11-10).
- [ ] New tests pass: the shared run, the gravity table per direction, Megaman's body fixtures (types, sizes,
  order) equal to the values before the branch, and the body hooks (properties, head and feet bounce, side stick).
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

All checks are control checks: Megaman plays the same. Run from the branch worktree.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: run both ways, the start ramp, release, run into a
  wall, jump and steer, slide under a 1-tile gap, crouch. Go into the water: slower fall, swim, run speed.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: use the 4 gravity switches; run, jump and fall in
  each gravity direction. (Changed 2026-11-10 at the review: `gravity_switch` has only up and down switches.)
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: run and stop on ice; wall slide on an ice wall.
- `./run-game-alpha.sh --level MOON_MAN --room room1`: the run speed is the low Moon speed.
- `./run-game-alpha.sh --level SMB3_1 --room room2`: hit a block from below: the bump sound plays. The land sound plays.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim --weapons RODENT_CLAWS,NEEDLE_SPIN,AXE_SWINGER`:
  Rodent Claws slash stops the run and turns Mega; Needle Spin hurts the `Met` and blocks shots; the axe shield
  reflects the `SniperJoe` shots. (Changed 2026-11-10 at the review: `movement` has no enemies.)
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: a hit during a run acts as before.

## Behaviors to check

- Death and respawn during a run: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`.

## Intended changes

None.
