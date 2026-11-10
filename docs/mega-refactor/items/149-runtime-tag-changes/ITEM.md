# 149: Tag changes at runtime, with an index that follows them

- Status: idea
- Type: refactor
- Objectives: `O1`, `O7`
- Depends on: none
- Related: `146`, `021`
- Branches: none yet

## Goal

The owner finds it advisable that an entity can gain or lose tags at runtime (2026-11-10, `146a` D9). `TagsComponent`
has `add` but no `remove`, because no code calls it yet. `MegaGameEntities` files an entity under its tags at spawn
and removes it under its tags at destroy, so a tag that changes while the entity is live leaves the index out of date.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Start only with a real caller.

- `TagsComponent.remove`, and a runtime `add` that keeps the index in sync: the component tells the index, or the
  index keeps the tags it filed for each entity and removes those at destroy.
- `reset()` keeps its current meaning (the tags survive a pooled reuse), unless the caller needs the class tags back.

## Owner's input

2026-11-10 (`146a` Diffity round): "It's okay and probably even advisable for the component to facilitate
adding/removing at runtime after instantiation".

## Open questions

- Which feature is the first caller?

## Sources

- `146a` `docs/SCOPE.md` (D9) and `docs/TODO.md`.
