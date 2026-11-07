# Process log: mega-refactor-047j-coop-bottom-warp

## Status

- State: merged
- Goal: the co-op default `bottom=WARP`, so a tall room keeps one player in view and the low player is held and warps, not lost
- Brief: https://claude.ai/artifact/Pp4d6cB8H6FD1dr37Wty1x
- Done: plan steps 1 to 5; automated checks, the review before the play test, the owner's play test and diff review pass; merged into `master` 2026-11-07
- Next: none (`127` is next)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-06

- Branch started from `master`. Owner's decision (item `ITEM.md`, "Decisions", 2026-11-06): the co-op default becomes `bottom=WARP`; found at the `047h` play test in `coop_shaft`.
- Scan done: one value in `GameMode.COOP`; `--viewEdges` already starts from the mode's rules. Draft scope and contract written; brief published.
- Owner's answers 2026-11-06: D1 co-op only; D2 1 s, the dev flag `--viewEdges delay=...` is the override; D3 `127` separate, right after. Added at approval: the `127` review found that slot 0 held while slot 1 dies is a soft lock (no warp spot, `DAMAGE` locked, slot 1 never respawns); the new default makes it reachable, so the fix goes in this branch (owner's choice). Scope, contract and brief (version 2) approved.
- Steps 1 to 3 done. A held player with no free partner is released and dies (`depleteHealth`). The `047g` test with both players out at the same edge now also checks that slot 0 dies (before, both stayed held). 2 mutation checks (no free partner rule; old co-op default) each fail 2 tests. `engine:test` 639, `core:test` 1123, `core:smokeTest` 450 pass; `lwjgl3:jar` builds.
- Review before the play test (1 agent, no game launch):
  - Found: a lone player (slot 1 dead) who falls below the view during the 0.5 s camera ease was held and then killed by the new rule at once. Fix: a player with no free partner is not held (`checkWarpOut`), so it acts as with `bottom=NONE`; test added; a mutation (no partner check at the hold) fails 1 test. The `047g` same-edge test now checks that only one of the two players is held, and nobody dies.
  - OK: `depleteHealth` on a held player (released first; one death); both held in one frame; the rule cannot fire at the level start, in a transition, a teleport or at a side exit.
  - Contract: the `coop_shaft --viewEdges left=KILL` check could not run (the room is one view wide, and no hazard there); removed, covered by unit tests. Real stage checks: `PRECIOUS_WOMAN` `room2`, `DESERT_MAN` `room9`.
  - Spike floors in tall rooms (death strips more than one view above the room bottom): `INFERNO_MAN` `room10`, `REACTOR_MAN` `room3`, `GLACIER_MAN` `room8`, `WILY_STAGE_2` `fork_room` and `path1_room1`, `WILY_STAGE_3` `room3` and `room5`. A player who falls fully below the view before the strip warps in place of dying; accepted as "out of view = warp", with a backlog note. 2 backlog candidates in `docs/TODO.md`.
  - After the fix: `engine:test` 639, `core:test` 1124, `core:smokeTest` 450 pass; `lwjgl3:jar` builds.

## 2026-11-07

- Diff review in Diffity (base `63bc3d3d0`): 0 threads. Server stopped.
- Play test: "tests passed" (owner). That is the merge approval.
- Merge: `master` had only docs changes (`127` decisions, item `134`, the Diffity process); merged into the branch so the checks run on the merge tree. `081d` does not touch the `047j` files.

