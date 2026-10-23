# 014: Table-driven contact and collision rules

- Status: active
- Type: refactor
- Depends on: none
- Related: `006`, `020`, `029`, `030`, `031`, `055`, `056`
- Branches: `014a` (merged), `014b` (merged), `014c`, `014d` (planned, see "Decisions")

## Goal

Replace the 1609-line `if/else` chain in `MegaContactListener` with a rule table that is easy to read and configure.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- An unordered fixture-pair table with begin, continue and end callbacks, ordered wildcard pre-rules and a build-time duplicate check.
- Replace the 16 `Megaman` references with fixture receivers or capabilities.
- `MegaCollisionHandler` reads body flags only (no `game.megaman`, no raw input).
- A contact trace and diff tool. Prove equality on a replay.
- Do `014` and `020` in separate branches, because `020` changes the contact order.
- `IContactListener` takes the two fixtures (`fixture1, fixture2, delta`), not a `Contact`. Then remove the type helpers from `Contact` (`fixturesMatch`, `fixtureSetsMatch`, `oneFixtureMatches`, `getFixturesIfOneMatches`, `getFixturesInOrder`, `getFixtureSetsInOrder`), because they exist only for the old listener. Other users to move: `CONTACT_LISTENER_DEBUG_FILTER` (`MegamanMaverickGame.kt:168-171`) and `ContactTest`.

## Decisions

2026-10-22, from the owner, after the research in `NOTES.md`:

- **Handler signature:** rule handlers take plain ordered fixtures, `(a, b, delta)` (option H2 in `NOTES.md`). The table puts the fixtures in the rule's role order.
- **`Contact` helpers:** a task for this item. `IContactListener` moves to plain fixture parameters, and the type helpers in `Contact` are removed (see "Proposed scope").
- **Receivers:** replace the `Megaman` checks with fixture receivers or capabilities in this item. Do not wait for `029`. `029` adjusts them later as needed.
- **Receiver style:** the owner sees the hand-written receiver extension trios (`setX`/`hasX`/`getX`) as a code smell. Typed fixture receivers replace them (research and scope in `items/056-typed-fixture-receivers/`).
- **Branch order:** the typed fixture receivers (former item `056`) come before the rule table work specified in `NOTES.md`. A fixture-type narrowing branch also comes before the table (research in `NOTES.md`, "Research: narrow the engine fixture type"). Planned branches:
  - `mega-refactor-014a-fixture-type`: narrow the engine fixture type from `Any`.
  - `mega-refactor-014b-typed-fixture-receivers`: the scope of the former item `056`. It does not depend on `014a`, so the two can run in parallel.
  - `mega-refactor-014c-contact-rules-table`: the rule table, with the rule bodies moved unchanged. It needs `014a` and `014b`.
  - `mega-refactor-014d-contact-receivers`: receivers and capabilities in place of the `Megaman` checks, the `MegaCollisionHandler` body flags, `SurfaceRule` and the wall-slide flag. It needs `014c`. The owner approved this split on 2026-10-22.
- **Fixture type:** the engine types fixtures as an `IFixtureType` interface with one property, `index: Int` (a dense slot index, as in Ashley's `ComponentType.getIndex()`), not `Any` and not a primitive ID. An enum implements it with one line, `override val index get() = ordinal`. There is no `name` property: nothing reads it, and logs use `toString()` (owner's decision, 2026-10-22, which replaced the earlier `ordinal` and `name`). An engine `FixtureTypeSet` owns the set of types: it validates that the indexes are `0..n-1` in order (`of`, `validated`), creates types for games without enums (`named`, which also gives them a readable `toString()`), and checks that a type belongs to the set (`owns`). Fixture types are single instances, with no value equality. `014a` adds `IFixtureType`. `FixtureTypeSet` goes in `014c` with the table, because only the table uses the index (owner's question, 2026-10-22: `WorldSystem` never reads the type, so a check there gives nothing) (`NOTES.md`, "Research: narrow the engine fixture type" and its follow-up).
- **No JSON rules:** simple surface rules are Kotlin data (`SurfaceRule`), not JSON (`NOTES.md` 4.6).
- **Wall-slide ice friction:** a body flag that the wall-slide behavior sets replaces the `Megaman` check (`NOTES.md` 4.6).
- **Contact trace:** `014c` builds the exhaustive selection test with a committed reference file (`NOTES.md`, "Research: contact trace"). No `--contactTrace` flag for now. The line-by-line diff waits for `055`.
- **Engine:** the table mechanism (`ContactRuleTable`, builder, dispatcher) goes in the engine. `FixtureType`, the rules and the receivers stay in `core`.
- **Phases:** one rule object per pair, with `onBegin`, `onContinue` and `onEnd` methods (option P1 in `NOTES.md` 4.4).
- **Files:** one class and file per pair effect with real logic; generic classes for the receiver-forward pairs; one `ProjectileRules.kt` (`NOTES.md`, "Per-pair classes"). The branch confirms it in practice.
- **Contract check:** a manual check of fixed routes is enough for now. A contact trace tool is under research.
- **Side findings:** filed as issues on 2026-10-22: `2026-10-22-shield-side-contact-never-made.md`, `2026-10-22-block-gravity-change-rule-never-runs.md`, `2026-10-22-player-death-contact-unhandled.md`, `2026-10-22-unused-contact-hooks.md`, `2026-10-22-wrong-contact-log-texts.md`, `2026-10-22-projectile-explosion-shape-order.md`.

## Owner's input

Answer to review item 13 (2026-10-22, verbatim):

> Yes, table-driven contact & collision logic would go a long way in cleaning up the code and also make configuration much easier.

## Open questions

None open.

## Sources

- `NOTES.md` in this folder.
