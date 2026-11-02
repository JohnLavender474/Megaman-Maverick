# 048: Replace entity inheritance with components

- Status: planned
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `017`, `018`, `023`, `029`, `030`, `084`
- Branches: none yet

## Goal

Replace the `AbstractHealthEntity` / `AbstractEnemy` / `AbstractBoss` layers and the type checks on them with components (for example a health component).

## Owner's input

From the first request (summary): entities get health by extending `AbstractHealthEntity`; enemies must extend `AbstractEnemy` and bosses `AbstractBoss`; logic does type checks on these classes. Replace the layers with components and remove the type checks.

## Decisions

- (2026-10-22) Entity roles come from components, not from base classes or layers. An entity that is both an enemy and a block has both parts (a solid block fixture, health and damage), with no child entity, extra interface or flag swapping. Code that checks `EntityType` or `is AbstractEnemy` checks a capability instead.
- (2026-10-23, from `029`) The player contract follows this direction: no `IPlayer` interface on the player entity; the game's player view resolves required components at registration, and optional parts are capability lookups (`getComponent(X::class)`; a `ComponentKey<T>` type was added in `029a` and removed on 2026-10-23, because `KClass` is already a typed key). When this item turns facing, direction, health or the lifecycle into components, only the player view's accessors change, not the call sites that `029` moves.
- (2026-10-30) The player side of the split (the logic and state in `Megaman.kt`) is item `084`, not this item.
  This item keeps the base classes and the state-only contracts for all entities; see `084` "Open questions" for
  the order between the two.
- (2026-10-30, owner, from `084`) Invincibility becomes a read through a component with one constructor hook, for all
  18 `invincible` getters, in one pass. It is never stored as a boolean: the timers end with no write, and a contact
  later in the same contact pass must see a hit at once, so a stored or per-frame value can be stale. `084d` keeps the
  getter on `Megaman`. Facts and options: `084` `NOTES.md`, "Invincibility research". The final form waits for the
  scan of this item, including the 4 getters that are constants and replace the base rule.
- (2026-10-31, owner, from `084`) This item owns `IFaceable`, not `063`. It also takes the interfaces that stay on `Megaman` after `084`: see `084` `NOTES.md`,
  "Who owns each interface".
- (2026-11-02, from `fix/quicksand-two-players`) The target for item drops is an `ItemDropComponent` and a system that decides the drop in `onEntityLeft`, which runs before the component reset, so body senses are still live. This replaces the `dropItemOnDeath` var and the drop runnable in `AbstractEnemy`, and it fixes `docs/issues/2026-11-02-enemy-drop-block-check-never-true.md`. The sand drop rule in `AbstractEnemy` has a `TODO` that links this item and `116`.

## Open questions

None yet. The scan for the first branch will find them.

## Sources

- `NOTES.md` in this folder.
