# Plan: mega-refactor-024a-weapon-defs

How to do the work in `SCOPE.md`. It needs no approval. When the plan changes, write the reason in the process log.

Steps 2 to 8 replace one type (`MegamanWeapon`) in all its callers, so the code compiles only at the end of step 8.
The check of steps 2 to 7 is a read of the step's diff; step 8 runs the build.

## New API and its first caller

| API | First caller |
|---|---|
| `WeaponDef` (`weapons/`) with `id`, `cooldown`, costs, `chargeable`, `infiniteAmmo`, `newState`, `canFire`, `fire`, `onDeselect` | `MegamanWeaponsHandler` |
| `WeaponDef` move hooks: `movesBlockedWhileFiring`, `airDash`, `enablesJetpack`, `stopsRunWhileFiring`, `gripsWall`, `firingFixtures`, `stopsCharge` | the 7 behaviors, `AirDashBehavior`, `JetpackBehavior`, `Megaman.runBlocked`, `WallSlideBehavior`, `Megaman.weaponPreStep`, the `B` actuator |
| `WeaponDef` look fields: `atlas`, `shootKey`, `airDashKey`, `trailKey`, `jumpDownChargeKeys`, `showsTornado`, `keepsAnimTime`, `restartsSameKey`, `hudBitRegion`, `displayName` | `MegamanAnimations`, `MegamanAnimationKeySupplier`, `EffectComponents`, `SpritesComponent`, `AnimationsComponent`, `PlayerStatsHandler`, `GetWeaponScreen` |
| `WeaponState` and the 4 subclasses | `MegamanWeaponsHandler` |
| `MegamanWeapons` (ordered list, `byId`) | `ControllerComponent.setToNextWeapon`, `PlayerStatsHandler.addWeaponEnergy`, `LaunchOptionsResolver` |

An API with no caller at the end of step 8 is removed.

## Steps

### 1. Tests that pin today's behavior

- Changes: new tests, written against `MegamanWeapon` and run on today's code: the derived strings of each weapon
  (animation keys, trail regions, pause and get-weapon regions, display text, `--weapons` values), the weapon order,
  the cost of each weapon at each charge level, infinite buster ammo, Rush Jet cost 0.
- Check: `./gradlew core:test` passes on today's code.
- Done: yes (2026-11-14)

### 2. `weapons/` package: `WeaponDef`, `WeaponState`

- Changes: the types, with no caller yet in this step.
- Check: diff read.
- Done: yes (2026-11-14)

### 3. `MegamanWeapons`: the 10 defs and their fire functions

- Changes: `entities/megaman/weapons/`. The `shoot*` functions and the 4 handler subclasses move here with no logic
  change. The claws `slash_index` moves into `RodentClawsState`.
- Check: diff read against the old handler, function by function.
- Done: yes (2026-11-14)

### 4. `MegamanWeaponsHandler` on the defs

- Changes: the states map, `fireWeapon` in the same step order, `canFireWeapon`, ammo, `onChangeWeapon` through
  `onDeselect`. `createWeaponEntry` and the 2 `when`s go.
- Check: diff read.
- Done: yes (2026-11-14)

### 5. Callers of the type: player, state, levels, launcher

- Changes: `Megaman.kt`, `ControllerComponent`, `WeaponChargeComponent`, `GameState`, `LevelRewards`, `LevelState`,
  `LoadoutOverride`, `LevelDefinition`, `LaunchOptionsResolver`, `MegamanMaverickGame`.
- Check: diff read.
- Done: yes (2026-11-14)

### 6. Move rules as hooks

- Changes: the 7 behaviors other than `GroundSlideBehavior`, `AirDashBehavior`, `JetpackBehavior`,
  `MegamanJetpackHandler`, `Megaman.runBlocked`, `weaponPreStep`, the `B` actuator.
- Check: diff read.
- Done: yes (2026-11-14)

### 7. Look rules and screens

- Changes: `MegamanAnimations`, `MegamanAnimationKeySupplier`, `MegamanTrailSpriteV2`, `EffectComponents`,
  `SpritesComponent`, `AnimationsComponent`, `PlayerStatsHandler`, `LevelPauseScreen`, `GetWeaponScreen`,
  `EndLevelEventHandler`.
- Check: diff read.
- Done: yes (2026-11-14)

### 8. `GroundSlideBehavior`, the enum goes, build

- Changes: if `060d` merged, merge `master` first. `GroundSlideBehavior` on the hook. Delete `MegamanWeapon.kt`.
  Update the tests that name it; the step 1 tests move to the defs with the same expected values.
- Check: `./gradlew lwjgl3:jar core:test` passes. `grep MegamanWeapon` finds nothing in `core/` (only `MegamanWeapons`).
- Done: yes (2026-11-14)

### 9. New tests and the package README

- Changes: a 2-player test (own claws combo, own moon scythe wait), a move-block test; `weapons/README.md` (the def, the state, the hooks, how to add a
  weapon).
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- Done: yes (2026-11-14)

### 10. Review before the play test

- Changes: code-review subagents on the diff and on each manual check (`docs/mega-refactor/CLAUDE.md`).
- Check: all findings fixed or answered; the process log has the result. Then the play checks and `diffity-open`.
- Done: yes (2026-11-14)
