# 128: Cull near players, not only by the camera view

- Status: idea
- Type: refactor
- Objectives: `O2`, `O5`
- Depends on: none
- Related: `047`, `094`
- Branches: none yet

## Goal

Blocks and entities cull after 1 s out of the camera view (`getGameCameraCullingLogic`). With 2 players on one
screen, a player who is out of view loses the floor and the world around it. Cull only what is out of the view and
far from every player (the "virtual camera" idea), so a player's surroundings stay alive. Split screen (`094`) needs
the same rule.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- One cull rule that reads the view and the player positions, in the cull helpers, so that most entity files need no
  edit.
- The scale on 2026-11-06: about 2270 blocks and 88 entity files use the camera cull.
- Measure the cost on weak hardware (`O5`).

## Owner's input

Option F3 of the `047g` warp review (play test, 2026-11-06): the real fix for a player out of view, in its own item.
The `047g` page: `docs/mega-refactor/items/047-co-op/branches/mega-refactor-047g-shared-camera/docs/WARP-REVIEW.html`.

## Open questions

- The distance from a player that keeps an entity alive.
- Spawn rules: does a spawner near a player but out of view spawn?

## Sources

- `NOTES.md` in this folder.
- `047g` branch `docs/TODO.md` and `docs/WARP-REVIEW.html`.
