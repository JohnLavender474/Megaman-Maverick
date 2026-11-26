# Test debt: mega-refactor-077i-reactor-monkey

The fight's spec while the moveset changes, one rule per line in plain words. Each line becomes a test when the owner
says the fight is final (`docs/ways-of-working/gameplay.md`, "Gameplay iteration"), on the `077b` rig pattern (a
scripted `GameRandom`). The code agent fills in the numbers it chooses and marks a line `[x]` when the code does it.

## Rules to make into tests

### Assets

- [ ] `ReactorMonkey` and `ReactorMonkeyBall` get every region they read from `TextureAsset.REACTOR_MONKEY` (and the
  calm ball from `PROJECTILES_1`); `core:smokeTest` covers it.

### Phases and damage

- [x] The monkey has 2 health phases; health rolls over at 0 in phase 1; defeat only at 0 in phase 2.
- [x] Damage on the monkey: each value is 2 × the `master` value (`BOSS_DMG_NEG` × 2, `Fireball`, `MagmaWave`,
  `MagmaFlame` 8, `MoonScythe` 4), so each weapon takes the same number of hits as on `master` over the 2 phases.
- [x] Damage to the player: `ReactorMonkey` (4) and `ReactorMonkeyBall` (owner `ReactorMonkey`, 3) keep the `master` values;
  a ball of another owner keeps the fallback value.
- [x] The tuning row follows the difficulty and the phase (`ReactorMonkeyTuning.of`): waits normal 2.0-2.5 /
  1.3-1.8 s, hard 1.2-1.5 / 0.8-1.1 s, longest at full health of the phase.

### Intro

- [x] The sprite shows in `ENTRANCE`; the strips run `intro_drop` (in the air), `intro_land`, `intro_roar`, then
  `stand`.
- [x] `isReady` is false until `intro_roar` ends, then true.
- [x] No damage to him in `ENTRANCE`.
- [x] No throw and no ball before `FIGHT`.
- [x] The land submits `SHAKE_CAM` once.
- [x] The drop starts with the sprite bottom at the view top and ends with his feet on the spawn point.

### Throw deck

- [x] Each throw is Lob, Skip or Fake; never the same type twice in a row.
- [x] The aims follow a shuffled cycle of `LEAD`, `CURRENT`, `NEAR`, `WIDE`: each aim once per 4 throws.
- [x] Skip: the ball's first contact is the floor 2 tiles ahead of his front edge; it hops 1.5 tiles high, the first
  hop lands at the aim x (speed 3 to 8 tiles/s); its bounce limit is the row's (4, phase 2: 5).
- [x] Fake: he holds the throw pose for a time in the row's range before the release.
- [x] The ball still drops in from `ball_spawn_y` over his head.

### Mad Beat and phase 2

- [x] The phase-2 start waits for the throw in progress (a ball held or falling in); no new ball spawns in the pose.
- [x] The pose plays `chest_beat` with `CHEST_BEAT_PASSES` (3) passes of cells 4 to 9 and submits `SHAKE_CAM` on cells 4
  and 7 of each pass (2 × passes shakes).
- [x] After the pose the strips are the `mad_` strips, and the steam shows at the ear points (a jet at the last
  beat cell, then jet + puff every 0.8 s); the steam is hidden in strips without ear points and when he is defeated.
- [x] Phase-2 balls use the `mad_` regions (balls spawned after the pose starts).
- [x] He can be hurt during the pose.
- [ ] Two-Ball Juggle (phase 2 only): 2 balls (3 on hard) drop 0.4 s apart; the throws aim `LEAD`, `CURRENT` (,
  `NEAR`).

### Hot Potato

- [ ] A fully charged shot that hits a ball in flight sends it back toward the monkey; a weaker shot does not.
- [ ] A ball that is held, falling in or exploding cannot be sent back.
- [ ] A returned ball does not hurt the player.
- [ ] A returned ball that touches him hurts him, explodes, and starts the stun (`ball_hit` in phase 1,
  `mad_ball_hit` in phase 2) for the row's stun time.
- [ ] A hit during the stun does the row's bonus damage.

### Death

- [ ] At the defeat every live ball explodes (thrown, falling, juggled).
- [ ] The death plays `dying`, then `collapse` once, then `dead` until the defeat time ends; no alpha fade, no defeat
  explosions.
- [ ] `defeatDur` = the death sequence + the hold.
- [ ] The Heart Tank of a `heart_tank` spawn appears at `onDestroy` when the save does not hold it.
- [ ] No ball is left in the engine after `onDestroy`.

## Existing tests broken by a round

- None yet.
