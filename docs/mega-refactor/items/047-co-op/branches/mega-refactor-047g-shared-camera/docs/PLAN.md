# Plan: mega-refactor-047g-shared-camera

## Steps

### 1. Test rooms

- Changes: a subagent adds `group_wide`, `group_clamp`, `group_pit` and `group_tall` to `RefactorTest047.tmx` with the
  `level-builder` skill, each with painted `tiles` and a player spawn (see `SCOPE.md`, "In scope", item 4).
- Check: the rooms parse (`level-analyzer`), each has a spawn and tiles. No game launch.
- Done: yes

### 2. `ViewEdgeRules` in `SessionRules`

- Changes: `ViewEdgeRule` enum, `ViewEdgeRules` value (4 rules, `delay`, `damage`), a `viewEdges` field in
  `SessionRules` with the default for each `GameMode`.
- Check: `core:test`.
- Done: yes

### 3. `--viewEdges` dev flag

- Changes: the flag in `DesktopAppArgs`, the parser in `resolveLaunchOptions` (exit code 2 on a bad key or value, or
  with 1 player), the session rules built from the mode plus the override.
- Check: parser tests in `core:test`.
- Done: yes

### 4. Group focus and ease on change

- Changes: `PlayersFocus` (`IFocusable`), set in `MegaLevelScreen.init()` with 2 players; `startSmoothFollow()` when
  the set of live players changes.
- Check: focus tests (mean of live players, slot 0 alone, slot 0 bounds); `lwjgl3:jar`.
- Done: yes

### 5. `CameraViewEdges`: `NONE` and `WALL`

- Changes: the camera effect, added after `CameraEdgeBlocks` with 2 players; off in a transition, a camera turn, or an
  active zone or scroll (`TODO`, phase 5).
- Check: tests: a block at an edge inside the room, none at a room edge, none when off.
- Done: yes

### 6. `WARP` and `KILL`

- Changes: the out timer per player and edge, `delay`, `damage`; warp 1 tile beside the partner.
- Check: timer tests (acts after `delay`, resets when back in view).
- Done: yes

### 7. `BOUNCE`

- Changes: a players-only edge block that throws a player back into the view, with `damage`.
- Check: `core:test`, `core:smokeTest`.
- Done: yes

### 8. Checks and review before the play test

- Changes: none, unless the review finds a fault.
- Check: all automated checks of `CONTRACT.md`, then the review subagents (diff and each manual check). Results in the
  process log.
- Done: yes
