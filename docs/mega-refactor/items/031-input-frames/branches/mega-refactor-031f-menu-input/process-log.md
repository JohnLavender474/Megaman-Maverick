# Process log: mega-refactor-031f-menu-input

## Status

- State: active
- Goal: the menus and skip screens read frames, with `Esc` and the back action; the `controllerPoller` facade goes
- Done: the scan on `c6b8cecf7`, `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` (approved), plan step 0
- Next: plan step 1
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- No branch exists yet. The folder was prepared on `master`. This is the "menu-input branch" of the `031` decisions
  of 2026-10-26 (item `ITEM.md`).
- Proposed goal (not a scope; the scan and `docs/SCOPE.md` decide):
  - The menus (`MegaMenuScreen` and its subclasses), the skip screens (`LogoScreen`, `AbstractStoryScreen`,
    `GameOverScreen`, `GetWeaponScreen`, `SimpleInitGameScreen`, `CreditsScreen`) and `ScreenController` read frames,
    not `game.controllerPoller`.
  - `Esc` is back in menus and pause in gameplay (`031` decision of 2026-10-25, with a `TODO` to consider another
    action later). The back action in menus.
  - Removals when no caller is left: `game.controllerPoller`, `FrameControllerPoller` and `FrameControllerPollerTest`,
    the engine `IControllerPoller`, and the diagnostics entry label `"controllerPoller"` in
    `MegamanMaverickGame.render`.
- The scan waits for `031e` to merge, because `031d` and `031e` change the code that this branch reads.

## 2026-11-01

- No branch exists yet. Scan on `master` at `c6b8cecf7`, read only. The game was not launched. Read: the root and
  `docs/mega-refactor/` `CLAUDE.md`, `docs/issues/README.md` and the 4 open issues on pause and input, the item `ITEM.md` and
  `NOTES.md`, the `031e` folder, the `012`, `013`, `034`, `047`, `055`, `082` and `084` items, the `051e` scope and diff, all menus and
  skip screens, `MegaLevelScreen` (pause and frame order), `MegamanMaverickGame.render`, `AbstractMenuScreen`, `KeyboardInput`,
  `InputFrameBuilder`, and the `gdx-backend-lwjgl3` `1.14.2` sources (`DefaultLwjgl3Input`, `Lwjgl3Window`, `Lwjgl3Application`).
- Search result: 9 files contain `controllerPoller`, 12 files contain it or `IControllerPoller` or `FrameControllerPoller`, 9 read
  sites in 8 files.
- Draft `docs/SCOPE.md` written, with 11 open questions. Main results:
  - The poller has no state. It reads the frame that `render()` writes once, so the 9 reads move with no change in any frame.
  - `Esc` reaches nothing today. It has no action in menus or gameplay.
  - `ScreenController` is dead code: `screenController` has no assignment.
  - Bug candidate, by reading only: a `START` held at the start of a lock pauses the game with a dead menu (`docs/SCOPE.md`, bug
    candidate 1, question 3).
- Overlaps are in `docs/SCOPE.md`, because this session had leave to write only that file and this log. Copy them to `docs/NOTES.md`
  at branch start.

## 2026-11-01 (contract)

- No branch exists yet. Draft `docs/CONTRACT.md` written on `master`, read at `72be803bd`. The code is the same as at `a41a82ceb`: the commits
  between them change only docs. The game was not launched, and `~/.prefs` was not read. The check counts: 18 automated, 33 named tests, 11 review,
  45 manual, plus `S1` and `X1`.
- The owner changed section 4 and intended change 6 of `docs/SCOPE.md` during this work (`72be803bd`). The contract follows the new text.
- Lock room: `REFACTOR_TEST_051 --room basic`. The spawn is 12 tiles left of the open right edge, with no enemies. `REFACTOR_TEST_029` and
  `REFACTOR_TEST_031` have no transition near a spawn.
- No check needs a known prefs state, so the contract has no backup step. `S1` and `X1` compare file hashes, as a read-only proof.
- Text in `docs/SCOPE.md` to correct is in the contract, open question 5. This session did not change that file.

## 2026-11-01 (contract approved)

- The owner approved `docs/CONTRACT.md` (verbatim: "yes"), with the recommendations of questions 2 to 6. `docs/SCOPE.md` text corrected as question 5 says, with the 2 pause seams in its API table. No answer changed.
- Issue filed on `master`: `docs/issues/2026-11-01-start-release-at-lock-start-pauses.md`.

## 2026-11-01 (plan)

- No branch exists yet. Draft `docs/PLAN.md` written on `master`, read at `5c02dc8aa`. No build, no launch, no commit. `~/.prefs` was not read. The 2
  local edits of the owner were not touched.
- 15 steps: 0 branch start, 1 baseline, 2 and 3 frames, 4 the system frame, 5 to 8 the back action, 9 and 10 the pause seam, `Esc` and the lock tests,
  11 removals, 12 contract checks, 13 the owner's play test, 14 merge. The contract's own text says "plan step 1" for the baseline, so the
  baseline is step 1 and the branch start is step 0.
- All 109 checks have a line in the check map: 18 A, 33 T, 11 R, 45 manual, `S1` and `X1`.
- `SCOPE.md` and `CONTRACT.md` line numbers were checked on the current code. The moved numbers are in the plan table. The contract and scope text that no
  longer fits is plan open question 1. This session changed neither file.
- Read at `084g` (`fd8e5e85a`, and its worktree docs): no shared file with A6, A7 or A8. `LevelRoomHandler.kt` is in the merge rule of step 14.
- A design choice in the plan: `isPauseRequested` takes a `ButtonStatus` for `Esc`, not the system frame. So `MLS` keeps the `Esc` read and the `TODO`,
  and A15, A16 and A18 stay true as written.

## 2026-11-01 (plan approved, branch start)

- The owner approved `docs/PLAN.md` (verbatim: "yes"), with a) on its 3 questions. `docs/CONTRACT.md` and `docs/SCOPE.md` text corrected as question 1 says: A2, A3, A11, R6, a note on the moved line numbers, section 4, the API table, "Measured size" and 2 question numbers. No answer changed.
- The 2 line numbers of the boss-defeat lock in the issue file are now `:754`.
- Plan step 0: branch start on `master`.
