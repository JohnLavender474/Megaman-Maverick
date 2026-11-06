# Notes: mega-refactor-118j-gate-unlock

## Decisions

- (2026-11-05, owner) The fight plan reads `boss_key` from the boss map props; `AbstractBoss.bossKey` goes.
- (2026-11-05, owner) Boss keys and gauntlet keys share 1 namespace in a level. A `Gate` with both properties throws at spawn.

## Overlap with other branches

Recheck at branch start (`73fdb0f8c`, 2026-11-06):

- `compactor-level`: no gate, gauntlet or fight code; `Compactor.tmx` has no `boss_key` or `gauntlet_key`.
- `x6-intro-extra-level`: changes only the `D1000` lines of the `when` in `FightPlans.find`; the new key read is after the `when`. `X6Intro_16x14_v1.tmx` has no key. Its 2 `D1000` reads from `118i` still need the change named in the `118i` merge entry.
- `mega-refactor-081a-player-lifecycle`: no commits yet; its docs name no gate, gauntlet or fight file.

Recheck before the merge (2026-11-06):

- `master` changed since the branch start, but not in a file of this branch (`Megaman.kt` imports, and docs). No merge of `master` into the branch.
- `mega-refactor-038a-respawn-routine` (new): it changes `EventType`, which this branch also changes, so a text conflict at its merge is possible. It does not change `Gate`. `038b` moves the 24 `PLAYER_SPAWN` level-reset listeners, `Gate` among them, and must also change the `PLAYER_SPAWN` case of `GateTest`.
