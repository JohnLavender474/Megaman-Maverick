# Process log: mega-refactor-011a-game-random

## Status

- State: merged
- Goal: one seedable, interceptable random source with named streams; all random calls go through it
- Done: all `PLAN.md` steps, review before the play test, play test, Diffity round 1, merge; brief (https://claude.ai/artifact/8eEELzdLQRU9i7pCZ68A7a)
- Next: none (merged into `master` 2026-11-15)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-14

- Branch start. Owner decision: merge target is `master` directly.
- Scan by a subagent: inventory in `temp/mega-refactor-011a-game-random/scan.md` (main checkout, not committed). Summary in `docs/SCOPE.md`.
- Draft `SCOPE.md` and `CONTRACT.md`. Brief published: https://claude.ai/artifact/8eEELzdLQRU9i7pCZ68A7a
- Owner answers (2026-11-14): `D1` inject `services.random`; `D2` one stream (not the recommended two); `D3` clock seed, logged, and `--seed`; `D4` core source-scan test. `SCOPE.md`, `CONTRACT.md` approved; brief updated to one stream and published again. `PLAN.md` written.
- Steps 1 to 7 done (commits `f891b9c69` to the README commit). Automated checks: `engine:test`, `core:test` (2025 tests), `core:smokeTest`, `lwjgl3:jar` pass.
- Plan changes: `SeededRandom(seed: Long)` added (caller: `postCreate()`), because the guard test rejected the `java.util.Random` that the game built; `Pendulum` takes a `GameRandom` constructor parameter; `GameEngine.spawnCount` gives the spawn order (caller: `MegaGameEntity.onSpawn`).
- The swap script also changed `getRandom` text inside commented-out code (`SphinxMiniBoss`, `MechaDragon_OLD`); no effect on behavior.
- The guard test failed on 2 real sites on its first run (`MegamanMaverickGame` `java.util.Random` import, `Pendulum` `MathUtils.*` import), so it can fail; both are fixed.
- Review before the play test, 2 agents. Diff review: no range, type or new throw found; fixed: 2 comment-only edits reverted (`SphinxMiniBoss`, `MechaDragon_OLD`), 3 unused `UtilMethods` imports, import order in 2 files, 2 reused arrays for the Wily swoop pick in place of 2 allocations per swoop, guard test widened (`*Random(` constructors except `SeededRandom`, `Collections.shuffle`, a `MathUtils` alias import, the `lwjgl3` sources). Checks review: check 2 room changed `room9` to `room8` (`room9` is after the mini boss room); check 1 wording (`- Seed:` in the terminal output, not the log file); check 6 says rain (the room has no leaves); check 7 expected result changed: the old stale-order fault of the Moon Scythe sort stays (bug candidate in `docs/TODO.md`, `TODO` at the sort).
- Diffity round 1 (2026-11-14): owner asked to rename the launch option `seed` to `randSeed`, because `seed` alone does not say what it seeds. Renamed `--seed` → `--randSeed`, `LaunchOptions.seed`/`ResolvedLaunch.seed` → `randSeed`, `parseSeed` → `parseRandSeed`, the start print → `- Rand Seed:`; docs and contract check 1 updated.
- Diffity round 1 result (2026-11-15): 0 anchored threads; 2 comments in the chat. (1) Rename `seed` → `randSeed`: fixed (above). (2) New `Vector2()` in place of `GameObjectPools`: the two sites (`SabreBlock.kt:112`, `D1000Defeat.kt:118`) existed before `011a` and stay; by the owner's choice the sweep and a `CODE_STYLE.md` rule went into item `045` (master `78be0497e`).
- Play test (2026-11-15): the owner ran check 1 with `--randSeed` (pass), then all 7 checks: "Tests pass". Merge approved.
- Merge of `master` into the branch (2026-11-15): `master` had `024a` (weapons as `WeaponDef`), `060d`, `159a` and docs. One conflict, `MegamanWeaponsHandler.kt`: `024a` moved the Moon Scythe sort and the Rodent Claws angles out of it. Resolved: took `master`'s file, and applied the two `011a` edits at the new places (`MegamanWeaponStates.kt` sort on `spawnOrder` with its `TODO`; `MegamanWeaponFire.kt` angles on `megaman.services.random.nextFloat`). `engine:test core:test core:smokeTest lwjgl3:jar` pass after the merge (the guard test also covers the new `master` code). The Rodent Claws angles are the same range and type; no play check runs again.
