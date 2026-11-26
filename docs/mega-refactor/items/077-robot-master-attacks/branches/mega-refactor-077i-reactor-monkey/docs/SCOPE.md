# Scope: mega-refactor-077i-reactor-monkey

What this branch changes and why, and its contract. Written from the `077h` design (`IDEAS.md`) and art notes, the
owner's decisions of 2026-11-26 and a scan of `master` `19e713e` (the base of `077h`).

- Status: draft, first pass; the owner reads the PR
- Approved: no (owner order 2026-11-26, from the phone: "Go ahead with the branch and the first-pass implementation,
  then push a draft PR"; the brief goes in the PR text, `claude-web.md`)

## Goal

A richer Reactor Monkey mini boss: 2 health phases with a loud "Mad Beat" between them, an intro and a full death
sequence, a throw deck with mixed aims, a phase-2 Two-Ball Juggle and enraged ball, and Hot Potato (a charged shot
knocks his ball back into him). The same class runs all 3 mini-boss rooms of Reactor Man's stage.

## Item

`077` (one branch per boss; a boss with health phases gets its harder attacks in its last phase; damage values 2 ×
`master` on 2 phases, so the hits to kill stay 1-for-1). Merge target `master`. This branch is stacked on the art
branch `077h` (cut from `mega-refactor-077h-reactor-monkey-art`), so it holds the drafts and `IDEAS.md`.

## Owner input (2026-11-26)

- Death plays the full sequence (`dying`, `collapse`, `dead`), no alpha fade.
- Hot Potato is in: a charged shot can knock his ball back; the monkey responds (`ball_hit`, `mad_ball_hit`).
- The number of chest-beat loop passes is decided after the owner's play test: ONE constant.
- First pass now, then a draft PR.

## Current state (`master` `19e713e`)

- `entities/bosses/ReactorMonkey.kt` (about 330 lines), `AbstractBoss(dmgDuration = DEFAULT_MINI_BOSS_DMG_DURATION,
  size = Size.LARGE)`. States `STAND` and `THROW`. A random throw delay `interpolate(min, max, getHealthRatio())`
  (normal 1.25 to 2.5 s, hard 0.75 to 1.5 s). The ball falls in from `ballSpawnY` (8 tiles) over his head, he catches
  it (`ballCatchArea`) and throws it with `calculateJumpImpulse` at `targetPlayer.body.getPosition()`, apex 6 tiles.
- Body `ABSTRACT`, 3.5 × 5 tiles, pinned to the spawn each frame in `preProcess`; sprite 10 tiles, hidden in
  `ENTRANCE`; alpha fade over `defeatTimer`; `explodeOnDefeat`. `isReady()` is `true` at once.
- `canBeDamagedBy` is the `AbstractBoss` one (checks `defeated` only), so a shot in `ENTRANCE` can hit (inferred).
- The thrown ball's reference is cleared at the throw (`monkeyBall = null`), so `onDestroy` cannot remove a ball in
  the air (inferred: a ball can hit after his death).
- Damage to him: `BOSS_DMG_NEG` plus `Fireball`, `MagmaWave`, `MagmaFlame` 4 and `MoonScythe` 2. 1 health phase.
- Damage to the player: no `PlayerDamageNegotiator` entry; fallbacks: the monkey is `Size.LARGE` (4), the ball goes
  by its own size or type (read it in `fallbackDamage` and keep the value).
- `entities/projectiles/ReactorMonkeyBall.kt`: fixtures `SHIELD`, `DAMAGER`, `PROJECTILE`, `WATER_LISTENER` (circle
  1.75 tiles), head/feet/side fixtures bounce on blocks; `MAX_BOUNCES = 3` then `explodeAndDie` (an
  `EXPLOSION_FIELD`); key = bounce count; regions `Projectiles1` `ReactorMonkeyBall/0..3`. A `ChargedShot` meets the
  `SHIELD` and bounces off (`ChargedShot.hitShield`); a `PROJECTILE`-`PROJECTILE` contact calls `hitProjectile` on both.
- `FightPlans.kt`: spawns with `mini = true` use `miniBossPlan` (no health bar, no boss music, no defeat lock).
- Spawns in `ReactorMan_v3.tmx` (`LevelDefinition.REACTOR_MAN`): obj 212 `mini_boss_room_1` (key 1), obj 213
  `alt_path_1_mini_boss_room_1` (key 2, `heart_tank=e`, toxic water), obj 557 `mini_boss_room_2` (key 3). The rooms
  are described in the `077h` `IDEAS.md` section 1.
- Shared machinery to reuse: `AbstractBoss.healthPhases`, `healthBarRegions`, `translateHealth` roll-over,
  `onHealthPhaseStart`; the `077g` pending-flag rule (`GlacierMan.phaseStartPending`: the phase pose waits for the
  move in progress); `OrbAim` (`bosses/bigassmaverickrobot/`, `LEAD`, `CURRENT`, `NEAR`, `WIDE`; callers
  `BigAssMaverickRobot`, `ElecDevil`, `GlacierMan`); tuning rows per difficulty and phase (`DesertManTuning`,
  `GlacierManTuning`); the `077g` death pattern (`defeatDur` = sequence + hold, no explosions, no fade).
- New art: `docs/ASSETS.md` (atlas `TextureAsset.REACTOR_MONKEY`, region names, timings, ear points).

## In scope (first pass)

1. **Atlas** (step 1, done): new atlas `ReactorMonkey` (frames, `.tps`, page, `.txt`, `TextureAsset.REACTOR_MONKEY`).
   `Bosses1` and `Projectiles1` unchanged.
2. **Phases:** `healthPhases = 2`; `healthBarRegions` with 2 entries (a mini boss shows no bar, but `AbstractBoss`
   needs one region per phase; use the `077g` pair). Damage to him 2 × `master`: `BOSS_DMG_NEG` × 2 for the damagers
   that he takes from the table, fire 8, `MoonScythe` 4, and so on (the `077g` table as the model).
3. **Mad Beat** (phase 2 start): `onHealthPhaseStart(2)` sets a pending flag; when the throw in progress ends, he plays
   `chest_beat` (N loop passes, ONE constant), `SHAKE_CAM` on each hit cell, a `steam_jet` from both ears at the end,
   then `mad_stand`. No new ball in the pose; a ball in the air finishes. He can be hit in the pose (hits count).
4. **Intro:** sprite visible in `ENTRANCE`; `preReady` drops him in from above the room in `intro_drop`, then
   `intro_land` (`SHAKE_CAM`), then `intro_roar`; `isReady()` is true at its end. No damage to him in `ENTRANCE`
   (`canBeDamagedBy`). The AI runs only in `FIGHT`.
5. **Death:** at `triggerDefeat` every live ball explodes (the thrown one, a falling one, both juggled ones). Then
   `dying` (loop, a set time), `collapse` once, `dead` held; `defeatDur` = sequence + hold. No defeat explosions and no
   alpha fade. The Heart Tank of the water room still appears at `onDestroy`.
6. **Throw deck:** each throw draws one of *Lob* (today's arc), *Skip* (spiked at the floor 2 tiles ahead, low hops
   across the room; a per-throw bounce limit) and *Fake* (holds overhead a random 0.3 to 0.6 s, then throws). No type
   twice in a row. Each throw takes the next aim of a shuffled `OrbAim` cycle (LEAD, CURRENT, NEAR, WIDE). Picks use
   `services.random`.
7. **Two-Ball Juggle** (phase 2): two balls drop 0.4 s apart; he throws the first at LEAD and the second at CURRENT
   (hard: a third at NEAR). The single ball reference becomes a small owned list.
8. **Enraged ball and face** (phase 2): the ball uses `ReactorMonkeyBall/mad_0..3`; he uses `mad_stand`,
   `mad_throw`, `mad_ball_hit` (and their `_damaged`); a looping `steam` puff from the ears at the `ASSETS.md` points,
   with `steam_jet` as a tell before a juggle.
9. **Hot Potato:** a fully charged `ChargedShot` that hits his ball in flight knocks it back toward him (the shot is
   spent). A returned ball that touches him damages him, explodes, and stuns him: `ball_hit` / `mad_ball_hit`, cells 3
   and 4 loop for the stun. A hit while stunned does more damage. A ball held, falling in or exploding cannot be
   returned. Charge level, ball damage, stun length and the stun bonus are tuning values of the code agent.
10. **Tuning rows** per difficulty × phase (`ReactorMonkeyTuning`, the `077b` pattern): waits, Fake hold, juggle
    count, ball speeds, the Hot Potato values.
11. **Damage to the player:** `PlayerDamageNegotiator` entries for `ReactorMonkey` and `ReactorMonkeyBall` (owner
    `ReactorMonkey`; the `else` keeps the value for other owners), with the `master` values (gameplay.md).

### New API and its first caller

| New API | First caller |
|---|---|
| `TextureAsset.REACTOR_MONKEY` | `ReactorMonkey.init`, `ReactorMonkeyBall.init` |
| `ReactorMonkeyTuning` (rows per difficulty and phase) | `ReactorMonkey` |
| Ball: enraged flag, throw type data (bounce limit), return state (spawn props or typed setters) | `ReactorMonkey` throw and Hot Potato |
| `PlayerDamageNegotiator` entries | `PlayerDamageNegotiator.get` |

## Out of scope (deferred; their art does not exist yet)

- Bank throw (needs `throw_up` and its copies), Chest-Beat Shockwave (needs `RadWave` and a slam frame), Gorilla Leap
  (needs `leap_crouch` and a real body), Fallout Pool (needs `FalloutPool`), Reactor Shake, Knuckle Charge. Each gets
  a `TODO` at the throw deck or the phase-2 move choice and a line in `docs/TODO.md`.
- Anything special in the water room (`alt_path_1_mini_boss_room_1`).
- Test maps and tests (`gameplay.md`: iterate first; the spec is in `docs/TEST-DEBT.md`).
- Removal of the old `Bosses1` `ReactorMonkey/*` and `Projectiles1` ball frames (later cleanup, `docs/TODO.md`).
- Changes to `AbstractBoss` or `FightPlans`.

## Decisions made without the owner

- The first-pass cut above: the moves with art now; the 6 moves without art deferred.
- The tuning numbers (waits, speeds, Fake hold, stun, damage of the returned ball, stun bonus) are chosen by the code
  agent from the `IDEAS.md` table; the owner tunes them at the play test.
- The branch is stacked on `077h` (the art branch is not merged yet): the PR diff shows the `077h` docs too until `077h`
  merges. Merge `077h` first.
- A new atlas `ReactorMonkey` instead of a repack of `Bosses1`/`Projectiles1`: no TexturePacker in the cloud, and a
  hand repack of a shared atlas risks the other regions. The page was packed by a script in `temp/`; a laptop repack
  with the `.tps` (Basic) gives the same names and pixels.
- The steam is drawn by the monkey (regions `ReactorMonkey/steam`, `steam_jet`), not by a new entity.
- The mad ball regions are `ReactorMonkeyBall/mad_0..3` in the new atlas.
- Default chest-beat passes: 4 (the art's count, 2.22 s); 3 is the alternative (1.82 s).
- `O4-R2` (a test room for each mechanic): the first pass uses the 3 live rooms; a `RefactorTest077.tmx` room for the
  monkey is a TODO before the merge (`docs/TODO.md`).

## Objective check

| Rule | Effect | Note |
|---|---|---|
| Always-on rules not listed | no effect | |
| `O1-R4` Typed state, not new property keys | no effect | ball data passes as typed fields or existing keys |
| `O1-R5` Code only with a production caller | no effect | each new API has a caller (table above) |
| `O2-R2` Random only from the seeded source | helps | the deck, the aims and the Fake hold use `services.random` |
| `O2-R5` No logic assumes one player | no effect | aims use `targetPlayer`, as today |
| `O5-R1` No per-frame allocation | helps | the rewritten aim and throw code uses owned fields or pools |
| `O6-R2` Named checks | waiting | the spec is in `TEST-DEBT.md`; tests after the moveset is final |
| `O4` | helps | richer fight |
| `O4-R2` A test room for each mechanic | gap | live rooms in the first pass; test room before merge (decision above) |
| `O4-R3` State machines | waiting (`015`) | the fight keeps its own state model |

## Overlap with other branches

See `NOTES.md`.

# Contract

Draft, first pass. Nothing compiles in the cloud session (Gradle is blocked by the proxy): the automated checks are
the CI run of the PR (`claude-web.md` rule 6).

## Automated checks

- [ ] CI of the PR: `engine:test`, `core:test`, `core:smokeTest` (the monkey and the ball get through `init()` with
  the new atlas), `lwjgl3:jar`, `lost-memories-desktop:jar`, `detektMain`, `core:verifyRandomRule`.
- [ ] `entity-tags.txt` unchanged, or updated with any new entity class.
- [ ] `ReactorMonkey.txt` regions equal their frames pixel for pixel (`temp/077i-reactor-monkey/verify.py`; done
  2026-11-26).
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from the worktree. Each check fights the mini boss, so each adds `--heartTanks all --healthTanks all`. `room5` is
the nearest room with a player spawn before both room-1 mini bosses: go up at its left end for the water room, and up
at its right end (through `room6`) for the first room. `room12` comes before the second.

- `./run-mega-man-maverick-alpha.sh --level REACTOR_MAN --room room5 --heartTanks all --healthTanks all` (up at the
  right end): intro (drop, land shake, roar; no damage before the fight); phase 1 throw deck (Lob, Skip, Fake) with
  mixed aims; Hot Potato with a full charge; the Mad Beat; phase 2 (red face, steam, red ball, Two-Ball Juggle); death
  (dying, collapse, dead, no fade); every ball gone at the defeat.
- `./run-mega-man-maverick-alpha.sh --level REACTOR_MAN --room room5 --heartTanks all --healthTanks all` (up at the
  left end): the water room: the same fight, waist-deep; the Heart Tank E appears after the death.
- `./run-mega-man-maverick-alpha.sh --level REACTOR_MAN --room room12 --heartTanks all --healthTanks all`: the second
  room with the `FloatingCanHole`; the same fight; the stage goes on after the gate.
- `./run-mega-man-maverick-alpha.sh --level REACTOR_MAN --room room12 --heartTanks all --healthTanks all --weapons all`:
  hits to kill per weapon equal to `master` over the 2 phases.
- `./run-mega-man-maverick-alpha.sh --level REACTOR_MAN --room room12 --heartTanks all --healthTanks all --difficulty hard`:
  quicker rows; 3 balls in the juggle.

## Behaviors to check

- Death and respawn in a mini-boss room: the fight restarts in phase 1 with the intro.
- Death of the player while a ball is in the air: no ball is left behind after the respawn.

## Intended changes

- 2 health phases, the Mad Beat, the intro, the death sequence (no explosions, no fade), the throw deck with mixed
  aims, the Two-Ball Juggle, the enraged ball and face, Hot Potato.
- No damage to him in `ENTRANCE`.
- Every ball explodes at his defeat (today a thrown ball can stay).
- New art: the mohawk on `stand` and `throw`, and all the new strips.
- Damage to him 2 × `master` (1-for-1 hits to kill).
- Damage to the player moves to `PlayerDamageNegotiator`, with the `master` values.

## Must not change

- The ball drops in from above his head (`ball_spawn_y`, default 8 tiles).
- The mini-boss flow of `FightPlans.miniBossPlan` (gate, no bar, no boss music) and the Heart Tank at `onDestroy`.
- `ReactorMonkeyBall` damage to the player for other owners (none today).
