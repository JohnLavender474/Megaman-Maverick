---
status: closed
verified: yes
area: engine, core
gameplay-change: no
found-by: GC audit during the 134b play test (2026-11-09)
related-items: [009, 020, 026, 044]
---

# Hot paths allocate per frame or per step where no item covers it

## Where

The full audit, with sizes and the inference marks, is in `temp/gc-audit/AUDIT.md` (sections A1, A2, A4, A5). The parts
that this issue covers:

1. Pathfinding:
   - `core/.../pathfinding/MegaPathfinderFactory.kt` `getWorldContainerSnapshot()` calls `container?.copy()` on each
     frame that starts a search. `engine/.../world/container/SimpleGridWorldContainer.kt:32-42` makes a new
     `OrderedSet` for each occupied cell. Each pathfinding entity (`Bat`, `DeathBat`, `FloatingCan`, `SubmarineJoe`)
     asks for a path every 0.1 s (`PathfindingComponent.kt:10,14`).
   - `entities/enemies/Bat.kt:375-376`: the filter makes 2 `GameRectangle`s and 1 lambda for each neighbour that A*
     tests.
   - `core/.../pathfinding/MegaPathfinderResultConsumer.kt`: runs on every frame while a path is set, and does
     `path.map { GameRectangle() }`, a new trajectory `Vector2`, and a default `body.getBounds()`.
2. World step: `engine/.../world/WorldSystem.kt:150,209-213,260-282,293-304` passes capturing lambdas to
   `forEachFixture` and the world container queries, 1 or more per body and per fixture, on each step.
3. Animation keys: `entities/megaman/MegamanAnimations.kt:42-43` `buildFullKey` builds 3-4 strings per player per
   frame (`weapon.name.lowercase().replace(...)` and a template). Animation key suppliers with templates per frame:
   `Jetto.kt:344`, `SpikeBot.kt:306`, `NuttGlider.kt:365`, `LampeonBullet.kt:165`, `StagedMoonLandingFlag.kt:413-434`,
   `PreciousWoman.kt:687`, `AstroAssAssaulter.kt:293`; `MoonMan.kt:342,385` look up timers by a template key.
4. Entity lookups: `entities/MegaGameEntities.kt` `getOfTag`, `getOfType` and `getOfId` pass `OrderedSet()` as the
   default value of `get`, so each call makes a new set, also when the key exists. Per-frame callers include
   `Rain.kt:341`, `DarknessV2.kt:344`, `DarknessV4.kt:526`, `Darkness.kt:269-270`, `Laser.kt:460,609`, `Snow.kt:123`.

## What happens

Each of these allocates on every frame or every physics step (2 or 3 steps per frame at `MEDIUM`). The objects are
short-lived, so they drive young garbage collections. A1 (the grid copy) is the largest: a structure the size of the
loaded level, several times per second in a room with bats. This breaks `O5-R1` (no per-frame allocation in common
paths).

## What should happen

No allocation on these paths in the normal case:

1. Pathfinding: one world snapshot per world step at most (or an immutable cell array that the world writes once per
   step), reused rectangles in the `Bat` filter, and a consumer that walks the path by index with reused objects.
2. World step: plain loops over the fixtures and container queries that write into reused arrays (for example an
   `inline` `forEachFixture` on `Body`, and a container query into a reused `Array`).
3. Animation keys: `buildFullKey` results cached per `(key, weapon)`, built once; key suppliers return prebuilt
   strings.
4. Entity lookups: a shared empty set returned when the key is missing, as a read-only view.

## How to reproduce

Read the code. To measure: record a run with `--diagnostics` and GC logging
(`JAVA_TOOL_OPTIONS="-Xlog:gc*,safepoint:file=gc.log:uptime"`) in a room with several `Bat`s, a rain or darkness
room, and a real stage with many blocks, for at least 60 s each. Compare the young GC count and the pause times
before and after the fix.

## Notes

- Not in this issue, because an item covers it: the draw queue `GamePair` and boxed `Long` per sprite (`026`); eager
  log strings (`044`), including `Background.kt:129` (`2026-11-09-background-debug-string-each-frame.md`).
- The same audit found that the machine swapped hard during the play test (19 of 19 GiB swap used, 18 Java processes),
  and that the dev run (`lwjgl3:run`) sets no heap or GC flags while the release build sets `-Xms512M -Xmx1024M` with
  ZGC. That is the strongest lead for the long freezes, and it is not a code bug. Measure with the other JVMs stopped
  before and after the fix, so that the result shows the code change.
- Side finding (not a GC cost): `AsyncPathfindingSystem` cancels a timed-out search with `future.cancel(true)`, but the
  A* loop does not check the interrupt, so a cancelled search can run while the next one starts with the same
  `DynamicBodyHeuristic`.

## Short contract

Branch: `fix/per-frame-allocations-hot-paths`, off `master`, in `.worktrees/fix/per-frame-allocations-hot-paths`.

Scan (2026-11-09, at `ca36f7d1e`): the 4 parts of "Where" are read from the code. `MegaPathfinderFactory` copies the
world container once on each frame that creates a pathfinder, and the search reads the copy only through `getBodies`
(the heuristic and the filters). No caller changes a set that `MegaGameEntities.getOfTag`, `getOfType` or `getOfId`
returns. `buildFullKey` has 3 callers.

Intended changes (no gameplay change; each part keeps its results and its order):

1. Pathfinding (design changed 2026-11-09 by the owner: a pathfinding snapshot, not a world container copy):
   - `IWorldContainer.copy()` is removed from all 3 containers (`QuadtreeWorldContainer.copy()` shared its live root,
     so it was not a snapshot). A search reads a `BodyCellsSnapshot` (engine) through `IBodyCells`, which gives only the
     bodies of a cell, the one query that the filters and the heuristic use. The snapshot is filled from the bodies of
     the live container: `WorldSystem` keeps them in a list filled in the same loop as the container
     (`IWorldBodies`; owner choice over a "fetch all bodies" method on the containers).
   - The factory keeps a pool of snapshots. A snapshot is filled again only when no search uses it (a wrapper
     releases it in `finally`). It is still filled once per frame that starts a search, with the same bodies.
   - The A* loop stops when its thread is interrupted, so a timed-out search ends at once.
   - `BlockCells` (core) holds the 2 block checks of the 4 filters and the heuristic, with thread-local scratch
     objects (the filters run on worker threads).
   - `MegaPathfinderResultConsumer` walks the path by index with reused objects; the same node is chosen.
2. World step: the callbacks of `WorldSystem` (`updateWorldContainer`, `addFixtures`, `collectContacts`,
   `resolveCollisions`) become reused function fields; the values that the lambdas capture today become fields set
   before the call. The same functions run in the same order, so the contacts and their order do not change.
3. Animation keys: `MegamanAnimations.buildFullKey` returns a cached string per `(region key, weapon)`; the 7
   suppliers named in "Where" (and the `MoonMan` timer keys) return prebuilt strings. The strings do not change.
4. Entity lookups: `getOfTag`, `getOfType` and `getOfId` return one shared empty set when the key is missing; it
   throws on `add`. The id map is an `IntMap`.
5. `engine:allocationTest`: a test suite in its own JVM measures the bytes per world frame and per snapshot fill, and
   fails above 1 KB. It runs with `engine:test`. In the shared test JVM, MockK instrumentation made the measure flaky.

Changed files: `WorldSystem.kt`, the 3 world containers and `IWorldContainer.kt`, the pathfinding types
(`PathfinderParams`, `WorldPathfinder`, `Pathfinder`; new `IBodyCells`, `BodyCellsSnapshot`, `IWorldBodies`),
`MegaPathfinderFactory.kt`, `MegaPathfinderResultConsumer.kt`, `BlockCells.kt`, `DynamicBodyHeuristic.kt`, `Bat.kt`,
`DeathBat.kt`, `FloatingCan.kt`, `SubmarineJoe.kt`, `MegaGameEntities.kt`, `MegamanAnimations.kt`, the 7 supplier files,
`engine/build.gradle`, tests.

Not changed:

- Path results with the default world container (`--worldContainer simple`): the snapshot holds the bodies of each
  grid cell, as the old copy of the grid did. With `--worldContainer quadtree` (a dev option) a search can see fewer
  bodies than before: the old copy of the loose quadtree gave every body of each loose node that overlaps the cell,
  the snapshot gives only the bodies in the cell (named as an intended change; review finding, 2026-11-09).
- The snapshot timing, the contact order (`ContactRuleSelectionTest`, the `WorldSystem` tests), the
  animation key strings, the lookup results.
- Not in this fix: the draw queue (`026`), eager log strings (`044`), the `AsyncPathfindingSystem` interrupt (side
  finding; filed on its own if it stays), the JVM flags of the dev run.

Checks:

- Unit tests:
  - `BodyCellsSnapshot`: it holds each body in the cells that its bounds cover, and keeps them until the next fill.
  - Snapshot pool: a snapshot that a running search holds is not refilled; a free snapshot is filled again in a new
    frame with the current bodies.
  - `MegaGameEntities`: a missing key gives the shared empty set twice; a present key gives its set.
  - `buildFullKey`: the cached string equals the old template for every weapon; the second call returns the same
    instance.
  - World step: the existing `WorldSystem` and contact tests pass unchanged.
- Break the code and confirm that each new test can fail.
- `engine:allocationTest`, before and after (`O5-R2`). Measured on 2026-11-09 against an export of `master`
  `34e5f2370`: world step (200 bodies) 53,216 -> 16 bytes per frame; snapshot (2,000 bodies) 1,735,288 bytes per copy
  -> 0 bytes per fill.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/per-frame-allocations-hot-paths`
(rooms checked by the review before the play test, 2026-11-09; a `--level` start is `NORMAL` difficulty, so
`hard_mode_only` objects do not spawn):

- `./run-game-alpha.sh --level RODENT_MAN --room room7`: the `Bat` chases Megaman around the blocks as before.
- `./run-game-alpha.sh --level TEST_1 --room room35`: 2 `Bat`s chase Megaman as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_chase`: the `Bat` chases Megaman as before.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room fork_room`: take the lower path into `path1_room1`; the 3
  `SubmarineJoe`s follow Megaman as before.
- `./run-game-alpha.sh --level GAUNTLET_1`: clear the `room2` wave; the `FloatingCan`s of the `room3` wave chase
  Megaman as before.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room8`: the `DarknessV2` room lights and darkens as before.
- `./run-game-alpha.sh --level DESERT_MAN --room room2`: the `Jetto` animations and the `LampeonBandito` bullets look as
  before.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: the `SpikeBot` animations look as before.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room1`: the `NuttGlider` animations look as before.
- `./run-game-alpha.sh --level MOON_MAN --room room5 --weapons all`: the 2 `AstroAssAssaulter`s stand, shoot, throw and
  freeze as before.
- `./run-game-alpha.sh --level MOON_MAN --room room14`: the room before the boss. The `MoonMan` shoots and changes the
  gravity as before.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room7`: the room before the boss. Her stand, laugh and throw poses
  look as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_134 --room open --weapons all`: switch weapons; Megaman's colors and
  poses for each weapon are as before.
- GC measure (Claude compares the young GC counts and pauses), with the other JVMs stopped. The game runs from the JAR,
  so that the GC log holds only the game JVM. Once in the main checkout (`master`) and once in the worktree:
  `./gradlew lwjgl3:jar && ./gradlew --stop`, then from `assets/`:
  `$HOME/.jdks/corretto-17.0.13/bin/java -Xlog:gc:file=/home/johnlavender/IdeaProjects/Megaman-Maverick/temp/fix-per-frame-allocations/gc-<master|branch>.log:uptime -jar ../lwjgl3/build/libs/Megaman-Maverick-1.0.1.jar --level RODENT_MAN --room room7`
  Play for 60 s in each run.

## Resolution

Fixed by `fix/per-frame-allocations-hot-paths` (2026-11-10). The squash landed in commit `50d0f7458`, together with
`fix/koopa-shell-kicker`: another session committed the shared index of the main checkout while both squashes were
staged. That commit holds 46 files: the 4 Koopa files (`GreenKoopaShell.kt`, `RedKoopaShell.kt`,
`KoopaShellKickerTest.kt`, `RefactorTest047.tmx`) and the 42 files of this fix. Its message names only the Koopa fix.
History is not rewritten (owner rule).

- Parts 1 to 5 of the short contract are done. The 7th supplier, `StagedMoonLandingFlag`, needed no change: its keys
  are `const val` templates, which the compiler builds once.
- `engine:allocationTest`: world step (200 bodies) 53,216 -> 16 bytes per frame; snapshot (2,000 bodies) 1,735,288
  bytes per copy -> 0 bytes per fill.
- The play test found a crash: after the 7th shot, `MoonMan` asked for `SHOOT_KEYS[7]` in a table of 7. The table now
  has the key `shoot_7` (no animation, as before), commit `f619fffe5` on the branch. The owner's play checks pass.
- The GC measure through the JAR was not done.
- Diffity: 0 threads. Owner question on the shared empty set: kept, with a `TODO` at `MegaGameEntities.noEntities` and
  the new issue `2026-11-10-mega-game-entities-shared-empty-set.md`.
