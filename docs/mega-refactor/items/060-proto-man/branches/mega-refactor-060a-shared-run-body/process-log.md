# Process log: mega-refactor-060a-shared-run-body

## Status

- State: merged
- Goal: Refactor only: move Megaman's run, body and momentum into `players/` functions with parameters; Megaman plays the same
- Done: planning; steps 1 to 5; automated checks pass; `master` (with `146`) merged in; play test passed; squash-merged into `master`
- Brief: https://claude.ai/artifact/NvdV27GDt5bc7Y5CSgfnR7
- Next: `060b` (`Protoman` load and move)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-10

- Branch start. The owner chose to merge the branches of `060` directly into `master`.
- Planning scan (2 read-only agents: code, sprite sheet). Mega's run, body and momentum are `Megaman` receiver
  functions. The owner split the work: this branch is the refactor only, and Proto Man starts in `060b`. The branch
  was renamed from `mega-refactor-060a-proto-run-jump` (no commits yet).
- Brief published and approved (D1 builder with hooks, D2 X stays, D3 `PlayerGravityComponent`, D4 Moon check stays
  in Megaman's parameter). `PLAN.md` written.
- Steps 1 to 5 done:
  - Golden test `MegamanBodyFixturesTest` with `megaman-body-fixtures.txt` (17 states per direction), recorded on the
    old code. A changed value makes it fail (checked by a deliberate edit of one value, then undone).
  - `PlayerGravityComponent`, `PlayerRun.kt` (`RunParams`, `playerRun`, `playerRunButton`, `playerMaxRunSpeed`),
    `PlayerBody.kt` (`PlayerBodyParams`, `playerBody`). The golden test passed unchanged after each step.
  - `MegamanRunTest`: 2 call lines changed from `megaman.run` to `megaman.playerRun(..., megaman.runParams)`.
  - Needle Spin and axe pre-step lines moved from the `DEFAULT` pre-step entry into a `WEAPON` entry after it.
  - `players/README.md` updated.
- Automated checks: `engine:test core:test core:smokeTest lwjgl3:jar` pass (JDK Corretto 17).
- Review before the play test started: 2 subagents (diff equivalence; play check validity).
- Review before the play test, 2 subagents:
  - Diff equivalence: no behavior difference found. No-effect notes: the hooks read `Megaman.body` (same object);
    the `WEAPON` pre-step entry runs between `DEFAULT` and `PRIOR`, and reads nothing `DEFAULT` writes; the debug log
    tag of the run buttons is now `PlayerRun`. Test gaps listed: the hooks. Fixed: 4 hook tests added in
    `MegamanBodyFixturesTest` (body and side properties, head bounce, feet bounce in and out of water, `GutsTank` side
    stick). The run actuators and Megaman's Rodent Claws lambdas are covered by `MegamanRunTest`.
  - Play checks: all 9 commands resolve. 2 rooms could not show what the check asks: `gravity_switch` has only up and
    down switches (now `REFACTOR_TEST_084 --room knockback`, 4 switches); `movement` has no enemies for the weapons
    check (now `--room enemy_aim`). Contract and brief updated.
- `core:test` passes after the fixes.
- `146` merged into `master` (`d7e421d0f`). `master` merged into this branch, no conflicts. `146` adds the engine
  `IPlayer` only (no `IMegaPlayer`) and leaves the shared `playerJumpBehavior` bounds as they were, so the new
  `playerRun`, `playerRunButton` and `playerBody` keep their generic bounds. The last `playerSlot!!` reads (one in
  `megaman/components/ControllerComponent.kt`) belong to item `148`. Automated checks pass on the merge (1775 core
  tests, 0 failures). A fresh review agent then reviews the branch with the merged changes.
- Review after the `146` merge (1 fresh agent; report in `temp/mega-refactor-060a-shared-run-body/review-after-146-merge.md`):
  no high or medium findings; `146` does not break the `060a` code. 3 low findings:
  - L1, fixed: the axe shield offset read `slipSliding` after the shared pre-step set the velocity to zero out of play
    (and after the ice momentum and the clamps). On the first frame out of play, while he ran with `AXE_SWINGER`, the
    offset was 0.75 tiles where the old code gave 0.5. Fix: `PlayerBodyParams.onPreStep`, called in the shared pre-step
    where the old code ran the weapon lines; the `WEAPON` `preProcess` entry is removed. New hook test (fails with the
    old order).
  - L2, fixed (owner: "go with your recommendations"): 7 hook tests in `MegamanBodyFixturesTest` for the feet block
    hooks (land sound, `aButtonTask`), the SMB3 head bump, the feet stick in water, `ICE_FRICTION_FILTER` and the
    water-begin velocity zero. Each test fails when its hook is broken.
  - L3, kept (owner): `PlayerGravityComponent` is added as a component, but no code reads it with `require`. Noted in
    `docs/NOTES.md` for `060b`.
- `core:test core:smokeTest lwjgl3:jar` pass after the fixes.
- Owner play test: the 8 checks pass (2026-11-10). Diffity review (base `d7e421d0f`): 0 threads.
- Branch merge: `master` had no new commits since the `146` merge. The other active branch, `081f`, shares no file
  with this branch; it changes `StunComponent` (`val duration`, `start(duration)` with a default), which keeps the
  calls in `PlayerRunTest` valid. Squash-merged into `master` as `2076b66f6`. No backlog or bug candidates in
  `docs/TODO.md`. `ITEMS.md`, `060` `ITEM.md` and `O8` updated.
- Worktree and branch removed; Diffity stopped. Checks on `master`: `engine:test core:test core:smokeTest lwjgl3:jar` pass
  (2460 engine and core tests, 0 failures).
