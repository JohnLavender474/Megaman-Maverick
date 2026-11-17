# 167: No per-frame allocations in `AnimatedBackground` and `Background`

- Status: idea
- Type: refactor
- Objectives: `O5`
- Depends on: none
- Related: `076`
- Branches: none yet

## Goal

Two background classes allocate on every frame, against `O5-R1`:

- `AnimatedBackground.update` passes a lambda that captures `delta` to the non-inline `Matrix.forEach`
  (`AnimatedBackground.kt:57`, `Matrix.kt:201`): one lambda object per frame per animated background.
- `Background.update` (`Background.kt:131`) builds a debug string each frame for every background, also when the tag
  is filtered out.

Remove both allocations. Gameplay and the look do not change.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A loop with no captured lambda in `AnimatedBackground.update` (or an inline `forEach`).
- No debug string when the tag is filtered out (or no per-frame debug log).

## Owner's input

None yet. Backlog candidate of `076b` (review before the play test, 2026-11-17); the code was on `master` before
`076b`.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `076b` `docs/TODO.md`.
