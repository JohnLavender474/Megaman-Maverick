# Notes: mega-refactor-038b-level-reset

## Decisions

- D1 (owner, 2026-11-06, brief): remove `PLAYER_SPAWN`. `LEVEL_RESET` takes its job; `047` adds a spawn event for each slot when a listener needs one.

## Overlap with other branches

- `081b` (`mega-refactor-081b-x-run-jump-shoot`): its X player will call `playerSpawnEnd()`. The function loses the submit, so X gets the change through it. At the scan, `081b` names no `PLAYER_SPAWN`. Check again before the merge.
