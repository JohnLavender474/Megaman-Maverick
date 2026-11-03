# Contract: mega-refactor-118b-batch-1

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-03

## Intended changes

1. The 6 fights of `TimberWoman`, `MoonMan` and `ReactorMan` run on a `Fight`, not on the screen's event branches. Apart from change 2, the player must see no difference.
2. After a mid-level fight of these 3 bosses, the `BOSS_DEFEATED` damage lock goes and the level music starts 1 frame earlier (before `engine.update`, not after it), and the first free input frame comes 1 frame earlier. The research in `NOTES.md` found no adverse effect. The same pause rule as in `118a` applies: the fight tick does not run during a pause.
3. Names: `stageBossPlan` is `standardBossPlan`. No behavior changes for `BigAssMaverickRobot`.

## Behavior that must not change

### The 3 end-of-level fights

The same flow as the `118a` contract: the intro with the controller off, the bar fill with the music paused and the world held, control at the end of the fill, the boss music, the defeat lock, the music stop, the listeners, the bar removal, the defeat explosions and orbs, and `VICTORY_EVENT` with the same next screen. A death in the fight, a re-entry, a defeated level and slot 0 dead at the defeat behave as in `118a`.

### The 3 mid-level fights

- The intro, the bar, the fight and the defeat are the same as in an end-of-level fight.
- After the defeat time, `INTERMEDIATE_BOSS_DEAD` goes out once, and the gate with the matching `boss_key` opens.
- The controller comes back, both players lose the `BOSS_DEFEATED` damage lock, and the level music plays again, once.
- The level does not end. No `VICTORY_EVENT`.
- If slot 0 is dead at the defeat or at the end of the defeat time, nothing happens, as today, until the level reset cancels the fight.
- A re-entry to the room after a death starts a full new fight.

### All other fights

- `BigAssMaverickRobot` plays as after `118a`.
- The other 18 live fights take the old flow, with no change in any screen branch. The screen's `INTERMEDIATE_BOSS_DEAD` branch acts for them as today.
- No boss AI, `AbstractBoss` hook, `BossHealthHandler`, `Gate`, `DeathRule` or map file changes.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.

Permanent tests:

- [ ] `FightPlans.find`: a plan for `BigAssMaverickRobot`, `TimberWoman`, `MoonMan` and `ReactorMan`, and `null` for each other boss; `midBossPlan` for `end=false`, and `standardBossPlan` for `end=true` and for no `end`.
- [ ] `FightPlans.resolve`: writes no `MUSIC` prop for a plan with no music.
- [ ] `midBossPlan`, slot 0 alive: sends `INTERMEDIATE_BOSS_DEAD` and no `VICTORY_EVENT`; then sends `TURN_CONTROLLER_ON` once, removes the `BOSS_DEFEATED` lock, and plays the level music once; the fight stays open until the boss is fully gone.
- [ ] `midBossPlan`, slot 0 dead at the defeat: sends nothing, does not resume, and stays open until a cancel.
- [ ] `standardBossPlan`: sends `VICTORY_EVENT`, and does not resume.
- [ ] The screen's `INTERMEDIATE_BOSS_DEAD` branch skips a boss that a fight owns, and acts for a boss with no fight.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118b-batch-1`

Experimental checks:

- `./run-game-alpha.sh --level TIMBER_WOMAN --room room14`: go through the gate and beat `TimberWoman`. Intro, bar, fight, defeat and level end as today.
- `./run-game-alpha.sh --level MOON_MAN --room room14`: go through the gate and beat `MoonMan`. As above, and the gravity is normal after the fight.
- `./run-game-alpha.sh --level REACTOR_MAN --room room14`: go through the gate and beat `ReactorMan`. As above.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room6`: go through the gate and beat `ReactorMan`. The gate opens, Megaman can move, the level music plays, and the level goes on. Go through the next gate.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room6`: go through the gate and beat `MoonMan`. The same checks as above, and the gravity is normal after the fight. Go on, beat `PreciousWoman` (old flow), then beat `TimberWoman`, with the same checks.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room6`: die in the `ReactorMan` fight. Megaman respawns, the bar is gone, and nothing stays frozen. Go through the gate again. A full new fight plays.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room6 --players 2`: beat `ReactorMan` with 2 players. Both players can be hurt again after the fight, both can move, and the music plays once.

Control checks:

- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: go through the gate and beat `BigAssMaverickRobot`. As after `118a`.
- `./run-game-alpha.sh --level GLACIER_MAN --room room12`: go through the gate and beat `GlacierMan`, an end-of-level fight on the old flow. As today.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room9`: go through the gate and beat `DesertMan`, a mid-level fight on the old flow. The gate opens, Megaman can move, and the level music plays.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate and beat `MoonHead`, a mini boss on the old flow. No bar, no freeze, and the next gate opens.

## Behaviors to check

- Death and respawn in a mid-level fight: see the sixth experimental check.
- 2 players after a mid-level fight: see the seventh experimental check.
