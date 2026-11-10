# Contract: mega-refactor-047c-slot-respawn

- Status: approved
- Approved: 2026-11-09

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with new tests for:
  - a death in co-op while the partner lives (either slot) starts its respawn: one life goes; after 1 s, the player is
    spawned at a safe spot near the partner (the order of tries of the warp), with the session gravity direction and
    the partner's facing;
  - the respawn waits while the partner does not stand or no spot is safe, up to the cap;
  - at 0 lives, a dead player stays out, and no game over comes while the partner lives;
  - the death of the last live player starts the routine (level reset) and cancels the slot respawns;
  - the `PLAYER_JUST_DIED` that `defeat()` sends starts no respawn;
  - `resetLevelForRespawn` and `respawn.cancel()` cancel a running slot respawn;
  - the slot 0 rules of the level screen with slot 0 dead and slot 1 in play (camera hold, view edges, exits, room
    scan focus, pause gate, death fade);
  - `Fight` with slot 0 dead: the victory and the gate unlock run;
  - `PVP` and single player: no respawn (as today);
  - the warp tests of `127a` in `SharedViewTest` still pass after the move to `PartnerSpot`;
  - `GauntletMaster` (D4) and `PreciousGem` / `PreciousGemCluster` (only the owner's death);
  - `MegamanWeaponsHandler.reset()` runs on the player's destroy; `throwAxe` does nothing for a dead player.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check runs as written. No game launch.

## Manual playthrough

Run from the branch worktree with `./run-game-test-coop.sh` (co-op, `--players 2 --mode coop`, default level
`REFACTOR_TEST_047`). Experimental checks first. The lives count (start 3) shows only on the pause screen.

| Level | Room | What to check |
|-------|------|---------------|
| `REFACTOR_TEST_047` | `slot_1_death` | slot 1 jumps into the pit (middle of the room); slot 0 stays on the floor: after 1 s, slot 1 beams down 1 tile from slot 0, on the side slot 0 faces (the other side if the pit or a wall is there); lives 3 → 2; slot 0 keeps control and the music plays |
| `REFACTOR_TEST_047` | `slot_0_death` | slot 0 jumps into the pit: no fade, no reset; the camera eases to slot 1; after 1 s, slot 0 beams down next to slot 1; lives 3 → 2 |
| `REFACTOR_TEST_047` | `respawn_trans` | walk slot 1 to the right room edge first (the exit wall holds it while slot 0 lives) and keep it pressed right; then slot 0 walks left into the pit: slot 1 goes through at once and the room transition runs with slot 1 alone; after the pan, slot 0 beams down 1 tile from slot 1 in `respawn_trans_next`, not on top of slot 1 |
| `REFACTOR_TEST_047` | `group_pit` | slot 0 walks right to the very edge of the left ledge (16 tiles right of the spawn) and faces the pit; slot 1 jumps in: slot 1 beams down behind slot 0, never over the pit |
| `REFACTOR_TEST_047` | `slot_1_death` | slot 1 jumps into the pit; slot 0 jumps again and again on the floor: slot 1 beams in only after slot 0 lands, never while slot 0 is in the air |
| `REFACTOR_TEST_047` | `slot_1_death` | slot 1 jumps into the pit, then slot 0 within about 1.5 s: the normal death flow (fade, defeat sound, reset); both players spawn at the checkpoint, once; lives 3 → 1 |
| `GAUNTLET_1` | `room1` | walk right through the gate into `room2`; the waves start; let the enemies hit slot 1 until it dies (do not shoot with it): the current wave goes on (no reset to wave 1), and slot 1 beams down beside slot 0 after 1 s |
| `REFACTOR_TEST_047` | `slot_0_death` | control: slot 0 jumps into the pit, then slot 1 before slot 0 is back: the normal death flow and reset; both players spawn again; lives 3 → 1 |
| `TEST_1` | (start) | control with `./run-game-test.sh` (1 player): the death flow as before |

## Behaviors to check

- Single player: no change.
- PvP: no change (no slot respawn).
- The death of the last live player: the death flow and level reset as before, also while the other waits to respawn.
- The `127a` warp: the same spots as before.
- Two deaths in the same frame cost one life and run one defeat (as one death of the last player).

## Intended changes

- A dead player in co-op respawns near the live partner after 1 s, with the beam (was: slot 1 dead until the next level
  reset; slot 0's death reset the level).
- Each death costs one life; at 0 lives the player stays out; the level reset comes only when no player is alive.
- A gauntlet keeps its waves on a death while a partner lives.
- With slot 0 dead, the camera, the room scan, the view edges, the exits, a fight and the level end act on slot 1.
- `PreciousGem` and `PreciousGemCluster` are destroyed only on their owner's death (was: any player's death).
- Bugs `2026-11-02-weapon-spawned-sets-survive-death.md` and `2026-11-02-throw-axe-no-live-check.md` are fixed.
