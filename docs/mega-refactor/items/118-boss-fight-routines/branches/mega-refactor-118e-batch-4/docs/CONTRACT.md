# Contract: mega-refactor-118e-batch-4

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-05

## Intended changes

1. After a mini boss is ready, and after its defeat, `TURN_CONTROLLER_ON` comes 1 frame earlier. So the first free input frame comes 1 frame earlier (owner, 2026-11-05). This is the same kind of change as the `118b` mid-level resume.
2. `PenguinMiniBoss` gets its `mini` value from the map only. On a map with no `mini`, it is not mini. No live map has it.
3. `Bospider` is commented out and can no longer spawn. No live map has it. An old map that names it logs a spawner error.

## Behavior that must not change

### The 5 live mini fights

The same flow as today:

- The controller is off in the intro, and comes back when the boss is ready (1 frame earlier, change 1).
- No health bar, no freeze of the world and no music pause. The level music goes on through the fight, and no boss music starts.
- At the defeat: no input lock, no damage lock and no music stop. The boss projectiles and other `IBossListener`s go. No orbs.
- After the defeat animation, the exit gate with the key of this boss opens, Megaman can move, and the level goes on.
- A death in the fight, a re-entry after a respawn, and slot 0 dead at the defeat behave as today.
- `MoonHead`: its `AsteroidsSpawner` goes at the defeat.
- `MechaDragon`: the block over the hole spawns as today.
- `ReactorMonkey`: the `FloatingCan`s go at the defeat in `mini_boss_room_2`. In `alt_path_1_mini_boss_room_1`, the heart tank `e` drops if Megaman does not have it.
- A mini fight in a level that is already defeated still runs, as today.

### All other fights

- The 19 fights on the new flow since `118a` to `118d` play as after `118d`. No map sets `mini` for them.
- `WilyFinalBoss`, the one fight that stays on the old flow, has no change.
- No boss AI, `AbstractBoss` hook, `BossHealthHandler`, `Gate`, `FightDirector` or map file changes. The steps of `standardBossPlan` and `midBossPlan` do not change.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.

Permanent tests:

- [ ] `FightPlans.find`: a plan for each moved boss (16 with the 4 of this branch), and `null` for each other boss. `Bospider` is in neither list.
- [ ] `FightPlans.find`: `miniBossPlan` for `mini=true`, with no `end`, `end=true` and `end=false`, for each of the 4 bosses.
- [ ] `miniBossPlan`: the exact intro and outro steps, in order, and no music.
- [ ] `FightPlans.find`: with no `mini`, or `mini=false`, a moved boss still gets `standardBossPlan` or `midBossPlan` from `end`.
- [ ] `EndBossSpawn` submits `END_BOSS_SPAWN`.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118e-batch-4`

Experimental checks:

- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate and beat `MoonHead`. No bar, no freeze, the level music goes on, the asteroids stop, the next gate opens and Megaman can move.
- `./run-game-alpha.sh --level INFERNO_MAN --room room8`: drop through the hole and beat `MechaDragon`. As above. The hole is shut while the fight runs.
- `./run-game-alpha.sh --level REACTOR_MAN --room room12`: go through the gate and beat `ReactorMonkey`. As above. The `FloatingCan`s go at the defeat.

Control check:

- `./run-game-alpha.sh --level WILY_STAGE_1 --room room9`: go through the gate and beat `DesertMan`, a mid-level fight. Bar, freeze, defeat lock, then the gate opens, Megaman can move, and the level music plays.

## Behaviors to check

- The 2 `ReactorMonkey` fights after `room5` use the same plan and code as the `room12` fight, so they have no separate play check. The heart tank drop is in `ReactorMonkey.onDestroy`, which the branch does not change.
- Death and respawn in a fight: the plan holds no lease, and `PLAYER_SPAWN_INIT` cancels the fight as in the other batches. No separate play check.
- `PenguinMiniBoss`: tests only, because no player can reach its fight.
