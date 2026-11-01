# Contract: mega-refactor-084h-charge

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-01

The branch moves the charge timer and the charge reads. The charge times, the sound, the sprites, the light, the shot and the stop rules must act the same, in the same frames.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with these tests:
  - `WeaponChargeComponentTest`: the component starts at 0. `half` becomes true at the half time and `full` at the full time. The hook runs once per charge, and again after `reset()`. `reset()` clears the charge.
  - `MegamanWeaponChargeTest`, written on the old code first, with expected values that do not change: `chargeStatus` at each stage, and `NOT_CHARGED` for a weapon that does not charge; `stopCharging()` clears the charge; a stun or damage clears the charge in the updatable; the `FASTER_BUSTER_CHARGING` scalar; a charge is 0 after a respawn, and the first spawn is not charged.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The old name is gone: `grep -rn 'chargingTimer' core/src/main/kotlin` finds nothing.
- [ ] `Megaman` keeps 8 interfaces on its class line.
- [ ] A short launch of `REFACTOR_TEST_029 --room movement` starts with no exception, at the end of the branch.

## Frame order

The timer update stays at the same place in the attack actuator, with the scalar. The reset at `:513` stays right after the stun update. The `reset()` of `WeaponChargeComponent` replaces the `onSpawn` line at `:367`, and no code writes the timer between the destroy and the next spawn. The 4 `stopSound` lines do not move. Review the final diff for any other change of order. Lines are at `63018006b`.

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084h-charge`

Experimental:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: hold the attack button. The charge sound and the half charge sprite start at the same time, and the full charge sprite comes at the same time. Do this while you stand, run, jump and hold `DOWN` in the air.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: release at no charge, at half charge and at full charge. Each gives the correct shot. After each shot, a new charge starts at 0 and the sound plays again.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement --weapons all`: charge, then change the weapon. The charge and the sound stop. Hold the attack button with a weapon that does not charge: no charge sprite and no sound.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room stun`: charge, and let the pulser stun Mega. The charge stops.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: charge, and take a hit. The charge and its sound stop.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die during a charge. After the beam-in, Mega has no charge, and a new charge starts at 0.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room pipe_portal`: enter a pipe during a charge. The charge and its sound stop at the start of the teleport, and no charge remains at the end.
- `./run-game-alpha.sh --level DESERT_MAN --room room6`: in the dark, the light grows at half charge and at full charge, and goes back after the shot.

Control:

- `./run-game-alpha.sh --level DESERT_MAN --room room13`: hold a charge through the gate to `room14`. The sound stops at the gate, and the charge stays as before.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: play through several rooms with charged shots.

## Behaviors to check

Control:

- The pause menu: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: pause during a charge, and resume. The charge continues if the button is held.

## Intended changes

A death during a charge clears the charge at the destroy, not at the next spawn. So in `Test1.tmx`, the `Darkness` entity no longer shows the charge light on a dead Mega until the respawn. `Test1.tmx` has no player spawn in the `Darkness` rooms (`room9` to `room11`), so `MegamanWeaponChargeTest` covers this, not a play check. No other level changes.
