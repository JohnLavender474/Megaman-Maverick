# 127: View edge warp: test the warp spot for solids

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `047`
- Related: `047g`, `047j`
- Branches: none yet

## Goal

The `WARP` view edge rule (`047g`) puts the out player 1 tile beside a partner in view, on the side of the view center.
It does not test that spot for solids, so the player can land inside a wall or a platform. A `TODO` in
`CameraViewEdges.kt` marks it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Test the spot for solids; if it is blocked, try the other side, then the partner's own spot.
- Or warp to the partner's last ground position.

## Owner's input

Found by the review before the play test of `047g` (2026-11-06), accepted for later.

2026-11-06, at the `047h` play test with `--room coop_shaft --viewEdges bottom=WARP,delay=1`: "it works good. One
minor thing, I think there's an edge case where a player that's being warped can potentially be warped into a block or
wall. We need to decide at some point how to handle that edge case. In the meantime, it needs to be recorded so that
it's not forgotten and can be addressed later."

Note: `047j` makes `bottom=WARP` the co-op default, so every co-op player can now meet this case in a tall room. Decide
the fallback before or with `047j`.

## Decisions (owner, 2026-11-06)

The owner's idea: "After the warp delay, the warp remains indefinitely delayed until the destination is "safe" to land
at. This means that it's NOT occupied by a solid block or an enemy. If the remaining player dies before the other
player's warp could be completed, then it counts as if both players were dead. I think the warp destination should be
left or right depending on which way the remaining player is facing". A review (see `NOTES.md`) gave these choices:

- Order of tries: the side the partner faces, then the other side, then the partner's own spot (two players do not
  collide). Each spot must be fully inside the view.
- Safe: no solid that blocks the player (the collision handler's block filter test; one-way blocks and slopes do not
  block; `CameraEdgeBlock` does), no `DEATH` fixture, no enemy `DAMAGER` that may hurt the player.
- Wait: while no spot is safe, the warp waits, up to a cap; after the cap, the player lands on the partner's spot.
- A spot in the air: the warp happens only while the partner stands (on the ground, on a ladder or in water).
- "Both count as dead": the case "slot 0 held, slot 1 dies" was a soft lock; its fix goes into `047j` (owner,
  2026-11-06). "Slot 1 held, slot 0 dies" already resets the level.

## Open questions

- The cap length.
- When facing is read: every frame, so the guide shows the live choice (recommended), or at the warp.
- What the waiting player sees: the guide circle in another color at the best candidate spot (recommended).
- Enemies: the `DAMAGER` overlap only (recommended), or also a margin around them.

## Sources

- `NOTES.md` in this folder.
- `047g` branch `docs/TODO.md` and `process-log.md`.
