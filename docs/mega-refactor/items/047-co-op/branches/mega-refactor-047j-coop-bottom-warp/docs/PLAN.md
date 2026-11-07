# Plan: mega-refactor-047j-coop-bottom-warp

1. [x] **Default.** `GameMode.COOP` gets `ViewEdgeRules(bottom = WARP, delay = 1f)`. Check: `LaunchOptionsResolverTest`
   (co-op default, PvP and single keep `ViewEdgeRules()`, `bottom=NONE` keeps the other co-op values).
2. [x] **No free partner.** `CameraViewEdges.updateHolds`: a held player with no live, not held partner is released and
   dies, with a `TODO` for phase 6. Check: `SharedViewTest` (dies when the partner leaves the world; stays held while
   the partner is live out of view; the same-edge case).
3. [x] **Automated checks** and 2 mutation checks.
4. [x] **Review before the play test** (subagent; no game launch), with a real tall room.
5. [x] **Play checks** to the owner.
