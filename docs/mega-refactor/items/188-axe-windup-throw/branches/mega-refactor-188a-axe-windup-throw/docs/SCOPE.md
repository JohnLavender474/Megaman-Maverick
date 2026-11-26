# Scope: mega-refactor-188a-axe-windup-throw

- Status: approved
- Approved: 2026-11-26 (owner, chat: "Go with recommendations, build out the branch with tests, then push a draft
  PR." The brief was given in the chat, over several turns.)

## Goal

Mega Man's `AXE_SWINGER` becomes a hold-to-aim throw: a tap throws a weak axe, a hold keeps the "axe above the head"
frame and draws a dotted arc, and the impulse grows with the hold (x more than y). The per-player weapon charge
becomes a plain windup that both the buster's levels and the axe's continuous hold fit.

## Item

`188` (`ITEM.md`): the whole item.

## Current state

Scan of `master` at `2189b7a` (2026-11-26). Base path `core/src/main/kotlin/com/megaman/maverick/game/`.

- Charge: `entities/state/WeaponChargeComponent.kt` (private `Timer(full, TimeMarkedRunnable(half, onHalf))`, `half`,
  `full`, `update`, `reset`); `players/PlayerCharge.kt` (`playerChargeComponent` hard-wires
  `MEGA_BUSTER_CHARGING_SOUND`, `playerChargeStatus`, `stopCharging()`, item 121 TODO);
  `entities/megaman/components/WeaponChargeComponent.kt` (`defineWeaponChargeComponent`, `chargeStatus`, `charging`,
  `halfCharged`, `fullyCharged`, `clearChargeWhenHurt`, all gated on `weaponsHandler.isChargeable(currentWeapon)`);
  `entities/megamanx/MegamanXController.kt:71-72`, `MegamanX.kt:105, 127, 133-152, 371-424`.
- Megaman input: `Megaman.kt:448-524` attack `ButtonActuator` (`onPressContinued` with the gates and the
  `FASTER_BUSTER_CHARGING` scalar; `onJustReleased` then `shoot()` then `stopCharging()`); per-frame reset for a weapon
  that does not charge at `Megaman.kt:280`; teleport stops at `:212, :219`; light at `:193-199`.
- Fire: `WeaponDef.kt:18, 21` (`fire(O, WeaponState, MegaChargeStatus)`, `chargeable`); only `MEGA_BUSTER` sets
  `chargeable` (`MegamanWeapons.kt:29`); `MegamanWeaponsHandler.kt:204` `isChargeable`, `:281` forces `NOT_CHARGED`.
- Axe: `MegamanWeapons.kt:65-71`; `MegamanWeaponFire.kt:381-438` (`throwAxe`, `fireAxeSwinger` with a 0.1 s
  `delayRun`); `MegamanValues.kt:67-69` (`AXE_IMPULSE_X = 8`, `AXE_IMPULSE_Y = 20`, `AXE_SLOW_SCALAR = 0.5`).
  `entities/projectiles/Axe.kt` (gravity 56.25 in `preProcess`; `RIGHT` has the sign of `LEFT`, `:157`).
- Pose: `MegamanAnimationKeySupplier.kt:27-111` (`amendKey` gives `<base>_axe_throw` only while `shooting`);
  `MegamanAnimationDefs.kt` (`*_axe_throw` = 2 frames of 0.1 s; `run_axe_throw` 2x2 at 0.125 s; no `climb_axe_throw`
  and no `climb_finish_axe_throw` def); `Megaman.kt:535-562` `Animator` (no `shouldUpdateAnimationPredicate`);
  `components/AnimationsComponent.kt:11-47` (`keepsAnimTimeAcrossKeys`); `behaviors/ClimbBehavior.kt:19`
  (`holdStill = { shooting }`).
- Drawing: Megaman effect sprites in `entities/megaman/components/SpritesComponent.kt` (always present, `hidden` per
  frame). `TextureAsset.COLORS` has a `white` region.

## In scope

### Windup (refactor of the charge)

```kotlin
// weapons/WeaponWindup.kt
interface WeaponWindup { fun hold(delta: Float); fun clear() }

// weapons/BusterCharge.kt: buster-style levels; the owner wires the hooks
class BusterCharge(fullTime, halfTime, speedScalar: () -> Float, onHalfCharged, onFullyCharged, onCleared) : WeaponWindup
    fun status(): MegaChargeStatus   // from its time and its thresholds

// entities/megaman/weapons/AxeThrowWindup.kt
class AxeThrowWindup(fullTime) : WeaponWindup { val time; val progress }

// entities/state/WeaponWindupComponent.kt (replaces entities/state/WeaponChargeComponent.kt)
class WeaponWindupComponent : IGameComponent { val active; fun hold(w, delta); fun stop() }
```

- Each player builds its own windups in its own code: Megaman a `BusterCharge` (his times, the charge sound, the
  enhancement scalar) and an `AxeThrowWindup`; X his own `BusterCharge` (his times, the same sound today). No
  windup on `WeaponDef`, no factory.
- `BusterCharge` keeps the half-time trigger on the engine `Timer` (`TimeMarkedRunnable`), and adds a full-time one;
  `onFullyCharged` has no caller yet (owner accepted unused hooks, 2026-11-26). `status()` is computed from the time
  and the thresholds (`MegaChargeStatus.of(time, halfTime, fullTime)` or inline).
- `WeaponDef.chargeable` goes. Megaman maps a weapon to its windup with
  `windupOf(weapon) = when (weapon) { MEGA_BUSTER -> busterCharge; AXE_SWINGER -> axeWindup; else -> null }`.
- `stopCharging()` is renamed `stopWindup()` at every call site (the name must not read as charge-only for the axe).
- The status reads check both the current weapon and the active windup:
  `chargeStatus = if (windupOf(currentWeapon) === busterCharge && windups.active === busterCharge) busterCharge.status() else NOT_CHARGED`.
- `Megaman.kt:280` becomes `if (windups.active != null && windups.active !== windupOf(currentWeapon)) stopWindup()`:
  a weapon switch clears the old windup in the same frame, before the animation and sprite systems.
- `MegamanWeaponsHandler.fireWeapon` keeps a charge status only for a weapon whose windup is a `BusterCharge`.
- `clearChargeWhenHurt` clears the windup through `stopWindup()`.

### Axe throw

- `AXE_HOLD_TIME = 0.7f`, `AXE_WINDUP_TIME = 0.1f` (frame 0 and the tap threshold). Ratio
  `r = clamp((t - 0.1) / 0.6, 0, 1)`, linear. Local impulse (tiles/s): `x = lerp(4, 12, r)`, `y = lerp(17, 23, r)`;
  the old `(8, 20)` is `r = 0.5`. The per-direction mapping, the facing, `AXE_SLOW_SCALAR` (Moon Man or water), the
  spawn offset and the gravity scalar stay as today, in one shared launch function (`AxeThrow`), used by the throw and
  the preview.
- Timing: the windup advances while SHOOT is held and the gates pass (it starts when the axe can first fire, so a press
  during the cooldown starts it at the end of the cooldown). On release, the fire reads the windup time and progress
  before any delay. If `t >= 0.1`, the axe spawns at once and the held `*_axe_throw` animation jumps to its throw frame
  (time 0.1). If `t < 0.1`, the axe spawns after `0.1 - t` (a 0 delay calls the throw directly; `delayRun` rejects 0).
- Hold pose: `holdingAxe` is derived (`windups.active === axeWindup`); `amendKey` treats it like `shooting` for the
  axe key; the Megaman `Animator` does not advance an `*axe_throw` key while `holdingAxe` (frame 0). Holding does not
  set `shooting` (enemies, trails and run stops do not change). While climbing, `holdStill` also holds for
  `holdingAxe`, and the climb uses `climb_still_axe_throw`.
- Run while holding: frozen on frame 0 of `run_axe_throw` (legs still), unless the frames show the overhead pose in
  frames 0 and 1; the implementer checks the frames and writes the choice in `NOTES.md`.
- Pause: the windup does not advance during a room transition or a camera turn; the arc hides. Damage, stun, teleport,
  a weapon switch and death clear it (as the buster charge today).

### Dotted arc

- `entities/projectiles/AxeTrajectory.kt`: `gravity(direction, gravityScalar, out)` (used by `Axe.preProcess`) and a
  dot function that steps with `services.settings.performance.fixedStep` in the order of `Body.process`
  (`v += g*dt; p += v*dt`), emits a dot each 0.5 tile of arc, at most 32 dots, and stops 1 tile below the launch
  height along the up axis, or at 2 s.
- 32 dot sprites in Megaman's `SpritesComponent` (region `COLORS` `white`, 0.125 tile, `DrawingPriority(FOREGROUND,
  0)`), alpha fading from 1 to 0.35 along the arc, slot tint as Megaman, hidden unless `holdingAxe` and
  `t >= 0.15 s`, in play, no room transition, no camera turn, not damaged. They get Megaman's interpolation offset.
- No new art. The arc ignores shield bounces (the axe flies through blocks).

## Out of scope

- X's own charge sounds and retuning (item `081`); X keeps today's charge.
- The `WeaponDef.fire` signature, the `spawned` buckets keyed by `MegaChargeStatus`, `stopsChargeOnHold`'s name (a TODO
  notes it).
- `cartin_axe_throw` and `climb_finish_axe_throw` art (bug candidate: no axe throw key while in a cart).
- `LumberJoe`'s axe (it uses the default `UP` direction, unaffected).

## Affected areas and risks

- Buster charge feel and sound (the refactor). Covered by characterization tests written first, on the old code.
- X's charge (same).
- Megaman's animation keys (the hold key and the freeze), climbing.
- Megaman's sprite count (32 more hidden sprites).

## Overlap with other branches

See `NOTES.md`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | no effect | Windups are per player. |
| `O1-R2` Components, not new inheritance | helps | The windup is a small interface; `WeaponWindupComponent` holds the active one. |
| `O1-R3` No new fragile order | helps | The switch clear runs in the same frame (`Megaman.kt:280`), before animations. |
| Always-on: allocation (`O5`) | no effect | Dots and vectors are made once; no per-frame allocation. |
| Always-on: random calls (`O2-R2`) | no effect | No random calls. |

## Decisions

All 2026-11-26, owner: "Go with recommendations".

- D1 Throw: immediate on release after the 0.1 s windup, with the animation jumped to the throw frame; a tap keeps
  press + 0.1 s.
- D2 Curve: x 4 to 12, y 17 to 23 tiles/s, full at 0.7 s, linear (old throw at the midpoint). No full-charge cue.
- D3 Dots: `FOREGROUND`, white 2x2 art px, fading, 0.5 tile spacing.
- D4 Fix the `RIGHT` gravity of `Axe` (folded bug; affects only an axe thrown in a `RIGHT` room).
- D5 Pause the windup in room transitions and camera turns; a weapon switch clears it.
- D6 The windup starts when the gates first pass (the cooldown delays it), as the buster today.
- D7 Generic windup (`WeaponWindup`, `BusterCharge` with hooks, `AxeThrowWindup`), per-player instances, rename
  `stopCharging` to `stopWindup` now.
- D8 Tests are written in this branch (owner asked for the branch "with tests"), not after a play-test round.

# Contract

## Intended changes

1. Axe: tap and hold throws, the impulse curve, the hold pose, the dotted arc (D1 to D3, D5, D6).
2. Axe `RIGHT` gravity is the opposite of `LEFT` (D4; folded bug).
3. A stun alone now stops the charging sound (today it resets the charge and leaves the sound).
4. Co-op: a player whose weapon does not charge no longer stops the other player's charging sound every frame (the stop
   was global).
5. A switch from the buster to another weapon clears the charge in the same frame for any weapon (today too for a
   weapon that does not charge; now also for the axe).

## Must not change

- Buster charge: the half time and sound once per charge, the enhancement speed, the light, `_charge_half` and
  `_charge_full` keys, the shot levels, the sound stop on release, damage, teleport, room transition and gate.
- X's charge: times, sound, tint, sabre and dash flow.
- Every other weapon's fire, cost and cooldown. `LumberJoe`'s axe.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

In this cloud session Gradle cannot run (the proxy blocks Maven Central and `services.gradle.org`); the CI run of the
pushed branch is the automated check (`claude-web.md`, rule 6).

## Manual playthrough

The test map is `REFACTOR_TEST_188` (`RefactorTest188.tmx`). Experimental checks first.

1. `--level REFACTOR_TEST_188 --room throw_range --weapons AXE_SWINGER`: tap gives a short throw; a hold keeps the
   overhead frame and shows the dots after a moment; the dots match where the axe flies; a full hold lands about 10
   tiles away and stays on screen vertically; a medium hold feels like the old throw.
2. Same room: hold while running, jumping, sliding and on the ladder (Mega Man stays still on it); the pose stays on
   the overhead frame; release throws at once with the throw frame.
3. Same room: hold, then take damage, switch weapon, or walk through the room transition: the hold clears (or pauses
   in the transition) and the dots hide.
4. `--level REFACTOR_TEST_188 --room water --weapons AXE_SWINGER`: the arc in water is slower and the dots still match.
5. `--level MOON_MAN --weapons AXE_SWINGER` (room in the play scripts): low gravity, the dots match.
6. Control: `--level REFACTOR_TEST_188 --room throw_range`: buster charge half and full, the sound, the flash, the
   shots; switch to the axe mid-charge: the sound stops.
7. Control: `./run-mega-man-x-lost-memories-alpha.sh`: X's charge, tint and shots as before.
8. Control: `./run-mega-man-maverick-test-coop.sh --weapons AXE_SWINGER`: each player holds its own axe and dots.
