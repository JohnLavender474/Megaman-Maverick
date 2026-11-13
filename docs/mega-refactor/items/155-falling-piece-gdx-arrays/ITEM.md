# 155: LibGDX arrays for the falling-piece region keys

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: `060` (branch `060c`), `139`
- Related: `137`
- Branches: none yet

## Goal

`FallingPiece.load`, `FallingPiece.throwDebris`, `WOOD_CRATE_REGIONS` and `BLOCK_REGIONS` use a Kotlin
`Array<String>`, not a LibGDX `Array` (`docs/CODE_STYLE.md`, "LibGDX collections"). `137a` kept them, because the
change touches the debris key lists of 5 owners and `D1000DefeatTest`, which `060c` and `139a` also changed.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Change the 4 places and their callers to LibGDX `Array` (`gdxArrayOf`).
- Remove the `TODO` at `FallingPiece.WOOD_CRATE_REGIONS`.

## Owner's input

A backlog candidate of `137a` (2026-11-13), from its review before the play test.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `137a` branch `docs/TODO.md`; the `TODO` in `FallingPiece.kt`.
