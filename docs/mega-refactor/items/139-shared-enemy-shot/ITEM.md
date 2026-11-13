# 139: A shared simple enemy shot, starting with `JunkShot`

- Status: active
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `081`
- Branches: `mega-refactor-139a-shared-enemy-shot`

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

## Decisions

- (owner, 2026-11-13) Merge target: each branch squash-merges directly into `master`.

## Owner's input

Found in `081e` (2026-11-09), when the "X6" prefix was removed from `X6JunkShot`. A backlog candidate in the `081e`
branch `docs/TODO.md`.

## Status after `139a` (2026-11-13)

`139a` merged: `JunkShot` is `EnemyShot`, and `RollingBotShot` and `FirePellet` are its kinds. The other straight
shots that the `139a` scan found each have a lookup keyed by their class or tag, which a move must change first:
`BunbyRedRocket`, `RocketBomb`, `SharpStar` (the `Asteroid` list), `TeardropBlast` (`BreakableIce`), `PurpleBlast`
(`Met`, `HeliMet`), `LampeonBullet`, `GreenPelletBlast` (`StandardDamageNegotiator`); `SubmarineMissile` adds a shield
fixture and an explosion death. About 12 lobbed shots fit later, because a kind has gravity. The table is in the
`139a` branch `docs/SCOPE.md`.

## Open questions

- Which shots go in the first branch, after `JunkShot`?

## Sources

- `081e` branch `docs/TODO.md` and `process-log.md`.
- The `TODO` at `JunkShot.kt:48`.
