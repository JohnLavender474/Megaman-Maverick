# Test debt: mega-refactor-078g-timber-walker

One line per rule of behavior that must stay true. The final pass (when the owner calls the moveset final) turns
each line into a test (`docs/ways-of-working/gameplay.md`).

## Timber Walker

- The intro runs far walk, crouch, jump in (4 sizes), land, turn (only when not facing left), `roar_start`, `roar` (1.2 s), `roar_end` in order; `isReady` is true only after `roar_end`.
- The far walk starts at the `far_start` bottom center; the landing is at home.
- Home is the `right_wall` left edge minus the rear extent (the largest `rightX - BODY_X` of the `idle` frames), on the floor of the spawn bounds.
- The walker faces left in every fight state.
- The far frames and `jump_mid_1` draw in `DrawingSection.BACKGROUND`; every other frame draws in `PLAYGROUND`.
- The landing of the intro and of each hop submits one `SHAKE_CAM` event.
- The AI runs only in `BossPhase.FIGHT`.
- The hull is a `SHIELD` and `DAMAGER` below the cab and behind the cab (the rear top box); the legs are boxes of 30 art px at the hull front and ending at the frame's `rightX`.
- A state change sets the state, the animation key, frame index 0 and the drawn region in one call; the sprite never draws the last frame of the previous play of an animation.
- The sprite and the fixtures place the floor line (`FLOOR_Y`) of each frame at the body bottom; the leg boxes end at the frame's `footY`.
- In `HOP_AIR`, while descending, the frame changes from `hop_air` to `jump` when the floor line is less than the tuck gap (`FLOOR_Y` - the lowest `footY` of `hop_air`, 1.75 tiles) above the floor.
- The cab `DAMAGEABLE` fixture is active only in `MET_STUNNED`; else the cab is a `SHIELD`.
- A player shot on the closed guard adds 1 to the guard count, a `ChargedShot` adds 2; the count stops at 4; a shot of the walker or of another enemy adds nothing.
- The guard counts no hits in the intro, in `GUARD_OPEN`, `MET_STUNNED`, `MET_RECOVER`, `GUARD_CLOSE` and the defeat.
- A counted hit plays a sound and shows `guard_hit` for 0.1 s; else the overlay shows `guard_crack_<min(count, 3)>` while the count is above 0; the overlay draws at the cab box of the current frame and never while the guard is open, opening or closing.
- At 4, the next `IDLE` (or the current `WALK`, or the end of a `TELL`) becomes `GUARD_OPEN`, and the count resets to 0.
- `GUARD_OPEN` -> `MET_STUNNED` (2.0 s) -> `MET_RECOVER` (animation, 0.89 s) -> `GUARD_CLOSE` -> `IDLE`; no attack starts in these states.
- A hit on the Met in `MET_STUNNED` plays `met_hit` once, then `met_stunned` again; the stun timer does not restart.
- Each of `SAW_THRUST_LOW`, `SAW_THRUST_LEVEL`, `SAW_CHOP`, `CLAW_HIGH`, `CLAW_GRAB` starts with its tell (0.5 s) and its sound; `LOG_THROW` and the hop have no tell.
- The same attack never starts 3 times in a row.
- A walk toward the player ends at most 2 tiles from home; the next pick away from home walks back home (after a walk) or hops back home (after a hop).
- The hop target is the player's x, clamped to `minX .. home - 2 tiles` before lift-off; the hop home targets home.
- The hop rise of the floor line is 3 tiles and the air time is 1.6 s (feet 4.75 tiles above the floor at the apex with the tuck).
- The shadow shows at the hop target on the floor in `HOP_CROUCH` and `HOP_AIR` only.
- Only the hop toward the player spawns 2 chip waves at the hull ends, one each way; a chip wave ends at a wall.
- The walker x stays inside `left_wall` right edge + front hull extent and home (walk and hop).
- A player under the hull gets the hop or the log; a player at 2.5 tiles or more above the floor gets the log or `CLAW_HIGH` (within 7.9 + 1 + 2 tiles); a low player under 6 tiles gets the thrusts or the hop, 6 to 8.5 tiles the chop or the hop, 8.5 tiles or more the thrusts or the log.
- `CLAW_GRAB` is a candidate only for a player under 1 tile high and 6.5 to 9 tiles away.
- A grab holds the player at the grip of the current frame, eased at 25 tiles/s between frames, with no gravity, `MOVEMENT` and `BEHAVIORS` locked (`BOSS_GRAB`).
- The throw puts the player at the `grab_throw` grip before the release.
- The held player takes no damage from the walker until the throw; the throw does the walker damage (4) once.
- The held player is released on every exit: the throw, a change to any state but `CLAW_GRAB`/`GRAB_THROW`, the defeat, `onDestroy`, the player's death.
- Below half health (in the fight), every animation with a `damaged_` row uses that row.
- The log is thrown on the `log_throw` frame from the grip point, in a fixed flight time toward the player's center.
- The log breaks on a block or a shield of another entity, and does no damage once broken.
- Player damage: walker contact, saw, claw and throw 4, log 3, chip wave 2 (`PlayerDamageNegotiator`).
- `TimberWalkerFrames` has an anchor row (with `footY` and `rightX`) for every canvas-size frame that an animation uses, and a `damaged_` copy of every row that uses `idle_1`.

Removed in round 2 (no test): `HURT`, `SAW_RECOVER`, the open guard on `stuck_open` of `SAW_CHOP`, "at most 2 attacks between chops", the turn toward a player behind the walker in the fight.
