---
status: open
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
