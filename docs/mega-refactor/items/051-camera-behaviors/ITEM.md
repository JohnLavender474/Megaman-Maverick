# 051: X-style camera bounds, auto-scroll and other camera behaviors

- Status: planned
- Type: feature
- Depends on: none
- Related: `019`, `036`, `038`, `041`, `047`
- Branches: none yet

## Goal

Support camera behaviors beyond room bounds (X-style bounds set by stage objects, auto-scroll), and decouple `MegaLevelScreen` from `CameraManagerForRooms`.

## Item text

See `NOTES.md` for the full item text: the behaviors, the X6 facts, the problems now, the goal, the research questions and the contract note.

## Owner's input

From the camera request (summary): X6 manages the camera through objects that control bounds; `CameraManagerForRooms` supports only rooms, and `MegaLevelScreen` is tightly coupled to it. Research the focusable versus first-class design, and one class versus several.

2026-10-29 (verbatim, about the `016c` camera state): "Do these changes play nicely with the item that exists for
support X-style "camera zones"? Ideally, the game camera's behavior is built in a composable and easy-to-modify
manner, i.e. a raw camera with components attached to it that affect is behavior and rules." Then "yes" to recording it
here.

## Decisions

- 2026-10-29 (owner): the camera is a raw camera with behavior components attached (room bounds, X-style bounds from
  stage objects, auto-scroll, rotation, focus), each with its own rules. `016c` adds `GameCameraState` (the flags
  `rotating` and `focusSnappedAway`; game and holder field `camera`) as the read side that entities use (39 reads).
  Here, those flags become values that the rotation and focus behaviors expose; the entity reads stay; the writers
  (today in `MegaLevelScreen` and the game reset) move into the behaviors.
- 2026-10-30 (owner, verbatim): "Fold the removal of `getGameCamera` into 051". This item removes the function
  `MegaServices.getGameCamera()`. Callers read the camera object (`RotatableCamera`) directly, through a property.
  They cannot use `GameCameraState` in its place: it has only the 2 flags, and every caller needs the camera geometry
  (`getRotatedBounds` 38, `overlaps` 9, `toGameRectangle` 2, `direction` 1, 32 bare calls; scan on 2026-10-30:
  82 lines in 61 files contain `getGameCamera()`, about 76 of them `services.getGameCamera()`). The host function
  `MegamanMaverickGame.getGameCamera()` (it reads `viewports` on each call; screens use it, for example
  `MegaLevelScreen.kt:236`) is in the same scope. The camera instance does not change in a session: the
  pixel-perfect toggle (`MegamanMaverickGame.kt:691-712`) makes new viewports with the same camera, so a stored
  property gives the same object as the function.
- 2026-10-30 (owner): entities read the camera through a read-only interface with live values, not through a
  snapshot. The interface goes in `engine`. `RotatableCamera` (or its 051 successor) implements it; the holder and
  host property and the `getGameCameraCullingLogic` parameters (`GameEntityUtils.kt:56-60`) take the interface type.
  No entity writes to the camera now (scan on 2026-10-30), so the interface removes no current use. Reasons against
  a snapshot (scan on 2026-10-30):
  - Frame order: `engine.update` runs at `MegaLevelScreen.kt:971`, and the camera moves after it
    (`MegaLevelScreen.kt:984-987`). Event listeners (`events.run()`), the queued spawns and `runQueue` run after the
    move and see the moved camera. A snapshot taken before `engine.update` gives them old values, and a correct
    snapshot needs a refresh at each of these points.
  - `DarknessV2.kt:331-332` and `DarknessV4.kt:593-594` change the rectangle that `getRotatedBounds()` returns
    (`translate`, `translateSize`). A shared snapshot rectangle would be corrupted. These 2 sites copy the bounds or
    take an `out` rectangle.
  - `Camera.overlaps` (`utils/extensions/CameraExtensions.kt:9`) tests the libGDX frustum, which changes only at
    `viewport.apply()` in the draw. It is not the same test as `getRotatedBounds()`, so a snapshot rectangle cannot
    replace its 9 calls with no behavior change.
- 2026-10-30 (owner): remove `GameCameraState`. Its 2 flags are not one kind of data:
  - `rotating` repeats the camera's own rotation state (`RotatableCamera.rotFinished`). It becomes a value of the
    rotation behavior. A camera that cannot rotate has no rotation behavior, so a reader gets it through a typed
    component lookup (`KClass` key, as the engine components do), not through string-keyed props. A `core` extension
    function may shorten the lookup where it helps the callers.
  - `focusSnappedAway` has no entity reader: only `MegaLevelScreen` reads it (lines 281 and 989). The 7 entity uses
    are writes (`PipePortal.kt:128,150`, `Megaman.kt:211,504,508`, `ClimbBehavior.kt:142,215`). It is a request to
    the focus behavior, not state. See the next decision for its form.
  - So the rename `camera` → `cameraState` is dropped. This replaces the part of the 2026-10-29 decision that keeps
    the flags in a state object; the entity reads of `rotating` stay, through the lookup.
- 2026-10-30 (owner asked for research; recommendation, accepted with these notes): the focus request is a method
  on the focus behavior, not an event. Reasons (scan on 2026-10-30):
  - Timing. The writers run inside `engine.update` (behaviors, contacts, the `Megaman` direction setter, the
    teleport callbacks). The reader, `cameraManagerForRooms.update` with `shouldInterpolate`
    (`MegaLevelScreen.kt:281,984`), runs in the same frame, after `engine.update`. Events are buffered until
    `events.run()`, after the render. An event moves the start of the interpolation one frame later, so the camera
    snaps for one frame where it moves smoothly now. That is a gameplay change.
  - Two operations, not one. Writers set the flag to `true` (start a smooth follow: `Megaman.kt:211`,
    `ClimbBehavior.kt:142`, `PipePortal.kt:150`) and to `false` (stop it before the camera reaches the focus:
    `Megaman.kt:504,508`, `ClimbBehavior.kt:215`, `PipePortal.kt:128`). The screen also clears it
    (`MegaLevelScreen.kt:489,604,993,1139`). The behavior exposes both operations; the screen-side clears move into
    the behavior (the 2026-10-29 decision).
  - The read-only camera interface does not carry these methods. The entity-facing type of the focus behavior,
    reached through the typed lookup, does.
- 2026-10-30 (owner): keep the frustum test of `overlaps` in 051. A change to a rectangle test is a behavior change
  and needs its own contract check.
- 2026-10-30 (owner): build a per-frame bounds cache behind the interface only if a `--diagnostics` run shows a gain.
  Measure in a room where the camera is not UP: with direction UP, `getRotatedBounds()` returns before the rotation
  math (`GameRectangle.kt:125`). Each call takes a pooled rectangle (`RotatableCamera.kt:71`).

## Contract notes

- `MegaLevelScreen.start()` sets `rotating = false` at level start (`MegaLevelScreen.kt:441`) and does not change
  `RotatableCamera.rotFinished`. When `rotating` becomes a value of the rotation behavior, check that the value at
  level start is the same as now.
- The `focusSnappedAway` reads and writes must stay in the same frames as now (see the focus request decision).

## Open questions

- Reuse the focus logic, make each behavior first-class, or express each behavior as bounds changes (the X6 way)?
- One camera class or several small parts?
- Which room-transition side effects belong to the camera, and which to a room or level system?
- The name of the camera property that replaces `getGameCamera()`, on the holder and on the host. `GameCameraState`
  goes away, so the name `camera` becomes free. Name collisions to check: the field `gameCamera` in `MegaLevelScreen`
  and the local `val gameCamera` at `MegamanMaverickGame.kt:314`.
- The name of the read-only camera interface in `engine`, and which members it has. Members with a caller now:
  `direction`, `getRotatedBounds`, `toGameRectangle`, `overlaps`. `direction` belongs to rotation, so it can move to
  the rotation behavior instead.
- The focus request is for the one player now (`player.focus`). With more players, which player's focus does the
  request change? Item `029` (player contract) is related.

## Sources

- `NOTES.md` in this folder.
