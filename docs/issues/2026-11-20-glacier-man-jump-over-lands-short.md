---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: `mega-refactor-077g-glacier-man` (2026-11-20)
related-items: [077]
---

# Glacier Man's jump over the player lands about 1 tile past the player, not 2

## Where

`GlacierMan.jumpOverTargetPlayer` (`entities/bosses/GlacierMan.kt`).

## What happens

The x speed is computed for an air time of `2 * 15 / 22.5` s with no drag. `Body.process` applies the default y
friction (`applyFrictionY = true`, `frictionOnSelf.y = 1`, `vy *= exp(-dt)` per step), and `GlacierMan` does not
turn it off. So the air time is shorter, and he lands about 1 tile past the player. `GlacierManRig`: 287.6 px for a
target of 320 px over a 6.75-tile jump.

## What should happen

He lands 2 tiles past the player in x (`JUMP_OVER_PAST`), clamped to the arena, or the design takes the shorter
landing as final.

## How to reproduce

`GlacierManMoveChoiceTest` (the sled chain case) checks only the launch speed; read `maxX` in the rig. In the game:
`./run-game-alpha.sh --level REFACTOR_TEST_077 --room glacier_man`, wait for a sled that stops at a wall and jumps.

## Notes

The owner play-tested the fight with this landing (077g round 1). A fix is a gameplay change.
