# 131: Room exit: a player past the exit wall outside the shared span

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`
- Related: `047h`
- Branches: none yet

## Goal

`CameraRoomExits` (`047h`) counts a player as at a side exit only when its center is inside the vertical span that the
two rooms share. A player who gets past the exit wall outside that span (for example a jump over the top of the room
into a taller next room) then never counts as at the exit, and the wall keeps it from coming back. The partner waits
until that player drops into the span or dies.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Count a player who is fully past the exit edge as at the exit, wherever it is in the next room.
- Or close the gap above the room top where the next room is taller.

## Owner's input

Found by the review before the play test of `047h` (2026-11-06). Rare: it needs open headroom above the room.

## Open questions

- Which of the two fixes, or both?

## Sources

- `047h` branch `docs/TODO.md` and `process-log.md`.
