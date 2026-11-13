# Contract: mega-refactor-138a-wreck-into-mock-sprite

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, with `SCOPE.md` and before `BRIEF.src.html`. The owner approves the brief, not this file.
Do the checks before the branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-11-13 (owner, at the brief)

## Intended changes

No gameplay change and no visible change.

- `MockSpriteEntity` is renamed to `MapSprite` (D2) and becomes a still map sprite: it draws the region that the map names (`atlas`, `region`) in the
  object rect, at `BACKGROUND` priority 1. Its old mock behavior (the `ElecDevil` grid, the flip each second, the debug
  box) goes away. No map or code uses that behavior today.
- `Wreck` is removed. The X6 intro map uses `MapSprite` for the 2 wrecks.
- A `MapSprite` object with no `atlas`, no `region`, or a region that is not in the atlas throws at spawn, with
  a message that names the map object id, the atlas and the region.

## Must not change

- Each wreck in `X6_INTRO` has the same art, the same place to the pixel, the same size and no flip:
  - object `218` (`Wreck/wreck_a`): Tiled rect 0, 1081, 96 x 72;
  - object `219` (`Wreck/wreck_b`): Tiled rect 1868, 1014, 36 x 28.
- The wrecks draw behind X, the enemies and the blocks' foreground, as before (`BACKGROUND` 1).
- The wrecks have no body and do not touch X or shots.
- All other objects and layers of `X6Intro_16x14_v1.tmx` stay the same (the diff of the map shows only objects `218`
  and `219`).

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes. `entity-tags.txt` has no `Wreck` line; `known-entity-failures.txt` does not
  change.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] New `MapSpriteTest` (`core/src/test/.../entities/decorations/`), through `testServices()` with a stubbed
  atlas:
  - after a spawn with `bounds`, `atlas` and `region`, the sprite has the region, its bounds equal the spawn bounds,
    it is not flipped, and its priority is `BACKGROUND` 1;
  - a second spawn of the same pooled instance with other props takes the new region and bounds;
  - a spawn with a region that is not in the atlas throws, and the message names the region;
  - a spawn with no `region` throws.
- [ ] A check, by script or by hand, that the `MapSprite` rects in the map equal the `TickFrames.place` result
  of the old `Wreck` (the values under "Must not change"). Write the result in the process log.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

Run from the worktree:

- `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-138a-wreck-into-mock-sprite`

Experimental checks:

- `./run-game-alpha.sh --level X6_INTRO --room room1`: at the start, wreck A (the claw) is at the left edge of the
  room, at the same place and size as on `master`, behind X. Walk left into it: X passes behind it.
- `./run-game-alpha.sh --level X6_INTRO --room room1`: walk right about 54 tiles from the start. Wreck B (the small
  wreck) is just before the second checkpoint, at the same place and size as on `master`, behind X and the enemies.

Control checks:

- No room uses `MapSprite` today, so no control room exists for its old behavior. The map diff check under
  "Must not change" covers the rest of the X6 intro map.

`room1` has 2 player spawns (`0` at x 128 and `1` at x 2112). `--room` starts at spawn `0`, so wreck B needs the walk.

## Behaviors to check

None outside `X6_INTRO`. The change has no effect on pause, death, respawn or the save state. After a death in
`room1`, both wrecks still show (the entity has no cull, as before).
