# Scope: mega-refactor-014d-contact-receivers

What this branch changes and why. This was written ahead of time on `master`, before the branch
existed.

- Status: approved
- Approved: 2026-10-22

## Goal

Remove the player special cases from the contact rules and from `MegaCollisionHandler`: no
`is Megaman` checks and no `game.megaman` in those files, and no raw input in the collision
handler. The player-specific code moves into Megaman, which registers it through typed receivers
(`014b`) or implements a small capability interface. The rules then treat every entity the same
way. Gameplay does not change: each moved piece of code runs at the same point, with the same
inputs, as before.

## Item

`014` (table-driven contact and collision rules). From its `ITEM.md`:

> - Replace the 16 `Megaman` references with fixture receivers or capabilities.
> - `MegaCollisionHandler` reads body flags only (no `game.megaman`, no raw input).

The decisions of 2026-10-22 that apply (`ITEM.md`, "Decisions"):

- **Receivers:** replace the `Megaman` checks with fixture receivers or capabilities in this item.
  Do not wait for `029`. `029` adjusts them later as needed.
- **Receiver style:** typed receivers (`Receivers`, `setReceiver`, `receiver`) from `014b`.
- **No JSON rules.** On 2026-10-22 the owner chose to keep the three feet-surface rules as small
  separate rules, with no `SurfaceRule` base class.
- **Wall-slide ice friction:** a predicate receiver on the side fixture (owner, 2026-10-22; it
  replaces the earlier "body flag" decision, because a pushed flag can disagree with the behavior
  state).
- The `014c` split put this work in `014d`.

Research: `items/014-contact-rules-table/NOTES.md` ("Per-pair classes and sample code" has the
feet-block receiver and `IGateTrigger` samples).

## Current state

Scan of `master` at `46cbaac38` (after `014c`). Rule files are in
`core/.../world/contacts/rules/`.

**Player references in the rules** (14 sites):

| Rule | Site | What the player code does | Point in the rule |
|---|---|---|---|
| `FeetBlockRule` begin | `is Megaman` | `aButtonTask = JUMP`; land sound if the block makes one (reads `direction`, velocity, `canMakeLandSound`) | After the stick translate, before `FEET_ON_GROUND` and `block.hitByFeet` |
| `FeetBlockRule` continue | `is Megaman` | `aButtonTask = JUMP` | Same point |
| `FeetBlockRule` end | `is Megaman` | `aButtonTask` = `SWIM` or `AIR_DASH` if not `bodyOverGround`; `canMakeLandSound = true` if no feet block | After `FEET_ON_GROUND`, before `block.hitByFeet` |
| `WaterRule` begin | `is Megaman` | `aButtonTask = SWIM` (if not grounded and not wall sliding); `gravityScalar` = water value; zero the velocity on entry | At the end, after the splash |
| `WaterRule` continue | `is Megaman` | `aButtonTask = SWIM` (same condition) | At the end |
| `WaterRule` end | `is Megaman` | `aButtonTask = AIR_DASH`; `gravityScalar = 1` | Inside "no longer in water", after the splash |
| `FeetIceRule` continue | `is Megaman` | Skip the ice friction when grounded and pushing into a wall | Between the sense and the friction |
| `SideIceRule` begin and continue | `is Megaman` + `WALL_SLIDING` | Apply side ice friction while wall sliding, although the body property `ice_friction_y` is `false` for Megaman (`megaman/components/BodyComponent.kt:79`) | In the friction condition |
| `GateRule` begin | `is Megaman` | Only Megaman triggers a gate | First check |
| `FeetCartRule` begin and end | `is Megaman` | Only Megaman owns a cart; sets the `TOUCHING_CART` sense and the `CART` property (both never read: `docs/issues/2026-10-22-unused-contact-hooks.md`) | Whole body |
| `FeetSandRule` begin | `game.megaman.ready` | Spawn a sand splash only once Megaman is ready | After the friction |
| `PlayerItemRule` begin | `is Megaman` | `itemEntity.contactWithPlayer(playerEntity)`, typed `Megaman` in `ItemEntity` (9 files) | Whole body |
| `BodyPlayerRule` begin | `as Megaman` | `HIT_BY_PLAYER` receiver typed `(Megaman) -> Unit` (3 users) | Whole body |

**`MegaCollisionHandler`** (`core/.../world/collisions/MegaCollisionHandler.kt`):

- `game.megaman` (`:41`), `megaman.isBehaviorActive(CLIMBING)` (`:45, :53, :108`), and
  `game.controllerPoller.isJustPressed(UP)` (`:46`).
- Three checks: a `PRESS_UP_FALL_THRU` block lets a climbing Megaman through when UP is just
  pressed; `COLLIDE_DOWN_ONLY` and `COLLIDE_UP_ONLY` blocks let a climbing Megaman through.
- It runs in every sub-step, after the contacts. `isJustPressed(UP)` is a per-frame value.

**Surface rules:** `FeetIceRule`, `FeetSandRule`, `FeetSnowRule` and `SideIceRule` each set a sense,
read an opt-out body property and set `frictionOnSelf`. Their differences: ice sets only `x`; sand
also spawns a splash on begin; snow also calls the `HIT_BY_FEET` receiver; the feet-ice continue
has the player check between the sense and the friction; side ice sets a left or right sense.

## In scope

**Receivers and capabilities for the player code.** Each moves the code as it is into Megaman
(`megaman/components/BodyComponent.kt` or a new file next to it), called at the same point:

1. `FeetBlockRule`: a new phased-state receiver on the feet fixture,
   `Receivers.FEET_BLOCK_CONTACT: (state, block) -> Unit`, called where the `is Megaman` block was.
   Megaman registers the three phase bodies. (The existing `HIT_BY_BLOCK` runs later, after
   `block.hitByFeet`, so it cannot replace this.)
2. `WaterRule`: a new receiver on the water-listener fixture,
   `Receivers.WATER_CONTACT: (state, wasInWater) -> Unit`, called where the `is Megaman` blocks were
   (for end: only inside "no longer in water"). (The existing `HIT_BY_WATER` runs before the
   splash and only on begin.)
3. `FeetIceRule` continue: a new predicate receiver on the feet fixture,
   `Receivers.ICE_FRICTION_FILTER: () -> Boolean`, checked where the player check was. Megaman
   registers "not (grounded and pushing into a wall)".
4. `SideIceRule`: a new predicate receiver on the side fixtures,
   `Receivers.SIDE_ICE_FRICTION: () -> Boolean`, checked where the player check was. Megaman
   registers `{ isBehaviorActive(WALL_SLIDING) }`. The rule applies the friction if the body
   property allows it or the predicate returns `true`.
5. `GateRule`: a capability `IGateTrigger` (marker interface in core). Megaman implements it.
6. `FeetCartRule`: a capability `ICartRider` (marker). Megaman implements it. The rule keeps its
   writes of `TOUCHING_CART` and `CART` as they are (removing them is the unused-hooks issue, not
   this branch).
7. `FeetSandRule`: the rule takes a `() -> Boolean` "splashes on" supplier instead of the game.
   `megaContactRules` passes `{ game.megaman.ready }`. The rule no longer knows the game; the
   global read moves to the wiring, where `029` replaces it.

**`MegaCollisionHandler` reads body flags only.** Two typed body keys (engine `PropertyKey`, in a
core `BodyKeys` object), registered by Megaman on its body:

- `BodyKeys.PASS_THROUGH_ONE_WAY: () -> Boolean` = "is climbing" (replaces the `CLIMBING` checks at
  `:53` and `:108`).
- `BodyKeys.FALL_THROUGH_PRESS_UP: () -> Boolean` = "is climbing and UP was just pressed" (replaces
  `:44-47`).

The handler calls `dynamicBody.getProperty(KEY)?.invoke() == true` instead of comparing the body
with `game.megaman.body`. Only Megaman registers the keys, so only Megaman's body matches, as
before. The handler no longer takes the game. The input read moves into Megaman (item `031`
replaces it with an input frame later).

**Tests** (core, MockK fixtures and bodies; no game needed):

- `FeetBlockRule` calls `FEET_BLOCK_CONTACT` after the stick translate and before `FEET_ON_GROUND`
  and `block.hitByFeet`, with the right state.
- `WaterRule` calls `WATER_CONTACT` with the right `wasInWater`, after the splash; on end only when
  no water is left.
- `FeetIceRule` continue skips the friction when the filter returns `false`.
- `SideIceRule` applies the friction when the predicate returns `true` and the body property is
  `false`.
- `GateRule` triggers only for an `IGateTrigger`.
- `MegaCollisionHandler`: a body with `PASS_THROUGH_ONE_WAY` returning `true` passes a
  `COLLIDE_DOWN_ONLY` block; a body without it collides as before.
- The selection test still matches the reference file (the table is unchanged).

## Out of scope

- `PlayerItemRule` (`ItemEntity.contactWithPlayer(Megaman)`) and `BodyPlayerRule`
  (`HIT_BY_PLAYER` typed `Megaman`): these are the player's identity in a signature, not a special
  case. `029` re-types them to `IPlayer`. A `TODO` comment at each site points to `029`.
- A `SurfaceRule` base class: the three feet-surface rules stay small and separate.
- Removing the unused `TOUCHING_CART` sense and `CART` property (filed issue).
- The filter, the dead rules, the broad phase.
- Any change to what Megaman does: the moved code is the same code.

## Affected areas and risks

- **Files:** the rule files above, `MegaContactRules.kt`, `Receivers.kt`, a new `BodyKeys.kt`, two
  marker interfaces, `MegaCollisionHandler.kt`, `MegamanMaverickGame.kt` (handler wiring),
  `Megaman.kt` (implements the two markers), `megaman/components/BodyComponent.kt` (registrations),
  new tests.
- **Risk: a different call point.** Each receiver is called exactly where the old `is Megaman` block
  was. The tests pin the order for the feet-block and water receivers.
- **Risk: closure values.** The moved code reads Megaman's state (velocity, direction, behaviors,
  `canMakeLandSound`) when the receiver runs, as the old code did. Nothing is captured at
  registration time.
- **Risk: another entity becomes a "player".** Only Megaman registers the new receivers and keys and
  implements the markers. A grep confirms it.
- **Risk: input timing.** "UP just pressed" is read inside the predicate, in the same sub-step as
  before.

## Overlap with other branches

- No `mega-refactor-` branch is active now.
- `029` (`IPlayer`) re-types the receivers and capabilities later; this branch keeps them small so
  that `029` only renames or widens them.
- `031` (`InputFrame`) replaces the input read inside Megaman's predicate.

## Open questions

None open.

- (2026-10-22) Owner's decisions: side ice friction uses the predicate receiver; `PlayerItemRule`
  and `BodyPlayerRule` wait for `029`, each with a `TODO` comment; the three feet-surface rules stay
  separate (no `SurfaceRule`).
