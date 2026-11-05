# Notes: mega-refactor-118f-final-boss

## Decisions

- (2026-11-02, owner, item `ITEM.md`) This batch removes the `isLevelDefeated` check for `wily_room`, as a named intended change.
- (2026-11-05, owner) The end scene (`DrWily` spawn, damage lock, controller off) stays in `WilyFinalBoss.onDestroy`. `finalBossPlan` has no exit step.
- (2026-11-05, owner) If slot 0 is dead when the defeat animation ends, the bar goes when the fight closes, not at the respawn.

## Overlap with other branches

- At the scan (`c19e75ad9`): `compactor-level` changes none of `fights/`, `MegaLevelScreen`, `AbstractBoss` or the bosses. `fix/spawner-rearm-margin` merged into `master` at `48a6ff917`; its 1 `MegaLevelScreen` line (`spawnsMan.rearm()` at `PLAYER_SPAWN_INIT`) is not near the boss branches, and this branch starts from a `master` that has it.
- `x6-intro-extra-level` (last commit `3d2a4573b`) adds `D1000.TAG -> MusicAsset.MMX6_BOSS_FIGHT_MUSIC` to the `when` in `FightPlans.find`. It already gets a text conflict there; this branch adds 1 more entry. Keep both sets of entries.
