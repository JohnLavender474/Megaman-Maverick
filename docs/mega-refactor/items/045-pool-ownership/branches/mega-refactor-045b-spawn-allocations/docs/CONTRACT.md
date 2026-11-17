# Contract: mega-refactor-045b-spawn-allocations

- Status: draft; Claude's choices pending owner confirmation (`SCOPE.md`, "Decisions")
- Approved: no

## Automated checks

- [ ] `./gradlew engine:test` passes (includes `MotionSetTest`, the new `CullableOnUncontainedTest` case and the
  changed `ShapeUtilsTest`).
- [ ] `./gradlew core:test` passes (includes `SpawnReuseTest`, `SawTest`, `LaserContactsTest`, `DelayRunTest` and
  `SpawnAllocationGuardTest`).
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

All commands run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-045b-spawn-allocations`.
`--poolChecks` sets each reclaimed scratch to NaN: an entity that keeps a scratch from its spawn jumps, vanishes or
freezes at once. Each check also looks at a second life from the pool: a death respawns the enemies only; hazards and
blocks spawn again when the player leaves their room and comes back.

Experimental:

| Level | --room | Options | What to check |
|---|---|---|---|
| `REFACTOR_TEST_045` | `swing` | `--poolChecks --debugShapes` | `SwingingPlatform` swings and carries Megaman; `SwingingAxe` swings and kills; the rotation `Saw` turns; the debug lines and circles follow them. Walk left into `spike_ball` and back: the platform and the saw start again from their first position (the axe never culls, so it keeps swinging) |
| `REFACTOR_TEST_029` | `enemy_aim` | `--poolChecks` | `Met` and `SniperJoe` aim and fire; their shots leave the screen and go away; shoot down (the buster down pose ends after each shot, also after rapid shots); die once: the enemies spawn again and act the same |
| `PRECIOUS_WOMAN` | `room4` | `--poolChecks` | Climb to `room5`: the `DrillHead`s idle and drill (debris, explosions) on their timing, `PreciousTron` moves between its spots and throws gems, the laser stops at blocks; go to `room6` and back to `room5`: the `LaserBeamer` lasers stop and reflect as before in both lives |
| `REACTOR_MAN` | `room10` | `--poolChecks` | `TubeBeamerV2` beams stop at their obstacles; go to `room9` or `room11` and back: same in the second life |
| `INFERNO_MAN` | `room1` | `--poolChecks` | The `FireballBar`s turn from their start angle; `Popoheli` flies from its start to its target; climb to `room2` and back: same in the second life |
| `X6_INTRO` | `room1` | `--poolChecks` | `HoverGunner`, `Junkroid` and `MetaWheelF` spawn on their spots and act as before; die once: same in the second life |
| `GLACIER_MAN` | `room6` | `--poolChecks --difficulty hard` | `IceFox` fires 6 shots per volley (normal mode: 3); the `Bat`s hang, then fly to Megaman and chase |
| `DESERT_MAN` | `room6` | `--poolChecks` | `CarriCarry` moves on its sine; the `Bat`s in `room8` to `room10` chase; die once: `CarriCarry` starts its sine again from its spawn |
| `MOON_MAN` | `room13` | `--poolChecks` | `TellySaucer` moves on its sine wave from its spawn |

Control:

| Level | --room | Options | What to check |
|---|---|---|---|
| `TIMBER_WOMAN` | `room9` | `--debugShapes` | Go down into `room10`: the 11 `Saw` pendulums swing, with the debug line from each anchor to its saw |

## Behaviors to check

- Enemy and shot culling (`AbstractEnemy`, `AbstractProjectile` now reuse their cullables): an enemy that leaves the
  view goes away after its cull time, and comes back when the room is entered again; a room transition removes the
  shots (in the `REFACTOR_TEST_029` and `DESERT_MAN` checks).
- The buster shoot-down pose (`delayRun` pool): the pose ends after each shot, also after rapid shots.

## Intended changes

- `CullableOnUncontained.reset()` also clears the time out of view. Before, a reused cullable kept the time of its
  last spawn if that spawn ended while out of view, so a respawned entity could be culled early.
- `Block` with `cull_room`: one cull listener per block, added at spawn and removed at destroy. Before, each reuse
  left the last spawn's listener registered (a growing list of listeners, no effect on play).
- `SwingingAxe`, `SwingingPlatform`: the debug shapes are made once. Before, each spawn added 3 more suppliers.
- `Saw` of trajectory type draws no debug shapes and hides its rings (before, a reused saw drew the shapes of its last pendulum or
  rotation spawn, or none on a first spawn).
- `Popoheli` with no target throws `IllegalStateException` (before, a null pointer error).
- `SmallIceCube`: an ignore-hit property with a null value is skipped (before, a cast error).
- No other change of play.
