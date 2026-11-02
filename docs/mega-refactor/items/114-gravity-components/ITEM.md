# 114: Gravity from components

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `047`, `048`, `058`
- Branches: none yet

## Goal

An entity's gravity direction and scale come from a component, so that a gravity change needs no type check on the
entity and no test for a player. Today `BodyGravityChangeRule` tests `IScalableGravityEntity` and `IDirectional`, and
it tests for a player to call `WorldGravity.set`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

## Owner's input

2026-11-02 (verbatim, about a cast in the gravity change rule): "Ideally, gravity changing is component-driven and
doesn't require blind and weird type casts like this".

## Open questions

- Does this item move `IScalableGravityEntity`, which `048` owns, or does it wait for `048`?
- How does it fit the `047` open question, "Is gravity one value for the world, or per player?"
- `058` separates the camera rotation from gravity. Which one goes first?

## Sources

- `NOTES.md` in this folder.
- `047b` branch `docs/TODO.md` (backlog candidate, 2026-11-02).
