# 117: Crush with a falling pushable block

- Status: idea
- Type: feature
- Objectives: `O4`
- Depends on: none
- Related: `051`, `079`, `089`
- Branches: none yet

## Goal

A `PushableBlock` that falls on an entity can crush it. This gives level design new options, for example a `SniperJoe` that shoots a block to crush Mega Man, and a player who pushes a block onto an enemy.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A crush from a falling `PushableBlock`, for players and for enemies.
- Extend `CrusherComponent` (`entities/state/CrusherComponent.kt`, from `051`). It reads `headFixture`, `feetFixture` and the side fixtures, so it acts only on entities with these fixtures, which today means players. An enemy has its own fixtures.
- Decide which entities a block can crush, with a declaration on the entity, as `SandReactionComponent` does for sand.
- A smarter `SniperJoe` that times its shots to push a block onto Mega Man. This part belongs with `079`, and it can come after the crush itself.

## Owner's input

2026-11-02 (verbatim): "I imagine it would make for interesting level design if for example a `SniperJoe` could shoot a pushable block in an attempt to crush Mega Man. The `SniperJoe` in this case might have "smarter" logic to time his shots correctly."

## Open questions

- Does a crush act only while the block falls, or also when a block slides into an entity against a wall?
- Can a player's shot on a block crush an enemy, and does this count as a kill for drops and points?
- `RODENT_MAN` rooms 9 to 11 have 5 `PushableBlock` objects. A crush must not break these rooms. Read them before the scope.

## Sources

- `NOTES.md` in this folder.
