# Scope: mega-refactor-047c-slot-respawn

- Status: approved
- Approved: 2026-11-09

## Goal

In co-op, a dead player (either slot) comes back near the live partner, at a safe spot, after 1 s, with the beam.
Each death costs one shared life. The level resets only when no player is alive (D3). Also: the two filed bugs of this
phase, and the death listeners and the slot 0 rules that break when one player dies and the other plays on.

## Item

`047` (Co-op with multiple controllers), branch row `047c`: "Player 2 death and respawn near the lead (shared screen
phase 6). Also fixes `2026-11-02-weapon-spawned-sets-survive-death.md` and `2026-11-02-throw-axe-no-live-check.md`".

From `ITEM.md`:

- 2026-11-02 (from `119`): `047c` owns any listener filter that `119` does not finish.
- Open question: "A slot 1 respawn near the lead (`047c`): what does it cost? Today a slot 1 death is free, and it
  removes no life. Where does it land if the lead is in the air or over a pit? Options: the lead's last safe ground, the
  current checkpoint, a drop on the lead."
- `CameraViewEdges.kt:252` `TODO`: "a held player with no free partner counts as dead; phase 6 (047c) must keep this
  when it adds the respawn of slot 1 and the rule for the death of every slot".

## Current state

Full scan notes: `NOTES.md`. Summary:

- Death: `AbstractHealthEntity.onHealthDepleted` destroys (`AbstractHealthEntity.kt:113-128`); `playerOnDestroy`
  submits `PLAYER_JUST_DIED` with the slot (`PlayerLifecycle.kt:69-73`). It fires on every destroy, also on the
  `defeat()` destroy of slot 1 (`TODO` `:68`, item `119`).
- `MegaLevelScreen.onEvent` (`:605-618`): only a slot 0 death starts `respawn.startAtDeath()` (item `038` routine:
  `WaitAndFadeOut` (4 s), `CheckLives`, `BeginRespawn`, `ResetLevel`, `BeamIn`, `Ready`). `defeat` destroys every other
  slot (`RespawnSteps.kt:25`), removes 1 life (`:30`), stops the music.
- A slot 1 death alone: slot 1 stays unspawned until the next slot 0 death or level start. No life, no music change.
  It can be spawned again (`engine.spawn`; slot-owned, `MegaPlayers`). `PlayPhase` is not reset on destroy.
- Spawn: `playerSpawnStart` (`PlayerLifecycle.kt:40-62`) places the body bottom centre, clears the `MOVEMENT` and
  `DAMAGE` locks and sets the session gravity from the spawn `direction`. Spawn protection is only `PRE_PLAY`, which
  only the routine sets (`beginRespawn`, `RespawnSteps.kt:43-61`; `Ready`, `:145-165`, also sends `PLAYER_READY`).
- Beam: `BeamInView` (`respawn/BeamInView.kt`) does the fade-in, the "READY" text, the beam down and the landing, for
  slot 0 only (`TODO` `:48`). A player not `IN_PLAY` is not drawn.
- Lives: one shared pool (`GameState.kt:268-284`), the slot parameter unused.
- Safe spot: `127a` built it for the warp: `CameraViewEdges.findWarpSpot` (order of tries, stand test, cap) with
  `WarpSpotTest`.
- Listeners that break with one death: `GauntletMaster.kt:168` resets its waves on any death (with slot 1 dead and no
  level reset, the wave enemies stay uncounted and the room soft-locks); `PreciousGem.kt:215` and
  `PreciousGemCluster.kt:109` destroy the guard of any player on any death; `SpiderWeb.kt:136` frees all webs on any
  death (`TODO` `047e`). `PLAYER_DONE_DYIN` and `PLAYER_READY` listeners (`DrWily`, `QuestionBlock`, `LaserBeamer`,
  `RisingLavaRiver`, `DarknessV2/V4`, the room spawners) treat the event as a level reset.
- Bugs: `MegamanWeaponsHandler.reset()` (`:156`) has no caller, so the `spawned` sets and cooldowns survive a death;
  `throwAxe` (`:1006`) runs from an uncancelled `delayRun` (`:660`) with no live check.

## In scope

- New `SlotRespawn` (`screens/levels/respawn/`): per slot, after a co-op death while another player is alive (not
  during the routine), take one life, wait the delay, find a spot near a live partner, beam down there and set the
  slot in play. At 0 lives the slot stays out. First caller: `MegaLevelScreen` (event and update). Cancelled when the
  routine starts, by `resetLevelForRespawn` and by `respawn.cancel()`.
- The routine (`RespawnRoutine`) starts when the last live player dies (was: when slot 0 dies). Its lead for the fade,
  the life and the beam-in stays slot 0 (`RespawnContext.lead`).
- The slot 0 rules of `MegaLevelScreen` read "any player": the checkpoint text, the camera hold, the scroll hold, the
  edge block gate, the view edge gate, the room exit gate, the pause gate and the forced resume, the death fade. The
  pause input stays slot 0's (`047d`). The edge block kill stays on slot 0 (phase 5 `TODO`).
- `PlayersFocus.getFocusBounds`: the first live player of the group in slot order (was: slot 0 always), so the room
  scan follows slot 1 while slot 0 is dead.
- `Fight.leadPlayerAlive` reads "a player is alive" (was: slot 0).
- `EndLevelEventHandler` beam-out and the `LevelRoomHandler` gravity aura: the first live player (was: slot 0).
- New `PartnerSpot` (camera package): the order of tries, the stand test and the cap, moved out of
  `CameraViewEdges.findWarpSpot`. Callers: `CameraViewEdges` (the warp) and `SlotRespawn`.
- New `BeamDown` (respawn package): the beam down and the landing of one player, moved out of `BeamInView`. Callers:
  `BeamInView` (the lead) and `SlotRespawn`.
- `SessionRules`: the respawn of a non-lead slot on or off and its delay (on for `COOP`; off for `PVP`, as today).
- Spawn props of the respawn: the spot, the current gravity direction (no gravity flip), the lead's facing.
- Listener fixes: `GauntletMaster` (D4), `PreciousGem` and `PreciousGemCluster` (only the owner's death).
- Bugs: `weaponsHandler.reset()` in `Megaman.onDestroy`; a live check at the top of `throwAxe`; close both issue files.
- Unit tests; manual checks in `REFACTOR_TEST_047` (`slot_1_death`, `coop_shaft`, `group_pit`) and `GAUNTLET_1`.

## Out of scope

- Per-player pause and HUD (`047d`).
- Per-player lives (`GameState` `TODO`).
- `SpiderWeb` (`047e`), boss intros that wait for all players (`047e`), `Fight.leadPlayerAlive` (`047e`).
- The `PLAYER_JUST_DIED` on every destroy (`119`). The new code reads only real deaths: it ignores the event while the
  lead's routine runs.
- Item `134` snaps (a respawn is a teleport for render interpolation; `134b`).

## Affected areas and risks

- `MegaLevelScreen.kt`, `respawn/` (new `SlotRespawn`, `BeamDown`; `BeamInView`, `RespawnSteps` read only),
  `CameraViewEdges.kt` (move to `PartnerSpot`), `SessionRules.kt`, `MegamanWeaponsHandler.kt`, `Megaman.kt`
  (`onDestroy`), `GauntletMaster.kt`, `PreciousGem.kt`, `PreciousGemCluster.kt`, `GameState` (if D1 takes a life).
- Risk: `defeat()` destroys slot 1 and sends `PLAYER_JUST_DIED`; a respawn that starts from it would race the level
  reset. Check: ignore the event while the routine runs; a test.
- Risk: `playerSpawnStart` sets the session gravity from the spawn props; a wrong `direction` turns the lead. Check:
  the props carry `services.gravity`'s direction; a test.
- Risk: the move of the warp search into `PartnerSpot` changes the warp. Check: the `127a` tests in `SharedViewTest`
  run unchanged.

## Overlap with other branches

- `134a` (draw interpolation): shares `MegaLevelScreen.kt`, `PlayersFocus.kt`, `Megaman.kt` (uncommitted there). Text
  overlap; its `134b` must snap the respawn. Read its folder again before the merge.
- `081f` (X6 intro): `PlayerDamageNegotiator.kt` only. No overlap.
- `compactor-level`: level content, `LevelDefinition.kt`. No overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | the respawn runs in the screen update, after the events, as the routine does |
| `O1-R4` Typed state | no effect | typed fields in `SessionRules` and `SlotRespawn` |
| `O1-R5` Code with a caller | no effect | each new class has 2 callers or 1 production caller (see "In scope") |
| `O2-R4` Stable order | no effect | slots in slot order |
| `O2-R5` No single-player logic | helps | a slot 1 death is a real phase; the lead is still slot 0 (`RespawnContext.lead`) |
| `O2-R7` Simulation apart from presentation | no effect | the beam timing is simulation time; the sprite is drawn only |
| `O2-R9` Events name a slot | no effect | no new event; the respawn sends no `PLAYER_READY` |
| `O2-R10` Freezes are simulation state | waiting | the delay is a timer on the update delta, not a system switch |
| `O5-R1` No per-frame allocation | no effect | fields for the spot and the props |
| `O6-R2` Named checks | helps | unit tests and manual checks for each case |
| `O8-R2` Capabilities, not character checks | no effect | spawn through the player lifecycle functions |

## Open questions

None.

## Decisions (owner, 2026-11-09, at the brief)

- D1 cost: one life from the shared count per death. At 0 lives, a dead player stays out until the next level reset;
  a death that leaves a live partner never causes a game over.
- D2: after 1 s, beam down at the spot as at the level start (no fade, no "READY" text).
- D3: slot 0 also respawns near slot 1 while slot 1 lives; the level reset comes only when no player is alive.
- D4: a death in a gauntlet does not touch the waves while a partner lives; the player respawns into the fight.
- From D3 (Claude, in the brief as "changed"): the slot 0 rules of the level screen read "any player"; the pause input
  stays slot 0's; the room scan follows the first live player; a fight and the level end read "a player is alive" and
  the first live player.
