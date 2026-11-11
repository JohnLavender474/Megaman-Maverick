# Process log: mega-refactor-060b-protoman-move

## Status

- State: merged
- Goal: `Protoman` in slot 0 on a test level: idle, step, walk, jump and fall, with an atlas sliced from the sheet
- Done: steps 2 to 5 (atlas, `Protoman`, tests, README)
- Brief: https://claude.ai/artifact/4wGA9VJ33La2zekPryBnTJ
- Next: none (merged 2026-11-11)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-10

- Branch start, after `060a` merged. Merge target: `master` (item decision).
- Planning scan (2 read-only agents; reports in `temp/mega-refactor-060b-protoman-move/`: `scan-code.md`,
  `scan-sprites.md`, the draft atlas and the preview `sprites/compare_megaman_protoman_x4.png`).
- Paused: after the `IPlayer` review the owner chose an abstract `MegaPlayer` base class, built in the refactor-only
  branch `060j` first. This branch goes on after `060j` merges. The scan finding "6 parts with no shared form that X
  already copied" goes to `060j`.

## 2026-11-11

- `060j` merged; `master` merged into this branch (`801816027`). `MegaPlayer` owns the 6 parts that the scan found
  copied, so that question is gone. Draft `SCOPE.md`, `CONTRACT.md` and the brief (with the sprite preview).
- Brief approved (D1 to D4 as recommended); `SCOPE.md`, `CONTRACT.md` approved; `PLAN.md` written.

## 2026-11-11 (steps 2 to 5)

- Step 2, atlas: prep script, prepared sheet and `Protoman.yaml` in `utils/sprite_slicer/sheets/`; frames in
  `assets/sprites/frames/Protoman/`; atlas `assets/sprites/sprite_sheets/Protoman.{txt,png}` (`stand` 2, `run_trans` 1,
  `run` 4, `jump` 1); credit lines in `assets/credits.txt`. The frames are byte-equal to the draft. The slicer tool and
  its tests did not change, so `run_tests.sh` did not run.
- Step 3, class: `entities/protoman/Protoman.kt` and `ProtomanValues.kt`; `PlayerCharacter.PROTOMAN`, the `postCreate`
  block, `TextureAsset.PROTOMAN`, `LevelDefinition.REFACTOR_TEST_060`, the `LevelCompletionScreensTest` row, the
  `entity-tags.txt` line. `TODO`s: listed in `docs/NOTES.md`.
- Step 4, tests: `ProtomanTest` (16 tests: run, jump, gravity sign, step key). Break check: the run impulse negated, the
  jump speed unsigned, the gravity sign fixed to `UP` and the step removed made 7 of the 16 fail (each break caught by
  at least one test); code restored.
- Step 5: `players/README.md` names the 3 player classes and the two forms of a class.
- `core:test` and `core:smokeTest` pass (`LevelCompletionScreensTest` does not read the map file).

## 2026-11-11 (review before the play test, and the fixes)

- Review: 2 read-only agents; reports `review-diff.md` (the branch diff) and `review-checks.md` (the 6 manual checks)
  in `temp/mega-refactor-060b-protoman-move/`. No blocking bug. Findings: the 10 `Megaman::class` `TODO`s named the
  wrong item (`060e`); no swim and no `TODO` for it; the README said `Protoman` copies no part of `Megaman`; no
  `frozen` key and no `TODO`; the currency pickup sound queued in idle roster players (low); the source sheet only
  in `temp/` (decision D2, no change); 2 long lines and an import order; 5 blocks of `Protoman.kt` copied from
  Megaman or X; the `water` pool 6 tiles deep, so he could not jump out (rise in water about 4.1 tiles).
- Owner decisions: the "Scope change after the review" block in `docs/SCOPE.md`.
- Fixes:
  - Shared: `players/PlayerSprite.kt` `applyHideAndFlash` (the alpha flash and the hide rule) and
    `Direction.upSign()` in `players/PlayerGravity.kt`. Megaman, X and Protoman call both; `PlayerGravityComponent`
    and Megaman's direction setter use `upSign()`. The 3 hide rules had the same truth table (frozen shows, out of
    play hides, a teleport that is not a `PipePortal` hides); the 3 sign rules were the same.
  - `TODO`s in `Protoman.kt` (item `060`): the updatables list, the land sound, the `GutsTank` side-stick rule. The
    swim `TODO` (`060g`) at `onWater`, the frozen `TODO` (`060e`) at the animation key.
  - The 10 `Megaman::class` `TODO`s now name item `030`.
  - `players/README.md`: the shared rules and the parts that are still per class.
  - Nits: the 2 long lines, the `protoman` import order in `MegamanMaverickGame.kt`.
  - `RefactorTest060.tmx`: the pool is 3 tiles deep (`Water` id 26 `y=288 h=96`, block id 20 `y=384 h=192`, tile
    `175` in rows 12 to 14, columns 106 to 115). Check (`level_analyzer` and `temp/.../check_map.py`): 0 block cells
    with no tile, 0 tiles with no block, spawn `2` in `water`, the water top level with both banks (`y=288`).
  - Bug candidate in `docs/TODO.md`: the currency pickup sound.
  - Tests: a "the sprite" block (4 tests) in `ProtomanTest` and in `MegamanXTest` drives the real sprite pre-process.
    Break check: the frozen rule and the flash removed made the 4 frozen and flash tests fail; code restored.
- Owner play check 1 (`walk`): Proto Man plays as expected for simple run and jump. Owner nitpick in Diffity:
  blank lines between single statements; removed in `Protoman.kt`, `ProtomanTest.kt` and Megaman's direction setter.
- Owner request: Proto Man v2 frames in the `Megaman_v3` style (stand, step, run, jump), 2 draft rounds, approved.
  `Protoman_v2` frames and atlas; `TextureAsset.PROTOMAN` reads it; `TODO` for the other frames (see `docs/NOTES.md`).
  `engine:test core:test core:smokeTest lwjgl3:jar` pass (675, 1361, 452). Uncommitted while the Diffity round is open.
- Owner play test passed: all 4 Proto Man checks (`walk`, `jump`, `water`, `transition`, with the v2 frames) and the
  5 control checks (`REFACTOR_TEST_029` `movement` and `enemy_aim`, `REFACTOR_TEST_084` `knockback`,
  `REFACTOR_TEST_081` `flat`, `X6_INTRO` `room1`).
- Diffity round 1: 1 owner remark (blank lines between single statements), fixed. 0 open threads. Owner: review done.
