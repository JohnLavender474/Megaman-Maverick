# Process log: mega-refactor-084e-class-line

## Status

- State: merged
- Goal: Remove the interfaces on `Megaman` that have no reason left, and the dead player code; the game plays the same.
- Done: branch start (docs commit on `master`), worktree, scan, `docs/SCOPE.md` (approved 2026-10-31), `docs/CONTRACT.md` (approved 2026-10-31), `docs/PLAN.md` (approved 2026-10-31), plan steps 1 to 9, the automated checks, the owner's play test.
- Next:
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-31

- Branch start. The owner agreed to this branch (see the 084 `ITEM.md`, "Decisions"). Created the branch and its worktree `.worktrees/mega-refactor-084e-class-line/` from `master`.
- Scan of the 5 interfaces, `input`, the fixture accessors, the sprite helpers and the dead code. Draft `docs/SCOPE.md` written for approval.
- The owner approved `docs/SCOPE.md`. Draft `docs/CONTRACT.md` written.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written.
- Step 1 done. A subagent added the room `cart` to `RefactorTest084.tmx` (20 x 14 tiles, above `out_of_bounds`, 1 tile gap). The `Cart` has `spawn_type=spawn_room` and `spawn_room=cart`, unlike `Test1.tmx`: `Cart` culls on `PLAYER_SPAWN`, and an in-camera spawner would not fire again after a death. So the cart appears when the player is ready, not during the beam-in. Baseline launch: code 124, no exception.
- Steps 2 to 5 done. Tests: 976 pass. The new rule tests use `SecondPlayerEntity` (test sources), a registered player that is not `Megaman`, so each one failed on the old check before the change. The `UnderwaterFan` test runs with no assets.
- In step 5 a script made the 6 small edits of the `isPlayer` swap, in place of the Edit tool. The plan permits a script for repeated import swaps only. The output is in the commit diff.
- Step 6 done. Step 7 done, with a change to `docs/SCOPE.md` item 6 and `docs/PLAN.md` step 7: the scope named "the users of `Megaman.bodyFixture`" as the first caller of a new `IGameEntity.bodyFixture`, but that accessor had no readers. I added the API, the compiler showed no user, and a `grep` confirmed it. By the "no caller, no API" rule the API is removed. The scope also said the accessors had users in `M/`; 13 files, most of them outside `M/`, import them. These are import swaps only.
- Step 8 done. Step 9 automated checks at `bde3ebcd1`: `engine:test`, `core:test` and `lwjgl3:jar` pass, 976 tests. The `grep` checks find nothing, and 16 casts to `Megaman` remain. The class line has 10 interfaces. A launch of `cart` gives code 124 with no exception. Review of each added line that is not an import against "Frame order": no line moves. `master` changed only the `051` `NOTES.md` since the branch start, so no merge is needed.
- The owner questioned the sprite helpers on `IGameEntity`: these functions apply only to Mega Man. The owner approved the change to `docs/SCOPE.md` item 7. `getSpritePriority` is now `getMegamanSpritePriority(out)` with no receiver. `shouldFlipSpriteX` and `shouldFlipSpriteY` are gone, and the 3 callers read `facing == Facing.LEFT` and pass `false`. The commented-out line in `MegamanTrailSprite.kt:75` still names the old functions.
- Owner play test: in `cart`, Mega passed through the `Cart`, with no reaction. Cause: the room held the old `Cart`, which is not solid and has no play effect on `master` either; `DESERT_MAN` uses `CartV2` (see `docs/NOTES.md`, "The two carts"). My contract check was wrong: it said the cart moves, and I had not checked that against the code or against a stage. No game code changed. At the owner's request, the room now holds a `CartV2` with `facing=right`, as in `DesertMan_16x14_v4.tmx`. `docs/SCOPE.md` item 10 and `docs/CONTRACT.md` changed. A launch with `--logTags CartV2` shows `init()` and `onSpawn()`, with no exception. Game logs go to `assets/logs.txt`.

## 2026-11-01

- The owner reported "tests pass": the play test of `docs/CONTRACT.md` passes, with `cart` retested on the `CartV2`.
- Automated checks again at `44660dced`, because the code changed after `bde3ebcd1`: `engine:test`, `core:test` and `lwjgl3:jar` pass, 976 tests, 0 failures. The `grep` checks find nothing, and 16 casts to `Megaman` remain.
- `master` has no change outside `docs/` since the branch start, so no merge is needed. Overlap with the other active branches is in `docs/NOTES.md`.
- The old `Cart` backlog candidate in `docs/TODO.md` is now covered by item `092`.
- The owner approved the merge (verbatim: "merge"). In the same period `051` merged into `master`, with 30 changed code files, so `master` went into this branch at `ab2e1bfb5`. One conflict, the imports of `GravityBlock.kt`: `master`'s side kept. After `./gradlew clean`: engine 600, core 461, 0 failures, `lwjgl3:jar`, 96 unique warnings. The `grep` checks find nothing, and 16 casts to `Megaman` remain. A launch of `cart` gives code 124 with no exception. The play test was not repeated on the merged code.
