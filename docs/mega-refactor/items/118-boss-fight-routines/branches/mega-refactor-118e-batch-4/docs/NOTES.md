# Notes: mega-refactor-118e-batch-4

## Decisions

- (2026-11-05, owner) `FightPlans.find` reads the map `mini`, and the forced `mini` lines in `MoonHead`, `MechaDragon` and `PenguinMiniBoss` go, so the map is the one source.
- (2026-11-05, owner) `TURN_CONTROLLER_ON` may come 1 frame earlier after a mini boss is ready and after its defeat, as with the `118b` mid-level resume.
- (2026-11-05, owner) Comment out `Bospider.kt` (no live map; a double-music line in `onReady`), as `029c` did with `SphinxMiniBoss`.

## Overlap with other branches

- At the scan (`ca1510f0e`): `compactor-level` changes none of `fights/`, `MegaLevelScreen`, `AbstractBoss`, the bosses or the fight tests.
- `x6-intro-extra-level` (last commit `3d2a4573b`) adds `D1000.TAG -> MusicAsset.MMX6_BOSS_FIGHT_MUSIC` to the `when` in `FightPlans.find`. It already gets a text conflict there from `118c` and `118d`; this branch adds entries to the same `when`. The fix stays the same: keep both sets of entries. If `D1000` is ever placed with `mini=true`, it gets `miniBossPlan`.
