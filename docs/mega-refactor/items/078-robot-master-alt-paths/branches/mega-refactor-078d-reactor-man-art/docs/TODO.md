# TODO: mega-refactor-078d-reactor-man-art

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- **One level animation clock for tiles and entities** (owner, 2026-11-20). Tiled tile animations run on the
  libGDX base time (`TiledMapLevelRenderer.beginRender` calls `AnimatedTiledMapTile.updateAnimationBaseTime()`):
  wall time, which goes on in a pause and does not reset at level start. Entity animations and `services.updatables`
  run on game time, which stops in a pause. So a tile animation (rail chase, pipe flow) and an entity animation
  (toxic water, waterfalls, laser tubes) can never be in sync, and tiles move in a pause. Goal: one paused-aware
  level clock that drives both, with a typed home on the level; then shared entity clocks such as
  `ToxicLiquidClock` (`entities/special/`, `TODO` there) can move onto it. Touches the engine renderer.
- **Backgrounds drawn per room** (owner, 2026-11-20: the preferred long-term fix; make it a new item, so it can go
  in phases). Today the level shows or hides a background for the whole view (`MegaLevelScreen.draw`, the
  `backgroundsToHide` set, changed by `SHOW_BACKGROUNDS` and `HIDE_BACKGROUNDS`). During a room pan between 2 areas,
  one side of the view always shows the wrong background, and a backtrack shows the pop again. Goal: a background
  object names its rooms, and the level draws it only over the part of the view that is inside those rooms (a
  scissor or clip per background), so a pan shows each room with its own background and no trigger is needed.
  Phases: (1) the clip in the level draw, with the room list as a background property; (2) move the Reactor Man
  triggers to it (the borders `room7`/`room8`, `room10`/`room11` and `room13`/`room14` still pop on a backtrack);
  (3) the other levels that use show and hide triggers (Desert Man uses a gate today). Related: `046` (backgrounds
  as pure functions of the camera) changes the same draw code; decide the order when the item is made.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
