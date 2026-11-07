# 134: Draw bodies and the camera between physics steps (render interpolation)

- Status: active
- Type: refactor
- Objectives: `O9`, `O5`
- Depends on: none
- Related: `010` (fixed tick), `067` (PPM 16, whole-pixel rounding), `081` (playable X, where the judder was found)
- Branches: `mega-refactor-134a-draw-interpolation` (active, 2026-11-07)

## Decisions (owner, 2026-11-07, at the item start)

- D1: render interpolation. The physics steps do not change.
- Merge target: directly into `master`. `134a` ships with `--interpolation` off by default.
- D2: vsync is not in `134`. The owner can try `--vsync true`; a default change is its own item if wanted.
- D3, D4 and D5 stay open for the `134a` brief.

## Goal

Motion on screen is even. Today the physics runs in fixed steps of `1/(2.5 x fps)` (`state/Performance.kt`), so at 60
FPS the frames alternate 2 and 3 steps of 1/150 s. A body at a constant speed moves 2.1 px, then 3.2 px, then 2.1 px
per frame (about +-20 %), and the camera follows the player, so the whole level scrolls with the same pulse. The X6
port that `081` compares with runs 1 update per frame and does not judder.

Render interpolation keeps the physics as it is and draws each body, and the camera, between its positions of the last
2 physics steps, by the leftover fraction of the step accumulator. The drawn motion is even at any refresh rate.
Gameplay, collisions, timers and spawns keep the stepped positions; only the drawing changes.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. From the 2026-11-06 scope (`NOTES.md`):

- Engine: each body keeps its position at the start of the last step and the move of that step; `WorldSystem` gives
  `alpha` (the leftover over the step; 1 at the step cap). `SpritesSystem` gives each sprite a draw offset that
  `GameSprite.draw` applies and removes after the draw, so the per-entity sprite code does not change. About 8 engine
  files.
- Camera: a draw-only offset with the room limits of `CameraRoomClamp`; 0 at a room edge, in a transition, on an axis a
  scroll owns, and in a smooth follow. The gameplay camera, edge blocks, culling and spawns do not change.
- Game: the level screen draw, backgrounds, the focus, the launcher flag `--interpolation`. About 6 files.
- Snaps: a distance guard plus explicit snaps at teleports and respawns (D3).
- Audit: bodies that follow another body outside the world steps (riders, boss parts), teleports, and sprites drawn
  outside the sprites system.
- The core per-body `PRIOR` position (`BodyComponentCreator.kt:47`) can move into the engine field.

Proposed branches:

- `134a` draw interpolation: the engine step data, the sprite offset, the draw camera, backgrounds, the flag (default
  off), engine tests, `--diagnostics` before and after. The camera and the sprites go together, because with only one
  of them the player shakes against the view.
- `134b` interpolation audit: the attachment links, the snaps, the direct drawables, the play test, then the flag on.

Not in scope: a whole number of steps per frame, a fixed 1/60 s update (`010` step 3), whole-pixel rounding (`067`),
and vsync as a default (a separate small change, D2).

## Owner's input

- 2026-11-06, at the `081d` play test: "Certain controls don't feel as fluid as they do in `mmx-x6`." The investigation
  found this judder as the strongest cause.
- 2026-11-06: the owner asked for a full scope of render interpolation, then: "Create 134".

## Open questions

- D1, D2: decided 2026-11-07 (see "Decisions").
- D3: the snap rule: a distance guard plus explicit snaps at teleports and respawns (recommended), or explicit snaps
  only.
- D4: rounding to whole world pixels: not in `134`; decide it with `067` (recommended).
- D5: the flag default after the audit: on (recommended) or off.

## Sources

- `NOTES.md` in this folder (the 2026-11-06 scope).
- The `081d` investigation of control fluidity (`temp/081d-investigate/fluidity.md` at the time; not committed).
