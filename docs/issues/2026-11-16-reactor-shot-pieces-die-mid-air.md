---
status: open
verified: no
area: core
gameplay-change: unknown
found-by: owner play test of `mega-refactor-024b-character-loadouts` (2026-11-16)
related-items: []
---

# Reactor Shot burst pieces sometimes die in mid-air

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/projectiles/ReactorManProjectile.kt`: the small pieces
  that `shatter()` spawns; they die only through `explodeAndDie()`, which `hitBlock`, `hitBody`, `hitShield` (after 3
  bounces), `hitSand` and `onDamageInflictedTo` call
- `assets/tiled_maps/tmx/Test1.tmx`: room `room35`

## What happens

Megaman fires a Reactor Shot, and it bursts into its small pieces. Some pieces die in mid-air, as if they hit a block
that the sprite does not touch. The owner saw it in `TEST_1` `room35`, not in `INTRO_STAGE`. A weapon change is not
the cause.

## What should happen

A piece dies only when its art touches a block, a body or a shield that the player can see.

## How to reproduce

`./run-mega-man-maverick-alpha.sh --level TEST_1 --room room35 --weapons all --debugShapes`: fire Reactor Shots at the walls and
the floor, and look at what each piece touches when it dies.

## Notes

- A small piece has a 16 px body and `PROJECTILE` fixture (`SMALL_SIZE = 0.5f`). Its art is 14 px wide (7 px in a
  16 px cell, drawn at 2x), so the fixture is only 1 px larger on each side. The fixture size alone does not explain
  the gap.
- Candidates: a block with no painted tile (for example a room ceiling `AbstractBlock`), or another entity's `BODY`
  or shield fixture that is larger than its sprite.
- `024b` did not change this code; it is the same as on `master`.
