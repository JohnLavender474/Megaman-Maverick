# Contract: mega-refactor-078a-wily-stage-1-art

- Status: approved
- Approved: 2026-11-18

## Automated checks

- [x] `./gradlew engine:test` passes.
- [x] `./gradlew core:test` passes.
- [x] `./gradlew core:smokeTest` passes (it builds `WilyGenerator` and finds its regions).
- [x] `./gradlew lwjgl3:jar` builds.
- [x] `./gradlew detektMain core:verifyRandomRule` passes.
- [x] The merged map keeps every block object and every non-tile object of `master` (a diff of the object
  layers shows only the background objects, the trigger changes and the generator).
- [x] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

| Level | What to check |
|-------|---------------|
| `WILY_STAGE_1` `room1`, `room2` | One background scene: the fortress on the hills, haze; in the pit the horizon moves down as the view goes up. |
| `room3` to `reactor_man_room` | The floor reads against the wall; the wall animates and stays fixed to the tiles in every room; the crests at the gates (none at gate 266). |
| `room7` | The room1 night sky (hills, haze, no fortress) behind the room; sand pours animate; on the pan into `room8` the sky stays on the `room7` side and the cave wall shows on the cave side. Watch the pan into `room7`: the sky shows 32 px into the pan. |
| `room8` to `boss_room` | Each lit cable window glows in the darkness (a light 1 tile across); each cable cut glows wider (2 tiles across); the cave wall animates and stays fixed to the tiles. |
| `boss_room` | The generator animates, sparks at random from the coil tips, and lights the darkness at its core and at each spark. |

## Behaviors to check

- `INTRO_STAGE` (`IntroStage_v3.tmx`, the only live map with a custom `AnimatedBackground`): its background animates, shows and hides as before.

- Death and respawn in `room2`, `room7` and `boss_room`: the right background shows after the respawn.

## Intended changes

- The 8 enemies of Wily Stage 1 use their `wily` skin there; every other map keeps the default look. `BulbBlaster` no longer fails the smoke test (its unused `frozen` region load is gone).

- New art in every room of Wily Stage 1. No gameplay change.
- A new decoration entity, `WilyGenerator`, in `boss_room`.
- `DarknessV2` lights each tile inside its bounds that has a `light` tile property (`glow`: radius 0.5 tile; `exposed`: radius 1 tile). Only the Wily Stage 1 tileset has the property today.
