# Scope: mega-refactor-084h-charge

- Status: approved
- Approved: 2026-11-01

## Goal

Move the charge timer and the charge reads off the `Megaman` class, into a `WeaponChargeComponent` and a Megaman-only file. Mega plays the same. The charge rules (which weapon charges, the cost and the fire rules) stay for `024`.

## Item

From the 084 `ITEM.md`, "Decisions" (2026-10-31, owner):

> The charge timer moves in `084h`; `024` keeps the charge rules.

From the 084 `ITEM.md`, "Not in this item":

> The weapon checks and the charge rules: `024`. The charge timer moves in `084h`.

From the 084 `NOTES.md`, "Proposed branches":

> `084h-charge`: only if the owner decides that the charge state moves here. `ChargeComponent`, with the charge rules left for `024`.

## Current state

Scan on 2026-11-01, `master` at `63018006b`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`. Line numbers are in `M/Megaman.kt` unless the line names a file.

**The timer.** `chargingTimer` (`:94-98`) has the duration `TIME_TO_FULLY_CHARGED` (1.5) and a mark at `TIME_TO_HALFWAY_CHARGED` (0.75). The mark plays `MEGA_BUSTER_CHARGING_SOUND`.

- Update: 1 line, `M/components/ControllerComponent.kt:85`, in the attack actuator, with the `FASTER_BUSTER_CHARGING` scalar.
- Reset: `onSpawn` (`:367`), `stopCharging()` (`:543`), and the updatable (`:513`) when Mega is stunned or damaged. That last line does not stop the sound.

**The reads.** 5 public members (`:121-134`): `canChargeCurrentWeapon`, `chargeStatus`, `charging`, `halfCharged`, `fullyCharged`. Each one applies the weapon rule `weaponsHandler.isChargeable(currentWeapon)`.

- All callers of these members are in `M/`: `ControllerComponent.kt:73-74,89,97`, `MegamanAnimationKeySupplier.kt:129-134`, the `PlayerLightComponent` hook (`:321`) and `shoot()` (`:547`).
- The other `fullyCharged` reads in the game are on `ChargedShot`, not on `Megaman`.
- `chargeStatus` is the argument to `weaponsHandler.canFireWeapon` and `fireWeapon`.

**`stopCharging()`** (`:541-544`): stops the sound and resets the timer. Callers: `ControllerComponent.kt:76,100,109`, the teleport hooks (`:268,275`), and the updatable (`:507`).

**The sound.** 4 more lines stop the sound with no timer reset: `onDestroy` (`:388`), the room transition (`:416`), the gate (`:432`) and damage (`:468`). A charge continues through a room transition and a gate with no sound.

**No tests.** `core/src/test/` has no test that names a charge value.

**Resets and the first spawn.** `GameEngine.destroyNow` runs each component `reset()`, and the controller system does not run between the destroy and the next spawn, so `reset()` can replace the `onSpawn` line at `:367`. The timer starts with `setToEnd()`, but `onSpawn` resets it before any read. If only `reset()` remains, a `setToEnd()` start gives a full charge on the first spawn, so the new timer must start at 0.

**The one change of order.** With the `onSpawn` line gone, the charge is 0 from the destroy, not from the next spawn. Only the `PlayerLightComponent` hook reads a dead Mega. `DarknessV2` and `DarknessV4` check `inPlay` first. `Darkness` (`C/entities/special/Darkness.kt:274-276`) does not. In the 23 live map files, `Darkness` is only in `Test1.tmx`, and `DarknessV2` is in `DesertMan_16x14_v4.tmx` and `WilyStage1_v2.tmx`. So a death in a charge in `Test1.tmx` no longer leaves the charge light until the respawn. No other level can show this.

## In scope

- A new `C/entities/state/WeaponChargeComponent.kt`: the timer, the 2 thresholds as constructor values, the mark as a hook, and `reset()`. It holds no weapon rule and no entity reference. It starts at 0.
- A new `M/components/WeaponChargeComponent.kt`: `defineWeaponChargeComponent()` passes `MegamanValues` and the sound hook. The 5 reads and `stopCharging()` move here as `internal` extensions on `Megaman`. They do not go on `IGameEntity`: only Mega uses them, and they read the weapon rule.
- `M/Megaman.kt`: remove the timer, the 5 reads, `stopCharging()` and the `onSpawn` line. The `:513` line and `shoot()` use the new API.
- `ControllerComponent.kt:85`: update through the component. The scalar stays here.
- Imports in `MegamanAnimationKeySupplier.kt` and the other callers.
- New tests: `WeaponChargeComponentTest` and `MegamanWeaponChargeTest`.

New API, with the first caller:

- `WeaponChargeComponent`: `defineWeaponChargeComponent()`.
- `WeaponChargeComponent.update(delta)`: the attack actuator.
- `WeaponChargeComponent.half` and `WeaponChargeComponent.full`: the `charging` and `fullyCharged` extensions.
- `WeaponChargeComponent.reset()`: the engine, `stopCharging()` and the `:513` line.
- `Megaman.defineWeaponChargeComponent()`: `Megaman.init()`.

`canChargeCurrentWeapon` has no caller outside the other reads, so it becomes `private` in the new file.

## Out of scope

- The weapon rules: `isChargeable`, `canFireWeapon`, the costs and `fireWeapon`: `024`.
- `shoot()`, `ammo`, `translateAmmo`, `shootAnimTimer` and any larger weapon component: `024` (owner decision, see `docs/NOTES.md`).
- The 4 `stopSound` lines. They stay as they are.
- `MegaChargeStatus` and `PlayerLight`: they stay where they are. `MegamanValues` does not change.
- The rule that a charge continues through a room transition and a gate. It stays.
- `FASTER_BUSTER_CHARGING`: it stays in the actuator, because it reads `gameState`.

## Affected areas and risks

- 2 new production files, 24 lines moved out of `M/Megaman.kt`, and 3 changed callers. `Megaman` keeps 8 interfaces.
- The charge sound mark must fire once per charge: `Timer.reset()` refills the mark queue, so the component must use `Timer.reset()`.
- The timer update must stay in the actuator, with the scalar, at the same point in the frame.
- The animation keys depend on `halfCharged` and `fullyCharged`, and a wrong read gives wrong sprites. The play test and `MegamanChargeTest` cover this.
- The light in dark rooms reads `charging` and `fullyCharged`.

## Overlap with other branches

`mega-refactor-031f-menu-input` is the only other active branch, and it changes no file in `M/` or `C/entities/state/`. `024` depends on this branch through the new file: it can take the charge rules from `M/components/WeaponChargeComponent.kt`.

## Objective check

Added after the approval: the objectives reached `master` during this branch. No rule is in conflict.

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` Components, not new inheritance | helps | A component in place of class state. |
| `O1-R3` No new fragile order | no effect | No new listener. The `reset()` at the destroy replaces an `onSpawn` line. |
| `O1-R4` Typed state | no effect | No new key. |
| `O1-R5` Code only with a caller | no effect | Each new API has a production caller in this branch. |
| `O5-R1` No per-frame allocation | no effect | The reads add `require` lookups and no new collection, lambda or `props(...)` call in an update path. |
| `O5-R2` Measure hot paths | no effect | Not treated as a hot path: 1 entity, with a few more `require` lookups per frame, in the form that all `084` parts use. Not measured. |
| `O8-R1` No copy of `Megaman.kt` | helps | The timer is shared state, with the values passed in. |
| `O8-R2` Capabilities, not character checks | no effect | No new `is Megaman` check or cast. |
| `O8-R4` One definition for each weapon | waiting | The `isChargeable` rule stays for `024`. |

## Open questions

None. The owner accepted the change in `Test1.tmx` (see `docs/NOTES.md`).
