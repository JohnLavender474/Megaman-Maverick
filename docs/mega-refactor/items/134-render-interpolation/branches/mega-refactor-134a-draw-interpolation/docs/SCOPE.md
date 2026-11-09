# Scope: mega-refactor-134a-draw-interpolation

- Status: approved
- Approved: 2026-11-09 (owner, at the brief)

## Goal

Draw each body's sprites, and the game camera, between the positions of the last 2 world cycles, by the leftover of
the step accumulator. The physics, collisions, timers, spawns, culling and the gameplay camera keep the stepped
positions. All of it is behind the launch flag `--interpolation` (default `false`), so with the default nothing
changes.

## Item

From `ITEM.md` (`134`), owner decisions of 2026-11-07: render interpolation (D1); merge directly into `master`; no
vsync change (D2). This branch takes the `134a` part of the proposed scope:

> `134a` draw interpolation: the engine step data, the sprite offset, the draw camera, backgrounds, the flag (default
> off), engine tests, `--diagnostics` before and after. The camera and the sprites go together, because with only one
> of them the player shakes against the view.

`134b` (the attachment links, the explicit snaps, the direct drawables, the flag on) is not in this branch.

## Current state

The scan of 2026-11-07 on `cba150aec` confirms `NOTES.md` (made on `63bc3d3d0`). Only `MegaLevelScreen.kt` (+7) and
`CameraViewEdges.kt` (+18) changed on the draw path since then. Paths: `E` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`C` = `core/src/main/kotlin/com/megaman/maverick/game/`.

- `C/state/Performance.kt:3-8`: every tier has `fixedStep * fps = 0.4`, so 2.5 cycles per frame on average.
- `E/world/WorldSystem.kt:100-148`: `process` adds `delta` to `accumulator` and runs whole cycles. The loop runs while
  `accumulator >= fixedStep` and takes off `fixedStep / fixedStepScalar` per cycle, so the leftover is always in
  `[0, fixedStep)`. So `alpha = accumulator / fixedStep` is correct with any `--fixedStepScalar`. At `maxIterations`
  (`:130-133`) the leftover is set to 0; then `alpha` must be 1 (draw the newest positions), not 0.
- `E/world/WorldSystem.kt:164-206` `cycle`: the `preProcess` loop (`:166`) and the `postProcess` loop (`:204`) are the
  2 snapshot points. No new loop.
- `WorldSystem.process` builds `bodyArray` only from bodies with `doUpdate()` true (`:111-118`). A body that is not in
  the array keeps an old step move; a cycle serial on the body detects this.
- `E/world/body/Body.kt:101-125`: `preProcess`, `process` (velocity, gravity, clamp, translate), `postProcess`,
  `reset`. `reset` runs at destroy (component reset), so the step state can clear there.
- `E/drawables/sprites/SpritesSystem.kt:14-33`: `component.preProcess(delta)`, then each sprite goes to the collector.
  The draw is later (`MegaLevelScreen.draw`). `GameSprite.draw` (`E/drawables/sprites/GameSprite.kt:60-62`) is the
  one place to apply a draw offset around `super.draw`.
- `E/camera/CameraFollow.kt:50-61` `moveToFocus`: sets the camera to the focus each frame, or a lerp with `smooth`.
  `minX`/`maxX` clamp x. `mode` `X_ONLY` does not follow y; `OFF` does nothing.
- `C/screens/levels/camera/CameraRoomClamp.kt`: clamps the camera to `stage.limits` with the blended view size.
- Focus: `C/entities/megaman/components/FocusComponent.kt` and `MegamanX.defineFocusComponent()`
  (`C/entities/megamanx/MegamanXBody.kt:161`): a point of the player's body bounds. So the focus draw offset is the
  player body's draw offset.
- `C/screens/levels/camera/PlayersFocus.kt:36-70` (co-op): `getFocusPosition()` writes ease and keeper state when it
  runs. A draw path must not call it again. It needs a separate read of the draw offset (decision D2).
- `C/screens/levels/MegaLevelScreen.kt:830-845`: `engine.update`, then `cameraController.update`, then backgrounds
  move by the camera delta. `:847-851` "reduce floating point rounding errors" is a no-op (`* PPM / PPM`).
  `:862-922` `draw`: backgrounds (own cameras), then the game viewport `apply()` (which calls `camera.update()`), the
  `BACKGROUND`, tiles, `PLAYGROUND`, `FOREGROUND` sections, then the UI camera.
- `C/drawables/backgrounds/Background.kt:108-111` `move` by `delta * parallax`; `:133-137` `draw` applies its own
  viewport.
- `C/MegamanMaverickGame.kt:957-998` builds `WorldSystem` and `SpritesSystem`; `MegamanMaverickGameParams`
  (`:134-150`) holds the launch flags; `DesktopLauncher.DesktopAppArgs` parses them.

## In scope

New or changed API, each with its first production caller in this branch:

| API | Where | First caller |
|-----|-------|--------------|
| `Body` step state: `beginStep(serial)`, `endStep()`, cleared in `reset()` | `E/world/body/Body.kt` (and `IBody` if `WorldSystem` needs it through the interface) | `WorldSystem.cycle` |
| `Body.getDrawOffset(alpha, serial, out)`: `(alpha - 1) * step`, or 0 when the serial does not match or the move is above the guard (D1) | `Body.kt` | `SpritesSystem.process`, `FocusComponent` |
| `WorldSystem.alpha`, `WorldSystem.stepSerial` (read-only) | `WorldSystem.kt` | `SpritesSystem`, the draw camera |
| `GameSprite` draw offset (2 floats), applied around `super.draw` and set each frame | `GameSprite.kt` | `SpritesSystem.process` |
| `SpritesSystem` gets an optional world read (alpha, serial, on/off) | `SpritesSystem.kt` | `createGameEngine()` |
| ~~`IFocusable.getFocusDrawOffset`~~ replaced 2026-11-09: `IDrawOffsetSource.getDrawOffset(offsets, out)` (core); `PlayersFocus` implements it (D2), 1 player uses the body of slot 0 | `C/screens/levels/camera/IDrawOffsetSource.kt` | `CameraDrawOffset` |
| 2026-11-09: `IRoomSubject.getSubjectBounds()` (engine), split from `IFocusable` (which keeps only `getFocusPosition()`); `FocusComponent` implements both | `E/camera/IRoomSubject.kt` | `CameraRoomBounds` |
| 2026-11-09: `CameraRoomBounds.subject`; `CameraRoomTransition.start(..., subjectCenter)` (no `follow` parameter) | core, engine | `MegaLevelScreen.setCameraSubject()`, `CameraRoomBounds.scanForNextRoom` |
| `CameraFollow`: records per axis if it put the camera on the focus this frame, with the value before the clamp | `CameraFollow.kt` | the draw camera |
| A draw camera part: per axis `offset = clamp(followed + focusOffset) - settled`, with the stage limits and the blended view; 0 when follow did not set that axis, in `smooth`, in a transition, on an axis that `CameraScroll` owns | new file in `C/screens/levels/camera/` (the clamp is core) | `MegaLevelScreen.draw` |
| `MegaLevelScreen.draw`: add the camera offset before `apply()`, take it off after the draw; backgrounds get `offset * parallax` for their draw | core | itself |
| Launch flag `--interpolation` (default `false`), param field, and the dev toggle of D3 | `DesktopLauncher.kt`, `MegamanMaverickGame.kt` | `createGameEngine()`, `MegaLevelScreen` |
| Test map `RefactorTest134.tmx` and `LevelDefinition.REFACTOR_TEST_134` | assets, `LevelDefinition.kt`, `LevelCompletionScreensTest` | the play checks |

Tests (engine, Kotest): `BodyTest` (step state, offset, serial, guard, reset), `WorldSystemTest` (alpha in 0..1,
alpha 1 at the cap, serial, world off), an evenness test (a body at constant velocity, step 1/150, frames of 1/60 s:
the drawn move per frame is even), `GameSprite` (offset applied at draw and removed after; no drift over 100 frames).
Core: the draw camera rule (0 at a room edge, 0 in a transition, 0 on a scroll axis, equal to the focus offset in an
open room).

Measure (`O5-R2`): `--diagnostics` of `WorldSystem` and `SpritesSystem` with the flag off and on, in the same room.
The owner runs it (no game launch by Claude); Claude reads the files with the `diagnostics-analyzer` skill.

## Out of scope

- `134b`: `drawOffsetSource` links for bodies that follow another body outside the cycles, explicit snaps at
  teleports and respawns (`PipePortal`, `CapsuleTeleporter`, `PortalHopper`, the `127a` co-op warp), the 3 direct
  drawables (`BeamInView`, `EndLevelEventHandler`, the game's own `addDrawable`), trails, fonts, and the flag default.
- Rounding drawn positions to whole world pixels (`067`, D4 of the item).
- Vsync (owner, 2026-11-07).
- The no-op "rounding" in `MegaLevelScreen.kt:847-851` (a bug candidate).
- Debug shapes: they keep the stepped shapes, which is correct for debugging.
- Moving the core `ConstKeys.PRIOR` per-body position (`BodyComponentCreator.kt:47`) into the engine field. It has
  14 callers in contact rules; it stays as is.

## Affected areas and risks

- Engine: `Body.kt`, `WorldSystem.kt`, `SpritesSystem.kt`, `GameSprite.kt`, `IFocusable.kt`, `CameraFollow.kt`.
- Core: `MegaLevelScreen.kt`, `Background.kt`, `FocusComponent.kt` (state), `PlayersFocus.kt`,
  `MegamanMaverickGame.kt`, one new camera file, `LevelDefinition.kt`; lwjgl3: `DesktopLauncher.kt`.
- With the flag off: the new code must give offset 0 on every path, so the draw is the same as today. Risk: a
  `GameSprite` subclass that overrides `draw` (`DarknessV2.kt:137`) skips the offset; with the flag off this has no
  effect.
- With the flag on: attached bodies (riders outside the cycles, boss parts) can jitter against their parent; this is
  the known `134b` work, not a bug of `134a`.
- With the flag on: a teleport inside a cycle streaks for 1 frame if it is below the guard.
- A sprite that the entity places away from its body (for example a fixed overlay) moves with the body's offset. Small
  (at most one cycle move) and only with the flag on.

## Overlap with other branches

- `127a` (`mega-refactor-127a-warp-spot-solids`): changes the `MegaLevelScreen` constructor wiring of
  `CameraViewEdges` and adds `REFACTOR_TEST_127` to `LevelDefinition` and `LevelCompletionScreensTest`. Text overlap
  only (simple conflicts). Its warp is a teleport outside a cycle: offset at most one cycle move for one frame; the
  explicit snap is `134b`.
- `081e` (`mega-refactor-081e-x-sabre`): no scope yet; it works in `entities/megamanx/`. No overlap expected. If it
  adds sprites that it places away from the body, they get the offset with the flag on.
- `compactor-level` (not a refactor branch): level content only.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` Components, not new inheritance | no effect | Fields on `Body` and `GameSprite`; no new base class. |
| `O1-R3` No new fragile order | no effect | The draw offset is read after the engine update and the camera update, which is the order today. The snapshot is inside the existing cycle loops. |
| `O1-R4` Typed state, not new property keys | helps | Typed fields; no new `ConstKeys`. |
| `O1-R5` Code only with a production caller | no effect | Each API in "In scope" has its caller in this branch. |
| `O2-R3` One fixed tick in all play | waiting | Draw-only; no frame-rate dependent gameplay. With `010`, the snapshot moves to the tick (`NOTES.md` 6.4). |
| `O2-R4` Stable order | no effect | No new iteration. |
| `O2-R5` No logic assumes one player | no effect | The co-op focus has its own rule (D2). |
| `O2-R6` No logic assumes one camera | waiting | One draw offset for the game camera; with `094` it is per view. |
| `O2-R7` Gameplay does not depend on rendering | no effect | Only draw code reads the drawn positions. The review checks that no gameplay code reads them. |
| `O5-R1` No per-frame allocation | no effect | Fields and pooled `Vector2`s only; no lambdas or collections per frame. |
| `O5-R2` Measure hot paths | no effect | `WorldSystem` and `SpritesSystem` measured with the flag off and on. |
| `O6-R1` CI passes | no effect | |
| `O6-R2` Named checks | helps | Engine tests for the step state, alpha and evenness; play checks with A/B. |
| `O9-R1` NES-style pad | no effect | No control change. The dev toggle (D3) is a keyboard dev key, like the screenshot key. |
| `O9-R3` Settings persist | no effect | A launch flag, not an in-game setting. |

## Open questions

None. Answers at the brief (owner, 2026-11-09):

- D1: a distance guard of 1 tile (`ConstVals.PPM` world px) per step. Explicit snaps stay in `134b`.
- D2: the shared co-op view gets the average draw offset of the grouped players; 0 while the ease or a view-edge
  keeper acts.
- D3: the launch flag `--interpolation` sets the start value, and a keyboard dev key switches it live in a dev run.
  The key is `I` (free in the default bindings). The launcher arms it only when the launch gives `--interpolation`,
  as a trigger lambda in `MegamanMaverickGameParams`, like `screenshotTrigger` (`P`). The brief first said "as the
  `--viewEdges` keys"; those do not exist, so the brief was corrected on 2026-11-09.

## Scope change (owner, 2026-11-09, during the diff review)

The owner asked why the draw offset was on `IFocusable`, and if `getFocusBounds` is still needed. Answers:

- The draw offset cannot go into `getFocusPosition()`: that value is a gameplay input (the gameplay camera, zones,
  scroll starts, limit snaps, the shield gems in `MegamanWeaponsHandler`), and the offset depends on frame timing
  (`O2-R7`).
- `getFocusBounds()` is the room subject (`CameraRoomBounds`, the start point of `CameraRoomTransition`). It is a
  different concept from the follow target: in co-op the view follows the group and the rooms follow slot 0.

Decisions: (1) remove the draw offset from `IFocusable` in `134a`; (2) split the room subject from `IFocusable` in
`134a`. Result: each camera part reads only what it needs.

| Input | Type | Reader | Given by |
|---|---|---|---|
| Follow target | `IFocusable.getFocusPosition()` | `CameraFollow`, zones, scroll, limit stage | `PlayersFocus` or slot 0's `FocusComponent` |
| Room subject | `IRoomSubject.getSubjectBounds()` | `CameraRoomBounds` (gives the center to `CameraRoomTransition.start`) | slot 0's `FocusComponent` (`entity.roomSubject`) |
| Draw offset | `IDrawOffsetSource.getDrawOffset(offsets, out)` | `CameraDrawOffset` | `PlayersFocus`, or `PlayerDrawOffset` (the body of slot 0) |

`CameraSubject` (`C/screens/levels/camera/CameraSubject.kt`, owner's choice of 2026-11-09, so that the screen does
not grow) sets all 3 in `apply()`, which `MegaLevelScreen.start` calls at the one place that called `setFocus`
before. No behavior change: the room parts get the same rectangle as before. The test `PlayersFocus` "gives the
bounds of slot 0" moved to `CameraSubjectTest` (slot 0 stays the room subject with 2 players).

## Scope change 2 (owner, 2026-11-09, at the play test)

`--interpolation` is on by default (`DesktopLauncher.DEFAULT_INTERPOLATION = true`). The known `134b` items (attached
bodies, sprite entities with no body, teleports below the guard, the shape renderer) now show in normal play. Each has
a `TODO` at its code. The contract adds play checks in real levels with these entities.

## Scope change 3 (owner, 2026-11-09, Diffity thread `7d2ecee3`)

The owner found `CameraSubject` tied to the player structures, and chose to split it now, with the parts that it needs
moved to `engine` (this overlaps item `130`).

| Part | Now in | Note |
|---|---|---|
| `CameraRoomBounds` | `E/camera/` | The 2 `core` helpers (`getCenter`, `toGameRectangle` on pooled objects) are replaced by scratch fields and the engine `Rectangle.toGameRectangle(out)`. |
| `CameraLimitStage` | `E/camera/` | No change. |
| `CameraDrawOffset`, `IDrawOffsetSource` | `E/camera/` | No change. |
| `CameraSubject` | `E/camera/` | Mechanism only: `set(target, room, draw)`. |
| `PlayersCameraSubject` | `C/screens/levels/camera/` | Policy (from `047`): the view and the draw offset follow the group or slot 0; the rooms follow slot 0. |

The tests of the moved parts stay in `core`, because they also build `core` parts (`CameraRoomClamp`, `CameraRoomScan`,
`CameraZones`). New: `engine` `CameraSubjectTest` (the mechanism); `core` `PlayersCameraSubjectTest` (the policy, the
old `CameraSubjectTest`).
