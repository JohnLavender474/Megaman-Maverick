# Plan: mega-refactor-051d-auto-scroll

- Status: approved
- Approved: 2026-10-31

The work is in `SCOPE.md`, and the checks are in `CONTRACT.md`. The branch starts from `mega-refactor-051-master`, and it
merges back into it.

## New APIs and their first callers

| API | Module | First production caller |
|---|---|---|
| `CameraAxisLimit.setPin` and a view size in `resolve` | `engine` | `CameraScroll`, `CameraLimitStage` |
| `CameraScrollSection` | `core` | `CameraScroll`; the layer builder gives the data |
| `CameraScroll` (a mover and an `ICameraLimitRule`) | `core` | a mover and a rule in `MegaLevelScreen` |
| `direction`, `stop` and `next` in `CameraSectionsLayerBuilder` | `core` | `CameraScrollSection` |
| A kill form of the trailing edge | `core` | `CameraEdgeBlocks` |

`CameraScroll` runs after `CameraFollow` and before `CameraLimitStage`. It keeps the scroll position in the part, so that
a shake does not add to it. A mover that holds the frame stops it during a pan, because `CameraRoomTransition` runs first.
It applies after `CameraZones`, so that it wins on its axis, and a zone still applies on the other axis.

The scroll hold is the `051c` hold, plus a dead player. It first ran during a spawn, and the scope change in `SCOPE.md` replaced
that.

## Steps

### 1. Baseline

- Changes: none.
- Check: after `./gradlew clean`, run `engine:test core:test lwjgl3:jar`. Write the test counts and the unique warnings in
  the process log.
- Done: yes

### 2. Test rooms

- Changes: a subagent adds the rooms to `RefactorTest051.tmx` with the `level-builder` skill, each with a player spawn and
  `type=scroll` objects: `scroll_corridor`, `scroll_turn`, `scroll_wide`, `scroll_wall`, `scroll_gate`, `scroll_gravity`,
  `scroll_rooms`, `scroll_spawn`, `scroll_spawn_past`, `scroll_pit`. `scroll_corridor` has low floor blocks, a block that can
  pin Megaman, and enemies and hazards. Existing rooms do not change, and no new spawn goes in the map's start area.
- Check: a short launch of each room on the baseline code: no exception, and the correct spawn in the log. The builder
  skips the unknown type.
- Done: yes

### 3. The section and a scroll to the right

- Changes: `CameraScrollSection` and the layer builder (`direction`, `speed`). `CameraScroll` starts when the focus enters
  the rectangle, from the camera position, and writes a pin. `setPin` comes back to `CameraAxisLimit`, and `resolve` takes
  the view size again. `CameraLimitStage` applies a pinned axis at once, with no ease. Permanent tests, including a
  controller with no section.
- Check: `core:test`. A short launch of `scroll_corridor`. Commit.
- Done: yes

### 4. All directions

- Changes: the 4 directions, with the other axis on its follow and its limits. Permanent tests, also with a shake.
- Check: `core:test`. Commit.
- Done: yes

### 5. Stop and room rules

- Changes: stop mode `exit`: the pin goes away, and the follow takes control with an ease. The section waits at a room edge,
  and it continues from the camera after a pan if the section overlaps the focus. Permanent tests.
- Check: `core:test`. A short launch of `scroll_wide` and `scroll_rooms`. Commit.
- Done: yes

### 6. Edges

- Changes: the leading and trailing blocks through `CameraEdgeBlocks`, the defaults, and `edge_blocks` as the override. The
  trailing block stays at a room edge, and the leading block turns off there. After the stop, the trailing block and its
  limit stay. A trailing edge in the direction of gravity kills, in place of a block. Permanent tests.
- Check: `core:test`. Commit.
- Done: yes

### 7. Spawn and holds

- Changes: the scroll hold. The scroll runs while a player spawns, and edge blocks and crush stay off until `PLAYER_READY`.
  It waits for a gate, the boss health fill, a camera turn and a dead player. Permanent tests.
- Check: `core:test`. A short launch of `scroll_gate` and `scroll_gravity`. Commit.
- Done: yes

### 8. Respawn

- Changes: a reset clears the scroll state and the kept limits, then derives them: a section counts as done if the camera at
  the spawn is past its stop. A level start clears the sections. Permanent tests.
- Check: `core:test`. A short launch of `scroll_spawn` and `scroll_spawn_past`. Commit.
- Done: yes

### 9. Chains

- Changes, as a separate commit group: `next` as a one-way object reference, stop mode `arrive`, and a section with no `next`
  that overlaps another one starts after the first stops, in Tiled order. Permanent tests.
- Check: `core:test`. A short launch of `scroll_turn`. Commit.
- Done: yes

### 10. Docs

- Changes: root `CLAUDE.md`: `CameraScroll` in the part order, and the `type=scroll` properties.
- Check: read against the code. Commit.
- Done: yes

### 11. Automated checks

- Changes: none.
- Check: after `./gradlew clean`, run `engine:test core:test lwjgl3:jar`. Compare the unique warnings with step 1. Launch each
  room in `CONTRACT.md`, with the spawn proved by the log. Tick the contract boxes only for what exists.
- Done: yes

### 12. Owner play test

- Changes: none.
- Check: the owner runs the manual checks in `CONTRACT.md`, new behavior first. Write the result in the process log.
- Done: yes

### 13. Before the merge

- Changes: set `DRAW_CAMERA_SECTIONS` in `MegaLevelScreen` to `false`, or remove it if the owner agrees.
- Check: `grep -n "DRAW_CAMERA_SECTIONS = true" core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt`
  finds nothing. `engine:test core:test lwjgl3:jar` passes.
- Done: yes
