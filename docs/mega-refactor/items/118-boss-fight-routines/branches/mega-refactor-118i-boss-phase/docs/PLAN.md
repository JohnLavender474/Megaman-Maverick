# Plan: mega-refactor-118i-boss-phase

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-05

## Design notes

- `BossComponent` and `BossPhase` in `entities/state/BossComponent.kt`:
  ```kotlin
  enum class BossPhase { ENTRANCE, INTRO, FIGHT }

  // TODO: 048 step 6 moves the boss hooks (isReady, preReady, onReady, onEndBossSpawnEvent) into this component
  class BossComponent : IGameComponent {
      var phase = BossPhase.ENTRANCE
      override fun reset() { phase = BossPhase.ENTRANCE }
  }
  ```
- `AbstractBoss` has `val bossComponent = BossComponent()` as a field, so it exists before `init()`. `init()` adds it with `addComponent`. The readers read `bossComponent.phase` on the boss (a field read, as today), not `getComponent`.
- The `AbstractBoss` updatable: `if (bossComponent.phase == ENTRANCE) { preReady(delta); if (isReady(delta)) { bossComponent.phase = INTRO; onReady() } }`. The base `onReady()` keeps only its log line. `onSpawn` sets `ENTRANCE` where it reset the 2 flags. `onDestroy` loses `ready = false`, because the engine resets the component before `onDestroy`.
- `canDamage`: `phase != ENTRANCE` in place of `ready`.
- `StartBossFight` in `FightSteps.kt`: `begin` sets `context.boss.bossComponent.phase = FIGHT`; `isFinished` is `true`. No lease: the phase is the boss's own state, and the boss resets it.
- `AwaitBossReady.isFinished`: `phase != ENTRANCE`.
- The reader map:
  - `if (betweenReadyAndEndBossSpawnEvent) return` (8 files) becomes `if (bossComponent.phase == BossPhase.INTRO) return`.
  - `ready && !betweenReadyAndEndBossSpawnEvent` (`BigAssMaverickRobot`) becomes `phase == FIGHT`; in `BigAssMaverickRobotHand`, `owner?.bossComponent?.phase == BossPhase.FIGHT`.
  - `!ready || betweenReadyAndEndBossSpawnEvent` (`PreciousWoman` sprite) becomes `phase != FIGHT`.
  - `!(parent as SigmaRat).ready` becomes `(parent as SigmaRat).bossComponent.phase == BossPhase.ENTRANCE`.
- `BossSpawnEventHandler` loses `boss.ready = false`.
- Unused imports: the compiler does not warn on them, so count each imported name in the changed files, and remove only the imports that this branch made unused.

### New APIs and their first callers

| API | First caller |
|---|---|
| `BossComponent`, `BossPhase` | `AbstractBoss`, `AwaitBossReady`, `StartBossFight`, the 12 reader files |
| `AbstractBoss.bossComponent` | the same readers |
| `StartBossFight` | `stageBossPlan` and `miniBossPlan` |

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date; `ITEM.md` "Branches", and the owner decisions (2026-11-05: phase only; the x6 branch adapts `D1000`; the `TODO` to `048` step 6). Then `git worktree add -b mega-refactor-118i-boss-phase .worktrees/mega-refactor-118i-boss-phase master`. Recheck the overlap with the other worktrees, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: yes

### 2. Tests first

- Changes: add `BossComponent`, `BossPhase`, `AbstractBoss.bossComponent` (not yet written by the boss) and an empty `StartBossFight` (no `begin`), so that the tests compile. New tests:
  - `BossComponentTest`: a new component has `ENTRANCE`; `reset()` sets `ENTRANCE` after `FIGHT`.
  - A boss built with `testServices()` and not spawned has its component, with `ENTRANCE`.
  - `FightStepsTest`: the `StageFight` mock boss gets a real `BossComponent`. `AwaitBossReady` ends only when the phase is not `ENTRANCE`; `StartBossFight` sets `FIGHT`; in a stage intro with a fill, the phase is `FIGHT` in the tick after the fill ends, and still `INTRO` in the tick of the fill.
  - `FightStepsTest` (it builds plans with `FightPlans.find`): the mini intro has `StartBossFight` right after `EndBossSpawn`; the stage intro has it right after `FillBossHealthBar`.
- Check: `core:test` fails only on the new tests that need the new behavior.
- Done: yes

### 3. The phase replaces the flags

- Changes: `AbstractBoss` (the updatable, `onSpawn`, `onDestroy`, `onReady`, `onEndBossSpawnEvent`, `canDamage`); remove `ready` and `betweenReadyAndEndBossSpawnEvent`; `StartBossFight.begin`; `AwaitBossReady`; `StartBossFight` in the 2 plans; the 12 reader files as in the reader map; `BossSpawnEventHandler`.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. `grep` finds no `betweenReadyAndEndBossSpawnEvent` and no `.ready` of a boss in live code (the commented-out bosses keep their old text). No import of the changed files is unused. Mutation checks, one at a time:
  - `StartBossFight` with no `begin`;
  - `StartBossFight` before `FillBossHealthBar` in the stage plan;
  - `AwaitBossReady` that reads `phase == FIGHT`;
  - `BossComponent.reset()` that does nothing.
  Confirm with `git diff` that each mutation is in the file, run the full `core:test`, and see a matching test fail. Log the result.
- Done: yes

### 4. The `fights` README

- Changes: a short part on the boss phase: the boss sets the intro phase when it is ready, and a fight step starts the fight phase, so the plan decides when the boss AI starts. The status line names the boss hooks that stay in `AbstractBoss` until `048`.
- Check: the README names no current boss, step list or count.
- Done: yes

### 5. Short launches

- Changes: none.
- Check: rebuild after the mutations, then one `timeout 12` launch for each room of the play checks: `INTRO_STAGE room12`, `PRECIOUS_WOMAN room7`, `GLACIER_MAN room12`, `MOON_MAN room11`. A clean exit is a pass.
- Done: yes

### 6. Owner play test

- Changes: none, unless a check fails. A fix means that the checks run again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: yes

### 7. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`. The process log entry names the 2 `D1000` lines (`D1000.kt:556`, `:583`) and their new reads, for the `x6-intro-extra-level` merge of `master`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: yes
