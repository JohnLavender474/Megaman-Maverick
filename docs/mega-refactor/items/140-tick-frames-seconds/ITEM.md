# 140: `TickFrames` as the engine `Animation`, with durations in seconds

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `081`
- Branches: none yet

## Goal

`TickFrames` and `TickFrameDef` (`entities/utils/TickFrames.kt`, 77 lines, renamed from the X6 prefix in `081e`) time
animation frames in 60 Hz ticks, as the X6 data does. The rest of the game uses the engine `Animation`, with frame
durations in seconds. The X6 ports (`D1000`, `D1000Core`, `D1000Bomb`, `Junkroid`, `MetaDridler`, `MetaWheelF`,
`DebrisPiece`, `JunkShot`; `Wreck` went in `138a`, as a still `MapSprite`) use `TickFrames`, so there are two animation systems. Change these entities to the
engine `Animation`, with the tick counts converted to seconds, and remove `TickFrames`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Convert each `TickFrameDef` to frame durations in seconds (ticks / 60).
- Find what `TickFrames` does that `Animation` does not do (for example a frame list with a different duration per
  frame, or a frame event), and use the engine feature that fits.
- Each animation plays at the same speed as before.

## Owner's input

Found in `081e` (2026-11-09), when the "X6" prefix was removed from `X6TickFrames`. A backlog candidate in the `081e`
branch `docs/TODO.md`.

## Open questions

## Sources

- `081e` branch `docs/TODO.md` and `process-log.md`.
- The `TODO` at `TickFrames.kt:12`.
