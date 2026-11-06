# Plan: mega-refactor-118j-gate-unlock

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: draft
- Approved: no

## Design notes

- `EventType.UNLOCK_GATE`, in the place of `INTERMEDIATE_BOSS_DEAD` and `GAUNTLET_CLEARED`, which go. Payload: `ConstKeys.KEY` (a `String`).
- `Gate`:
  - `onSpawn` reads `boss_key` and `gauntlet_key`. If both are set, it throws an `IllegalStateException` that names the gate (`mapObjectId`, `nextRoomKey`) and says to keep 1 of the 2 properties. Else `unlockKey` is the one that is set, or `null`.
  - `thisBossKey` and `gauntletKey` become 1 field, `unlockKey: String?`. `isUnlockedByDefault()` is `unlockKey == null`.
  - The mask has `UNLOCK_GATE` in the place of the 2 old events. `onEvent`: if `unlockKey` equals the event `KEY`, then `triggerable = true`, with 1 debug line.
  - The `AbstractBoss` import goes.
- `GauntletMaster` submits `Event(EventType.UNLOCK_GATE, props(ConstKeys.KEY pairTo key))`. Its class doc names `UNLOCK_GATE`.
- `FightPlan` gets `val gateKey: String?` as its last constructor parameter, with no default. `FightPlans.find` reads `props.get("${ConstKeys.BOSS}_${ConstKeys.KEY}", String::class)` and gives it to each plan; the final boss plan gets `null`. `Fight` exposes `val gateKey: String? get() = plan.gateKey`, because `plan` is private.
- `UnlockGate` (rename of `ExitMidBossFight`): `begin` returns if slot 0 is dead or `context.gateKey` is `null`; else it submits `UNLOCK_GATE` with the key. `isFinished` stays `context.leadPlayerAlive`.
- `AbstractBoss`: `bossKey` and its line in `onSpawn` go.
- The `fights` README: change only a line that names `INTERMEDIATE_BOSS_DEAD` or `ExitMidBossFight`, if one exists.
- Unused imports: the compiler does not warn on them, so count each imported name in the changed files, and remove only the imports that this branch made unused.

### New APIs and their first callers

| API | First caller |
|---|---|
| `EventType.UNLOCK_GATE` | `UnlockGate`, `GauntletMaster` (send); `Gate` (listen) |
| `FightPlan.gateKey`, `Fight.gateKey` | `FightPlans.find` (writes), `UnlockGate` (reads) |
| `UnlockGate` | `midBossPlan`, `miniBossPlan` |

## Steps

### 1. Start the branch

- Changes: on `master`, a docs-only commit: `ITEMS.md` branch row `active` with the start date; `ITEM.md` "Branches", and the owner decisions (2026-11-05: the plan reads `boss_key` from the map; 1 key namespace). Stage `ITEMS.md` so that the owner's local lines stay out. Then `git worktree add -b mega-refactor-118j-gate-unlock .worktrees/mega-refactor-118j-gate-unlock master`. Recheck the overlap with the other worktrees, and write it in `docs/NOTES.md`.
- Check: `git worktree list` shows it. `./gradlew engine:test core:test` passes on the base, with the Corretto 17 JDK.
- Done: no

### 2. Tests first

- Changes: add `EventType.UNLOCK_GATE`, `FightPlan.gateKey`, `Fight.gateKey` and the rename to `UnlockGate`, with no new behavior, so that the tests compile. New tests:
  - `GateTest` (new, with `testServices()`): a gate with `boss_key` and a gate with `gauntlet_key` unlock on `UNLOCK_GATE` with their key; a keyed gate stays locked on another key; an unlocked resettable gate is locked again on `PLAYER_SPAWN`; a gate with both properties throws at spawn. "Unlocked" means that `trigger()` moves the gate out of `isTriggerable()`.
  - `FightStepsTest`: `FightPlans.find` puts the map `boss_key` in a mid-level plan (`end=false`) and in a mini plan, and `null` when the map has none; `UnlockGate` submits `UNLOCK_GATE` with the key when slot 0 is alive, and submits nothing with no key or with slot 0 dead. The old `INTERMEDIATE_BOSS_DEAD` checks change to `UNLOCK_GATE`.
  - `MegaLevelScreenEventsTest`: the mask does not have `UNLOCK_GATE`.
- Check: `core:test` fails only on the new tests that need the new behavior.
- Done: no

### 3. One keyed unlock

- Changes: `Gate`, `GauntletMaster`, `FightPlans.find`, `UnlockGate.begin`, and the removal of `INTERMEDIATE_BOSS_DEAD`, `GAUNTLET_CLEARED` and `AbstractBoss.bossKey`.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes. `grep` finds no `INTERMEDIATE_BOSS_DEAD`, `GAUNTLET_CLEARED`, `bossKey` or `NO_BOSS_KEY` in live code (the commented-out bosses keep their old text). No import of the changed files is unused. Mutation checks, one at a time:
  - `Gate` that unlocks on any key;
  - `Gate` that reads only `boss_key`;
  - `Gate` that does not throw with both properties;
  - `FightPlans.find` that gives no key;
  - `UnlockGate` that sends also when slot 0 is dead.
  Confirm with `git diff` that each mutation is in the file, run the full `core:test`, and see a matching test fail. Log the result.
- Done: no

### 4. The `fights` README

- Changes: only if a line names the old event or the old step name.
- Check: the README names no current boss, step list or count.
- Done: no

### 5. Short launches

- Changes: none.
- Check: rebuild after the mutations, then one `timeout 12` launch for each room of the play checks: `MOON_MAN room11`, `REACTOR_MAN room12`, `WILY_STAGE_1 room6`, `GAUNTLET_1 room1`. A clean exit is a pass. The both-properties throw needs no launch: the scan found no gate with both properties in the live maps.
- Done: no

### 6. Owner play test

- Changes: none, unless a check fails. A fix means that the checks run again.
- Check: all checks in `CONTRACT.md`, "Manual playthrough". Write the result in the process log.
- Done: no

### 7. Merge

- Changes: the steps in `docs/mega-refactor/CLAUDE.md`, "Branch merge", into `master`. Stage by path, and check the squash file list.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes on `master`.
- Done: no
