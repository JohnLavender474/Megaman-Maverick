# 152: Record the last Wily stage as completed at the ending

- Status: idea
- Type: feature
- Objectives: `O9`
- Depends on: none
- Related: `036` (replays of a completed boss), `007` (save slots), `151`, the issue `2026-11-02-boss-props-have-two-sources.md`
- Branches: none yet

## Goal

`WILY_STAGE_3` is never recorded as completed. The `END_LEVEL` path of `MegaLevelScreen` calls
`GameState.addLevelCompleted`, but the final boss sends `GAME_COMPLETE`, which goes to the ending screen and records
nothing. So a save never says that the game was beaten, and the check `isLevelCompleted(WILY_STAGE_3)` in the level
screen (for a replay of a completed boss) cannot be true. `151a` deleted `allLevelsDefeated` for this reason (0
callers; it could never be true). Decide what the ending should record, and what a save that has beaten the game
unlocks (a replay of the last stage, a "game beaten" mark on the stage select, nothing).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Record `WILY_STAGE_3` (or a "game beaten" flag) on `GAME_COMPLETE`, before the ending screen.
- Decide the replay behavior of the final boss room with the level completed (`MegaLevelScreen` fires the victory
  at once for a completed level).
- A test for the save string with the last stage.

## Owner's input

None yet. The gap was found in the `151` review (2026-11-11), and the owner chose to delete `allLevelsDefeated`
rather than fix the gap in a rename branch.

## Open questions

- Is the ending meant to leave the save as it is, so the player can replay the last stage?

## Sources

- `NOTES.md` in this folder.
- `151` `NOTES.md`, "Edge cases found".
