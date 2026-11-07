# Notes: 130 `LevelCamera` and `SharedView`

## Review of 2026-11-06 (read-only design agent, worktree `047h` at `a6a8827fc`)

### Inventory

`MegaLevelScreen.kt`, 1027 lines; about 165 lines of camera code can move.

| Where | Lines | Count | What |
|---|---|---|---|
| imports | 20-22, 71, 75, 85-97 | 18 | camera parts, `ViewEdgeHazard`, `Gate` |
| companion | 127-129 | 3 | `DRAW_CAMERA_SECTIONS`, `ROOM_DISTANCE_ON_TRANSITION`, `TRANSITION_SCANNER_SIZE` |
| fields | 207-217, 223-224, 226 | 14 | `gameCamera`, `uiCamera`, 9 part fields, `gameCameraPriorPosition`, `camerasSetToGameCamera`, `spawnCameraBounds` |
| `init()` | 284-370 (+372-374) | 87 (+3) | builds the parts, adds them in order, 2-player branch, `controller.reset()` |
| `start()` | 397, 406-407, 453-466 | 17 | `setFocus` (before `super.start`), room suppliers, `stopSmoothFollow`, start position, follow `minX`/`maxX` |
| `buildLevel()` | 499-512 | 14 | rooms, gate rectangles, zones, scrolls |
| `roomHasCameraSection()` | 547-550 | 4 | |
| `resetLevelForRespawn()` | 556, 563, 578 | 3 | `controller.reset`, `stopSmoothFollow`, `cameraShake.stop` |
| `onEvent` | 645-658, 718-727, 768-777 | 34 | `NEXT_ROOM_REQ`, `SHAKE_CAM`, `START_GAME_CAM_ROTATION` |
| `render()` | 815, 831, 837-847 | 13 | spawn bounds, `controller.update`, background delta, viewport rounding |
| `draw(renderer)` | 920, 927-948, 950-964 | 38 | camera sections debug draw, warp guides, camera bounds debug draw |
| `reset()` | 992-994, 1001 | 4 | `controller.reset`, `rotation.snap(UP)`, `applyOrientation`, `stopSmoothFollow` |

Readers of the part fields: `roomBounds` (348, 358, 372, 406-407, 500, 548, 649), `roomTransition` (330, 339, 359,
372), `cameraFollow` (397, 453, 459-460, 563, 1001), `cameraZones` (509, 549, 934-941), `cameraScroll` (512, 549,
931-938), `cameraShake` (578, 720-725), `viewEdges` (945), `playersFocus` (397), `roomExits` (501), `gameCamera` (257,
456, 649, 837-840, 865, 873, 920).

Repeated code:

- `player.inPlay && !roomTransition.transitioning` 3 times; `!cameraView.rotating && cameraRotation.direction == UP`
  2 times; `settledPosition` passed 3 times; `{ edges.isHeld(it) }` 2 times.
- `controller.reset()` 3 times (370, 556, 992); `stopSmoothFollow()` 3 times (453, 563, 1001).
- "2 players" held as 3 nullable fields, checks at 352, 357, 363 and a `!!` at 366.
- `camerasSetToGameCamera` is written and never read.
- 5 test rigs build the part order by hand with 37 `addMover`/`addEffect` lines.

Facts to keep: the controller belongs to the game, and `getPart(CameraFollow::class)` serves smooth follow to
`Megaman`, `ClimbBehavior`, `PipePortal` and `PlayerLifecycle`; `init()` runs once per screen object; slot 0 can change
per level (`PlayerRoster`), so the focus is set in `start()`.

### Options

- A: one `LevelCamera` owns the whole stack. The 2-player parts stay as nullable fields inside it.
- B: a builder function plus a typed holder. Moves only the construction (about 90 lines); the screen still reads 9
  part fields.
- C (chosen): `LevelCamera` plus a `SharedView` add-on for 2 players, and the conditions written once.
- D: A or C plus a `LevelCameraEvents` listener. Not advised: a new listener is new order coupling (`O1-R3`), the
  rotation event also turns the backgrounds (`046`, `058`), and event cleanup is `053`/`057`.

### Risks

- The 3 level start points must keep their order.
- The 3 reset paths differ; each keeps its own method.
- The `SharedView` effects go after `CameraEdgeBlocks`, as today.
