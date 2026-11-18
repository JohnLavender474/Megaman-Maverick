# Scope: mega-refactor-047k-gate-gather

- Status: approved
- Approved: 2026-11-17 (scope change D4 to D7 approved 2026-11-17)

## Goal

Shared screen phase 4. With 2 or more players in the session, a `Gate` opens only when every live player is at it
(stands in its `GATE` fixture), the same rule as a side room exit (`047h`). At a horizontal gate, the transition then
pans each player on its axis only (`047i`), so each player keeps its own height. With 1 player, nothing changes.

## Item

`047`, `ITEM.md`, "Decisions":

> 2026-11-17 (owner, at the `047i` play test): a gate works like a side room exit with 2 players: it needs every live
> player at the gate before it opens. This is phase 4 of the shared screen (gates).

> 2026-11-06: build the shared screen in phases. ... (4) gates ...

## Current state

From a read-only scan agent (2026-11-17, `master` `806087be9`; full output in the worktree
`temp/047k-gate-scan/gates.txt`):

- `GateRule.onBegin` (`world/contacts/rules/GateRule.kt`) calls `gate.trigger()` on the first contact of any player
  (`SIDE`, `FEET`, `HEAD` against `GATE`, `MegaContactRules.kt:40`). No END or CONTINUE rule.
- `Gate` (`entities/sensors/Gate.kt`): body `ABSTRACT` 2x3 tiles on the map object; the `GATE` fixture (1x1.5) is the
  half on the side the player comes from (`DirectionPositionMapper`). Not solid. Active only when a live player is within
  10 tiles (`Gate.kt:327-332`). `trigger()` → `OPENING` + `GATE_INIT_OPENING`; after 0.5 s `NEXT_ROOM_REQ`
  (`Gate.kt:243-249`) → `MegaLevelScreen.kt:685-689` `roomBounds.transitionToRoom(..., transDirection)`, shared point.
- `GATE_INIT_OPENING` turns off World, Motion, Behaviors and pathfinding (`MegaLevelScreen.kt:700-713`): every player
  freezes where it stands. Then every player moves to the shared point (the first group player's center).
- What holds a player at a closed gate: a plain map block on the gate rectangle (68 of 72 live gates; half the rectangle
  at the 3 `UP` gates; `Gauntlet1` gate `377` has none). The room edge is 1 tile into the object, so the fixture lies
  fully in the "from" room.
- `CameraRoomExits.coveredByGate` skips the wall of an exit that a gate covers (`TODO` names phase 4). The gate's map
  block is the wall, so this stays.
- 72 live gates: 66 `LEFT`, 3 `RIGHT`, 3 `UP` (WilyStage1 `432`, WilyStage2 `347`, `492`); 13 `boss_key`, 8
  `gauntlet_key`, 9 `trans_direction`.
- `CameraRoomTransition.start` throws for an axis-only start that is not `LEFT` or `RIGHT` (`047i`).
- `RefactorTest047.tmx` has no gate. `RefactorTest051.tmx` gate `212` with block `211` is a copy target.

## In scope

1. `GateRule` runs on `BEGIN` and `CONTINUE` (SIDE, FEET, HEAD against GATE) and only reports the touching player:
   `gate.markTouched(player)`. `contact-rule-selection.txt` gets the 3 `CONTINUE` lines.
2. `Gate` keeps a set of the players that touched it in this frame (made once; cleared each frame). At the start of
   its updatable, before the `OPENING` step: if it is triggerable, at least one player touched it, and every live
   player touched it, it triggers. One rule for 1 and 2 players (D1): with 1 player it opens in the frame of the first
   touch, as today. A dead player is not live and does not count; a held player is live and counts (D2).
3. `trigger(axisOnly)`: `NEXT_ROOM_REQ` carries `RoomTransitionEventKeys.AXIS_ONLY`, `true` for a `LEFT` or `RIGHT`
   gate whose `trans_direction` is absent or horizontal (D3). `MegaLevelScreen` passes it to `transitionToRoom`. `UP`
   gates send `false` (phase 3). All 69 horizontal live gates pan `LEFT` or `RIGHT` (script output in the main checkout
   `temp/mega-refactor-047k-gate-gather/gate_pan_dirs.txt`), so no live gate throws. With 1 player the axis-only pan
   gives the same point as the shared point, because physics is off during the 0.5 s opening.
4. Test map: a room pair `coop_gate` and `coop_gate_next` in `RefactorTest047.tmx`: a `LEFT` gate with its map block,
   and a pit or spike so that slot 1 can die before the gate.
5. Tests: `Gate` opens only when every live player touched it; a dead player does not count; 1 player opens on the
   first touch; at least one touch is needed; `NEXT_ROOM_REQ` axis-only for a horizontal gate, not for an `UP` gate;
   `GateRule` marks the player on `BEGIN` and `CONTINUE`.

New API and first caller: `Gate.markTouched(player)` (caller `GateRule`); `trigger(axisOnly)` is internal to `Gate`;
`GateBlock` (callers: the `GateBlock` objects of the 3 `UP` gates and of `coop_gate_down`); `SessionRules.spawnGateBlocks`
(caller `GateBlock.canSpawn`).

## Out of scope

- Vertical axis-only pans (phase 3).
- A player outside a boss room when the gate closes (`ITEM.md`, open question): with gather, both players pass.
- The gate with no block (`Gauntlet1` `377`): a player can walk past it today; not changed.

## Affected areas and risks

- With 2 players, a gate never opens while one player is far away. A dead player is not live, so it does not count. A
  player held below the view (`CameraViewEdges`) is live and counts: the gate waits until it warps back.
- The freeze and the shared point after a gate stay the same for 1 player.

## Overlap with other branches

Active worktrees `compactor-level`, `fix/projectiles-ignore-death`, `mega-refactor-060k-protoman-frame-edits`: none changes `Gate`, `GateRule`, `MegaLevelScreen`, `MegaContactRules`, `RefactorTest047.tmx` or `contact-rule-selection.txt` (2026-11-17).

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | The check runs in the gate's own updatable. |
| `O1-R4` Typed state | no effect | Reuses the typed `AXIS_ONLY` key. |
| `O1-R5` Code only with a production caller | no effect | Each new API has its caller in the branch. |
| `O2-R4` Players in slot order | no effect | The check reads all live players. |
| `O2-R5` No logic assumes one player | helps | A gate no longer opens for one player in a 2-player game. |
| `O5-R1` No per-frame allocation | to check | The per-frame overlap test must use pool scratch. |

## Decisions

- D1 (owner, 2026-11-17): the check lives in `Gate`, as one rule for any player count: `GateRule` reports touching
  players on `BEGIN` and `CONTINUE`, and the gate opens when every live player touched it (the owner asked why the 1-
  and 2-player triggers were separate; the single path replaced the two-path draft).
- D2 (owner, 2026-11-17): a player held below the view counts; the gate waits until it warps back.
- D3 (owner, 2026-11-17): horizontal gates pan axis-only; `UP` gates keep one shared point until phase 3.
- D4 (owner, 2026-11-17, after the review before the play test; owner's words: "It needs to be an 'all or nothing'
  thing; if a player's spawn-in has begun, then the gate is no longer triggerable by only one player"): a player counts
  for the gate from the start of its spawn-in (spawn queued or live, and not `POST_PLAY`); a dead player in the respawn
  wait does not count. A slot respawn whose spawn-in has not started waits while a gate opens and pans (`SlotRespawn`
  takes a hold: room transition, camera turn, or a gate between `GATE_INIT_OPENING` and `GATE_INIT_CLOSING`; first
  built on `WorldSystem` off, narrowed after review round 2 because World off also covers a boss intro and the fills). The open rule then needs at least one touch
  (`players.forEach` has no fallback when no player counts). Replaces D2's "dead does not count" for a respawning
  player.
- D5 (owner, 2026-11-17): a player who already stands in a locked gate opens it at once when it is unlocked (boss and
  gauntlet keys), also with 1 player, because `GateRule` runs on `CONTINUE`. Named in `CONTRACT.md`.
- D6 (owner accepted the recommendation, 2026-11-17): an axis-only transition whose direction resolves to `UP` or
  `DOWN` (a horizontal gate with no `trans_direction` between rooms whose centers lie mostly above or below) fails fast
  in `CameraRoomBounds.transitionToRoom` with a message that names the room and asks for a horizontal transition
  direction. No live gate does this (script output in `gate_pan_dirs.txt`).
- D7 (accepted edge case, 2026-11-17): the touched set is cleared per frame, not per world step; in a frame with 2 or
  more world steps, a player who leaves in one step and a partner who arrives in a later step both count. `TODO` at
  `Gate`.
- D8 (owner, 2026-11-18): an `UP` gate gets a `GateBlock` on its top half, which spawns only in co-op, so a waiting
  player stands on top of the gate. A `TODO` at `GateBlock.canSpawn` notes that a single-player use can widen the
  rule later. Found at the play test: the floor under each `UP` gate lies in the room below, out of the camera view,
  and the default cull removed it after 1 s. A first fix (no cull on those floors) was reverted by the owner: the
  waiting player then stood below the gate top and looked as if inside the gate.
- D9 (owner, 2026-11-18): at the play test, both players stood on the `GateBlock` and the gate stayed shut: the
  `GATE` fixture of a vertical gate is 1.5 tiles wide in the middle of a 3-tile gap, and a player at a side misses it.
  In co-op (`spawnGateBlocks`) the fixture is 3 tiles wide. Single-player keeps 1.5 tiles: the owner saw an earlier
  full-width try open the gate too early while the player fell in. Rejected: a `GateBlock` linked to its `Gate` that
  calls `trigger()` (a second open rule, against D1).
