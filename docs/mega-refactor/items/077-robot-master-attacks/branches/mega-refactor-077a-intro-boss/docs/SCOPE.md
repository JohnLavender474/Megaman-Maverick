# Scope: mega-refactor-077a-intro-boss

What this branch changes and why. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-13 (owner, brief D1 to D4)

Scan of 2026-11-13 on `035e5dbec`. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/` unless
they start with `assets/`, `docs/`, `engine/` or `core/`. The designs (state machines, health phases, the bar,
tuning rows) are in `NOTES.md`.

## Goal

Make the `INTRO_STAGE` boss (`BigAssMaverickRobot`) a richer fight, and add health phases that every boss can use.
The player sees: a second health phase with a two-color bar, hands that move in more than one pattern and attack in
three ways with clear tells, orbs that aim in a mix of ways, floor shock waves, a longer death, and more damage per
buster shot.

## Item

`077` (`O4`). From its `ITEM.md`:

- "For each boss: design the new attacks, the panic attack, and how the boss chooses them." For this boss the
  panic attack is replaced by phase 2 (decision of 2026-11-13: "a boss with health phases gets its harder attacks in
  its last phase in place of the panic attack under 25%").
- "Aiming: in an attack that fires several shots, not every shot aims at the player's current position. Mix the
  aims: one shot leads the player (where the player will be in about 1-2 seconds, from the current position and
  velocity), one aims at the current position, one lands at a random spot near the player, and one lands at a wider
  random spot to distract."
- "Death: replace the simple cartoon death (for example the X eyes of `GutsTank` and of `BigAssMaverickRobot`, the
  `INTRO_STAGE` boss) with a richer dying animation for each boss."
- "A shared piece (the panic phase start, a lead-aim helper, a death-sequence step in the `118` fight routines) goes
  in the first branch that needs it." This branch adds the shared health phases. The lead aim stays in this boss's
  package until a second boss needs it (`O1-R5`).
- Decision 2026-11-13 (owner): "a shared 'overdrive' mechanism for every fight: the boss health bar has more than
  one phase. A boss with 2 phases has 2 x `MAX_HEALTH` bits. A lost bit takes the color of the next phase in place of
  going empty, and at 0 the bar rolls over to the next phase at once, with no rest time."
- Decision 2026-11-13 (owner, `BigAssMaverickRobot`): pellet 1 -> 2, half charge 2 -> 3, full charge 3 -> 4; the
  body that stays on screen is on purpose; keep the circular orbit, add new motions with a proper state machine.

## Current state

| Part | Today | Where |
|---|---|---|
| Health | one `Points` 0..30; a points listener calls `triggerDefeat()` at 0 | `entities/contracts/AbstractBoss.kt:214-225` |
| Damage path | `takeDamageFrom` -> negotiator -> `editDamageFrom` -> `translateHealth` -> hit lock 1.25 s | `entities/contracts/AbstractHealthEntity.kt:80-101`, `AbstractBoss.kt:59` |
| Bar | `BossHealthHandler` builds one `BitsBar` (one region, `standard`) and fills 30 bits in 2.25 s while the world is held | `screens/levels/stats/BossHealthHandler.kt:37-65`, `drawables/ui/BitsBar.kt`, `fights/FightPlans.kt:82-101` |
| Boss damage map | own `StandardDamageNegotiator`: pellet 1, charge 2/3, charge explosion 1/2; every other `ISizable` player shot falls to the MEDIUM size table (Axe 30, Needle 20, Moon Scythe 15, Magma Wave 15, Precious Gem 15, Reactor shot 10/20, Frigid Shot 5); `MagmaExplosion`, `ReactorExplosion`, `IceBombExplosion`, `MagmaFlame` do 0 | `entities/bosses/BigAssMaverickRobot.kt:148-160`, `damage/StandardDamageNegotiator.kt:67-101`, `:151-164` |
| Hands | 6 states; `ROTATE` is a mirrored circle (160 to 256 degrees/s); one attack (a punch with a 1 s still tell, a 1 s wait and a 4 tiles/s return); the boss picks the hand (76/101 nearest) | `entities/bosses/BigAssMaverickRobotHand.kt:101`, `:257-350`; `BigAssMaverickRobot.kt:485-510` |
| Orbs | a timed volley (2 normal / 4 hard); each orb aims at the player at spawn and then holds 1 to 0.25 s, so it misses a moving player | `BigAssMaverickRobot.kt:247-264`, `:606-649`; `entities/projectiles/BigAssMaverickRobotOrb.kt:157-162` |
| Shock wave art | `BigAssMaverickRobotOrbExplosion` has no caller; its pulse is sliced 1x3 on a 2-frame strip | `entities/explosions/BigAssMaverickRobotOrbExplosion.kt:137-141` |
| Death | X-eyes frame, explosions in the head for 3 s, hands fall and blink, all 8 arm orbs pop at once, then the orb ring; the body stays (on purpose) | `AbstractBoss.kt:251-256`, `BigAssMaverickRobot.kt:410-428`, `:563-567`; `Hand.kt:222-229`; `fights/FightSteps.kt:118-128` |
| Tests | `BigAssMaverickRobotHandTest` (block spawn); no test of boss health or of this damage map | `core/src/test/.../bosses/BigAssMaverickRobotHandTest.kt` |

Problems: the hands move in one fixed pattern; one hand attack; the orbs miss a moving player by design; the
charge is no better per hit window than a pellet after pellet 2; the Axe kills in 1 hit; the death is a frame swap.

## In scope

1. **Shared health phases** on `AbstractBoss` (`healthPhases`, default 1, so no other boss changes; `healthPhase`;
   `healthBarRegions`; `onHealthPhaseStart`; `translateHealth` and `getHealthRatio` overrides). Overshoot carries
   over; the bar rolls over at once, no rest; defeat only at 0 in the last phase.
2. **The bar:** new `BossBitsBar`; `BossHealthHandler` takes the boss and draws 2 runs; 1 new bit region in `Bits`
   (art files, packed atlas).
3. **Damage on this boss:** pellet 2, half charge 3, full charge 4, charge explosion 2/3, and an explicit value for
   each special weapon (Q3). The map gets a base that gives an unlisted player weapon a boss value, not a size-table
   value.
4. **Hand motion and state machine:** boss motion modes `ORBIT`, `HOVER`, `BLEND`; hand states for idle, punch,
   slam, double clap, return, defeat; the boss chooses the target, the attack and the hand; tuning rows per
   difficulty and phase. Keep: no attack with a hand that a player stands on; the platform follows the hand in every
   state.
5. **Mixed-aim orbs:** lead, current, near, wide; the aim is set when the orb starts to move.
6. **Slam shock waves** with `BigAssMaverickRobotOrbExplosion` (fixed slicing).
7. **Phase 2 for this boss:** `healthPhases = 2`, the phase-2 tuning row, the double clap, the start cue.
8. **Richer death** (4 s): chains pop one by one, hands bounce, head shakes and sinks, a last explosion. The body
   stays.
9. **Co-op targets:** the boss alternates the target player in slot order for hand attacks and orbs.
10. **Tests:** health phases, bar runs, damage map, aim points, motion slots, hand transitions.
11. **README** for the new boss package (`entities/bosses/bigassmaverickrobot/`), as `d1000/README.md`.

## Out of scope

- Health phases on any other boss (each is a later `077` branch). `D1000` keeps its own half-health overdrive.
  `WilyFinalBoss` keeps its scripted phases (1 health phase).
- A panic attack for this boss (replaced by phase 2).
- A shared lead-aim helper and a shared death step in `fights/`: no second caller yet (`O1-R5`).
- New hand or head art (open palm, clap, cracked head). The new art is only the bit region.
- Moving `Proto Man`'s values into this map before `060c` merges (see `NOTES.md` "Overlap").
- The panic attack questions in `ITEM.md` "Open questions" for single-phase bosses.
- Item `015` (`Fsm<S>`), item `011` (seeded random), item `112` (targeting policies).

## New API and first caller

| API | First production caller |
|---|---|
| `AbstractBoss.healthPhases`, `healthPhase`, `healthBarRegions`, `onHealthPhaseStart` | `BigAssMaverickRobot`; `BossHealthHandler` |
| `AbstractBoss.translateHealth`, `getHealthRatio` overrides | the damage path; 23 ratio reads |
| `BossBitsBar` | `BossHealthHandler.set` |
| Tuning row class, motion slot functions, aim function (boss package) | `BigAssMaverickRobot`, `BigAssMaverickRobotHand` |
| Hand commands (`punch`, `slam`, `clap`) | `BigAssMaverickRobot` attack choice |
| Orb launch-aim interface on the owner | `BigAssMaverickRobotOrb` (called), `BigAssMaverickRobot` (implements) |

## Affected areas and risks

| Part | Files changed | New files | Call sites |
|---|---|---|---|
| Health phases | `AbstractBoss.kt` (287 lines) | 1 test | `translateHealth`: 3 production sites reach the override (`AbstractHealthEntity.kt:91`, `D1000.kt:434`, `:844`); `getHealthRatio`: 23 sites in 8 classes, same value with 1 phase; 18 live `AbstractBoss` subclasses, 0 change |
| Bar | `BossHealthHandler.kt` (72), `assets/sprites/sprite_sheets/Bits.png`, `Bits.txt` | `drawables/ui/BossBitsBar.kt`, 1 frame in `assets/sprites/frames/Bits/`, 1 test | `BossHealthHandler.set`: 1 caller (`FightSteps.kt:48`), text unchanged; `FightStepsTest` mock unchanged |
| Damage | `BigAssMaverickRobot.kt:148-160` | 1 test | 1 map, about 20 entries |
| Hands and attacks | `BigAssMaverickRobotHand.kt` (454; states, update, platform and damager rules, anim key), `BigAssMaverickRobot.kt` (651; hand spawn, `isReady`, attack choice), `BigAssMaverickRobotHandTest.kt` (spawn props) | 3 to 4 files in `entities/bosses/bigassmaverickrobot/` (tuning, motion, aim, README), 2 tests | 2 hands per boss |
| Orbs | `BigAssMaverickRobot.shootOrb` (`:606-649`), `BigAssMaverickRobotOrb.kt` (233; spawn, update, `onBossDefeated`) | aim in the boss package | 7 other orb fetch sites keep the default path (`GutsTank.kt:409`, 5 in `WilyFinalBoss.kt`, the arm orbs) |
| Shock waves | `BigAssMaverickRobotOrbExplosion.kt` (145) | none | 1 new caller (the hand) |
| Death | `BigAssMaverickRobot.kt`, `BigAssMaverickRobotHand.kt` | none | `defeatDur` 3 -> 4 s: `AwaitBossDefeatEnd` waits 1 s more |

Total: 6 main files changed, 4 to 5 main files added, 3 art files, 1 test changed, 5 to 6 tests added.

Risks:

- The roll-over runs inside the world step (contact). The hook only sets values and queues spawns; a boss reads the
  new phase at its next decision. A boss that does more in the hook could break the step order.
- The platform with a rider: every hand motion now moves by velocity, and `RETURN` and `REST` carry a rider. A rider
  on a fast spring could slip off. Play test.
- The fight gets harder for an intro boss. The tuning rows are first values; the play test sets them.
- Merge with `060c`: a text conflict in `BigAssMaverickRobot.kt` (imports, damage map). Simple to resolve.

## Overlap with other branches

`060c` (`mega-refactor-060c-protoman-buster`): `BigAssMaverickRobot.kt` (damage map, imports), `AbstractBoss.kt`
(`BOSS_DMG_NEG`, imports), `StandardDamageNegotiator.kt` (size tables, no change here). Text conflicts only. The
second branch to merge sets `ProtoBuster` to 3 on this boss (owner). Details: `NOTES.md` "Overlap with other
branches".

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | no effect | waits for `096`; no new global; the hand reads its boss, not a global |
| `O1-R2` Components, not new inheritance (preference) | no effect | no new base class. The phase state goes on `AbstractBoss`, which already owns the health points and the defeat rule; a component would split the defeat rule over two places |
| `O1-R3` No new fragile order | no effect | the roll-over runs in the damage path; the hook only sets state; a boss acts on it at its next decision (the `D1000` pattern) |
| `O1-R4` Typed state, not new property keys | helps | the hand reads typed values from its boss (tuning row, target point) in place of new supplier props; the orb aim kind is a typed value |
| `O1-R5` Code only with a production caller | no effect | each new API has a caller ("New API and first caller"); no shared lead-aim helper or death step |
| `O2-R1` Input only through `InputFrame` | no effect | no input read |
| `O2-R2` Random only from the seeded source | waiting | after `011` (`planned`). +3 random call lines (near, wide, side), -2 (hand choice), through `UtilMethods.getRandom` as today |
| `O2-R3` One fixed tick | waiting | after `010`. Timers in seconds with `delta`, as today |
| `O2-R4` Stable order; players in slot order | helps | target alternation in slot order |
| `O2-R5` No logic assumes one player | helps | orbs and hand attacks alternate over the live players; `isBeingStoodUpon` checks every player (today) |
| `O2-R6` No logic assumes one camera | no effect | waits for `094`; only the existing `SHAKE_CAM` event |
| `O2-R7` Gameplay does not depend on rendering or audio | no effect | the head sink and shake are draw offsets of a defeated boss (no damage fixtures act); hit boxes follow the bodies |
| `O2-R8` Local-only data out of the simulation | no effect | none |
| `O2-R9` Events and progress name a slot | no effect | `BOSS_DEFEATED` unchanged; no new event |
| `O3-R1` No JVM-only API on the web path | no effect | waits for `032`; plain Kotlin math |
| `O5-R1` No per-frame allocation in common paths | no effect | pooled vectors in the motion and aim math; `BossBitsBar` makes its sprites once at `init` |
| `O5-R2` Measure hot paths | no effect | no hot path changes (one boss, one bar) |
| `O6-R1` CI passes | no effect | the contract runs the CI tasks |
| `O6-R2` Named checks | helps | new tests for the phases, the bar runs, the damage map, the aim and the hand states |
| `O7-R3` No level-identity checks (preference) | no effect | none |
| `O8-R2` Capabilities, not character checks | no effect | targets are `IPlayer`; no character check |
| `O9-R1` NES-style pad | no effect | no input change |
| `O9-R2` Saves stay loadable | no effect | waits for `007`; boss health is not saved |
| `O4-R1` Mechanics from Tiled | no effect | health phases are a boss property in code, not a level mechanic; the boss room data does not change |
| `O4-R2` A test room for each mechanic | no effect | no new level mechanic. The checks use the real boss room through `--room room12` (the room with the boss gate), which reaches the fight at once; a test map would copy the same room and its 12 child objects |
| `O4-R3` Patterns as states | waiting | after `015`. The hand and the motion modes are explicit enum states with one entry function and one state clock (the `015` target shape); see `NOTES.md` "State machine tool" |

## Owner answers (2026-11-13)

- D1 (Q1): a) one new shared red-orange region for phase 2 of every boss.
- D2 (Q2): c) the phase-2 color shows under the bar from the start; the phase-1 bits fill over it in 2.25 s.
  Replaced by the owner decision of 2026-11-15: the fill runs in passes, one per phase, last phase first.
- D3 (Q3): a) the table below: each special weapon 2, the Axe 3, the big Reactor shot 4, explosions 1 to 2.
- D4 (Q4): a) one branch; health phases and the bar first in the plan, then the damage, then the boss.

## Open questions (answered, see above)

For the brief (2 to 4 decisions):

**Q1. Phase-2 bit color.**
- a) One new shared region for phase 2 of every boss, red-orange (NES `#D82800` outer, `#FC9838` inner). (recommended:
  one art file now; each later boss reads the same color, so the player learns "orange = second phase")
- b) A region per boss, chosen per boss.
- c) Reuse the `fire` weapon region, no new art (it then looks like Infernal Barrage energy).

**Q2. Intro fill with 2 phases** (the world is held during the fill).
- a) Fill only the phase-1 bits, 2.25 s; the phase-2 color shows after the first hit.
- b) Fill the phase-2 color, then phase 1 over it: 4.5 s, and 2.25 s more of held world on each retry.
- c) Show the phase-2 color under the bar from the start and fill the phase-1 bits over it: 2.25 s. (recommended:
  the player sees the second phase at once, and the fill time stays as today)

**Q3. Special-weapon damage on this boss** (today the size table: Axe 30, Needle 20, Moon Scythe 15, ...).

| Weapon | Proposal |
|---|---|
| Frigid Shot (`SmallIceCube`; `IceBombExplosion`) | 2; 1 |
| Infernal Barrage (`MagmaWave`, `Fireball`; `MagmaExplosion`, `MagmaFlame`) | 2; 1 |
| Moon Scythes (`MoonScythe`) | 2 |
| Precious Guard (`PreciousGem`; `PreciousShard`) | 2; 1 |
| Axe Swinger (`Axe`) | 3 |
| Needle Spin (`Needle`) | 2 |
| Rodent Claws (`SlashWave`) | 2 |
| Reactor Shot (`ReactorManProjectile` small / big; `ReactorExplosion`) | 2 / 4; 2 |

- a) The table above: each special weapon equal to the pellet (2), the slow Axe 3, the big Reactor shot equal to a
  full charge, explosions 1 to 2. (recommended: no weakness on an intro boss, and no one-hit kill)
- b) Every special weapon 1 (the `BOSS_DMG_NEG` default): weaker than the pellet.
- c) A weakness: one weapon 4 (for example Moon Scythes), the others 2.

**Q4. One branch or two.**
- a) One branch, in plan steps: health phases and the bar first, then the damage, then the boss. (recommended: the
  phase API needs this boss as its first caller (`O1-R5`), and the play test of phase 2 needs the new attacks)
- b) Two branches: `077a` health phases, the bar and the damage (with the boss at 2 phases and the old attacks);
  `077b` the hands, the orbs, the shock waves and the death.

Not for the brief (Claude's proposals, tuned at the play test): the charge explosion values (2/3), the tuning rows,
the phase-2 cue, the death timeline, the co-op alternation. The `060c` sabre value on this boss (half 3, full 4) is a
note for the second branch to merge.
