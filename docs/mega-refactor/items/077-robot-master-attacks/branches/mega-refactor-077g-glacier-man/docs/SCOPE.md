# Scope: mega-refactor-077g-glacier-man

What this branch changes and why, and its contract. Draft written ahead of time on `master` (2026-11-19), before the
branch exists. It is the fight design that the art branch `077f` draws for. It still goes through the brief at branch
start.

- Status: draft
- Approved: no

## Goal

A richer Glacier Man fight: less predictable, less repetitive, more varied. 2 health phases, smarter choice of moves,
dedicated throw animations for the ice cubes, a phase-2 Icicle Rain and Sled Ram, and a freeze-and-shatter death. The
same class is the Glacier Man stage boss and the `WILY_STAGE_2` refight; both get the new fight.

## Item

`077` (one branch per boss; a boss with health phases gets its harder attacks in its last phase in place of a panic
attack; damage values 2 × `master` on 2 phases, so the hits to kill stay 1-for-1).

## Owner input (2026-11-19, planning conversation)

- The fight is dull because it is predictable, repetitive and simple. It was the owner's first boss fight, made by
  hand. It is time for an upgrade.
- Direction: keep his moveset and identity, add a smarter choice of moves, and add new moves (option B of the
  planning conversation).
- No "frozen floor" attack: the floor and the walls of the arena are already ice.
- Icicles: they form on the ceiling, shake, then drop.
- Ice cubes: dedicated animations for the chunk (lob) and the aim (straight throw), in place of the plain launch
  from the hands.
- Icicle Rain start: Glacier Man lobs a big ice chunk up; it bursts on the ceiling, and the icicles form there.
- Death: freeze and shatter. He freezes solid in a block of ice, cracks spread, the block bursts into shards.
- Not objected to (proposals in the conversation): phase 2 also adds Sled Ram; the `FIRED` stun on fire hits stays;
  damage table 2 × `master`; the `WILY_STAGE_2` refight gets the same fight.
- Art first: the art branch `077f` draws the new frames, then this branch codes the fight on the final art.

## Current state (`master` `f21e2a53a`)

- `entities/bosses/GlacierMan.kt` (711 lines). States `INIT`, `STAND` (1 s, 1 snowball at 0.5 s), `DUCK` (0.75 s, 1
  snowball), `SLED` (1.5 s, 2 snowballs), `BRAKE`, `STOP` (0.8 s), `JUMP`, `ICE_BLAST_ATTACK` (4 s, 6 cubes, 12 in hard),
  `FIRED` (1 s stun after a fire hit).
- Choice: the guard `StateMachine` takes the first true guard from `STAND`: ice blast 50 % (not twice in a row), duck
  60 % (player not above), sled (player more than 2.5 tiles away), else jump. The order of the guards makes the
  pattern easy to read.
- Ice cubes (`SmallIceCube`): each one is a coin flip between a lob (`calculateJumpImpulse` at the player's center) and
  a straight shot at the player's center. Both aim at the current position only. The hands alternate. The animation is
  1 strip (`ice_blast_attack`, 2 frames, sparkles at the hands).
- Snowballs (`Snowhead`, type `BLANK`): straight or up, fixed velocities, no aim.
- 1 health phase. No tuning rows. No tells except the 0.5 s stand before the first snowball.
- Death: the `defeated` strip (3 frames) and the `AbstractBoss` explosions.
- Unused art: `brake_flash`, `fall_flash` (white flash frames).
- Damage to the boss: `BOSS_DMG_NEG` plus overrides fire 4 (`Fireball`, `MagmaWave`, `MagmaFlame`) and
  `ReactorManProjectile` 3. Damage to the player: no `PlayerDamageNegotiator` entry for `Snowhead`, `SmallIceCube` or the
  body.
- Arenas (16 × 14 rooms, all ice):
  - `GlacierMan_16x14_v2.tmx` `boss_room` 480, boss 445 (`wall_1` 402, `wall_2` 406). Inner width 14 tiles. Floor top
    at row 12; ceiling bottom at row 3 in the middle (x 3 to 13) and row 4 at the sides. About 8 to 9 tiles of height.
  - `WilyStage2_v2.tmx` `glacier_man_room` 438, boss 457 (`boss_key` 2, `end` false). Inner width 14 tiles. Floor top
    at row 11, ceiling bottom at row 2. 2 `FloatingCanHole` enemies at row 2 (x 2 and 13), near the ceiling.
- Reusable: `AbstractBoss.healthPhases` and `onHealthPhaseStart` (`077a`), the per-difficulty, per-phase tuning rows
  (`DesertManTuning`, `077b`), the boss test rig (`BossRig.kt`, `desertman/DesertManRig.kt`), `FallingIcicle`
  (`still`, `shake`, `shatter`; states `STILL`, `SHAKE`, `FALL`), `IceShard`, `IcicleShards`.

## In scope (the fight design)

### Phase 1: the current moves, made less predictable

1. Weighted choice of the next move in place of the first-true-guard order: each move has a weight, the last move
   gets a low weight, and a move cannot run 3 times in a row. The weights depend on the distance to the player and on
   the player's height.
2. Chains: some moves lead into a set follow-up, for example sled → jump over the player → shoot back, or duck →
   snowball → sled.
3. Tells: a short pose before each attack. The flash frames (`brake_flash`, `fall_flash`) mark the start of a fast
   move.
4. Variable timing: the stand time and the shot times get a small random range, so the rhythm is not fixed.
5. Ice cubes, with the new art of `077f`:
   - **Chunk:** he winds up and lobs a cube overhand in an arc.
   - **Aim:** he points, the cube forms in his hand, and he throws it straight.
   - Mixed aim (the `077` rule): in a volley, one cube leads the player (the position in about 1 to 2 s from the
     current velocity), one aims at the current position, one lands near the player, and one lands at a wider random
     spot.
6. Snowballs: some aim at the player in place of the fixed velocities.

### Phase 2: harder, with new moves

7. `healthPhases = 2`. `onHealthPhaseStart(2)`: a short ice-power pose (`ice_blast_attack` sparkles and a flash), a
   sound and a screen shake. Quicker tells and shorter stands from the tuning rows.
8. **Icicle Rain:** he lobs a big ice chunk straight up. It bursts on the ceiling. Icicles form along the ceiling
   (form → still → shake → drop), one after the other or in a wave. One icicle forms over the player's lead position,
   the others scatter. A gap always stays safe. The icicles shatter on the floor.
9. **Sled Ram:** a fast sled across the arena. At the wall he bounces back and shoots snowballs both ways.

### All phases

10. Death: freeze and shatter. Ice grows over him from the feet to a full block, cracks spread, and the block bursts
    into shards (with `IceShard` pieces), then the standard boss explosion and defeat flow.
11. Damage to the boss: 2 × the `master` values (1-for-1 hits to kill over 2 phases).
12. Damage to the player: one entry per damager of this fight in `PlayerDamageNegotiator` (the `077` rule), at the
    `master` values unless the owner sets new ones.
13. Tuning rows per difficulty and per phase (`GlacierManTuning`, the `077b` pattern).
14. The `WILY_STAGE_2` refight runs the same fight.

## Out of scope

- The art itself (branch `077f`).
- A redesign of the arenas.
- New sounds, unless the owner asks for them.

## Open questions (for the brief at branch start)

- Icicle Rain on a ceiling with enemies near it (`WilyStage2_v2.tmx`, the 2 `FloatingCanHole` at row 2): keep them,
  move them, or let the icicles skip their columns.
- Damage to the player per damager (keep the `master` values, or new values).
- Whether the phase-2 Icicle Rain also comes back as a panic finisher at low health.

# Contract

Written at branch start.
