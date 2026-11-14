# 158: A shared lead-aim helper for bosses

- Status: idea
- Type: refactor
- Objectives: `O1`, `O4`
- Depends on: none
- Related: `077`
- Branches: none yet

## Goal

`BigAssMaverickRobot` aims orbs with `OrbAim` (lead, current, near, wide; `entities/bosses/bigassmaverickrobot/OrbAim.kt`). When a second boss needs the same aim (for example `GutsTank`, from the `077` owner input), move the lead-aim math to a shared helper. Not before a second caller exists.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

## Owner's input

## Open questions

## Sources

- From the `077a` backlog candidates (2026-11-15). The `ArenaPerimeter` reuse research of 2026-11-15 found no second caller for that class either; if one comes, the proposed shape is a `RectPerimeter` in `engine/.../common/shapes/`.
