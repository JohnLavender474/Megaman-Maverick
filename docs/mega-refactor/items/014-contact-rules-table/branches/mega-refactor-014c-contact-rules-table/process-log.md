# Process log: mega-refactor-014c-contact-rules-table

## Status

- State: merged
- Goal: Replace the contact `if/else` chain with an engine rule table and per-pair rule classes
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-22

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `0edd9649c`. Draft `docs/SCOPE.md` written; it proposes a split into `014c` (table) and `014d` (receivers and flags).
- Scope approved by the owner (split into `014c`/`014d`, reference file committed, `owns` checked in dispatch, `FixtureTypeSet` in `world/body/`). `014d` added as a planned branch. Draft `docs/CONTRACT.md` written.
- Contract approved by the owner. Draft `docs/PLAN.md` written.
- Plan approved by the owner. Step 1: branch start on `master`, worktree created.
- Step 2: baseline: 436 engine and 77 core tests, 115 compiler warnings.
- Step 3: `ContactRuleIds` (35 IDs, one per pair) and 80 hook lines in the old chain, one per branch, right
  after the role destructuring (the per-phase counts match the phase coverage in `NOTES.md`).
- Step 4: `ContactRuleSelectionTest` and `core/src/test/resources/contact-rule-selection.txt` (2523 lines),
  made from the old chain with `./gradlew core:test -PupdateContactRuleSelection`. **Stage-0 commit: `4dad76012`.**
  Matched ordered pairs per phase without `CONSUMER`: 93 / 57 / 33, which matches 48 / 29 / 17 pairs.
  A mutation check (one line changed) makes the test fail.
  Deviation from the plan: the role order (`12`/`21`) is recorded only for same-type pairs. For two different
  types the roles are bound by type, and the old chain names some pairs in a different order per phase (for
  example `BLOCK`-`SIDE` in begin, `SIDE`-`BLOCK` in continue and end), while the table has one order per pair.
  Only the same-type pairs (`BODY`, `SIDE`, `PROJECTILE`, `CONSUMER`) depend on the contact order.
- Step 5: `FixtureTypeSet` in engine `world/body/` (`of`, `validated`, `named`, `owns`, iterable) with 8 tests.
- Step 6: engine `IContactRule`, `ContactRulesBuilder` (build checks: same pair from two rules, a pair twice in a
  phase, a set rule with a type on both sides, a wildcard clash, a type not in the set), `ContactRuleTable`
  (`dispatch` with the `owns` check, `ruleId`, `select`) and `ContactRuleDispatcher`, with 12 tests. The builder
  copies its arrays into the table.
- Step 7: `IContactListener` takes `(fixture1, fixture2, delta)`; `WorldSystem` passes the contact's fixtures; the
  old listener ran its chain on a private scratch `Contact`. The selection test still matched.
- Step 8: 24 rule files in `world/contacts/rules/` plus `MegaContactRules.kt` (35 registrations, the same IDs and
  phases as the chain), `ContactValues` for the friction constants (`Penguin` uses it). The table matches the
  reference file on all 2523 lines; removing one phase from one registration makes the test fail.
  Body check by script: 69 of 80 old branch bodies appear unchanged in the rule files (with `ProcessState.X` and the
  merged `state` parameter treated as one). The other 11 were checked by hand: the damage rule's three value logs
  take the two fixtures, and the shared `HitByFeetRule`/`HitByExplosionRule` name the target `targetFixture`.
  Where the bodies of two or three phases were the same except for the `ProcessState` value (consumer, body-side,
  side-side, hit-by-feet, feet-snow begin/continue, laser-body, body-force with its sense value), they share one
  private function with a parameter. Where they were identical (death, side-ice begin/continue, block-gravity,
  teleporter, laser-shield, hit-by-explosion), both phases call one function.
  Imports: each rule file uses `world.body.*` and the same explicit `utils.extensions` imports as the old file, so
  overloaded extensions (`getCenter`, `getPositionPoint`, `getBoundingRectangle`) bind to the same functions.
  `PROJECTILE` is a set rule for 6 types plus a same-type pair with the same ID and rule (the builder rejects a
  set rule with a type on both sides).
- Step 9: the game uses `ContactRuleDispatcher(megaContactRules(...))`; the debug filter takes two fixtures; the
  dispatcher logs `phase ruleId fixtures`; `MegaContactListener.kt` and the 6 `Contact` type helpers (with 2
  `ContactTest` cases) are deleted. The selection test runs on the table only; its update switch now writes
  from the table, for intended rule changes. Root `CLAUDE.md` describes the table. The reference file is
  unchanged since the stage-0 commit `4dad76012`. Launches of `TEST_1` and `GLACIER_MAN` show no exception.
- Step 10 (automated): clean build passes; 454 engine (456 - 2 deleted helper tests + 0) and 79 core tests,
  0 failures; 114 compiler warnings (115 before: the laser unchecked casts went from 4 to 3); no `Contact` helper
  is used; the dispatch path does not allocate (review).
- Step 10: the owner's play test passed. All contract checks pass.
- Step 11: the owner approved the merge (2026-10-22).
