# Notes: 045 Clear ownership rules and debug checks for `GameObjectPools`

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 44. Safety of `GameObjectPools`".

````text
(1) Current state
- `GameObjectPools` (`core/.../utils/GameObjectPools.kt`) is a generational arena. `fetch(reclaim = true)` puts the object in the newest bucket. `performNextReclaim()` runs once at the end of each frame (`MegamanMaverickGame.kt:586`) and frees the oldest of 3 buckets.
- So an object fetched in frame N is freed at the end of frame N+2.
- There are 343 `fetch` calls in 172 files, about 51 non-reclaim fetches (`(false)` variants), and 18 `GameObjectPools.free` sites.
- The common manual pattern: in `onSpawn`, convert the `RectangleMapObject`s from the spawn props into non-reclaim pooled objects, then free them in `onDestroy`. Examples: `RodentMan.kt:289/346`, `DrillHead.kt:192/216`, `Laser.kt:210/253`, `PreciousBlock.kt:75/98`. These are the scattered `.free()` calls the owner wants to avoid.

Hazards:
- An object that is stored in a field, or that sits in a queue for longer than 2 frames, is silently reused. Example: spawn props hold a scratch object while they wait in a spawn queue.
- `free()` on an object that was fetched with reclaim on is a double free that the pool cannot detect. The pool rejects a double free only while the object is in the free queue. So after a manual free and a new fetch by someone else, the reclaim frees the object again while the second owner still uses it.
- Scratch `Camera.toGameRectangle()` calls are hidden inside default parameters (`Extensions.kt:112`), so callers do not see that they get a pooled object.

(2) Options
- (a) Manual free everywhere. The owner already rejects it. Error-prone.
- (b) Scoped borrow: `inline fun <T> borrow(cls, block: (T) -> R): R`. It frees at block end, and inline means no lambda allocation. Pros: exact lifetime, no free calls. Cons: only for local scratch use.
- (c) An explicit tick arena: keep today's mechanism, but define the contract. "A scratch object is valid until the end of the current tick plus one, and must never be stored." Pros: most existing code already follows this. Cons: it needs detection.
- (d) An ownership rule for stored data: "spawn props never transfer ownership". The creator owns their content: map-built props live as long as the level; runtime props are scratch. The entity copies values into its own preallocated fields (`bounds.set(props.bounds)`). For lists (hover spots, obstacles), the entity uses an owned, growable list that reuses its own `Vector2`/`GameRectangle` instances (`ensureSize(n)` and `set`), so no pool and no free is needed. Entities are pooled, so this allocates once for each instance.
- (e) Debug detection, enabled by a flag:
  - On reclaim, poison the object: set it to NaN. Then any use-after-reclaim shows NaN positions at once.
  - Keep a debug identity map from object to fetch generation.
  - `free()` asserts that the object was fetched with `reclaim = false`.
  - An optional "stored scratch" check: in debug mode, `Properties.put` and the entity fields helper reject objects whose generation is still active.

(3) Recommendation
- Combine (c), (d) and (e), and use (b) for hot local code.
- The rule set: scratch by default and never stored; stored data is copied into owned fields or owned lists; spawn props never pass ownership.
- This removes most of the 18 `free()` sites and makes violations visible in debug runs.
- Keep the 3-bucket lag (or tie it to ticks, item 9). The entries of the spawn queue must be consumed within that window. With one spawn phase (item 18) this is guaranteed.

(4) Open questions for the owner
- Is a debug-only NaN poison acceptable? (It changes behavior only in debug builds.)
- Must the arena run on ticks or on frames once item 9 lands? (Ticks are safer.)

(5) Proposed backlog wording

Title: "Clear ownership rules and debug checks for `GameObjectPools`"
- Write the contract: scratch objects live until a defined reclaim point and are never stored. Spawn props never pass ownership.
- Add owned value fields and owned reusable lists for entities. Remove the `onSpawn` fetch / `onDestroy` free pairs.
- Add an inline `borrow {}` for local scratch use.
- Add debug checks: poison on reclaim, track the generation, assert on `free()` of a reclaimable object.
- Make hidden pooled default parameters explicit (for example `Camera.toGameRectangle()`).

(6) Dependencies
- Item 9 (reclaim on ticks).
- Item 18 (bounded spawn queue lifetime).
- Item 21 (typed spawn props, which copy into typed fields).
- Item 8 (to measure the effect on GC).
````
