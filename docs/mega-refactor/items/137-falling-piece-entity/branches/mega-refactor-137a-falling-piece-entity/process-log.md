# Process log: mega-refactor-137a-falling-piece-entity

## Status

- State: merged (2026-11-13)
- Goal: One falling-piece entity for `DebrisPiece`, `BlockPiece` and `WoodCratePiece`
- Done: branch start, scan, `SCOPE.md` and `CONTRACT.md` approved, `PLAN.md` steps 1 to 7 (code, tests, test map); not committed
- Brief: https://claude.ai/artifact/97zRVT184DV1skZZ4QwkHD
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-13

- Branch start (owner chose this item while `060c` runs in another session; merge target: directly into `master`).
- Planning scan (subagent): the 3 classes compared, 12 caller files (8 spawn sites, 5 load sites), tests and data.
  Draft `docs/SCOPE.md` (3 decisions) and `docs/CONTRACT.md`; overlap with `060c`, `138a`, `139a` in `docs/NOTES.md`.
  Helper script for the room scan: `temp/137a/rooms.py` (not committed).
- Brief published; owner answers: D1 A typed def, and `onSpawn` also takes per-value spawn properties: each of the 7 values reads its property first, then the def; with no def, every value must come from properties, or the spawn fails fast (owner, 2026-11-13). The override path is an owner-requested API with no production caller yet: an owner exception to the rule "an API needs a real caller"; tests cover it.; D2 Region keys and one cache in the piece; debris owners keep their load call.; D3 One spin rule: the angle resets to 0 at each spawn. Scope and contract approved.

## 2026-11-13 (implementation)

- `docs/PLAN.md` written (7 steps). All steps done in the worktree; nothing committed yet.
- Step 1: test map `RefactorTest137.tmx`, room `mixed` (16 × 14): floor, ceiling, 2 walls, 2 `WoodCrate`s,
  2 brick `BreakableBlock`s, 2 `SMB3_GoldBreakableBlock`s 3 tiles over the floor, spawn `0`, painted `tiles` (tile
  175). Built with the `level-builder` CLI plus `temp/137a/paint_map.py` (named objects and the `tiles` layer).
  `REFACTOR_TEST_137` (`TEST_LEVEL`) and its `LevelCompletionScreensTest` line.
- Step 2: temporary side-by-side suite `FallingPieceLegacyContractTest` with frozen copies `LegacyDebrisPiece`,
  `LegacyBlockPiece`, `LegacyWoodCratePiece`: 9 of 9 green against the old classes. (2 harness fixes: reset the
  entity pool before each run; spread the seeds, because `java.util.Random` with small sequential seeds gives
  correlated first draws.)
- Steps 3 to 5: `FallingPiece`, `FallingPieceDef` (with `FallingPieceSize`, `FallingPieceSpin`), `BlockPieceColor`;
  new keys `ConstKeys.JITTER` and `ConstKeys.BLINK`; `ConstKeys.THUMP` removed. 8 spawn sites and 5 load sites moved;
  the 3 classes removed; `D1000Rig`, `D1000DefeatTest` and `entity-tags.txt` updated. The suite against
  `FallingPiece`: 9 of 9 green. Decisions in `docs/NOTES.md`; one intended change added to `CONTRACT.md` (the order
  of the random draws in the X6 throw).
- Step 6: permanent `FallingPieceTest` (25 tests). Mutation checks, each restored after the run (failed tests of the
  2 specs out of 34):
  - block spin step 45 → 40: 5 (4 contract, 1 permanent);
  - angle not reset at spawn: 1 (pool reuse);
  - def read before the spawn property: 2 (override tests);
  - no def and no property gives `null`: 3 (the 3 optional keys; the 4 required keys still throw by the null check);
  - fall clamp kept from the last life: 1 (pool reuse);
  - debris jitter 0: 4;
  - blink on even steps: 6;
  - life 2.1 s: 12;
  - X friction always on: 7;
  - missing atlas region kept: 1; unloaded key falls back: 1.
- Step 7: temporary suite and frozen copies deleted (last result 9 of 9; a copy is in `temp/137a/`).
- Full run after the deletion: `engine:test` 675 passed, `core:test` 1731 passed, `core:smokeTest` 455 passed,
  `lwjgl3:jar` built (`Megaman-Maverick-1.0.1.jar`). After it, one `TODO` comment at `FallingPiece.load` (the Kotlin
  `Array` keys, backlog candidate); `core:compileKotlin` passes.
- Review before the play test (1 agent): no blocking bug; every value per kind and all 12 callers match the old code;
  every manual check is valid. Fixed: (1) a wood piece no longer spins in a pause or a world hold: named in
  `CONTRACT.md` as an intended change (no code change); (2) the reuse check said "fast", which avoids the pool reuse:
  reworded; (3) blank lines in `onSpawn` removed; (4) the region lists join the `TODO` and the backlog line for
  LibGDX arrays; (5) a spawn property of the wrong type now throws with the key (new test).
- Owner: all play checks pass; Diffity: no threads. `master` (with `138a`) merged into the branch: one simple conflict in
  `entity-tags.txt` (`master` removed `Wreck`, this branch removed `WoodCratePiece`, on neighbouring lines; both
  removed). Checks after it: `engine` 675, `core` 1738, smoke 454, JAR built. Merged into `master` (owner chose to
  merge before `060c`).
