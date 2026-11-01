# 104: Remove the dead `LevelSelectScreen`

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `031`, `039`, `092`
- Branches: none yet

## Goal

`LevelSelectScreen` is dead, and `LevelSelectScreenV2` is live. Remove the dead class, so that a change to the menus does not need to read it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `TextureAsset.LEVEL_SELECT_SCREEN` has the same name. Check who reads it before a removal.
- `092` asks if the screens join that item (its open question 6).

## Owner's input

None yet. `031f` found this candidate.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `items/031-input-frames/branches/mega-refactor-031f-menu-input/docs/TODO.md`.
