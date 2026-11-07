# Scope: mega-refactor-047h-side-exits

- Status: approved
- Approved: 2026-11-06

## Goal

Phase 2 of the shared screen. With 2 players, a side room exit (a left or right room edge that touches another room)
holds every player until all live players are at it. Then the room transition starts, and it carries every player, as
today. With 1 player, nothing changes.

## Item

Item `047`. From `ITEM.md`, "Decisions" (2026-11-06):

- Side exits with 2 players (phase 2, `047h`): "gather at the exit". A side room exit is a wall for every player. It
  opens only when every live player touches it from inside; then slot 0 crosses, the transition runs, and the other
  players are carried. Chosen over "only slot 1 waits" and "any player leads". Found at the play test of the `coop_*`
  route: slot 1 ran into the next room alone.
- Build the shared screen in phases: ... (2) side exits; (3) vertical exits (ladders, jumps, falls); (4) gates;
  (5) camera sections (`051`); (6) death and respawn.
- For now, every room exit uses "carry".

## Current state

- A transition starts when slot 0 leaves the room: `CameraRoomBounds.update` clears the current room when
  `focus.getFocusBounds()` no longer overlaps it (`CameraRoomBounds.kt:91-96`), and `scanForNextRoom` starts the
  transition when the focus bounds overlap another room (`:132-155`). With 2 players, `PlayersFocus.getFocusBounds()`
  gives slot 0's bounds (`PlayersFocus.kt:54`).
- `CameraRoomBounds.transitionToRoom(camera, name, direction)` starts a transition directly (`:109-130`). Gates use
  it.
- The carry exists. `LevelRoomHandler` sends `BEGIN_ROOM_TRANS` and `CONTINUE_ROOM_TRANS` with the transition
  position (`LevelRoomHandler.kt:46-104`); every player sets its center to it (`PlayerLifecycle.kt:75-97`). The path
  starts at the focus center (`CameraRoomTransition.kt:54`), so a transition can start while slot 0 is still inside the
  room. After the transition, the players stand on one spot.
- `CameraViewEdges` acts only where the view edge is inside the room (`viewEdgeInsideRoom`, `CameraViewEdges.kt:38`).
  At a room edge it does nothing, so slot 1 can walk into the next room alone. This is the play test report.
- `CameraEdgeBlock` is a players-only block with no side sense (`047g`, `051`). `CameraViewEdges.placeBlock` spawns
  and moves one per edge (`:139-157`).
- `RefactorTest047.tmx` has the 7-room `coop_*` route (`master`, 2026-11-06), with 6 side exits, and the rooms
  `transition` and `transition_next` (`047a`), which touch.

## In scope

1. **Side exits.** A pure function finds the exits of a room: for the left and the right edge, each other room whose
   opposite edge touches it (within 0.5 px) and whose vertical range overlaps it. An exit is the side, the next room
   and the shared vertical span.
2. **Exit walls.** A new camera part `CameraRoomExits` (`ICameraPart`, `Resettable`). While it is on and 2 or more
   players are live (not held by `WARP`), it keeps one players-only `CameraEdgeBlock` 1 tile outside each side exit,
   over the shared span. Blocks are spawned once and reused.
3. **Gather.** A player is at an exit when its body touches the exit edge from inside (within 2 px) and its center is
   inside the span. When every live player is at the same exit, the part removes the walls and calls
   `roomBounds.transitionToRoom(camera, next, side)`. The carry is as today.
4. **On:** 2 players registered, slot 0 in play, no transition, no camera turn, direction `UP`. Camera sections do
   not turn it off (D2): the wall is at the room edge and does not move.
5. **One live player:** no walls; the transition is as today (D3).
6. **Tests:** the exit function (touch, no touch, partial span, two rooms on one side), the walls (placed, moved, off,
   one live player), the gather (both at the exit, one away, a held player, a dead player).

## Out of scope

- Vertical exits: a ladder, a jump or a fall into another room (phase 3). Slot 1 can still leave alone through a top or
  bottom room edge, with a `TODO`.
- Gates (phase 4). A `Gate` starts its own transition when any player touches it (`GateRule.kt:17`, then
  `NEXT_ROOM_REQ`, `MegaLevelScreen.kt:625`). An exit that a `Gate` covers gets no wall, with a `TODO` for phase 4.
- A spread of the players after the transition (they stand on one spot, as today). Backlog candidate.
- A cue that shows who waits at an exit. Backlog candidate.
- 3 or more players: the rule is written for any count, but only 2 are tested.

## New API and first callers

| API | First caller |
|---|---|
| `CameraRoomExits` (`ICameraPart`, `Resettable`) | `MegaLevelScreen.init()`, `addEffect` after `CameraViewEdges` |
| the side exits function | `CameraRoomExits.update` |

`CameraRoomBounds.gameRooms`, `currentGameRoom` and `transitionToRoom` exist. No other new API.

## Affected areas and risks

- `MegaLevelScreen.kt` (camera setup), 1 new file in `screens/levels/camera/`, tests.
- Risk: a `Gate` at a side exit. A wall there could keep the players from the gate, so such an exit gets no wall. A
  control check plays a real gate with 2 players.
- Risk: the 1-frame lag of the walls, as in `047g`.
- Risk: the `transition` room of `047a` changes on purpose: slot 0 alone no longer leaves.

## Overlap with other branches

No other active `mega-refactor` branch. `compactor-level` changes `CeilingCrusher` and its level: no overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | One effect, after `CameraViewEdges`, set in one place. |
| `O1-R5` Code only with a production caller | no effect | Both new parts have a caller. |
| `O2-R4` Players in slot order | no effect | "Every live player" is order-free. |
| `O2-R5` No logic assumes one player | helps | A room exit waits for every player, not only slot 0. |
| `O2-R6` No logic assumes one camera | waiting | One camera, as in `047g`. |
| `O2-R7` Gameplay does not depend on rendering | waiting | The part runs in the camera path, as `047g` and `051` do. |
| `O5-R1` No per-frame allocation | no effect | Reused arrays and blocks. |
| `O6-R2` Named checks | helps | Checks in the `coop_*` route. |

## Decisions (owner, 2026-11-06)

- D1: when every live player is at the exit, the part starts the transition at once (`transitionToRoom`).
- D2: the exit walls also act in rooms with a camera zone or scroll.
- D3: with one live player, no walls; the transition is as in single player.
- D4: "at the exit" means touching the exit wall from inside.

## Open questions

- None.
