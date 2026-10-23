# 058: Camera rotation as its own mechanism, separate from gravity

- Status: idea
- Type: refactor
- Depends on: none
- Related: `029`, `051`
- Branches: none yet

## Goal

Drive the camera rotation with its own mechanism instead of the player's gravity direction, so that gravity and camera can be configured separately per entity and per level.

Today every change of a player's direction goes through `WorldGravity.set` (`029d`) and then the player's direction setter, which also starts the camera rotation (`START_GAME_CAM_ROTATION`) and stops the player until it ends. Gravity and camera rotation are therefore always in sync.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A camera rotation source of truth (for example on the camera manager or a level service) that decides when and how the camera rotates.
- Move the rotation request out of `Megaman.direction`'s setter. With more players, each player's setter would ask for the rotation today.
- Let levels and entities choose: gravity change with a camera rotation, gravity change without one, or a camera rotation with no gravity change.
- Keep today's behavior as the default configuration.

## Owner's input

From `029d` (2026-10-23, verbatim):

> This also deserves its own TODO since, ideally, camera rotation is driven by its own mechanism instead of forcing the game gravity and camera rotation to be in sync. I might want to get very whacky with camera/gravity configuration across entities/levels, so having the ability to mix and match as I please would be ideal. This is a TODO item; nothing to change here.

## Open questions

- Who owns the rotation: the camera manager (`CameraManagerForRooms`), a level flow object (`013`), or `WorldGravity` with a separate rotation policy?
- In split screen (`047`), is the rotation per camera?

## Sources

- `NOTES.md` in this folder.
- `items/029-player-contract/branches/mega-refactor-029d-world-players/docs/SCOPE.md`, "Gravity direction".
- `TODO`s at `Megaman.direction`'s setter and at the `BodyGravityChangeRule` registration in `MegaContactRules`.
