# Notes: mega-refactor-084a-player-state

## Decisions

- (2026-10-30, owner) `docs/SCOPE.md` approved with both recommendations:
  - The teleport hooks are constructor parameters of `TeleportableComponent`, not an `ITeleportable` interface on
    `Megaman`. Reason: fewer interfaces on `Megaman`, and the 3 teleporters already read the component.
  - The 4 fields are plain typed fields on `Megaman`, not a component. Reason: no second user yet.

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

Checked again before the merge (2026-10-30, `master` at `201759471`):

- `031d` and `051a` are merged. `git merge master` into this branch had no conflict. The 2 files that both sides
  changed are `M/Megaman.kt` and `C/entities/utils/GameEntityUtils.kt`.
- `mega-refactor-051b-camera-behaviors` (active, with uncommitted early work: a test map, temporary tests and
  `LevelDefinition.kt`; no change yet to `M/Megaman.kt` or `PipePortal.kt`): its `SCOPE.md`, `NOTES.md` and `PLAN.md`
  already name this overlap. Its step 8 takes this branch from `master`, and it stops and tells the owner if
  this branch is not merged by then. The write it replaces is now at `M/Megaman.kt:343`, in the `onEnd` hook.
