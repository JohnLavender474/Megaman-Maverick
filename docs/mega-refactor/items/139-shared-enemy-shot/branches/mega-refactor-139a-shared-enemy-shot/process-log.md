# Process log: mega-refactor-139a-shared-enemy-shot

## Status

- State: merged (2026-11-13)
- Goal: A shared simple enemy shot, starting with `JunkShot`
- Done: branch start; scan; `SCOPE.md`, `CONTRACT.md` approved; `PLAN.md` steps 1 to 8 (code, `EnemyShotTest`, automated checks); not committed
- Brief: https://claude.ai/artifact/B2GxKBJTa6vNmJSRhmGJcM
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-13

- Branch start (owner chose this item while `060c` runs in another session; merge target: directly into `master`).
- Scan of `master` at `add9d66d2` (subagent): 73 projectile classes classified; `JunkShot` already has a typed
  `Type` for 3 enemies; damage per class or tag listed. Draft `SCOPE.md` (decisions `D1` name, `D2` damage key,
  `D3` first-branch set), draft `CONTRACT.md`, overlap in `docs/NOTES.md`. No code change. Helper script:
  `temp/mega-refactor-139a-shared-enemy-shot/rooms.py` (enemy to room lookup in a TMX file).
- Brief published; owner answers: D1 Rename `JunkShot` to `EnemyShot`; the atlas regions keep their names.; D2 One tag entry in `PlayerDamageNegotiator` reads `kind.damage`.; D3 The 4 `JunkShot` kinds, `RollingBotShot` and `FirePellet`. Scope and contract approved.
- `PLAN.md` written (8 steps) and done in the same session (subagent; no commit, no game launch):
  - `JunkShot` renamed to `EnemyShot` (`Type` to `Kind`, `type` to `kind`); the atlas regions keep `JunkShot/<key>`.
  - The kind holds art (`Ticks` or `Grid`, with atlas and region), body size in tiles, sprite turn, draw priority,
    `damageable`, `shootable`, `diesOnHit`, `diesOnShield`, death effect and death sound. `onSpawn` and the sprite
    pre-process set every per-kind value.
  - `RollingBotShot` and `FirePellet` moved to kinds `ROLLING_BOT` and `FIRE_PELLET`; their classes, their
    `ProjectilesFactory` keys and pools and their `entity-tags.txt` lines are deleted. `RollingBot` and `DemonMet`
    pass `TYPE` and `TRAJECTORY`.
  - `PlayerDamageNegotiator`: one tag entry, `kind.damage`. Damage checked in the old code: `CORE` 2, `JUNKROID` 1,
    `JUNKROID_HARD` 1, `HOVER_GUNNER` 3 (old `Type` values); `RollingBotShot` and `FirePellet` 3 (no tag entry,
    default `Size.MEDIUM` of `AbstractProjectile`, which the negotiator maps to 3).
  - Decision: the Rolling Bot kind has no `BODY` fixture (see `docs/NOTES.md`). No live behavior changes.
  - New `EnemyShotTest` (44 cases): real `WorldSystem` with the game contact rules, a `TestBodyPlayer` with a
    `DAMAGEABLE` fixture, a player `Bullet`, a `Block` and a test shield entity. Mutations, each caught and restored:
    a damage value, `shootable` of the hard kind, `diesOnHit` of the pellet, `diesOnShield` of a junk kind, an extra
    `BODY` fixture, the draw-priority reset, the `DAMAGEABLE` reset, the rotation reset, the death effect and sound,
    the gravity reset, a body size.
  - Helper scripts: `temp/139a/run-shot-test.sh` (runs `core:test` with a Kotest spec filter in the environment
    variable `kotest_filter_specs`; `-P` does not reach the test JVM) and `temp/139a/mutate.py`.
  - Automated checks, one run of `engine:test core:test core:smokeTest lwjgl3:jar`: all pass. `engine:test` 675,
    `core:test` 1750 (1706 before, plus 44 in `EnemyShotTest`), `core:smokeTest` 455, 0 failures; the JAR builds.
- Review before the play test (1 agent): no code bug; the `BODY` drop is correct for play; every manual check is
  valid. Fixed: (1) the lost toxic splash at the `ReactorMan` `room4` waterfall is now named in `CONTRACT.md` and
  `NOTES.md` (cosmetic, accepted); (2) the `NOTES.md` sentence on enemy `hitBody` readers corrected (`Bullet` too);
  (3) named arguments `loop = true` and `loop = false` in `EnemyShot`. Gaps noted, not fixed: the mock atlas cannot
  catch a wrong region name (the smoke test does), and no test drives the `RollingBot` and `DemonMet` callers.
- Owner: all play checks pass; Diffity: no threads. `master` (with `138a` and `137a`) merged into the branch: one simple
  conflict in `D1000Rig.kt` (one line; `137a` renamed `DebrisPiece` to `FallingPiece`, this branch renamed `JunkShot`
  to `EnemyShot`; both kept). Checks after it: `engine` 675, `core` 1782, smoke 452, JAR built. Merged into `master`
  (owner chose to merge before `060c`).
