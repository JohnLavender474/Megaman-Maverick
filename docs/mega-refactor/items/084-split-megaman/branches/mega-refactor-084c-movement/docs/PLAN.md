# Plan: mega-refactor-084c-movement

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-30

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`, `S/` = `C/entities/state/`,
`T/` = `core/src/test/kotlin/com/megaman/maverick/game/`. All commands run from
`/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084c-movement`.

Each step ends with one commit on the branch. Line numbers are in `M/Megaman.kt` at `ba4b3ca1c`; they move as the steps go. Before
each code step, check `SCOPE.md`, "Frame-order constraints", and `CONTRACT.md`, "Frame order".

## Steps

### 1. Baseline

- Changes: none in code.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes. Write the result in the process log.
- Done: yes (2026-10-30)

### 2. Test map

- Changes: a subagent adds 3 adjoining rooms to `assets/tiled_maps/tmx/RefactorTest084.tmx` with the `level-builder` skill: `carry_left` and
  `carry_right` side by side with a floor across the boundary, and `carry_top` above `carry_right`, with a ladder across that boundary and
  a gap that Mega can jump and fall through. `carry_left` and `carry_right` have a player spawn. The rooms `knockback` and `spring_head`
  do not change, because the `084d` contract uses them.
- Check: a short launch of each new room on the baseline code has no exception. Log the launches.
- Done: yes (2026-10-30)

### 3. Temporary suite: the frozen copy

- Changes: `T/entities/state/Temp084cFrozenMovement.kt`: a copy of `:676-697`, `:847-874` and the carry at `:532`, `:540-552`, `:564-576`, with the
  inputs as parameters. `T/entities/state/Temp084cMovementEquivalenceTest.kt`: the seeded frame generator, which first runs the frozen copy
  alone.
- Check: `./gradlew core:test` passes. The suite gets one comparison for each new part in steps 4-5.
- Done: yes (2026-10-30)

### 4. Momentum

- Changes: `S/PostActionMomentumComponent.kt`, `S/PostActionMomentum.kt`, `M/components/MovementComponents.kt` with `defineMomentumComponent()`.
  `Megaman.init()` adds it. The lambda calls `updatePostActionMomentum(delta, running)` at the `:678` position. The state, the 4 functions and
  `onSpawn` lines `:488-489` leave `Megaman`. The 9 outside lines import the functions; the 2 direct writes call `requestMomentumKill()`.
- Check: `./gradlew core:test lwjgl3:jar` passes, with the momentum tests and the temporary comparison.
- Done: yes (2026-10-30)

### 5. Room carry

- Changes: `S/RoomTransitionCarryComponent.kt`, `S/RoomTransitionCarries.kt`, `defineRoomTransitionCarryComponent()`. `Megaman.init()` adds it. The
  event arms call `beginRoomTransitionCarry()` and `endRoomTransitionCarry()`; the lambda calls `update(delta)` at the `:676` position.
  `MegamanAnimationKeySupplier` reads `roomTransitionPaused`. The `!!` stays, with a `TODO` that names the case and the follow-up. `onSpawn`
  line `:485` and `:502` leave `Megaman`.
- Check: `./gradlew core:test lwjgl3:jar` passes, with the carry tests and the temporary comparison.
- Done: yes (2026-10-30)

### 6. `AButtonTask`

- Changes: `M/MegamanAButtonTask.kt` with `updateAButtonTask()`: `:727-744` unchanged. The lambda calls it last.
- Check: `./gradlew core:test lwjgl3:jar` passes. Read the moved block against the old one line by line.
- Done: yes (2026-10-30)

### 7. The destroy window and the final review

- Changes: fix only what this check finds.
- Check: read each reader of the new state, and what runs between `destroy()` and the next spawn. Review the lambda diff against the table in
  `CONTRACT.md`. Run the 2 `grep` checks. Write the result in the process log.
- Done: yes (2026-10-30)

### 8. Automated checks and a smoke launch

- Changes: none in code.
- Check: all of "Automated checks" in `CONTRACT.md`, with a short launch of each new room. Write the results in the process log.
- Done: yes (2026-10-30)

### 8a. Room-carry crash fix

- Changes: `SCOPE.md`, "In scope", item 7, in `S/RoomTransitionCarries.kt`. The 2 `TODO` comments and the bug candidate leave. The test that recorded
  the throw is replaced. The temporary carry comparison uses begin and end pairs with the same `carries` value.
- Check: the new tests fail on the old code, then `./gradlew engine:test core:test lwjgl3:jar` passes, with a short launch of `carry_left` and `carry_right`.
- Done: yes (2026-10-30)

### 9. The owner's play test

- Changes: none, unless the play test finds a problem.
- Check: the owner runs "Manual playthrough" and "Behaviors to check" in `CONTRACT.md`. Write the result in the process log.
- Done: yes (2026-10-30)

### 10. Before the merge

- Changes: delete `Temp084cFrozenMovement.kt` and `Temp084cMovementEquivalenceTest.kt`, in the last commit.
- Check: read the folders of the other active branches again. If `master` changed the same files, merge `master` and run the automated checks
  again. The merge needs the owner's explicit approval. After the merge, tell the owner, because `051c` waits for this branch.
- Done: temporary suite deleted and checks run (2026-10-30); the merge waits for the owner's approval

## New API and its first caller

See `SCOPE.md`, "New API and its first caller". This plan adds no other API. The temporary suite is test code.
