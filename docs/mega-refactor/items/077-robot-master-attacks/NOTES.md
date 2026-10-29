# Notes: 077 New attacks and a panic attack for each robot master

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-29, `master` `dc64f75ff`

The 8 robot master levels in `LevelDefinition` and their boss classes (`entities/bosses/`):

- `TIMBER_WOMAN`: `TimberWoman.kt`
- `MOON_MAN`: `MoonMan.kt`
- `RODENT_MAN`: `RodentMan.kt`
- `DESERT_MAN`: `DesertMan.kt`
- `INFERNO_MAN`: `InfernoMan.kt`
- `REACTOR_MAN`: `ReactorMan.kt` (`ReactorMan_OLD.kt` is an old copy)
- `GLACIER_MAN`: `GlacierMan.kt`
- `PRECIOUS_WOMAN`: `PreciousWoman.kt`

All extend `AbstractBoss`, which extends `AbstractHealthEntity` (health and `onHealthDepleted()`).
