# 138: Merge `Wreck` into `MockSpriteEntity`

- Status: active
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `081`, `137`
- Branches: `mega-refactor-138a-wreck-into-mock-sprite`

## Goal

`Wreck` (`081e`, 71 lines, `entities/decorations/`) draws one still sprite from the map, as `MockSpriteEntity` (99
lines) does. Two classes do one job. Merge them, so that a map uses one entity for a still sprite.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Find what `Wreck` does that `MockSpriteEntity` does not do, and add it to `MockSpriteEntity` as spawn data if a map
  needs it.
- Change the `Wreck` spawners in the maps (the X6 intro map) to `MockSpriteEntity`, and remove `Wreck`.
- Each wreck in the X6 intro stage looks the same as before.

## Decisions

- (owner, 2026-11-13) Merge target: each branch squash-merges directly into `master`.

## Owner's input

Found in `081e` (2026-11-09), when the "X6" prefix was removed from `X6Wreck`. A backlog candidate in the `081e`
branch `docs/TODO.md`.

## Open questions

## Sources

- `081e` branch `docs/TODO.md` and `process-log.md`.
- The `TODO` at `Wreck.kt:28`.
