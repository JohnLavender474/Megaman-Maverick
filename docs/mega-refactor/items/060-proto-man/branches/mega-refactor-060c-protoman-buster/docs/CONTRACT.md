# Contract: mega-refactor-060c-protoman-buster

- Status: approved
- Approved (scope change 5): 2026-11-14 (shield strapped on his back in the plain poses, crouch attacks:
  a low slash in the sabre slot and the standing shot from the crouch in the buster slot, round 35 art)
- Approved (scope change 4): 2026-11-13 (crouch guard on the ground, one 7x18 shield size, 4 and 3 pull-out frames,
  put-away frames, round 34 art)
- Approved (scope change 3): 2026-11-13 (held `DOWN` guard, full-body shield, pull-out frames, HUD icon under
  the health bar)
- Approved (scope change 2): 2026-11-13 (weapon slots, the buster that shoots while moving, damage per hit, the
  passive shield, the HUD slot icon; see `SCOPE.md`, "Scope change 2")
- Approved: 2026-11-11; changed and approved 2026-11-12 (scope change: Proto Man fights like X6 Zero, `060f` folded
  in; see `SCOPE.md`, "Scope change")

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with:
  - `ProtomanTest`, weapon slots: he starts with the sabre slot; `SELECT` (with `selectButtonAction` `NEXT_WEAPON`)
    switches sabre → buster → sabre and plays `SELECT_PING_SOUND`; with another `selectButtonAction`, `SELECT` does
    not switch.
  - `ProtomanTest`, buster slot: `B` fires one `ProtoBuster` on the ground, while he runs and in the air; left and
    right still move him; the cooldown holds; `UP`+`B` starts one sabre slash (no combo chain).
  - `ProtomanTest`, sabre slot: `B` and `UP`+`B` start the sabre; the 3-slash floor combo chains only on a press
    during a slash; a fall ends it; the landing rules of play test 4 hold; a hit and a room transition end the slash.
  - `ProtomanTest`, guard (scope changes 3 and 4): up only while `DOWN` is held, after the pull-out frames (4 of
    0.05 s on the ground, 3 in the air), on the ground and in the air; the shield fixture turns on with the guard pose
    and drops at once on the `DOWN` release; on the ground he stops and left and right only turn him; in the air he
    keeps his arc; `B` does not attack while `DOWN` is held; `DOWN` + `A` on the ground does not jump; a slash, a
    shot, a hit, a freeze, a teleport and a room transition drop it. Crouch (scope change 4): while he guards on the
    ground (pull-out and hold), his `BODY` and `DAMAGEABLE` boxes are 18 art px high with the feet on the floor, for
    each gravity direction; a shot at head height passes over the crouch and hits the stand; no crouch in the air;
    the stand boxes come back at once on the release. The round 34 shield box (7x18 art px; ground x 31..37,
    y 30..47; air x 32..38, y 22..39) on his facing side for each gravity direction; its top edge is the top edge
    of the crouched damage box; a `Bullet` from the front bounces off it with one spark, one from behind hurts. Put-away: after a release
    of a guard whose pose showed, 4 frames on the ground and 3 in the air, then the plain pose; none after a release
    in the pull-out (also on the step after the pull-out timer ends, before the guard pose shows) or after a break; `B`, `A` (a new press), left and right cut it at once; a hit, a freeze, a teleport, a
    room transition and out of play end it. Crouch attacks (scope change 5): with `DOWN` held on the ground (pull-out
    or guard), `B` with the sabre slot starts one `crouch_slash` (state `CROUCH`, half strength, no chain; `B` after
    it ends starts another; he neither runs nor turns during it), and `B` with the buster slot fires one `ProtoBuster`
    from the crouch muzzle (0.8125, -0.1875 tiles from the body center, mirrored to the left), with the buster
    cooldown, in the `crouch_shoot` pose; left and right only turn him during the shot. Each crouch attack drops the
    guard (shield off, no put-away) and keeps the crouch boxes for the whole attack; with `DOWN` still held, the full
    4-frame pull-out runs again after it; with `DOWN` released during it, he stays crouched to its end and then
    stands. `B` with `DOWN` held in the air does not attack. Each rule is seen to fail once with the rule removed.
  - The X sabre tests in `MegamanXSabreTest` and the X buster tests in `MegamanXTest` unchanged, except for the
    renamed types.
  - Damage tests: `ProtoBuster` does the half-charge damage (3, 10, 15). `PlayerSabreSlash` does the "half" sabre
    damage (4, 12, 20; raised 2026-11-14 by the owner) on Proto Man's hits 1 and 2, his air slash and his crouch slash,
    and the full-charge damage (5, 15, 30) on his hit 3 and on every X move. The X6 enemies keep their own slash
    damage for a full hit and take 2/3 of it (rounded up) from a half hit: `Junkroid` 23 / 16, `HoverGunner` 30 / 20,
    `MetaWheelF` 9 / 6, `SabreBlock` 9 / 6. `WoodCrate`: 20 from a half hit. Bosses keep 2 / 1 (`AbstractBoss`) and
    3 / 2 (`BigAssMaverickRobot`).
- [ ] `./gradlew core:smokeTest` passes: every new region is found (`guard_crouch_start`, `guard_crouch`,
  `guard_crouch_end`, `guard_jump_start`, `guard_jump`, `guard_jump_end`, `run_shoot`, `run_trans_shoot`,
  `jump_shoot`, `crouch_shoot`, `crouch_slash`, `crouch_slash_arc`, the 2 HUD slot icons); `ProtomanGuardStripsSmokeTest` finds each guard strip with the frame count
  of the code, `crouch_shoot` (1 frame), `crouch_slash` (4) and `crouch_slash_arc` (4 of 106x40), and no
  `guard_stand_start` or `guard_stand`; `entity-tags.txt` has `PlayerSabreSlash` and no
  `ProtoStrike`.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from the branch worktree. Proto Man checks first (new behavior), then the control checks.

Proto Man (new):

- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room sabre`: with the sabre slot (the HUD icon shows the sabre),
  `B` on the ground: up to 3 slashes when `B` is pressed again during a slash, purple arcs over his body; he does not
  move during a floor slash, but left and right turn him at any point, and the arc and its hit box turn with him. Walk off the ledge and press `B` in the air: 1 slash. Jump (`A`) in the middle of the
  combo: the slash ends at once. Slash just before a landing: the swing goes on on the ground with standing legs (no
  second sound, no chain); slash high enough that he lands in the follow-through: the slash ends on landing. The
  `Met` (while it is up), the `PicketJoe` and the `SniperJoe` take damage, also through their shields. The `Robbit`
  (`LARGE`, 30 health) shows hit 3: hits 1 and 2 and the air slash do 4, hit 3 does 5. With hits 1 and 2 only, it
  dies on the 8th hit; each hit 3 that lands makes it die sooner.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room buster`: press `SELECT`: a ping, and the HUD icon shows the
  buster (the icon is under the health bar). `B` fires while he stands, runs and jumps, with no lock. `UP`+`B` does one slash. The `PicketJoe` dies in 3
  shots on his body (10 each on 30 health; only while he throws; shots on his shield bounce).
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room shield`: he spawns in the middle of the room, and no enemy
  is on screen or shoots at him. A `SniperJoe` stands 10 tiles to the right on the same floor (by the right wall), and
  a second one 10 tiles to the left on a 0.75-tile red ledge (by the left wall); each spawns when he walks about 2
  tiles toward it, and the other one is then off screen. The ledge tiles fill the ledge block exactly, and the left
  `SniperJoe` stands on top of them. Walk right until the right `SniperJoe` shows, face it and hold `DOWN`: he
  crouches and pulls the shield out (4 frames: back, side, edge-on, angled), then the crouch guard shows the shield
  in front of his crouched body; its shots bounce off with a dink and a white burst, and do no damage. With
  `--debugShapes` added to the command, his body box is lower while he crouches (the purple damage box ends under the
  helmet of the standing pose). The right `SniperJoe` shoots with the bullet centre 0.65 tiles over the floor (top
  0.825), under the crouch top (1.125 tiles), so its shots meet the shield. Walk left until the left `SniperJoe`
  shows, face it and crouch: its shots (centre 1.4 tiles over his floor, bottom 1.225) pass over him; stand: they hit
  him. With `DOWN` held, left and right only turn him, and `A` does nothing. Crouch attacks (scope change 5): with
  `DOWN` held, `B` with the sabre slot does one low slash with a purple arc (no combo; `B` after it starts another);
  he stays crouched and does not move, the shield goes on his back, and the shots of the right `SniperJoe` then hurt
  him. With the buster slot (`SELECT`), `B` with `DOWN` held fires the pellet from the crouch in the `crouch_shoot`
  pose, at crouch height (it hits the right `SniperJoe` while he shoots, when his shield is down); left and right turn him. After each crouch
  attack with `DOWN` still held, the 4 pull-out frames play again and the shield is in front again. Release `DOWN`
  during a crouch attack: he stays crouched to its end, then stands with no put-away. Release `DOWN` in the guard:
  the shield drops at once (the shots hit him) and the put-away frames play, then he stands. Hold `DOWN` again,
  release, then at once press `B`, `A`, left or right: the put-away stops and the slash, the jump or the run starts
  at once. Jump and hold `DOWN` in the air: the guard jump pose shows after 3 pull-out frames, he keeps his arc, the
  shots bounce off, and `B` does nothing; the air shield leaves the bottom 8 art px of his body open, so a low shot
  can hit his feet in the air. Release in the air with no direction held: the air put-away frames play. Guard while
  facing away: the shots hit his back. Stand still without `DOWN`: no guard.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room walk`: stand, step, walk and jump play as before; with no
  `DOWN`, no guard pose shows. The shield is strapped on his back in `stand`, `run_trans`, `run` and `jump` (round
  35): no back arm on `stand` and `run_trans`, the shield top shows beside the helmet, and the scarf tip shows past
  it on `stand`. The shoot and sabre poses show the same back shield.

Control:

- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room flat`: X shoots, charges, fires half and full shots, and
  slashes (floor, air, wall) as before.
- `./run-game-alpha.sh --level X6_INTRO --room room1 --heartTanks all`: X's sabre and buster on the X6 enemies, as
  before.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`: Megaman shoots, charges and switches weapons as
  before; his HUD is unchanged.

## Behaviors to check

- A hit, a room transition and a death end Proto Man's slash and drop his shield (the room transition: in the room
  `transition`; the `sabre` and `buster` rooms have no neighbor).
- A respawn starts him with the sabre slot.

## Intended changes

- Proto Man on `REFACTOR_TEST_060` (a test level only):
  - 2 weapon slots on `SELECT`: the sabre (3-slash floor combo, 1 air slash, purple arc) and the Proto Buster (shoots
    while he stands, runs and jumps; no charge; `UP`+`B` slashes once).
  - A shield held on `DOWN` (scope changes 3 and 4): on the ground a crouch with a lower body box, in the air the
    jump pose; pull-out frames, then a 7x18 shield in front; put-away frames after the release.
  - Crouch attacks (scope change 5): `B` with `DOWN` held on the ground does a low half-strength slash (sabre slot)
    or the standing shot from the crouch (buster slot); the shield is strapped on his back in the plain poses.
  - A HUD icon for the slot, under the health bar.
  - No charge and no Proto Strike (`ProtoStrike` is removed).
- X's sabre moves to `players/` (`PlayerSabre`, `PlayerSabreSlash`); X plays the same, except that his sabre now
  also hurts classic enemies (B2, B3).
- `ProtoBuster` does the half-charge damage. `PlayerSabreSlash` does the half-charge damage on Proto Man's hits 1 and
  2 and his air slash, and the full-charge damage on his hit 3 and on X's moves (scope change 2).
- X's buster and the charge functions of the 3 classes moved to `players/` (first scope); X and Megaman play the same.
