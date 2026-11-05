# Plan: mega-refactor-118f-final-boss

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: draft
- Approved: no

## Design notes

- `finalBossPlan()` is `stageBossPlan(MusicAsset.OUR_BLOOD_BOILS_8_BIT)` with no exit steps. `stageBossPlan` already takes the exit as a `vararg`, so the outro ends at `AwaitBossDefeatEnd`. In the `when` of `find`: `WilyFinalBoss.TAG -> return finalBossPlan()`. So `find` reads no `mini` or `end` for it, as the boss forced `END=false` and set no `mini`.
- `FightPlan.endsLevel` is a property: `outro.contains(LevelVictory, true)`.
- `ENTER_BOSS_ROOM` in `MegaLevelScreen`:
  ```kotlin
  val plan = FightPlans.find(bossMapObject.name, bossSpawnProps)
  val endsLevel = plan?.endsLevel ?: (!mini && end)
  if (endsLevel && game.gameState.isLevelDefeated(levelDef)) { submit VICTORY_EVENT; return }
  bossSpawnProps.put(ConstKeys.BOUNDS, ...)
  if (plan != null) FightPlans.resolve(plan, bossSpawnProps)
  ```
  `find` only reads the props, so it can run before the `BOUNDS` write. The `mini` and `end` reads stay for a boss with no plan, until `118g`.
- `WilyFinalBoss.onSpawn`: remove the `MUSIC` and `END` lines, and the `MusicAsset` import, which only the `MUSIC` line uses. `AbstractBoss.end` is private, and its only reader is `getEventOnDefeated` (`AbstractBoss.kt:236`), which `WilyFinalBoss` overrides. So `END` has no effect.
- Frames: the intro and the outro up to `AwaitBossDefeatEnd` are the stage plan, with the same frame match as in `118a`. The fight closes in tick D+1 after the boss `destroy()` in frame D. `onDestroy` runs at the start of `engine.update` D+1, after that tick, as it does today, so the end scene does not move.

### New APIs and their first callers

| API | First production caller |
|---|---|
| `finalBossPlan()` (private, `FightPlans.kt`) | `FightPlans.find` |
| `FightPlan.endsLevel` | `MegaLevelScreen` at `ENTER_BOSS_ROOM` |

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date, `ITEM.md` "Branches". Then `git worktree add -b mega-refactor-118f-final-boss .worktrees/mega-refactor-118f-final-boss master`. Recheck the overlap with the other active branches for `fights/`, `MegaLevelScreen`, `AbstractBoss` and `WilyFinalBoss`, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: no

### 2. Tests first

- Changes: in `FightTest`:
  - `WilyFinalBoss` moves from the "no plan" list to the moved-boss list;
  - `WilyFinalBoss` gives the exact stage intro and the outro `BossDefeatLock`, `NotifyListenersOfBossDefeat`, `HideBossHealthBar`, `AwaitBossDefeatEnd`, with no props, `end=true`, `end=false` and `mini=true`;
  - `resolve` writes `OUR_BLOOD_BOILS_8_BIT` for `WilyFinalBoss`;
  - `endsLevel` is `true` for a stage plan and `false` for a mid-level, a mini and the final plan.
  Add `endsLevel` with the value `false` first, so that the tests compile.
- Check: `core:test` fails only on the new or changed tests.
- Done: no

### 3. The final plan, the level check and the boss

- Changes: `endsLevel`, `finalBossPlan()` and the `WilyFinalBoss` entry in `find`; the `ENTER_BOSS_ROOM` order and check in `MegaLevelScreen`; remove the `MUSIC` and `END` lines from `WilyFinalBoss.onSpawn`, and any import that becomes unused.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. Mutation checks, one at a time:
  - remove `WilyFinalBoss` from `find`;
  - give it a different music;
  - give it `standardBossPlan` (with `LevelVictory`);
  - `endsLevel` always `true`;
  - `endsLevel` always `false`.
  Confirm with `git diff` that each mutation is in the file, run the full `core:test`, and see the matching test fail. Log the result. The screen change has no unit test; the `endsLevel` tests and the play checks cover it.
- Done: no

### 4. The `fights` README

- Changes: the open-fight rule says that a gate or a spawned block shuts each fight room.
- Check: the README names no current boss, step or count (root `CLAUDE.md`, "Package READMEs").
- Done: no

### 5. Short launches

- Changes: none.
- Check: build first, then one `timeout 12` launch for each room: `WILY_STAGE_3 room8`, `GLACIER_MAN room12`. A clean exit is a pass.
- Done: no

### 6. Owner play test

- Changes: none, unless a check fails. A fix means that the control check runs again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: no

### 7. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: no
