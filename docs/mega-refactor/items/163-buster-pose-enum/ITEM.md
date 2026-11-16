# 163: One shot pose in `PlayerBuster`

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `060`, `081`
- Branches: none yet

## Goal

Since `060m`, `PlayerBuster.shoot` takes 6 pose flags (crouch, climb, slide, wall slide, air dash, swim), and
`BusterParams` has 6 pose offsets, each with its own "offset missing" exception. One pose enum, with a map of offsets
and one exception, gives every pose one shape (`docs/CODE_STYLE.md`) and makes a new pose one entry.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A shot pose enum; `BusterParams` holds an offset per pose; one exception for a missing offset.
- X and Proto Man call it; behavior stays the same.

## Owner's input

None yet. A backlog candidate of `060m` (step 6, 2026-11-16).

## Open questions

## Sources

- `060m` `docs/TODO.md` and `docs/NOTES.md`.
