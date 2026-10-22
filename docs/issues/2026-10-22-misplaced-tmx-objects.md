---
status: open
verified: yes
area: content
gameplay-change: yes
found-by: owner, during the mega-refactor-019a play test (2026-10-22)
related-items: [003, 021]
---

# 14 TMX objects in live maps never spawn: wrong layer or unknown class

## Where

`SpawnersLayerBuilder.kt:74-83`. For each object, the layer selects the entity package
(`blocks` → `entities.blocks`, `enemies` → `entities.enemies`, and so on), and
`MegaEntityFactory.getEntityClass` calls `Class.forName` on `<package>.<object name>`. If the
class is not in that package, the builder logs an error and skips the object.

## What happens

At level load the log shows errors like:

```
ERROR | SpawnersLayerBuilder | Failed to create spawner for entity: name=DuoBallCanon, layer.name=blocks | com.megaman.maverick.game.entities.blocks.DuoBallCanon
```

and the object never spawns. Count on 2026-10-22 (visible spawner layers of the 18 maps named in
`LevelDefinition.kt`): 14 objects in 7 maps.

| Level | Map | Layer | Object | Object id | Class exists in |
|-------|-----|-------|--------|-----------|-----------------|
| `DESERT_MAN` | `DesertMan_16x14_v4.tmx` | `blocks` | `Cactus` | 103, 104, 710, 711 | `enemies` |
| `DESERT_MAN` | `DesertMan_16x14_v4.tmx` | `specials` | `Bat` | 616 | `enemies` |
| `GLACIER_MAN` | `GlacierMan_16x14_v2.tmx` | `blocks` | `Sealion` | 695 | `enemies` |
| `PRECIOUS_WOMAN` | `PreciousWoman_v2.tmx` | `blocks` | `FirstSpawn` | 443 | no class |
| `WILY_STAGE_1` | `WilyStage1_v2.tmx` | `blocks` | `DuoBallCanon` | 322 | `enemies` |
| `WILY_STAGE_2` | `WilyStage2_v2.tmx` | `enemies` | `Saw` | 187, 188 | `hazards` |
| `TEST_1` | `Test1.tmx` | `hazards` | `SwinginAxe` | 65 | no class (`SwingingAxe` exists) |
| `TEST_1` | `Test1.tmx` | `enemies` | `JetpackIceBlaster` | 274 | no class |
| `TEST_2` | `QuickRun.tmx` | `hazards` | `SpikeCopter` | 509, 510 | `enemies` |

## What should happen

For each object, decide whether it was meant to spawn:

- If yes: move it to the layer that matches its class (or fix the name). Then it spawns, which
  changes gameplay in that room, so play-test the room.
- If not (a note or a leftover): move it to a layer that is not built (for example `temp`) or
  delete it.

## How to reproduce

Start a level from the table (for example `--level WILY_STAGE_1`) and read the error log at
level load.

## Notes

- Today these objects do not spawn, so the current gameplay does not include them. Fixing them is
  a deliberate gameplay change; do it in a `fix/<slug>` branch, one map at a time if needed.
- Item `003` (level content validator) would report these at build time; item `021` (entity
  registry) removes the layer-as-package rule that causes most of them.
- The table was made with a script that checks, for each object, whether a class with that name
  is declared in the package that the layer selects.
