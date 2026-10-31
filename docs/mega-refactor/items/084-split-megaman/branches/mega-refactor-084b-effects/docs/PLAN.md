# Plan: mega-refactor-084b-effects

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-31

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`, `S/` = `C/entities/state/`,
`T/` = `core/src/test/kotlin/com/megaman/maverick/game/`. All commands run from
`/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084b-effects`.

Each step ends with one commit on the branch. Line numbers are in `M/Megaman.kt` at `876b90a6e`; they move as the steps go. Before each code step, check `SCOPE.md`, "Frame-order
constraints", and `CONTRACT.md`, "Frame order". In steps 3-6, write the tests first and see them fail.

## Steps

### 1. Baseline

- Changes: none in code.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes. Write the result in the process log.
- Done: yes (2026-10-31)

### 2. Test room

- Changes: a subagent adds the room `out_of_bounds` to `assets/tiled_maps/tmx/RefactorTest084.tmx` with the `level-builder` skill: a floor with a pit that has no `Death` block and a player spawn on the
  floor, with no other room below the pit, and painted tiles. The pit must reach past the bottom of the map. The rooms `knockback`, `spring_head`, `carry_left`, `carry_right` and `carry_top` do not change.
- Check: a short launch of the room on the baseline code has no exception. Log the launch.
- Done: yes (2026-10-31)

### 3. Bubbles

- Changes: `T/entities/state/BubbleEmitterComponentTest.kt`. `S/BubbleEmitterComponent.kt`. `M/components/EffectComponents.kt` with `defineBubbleEmitterComponent()`, and `spawnBubble()` and the water condition as
  private functions. `UNDER_WATER_BUBBLE_DELAY` moves to `MegamanValues`. `Megaman.init()` adds the component. The lambda calls `update(delta)` at the `:657` position.
- Check: `./gradlew core:test lwjgl3:jar` passes. Read the moved lines against the old lines in `git diff`.
- Done: yes (2026-10-31)

### 4. Trail

- Changes: `T/entities/state/TrailSpriteComponentTest.kt`. `S/TrailSpriteComponent.kt`. `defineTrailSpriteComponent()` and `tryToSpawnTrailSprite()` in `M/components/EffectComponents.kt`. `TRAIL_SPRITE_DELAY` moves
  to `MegamanValues`. `Megaman.init()` adds the component. The lambda calls `update(delta)` right after the bubbles.
- Check: `./gradlew core:test lwjgl3:jar` passes. Read the moved lines against the old lines in `git diff`.
- Done: yes (2026-10-31)

### 5. Out-of-bounds check

- Changes: `T/entities/utils/LevelBoundsTest.kt`. `C/entities/utils/LevelBounds.kt` with `isOutsideLevel`. The `if` at `:631` calls it; the log line, the `destroy()` and the 2 offsets stay in `Megaman`.
- Check: `./gradlew core:test lwjgl3:jar` passes.
- Done: yes (2026-10-31)

### 6. Explosion orbs

- Changes: `T/entities/explosions/ExplosionOrbTest.kt`. `ExplosionOrb`: `DEFAULT_EXPLOSION_ORB_SPEED`, the private table of 8 directions, `spawnRing(center, speed)`, and the optional `ConstKeys.SPEED` prop in `onSpawn`. `Megaman.onHealthDepleted`
  and `AbstractBoss.spawnExplosionOrbs` call `spawnRing`. The 2 old tables and `MegamanValues.EXPLOSION_ORB_SPEED` go, with their imports.
- Check: `./gradlew core:test lwjgl3:jar` passes. Compare the 8 new directions with the old tables by hand.
- Done: yes (2026-10-31)

### 7. Final review

- Changes: fix only what this check finds.
- Check: review the lambda diff against the table in `CONTRACT.md`, "Frame order". Run the 3 `grep` checks. Write the result in the process log.
- Done: yes (2026-10-31)

### 8. Automated checks and a smoke launch

- Changes: none in code.
- Check: all of "Automated checks" in `CONTRACT.md`, with a short launch of `out_of_bounds`. Write the results in the process log.
- Done: yes (2026-10-31)

### 9. The owner's play test

- Changes: none, unless the play test finds a problem. After a fix, the control checks run again.
- Check: the owner runs "Manual playthrough" and "Behaviors to check" in `CONTRACT.md`, the experimental checks first. Write the result in the process log.
- Done: yes (2026-10-31)

### 10. Before the merge

- Changes: none. This branch has no temporary suite.
- Check: read the folders of the other active branches again. If `master` changed the same files, merge `master` and run the automated checks again. The merge needs the owner's explicit approval.
- Done: checks run (2026-10-31); the merge waits for the owner's approval

## New API and its first caller

See `SCOPE.md`, "New API and its first caller". This plan adds no other API.
