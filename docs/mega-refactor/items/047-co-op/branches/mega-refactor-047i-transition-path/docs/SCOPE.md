# Scope: mega-refactor-047i-transition-path

- Status: approved
- Approved: 2026-11-17 (scope change D3 approved 2026-11-17)

## Goal

At a gathered side room exit (every live player touches the exit, `047h`), each player keeps its own coordinate across
the transition axis. Only the coordinate along the axis comes from the shared transition point. A transition that is
not gathered (1 player, a gate, the room scan) keeps the shared point.

## Item

`047`, `ITEM.md`, "Decisions", 2026-11-06:

> per-player transition path (`047i`, after `047h` merges). Found at the `047h` play test: slot 1 jumped into the exit
> wall while slot 0 stood on the floor, and slot 1 "teleported" to slot 0, because the carry moves every player to one
> point that starts at slot 0's center. When every live player is at the exit, each player takes only the
> transition-axis coordinate of that point and keeps its own other coordinate (its own path). When not every player is
> at the exit (a gate, which any one player triggers; later vertical exits), the shared point stays, so no player lands
> inside a solid; phases 3 and 4 decide more. This revises "warp next to that player" below for gathered exits. With 1
> player, nothing changes. The spread of the players after a transition stays a separate backlog candidate.

Branch split row: "Changes `playerOnRoomTransition` (`PlayerLifecycle.kt`) and the event data from `LevelRoomHandler`,
with 1-player control checks." `PlayerLifecycle.kt` no longer exists; the function is in `MegaPlayer.kt`.

## Current state

- `CameraRoomExits.update` (`screens/levels/camera/CameraRoomExits.kt:97-102`) is the only caller that starts a
  gathered transition: `everyoneAt(exit)` is true, then `roomBounds.transitionToRoom(camera, name, exit.side)`. The
  return value is not read.
- Other starts: the room scan in `CameraRoomBounds` (`engine/.../camera/CameraRoomBounds.kt:151`), a gate through
  `NEXT_ROOM_REQ` in `MegaLevelScreen.onEvent` (`MegaLevelScreen.kt:689`).
- `CameraRoomTransition.start` (`engine/.../camera/CameraRoomTransition.kt:47`) sets `focusStart` to the camera subject
  center and moves `focusTarget` on one axis only. `getTransitionInterpolation` gives the point between them.
- `LevelRoomHandler` (`screens/levels/LevelRoomHandler.kt:60-93`) puts that point in `ConstKeys.POSITION` of
  `BEGIN_ROOM_TRANS` and `CONTINUE_ROOM_TRANS`.
- `MegaPlayer.playerOnRoomTransition` (`players/MegaPlayer.kt:316-343`) is the only reader of that `POSITION`. It calls
  `body.setCenter(position)` on every player, so all players stack on one point.
- `LevelState.roomTransition` (`levels/LevelState.kt:19`) is the typed flag of a running transition; the handler sets it,
  and `MegaLevelScreen.kt:608,1072` clear it at a level start and a reset.
- Tests: `PlayerLifecycleTest.kt:168-197` (position on begin and continue), `LevelRoomHandlerTest`,
  `CameraRoomExitsTest`.

## In scope

Changed on 2026-11-17 by D3 (see "Decisions"); the first design put a field on `LevelState`.

1. `CameraRoomTransition` (engine) gets `axisOnly: Boolean` (read-only). `start(..., axisOnly)` sets it; `cancel()` and
   the `END` step clear it. First caller: `CameraRoomBounds.transitionToRoom(..., axisOnly)`, called with
   `axisOnly = true` by `CameraRoomExits` at a gathered exit. Every other start keeps the default `false`.
2. `LevelRoomHandler` puts `transition.axisOnly` on `BEGIN_ROOM_TRANS` and `CONTINUE_ROOM_TRANS` with a typed key
   `RoomTransitionEventKeys.AXIS_ONLY` (`PropertyKey<Boolean>`, the `PlayerEventKeys` pattern; no new `ConstKeys`).
3. `MegaPlayer.playerOnRoomTransition` reads the key: `true` sets only the x of the center; `false` sets the full
   point, as today. A `TODO` names phase 3 (a vertical axis-only pan sets only y).
4. No `LevelState` field and no manual clear sites.
5. Test map: a new room pair `coop_ledge` and `coop_ledge_next` in `RefactorTest047.tmx`: a ledge at the right exit
   of `coop_ledge`, so one player stands high and one low at the exit (D2).
6. Tests: engine `CameraRoomTransitionTest` (set by start, cleared at the end and by cancel); `CameraRoomExitsTest`
   (a gathered exit starts an axis-only pan); `LevelRoomHandlerTest` (the events carry the value);
   `PlayerLifecycleTest` (x only with the key, full point without it).

## Out of scope

- Vertical exits (phase 3) and gates (phase 4): they keep the shared point.
- The spread of the players after a transition (`132`).
- A test of the target room for solids at a player's own y (see risks).

## Affected areas and risks

- A player keeps its own y in the next room. The y is inside the exit span (the overlap of both rooms), which
  `everyoneAt` checks, but a block in the next room at that y next to the edge would hold the player inside it. Same
  risk as slot 0 has today with 1 player.
- A player in the air at the exit stays in the air during the pan (gravity is off), then falls at the end. Today it
  is moved to slot 0's y.
- The flag must not leak into a later transition: cleared at the end, at a level start and at a reset.

## Overlap with other branches

Active worktrees `compactor-level`, `fix/projectiles-ignore-death`, `mega-refactor-060k-protoman-frame-edits`: none
changes `players/`, `screens/levels/` or `engine/.../camera/` (`git diff --stat master...<branch>`, 2026-11-17).

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | The field is set before `BEGIN_ROOM_TRANS` is sent and read in the same transition. |
| `O1-R4` Typed state | helps | A typed `LevelState` field, not a new event key. |
| `O1-R5` Code only with a production caller | no effect | The one new field has a writer and a reader in production. |
| `O2-R4` Stable order; slot order | no effect | No new iteration. |
| `O2-R5` No logic assumes one player | helps | The carry no longer moves every player to one player's point. |
| `O2-R9` Events name a slot | no effect | No new event. |
| `O5-R1` No per-frame allocation | no effect | No new object per frame. |
| `O6-R2` Named checks | helps | New tests for both paths. |

## Decisions

- D1 (owner, 2026-11-17): the mark is a typed field `LevelState.gatheredExitSide: Direction?`. Replaced by D3.
- D2 (owner, 2026-11-17): add a ledge room pair to `RefactorTest047.tmx` for a standing height difference, next to the `coop_start` jump check.
- D3 (owner, 2026-11-17, after a 6-agent review of the field and of `LevelState`): the transition owns the mark.
  `CameraRoomTransition.axisOnly`, cleared at the end and by `cancel()`, sent on the transition events; no
  `LevelState` field. Reasons: the value describes one transition, not the session or the co-op mode (`CameraRoomExits`
  also runs in 2-player PvP); the transition already drops its state at the end and on cancel, so the 3 manual clears
  go. Accepted objections: the engine gets a flag whose only `true` caller is in `core` (a genre concept, allowed in
  `engine`); the value reaches the player through the event bag with a typed `PropertyKey`.
- D4 (owner, 2026-11-17): the other `LevelState` findings (the floating-points combo carries over into the next level;
  `roomTransition` and the beam fields copy state of other types; level values stay after the level ends) become new
  items and a bug file on `master` after `047i`. Not in this branch.
