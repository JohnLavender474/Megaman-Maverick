# Notes: 020 Cheaper broad phase with stable contact order

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 19. Broad phase (sanity check)".

````text
(1) Current state
- In each sub-step, `cycle` clears the container and adds all bodies and active fixtures again (`WorldSystem.kt:169-187`). After the loop it rebuilds the container one more time (`:136-142`). At 2.5 sub-steps per frame, that is the "about 3.5x" rebuild.
- `Fixture.getShape()` (`Fixture.kt:51-65`) recomputes the shape on each call. It is called for each fixture insert, for each query fixture, and for each candidate in `collectContacts`.
- `Contact` equality does not depend on the order of the two fixtures (`Contact.kt:54-58`). So a pair found from both sides is de-duplicated by `currentContactSet.add`, and the first orientation found wins.

The proposal is sound. These things must be preserved:
- The callback order: `processContacts` goes through the `OrderedSet` in insertion order. That order comes from the body order (system insertion order), the fixture order, and the grid candidate order. "Test each pair once" must keep the same first-found orientation and the same order, or `MegaContactListener`'s first-match branches can change behavior. This can be checked with the golden replay.
- Begin, continue and end are computed for each sub-step.
- `BodyType.STATIC` does not mean "does not move". `Block` is STATIC (`Block.kt:255`), and its subclasses move (for example `AnimatedBlock.kt:93` sets the velocity). `Body.process` translates STATIC bodies too. A persistent static layer must use an explicit "immobile" flag or detect movement. It must not use the body type.
- A shape cache must be invalidated when the body moves. That happens in `process`, in `resolveCollisions`, and in the contact listener. The safe first step: cache only inside the `collectContacts` phase.

(2) Options
- (a) A persistent layer for immobile bodies, rebuilt on spawn, destroy or move.
- (b) Pair de-duplication with a stable order.
- (c) Shape cache for each phase.
- Do (c) first (lowest risk), then (a), then (b).

(3) Recommendation: as above. Gate each step on the replay or contract check.

(4) Open questions for the owner
- Is there a list of "moving static" blocks? Or may an `immobile` flag be added in Tiled?

(5) Proposed backlog wording

Title: "Cheaper broad phase with stable contact order"
- Cache fixture shapes inside the contact-collection phase.
- Keep immobile bodies in a persistent layer, decided by an immobile flag, not by `BodyType.STATIC`.
- Test each pair once, but keep the current callback order and orientation.

(6) Dependencies
- Item 9 (the sub-step count).
- Item 13 (the contact table makes the order rules explicit).
- The replay item and item 5 (verification).
````
