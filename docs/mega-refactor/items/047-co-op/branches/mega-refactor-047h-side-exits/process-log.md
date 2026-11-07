# Process log: mega-refactor-047h-side-exits

## Status

- State: merged
- Goal: shared screen phase 2: with 2 players, a side room exit holds every player until all live players are at it, then the transition carries them
- Brief: https://claude.ai/artifact/LKXzMXdoqwJ3HaCYRnUCYg
- Done: plan steps 1 to 6; automated checks, the review before the play test and the owner's play test pass; merged into `master` 2026-11-06
- Next: none (`047i`, `047j` and `130` are the next branches)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-06

- Branch started from `master`. Cause found at the `047g` route play test: in `coop_start`, slot 1 ran off the screen into `coop_wide`, because the view edge rules skip a view edge at a room edge and the transition reads only slot 0. Owner's decision: "gather at the exit" (see the item `ITEM.md`, "Decisions", 2026-11-06).
- Scan done: the transition reads slot 0's focus bounds (`CameraRoomBounds`), the carry already moves every player, `transitionToRoom` can start a transition directly, and a `Gate` starts its own. Draft scope and contract written; brief published.
- Owner's answers 2026-11-06: D1 to D4 as recommended (start at once; on in section rooms; no walls with one live player; touching). Scope and contract approved. `docs/PLAN.md` written.
- Steps 1 to 4 done: `CameraRoomExits.kt` (`findSideExits`, the part), the setup in `MegaLevelScreen` (the gate rectangles of the `sensors` layer in `buildLevel`), `CameraRoomExitsTest` (11 tests). 3 mutation checks (span ignored, walls with one live player, gate ignored) each fail at least 1 test. `engine:test` 639, `core:test` 1119, `core:smokeTest` 450 pass; `lwjgl3:jar` builds.
- Map: spawns `17` to `20` in `coop_wide`, `coop_shaft`, `coop_hscroll`, `coop_vscroll`, so `--room` works there.
- Review before the play test (2 agents, no game launch):
  - Diff: nothing serious. `transitionToRoom` from an effect is safe: the next frame, `CameraRoomBounds.update` returns early while the transition runs, so it does not clear the room. A player against the wall reaches the 0.1-tile tolerance. The 1-player path and `047g` do not change. All live `Gate` objects are in a top-level `sensors` layer, and each side gate matches an exit. 4 minor findings: 2 are backlog candidates (`docs/TODO.md`), and 2 come from the existing APIs (rooms looked up by name; gates only in a top-level layer).
  - Manual checks: 3 check texts fixed (the `coop_hscroll` scroll block; the walk-away check, because the transition starts on the first frame that both touch; the death check, now with `left=KILL`). The real gate check is `DESERT_MAN` `room14`; added `GLACIER_MAN` `room11`, a real exit with no gate. The `coop_vscroll` up scroll kills slot 0 below its bottom edge, as before.
- Play test, first report: `coop_start` works. Quirk: slot 1 jumped into the exit wall while slot 0 stood on the floor, and slot 1 "teleported" to slot 0 at the transition. Cause: the `047a` carry, not `047h` (the shared point starts at slot 0's center). A review agent (read-only) found that only `PlayerLifecycle.playerOnRoomTransition` reads the transition `POSITION`. Owner's decision: a per-player path at a gathered exit, in a separate branch `047i` after `047h` merges (item `ITEM.md`, "Decisions"). `047h` does not change; the other play checks continue.
- Play test, second report: in `coop_shaft`, one player at the top ledge and one at the shaft floor: the camera showed neither, and the low player died. Cause: the `047g` defaults (`top=NONE`, `bottom=NONE`) and the cull of the floor out of view, not `047h`. Owner's decision: the co-op default `bottom=WARP`, in a separate branch `047j` (item `ITEM.md`, "Decisions"). The `047h` play test goes on, with both players dropping down in `coop_shaft`.
- Play test, third report: "All tests passed" (owner, 2026-11-06). That is the merge approval. The `--viewEdges bottom=WARP` warp into a block was recorded in item `127`.
- Merge: `master` had only docs changes; merged into the branch so the checks run on the merge tree. `081d` overlap noted in `docs/NOTES.md`.
