# Contract: mega-refactor-060j-mega-player

- Status: approved
- Approved: 2026-11-10

Refactor only: Megaman and X play the same. The checks cover each lifecycle point that moves into `MegaPlayer`
(spawn, room transition, gate, damage, stun, death and respawn, heart tanks, level defeat) and each shared function
that changes its receiver (run, jump, wall slide, air dash, the `SELECT` action).

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with the existing `MegamanBodyFixturesTest`, `MegamanRunTest`, `PlayerRunTest`,
  the heart tank tests and the tests that use `TestHealthPlayer`.
- [ ] `./gradlew core:smokeTest` passes (`entity-tags.txt` does not change).
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from the branch worktree. All checks are control checks: the result must be the same as on `master`.

Megaman:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement --weapons MOON_SCYTHES`: the beam-in; run, jump,
  slide, air dash; `SELECT` changes the weapon (the slot setting read).
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room stun`: the stun and its recovery.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`: a hit: damage sound, knockback, blink, the
  invincible time.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room heart`: a heart tank raises the max health at once.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: death (orb ring), respawn, full health.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room carry_left`: the room transition carries him; the
  transition stops the charge sound.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: the gravity switches and the camera turn; he
  moves again after the turn ends.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room14 --heartTanks all --healthTanks all`: the boss gate (the
  charge sound stops); beat the boss; the get-weapon screen shows the new weapon.

X:

- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room flat`: the beam-in; walk, jump, dash, sabre.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room walls`: wall slide and wall jump.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room dash`: the air dash on `SELECT`.
- `./run-game-alpha.sh --level X6_INTRO --room room1 --heartTanks all`: the max health is the heart tank value at
  spawn; a hit (damage sound, knockback; a hit during a slash ends the slash); the room transition to `room2`.
- `./run-game-alpha.sh --level X6_INTRO --room room2 --heartTanks all --healthTanks all`: the boss gate; death and
  respawn (the sabre slash is gone after the respawn).

## Behaviors to check

- Pause menu and the HUD health bar for both characters (any check above).

## Intended changes

None. D1: Megaman also sets his max health from the heart tanks at each spawn, as X does; the scan finds that this
gives the value he already has in every current flow.
