# Process log: mega-refactor-183b-charged-shot

## Status

- State: merged
- Goal: `ChargedShot`, its explosion and its residual draw only at multiples of 90 degrees
- Done: planning (scope approved 2026-11-23: D1 a, D2 b, D3 a; `PLAN.md` written); steps 1 to 7; play test passed; merged
- Brief: https://claude.ai/artifact/DNjsD3qB57V6KdNtWrZaGG
- Next:
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-23

- Branch started from `master`. First branch of `183`; merges directly into `master` (owner, 2026-11-23). `183a` (gravity turn) is deferred.

## 2026-11-23 (planning)

- Scan of the 3 classes, their spawners, the art and the other worktrees (no overlap). Draft `SCOPE.md` and the brief, published.
- Owner answered: D1 diagonal explosion art, D2 diagonal residual art too, D3 art here by a Fable agent. `SCOPE.md` approved, `PLAN.md` written.
- Plan change (owner question on the brief): the rounding function is the engine extension `Float.roundAngleToStep`, not a `core` util; the 3 inline copies get a `TODO`.

## 2026-11-23 (implementation)

- Step 1: test map `RefactorTest183.tmx` (rooms `wall_bounce`, `third_bounce`, `diagonal_hit`, `shoot_down`, `gravity_left`) and `REFACTOR_TEST_183`, built by a `level-builder` subagent. `diagonal_hit` uses `Shotman`, not `Met`: the `Met` helmet bounces the shot (`hitShield`). Control rooms proposed by the agent, not yet read: check 6 `REACTOR_MAN` `room1`; check 7 `INTRO_STAGE` `room1` and `X6_INTRO` `room1`. Renders in `temp/mega-refactor-183b-charged-shot/map/`.
- Step 4: `Float.roundAngleToStep` with tests; `TODO` at the 3 inline copies (`HomingMissile`, `CactusMissile`, `WilyCapsuleTentacleScissor`). `ChargedShot` rounds the trajectory at spawn and after each bounce with a table of 8 unit vectors, so an axis trajectory stays bit for bit the same. `ChargedShotTest`: 32 bounce cases (4 gravity x 4 motion x shot down), axis spawns unchanged, off-axis spawn to 45. `engine:test` and `core:test` pass.
- Mutation check (1 run): the bounce without rounding and the spawn without rounding. 25 cases failed: the 24 bounce cases whose old angle is not a multiple of 45, and the off-axis spawn case. The 8 bounce cases with an axis result and the 4 axis spawn cases pass on both (expected). Code restored.
- Step 2: the Fable art agent drew the 6 diagonal strips (`temp/mega-refactor-183b-charged-shot/art/`, `check.py` passes). Sources and colour mapping in `sources.md` there. Waiting for the owner's review.
- Owner approved the art (2026-11-23): pack it; residual wing tips swept forward (art agent option a); half explosion with one green `58D854` (option a). Not in scope, noted by the art agent: the straight strips use some colours off the NES palette (for example `48D848` against the diagonal `58D854`), so a shot changes shade a little when it turns diagonal.
- Step 3: the 6 strips copied to the frames folders, the 3 atlases repacked with their `.tps` projects (`Basic`, no project change). `temp/mega-refactor-183b-charged-shot/compare_atlases.py`: all old regions keep name and pixels (123 in `Projectiles1`, 25 in `Explosions1`, 71 in `Decorations1`).
- Step 5: the 3 classes draw the straight or the diagonal animation from the angle rounded to 45, turned by `angle` or `angle - 45`. `ChargedShotTest` adds draw-turn cases (shot after each bounce case; explosion and residual at the 8 angles). The spec now calls `clearAllMocks(answers = false)` in `afterEach`: without it the `core:test` JVM ran out of its 1 GB heap.
- Mutation checks (2 runs): shot turn without `- 45` (24 diagonal bounce cases failed), explosion turn without `- 45` (the 4 diagonal explosion cases failed), residual with no rounding (all 8 residual cases failed). Code restored. `core:test`, `core:smokeTest`, `lwjgl3:jar`, `detektMain` and `core:verifyRandomRule` pass.
- Step 6: README credit under "Megaman". Owner (2026-11-23): the straight charged shot frames are part of the Megaman spritesheet by SamuelX4 and oldlavygenes. Art sources: `temp/mega-refactor-183b-charged-shot/art/sources.md`.
- Step 7: review before the play test, 1 Sonnet agent. No bug and no contract finding. Notes: (1) the draw rule repeats in the 3 classes, as allowed by the scope; no change. (2) In `diagonal_hit` the shot hits `Shotman` and explodes (it does not pass through); play script text says so. (3) Checks 3 and 6 need a walk right first; in the play scripts. Contract rooms for checks 6 and 7 set in `SCOPE.md`. Play scripts `temp/play/play-1.sh` to `play-8.sh` (check 7 split into Megaman and X).

## 2026-11-23 (merge)

- Owner: play tests pass, ready to merge. Diffity: no threads.
- `master` merged into the branch first (it changed `LevelDefinition.kt` and `LevelCompletionScreensTest.kt`, from `182a`). Simple conflicts, both resolved by keeping both entries (`REFACTOR_TEST_182`, then `REFACTOR_TEST_183`). Automated checks of the contract pass after the merge.
- Other active branches: `compactor-level` adds to the same 2 level lists, `078e` adds to `README.md` credits; list additions only, no risk to behavior.
- Squash-merged into `master`. The owner's unstaged edit of `PlayerStatsHandler.kt` in the main checkout stays out of the commit (owner, 2026-11-23).
