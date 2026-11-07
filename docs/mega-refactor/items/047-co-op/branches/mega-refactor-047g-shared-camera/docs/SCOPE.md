# Scope: mega-refactor-047g-shared-camera

- Status: approved
- Approved: 2026-11-06

## Goal

Phase 1 of the shared screen. With `--players 2`, the camera follows the players as a group, and the left and right
edges of the view are walls for each player inside the room. With 1 player, nothing changes.

## Item

Item `047`. From `ITEM.md`, "Decisions" (2026-11-06):

- The shared screen uses a group camera with edge walls. The camera follows the players as a group inside the 16x14
  room view (the view does not zoom). The left and right edges of the view block each player, so no player leaves the
  view sideways and cull and spawn keep one camera.
- Room bounds come before the shared view. At a room edge, the single-player rule applies (a transition, a wall, or a
  pit death at the bottom); the view edge is only a wall inside the room.
- For now, every room exit uses "carry". (Phase 2. Not in this branch.)
- Build the shared screen in phases: (1) one room with no exits, group camera and edge walls; (2) side exits; (3)
  vertical exits; (4) gates; (5) camera sections (`051`); (6) death and respawn. This branch is phase 1.

From "Open questions": tall rooms (a player below or above the view inside the room). The owner decides at the phase 1
play test.

## Current state

- The camera follows one `IFocusable` (`engine/.../camera/CameraFollow.kt`). `focus.getFocusPosition()` moves the
  camera; `focus.getFocusBounds()` is read only by `CameraRoomBounds` (`:96`, `:107`, `:135`, `:140`, `:163`) for the room
  scan and the room transitions.
- The level screen sets the focus once, to slot 0 (`MegaLevelScreen.kt:326`, `cameraFollow.setFocus(gameCamera,
  player.focus)`). `player` is slot 0 (`:165`).
- `Megaman`'s focus (`megaman/components/FocusComponent.kt`) is its body bounds, and a position point offset by half
  a body height in the gravity direction.
- The camera parts run in the render path after `engine.update` (`O2-R7`). Movers: `CameraRoomBounds`,
  `CameraRoomTransition`, `CameraFollow`, `CameraScroll`, `CameraLimitStage`, `CameraRoomClamp`, `CameraRoomScan`.
  Effects: `CameraRotation`, `CameraShake`, `cameraFollow.stopRule`, `CameraEdgeBlocks` (`MegaLevelScreen.kt:288-324`).
- `CameraEdgeBlocks` spawns one `CameraEdgeBlock` for each blocked edge of the camera limits (zones and scrolls), only
  when the edge is inside the room (`CameraLimitStage.activeEdge`). `cameraEdgeBlockBounds()` places a 1-tile block
  outside the edge.
- `CameraEdgeBlock` blocks only players (`addBlockFilter`), does not draw, does not cull, and never sets a side sense
  (`NO_SIDE_TOUCHIE`, `051` `NOTES.md`), so a player cannot wall-slide on it. It crushes only when it moves into the view
  and a crush surface is active.
- `MegaPlayers.isLive(player)` (body, `liveNow`, `inPlay`) and `forEachLive` exist since `047b`.
- With `--players 2` today, the camera follows slot 0 only. Slot 1 can walk out of the view. Blocks that spawn by camera
  cull after 1 s out of view (`Block.kt:75`), so slot 1 can lose its floor (item `ITEM.md`, "Camera and off-screen").
- `RefactorTest047.tmx` has 11 rooms, each 16x14 tiles (one view). The camera never moves in them.

## Why no wall pushes a player

The camera centers on the midpoint of the live players. The view moves only when the midpoint moves. If player A
moves inward at speed `v`, its own wall moves inward at `v / 2`, so A moves away from it, and the other wall moves away
from the other player. When the room clamp holds the view, the walls do not move. So in phase 1 a wall never moves into
a player. Camera sections (`051`) can move the view on their own; they are phase 5.

## In scope

1. **Group focus.** A new `IFocusable` for the players, used only when slot 1 is registered:
   - position: the mean of the focus positions of the live players (`MegaPlayers.isLive`). If none is live, slot 0.
   - bounds: slot 0's focus bounds. So the room scan and the room transitions keep the slot 0 trigger of today. "Carry"
     is phase 2.
   - no allocation per frame (`O5-R1`): one reused `Vector2`.
   - first caller: `MegaLevelScreen.init()`, in place of `player.focus` when 2 players are registered.
2. **View edge rules (D4).** Each of the 4 view edges has one rule: `NONE`, `WALL`, `BOUNCE`, `WARP` or `KILL`. A rule
   acts only where the view edge is inside the current room (room bounds come first). A player is "out" when its body
   is fully past the edge (`isPastCameraEdge`).
   - `WALL`: a players-only `CameraEdgeBlock` at the edge (`cameraEdgeBlockBounds()`). At the bottom edge, the player
     stands on air.
   - `BOUNCE`: a player past the edge is put back at the edge with a speed into the view; the damage goes through the
     player damage path (`ViewEdgeHazard`), so the player gets the damage reaction, the blink and the iframes
     (changed at the play test, 2026-11-06).
   - `WARP`: a player fully past the edge is held: frozen in place (movement, behaviors and damage locked, no gravity,
     no collision) and left out of the group focus, so the camera follows the players in view. A white line and circle
     show the warp spot, 1 tile beside a partner in view, on the side of the view center. After `delay` seconds the
     player appears there, with the damage below. When the rules turn off, a held player is released where it is
     (changed at the play test, 2026-11-06: the floor culled before the warp, and the side warp snapped).
   - `KILL`: after `delay` seconds out, the player dies (slot 0: the level resets, as for any slot 0 death).
   - `NONE`: nothing (today's behavior).
   - The values live in a typed `ViewEdgeRules` (a rule for each edge, `delay` in seconds, `damage` in health points)
     in `SessionRules`. Default for every mode: `left=WALL, right=WALL, top=NONE, bottom=NONE, delay=0, damage=0`.
   - A dev flag overrides it at launch: `--viewEdges left=WALL,right=WALL,top=NONE,bottom=WARP,delay=1.5,damage=3`.
     A key that is not given keeps its default. A bad key or value exits with code 2, as the other dev options do
     (`resolveLaunchOptions`). Only with 2 players; with 1 player the flag is a launch error.
   - On: 2 players registered, slot 0 in play, no room transition, no camera turn, and no active camera zone or
     scroll (D3, with a `TODO` that points to phase 5).
   - The out timer is per player and per edge, and it resets when the player is back in view.
3. **Ease on a change of the group (D2).** When the set of live players changes (for example slot 1 dies), the camera
   eases to the new focus, not a snap: the group focus moves from the old midpoint to the new one over 0.5 s (changed at the play test, 2026-11-06: the smooth follow was too fast to see). The same ease runs when the
   midpoint jumps by more than 1 tile in one read, for example after a warp.
   On an axis with a `WARP` or `KILL` edge, the group focus keeps one player fully in view (the keeper), so the warp
   target is always in view: with a leave edge on one side, the player farther from it; with leave edges on both
   sides, the last keeper stays, and at the first split the one nearer the view center (owner's option B, play test
   2026-11-06).
4. **Test rooms** in `RefactorTest047.tmx`, built with the `level-builder` skill: `group_wide` (48 tiles wide, flat, with
   platforms), `group_pit` (wide, with a pit in the middle), `group_tall` (16 wide, 3 views tall, with a ladder),
   `group_clamp` (wide, a player spawn near the left room edge). Each with painted `tiles` and a player spawn.

## Out of scope

- Room exits with 2 players ("carry", phase 2). Slot 0 still triggers a transition, and all players stack at the
  transition point as today (`Megaman.kt:386`).
- The final choice of the edge rules: the play test tries them through the flag, and the owner then sets the defaults.
- Edge rules in a turned room (`CameraRotation`) and with an active camera zone or scroll (phase 5). The rules are off
  there, with a `TODO` (D3).
- A map or room override of the edge rules.
- Death and respawn of slot 1 (`047c`), HUD (`047d`).
- Split screen (`094`).

## New API and first callers

| API | First caller |
|---|---|
| Group focus class (`IFocusable`) | `MegaLevelScreen.init()` |
| View edges camera effect (`ICameraPart`, `Resettable`) | `MegaLevelScreen.init()`, `addEffect` |
| `ViewEdgeRule` enum and `ViewEdgeRules` value, field in `SessionRules` | the view edges effect |
| `--viewEdges` dev flag and its parser | `DesktopLauncher` and `resolveLaunchOptions` |
| Bounce reaction of a player on a `BOUNCE` block | the `BOUNCE` edge block |

No other new API. `CameraEdgeBlock` and `cameraEdgeBlockBounds()` are reused. A map or room override of the rules waits
for a real map that needs it (`O1-R5`).

## Affected areas and risks

- `MegaLevelScreen.kt` (camera setup), the 2 new files in `screens/levels/camera/`, `RefactorTest047.tmx`.
- Risk: the 1-frame lag of the walls. The camera moves after the physics step, so the walls of frame N act in frame
  N+1. A fast player (air dash) can be 1 frame past the edge. The block then pushes them back.
- Risk: vertical spread. A player can still leave the view up or down in a tall room, and its floor can cull. This is
  the open question for the play test.
- Risk: 1 player. The single-player path must not change: the group focus and the walls are not built with 1 player.

## Overlap with other branches

- `081b` (`mega-refactor-081b-x-run-jump-shoot`) changes `MegaLevelScreen.kt` at the weapon energy pickup, and keeps
  `cameraFollow.setFocus` (whitespace only). A merge conflict at `:326` is possible, and simple.
- `038b` and `compactor-level`: no overlap in camera or players code.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | The walls effect runs after `CameraEdgeBlocks`; the order is set in one place. |
| `O1-R5` Code only with a production caller | no effect | Both new types have a caller in `MegaLevelScreen`. |
| `O2-R4` Players in slot order | no effect | The mean is order-free. |
| `O2-R5` No logic assumes one player | helps | The camera follows all live players. |
| `O2-R6` No logic assumes one camera | waiting | One camera, as the 2026-11-06 decision intends. |
| `O2-R7` Gameplay does not depend on rendering | waiting | The walls come from the camera, which moves in the render path, as the `051` edge blocks do. |
| `O5-R1` No per-frame allocation | no effect | One reused vector; blocks spawn once and move. |
| `O6-R2` Named checks | helps | Contract checks in new test rooms. |

## Decisions (owner, 2026-11-06)

- D1: the group focus is the midpoint on both axes.
- D2: on a change of the live players, the camera eases (smooth follow), no snap.
- D3: no edge rules in a turned room or while a camera zone or scroll acts, with a `TODO` (phase 5).
- D4: in place of "observe only" or one warp rule, each view edge gets a rule from `NONE`, `WALL`, `BOUNCE`, `WARP`,
  `KILL`, with a `delay` and a `damage` toggle, set by a dev flag. `WARP` puts the player 1 tile beside the partner.
  Default damage is 0. The owner wants to discuss the rules more after the play test.

## Open questions

- The final default for each edge, after the play test.
