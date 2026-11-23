# Process log: mega-refactor-178a-bot-input

## Status

- State: merged
- Goal: an AI brain drives player slot 1: a delayed copy of P1's input, with a launch option and the test map `REFACTOR_TEST_178`
- Done: branch start; planning (brief approved 2026-11-22, D1 to D4 as recommended; `PLAN.md` written); `PLAN.md` steps 1 to 6 (automated checks pass, review before the play test done)
- Done also: owner play test, all 8 checks pass (2026-11-23)
- Done also: diff review closed (0 threads)
- Done also: squash merge into `master` (`86e58c2ee`, 2026-11-23)
- Next:
- Brief: https://claude.ai/artifact/44yTaHzqxUghJ99iooaMfR
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-22

- Branch start on `master`; worktree `.worktrees/mega-refactor-178a-bot-input`.
- Planning: scan of slot input, launch options, session rules, camera focus and view-edge warp; `SCOPE.md` drafted;
  brief rendered to `temp/mega-refactor-178a-bot-input/BRIEF.html` and published.
- Owner answered D1 to D4 (all as recommended); `SCOPE.md` approved; `PLAN.md` written (6 steps).

## 2026-11-22 (implementation)

- Two subagents in parallel: step 1 (map, no Gradle) and steps 2 to 5 (code).
- Step 1: `RefactorTest178.tmx`, 7 rooms in one row (214 x 32 tiles); `follow_pit` is 36 x 28 with a second wide
  drop that steps back up (no dead end); `Met`s 26 and 30 tiles from the left wall of `follow_enemies`. Scratch in
  `temp/mega-refactor-178a-bot-input/map/`. Not checked in the game: whether the 5-tile wall of `follow_outrun`
  stops a copy.
- Steps 2 to 5: `core:test lwjgl3:jar` passed (2875 tests, 0 failures). Start values: `DELAY` 16/60 s,
  `STEER_DISTANCE` 1 tile, `ASSIST_HEIGHT` 2 tiles, `STUCK_TIME` 1 s, `ASSIST_JUMP_TIME` 0.5 s, ring of 64 records.
- Plan changes:
  - The bot rule is in `PlayersFocus.inGroup` (`!players.isBot(player.slot)`), not a lambda in `MegaLevelScreen`:
    a `SharedViewTest` case reaches it through the public flow. `MegaLevelScreen` is unchanged; the first caller of
    `isBot` is `PlayersFocus`.
  - While P1 is not live, the brain gives `0` and clears its record (no stale replay after the respawn).
  - While paused, the brain gives its last output, not `0` (no false release and press at resume).
- Mutations (one batched run of `FollowBrainTest`, `LaunchOptionsResolverTest`, `SharedViewTest`; all restored):
  delay halved, no `START` mask, steer reversed, assist without `A`, no pause guard, no bot rule in `PlayersFocus`,
  no `--mode pvp` error. Each failed its target case.
- Known gaps (for the play test): while P1 is dead the camera stays at P1's last place (D1); steer and assist
  compare world X and Y (turned rooms, other gravity); a change of P1's `selectButtonAction` during play does not
  reach the bot; in a filtered run the `SharedViewTest` case "puts no wall while a player is outside it" fails with
  a `ClassCastException` in `Bullet.init` (passes in the full run; not changed).
- Main session: import order fix in `MegamanMaverickGame.kt`. Owner: named player slots are a backlog candidate
  (`TODO.md`), name `PLAYER_1`.

## 2026-11-22 (review before the play test)

- Full automated checks passed (`engine:test core:test core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule`).
- One Sonnet review agent (notes in `temp/mega-refactor-178a-bot-input/review.md`). No bug, no behavior change for
  1 player or 2-human co-op, no style finding. 2 check problems, fixed:
  - Check 2: the assist test was `> ASSIST_HEIGHT`, so a 2-tile step (exactly 64 px) never set off the assist. Now
    `>=`; new `FollowBrainTest` case "holds A when the leader is exactly the assist height above". Mutation back to
    `>` failed only that case; restored.
  - Check 4: a delayed copy follows P1's path, so it clears the wall unless P1 jumps late. The check now says to
    jump at the last moment (under about 1.5 tiles before the wall). Map unchanged.
- Full automated checks passed again after the fixes. Play scripts `temp/play/play-1.sh` to `play-8.sh`.

## 2026-11-23 (play test)

- Owner: all 8 manual checks pass. Diff review open at `http://localhost:5392/diff?ref=781211e25da52174688005fe06c414e6a9e5349c`.
- Diff review: owner closed it with 0 threads. No code change since the last full automated checks (after the
  review fixes), so they were not run again. Diffity server stopped.

## 2026-11-23 (merge)

- `master` changed since the branch start: docs only (`docs/issues/`, `047` docs, `art.md`); no `master` merge into
  the branch.
- Other active branches: `compactor-level` and `078g` also add to `LevelDefinition.kt` and
  `LevelCompletionScreensTest.kt` (list entries; a simple conflict for them later). No other shared files.
- Squash `86e58c2ee`. The owner's unstaged change in `PlayerStatsHandler.kt` (main checkout) is not in it.
- New item `179` (named player slots) from the backlog candidate.
