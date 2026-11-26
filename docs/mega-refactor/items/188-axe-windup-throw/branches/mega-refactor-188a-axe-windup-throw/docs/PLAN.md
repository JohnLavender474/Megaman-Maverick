# Plan: mega-refactor-188a-axe-windup-throw

Each group goes to one implementation subagent. Gradle cannot run in this session; CI on the pushed branch checks.

## Group A: windup refactor (no gameplay change except the named ones)

1. Characterization tests on the old code (`MegamanWeaponChargeTest`, X tests): the half sound once, also on one large
   delta; no sound when cleared before half; the sound again after a clear and a recharge; the enhancement time; light
   and `_charge_*` keys at the thresholds; X's sound once at 0.5 s. Check: the tests describe today's behavior.
2. `WeaponWindup`, `BusterCharge`, `WeaponWindupComponent`; Megaman and X build their own windups; `stopCharging` →
   `stopWindup`; `WeaponDef.chargeable` removed; `windupOf`; the status gate; `Megaman.kt:280`; the `fireWeapon`
   guard; `clearChargeWhenHurt`. Rewrite `WeaponChargeComponentTest`. Check: every old charge test still passes,
   except the named changes 3 to 5 of the contract, which change their tests with a note.

## Group B: axe throw (Megaman side)

3. `AxeThrowWindup`, `AXE_SWINGER` windup, the gates in `onPressContinued` (pause in transition and turn), constants.
4. `AxeThrow` launch function (ratio, impulse, direction map, slow scalar, spawn point); `throwAxe` uses it.
5. Timing: progress read at release; immediate or remaining delay; animation jump to the throw frame.
6. Pose: `holdingAxe`, `amendKey`, `Animator` freeze, climb `holdStill`, `climb_still_axe_throw`.
7. Tests: curve, launch table, hold and release, timing (`MegamanDeathCleanupTest`), pose, climb.

## Group C: arc (projectile side, parallel to B)

8. `AxeTrajectory` (`gravity`, dots); `Axe` uses it; `RIGHT` fix. Parity test against a real `Axe` (the `NeedleTest`
   harness).

## Group D: wiring and map

9. Preview updatable and the 32 dot sprites in Megaman.
10. Test map `RefactorTest188.tmx` (`throw_range`, `water`) and `REFACTOR_TEST_188`.
11. Review, CI, play scripts, draft PR.
