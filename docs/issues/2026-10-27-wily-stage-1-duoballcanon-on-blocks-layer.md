---
status: open
verified: question
area: content
gameplay-change: yes
found-by: mega-refactor-019b-destroy-notification (2026-10-27)
related-items: []
---

# A `DuoBallCanon` in `WILY_STAGE_1` is on the `blocks` layer and never spawns

## Where

`assets/tiled_maps/tmx/WilyStage1_v2.tmx` (the live map of `LevelDefinition.WILY_STAGE_1`): object `322`, name
`DuoBallCanon`, on the `blocks` layer at `x=11320, y=1680`. Seven other `DuoBallCanon` objects in the map are on the
`enemies` layer.

## What happens

`SpawnersLayerBuilder` looks for `entities.blocks.DuoBallCanon`, which does not exist (the class is in
`entities.enemies`). At level load it logs `Failed to create spawner for entity: name=DuoBallCanon, layer.name=blocks`
and skips the object, so this enemy never appears.

## What should happen

Unclear. If the enemy is meant to be in the level, the object belongs on the `enemies` layer. If it was turned off on
purpose, the object can be deleted, so the level loads with no error.

## How to reproduce

`./run-game-alpha.sh --level WILY_STAGE_1 --room room8` and read `assets/logs.txt`: one `SpawnersLayerBuilder` error at
load.

## Notes

The same kind of map problem as `2026-10-27-desert-man-enemies-on-wrong-layers.md`. Seen in a launch of `019b`; that
branch did not touch the map or the builders.
