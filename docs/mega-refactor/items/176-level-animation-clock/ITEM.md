# 176: One level animation clock for tiles and entities

- Status: idea
- Type: refactor
- Objectives: `O9`, `O1`
- Depends on: none
- Related: `078`
- Branches: none yet

## Goal

One animation clock per level that stops in a pause and drives both the Tiled tile animations and the entity
animations, so that a tile animation and an entity animation can run in sync and no tile moves in a pause.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Today the Tiled tile animations run on the libGDX base time (`TiledMapLevelRenderer.beginRender` calls
  `AnimatedTiledMapTile.updateAnimationBaseTime()`): wall time, which goes on in a pause and does not reset at level
  start. Entity animations and `services.updatables` run on game time, which stops in a pause.
- Give the clock a typed home on the level, feed the tile renderer from it, and move shared entity clocks onto it.
  The first one is `ToxicLiquidClock` (`entities/special/`; a `TODO` comment there points here).
- Touches the engine renderer.
- `services.updatables` also runs in a pause (`MegamanMaverickGame.render`), so `ToxicLiquidClock` animates the
  toxic water and waterfalls in a pause. The water did so before `078d` (its old animator was an updatable); the
  waterfalls stopped in a pause before `078d`. The level clock fixes both.

## Owner's input

- 2026-11-20 (branch `078d`): "Go with isolated clock, 0.125 s, and file the backlog item with a TODO in the code".
  `ToxicLiquidClock` is that isolated clock.

## Open questions

## Sources

- Branch `078d` `docs/TODO.md` (backlog candidate) and `process-log.md`.
