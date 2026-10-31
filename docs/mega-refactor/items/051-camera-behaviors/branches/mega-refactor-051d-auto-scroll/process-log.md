# Process log: mega-refactor-051d-auto-scroll

## Status

- State: active
- Goal: Tiled scroll sections that take control of the camera, with edge blocks, stop modes and chains
- Done: `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` approved; plan steps 1 to 11.
- Next: owner approval to merge into `mega-refactor-051-master`.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- No branch exists yet. The folder and a draft `docs/SCOPE.md` were prepared on `master`.
- Owner approved `docs/SCOPE.md`.

### 2026-10-31

- Branch start from `mega-refactor-051-master` at `b3d63f66f`. The scope references still match the code after the `051c`
  changes. Scope change (owner approved): `next` is a one-way object reference. Contract approved. No temporary suite, because
  this branch adds behavior and does not rewrite code.

### 2026-10-31 (implementation)

- Step 1 baseline: engine 594, core 391, 0 failures, 96 unique warnings.
- Plan change: steps 3 to 8 share one commit, because `CameraScroll` was written as one part. Chains stay a separate commit.
- `CameraEdgeBlocks` takes a kill action, and `CameraLimits` has `keptEdges` and `killEdges`. A kept edge stays on at a room edge.
- Step 2: a subagent built 13 rooms for the 10 checks, and the map grew to 276x130, to the right. `scroll_corridor_section`
  now ends at tile 244, so that a real `exit` stop happens. With `megaman_direction=left`, gravity pulls toward the leading edge,
  so `scroll_gravity` cannot show the kill edge. Check 2 now proves it, and the check text is corrected.
- Fix of a `051c` map error: spawn `23` at Tiled (192, 3664) was inside ceiling block `127`. Collision sometimes pushed Megaman
  to x 560, and he entered `edge_rotated` with no input. The spawn is now at (512, 4000), 4 of 4 launches show no transition, and
  it is still outside the start area. The parent has the old position until this branch merges.
- Step 11: `engine:test core:test lwjgl3:jar` passes: engine 596, core 412, 0 failures. After `clean`, 96 unique warnings, none
  new. 12 room launches: no exception and the correct spawn. Launches have no input, so no scroll has been played. A mutation
  that removes the position reset after a pan fails `CameraScrollTest`.
- Owner request: temporary `DRAW_CAMERA_SECTIONS` in `MegaLevelScreen` draws scroll sections in green and zones in cyan, in all runs.
  Plan step 13 sets it to `false` before the merge.
- Play test: in `scroll_corridor`, Megaman died near the kept trailing block after the stop, with no hazard near. An edge block now
  crushes only in a frame where it moves into the view, because a stopped edge cannot pin a player. This also covers a scroll that
  waits at a room edge. The cause of the false opposite-side sense is not proved: `CameraEdgeBlock` has `NO_SIDE_TOUCHIE`, and no
  block was near. A temporary error log prints each crush.
- Scope change: crush surfaces, the past-edge safety rule, and block length from the section or zone. See `SCOPE.md`. A likely
  cause of the false kill: a side fixture is 1 tile tall, so while crouched its bottom is 0.05 tile above the floor, and contacts
  run before collisions, so a landing can touch the floor with both sides. Not proved. The sense is gone, so the temporary crush
  log is removed.
- Map: 7 `crush_surface` objects in `scroll_corridor`, `scroll_wall`, `edge_crush` and `edge_rotated`, drawn in red.
- `engine:test core:test lwjgl3:jar` passes: engine 596, core 418, 0 failures.

### 2026-10-31 (play test)

- Owner: "The tests pass", for the 16 checks. Before the merge, the owner asked for more rooms: scrolls in other directions, and after
  a `GravitySwitcharoo`, in combinations. No code change is expected.
- A subagent added 9 rooms for checks 17 to 25, in numbers after the first 16, and the map grew to 408x130, to the right. Control
  checks 11 to 16 stay in the contract. The new rooms have no `crush_surface` objects.
- Play test: `scroll_down` showed that the bottom edge held Megaman up. Owner-approved rule: an edge in the fall direction has no block,
  and a player fully past it dies, also on the leading edge, except at a room edge. See `SCOPE.md`. The tables are in the item
  `NOTES.md` and on `CameraScroll`, at the owner's request. 40 more `crush_surface` objects in `scroll_down`, `scroll_left`,
  `scroll_flip_right`, `scroll_switch_mid`, `scroll_side_up` and `scroll_side_down`. `engine:test core:test lwjgl3:jar` passes: engine
  596, core 423, 0 failures.
- Owner: the switch mix means switches inside a running section. A subagent added `scroll_switch_sides` and `scroll_switch_vertical` for
  checks 26 and 27, and the map grew to 502x130, to the right. No code change.
- Gravity switch research (read only): 2 snaps. The new one came from stale pinned limits during a turn, and the old one from the view
  size at the start of a turn. Owner approved the fix: a pin ignores the hold, and the view size blends. The blend has its own timer,
  because a turn that starts during a turn keeps the rotation timer, and that timing stays as it is.
- Play test: the owner saw the jump after a spawn in `scroll_spawn`, which came from the pin not reaching the limits during a spawn. Owner
  asked if the scroll must wait, then approved the recommendations: the scroll now waits until the player is ready, with the shared hold.
- Crush sensor probe (read only): no code change. Backlog candidate in `docs/TODO.md`. The owner judges the air kill in play.
- `engine:test core:test lwjgl3:jar` passes after `clean`.

### 2026-10-31 (play test passed)

- Owner: "The tests pass", for the camera fix and the control checks. Step 13: `DRAW_CAMERA_SECTIONS` is `false`, and its `TODO` is
  removed. After `clean`, `engine:test core:test lwjgl3:jar` passes: engine 596, core 429, 0 failures, 96 unique warnings, the baseline
  count, none new.
- `master` took code from other branches since the parent was cut, so the parent takes `master` after this merge. `031e` also changes
  `Constants.kt` and `MegaLevelScreen.kt`.
