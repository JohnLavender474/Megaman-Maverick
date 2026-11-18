# Contract: mega-refactor-047k-gate-gather

- Status: approved
- Approved: 2026-11-17

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with new tests: with 2 players a gate opens only when every live player touched it;
  a dead player does not count; with 1 player the first touch opens it in the same frame; no touch, no open; a
  horizontal gate asks for an axis-only transition, an `UP` gate does not; `GateRule` marks on `BEGIN` and `CONTINUE`;
  `ContactRuleSelectionTest` with the 6 new `CONTINUE` lines; a spawn-queued or beaming-in player counts, a dead one
  does not; with no counted player and no touch the gate stays shut; a locked gate with a player in it opens on the
  unlock; `SlotRespawn` starts no spawn-in during the hold; `CameraRoomBounds` throws for an axis-only transition that
  resolves to `UP` or `DOWN`; `GateBlockTest`: a `GateBlock` spawns in co-op, not in single-player or PvP; the `GATE` fixture of
  an `UP` gate is 3 tiles wide in co-op and 1.5 tiles in single-player.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-047k-gate-gather`.

Experimental checks (2 players):

- `./run-game-test-coop.sh --room coop_gate`: jump the pit. Slot 0 walks into the gate alone: it stays shut. Slot 1
  also walks in: the gate opens, and both pan into `coop_gate_next`. For "each at its own height", let one player jump
  in the gate as it opens (players frozen on the floor show the same height).
- `./run-game-test-coop.sh --room coop_gate`: slot 0 stands at the gate (it stays shut); then slot 1 falls into the
  pit: the gate opens at once, and slot 1 beams back next to slot 0 in `coop_gate_next` after the pan. (If slot 0
  reaches the gate more than about 1 s after slot 1 dies, slot 1's spawn-in has begun and it must touch too.)
- `./run-game-test-coop.sh --room coop_gate`: slot 0 crosses the pit and stands 1 to 2 tiles left of the gate, facing
  left. Slot 1 falls into the pit. When the beam of slot 1 appears (about 1 s), slot 0 walks into the gate during the
  0.85 s beam: the gate stays shut, also after the beam, until slot 1 walks in. (Slot 0 faces left so that slot 1 does
  not land in the gate.)
- `./run-game-alpha.sh --level DESERT_MAN --room room14 --players 2`: the same gather at gate 601 (room14 to room15).
- `./run-game-test-coop.sh --room coop_gate_down`: walk right to the 3-tile floor gap. Slot 0 drops into the gap alone
  and stands on top of the `UP` gate (on its `GateBlock`), also after more than 1 s; the gate stays shut. Slot 1 also
  drops in: the gate opens, and both pan down into `coop_gate_down_next` to one shared point.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room10 --players 2 --heartTanks all --healthTanks all`: the same
  gather at `UP` gate 432.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room4 --players 2 --heartTanks all --healthTanks all`: the same at
  `UP` gate 347.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room9 --players 2 --heartTanks all --healthTanks all`: the same at
  `UP` gate 492.

- `./run-game-alpha.sh --level GAUNTLET_1` (1 player; no `--room`: `room1` has the only player spawn): walk right into
  `room2`; in the last wave, stand against the right wall (the locked gate) and kill the last enemies from there. When
  the last enemy dies, the gate opens at once, with no step out and back (D5).

Control checks (behavior that must not change):

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room coop_gate`: 1 player: the first touch opens the gate.
- `./run-game-alpha.sh --level DESERT_MAN --room room14`: 1 player at a real gate, as before.

## Intended changes

- With 2 players, a gate opens only when every live player touched it. Before, any one player opened it.
- With 2 players, a horizontal gate pans each player on its axis only (own height). Before, every player moved to one
  point.
- With 2 players, a player who is beaming in after a death counts for a gate; the partner cannot open it alone during
  the beam. A respawn waits while a gate opens and pans, and then beams in next to the partner in the new room.
- A player who already stands in a locked gate (boss or gauntlet key) opens it at once when it is unlocked, also with
  1 player. Before, the player had to step out and walk in again.
- A player who dies in the frame of its touch no longer opens a gate (it no longer counts), also with 1 player. Before,
  the gate opened on that touch while the death began.
- New block `GateBlock`: it spawns only in co-op (`SessionRules.spawnGateBlocks`). One sits on the top half of each
  `UP` gate (test room `coop_gate_down`; Wily 1 gate 432; Wily 2 gates 347, 492), so a player who waits for the
  partner stands on top of the gate. Before, the player stood on the floor below the gate top, which lies in the room
  below, out of the camera view; that floor was culled after 1 s, and the player fell into the room below (owner,
  2026-11-18). In co-op, the `GATE` fixture of an `UP` or `DOWN` gate covers the full gate width (3 tiles, was 1.5),
  so two players side by side on the `GateBlock` both touch it (D9). Single-player and PvP: no change.
