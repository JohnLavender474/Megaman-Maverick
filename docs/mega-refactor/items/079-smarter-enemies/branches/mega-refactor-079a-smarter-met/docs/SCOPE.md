# Scope: mega-refactor-079a-smarter-met

- Status: approved
- Approved: 2026-11-18

## Goal

A smarter, more dynamic `Met`. It does not run into a death pit. It jumps when a jump is safe and useful. A charged
shot stuns it and knocks its helmet off (the helmet flies up and lands back); the 3rd charged hit breaks the helmet, and the `Met` goes desperate. A
hard-mode `Met` seeks the player more and dodges some shots. The ground check is a shared helper that other walking
enemies can use.

## Item

`079` (smarter behavior for simple enemies), objective `O4`. This branch takes the `Met` part of the item:

> For `Met`: a ledge check (for example a sensor in front of his feet) so that he stops or turns at the edge of a
> bottomless pit. Other walking enemies can use the same check if two or more need it.

The owner widened it on 2026-11-18 (verbatim request):

> It should avoid falling into a death pit using a helper function that other entities (enemies) can reuse. Being hit
> by a half-charged Mega Buster shot should cause it to stop in place if it's running and its helmet should
> momentarily "bounce up" and make the enemy momentarily exposed. The same should happen if it's hit by a
> fully-charged shot, but the effect should be slightly more dramatic, and 3 hits by a charged shot causes the helmet
> to break apart (use the "break apart" effect seen with X6 enemies such as Junkroid), causing the `Met` to go into
> its "desperate" phase of running and shooting nonstop, taking only short breaks to stop. Also, the `Met` should be
> able to jump and be able to calculate if and when it can safely make a jump and if it's advantageous.

Owner answers on 2026-11-18, before the brief:

- A charged hit on a `Met` that has its helmet does no damage: it staggers the `Met` (stop, helmet up, a short exposed
  window). Plain bullets keep today's rules.
- Claude draws the new art as drafts in `temp/079a-met/`, for the owner's approval.
- One shared world-probe helper (`GroundProbe`), not fixture sensors.
- Jump rules as proposed. A hard-mode `Met` is smarter: it seeks the player and dodges some of the player's attacks.
- Merge target: `master`.

## Current state

Scan of `master` at `cf4a1ab49`.

- `Met` (`entities/enemies/Met.kt`, 358 lines): 3 behaviors (`SHIELDING`, `POP_UP`, `RUNNING`) in a `when` with
  hand-run timers (`Met.kt:199-252`). It runs for 0.5 s at 8 tiles/s; it notices a pit only when `FEET_ON_GROUND`
  turns false, then falls with the helmet down (`Met.kt:229-232`). Fixtures: `FEET` plus full-body `BODY`,
  `DAMAGER`, `DAMAGEABLE`, `SHIELD`; `SHIELD` is active only while shielding, `DAMAGEABLE` only while not
  (`Met.kt:289-290`). It supports 4 gravity directions (`Met.kt:281-286`).
- Charged shots: `ChargedShot.fullyCharged` (`ChargedShot.kt:75`) tells half from full. On the helmet, a charged shot
  bounces (`ChargedShot.kt:166-176`) and never reaches `Met` code; `Receivers.HIT_BY_PROJECTILE` on the `SHIELD`
  fixture is called first (`ProjectileRules.kt:36`; precedent `LumberJoe.kt:285`). On an exposed `Met`, a full charge
  does 30 of 30 health and a half charge 15 (`StandardDamageNegotiator.kt:112-135`).
- Break effect: `FallingPiece.throwDebris(keys, position, random)` with hand-drawn debris regions
  (`decorations/FallingPiece.kt:73`; `Junkroid.kt:208-217`).
- Ground: no shared ledge helper. About 8 enemies copy a `CONSUMER` foot-sensor pair (`RedKoopa`, `DarkSerket`,
  `BunbyTank`, `RatRobot`, `DrillTankXT`, `FireMet`, `SpikeBot`, `Darspider`). `WarpSpotTest.kt:58-145` has the
  "solid ground, no `DEATH`" rules, for players only. Nothing predicts a jump landing.
- A pit is a gap with a `Death` (`FixtureType.DEATH`) rectangle in the `sensors` layer; `PlayerDeath` has no fixture
  and only kills players.
- Art: `Enemies1` has `Met/frozen`, `GetUp`, `LayDown`, `PopUp`, `Run` and the `SnowMet/*` set. No no-helmet, helmet
  popped up, helmet piece or jump art.
- Use in live levels: Desert Man 7, `Gauntlet1_v2` 24 (wave enemies), test maps; `GutsTank` (Wily 2) spawns
  `run_only` `Met`s (`GutsTank.kt:480`); `RefactorTest051` has 2 `run_only` `Met`s. No live map uses `type` or
  `direction`.

## In scope

1. **`GroundProbe`** (`entities/utils/`, new): world queries through `levelState.worldContainer`, in the local frame
   of a gravity `Direction`. "Is there solid ground under this rectangle within N tiles, and no `DEATH` above it?" and
   "does this rectangle overlap a solid?" The solid rules follow `WarpSpotTest` (static, collision on, not
   `COLLIDE_UP_ONLY` for ground, block filters), for any `IBodyEntity`. First caller: `Met`.
2. **`JumpPlanner`** (`entities/utils/`, new): samples a jump arc (gravity, velocity clamp, body size) against
   `GroundProbe`, for a short list of candidate jumps, and returns the first jump whose path is clear and whose
   landing is safe ground. First caller: `Met`.
3. **`Met` states** on the engine `StateMachine` (`EnumStateMachineBuilder`): `SHIELDING`, `POP_UP`, `RUNNING`,
   `STUNNED`, `HELMET_LAND`, `JUMPING`, `DESPERATE_RUN`, `DESPERATE_PAUSE` (and the frozen override as today).
4. **Pit avoidance** while running (normal and desperate): before each step, `GroundProbe` checks the ground ahead. A
   `DEATH` area or no ground within 4 tiles below: the `Met` stops, then jumps (if a jump rule applies) or turns. Safe
   ground lower down: it may walk off.
5. **Jumps** (normal mode): across a gap or pit toward the player when the far side is safe; up a step or a wall that
   it can clear; to a higher platform when the player is there and in reach.
6. **Charged-shot stun** while the helmet is on (changed in round 1, owner, 2026-11-18): a `ChargedShot` on the
   helmet or the body does no damage. On the helmet it bounces with the dink (owner, D1); on the exposed body it
   bursts (Claude); on an exposed `Met` it does its normal damage, and a `Met` that survives is stunned (owner,
   round 1). The helmet becomes its own entity `MetHelmet` (`entities/hazards/`: `DYNAMIC` body with `BODY`,
   `SHIELD`, `FEET`, `HEAD`; no damage to the player, owner, round 1). A block on its head cancels its upward speed (then gravity); a block
   under its feet makes it rest. When its `Met` dies, it falls free and rests, and is culled off camera. It pops up (apex 1.5 tiles for a half
   charge, 3 tiles for a full charge; no knockback, owner round 1), spins in 45-degree steps, follows the head and falls back onto it. The `Met` is
   `STUNNED` (dizzy, spinning stars, plain shots hurt it) until the helmet lands, within a minimum and a maximum
   time (derivation in `NOTES.md`); then `HELMET_LAND` (pushed down and back up), then `SHIELDING`. When the helmet
   rests away from the head (after `T_min`, or still flying at `T_max`), the `Met` runs to fetch it, worried
   (`FETCH_HELMET`, no shots, jumps allowed), picks it up (`PICKUP_HELMET`) and shields. When it cannot get it (way
   blocked, helmet culled, 4 s timeout), it cries (`CRY`, 1 s), releases the helmet and goes desperate. Helmets
   change ownership: the hit count lives on the helmet, and a helmetless `Met` that touches a free helmet puts it on
   (owner, round 1). The airborne
   helmet's shield deflects shots; a charged hit on it counts toward the 3.
7. **Helmet break** on the 3rd charged hit in a spawn (either charge level): the helmet breaks into `FallingPiece`
   debris (where it is, also in the air); the `Met` takes no damage from that hit and goes desperate (a stunned `Met`
   first waits for the minimum stun time).
8. **Desperate phase:** no helmet, always damageable, runs toward the player faster, does not shoot (owner, round 1), and
   pauses for about 0.3 s about every 2 s. It keeps pit avoidance and checks jumps more often.
9. **Hard mode** (`DifficultyMode.HARD`): the `Met` turns toward the player before each run and runs longer; it uses
   the jump rules to reach the player's height; when a player shot comes at it while it is exposed, it dodges some of
   them (chance from `services.random`): it drops the helmet, or hops over the shot if the hop is safe.
10. **Art** (drafts in `temp/079a-met/` first, owner approval, then `Enemies1`): no-helmet run and shoot, helmet popped
    up (small and large), 3-4 helmet pieces, jump; the `SnowMet` set as a palette copy. README credit lines as in
    `docs/ways-of-working/art.md`.
11. **Test map** `RefactorTest079.tmx` with `REFACTOR_TEST_079`: one room per behavior (see `CONTRACT.md`).
12. Tests: `GroundProbe` and `JumpPlanner` unit tests; `Met` reaction tests with `testServices()` (charged hit, 3rd
    hit, plain bullet), written when the owner calls the behavior final (`docs/ways-of-working/gameplay.md`).

## Out of scope

- Other enemies (`Starkner` and others): later `079` branches. Moving the 8 copied foot-sensor pairs or
  `WarpSpotTest` to `GroundProbe`: backlog candidate.
- `run_only` `Met`s (`GutsTank`, `RefactorTest051`): they keep today's behavior, so the Wily 2 fight does not change.
- Damage of plain bullets, the `Met` health, the `Met` bullet.
- The `ChargedShot` rules for other enemies.

## Affected areas and risks

- `Met.kt` (rewrite of the behavior part), new `GroundProbe.kt`, `JumpPlanner.kt`, `Enemies1` atlas and frames, new
  test map and `LevelDefinition` entry, `README.md` credits, smoke-test data if a tag or region changes.
- Risk: a `Met` in a `Gauntlet1` wave that jumps or turns away could slow a wave; it still dies, so no lock.
- Risk: the 4 gravity directions. No live map uses a turned `Met`, but the code must keep them; one test room checks
  a turned `Met`.
- Risk: per-frame world queries. `GroundProbe` runs only while a `Met` runs or plans a jump, over a few grid cells,
  with pooled shapes (`O5-R1`).

## Overlap with other branches

Checked on 2026-11-18: `compactor-level`, `fix/projectiles-ignore-death`, `mega-refactor-047k-gate-gather`,
`mega-refactor-060k-protoman-frame-edits`, `mega-refactor-077c-elec-devil`. None touch `Met`, `ChargedShot`,
`ProjectileRules`, `Enemies1` or `FallingPiece`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` Components, not new inheritance | no effect | Helpers are plain utilities; no new base class. |
| `O1-R4` Typed state | helps | Helmet hits and states are typed fields, no new property keys. |
| `O1-R5` Code only with a production caller | no effect | `GroundProbe` and `JumpPlanner` have `Met` as caller. |
| `O2-R2` Random from the seeded source | no effect | Dodge chance from `services.random`. |
| `O2-R5` No logic assumes one player | no effect | Seek and dodge use `targetPlayer` and check the shots of every player. |
| `O2-R7` Gameplay does not depend on rendering | no effect | Debris is a decoration; the stun is a state; the helmet entity moves in its own updatable. |
| `O4-R1` Mechanics from Tiled | no effect | No new map mechanic; existing `Met` properties stay. |
| `O4-R2` A test room for each mechanic | helps | `RefactorTest079`, one room per behavior. |
| `O4-R3` Patterns as states | waiting | After `015`; the branch already uses the engine `StateMachine`. |
| `O5-R1` No per-frame allocation | no effect | Probes use pooled shapes and owned fields. |
| `O6-R2` Named checks | helps | New unit tests and the contract checks. |

## Brief answers (owner, 2026-11-18)

- D1: a charged shot on the helmet bounces off with the dink, as today, and counts toward the 3 hits. Claude's call:
  a charged shot on the exposed body of a helmeted `Met` bounces the same way.
- D2: `SnowMet` gets a palette copy of each new `Met` frame; same behavior for both looks.
- D3: a hard-mode `Met` dodges about 1 plain shot in 3 that it sees coming while exposed.

## Open questions

None.
