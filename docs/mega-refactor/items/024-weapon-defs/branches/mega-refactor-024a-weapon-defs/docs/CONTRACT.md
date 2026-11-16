# Contract: mega-refactor-024a-weapon-defs

The behavior that this branch changes on purpose, the behavior that it must not change, and how to check them. The
owner approves the brief, not this file. Do the checks before the branch merges, and write the result in the branch
process log.

- Status: approved
- Approved: 2026-11-14

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] New test: every string made from a weapon id (animation keys, trail regions, pause regions, get-weapon regions
  and text, `--weapons` values) equals the string made from the old enum name.
- [ ] New test: the order of `MegamanWeapons` is the old enum order (the `SELECT` cycle and the energy fill order).
- [ ] New tests: the buster costs 0 at each charge level and has full ammo; Rush Jet fires the buster at cost 0.
- [ ] New test: 2 Megamans have their own state of each weapon (the claws combo, the moon scythe wait).
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-024a-weapon-defs`.

| Level | What to check |
|-------|---------------|
| `./run-game-alpha.sh --level TEST_1 --room room35 --weapons all` | `SELECT` cycles all 10 weapons in the same order. Each weapon fires with the same shot, sound, cost and cooldown. The HUD bar of each weapon goes down. Buster and charge as before. |
| `./run-game-alpha.sh --level TEST_1 --room room35 --weapons all` | Needle Spin blocks jump, slide, climb and wall slide while it fires. Rush Jet: the air dash and the jetpack as before. Rodent Claws: the 3-slash combo, the run stop, the wall grip, the ammo refill. Axe Swinger: the axe and the shield. Precious Guard: the shield, the release, the deselect clears the gems. Moon Scythes: at most 4 live. |
| `./run-game-alpha.sh --level TEST_1 --room room35 --weapons all` | Pause menu: the same grid, icons and bars; a weapon chosen there is the current weapon. |
| `./run-game-alpha.sh --level MOON_MAN --room room14 --weapons all --heartTanks all --healthTanks all` | Beat Moon Man. The beam-out uses the current weapon's art. The get-weapon screen shows Moon Scythes and Rush Jet with their names and art. |
| `./run-game-test-coop.sh --weapons all` | Each player has its own ammo and its own claws combo. |

## Behaviors to check

- A weapon energy bulb fills the current weapon first, then the others in the old order.
- A death and a respawn reset the cooldowns and the spawned sets, as before.
- The save: a save made on `master` loads, and the owned weapons follow the completed levels.

## Intended changes

None. This branch is a refactor.
