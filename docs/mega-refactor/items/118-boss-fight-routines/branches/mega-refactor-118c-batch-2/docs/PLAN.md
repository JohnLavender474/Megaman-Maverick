# Plan: mega-refactor-118c-batch-2

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-03

## Design notes

- No new type, step, plan or parameter. The 5 bosses go in the `when` of `FightPlans.find`, in the branch that gives `null` music, next to `TimberWoman`, `MoonMan` and `ReactorMan`. The plan comes from `end`, as in `118b`.
- The frame match is the same as in `118b` (`118b` `PLAN.md`, "Frame match at the end of a mid-level fight").
- `master` moved after the scope scan: `068a` (slopes) and `019c` (map spawns go straight to the engine). `019c` changes the map spawn path in `MegaLevelScreen`, not the `ENTER_BOSS_ROOM` route or the boss spawn. Step 1 checks this again in the worktree.

### New APIs and their first callers

None.

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date, `ITEM.md` "Branches". Then `git worktree add -b mega-refactor-118c-batch-2 .worktrees/mega-refactor-118c-batch-2 master`. Recheck the overlap with the other active branches (`compactor-level`, `x6-intro-extra-level`) for `fights/`, `MegaLevelScreen`, `AbstractBoss` and the 5 bosses, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: no

### 2. Tests first

- Changes: in `FightTest`, a test that `find` gives a plan with no music for each of the 5 bosses, `midBossPlan` for `end=false`, and `standardBossPlan` for `end=true` and for no `end`. Add the 5 bosses to the moved-boss list, and remove them from the "no plan" list.
- Check: `core:test` fails only on the new tests and the moved-boss list, because `find` does not know the 5 bosses yet.
- Done: no

### 3. Move the 5 bosses

- Changes: add `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan` and `PreciousWoman` to `FightPlans.find`, with no music.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. Mutation checks, one at a time: remove each new boss from `find`; give one new boss a music value; make the `end` read always `true`. Confirm with `git diff` that each mutation is in the file, run the full `core:test`, and see the matching test fail. Log the result.
- Done: no

### 4. README

- Changes: in `core/.../fights/README.md`, change the supplier line to "a value that the screen owns". Add the rule "a fight starts only when no fight is open", with the reason: each fight room is shut by a gate that opens only on the fight's exit event, after the leases are released. Next to the `FightDirector` part, add a short warning: `start` replaces an open fight with no cancel, so its leases would not be released.
- Check: the text names no current boss, step list or count, and its Mermaid blocks render.
- Done: no

### 5. Short launches

- Changes: none.
- Check: build first, then one `timeout 12` launch for each room: `GLACIER_MAN room12`, `DESERT_MAN room16`, `INFERNO_MAN room11`, `RODENT_MAN room16`, `PRECIOUS_WOMAN room7`, `WILY_STAGE_1 room9`, `WILY_STAGE_2 room4`, `WILY_STAGE_2 room8`, `WILY_STAGE_3 room4`. A clean exit is a pass.
- Done: no

### 6. Owner play test

- Changes: none, unless a check fails. A fix means that the control checks run again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: no

### 7. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: no
