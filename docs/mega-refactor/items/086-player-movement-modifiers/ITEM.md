# 086: Player movement modifiers as a component

- Status: idea
- Type: refactor
- Depends on: none
- Related: `025`, `036`, `048`, `051`, `084`
- Branches: none yet

## Goal

One player component holds the movement and gravity multipliers, so that effects outside the player's logic (water, a
gravity zone, a level default, a future scroll boost) change them through one place. Today each writer replaces the
value, and the speed rules are in several places.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Move `movementScalar`, `gravityScalar` and `applyMovementScalarToBullet` from `Megaman` into a component.
- Decide how sources combine, and who removes a source on death and on a room change.
- Include or leave out the top run speed in `getMaxRunSpeed()`.

## Owner's input

2026-10-30 (verbatim): "I think delegating these to a new component is ideal, even if it's not done right now in this
branch."

## Open questions

- When does this start? Under "no code without a caller", a stack for movement needs a second live source. Gravity has
  3 kinds of writer today.
- Does the component own the top speed too? `movementScalar` does not change it.
- Item `048` owns `IScalableGravityEntity`, and `084` may do the player side first (`084` `ITEM.md`). Which goes first?
- Items `025` and `036` plan to move the Moon scalars to level defaults. Should this wait for them?

## Sources

- `NOTES.md` in this folder.
