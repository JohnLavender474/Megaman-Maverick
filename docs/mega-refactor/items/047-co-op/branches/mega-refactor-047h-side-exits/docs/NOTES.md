# Notes: mega-refactor-047h-side-exits

## Decisions

## Overlap with other branches

## Overlap with other branches (at the merge, 2026-11-06)

- `081d` (`mega-refactor-081d-x-dash-sabre`) changes `MegaLevelScreen.kt` in the pause input (`selectAction` for X,
  about line 777) and adds 2 imports. `047h` changes `init()`, `buildLevel()` and the imports. Independent hunks; the
  import lines can conflict as a simple conflict when `081d` takes in `master`.
