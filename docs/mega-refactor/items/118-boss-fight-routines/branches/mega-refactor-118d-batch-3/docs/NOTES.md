# Notes: mega-refactor-118d-batch-3

## Decisions

- (2026-11-04, owner) Move `SigmaRat` with no live fight, with tests only, so that `118g` leaves no boss on the old flow.
- (2026-11-04, owner) The key-based gate unlock stays in `118j`. The `118` `ITEM.md` decision line that named `118d` is corrected.

## Overlap with other branches

- At branch start (`e7ee29b36`): `compactor-level` changes none of `fights/`, `MegaLevelScreen`, `AbstractBoss`, the 3 bosses or the fight tests.
- `x6-intro-extra-level` (last commit `3d2a4573b`) adds `D1000.TAG -> MusicAsset.MMX6_BOSS_FIGHT_MUSIC` and its import to the same `when` in `FightPlans.find`. Its merge base is before `118c`, so it gets a text conflict in that `when` (and maybe in the `FightTest` lists) when it takes in `master`. The fix is to keep both sets of entries. No change in this branch.
