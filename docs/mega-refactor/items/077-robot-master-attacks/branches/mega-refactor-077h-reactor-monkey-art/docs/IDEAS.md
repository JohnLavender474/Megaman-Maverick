# Reactor Monkey: fight ideas (077h)

Draft by the design agent, 2026-11-26. Code refs are `master` `19e713e`. "Inferred" = read from the code, not play-tested.

## 1. What is flat today

- **One loop, one variable.** States `STAND` and `THROW` only. The only thing that changes over the fight is the
  wait between throws: `interpolate(min, max, getHealthRatio())`, 2.5 → 1.25 s (hard 1.5 → 0.75 s).
- **One throw.** Every throw is `calculateJumpImpulse` at `targetPlayer.body.getPosition()` (the player's position
  now), apex 6 tiles. Walk after the release and it misses. Never a lead aim, never a second ball.
- **He never moves.** The body is `ABSTRACT` and pinned to the spawn point each frame (`preProcess`). He only turns
  to face the player.
- **No intro, no death.** `isReady()` is `true` at once and the sprite is hidden in `ENTRANCE`, so he pops in.
  Death = the shared `explodeOnDefeat` + a 3 s alpha fade, no orbs.
- **Loose ends.** The thrown ball's reference is dropped at the throw (`monkeyBall = null`), so `onDestroy` does not
  remove it: inferred, a ball in the air can still hit after his death. No `PlayerDamageNegotiator` entry (size
  fallbacks). No map spawn sets `ball_spawn_y` (all use 8 tiles over his head, so the ball always drops in through
  the ceiling; keep that, it is his signature).

### The 3 arenas (`ReactorMan_v3.tmx`, all 16 × 14, one screen)

| Room | Layout | Monkey | Player strip |
|---|---|---|---|
| `mini_boss_room_1` (obj 212) | Flat floor (row 12), ceiling at row 0 to 1, no platforms, no hazards. Enter from the right (rows 7-9, a short drop), exit left behind him | left, center col 4.5 | ~8.5 tiles |
| `mini_boss_room_2` (obj 557) | Same flat box, 13 wide. A `FloatingCanHole` at col 1 row 3 on the player's side (extra pressure). Enter left, exit right behind him | right, col 11.5 | ~8.5 tiles |
| `alt_path_1_mini_boss_room_1` (obj 213, gives Heart Tank E) | `ToxicWater` 2 tiles deep over the whole floor; a 3-wide dip at cols 6-8 with a `Death` sensor (a death pit under the water, inferred). Enter top-left (a ~9-tile drop into water), exit right at rows 6-8 | right, col 12.5, waist-deep | ~9.5 tiles, in water |

## 2. Phase structure (recommended)

**2 health phases** with the shared mechanism: `healthPhases = 2`, two `healthBarRegions`, `onHealthPhaseStart(2)`.
Damage to him 2 × `master` (1-for-1 hits to kill: Fireball/MagmaWave/MagmaFlame 8, MoonScythe 4, the rest from
`BOSS_DMG_NEG` × 2). Tuning rows `ReactorMonkeyTuning` per difficulty × phase (the `077b` pattern). No 3rd phase: a
mini boss with a 30-bit phase is already short.

- **Phase 1 "playful":** the throw deck (move 1). Calm face.
- **Roll-over, the Mad Beat (~1.6 s):** `onHealthPhaseStart(2)` only sets a pending flag (the `077g` `PHASE_START`
  rule); the pose starts when the throw in progress ends. Frames: red mad face flash → `chest_beat` ×3, one
  `SHAKE_CAM` per beat → `steam_jet` from both ears on the last beat → `mad_stand`. No new ball during the pose; a
  ball already in the air finishes. He **can** be hit in the pose (precedent: Desert Man, Elec Devil, Glacier Man),
  and the pose itself does no damage beyond his body. Mini bosses show **no health bar** (the mini plan has no
  `FillBossHealthBar`), so this beat is the only sign of phase 2. It must be loud.
- **Phase 2 "enraged":** `mad_stand` with a looping `steam` puff from the ears, the red-symbol ball (the drafted
  enraged ball), quicker tuning rows, and the phase-2 moves below. The steam can double as a tell: a big `steam_jet`
  0.4 s before each phase-2 special.

Reuse: `AbstractBoss` phases and roll-over, `OrbAim` (LEAD / CURRENT / NEAR / WIDE; its 4th caller), the tuning row
pattern, the `PHASE_START` pending-flag rule, `SHAKE_CAM`. New: the state set, the throw deck, the new projectiles.

## 3. New moves, ranked

1. **Throw deck** (P1 + P2). Each throw draws one of: *Lob* (today's arc), *Skip* (he spikes the ball at the floor
   2 tiles ahead; low 1.5-tile hops across the room; jump it), *Bank* (thrown up at the ceiling, drops on the
   player; the ball's head fixture already bounces it down), *Fake* (holds overhead 0.3-0.6 s random, then throws).
   No same type twice in a row. Each throw takes the next `OrbAim` slot of a shuffled LEAD/CURRENT/NEAR/WIDE cycle,
   so single throws mix aims over time. Art: Skip reuses `throw` frame 2 (already a low swing); Bank needs
   `throw_up`. Skip needs a per-throw bounce count (today `MAX_BOUNCES = 3`).
2. **Chest-Beat Shockwave** (P2). Steam tell → 3 beats → both fists hit the floor → a green radiation wave runs
   along the floor toward the player (1.5 tiles tall; jump it), a 2nd wave 0.5 s later on hard. Uses the drafted
   `chest_beat`; new `RadWave` strip. In the water room the wave runs on the water surface.
3. **Two-Ball Juggle** (P2). Two balls drop 0.4 s apart; he catches and throws the first at LEAD, the second at
   CURRENT (hard: a 3rd at NEAR). The fight's main pressure step. No new art (enraged ball). Code: one ball slot → a
   small list.
4. **Gorilla Leap** (P2). Crouch tell (0.5 s) → he leaps to the other side of the room and lands with a shake;
   the arena flips. Dodge: run under the arc toward his old side. The drafted `intro_drop` / `intro_land` cover the
   air and landing frames; only `leap_crouch` is new. Needs a real (non-`ABSTRACT`) body or a scripted arc. Off in
   the water room (death pit in the middle).
5. **Fallout Pool** (P2). The enraged ball's last bounce leaves a bubbling green puddle (2 tiles wide, 2 s) instead
   of only the explosion. It shrinks the safe floor for the next throws. Not in the water room.
6. **Hot Potato** (P1 + P2, a player verb). A full charged shot knocks a falling ball back up; if it lands on him,
   it hurts him (big hit). Today the ball is a `SHIELD` that eats shots, so this changes a contact rule. Owner taste
   call.
7. **Reactor Shake** (P2, alternative to 2). Ground pound; 4 chunks of rubble fall with mixed aims
   (LEAD/CURRENT/NEAR/WIDE), dust marks on the floor 0.5 s before. Close to Glacier Man's Icicle Rain, so rank low.
8. **Knuckle Charge** (P2). On all fours (the `throw` frame 2 pose) he charges the far wall and back. Only fair if
   his body there is at most 2.5 tiles tall (full jump apex is about 3.4). Needs an art check first.

## 4. Intro and death

**Intro (in `ENTRANCE`, ~1.5 s, replays on each retry so keep it short):** `preReady` runs it: `intro_drop` (he
falls in through the ceiling like his ball) → `intro_land` (+ `SHAKE_CAM`, dust) → `intro_roar` (a short half
chest-beat). `isReady()` returns true at the end of `intro_roar`. Show the sprite in `ENTRANCE` (today it is hidden).
The player cannot move: the room transition turns the controller off and the mini plan's `ControllerOn` turns it
back on after `AwaitBossReady` (inferred from `LevelRoomHandler` and `FightPlans.miniBossPlan`). Also block damage
to him in `ENTRANCE` (`canBeDamagedBy` only checks `defeated` today; a shot fired before the gate could hit), and
start the AI only in `FIGHT`.

**Death:** at `triggerDefeat` explode every live ball (thrown, falling, juggled) and any pool or wave. Then
`dying` (sparks, steam bursts, staggering) → `collapse` → `dead` held 1 s; `defeatDur` = sequence + 1 s (the
`077g` pattern, no `AbstractBoss` change). No defeat explosions and no alpha fade (the owner removed them for
Glacier Man); a final small blast at `collapse` is optional. The mini outro has no `BossDefeatLock`, so the player
keeps control; that is fine, the gate opens only when he is gone, and the Heart Tank (alt room) appears at
`onDestroy` as today.

## 5. Difficulty and arenas

| | Normal P1 | Normal P2 | Hard P1 | Hard P2 |
|---|---|---|---|---|
| Wait between moves | 2.0-2.5 s | 1.3-1.8 s | 1.2-1.5 s | 0.8-1.1 s |
| Juggle balls | - | 2 | - | 3 |
| Shockwaves | - | 1 | - | 2 |
| Fake hold | 0.3-0.6 s | 0.3-0.5 s | 0.2-0.5 s | 0.2-0.4 s |

Same fight in all 3 rooms; the rooms already differ. Room 1 is the open first meeting. Room 2 adds the
`FloatingCanHole` on the player's side, so keep it as is. The water room (optional path, Heart Tank reward) is the
hardest by nature: water changes the player's jump, and a hit knock-back near cols 6-8 risks the death pit.
Turn off Leap and Fallout Pool there with one spawn prop read once into a typed field (`O1-R4`), and use the
existing `ball_spawn_y` only if a room needs it.

## 6. First implementation branch (`077i`)

Package: 2 phases + Mad Beat, intro, death + ball cleanup, the throw deck (Lob, Skip, Bank, Fake), Two-Ball Juggle,
Chest-Beat Shockwave, tuning rows, `PlayerDamageNegotiator` entries, test rooms in `RefactorTest077.tmx` (a copy of
`mini_boss_room_1` and of the water room). Later branch: Gorilla Leap, Fallout Pool, the others.

| Move | New strip / entity | Size | Draft covers? |
|---|---|---|---|
| Intro | `intro_drop`, `intro_land`, `intro_roar` | 160 × 160 cells | yes |
| Death | `dying`, `collapse`, `dead` | 160 × 160 | yes |
| Mad Beat | `chest_beat`, `mad_stand`, `mad_throw` (+ `_damaged`), `steam`, `steam_jet` | 160 × 160; steam per the drafts | yes |
| Enraged ball | red-symbol ball | 64 × 64 (as `ReactorMonkeyBall`) | yes (optional draft) |
| Bank throw | `throw_up` (+ `_damaged`, `mad_` and `mad_..._damaged`) | 160 × 160, ~3 frames | **no** |
| Skip / Fake | reuse `throw` / `mad_throw` | - | yes (existing) |
| Shockwave | `chest_beat` + new `RadWave` entity strip | ~32 × 24 art px, 3-4 frames | **no** (beat yes) |
| Fist slam end of the beat | `chest_beat_slam` (or the last beat frame) | 160 × 160, 1-2 frames | **no** |
| Dust at landing / slam | small puff | ~32 × 16 art px, 3 frames | **no** (maybe in `intro_land`) |
| (later) Leap | `leap_crouch` | 160 × 160, 1-2 frames | partly (drop, land) |
| (later) Fallout Pool | `FalloutPool` | ~32 × 8 art px, 4 frames | **no** |

## 7. Risks and open questions

1. **No health bar for mini bosses.** Phase 2 is visible only through the Mad Beat. *Rec:* keep no bar; make the
   beat loud (shake, steam, red flash). Add a bar only if play tests show players miss the change.
2. **Strip count grows ×4** (normal, damaged, mad, mad damaged). *Rec:* moves that exist only in phase 2 get only the
   `mad_` + `mad_..._damaged` strips; only `stand` and `throw`/`throw_up` need all 4.
3. **Should he move?** Leap and Charge break "he never moves" and need a new body. *Rec:* not in `077i`; try Leap
   in a 2nd branch once the static fight feels good.
4. **Hot Potato** changes a shield rule that other balls may share. *Rec:* a yes/no from the owner before any design
   work; if no, drop it.
