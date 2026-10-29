---
status: open
verified: question
area: core
gameplay-change: unknown
found-by: owner play-test of mega-refactor-016d-system-services, `REFACTOR_TEST_016` room `gravity_change` (2026-10-29)
related-items: [058]
---

# Question: a `down` gravity area turns the camera upside down, so the level shows upside down

## Where

- `core/.../world/contacts/MegaContactRules.kt:49-58`: for a player, the `BODY`-`GRAVITY_CHANGE` rule calls
  `game.gravity.set(direction)`. The `TODO` at `:49` already says that gravity and camera rotation are forced into sync.
- `core/.../players/WorldGravity.kt:12-15`: `set` writes the direction of each player.
- `core/.../entities/megaman/Megaman.kt:198-230`: the `direction` setter. When the direction changes, it submits
  `START_GAME_CAM_ROTATION` with `camDir` (`DOWN` for `DOWN`), locks movement, sets `gravityOn = false` and zeroes the
  velocity. `:644-647`: `END_GAME_CAM_ROTATION` unlocks movement and sets `gravityOn = true`.
- `core/.../screens/levels/MegaLevelScreen.kt:903-911`: starts the camera and background rotation.
- `core/.../screens/levels/camera/RotatableCamera.kt:101-112` and `engine/.../common/enums/Direction.kt:5`
  (`DOWN(180f)`): the camera turns 180 degrees.
- `core/.../entities/special/GravityChange.kt`: the only spawn properties are `gravity`, `direction` and `cull`. There
  is no property that turns the camera rotation on or off.

## What happens

When Megaman enters a `GravityChange` area with `direction` `down`:

1. The world gravity and Megaman's direction become `DOWN`. Megaman's jump and fall values change sign
   (`Megaman.kt:249-262`), so he falls toward world +y (the ceiling).
2. The game camera and the backgrounds turn 180 degrees. During the turn, Megaman cannot move and has no gravity.
3. After the turn, Megaman falls to the ceiling. With the camera turned, the ceiling is at the bottom of the screen,
   and his sprite (turned 180 degrees, `SpritesComponent.kt:55`) shows upright. The tiles, the enemies and the
   backgrounds show upside down.
4. The left and right buttons stay relative to the screen: with direction `DOWN`, "left" adds +x velocity
   (`ControllerComponent.kt:194`) and "right" adds -x velocity (`:236`). On the turned screen, "left" moves Megaman to
   the screen left. In world terms, the controls are reversed.

`GravitySwitcharoo` (`GravitySwitcharoo.kt:195`), `CapsuleTeleporter`, `PortalHopper` and the room property
`megaman_direction` use the same path (`WorldGravity.set` and the `Megaman.direction` setter). Thus `MOON_MAN` and
`WILY_STAGE_3` gravity switches also turn the camera. The difference: a `GravitySwitcharoo` switches once, with an
animation, a sound and an aura; a `GravityChange` area switches as soon as Megaman's leading point is in it, and it has
no visual or sound.

The owner reports (2026-10-29): "Mega Man appears at the top of the screen even though the camera is oriented 'down'.
This means that the player is playing the game upside down." In code, the turned camera is the designed result of every
gravity change of the player. In a steady state, the code puts Megaman at the bottom of the turned screen, not at the
top. Megaman is at the top of the turned screen only for a short time: during the turn and while he falls to the
ceiling after it. See "Notes" for one rare case that can leave the camera and Megaman out of sync.

## What should happen

Unknown. The owner decides. Options:

1. Keep the current behavior (the camera follows the gravity; the level shows upside down; the controls are relative to
   the screen). No change.
2. Mega Man 5 style for `GravityChange` areas: the camera does not turn; Megaman walks on the ceiling at the top of the
   screen, upside down. Decide if the controls stay as they are on screen or flip as in Mega Man 5.
3. Keep the turn for `GravitySwitcharoo` and the other devices, and make the camera turn a per-area property of
   `GravityChange` (for example `rotate_camera`, default `true` so that the current behavior stays). This is the
   "camera rotation as its own mechanism" idea of item `058`.

## How to reproduce

`./run-game-alpha.sh --debugShapes --level REFACTOR_TEST_016 --room gravity_change` on branch
`mega-refactor-016d-system-services` (the test map is only on that branch). Walk right into the red (`down`) area.

On `master`, `Test1.tmx` has the same setup (`GravityChange` objects 63, 106 and 107: an `up`, a `down` and an `up`
area next to each other), without a camera property.

## Notes

- `016d` did not change this behavior. Its diff on these paths replaces `game` with narrow services
  (`game.gravity` to `gravity`, `game.setFocusSnappedAway` to `game.camera.focusSnappedAway`, `setCameraRotating` to
  `game.camera.rotating`) and adds the debug colors of `GravityChange`. The same code path is on `master`.
- No live level uses a `GravityChange` area with a `direction`: only `Test1.tmx` has them. `MOON_MAN` and
  `WILY_STAGE_3` use `GravitySwitcharoo`.
- Possible out-of-sync case (found by reading, not seen in the game): the setter checks
  `game.getGameCamera().direction != camDir` (`Megaman.kt:214`), but the camera direction changes only when the event
  runs, after the frame. If two areas with different directions both contain Megaman's leading point in the same frame
  (`Rectangle.contains` includes the edges; in `RefactorTest016.tmx` the `down` and `up` areas touch at x = 512), the
  first set submits a rotation, and the second set (back to the old direction) submits nothing. The camera then turns
  while Megaman keeps the old direction. Example: camera `DOWN` with Megaman `UP` on the floor puts Megaman at the top
  of the turned screen, upside down. This stays until Megaman enters a `down` area again. If the owner saw this in a
  steady state, this case is the likely cause, and it is a bug.
