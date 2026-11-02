# 115: Partial sink and stuck behavior in sand

- Status: idea
- Type: feature
- Objectives: `O4`, `O1`
- Depends on: none
- Related: `048`
- Branches: none yet

## Goal

An entity can sink partway into sand and stop, or be stuck in sand with no sinking. Today `SandReactionComponent` gives one of two modes, `SINK` and `SURFACE`. `fix/quicksand-two-players` adds them.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

Height and movement are separate questions, so these are not new `SandReaction` values. Add one field for each question, with a default, so that no existing declaration changes:

```kotlin
class SandReactionComponent(
    val reaction: SandReaction,
    val maxDepth: Float? = null,
    val stuck: Boolean = false
) : IGameComponent
```

- Partial sink: `SINK` with a `maxDepth`.
- Stuck at the surface: `SURFACE` with `stuck = true`.
- Stuck partway down: `SINK`, a `maxDepth` and `stuck = true`.

Friction stays a separate setting.

## Owner's input

2026-11-02 (verbatim): "It might be the case that certain entities will be stuck standing/resting in the sand but not sink (or sink slightly but not totally). This means the entity is "stuck" but not sinking."

## Open questions

- Which entities need this? Add a field only with a real caller (`O1-R5`).

## Sources

- `NOTES.md` in this folder.
- `docs/issues/2026-11-01-two-players-stuck-in-quicksand.md`.
