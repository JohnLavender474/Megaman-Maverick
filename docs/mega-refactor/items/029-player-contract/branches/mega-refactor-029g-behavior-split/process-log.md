# Process log: mega-refactor-029g-behavior-split

## Status

- State: merged
- Goal: split the player behaviors into 8 factory functions with no change in their bodies; tests fix the order
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-25

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `ab5f8cd31`. Findings and open questions in `docs/NOTES.md` ("Scan"). No scope yet.

- The owner accepted the scan recommendations 1-9 and the core placement of `CharacterDef`. Decisions in `docs/NOTES.md`. Draft `docs/SCOPE.md` written.
- The owner asked about the values table; decision: no table, behavior factories take their values as parameters, numbers stay in `MegamanValues` (`docs/NOTES.md`, "Decisions"). `docs/SCOPE.md` rewritten. One open question: the package of `CharacterDef`.
- Independent review of the draft scope (`docs/NOTES.md`, "Review"). The owner chose the smaller cut: split the behaviors only, with the order and two-instance tests; `CharacterDef` goes to a Proto Man item (backlog candidate in `docs/TODO.md`). `docs/SCOPE.md` rewritten. On `master`: "`CharacterDef` design" added to the item `NOTES.md`; decision added to `ITEM.md`; the Proto Man open question removed from it.
- The owner approved `docs/SCOPE.md` and the rename of the branch from `mega-refactor-029g-character-def` to `mega-refactor-029g-behavior-split` (folder, `ITEMS.md` row, `ITEM.md` branch split).
- Draft `docs/CONTRACT.md` written.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written.
- The owner approved `docs/PLAN.md`. Branch start (step 1): `ITEMS.md` row and `ITEM.md` set to active; worktree created.
- Step 2 (baseline, worktree on `37a488841`): clean build passes; 470 engine and 142 core tests, 0 failures; 114 warnings.
- Step 3: a subagent added the room `movement` to `RefactorTest029.tmx` with the `level-builder` skill (map widened from 332 to 366 columns; no existing object moved). Room 32x14 tiles: spawn `16` on the left; a wall-jump shaft between a hanging wall and a standing wall (4-tile gap); a ledge with a `Ladder` to the floor; a passage 1 tile high under a 4-tile block; a `Water` pool 6 wide and 4 deep with rims. Tile 175 painted on all 298 block cells. A launch of `--room movement` ran with no exception until the timeout.
- Step 4: `MegamanBehaviorsTest` (builds a `Megaman` as `MegamanHeartTankTest` does and calls `defineBehaviorsComponent()`): the 8 keys in the evaluation order; two `Megaman` objects get a different object for each of the 8 keys. Both pass on the code before the split. Build passes; 470 engine and 144 core tests, 0 failures.
- Step 5: the 8 behaviors moved to `entities/megaman/behaviors/` (one file and one `internal fun Megaman.<name>Behavior(): IBehavior` each), with a script that moved the line ranges as they were; each file holds the tag constant that only its behavior uses (the climb also `MEGAMAN_LADDER_MOVE_OFFSET`); the imports follow the code (`bodyOverGround` from `components` in the jump, air dash and ground slide). `defineBehaviorsComponent` keeps the 8 `addBehavior` calls in the same order, with the `024` `TODO`. A script compared each body with `master` with all whitespace removed: 7 are the same apart from `val <name> =` becoming `return`; the air dash also has `this@airDashBehavior` in place of `this@defineBehaviorsComponent` (the receiver label of the old wrapping function; the same `Megaman`). The step 4 tests pass with no change. No change in `MegamanValues.kt`, `Megaman.kt` or the other components. Clean build passes; 470 engine and 144 core tests, 0 failures; 114 warnings. A launch of `--room movement` ran with no exception until the timeout.
- Step 6: the `MegaPlayer.megaman` `TODO` names `024` for the loadout reads and the second playable character (Proto Man item) for `running` and the sprite look. Plan detail (no approval needed): the step 6 grep also found 7 older `TODO`s that named `029g` (the stomp bounce in `Goomba`, `BulletBill`, `RedKoopa`, `RedKoopaShell`, `GreenKoopa`, `GreenKoopaShell`, and the boost in `SwimBooster`; written in `029b`/`029d`, missed by the scan). `029g` no longer does that work, so they now name the Proto Man item; comment text only, wrapped under 120 columns. The grep finds no `029g` in `core/src/main` or `lwjgl3/src`. Build passes; 470 engine and 144 core tests, 0 failures (the one warning of the incremental build is the existing unchecked cast in `BulletBill:73`, part of the 114).
- Step 7, automated checks (on `ab73ed5c7`): clean build passes; 470 engine and 144 core tests (142 on `master` plus 2 new), 0 failures; 114 warnings (same as `master`); `lwjgl3:jar` builds; `contact-rule-selection.txt` unchanged; the order and two-instance tests pass; the body comparison (step 5) shows no change except `return` and the air-dash receiver label; no change in `MegamanValues.kt`, `Megaman.kt` or the other player components (only `BehaviorsComponent.kt`, the 8 new files, `PlayerExtensions.kt` and 7 comment-only `TODO` lines changed); the `TODO`s exist (`024` at `defineBehaviorsComponent`; the accessor names `024` and the Proto Man item). Waiting for the owner's play test.
- Owner's play test of `movement`: the ladder had no tiles and no `LadderTop`. Fix by a subagent (owner's request), after it read the ladders of 7 live stages: gid 131 (`Tileset1.tsx` local id 130) painted on the 8 ladder cells; `LadderTop` (id 145, `blocks`, 32x8 px at the ladder's x and y) added. Small out-of-scope QoL (owner's request): the `level-builder` skill gets a "Ladders" section (the `Ladder`, the `LadderTop` and the painted tiles), and `utils/level_builder` gets an `add-ladder` command (`Ladder` plus `LadderTop`) with a test step `19_add_ladder`; `utils/level_builder/run_tests.sh` passes. A launch of `--room movement` ran with no exception until the timeout.
- Step 7, play test: the owner played every manual check in `docs/CONTRACT.md` (`movement`, `gravity_switch`, `GLACIER_MAN` `room1`) and reported "All manual tests pass". The contract checks are done. The owner approved the merge ("Branch approved").
