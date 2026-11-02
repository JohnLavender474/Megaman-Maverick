# Contract: mega-refactor-118a-fight-routine

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-02

## Intended changes

1. The `BigAssMaverickRobot` fight runs on a `Fight`, not on the screen's event branches. The player must see no difference. This is a change of structure only.
2. Pause: the fight tick does not run during a pause. So a `BigAssMaverickRobot` flow step that an old handler ran in `events.run()` in the frame just before a pause now waits for resume. Example: `BOSS_DEFEATED` in the frame of a focus-loss pause. Then the input lock and the music stop come after resume. A pause by button is blocked during the intro and the defeat, because the input is locked (`LevelPauseRules.kt`), so this case needs a pause in the same frame as the defeat or a focus-loss pause.
3. The music of this fight has 1 source: `FightPlans.resolve`. `BigAssMaverickRobot.kt:211` goes. The same track plays.

## Behavior that must not change

### The `BigAssMaverickRobot` fight

Intro:

- The controller stays off from the room transition until the bar fill ends.
- The boss falls, shakes and raises its hands. The bar does not start before `isReady` is true.
- When the boss is ready, the music pauses, all systems stop except sprites and animations, and the bar fills from 0 to full.
- When the fill ends, the same music resumes, all systems turn on, the controller turns on, `betweenReadyAndEndBossSpawnEvent` clears, and the boss music starts.
- The boss deals no damage before `ready`.

Defeat:

- At 0 health, `BOSS_DEFEATED` is submitted, and `Spike` and `PreciousGem` still receive it.
- The input locks with the controller system left on, all players get the `DAMAGE/BOSS_DEFEATED` lock, the music stops, `IBossListener` entities react, and the bar goes.
- The boss explodes for its defeat time, spawns its orbs and is destroyed.
- `VICTORY_EVENT` follows, and the level ends as today, with the same next screen.

Failure:

- If slot 0 is dead at `BOSS_DEFEATED` or at the end of the defeat time, the flow does nothing, as today.
- A death in the fight respawns the player at the last spawn. The boss is destroyed on `PLAYER_SPAWN`, the bar goes, all systems are on, and a new entry to the room starts a full new fight.
- If the level is already defeated, a new entry to the room gives `VICTORY_EVENT` with no boss spawn.

### All other fights

- The other 24 live fights take the old flow, with no change in any screen branch.
- `BOSS_READY`, `BOSS_DEAD`, `BEGIN_BOSS_SPAWN`, `END_BOSS_SPAWN`, `BOSS_DEFEATED` and `INTERMEDIATE_BOSS_DEAD` keep their senders and their listeners outside the screen.
- No boss AI, `AbstractBoss` hook, `BossHealthHandler`, `Gate`, `DeathRule` or map file changes.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] CI passes on the branch.

### Temporary contract suite

`BossFlowContractTest`, with `FrozenBossFlow`, a frozen copy of the 6 screen branches at `MegaLevelScreen.kt:691-804` in the test sources. It runs the frozen flow and `Fight` on the same scripted frames, with a real `EventsManager`, mock audio, a fake systems list and a fake boss. It compares an ordered trace for each frame: each system's `on` value, controller events, audio calls, submitted events, locks and bar state. The 2 traces must be equal.

Rows:

- [ ] Normal fight: intro, fill, fight, defeat, victory.
- [ ] Level already defeated: `VICTORY_EVENT` with no spawn.
- [ ] Slot 0 dead at `BOSS_DEFEATED`.
- [ ] Slot 0 dead at `BOSS_DEAD`.
- [ ] `PLAYER_SPAWN` during the intro.
- [ ] `PLAYER_SPAWN` during the fill.
- [ ] `PLAYER_SPAWN` during the fight.
- [ ] A game over with a fight in progress: `reset()` cancels it.

Both the suite and the frozen copy are deleted in the last commit before the merge.

### Permanent suite

- [ ] `Routine`: steps run in order, one at a time, and `end` runs once for each started step, with `interrupted` set on a cancel.
- [ ] `Fight`: each lease is released once at close, in reverse order, on a normal end and on a cancel.
- [ ] `FightPlans.find`: it gives a plan for `BigAssMaverickRobot` and `null` for each other boss in `entities/bosses/`.
- [ ] `FightPlans.resolve`: it writes the music value, and it runs once for each fight.
- [ ] Each old screen branch acts for a boss with no fight and skips a boss that a fight owns.

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118a-fight-routine`

Experimental checks:

- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: go through the gate. The boss falls, shakes and raises its hands. Megaman cannot move. The bar fills as the music pauses. When it is full, the music comes back and Megaman can move at once.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: beat the boss. The input stops at the killing hit, the music stops, the bar goes, the boss explodes, orbs appear, and the level ends on the same next screen as today.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: die in the fight. Megaman respawns, the bar is gone, and nothing stays frozen. Go through the gate again. A full new intro plays with a full bar.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: pause during the fight, and resume. The fight continues, and the bar and the music are correct.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: click out of the game window during the defeat explosions, then click back. The defeat continues, and the level ends.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --players 2`: beat the boss with 2 players. Both players stop taking damage at the killing hit, and the level ends once.

Control checks:

- `./run-game-alpha.sh --level TIMBER_WOMAN --room room14`: go through the gate. Stage boss on the old flow: intro, bar, fight, defeat and level end as today.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate. `MoonHead`, a mini boss on the old flow: no bar, no freeze, and the next gate opens after the defeat.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room6`: go through the gate. `MoonMan`, a mid-level boss on the old flow: the gate opens after the defeat, and the level music returns.

## Behaviors to check

- Death and respawn in the fight: see the third experimental check.
- Pause: see the fourth and fifth experimental checks.

The ordered log diff in `118` `ITEM.md`, "Behavior check", needs a baseline play. This branch drops it, because the frame trace covers the same order and the owner's play test runs once.
