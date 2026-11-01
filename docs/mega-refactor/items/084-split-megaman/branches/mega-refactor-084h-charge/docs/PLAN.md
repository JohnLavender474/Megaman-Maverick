# Plan: mega-refactor-084h-charge

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-01

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`. Line numbers are at `63018006b`.

Each step is one commit. Each step ends with `./gradlew engine:test core:test lwjgl3:jar`, on JDK 17, plus the check that the step names. Where a step adds an API, the test comes first, and it must fail for the expected reason before the code changes. Stage files by path, and read `git status --short` before each commit.

## Steps

### 1. Charge tests on the old code

- Changes: new `MegamanWeaponChargeTest`, with no production change: `chargeStatus` at no charge, half charge and full charge; `NOT_CHARGED` for a weapon that does not charge; `stopCharging()` clears the charge and stops the sound; a stun or damage clears the charge in the updatable, with no sound stop; the `FASTER_BUSTER_CHARGING` scalar in the attack actuator; the sound mark plays once per charge, and again after a reset; a charge is 0 after a respawn.
- Check: the tests pass on the old code. The test reads go through `chargeStatus`, `charging`, `halfCharged` and `fullyCharged`, and the test writes go through the attack actuator or a small test helper, so the expected values do not change in step 3.
- Done: yes

### 2. `WeaponChargeComponent`

- Changes: tests first: new `WeaponChargeComponentTest`: the start at 0, `half` at the half time, `full` at the full time, the hook once per charge and again after `reset()`, and `reset()` clears the charge. Then add `C/entities/state/WeaponChargeComponent.kt`. The timer starts at 0, with no `setToEnd()`. `reset()` calls `Timer.reset()`.
- Check: the tests fail with no class, and pass with the class. The production caller comes in step 3, in the same branch.
- Done: yes

### 3. Move the charge off `Megaman`

- Changes: tests first: in `MegamanWeaponChargeTest`, the first spawn is not charged, and `charging` is false after the destroy. Then add `M/components/WeaponChargeComponent.kt`: `defineWeaponChargeComponent()`, the 5 reads and `stopCharging()` as `internal` extensions, with `canChargeCurrentWeapon` `private`. `Megaman.init()` adds the component, before `defineControllerComponent()`. `ControllerComponent.kt:85` and the `:513` line use the component. Delete the timer (`:94-98`), the 5 reads (`:121-134`), `stopCharging()` (`:541-544`) and the `onSpawn` line (`:367`). Add the imports in the callers. Delete imports that have no use.
- Check: `grep -rn 'chargingTimer' core/src/main/kotlin` finds nothing. 8 interfaces on the class line. The tests from step 1 pass with no change to their expected values.
- Done: yes

### 4. Final checks

- Changes: none in code, unless a check fails.
- Check: all automated checks in `CONTRACT.md`. Review the full diff against the "Frame order" section. Build, then `timeout 12 ./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`. Compare the diff with `master`, and merge `master` first if it changed the same files. Then the owner's play test.
- Done: yes
