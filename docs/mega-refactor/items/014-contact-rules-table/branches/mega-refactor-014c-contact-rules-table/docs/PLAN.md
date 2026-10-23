# Plan: mega-refactor-014c-contact-rules-table

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-22

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`.

The order keeps the game working after every step. The old chain stays in use until step 9, and
the selection test, made from the old chain in step 4, checks the new table before the game
switches to it.

## Steps

### 1. Branch start

- Changes:
  - On `master`, in a docs-only commit: set the `014c` row in `ITEMS.md` to `active` with the start
    date, mark it active under "Branches" in `ITEM.md`, and set the branch `process-log.md` state to
    `active`.
  - `git worktree add -b mega-refactor-014c-contact-rules-table .worktrees/mega-refactor-014c-contact-rules-table master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 436 engine, 77 core) and the warning
  count (expected 115) in the process log.
- Done: yes

### 3. Stage 0: rule IDs and the hook in the old chain

- Changes:
  - `core/.../world/contacts/ContactRuleIds.kt`: one `const val` per rule, named after the pair (for
    example `FEET_BLOCK = "feet-block"`, `CONSUMER = "consumer"`, `PROJECTILE = "projectile"`).
    The same ID is used by all phases of a pair.
  - `MegaContactListener`: an optional hook `var onRule: ((String, IFixture, IFixture) -> Unit)? =
    null`. In each of the 80 branches, right after the role destructuring, one line
    `onRule?.invoke(ContactRuleIds.X, roleA, roleB)`. The `SIDE`-`SIDE` branches use `contact`'s two
    fixtures. Nothing else changes.
- Check: `./gradlew core:test lwjgl3:jar` passes. Each of the 80 branches has exactly one hook line
  (count). A 25-second launch of `--level TEST_1` shows no exception.
- Done: yes

### 4. Stage 0: the selection test and the reference file

- Changes:
  - `core/src/test/.../world/contacts/ContactRuleSelectionTest.kt`. For each phase and each ordered
    pair `(t1, t2)` of the 29 `FixtureType` values, it builds two fixtures on an abstract `Body`
    (`Fixture(body, t1)`, `Fixture(body, t2)`), calls the listener with them, and records the hook
    call: the rule ID and the role order (`12` if `fixture1` is role A, `21` if not). The hook throws a
    private exception after it records, so the rule body never runs; the test catches it. With no
    hook call, the line is `-`.
  - The listener is built with a relaxed MockK `MegamanMaverickGame` and a debug filter that
    returns `false`.
  - Output, one line per phase and ordered pair: `BEGIN FEET BLOCK -> feet-block 12`. The test
    compares it with `core/src/test/resources/contact-rule-selection.txt`. If the system property
    `updateContactRuleSelection` is set, it writes the file instead (to make it once).
  - Make the file once from the old chain, read it, and commit it. Write the commit SHA in the
    process log as the "stage-0 commit".
- Check: `./gradlew core:test` passes with the new test. The file has 3 × 29 × 29 = 2523 lines; the
  matched lines per phase are 48 + 29 + 17 exact pairs plus the `CONSUMER` lines (from `NOTES.md`).
- Done: yes

### 5. Stage 1: `FixtureTypeSet`

- Changes: `engine/.../world/body/FixtureTypeSet.kt` with `of(entries)` (for an enum that implements
  `IFixtureType`), `validated(list)` (indexes `0..n-1` in list order, else it throws with the type),
  `named(vararg names)` (creates the types and assigns the indexes; `toString()` is the name),
  `size`, `get(index)`, `owns(type)`. Tests in `FixtureTypeSetTest`.
- Check: `./gradlew engine:test` passes.
- Done: yes

### 6. Stage 1: the engine table

- Changes (engine `world/contacts/`):
  - `IContactRule`: `onBegin`, `onContinue`, `onEnd`, each `(a: IFixture, b: IFixture, delta: Float)`,
    default no-op.
  - `ContactRulesBuilder(types: FixtureTypeSet)`: `pair`, `sets`, `wildcard`, `build()`, with the
    build-time checks from `SCOPE.md`.
  - `ContactRuleTable`: slots per phase, the `flip` flag, the IDs; `select(phase, f1, f2)` returns the
    rule ID and the role order for tests; `dispatch(phase, f1, f2, delta)` checks `owns` for both
    types, then calls the rule with no allocation.
  - `ContactRuleDispatcher(table, debugFilter, log)`: implements `IContactListener` once it takes
    fixtures (step 7). Until then it is a plain class with `beginContact(f1, f2, delta)` and so on.
  - Tests (`ContactRuleTableTest`): role order in both contact orders; a same-type pair keeps the
    contact order; the wildcard expands to every type and `(CONSUMER, CONSUMER)` uses `fixture1`; a
    set rule expands; each build check throws; `dispatch` throws for a type that is not owned; a
    phase with no rule does nothing.
- Check: `./gradlew engine:test` passes.
- Done: yes

### 7. `IContactListener` takes fixtures

- Changes:
  - `IContactListener`: `beginContact(fixture1, fixture2, delta)` and so on.
  - `WorldSystem.processContacts` passes `it.fixture1, it.fixture2`.
  - `MegaContactListener` adapts with no change to the chain: each method sets a private scratch
    `Contact` to the two fixtures and runs the chain on it as before. The debug filter still takes
    that `Contact`.
  - `ContactRuleDispatcher` now implements `IContactListener`.
  - The selection test calls the listener with fixtures.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes; the selection test still matches the
  reference file. A launch of `--level TEST_1` shows no exception.
- Done: yes

### 8. Stage 2: the rule classes and the core table

- Changes:
  - `core/.../world/contacts/rules/`: one class per pair, as decided (about 18 files with real logic,
    a few generic or shared classes for the receiver-forward, mirror and surface pairs, and one
    `ProjectileRules.kt`). Each body moves from the chain with only the mechanical changes in
    `SCOPE.md`. A generic class is allowed only where the bodies are the same character for
    character today.
  - `core/.../world/contacts/MegaContactRules.kt`: `fun megaContactRules(game): ContactRuleTable`,
    one registration per rule with the `ContactRuleIds` constants, the same phases as the chain, and
    `FixtureTypeSet.of(FixtureType.entries)`.
  - The friction constants move to a core object (for example `ContactValues`); `Penguin.kt` uses it.
  - The selection test also runs against `megaContactRules(game).select(...)` and must give the same
    reference file. Core table tests: it builds, every `FixtureType` is owned.
  - Review each rule class against its old branch (`git diff --color-moved` against the listener,
    and by reading).
- Check: `./gradlew core:test lwjgl3:jar` passes; both the old chain and the table match the
  reference file. The game still uses the old chain.
- Done: yes

### 9. Stage 2: switch the game to the table and delete the old chain

- Changes:
  - `MegamanMaverickGame.createGameEngine()` uses `ContactRuleDispatcher(megaContactRules(this), ...)`.
  - The debug log: the dispatcher logs `phase ruleId fixture1 fixture2` when the debug filter
    accepts the fixtures. `CONTACT_LISTENER_DEBUG_FILTER` becomes `(IFixture, IFixture) -> Boolean`.
    The three value logs stay in the damage rule, behind the same filter.
  - Delete `MegaContactListener.kt` (with the hook) and the 6 type helpers in `Contact`, and their
    tests in `ContactTest`. The selection test now runs only against the table.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes. The reference file is unchanged since
  the stage-0 commit. Launches of `--level TEST_1` and `--level GLACIER_MAN` show no exception.
- Done: yes

### 10. Contract checks

- Changes: none, unless a check fails.
- Check:
  - A clean `./gradlew clean engine:test core:test lwjgl3:jar`; compare with the baseline.
  - The checks and reviews in `CONTRACT.md`.
  - At merge time on `master`: close `docs/issues/2026-10-22-wrong-contact-log-texts.md`.
  - The owner's play test from `CONTRACT.md`.
  - Write the results in the process log.
- Done: yes

### 11. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval. Compare with `master` first, and merge `master` into the branch if it touches
  the same files.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: yes
