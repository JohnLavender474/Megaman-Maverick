# Contract: mega-refactor-118d-batch-3

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-04

## Intended changes

None. The 2 live fights of `ElecDevil` and `GutsTank` run on a `Fight`, not on the screen's event branches, and the player must see no difference. `SigmaRat` gets a plan, but no live level has its fight.

## Behavior that must not change

### The 2 live fights

The same flow as in `118a` to `118c`: the intro with the controller off, the bar fill with the music paused and the world held, control at the end of the fill, the boss music, the defeat lock, the music stop, the listeners, the bar removal, the defeat explosions and orbs, and `VICTORY_EVENT` with the same next screen. A death in the fight, a re-entry, a defeated level and slot 0 dead at the defeat behave as today.

- `ElecDevil` plays `JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC`.
- `GutsTank` plays `MMX6_SIGMA_2_BATTLE_INTRO_MUSIC`, and then its loop track.
- The bar fills only after the boss is ready: for both bosses, after all players are on the ground and the boss timer ends.
- At the defeat, the `ElecDevil` body pieces and the projectiles of both bosses go, and `GutsTank` destroys its fist, its tank block, the Mets and the HeliMets.
- The Wily 1 darkness and its light stay as today in the `ElecDevil` room.

### All other fights

- The 17 fights on the new flow since `118a` to `118c` play as after `118c`.
- The 6 fights that stay on the old flow (`MoonHead`, `MechaDragon`, `ReactorMonkey` ×3, `WilyFinalBoss`) have no change.
- No boss AI, `AbstractBoss` hook, `BossHealthHandler`, `Gate`, `FightDirector`, step or map file changes.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.

Permanent tests:

- [ ] `FightPlans.find`: a plan for each of the 12 moved bosses, and `null` for each other boss.
- [ ] `FightPlans.resolve`: `ElecDevil` and `GutsTank` write their music to the spawn props. `SigmaRat` has no plan music.
- [ ] `FightPlans.find`: `standardBossPlan` for the 3 bosses with no `end` and with `end=true`.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118d-batch-3`

Experimental checks:

- `./run-game-alpha.sh --level WILY_STAGE_1 --room room10`: go through the gate and beat `ElecDevil`. Intro, bar, fight, defeat and level end as today, with the same boss music. The room stays dark with its light as today.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room9`: go through the gate and beat `GutsTank`. As above. The intro track goes on into the loop track, and the fist, the Mets and the HeliMets go at the defeat.

Control check:

- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate and beat `MoonHead`, a mini boss on the old flow. No bar, no freeze, and the next gate opens.

## Behaviors to check

- Death and respawn in a fight: the code is the same as in the `118b` and `118c` fights. No separate play check.
- `SigmaRat`: tests only, because no player can reach its fight.
