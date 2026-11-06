# Notes: 126 Movement and damage blocks as lock reasons

## 2026-11-05: facts from the `081a` scan (`master` at `444681c2d`)

- `MOVEMENT` lock reasons today: `CAMERA_ROTATION` (`Megaman.kt:154`, cleared at `:426`) and `SPIDER_WEB`
  (`SpiderWeb.kt:160`, cleared at `:126`). `onSpawn` clears the group (`Megaman.kt:340`).
- `DAMAGE` lock reasons today: `TELEPORT`, `SPAWN`, `LEVEL_END` (no unlock found), `BOSS_DEFEATED` and
  `WILY_FINAL_BOSS` (no unlock). `onSpawn` clears the group (`Megaman.kt:342`).
- `canMove` has 9 production readers, all `Megaman` code. `isLocked(MOVEMENT)` alone is read in production only by
  `Megaman.kt:94`.
- `isLocked` returns `false` for an entity with no `LocksComponent` (`engine/.../locks/Locks.kt`).
- The order in `onSpawn`: the world gravity set can lock `MOVEMENT` (`CAMERA_ROTATION`) before the lock clear.
