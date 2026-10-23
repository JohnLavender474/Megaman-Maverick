# Process log: mega-refactor-029d-world-players

## Status

- State: planned
- Goal: World entities, contact rules and world body code reach the player through the players service; `HIT_BY_PLAYER` takes the player view
- Done: scan; draft `docs/SCOPE.md`.
- Next: the owner approves `docs/SCOPE.md`.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-23

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `9432e437b`. Draft `docs/SCOPE.md` written.
- The owner answered open questions 1-4 (gravity-device receiver; `teleporting` in `PlayerStateComponent`;
  light-radius receiver; `players.any { it.ready }`), asked that `PushableBlock` and `FlipperPlatform` work for any
  player in contact, and asked to rename `MegamanDeath` to `PlayerDeath` (class and the 29 TMX objects) with a test
  room. Scope updated.
- The owner asked for one source of truth for the gravity direction. Scan of every write to the player's direction
  (on `9432e437b`): all go through `Megaman.direction`'s setter, which sets the body direction, the physics values
  and, when the direction changes, starts the camera rotation (`START_GAME_CAM_ROTATION`) and stops the player
  until it ends. The writers:
  1. `GravitySwitcharoo:191` (live in `MOON_MAN` and `WILY_STAGE_3`) and `GravityCapsule:181` (in no TMX file);
  2. `MoonMan.activateGravityChange` (`MoonMan:524`; skips a dead player);
  3. direct writes, which do happen:
     - `Megaman.onSpawn` sets the direction from the player spawn's `direction` property (5 spawns in
       `MoonMan_16x14_v5.tmx`);
     - `PortalHopper` sets `entity.direction` from `next_entity_direction` (3 hoppers in `MoonMan_16x14_v5.tmx`),
       and could set `entity.body.direction` from `next_body_direction` (no live map uses it; it would skip the
       setter);
     - `CapsuleTeleporter:208` sets the received player's direction from `megaman_direction_on_receive` (1 in
       `WilyStage3_v4.tmx`);
     - `MegaLevelScreen:367` sets it at the end of a room transition from the room property `megaman_direction`
       (2 rooms in `WilyStage3_v4.tmx`).
  So the direct writes exist, but every one of them still goes through the setter and rotates the camera: today
  the player's direction and the camera's direction are the same "world" gravity, also for the direct writes.
- The owner decided the gravity questions: `WorldGravity` on the game; all writers call `gravity.set`; world-direction readers read it; camera rotation stays in the player's setter with a `TODO`; Moon Man sets gravity only when the target player is alive (`TODO` for `047`); all in `029d`. Scope updated.
