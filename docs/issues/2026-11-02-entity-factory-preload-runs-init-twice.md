---
status: closed
verified: yes
area: core
gameplay-change: unknown
found-by: 047 pooling research (2026-11-02)
related-items: [065]
---

# The factory preload runs `init()` twice for 25 `Bullet` and 25 `Block`

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/MegaEntityFactory.kt:42`
- `engine/src/main/kotlin/com/maverick/game/engine/GameEngine.kt:122`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/blocks/Block.kt:57`

## What happens

The preload calls `entity.init()` and does not set `entity.initialized`. So `spawnNow` calls `init()` again at the first spawn of each preloaded entity.

`init()` is not safe to repeat. For example, `Block.kt:57` adds a second debug shape supplier each time. `init()` also builds the components again.

## What should happen

Each entity runs `init()` once. The preload sets `initialized = true`, as `MegamanMaverickGame.kt:452` does.

## How to reproduce

Read the code, or count `init()` calls on a preloaded `Bullet` in a test.

## Resolution

Fixed in `mega-refactor-146a-player-marker` (squash `f84ee0d0e`, 2026-11-10): the preload sets
`entity.initialized = true`, and `GameEngine.spawn()` now runs `init()` (once, guarded by `initialized`) before
`canSpawn`.
