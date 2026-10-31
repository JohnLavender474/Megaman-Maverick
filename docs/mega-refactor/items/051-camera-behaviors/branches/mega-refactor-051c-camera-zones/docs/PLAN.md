# Plan: mega-refactor-051c-camera-zones

- Status: approved
- Approved: 2026-10-30

Scope: `docs/SCOPE.md`. Contract: `docs/CONTRACT.md`. Both approved on 2026-10-30.

## New APIs and their first callers

| API | Module | First production caller |
|---|---|---|
| `CameraController.reset()` | `engine` | `MegaLevelScreen` (3 sites) |
| `CameraLimits` (per axis a range) | `engine` | `CameraLimitStage`, `CameraRoomClamp` |
| `ICameraLimitRule` | `engine` | `CameraZones` |
| `CameraLimitStage` (target, cut, ease) | `core` | a mover in `MegaLevelScreen` |
| `CameraRoomScan` | `core` | a mover in `MegaLevelScreen` |
| `CameraZones`, `CameraZone` | `core` | `CameraLimitStage`; the layer builder gives the data |
| `CameraSectionsLayerBuilder` | `core` | `MegaMapLayerBuilders` |
| `CameraEdgeBlock` | `core` | `CameraEdgeBlocks` |
| `CameraEdgeBlocks` | `core` | an effect in `MegaLevelScreen` |
| `CrusherComponent` | `core` | `GravityBlock`, `CameraEdgeBlock` |

`CameraShake` stays not `Resettable`, so that `CameraController.reset()` cannot start a shake. `PLAYER_SPAWN_INIT` keeps
its own `cameraShake.stop()`.

## Steps

### 1. Baseline

- Changes: none.
- Check: after `./gradlew clean`, run `engine:test core:test lwjgl3:jar`. Write the test counts and the unique warnings
  in the process log.
- Done: yes

### 2. Test rooms

- Changes: a subagent adds the rooms to `RefactorTest051.tmx` with the `level-builder` skill: `zone_overlap`,
  `zone_lock`, `zone_pipe`, `zone_gap`, `zone_pan`, `zone_shake`, `edge_block`, `edge_room`, `edge_crush`,
  `edge_rotated`, each with a player spawn, and the `camera_sections` layer. Existing rooms do not change.
- Check: a short launch of each room on the baseline code: no exception. The builder skips the unknown layer.
- Done: yes

### 3. Temporary suite

- Changes: copy the `051b` suite from `b7fb77bb8` as `Temp051cCameraEquivalenceTest` and `Temp051cFrozenCameraManager`,
  and adapt it to the current constructors. Add the 4 new cases. The first commit of this step keeps the old suite
  safe in the branch.
- Check: the suite passes against the unchanged code. Mutation check: a deliberate defect fails it.
- Done: yes

### 4. Split the room scan

- Changes: `CameraRoomScan` takes the `scanForNextRoom` call from `CameraRoomClamp`, with the same mode test, and runs as
  the mover after the clamp.
- Check: the suite passes. Commit.
- Done: yes

### 5. `CameraController.reset()`

- Changes: it resets each part that is `Resettable`. The 3 sites (`MLS:296, 488, 1011`) call it. Permanent test.
- Check: the suite passes, also a reset in the same frame as a pan. Commit.
- Done: yes

### 6. The limits stage, with rooms only

- Changes: `CameraLimits` and `CameraLimitStage` as a mover after the follow.
  The cut uses `currentGameRoom ?: priorGameRoom`. No stage in `CameraFollowMode.OFF`. `CameraRoomClamp` reads the limits
  and keeps its order, min wins, and the viewport swap. All target changes snap in this step. Permanent tests.
- Check: the suite passes, with all new cases. A mutation of the cut and of the clamp order fails it. A short launch of
  `INTRO_STAGE`. Commit.
- Done: yes

### 7. Ease and hold test

- Changes: the ease with its snap rule (room change, reset, focus out of view). The controller keeps the position after
  the movers, and the ease reads it, not the shaken position. An injected hold predicate: a pan, `WorldSystem` off,
  `rotating`, a dead player. With rooms only, nothing eases, because each target change is a room change. Permanent
  tests with a fake rule.
- Check: the suite passes. Commit.
- Done: yes

### 8. Zones

- Changes: `CameraZone`, `CameraSectionsLayerBuilder` (a key in `ConstKeys`, the entry in `MegaMapLayerBuilders`, the
  level result), `CameraZones` as a rule that folds the active zones in Tiled order per edge. The limit properties are
  object references. A default ease speed in `ConstVals`, with a zone property that overrides it. `MegaLevelScreen`
  gives the zones to the part at level start, and clears them at level end. Permanent tests.
- Check: the suite passes. A short launch of each `zone_*` room. Commit.
- Done: yes

### 9. `CrusherComponent`

- Changes: the component, with the side, the active predicate and the opposite solid, and one kill path. `GravityBlock` moves to it with `instant` on and no opposite solid. World sides map to the player's
  fixtures when gravity is rotated. Permanent tests, with `GravityBlock` cases from before the move.
- Check: all tests pass. Temporary suite `Temp051cGravityBlockCrushTest`, with a frozen copy of the old check, runs the
  old and new logic side by side on seeded states (block position, ground sense, head position, player direction and
  invincibility) and asserts the same kill result. Delete it in step 14. A short launch of `MOON_MAN --room room6`.
  Commit.
- Done: yes

### 10. Edge blocks

- Changes: `CameraEdgeBlock` on `AbstractBlock`: a block filter that skips non-players, added after `super.onSpawn`; no
  culling; no drawing; the opposite-solid `CrusherComponent`. `CameraEdgeBlocks` as an effect spawns the
  blocks that the active zones ask for through `edge_blocks`, places each one at its view edge, and removes it when no
  rule sets that edge or the edge equals the room edge. Blocks are off until `PLAYER_READY`. Permanent tests.
- Check: all tests pass. `EntityConstructorsTest` passes. A short launch of each `edge_*` room. Commit.
- Done: yes

### 11. Docs

- Changes: the root `CLAUDE.md`: the part list, the `camera_sections` layer, and `CrusherComponent`.
- Check: a read of the changed lines. Commit.
- Done: yes

### 12. Automated contract checks

- Changes: none.
- Check: all items under "Automated checks" in `CONTRACT.md`, after `./gradlew clean`, with a warnings comparison.
  Tick them.
- Done: yes

### 13. Owner play test

- Changes: none.
- Check: the owner runs the manual checks in `CONTRACT.md`. Write the result in the process log.
- Done: yes

### 14. Remove the temporary suite

- Changes: delete `Temp051cCameraEquivalenceTest`, `Temp051cFrozenCameraManager` and `Temp051cGravityBlockCrushTest`, in
  the last commit before the merge.
- Check: `./gradlew clean engine:test core:test lwjgl3:jar` passes. Write the counts in the process log.
- Done: yes
