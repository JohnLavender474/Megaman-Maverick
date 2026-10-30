# Notes: mega-refactor-084a-player-state

## Decisions

## Overlap with other branches

Checked at branch start (2026-10-30, `master` at `2307c5414`). `M/` =
`core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`.

- `mega-refactor-031d-devices` (active): no overlap. It changes 5 files in `input/` and `controllers/` and their
  tests; its contract (A5, A7) keeps the engine and `MegamanMaverickGame.kt` unchanged. `031f` (blocked) changes the
  menus and skip screens, not the player.
- `mega-refactor-051a-camera-read-interface` (active): one line in `M/Megaman.kt:214`, in the `direction` setter
  (`services.getGameCamera()` becomes `services.gameCamera`). 084 leaves the camera rotation in that setter to `058`.
  Merge 051a first, or merge `master` into this branch after it.
- `051b-camera-behaviors` (not started): it replaces the `focusSnappedAway` writes with a method on the focus
  behavior. Two of the writes are in the teleport hook lambdas (`M/Megaman.kt:504,508`, inside `:492-505`), which
  this branch moves. Move those lines as they are; do not redesign the focus request here. The focus position
  (`M/Megaman.kt:904-918`) stays with `051`. 051b must start from a `master` that has this branch, or plan for
  conflicts in the teleport hooks.
