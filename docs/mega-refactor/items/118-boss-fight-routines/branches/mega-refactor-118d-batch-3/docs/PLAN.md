# Plan: mega-refactor-118d-batch-3

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-04

## Design notes

- No new type, step, plan or parameter. In the `when` of `FightPlans.find`:
  - `ElecDevil.TAG -> MusicAsset.JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC`;
  - `GutsTank.TAG -> MusicAsset.MMX6_SIGMA_2_BATTLE_INTRO_MUSIC`;
  - `SigmaRat.TAG` goes in the branch that gives `null` music.
- The music moves as for `BigAssMaverickRobot` in `118a`: `resolve` writes the plan music to the boss spawn props before the spawn, and `AbstractBoss.onSpawn` reads it as today. The forced line in each boss's `onSpawn` goes (`ElecDevil.kt:139`, `GutsTank.kt:204`). The value is the same, so the boss plays the same track.
- The frame match is the same as in `118a` for a standard plan (`118a` `PLAN.md`, "Frame match").

### New APIs and their first callers

None.

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date, `ITEM.md` "Branches". Then `git worktree add -b mega-refactor-118d-batch-3 .worktrees/mega-refactor-118d-batch-3 master`. Recheck the overlap with the other active branches for `fights/`, `MegaLevelScreen`, `AbstractBoss` and the 3 bosses, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: yes

### 2. Tests first

- Changes: in `FightTest`:
  - the 3 bosses move from the "no plan" list to the moved-boss list;
  - `resolve` writes `JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC` for `ElecDevil` and `MMX6_SIGMA_2_BATTLE_INTRO_MUSIC` for `GutsTank`;
  - `SigmaRat` has no plan music;
  - each of the 3 gives `standardBossPlan` for no `end` and for `end=true`.
- Check: `core:test` fails only on the new or changed tests, because `find` does not know the 3 bosses yet.
- Done: yes

### 3. Move the 3 bosses

- Changes: add the 3 bosses to `FightPlans.find`. Remove the forced music line from `ElecDevil.onSpawn` and `GutsTank.onSpawn`, and any import that becomes unused.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. Mutation checks, one at a time:
  - remove each of the 3 bosses from `find` (3 runs);
  - give `ElecDevil` the `GutsTank` music;
  - give `SigmaRat` a music value;
  - make the `end` read always `false`.
  Confirm with `git diff` that each mutation is in the file, run the full `core:test`, and see the matching test fail. Log the result.
- Done: yes

### 4. Short launches

- Changes: none.
- Check: build first, then one `timeout 12` launch for each room: `WILY_STAGE_1 room10`, `WILY_STAGE_2 room9`, `MOON_MAN room11`. A clean exit is a pass.
- Done: yes

### 5. Owner play test

- Changes: none, unless a check fails. A fix means that the control check runs again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: yes

### 6. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: no
