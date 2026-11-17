# Notes: 076 Better tileset art for every level

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-29, `master` `dc64f75ff`

- The last Wily level in `LevelDefinition` is `WILY_STAGE_3` (`WilyStage3_v4.tmx`). It has the `WilyFinalBoss`
  object.
- Its tilesets: `Tileset1.tsx` and `mm9endless.tsx`. `Tileset1.tsx` is also used by `WilyStage1_v2.tmx` and
  `WilyStage2_v2.tmx`.

## 2026-11-17, `master` `d38a97cee`: tilesets of the live levels

The item now covers every level. The `.tsx` files are in `assets/tiled_maps/tsx/`. Only the TMX file that
`LevelDefinition` names is live. Test maps (`TEST_*`, `REFACTOR_TEST_*`, `FIX_TEST_*`) are left out.

| Level | Live TMX | Tilesets |
|---|---|---|
| `INTRO_STAGE` | `IntroStage_v3.tmx` | `PunkCityTileset`, `Tileset1` |
| `TIMBER_WOMAN` | `TimberWoman_16x14_v4.tmx` | `Anokolisa_Cave_Tileset`, `Anokolisa_Swamp_Tileset`, `pipe_forest`, `Tileset1` |
| `MOON_MAN` | `MoonMan_16x14_v5.tmx` | `MoonManTiles` |
| `RODENT_MAN` | `RodentMan_16x14.tmx` | `sandy_pipes`, `Tileset1` |
| `DESERT_MAN` | `DesertMan_16x14_v4.tmx` | `tomahawk_man_stage_tileset`, `Tileset1` |
| `INFERNO_MAN` | `InfernoMan_16x14_v3.tmx` | `InfernoManTileset`, `MagmaMan`, `Tileset1` |
| `REACTOR_MAN` | `ReactorMan_v3.tmx` | `Reactor_Man_1`, `React_Man_2`, `Reactor_Man_3`, `Reactor_Man_4`, `Reactor_Man_No_Black`, `Tileset1` |
| `GLACIER_MAN` | `GlacierMan_16x14_v2.tmx` | `Blizzardman`, `GlacierManTileset` |
| `PRECIOUS_WOMAN` | `PreciousWoman_v2.tmx` | `CrystalManTileset`, `mm9jewel`, `precious_woman_background_tileset`, `precious_woman_foreground_tileset`, `precious_woman_platforms_tileset`, `Tileset1` |
| `WILY_STAGE_1` | `WilyStage1_v2.tmx` | `MegamanWanted_WilyStage1`, `MM10Endless`, `Reactor_Man_1`, `Reactor_Man_3`, `Tileset1` |
| `WILY_STAGE_2` | `WilyStage2_v2.tmx` | `GlacierManTileset`, `UnderwaterFanPillar_Gray`, `Wily2`, `Tileset1` |
| `WILY_STAGE_3` | `WilyStage3_v4.tmx` | `mm9endless`, `Tileset1` |
| `GAUNTLET_1` | `Gauntlet1_v2.tmx` | `Tileset1` |
| `X6_INTRO` | `X6Intro_16x14_v1.tmx` | `X6IntroNES_v1` |
| `SMB3_1` | `SMB3_1.tmx` | `SuperMarioBros3_World1Stage1` |

- `Tileset1` (`png/Tileset1_Full_32x32.png`) is in 11 of the 15 live levels. A change to its art reaches all of
  them. Find which of its tiles each level paints before a branch replaces it.
- Several levels use 2 or more tilesets from different sources. A level can keep one tileset and replace another.
- Not checked yet: which tilesets are commissioned art and which come from free asset packs. The owner sorts them.
