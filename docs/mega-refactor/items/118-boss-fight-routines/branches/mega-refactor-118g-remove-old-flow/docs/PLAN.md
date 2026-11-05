# Plan: mega-refactor-118g-remove-old-flow

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-05

## Design notes

- `MissingFightPlanException(bossName: String)` in `fights/`, next to `FightPlans`, extends `IllegalStateException`. Message: the boss name, that `FightPlans.find` has no plan for it, and the fix (add the boss to `FightPlans.find`, or change the boss object of the boss room). No cause, because no other error comes first.
- `FightPlans.find(bossName, props): FightPlan`. The `when` branch `else -> throw MissingFightPlanException(bossName)`.
- `ENTER_BOSS_ROOM` after the change:
  ```kotlin
  val plan = FightPlans.find(bossMapObject.name, bossSpawnProps)
  if (plan.endsLevel && game.gameState.isLevelDefeated(game.levelState.currentLevel!!)) {
      eventsMan.submitEvent(Event(EventType.VICTORY_EVENT))
      return
  }
  bossSpawnProps.put(ConstKeys.BOUNDS, bossMapObject.rectangle.toGameRectangle())
  FightPlans.resolve(plan, bossSpawnProps)
  val boss = bossSpawnEventHandler.init(bossMapObject.name, bossSpawnProps)
  fights.start(boss, plan, game.services, bossHealthHandler) { music }
  ```
- `AbstractBoss.onReady()` keeps its log and `betweenReadyAndEndBossSpawnEvent = true`, and loses the event. The defeat timer end keeps `destroy()` and loses the `BOSS_DEAD` submit.
- Imports that become unused go, in each file. The Kotlin compiler does not warn on an unused import, so step 3 also counts each imported name in the changed files, and removes only the imports that this branch made unused.

### New APIs and their first callers

| API | First production caller |
|---|---|
| `MissingFightPlanException` | `FightPlans.find` |

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date, `ITEM.md` "Branches". Then `git worktree add -b mega-refactor-118g-remove-old-flow .worktrees/mega-refactor-118g-remove-old-flow master`. Recheck the overlap with the other active branches, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: yes

### 2. Tests first

- Changes: add `MissingFightPlanException`, with no caller yet, so that the tests compile. In `FightTest`, the "no plan" test expects the exception for `MechaDragon_OLD`, `ReactorMan_OLD`, `SphinxMiniBoss` and a name that is no boss, with the boss name in the message. In `MegaLevelScreenEventsTest`, a test that the mask has none of `END_BOSS_SPAWN`, `BOSS_DEFEATED` and `INTERMEDIATE_BOSS_DEAD`, and has `ENTER_BOSS_ROOM` and `VICTORY_EVENT`.
- Check: `core:test` fails only on the new or changed tests.
- Done: yes

### 3. Remove the old flow

- Changes: all of `SCOPE.md`, "In scope", items 1 to 6: the screen branches, the mask entries and the `ENTER_BOSS_ROOM` route; `find` throws; the 3 events and their sends; `getEventOnDefeated`, `AbstractBoss.end`, `getHealthFillType` and `ConstKeys.HEALTH_FILL_TYPE`; `FightDirector.owns`; the old-branch tests, the `owns` test and the `getEventOnDefeated` stubs.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. No new compiler warning in the changed files. `grep` finds no use of the removed names in `core/src`. Mutation checks, one at a time:
  - `find` returns a plan for an unknown boss in place of the throw;
  - the exception message leaves out the boss name;
  - `END_BOSS_SPAWN` back in the screen mask.
  Confirm with `git diff` that each mutation is in the file, run the full `core:test`, and see the matching test fail. Log the result.
- Done: yes

### 4. The `fights` README

- Changes: "Status" and "Events", as in `SCOPE.md`, item 7.
- Check: the README names no current boss, step or count (root `CLAUDE.md`, "Package READMEs"). `grep` finds no "old branches" or "guard" text left in it.
- Done: yes

### 5. Short launches

- Changes: none.
- Check: build first, then one `timeout 12` launch for each room: `GLACIER_MAN room12`, `WILY_STAGE_1 room9`, `MOON_MAN room11`, `WILY_STAGE_3 room8`. A clean exit is a pass.
- Done: yes

### 6. Owner play test

- Changes: none, unless a check fails. A fix means that the checks run again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: yes

### 7. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: no
