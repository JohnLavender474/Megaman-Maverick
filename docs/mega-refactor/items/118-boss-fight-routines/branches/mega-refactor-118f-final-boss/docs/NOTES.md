# Notes: mega-refactor-118f-final-boss

## Decisions

- (2026-11-02, owner, item `ITEM.md`) This batch removes the `isLevelDefeated` check for `wily_room`, as a named intended change.
- (2026-11-05, owner) The end scene (`DrWily` spawn, damage lock, controller off) stays in `WilyFinalBoss.onDestroy`. `finalBossPlan` has no exit step.
- (2026-11-05, owner) If slot 0 is dead when the defeat animation ends, the bar goes when the fight closes, not at the respawn.

## Overlap with other branches

- At the scan (`c19e75ad9`): `compactor-level` changes none of `fights/`, `MegaLevelScreen`, `AbstractBoss` or the bosses. `fix/spawner-rearm-margin` merged into `master` at `48a6ff917`; its 1 `MegaLevelScreen` line (`spawnsMan.rearm()` at `PLAYER_SPAWN_INIT`) is not near the boss branches, and this branch starts from a `master` that has it.
- `x6-intro-extra-level` merged `master` at `3637a5ff7` (its last commit is `145f93af9`). In this branch's files it now changes only `FightPlans.kt`: the `D1000` import and `D1000.TAG -> MusicAsset.MMX6_BOSS_FIGHT_MUSIC` after the `BigAssMaverickRobot` line. This branch puts the `WilyFinalBoss` entry just before `else`, and its import after `TimberWoman`, away from those lines, so the next merge has no text conflict. `D1000` has no `mini` on its map, so it keeps the stage or mid plan from `end`.
