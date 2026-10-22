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

## Research: rule table design (2026-10-22)

The owner asked for this research. Three read-only research agents examined the code: one for the current flow, one for the blast radius, and one for the design options. This section is a summary of their results and my analysis. It is not a scope decision.

- The base commit is `b15bad39f`.
- `L:` means `core/src/main/kotlin/com/megaman/maverick/game/world/contacts/MegaContactListener.kt`.
- The review numbers above are old. The current item IDs are used here: `020` broad phase, `029` `IPlayer`, `030` faction, `031` `InputFrame`, `006` headless run, `055` replay.

### 1. Current flow (facts)

**Structure**

- There are 80 branches: 35 in begin (`L:74-833`), 27 in continue (`L:835-1328`) and 18 in end (`L:1330-1570`). That is 77 rules plus 3 `CONSUMER` wildcards.
  - After the set rules expand, begin has 48 exact pairs, continue has 29 and end has 17. Together they make 48 distinct pairs.
- Match kinds:
  - **Exact pair:** `fixturesMatch(A, B)`.
  - **Wildcard:** `oneFixtureMatches(CONSUMER)`. It is always the first branch.
  - **Set × set:** `fixtureSetsMatch`. There are four:
    - `DEATH`×{`FEET`,`SIDE`,`HEAD`,`BODY`} (begin and continue)
    - {`SIDE`,`FEET`,`HEAD`}×`GATE`
    - `BOUNCER`×{`BODY`,`FEET`,`HEAD`,`SIDE`}
    - `PROJECTILE`×{`BLOCK`,`BODY`,`SHIELD`,`WATER`,`SAND`,`PROJECTILE`,`EXPLOSION`}
- **No rule overlaps another rule.** This was checked with a script for all three phases. No exact pair occurs twice, no exact pair overlaps a set rule, and no rule names `CONSUMER`. So the order of the branches has no effect today, and a map lookup gives the same result for every pair.
- **Every match uses only the fixture types.** No branch reads a property or an entity type before it matches. All such checks are guards inside the branch bodies.
  - One exception in form: the `PROJECTILE` set rule matches once, then dispatches again on `otherFixture.getType()` (`L:659`). It is really 7 sub-rules with a shared prefix:
    1. the `NO_PROJECTILE_COLLISION` guard
    2. the `hitByProjectile` receiver
    3. `shape.copy()`
- **Early `return` statements** (17 in begin, 17 in continue, 4 in end) exit the whole callback. But no code follows the chain in any callback, so each `return` only ends its own branch. In a handler, it becomes a plain `return`.

**What the `Contact` object holds**

- `Contact` (`engine/.../world/contacts/Contact.kt`) is a pooled `data class` with only `fixture1` and `fixture2`. It has no normal, no overlap and no manifold.
- The rules read `contact` for three things only:
  - to match the types;
  - for `printDebugLog`;
  - to destructure `val (a, b) = contact` in `SIDE`×`SIDE` (`L:227, 972, 1353`).
- So, today, no rule needs the contact for its data. A handler that gets the two ordered fixtures gets everything that a rule uses.

**Other inputs the branches use**

- `delta`: 23 lines. It is the fixed sub-step delta, not the frame delta. It is only passed on to receivers and to velocity changes.
- Listener state:
  - the shared `out` pair (all branches);
  - `set1`/`set2` (set rules);
  - `tempVec2Set` (laser rules);
  - the friction constants.
- `game`: only `game.megaman.ready` at `L:378`, which gates the sand splash.
- Concrete classes:
  - `Megaman`: 15 sites (`L:277, 292, 326, 488, 615, 735, 746, 928, 1034, 1104, 1285, 1399, 1549, 1565`, plus `L:378`). That makes 16 player references, as the review said.
  - `Block`: 13 sites.
  - `Gate`, `Cart`, `ItemEntity`, `HeartTank`, `Splash`.
  - The deprecated `EntityFactories` at `L:379` and `L:691`.

**How `WorldSystem` calls the listener** (`engine/.../world/WorldSystem.kt`)

- `cycle()` runs once for each fixed sub-step, so it can run several times per frame. The order in a sub-step:
  1. preProcess
  2. `Body.process`, which applies friction and then resets `frictionOnSelf`
  3. rebuild the container
  4. `collectContacts`
  5. `processContacts`
  6. `resolveCollisions` (`MegaCollisionHandler`)
  7. postProcess
- **Orientation.** `collectContacts` (`:230-253`) calls `contact.set(queryingFixture, candidate)`. So `fixture1` is always the fixture that ran the query.
  - Only filter keys and `CONSUMER` run queries. So when only one side is a key, the order is fixed by type. For example, `BLOCK` never queries, so a block is always `fixture2`.
  - When both sides query (`BODY`×`BODY`, `SIDE`×`SIDE`, `PROJECTILE`×`PROJECTILE`, `BODY`×`FEET`, and others), the body that comes first in the iteration order wins. The order is deterministic, but it has no meaning in the game. It can differ between the `SimpleGrid` and `LooseQuadtree` containers.
- `processContacts` (`:208-228`) runs in two passes:
  1. All begin and continue calls, in `OrderedSet` order.
  2. All end calls.
  - The orientation is found again in each sub-step, so continue can get the opposite orientation to begin.

**Same-type pairs and orientation**

| Pair | Handling | Symmetric? |
|---|---|---|
| `BODY`×`BODY` (`L:158`) | Calls the receiver of `fixture1`, then of `fixture2` | Yes, but the call order is `fixture1` first |
| `SIDE`×`SIDE` (`L:224, 969, 1350`) | Destructures the contact, then calls both receivers | Yes, `fixture1` first |
| `PROJECTILE`×`PROJECTILE` (inside `L:619-724`) | Only `fixture2` gets the `NO_PROJECTILE_COLLISION` check and `hitByProjectile`. Then `p1.hitProjectile(p2)`, then `p2.hitProjectile(p1)` | **No.** The result depends on the query order |
| `CONSUMER`×`CONSUMER` | `getFixturesIfOneMatches` uses `fixture1` as the consumer | No: only one consumer runs |

**Phase coverage**

| Phases | Pairs |
|---|---|
| Begin + continue + end (17) | `CONSUMER`, `BLOCK`-`BODY`, `BODY`-`FEET`, `BODY`-`SIDE`, `SIDE`-`SIDE`, `SIDE`-`BLOCK`, `SIDE`-`ICE`, `FEET`-`BLOCK`, `FEET`-`ICE`, `FEET`-`SAND`, `FEET`-`SNOW`, `HEAD`-`BLOCK`, `HEAD`-`FEET`, `WATER_LISTENER`-`WATER`, `HEAD`-`LADDER`, `FEET`-`LADDER`, `BODY`-`FORCE` |
| Begin + continue (10) | `DAMAGER`-`DAMAGEABLE`, `DEATH` set, `BODY`-`EXPLOSION`, `BLOCK`-`EXPLOSION`, `BODY`-`GRAVITY_CHANGE`, `BLOCK`-`GRAVITY_CHANGE`, `TELEPORTER_LISTENER`-`TELEPORTER`, `LASER`-`BLOCK`, `LASER`-`BODY`, `LASER`-`SHIELD` |
| Begin + end | `FEET`-`CART` |
| Begin only | `BODY`-`BODY`, `SHIELD`-`SIDE`, `GATE` set, `BOUNCER` set, `BODY`-`PLAYER`, `PROJECTILE` set, `PLAYER`-`ITEM` |

**The phases are not symmetric.** A generic rule that runs the same body in every phase would change the behavior:

- End skips guards that begin and continue use:
  - `BLOCK`-`BODY` end has no `NO_BODY_TOUCHIE` guard (`L:1467`).
  - `SIDE`-`BLOCK` end has no `NO_SIDE_TOUCHIE` guard (`L:1338`).
  - `HEAD`-`BLOCK` end has no `COLLIDE_DOWN_ONLY` guard (`L:1443`).
  - So end can fire for a contact whose begin was skipped.
- `SIDE`-`BLOCK`:
  - begin calls `hitBySide` and the receiver;
  - only continue does the stick-to-block translate.
- `BODY`-`GRAVITY_CHANGE`: begin and continue check in a different order, and only begin changes the gravity scalar.
- `FEET`-`ICE`: continue has a `Megaman` wall-push early return; begin does not.
- `hitByBody` fires:
  - in begin only for `BODY`-`BODY`;
  - in all three phases for `SIDE`;
  - in begin and continue for `LASER`.

**Repeated code shapes (DRY candidates)**

| Shape | About how many branches |
|---|---|
| Forward to a fixture receiver (`if has…Receiver → get…(state)`) | 22 |
| Block hit (`block.hitByX` + `getHitByBlock` + sense) | 10 |
| `NO_*_TOUCHIE` / `COLLIDE_DOWN_ONLY` guard | 12 |
| Friction surface (set sense, check an opt-out property, set `frictionOnSelf`) | 8 |
| Clear a sense on end | 7 |
| Left/right sense from the `SIDE` fixture property | 6 |
| Ladder (sense + `LADDER` body property, a point-in-shape check in continue) | 6 |
| Laser intersection collect | 4 |
| Velocity alteration (`BOUNCER`, `FORCE`) | 4 |
| The same body in begin and continue (differs only in `ProcessState`, or not at all) | about 14 |

### 2. Side findings (bug candidates, not yet filed)

This research writes only to this file. Each item below can go into `docs/issues/` when the owner agrees. Under the bug rule in `docs/mega-refactor/CLAUDE.md`, a `014` branch must keep these behaviors, or name them in its `CONTRACT.md` under "Intended changes".

1. **Dead rule `SHIELD`-`SIDE`** (`L:236-241`).
   - It was added in `926c46c55` (2025-11-14) for the shield receiver at `MagmaWave.kt:176,184`.
   - But `MegaContactFilter` never lets this contact be made: `SHIELD` is not a key, and `SIDE`'s set has no `SHIELD`. So the `MagmaWave` receiver never fires. This is probably a latent bug.
2. **Dead rule `BLOCK`-`GRAVITY_CHANGE`** (begin `L:591`, continue `L:1191`). Neither type is a filter key.
3. **`PLAYER`-`DEATH` has no rule.** The filter makes this contact (`MegaContactFilter.kt:22-26`), but no rule handles it in any phase. It is harmless: `DEATH` is handled through `FEET`/`SIDE`/`HEAD`/`BODY`.
4. **`FixtureType.CUSTOM`** (used only by `DragonFly`) has no rule and no filter entry. It only gets contacts as the other side of a `CONSUMER`.
5. **Written but never read:** the `TOUCHING_CART` sense and the body property `ConstKeys.CART`.
6. **A receiver with no users:** `setHitByLaserReceiver` has 0 users.
7. **Wrong log texts:**
   - `L:117` says "Block", but the branch is for `BODY`.
   - `L:890` says "endContact" in continue.
   - `L:1212`, `L:1240` and `L:1255` say "beginContact" in continue.
8. **Shape order quirk:** `L:721` calls `hitExplosion(other, otherShape, thisShape)`. This order is the reverse of the other `PROJECTILE` sub-rules.
9. **Eager log strings:** `printDebugLog` builds its message on every call, even when the filter is false (see item `044`).

### 3. Blast radius

**Wiring**

- The listener, the filter and the collision handler are built in one place only: `MegamanMaverickGame.kt:863-865`.
- `Penguin.kt:36,85-86` uses `MegaContactListener.ICE_FRICTION`. If the constant moves, update this file too.
- `IContactListener` and `IContactFilter` are used only by `WorldSystem` and `WorldSystemTest`.

**Who depends on the listener's side effects**

| Effect | Files / refs outside the listener |
|---|---|
| `FEET_ON_GROUND` | 82 / 262 |
| `SIDE_TOUCHING_BLOCK_LEFT`/`RIGHT` | 34 / 53 each |
| `HEAD_TOUCHING_BLOCK` | 26 / 32 |
| `IN_WATER` | 12 / 29 |
| `FEET_ON_SAND` | 5 / 18 |
| `BODY_TOUCHING_BLOCK`, `FEET_ON_ICE`, `FORCE_APPLIED`, ladder and ice-side senses | 1-5 each |
| Feet-block set (`addFeetBlock`) | `GravityBlock`, `MegaCollisionHandler:63-96` |
| `frictionOnSelf` | Applied in the next `Body.process`. Ice, sand and snow overwrite it, so the last contact wins |
| `Megaman` state | `aButtonTask`, `canMakeLandSound`, `gravityScalar`, velocity, land sound |

The senses are the real contract. About 100 entity files read what the listener writes. A table refactor must write the same senses in the same order of contacts. The entity files themselves do not change.

**Receivers that entities already register** (`world/body/IFixtureExtensions.kt`)

These are the extension points that already exist:

| Receiver | Calls / files |
|---|---|
| `setConsumer` | 38 / 25 |
| `setFilter` | 41 / 27 |
| `setHitByBlockReceiver(state)` | 27 / 15 |
| `setHitByProjectileReceiver` | 17 / 12 |
| `setVelocityAlteration` | 10 / 9 |
| `setHitByFeetReceiver(state)` | 9 / 7 |
| `setHitByBodyReceiver` | 8 / 6 |
| `setShouldStickToBlock` | 7 / 5 |
| `setHitByPlayerReceiver(Megaman)` | 4 / 3 |
| `setHitByExplosionReceiver` | 4 / 3 |
| `setHitByWaterReceiver` | 3 / 3 |
| `setHitBySideReceiver` | 2 / 1 |
| `setHitByDamageableReceiver` | 2 / 1 |

Two signatures are tied to `Megaman`: `setHitByPlayerReceiver(Megaman)` and `ItemEntity.contactWithPlayer(Megaman)`. Item `029` re-types them.

**Tests**

- `ContactTest` covers the `Contact` helpers.
- `WorldSystemTest` uses a relaxed mock listener.
- **No test covers the begin/continue/end dispatch, the contact orientation, `MegaContactListener`, `MegaContactFilter` or `MegaCollisionHandler`.**

**`MegaCollisionHandler`**

- It runs after `processContacts` in each sub-step.
- It reads three things from outside the bodies:
  - `game.megaman` (`:41`);
  - `isBehaviorActive(CLIMBING)` (`:45, 53, 108`);
  - `game.controllerPoller.isJustPressed(UP)` (`:46`). This is a per-frame value, so it is true in every sub-step of that frame.
- It also reads `hasFeetBlock` and `FEET_ON_GROUND`, which the listener writes in the same sub-step.
- To remove these reads:
  - a body flag (for example `PASS_THROUGH_UP`) must be set before `WorldSystem` runs, and must stay set for all sub-steps of the frame;
  - the system order (Controller, Behaviors, World) allows the climb behavior to set it;
  - a clean input source needs `031`.

**Churn**

- The file has 152 commits since 2023-10 (`git log --follow -M20%`).
- In the last 12 months it has 10 commits, and only 2 of them added a branch:
  - `926c46c55`: the dead `SHIELD`-`SIDE` rule;
  - `d5f2cbbb6`: the `TELEPORTER` continue rule.
- Most of the other changes tweak the `Megaman` special cases inside existing rules, for example in `FEET`-`BLOCK` end and in water entry.
- **Conclusion:** new pairs are rare. The work goes into the player special cases inside existing rules. So moving those into receivers gives more value than a configurable table.

**Related items**

| Item | Interaction |
|---|---|
| `020` broad phase | It changes how contacts are collected, and it can change the orientation of same-type pairs. Do it on a separate branch, and diff the traces. If `014` builds the trace tool first, `020` can use it. |
| `029` `IPlayer` | The target type of the 16 player references. Either `014` adds narrow receivers now and `029` re-types them, or `014` waits for `029`. Both items touch `Megaman` and the receiver signatures. |
| `030` faction | The `PROJECTILE` and damage rules are shared hot spots. |
| `031` `InputFrame` | Needed to remove the input read from `MegaCollisionHandler`. Without it, `014` can only move the read into the climb behavior as a body flag. |
| `006`, `010`, `011` | A headless run, a fixed tick and a seedable random. They are needed for a trace diff that can be repeated. |
| `055` replay | The review's replay proof needs it. It is far down the dependency chain, so `014` cannot wait for it. |
| `017` world step hooks | The listener's senses feed gravity in the same `cycle`. |

### 4. Design: a keyed rule table

#### 4.1 The key

`FixtureType` is a plain enum with 29 values. The engine knows only `IFixture.getType(): Any`, but `MegaContactFilter` already casts every type with `as FixtureType`.

| Encoding | Lookup | Pros | Cons |
|---|---|---|---|
| **Dense `n×n` array of slots, each with a rule and a `flip` flag** | One multiply-add and one array read. No hashing, no boxing | Fastest. The `flip` flag is the "smart" swap, decided once at build time. A test can print it as a matrix (like Unity's layer collision matrix) | 29 × 29 = 841 slots, which is small. Needs enum ordinals |
| Packed `Int` key `(lo shl 16) or hi` in an `IntMap` | One hash lookup | Sparse | Still needs a swap check at each call. Gives nothing more than the array |
| `ObjectMap<GamePair, Rule>` | A hash lookup plus a key object | Familiar | A key allocation or pool per lookup. An unordered `equals` is easy to get wrong |
| `when (lo * n + hi)` | A compiler table switch | No data structure | Fixed at compile time. Set and wildcard expansion and the swap stay manual |

**Recommendation:** the dense array.

The build step fills the slots as follows:
- For a rule `(a, b)`, it writes `[a][b] = rule, flip = false` and `[b][a] = rule, flip = true`.
- For `a == b`, it writes one slot with `flip = false`, so the handler gets the contact order (see 4.4).

#### 4.2 The match kinds in a table

| Today | In the table |
|---|---|
| Exact pair | One entry. The build writes both orientations |
| Set × set | Expanded at build time into one entry per pair, with the same handler and the same role order. This removes `typesSet1`/`typesSet2` and the shared `set1`/`set2` |
| `CONSUMER` wildcard | Expanded at build time into `(CONSUMER, X)` for every `X`, with the consumer as param 1. `(CONSUMER, CONSUMER)` uses `flip = false`, so `fixture1` is the consumer, as today. At run time, wildcard order no longer exists |
| `PROJECTILE`'s inner `when` | Seven entries that share a prefix function, called first in each. Or one entry that keeps the inner `when`. The second form is a smaller change, the first is cleaner |

**Build-time checks** (`build()` throws, and names both rule IDs):
- **Duplicate pair:** the same unordered pair is registered twice in the same phase.
- **Ambiguous set:** a set rule such as `A × {A, B}`, where the `(A, A)` orientation is not defined.
- **Wildcard clash:** an exact pair hits a slot that a wildcard already fills, and no explicit priority or override resolves it.
  - Today no clash occurs.
  - The review's `priority` field is therefore needed only for this case. YAGNI says: make a clash fail the build, and add `priority` only when a real clash occurs.

#### 4.3 The handler signature

The owner's question: should the handler get the contact, the two fixtures, or both? And how is it made "smart" about role order?

| # | Signature | Smart order? | Pros | Cons |
|---|---|---|---|---|
| H1 | `(contact, delta)` | No. The handler swaps again with `getFixturesInOrder` | The same shape as today. The smallest change | The table knows the order but throws it away. Each handler repeats the swap and needs a scratch `out` pair. Each unit test must cover both orientations |
| H2 | `(a, b, delta)`, table swaps | Yes, by registration | Clean. Gives everything that rules use today (section 1). No allocation. A unit test needs only two mock fixtures | No contact. If `Contact` later gets a normal or an overlap, the signature must change. Logging and trace move to the dispatcher, which knows the rule ID, so this is good |
| H3 | `(a, b, contact, delta)` | Yes | Both are available | The contact is noise today, because no rule needs it. The contact's own `fixture1`/`fixture2` can disagree with `a`/`b`, which invites mistakes |
| H4 | One reusable `ContactContext` (`a`, `b`, `entityA`, `entityB`, `contact`, `delta`, `phase`, `ruleId`), filled by the dispatcher | Yes | No allocation. New fields (a normal, an overlap, a trace hook, the phase) can be added without touching any handler. Phase-shared handlers can branch on `phase` | A mutable object passed around. A handler that keeps a reference after the call is a bug. Harder to read than plain parameters |
| H5 | Typed roles: `rule<IDamager, IDamageable>(DAMAGER, DAMAGEABLE) { damager, damageable -> }`, with the entity casts done by the table | Yes, and the entities are typed | Handlers read like game rules. Good for rules that need only entities (`DAMAGER`-`DAMAGEABLE`, `PLAYER`-`ITEM`, `TELEPORTER`) | Most rules need the fixtures too (properties, labels, shapes, senses). The generics are erased, so this needs `reified` registration and checked casts. It adds a second API |

**How "smart" order works with each input:**

- **Fixtures (H2, H4, H5):**
  - The rule's registration order defines the roles: `pair(DAMAGER, DAMAGEABLE, …)` means param 1 is the damager.
  - The `flip` flag at the slot swaps the two fixtures before the call.
  - The handler never checks types. This is the Phaser `physics.add.overlap(groupA, groupB, cb)` model: the callback arguments follow the registration order.
- **Contact (H1):**
  - Smart order is possible only by a wrapper: a pooled "ordered contact" view that exposes `first`/`second`.
  - This is H4 under a different name. A raw `Contact` cannot be smart, because its order comes from the broad phase.

**Same-type pairs `(A, A)`:**

- The handler runs once, with `(fixture1, fixture2)` in contact order. This keeps all three current cases the same.
- A helper `symmetric(…)` could call a one-direction handler twice, with `(f1, f2)` and then `(f2, f1)`.
  - That gives exactly the `BODY`-`BODY` and `SIDE`-`SIDE` behavior.
  - It must not be used for `PROJECTILE`-`PROJECTILE`, which is not symmetric.
  - Each `(A, A)` rule needs a test that fixes its order.

**Allocation on the hot path:**

The dispatch runs once per contact per sub-step, so it must not allocate. The rules:
- Handlers are `object`s, or instances built once at startup.
- The dispatcher calls `rule.begin(f2, f1, delta)` or `rule.begin(f1, f2, delta)` from an `if`, not through a `Pair`.
- A plain Kotlin lambda `(IFixture, IFixture, Float) -> Unit` compiles to `Function3` with a boxed `Float`. Use an interface method, or a `fun interface` with a primitive `Float` parameter.
- A registration DSL with lambdas is fine, because it runs once.

**Recommendation:**
- H2 is the base.
- H4 is the forward-looking variant. Choose it only if `020` or `017` is expected to add contact data (a normal, an overlap), or if a trace or phase needs to reach the handlers.
- H5 is optional sugar on top of H2, for the few rules that need only entities. It is not needed in the first branch.

#### 4.4 Phases: one object per pair, or three tables

| Option | Pros | Cons |
|---|---|---|
| **P1: one object per pair**, with `begin`/`continueContact`/`end` methods (default no-op) | All phases of one pair sit together. Stateful pairs (`FEET`-`BLOCK`, `WATER`, `LADDER`) read as one unit. Shared helpers for begin and continue are private methods in the same object. One table | The phase set of a pair is hidden by the defaults. Needs a `phases` mask, or a check of which methods are overridden, so that a test can compare the key set of each phase with the old chain |
| P2: three tables, one per phase | Matches the current layout. The key set of each phase compares directly with the old chain. A phase with no rule is an empty slot, so it is visible | The parts of one pair are spread over three places. Shared logic for begin and continue needs a separate helper |

**Recommendation:** P1, with an explicit phase set on each entry (for example `pair(FEET, BLOCK, FeetBlockRule, BEGIN, CONTINUE, END)`), so that the phases a pair handles are visible in the registration list, and a test can check them. This also blocks the risk in section 1, "the phases are not symmetric": an object with three methods keeps three separate bodies.

#### 4.5 Where the code lives

| Option | Pros | Cons |
|---|---|---|
| **One registration file** (`MegaContactRules.kt`, the list only) plus rule objects in group files (damage, terrain and friction, water, projectile, laser, triggers, gravity and force) | The full matrix is about 60 readable lines. Easy to compare with `MegaContactFilter`. Group files of a few hundred lines | Two places to look: the list and the body |
| Each group file registers its own rules (`fun ContactRulesBuilder.damageRules()`) | Clear ownership | The full pair list is spread out, unless a test prints it |
| Entities register rules at `init` | Very easy to extend | The table becomes run-time global state, and its contents depend on spawn order. The duplicate check moves from build time to spawn time. This is against the goals of the refactor |

**Recommendation:** the first option.
- Behavior that belongs to one entity stays in fixture receivers, not in table rows. The receivers already exist (13 kinds), and they are the extension point.
- New receivers replace the player special cases, for example:
  - `onLanded` for `FEET`-`BLOCK`;
  - an `IGateTrigger` capability for `GATE`;
  - water enter and exit hooks.

**Engine or core?**
- The mechanism is genre-neutral: the builder, the slot table, the dispatcher that implements `IContactListener`, and the build checks. So it can go in the engine as `ContactRuleTable<T : Enum<T>>`.
- The rules and `FixtureType` stay in core.
- YAGNI: put it in core first. Move it to the engine only when a second user exists, or when the engine-scope item asks for it. The move is cheap, because it does not depend on core.

#### 4.6 Data rows (JSON) or Kotlin

- Only the surface rules are really data:
  - `FEET`-`ICE`, `FEET`-`SAND`, `FEET`-`SNOW`, `SIDE`-`ICE`: a sense, an opt-out property and a friction value;
  - and in part the ladder rules.
- Even these have conditions in code:
  - `game.megaman.ready` in `FEET`-`SAND`;
  - the wall-slide check in `SIDE`-`ICE`;
  - the wall-push return in `FEET`-`ICE` continue.
- A Kotlin `data class SurfaceRule(sense, frictionX, frictionY, optOutKey)` with one generic handler gives "data" without a loader.
- JSON adds a loader, a schema and load-time checks. It gives nothing until levels need surfaces per map.
- **Recommendation:** defer JSON (YAGNI). This answers the open question in `ITEM.md`.
- **Wall-slide ice friction as a body property:** possible. `SIDE`-`ICE` already reads a body property (`ICE_FRICTION_Y`). The `Megaman` + `WALL_SLIDING` check can become a property that the wall-slide behavior sets, for example "apply side ice friction". This is the same "behavior sets a body flag" pattern as the climb fix in `MegaCollisionHandler`.

#### 4.7 Sketch (illustrative only)

```kotlin
interface ContactRule {
    fun begin(a: IFixture, b: IFixture, delta: Float) {}
    fun continueContact(a: IFixture, b: IFixture, delta: Float) {}
    fun end(a: IFixture, b: IFixture, delta: Float) {}
}

class ContactRuleTable<T : Enum<T>>(private val n: Int, private val typeOf: (IFixture) -> T) {
    internal val rules = arrayOfNulls<ContactRule>(n * n)
    internal val flip = BooleanArray(n * n)
    internal val ids = arrayOfNulls<String>(n * n)
    fun slot(contact: Contact) = typeOf(contact.fixture1).ordinal * n + typeOf(contact.fixture2).ordinal
}

class TableContactListener<T : Enum<T>>(private val table: ContactRuleTable<T>) : IContactListener {
    override fun beginContact(contact: Contact, delta: Float) {
        val i = table.slot(contact)
        val rule = table.rules[i] ?: return
        if (table.flip[i]) rule.begin(contact.fixture2, contact.fixture1, delta)
        else rule.begin(contact.fixture1, contact.fixture2, delta)
    }
    // continueContact and endContact have the same shape
}

val megaContactRules = contactRules<FixtureType> {
    wildcard("consumer", CONSUMER, ConsumerRule, BEGIN, CONTINUE, END)
    pair("damage", DAMAGER, DAMAGEABLE, DamageRule, BEGIN, CONTINUE)
    sets("death", setOf(DEATH), setOf(FEET, SIDE, HEAD, BODY), DeathRule, BEGIN, CONTINUE)
    pair("feet-block", FEET, BLOCK, FeetBlockRule, BEGIN, CONTINUE, END)
    pair("feet-ice", FEET, ICE, SurfaceRule(FEET_ON_ICE, frictionX = ICE_FRICTION), BEGIN, CONTINUE, END)
}

object DamageRule : ContactRule {
    override fun begin(a: IFixture, b: IFixture, delta: Float) = hit(a, b, ProcessState.BEGIN)
    override fun continueContact(a: IFixture, b: IFixture, delta: Float) = hit(a, b, ProcessState.CONTINUE)
    private fun hit(damagerFixture: IFixture, damageableFixture: IFixture, state: ProcessState) { /* old body */ }
}
```

### 5. Alternatives, including no change

| Option | What it is | Readability | Duplicate detection | Smart order | Change size | Notes |
|---|---|---|---|---|---|---|
| **K: keep as is** | The 1609-line chain | Poor. A pair's three phases are about 500 lines apart | None | Manual (`getFixturesInOrder`) | 0 | Works, and is deterministic. Today there are no overlaps, but nothing prevents them |
| **S: split the chain** | The same chain, but each branch calls a private function, for example `beginFeetBlock(feet, block, delta)` | Good: a dispatcher of about 150 lines | None | Still manual in the dispatcher | Small, a pure move. Easy to review with `git diff --color-moved` | Gives most of the readability gain at the lowest risk. No configuration |
| W: `when` on a normalized key | A `when (lo * n + hi)` | Good | Only for duplicate `const` values | Manual | Medium | A table fixed at compile time. Wildcard and set expansion by hand |
| O: ordered predicate list | A list of `(matcher, handler)`, first match wins | Medium | Overlaps stay hidden | Per matcher | Medium | The same rules as today by construction. But it keeps the problem that the review found, and it adds no checks |
| D: double dispatch | Each fixture type owns its handlers | Poor for pairs: `FEET`-`BLOCK` is split over two owners | Hard | Natural ("self" first) | Large | Rejected |
| R: receivers only | All logic moves into entity receivers | Good per entity, poor overall | None | Natural | Very large | Copies shared rules (death, friction, damage) into many entities. Use it only for the parts that belong to one entity |
| **T: keyed table** | Section 4 | Good. The matrix is visible in one list | At build time | By registration (`flip`) | Medium to large | Gives configuration and per-rule tests |

### 6. Pros and cons by lens

Only K (keep), S (split) and T (table) are compared, because the others were rejected above.

**Maintainability**
- K: To change one pair, you read three places in a 1609-line file. The log texts are already wrong in 5 places, which shows how copy-and-paste in the chain drifts. There are no tests, so each change is checked only by play-testing.
- S: Each pair becomes a named function. The phase asymmetries become visible side by side, for example `beginSideBlock` next to `endSideBlock`. Still no build checks.
- T: Gives what S gives, plus a single matrix, build checks, and rule objects that a unit test can call directly with two mock fixtures. Cost: a new small mechanism (about 150 lines with tests) that readers must learn.

**YAGNI**
- The churn data argues against a large framework. In the last year only 2 pairs were added, and one of them is dead.
- The features that the churn does not justify:
  - JSON rules;
  - registration of rules by entities at run time;
  - a `priority` system;
  - a generic engine module;
  - typed role sugar (H5).
- The features that the history does justify:
  - **Receivers that replace the `Megaman` special cases.** That is where most edits happen.
  - **The build-time duplicate check.** It is cheap, and it protects the "no overlaps" property that today holds only by chance.
- S alone meets YAGNI the best. T is justified mainly by the configuration goal of the refactor (the owner's answer to review item 13), not by the rate of change.

**DRY**
- K: 14 bodies are copied between begin and continue. The friction, sense, block-hit and guard code is repeated about 10 times each.
- S: The copied bodies can share helpers, but each phase still needs its own entry in the chain.
- T: One object per pair holds its shared helpers. One `SurfaceRule` class replaces 8 friction branches. Set and wildcard expansion removes the shared `set1`/`set2`/`out` scratch state.
- Warning: DRY must not merge bodies that only *look* the same. The guards that differ between phases (section 1) are real behavior today. Merge them only where they are the same character for character.

**Risks**

| Risk | K | S | T |
|---|---|---|---|
| Behavior change by a mistake in the move | None | Low (pure move) | Medium |
| The orientation of same-type pairs (`PROJECTILE`×`PROJECTILE`) | — | Low | Medium: `(A, A)` must not use `flip` |
| The phase asymmetries become "normalized" | — | Low | Medium with P1 shared helpers |
| Dead rules become live (for example, if the filter is later derived from the table keys) | — | — | Medium. Keep the filter separate, or record it as an intended change |
| No automated proof (no tests, replay `055` is far away) | — | Medium | High |
| Merge conflicts with `029`/`030`/`020` | — | Low | Medium to high |

**Forward-looking**
- T fits the other planned items best:
  - `029` re-types receivers, not chain branches.
  - `030` faction checks go into one `DamageRule` and the `PROJECTILE` rules.
  - `020` needs a trace keyed by rule ID, and the table dispatcher is the single place to emit it.
  - Mods or new games on the engine get a contact API, and not a file to edit.
- H4 (`ContactContext`) is the most future-proof signature. Choose it only if contact data or tracing is expected in the handlers.
- K blocks these items: each of them must edit the 1609-line file.

### 7. How to keep the risk low

1. **Stage the work.** Each stage is one commit that can be diffed:
   1. S: split each branch into a private function. A pure move.
   2. Swap the dispatcher from the chain to the table. The function bodies do not change.
   3. Replace the `Megaman` checks with receivers or body flags. One pair at a time, each a small, readable diff.
   - Stage 3 can be a separate branch (`014b`), so that stages 1-2 merge before `029`.
2. **Tests before the move** (no game needed):
   - The key set of each phase equals a list taken once from the old chain: 48 begin pairs, 29 continue pairs, 17 end pairs.
   - For every pair, both contact orientations reach the same `(roleA, roleB)`. For `(A, A)`, the contact order is kept.
   - Every `(CONSUMER, X)` goes to the consumer rule, and `(CONSUMER, CONSUMER)` uses `fixture1`.
   - `build()` rejects duplicates, ambiguous sets and wildcard clashes.
   - The filter and the table differ in exactly the known cases (section 2, items 1-3). A change to these differences then fails a test.
3. **A contact trace** (one line per handled contact: tick, phase, type A, type B, entity IDs, rule ID) in the old listener and the new dispatcher.
   - Diff the traces from a run with fixed settings: the same container, the same performance tier, and the same seed when `011` exists.
   - Until `006`/`055` exist, use a manual run of a fixed route. Compare `--level X --room Y` runs of the levels that use the special pairs, for example ice, sand, water, ladders, gates and lasers.
   - This trace is also the proof tool for `020`.
4. **Keep the dead rules dead,** and file them as issues. Do not "fix" them by accident.

### 8. Open questions for the owner

1. Which signature: H2 (plain ordered fixtures), or H4 (a context object that can grow)?
2. P1 (one object per pair) or P2 (three tables)?
3. Should the mechanism start in core, or go into the engine at once?
4. Should stage 3 (the `Megaman` receivers) be in this item, or wait for `029`'s `IPlayer` types?
5. File the side findings in section 2 as issues now?
6. Is a manual trace diff of fixed routes enough for the `014` contract, without `006`/`055`?

The owner answered questions 1, 4 and 5 on 2026-10-22. See "Decisions" in `ITEM.md`.

## Per-pair classes and sample code (2026-10-22)

The owner asked: can each pair effect be its own class and file, and is that justified?

### How the 35 rule entries split

A set rule counts as one entry here. `PROJECTILE` counts as one entry with 7 sub-rules.

| Kind | Entries | Own class? |
|---|---|---|
| **Receiver forward only** (the body is "if the fixture has receiver X, call it") | `BODY`-`BODY`, `SIDE`-`SIDE`, `BODY`-`FEET`, `HEAD`-`FEET`, `BODY`-`SIDE`, `BODY`-`EXPLOSION`, `BLOCK`-`EXPLOSION`, `SHIELD`-`SIDE`, `BODY`-`PLAYER` (9) | No. About 4 generic classes cover them (`HitByFeetRule`, `HitByBodyRule`, `HitByExplosionRule`, a symmetric variant). Each pair is one row in the registration file |
| **Mirrors** (the same body with a different sense or fixture) | `FEET`-`LADDER` and `HEAD`-`LADDER`; `LASER`-`BLOCK` and `LASER`-`SHIELD` (4) | One class each, used twice with a parameter |
| **Surfaces** | `FEET`-`ICE`, `FEET`-`SAND`, `FEET`-`SNOW`, `SIDE`-`ICE` (4) | One shared base (sense + opt-out + friction). Small subclasses for the sand splash, the snow receiver and the left/right ice sense |
| **Real logic** (guards, state, several steps) | `CONSUMER`, `DAMAGER`-`DAMAGEABLE`, `DEATH` set, `BLOCK`-`BODY`, `FEET`-`BLOCK`, `HEAD`-`BLOCK`, `SIDE`-`BLOCK`, `GATE` set, `BOUNCER` set, `WATER_LISTENER`-`WATER`, `BODY`-`FORCE`, `BODY`-`GRAVITY_CHANGE`, `BLOCK`-`GRAVITY_CHANGE`, `PROJECTILE` set, `PLAYER`-`ITEM`, `FEET`-`CART`, `TELEPORTER_LISTENER`-`TELEPORTER`, `LASER`-`BODY` (18) | Yes. One class and one file each |

The result is about 18 rule files, 3 or 4 generic or shared rule files, the registration file and
the table mechanism, all in a new package `world/contacts/rules/`.

`PROJECTILE` is the exception to "one file per class". Its 7 sub-rules share a prefix, and they read
best together. Use one file `ProjectileRules.kt` with a base class and 7 small `object`s.

### Is it justified?

For the 18 entries with real logic: yes.

- **Parallel work:** `029` (`IPlayer`), `030` (faction) and `020` (broad phase) each touch a few
  rules. With one file per rule, their diffs touch different files, and merges do not conflict in
  one 1609-line file.
- **Tests:** each file is one test target (`FeetBlockRuleTest`), with two mock fixtures and no
  `Contact`.
- **Phase asymmetries become visible:** `onBegin`, `onContinue` and `onEnd` sit together in one
  class of 40-120 lines, so a reader sees at once that end skips a guard.
- **Search and history:** `FeetBlockRule.kt` can be found by name. `git log` of that file shows only
  that rule's changes.

For the 9 receiver-forward entries: no. A file per pair would hold more `package` and `import`
lines than logic. A generic class and one registration row give the same name and test without the
boilerplate.

Costs to accept:

- About 25 new files instead of one. The registration file is the single view of the whole matrix.
- Shared helpers (the left/right sense from the `SIDE` property, the `NO_*_TOUCHIE` guards, the
  block-hit steps) need a home, for example `ContactRuleHelpers.kt`. Without it, the copies move
  from the chain into the files.

YAGNI check: this costs no more than the table itself. Each rule must become a `ContactRule`
anyway, so making it a top-level class in its own file adds only the file.

### Sample code (illustrative, not compiled)

**Engine: the listener takes plain fixtures.** `WorldSystem.processContacts` passes
`it.fixture1, it.fixture2` instead of `it`. Then nothing outside `WorldSystem` needs `Contact`,
and its type helpers can be removed.

```kotlin
interface IContactListener {

    fun beginContact(fixture1: IFixture, fixture2: IFixture, delta: Float)

    fun continueContact(fixture1: IFixture, fixture2: IFixture, delta: Float)

    fun endContact(fixture1: IFixture, fixture2: IFixture, delta: Float)
}
```

**The rule interface.** The table calls the methods with the fixtures in the rule's role order.

```kotlin
interface ContactRule {

    fun onBegin(a: IFixture, b: IFixture, delta: Float) {}

    fun onContinue(a: IFixture, b: IFixture, delta: Float) {}

    fun onEnd(a: IFixture, b: IFixture, delta: Float) {}
}
```

**The table and its builder.** One slot per ordered type pair. The rules are stored per phase, so a
test can compare each phase's pairs with the old chain. The `flip` flag is the "smart" swap.

```kotlin
class ContactRuleTable internal constructor(
    private val size: Int,
    private val rules: Array<Array<ContactRule?>>,
    private val flip: BooleanArray,
    private val ids: Array<String?>
) {

    fun dispatch(phase: ProcessState, fixture1: IFixture, fixture2: IFixture, delta: Float) {
        val slot = slot(fixture1, fixture2)
        val rule = rules[phase.ordinal][slot] ?: return
        if (flip[slot]) call(rule, phase, fixture2, fixture1, delta)
        else call(rule, phase, fixture1, fixture2, delta)
    }

    fun pairs(phase: ProcessState): Set<Pair<FixtureType, FixtureType>> = ...

    private fun slot(fixture1: IFixture, fixture2: IFixture) =
        (fixture1.getType() as FixtureType).ordinal * size + (fixture2.getType() as FixtureType).ordinal

    private fun call(rule: ContactRule, phase: ProcessState, a: IFixture, b: IFixture, delta: Float) =
        when (phase) {
            ProcessState.BEGIN -> rule.onBegin(a, b, delta)
            ProcessState.CONTINUE -> rule.onContinue(a, b, delta)
            ProcessState.END -> rule.onEnd(a, b, delta)
        }
}

class ContactRulesBuilder {

    private val size = FixtureType.entries.size
    private val rules = Array(ProcessState.entries.size) { arrayOfNulls<ContactRule>(size * size) }
    private val flip = BooleanArray(size * size)
    private val ids = arrayOfNulls<String>(size * size)

    fun pair(id: String, a: FixtureType, b: FixtureType, rule: ContactRule, vararg phases: ProcessState) {
        put(id, a.ordinal * size + b.ordinal, rule, phases, flipped = false)
        if (a != b) put(id, b.ordinal * size + a.ordinal, rule, phases, flipped = true)
    }

    fun sets(id: String, a: Set<FixtureType>, b: Set<FixtureType>, rule: ContactRule, vararg phases: ProcessState) {
        check(a.intersect(b).isEmpty()) { "Contact rule '$id': a type is on both sides, so its role order is not defined" }
        a.forEach { x -> b.forEach { y -> pair(id, x, y, rule, *phases) } }
    }

    fun wildcard(id: String, a: FixtureType, rule: ContactRule, vararg phases: ProcessState) =
        FixtureType.entries.forEach { pair(id, a, it, rule, *phases) }

    fun build() = ContactRuleTable(size, rules, flip, ids)

    private fun put(id: String, slot: Int, rule: ContactRule, phases: Array<out ProcessState>, flipped: Boolean) {
        val owner = ids[slot]
        check(owner == null || owner == id) { "Contact rules '$owner' and '$id' register the same pair" }
        phases.forEach { phase ->
            check(rules[phase.ordinal][slot] == null) { "Contact rule '$id' registers a pair twice for $phase" }
            rules[phase.ordinal][slot] = rule
        }
        ids[slot] = id
        flip[slot] = flipped
    }
}
```

**The listener becomes a thin dispatcher.**

```kotlin
class MegaContactListener(private val rules: ContactRuleTable) : IContactListener {

    override fun beginContact(fixture1: IFixture, fixture2: IFixture, delta: Float) =
        rules.dispatch(ProcessState.BEGIN, fixture1, fixture2, delta)

    override fun continueContact(fixture1: IFixture, fixture2: IFixture, delta: Float) =
        rules.dispatch(ProcessState.CONTINUE, fixture1, fixture2, delta)

    override fun endContact(fixture1: IFixture, fixture2: IFixture, delta: Float) =
        rules.dispatch(ProcessState.END, fixture1, fixture2, delta)
}
```

**The registration file: the whole matrix in one place.**

```kotlin
fun megaContactRules() = ContactRulesBuilder().apply {
    wildcard("consumer", CONSUMER, ConsumerRule, BEGIN, CONTINUE, END)
    pair("damage", DAMAGER, DAMAGEABLE, DamageRule, BEGIN, CONTINUE)
    sets("death", setOf(DEATH), setOf(FEET, SIDE, HEAD, BODY), DeathRule, BEGIN, CONTINUE)
    pair("feet-block", FEET, BLOCK, FeetBlockRule, BEGIN, CONTINUE, END)
    pair("body-feet", BODY, FEET, HitByFeetRule, BEGIN, CONTINUE, END)
    pair("head-feet", HEAD, FEET, HitByFeetRule, BEGIN, CONTINUE, END)
    pair("feet-ladder", FEET, LADDER, LadderRule(BodySense.FEET_TOUCHING_LADDER), BEGIN, CONTINUE, END)
    pair("head-ladder", HEAD, LADDER, LadderRule(BodySense.HEAD_TOUCHING_LADDER), BEGIN, CONTINUE, END)
    sets("gate", setOf(SIDE, FEET, HEAD), setOf(GATE), GateRule, BEGIN)
    // ... the other entries
}.build()
```

**A rule with real logic and a new receiver: `FeetBlockRule`.**

- Today begin and continue differ only in the `Megaman` code: begin plays the land sound, continue
  does not. When that code moves into Megaman's receiver, which gets the `ProcessState`, begin and
  continue share one function. This is a DRY gain that the receivers make possible.
- The new receiver is called at the same point as the old `is Megaman` block: after the stick
  translate, before `FEET_ON_GROUND` and `block.hitByFeet`.
  - The existing `hitByBlockReceiver` runs later, after `block.hitByFeet`. A `Block` override (there
    are 4) can change the body before the land-sound check reads the velocity. So reusing it could
    change behavior.
  - Hence the separate receiver.

```kotlin
object FeetBlockRule : ContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = touch(ProcessState.BEGIN, a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = touch(ProcessState.CONTINUE, a, b, delta)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) {
        val feetFixture = a
        val blockFixture = b
        if (!canTouch(feetFixture, blockFixture)) return

        val block = blockFixture.getEntity() as Block
        val body = feetFixture.getBody()

        body.removeFeetBlock(block)
        if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)

        feetFixture.getFeetBlockContact(ProcessState.END, block)

        block.hitByFeet(ProcessState.END, feetFixture)
        if (feetFixture.hasHitByBlockReceiver(ProcessState.END))
            feetFixture.getHitByBlock(ProcessState.END, block, delta)
    }

    private fun touch(state: ProcessState, feetFixture: IFixture, blockFixture: IFixture, delta: Float) {
        if (!canTouch(feetFixture, blockFixture)) return

        val block = blockFixture.getEntity() as Block
        val body = feetFixture.getBody()

        if (!blockFixture.getBody().physics.collisionOn) {
            body.removeFeetBlock(block)
            if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)
            return
        }

        body.addFeetBlock(block)
        if (feetFixture.shouldStickToBlock(state, blockFixture))
            body.translate(blockFixture.getBody().getPositionDelta())

        feetFixture.getFeetBlockContact(state, block)

        body.setBodySense(BodySense.FEET_ON_GROUND, true)

        block.hitByFeet(state, feetFixture)
        if (feetFixture.hasHitByBlockReceiver(state)) feetFixture.getHitByBlock(state, block, delta)
    }

    private fun canTouch(feetFixture: IFixture, blockFixture: IFixture) =
        !blockFixture.hasFixtureLabel(FixtureLabel.NO_FEET_TOUCHIE) &&
            (!feetFixture.hasFilter() || feetFixture.getFilter().invoke(blockFixture))
}
```

**The receiver helpers**, in the style of `IFixtureExtensions.kt`:

```kotlin
fun IFixture.setFeetBlockContactReceiver(receiver: (ProcessState, Block) -> Unit): IFixture {
    putProperty(ConstKeys.FEET_BLOCK_CONTACT, receiver)
    return this
}

fun IFixture.getFeetBlockContact(state: ProcessState, block: Block) {
    (getProperty(ConstKeys.FEET_BLOCK_CONTACT) as ((ProcessState, Block) -> Unit)?)?.invoke(state, block)
}
```

**Megaman registers the receiver** in `megaman/components/BodyComponent.kt`. The code is the old
`is Megaman` blocks from `L:326-342`, `L:928` and `L:1399-1407`:

```kotlin
feetFixture.setFeetBlockContactReceiver { state, block ->
    when (state) {
        ProcessState.BEGIN -> {
            aButtonTask = AButtonTask.JUMP
            if (blockMakesLandSound(block) && canMakeLandSound) {
                requestToPlaySound(SoundAsset.MEGAMAN_LAND_SOUND, false)
                canMakeLandSound = false
            }
        }
        ProcessState.CONTINUE -> aButtonTask = AButtonTask.JUMP
        ProcessState.END -> {
            if (!bodyOverGround)
                aButtonTask = if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
            if (!body.hasAnyFeetBlock()) canMakeLandSound = true
        }
    }
}

private fun Megaman.blockMakesLandSound(block: Block) = block.getOrDefaultProperty(
    "${ConstKeys.FEET}_${ConstKeys.SOUND}",
    !block.body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) || when (direction) {
        Direction.UP -> body.physics.velocity.y <= 0f
        Direction.DOWN -> body.physics.velocity.y >= 0f
        Direction.LEFT -> body.physics.velocity.x >= 0f
        Direction.RIGHT -> body.physics.velocity.x <= 0f
    },
    Boolean::class
)
```

**A capability instead of a receiver: `GateRule`.** When the check is "is this entity allowed to
do X" and not "what does it do", a small capability interface fits better than a receiver. `029`
can fold `IGateTrigger` into `IPlayer` later.

```kotlin
interface IGateTrigger

object GateRule : ContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) {
        if (a.getEntity() !is IGateTrigger) return
        val gate = b.getEntity() as Gate
        if (gate.isTriggerable()) gate.trigger()
    }
}
```

**A generic receiver rule, used by two pairs.**

```kotlin
object HitByFeetRule : ContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = forward(ProcessState.BEGIN, a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = forward(ProcessState.CONTINUE, a, b, delta)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = forward(ProcessState.END, a, b, delta)

    private fun forward(state: ProcessState, target: IFixture, feetFixture: IFixture, delta: Float) {
        if (target.hasHitByFeetReceiver(state)) target.getHitByFeet(state, feetFixture, delta)
    }
}
```

**Tests.** No game and no `Contact` are needed.

```kotlin
class ContactRuleTableTest : DescribeSpec({

    fun fixture(type: FixtureType) = mockk<IFixture>(relaxed = true) { every { getType() } returns type }

    describe("dispatch") {
        it("passes the fixtures in role order for both contact orders") {
            val calls = mutableListOf<Pair<IFixture, IFixture>>()
            val rule = object : ContactRule {
                override fun onBegin(a: IFixture, b: IFixture, delta: Float) {
                    calls.add(a to b)
                }
            }
            val table = ContactRulesBuilder().apply {
                pair("damage", FixtureType.DAMAGER, FixtureType.DAMAGEABLE, rule, ProcessState.BEGIN)
            }.build()
            val damager = fixture(FixtureType.DAMAGER)
            val damageable = fixture(FixtureType.DAMAGEABLE)

            table.dispatch(ProcessState.BEGIN, damager, damageable, 0f)
            table.dispatch(ProcessState.BEGIN, damageable, damager, 0f)

            calls shouldBe listOf(damager to damageable, damager to damageable)
        }
    }

    describe("build") {
        it("rejects two rules for the same pair") {
            shouldThrow<IllegalStateException> {
                ContactRulesBuilder().apply {
                    pair("one", FixtureType.FEET, FixtureType.BLOCK, object : ContactRule {}, ProcessState.BEGIN)
                    pair("two", FixtureType.BLOCK, FixtureType.FEET, object : ContactRule {}, ProcessState.BEGIN)
                }
            }
        }
    }

    describe("megaContactRules") {
        it("has the same begin pairs as the old chain") {
            megaContactRules().pairs(ProcessState.BEGIN) shouldBe OLD_BEGIN_PAIRS
        }
    }
})
```

### Notes on the sample

- The dispatch does not allocate: the rules are `object`s or instances built once, and `delta` stays
  a primitive `Float` through the interface methods.
- A same-type pair (`a == b`) fills one slot with `flipped = false`. So `BODY`-`BODY`, `SIDE`-`SIDE`
  and `PROJECTILE`-`PROJECTILE` get the contact order, as today.
- The `id` strings are for the build errors and a later contact trace (one line per dispatch with
  the rule ID).
- The builder rejects a second rule for the same pair even across phases. The old chain has none of
  those, so this is safe today. If two rules ever need the same pair in different phases, relax the
  check to "per phase".

## Research: contact trace ("print the paths") (2026-10-22)

The owner asked whether a setup that prints which rule each contact takes is feasible, or too much.
A read-only research agent did this research on `master` at `32257e0e2`.

### Verdict

- **Too much for `014`:** a full trace diffed line by line against manual play. Two manual runs
  never give the same lines. Making them match needs deterministic replay, which is the work of
  `010`, `011`, `031`, `006` and `055`.
- **Cheap, and a stronger proof than play alone:**
  1. **An exhaustive selection test** (do it in `014`).
  2. **A per-room rule-hit summary** behind a flag (optional in `014`).

### Facts that drive the design

- **Selection depends only on the fixture types** (section 1). So "which rule, in which role
  order" is a pure function of `(phase, type1, type2)`. 29 × 29 × 3 = 2523 cases cover every
  possible input. A test can check all of them. Play cannot.
- **The old log texts cannot serve as IDs.** 20 of the 80 branches have no `printDebugLog`, and 5
  texts are wrong. So the trace needs explicit IDs.
- **The listener can be wrapped with no engine change.** It is built only at
  `MegamanMaverickGame.kt:863`, and `WorldSystem.contactListener` is a `var` (`WorldSystem.kt:28`).
  `WorldSystem.iteration` (`:87`) gives the sub-step. There is no frame counter yet; add one next to
  `diagnostics?.beginFrame()` (`MegamanMaverickGame.kt:435`).
- **Contact order is deterministic with the default `SimpleGrid` container**, for the same body
  order. `QuadtreeWorldContainer` iterates `HashSet`s (`:133-134`), so use `--worldContainer simple`
  (the default).
- **Entity IDs:**
  - Tiled entities use their map object ID, which is stable.
  - Other entities get negative IDs from a global LIFO pool. These match between runs only if the
    spawn and destroy order matches.
  - So a full trace prints `Class#id`, and a summary prints the class only.
- **The existing outputs do not fit:**
  - `RuntimeDiagnostics` drops frames when its 256-slot queue is full (`RuntimeDiagnostics.kt:155`).
  - `AsyncFileWriter` opens the file once per line.
  - `GameLogger` adds timestamps and filters by tag.
  - So the trace needs its own buffered writer, or it keeps counters in memory and writes them in
    `dispose()`.
- **Volume:** at `MEDIUM` there are 150 sub-steps per second, and each live pair calls `continue`
  in every sub-step. That is about 10^4-10^5 lines per second in busy rooms. So a full mode writes
  begin and end lines only, plus a `continue` count at end.

### Comparison strategies

| Strategy | Needs determinism | What it catches | Cost | Verdict |
|---|---|---|---|---|
| a. Exact diff with record and replay (`055`) | Yes | Everything, including order and effects (with a state hash) | Far away in the dependency chain | Wait for `055` |
| b. A small recorder and replayer only for `014` (wrap `controllerPoller`, fixed delta through a `Gdx.graphics` proxy, seed `UtilMethods.random` and `MathUtils.random`) | Yes, partly | Same as a, where the run is deterministic | About 250 LOC. `superRandom`, Kotlin `Random.Default` (about 30 calls), `timeSpawned` and the Darkness clock stay non-deterministic. Mostly a throwaway copy of `010`/`011`/`031`/`055` | No |
| c. **Aggregate summary**: counts per `(room, phase, typeA, typeB, ruleId)` on a manual route, plus a "rules never hit" list | No | A rule that no longer fires, an extra rule, a large change in count. Not order changes or wrong effects | About 150-200 LOC, including the flag | Optional, recommended |
| d. Live shadow mode: the table's `select` runs next to the old chain and logs mismatches | No | Selection mismatches on the pairs that the route hits | About 10 LOC on top of stage 0 | No: the offline form below is stronger |
| e. **Exhaustive selection test**, offline | No | Every selection difference, for every pair, both orders, all phases | About 180 LOC, about 1 day | **Yes** |
| f. Headless scenario tests per rule (mock fixtures, check senses, receivers, `frictionOnSelf`) | No | Wrong effects in the rules tested | About 30-60 LOC per rule. Cheap only after the receivers remove the concrete-class casts | Yes, for the rules that stage 3 changes |

### The exhaustive selection test ("record and abort")

1. **Stage 0 on the `014` branch:**
   - Add a `ContactRuleIds` object (for example `FEET_BLOCK = "feet-block"`).
   - In the old chain, add one line right after each branch's role destructuring:
     `onRule?.invoke(ContactRuleIds.FEET_BLOCK, a, b)`. That is 80 lines.
   - For `PROJECTILE`, use the ID `"projectile/" + otherFixture.getType()`, placed before the
     `NO_PROJECTILE_COLLISION` guard.
   - The new `ContactRulesBuilder.pair(id, …)` calls use the same constants, so the IDs cannot drift.
2. **The test:**
   - It sets `onRule` to a lambda that records `(phase, id, a, b)` and then throws a private
     sentinel exception. So the rule body never runs, and stub fixtures (a type and empty
     properties) are enough.
   - It loops over the 29 × 29 ordered pairs × 3 phases and writes `phase typeA typeB -> id roleOrder`
     to a golden file (about 200 matched lines; an unmatched pair gives `-`).
3. **After the swap to the table,** the same test runs against `ContactRuleTable.select` and must
   give the same golden file.

This covers the tests planned in section 7.2 from data, not from hand-written lists:
- the key sets (48 / 29 / 17);
- both orders;
- `(A, A)` keeps the contact order;
- the `CONSUMER` wildcard, and `CONSUMER`×`CONSUMER` uses `fixture1`.

It runs in CI.

### Where to hook a live trace

| Hook | Pros | Cons |
|---|---|---|
| Engine `WorldSystem.processContacts` | Generic; `020` could use it | A new engine API. Does not know the rule ID |
| `ContactRuleTable.dispatch` | One line | New code only |
| **Decorator `TracingContactListener(delegate)`** in `core`, set at `MegamanMaverickGame.kt:863` | One wrapper for the old code, the new code and `020`. Zero cost when off. No engine change. Can replace `CONTACT_LISTENER_DEBUG_FILTER`, which `014` must move anyway | Gets the rule ID from the delegate (a `lastRuleId` field) |

Recommendation: the decorator for the context; the rule ID from the old branch calls, or from the
one dispatcher line.

Line format for a full mode:

```
f=1234 s=2 BEGIN feet-block FEET:Megaman#-3 BLOCK:Block#57 flip=0
```

**Flag:**
- `--contactTrace <file>` and `--contactTraceMode summary|full` (default `summary`).
- Plumb it like `--diagnostics`: `DesktopAppArgs` → `MegamanMaverickGameParams` → wrap the listener
  in `createGameEngine()` only when set.
- `resolveLaunchOptions` is for start options, so it is not needed. It can check that the path is
  writable, if an exit with code 2 is wanted.

**Not traced:**
- **Guard exits:** they double the instrumentation, and they are only meaningful with replay. Stages
  1-2 are verbatim moves (`git diff --color-moved`), and stage 3 has unit tests.
- **Per-tick state hash, effect fingerprint (sense bits after each dispatch):** replay only, so wait
  for `055`.

### Staged plan

1. **Stage 0:** `ContactRuleIds`, the 80 `onRule` lines, and the golden selection test. This is the
   main proof for stages 1-2.
2. **Stages 1-2:** move the bodies verbatim, swap to the table, run the golden test against
   `select`.
3. **Optional:** `TracingContactListener` and the summary flag. Use it on the agreed manual routes:
   ice, sand, snow, water, ladders, gates, lasers, carts, teleporters, bouncers and gravity. Compare
   the rule sets per room, and play until the "never hit" list is empty or explained.
4. **Stage 3** (the receivers): unit tests per changed rule, plus the manual routes with the summary.

The decorator and the line format become the trace for `020` and `055`, so this work is not lost.

## Research: narrow the engine fixture type (2026-10-22)

The owner asked whether the engine should type fixtures as something other than `Any`, for example
`Int`, `Short` or `Long`. A read-only research agent did this research on `master` at `26a1597f2`.
It compiled the interface sketch below with the project's Kotlin 2.3.20 compiler in a scratch
folder.

### Verdict

Narrow it, but to a small engine interface, not to a primitive:

```kotlin
interface IFixtureType {
    val ordinal: Int
    val name: String
}

interface IFixture : ITypable<IFixtureType>, IPropertizable

enum class FixtureType : IFixtureType { BLOCK, PLAYER, ... }
```

- An enum gets `ordinal` and `name` from the interface for free, so `FixtureType` needs no code.
- `when (fixture.getType())` and `getType() == FixtureType.X` still compile.
- If a name that does not suggest enums is wanted, call the property `index` and add one line:
  `override val index get() = ordinal`.

### Inventory

- **`ITypable<T>`** has 3 users. Only `IFixture : ITypable<Any>` uses `Any`. `IMegaGameEntity` uses
  `EntityType` and `Fade` uses `FadeType`, which are already narrow.
- **Engine:**
  - `Fixture.kt:17,38` (`type: Any`);
  - `Body.kt:30` (`fixtures: OrderedMap<Any, OrderedSet<IFixture>>`) and `:62-92`;
  - `IBody.kt:18` (`getFixtures(out, vararg params: Any)`);
  - `BodyBuilder.kt:47`;
  - the 6 `Contact` helpers;
  - a dummy `getType()` at `WorldSystem.kt:65`.
  - The engine itself never reads the type on the hot path. Only the client filter does.
- **Core:**
  - 1751 `FixtureType.X` references in 321 files;
  - about 876 `Fixture(...)` calls;
  - about 32 `getType() ==` checks, 17 `when (getType())`, 17 `getType().equalsAny(...)`;
  - 23 `body.fixtures[...]` lookups;
  - 4 `as FixtureType` casts (`MegaContactFilter.kt:96,143,144`, `FixtureTypeOverlapSpawn.kt:71`).
  - Data paths stay valid: `FixtureType.valueOf` from Tiled strings (`FixtureTypeOverlapSpawn.kt:56`)
    and `ConstKeys.FIXTURES` pairs (`Block.kt:173`).
- **Engine tests use strings as fixture types:**
  - 44 `Fixture(body, "...")` constructions, in `FixtureTest`, `ContactTest`, `WorldSystemTest`
    (`ObjectMap<Any, ObjectSet<Any>>`), the two container tests (`getType() == "keep"`) and
    `BodyTest`.
  - No core test builds a `Fixture`.

### Options

| Option | Type safety | Hot path | Genericity | Logs and trace | Migration |
|---|---|---|---|---|---|
| a. `Any` (today) | None. A wrong type gives a `ClassCastException` at run time | As c | High on paper, but the table needs a cast or an `(Any) -> Int` function | Good | 0 |
| b. `Int`/`Short`/`Long` ID | Weak: any number is accepted | No boxing only with a dedicated `val typeId: Int`. `ITypable<Int>` boxes. The gain over c is too small to measure | Data-driven IDs are easy, but they have no name | Numbers only. The trace needs a name table | Large, and with **silent breaks** (see below) |
| c. **`IFixtureType` (ordinal + name)** | Good: only declared types | As today: identity hash in maps; the table reads `getType().ordinal` with no boxing and no cast | Any enum, or a class such as `NamedFixtureType(name, ordinal)` for types from JSON | Names for free | Small |
| d. Generic `IFixture<T : Enum<T>>` | Best in theory | As today | Low: the type parameter spreads into `Body`, `IBody`, `Contact`, `WorldSystem` and the containers (about 1000 uses). `Enum<T>` also blocks data-driven types | Good | Very large |
| e. `@JvmInline value class FixtureTypeId(val v: Int)` | Medium | **Boxed** (one allocation each time) as an `OrderedMap` key, in `ObjectSet` and in `vararg Any`, which are exactly the lookup paths | Medium | Numbers | Large |

**The silent breaks with option b.** These APIs take `Any`, so they still compile when they are
given an enum, and then they always return false:
- `equalsAny(vararg Any)` (17 sites);
- `getFixtures(vararg Any)`;
- the `Contact` helpers;
- `ObjectSet<Any>`.

The `==` and `when` sites do fail to compile. This is the main reason to choose c over b.

**`Short`, `Byte` and `Long`:**
- Memory does not matter at 29 types, and the JVM does arithmetic on `int`.
- `Long` helps only as a Box2D-style category/mask bitmask (at most 64 types). A mask can be built
  from the dense ordinal, so the type itself does not need to be a `Long`.
- A mask would replace only the last lookup in `MegaContactFilter` (`:143-144`). It cannot replace:
  - the key-only `shouldProceedFiltering`;
  - the same-entity, enemy-enemy and gate/cart guards;
  - the `CONSUMER` filter;
  - the per-fixture filters.
- An option for later, when the table branch touches the filter.

**Fit with the table:** `ContactRuleTable(types: List<IFixtureType>)` can check `types[i].ordinal == i`
at build time. A test can check `types[t.ordinal] === t`, which catches a type from another set.

**Fit with the typed receivers:** independent. The receivers are typed `Properties` keys, not fixture
types.

### Other `Any`-typed engine keys

`Event.key` and `eventKeyMask`, `Properties` keys, and the map keys of the sprites, animations,
behaviors, updatables, points, controller, fonts and body `preProcess`/`postProcess` maps all use
`Any`.

All of these are names for lookups only. None needs a dense index or a dispatch table, and none is
cast back on a hot path. So narrow only fixtures. A convention for later: "when the engine dispatches
on a client classification, it takes a small interface with a dense `ordinal` and a `name`." Event
keys can follow it in `053`, if events get a dispatch table.

### Proposed branch scope

**Engine:**
- `world/body/IFixtureType.kt`;
- `IFixture : ITypable<IFixtureType>`;
- `Fixture(type: IFixtureType)`;
- `Body.fixtures: OrderedMap<IFixtureType, OrderedSet<IFixture>>`;
- `BodyBuilder`;
- `IBody.getFixtures(out, vararg params: IFixtureType)`;
- the dummy at `WorldSystem.kt:65`.

**Core:**
- `enum class FixtureType : IFixtureType`;
- the dummy at `MegaContactListener.kt:1603`.
- Keep the 4 casts. The table branch removes the filter casts.

**Engine tests:** an `internal enum class TestFixtureType : IFixtureType` replaces the 44 string
types and the `ObjectMap<Any, ...>` in `WorldSystemTest`.

**Optional:** an `IFixtureType` overload of `equalsAny`. The `Contact` helpers keep `Any`, because the
table branch deletes them.

**Out of scope:** event keys, `Properties` keys, component map keys, bitmask filters, the table.

**Keep behavior identical:**
- The run-time representation does not change. Only the compile-time types change.
- Keep `Body.fixtures` an insertion-ordered `OrderedMap`. Do not change it to an array indexed by
  ordinal: `forEachFixture` order sets the contact order.
- Proof: the build, `engine:test core:test lwjgl3:jar`, a grep that shows no fixture-type `Any` left
  in `engine/.../world/body`, and a short smoke run.

**Order:**
- It must merge before the table branch.
- It does not depend on the typed-receiver branch. They touch different files: this branch changes
  the `Fixture` constructor, `Body` and the tests; the receivers change `IFixtureExtensions.kt`. So
  they can run in parallel.

### Follow-up: fixture types that are not enums (2026-10-22)

The owner asked: how is `IFixtureType` usable without an enum, and what happens when two instances
share an ordinal but have different names, or the reverse?

**Without an enum,** a game writes objects or instances of a class:

```kotlin
object Feet : IFixtureType { override val ordinal = 0; override val name = "FEET" }
class NamedFixtureType(override val name: String, override val ordinal: Int) : IFixtureType
```

Nothing checks that the ordinals are unique, dense (`0..n-1`) or match the names. An enum gives
this by construction. A hand-written set does not.

**What a collision does, by place:**

| Place | Lookup | Same ordinal, different name | Same name, different ordinal |
|---|---|---|---|
| `Body.fixtures`, `MegaContactFilter` maps, `equalsAny`, `==` | `equals`/`hashCode` (identity for a plain class or object) | No overwrite: two distinct keys. A `data class` or a hand-written `equals` on the ordinal would merge them | No overwrite |
| `014c` rule table | `ordinal` as the slot index | **Shares a slot.** If both types get rules, the builder's duplicate check throws. If only one gets a rule, **the other type silently runs it**. This is the real danger | No overwrite. Two slots |
| Logs and the contact trace | `name` | Distinct names, correct | **Ambiguous** output |
| Data paths (`valueOf` from Tiled strings) | `name` | Correct | **Ambiguous**: one of them wins |
| An ordinal `>= n` or `< 0` | slot index | `ArrayIndexOutOfBoundsException` at dispatch | — |

**Fix: one engine-owned type set** that the table, the world and data lookups are built from:

```kotlin
class FixtureTypeSet private constructor(private val types: Array<IFixtureType>) {

    val size get() = types.size

    operator fun get(ordinal: Int) = types[ordinal]

    fun valueOf(name: String): IFixtureType = ...

    fun owns(type: IFixtureType) = type.ordinal in types.indices && types[type.ordinal] === type

    companion object {
        fun <E> of(entries: List<E>) where E : Enum<E>, E : IFixtureType = validated(entries)

        fun named(vararg names: String): FixtureTypeSet = ...

        fun validated(types: List<IFixtureType>): FixtureTypeSet {
            types.forEachIndexed { i, t -> check(t.ordinal == i) { "Fixture type ${t.name} has ordinal ${t.ordinal}, expected $i" } }
            check(types.map { it.name }.toSet().size == types.size) { "Fixture type names must be unique" }
            return FixtureTypeSet(types.toTypedArray())
        }
    }
}
```

- `named(...)` creates the instances and assigns the ordinals itself, so a game without enums cannot
  make a collision.
- `of(entries)` and `validated(list)` check a hand-written set: ordinals dense and in order, names
  unique.
- `ContactRuleTable` takes the `FixtureTypeSet`, not a raw list.
- A stray instance with a colliding ordinal is caught by `owns(type)`, which is one array read and
  one reference compare. Options for where to call it:
  - in the builder only (free, but misses strays created after the build);
  - in `Fixture`'s constructor, when a set is given (cheap; the check runs once per fixture);
  - in dispatch (one check per contact; probably not needed if the constructor checks).
- Rule: a fixture type is a singleton. Do not give it value equality (no `data class`).

With this, the interface stays usable without enums, and an enum still needs no code. This belongs
in `014a`.
