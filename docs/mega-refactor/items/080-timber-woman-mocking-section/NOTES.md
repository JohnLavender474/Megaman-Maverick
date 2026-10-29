# Notes: 080 A larger MockingBit and MockingByte section in Timber Woman's stage

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-29, `master` `dc64f75ff`

- Live map: `LevelDefinition.TIMBER_WOMAN` (`TimberWoman_16x14_v4.tmx`).
- The live map has 3 `MockingByte` objects and no `MockingBit` object by that name. A `MockingByte` can spawn the
  `MockingBit`s (to check in `MockingByte.kt`).
- Classes: `entities/enemies/MockingBit.kt`, `entities/enemies/MockingByte.kt`,
  `entities/projectiles/MockingByteEgg.kt`, `entities/decorations/MockingByteNest.kt`.
