# 139: A shared simple enemy shot, starting with `JunkShot`

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `081`
- Branches: none yet

## Goal

Many enemies have their own one-off shot class in `entities/projectiles/`, for example `JunkShot` (`081e`, 141
lines). Most of these shots only fly in a line, damage a player and go away, with their own art and values. One shared
simple enemy shot, with the art and the values as data, removes the copies, so that a new enemy needs no new shot
class.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- List the one-off enemy shots and what each one does that a straight shot does not do.
- One shared simple shot class that takes the differences as spawn data or as a small typed def. `JunkShot` is the
  first caller.
- Move the other simple shots to it one at a time. Each shot looks, moves and does damage the same as before.

## Owner's input

Found in `081e` (2026-11-09), when the "X6" prefix was removed from `X6JunkShot`. A backlog candidate in the `081e`
branch `docs/TODO.md`.

## Open questions

- Which shots go in the first branch, after `JunkShot`?

## Sources

- `081e` branch `docs/TODO.md` and `process-log.md`.
- The `TODO` at `JunkShot.kt:48`.
