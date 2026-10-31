# 051: X-style camera bounds, auto-scroll and other camera behaviors

- Status: active
- Type: feature
- Depends on: none
- Related: `019`, `036`, `038`, `041`, `047`, `058`
- Branches: `mega-refactor-051a-camera-read-interface` (started 2026-10-30, merged into `master` 2026-10-30, squash `7370943a0`); `mega-refactor-051b-camera-behaviors` (started 2026-10-30, merged into `master` 2026-10-30, squash `7fe00b8b8`); `mega-refactor-051c-camera-zones` (started 2026-10-30)

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
- 2026-10-30 (owner): merge target is directly into `master`. Each branch is safe to ship alone: `051a` and `051b`
  are refactors that play the same, and `051c` and `051d` add behavior that a level must turn on.
- 2026-10-30 (owner): the branch split. Each branch `SCOPE.md` decides its final scope, and `051b` may split again.
  1. `051a-camera-read-interface` (refactor): the read-only camera interface in `engine`, implemented by
     `RotatableCamera`; `getGameCamera()` becomes a property on the holder and on the host; the
     `getGameCameraCullingLogic` signatures take the interface; the 2 Darkness sites stop changing the returned
     rectangle. The flags stay.
  2. `051b-camera-behaviors` (refactor): the camera is built from behavior components (room bounds, focus follow
     with the smooth move, rotation, maybe the shaker); `MegaLevelScreen` talks to a camera control interface, not to
     `CameraManagerForRooms`; `GameCameraState` goes. Its scan answers the research questions in `NOTES.md` first.
  3. `051c-camera-zones` (feature): X-style bounds from stage objects, opt-in in Tiled.
  4. `051d-auto-scroll` (feature): opt-in in Tiled.
- 2026-10-30 (owner): the camera is dumb: it knows where it points and how it is rotated. Focus, rooms and bounds are
  rules of a controller with parts that a context can add or omit, so more than one camera can follow the same rules.
  `coerceIntoBounds` leaves the camera. Model: one follow, each behavior a bounds change, plus a position-override part
  for scripted moves (the room pan). Research: `051b` `docs/NOTES.md`.
- 2026-10-30 (owner): new branch `051e-camera-rotation`, with or after `046`: the rotation part, the typed lookup for
  `rotating`, the backgrounds' own rotating cameras and the removal of `GameCameraState`. `051b` removes only
  `focusSnappedAway`. The typed lookup and the `051b` follow part use the names in `051b` `docs/NOTES.md`.
- 2026-10-30 (owner): the view (reading) is separate from the controller (management). `services.gameCamera` is for
  reads; `services.cameraController` gives requests (`051b`).
- 2026-10-30 (owner): `051e` removes `RotatableCamera`. The game camera and the background cameras become plain
  `OrthographicCamera`s, and the rotation behavior stays the same. Design (option C of the 2026-10-30 brainstorm):
  - A `CameraRotation` part holds `direction`, `rotating` and `angle`, and never touches the camera.
  - The controller writes the orientation after the parts update: the part's angle, or 0 if no part exists. So a reset
    or removed part gives the default orientation by construction, with no cleanup hook.
  - A thin `CameraView` holds the plain camera, implements `ICameraView`, and reads the direction from the rotation
    part (`UP` if there is none).
  - A background with `rotatable = false` omits the part.
  - Keep: the direction changes at the start of a turn, not at its end. Reset writes the orientation at once (the
    controller does not update while paused).
  - For the `051e` contract: a reset during a turn sets `rotating = false` at once (today it stays `true` until the
    next level start, and no end callback fires; check the `END_GAME_CAM_ROTATION` listeners). `up` can differ from
    today by a small float amount, so the equivalence check needs a tolerance.
  - Rejected: delta writes with a cleanup hook, absolute writes by the part with a cleanup hook, and a turn applied
    only at render (the frustum test and the map renderer read the camera).
- 2026-10-30 (owner): `051c` and `051d` are scoped together and built one after the other, `051c` first. They change
  the same clamp, and `051c` needs the edge blocks too. No live level uses them; the first users are future extra
  levels. Research: `NOTES.md`, "Camera rules research for `051c` and `051d`".
- 2026-10-30 (owner): camera rules are composable parts. Each frame: room tracking; the transition hold; target limits
  from rooms, then zones, then the scroll; the room cut; the ease; follow; one clamp; the room scan; the effects.
  - A rule sets only the edges it names. Each axis is a range or a pin (a camera center), and a pin beats a range.
  - Active zones fold in Tiled object order, and a later object wins per edge. No stack and no numeric priority. A zone
    is active while the focus point is inside it, so the limits depend only on the position.
  - The rectangle of a zone or a scroll section may overlap rooms, but the view never goes past the current room.
  - Snap when the room changes, on a reset, or when the focus is out of view after the move. Ease in all other cases.
    With no zones this gives today's behavior.
  - One hold test for the ease and the scroll: a pan, the world off, a camera turn, or a dead player. The scroll runs
    while a player spawns.
- 2026-10-30 (owner): a scroll section is a Tiled rectangle. It starts when the focus point enters it and takes control
  from the current camera position, with no jump. Scroll directions are horizontal and vertical. Speed comes from the
  section, and the player's speed does not change.
  - Default stop (`exit`): the camera's trailing edge passes the far edge of the rectangle. The follow then takes
    control again, with an ease.
  - A players-only block sits at each screen edge. After the stop, the trailing block stays permanently, so the player
    cannot go back into the section. A respawn clears it and derives it again from the camera at the spawn.
  - At a room edge the leading block turns off, so the player can reach the next room. The trailing block stays on. A
    room transition has priority and the camera stops at the room edge. The camera position is the scroll position, so
    the scroll continues after the pan if the section still overlaps.
  - Spawn points can be in a section. Rotation is allowed; the scroll direction is a world axis.
  - The owner, verbatim: "the onus of success lies with the designer's choices. We can't validate and prevent every
    single edge case or bad design choice." So there is no level validator.
- 2026-10-30 (owner): a player pinned by a screen edge against a solid dies at once, as in the X games and Super Mario
  Bros. 3. A `CrusherComponent` goes on the entity that crushes, so that any entity can be a crusher, and `GravityBlock`
  moves to it.
- 2026-10-30 (owner): edge blocks are a new class on `AbstractBlock`, so callers do not repeat the flags. A per-section
  Tiled property enables them per edge: on by default for scrolls, off for zones. Tiled walls stay valid.
- 2026-10-30 (owner): chained scrolls are separate sections with an optional `next` property, as `PipePortal` links
  objects. No parent object. A second stop mode, `arrive`, stops when the leading edge reaches the far edge, for corners.
- 2026-10-30 (owner): movement multipliers should move to a player component later, not in `051d`. Item `086` holds this.
  `051d` keeps the player's speed unchanged. If a level later needs a boost, it is a term in `getMaxRunSpeed()`, because
  `movementScalar` scales the run impulse and not the top speed (`ControllerComponent.kt:177-196`).
- 2026-10-30 (owner): test rooms and play checks start from the basics and work up to edge cases. `051c` gets a plain
  `zone_hold` room and a new check order. `051d` starts with `scroll_corridor` and `scroll_turn`.
- 2026-10-31 (owner): zone limits are references to Tiled objects, with no numeric form. A `min_*` property reads the low
  edge of its object, and a `max_*` property reads the high edge. `051c` removes the pin, so `051d` adds the form that its
  scroll needs.

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
