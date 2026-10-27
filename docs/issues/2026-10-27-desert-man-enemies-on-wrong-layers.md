---
status: open
verified: question
area: content
gameplay-change: yes
found-by: mega-refactor-062b-spawn-order-guards (2026-10-27)
related-items: []
---

# Five enemies in `DESERT_MAN` are on layers where they cannot spawn

## Where

`assets/tiled_maps/tmx/DesertMan_16x14_v4.tmx` (the live map of `LevelDefinition.DESERT_MAN`):

- `blocks` layer: `Cactus` objects with ids `103` (`x=2976, y=2880`), `104` (`x=2368, y=2784`), `710`
  (`x=2608, y=2880`), `711` (`x=1104, y=2880`).
- `specials` layer: `Bat` object with id `616` (`x=7344, y=1824`).

## What happens

`SpawnersLayerBuilder` resolves an object name to a class in the package of the layer's `EntityType`. It looks for
`entities.blocks.Cactus` and `entities.special.Bat`, which do not exist (`Cactus` and `Bat` are in
`entities.enemies`). At level load it logs five errors (`Failed to create spawner for entity: name=Cactus,
layer.name=blocks`, `name=Bat, layer.name=specials`) and skips these spawners, so these five enemies never appear.

## What should happen

Unclear. If the enemies are meant to be in the level, the objects belong on the `enemies` layer. If they were turned
off on purpose, the objects can be deleted, so the level loads with no errors.

## How to reproduce

`./run-game-alpha.sh --level DESERT_MAN --room room3` and read `assets/logs.txt`: five `SpawnersLayerBuilder` errors
at load.

## Notes

Seen in the launches and the owner's play test of `062b`; that branch did not touch the map or the builders.
