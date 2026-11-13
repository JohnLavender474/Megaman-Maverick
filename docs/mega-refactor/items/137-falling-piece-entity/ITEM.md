# 137: One falling-piece entity in place of `DebrisPiece`, `BlockPiece` and `WoodCratePiece`

- Status: done
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `081`
- Branches: `mega-refactor-137a-falling-piece-entity`

## Goal

`DebrisPiece` (`081e`, 134 lines), `BlockPiece` (131 lines) and `WoodCratePiece` (111 lines) in
`entities/decorations/` are the same kind of entity: a small piece that flies out, falls and goes away. Three classes
repeat the same body, sprite and life cycle code. One falling-piece entity, with the art and the values as spawn data,
removes the copies, so that a new kind of piece needs no new class.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Compare the 3 classes and list what is the same and what is different (art, size, speed, gravity, life time, sound).
- One entity class that takes the differences as spawn properties or as a small typed def.
- Move the callers of the 3 classes to the new entity. Each piece looks and moves the same as before.

## Decisions

- (owner, 2026-11-13) Merge target: each branch squash-merges directly into `master`.

## Owner's input

Found in `081e` (2026-11-09), when the "X6" prefix was removed from `X6DebrisPiece`. A backlog candidate in the `081e`
branch `docs/TODO.md`.

## Open questions

- Spawn properties, or one typed def per kind of piece?

## Sources

- `081e` branch `docs/TODO.md` and `process-log.md`.
- The `TODO` at `DebrisPiece.kt:35`.
