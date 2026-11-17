# Notes: 169 `LevelState` by lifetime and owner

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-11-17: 3 read-only agents, branch `047i` (base `4bfc07420`)

### Fields by lifetime and owner

| Field | Lifetime | Owner of the concern | Reset sites | Issue |
|---|---|---|---|---|
| `currentLevel` | staged next level (menus) and level in play | `LevelState` | never cleared | 2 roles in 1 field; 35 reader lines in 21 files |
| `roomsSupplier`, `currentRoomSupplier` | level | `CameraRoomBounds` (read through lambdas) | never cleared | no stale copy; fine |
| `worldContainer` | level | `MegaLevelScreen` builds it | never cleared | kept after the level ends |
| `tiledMapLoadResult` | level | `MegaLevelScreen` | never cleared | keeps the last `TiledMap` alive |
| `roomTransition` | room transition | copy of `CameraRoomTransition.transitioning` | `LevelRoomHandler.onEnd` + 2 `MegaLevelScreen` sites | manual clears, because `cancel()` skips `onEnd` |
| `megamanBeam` | respawn beam-in, level-end beam-out | beam steps (`RespawnSteps`, `EndLevelEventHandler`) | only the respawn end | stays `true` after a level end |
| `megamanBeamCenter` | frame | beam steps | never cleared | aliases another object's `Vector2` |
| `coins`, `levelPoints` | level | `LevelState` | level start | shared for both slots |
| `timeSinceLastFloatingPoints`, `lastFloatingPointsType` | level | `AbstractEnemy` | level start resets only the timer | bug: the combo carries over |
| `weaponsAttained` | until the get-weapon screen reads it | screen flow | 3 correct clears | a hand-off, not level state |

### History (from `git log --follow`)

- Created 2026-10-28 by `016a-typed-level-state` (`07a4a3297`): replaced the game property bag (58 calls, 11 internal
  keys). `016` decision 3: session-lived, not per level, because pooled entities outlive a level. `016a` left a
  level-start reset function out of scope.
- `016c` added `weaponsAttained`, `currentRoom`, `getRooms(out)`, `isInExtraLevel`; `051e` added `isInTestLevel`.
- `016` decision 13: camera flags do not go into `LevelState`; the owner on `showEndingScreen`: "It doesn't seem like
  good design to tightly couple a flag for the game's ending screen to the level state."
- No planned item owns a redesign of the whole class. `013` owns the level reset and lists a "room transition" state.

### `047i` outcome

`047i` first added `LevelState.gatheredExitSide`; after the review it moved the value onto the transition
(`CameraRoomTransition.axisOnly`, cleared at the end and by `cancel()`), with no `LevelState` field. That is the
pattern for move 1 of the proposed scope.
