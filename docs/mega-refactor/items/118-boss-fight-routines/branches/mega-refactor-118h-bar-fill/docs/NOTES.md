# Notes: mega-refactor-118h-bar-fill

## Decisions

- (2026-11-05, owner) A lean `118h`: remove `HealthFillType` and its dead `ALL_AT_ONCE` branch, and let the fight run the bar fill, so the bar leaves the screen's `when` chain. No `HudBars`, `BarSpec` or `BarSource`: their only caller would be the boss bar (`O1-R5`). The merge docs add them to the item "Possible later" table.

## Overlap with other branches

- At the scan (`e33f2e491`): `compactor-level` and `x6-intro-extra-level` change none of `fights/` (except the `D1000` lines in `FightPlans.kt`), `MegaLevelScreen`, `BossHealthHandler` or `HealthFillType`.
