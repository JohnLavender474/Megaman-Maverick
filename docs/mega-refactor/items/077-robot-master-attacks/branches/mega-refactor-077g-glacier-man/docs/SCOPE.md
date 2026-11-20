# Scope: mega-refactor-077g-glacier-man

What this branch changes and why, and its contract. The fight design was drafted on `master` (2026-11-19) before the
art branch `077f`; the scan at branch start (2026-11-19) added the current state, the parts, the objective check and
the contract.

- Status: approved
- Approved: 2026-11-19 (brief https://claude.ai/artifact/48Cs2LR9WkMbzS9dPjUoAm)

## Goal

A richer Glacier Man fight: less predictable, less repetitive, more varied. 2 health phases, smarter choice of moves,
dedicated throw animations for the ice cubes, a phase-2 Icicle Rain and Sled Ram, and a freeze-and-shatter death. The
same class is the Glacier Man stage boss and the `WILY_STAGE_2` refight; both get the new fight.

## Item

`077` (one branch per boss; a boss with health phases gets its harder attacks in its last phase in place of a panic
attack; damage values 2 × `master` on 2 phases, so the hits to kill stay 1-for-1). Merge target `master`.

## Owner input (2026-11-19, planning conversation)

- The fight is dull because it is predictable, repetitive and simple. It was the owner's first boss fight, made by
  hand. It is time for an upgrade.
- Direction: keep his moveset and identity, add a smarter choice of moves, and add new moves (option B of the
  planning conversation).
- No "frozen floor" attack: the floor and the walls of the arena are already ice.
- Icicles: they form on the ceiling, shake, then drop.
- Ice cubes: dedicated animations for the chunk (lob) and the aim (straight throw), in place of the plain launch
  from the hands.
- Icicle Rain start: Glacier Man pounds the ground with his fist, and the icicles form on the ceiling (owner,
  2026-11-19, `077f` art review: in place of a big ice chunk).
- Death: freeze and shatter. He freezes solid in a block of ice, cracks spread, the block bursts into shards.
- Not objected to (proposals in the conversation): phase 2 also adds Sled Ram; the `FIRED` stun on fire hits stays;
  damage table 2 × `master`; the `WILY_STAGE_2` refight gets the same fight.

## Current state (`master` `bfd12356d`)

- `entities/bosses/GlacierMan.kt` (711 lines). States `INIT`, `STAND` (1 s, 1 snowball at 0.5 s), `DUCK` (0.75 s, 1
  snowball), `SLED` (1.5 s, 2 snowballs), `BRAKE`, `STOP` (0.8 s), `JUMP`, `ICE_BLAST_ATTACK` (4 s, 6 cubes, 12 in
  hard), `FIRED` (1 s stun after a fire hit).
- Choice (`buildStateMachine`, lines 461-503): the guard `StateMachine` takes the first true guard from `STAND`: ice
  blast 50 % (not twice in a row), duck 60 % (player not above), sled (player more than 2.5 tiles away), else jump.
  The order of the guards makes the pattern easy to read. `DUCK` and `SLED` chain by fixed guards.
- Ice cubes (`iceBlast`, lines 581-614; `SmallIceCube`): a coin flip between a lob (`calculateJumpImpulse` at the
  player's center) and a straight shot at the player's center. Both aim at the current position only. The hands
  alternate. One strip (`ice_blast_attack`, 2 frames).
- Snowballs (`shoot`, lines 618-652; `Snowhead`, type `BLANK`): straight or up, fixed velocities, no aim.
- 1 health phase. No tuning rows. Timers are built once per spawn in `buildTimers` (difficulty read there).
- Death: the `defeated` strip (3 frames) and the `AbstractBoss` explosions (`explodeOnDefeat`, `defeatTimer`).
- Allocations: `canIceBlast` builds a `gdxArrayOf` per call, `shoot` a `Vector2()` per shot, `getIceBlast*Pos`
  return the `body.getCenter()` scratch.
- Unused art: `brake_flash`, `fall_flash`. New `077f` art, not loaded yet: `chunk`, `aim`, `aim_up`, `pound`,
  `freeze`, `frozen_crack`, `frozen_shatter` (`Bosses1`) and `FallingIcicle/form` (`Projectiles2`). Frame counts,
  release frames and spawn points are in the `077f` `docs/NOTES.md` "Final frame list".
- Damage to the boss: `BOSS_DMG_NEG` plus overrides fire 4 (`Fireball`, `MagmaWave`, `MagmaFlame`) and
  `ReactorManProjectile` 3.
- Damage to the player: no `PlayerDamageNegotiator` entry for Glacier Man or his damagers, so the fallbacks apply:
  the body, `Snowhead` and `FallingIcicle` are `Size.MEDIUM` (3); `SmallIceCube` is `Size.MEDIUM` (3) and also freezes.
- `FallingIcicle` (`projectiles/`, 244 lines): states `STILL` (drops when a player is under it), `SHAKE` (0.25 s),
  `FALL`; it shatters into `IceShard.spawn5`. Callers: `GapingFish`, `IcicleTelly`. No map spawns it. It has no
  `form` animation and no timed drop.
- Lead aim: `OrbAim` (`bosses/bigassmaverickrobot/`, `LEAD`, `CURRENT`, `NEAR`, `WIDE`) is the `077` mixed aim. Its
  callers are `BigAssMaverickRobot` and `ElecDevil`.
- Tuning pattern: `DesertManTuning` (`bosses/desertman/`), one row per difficulty and phase. Test rig: `BossRig.kt`,
  `desertman/DesertManRig.kt` (a scripted `GameRandom` picks the move).
- Arenas (16 × 14 rooms, all ice):
  - `GlacierMan_16x14_v2.tmx` `boss_room`, boss object 445 (`wall_1` 402, `wall_2` 406). The room before it with a
    player spawn is `room12`. Ceiling bottom at row 3 in the middle (x 3 to 13), row 4 at the sides.
  - `WilyStage2_v2.tmx` `glacier_man_room`, boss object 457. The room before it with a player spawn is `room8`. 2
    `FloatingCanHole` enemies at row 2 (x 2 and 13), near the ceiling.
- Test map: `RefactorTest077.tmx` (`REFACTOR_TEST_077`) has the rooms `elec_devil` and `elec_devil_boss` (`077c`).

## In scope (the fight design)

### Phase 1: the current moves, made less predictable

1. Weighted choice of the next move in place of the first-true-guard order: each move has a weight, the last move
   gets a low weight, and a move cannot run 3 times in a row. The weights depend on the distance to the player and on
   the player's height. The pick uses `services.random`.
2. Chains: some moves lead into a set follow-up, for example sled → jump over the player → shoot back, or duck →
   snowball → sled.
3. Tells: a short pose before each attack. The flash frames (`brake_flash`, `fall_flash`) mark the start of a fast
   move.
4. Variable timing: the stand time and the shot times get a small random range, so the rhythm is not fixed.
5. Ice cubes, with the `077f` art:
   - **Chunk:** `chunk` (release frame 2): he winds up and lobs a cube overhand in an arc.
   - **Aim:** `aim` or `aim_up` (release frame 2): he points, the cube forms in his hand, and he throws it straight.
   - Mixed aim with `OrbAim`: in a volley, one cube leads the player, one aims at the current position, one lands
     near the player, and one lands at a wider random spot.
6. Snowballs: some aim at the player in place of the fixed velocities.

### Phase 2: harder, with new moves

7. `healthPhases = 2`, with a second bar region. `onHealthPhaseStart(2)`: a short ice-power pose
   (`ice_blast_attack` and a flash), a sound and a screen shake. Quicker tells and shorter stands from the tuning rows.
8. **Icicle Rain:** `pound` (impact frame 2; `SHAKE_CAM`). Icicles form along the ceiling (`form` → `still` →
   `shake` → drop), one after the other or in a wave. One icicle forms over the player's lead position, the others
   scatter. A gap always stays safe. The icicles shatter on the floor. The ceiling line comes from the room (a map
   property or a scan of the blocks; decided in `PLAN.md`).
8b. **Panic finisher (owner D3):** under 25 % of the phase-2 health, Icicle Rain comes as a double rain (2 waves,
    each with its own safe gap).
9. **Sled Ram:** a fast sled across the arena. At the wall he bounces back and shoots snowballs both ways.

### All phases

10. Death: freeze and shatter: `freeze` → `frozen_crack` → `frozen_shatter` with `IceShard` pieces at frame 0, then
    the standard boss explosion and defeat flow.
11. Damage to the boss: 2 × the `master` values (1-for-1 hits to kill over 2 phases).
12. Damage to the player: one entry per damager of this fight in `PlayerDamageNegotiator` (the `077` rule).
13. Tuning rows per difficulty and per phase (`GlacierManTuning`, the `077b` pattern).
14. The `WILY_STAGE_2` refight runs the same fight.
15. Test rooms `glacier_man` (player spawn) and `glacier_man_boss` (a copy of the stage arena with the boss) in
    `RefactorTest077.tmx`, built with the `level-builder` skill before the code.
16. Fight tests on the `077b` rig after the moveset is final (`docs/TEST-DEBT.md` until then).

### New API and its first caller

| New API | First caller |
|---|---|
| `GlacierManTuning` (rows per difficulty and phase) | `GlacierMan` |
| `FallingIcicle` `FORM` state and a timed drop (spawn props) | `GlacierMan` Icicle Rain |
| `PlayerDamageNegotiator` entries for the fight's damagers | `PlayerDamageNegotiator.get` |

## Out of scope

- The art itself (`077f`, merged).
- A redesign of the arenas.
- New sounds, unless the owner asks for them.
- A move of `OrbAim` out of `bigassmaverickrobot/` (a backlog candidate).
- The state model of `015` (`O4-R3` waits for it).

## Objective check

| Rule | Check |
|---|---|
| Always-on rules not listed | no effect |
| `O1-R4` Typed state, not new property keys | no effect: the icicle form and delay use existing keys or typed spawn values |
| `O1-R5` Code only with a production caller | no effect: each new API has a caller (table above) |
| `O2-R2` Random only from the seeded source | helps: every new pick uses `services.random` |
| `O2-R5` No logic assumes one player | no effect: aims use `targetPlayer`; icicles drop on a timer, not on one player |
| `O5-R1` No per-frame allocation | helps: the rewritten aim and spawn code uses owned fields or pools |
| `O6-R2` Named checks | helps: fight tests on the rig |
| `O4` | helps (richer fight) |
| `O4-R1` Content from data | no effect: the arena stays as it is |
| `O4-R2` A test room for each mechanic | helps: `glacier_man` rooms in `RefactorTest077.tmx` |
| `O4-R3` State machines | waiting (`015`): the fight keeps its own state model |

## Decisions (owner, 2026-11-19, brief)

- D1: the 2 `FloatingCanHole` near the ceiling of `WilyStage2_v2.tmx` stay as they are; icicles do not skip their
  columns, and can form inside them. No map change.
- D2: damage to the player keeps the `master` values (3 for the body, `Snowhead`, `SmallIceCube`, `FallingIcicle`);
  only the table moves to `PlayerDamageNegotiator`.
- D3: yes: under 25 % of the phase-2 health, Icicle Rain comes as a double rain (item 8b).
- D4: hits to kill 1-for-1 with `master`: each boss damage value is 2 × `master`.

## Overlap with other branches

See `NOTES.md`.

# Contract

Approved 2026-11-19 with the scope.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with the new Glacier Man fight tests.
- [ ] `./gradlew core:smokeTest` passes; `entity-tags.txt` unchanged or updated.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-077g-glacier-man`. Experimental
checks first. Each boss check adds `--heartTanks all --healthTanks all`.

| Level | Room | What to check |
|---|---|---|
| `REFACTOR_TEST_077` | `glacier_man` | Phase 1: varied move order, tells, chunk and aim throws, mixed aim, aimed snowballs |
| `REFACTOR_TEST_077` | `glacier_man` | Phase 2: phase-start tell, Icicle Rain with a safe gap, Sled Ram bounce; under 25 % the double rain |
| `REFACTOR_TEST_077` | `glacier_man` | `--weapons all`: hits to kill equal to `master` per weapon; fire stun; freeze-and-shatter death |
| `REFACTOR_TEST_077` | `glacier_man` | `--difficulty hard`: quicker timings, 12-cube volleys |
| `GLACIER_MAN` | `room12` | The real fight from the gate: intro, both phases, defeat, stage exit |
| `WILY_STAGE_2` | `room8` | The refight: both phases; icicles next to the `FloatingCanHole` enemies (no skip, D1) |

## Behaviors to check

- Death and respawn in the boss room: the fight restarts in phase 1.
- `GapingFish` and `IcicleTelly` icicles act as on `master` (unit test on `FallingIcicle`).

## Intended changes

- 2 health phases; the move choice, the throws, the snowball aim, Icicle Rain, the double rain under 25 % of phase 2,
  Sled Ram and the death above.
- Damage to the player moves to `PlayerDamageNegotiator`, with the `master` values (D2).
- Damage to the boss is 2 × `master` (D4).
- Defeat (owner, play round 1, 2026-11-20): no defeat explosions; the orb ring and its sound start at the shatter,
  drawn under his frames; the fight ends 1 s after the shatter.
- Sled Ram fires no snowballs (owner, play round 1).

## Must not change

- The intro, the health fill, and the end of the fight after the defeat time (the `AbstractBoss` flow).
- The `FIRED` stun on a fire hit.
- `FallingIcicle` with no new spawn props (the `GapingFish` and `IcicleTelly` icicles).
