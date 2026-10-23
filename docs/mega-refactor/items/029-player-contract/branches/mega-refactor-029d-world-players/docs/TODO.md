# TODO: mega-refactor-029d-world-players

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Camera rotation with its own mechanism, not driven by the world gravity. Today every gravity change of a player goes through the player's direction setter, which also rotates the camera, so gravity and camera rotation are always in sync. The owner wants to be able to mix and match gravity and camera configuration across entities and levels (2026-10-23). Related: `051` (camera behaviors), `029d` `WorldGravity`. `TODO`s at `Megaman.direction`'s setter and at the `BodyGravityChangeRule` registration.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
