# Scope: mega-refactor-051b-camera-behaviors

- Status: approved
- Approved: 2026-10-30

## Goal

The game camera is driven by one camera controller with ordered parts (follow, room bounds, room transition,
shake), in place of `CameraManagerForRooms` and the camera code in `MegaLevelScreen`. The room side effects (music,
room events, enemy cleanup, gravity by room) move out of the camera into a room handler. The `focusSnappedAway`
flag becomes 2 requests on the follow part. The game plays the same.

The camera stays dumb: it knows where it points and how it is rotated. It does not know about a focus, rooms or
bounds. These are rules of the controller and its parts, so a context can build only the parts that it needs, and
more than one camera can follow the same rules.

## Item

`051`, from the 051 `ITEM.md`, "Decisions":

- 2026-10-29 (owner): the camera is a raw camera with behavior components attached (room bounds, X-style bounds from
  stage objects, auto-scroll, rotation, focus), each with its own rules. The writers of the flags move into the
  behaviors.
- 2026-10-30 (owner): remove `GameCameraState`. `rotating` becomes a value of the rotation behavior (typed component
  lookup, a `core` extension may shorten it). `focusSnappedAway` is a request to the focus behavior.
- 2026-10-30: the focus request is a method on the focus behavior, not an event (timing: the writers run in
  `engine.update`, the reader runs after it in the same frame).
- 2026-10-30 (owner): the camera interface goes in `engine`.
- The branch split, item 2: `051b-camera-behaviors` (refactor): "the camera is built from behavior components (room
  bounds, focus follow with the smooth move, rotation, maybe the shaker); `MegaLevelScreen` talks to a camera control
  interface, not to `CameraManagerForRooms`; `GameCameraState` goes. Its scan answers the research questions in
  `NOTES.md` first. It may split again."

## Current state

The scan is in `docs/NOTES.md` ("Scan"). In short:

- `CameraManagerForRooms` (324 lines) does 4 jobs in one class: room tracking, follow (with the
  `focusSnappedAway` lerp and the absolute x clamp), room bounds (`coerceIntoBounds`, and the x-only follow in the
  prior room when the focus is in no room), and the room transition (a state machine: 0.35 s delay, 1 s pan).
- `MegaLevelScreen` wires 5 callbacks into it. The callbacks hold room side effects, not camera logic. The screen
  also owns the shaker, the rotation start, the `focusSnappedAway` clear and the level-start resets. 95 lines of
  `MegaLevelScreen` mention the camera.
- `focusSnappedAway`: 7 entity writes (`Megaman` ×3, `ClimbBehavior` ×2, `PipePortal` ×2), 1 game write
  (`MegamanMaverickGame.kt:316`, the rotation end), 5 level-screen writes and 2 level-screen reads.
- `rotating`: 39 entity reads, 2 level-screen writes, 1 game write. The backgrounds rotate their own
  `RotatableCamera`s (`Background.kt:70,108-129`), and item `046` plans to compute background positions from the
  game camera.

## Research results (the `NOTES.md` questions)

The full research is in `docs/NOTES.md` ("Research"). Sources: `mmx-demake-tools/docs/engine.md` ("Camera",
"Camera zones", "The game frame", "Checkpoints"), `docs/enemies/conveyor-belt.md` ("The forced scroll"),
`docs/enemies/boss-gate.md`, `docs/stages/shark-2.md`.

1. **Focus reuse, first-class behaviors, or bounds changes?** Recommended: one follow, and each behavior as a change
   to the camera bounds (option C, the X6 way). X6, Sonic, Mega Man X (SNES), Hollow Knight and Godot use it. In X6,
   zones, gates and auto-scroll only write target limits, and the follow and one clamp do the rest. The one exception
   is a scripted move (a room transition pan, a warp): a part that takes the camera position for a time and suspends
   the follow. This keeps the current room transition as it is.
2. **One class or several?** Recommended: several small parts, run by one controller in a fixed order: the follow
   (with the smooth-move requests), the bounds (rooms now), the position override (the transition pan), and the
   effects after them (shake now, rotation later). A new behavior is a new part; each part can be tested alone.
3. **Who owns the room logic?** The camera owns only the room bounds and the pan. The controller off/on, music,
   `BEGIN/CONTINUE/END_ROOM_TRANS` and `SET_TO_ROOM_NO_TRANS` events, the enemy and projectile cleanup,
   `BehaviorsSystem` on/off, `levelState.roomTransition`, the boss and victory room events, the room gravity
   direction and the players' `running` reset go to a room handler in `core` that the controller notifies.

## In scope

1. **`engine`: the camera controller.** A controller that owns the parts and updates them in a fixed order, and the
   part interface. Each part has a caller in this branch (the list below). The level screen calls the controller in
   the same place in the frame where it calls the manager, the shaker and the flag clear now (`MLS:985-994`).
2. **The parts** (placement: see `docs/NOTES.md`):
   - **Follow:** puts the camera on the focus (`IFocusable` moves with it), with the absolute x clamp, and the smooth
     move: `startSmoothFollow()` and `stopSmoothFollow()` replace the flag writes (7 entity writes, the level-screen
     writes and the clear when the camera reaches the focus). Same lerp value (`20 * deltaTime`) and the same
     0.25-tile stop rule.
   - **Room bounds:** `gameRooms`, `currentGameRoom`, `priorGameRoom`, `nextGameRoom()`, the clamp into the current
     room, and the x-only follow in the prior room.
   - **Room transition:** the pan (delay, duration, start and target, `transitionInterpolation`, the scanner
     direction) and `transitionToRoom(name, direction)` for `NEXT_ROOM_REQ`.
   - **Shake:** `CameraShaker`, as a part.
   - Bounds leave the camera: `RotatableCamera.coerceIntoBounds` (`RotatableCamera.kt:88`) moves into the room bounds
     part. A part gets the camera as a parameter of its update and does not store it, so the same rules can drive
     another camera later. Each camera has its own controller and part instances, because parts hold state (for
     example the current room). This branch has one game camera, and it adds no code for a second one.
   - A context chooses its parts when it builds the controller. A part that is omitted has no effect on the others:
     follow works with no rooms; the room parts are optional. `remove` at run time has no caller yet, so it is not in
     this branch.
   - The order must stay exact, and rotation stays in `RotatableCamera.update` until `051e`. So a one-line adapter part
     calls `camera.update(delta)` at its current place in the order (after the room parts, before shake). `051e`
     replaces it.
3. **`core`: the room handler.** The 5 callbacks of `MLS:286-409` move, line for line, into one class that the
   level screen makes and the controller notifies (begin, continue, end, set to room with no transition, first update
   after reset). Their order and their events do not change.
4. **The level screen** uses the controller and the room handler; it no longer names `CameraManagerForRooms`.
   `levelState.roomsSupplier` and `currentRoomSupplier` read the room bounds part (12 readers unchanged).
5. **The smooth-move requests** for entities. `services.gameCamera` stays read-only. A new holder field
   `services.cameraController` gives the controller; a typed lookup (`getPart(CameraFollow::class)`) and a `core`
   extension give the 2 requests, for example `services.cameraController.startSmoothFollow()`. The host owns the
   controller (`game.cameraController`, made with the game camera, so it exists when `postCreate()` builds the
   holder); the level screen adds its parts. The 7 entity writes and the game write at
   `MegamanMaverickGame.kt:316` change. `GameCameraState.focusSnappedAway` goes. `RotatableCamera` still implements
   `ICameraView` in this branch; the separate `CameraView` class comes in `051e`, where its first caller is.
6. **A temporary equivalence suite** (named in `CONTRACT.md`): a frozen copy of `CameraManagerForRooms`, the shaker
   update and the flag clear, run side by side with the new controller on seeded room layouts and focus paths
   (walks across rooms, gaps between rooms, requested transitions, resets, smooth-move requests). Each frame it
   compares the camera position, the current and prior room, and the order of the callbacks.
7. **Permanent tests** for the parts: the follow clamp and the smooth-move stop rule, the room clamp, the transition
   timing and target.

## Out of scope

- **Rotation and the end of `GameCameraState`** (proposed new branch `051e-camera-rotation`): the rotation part, the
  typed lookup for `rotating` (39 entity reads), and the backgrounds' own rotating cameras. Item `046` changes how
  backgrounds follow the camera, so this branch should come with or after `046`. Until then `GameCameraState` keeps
  only `rotating`.
- Limit easing, a stack or priority of bounds sources, and camera zones: `051c` (no caller before it).
- Auto-scroll: `051d`.
- Moving the rotation request out of `Megaman.direction`: item `058`.
- The `ConstKeys.FOCUS` override in `Megaman.getFocusPosition()` that nothing sets: a backlog candidate.
- The backgrounds' delta move (`MLS:1001-1004`) and the position rounding of the viewports (`MLS:1007-1011`): they
  stay in the level screen.

## Affected areas and risks

- Files: `CameraManagerForRooms.kt` (replaced), `CameraShaker.kt` (becomes a part), `MegaLevelScreen.kt` (the camera
  set-up, the 5 callbacks, the frame block, 4 event cases, the level start and end), `GameCameraState.kt`,
  `MegamanMaverickGame.kt`, `Megaman.kt`, `ClimbBehavior.kt`, `PipePortal.kt`, `EntityQueries.kt` (`IFocusable`
  import), new controller and part files, new room handler, tests.
- Risk: the room logic has edge cases (the focus in no room, the prior-room x-only follow, a reset in the same frame
  as a transition, `transitionToRoom` with no direction). The equivalence suite compares them frame by frame.
- Risk: the callback order. The room handler gets the same calls in the same order; the suite records the order.
- Risk: frame order. The controller runs at the same point in the frame, and its parts in the old order: room
  manager, then (rotation stays in `gameCamera.update` for now), then shake, then the smooth-move stop.
- Risk: `084a` changes one of the smooth-move writes (see "Overlap").

## Overlap with other branches

See `docs/NOTES.md`. `084a-player-state` (at its play test) moves a `focusSnappedAway = false` write into a
`TeleportableComponent` hook and changes `PipePortal`. This branch takes `084a` from `master` before the code steps
that change these writes. `031d-devices`: no overlap.

## Open questions

None. All 4 are answered in `docs/NOTES.md`, "Decisions".
