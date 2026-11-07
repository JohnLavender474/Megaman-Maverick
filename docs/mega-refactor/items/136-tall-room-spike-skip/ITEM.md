# 136: Co-op: a player can skip a spike floor in a tall room by falling below the view

- Status: idea
- Type: feature
- Objectives: `O2`, `O4`
- Depends on: `047`
- Related: `047j`
- Branches: none yet

## Goal

With the co-op default `bottom=WARP` (`047j`), a player who falls fully below the view before a death strip (spikes)
in a tall room is held and warps beside the partner in place of dying, while the partner is about one view or more
above. This follows the rule "out of view = warp", but it lets co-op players skip spike floors. Rooms with such strips
(review of 2026-11-06): `INFERNO_MAN` `room10`, `REACTOR_MAN` `room3`, `GLACIER_MAN` `room8`, `WILY_STAGE_2`
`fork_room` and `path1_room1`, `WILY_STAGE_3` `room3` and `room5` (the most exposure).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Accept it, or a per-room rule (a Tiled property that sets `bottom` to `KILL` or `NONE` in such a room), or a damage on
  the warp in such rooms.

## Owner's input

Accepted as "out of view = warp" for `047j` (review before the play test, 2026-11-06); recorded for later.

## Open questions

- Accept, a per-room override, or a damage?

## Sources

- `047j` branch `docs/TODO.md` and `process-log.md`.
