# 068: Floor slopes for extra levels

- Status: planned
- Type: feature
- Depends on: none
- Related: `003`, `010`, `017`, `051`, `058`, `060`, `067`, `069`, `081`
- Branches: none yet

## Goal

Support X6-style floor slopes, so that "extra" levels built from X6 stages can use them. These levels are for a
future playable X (ported from X6). The main levels must not get slopes (the 8-bit games have none), and they must
play exactly as now. Slopes are a feature that a level turns on.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Facts are in `NOTES.md`.

- **Where the code lives:** core. Everything a slope touches is core: `Block`, `FeetBlockRule`, the `FEET`
  fixture, `FEET_ON_GROUND`, `BodyLabel` and the feet-gravity consumer. The engine has no "feet" or "floor" concept,
  so a slope helper in the engine would have no engine caller.
  - A `Slope` entity in `entities/blocks/` that extends `Block`. Tiled resolves it by name with no registry, and
    `FeetBlockRule` needs a `Block`.
  - The surface function (stepped, as X6) and the snap: a special case in `MegaCollisionHandler`, keyed on a new
    body label, so a slope skips the rectangle push-out.
  - The down-slope stick probe must run when the body is above the surface (no AABB overlap). The engine gets one
    generic step after `resolveCollisions` in `WorldSystem` (a first narrow piece of `017`), and the slope probe is
    its first caller.
  - The slope code can move to the engine later, if the engine gets a floor concept.
- **The switch:** none. A slope exists only as a `Slope` object in a map, and the main maps have none. The stick
  probe runs always, but only for a body whose floor in the last sub-step was a slope. So in the main levels it is
  one boolean check per dynamic body per sub-step, and it never acts.
  - The X6 flat step-down (up to 8 px) applies only when a body leaves a slope. Applied to all bodies, it would
    change the main levels.
- **Tiled authoring:** a rectangle object named `Slope` in a `blocks` layer, with properties for the rise and the
  direction. The object bounds are the bounding box of one straight run (X6 cells 1–4 or 9–10 merge into one
  object). No polygon parsing. The X6 converter writes these objects.
- **Who uses slopes:** any body with a feet fixture, through the same feet and gravity code. So Megaman and enemies
  work on slopes too, not only X. Slopes work only with `Direction.UP` gravity in the first version.
- **Not in the first version:** ice slopes and belt slopes (X6 values 17–28), slopes with other gravity directions,
  moving slopes. Ice slopes come with X (they push the player down at a dash-capped speed).
- **Tests:** engine unit tests for the surface function, the snap and the stick. A test level with both angles: a
  down-slope run at full speed (no air steps), a jump onto a slope from above, a dash across the top of a slope, a
  slope next to a flat floor (step-down at most 8 X6 px).

## Owner's input

2026-10-28 (summary): from a session in `mmx-x6`. The goal is to build "extra" levels in Maverick that mimic X6
levels, for a ported X. Slopes are needed in these levels only. The main levels must not change.

## Decisions

- 2026-10-28: the slope surface is stepped, as in X6 (one step per 4 px of x on the shallow slope, per 2 px on the
  steep slope, in X6 pixels).
- 2026-10-28: all slope code goes in core, with one generic engine step after `resolveCollisions`. No level flag:
  the stick probe is always on, guarded by "last floor was a slope". The other proposals in this file are agreed.
  (Item `069` adds `LevelType.X_LEVEL` for the select-button override; slopes do not depend on it.)

## Open questions

1. Units: at `PPM = 32` the X6 rises are 8 px and 16 px per 32 px tile. After `067` phase 2 (1x art and maps) they
   are 4 px and 8 px per 16 px tile, as in X6. Should this item wait for `067`, or work in world units so that it
   does not matter?
2. Side and head fixtures: `BLOCK_SIDE` and `HEAD_BLOCK` rules also fire against a slope's `BLOCK` fixture. How
   should a slope opt out (a fixture filter, a body label)? A slope is never a wall or a ceiling in X6.
3. Feet width: Megaman's feet fixture is 16 px wide. X6 uses only the bottom-centre point. Filter the feet contact to
   the centre point for slopes only, or for all floors in slope levels?
4. Should enemies that walk (for example the ones that turn at edges) be checked on slopes in the first version, or
   only Megaman and the player?
5. Does the dash and run speed on a slope stay the same as on a flat floor (X6: the x speed does not change on a
   slope)?

## Sources

- `NOTES.md` in this folder.
- X6: `~/IdeaProjects/mmx-demake-tools/docs/engine.md`, "The tests by value" and the slope profiles below it;
  "Slippery floors and slopes (stage logic 7)".
- Java remake: `~/IdeaProjects/mmx-x6/src/main/java/mmx/x6/physics/TerrainMover.java` and `TileRules.java`.
