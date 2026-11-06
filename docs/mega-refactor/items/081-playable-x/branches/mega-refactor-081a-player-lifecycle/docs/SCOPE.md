# Scope: mega-refactor-081a-player-lifecycle

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: draft
- Approved: no

## Goal

Move the player lifecycle rules out of `Megaman` into shared functions that a second player class can call: the spawn
and death work, the room-transition handling, the damage rules and the out-of-bounds kill. `Megaman` calls the shared
functions in place of its own code. Megaman plays the same. The branch adds no X code.

## Item

`081`, "Playable X for the extra levels, with `CharacterDef`, ported from `mmx-x6`". From `ITEM.md`, "Branch split"
(owner, 2026-11-05):

> | 0 | `081a` (`mega-refactor-081a-player-lifecycle`) | Refactor only, with the contract "Megaman plays the same", and
> no X code. Move the player lifecycle out of `Megaman` into shared functions or components that a second player class
> can call. See below. |

And "`081a`: the shared player lifecycle":

> - The spawn and destroy work: the `PLAYER_SPAWN` submit (the level reset), the `PLAYER_JUST_DIED` submit, and the
>   world gravity set from the spawn data.
> - The event handling: the room-transition events, the charge stop at `GATE_INIT_OPENING`, and `STUN_PLAYER`.
> - The out-of-bounds kill call (`084b` moved only the test).
> - The invincibility rule, `canMove`, and the behaviors lock hook.
> - The `takeDamageFrom` tail (knockback, freeze, `players.onPlayerDamaged`) and `editDamageFrom`.
> - The damage negotiator takes a player entity in place of `Megaman`.
>
> Each part that another item owns gets a `TODO` that points to that item, and the move does not change its behavior:
> `editDamageFrom` (`023`), the camera turn in the `direction` setter (`058`), the heart-tank max health (`059`), the
> owner walk in `canBeDamagedBy` (`030`), `PLAYER_SPAWN` as a level reset (`038`), `PLAYER_JUST_DIED` (`119`), and
> `AbstractHealthEntity` (`048`).

Owner decisions (2026-11-05): `081a` is a refactor only, and each `081` branch squash-merges directly into `master`.

## Current state

Scan of `master` at `444681c2d`. `Megaman.kt` has 535 lines. `084` moved most mechanisms into components and free
functions in `entities/state/` (for example `RoomTransitionCarries.kt`, `Stuns.kt`, `Knockbacks.kt`, `Freezes.kt`,
`PostActionMomentum.kt`). Those functions take any entity. The code that calls them in the right order, at the right
point of the life cycle, is still in `Megaman`.

### Lifecycle code in `Megaman`

| Part | Where | Shared or Megaman's own |
|---|---|---|
| Rule `invincible` (damaged, recovering, `DAMAGE` lock, not live, not in play) | `Megaman.kt:87-89`, `TODO` for `081` | shared |
| Rule `canMove` (`MOVEMENT` lock, stunned, damaged, frozen) | `:92-94`, `TODO` for `081` | shared |
| `onSpawn`: event listener add, place on the bottom center of the spawn bounds, velocity zero, collision and gravity on, fixtures active, `facing` from the spawn data, `services.gravity.set` from the spawn data | `:318-331` | shared |
| `onSpawn`: `aButtonTask`, weapons and ammo, scalars, timers, momentum reset, air dash | `:333-358` | Megaman's own |
| `onSpawn`: health to max, `MOVEMENT` and `DAMAGE` locks cleared, teleporter cleared, smooth follow stopped | `:335`, `:340-342`, `:360` | shared |
| `onSpawn`: `PLAYER_SPAWN` for slot 0 only, `TODO` for `038` | `:362-365` | shared |
| `onDestroy`: listener removed, `PLAYER_JUST_DIED` | `:372-374` | shared |
| `onDestroy`: the charge sound stops | `:376` | Megaman's own (his buster sound) |
| `onHealthDepleted`: orb ring; the `MARIO_LEVEL` sound | `:383-386` | Megaman's own (1 call; the level check is `025`) |
| `onEvent` `BEGIN_ROOM_TRANS`, `CONTINUE_ROOM_TRANS`: center on the path, carry, gravity off, momentum stop | `:391-403` | shared |
| `onEvent` `END_ROOM_TRANS`: carry end, gravity on unless climbing, momentum stop | `:407-415` | shared |
| `onEvent`: the charge sound stops (room transition, `GATE_INIT_OPENING`) | `:404`, `:417-421` | Megaman's own |
| `onEvent` `STUN_PLAYER` | `:423`, already `onStunPlayerEvent` (`Stuns.kt`) | already shared |
| `onEvent` `END_GAME_CAM_ROTATION` | `:425-429` | Megaman's own (`058`) |
| The `direction` setter: camera turn, `MOVEMENT` lock, signed values | `:130-194` | Megaman's own (`058`, `081` `CharacterDef`) |
| `canBeDamagedBy`: not live, and the friendly-fire rule through `rootPlayer` | `:433-437` | shared |
| `takeDamageFrom` tail: knockback, freeze, `players.onPlayerDamaged` | `:449-456` | shared |
| `takeDamageFrom` tail: the charge sound stops, the damage sound | `:453-454` | Megaman's own |
| `editDamageFrom`: damage-increase enhancement, hard-mode +1 | `:461-473` | shared, `TODO` for `023` |
| The updatable: out-of-bounds kill (`isOutsideLevel`, 1.5 tiles, `destroy()`) | `:79-80`, `:487-490` | shared |
| `LocksComponent(..., behaviorsLockHook(this))` | `:268` | already shared (`locks/BehaviorsLockHook.kt`) |

### `MegamanDamageNegotiator`

`MegamanDamageNegotiator(private val megaman: Megaman)` (`MegamanDamageNegotiator.kt:22`) reads only
`megaman.services` and `owner == megaman`. A second player class cannot use it, because the type is `Megaman`. The
`x6-intro-extra-level` branch adds 7 entries to its table (+14 lines).

### Order inside `onSpawn`

`services.gravity.set` (`:329`) sets the `direction` of every `IDirectional` player (`WorldGravity.kt:12-15`). The
`Megaman` setter can start a camera turn and lock `MOVEMENT` (`CAMERA_ROTATION`, `:154`). The lock clear at `:340`
comes after it. A move of the shared lines must keep this order: the gravity set before the lock clear.

### Tests

`core/src/test/.../players/SecondPlayerEntity.kt` is a test player that is not `Megaman`. `testServices()` builds a
real `MegaServices`. `Megaman` tests exist (`MegamanFocusTest`, `MegamanRunTest`, `MegamanAirDashDirectionTest`).

## In scope

1. New shared functions, in `players/` (core). The names are proposals; the plan fixes them.
   - `playerInvincible` and `playerCanMove`: the 2 rules, as free functions that compute the value at each read, with
     the same terms in the same order. They read the components with `require(...)`, not with the queries that return
     a silent default (for example `inPlay`, `isLocked`, `stunned`), so a player class that lacks a part fails fast.
     The receiver gives `damaged` at compile time (an `AbstractHealthEntity`, with a `TODO` for `048`). `Megaman`
     keeps its 2 members, and each member calls 1 function. The 2 `TODO`s at `Megaman.kt:87` and `:92` go.
     `playerCanMove` gets a new `TODO`: stun, freeze and damage as `MOVEMENT` lock reasons (item `126`). See
     `NOTES.md`, "Decisions", for the forms that were not chosen.
   - A spawn function: the shared `onSpawn` lines in the same relative order, including the `PLAYER_SPAWN` submit for
     slot 0 with its `TODO` for `038`.
   - A destroy function: the listener removal and `PLAYER_JUST_DIED`, with a `TODO` for `119`.
   - A room-transition function for the 3 events, without the charge sound.
   - A damage-permission function: not live, and the friendly-fire rule, with the `TODO` for `030`.
   - A damage-tail function: knockback, freeze, `players.onPlayerDamaged`.
   - A damage-edit function: the enhancement and the hard-mode +1, with a `TODO` for `023`.
   - An out-of-bounds kill function with the 1.5-tile offsets.
2. `MegamanDamageNegotiator` becomes `PlayerDamageNegotiator` (owner, 2026-11-05) and takes a `MegaGameEntity` (the
   player) in place of `Megaman`. Its table applies to every player. A player class can give direct overrides that
   the negotiator reads before the shared table (owner, 2026-11-05). See open question 2 for when the overrides
   parameter comes.
3. `Megaman` calls the shared functions. Its own lines stay in `Megaman`: the charge sound, the damage sound, the
   `aButtonTask`, the weapons, the scalars, the timers, the `direction` setter, `END_GAME_CAM_ROTATION` and the
   `MARIO_LEVEL` sound.
4. Tests for the shared functions, on `SecondPlayerEntity` or `Megaman`: the 2 rules, the hard-mode +1, the
   out-of-bounds kill, the room-transition position, and `PLAYER_SPAWN` only for slot 0.
5. `players/README.md`: how a player class uses the shared functions, and in which life-cycle hook. The package has no
   README today, and the design spans several functions with an order.

### New APIs

Each shared function has `Megaman` as its first production caller in this branch. The second caller is X in `081b`.

| API | First caller |
|---|---|
| `playerInvincible`, `playerCanMove` | `Megaman.invincible`, `Megaman.canMove` |
| `PlayerDamageNegotiator` (renamed) | `Megaman.damageNegotiator` |
| spawn function | `Megaman.onSpawn` |
| destroy function | `Megaman.onDestroy` |
| room-transition function | `Megaman.onEvent` |
| damage-permission function | `Megaman.canBeDamagedBy` |
| damage-tail function | `Megaman.takeDamageFrom` |
| damage-edit function | `Megaman.editDamageFrom` |
| out-of-bounds kill function | the `Megaman` updatable |

### Intended changes

None.

## Out of scope

- X: the class, the `characters` field, the slot swap, the focus at level start, the body def (`081b`).
- The `direction` setter, the camera turn and `END_GAME_CAM_ROTATION` (`058`). The signed values (`081`, `CharacterDef`).
- The weapon state and the 9 `as Megaman` casts in the screens (`024`, `081b`).
- The heart-tank max health and the `gameState` listener (`059`, `081b`).
- `PLAYER_SPAWN` as a level reset (`038`) and the meaning of `PLAYER_JUST_DIED` (`119`). The move keeps both events as
  they are.
- `AbstractHealthEntity` and the class line of `Megaman` (`048`).
- The `MARIO_LEVEL` check in `onHealthDepleted` (`025`).
- Locks for stun, freeze and damage (`canMove` as `!isLocked(MOVEMENT)` only), and `invincible` as a `DAMAGE` lock
  only. See `NOTES.md`, "Decisions".
- An invincibility component for all entities (`048`).

## Affected areas and risks

Files: `Megaman.kt`, `MegamanDamageNegotiator.kt`, 1 or 2 new files in `players/`, `players/README.md`, and new tests.

| Risk | Result if wrong | Control |
|---|---|---|
| The order inside `onSpawn` changes | A spawn in a turned-gravity room keeps the `CAMERA_ROTATION` lock, and Megaman cannot move | The shared spawn function keeps the gravity set before the lock clear; Megaman's own lines move only where they read no state that the shared lines write. The plan lists the order. Play a turned-gravity room |
| The order inside `takeDamageFrom` changes | A boss that reacts to the damage signal sees a different state | Only the sounds move relative to the signal; the signal listeners do not read Megaman's sounds. The plan checks each listener |
| A shared function requires a component that a later X lacks | A crash in `081b` | This is the fail-fast rule; the README lists the components that each function requires |
| The negotiator rename conflicts with `x6-intro-extra-level` | A merge conflict in that branch, which adds 7 table entries | That branch takes the rename when it next merges `master`; its 7 entries move to the renamed file. The merge docs name the file |
| A function exists only for a later caller (`O1-R5`) | A function with the wrong shape for X | Each function moves code that `Megaman` runs today; `081b` is the second caller, and changes the shape if X needs it |

## Overlap with other branches

Worktrees at `444681c2d`:

- `mega-refactor-118i-boss-phase` adds `entities/state/BossComponent.kt`. It does not touch player code.
- `x6-intro-extra-level` changes `MegamanDamageNegotiator.kt` (+14 lines: imports and 7 table entries). See the risk
  table.
- `compactor-level` does not touch player code.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` | helps | Shared logic as functions, not a player base class |
| `O1-R3` | no effect | No new listener and no new event; the order inside `onSpawn` stays |
| `O1-R4` | no effect | No new property key |
| `O1-R5` | holds | Each new function has `Megaman` as its caller in this branch |
| `O2-R5` | no effect | The slot-0 rule for `PLAYER_SPAWN` stays, with its `TODO` for `038` |
| `O2-R9` | no effect | The events keep their slot |
| `O5-R1` | no effect | No new allocation per frame |
| `O6-R2` | helps | New tests name the shared rules |
| `O7-R3` | no effect | The `MARIO_LEVEL` check stays where it is (`025`) |
| `O8-R1` | helps | A second character can use the lifecycle with no copy of `Megaman.kt` |
| `O8-R2` | no effect | No character check is added |

## Open questions

All closed by the owner on 2026-11-06 ("Go with your recommendations"):

1. Closed: the new functions go in `players/`.
2. Closed: `081a` renames the class to `PlayerDamageNegotiator` and keeps one table. `081b` adds the overrides
   parameter, with X as its first caller (`O1-R5`).
3. Closed: this branch writes `players/README.md`.
4. Closed: stun, freeze and damage do not become lock reasons in this branch. `playerCanMove` gets a `TODO` that
   points to item `126`.
