# Plan: mega-refactor-014b-typed-fixture-receivers

How to do the work in `SCOPE.md`. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-22

Build with the full JDK 17: `JAVA_HOME=~/.jdks/corretto-17.0.13`.

The order keeps every commit consistent at run time: first the old functions start to use the new
keys internally (step 4), then the readers move (step 5), then the registration sites (step 6),
and only then the old functions go (step 7). At no step does a reader look under a different key
than the writer used.

## Steps

### 1. Branch start

- Changes:
  - On `master`, in a docs-only commit: set the `014b` row in `ITEMS.md` to `active` with the start
    date, list it as active under "Branches" in `ITEM.md`, and set the branch `process-log.md` state
    to `active`.
  - `git worktree add -b mega-refactor-014b-typed-fixture-receivers .worktrees/mega-refactor-014b-typed-fixture-receivers master`.
- Check: `git worktree list` shows the worktree on the new branch.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run `./gradlew clean engine:test core:test lwjgl3:jar`.
- Check: the build passes. Write the test counts (expected 427 engine, 77 core) and the compiler
  warning count (expected 133) in the process log.
- Done: yes

### 3. Engine: `PropertyKey`, `PhasedPropertyKey`, typed accessors

- Changes:
  - `engine/.../common/objects/PropertyKey.kt`: `class PropertyKey<T : Any>(val name: String)` with
    identity equality (no `equals`/`hashCode` override) and `toString() = name`.
  - `engine/.../common/objects/PhasedPropertyKey.kt`: one `PropertyKey<T>` for each `ProcessState`,
    built once in an array; `operator fun get(state: ProcessState): PropertyKey<T>`; names
    `"${name}_${state.name}"`.
  - `IPropertizable`: `putProperty(key: PropertyKey<T>, value: T)`, `getProperty(key: PropertyKey<T>):
    T?` (the one unchecked cast), `hasProperty(key: PropertyKey<*>)`, `removeProperty(key:
    PropertyKey<*>)`. They store in the same `properties` map.
  - If an overload is ambiguous with the existing `Any`-keyed functions, give the typed functions
    their own names and write the reason in the process log.
  - Engine tests (`PropertyKeyTest`): put and get through the typed accessors; two keys with the same
    name are different keys; a phased key gives the same `PropertyKey` object for the same state,
    and different objects for different states; `hasProperty` and `removeProperty` work; a typed key
    and a string key with the same text do not collide.
- Check: `./gradlew engine:test` passes, with the new tests.
- Done: yes

### 4. Core: `Receivers`, typed fixture accessors; old functions use the new keys

- Changes:
  - `core/.../world/body/Receivers.kt`:
    - `object Receivers` with one key for each kind in the `SCOPE.md` table, except `hitByLaser`.
      Function types with named parameters, for example
      `val HIT_BY_BLOCK = PhasedPropertyKey<(block: Block, delta: Float) -> Unit>("hit_by_block")`.
      `hitByBlock`, `hitByFeet` and `forceAlteration` are phased; the other kinds keep their shape.
    - `IFixture.setReceiver(key, receiver): IFixture`, `IFixture.setReceiver(key, state, receiver):
      IFixture`, `IFixture.receiver(key)`, `IFixture.receiver(key, state)`.
  - In `IFixtureExtensions.kt` and `FixtureLabel.kt`: change the body of every old receiver function
    to store and read through the `Receivers` keys. Their signatures and behavior stay the same.
- Check: `./gradlew core:test lwjgl3:jar` passes. The game now runs on the new keys, with no call site
  changed. A 25-second launch of `--level TEST_1` shows no exception.
- Done: yes

### 5. Migrate the readers by hand

- Changes:
  - `MegaContactListener.kt` (93 reads) and `MegaContactFilter.kt` (3 reads) call
    `receiver(Receivers.X)` / `receiver(Receivers.X, state)` directly. A `hasX()` + `getX()` pair
    becomes one `receiver(...)?.invoke(...)`, at the same place in the code.
  - Keep the four special cases, written out at the read site:
    - velocityAlteration: `receiver(Receivers.VELOCITY_ALTERATION)!!.invoke(...)`;
    - stickToBlock: `receiver(...)?.invoke(...) ?: true`;
    - forceAlteration: call the receiver if present, else `VelocityAlterator.alterate(...)`;
    - the no-projectile-collision exception: `receiver(...)?.invoke(...) ?: false`.
  - Delete the two `hitByLaser` reads (`L:806`, `L:1248`). No entity registers this kind.
- Check: `./gradlew core:test lwjgl3:jar` passes. No old read function is used in the listener or
  the filter (grep). A launch of `--level TEST_1` shows no exception.
- Done: yes

### 6. Migrate the registration sites by script

- Changes:
  - A script (in the scratch folder, not committed) rewrites each call
    `.setXReceiver(state) {` / `.setXReceiver {` / `.setConsumer {` / ... to
    `.setReceiver(Receivers.X, state) {` / `.setReceiver(Receivers.X) {`, labelled returns
    `return@setXReceiver` to `return@setReceiver`, and the imports.
  - Fix by hand the sites the script cannot handle: the 7 sites that pass a function-typed `val`
    (`megaman/components/BodyComponent.kt:156,165,178`, `CartinJoe.kt:201,209`,
    `FixtureTypeOverlapSpawn.kt:73`), the `setShouldStickToBlock(Boolean)` calls (become
    `setReceiver(Receivers.STICK_TO_BLOCK) { _, _ -> value }`), and labels such as `filter@`.
  - Review the script's diff and about 15 sites by hand.
- Check: `./gradlew core:test lwjgl3:jar` passes. The old setters have no callers (grep).
- Done: yes

### 7. Delete the old functions

- Changes: delete the receiver trios in `IFixtureExtensions.kt` (keep `getBody`, `setEntity`,
  `getEntity`, `depleteHealth`), `hasConsumer`, the exception helpers in `FixtureLabel.kt`, and
  `setHitByLaserReceiver` and its pair. Leave the `ConstKeys` string values in place: some
  (`HIT_BY_BLOCK`) are also `spawnProps` keys.
- Check: `./gradlew core:test lwjgl3:jar` passes. The contract greps find nothing.
- Done: yes

### 8. Contract checks

- Changes: none, unless a check fails.
- Check:
  - A clean `./gradlew clean engine:test core:test lwjgl3:jar`; compare the tests and the warning
    count with the baseline.
  - The greps and the reviews in `CONTRACT.md`.
  - Update `docs/issues/2026-10-22-unused-contact-hooks.md`: the `setHitByLaserReceiver` part is
    resolved by this branch (the rest stays open). This is a `master`-only file, so do it at merge
    time.
  - The owner's play test from `CONTRACT.md`.
  - Write the results in the process log.
- Done: yes

### 9. Merge

- Changes: the merge steps in `docs/mega-refactor/CLAUDE.md` ("Branch merge"), after the owner's
  explicit approval. Compare with `master` first (`git diff --stat <branch>...master`), and merge
  `master` into the branch if it touches the same files.
- Check: step 9 of the merge rules: the build and tests pass on `master`.
- Done: yes
