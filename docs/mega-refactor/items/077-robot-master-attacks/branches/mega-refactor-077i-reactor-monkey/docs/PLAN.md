# Plan: mega-refactor-077i-reactor-monkey

How to do the first pass of `SCOPE.md`. It needs no approval. When the plan changes, write the reason in the process
log.

Rules for every step: `docs/CODE_STYLE.md`; `docs/ways-of-working/gameplay.md` (fast play rounds, no new tests, spec
in `docs/TEST-DEBT.md`); no game launch. Nothing compiles in this cloud session: the check of a code step is the CI
run of the PR (`claude-web.md` rule 6); re-read each change before the commit. Frame data: `docs/ASSETS.md`. Model
code: `GlacierMan` and `glacierman/GlacierManTuning.kt` (`077g`: phases, pending phase pose, death), `ElecDevil`
(`077c`), `DesertMan` (`077b`, tuning rows), `OrbAim`.

Subagent groups: A = steps 1 and 2 (docs and assets, this session); B = steps 3 to 9 (fight code, one agent, the
same 3 files: `ReactorMonkey.kt`, `ReactorMonkeyBall.kt`, the new tuning file); C = step 10 (PR).

## Steps

### 1. Branch docs

- Changes: this folder: `SCOPE.md`, `PLAN.md`, `NOTES.md`, `TODO.md`, `TEST-DEBT.md`, `ASSETS.md`, `process-log.md`.
- Check: the room names of the play checks exist in `ReactorMan_v3.tmx` and have a player spawn (`room5`, `room12`).
- Done: yes (2026-11-26)

### 2. Atlas and credit

- Changes: `assets/sprites/frames/ReactorMonkey/` (copies of the `077h` round-4 files, `chest_beat*` from round 5,
  `ball_mad/<i>` as `ReactorMonkeyBall/mad_<i>`); `sprite_sheets/ReactorMonkey.png` and `.txt` (packed by
  `temp/077i-reactor-monkey/pack.py`); `texture_packer/ReactorMonkey.tps` (from `Backgrounds13.tps`: Basic, no
  rotation, padding 2); `TextureAsset.REACTOR_MONKEY`; README `CREDITS` line for the update.
- Check: `temp/077i-reactor-monkey/verify.py` (every region equals its frame; 2 px apart; inside the border).
- Done: yes (2026-11-26)

### 3. Atlas switch, phases, damage, tuning rows

- Changes: `ReactorMonkey` reads every region from `REACTOR_MONKEY` (the new `stand`/`throw`); the ball reads
  `mad_0..3` from it and keeps `0..3` from `PROJECTILES_1`. New `bosses/reactormonkey/ReactorMonkeyTuning.kt` (rows
  normal, normal phase 2, hard, hard phase 2). `healthPhases = 2`, `healthBarRegions` pair. Boss damage 2 × `master`.
  `PlayerDamageNegotiator` entries for `ReactorMonkey` and `ReactorMonkeyBall` (owner check) with the `master` values.
- Check: CI; the values in `TEST-DEBT.md`.
- Done: yes (2026-11-26, first pass, not compiled). `ReactorMonkey` reads every region from `REACTOR_MONKEY`; the
  ball reads `0..3` from `PROJECTILES_1` and `mad_0..3` from `REACTOR_MONKEY`. `healthPhases = 2`, bars
  `STANDARD` + `BossBitsBar.PHASE_2_BITS` (the `077g` pair). Damage: a private `DAMAGE` table (2 × `BOSS_DMG_NEG`,
  fire 8, `MoonScythe` 4) through a `BossDamageNegotiator` override; `damageOverrides` gone. Player side:
  `ReactorMonkey` 4, `ReactorMonkeyBall` 3 when its owner is `ReactorMonkey`, else the fallback. New
  `bosses/reactormonkey/ReactorMonkeyTuning.kt` (`of(hard, phase)`): wait between throws normal 2.0-2.5 / 1.3-1.8 s,
  hard 1.2-1.5 / 0.8-1.1 s (interpolated by the health of the current phase, full = max); Fake hold 0.3-0.6 /
  0.3-0.5, hard 0.2-0.5 / 0.2-0.4 s; lead time 0.8 s (hard 1 s); Skip bounce limit 4 (phase 2: 5).
  `throwDelayTimer` is now a field (`spawn-allocation-baseline.txt` line removed).

### 4. Intro

- Changes: sprite shown in `ENTRANCE`; `preReady` runs drop (from above the room top, `intro_drop`), land
  (`intro_land`, `SHAKE_CAM`), roar (`intro_roar`, sound at cell 2); `isReady` at the end; `canBeDamagedBy` false in
  `ENTRANCE`; the AI updates only in `FIGHT`. The pinned `position` stays the spawn point (the drop draws or moves him
  above it and lands on it).
- Check: CI; owner play check 1.
- Done: yes (2026-11-26). States `INTRO_DROP`, `INTRO_LAND`, `INTRO_ROAR` run in `preReady`. The body stays pinned;
  the drop is a draw offset (`introDropOffsetY`) that starts at the camera top (read in `onSpawn`) and falls at
  10 tiles/s plus gravity 22.5 tiles/s². Land: `SHAKE_CAM` 0.4 s + `QUAKE_SOUND`; roar: `MM2_MECHA_DRAGON_SOUND` at
  cell 2. `isReady()` = state `STAND`. `canBeDamagedBy` is false in `ENTRANCE`; the AI updatable runs only in `FIGHT`.

### 5. Throw deck and mixed aims

- Changes: a throw type per throw (Lob, Skip, Fake; no type twice in a row) and a shuffled `OrbAim` cycle; Skip
  sets a per-throw bounce limit and a low impulse at the floor ahead; Fake holds the `throw` pose (cell 1) for a
  random time from the row. `TODO`s for Bank (needs `throw_up`) at the deck. No per-throw allocations.
- Check: CI; owner play check 1.
- Done: yes (2026-11-26). `ReactorMonkeyThrowType { LOB, SKIP, FAKE }`, drawn with `services.random`, never the last
  type. Aims: `aimCycle` (the 4 `OrbAim`s, shuffled at spawn and after each pass), arena = camera x range, y from his
  floor to the view top; `CURRENT` = the player's body position (today's target). Lob and Fake use today's impulse;
  Fake holds `throw` cell 1 (`throw_hold`, a 1-cell slice). Skip: new `ReactorMonkeyBall.skip(floorTarget, hopTargetX,
  maxBounces)`: spike in 0.3 s to the floor 2 tiles ahead of his front edge, then 1.5-tile hops whose x speed makes
  the first hop land at the aim x (clamped 3-8 tiles/s); `lob(velocity)` for the other throws. The bounce limit is per
  ball (`maxBounces`, default 3). Bank: a `TODO` at the enum.

### 6. Mad Beat, enraged face, steam

- Changes: `onHealthPhaseStart(2)` sets a pending flag; the pose starts after the throw in progress: `chest_beat`
  (one `CHEST_BEAT_PASSES` constant, cells 4 to 9 repeated), `SHAKE_CAM` on cells 4 and 7 of each pass, a
  `steam_jet` at the end; then the `mad_` strips. Steam sprites at the ear points of `ASSETS.md` (2 extra sprites,
  flipped for the right ear and with the facing). The ball spawns as the enraged ball in phase 2. `TODO`s for the
  Chest-Beat Shockwave (needs `RadWave`), Reactor Shake, Gorilla Leap, Knuckle Charge, Fallout Pool at the phase-2 move
  choice.
- Check: CI; owner play check 1.
- Done: yes (2026-11-26). `onHealthPhaseStart` sets `madBeatPending`; `STAND` starts the beat when no ball is held
  or falling in (a ball in the air finishes). `CHEST_BEAT_PASSES = 3` (owner order of this session; the art default
  was 4): one `chest_beat` `Animation` with cells 4-9 repeated; hit times, the steam time and the timer length are
  derived from it in `init()` (`defineChestBeatTimes`). Red tint blink for 0.5 s (mad face flash), roar at cell 3,
  `SHAKE_CAM` 0.15 s + `POUND_SOUND` on cells 4 and 7 of each pass, `steam_jet` from both ears at the last cell
  (the `mad_stand` pose, so the `mad_stand` 1 ear points). After it: `enraged`, so `mad_stand`/`mad_throw`
  (`_damaged` while the damage timer runs), the red ball (spawn prop `ConstKeys.RED`) and a steam loop (jet 0.18 s,
  puff 0.45 s, every 0.8 s) drawn by 2 extra monkey sprites at the `ASSETS.md` ear points (mirrored with the facing),
  hidden in strips with no ear points. `TODO`s: shockwave at the beat end, phase-2 moves at the move choice in
  `updateStand`, Fallout Pool at `ReactorMonkeyBall.explodeAndDie`.

### 7. Two-Ball Juggle

- Changes: the ball reference becomes an owned list (size from the row: 2 normal, 3 hard); the juggle drops the balls
  0.4 s apart and throws them at LEAD, CURRENT (, NEAR). It is a phase-2 move in the deck choice.
- Check: CI; owner play checks 1 and 5.

### 8. Hot Potato

- Changes: in `ReactorMonkeyBall`, a fully charged `ChargedShot` hit on a ball in flight (`hitProjectile` or the
  shield hit; the shot is spent) reverses it toward its owner. A returned ball hurts
  the monkey on contact (his body, not the player-side damage rule), explodes, and he plays `ball_hit` /
  `mad_ball_hit` with a stun (cells 3 and 4 loop); a hit while stunned does more damage (row value). A returned ball
  does not hurt the player.
- Check: CI; owner play check 1.

### 9. Death and ball cleanup

- Changes: `triggerDefeat` explodes every live ball; the death plays `dying` (a set time), `collapse` (thud at cell
  5), `dead`; `defeatDur` = sequence + hold; no `explodeOnDefeat`, no alpha fade. `onDestroy` destroys any ball left.
- Check: CI; owner play checks 1 and 2.

### 10. Draft PR

- Changes: commit, push, a draft PR (`claude-web.md` rule 4; base `master`, or the `077h` branch while
  it is open, so the diff shows only this branch) with the brief parts, the decisions made
  without the owner, the play checks, and the note that the branch is stacked on `077h`.
- Check: the CI result of the PR head (`check-runs`), listed as run or not run.
