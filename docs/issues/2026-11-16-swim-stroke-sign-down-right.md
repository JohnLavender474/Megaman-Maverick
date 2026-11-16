---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-060m-protoman-moves (2026-11-16)
related-items: [060, 058]
---

# The swim stroke pushes the wrong way in `DOWN` and `RIGHT` gravity

## Where

`core/src/main/kotlin/com/megaman/maverick/game/players/PlayerSwim.kt`, the stroke impulse in `init()` (a `TODO`
marks it). Megaman and Proto Man both call it.

## What happens

The swim speed is already signed by the gravity (`SWIM_VEL_Y * upSign()`). The stroke negates it again for `DOWN`
and `RIGHT`, so in those gravities the stroke pushes toward the floor. `UP` and `LEFT` are correct. The jump uses the
signed speed with no second sign. `MegamanSwimTest` pins the old signs.

## What should happen

A stroke pushes away from the floor in every gravity, as the jump does.

## How to reproduce

Swim in water with the gravity turned `DOWN` or `RIGHT` (a gravity switch room with water): the stroke sinks the
player. Found by reading the code at `060m` step 2; the `060m` tests pin the current signs.
