# Notes: 175 `LevelPlayers`

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-11-20, branch `109b` (`42506ff09`)

- `MegaLevelScreen.kt` is 1131 lines on `109b`. `109b` adds about 12 lines: the `SlotRespawnRule` switch at the
  `SlotRespawn` build, `checkpointSpawnProps()` and `slotRespawnProps()`.
- Parts that stay in the screen today: the `SlotRespawn` build, the `PLAYER_JUST_DIED` branch of `onEvent`
  (`slotRespawn?.onDeath(...) ?: (slot == PlayerSlot(0))`, then `respawn.startAtDeath()`), and the player spawn loop in
  `resetLevelForRespawn()`.
- Overlap: `013` makes `PLAYER_JUST_DIED` a level flow input and owns the level reset; `130` moves the camera stack,
  not the `SlotRespawn` build (its `NOTES.md` lists the camera lines of `resetLevelForRespawn()`).
