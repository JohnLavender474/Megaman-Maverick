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

## Open questions

- Allow a backward activation by default? Today it can happen by accident. (Decision G.)
- Should the "CHECKPOINT n" text show the index or a name?
- Fix the ReactorMan and DesertMan same-name "4" pairs (the far-side spawn likely wins today)?

## Sources

- `NOTES.md` in this folder.
