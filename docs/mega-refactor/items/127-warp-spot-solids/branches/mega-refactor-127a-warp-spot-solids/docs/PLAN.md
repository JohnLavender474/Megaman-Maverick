# Plan: mega-refactor-127a-warp-spot-solids

1. **Test map.** A subagent builds `RefactorTest127.tmx` (rooms `warp_wall`, `warp_spikes`, `warp_enemy`, `warp_air`)
   and adds `LevelDefinition.REFACTOR_TEST_127`. Check: the review before the play test; `LevelCompletionScreensTest`.
2. **`WarpSpotTest`.** New class in `screens/levels/camera/`: `isSafe(player, centerX, centerY)`; query lambdas held in
   fields. First caller: step 3. Check: new unit tests with a real `SimpleGridWorldContainer`.
3. **`CameraViewEdges`.** The order of tries (facing side, other side, partner's spot), the stand test, the cap timer,
   the waiting state of the guide (gray). `ViewEdgeRules.warpCap` (default 3 s) and the `cap` key in `--viewEdges`.
   `MegaLevelScreen` passes the world container supplier. Check: `SharedViewTest` and `LaunchOptionsResolverTest`.
4. **Automated checks.** `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar`.
5. **Review before the play test.** Subagents: the diff, and the manual checks of the contract.
6. **Play test and Diffity review** by the owner.
