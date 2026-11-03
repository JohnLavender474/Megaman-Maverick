# Contract: mega-refactor-118c-batch-2

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-03

## Intended changes

1. The 10 fights of `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan` and `PreciousWoman` run on a `Fight`, not on the screen's event branches. Apart from change 2, the player must see no difference.
2. After a mid-level fight of these 5 bosses, the `BOSS_DEFEATED` damage lock goes and the level music starts 1 frame earlier, and the first free input frame comes 1 frame earlier. This is the `118b` intended change, with the same research (`118b` `NOTES.md`).

## Behavior that must not change

### The 5 end-of-level fights

The same flow as in `118a` and `118b`: the intro with the controller off, the bar fill with the music paused and the world held, control at the end of the fill, the boss music, the defeat lock, the music stop, the listeners, the bar removal, the defeat explosions and orbs, and `VICTORY_EVENT` with the same next screen. A death in the fight, a re-entry, a defeated level and slot 0 dead at the defeat behave as today.

- `GlacierMan` plays `MMX6_BOSS_FIGHT_MUSIC`, from its map prop, as today.

### The 5 mid-level fights

- The intro, the bar, the fight and the defeat are the same as in an end-of-level fight.
- After the defeat time, `INTERMEDIATE_BOSS_DEAD` goes out once, and the gate with the matching `boss_key` opens.
- The controller comes back, both players lose the `BOSS_DEFEATED` damage lock, and the level music plays again, once.
- The level does not end.
- If slot 0 is dead at the defeat or at the end of the defeat time, nothing happens until the level reset cancels the fight.
- A re-entry to the room after a death starts a full new fight.
- The defeat listeners act as today: `BulbBlaster` (Wily 1), `PipePuff` and the `FloatingCan` hazards (Wily 2), and `Spike` (Wily 3) stop or go. The `DesertMan` room in Wily 1 keeps its darkness and light.

### Boss defeat side effects

`RodentMan` destroys its `RatRobot` children, and `PreciousWoman` destroys its shield gems, as today.

### All other fights

- The 7 fights on the new flow since `118a` and `118b` play as after `118b`.
- The 8 fights that stay on the old flow (`MoonHead`, `MechaDragon`, `ReactorMonkey` ×3, `ElecDevil`, `GutsTank`, `WilyFinalBoss`) have no change.
- No boss AI, `AbstractBoss` hook, `BossHealthHandler`, `Gate`, `DeathRule`, `FightDirector`, step or map file changes.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.

Permanent tests:

- [ ] `FightPlans.find`: a plan for each of the 9 moved bosses, and `null` for each other boss.
- [ ] `FightPlans.find`: no plan music for the 5 new bosses; `midBossPlan` for `end=false`, and `standardBossPlan` for `end=true` and for no `end`.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118c-batch-2`

Experimental checks:

- `./run-game-alpha.sh --level GLACIER_MAN --room room12`: go through the gate and beat `GlacierMan`. Intro, bar, fight, defeat and level end as today, with the same boss music.
- `./run-game-alpha.sh --level DESERT_MAN --room room16`: go through the gate and beat `DesertMan`. As above.
- `./run-game-alpha.sh --level INFERNO_MAN --room room11`: go through the gate and beat `InfernoMan`. As above.
- `./run-game-alpha.sh --level RODENT_MAN --room room16`: go through the gate and beat `RodentMan`. As above, and the `RatRobot`s go at the defeat.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room7`: go through the gate and beat `PreciousWoman`. As above, and the shield gems go at the defeat.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room9`: go through the gate and beat `DesertMan`. The room is dark with its light as today, the `BulbBlaster`s stop at the defeat, the gate opens, Megaman can move, the level music plays, and the level goes on. Go through the next gate.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room4`: go through the gate and beat `RodentMan`. The `PipePuff`s stop at the defeat. The gate opens, Megaman can move, the level music plays, and the level goes on.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room8`: go through the gate and beat `GlacierMan`. The `FloatingCan`s stop at the defeat. The gate opens, Megaman can move, the level music plays, and the level goes on.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room4`: go through the gate and beat `InfernoMan`. The gate opens, Megaman can move, the level music plays, and the level goes on.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room6`: beat `MoonMan`, then `PreciousWoman`, then go straight through the gate into the `TimberWoman` fight and beat it. Each fight has its intro and bar, the `Spike`s act at the `PreciousWoman` defeat as today, and after each fight the gate opens, Megaman can move, and the level music plays. Nothing stays frozen, and no old bar stays on the screen in the `TimberWoman` fight. `room6` is the nearest room with a player spawn before the `PreciousWoman` fight.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room4`: die in the `RodentMan` fight. Megaman respawns, the bar is gone, and nothing stays frozen. Go through the gate again. A full new fight plays.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room8 --players 2`: beat `GlacierMan` with 2 players. Both players can be hurt again after the fight, both can move, and the music plays once.

Control checks:

- `./run-game-alpha.sh --level WILY_STAGE_1 --room room6`: go through the gate and beat `ReactorMan` (new flow since `118b`). As after `118b`.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room10`: go through the gate and beat `ElecDevil` (old flow). Intro, bar, fight, defeat and level end as today.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room9`: go through the gate and beat `GutsTank` (old flow). As above.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate and beat `MoonHead`, a mini boss on the old flow. No bar, no freeze, and the next gate opens.

## Behaviors to check

- Two new-flow fights in a row: see the `WILY_STAGE_3 room6` experimental check.
- Death and respawn in a mid-level fight: see the `WILY_STAGE_2 room4` death check.
- 2 players after a mid-level fight: see the `--players 2` check.
