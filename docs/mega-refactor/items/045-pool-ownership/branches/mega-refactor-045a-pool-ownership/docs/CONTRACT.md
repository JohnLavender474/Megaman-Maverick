# Contract: mega-refactor-045a-pool-ownership

- Status: draft; Claude's choices pending owner confirmation (`SCOPE.md`, "Decisions")
- Approved: no

## Automated checks

- [ ] `./gradlew engine:test` passes (includes the new `WorldPathfinderTest` copy case and the changed `ArcMotionTest`
  and `PendulumTest`).
- [ ] `./gradlew core:test` passes (includes `GameObjectPoolsTest`, `OwnedArrayTest`, `PoolOwnershipGuardTest`).
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

All commands run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-045a-pool-ownership`.
Experimental checks first. `--poolChecks` poisons each reclaimed scratch with NaN and never reuses it: an entity that
still keeps a scratch jumps, vanishes or freezes at once. In each `--poolChecks` check, look for that, and for an
error in the terminal.

Experimental (debug checks on):

| Level | --room | Options | What to check |
|---|---|---|---|
| `PRECIOUS_WOMAN` | `room4` | `--poolChecks` | Walk into `room5` and `room6`: `DrillHead` hovers between its spots, `PreciousBlock`s move on their tracks, `PreciousTron` spawns, the `LaserBeamer` lasers stop at their obstacles and reflect |
| `REACTOR_MAN` | `room10` | `--poolChecks` | `TubeBeamerV2` beams stop at their obstacles |
| `MOON_MAN` | `room4` | `--poolChecks` | `BunbyTank` scans and shoots in each direction; asteroids spawn |
| `MOON_MAN` | `room11` | `--poolChecks --heartTanks all --healthTanks all` | `MoonHead` arcs (`ArcMotion`) between points as before |
| `RODENT_MAN` | `room1` | `--poolChecks` | The `RatRobot` waits for its trigger, then runs; the `Bat` chases Megaman (async pathfinding) |
| `TIMBER_WOMAN` | `room13` | `--poolChecks --difficulty hard` | `MockingByte`s return to their nests; the `Bat`s (hard mode only) chase Megaman |
| `REFACTOR_TEST_134` | `attach` | `--poolChecks` | `Electrocutie` children stay at their posts (before: the posts could move after 3 frames) |
| `TIMBER_WOMAN` | `room8` | `--poolChecks` | Walk right: the 2 `Saw` pendulums swing as before |
| `REFACTOR_TEST_045` | `spike_ball` | `--poolChecks` | The 2 `SpikeBall`s (chain 3 and 4) swing, and each chain follows its ball. Die once: after the respawn they swing again with the same chains, and no extra link shows (the sprites are reused, not made again) |
| `REFACTOR_TEST_029` | `enemy_aim` | `--poolChecks` | `Met` and `SniperJoe` move, aim and fire; die once to see the death fade and the beam-in |

Control (debug checks off):

| Level | --room | Options | What to check |
|---|---|---|---|
| `RODENT_MAN` | `room16` | `--heartTanks all --healthTanks all` | `RodentMan` spawns rats at its spots |
| `WILY_STAGE_1` | `room10` | `--heartTanks all --healthTanks all --debugShapes` | `ElecDevil` pieces fly out and back to their cells; the damage boxes follow the pieces |
| `WILY_STAGE_3` | `room8` | `--heartTanks all --healthTanks all` | Final boss: fly-in positions, phase 2 sway, phase 3 capsule spawns, the tentacle joints |
| `X6_INTRO` | `room2` | `--heartTanks all --healthTanks all` | `D1000` fight plays as before (forecast and flank shots) |
| `INTRO_STAGE` | `room6` | | `SteamRollerMan` and its roller blocks move together |
| `REFACTOR_TEST_031` | `spring_bouncer` | | Each `SpringBouncer` bounces Megaman and shows its bounce sprite |
| `INFERNO_MAN` | `room1` | `--debugShapes` | The `Popoheli` flame damage box is on the flame side when it turns |

## Behaviors to check

- Death and respawn (`DeathFadeOut`, `BeamInView` now use an owned rectangle for the UI camera): the fade covers the
  screen and the beam-in runs (in the `REFACTOR_TEST_029` check).
- Checkpoints (`PlayerCheckpointManager`): `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`, walk into
  `room6`, die, and see the respawn at `room6`.

## Intended changes

- Stored-scratch bug fixes: an entity that kept a pooled scratch object now keeps its own copy. Before, the value
  could change to a random position some frames after the spawn. Affected: `Lift` stop point, `CeilingCrusher` start,
  `ElectrocutieChild` post, `ExplosionField`/`Tree`/`WindyGrass`/`ForceDecoration`/`Darkness`/
  `DecoratedGravityForce` bounds, `SniperJoeShield`/`TubeBeam` trajectory, `DarknessV4` light centers, `ElecDevil`
  piece positions, the async pathfinder start and target, the `SpikeBall` pendulum anchor, the `UnderwaterPenguinBot`
  trigger box and start.
- `MockingByte` return to nest: the position list is cleared before each return. Before, it grew with each return and
  kept the positions of earlier nests, so a `MockingByte` could fly to a stale or foreign spot.
- New dev flag `--poolChecks` (off by default; no effect on normal play).
- No change to `SCRATCH_LIFETIME_FRAMES` (3); see `NOTES.md`.
