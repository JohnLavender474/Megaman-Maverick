# Levels

Read this file before you draft or edit a level (TMX), or an entity that reads places from its room.

- **A level draft is a playable skeleton.** Before you draft a TMX level, read `assets/tiled_maps/tmx/Test8.tmx`,
  `IntroStage_v3.tmx`, `ReactorMan_v3.tmx` and `InfernoMan_16x14_v2.tmx` from start to end.
  - Put real block geometry in the `blocks` layer (floors, walls, ceilings, platforms), not only spawn points. Copy
    the block patterns of `Test8.tmx`.
  - Give each spawn object the entity's real size: copy it from a reference level, or read the entity's `onSpawn`.
  - Every alternative path joins the main path again; no dead ends.
  - Tile layers in a level draft: ask the owner whether to paint them to match the blocks or to leave them out
    (default: leave them out; no empty placeholder layers). A test map is different: it always has a painted `tiles`
    layer (`docs/mega-refactor/CLAUDE.md`, "Test maps").
- **The tiles agree with the goal.** The tiles show the same shape as the blocks, and the look that the task is
  about. A plain square tile on a block that is not a rectangle (a polygon, a slope) is wrong: the room looks flat,
  and a check of that shape cannot be done by eye.
  - Before you paint, find the real art. When the task names a stage or a stage look (for example "the spiked
    ceilings of Precious Woman"), read that stage's TMX, note the tileset and the tile ids at that place, and add
    that tileset to the map. Paint each cell of a shaped block with a tile whose art follows the block's edges.
  - Give each block only a shape that the art can show. If no tile fits a shape that the task needs, ask the owner
    before you build: change the shape to one the art fits, draw new tiles (`art.md`), or accept plain tiles.
  - After you paint, render the tiles with the block outlines over them to a PNG in `temp/`, look at it, and fix
    each cell where the art and the block do not agree.

  Example (owner, 2026-11-23, branch `182a`): the polygon ceilings of `RefactorTest182` were painted with the flat
  tile 175. The check was about the V-point crystal ceilings of the Precious Woman stage, so the room showed a flat
  ceiling and the owner could not see the V points.
- **Only spawn `0` near the world origin.** At level start the camera is near the world origin (the bottom-left
  corner of the map), and a player spawn that overlaps that first view takes the place of the `--room` spawn (issue
  `docs/issues/2026-11-23-launch-room-spawn-taken-by-camera-origin.md`). `--room` drops only the spawns whose names
  sort before its start spawn, so a spawn `0` near the origin is harmless. Until that issue is closed, a spawn whose
  bottom is less than 8 tiles above the bottom edge of the map and less than 16 tiles from its left edge must be spawn
  `0`. Otherwise leave empty rows at the bottom, or move that room. Check it with a script before you hand over the
  checks, and give the same rule to a subagent that builds the map.

  Example (owner, 2026-11-23, branch `182a`): room `rect_ceiling` was the bottom room of `RefactorTest182`, and its
  spawn was 7 tiles above the map bottom. Checks 1 to 5 all started in `rect_ceiling`, and the owner tested the wrong
  room twice.
- **Polygon ceilings (`CeilingPolygonBlock`) are for ceilings only** (owner, 2026-11-23, item `182`).
  - Draw each one as an unrotated convex polygon with each edge flat or at 45 degrees; the block throws at spawn
    otherwise. A saw-tooth is several objects.
  - Keep enough clearance under each polygon: a grounded body that walks into a face is only pushed sideways out
    of it, so a gap under a tip that is lower than the walker is a wall. Leave such a gap only where a wall is meant.
  - Do not use one in a level or room where the gravity turns: the block throws when it spawns with gravity not
    `UP`, and it gives no push-out to a body whose gravity is not `UP`.
  - Paint it with tiles whose art follows its edges (see "The tiles agree with the goal").
- **Read room geometry from map objects, not from hard-coded values.** When an entity needs a place in its room (a
  floor, a wall, a ceiling, an arena, a spawn or target point), give its map object an object property that refers
  to the map object of that place, and read it in `onSpawn` with `!!`, so that a missing reference fails fast. Do
  not compute the place from constants or from offsets to another position (for example "the floor is 2 tiles below
  the landing point"). Such values break without an error when the room changes, and the next reader cannot see
  where they come from. Refer to the block objects that are already in the map where you can, in place of new
  marker objects. A value that is not a place in the room, for example a speed, a time or a size of the entity
  itself, stays in code.

  Example: `TimberWoman` reads its `ground` and `wall_*` references. In branch `077a` (owner, 2026-11-14), the arena
  of `BigAssMaverickRobot` changed from offsets to the landing point to `ground`, `left_wall` and `right_wall`
  references to the floor and wall blocks of the boss room.
- **A gauntlet wave holds only enemies that can die.** A `GauntletMaster` wave ends only when all its enemies are
  destroyed, so an enemy that cannot take damage locks the player in the room. Before you put an enemy in a wave,
  read its class: look for `invincible` (for example `SpringHead`) and other no-damage logic such as a permanent
  shield, and check that the enemy cannot be culled or stuck out of reach. Keep drafts simple, and check each enemy's
  real behavior in its code.
- **A test room for an enemy lets the enemy respawn.** When a check kills or changes an enemy that the owner tests
  again and again, make the room wide enough to respawn it without a restart. A destroyed enemy's spawner rearms when
  its spawn rectangle is fully outside the camera view, and spawns again when the rectangle comes back into view
  (`spawns/SpawnerForBoundsEntered.kt`). So put the player spawn near one wall and the enemy more than one view width
  (16 tiles) plus a margin of about 8 tiles from that wall: the owner walks to the wall, the enemy goes off camera,
  and walking back respawns it. Say it in the check: "walk to the left wall and back to respawn the enemies".

  Example (owner, 2026-11-18, branch `079a`): `RefactorTest079` room `charged_stagger` became 40 tiles wide, with the
  `Met`s 26 and 30 tiles from the left wall, so the charged-shot stun can be tried many times.
- **A level for co-op has no `UP` or `DOWN` gate.** Use `LEFT` and `RIGHT` gates only. In co-op, vertical gates are not
  yet as the owner wants (owner, 2026-11-18, branch `047k`; `TODO` at `Gate`). This rule ends when item `170` is
  done. Existing single-player levels keep their vertical gates.
