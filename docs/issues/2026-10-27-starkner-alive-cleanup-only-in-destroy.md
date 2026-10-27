---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-062c-existence-readers (2026-10-27)
related-items: [063]
---

# `Starkner` clears its `ALIVE` entry only in its `destroy()` override

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/Starkner.kt`: the static set `ALIVE` (`:73`), the
check in `canSpawn` (`:147`), the add in `onSpawn` (`:186`), and the only remove, in the `destroy()` override
(`:192-196`).

## What happens

A `Starkner` adds its map object id to `ALIVE` when it spawns, and `canSpawn` refuses an id that is in `ALIVE`. Only
the member `destroy()` removes the id. A destroy that does not go through that member does not remove it:

- the engine's `reset()`/`dispose()` at a level end call `destroyNow` directly;
- any `engine.destroy(starkner)` call.

A `Starkner` that is live at a level end then stays in `ALIVE` for the rest of the run, so that `Starkner` does not
spawn again when its level is played again [inferred, not played].

## What should happen

The id leaves `ALIVE` on every destroy: the remove belongs in `onDestroy` (or `onDestroyQueued`), which the engine
calls on every path.

## How to reproduce

Not played yet. Find a level with a `Starkner`; play until it is on screen and live; leave the level (death with game
over, or the level end) while it is live; start the same level again in the same run: check whether it spawns.

## Notes

Found by a read-only review agent in `062c` (the question of `destroy()` on `IGameEntity`, item `063`), and checked in
the code. Item `063` must not change the culler at `MegamanMaverickGame.kt:900` to `engine.destroy(entity)` before
this is fixed, or a culled `Starkner` would keep its id in `ALIVE`.
