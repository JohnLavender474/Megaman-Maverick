# Notes: 152 Record the last Wily stage as completed at the ending

Research for this item. Give the date and the `master` commit that each finding is based on.

- 2026-11-11, `master` at `91cc4e9da` (from the `151` review): the only callers of `GameState.addLevelCompleted` are
  `MegaLevelScreen` on `END_LEVEL` and `GameState.fromString`. `DrWily` submits `GAME_COMPLETE`, and the level screen
  then opens the ending screen with no write.
