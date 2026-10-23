# Process log: mega-refactor-014a-fixture-type

## Status

- State: merged
- Goal: Narrow the engine fixture type from `Any` to `IFixtureType`
- Done: all steps. Merged into `master`.
- Next: none. Merged.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-22

- No branch exists yet. The folder was prepared on `master`.
- Scan done. Draft `docs/SCOPE.md` written for the owner to review.
- Draft scope revised: `FixtureTypeSet` and the `WorldSystem` check removed; the set moves to `014c`.
- Scope approved by the owner (index property named `ordinal`). Draft `docs/CONTRACT.md` written.
- Contract approved by the owner. Draft `docs/PLAN.md` written.
- Scope change approved by the owner: `IFixtureType` has only `index: Int` (no `name`, no `ordinal`). Scope and plan updated.
- Plan approved by the owner. Step 1: branch start on `master`, worktree created.
- Step 2: baseline on the branch: build passes, 427 engine and 77 core tests, 0 failures.
- Step 3: `IFixtureType` added; `IFixture`, `Fixture`, `Body`, `IBody`, `BodyBuilder` narrowed. Deviation:
  `FixtureBuilder` (engine, no users) also took `type: Any`. The scan missed it. It is narrowed the same
  way, because it is a fixture-type constructor path and the scope's goal is "no fixture type typed `Any`".
- Step 4: `TestFixtureType` enum replaces the string fixture types in 6 engine test files (54 literals,
  by script), plus two typed lambdas/maps the compiler pointed to. 427 engine tests pass.
- Step 5: `FixtureType : IFixtureType` with `override val index get() = ordinal`. No other core change
  was needed. 77 core tests pass, the JAR builds.
- Step 6 (automated): clean build `engine:test core:test lwjgl3:jar` passes; 133 compiler warnings, the
  same as `master`; 427 + 77 tests, 0 failures; no fixture type typed `Any` in `engine/.../world/body/`
  (only the `preProcess`/`postProcess`/`onReset` keys and `equals(Any?)`, out of scope); `Body.kt` and
  `BodyBuilder.kt` change only in types. A 25-second launch of `--level TEST_1` loaded the level with
  no exception; the only errors are the two known spawner errors from
  `docs/issues/2026-10-22-misplaced-tmx-objects.md`.
- Small QoL change outside the scope, at the owner's request: the three `run-game-*.sh` scripts pass
  extra arguments through (`$*` at the end of `--args`). Root `CLAUDE.md` notes it.
- Step 6: the owner's smoke run passed. All contract checks pass.
- Step 7: the owner approved the merge (2026-10-22). `master` gained only a docs commit since the branch start, so no merge of `master` into the branch.
