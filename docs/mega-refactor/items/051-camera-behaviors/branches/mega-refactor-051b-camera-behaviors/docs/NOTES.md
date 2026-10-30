# Notes: mega-refactor-051b-camera-behaviors

## Scan (2026-10-30, at `5787a0151`)

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. `MLS` = `C/screens/levels/MegaLevelScreen.kt`.

### The camera parts now

- `RotatableCamera` (`C/screens/levels/camera/RotatableCamera.kt`): the `OrthographicCamera`, the rotation state
  (`direction`, `startRotation`, `immediateRotation`, `update` interpolates the turn, `onJustFinishedRotating`),
  `coerceIntoBounds(bounds)`, and the `ICameraView` reads (`051a`).
- `CameraManagerForRooms` (`C/screens/levels/camera/CameraManagerForRooms.kt`, 324 lines): one class that does 4
  things:
  1. Room tracking: `gameRooms`, `currentGameRoom`, `priorGameRoom`, `nextGameRoom()` (the room that contains the
     focus center).
  2. Follow: `setCameraToFocusable(focusY)` puts the camera on `focus.getFocusPosition()`, with an optional lerp
     (`shouldInterpolate` = `focusSnappedAway`, `interpolationValue` = `20 * deltaTime`), and clamps x to
     `absoluteMinX..absoluteMaxX`.
  3. Bounds: `camera.coerceIntoBounds(currentRoom)`; when the focus leaves the current room and is in no room, it
     follows x only and clamps to the prior room.
  4. Room transition: when the focus overlaps another room, `getOverlapPushDirection` with a 5x5-tile scanner gives
     the direction; a state machine (`BEGIN`, `CONTINUE`, `END`) waits `ROOM_TRANS_DELAY_DURATION` (0.35 s), then
     moves the camera from start to target over `ROOM_TRANS_DURATION` (1 s); `transitionInterpolation` moves the
     focus target by `ROOM_DISTANCE_ON_TRANSITION` (2 tiles) into the next room. `transitionToRoom(name, dir)` starts
     one on request (`NEXT_ROOM_REQ`, from `Gate`).
  - Callbacks: `beginTransition`, `continueTransition`, `endTransition`, `onSetToRoomNoTrans`,
    `onFirstUpdateAfterReset`. `reset()` sets a flag; the next `update` snaps to the focus and finds the room.
- `CameraShaker` (`C/screens/levels/camera/CameraShaker.kt`, 58 lines): moves `position` by `±shake` each interval
  for a duration. `SHAKE_CAM` (7 entity submitters) starts it; `PLAYER_SPAWN_INIT` stops it.
- `GameCameraState` (`rotating`, `focusSnappedAway`): 56 reads and writes in `core` (main and test).
- `IFocusable` (`getFocusBounds`, `getFocusPosition`), in `CameraManagerForRooms.kt`. Only `Megaman` implements it
  (`Megaman.kt:904-918`: the body bounds; the position is a point above the feet in the gravity direction).
  `entities/EntityQueries.kt:15` gives `IGameEntity.focus`. `ConstKeys.FOCUS` is read at `Megaman.kt:907` and removed
  at `:507`, but nothing puts it (dead override).

### What the level screen does with the camera (`MLS`)

- Builds the manager (`MLS:273-409`) and wires 5 callbacks. The callbacks hold the room side effects, not camera
  logic:
  - `beginTransition` (`MLS:289-324`): `TURN_CONTROLLER_OFF`; music fade (room property `fade_out_music`);
    `BEGIN_ROOM_TRANS` event; destroys all enemies and the projectiles that a player does not own; turns off
    `BehaviorsSystem`; `levelState.roomTransition = true`; room music.
  - `continueTransition` (`MLS:326-345`): `CONTINUE_ROOM_TRANS` event; `roomTransition = true`.
  - `endTransition` (`MLS:346-399`): `END_ROOM_TRANS` event; the room `event` property (`boss` →
    `ENTER_BOSS_ROOM`, `success` → `VICTORY_EVENT`) or `TURN_CONTROLLER_ON`; the room `megaman_direction` property
    (gravity change, aura, sound); `BehaviorsSystem` on; `roomTransition = false`; players stop running.
  - `onSetToRoomNoTrans` (`MLS:400-409`): `SET_TO_ROOM_NO_TRANS` event; room music.
  - `onFirstUpdateAfterReset` (`MLS:286`): room music.
- Level state: `levelState.roomsSupplier` and `currentRoomSupplier` read the manager (`MLS:443-444`); 12 files read
  them.
- Frame order (`MLS:940-1004`), when not paused: `spawnCameraBounds` = rotated bounds; spawns; handlers;
  `engine.update`; player spawns; queued spawns; backgrounds update; **`cameraManagerForRooms.update`**;
  **`gameCamera.update`** (rotation); **`gameCameraShaker.update`**; **the `focusSnappedAway` clear** (focus within
  0.25 tile of the camera); then (also when paused) the backgrounds move by the camera delta, and every viewport
  camera position is rounded.
- Events: `NEXT_ROOM_REQ` → `transitionToRoom`; `SHAKE_CAM` → shaker; `START_GAME_CAM_ROTATION` →
  `gameCamera.startRotation` + backgrounds + `rotating = true`; `PLAYER_SPAWN_INIT` → manager `reset()`, shaker
  stop, `focusSnappedAway = false`, music from `nextGameRoom()`.
- Level start (`MLS:442,490-509`) and end (`MLS:1132-1140`): resets of the manager, rotation, flags, the
  absolute x limits (`VIEW_WIDTH / 2` to the world width).

## Research (2026-10-30)

A research agent read the X6 docs and summarized other engines; the X6 facts below were spot-checked against the
sources. Facts only; no code from `mmx-reference`.

### X6 (`~/IdeaProjects/mmx-demake-tools/docs/`)

- Camera state (`engine.md`, "Camera"): per layer, the camera x/y, current limits (min/max x/y), target limits, the
  follow line (`+0x2E`), follow on/off (`+0x44`), the limit speed in px/frame (`+0x47`). `CameraLimitEase` moves each
  limit toward its target, relative to the camera.
- Zones (`engine.md`, "Camera zones"): `CameraZone` is a trigger rectangle plus 4 limits (`0xFFFF` = keep); while the
  player is inside, it writes the target limits and sets the speed to 2. No stack.
- Gates (`enemies/boss-gate.md`): a gate writes new target limits after it opens. Teleports and warps set the camera
  position and both limit sets directly, with no ease.
- Frame order (`engine.md`, "The game frame"): player, weapons, kind-7 objects (`AutoScroll`), main objects, stage
  logic, `CollideCheck`, then the cameras. The camera follows after collision. Hit stop freezes the layer-0 camera.
- Auto-scroll (`enemies/conveyor-belt.md`, "The forced scroll"; `stages/shark-2.md`): an object with no sprite; state
  2 adds 0.5 px per update to the camera x and sets min x = max x = camera x (current and target); a stage flag ends
  it. It stops in hit stop.
- Screen edge (`engine.md`, "Camera"): `CameraClamp` clamps the player in x only, to min x + 8 … max x + 312. Pit
  death: 256 px below the lowest limit (`players/ladder-hurt-events.md`, "Pit"). Crush: terrain on 2 opposite
  sides kills (`ladder-hurt-events.md`, "Crush"); the Ride Armor has a screen-edge crush (`players/ride-armor.md`).
- Respawn (`engine.md`, "Checkpoints"): `CheckpointStart` writes each limit and its target from the same value (no
  ease); the objects come back, so `AutoScroll` starts again.

### Other games and engines (summary)

- Mega Man (NES): fixed sections, scripted pan between them (the current Maverick model).
- Mega Man X (SNES), Sonic (Genesis), Godot `Camera2D`, Phaser: one follow inside limits; events, zones or scripts
  change the limits (Sonic eases the bottom limit at 2 px/frame).
- Celeste: one follow clamped to the room; triggers add offsets or lock an axis; a room change is a slide.
- Hollow Knight: `CameraLockArea` volumes set bounds on one follow camera; overlapping locks resolve as a stack.
- Unity Cinemachine: several virtual cameras built from components, picked by priority and blended. Not a fit: no
  cuts between framings are needed, and the blends cost per-frame work.

### Conclusions for 051 (see `docs/SCOPE.md`, "Research results")

- Model: one follow, each behavior a bounds change (option C), plus a position-override part for scripted moves (the
  room pan, warps).
- Structure: several small parts in a fixed order under one controller.
- Questions for `051c`/`051d` (not this branch): overlapping bounds (stack, newest wins, "keep" per edge), ease or
  snap per source (rooms snap, as now), the player at the screen edge in auto-scroll (clamp; crush rule), respawn
  (current = target, no ease), auto-scroll stops while paused or frozen.

## Decisions

- 2026-10-30 (owner): `SCOPE.md` approved. The research answers and the 4 open questions are accepted:
  1. Rotation and the removal of `GameCameraState` go to a new branch `051e-camera-rotation`, with or after `046`.
  2. `engine`: the controller, the part interface, follow, shake, the transition pan. `core`, for now: the room bounds
     and the room handler.
  3. Replaced on 2026-10-30, see below. (It was: a typed lookup on `services.gameCamera`.)
  4. Names: `CameraController`, `ICameraPart`, `CameraFollow`, `CameraRoomBounds`, `CameraRoomTransition`, `CameraShake`,
     `LevelRoomHandler`.
- 2026-10-30 (owner, verbatim): "the camera should be dumb and only know about where to point and what bounds to
  respect (or is the latter also the manager's responsibility? if it's the manager's, then multiple cameras can exist
  and follow the same rules at the same time)". Answer: the bounds are the controller's. `coerceIntoBounds` leaves
  `RotatableCamera`. Parts get the camera as an update parameter.
- 2026-10-30 (owner): the view (reading) is separate from the controller (management). This replaces answer 3:
  `services.gameCamera` is for reads only, and a new `services.cameraController` gives the 2 smooth-move requests,
  through the typed lookup and a `core` extension.
- 2026-10-30 (owner, verbatim): "several small parts that can be added, removed, or omitted, that way the camera logic
  can be built on demand to suit the current context's needs".

## Overlap with other branches

- 2026-10-30: `084a-player-state` (active, at its play test) moves the `Megaman` teleport hooks into
  `TeleportableComponent` hooks set in `Megaman.init()`. One of them writes `services.camera.focusSnappedAway = false`
  (on `084a`: `Megaman.kt:343`), and `PipePortal` (2 writes) is in its diff. This branch replaces every
  `focusSnappedAway` write. Plan: take `084a` from `master` (merge `master` into this branch) before the code steps
  that change these writes; if `084a` is not merged by then, write the overlap in the process log and change the
  writes at their `master` places.
- 2026-10-30: `031d-devices` (active, at its play test): input and controller files only. No overlap.
- 2026-10-30, before the merge: `master` has no code change since this branch last took it in (docs only), so no new
  merge of `master` is necessary. `031e-bindings` also changes `MegamanMaverickGame.kt` and `MegaLevelScreen.kt`, but
  in other places (imports, the `buttons` set-up in `create()`, the select button read at `MLS:926`), not the camera
  code. `084d-damage-reaction` has no shared file now; its `NOTES.md` expects at most a small import conflict in
  `Megaman.kt`. The branch that merges later takes `master` and runs its checks again.
