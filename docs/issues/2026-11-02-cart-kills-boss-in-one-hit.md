---
status: open
verified: question
area: core
gameplay-change: yes
found-by: 048 research (2026-11-02)
related-items: [048]
---

# Question: a moving `CartV2` kills a boss in one hit

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/special/CartV2.kt:150-157`, and `:167,171`.

## What happens

The cart calls `depleteHealth()` on any `AbstractEnemy` that it hits while it moves. `AbstractBoss` extends `AbstractEnemy`, so a boss is included. This looks unplanned. Live `CartV2` objects are in `DESERT_MAN` `room9` and `room10`. Whether a player can bring a moving cart to a boss is not checked.

## What should happen

The owner decides.

## How to reproduce

Place a moving `CartV2` in a boss room.

## Notes

`048` keeps this behavior: an actor test on `FactionComponent` gives the same result.
