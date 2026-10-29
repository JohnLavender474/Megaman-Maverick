# 077: New attacks and a panic attack for each robot master

- Status: idea
- Type: feature
- Depends on: none
- Related: `015`, `023`
- Branches: none yet

## Goal

Give each robot master one or two new attacks. At least one of them is a "panic" attack: an attack that the robot
master uses only when its health is less than 25%.

This item changes gameplay on purpose. It is not a refactor.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- For each of the 8 robot masters: design the new attacks, the panic attack, and how the boss chooses them.
- A common way to start the panic phase (the health check at 25%), if two or more bosses need the same code.
- One branch per boss or per small group of bosses is probable, because each boss is its own class.

## Owner's input

2026-10-29 (verbatim):

> Each and every single robot master in the game needs one or two more attacks, especially at least one "panic"
> attack (a new attack that the robot master uses only when their health is < 25%)

## Open questions

- Is 25% measured on the boss's maximum health, and does the panic attack also apply in hard mode only, normal mode
  only, or both?
- Does the boss enter the panic phase once (with a signal, for example a flash or a sound), or does it check the
  health before each attack?
- Do the mini bosses and the Wily bosses also get new attacks, or only the 8 robot masters?
- Does this wait for `015` (entity state machine), so that the new attacks use the new state model?

## Sources

- `NOTES.md` in this folder.
