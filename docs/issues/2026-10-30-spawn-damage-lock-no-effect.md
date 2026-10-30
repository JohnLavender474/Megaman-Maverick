---
status: open
verified: no
area: core
gameplay-change: unknown
found-by: mega-refactor-084d-damage-reaction scan (2026-10-30)
related-items: [084]
---

# The `SPAWN` damage lock appears to have no effect

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/events/PlayerSpawnEventHandler.kt:127` (lock) and `:209` (unlock)
- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt:471` (`clearLocks(MegaLockGroup.DAMAGE)` in `onSpawn`)

(`master` `4b65b1fdb`)

## What happens

`PlayerSpawnEventHandler.init()` locks `MegaLockGroup.DAMAGE` with `MegaLockReason.SPAWN` and submits `PLAYER_SPAWN_INIT`. The
level screen then queues `engine.spawn(player)`, and on the next update `Megaman.onSpawn` clears all `DAMAGE` locks. So the lock is
gone before the beam-in, and the unlock at `:209` does nothing. During the beam-in, only `!inPlay` in `invincible` protects Mega.

## What should happen

Either the lock stays through the beam-in, or the lock and the unlock go, because `!inPlay` already gives the protection.

## How to reproduce

Traced through the code, not run. Log `isLocked(MegaLockGroup.DAMAGE)` during the beam-in.

## Notes

`invincible` does not change in either case. `SmallIceCube.kt:228` reads the lock directly, so a fix can change what a
`SmallIceCube` does to Mega during the beam-in. `clearLocks(DAMAGE)` must not leave `onSpawn` until this is decided.
