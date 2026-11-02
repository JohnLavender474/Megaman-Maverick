# Notes: mega-refactor-047a-second-slot

## Decisions

- 2026-11-01, plan: `room16` of `DESERT_MAN` is the room for the level-end checks. It holds the boss `Gate` (object 664)
  and a player spawn (object `10`).
- 2026-11-01, plan: the engine already handles a spawn while a destroy is queued (`GameEngine.kt:90`), so slot 1 can
  respawn through the normal `engine.spawn`.

## Overlap with other branches

Read on 2026-11-01.

- `mega-refactor-004a-entity-smoke-test`: adds a `smokeTest` suite and its data. No overlap in `core/src/main` was seen.
- `compactor-level`: adds entries to `LevelDefinition.kt` and `LevelCompletionScreensTest.kt`. This branch adds one entry
  to each, so a merge can have a small conflict in those 2 files.
- `megaman-x-sprites`: local changes to `CLAUDE.md` and `.github/workflows/ci.yml`, and new sprite files. This branch
  adds one line to `CLAUDE.md`, so a merge can have a small conflict there.
- `019`: `active` in `ITEMS.md`, but no worktree exists. Its proposed scope is the entity spawn queues, not player
  spawn. No overlap now.
