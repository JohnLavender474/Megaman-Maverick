# 014: Table-driven contact and collision rules

- Status: planned
- Type: refactor
- Depends on: none
- Related: `006`, `020`, `029`, `030`, `031`, `055`
- Branches: none yet

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
- **Side findings:** filed as issues on 2026-10-22: `2026-10-22-shield-side-contact-never-made.md`, `2026-10-22-block-gravity-change-rule-never-runs.md`, `2026-10-22-player-death-contact-unhandled.md`, `2026-10-22-unused-contact-hooks.md`, `2026-10-22-wrong-contact-log-texts.md`, `2026-10-22-projectile-explosion-shape-order.md`.

## Owner's input

Answer to review item 13 (2026-10-22, verbatim):

> Yes, table-driven contact & collision logic would go a long way in cleaning up the code and also make configuration much easier.

## Open questions

- Rules in data (JSON) now, or only the simple friction and sense rules? The research recommends Kotlin `SurfaceRule` rows and no JSON for now (`NOTES.md` 4.6).
- Can some special cases (wall-slide ice friction) become a general body property? The research says yes: a body flag that the wall-slide behavior sets (`NOTES.md` 4.6).
- One rule object per pair with three phase methods, or one table per phase (`NOTES.md` 4.4)?
- Should the table mechanism start in `core`, or go into the engine at once?
- One class and file per pair effect, or group files (`NOTES.md` section "Per-pair classes")?
- Is a manual trace diff of fixed routes enough for the contract, without `006`/`055`?

## Sources

- `NOTES.md` in this folder.
