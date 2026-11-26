# Test debt: mega-refactor-077i-reactor-monkey

The fight's spec while the moveset changes, one rule per line in plain words. Each line becomes a test when the owner
says the fight is final (`docs/ways-of-working/gameplay.md`, "Gameplay iteration"), on the `077b` rig pattern (a
scripted `GameRandom`). The code agent fills in the numbers it chooses and marks a line `[x]` when the code does it.

## Rules to make into tests

### Assets

- [ ] `ReactorMonkey` and `ReactorMonkeyBall` get every region they read from `TextureAsset.REACTOR_MONKEY` (and the
  calm ball from `PROJECTILES_1`); `core:smokeTest` covers it.

### Phases and damage

- [ ] The monkey has 2 health phases; health rolls over at 0 in phase 1; defeat only at 0 in phase 2.
- [ ] Damage on the monkey: each value is 2 × the `master` value (`BOSS_DMG_NEG` × 2, `Fireball`, `MagmaWave`,
  `MagmaFlame` 8, `MoonScythe` 4), so each weapon takes the same number of hits as on `master` over the 2 phases.
- [ ] Damage to the player: `ReactorMonkey` and `ReactorMonkeyBall` (owner `ReactorMonkey`) keep the `master` values;
  a ball of another owner keeps the fallback value.
- [ ] The tuning row follows the difficulty and the phase.

### Intro

- [ ] The sprite shows in `ENTRANCE`; the strips run `intro_drop` (in the air), `intro_land`, `intro_roar`, then
  `stand`.
- [ ] `isReady` is false until `intro_roar` ends, then true.
- [ ] No damage to him in `ENTRANCE`.
- [ ] No throw and no ball before `FIGHT`.
- [ ] The land submits `SHAKE_CAM` once.

### Throw deck

- [ ] Each throw is Lob, Skip or Fake; never the same type twice in a row.
- [ ] The aims follow a shuffled cycle of `LEAD`, `CURRENT`, `NEAR`, `WIDE`: each aim once per 4 throws.
- [ ] Skip: the ball's first contact is the floor ahead of him; it hops low with its own bounce limit.
- [ ] Fake: he holds the throw pose for a time in the row's range before the release.
- [ ] The ball still drops in from `ball_spawn_y` over his head.

### Mad Beat and phase 2

- [ ] The phase-2 start waits for the throw in progress; no new ball spawns in the pose.
- [ ] The pose plays `chest_beat` with `CHEST_BEAT_PASSES` passes of cells 4 to 9 and submits `SHAKE_CAM` on cells 4
  and 7 of each pass (2 × passes shakes).
- [ ] After the pose the strips are the `mad_` strips, and the steam shows at the ear points.
- [ ] Phase-2 balls use the `mad_` regions.
- [ ] He can be hurt during the pose.
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
