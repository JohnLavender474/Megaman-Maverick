# Plan: mega-refactor-047c-slot-respawn

1. **Bugs.** `weaponsHandler.reset()` in `Megaman.onDestroy`; a live check at the top of `throwAxe`; close both issue
   files (on `master` at the merge). Check: new tests.
2. **`PartnerSpot`.** Move the order of tries, the stand test and the cap out of `CameraViewEdges.findWarpSpot`. First
   callers: `CameraViewEdges`, then `SlotRespawn` (step 4). Check: the `127a` tests in `SharedViewTest`, unchanged.
3. **`BeamDown`.** Split the beam down and the landing of one player out of `BeamInView`. Callers: `BeamInView`, then
   `SlotRespawn`. Check: compile; the level start in the control checks.
4. **`SlotRespawn`.** `SessionRules.slotRespawn` (on for `COOP`); the screen's `PLAYER_JUST_DIED`: during the routine
   ignore; with a live partner take one life and start a slot respawn; else cancel the slot respawns and start the
   routine. Spawn props: the spot, the gravity direction, the partner's facing. `PRE_PLAY` and the `SPAWN` locks until
   the beam lands. Check: new tests (start, delay, wait, 0 lives, cancel, gravity, `PVP`).
5. **Slot 0 rules.** `MegaLevelScreen` (checkpoint text, camera hold, scroll hold, edge block gate, view edge gate, exit
   gate, pause gate, forced resume, death fade), `PlayersFocus.getFocusBounds`, `Fight.leadPlayerAlive`,
   `EndLevelEventHandler`, `LevelRoomHandler` aura. Check: new tests with slot 0 dead.
6. **Listeners.** `GauntletMaster` (no reset while a partner lives), `PreciousGem` and `PreciousGemCluster` (only the
   owner's death). Check: new tests.
7. **Automated checks.** `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar`.
8. **Review before the play test.** Subagents: the diff; the manual checks (the rooms of `REFACTOR_TEST_047` and the
   first wave room of `GAUNTLET_1`).
9. **Play test and Diffity review** by the owner.
