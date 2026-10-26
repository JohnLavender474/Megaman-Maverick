# Notes: mega-refactor-031c-input-pipeline

## Decisions

- 2026-10-26 (owner, before the branch; also in the item `ITEM.md`): `031c` is the parity branch (the devices and the
  bindings follow in `031d` and `031e`). `ControllerButtons` stays as code storage only. The menus and skip screens
  read a thin `IControllerPoller` facade over the slot-0 frame. The edge builder drops `enabled`. The lock moves to the
  edge builder and `TURN_CONTROLLER_*` still switches it. The edge builder goes in the engine, the device adapters in
  core.

## Overlap with other branches

None active.
