# Plan: mega-refactor-084a-player-state

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-30

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`,
`T/` = `core/src/test/kotlin/com/megaman/maverick/game/`. All commands run from
`/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084a-player-state`.

Each step ends with one commit on the branch.

## Steps

### 1. Baseline and the weapon setter test

- Changes:
  - Run `./gradlew engine:test core:test lwjgl3:jar` on the unchanged branch. Write the result in the process log.
  - New test `T/entities/megaman/MegamanCurrentWeaponTest.kt`. Build `Megaman` with `testServices()` as in
    `MegamanHeartTankTest` (no `init()`), and set the `internal` field `weaponsHandler` to a relaxed mock. Assert:
    - the first `currentWeapon = MEGA_BUSTER` calls `onChangeWeapon(MEGA_BUSTER, null)`;
    - then `currentWeapon = FRIGID_SHOT` calls `onChangeWeapon(FRIGID_SHOT, MEGA_BUSTER)`, and `currentWeapon` reads
      `FRIGID_SHOT`;
    - `onChangeWeapon` runs before the value is stored: inside the mock's answer, `currentWeapon` still reads the old
      value (on the second set).
- Check: the new test passes on the old code. So it locks the current behavior before the change.
- Done: yes (2026-10-30)

### 2. The 4 fields become typed fields

- Changes in `M/Megaman.kt:266-291`:
  - `override lateinit var facing: Facing`.
  - `lateinit var aButtonTask: AButtonTask`.
  - `currentWeapon`: `private var weapon: MegamanWeapon? = null`; getter `weapon!!`; setter
    `weaponsHandler.onChangeWeapon(value, weapon)`, then `weapon = value`.
  - `running`: `private var runningFlag = false`; getter `inPlay && runningFlag`; setter writes `runningFlag`.
  - Delete `M/constants/MegamanProps.kt` and `ConstKeys.RUNNING` (`C/Constants.kt:446`). Remove the imports.
- Check: `./gradlew core:test lwjgl3:jar` passes (with the step 1 test). `grep -rn 'MegamanProps\|ConstKeys.RUNNING'
  core/src engine/src` finds nothing.
- Done: yes (2026-10-30)

### 3. The teleport hooks move to `TeleportableComponent`

- Changes:
  - `C/entities/state/TeleportableComponent.kt`: add the constructor parameters `onStart: ((ITeleporterEntity) ->
    Unit)? = null`, `onContinue: (() -> Unit)? = null`, `onEnd: (() -> Unit)? = null` as `val`s. `reset()` does not
    change (it clears only `teleporter`).
  - `C/entities/state/Teleports.kt`: `beginTeleport` calls `teleportable.onStart?.invoke(teleporter)`, then sets
    `teleporter`. `endTeleport` calls `teleportable.onEnd?.invoke()`, then clears it. Same order as now.
  - The 5 continue reads (`CapsuleTeleporter.kt:224,250`, `PipePortal.kt:195,225`, `PortalHopper.kt:244`):
    `entity.getComponent(TeleportableComponent::class)?.onContinue?.invoke()`.
  - `M/Megaman.kt`: `addComponent(TeleportableComponent(...))` at `:338` takes the 3 lambdas. The start and end lambda
    bodies are copied from `:492-505` with no change. The continue lambda is `{ standardOnTeleportStart(this) }`.
    Delete the 3 hook lines from `onSpawn`.
  - Delete `ConstKeys.ON_TELEPORT_START`, `ON_TELEPORT_CONTINUE`, `ON_TELEPORT_END` (`C/Constants.kt:319-321`) and
    `setStandardOnTeleportContinueProp` (`C/entities/utils/GameEntityUtils.kt:151-153`). Remove the imports.
  - `T/entities/state/TeleportsTest.kt`: give the hooks to the component constructor in place of `putProperty`. Add
    a case: after `reset()`, `teleporter` is `null` and the 3 hooks are the same instances as before.
- Check: `./gradlew core:test lwjgl3:jar` passes. `grep -rn 'ON_TELEPORT_\|setStandardOnTeleportContinueProp'
  core/src engine/src` finds nothing.
- Done: yes (2026-10-30)

### 4. All automated checks and a smoke launch

- Changes: none in code.
- Check: every item of "Automated checks" in `CONTRACT.md`: `./gradlew engine:test core:test lwjgl3:jar`, the full
  `grep`, and a short launch (about 15-20 s) of `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`. A
  clean exit with code 0 is a pass. Write the results in the process log.
- Done: yes (2026-10-30)

### 5. The owner's play test

- Changes: none.
- Check: the owner runs the "Manual playthrough" and "Behaviors to check" lists in `CONTRACT.md`. Write the result in
  the process log. Then do the merge overlap check with the other active branches (see `docs/NOTES.md`).
- Done: yes (2026-10-30)

## New API and its first caller

| API | First caller | Step |
|---|---|---|
| `TeleportableComponent.onStart`, `onEnd` | `beginTeleport`, `endTeleport` in `Teleports.kt`; set by `Megaman` | 3 |
| `TeleportableComponent.onContinue` | `CapsuleTeleporter`, `PipePortal`, `PortalHopper`; set by `Megaman` | 3 |

The private backing fields `weapon` and `runningFlag` in `Megaman` (step 2) are not API.
