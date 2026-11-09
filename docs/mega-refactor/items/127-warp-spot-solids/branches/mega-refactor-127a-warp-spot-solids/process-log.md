# Process log: mega-refactor-127a-warp-spot-solids

## Status

- State: merged
- Goal: the view edge warp tests its spot for solids, hazards and enemies, and waits for a safe spot
- Done: brief, test map, code, edge cases (pit, crevice, teleporter), reviews, play test, merge
- Brief: https://claude.ai/artifact/WbC86hQuTT8o7yabZ31ZmF
- Next: none (merged 2026-11-09)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-07

- Branch start from `master` at `122aa53bb`.
- Brief published and approved (D1 cap 3 s, D2 gray guide, D3 damager overlap only, D4 merge into `master`).
- Plan step 1: a subagent built `RefactorTest127.tmx` and `REFACTOR_TEST_127` (`477498765`): rooms `warp_wall`,
  `warp_spikes` (`Spike`, from `RodentMan_16x14.tmx`), `warp_enemy` (`Cactus` `big=true`, from `DesertMan_16x14_v4.tmx`),
  `warp_air`. The spikes keep `has_block` (default), so that room tests the solid and the `DEATH` test together.
- Plan steps 2-3: `WarpSpotTest`, the order of tries, the stand test, the cap, the gray guide, `--viewEdges cap`
  (`06a1f7fb9`). 11 new tests; a mutation (no spot test, no stand test) failed 6 of them.
- Automated checks: `engine:test` 639, `core:test` 1135, `core:smokeTest` 450, `lwjgl3:jar` builds.
- Review before the play test, 2 subagents:
  - Diff review: (1) bug: the spot was centred on the partner's centre, so a crouching or sliding partner put every
    try in the floor; fixed: the spot lines up with the partner's feet. (2) risk: a floor at a fractional height could
    block by rounding; fixed: the test area shrinks by `0.01` tile per edge. (3) the cap does not run while no partner
    is in view (old behavior): `TODO` and a backlog candidate. (5) `warpReady` reset at each try. (6)
    `PRESS_UP_FALL_THRU` now counts as solid (the handler passes it only while the player presses up). Not fixed: the
    guide shows the facing-side try while it waits (the owner's D2); no unit test for a `CameraEdgeBlock` at the spot
    or for a climbing partner (the code paths are the generic block filter and `isBehaviorActive`). 2 new tests
    (feet line-up, the cap while the partner stays in the air).
  - Manual checks review: all 5 pass. Risk in `warp_enemy` (the `Cactus` needles can push slot 0 away) and a note for
    `warp_air` (slot 0 must be in the air when the delay ends): both go in the check text.
- Checks again: `engine:test` 639, `core:test` 1137, `core:smokeTest` 450, `lwjgl3:jar` builds.

## 2026-11-09

- Owner: "Tests pass" for the first 5 checks. Before the squash, `master` (60 files, also `MegaLevelScreen.kt` and the
  block contact rules) merged into the branch with no conflicts (`7f4f13e54`); checks pass.
- Merge stopped: the owner added 3 edge cases. Decisions (in `SCOPE.md`): a side spot needs ground (pit); after the cap,
  wait on while a solid blocks the partner's spot (crevice); a `TELEPORTER` fixture is unsafe.
- `2d897f66d`: `WarpSpotTest.hasGround`, `isFree`, `TELEPORTER`; rooms `warp_pit`, `warp_crevice`, `warp_teleporter`.
  6 new tests; a mutation (no ground test, no teleporter test, no free test) failed 6 tests.
- Automated checks: `engine:test` 639, `core:test` 1193, `core:smokeTest` 450, `lwjgl3:jar` builds.
- Review before the play test (1 subagent, code and the 3 new manual checks): (1) risk: any sliver of ground under the
  spot counted, so a player could land on 1-8 px of ledge and fall again; fixed: ground is tested under the centre
  0.5 tile (the feet width). Also fixed: a floor with a `DEATH`, hurtful or `TELEPORTER` fixture in its top is not
  ground. Not fixed, backlog candidates: a partner on a ladder (side spots have no ground, `TODO`); the landing after
  the cap tests only solids. Manual checks: all 3 pass; the `warp_pit` and `warp_teleporter` texts now say where slot 0
  must stop. 2 new tests; a mutation failed both.
- Checks again: `engine:test` 639, `core:test` 1195, `core:smokeTest` 450, `lwjgl3:jar` builds.
- Owner: "All tests pass" for all 8 rooms. No Diffity threads. `master` gained `081e` since the last merge in (X and
  X6 enemy files only, no shared file), so no second merge in. `134a` shares 4 files (text only; its notes list
  `127a`). Squash-merged into `master`.
