# 036: Checkpoints as typed Tiled data: respawn point, triggers, order

- Status: planned
- Type: feature
- Depends on: none
- Related: `003`, `022`, `025`, `037`, `038`, `041`, `046`, `051`
- Branches: none yet

## Goal

Make checkpoints explicit data with a clear activation rule, so that a checkpoint does not have to be in the camera frame when the player passes it.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `Checkpoint` (index, start, facing, room, state) and `CheckpointTrigger` (object reference to the checkpoint; rule `area`, `cross_x`, `cross_y` or `room_enter`), tested against the player body, not the camera.
- Forward-only by index unless `allow_backtrack`. Several triggers per checkpoint replace `duplicate=true`.
- Level-wide defaults in map properties (replace the moon scalars repeated on each MoonMan spawn). The validator checks one start, unique indexes and resolved references.
- Old maps keep working: a rectangle with no trigger is its own activation area.

## Owner's input

Answer to review item 35 (2026-10-22, verbatim):

> yes, and the activation rule is a big one. right now, i have to design levels to make sure that the checkpoint is ALWAYS within frame when Mega passes by it, and this prevents me from being able to implement certain things without fragility

## Decisions

- G (2026-10-22): checkpoints are forward-only by rank. Example: `0`, `1`, `2a`, `2b`, `3`, where `2a` and `2b` are on forked paths that meet at `3`. `2a` and `2b` have the same rank (2). A checkpoint activates when its rank is at least the current rank and it is not the current checkpoint. So a player who goes back from path a to path b activates `2b`, and going back again activates `2a` again. The player never goes back to `1` or `0`.
- G (2026-10-22): this re-activation is the default. Config can add rules to a checkpoint (for example when paths meet but not on the same screen).
- G (2026-10-22): activation rules per checkpoint: `camera` (the default, as today: the checkpoint enters the camera bounds), `bounds` (the player enters given bounds: a child object, or the checkpoint itself when its property points to itself), or a custom trigger.
- G (2026-10-22): this changes one rule on purpose: today each spawn activates only once. The branch `CONTRACT.md` must list it under "Intended changes".

## Open questions

- Should the "CHECKPOINT n" text show the index or a name?
- Fix the ReactorMan and DesertMan same-name "4" pairs (the far-side spawn likely wins today)?

## Sources

- `NOTES.md` in this folder.
