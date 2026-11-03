# 125: Spatial or room index for spawners, if a measure shows a cost

- Status: idea
- Type: refactor
- Objectives: `O5`
- Depends on: none
- Related: `019`, `009`, `040`
- Branches: none yet

## Goal

Test only the spawners near the camera in the spawn phase, instead of all spawners of the level on each frame. Do it
only if a measure shows that the spawner pass costs time on weak hardware.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- First, measure `spawnsMan.update` with `--diagnostics` at `--performance low` or on a weak machine, in a level with
  many spawners. Stop here if the cost is small.
- If the cost shows: put the spawners in a uniform grid (for example one cell per screen) or group them by room, and
  test only the cells near the camera. Keep the edge-trigger rule of `SpawnerForBoundsEntered`: a spawner that leaves
  the tested set must count as "not entered".

## Owner's input

- 2026-11-03 (`019c` scope): "Sure, go for it" (to moving the index out of `019`). The owner asked whether a spawner
  cost would already show. Answer given: a large cost would, but a small one may not, because play tests run on a fast
  laptop and no measure of the spawner pass exists. `019b` removed the largest known cost (the pooled camera
  rectangles).

## Open questions

## Sources

- `NOTES.md` in this folder.
- `items/019-spawn-phase/NOTES.md` (options (2) and (3)).
