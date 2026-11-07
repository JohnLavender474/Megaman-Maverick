# 135: A player held below the view is left behind at a side room exit

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`
- Related: `047h`, `047j`, `127`
- Branches: none yet

## Goal

`CameraRoomExits` (`047h`) leaves held players out of "every live player at the exit". So when one player is held
below the view (`WARP`, the co-op default since `047j`) and the partner walks into a side room exit, the transition
starts, `CameraViewEdges` releases the held player where it is (the rules are off during a transition), and that
player stays in the old room. Example: `coop_shaft` into `coop_wide`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Warp the held player beside the partner before the transition starts, or wait for the warp first.
- Or count a held player as "at the exit" and carry it.

## Owner's input

Found by the review before the play test of `047j` (2026-11-06).

## Open questions

- Warp first, or carry the held player with the transition?

## Sources

- `047j` branch `docs/TODO.md` and `process-log.md`.
