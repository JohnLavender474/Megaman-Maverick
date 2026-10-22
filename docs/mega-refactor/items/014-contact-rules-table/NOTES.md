# Notes: 014 Table-driven contact and collision rules

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Players, input and entity design (2026-10-22)

Copied as written from the review section "ITEM 13: TABLE-DRIVEN CONTACT DISPATCH".

````text
1. Current state
- `C/world/contacts/MegaContactListener.kt` is 1609 lines: begin `:74-834`, continue `:835-1329`, end `:1330-1609`. Each is an `if / else if` chain of `fixturesMatch(A, B)`.
- There is one wildcard: `oneFixtureMatches(CONSUMER)`, checked first (`:76, :837, :1332`).
- There are some set rules. `fixtureSetsMatch`: DEATH x {FEET,SIDE,HEAD,BODY} (`:110`), {SIDE,FEET,HEAD} x GATE (`:264`), BOUNCER x {BODY,FEET,HEAD,SIDE} (`:409`), PROJECTILE x {BLOCK,BODY,SHIELD,WATER,SAND,PROJECTILE,EXPLOSION} (`:619`).
- Key finding: a contact is one pair of fixture types, and in `beginContact` no exact pair is listed twice and no exact pair overlaps a set rule (checked with a script). So first-match order only matters for the CONSUMER wildcard. The table can be a map lookup.
- Player special cases use `is Megaman` (`:277, 292, 326, 488, 735, 746, 928, 1034, 1104, 1285, 1399, 1549, 1565`). Examples: land sound plus `aButtonTask` (`:326-343`), gate trigger (`:277`), wall-slide ice friction (`:292`).
- Contacts come from an `OrderedSet` (`E/world/WorldSystem.kt:91-92, 210-212`), so the order is deterministic.
- `C/world/collisions/MegaCollisionHandler.kt:41-53, 108` reads `game.megaman`, `isBehaviorActive(CLIMBING)` and raw input (`isJustPressed(UP)`). This runs inside physics substeps.

2. Options
A. Unordered-pair map plus ordered wildcard pre-rules. Set rules are expanded into pair entries at build time. Pro: O(1) lookup, and duplicates are found when the table is built. Con: wildcard rules still need an order.
B. An ordered rule list (same first-match as today) with a predicate per rule. Pro: same semantics by construction. Con: linear scan, and overlaps stay hidden.
C. Double dispatch on fixture types: each fixture type owns its handlers. Con: logic splits across many files, and pairs are hard to see.

3. Recommendation
Option A:
- `ContactRule(a, b, begin?, continue?, end?, priority = 0)`, keyed by `(min(a,b), max(a,b))`.
- Callbacks get the fixtures in the rule's (a, b) order, which replaces `getFixturesInOrder`.
- Wildcard rules (CONSUMER) go in an ordered list and are checked first. A rule's `priority` field is used only where two rules share a key. `build()` fails on an unresolved duplicate.
- Remove `is Megaman` with fixture receivers. The pattern exists already: `getConsumer`, `hitByBlockReceiver`, `getHitByDamageable`. For example, the FEET-BLOCK rule calls `feet.onLanded(block)`, and Megaman registers his land sound and his `aButtonTask` reset. GATE calls a `IGateTrigger` capability on the entity.
- Configurable: entities register receivers or extra rules by fixture label at `init`. Simple physical rules can be data rows, for example `(FEET, ICE) -> sense FEET_ON_ICE, frictionX 0.025` or `(FEET, SAND) -> 1.15`.
- `MegaCollisionHandler`: the climb ability sets a body flag such as `PASS_THROUGH_UP` from the player's `InputFrame` (item 30). The handler then reads body state only, with no `megaman` and no input.

Equality proof:
(a) A unit test builds the table from the 77 rules and checks: no overlaps, and the set of keys is the same as the old chain.
(b) A trace with one line per processed contact: tick, phase, the two fixture types, entity ids, rule id, plus an entity-state hash per tick. Run one replay on the old and the new code and diff the traces. This needs replay (items 5/9/10/30). Until then, use a scripted headless run with a fixed seed.

4. Open questions for the owner
- Do you want rules in data (JSON) now, or only the simple friction and sense rules?
- Can some special cases (wall-slide ice friction `:292`) become a general body property instead of a player check?

5. Backlog wording
"Table-driven contact and collision rules"
- Unordered fixture-pair table with begin/continue/end, wildcard pre-rules and a build-time duplicate check.
- Replace the 16 Megaman references with fixture receivers or capabilities.
- `MegaCollisionHandler` reads body flags only (no `game.megaman`, no raw input).
- Contact trace and diff tool. Prove equality on a replay.
- Fix the "48 megaman refs" number to 16.

6. Dependencies
Item 28 (`IPlayer` capabilities), 29 (faction for projectile rules), 30 (input out of the collision handler), 5/9/10 (replay for the proof), 19 (broad phase changes contact order, so do 13 and 19 in separate branches).
````
