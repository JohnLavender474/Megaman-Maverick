# Notes: mega-refactor-016e-service-names

## Decisions

- 2026-10-29 (owner, verbatim: "Scope approved, go with your recommendations"): the recommendations of the 7 open
  questions in `SCOPE.md`:
  1. `game` (type `MegaServices`) → `services`.
  2. `eventsMan` → `events`, `assMan` → `assets`, `audioMan` → `audio`, `state` → `gameState`; `levelState`,
     `engine`, `players`, `gravity`, `clock`, `camera`, `settings`, `updatables`, `runQueue`, `groups` keep their
     names.
  3. The host `MegamanMaverickGame` renames the same 4 members in the same mechanical commit; the 6-line pre-step
     renames the local `assets` in `queueAssets()` to `queuedAssets`.
  4. `camera` and `getGameCamera()` keep their names (item `051`).
  5. Locals and parameters of other code that use an old name stay.
  6. Comments and string text stay.
  7. The branch is done now.

## Overlap with other branches
