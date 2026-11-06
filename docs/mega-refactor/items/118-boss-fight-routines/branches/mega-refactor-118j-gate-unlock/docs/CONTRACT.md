# Contract: mega-refactor-118j-gate-unlock

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-06

## Intended changes

None that a player can see. A `Gate` with both `boss_key` and `gauntlet_key` now throws at spawn; no live map has one.

## Behavior that must not change

- After a mid-level or mini boss is defeated, the exit gate with its `boss_key` unlocks in the same frame as today, and only that gate.
- After the last wave of a `GauntletMaster`, the exit gate with its `gauntlet_key` unlocks in the same frame as today, and only that gate.
- A gate with a key stays locked until then. A gate with no key is unlocked, as today. The `trigger` map property still sets the start value.
- After a player death, a resettable gate with a key is locked again, as today.
- If slot 0 is dead when a mid-level or mini fight exits, no gate unlocks, as today.
- A stage-end fight and the final boss unlock no gate, as today.
- The gate states, sounds, timers and room transitions, the gauntlet waves, the fight steps and their order stay as today.
- No map file changes.

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] CI passes on the branch, if the owner pushes it.
- [ ] `grep` finds no `INTERMEDIATE_BOSS_DEAD`, `GAUNTLET_CLEARED`, `bossKey` or `NO_BOSS_KEY` in live code.

Permanent tests:

- [ ] A `Gate` with `boss_key` unlocks on `UNLOCK_GATE` with that key.
- [ ] A `Gate` with `gauntlet_key` unlocks on `UNLOCK_GATE` with that key.
- [ ] A `Gate` with a key stays locked on `UNLOCK_GATE` with another key.
- [ ] A resettable `Gate` that was unlocked is locked again on `PLAYER_SPAWN`.
- [ ] A `Gate` with both properties throws at spawn.
- [ ] `FightPlans.find` puts the boss map `boss_key` in the plan of a mid-level and of a mini fight, and no key when the map has none.
- [ ] `UnlockGate` sends `UNLOCK_GATE` with the plan key when slot 0 is alive, and sends nothing with no key or with slot 0 dead.
- [ ] The `MegaLevelScreen` event mask does not have `UNLOCK_GATE`.
- [ ] Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).

## Manual playthrough

Run from the worktree: `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-118j-gate-unlock`

The branch has no intended change that a player can see, so it has no experimental check. Control checks:

- `./run-game-alpha.sh --level MOON_MAN --room room11`: go through the gate and beat `MoonHead`, a mini boss. The exit gate opens after the defeat, and not before.
- `./run-game-alpha.sh --level REACTOR_MAN --room room12`: go through the gate and beat `ReactorMonkey`, a mini boss. The exit gate opens after the defeat, and not before.
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room6`: go through the gate and beat `ReactorMan`, a mid-level boss. The level music comes back, and the exit gate opens after the defeat, and not before.
- `./run-game-alpha.sh --level GAUNTLET_1 --room room1`: go through the gate and clear all the waves of the first gauntlet room. The exit gate opens after the last wave, and not before.

## Behaviors to check

- The other mid-level and mini fights (`MechaDragon`, the other 2 `ReactorMonkey` rooms, and the mid bosses of `WILY_STAGE_1` to `WILY_STAGE_3`) use the same step and the same gate code. No separate play check.
- `GAUNTLET_1` has 8 gauntlet rooms with the same code. 1 room is checked.
