# Contract: mega-refactor-118f-final-boss

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: draft
- Approved: no

## Intended changes

1. In a defeated `WILY_STAGE_3`, `wily_room` spawns `WilyFinalBoss`. It no longer sends `VICTORY_EVENT` (owner, 2026-11-02). Normal play cannot reach this case (inference, item `NOTES.md`, fact 14).
2. If slot 0 is dead when the Wily defeat animation ends, the bar goes at that moment, because the fight closes. Today it stays until the respawn (owner, 2026-11-05).

## Behavior that must not change

### The Wily fight

- The intro: the init swoop waits until Megaman is on the ground. Then the bar fills with the music paused and the world held, and control comes back at the end of the fill.
- The boss track `OUR_BLOOD_BOILS_8_BIT` plays after the fill. No second track starts.
- Phases 1 and 2: at 0 health, the phase transition runs, the health refills with no bar fill, and the fight goes on. No defeat lock and no bar removal.
- Phase 3 defeat: the input lock, the damage lock, the music stop, the listeners, the bar removal and the defeat explosions. No orbs.
- The end scene: `DrWily` spawns, Megaman faces Wily and cannot move, and the game goes to the ending screen.
- A death in the fight, and a re-entry after a respawn, behave as today.

### All other fights

- The 24 fights on the new flow since `118a` to `118e` play as after `118e`.
- In a defeated level, a stage boss room still sends `VICTORY_EVENT` and spawns no boss. A mid-level or a mini-boss room still spawns its boss.
- No boss AI, `AbstractBoss` hook, `BossHealthHandler`, `Gate`, `FightDirector`, `DrWily` or map file changes. The steps of the other plans do not change.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.

Permanent tests:

- [ ] `FightPlans.find`: `finalBossPlan` for `WilyFinalBoss` with no props, `end=true`, `end=false` and `mini=true`, with its exact intro and outro steps.
- [ ] `FightPlans.resolve`: `WilyFinalBoss` writes `OUR_BLOOD_BOILS_8_BIT` to the spawn props.
- [ ] `FightPlans.find`: a plan for each moved boss (17), and `null` for each other boss.
- [ ] `FightPlan.endsLevel`: `true` for `standardBossPlan`, `false` for `midBossPlan`, `miniBossPlan` and `finalBossPlan`.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118f-final-boss`

Experimental check:

- `./run-game-alpha.sh --level WILY_STAGE_3 --room room8`: drop down through the next room into the boss room, and beat `WilyFinalBoss` through all 3 phases. Intro, bar and boss music as today; each phase change refills the health with no bar fill and no lock; at the phase 3 defeat, the lock and the explosions; then `DrWily` and the ending screen. (`room9`, the room before the boss room, has no player spawn.)

Control check:

- `./run-game-alpha.sh --level GLACIER_MAN --room room12`: go through the gate and beat `GlacierMan`. Intro, bar, fight, defeat and level end as today, with the same boss music.

## Behaviors to check

- The level check in a defeated level: `endsLevel` gives the old result for each plan, and a test checks each plan. The dev options cannot start a level as defeated, so it has no play check.
- Death and respawn in the fight: the code is the same as in the other batches, and `PLAYER_SPAWN_INIT` cancels the fight. No separate play check.
