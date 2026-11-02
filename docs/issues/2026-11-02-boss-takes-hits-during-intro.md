---
status: open
verified: question
area: core
gameplay-change: yes
found-by: 118 research (2026-11-02)
related-items: [118]
---

# Question: a player shot in flight can hit a boss during its intro

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractBoss.kt:178-180`.

## What happens

`canDamage` tests `ready`, but `canBeDamagedBy` tests only `defeated`. So a shot that is still in flight can damage a boss before the fight starts. `LevelRoomHandler.kt:73-76` keeps player shots through a room transition.

The health bar then shows a false value: the fill counts to `getMaxHealth()` at `BossHealthHandler.kt:59`, and it drops to the real health when the timer ends.

## What should happen

The owner decides whether a boss can take damage in its intro.

## How to reproduce

Fire a charged shot at the door, and pass through a boss gate before it leaves the screen.

## Notes

`118` removes the bar fault when its fill reads the real value.
