# Plan: mega-refactor-084d-damage-reaction

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-30

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`,
`S/` = `C/entities/state/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`. All commands run from
`/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084d-damage-reaction`.

Each step ends with one commit on the branch. Line numbers are in `M/Megaman.kt` at `2aff79ae5`; they move as the steps
go. Before each code step, check `SCOPE.md`, "Frame-order constraints", and `CONTRACT.md`, "Frame order".

## Steps

### 1. Baseline

- Changes: none in code.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes. Write the result in the process log.
- Done: yes (2026-10-30)

### 2. Test map

- Changes:
  - A subagent builds `assets/tiled_maps/tmx/RefactorTest084.tmx` with the `level-builder` skill: room `knockback` (a damager
    that can hit Mega from both sides, and gravity switches for all 4 directions) and room `spring_head` (a `SpringHead`).
    Each room has a player spawn and a `tiles` layer.
  - `REFACTOR_TEST_084` in `C/levels/LevelDefinition.kt` (type `TEST_LEVEL`), and its row in `LevelCompletionScreensTest`.
- Check: a short launch of each room on the baseline code has no exception. Read `TestStunPulser` and the `stun` room: if the
  room does not cover `STUN_BOUNCE_IF_ON_SURFACE`, add this to the room or the contract. Read `Coldier` and `IceFox`: if
  `GLACIER_MAN --room room6` cannot freeze Mega reliably, add a `freeze` room here and update `CONTRACT.md`. Log the change.
- Done: yes (2026-10-30)

### 3. Temporary suite: the frozen copy

- Changes: `T/entities/state/Temp084dFrozenDamageReaction.kt`: a copy of `:744-756`, `:807-827`, `:600-634`, `:664-689` and
  `:691-695`, with the inputs as parameters and the effects as a recorded list. `T/entities/state/Temp084dDamageReactionEquivalenceTest.kt`:
  the seeded frame generator. It first runs the frozen copy alone, with assertions for the cases in `CONTRACT.md`.
- Check: `./gradlew core:test` passes. The suite gets one comparison for each new part in steps 4-7.
- Done: yes (2026-10-30)

### 4. Knockback

- Changes: `S/KnockbackComponent.kt`, `S/Knockbacks.kt`, and `M/components/DamageReactionComponents.kt` with
  `defineKnockbackComponent()`. `Megaman.init()` adds it; `takeDamageFrom` calls `knockBackFrom(damager)` at the `:664` position.
  `noDmgBounce` and the `when` leave `Megaman`. Copy the 4 arms unchanged; `x` and `y` are scaled in the `define` function.
- Check: `./gradlew core:test lwjgl3:jar` passes, with the knockback tests and the temporary comparison.
- Done: yes (2026-10-30)

### 5. Freeze

- Changes: `S/IThawRule.kt`, `S/MashThawRule.kt`, `S/FreezeComponent.kt`, `S/Freezes.kt`, `defineFreezeComponent()`. `Megaman.init()` adds
  it. `takeDamageFrom` calls `freezeFrom(damager)` at the `:691` position; the lambda calls `update(delta)` at the `:807` position.
  `frozen`, `hitsToUnfreeze`, `frozenPushTimer`, `onSpawn` lines `:475-477` and `IFreezableEntity` leave `Megaman`. The 4 readers
  (`BodyComponent`, `SpritesComponent`, `MegamanAnimationKeySupplier`, `MegamanWeaponsHandler`) import the extension.
- Check: `./gradlew core:test lwjgl3:jar` passes, with the freeze tests and the temporary comparison.
- Done: yes (2026-10-30)

### 6. Stun

- Changes: `S/StunComponent.kt`, `S/Stuns.kt`, `defineStunComponent()`. `Megaman.init()` adds it. The `STUN_PLAYER` arm calls
  `onStunPlayerEvent(event)`; the lambda calls `update` at the `:744` position. `stunTimer`, `stunned` and `onSpawn` line `:483`
  leave `Megaman`. `ControllerComponent` and `MegamanAnimationKeySupplier` import the extension. `MegamanStunTargetTest` uses
  `defineStunComponent()`.
- Check: `./gradlew core:test lwjgl3:jar` passes, with the stun tests, `MegamanStunTargetTest` and the temporary comparison.
- Done: yes (2026-10-30)

### 7. Recovery flash

- Changes: `S/DamageRecoveryComponent.kt`, `defineDamageRecoveryComponent()`. `Megaman` gets the field `damageRecovery`, set in the
  class body and added in `init()`. `invincible` reads `damageRecovery.recovering`; a `TODO` at the getter says that a second
  player entity needs the same rule (`060`). `SpritesComponent` reads `damageRecovery.flash`. The lambda calls `update` after
  `:746`. The timers, `recoveryFlash` and `onSpawn` lines `:472`, `:485-486` leave `Megaman`. `:484` stays.
- Check: `./gradlew core:test lwjgl3:jar` passes, with the recovery tests, the `invincible` tests and the temporary comparison.
- Done: yes (2026-10-30)

### 8. The destroy window and the final review

- Changes: fix only what this check finds.
- Check: read each reader of `stunned`, `frozen` and the flash, and everything that runs between `destroy()` and the next spawn
  (`onDestroy`, `PLAYER_JUST_DIED` and game-over handlers), to confirm that the earlier clear has no effect. Check that no
  reader of `Megaman` runs before the first spawn with a missing component. Review the lambda diff against the table in
  `CONTRACT.md`. Run the 2 `grep` checks in the contract. Write the result in the process log.
- Done: yes (2026-10-30)

### 9. Automated checks and a smoke launch

- Changes: none in code.
- Check: all of "Automated checks" in `CONTRACT.md`, with a short launch of `REFACTOR_TEST_084`. Write the results in the process log.
- Done: yes (2026-10-30)

### 10. The owner's play test

- Changes: none, unless the play test finds a problem.
- Check: the owner runs "Manual playthrough" and "Behaviors to check" in `CONTRACT.md`, with the `--diagnostics` captures before and
  after. Write the result in the process log.
- Done: yes (2026-10-30)

### 11. Before the merge

- Changes: delete `Temp084dFrozenDamageReaction.kt` and `Temp084dDamageReactionEquivalenceTest.kt`, in the last commit.
- Check: read the folders of the other active branches again (`docs/NOTES.md` names them). If `master` changed the same files, merge
  `master` and run the automated checks again. The merge needs the owner's explicit approval. File the `SPAWN` lock bug candidate on `master`.
- Done: yes, except the merge and the bug filing (2026-10-30)

## New API and its first caller

See `SCOPE.md`, "New API and its first caller". This plan adds no other API. The temporary suite is test code.
