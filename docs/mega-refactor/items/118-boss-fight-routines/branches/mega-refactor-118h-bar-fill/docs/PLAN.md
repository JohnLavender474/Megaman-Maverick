# Plan: mega-refactor-118h-bar-fill

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-05

## Design notes

- `Fight.update(delta: Float)`: the phases run as today; then `if (!bar.finished) bar.update(delta)`. `FightDirector.update(delta)` passes it on. The screen calls `fights.update(delta)` at the same place as today.
- Order in a tick, as today: the steps first (so `FillBossHealthBar.begin` can set the bar, and its `isFinished` reads the state before this frame's fill), then the fill. In the tick where `FillBossHealthBar` begins, the fill gets its first update, so the music pause comes in the same frame as today.
- A fight that closes in a tick releases its leases first; the bar lease unsets the bar, so `finished` is `true` and no update follows.
- `BossHealthHandler.set(entity, runOnFirstUpdate, runOnFinished)`: the `BIT_BY_BIT` code, with no `when`. `FillBossHealthBar` stops passing the type.
- The screen's `when` chain keeps its other 3 branches, in the same order.

### New APIs and their first callers

None. `delta` is a new parameter of `Fight.update` and `FightDirector.update`; the callers are `FightDirector.update` and `MegaLevelScreen.render`.

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date; `ITEM.md` "Branches" and the owner decision (2026-11-05, the lean `118h`). Then `git worktree add -b mega-refactor-118h-bar-fill .worktrees/mega-refactor-118h-bar-fill master`. Recheck the overlap with the other active branches, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: no

### 2. Tests first

- Changes: add the `delta` parameter to `Fight.update` and `FightDirector.update`, still unused, and pass it from the screen and the tests, so that the tests compile. New tests in `FightTest`: the bar update comes after the steps of the tick, with the same `delta`; no update for a finished bar. In `FightStepsTest`: with a bar that finishes on its first update, `ReleaseWorld` and `ControllerOn` run in the next tick, not in the tick of the fill.
- Check: `core:test` fails only on the new tests.
- Done: no

### 3. The fight runs the fill

- Changes: the bar update in `Fight.update`; remove the bar branch from the screen's `when` chain; remove `HealthFillType`, the `type` parameter of `BossHealthHandler.set` and the `ALL_AT_ONCE` branch.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. `grep` finds no `HealthFillType`. Mutation checks, one at a time:
  - the bar update before the phases;
  - the bar update with no `finished` check;
  - no bar update.
  Confirm with `git diff` that each mutation is in the file, run the full `core:test`, and see the matching test fail. Log the result.
- Done: no

### 4. The `fights` README

- Changes: "How it runs in a frame" says that a fight runs its bar fill after its steps, and that the screen only draws the bar; "Status" loses "the health bar" from the later-branches line.
- Check: the README names no current boss, step or count.
- Done: no

### 5. Short launches

- Changes: none.
- Check: build first, then one `timeout 12` launch for each room: `GLACIER_MAN room12`, `WILY_STAGE_1 room9`. A clean exit is a pass.
- Done: no

### 6. Owner play test

- Changes: none, unless a check fails. A fix means that the checks run again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: no

### 7. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`, with the item "Possible later" rows for `HudBars`, `BarSpec` and `BarSource`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: no
