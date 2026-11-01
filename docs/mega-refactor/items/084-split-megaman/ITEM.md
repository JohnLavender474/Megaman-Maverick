# 084: Split `Megaman` into player components

- Status: active
- Type: refactor
- Depends on: none
- Related: `017`, `023`, `024`, `025`, `029`, `030`, `042`, `048`, `051`, `053`, `058`, `059`, `060`, `081`
- Branches: `mega-refactor-084a-player-state` (merged 2026-10-30)
  `mega-refactor-084d-damage-reaction` (merged 2026-10-30)
  `mega-refactor-084c-movement` (merged 2026-10-30)
  `mega-refactor-084b-effects` (merged 2026-10-31)
  `mega-refactor-084e-class-line` (merged 2026-11-01)
  `mega-refactor-084f-focus-progress` (merged 2026-11-01)
  `mega-refactor-084g-movement-state` (merged 2026-11-01)

## Goal

Move the logic and state that was buried in `Megaman.kt` (990 lines and 16 interfaces at the start of this item) into components
and typed state, so that the player class is easy to maintain and a second playable character (`060` Proto Man, `081`
X) can use the same parts without a copy of `Megaman.kt` and without a player base class (`048`).

This item is a refactor only: Mega must play the same. It comes before `060` and `081`, so that those feature items do
not mix the split with new gameplay.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. The facts and line numbers are in `NOTES.md`.

- **Typed state in place of the property bag:** done in `084a` for `facing`, `aButtonTask`, `currentWeapon`, `running`
  and the teleport hooks. Still in the bag: 15 lines in `entities/megaman/`. 4 go to `084g`, 2 (`ConstKeys.FOCUS`) go to `084f`, and 9 are weapon
  lines that go to `024`.
- **Damage reaction as components:** done in `084d`.
- **Movement support as components:** done in `084c`.
- **Effects as components:** done in `084b`. The bubbles and the trail are components. The out-of-bounds rule is `isOutsideLevel`, and the
  orbs are `ExplosionOrb.spawnRing`, with no component.
- **The `AButtonTask` transitions:** done in `084c`, as `Megaman.updateAButtonTask()`, with no component.
- **Resets:** each new component resets its own state in `reset()`, so its lines leave `onSpawn`. Exception:
  `PostActionMomentumComponent.reset()` also runs in `onSpawn` until `087` is done, because behaviors can write momentum after the
  component reset.
- **Interfaces:** remove each interface on `Megaman` that has no reason left: `084e` (15 to 10) and `084f` (10 to 8). The rest goes to `048` and other owners
  (see `NOTES.md`, "Interfaces and a second playable entity").
- **Further branches (planned):** `084f-focus-progress`, `084g-movement-state` (after `031e` merges), `084h-charge`.

Not in this item (other items own these parts):

- The camera rotation in the `direction` setter: `058`.
- The heart-tank max health: `059`.
- The difficulty and enhancement changes in `editDamageFrom`: `023`.
- The owner walk in `canBeDamagedBy`: `030`.
- The weapon checks and the charge rules: `024`. The charge timer moves in `084h`.
- Level checks such as `MARIO_LEVEL` in `onHealthDepleted`: `025`.
- `AbstractHealthEntity` and the state-only contracts for all entities (`IFreezableEntity`, `IScalableGravityEntity`):
  `048`. This item can do the player side first, if `048` has not started.
- The values that make a character different (`CharacterDef`): `060`. This item moves logic, not numbers; Mega's
  numbers stay in `MegamanValues`.

## Owner's input

2026-10-30 (verbatim):

> The `Megaman` class is a catastrophe of a ton of interfaces and buried variables/funcs/logic. Ideally, a lot of
> these get moved over into components and props so that the Mega Man player class is easier to maintain and
> customize, and it fits into the grander vision of "multiple entities are playable, so don't dump too much game logic
> directly into the Megaman class".

2026-10-30, on the choice between a new item and a larger `048` scope (verbatim):

> Go with your recommendation of a new item so that gameplay and refactor don't become mixed into one item

## Decisions

- (2026-10-30) A new item, not a larger `048` scope and not part of `060`/`081`. Reason: keep the refactor apart
  from the gameplay features. `060` and `081` depend on this item.
- (2026-10-30, owner) Merge target: directly into `master`. Reason: each branch is a refactor that plays the same, so
  each one is safe to ship alone, and small merges keep the conflicts with `051b` in `Megaman.kt` small.
- (2026-10-30, owner) The first branch is `mega-refactor-084a-player-state`: typed state in place of the property bag
  and the teleport hook lambdas. Its `SCOPE.md` decides the final scope.
- (2026-10-30, owner) The next branch is `mega-refactor-084d-damage-reaction`: stun, knockback, recovery flash and freeze. It runs
  before the movement and effects branches, and it does not wait for `048`. Its `SCOPE.md` decides the final scope.
- (2026-10-30, owner) The thaw condition is passed into `FreezeComponent` as a rule, not written into it, so that
  mashing is the player's rule and other entities can use another one.
- (2026-10-30, owner) A type that has `fun reset()` extends `Resettable`.
- (2026-10-30, owner) No invincibility component in this item. The getter stays on `Megaman`; `048` owns the rule for
  all entities (see the `048` `ITEM.md`).
- (2026-10-30, owner) `AButtonTask` has no component, until `060` shows that Proto Man needs one.
- (2026-10-30, owner) `084c` also fixes the room-carry crash on a direction change, at the request of `051c`. This is the one
  gameplay change in this item so far (see the branch `docs/CONTRACT.md`, "Intended changes").
- (2026-10-30, owner) The trail and bubble timers survive a respawn, so these 2 components have no `reset()`.
- (2026-10-30, owner) The effects stay in this item, not in `042`.
- (2026-10-30, owner) `EXPLOSION_ORB_SPEED` moved to `ExplosionOrb` as `DEFAULT_EXPLOSION_ORB_SPEED`, and the spawn props allow an
  optional override.
- (2026-10-31, owner) All 8 decisions from the research in `NOTES.md`, "Interfaces and a second playable entity":
  - `isPlayer` replaces `IGateTrigger` and `ICartRider`.
  - `085` folds into `084f`.
  - The charge timer moves in `084h`; `024` keeps the charge rules.
  - `AButtonTask` stays with no component.
  - `IEventListener` stays on the class; `060` shares the handler body as one function.
  - The commented `regionProcessor` block in `Megaman.kt` goes, and its idea is in the `024` `NOTES.md`.
  - `048` owns `IFaceable`.
  - `084` closes after the agreed branches, and the rest of the interface work moves to `048`.

## Open questions

- Engine or core for each new component? Some are genre mechanisms (stun, knockback, momentum, room-transition carry);
  others are content (death orbs, trail sprites). `001` keeps Megaman Maverick content out of `engine/`.
- Is the stun bounce direction (the `TODO` that assumes `UP`) kept as is? A fix changes gameplay, so it is a bug in
  `docs/issues/`, not part of this item.

## Sources

- `NOTES.md` in this folder.
- `029` `NOTES.md`, "Review 1: the inside of the player" and "`CharacterDef` design".
- `063` `NOTES.md` (state-only contracts as components).
