# 084: Split `Megaman` into player components

- Status: active
- Type: refactor
- Depends on: none
- Related: `017`, `023`, `024`, `025`, `029`, `030`, `042`, `048`, `051`, `053`, `058`, `059`, `060`, `081`
- Branches: `mega-refactor-084a-player-state` (started 2026-10-30)

## Goal

Move the logic and state that is buried in `Megaman.kt` (990 lines, 17 interfaces on the class line) into components
and typed state, so that the player class is easy to maintain and a second playable character (`060` Proto Man, `081`
X) can use the same parts without a copy of `Megaman.kt` and without a player base class (`048`).

This item is a refactor only: Mega must play the same. It comes before `060` and `081`, so that those feature items do
not mix the split with new gameplay.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. The facts and line numbers are in `NOTES.md`.

- **Typed state in place of the property bag:** `facing`, `aButtonTask`, `currentWeapon` and `running` are read and
  written through `getProperty`/`putProperty` (`MegamanProps`, `ConstKeys.RUNNING`). The teleport hooks are lambdas
  stored as properties (`ConstKeys.ON_TELEPORT_START`, `ON_TELEPORT_END`). Give them typed fields or components.
- **Damage reaction as components:** the knockback per direction and `noDmgBounce`, the recovery flash, the stun
  (`STUN_PLAYER`, `stunTimer`), and the freeze with mash-to-unfreeze (`frozen`, `hitsToUnfreeze`, `frozenPushTimer`,
  the ice shards).
- **Movement support as components:** post-action momentum (`preserveMomentum`, `stopMomentum`,
  `killPostActionMomentum`, the decay in the updatables), and the room-transition velocity carry (`onEvent`,
  `BEGIN/CONTINUE/END_ROOM_TRANS`).
- **Effects as components:** trail sprites, underwater bubbles, the death explosion orbs, and the out-of-bounds kill.
- **The `AButtonTask` transitions** that run in the updatables, next to the ones in the behaviors.
- **Resets:** each new component resets its own state in `reset()`, so its lines leave `onSpawn`.
- **Interfaces:** remove each interface on `Megaman` that has no reason left after the steps above.

Not in this item (other items own these parts):

- The camera rotation in the `direction` setter: `058`.
- The heart-tank max health: `059`.
- The difficulty and enhancement changes in `editDamageFrom`: `023`.
- The owner walk in `canBeDamagedBy`: `030`.
- The weapon charge state and the weapon checks: `024` (see "Open questions").
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

## Open questions

- Engine or core for each new component? Some are genre mechanisms (stun, knockback, momentum, room-transition carry);
  others are content (death orbs, trail sprites). `001` keeps Megaman Maverick content out of `engine/`.
- Does the charge state (`chargingTimer`, `chargeStatus`, `shoot()`) move here or wait for `024`?
- Do the effects (trail sprites, bubbles) belong here or in `042` (reusable components)?
- Order against `048`: does this item make the player-side health and freeze components that `048` later takes for
  all entities, or does it wait for `048`?
- Is the stun bounce direction (the `TODO` that assumes `UP`) kept as is? A fix changes gameplay, so it is a bug in
  `docs/issues/`, not part of this item.

## Sources

- `NOTES.md` in this folder.
- `029` `NOTES.md`, "Review 1: the inside of the player" and "`CharacterDef` design".
- `063` `NOTES.md` (state-only contracts as components).
