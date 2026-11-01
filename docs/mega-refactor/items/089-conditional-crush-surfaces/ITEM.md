# 089: Conditional crush surfaces

- Status: idea
- Type: feature
- Objectives: `O4`
- Depends on: `051`
- Related: `051`
- Branches: none yet

## Goal

Let a `crush_surface` act only under a condition, for example a gravity direction. Do this only when a real level has a surface that
must kill a grounded player in one gravity and not in another.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

A `gravity` property on a `crush_surface` with one or more directions, read as `edge_blocks` is read.

## Owner's input

2026-10-31: "it's worth exploring to see if it's feasible and advisable to make it so that "crush" sensors are active only when certain
conditions are true." The owner then accepted the recommendation of no code change in `051d`.

## Open questions

- If a surface is off, a player can be squeezed between 2 solids. The result in `MegaCollisionHandler` was not traced.

## Sources

- `NOTES.md` in this folder.
