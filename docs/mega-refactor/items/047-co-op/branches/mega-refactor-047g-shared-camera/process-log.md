# Process log: mega-refactor-047g-shared-camera

## Status

- State: merged
- Goal: phase 1 of the shared screen: with `--players 2`, a group camera with edge walls in one room with no exits
- Brief: https://claude.ai/artifact/Q6XpNfvBmYcUoyaEy9jotU
- Done: plan steps 1 to 8; automated checks, the review before the play test and the owner's play test pass; merged
  into `master` 2026-11-06
- Next: none (phase 2, side exits, is the next branch of `047`)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-06

- Branch started from `master`. The owner's decisions of 2026-11-06 (group camera with edge walls, room bounds first, carry for all exits, 6 phases) are in the item `ITEM.md`, "Decisions". This branch is phase 1.
- The letter is `g`, not `c`: the planned branches `047c` to `047f` keep their letters, because about 80 lines in other docs and code `TODO`s name them.
- Scan done. `docs/SCOPE.md`, `docs/CONTRACT.md` and the brief written. The owner answered D1 to D3 as recommended. For D4, the owner chose a rule for each view edge (`NONE`, `WALL`, `BOUNCE`, `WARP`, `KILL`) with `delay` and `damage`, set by a dev flag, so the play test can try them; the owner wants to discuss the rules more after the play test. Scope and contract approved 2026-11-06. `docs/PLAN.md` written.
- Steps 1 to 7 done. A subagent added `group_wide`, `group_clamp`, `group_pit` and `group_tall` to `RefactorTest047.tmx` (the map grew to 356x46 tiles at the right and the bottom, so the old coordinates stay). New: `ViewEdgeRules` in `SessionRules`, the `--viewEdges` flag, `PlayersFocus`, `CameraViewEdges`, `SharedViewTest` (14 tests; each fails when the code it covers is broken).
- Changes from the plan:
  - The group is "in the world" (`hasBody` and `liveNow`), not "in play", so the beam-in at a level start does not start a smooth follow.
  - `BOUNCE` uses no block: the part puts the player back at the edge and sets its speed into the view (12 tiles/s); the damage has a 0.5 s cooldown. A sideways bounce can be overridden by the run speed, so on the sides it acts like a soft wall.
  - "A zone or scroll acts" is "the current room overlaps a camera zone or scroll section". The rules are off in such a room.
  - `WARP` puts the player 1 tile beside the partner, on the side toward the view center.
- Automated checks: `engine:test` 639, `core:test` 977, `core:smokeTest` 448, all pass; `lwjgl3:jar` builds.
- Review before the play test (2 subagents, no game launch):
  - Manual checks: 13 of 14 OK. `bottom=WALL` held the player in the fall state, because `CameraEdgeBlock` has `NO_FEET_TOUCHIE`. Fix: the view edges part spawns its bottom block with `ConstKeys.FEET` = true, and `CameraEdgeBlock` then drops that label (the `051` edge blocks keep it). The 2 open control checks are filled in (`REFACTOR_TEST_051` `edge_block`, `DESERT_MAN` `room3`).
  - Diff: no crash, no change on the 1-player path. Fixed: a wall could spawn while a player was already outside it (after a camera turn, or a gap between rooms), which locked both players out; now a `WALL` block is not placed while a player's center is past the edge. Fixed: `WARP` chose a partner that was out at the same edge. Fixed: `BOUNCE` acted on a player who only touched the edge. Fixed: the per-frame `Direction.entries` iterator.
  - Accepted, not fixed: `WARP` does not test the spot for solids (`TODO` and backlog); zones and scrolls read the group midpoint with 2 players (backlog, phase 5); slot 1 can walk out through an open room edge into the next room (phase 2, "carry"); a `WARP` or `BOUNCE` move shows one frame late; `viewEdgeInsideRoom` boxes a `Float?`, as `CameraEdgeBlocks` does.
  - After the fixes: `engine:test` 639, `core:test` 980, `core:smokeTest` 448 pass; `lwjgl3:jar` builds.
- Play test, first report: in `group_pit`, the camera snapped to slot 0 when slot 1 died. Cause: the existing smooth follow (`20 × delta` of the remaining distance per frame) closes half a screen in about 0.14 s at 60 FPS, which looks like a snap. Fix (owner chose 0.5 s): `PlayersFocus` eases its own position from the old midpoint to the new one over 0.5 s with a smooth curve, and no longer uses the smooth follow; the group change is found where the focus is read, because the dead player has already left the world when the camera movers run in that frame. `engine:test` 639, `core:test` 980, `core:smokeTest` 448 pass; `lwjgl3:jar` builds.
- Play test, second report. All checks passed except 3:
  - `BOUNCE`: no damage reaction, and a player can be stuck. Fix (owner's request): the bounce damage goes through the player damage path through a small `ViewEdgeHazard` (an `IHazard` damager), so the player gets the reaction, the blink and the iframes; the iframes replace the 0.5 s cooldown. A `TODO` says the bounce is awkward and needs tuning.
  - `bottom=WARP`: the floor culled 1 s after it left the view, before the 1 s warp timer, so the low player fell out of the room and died. Reviewed with the owner on a page of sequence diagrams (`docs/WARP-REVIEW.html`). Owner chose F1, F2 and F4: an out player is held (frozen, locked with the new `MegaLockReason.VIEW_EDGE`) and left out of the group focus; it warps to a partner in view after the delay. A white guide line and circle show the warp spot (phase 1 of the owner's visual idea; float and bubbles are a backlog candidate). F3 (cull near players) is a backlog candidate.
  - Side `WARP`: the camera snapped after a warp, and two players out at once converged. F2: the group focus also eases when the midpoint jumps by more than 1 tile. F1 means only one player can be out. F4: side `WARP` is marked experimental with a `TODO`.
  - New tests fail when the hold, the release, the exclusion or the jump ease is broken. `engine:test` 639, `core:test` 984, `core:smokeTest` 449 pass; `lwjgl3:jar` builds.
- Play test, third report: in `group_tall` with `bottom=WARP`, the climber went out of view while the low player was held. Cause: the midpoint camera puts both players at opposite edges at the same moment, so the warp target was out of view until the 0.5 s ease ended. A headless test with the real camera parts confirmed the camera then catches up. Owner chose option B over "hold earlier" (B also holds during an ease and at a room clamp): on an axis with a `WARP` or `KILL` edge, `PlayersFocus` clamps the focus so one keeper player stays fully in view (one-sided: the player farther from the edge; both sides: sticky, first chosen by the view center). New tests (the climber in view on every frame, the keeper, the sticky keeper) fail without the clamp. `engine:test` 639, `core:test` 987, `core:smokeTest` 449 pass; `lwjgl3:jar` builds.
- Play test, fourth report: all play checks pass (owner, 2026-11-06). That is the merge approval.
- Merge of `master` into the branch (`master` had `081b`, `081c` and `038b`). 2 conflicts, both simple:
  - `MegaLevelScreen.kt`: `081b` moved `cameraFollow.setFocus` from `init()` into `start()`, because slot 0 can change
    per level. The view edge parts stay in `init()`; the group focus is now a field, and `start()` sets
    `playersFocus ?: player.focus` (as the `081b` merge note asked).
  - `LaunchOptionsResolver.kt`: both sides added imports and a line after `parseMode`; both kept.
  - After the merge: `engine:test` 639, `core:test` 1108, `core:smokeTest` 450 pass; `lwjgl3:jar` builds.
- Second merge of `master`: `master` had replaced the `081c` commit of the first merge (`e378b014a`) with `61460cd31`
  (the `DASH` button renamed `R`), so a squash would have undone the rename. Merged the new `master` and took
  `master`'s version of the 6 files that this branch does not change (`ControllerMappingExtensions.kt`,
  `MegaControllerButton.kt`, `MegamanXMoves.kt`, `MegamanXMovesTest.kt`, `PlayerSettingsStoreTest.kt`, the `081c`
  process log). The diff to `master` is then only this branch's 23 files. `engine:test` 639, `core:test` 1108,
  `core:smokeTest` 450 pass; `lwjgl3:jar` builds.
