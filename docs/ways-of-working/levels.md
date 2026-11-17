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
