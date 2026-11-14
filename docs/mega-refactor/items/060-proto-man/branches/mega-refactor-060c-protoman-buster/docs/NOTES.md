# Notes: mega-refactor-060c-protoman-buster

## Decisions

- 2026-11-14 (owner, play test 7): left and right turn Proto Man at any point of a standing (floor) slash; he still
  does not move. The arc and the hit box follow `facing` each frame. The crouch slash still does not turn.

- 2026-11-14 (owner, play test 7): the sabre's half hits are stronger: 3/10/15 → 4/12/20 on standard targets
  (`WoodCrate` 15 → 20), and the X6 enemies take 2/3 of their full slash damage (rounded up) in place of 1/2. Full
  hits (Proto Man's hit 3, all X moves) and the boss values are unchanged. `proto_sabre.ogg` is 2 dB louder (peak
  -3.9 dBFS).

- 2026-11-14 (owner): Proto Man's sabre plays `PROTO_SABRE_SOUND` (`assets/sounds/proto_sabre.ogg`; X keeps
  `WHIP_V2_SOUND`). Source: X6 Shadow Giga slash, X bank 1 id 18, file `m088-e11/vag-17.wav` in
  `~/IdeaProjects/mmx-x6/assets/sounds/`, made 8-bit ("medium": 13 kHz, 8-bit samples, NES-style 4-bit volume steps
  per 60 Hz frame, shorter tail; script `temp/proto-sabre-sfx-x6/build.py` in the main checkout). The owner allows
  this one X6-derived sound in the repo, as an exception to the "no X6 game data" rule of
  `docs/mega-refactor/CLAUDE.md`.

- 2026-11-14 (owner, scope change 5; Claude for the points marked "Claude"):
  - Art: round 35 (`temp/mega-refactor-060c-protoman-buster/sabre/v2/guard/round35/`, "Final set" in its
    `notes.md`): the shield strapped on his back in `stand`, `run_trans`, `run` and `jump`, the first pull-out frame
    (and the last put-away frame), and the new `crouch_shoot`, `crouch_slash`, `crouch_slash_arc`.
  - Claude: `DOWN` released during a crouch attack: he stays crouched to the end of the attack, for both attacks
    (one rule), then stands with no put-away. So a crouch shot holds him in place for the rest of its 0.3 s pose
    (`SHOT_POSE_TIME`), with left and right only turning him, also after the release. A jump (`A` after the release)
    ends the crouch: the crouch slash ends (a state change ends Proto Man's slash), and the shot pose goes on as
    `jump_shoot`.
  - Claude: while the crouch shot pose runs, `B` fires another crouch shot (with the cooldown), also after the
    release, because he is still crouched. `DOWN` + `B` during a standing shot pose fires a crouch shot. `UP` +
    `DOWN` + `B` on the ground is a crouch attack (the crouch wins over the `UP`+`B` slash).
  - Claude: left and right do not turn him during the crouch slash, as during a floor slash (a turn would flip the
    arc in the middle of the swing). During the crouch shot they turn him, as in the guard.
  - Claude: the crouch muzzle offset is the stand offset (`SHOT_OFFSET_X` 1, `SHOT_OFFSET_Y` 0) moved by the art
    difference of the 2 muzzle points (`stand_shoot` (43, 35), `crouch_shoot` (40, 38); 1 art px = 0.0625 tile):
    (0.8125, -0.1875) tiles from the body center. The pellet centre is 0.5625 tiles over the floor (top 0.7375,
    under the crouch top 1.125).
  - Claude: the crouch slash is a new shared `SabreState.CROUCH` with its own move list (1 move, so no chain), and
    `PlayerSabre.start(state = ...)` names the state at the start; Proto Man's state rule keeps `CROUCH` while that
    slash runs on the ground. X never chooses `CROUCH`. The floor stop (`running = false`, side speed 0) applies to
    `CROUCH` too.
  - New API and its caller (both Proto Man): `SabreState.CROUCH`, `PlayerSabre.start(state)`; `BusterOffset`,
    `BusterParams.crouchOffset` (X passes `null`), `PlayerBuster.shoot(status, crouched)`, which throws
    `BusterCrouchOffsetMissingException` for a crouched shot with no offset. No visibility was widened.
  - Test room `shield` (`RefactorTest060.tmx`, room cols 216..239): the spawn `7` moved to col 227 (the middle), the
    right `SniperJoe` (id 58) to col 237 (by the right wall); the left `SniperJoe` (id 62) stays at col 217 on the
    ledge (block 61, cols 217..218, 0.75 tile high). Enemies spawn when their map rectangle enters the camera, and
    the 16-tile view at the spawn is cols 219.5..235.5 (centered on the player, inside the room), so both
    `SniperJoe`s are 1.5 tiles out of the view and neither shoots at the spawn. Each one spawns when he walks about
    2 tiles toward it; the other one is then out of the view. `CameraFollow` puts the view center on the focus x
    (clamped to the room), so the view is centered on him at the spawn. A `SniperJoe` that spawned stays until it
    is culled out of the view, so for a short time after he turns back it can still shoot from off screen.
  - Ledge tiles: the 2 ledge cells are painted with `Tileset1` local id 10 (gid 11), a red block whose art fills art
    rows 8..31 of the cell, so the painted block is 24 px high, the same as the block object. Before, a full 32 px
    tile showed the `SniperJoe` 8 px deep in the paint.
  - Height arithmetic (tiles over Proto Man's floor). A `SniperJoe` bullet is 0.35 high, its centre 0.65 over the
    `SniperJoe`'s floor. With a ledge of height d, the bullet spans d + 0.475 .. d + 0.825. It passes over the
    crouch when d + 0.475 > 1.125 (d > 0.65), and it hits the stand when d + 0.475 < 1.5 (d < 1.025). Whole-tile
    floors give d = 0 or 1 (a dip or a raised floor for Proto Man changes d by whole tiles too); d = 1 leaves
    0.025 tile (0.8 px) of overlap with the stand. So the ledge stays 0.75 tile with the 24 px tile: bullet
    1.225 .. 1.575, 0.1 over the crouch top and 0.275 into the stand body.

- 2026-11-14 (owner, scope change 4): `Protoman.puttingAwayGuard` (private set) is `internal` so that `ProtomanTest`
  reads it, as `guarding` already is. `Protoman.crouched` was approved as `internal` too, but no test reads it, so it
  stays `private`. The `shield` room of `REFACTOR_TEST_060` gets a second, higher `SniperJoe` whose shots pass over
  the crouch.

- 2026-11-12 (owner, in the play test): Proto Man stays full-charge only, with a full time of 0.5 s. The charge
  sound means "a charged shot is ready", as for Megaman and X, so for Proto Man it starts at the full time, with the
  gold frames. `playerChargeComponent` takes a nullable half time; `null` puts the sound at the full time. Megaman and
  X do not change. (A first try, a 0.15 s "cue time" for the sound, was dropped: the sound would promise a charged
  shot while a release still fires a pellet.) A half shot is not in this branch.

- 2026-11-12 (owner, sabre art): the poses: slash 1 (ground, upward) = sheet cells 187-191, slash 2 (ground, downward)
  = 172-176, slash 3 (ground, forward horizontal) = new by hand from 184-186 with standing legs, air = 197-201 (cell
  numbers in `temp/mega-refactor-060a-proto-run-jump/sheet-scan/out/cells.json`; views in
  `temp/mega-refactor-060c-protoman-buster/sabre/rows_a.png`, `rows_b.png`). The arcs are the Tengu Blade crescents
  (row 2), recoloured purple. The v2 restyle (Megaman_v3 physique) comes first, before the sabre code runs in the game.

- 2026-11-12 (owner, art round 1 review): round 1 kept the classic sheet bodies (v3 has no sword poses). Round 2
  redraws by hand the swing frame of each slash in v2 proportions first; the in-between frames follow in round 3.
  Ramp A for the arcs (`#F8F8F8`, `#D8B8F8`, `#9878F8`, `#6844FC`). Slash 3 is a new pose: wind-up with the arm back
  at shoulder height, then a wide horizontal swing with a lunge. The arc draws under the body. Forearms for the
  floating fists, the scarf on slash 3 and the air slash, and own bodies for slash 2 frames 4-5.

- 2026-11-12 (owner, art round 2b review): the shorter arms are good (the fist 4 px wide; slash 1's muscle line
  along the raised arm). Each ground slash gets its own stance (slash 1 rises from a crouch, slash 2 a deep lunge,
  slash 3 leans forward). The arc is a separate sprite and draws OVER the body, as X's does (replaces "under", so the
  shared slash needs no draw-order field). The arcs are redrawn at X's scale (about 2.3x the body width) in flat NES
  style, ramp A. Round 3 does all frames, with per-frame hit boxes from the arc art.

- 2026-11-12 (owner, play test 3): any state change during Proto Man's slash (a landing, a jump, a fall off a
  ledge) ends the slash; `B` then starts the slash of the new state. `SabreParams.endOnStateChange`: Proto Man
  `true`, X `false` (X keeps his phase-matched change). The trail shading gets natural deviation (art round 8).

- 2026-11-12 (owner, play test 4, after X6 research): a landing during Proto Man's air slash before its
  follow-through goes on with the swing on the ground at the same time, with standing legs (`AIR_LAND`, the air
  arc and hit boxes), no new sound and no chain; a landing in the follow-through ends it. As Zero in X6 (animation
  92; sources: `~/IdeaProjects/mmx-demake-tools/docs/players/zero.md` "The Z-Saber",
  `~/mmx-data/x6/re/zero/attacks/pseudocode.md` "Air sabre"). A jump or a ledge during a floor slash still ends it,
  as for Zero. `SabreParams.landingMoves`; X passes none.

- 2026-11-13 (owner, step 24): a new HUD atlas, `Hud` (`TextureAsset.HUD`, frames in `assets/sprites/frames/Hud/`),
  in place of the `Bits` atlas. Item `153` adds the weapon icon of every player to it.

- 2026-11-13 (Claude, steps 20 to 25; scope change 2 does not cover these, each is the simplest option that keeps X
  and Megaman the same):
  - `SHOT_POSE_TIME` stays 0.3 s: it is Megaman's shoot-frame time (`MegamanValues.SHOOT_ANIM_TIME`). It is now only
    the time of the shoot frame, not a lock.
  - A jump no longer ends the shot frame (that was part of the old pose lock, B1); a jump right after a shot shows
    `jump_shoot`.
  - The sabre still does not start during the shot frame (`canStart = !buster.shooting`, unchanged), so `UP`+`B`
    in the 0.3 s after a shot does nothing.
  - `B` during the single `UP`+`B` slash does nothing (no chain and no shot), as X fires no shot during a slash.
  - `PlayerSabre.start(chains = true)`: the default keeps X's call unchanged; Proto Man passes `chains = false`.
  - `PlayerSabreSlash.fullStrength` is the strength of the move that last placed a hit box; a slash that never
    placed a box reads full.
  - The guard is also off out of play; a room transition, a gate opening and a respawn restart the guard timer
    (the same points that end the slash).
  - The shield box is 6 x 10 art px in both poses (stand x 30-35, y 33-42; jump x 31-36, y 29-38). In world units:
    0.375 x 0.625 tiles, 0.5625 tiles (stand) or 0.625 tiles (jump) in front of the body center, 0.125 tiles below
    it in both poses (the jump sprite drop of 0.25 tiles cancels the higher jump box). So it covers a band of
    20 world px at chest height; a shot at head or foot height still hits him.
  - The deflect spark is a `WhiteBurst` at the projectile center. `WhiteBurst` gets an optional `ConstKeys.DURATION`
    spawn prop and destroys itself after it; `Laser` passes none and is unchanged. The spark lives 0.15 s (one cycle
    of its 3 frames). His own shots make no spark.
  - The HUD icon draws at the place of Megaman's weapon bar (`WEAPON_BAR_X`, `STATS_BAR_Y`), 0.5 x 0.5 tiles,
    because Proto Man has no weapon bar.
  - Test map: the `buster` room already has a `SniperJoe` (object 41), which turns to face the player, so
    `RefactorTest060.tmx` does not change.

- 2026-11-13 (owner, review before the play test of steps 20 to 25):
  - `SELECT` does nothing while a slash runs, so the slot cannot change in the middle of a combo.
  - The 4 X6 sabre targets take half of their slash damage (rounded up) from a half-strength hit: `Junkroid` 23 / 12,
    `HoverGunner` 30 / 15, `MetaWheelF` 9 / 5, `SabreBlock` 9 / 5. X's moves are all full, so X does not change.

- 2026-11-13 (Claude, review fixes):
  - A real deflect is seen after the contact pass: the shield receiver records the projectile, its velocity and its
    center; Proto Man's updatables (after the world step) show the spark when the projectile died in that frame, or
    when its velocity turned back on an axis in that frame or the next one (a `Bullet` that follows a trajectory
    takes its new velocity at the next pre-step). The shared `ProjectileRule` does not change. A projectile with no
    shield reaction (for example a `Picket`) shows no spark.
  - The deflect spark is a `WhiteBurst` spawned with `ConstKeys.COLLIDE` `false`: its `EXPLOSION` fixture stays off.
    With no prop the fixture is on, as before, for `Laser`.
  - The guard also drops while frozen, teleporting and during a room transition.
  - The shield offset is set in the body's own frame. The engine turns an attached fixture with the body direction,
    so the first version turned it twice for a sideways or upside-down gravity (found by the new direction test).
  - `PlayerSabreSlash.fullStrength` keeps its default (`true`): `MegamanXSabreTest` builds slashes with no move for
    its damage tests, and those tests stay unchanged. No production path reads it before a hit box is placed.
  - Test map: a new room `shield` (spawn `7`, a lone `SniperJoe` 7 tiles to the right on a flat floor), and a
    `Robbit` (`LARGE`, 30 health, only `BOUNDS`) in the `sabre` room, so that hit 3 (5) shows against hits 1 and 2
    (3).

- 2026-11-13 (Claude, scope change 3, steps 28 to 30):
  - `DOWN` and one-way platforms: `DOWN` does not pass Proto Man through a one-way platform today. His body
    (`playerBody`) passes one-way blocks only while he climbs (`PASS_THROUGH_ONE_WAY`), and falls through only on
    `UP` while he climbs (`FALL_THROUGH_PRESS_UP`); the `DOWN` pass-through is X's own rule (`MegamanX.kt`). So a
    guard on a one-way platform works with no change, and the rule stays: `DOWN` is the guard, and it never drops
    him through. A drop-through, if `060d` wants one, must use another input (for example `DOWN` + `A`, which does
    nothing now). Other `DOWN` readers that also act while he guards: a `PipePortal` that points up takes a player
    who stands on it and holds `DOWN`, and a `GroundSlideBlocker` turns off while any player holds `DOWN`. Both are
    existing rules for every player and stay. Two more: a `SpringBouncer` that points down doubles the bounce while
    a player holds `DOWN` (`SpringBouncer.kt:214`), and a `SpiderWeb` counts `DOWN` as an escape press
    (`SpiderWeb.kt:69`). Both are rules for every player and stay.
  - The guard reads `DOWN` and the breaks live, so a release drops the shield at once, also before the next update.
    A run no longer breaks the guard: on the ground the guard blocks the run (left and right only turn him, and the
    first guard frame stops him); in the air the run buttons work as before, so he keeps his arc and can steer.
  - `B` does nothing while `DOWN` is held and no break holds. During an attack the guard is broken, so `B` still
    chains a combo or fires after a shot.
  - `DOWN` + `A` on the ground: `JumpParams.downBlocksGroundJump` is now `true` (the shared rule that Megaman uses),
    with a `TODO` for the `060d` slide. In the air `A` is unchanged.
  - The pull-out time is 2 frames of 0.05 s on the ground and in the air (round 27 has 2 pull-out frames each). The
    guard keys do not take over the frame time of the plain poses.
  - The shield box comes from the art in one place: `ArtBox` values in `ProtomanValues` (round 28: stand x 30-36,
    y 25-47; jump x 32-38, y 21-47, in 48 px frames). Stand: 14 x 46 world px, 19 px in front of the body center and
    1 px below it. Jump: 14 x 54 world px, 23 px in front and 5 px below. New art changes the 2 values and the
    placement tests.
  - Accepted (review, 2026-11-13): the top of the stand shield is 2 px under the top of his damage box. It follows
    the owner's art box, so a shot that grazes the top of his head can still hurt him.
  - The guard pose and the shield read one state: the guard update stores `guardUp` when the pull-out ends, the body
    pre-step turns the shield on from it, and the pose shows only once a world step ran with the shield on. A landing
    or a jump during the pull-out or the guard keeps the frame time between the air and ground guard keys.
  - The HUD icon draws under the health bar, at `HEALTH_BAR_X`, one bit below `STATS_BAR_Y`.

- 2026-11-13 (owner, scope change 4; Claude for the points marked "Claude"):
  - Art: round 34 (`temp/mega-refactor-060c-protoman-buster/sabre/v2/guard/round34/`, "Final set" in its
    `notes.md`). `guard_stand_start` and `guard_stand` are gone; the ground guard is the crouch.
  - Shield boxes (`ArtBox`, 48 px frames): crouch x 31..37, y 30..47; jump x 32..38, y 22..39 (7x18 art px each,
    14 x 36 world px). Crouch: 21 px in front of the body center, 6 px below it. Jump: 23 px in front, 2 px above
    (with the air sprite drop).
  - Pull-out: 4 frames on the ground, 3 in the air, 0.05 s each (`GUARD_TURN_FRAME_TIME`). Put-away: the same counts
    and time, from the `*_end` strips. `guard_crouch` keeps the `stand` times.
  - Claude: the crouch changes only the `BODY` and `DAMAGEABLE` fixtures, not the physics body. While he guards on
    the ground (`crouched`: the guard is raised and planted, so the pull-out and the hold), both are 18 art px high
    (`GUARD_CROUCH_HEIGHT`, 1.125 tiles: art rows 30..47, so the top is the shield top), with the feet edge of each box where it is when he stands, for each
    gravity direction (`BODY` through its offset, which turns with the body; `DAMAGEABLE` through the same feet point
    as in `playerBody`). Why not the shared crouch support (`BehaviorType.CROUCHING` and `crouchHeight` in
    `playerBody`): it shrinks the physics body, which needs a feet shift per direction (Megaman's `crouchBehavior`
    translates the body), and its crouch damage box is a square of the body width (16 art px), not the 19 px of the
    art. `crouchHeight` stays `BODY_HEIGHT`.
  - Claude: low ceiling. The physics body keeps the stand height, so he can always stand up: he cannot get under a
    ceiling that is lower than his stand, crouched or not. No ceiling test is needed. Blocks and the `HEAD` fixture
    see the stand body while he crouches.
  - Claude: the stand boxes come back at once on the release (also during the put-away) and on a break.
  - Accepted: the shorter `BODY` box while he crouches also changes the other `BODY` contact rules (projectile hit
    body, laser, explosion, force, gravity change, bouncer, enemy feet); an area that reaches only above the crouch
    height misses him while he crouches.
  - Claude: the put-away plays only after a release of a guard whose pose showed (the shield was on in the step
    before the release). A release during the pull-out, also on the step after the pull-out timer ends and before
    the guard pose shows, goes to the plain pose at once, with no put-away. A break (a hit, a stun, a freeze, a teleport, a room
    transition, out of play, an attack) plays none and ends one that plays.
  - Claude: the cut inputs are left and right held (movement), and a new press of `A` or `B`. An `A` held from the
    jump does not cut an air put-away. `B` cuts it also through the break, because the slash or the shot it starts
    breaks the guard.
  - Claude: a landing or a jump during the put-away keeps its timer (set at the release: 4 or 3 frames); the frame
    time carries between `guard_crouch_end` and `guard_jump_end`, as between the other guard key pairs.
  - (The spawn and the right `SniperJoe` moved, and the ledge paint changed, in scope change 5; see above.) The
    `shield` room has 2 `SniperJoe`s. The right one (7 tiles right of the spawn, same floor) shoots with the
    bullet centre 0.65 tiles over the floor (bullet 0.475..0.825), under the crouch top (1.125 tiles), so its shots
    meet the shield. The left one (5 tiles left of the spawn) stands on a 2x0.75 tile ledge, so its bullet centre is
    1.4 tiles over his floor (bullet 1.225..1.575): it clears the crouch top by 0.1 tile and hits the stand body
    (top 1.5 tiles). The ledge is 0.75 tile high because a 1 tile ledge leaves only 0.025 tile of overlap with the
    stand body. The painted tile is a full tile, 8 px higher than the ledge block. `ProtomanTest` also covers the
    head-height shot.

## Overlap with other branches

- `081g` (X on ladders): the ladder rule of scope change 2 (on a ladder `B` is always the buster) is for every
  character. Proto Man has a `TODO` for it at his `B` actuator (`060d`); X's part goes to `081g` (backlog candidate in
  `docs/TODO.md`).

## Steps 2 to 4 (2026-11-11)

- Step 2: the room `buster` in `RefactorTest060.tmx` (24x14 tiles, cols 160-183; map widened from 158 to 184 tiles,
  no old object moved): floor, 2 walls, a 2x2 step, a ceiling block, player spawn `5`, `PicketJoe` and `SniperJoe`
  with no properties (both face the player by themselves). Old rooms unchanged (analyzer before and after).
- Step 3: `players/PlayerCharge.kt`: `playerChargeComponent(player, half, full)`, `MegaPlayer.playerChargeStatus`,
  `MegaPlayer.stopCharging()`. Megaman keeps `chargeStatus` with his weapon gate (`playerChargeStatus` when the
  weapon can charge); X reads `playerChargeStatus`.
- Step 4: `players/PlayerBuster.kt` with `BusterParams` and one `BusterShot` per charge level (speed, wait, pose time,
  cap or none, sound, fetch). A null `halfCharged` makes a half charge fire the uncharged shot (Proto Man, D2). X's
  values are `MEGAMAN_X_BUSTER` in `MegamanXBuster.kt`.
- Change from the scope: the fire rule (press or release) is not a buster parameter. It stays in each class's `B`
  actuator, because X's actuator also drives the sabre, and a shared actuator would have 1 caller (Proto Man).

## Steps 5 to 7 (2026-11-11)

- `ProtoBuster` is a subclass of `Bullet` (now `open`, with an `open` animations builder): `Bullet`'s rules with no
  copy; its look is the region `ProtoBuster` in `Protoman_v2`. `is Bullet` checks match it.
- `ProtoStrike` is its own class (about 130 lines): it moves on its trajectory and explodes on every contact (target,
  kill, block, sand, a shield that is not his own) with a full `ChargedShotExplosion`. Claude's call: it explodes on a
  block too (the owner chose "explodes always"); a subclass of `ChargedShot` would override most of it.
- Step 6 (subagent): 44 lines in 21 files get an entry or a check; 6 lines stay (4 `is Bullet` lines that match
  `ProtoBuster` and are right for a player pellet, 2 lines of commented-out code); 11 lines only fetch an enemy
  `Bullet`. The table: `class-entries.md`. Override maps that lower a `Bullet` or `ChargedShot` value
  (`BigAssMaverickRobot`) get the same values for the new classes. `ProtoShotDamageTest`: the size tables give the
  new classes the `Bullet` and full `ChargedShot` values.
- `players/PlayerRules.kt`: `playerCanAttack` (X's `canAttack`, now shared by X and Proto Man).
- `Protoman`: `PlayerBuster(this, PROTOMAN_BUSTER)` (`ProtomanBuster.kt`), the charge component, the `B` actuator
  (hold charges; release fires), the charge stops (hit, stun, room transition, gate, destroy). 8 buster tests.

## Art and animation keys (2026-11-11)

- Owner choices: charge colours option `2_gold` (outline (0,0,0), dark (216,40,0), light (240,184,56)); the v3 cannon
  on `jump_shoot`; a cleanup round (11 pixel fixes; `run_shoot` r1 shows the shield and the torso as 2 shapes, as fan
  cell `#23`); the v2 jump scarf pasted on `jump_shoot`; the cannon red with a grey band.
- Drafts, rounds and scripts: `temp/mega-refactor-060c-protoman-buster/v2/` (`notes.md`, `round1/`, `round2/`,
  `scripts/compose_shoot.py`, `scripts/build.py`); shots in `temp/.../shots/`.
- `assets/sprites/frames/Protoman_v2/` holds 13 sources; `Protoman_v2.{png,txt}` is packed from them by
  `temp/mega-refactor-060c-protoman-buster/scripts/pack_atlas.py` (2 shelves; each region checked byte-equal to its
  file; the 4 `060b` regions unchanged). `ProtoBuster.png` is the 8x8 pellet in the centre of a 32x32 cell, so that
  `Bullet`'s 2-tile sprite draws it at 2x, as the frames are.
- Keys: `animationKey` = the pose key (the `060b` rules) plus `_shoot` while the pose timer runs, else
  `_charge_full` at a full charge (the step shows `stand_charge_full`). A key change that keeps the base pose keeps
  the animation time (as X). Each charge strip ends with the uncharged first frame (as Megaman).
