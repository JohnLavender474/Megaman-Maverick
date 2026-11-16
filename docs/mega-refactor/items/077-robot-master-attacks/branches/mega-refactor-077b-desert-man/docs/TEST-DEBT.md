# Test debt: mega-refactor-077b-desert-man

The fight's spec while the moveset changes, one rule per line in plain words. Each line becomes a test when the owner
says the fight is final (`docs/WAYS_OF_WORKING.md`, "Gameplay iteration").

## Rules to make into tests

- [x] Desert Man has 2 health phases; the bar rolls over at 0 in phase 1; defeat only at 0 in phase 2. (`DesertManDamageTest`)
- [x] Damage on Desert Man: each value is 2 × the `master` boss table (pellet 2, half 2, full 4, charge explosion 2 / 4,
  sabre 2 / 4, `ProtoBuster` 2, Moon Scythe 8, Reactor Shot 6, other specials and explosions 2; `MagmaExplosion` and
  `PreciousShard` 0), so each weapon takes the same number of hits as on `master` over the 2 phases. (`DesertManDamageTest`)
- [x] Every Desert Man damager takes 3 from the player. (`DesertManDamageTest`)
- [x] The punch arm damager is off during the wind-up. (`DesertManPunchTest`)
- [x] During the moving tornado the fist stays forward (`tornado_punch`) and the short-punch damager is on; the
  direction does not follow the player after the spin-up. (`DesertManTornadoTest`)
- [x] The spin-up shows `tornado_start` once (0.2 s), then `tornado` until he moves. (`DesertManTornadoTest`)
- [x] The dance never comes twice in a row; when no guard passes, he jumps. (`DesertManStandTest`)
- [x] The burrow happens only in phase 2; the trail ends at the target x; he erupts after the splash tell. (`DesertManBurrowTest`)
- [x] (no longer applies: the punch rebuild draws one arm sprite from the shoulder) Long punch: extension 1 starts
  where the main sprite's arm ends (no gap), on both sides.
- [x] A `Needle` of another owner still does 3 to the player. (`DesertManDamageTest`)
- [x] The tuning row follows the difficulty and the phase (`DesertManTuning.of`); a running attack keeps the row of
  its start. (`DesertManStandTest`)
- [x] The phase-2 start shakes the camera and plays `QUAKE_SOUND` at once; the `dance_flash` frame shows at the next
  `STAND` for 0.6 s, and the stand timer waits for it. (`DesertManStandTest`)
- [x] (no longer applies: replaced by the punch rebuild lines below) The punch wind-up shows the white-fist frame for
  the row's wind-up time (0.5 / 0.4 / 0.35 s); the facing does not change during the punch.
- [x] The tornado spin-up lasts the row's time (0.5 s; 0.35 s on hard phase 2) with no move; the short-punch damager
  is off during the spin-up and on for the whole moving tornado after it. (`DesertManTornadoTest`)
- [x] In phase 2 the tornado turns once: when the target is more than 1 tile behind him, or at the first wall; the
  next wall ends it. In phase 1 the first wall ends it. (`DesertManTornadoTest`)
- [x] Every attack guard of `STAND` has a roll from the row; the burrow chance is 0 in phase 1. (`DesertManStandTest`)
- [x] From the start of the burrow to the end of the tell, Desert Man does no damage, takes no damage, and is not drawn
  after the sink. (`DesertManBurrowTest`)
- [x] The burrow target x is clamped between the inner edges of `left_wall` and `right_wall` (half a body width in). (`DesertManBurrowTest`)
- [x] The erupt starts at the trail end x and ends in `STAND` on the sand. (`DesertManBurrowTest`)
- [x] The sink shows the `burrow` frames once (4 frames, 0.4 s) at the sink point; the body moves down 1 tile in the
  same time. (`DesertManBurrowTest`)
- [x] From the start of the burrow to the end of the tell, the feet, head and side fixtures are off, block collision
  is off and gravity is off. (`DesertManBurrowTest`)
- [x] The erupt starts with the `erupt` frames once (3 frames, 0.3 s) with no move and the body on the sand at the
  trail end x; the jump up starts only after them. (`DesertManBurrowTest`)
- [x] Erupt jump: the y speed is the larger of the row's erupt impulse and the speed that lifts his center 1 tile above
  the target's center (gravity 22.5 tiles/s2). The x speed brings him to the target x (clamped to the arena) at the
  top of the jump, at most 12 tiles/s either way; 0 when the target is straight above. Air friction is off during
  the jump and back on when the erupt ends. (`DesertManBurrowTest`)
- [x] A `DustPuff` spawns at the sink point at the start of the sink, and at the trail end at the start of the erupt. (`DesertManBurrowTest`)
- [x] In a dark room, the trail and the tell send a light source event at the trail point. (`DesertManBurrowTest`)
- [x] `previousAttackState` is empty after a spawn. (`DesertManStandTest`)
- [x] (no longer applies: see the punch rebuild lines below) Long punch: the hit box ends at the drawn fist
  (`c - 56 - n x 112` px facing left, mirrored facing right).
- [x] Punch: the guard needs the target center within 8 tiles in x and the target bottom within 1.5 tiles in y. (`DesertManPunchTest`)
- [x] Punch: the facing is set at the start of the wind-up and does not change until the punch ends. (`DesertManPunchTest`)
- [x] Punch: he does not move in x during the punch. (`DesertManPunchTest`)
- [x] Punch wind-up: it lasts the row's wind-up time (0.5 / 0.4 / 0.35 s); the fist pulls back 4 art px, flashes
  white and green every 0.06 s, and the body trembles 1 art px; the arm and fist fixtures are off. (`DesertManPunchTest`)
- [x] Punch extend: the reach is 9 tiles from the shoulder (without the wind-up tremble), or less when the inner edge
  of `left_wall` / `right_wall` is nearer; the fist tip never goes past that edge. (`DesertManPunchTest`)
- [x] Punch extend: the arm length eases out (`sineOut`) from the wind-up length to the reach; its time is the
  larger of the distance divided by the row's extend speed (24 / 28 / 28 / 32 tiles/s) and the time that keeps the
  peak speed at 30 tiles/s or less (0.5 tile per frame at 60 FPS). (`DesertManPunchTest`)
- [x] Punch extend: when the reach stops at the wall, a sand splash spawns at the fist tip, the camera shakes 0.2 s
  and `POUND_SOUND` plays, once. (`DesertManPunchTest`)
- [x] Punch hold: the arm stays at full reach for the row's hold time (0.35 / 0.3 / 0.3 / 0.25 s). (`DesertManPunchTest`)
- [x] Punch retract: the arm eases back (`sineIn`) to the wind-up length at 16 tiles/s average, with the same
  30 tiles/s peak cap; then `STAND`. (`DesertManPunchTest`)
- [x] Punch: the arm and fist fixtures are set from the current arm length on each tick (in the punch update and in
  the body pre-process), so the hit boxes match the drawn arm in every frame. (`DesertManPunchTest`)
- [x] Punch: from the end of the wind-up to the end of the retract, the arm fixtures (damager and damageable) span
  from the shoulder to the fist tip, 5 art px high, and the fist fixtures cover the fist (11 x 9 art px); both
  sides mirror. (`DesertManPunchTest`)
- [x] Punch: the arm sprite always joins the shoulder (no gap at any length) and the fist sprite is at the tip. (`DesertManPunchTest`)
- [x] Punch: the speed lines show only in the first 75 % of the extend. (`DesertManPunchTest`)
- [x] Tornado and erupt: the short-punch fixture keeps its size (0.75 x 0.5 tile) after a long punch. (`DesertManTornadoTest`, `DesertManBurrowTest`)
- [x] Needle throw: the `STAND` guard needs the target center 3 tiles or more away in x, the throw not as the last
  attack, and the row's roll (0.5 / 0.6 / 0.6 / 0.7); it is checked after the tornado and before the dance. (`DesertManThrowTest`)
- [x] Needle throw: he does not move in x during the throw. (`DesertManThrowTest`)
- [x] Needle throw wind-up: the first wind-up lasts the row's time (0.6 / 0.5 / 0.45 / 0.4 s), each later one 0.3 s;
  `throw_windup` flashes every 0.06 s, the body trembles 1 art px, the facing follows the target, `REV_SOUND` plays. (`DesertManThrowTest`)
- [x] Needle throw release: the `throw` pose shows for the row's hold time (0.35 / 0.3 / 0.3 / 0.25 s); the
  `throw_burst` frames play once (3 frames, 0.15 s) at the fist; `BURST_SOUND` plays. (`DesertManThrowTest`)
- [x] Needle throw volleys: 1 / 2 / 2 / 3 per throw; then `STAND`. (`DesertManThrowTest`)
- [x] Needle throw pattern: a straight fan (no gravity) from the fist, centered on the angle to the target center,
  with that angle clamped to 60 degrees up or down from the facing; the row's needle count (5 / 5 / 5 / 7), step
  (20 / 18 / 18 / 15 degrees) and speed (10 / 11 / 12 / 12 tiles/s). (`DesertManThrowTest`)
- [x] Needle throw pattern: each even volley (2nd, 4th) has one needle more and no needle on the aim line, so the
  gap is where the target was. (`DesertManThrowTest`)
- [x] Needle throw: no arm or fist damager is on during the throw; only the body hurts on contact. (`DesertManThrowTest`)
- [x] Needle spin: the `STAND` guard is the row's chance (0.45 / 0.5 / 0.5 / 0.55), never twice in a row; it is
  checked after the tornado and before the needle throw. (`DesertManNeedleSpinTest`, `DesertManStandTest`)
- [x] Needle spin tell: before the jump he stands still for the row's tell time (0.6 / 0.5 / 0.35 / 0.3 s), shows
  `needle_spin_tell` (crouch, arms crossed; 2 frames, normal and flash, 0.08 s each, loop) and plays `REV_SOUND`; no needle fires during the tell. (`DesertManNeedleSpinTest`)
- [x] Needle spin: after the tell he jumps at 12 tiles/s up; `needle_spin_tuck` (knees up, arms crossed) shows until
  the top. While he rises, his x speed moves him toward the target: the x distance minus 3 tiles, at most 6 tiles, at
  least 0, and clamped to the arena walls, spread over the rise time (12 / 22.5 s). From the first burst tick on, his
  x speed is 0. (`DesertManNeedleSpinTest`)
- [x] A respawn turns the X friction back on, also after a death in the erupt jump. (`DesertManStandTest`)
- [x] Needle spin: at the top of the jump (y speed 0 or less) the first burst fires, then one burst per row delay
  (0.6 / 0.5 / 0.4 / 0.35 s) up to the row's burst count (2 / 2 / 3 / 4). (`DesertManNeedleSpinTest`)
- [x] Needle spin: each burst sets his y speed to 5 tiles/s up (he hovers, as Mega Man's needle spin) and plays
  `WHOOSH_SOUND`. (`DesertManNeedleSpinTest`)
- [x] Needle spin: the bursts alternate 2 fans of `Needle` (owner Desert Man, 15 tiles/s in every row, the
  same impulse as the dance needles, gravity -15 tiles/s2):
  7 needles at 90, 60, 30, 0, 330, 300, 270 degrees from up, then 6 needles at 75, 45, 15, 345, 315, 285 degrees,
  so the gaps of one burst are filled by the next. (`DesertManNeedleSpinTest`)
- [x] Needle spin: `needle_spin` (4 frames, 0.06 s each, loop) shows from the first burst to 0.3 s after the last;
  then `jump` until he lands on the sand, and then `STAND`. (`DesertManNeedleSpinTest`)
- [x] Intro: in `INIT`, he shows `jump` in the air; when his feet touch the ground, `intro` (7 frames: 0.2, 0.12,
  0.12, 0.25, 0.08, 0.08 s, then the last frame) plays once and holds the last frame (fist up) through the health
  bar fill, until `STAND`. It never shows the `dance` frames. (`DesertManStandTest`)

## Existing tests that an iteration round broke or made stale

- None in `core/src/test` (searched for `DesertMan`, `Needle`, `PlayerDamageNegotiator`: `FightTest` reads only the
  fight plan of `DesertMan`; `PlayerRulesTest` and `BigAssMaverickRobotDamageTest` read other entries).
- `core/src/smokeTest/resources/known-entity-failures.txt`: the `DesertMan` line is removed, because the class no
  longer requests the missing `DesertMan/init` region. Run `core:smokeTest` in the final pass to confirm that the
  class passes.
