# Scope: mega-refactor-014c-contact-rules-table

What this branch changes and why. This is a draft written ahead of time on `master`. The branch
does not exist yet, and the user has not approved this scope.

- Status: draft
- Approved: no

## Goal

Replace the `if / else if` chain in `MegaContactListener` with an engine rule table: one rule
object per fixture-type pair, looked up by the two fixture types, with the fixtures passed in the
rule's role order. `IContactListener` takes the two fixtures instead of a `Contact`, and the type
helpers in `Contact` go away. A test proves that the table picks the same rule, in the same role
order, as the old chain for every possible pair. Gameplay does not change: the rule bodies move as
they are.

This draft proposes to split the work of item `014` that remains into two branches (see "Split"
below): this branch builds the table and moves the bodies unchanged; a new branch `014d` replaces
the `Megaman` checks inside the bodies.

## Item

`014` (table-driven contact and collision rules). From its `ITEM.md`:

> - An unordered fixture-pair table with begin, continue and end callbacks, ordered wildcard
>   pre-rules and a build-time duplicate check.
> - Replace the 16 `Megaman` references with fixture receivers or capabilities.
> - `MegaCollisionHandler` reads body flags only (no `game.megaman`, no raw input).
> - A contact trace and diff tool. Prove equality on a replay.
> - Do `014` and `020` in separate branches, because `020` changes the contact order.
> - `IContactListener` takes the two fixtures (`fixture1, fixture2, delta`), not a `Contact`. Then
>   remove the type helpers from `Contact` [...]. Other users to move:
>   `CONTACT_LISTENER_DEBUG_FILTER` (`MegamanMaverickGame.kt:168-171`) and `ContactTest`.

The decisions of 2026-10-22 that apply (`ITEM.md`, "Decisions"):

- Handlers take plain ordered fixtures, `(a, b, delta)`.
- One rule object per pair, with `onBegin`, `onContinue` and `onEnd`.
- One class and file per pair effect with real logic; generic classes for the receiver-forward
  pairs; one `ProjectileRules.kt`.
- The table mechanism goes in the engine. `FixtureType`, the rules and the receivers stay in
  `core`.
- `FixtureTypeSet` goes in this branch, with the table (indexes `0..n-1` in order; `of`,
  `validated`, `named`, `owns`).
- Contact trace: the exhaustive selection test with a reference file. No `--contactTrace` flag.
- No JSON rules. Surface rules as Kotlin data (`SurfaceRule`).
- Wall-slide ice friction becomes a body flag that the wall-slide behavior sets.

Research: `items/014-contact-rules-table/NOTES.md` (all sections).

## Current state

Scan of `master` at `0edd9649c` (after `014a` and `014b`). `L` =
`core/.../world/contacts/MegaContactListener.kt` (1582 lines).

**The chain** (facts from `NOTES.md` section 1, checked again on this commit):

- 80 branches: 35 in begin, 27 in continue, 18 in end. 77 rules plus 3 `CONSUMER` wildcards.
  Four set rules (`DEATH`, `GATE`, `BOUNCER`, `PROJECTILE`).
- No rule overlaps another. Every match uses only the two fixture types. No code runs after the
  chain, so an early `return` only ends its own branch.
- Same-type pairs depend on the contact order: `BODY`-`BODY` and `SIDE`-`SIDE` call `fixture1`
  first, and `PROJECTILE`-`PROJECTILE` is not symmetric.
- The phases are not symmetric (guards that end skips, steps that only one phase does).
- The bodies read `contact` only to match types, for `printDebugLog` (72 calls, 5 with wrong
  text), and in the `SIDE`-`SIDE` destructuring (3 places).
- Listener state: the shared `out` pair, `set1`/`set2` (set rules), `tempVec2Set` (laser rules),
  `game` (only `game.megaman.ready` in `FEET`-`SAND`), and four friction constants
  (`ICE_FRICTION` is also used by `Penguin.kt`).
- 15 `Megaman` references in the bodies plus `game.megaman.ready`.
- Since `014b`, every receiver read is `fixture.receiver(Receivers.X)?.invoke(...)`.

**The engine contract:**

- `IContactListener` has `beginContact`, `continueContact`, `endContact`, each `(Contact, delta)`.
  `WorldSystem.processContacts` (`WorldSystem.kt:208-228`) is the only caller. Only
  `MegaContactListener` and a relaxed mock in `WorldSystemTest` implement it. `WorldSystemTest`
  does not check the listener calls.
- `Contact` (`engine/.../world/contacts/Contact.kt`) has `fixture1`, `fixture2`, `set()`, order-free
  `equals`/`hashCode`, and 6 type helpers. Outside `L`, the helpers are used only by
  `CONTACT_LISTENER_DEBUG_FILTER` (`MegamanMaverickGame.kt:168-171`, `fixturesMatch`) and
  `ContactTest`.

**The filter:** `MegaContactFilter` is separate from the listener. It decides which contacts
exist. Two listener rules are dead because the filter never makes their contacts (`SHIELD`-`SIDE`,
`BLOCK`-`GRAVITY_CHANGE`), and one filter pair has no rule (`PLAYER`-`DEATH`). All three are filed
issues.

**`MegaCollisionHandler`** reads `game.megaman`, a behavior and raw input (`NOTES.md` section 3).

## Split

Item `014` still has four parts: the table, the receivers for the `Megaman` checks, the
`MegaCollisionHandler` flags, and the surface-rule and wall-slide clean-ups. Together they are too
large for one review and one play test. This draft proposes:

- **`014c` (this branch): the table, with the bodies moved unchanged.** Each body keeps its
  `Megaman` checks. The proof is mechanical: the selection test plus a verbatim move.
- **`014d` (new, planned): receivers and flags.** Replace the 16 `Megaman` references in the rule
  classes with receivers or capabilities (for example the feet-block contact receiver and
  `IGateTrigger` from the `NOTES.md` sample), the wall-slide body flag, the
  `MegaCollisionHandler` body flags, and `SurfaceRule`. These change rule bodies, so they need
  per-rule tests and a play test of their own. With the rules in small files, each change is a
  small diff.

## In scope

**Stage 0: rule IDs and the selection test against the old chain.**

- A `ContactRuleIds` object in core with one ID constant per rule (for example
  `FEET_BLOCK = "feet-block"`; the `PROJECTILE` set rule is one ID).
- In the old chain, one line after each branch's role destructuring reports the ID and the role
  order to an optional hook on the listener. The hook is `null` in the game.
- A core test (the "selection test") that, for every ordered pair of the 29 fixture types and each
  of the 3 phases, records which rule runs and in which role order, by setting the hook to record
  and then stop the branch. It writes the result as a reference file: about 200 lines of
  `phase typeA typeB -> ruleId roleOrder`, plus `-` for pairs with no rule.
- The test compares the result with the reference file, so it fails if the selection changes.

**Stage 1: engine mechanism.**

- `FixtureTypeSet` (engine `world/body/` or `world/contacts/`): `of(entries)`, `validated(list)`,
  `named(vararg names)` (gives its types a readable `toString()`), `size`, `get(index)`,
  `owns(type)`. With tests.
- `IContactRule` (engine `world/contacts/`): `onBegin(a, b, delta)`, `onContinue(a, b, delta)`,
  `onEnd(a, b, delta)`, all default no-ops.
- `ContactRulesBuilder`: `pair(id, a, b, rule, vararg phases)`, `sets(id, setA, setB, rule,
  vararg phases)`, `wildcard(id, a, rule, vararg phases)`, `build()`. Build-time checks: a pair
  registered by two rules, a set rule with a type on both sides, a wildcard that clashes with an
  explicit pair. A same-type pair fills one slot and keeps the contact order.
- `ContactRuleTable`: dense slots indexed by `typeA.index * n + typeB.index`, rules stored per
  phase, a `flip` flag per slot, rule IDs, `select(phase, f1, f2)` (for tests) and
  `dispatch(phase, f1, f2, delta)`. No allocation on the dispatch path.
- `ContactRuleDispatcher : IContactListener`: calls `table.dispatch` for each phase.
- Engine tests for the builder checks, the role order in both contact orders, same-type pairs, the
  wildcard expansion and `owns`.

**Stage 2: core rules and the swap.**

- `IContactListener` takes `(fixture1, fixture2, delta)`. `WorldSystem.processContacts` passes
  `contact.fixture1, contact.fixture2`. `Contact` keeps `fixture1`, `fixture2`, `set()` and its
  order-free equality; its 6 type helpers are deleted, with their tests in `ContactTest`.
- One rule class per pair in `core/.../world/contacts/rules/`, following the decided file split.
  Each body moves as it is from the chain, with these mechanical changes only:
  - the role destructuring goes away (the rule gets `a` and `b` in role order);
  - `contact` is no longer available: the `SIDE`-`SIDE` destructuring uses `a` and `b`;
  - `printDebugLog` lines go away (see the debug log below);
  - `out`, `set1` and `set2` go away; `tempVec2Set` moves into the laser rules;
  - the `FEET`-`SAND` rule gets `game` through its constructor.
- `MegaContactRules.kt`: one registration list of all rules, built with
  `FixtureTypeSet.of(FixtureType.entries)`.
- `MegaContactListener` is replaced by the engine dispatcher with the core table.
  `MegamanMaverickGame.createGameEngine()` wires it.
- The friction constants move to a core place that `Penguin.kt` can still use.
- The debug log: the dispatcher logs `phase ruleId fixture1 fixture2` when a debug filter accepts
  the two fixtures. `CONTACT_LISTENER_DEBUG_FILTER` becomes `(IFixture, IFixture) -> Boolean`.
  The three extra value logs in `DAMAGER`-`DAMAGEABLE` (`canBeDamaged`, `canDamage`,
  `takeDamageFrom`) stay in that rule, behind the same filter. This removes the five wrong log
  texts (`docs/issues/2026-10-22-wrong-contact-log-texts.md`).
- The selection test runs against `ContactRuleTable.select` and must give the same reference file
  as stage 0.

**Tests for the core table** (no game needed): it builds; every `FixtureType` is owned; the pair
set of each phase equals the old chain's (from the reference file).

## Out of scope

- Changing any rule body beyond the mechanical changes above. In particular the `Megaman`
  checks, the wall-slide check and `game.megaman.ready` stay. They are `014d`.
- `MegaCollisionHandler` (`014d`).
- `SurfaceRule` and other DRY merges of rule bodies (`014d`), and splitting the `PROJECTILE` rule's
  inner `when` into 7 entries.
- `MegaContactFilter`, including the dead rules and the unhandled `PLAYER`-`DEATH` contact. The
  table keeps the dead rules, so they stay dead. A filter derived from the table is not part of
  `014`.
- The `--contactTrace` flag, the line-by-line trace, replay (`055`).
- The broad phase and the contact order (`020`).

## Affected areas and risks

- **Files:** engine `world/contacts/` (new table files, `IContactListener`, `Contact`),
  `WorldSystem.kt`, engine tests; core: `MegaContactListener.kt` (deleted at the end), about 25 new
  rule files, `MegaContactRules.kt`, `ContactRuleIds.kt`, `MegamanMaverickGame.kt`, `Penguin.kt`,
  the selection test and its reference file.
- **Risk: a body changes while it moves.** Stage 2 moves code. Each rule class is checked against
  its old branch with `git diff --color-moved` and by reading; only the listed mechanical changes
  are allowed.
- **Risk: role order.** The selection test checks every pair in both contact orders and every
  phase. Same-type pairs keep the contact order.
- **Risk: an early `return` in a moved body.** It now returns from the rule method. That is the
  same, because no code ran after the chain.
- **Risk: phase coverage.** A rule that registers a phase it did not handle before would run an
  empty default method, which changes nothing; a rule that misses a phase is caught by the
  selection test.
- **Risk: hot path.** The dispatch must not allocate. Rules are objects or instances built once.
- **No gameplay change by design.** The dead rules stay dead, and the debug log changes text only.

## Overlap with other branches

- No `mega-refactor-` branch is active now.
- `020` (broad phase) changes the contact order and must not run at the same time (`ITEM.md`).
  The selection test and its reference file help `020` later.
- `014d` (proposed) needs this branch.

## Open questions

1. **The split:** do you approve `014c` (the table, bodies unchanged) and a new `014d` (receivers,
   `MegaCollisionHandler` flags, `SurfaceRule`, wall-slide flag)?
2. **The reference file:** commit it (recommended: after stage 2 deletes the old chain, nothing can
   make it again, and CI needs it), or git-ignore it in `temp/`?
3. **`owns` check:** at table build time only (free), or also in `dispatch` (two array reads and two
   reference compares per contact, and a clear error for a stray fixture type)? The draft checks
   in `dispatch`.
4. **Where `FixtureTypeSet` lives:** engine `world/body/` (next to `IFixtureType`, recommended) or
   `world/contacts/` (next to the table)?
