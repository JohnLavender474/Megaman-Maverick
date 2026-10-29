# Notes: 073 More gravity-block puzzles in Moon Man's stage

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-29, `master` `dc64f75ff`

- No level is named Gravity Man. The owner's "Gravity Man's stage" is `LevelDefinition.MOON_MAN`
  (`MoonMan_16x14_v5.tmx`). `MoonMan.kt:85` still has the old name in a comment ("Gravity Man's direction
  rotation").
- Objects in the live map: `GravityBlock` 30, `GravitySwitcharoo` 9.
- Classes: `entities/blocks/GravityBlock.kt`, `entities/special/GravitySwitcharoo.kt`,
  `entities/decorations/GravitySwitchAura.kt`.
- `WilyStage3_v4.tmx` also uses them (`GravityBlock` 9, `GravitySwitcharoo` 21). A change to the classes can affect
  that stage too.
