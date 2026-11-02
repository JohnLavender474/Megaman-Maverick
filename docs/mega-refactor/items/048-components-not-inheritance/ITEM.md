# 048: Replace entity inheritance with components

- Status: planned
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `005`, `015`, `017`, `018`, `023`, `029`, `030`, `041`, `042`, `050`, `065`, `084`, `087`, `112`, `116`
- Branches: none yet

## Goal

Replace the `AbstractHealthEntity` / `AbstractEnemy` / `AbstractBoss` / `AbstractProjectile` layers and the type checks on them with components (for example a health component). Each entity keeps its Kotlin class, which extends `MegaGameEntity` and gets its abilities from the components that it adds.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. The measurements are in `NOTES.md`.

Move the logic out first, and keep each base class as a thin shell until it is empty. Then each branch can ship alone.

1. Add the safety nets: `005`, and a snapshot of the components of each entity.
2. Move health, taking damage and dealing damage into components. `023` owns the damage amounts.
3. Move hit feedback and culling into parts that enemies and projectiles share.
4. Move enemy death and the helpers. Item drops go in `ItemDropComponent` and its system (see "Decisions").
5. Replace the enemy and boss identity checks with capability checks.
6. Move the boss flow and its readers (`MegaLevelScreen`, `Gate`, `IBossListener`).
7. Migrate the enemy and boss classes in batches.
8. Delete `AbstractHealthEntity`, `AbstractEnemy` and `AbstractBoss`.
9. In separate branches, do the projectile steps: move the hit hooks and `owner` into `ProjectileComponent`, replace the projectile type checks, migrate the 69 classes in batches, and delete `AbstractProjectile` and `IProjectileEntity`.

The 5 classes with mixed roles (`PreciousGem`, `DarkSerketClaw`, `GroundPebble`, `SealionBall`, `BulletBill`) migrate last.

Out of scope: `MegaGameEntity` stays, so `onSpawn` and `onDestroy` overrides stay. `EntityType` stays as a label and for the Tiled package lookup; only the checks on it change. `WilyFinalBoss` stays in `042`.

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
- (2026-11-02, owner) The per-enemy Kotlin classes stay for now. This item does not build enemies from definitions; that stays with `050`.
- (2026-11-02, owner) `AbstractProjectile` is in this item, as separate branches.

## Open questions

- Must this item depend on `005`? The proposed scope assumes yes.
- What is the order between this item and `023`? Both change `canBeDamagedBy` and the damage negotiators.
- Does the projectile branch move `owner` with no change in meaning, and does `030` then change the component? The proposed scope assumes yes.
- What is the merge target? The proposed scope assumes directly into `master`, because each branch can ship alone. The owner chooses before the first branch starts.
- What form replaces the OR gates, and what form replaces the 4 `invincible` constants? See `NOTES.md`, "Risks".
- What identifies an enemy after `AbstractEnemy` goes: a marker component or another capability? `addComponent` keys on the exact class, so a boss cannot use a subclass of an enemy component.

## Sources

- `NOTES.md` in this folder.
