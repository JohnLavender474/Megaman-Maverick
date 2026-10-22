# 041: Lifecycle policy per entity, room inferred from geometry

- Status: planned
- Type: refactor
- Depends on: `021`, `022`
- Related: `003`, `017`, `019`, `036`, `037`, `051`
- Branches: none yet

## Goal

Spawn and cull entities by a default policy, so that level designers stop writing `spawn_room` and cull settings by hand (84% of the values are redundant).

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Each `EntityDef` declares a default lifecycle: `ROOM`, `CAMERA`, `LEVEL`, `EVENT` or `ONCE` (spawn rule, cull rule, respawn rule).
- Infer the room from the object center. `spawn_room`, `events`, `cull_*` and `respawnable` in TMX are overrides only.
- A script removes the redundant values (895 of 1072). A snapshot of the resolved lifecycle per object proves no change. List the 141 mismatches for review.
- The validator reports objects outside every room and overrides that do nothing.

## Owner's input

Answer to review item 40 (2026-10-22, verbatim):

> yes, there's a lot of manual work that goes into maintaining the "life cycle" of entities - spawning and culling by room is one of those things. If this can be streamlined and automated, that would relieve a major headache

## Open questions

- Are the 141 objects whose center is outside their named room intended?
- With X-style camera zones (`051`), should the lifecycle key on rooms, zones or both?
- Should a killed enemy respawn when the room is entered again (today: yes)?

## Sources

- `NOTES.md` in this folder.
