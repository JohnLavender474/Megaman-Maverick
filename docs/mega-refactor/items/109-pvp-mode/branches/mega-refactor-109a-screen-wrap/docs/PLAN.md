# Plan: mega-refactor-109a-screen-wrap

How to do the work in `SCOPE.md`. Write this file after the owner approves the brief. It needs
no approval. When the plan changes, write the reason in the process log.

Rules for every step: `docs/CODE_STYLE.md`, `docs/ways-of-working/git.md`, no game launch, no comments except the
`TODO`s named here. Each new API has its caller in this branch (listed per step).

## Subagent groups

| Group | Steps | Files |
|---|---|---|
| A | 1 | test map, `LevelDefinition`, `LevelCompletionScreensTest` |
| B0 | 2-2b | `WorldSystem`, new `BodyInterpolation`, `SpritesSystem`, `CameraDrawOffset` wiring, `MegamanMaverickGame.kt`, tests |
| B | 3-6 | `engine/world/` (`WorldWrap`, `Body`, `IBody`, `WorldSystem`) and engine tests |
| C | 7 | `engine/drawables/sprites/` and engine tests |
| D | 8-9 | `core` wiring, policy, room checks, guards, `TODO`s |

## Steps

### 1. Test map `REFACTOR_TEST_109`

- Changes: `assets/tiled_maps/tmx/RefactorTest109.tmx` with the `level-builder` skill: one room `arena` in
  `game_rooms`, exactly 16x14 tiles, x offset of at least 1 tile from 0, property `wrap=true` (bool). A floor across
  the full width; 2 platforms that cross the seam (a part at each edge, same height); one platform inside. A
  `player` spawn with `player_2` pointing at a rect in `player_slot_spawns`. Painted `tiles` on every block cell. No
  ladders, sand, gravity changers or one-way blocks at the edges. `LevelDefinition.REFACTOR_TEST_109` (`TEST_LEVEL`,
  `SAVE_GAME_SCREEN`), its line in `LevelCompletionScreensTest`.
- Check: `./gradlew core:test --tests '*LevelCompletionScreensTest*'`; the `level-analyzer` skill shows the room,
  its width and the spawns.
- Done: yes (2026-11-19)

### 2. Baseline measurement

- Changes: none. Run `WorldAllocationMeasureTest` and record the step timing of `WorldSystemTest` (or a short
  engine timing test kept in `temp/`) before any engine change.
- Check: numbers in the process log.
- Done: yes (2026-11-19)

### 2b. Move the draw offsets out of `WorldSystem` (added 2026-11-19, owner)

- Changes: new engine class `BodyInterpolation` in the drawables side (it implements `IDrawOffsets`): it reads
  `stepSerial` and `alpha` from the world (through a small read interface that `WorldSystem` implements, or the
  existing public fields), and holds `interpolation` and the 1-tile guard with its `TODO`. `WorldSystem` drops
  `IDrawOffsets`, `getDrawOffset` and `interpolation`. `MegamanMaverickGame` builds one `BodyInterpolation`, passes
  it to `SpritesSystem` and to the `CameraDrawOffset` construction, and the `I` toggle writes its flag.
  `WorldDrawOffsetsTest` moves to the new class with the same cases.
- Check: `./gradlew engine:test core:test`; grep shows no `getDrawOffset` on `WorldSystem`.
- Done: yes (2026-11-19)

### 3. `WorldWrap` and the body wrap state

- Changes: engine `world/WorldWrap.kt`: `region(out: GameRectangle): GameRectangle?`, `wraps(entity): Boolean`,
  `maxWraps(entity): Int` (lambdas given by the game; an unlimited constant). `Body` / `IBody`: `wrapCount`
  (cleared by `reset()`), and a call that translates the body by `dx` and moves its step start by the same `dx`.
  Callers: `WorldSystem` (step 4).
- Check: engine tests for the step-start move (draw offset after a wrap equals the small step move) and the reset.
- Done: yes (2026-11-19)

### 4. The wrap in the step

- Changes: `WorldSystem` takes `wrap: WorldWrap? = null`. In `process`, with a region, collect the bodies of
  wrapping entities while building `bodyArray` (one filter call per entity per frame). In `cycle()`, after
  `postProcess` and `endStep` (plan change, see the process log): a wrapping body whose center is past an edge, and whose `wrapCount` is under
  its cap, moves by ±W with its step start, and `wrapCount` goes up.
- Check: engine tests: center crossing wraps (both sides); under the edge no wrap; at the cap no wrap; with no
  region no change; a `postProcess` reader sees the unwrapped position.
- Done: yes (2026-11-19)

### 5. Seam contacts

- Changes: `collectContacts` for a wrapping body: for each active fixture (key or not) whose bounding box crosses an
  edge, a second query with a scratch copy of the shape translated by ∓W (one scratch per shape class, made once).
  The contact is `Contact(realFixture, candidate)`. The non-key second query keeps `filterContact`.
- Check: engine tests: a fixture across an edge contacts a fixture on the far side; a contact that is held while
  the body crosses and wraps begins once and does not end; no duplicate contact; a non-key fixture on a wrapping
  body is found across the edge; no allocation per step.
- Done: yes (2026-11-19)

### 6. Seam collisions and the measurement

- Changes: `resolveCollisions` for a wrapping dynamic body whose bounds cross an edge: translate by ∓W, query,
  handle, translate back. `TODO` at `MegaCollisionHandler` friction add (twice on one-way blocks across an edge,
  item 109).
- Check: engine test: a body that stands across an edge on a floor that exists only on the far side stays on it.
  `WorldAllocationMeasureTest` and the timing again; compare with step 2 in the process log.
- Done: yes (2026-11-19)

### 7. Far-half draw

- Changes: `GameSprite`: a draw field for the far-half offset (0 = none); `draw` draws the vertices again at that
  x offset. `SpritesSystem` takes `wrap: WorldWrap? = null`; with a region, for an entity that wraps and has wraps
  left, it sets the field to ±W when the sprite's bounding box (with its draw offset) crosses an edge, else 0.
- Check: engine tests: the field per case (left edge, right edge, inside, no region, entity not accepted, at the
  cap); `draw` calls the batch twice when the field is set.
- Done: yes (2026-11-20)

### 8. Core policy, region and room width

- Changes: `ConstKeys.WRAP = "wrap"`. Core `WrapPolicy`: players (`isPlayer`) and `IProjectileEntity`; cap 1 for
  shots, unlimited for players; `TODO` for the per-instance override, enemies, items, effects (item 109 phase 2)
  and for a weapon cap override. Region supplier: the bounds of `levelState`'s current room when its object has
  `wrap=true`. At level load, each room with `wrap=true` that is not exactly `ConstVals.VIEW_WIDTH` tiles wide
  throws a custom exception (class next to the check, with a test). Wire `WorldWrap` into `WorldSystem` and
  `SpritesSystem` in `MegamanMaverickGame.createGameEngine()`.
- Check: core tests for the policy and the width exception; `./gradlew core:test core:smokeTest`.
- Done: yes (2026-11-20)

### 9. No transitions in a wrap room, and the D2 `TODO`s

- Changes: guards in `CameraRoomScan`, `CameraRoomExits.update` and the `NEXT_ROOM_REQ` handler: no transition
  while the current room has `wrap=true`, each with `TODO` to revisit in item 111. `TODO`s at the rules that read the
  real shape: `FeetLadderRule`, `HeadLadderRule`, `FeetSandRule`, `BodyGravityChangeRule`, `BlockSideRule` end prune,
  `ProjectileRules` hit shapes (item 109, D2).
- Check: core test for a guard where the part can be built with test data; else covered by the review.
- Done: yes (2026-11-20)

### 10. Automated checks and review

- Changes: none.
- Check: all automated checks of the contract; then the review before the play test (Sonnet subagent).
- Done: yes (2026-11-20; play test open)
