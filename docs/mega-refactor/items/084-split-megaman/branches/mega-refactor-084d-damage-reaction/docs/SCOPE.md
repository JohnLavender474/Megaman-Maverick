# Scope: mega-refactor-084d-damage-reaction

- Status: approved
- Approved: 2026-10-30

## Goal

Move stun, knockback, the recovery flash and freeze out of `Megaman.kt` into components, one per mechanism, so that a
second character can use them without a copy of `Megaman.kt`. Mega plays the same.

## Item

From the 084 `ITEM.md`, "Proposed scope":

> **Damage reaction as components:** the knockback per direction and `noDmgBounce`, the recovery flash, the stun
> (`STUN_PLAYER`, `stunTimer`), and the freeze with mash-to-unfreeze (`frozen`, `hitsToUnfreeze`, `frozenPushTimer`,
> the ice shards).
>
> **Resets:** each new component resets its own state in `reset()`, so its lines leave `onSpawn`.
>
> **Interfaces:** remove each interface on `Megaman` that has no reason left after the steps above.

Owner decisions that apply (084 `ITEM.md`, "Decisions"): the thaw rule is passed into `FreezeComponent`; a type with
`fun reset()` extends `Resettable`; no invincibility component; this branch does not wait for `048`.

## Current state

Scan on 2026-10-30, branch at `2aff79ae5`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`,
`M/` = `C/entities/megaman/`. Line numbers are in `M/Megaman.kt` unless the line names a file. Research: 084 `NOTES.md`,
"Design research" and "Invincibility research".

| Mechanism | State | Logic | Readers outside `Megaman.kt` |
|---|---|---|---|
| Stun | `stunTimer` (`:129`), `stunned` (`:115`) | Event `:597-635`; update `:744` | `ControllerComponent.kt:57,73,80`, `MegamanAnimationKeySupplier.kt:41`, `MegamanStunTargetTest` |
| Knockback | `noDmgBounce` (`:134`) | `:664-689`, in `takeDamageFrom` | None |
| Recovery flash | `damageRecoveryTimer`, `damageFlashTimer`, `recoveryFlash` (`:130-132`) | `:745`, `:748-756` | `SpritesComponent.kt:130` |
| Freeze | `frozen`, `hitsToUnfreeze`, `frozenPushTimer` (`:122-124`) | `:691-695`, `:807-827` | `BodyComponent.kt:374,378`, `SpritesComponent.kt:101`, `MegamanAnimationKeySupplier.kt:39`, `MegamanWeaponsHandler.kt:585` |

- Shared reads in `Megaman`: `invincible` (`:118`) and `canMove` (`:126`). `:746` resets the charge timer when stunned or
  damaged. `damageTimer` is `protected` in `AbstractHealthEntity`; `damaged` is public.
- `onSpawn` resets this state at `:472`, `:475-477`, `:483`, `:485-486`. `:484` is `damageTimer` and stays.
- The bosses `RodentMan`, `PreciousWoman` and `BigAssMaverickRobot` have their own private `stunned`. They do not change.
- `IFreezableEntity` on `Megaman` has no user: its only type uses are the `FreezableEntityHandler<T>` bound and
  `ShieldEntity.owner`, and `Megaman` uses neither. There is no `is IFreezableEntity` check.
- `IceShard.spawn5(center)` passes the defaults of `IceShard.onSpawn`, so it equals the loop at `:822-825`.
- `MegamanStunTargetTest` sets `stunTimer` directly and calls `onEvent` with no `init()`.

### Frame-order constraints

- The base lambda runs first, so `damageTimer.isJustFinished()` is valid in `Megaman`'s lambda in that frame.
- `:744` must run before `:746`. `:745` must run before `:748`. It may move after `:746`, because `:745` writes only the
  recovery timer and `:746` reads `stunned` and `damaged` and writes only `chargingTimer`.
- `:756` must stay outside the `:748` guard.
- The thaw must stay in `UpdatablesSystem`, because the animation and sprite systems read `frozen` in the same frame.
- `STUN_PLAYER` arrives in `events.run()`. `Megaman` stays the listener, because listener order is registration order.
- In `takeDamageFrom`: knockback, then freeze, then the sounds, as now.

## In scope

All new types go in `C/entities/state/`, and they do not import `Megaman`, `MegamanValues` or a `SoundAsset`.

1. **`StunComponent(duration, onSurface, onStun)`**: the timer, `stunned`, `start()`, `update(delta, grounded)`.
   `Stuns.kt`: `IGameEntity.stunned` and `IGameEntity.onStunPlayerEvent(event)`, which keeps the 3 checks at `:600-625`
   in order. `onStun` holds the impulse at `:629-632` with its `TODO`, unchanged.
2. **`KnockbackComponent(x, y, exempt)`**: values only, with no state and no `reset()` of its own. `Knockbacks.kt`:
   `IBodyEntity.knockBackFrom(damager)`, with the 4 arms of `:667-688`. `x` and `y` are scaled by `ConstVals.PPM` in the
   `define` function.
3. **`DamageRecoveryComponent(duration, flashInterval)`**: `recovering`, `flash`, and
   `update(delta, damaged, damageJustFinished)`, which holds `:745` and `:748-756` as one call, after `:746`.
4. **`FreezeComponent(thawRule, onThaw)`**, **`IThawRule : Resettable`** and **`MashThawRule(mashesToThaw, mashCooldown,
   mashed, onMash)`**. `Freezes.kt`: `IGameEntity.frozen` and `IGameEntity.freezeFrom(damager)`. `onThaw` calls
   `IceShard.spawn5(body.getCenter())`.
5. **Wiring:** `M/components/DamageReactionComponents.kt` has the 4 `define<X>Component()` functions. Only these read
   `MegamanValues`, the sound and the input. `Megaman.init()` adds the components.
6. **`Megaman`:**
   - `private`/`internal` field `damageRecovery`, set in the class body with `defineDamageRecoveryComponent()` and added in
     `init()`. `invincible` reads `damageRecovery.recovering`, so a read keeps today's 2 map lookups. `SpritesComponent`
     reads `damageRecovery.flash`.
   - The lambda keeps one call per part. `invincible` keeps all its terms in the same order. `canMove` keeps its text.
   - The state, the `onSpawn` lines above and `IFreezableEntity` leave the class. `:471 clearLocks(DAMAGE)` stays.
7. **Tests:**
   - One test class per stateful component: a new instance equals the state after `reset()`, and the hooks stay.
   - Stun counts down only on the ground; a second stun is ignored. Mash cooldown and thaw count. Recovery starts on the
     frame that damage ends, and `flash` is `false` at the end.
   - `invincible` with `testServices()`: true from the hit to the end of recovery, with no false frame between `damaged`
     and `recovering`; false while stunned or frozen.
   - `MegamanStunTargetTest` uses `defineStunComponent()`.
   - A temporary suite with a frozen copy of the old logic compares the old and new code frame by frame. `CONTRACT.md`
     lists it.

### New API and its first caller

| API | First caller |
|---|---|
| `StunComponent`, `Megaman.defineStunComponent` | `Megaman.init()` |
| `IGameEntity.stunned` | `canMove`, `ControllerComponent:57`, `MegamanAnimationKeySupplier:41` |
| `onStunPlayerEvent` | `Megaman.onEvent` |
| `KnockbackComponent`, `Megaman.defineKnockbackComponent` | `Megaman.init()` |
| `knockBackFrom` | `Megaman.takeDamageFrom` |
| `DamageRecoveryComponent`, `Megaman.defineDamageRecoveryComponent` | `Megaman` field, `invincible`, the lambda |
| `FreezeComponent`, `IThawRule`, `MashThawRule`, `Megaman.defineFreezeComponent` | `Megaman.init()`, the lambda |
| `IGameEntity.frozen` | `canMove`, `BodyComponent:374`, `SpritesComponent:101`, `MegamanAnimationKeySupplier:39`, `MegamanWeaponsHandler:585` |
| `freezeFrom` | `Megaman.takeDamageFrom` |

`IThawRule` has one implementation in this branch, by owner decision. `TimedThawRule` and a public `thaw()` are for
`048`; this branch does not add them. A `recovering` or `recoveryFlash` extension has no caller here, so this branch does
not add them.

## Out of scope

- `invincible` as a component, and any stored or cached value of it (`048`).
- `AbstractHealthEntity`, `damageTimer`, `invinciblePredicates`, `FreezableEntityHandler`, `IFreezableEntity` itself
  (`048`). The freeze of enemies does not change.
- The charge reset at `:746`: it stays in `Megaman` (`024`).
- A shared player invincibility function (`060`). A `TODO` at the getter names the second-character case.
- `editDamageFrom` (`023`), the owner walk in `canBeDamagedBy` (`030`), the stun bounce direction (a change of gameplay).
- `clearLocks(DAMAGE)` and the `SPAWN` lock.
- Moving parts to `engine/`.

## Affected areas and risks

Files: `M/Megaman.kt`, `ControllerComponent.kt`, `SpritesComponent.kt`, `BodyComponent.kt`, `MegamanAnimationKeySupplier.kt`,
`MegamanWeaponsHandler.kt`, 9 new files in `C/entities/state/`, 1 new file in `M/components/`, `MegamanStunTargetTest.kt`, new
tests, and a new test map.

Risks:

- **State now clears at destroy, not at the next spawn.** The engine calls `reset()` on destroy. Today `frozen`, `stunned`
  and `recoveryFlash` keep their values until `onSpawn`. Mega is out of the engine in that window, and `invincible` is true
  through `!liveAfterUpdate`, but the plan must check each reader in that window.
- **First spawn.** No `reset()` runs before it. The constructor state must equal the `reset()` state; a test checks it.
  `Timer.setToEnd()` in a constructor leaves `justFinished` true until the next update. Today `onSpawn` clears it. `:756`
  only writes `false` to a field that is already `false`, so this has no effect.
- **Order of hook evaluation.** `mashed` must stay lazy, because `input` uses `!!`, and it must run only while frozen and
  off cooldown.
- **Knockback arms:** `LEFT` and `RIGHT` swap the axes. Copy them unchanged.
- **Log text:** the debug text at `:606-611` loses `getRatio()`. No gameplay effect.
- **Proof of order is partly manual.** `testServices()` cannot run the whole lambda. The frozen-copy suite covers each part,
  `CONTRACT.md` has a table of old line to new position, and the owner's play test covers the rest.
- **Performance:** each `stunned`, `frozen` and `require` read adds a map lookup, but none of these is on the contact path
  after the `damageRecovery` field. Measure with `--diagnostics` before and after.

## Overlap with other branches

See `docs/NOTES.md`. In short: `051b` changes other lines of `M/Megaman.kt`. `031e` lists `ControllerComponent.kt:99`,
and this branch changes only that file's imports and reads at `:57,73,80`.

## Open questions

None. The 3 questions of the draft are answered in `docs/NOTES.md`, "Decisions".
