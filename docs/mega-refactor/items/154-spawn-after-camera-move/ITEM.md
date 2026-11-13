# 154: Spawners test the camera after it moves

- Status: idea
- Type: refactor
- Objectives: `O9`, `O1`
- Depends on: none
- Related: `138`
- Branches: none yet

## Goal

`MegaLevelScreen` updates the spawners with the camera bounds of the frame before, then moves the camera, then draws.
An entity whose spawn rect is its sprite rect (for example a `MapSprite`) can be missing for 1 frame at the place
where it enters the view. Test the spawners after the camera moves, or with a small margin around the view, so that
an entity is in place on the first frame that shows it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Measure which entities show this today (a spawn rect equal to or inside the sprite rect).
- Choose: move the spawner update after the camera update, or keep the order and add a view margin.
- Keep the spawn and cull order of every other entity the same.

## Owner's input

Found by the `138a` review before the play test (2026-11-13): wreck B in the X6 intro lost the 4 px spawn lead of its
old 32 px box. A backlog candidate in the `138a` branch `docs/TODO.md`.

## Open questions

## Sources

- `NOTES.md` in this folder.
- `138a` branch `docs/TODO.md` and `process-log.md`; the `TODO` at `MapSprite`.
