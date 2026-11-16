# Contract: mega-refactor-024b-character-loadouts

The behavior that this branch changes on purpose, the behavior that it must not change, and how to check them. The
owner approves the brief, not this file. Do the checks before the branch merges, and write the result in the branch
process log.

- Status: approved
- Approved: 2026-11-15

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] New test: owned weapons are per character. A completed level gives each character the rewards of its own
  loadout; X and Proto Man own no `WeaponDef`; `alwaysOwned` counts only in the loadout that lists the def.
- [ ] New test: the reward of a level is the slot-0 character's (`weaponsAttained`), once, also with 2 players.
- [ ] New test: `--weapons` gives each named def to the characters whose loadout lists it; `all` gives every loadout
  weapon.
- [ ] New test: every look string of Megaman's weapons (animation keys, trail, HUD, pause and get-weapon regions)
  equals today's string.
- [ ] New test: `isMegamanShootingAtMe` is true when X or Proto Man shoots toward the enemy, and false when they do
  not shoot.
- [ ] The existing weapon, progress and save tests pass with the same expected values (the save string is unchanged).
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-024b-character-loadouts`.

| Level | What to check |
|-------|---------------|
| `./run-game-alpha.sh --level REFACTOR_TEST_060 --room sabre` | Experimental: from the floor, shoot the buster (`B`) at the `Met`. It stays under its helmet while Proto Man shoots at it, as it does for Megaman. |
| `./run-game-alpha.sh --level REFACTOR_TEST_081 --room met` | Experimental: the same with X (new room with a `Met`). |
| `./run-game-alpha.sh --level MOON_MAN --room room14 --weapons all --heartTanks all --healthTanks all` | Beat Moon Man. The beam-out uses the current weapon's art. The get-weapon screen shows Moon Scythes and Rush Jet with their names and art. |
| `./run-game-alpha.sh --level TEST_1 --room room35 --weapons all` | The HUD weapon bar, the pause grid with its bars, and a weapon chosen in the pause menu, as before. `SELECT` cycles the 10 weapons in the same order. |
| `./run-game-alpha.sh --level REFACTOR_TEST_047 --room pickups --weapons all` | Fire a weapon until its bar is low, then take a `WeaponEnergyBulb`: the current weapon fills first, then the others in the `SELECT` order, as before. |
| `./run-game-alpha.sh --level REFACTOR_TEST_081 --room flat` | X: the HUD and the pause screen look as before; `SELECT` and the pause menu change no weapon. |
| `./run-game-alpha.sh --level REFACTOR_TEST_060 --room buster` | Proto Man: the HUD and the pause screen as on `master` (no slot icon since `060l`); `SELECT` changes nothing. |
| `./run-game-test-coop.sh --weapons all` | Each player has its own ammo. Slot 1 changes to a weapon with `SELECT` and fires it until it is empty; slot 0's HUD bar and pause bars for that weapon stay full. Slot 0 then fires the same weapon: its bar goes down from full. Only slot 0 pauses and has a HUD, as before (item `047`). |

## Behaviors to check

- A save made on `master` loads, and Megaman owns the weapons of the completed levels.
- "New game" (`resetPlayerProgress`) sets Megaman back to the buster and clears the owned weapons.
- A death and a respawn keep the owned weapons and reset the current weapon as before.

## Intended changes

- A `Met` (and any enemy that uses `isMegamanShootingAtMe`) reacts to X's and Proto Man's buster shots, as it does to
  Megaman's (D4).
- A level completed with a character whose loadout has no reward for it (for example X or Proto Man in slot 0 on a
  robot master level) sets no `weaponsAttained`, so the get-weapon screen is skipped. On `master` the get-weapon
  screen crashed in that case on a cast to `Megaman`. No live level has this case today.
- Dev only: `removeLevelCompleted` no longer removes a weapon that `--weapons` or a `LoadoutOverride` gave.
