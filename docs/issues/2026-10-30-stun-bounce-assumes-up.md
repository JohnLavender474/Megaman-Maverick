---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: `084` scan (2026-10-30)
related-items: [084]
---

# The stun bounce assumes that Mega's body direction is `UP`

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/components/DamageReactionComponents.kt:37-44`: the `onStun` hook, with the `TODO` at `:38`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/state/Stuns.kt`: `onStunPlayerEvent` calls the hook
- `core/src/main/kotlin/com/megaman/maverick/game/entities/state/Knockbacks.kt:14-31`: `knockBackFrom` has a case for each of the 4 directions
- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/components/ControllerComponent.kt:217-222`: the run impulse has a case for each of the 4 directions

Found by a read of the code on `master` at `d81b58a92`. No change in the game confirmed it.

## What happens

`onStun` always writes the same 2 velocity values:

- `x = STUN_IMPULSE_X * PPM * movementScalar * -facing.value`
- `y = STUN_IMPULSE_Y * PPM`

For a body direction other than `UP`, this gives the wrong bounce:

- `DOWN`: the `y` value pushes Mega into the ceiling that he stands on, and the `x` value pushes him the opposite way from `UP`, relative to his facing.
- `LEFT` and `RIGHT`: the bounce goes along the wall, and the backward push goes into the wall or away from it.

## What should happen

The bounce is the same as in `UP`, relative to Mega's body: up from his feet and back from his facing. The signs below follow `knockBackFrom` and the run impulse. `backward` is `STUN_IMPULSE_X * PPM * movementScalar * facing.value`, and `up` is `STUN_IMPULSE_Y * PPM`.

| Direction | `x` | `y` |
|-----------|-----|-----|
| `UP` | `-backward` | `up` |
| `DOWN` | `backward` | `-up` |
| `LEFT` | `-up` | `-backward` |
| `RIGHT` | `up` | `backward` |

The `UP` row is the current behavior, and it must not change.

## How to reproduce

No room has a `TestStunPulser` and a gravity change. The fix adds one: a `TestStunPulser` in the `knockback` room of `RefactorTest084.tmx`, which has `GravitySwitcharoo` objects for all 4 directions, or a new room.

Then `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: turn gravity to each direction, stand on a surface and wait for a pulse.

## Notes

- The bug has no known effect in the shipped levels. `TimberWoman` is the only production sender of `STUN_PLAYER`, and neither fight room has a gravity object: `TimberWoman_16x14_v4.tmx` has 0 in the whole map, and `WilyStage3_v4.tmx` has none in that room. So the risk of the fix for the shipped levels is low, and the value is for later content.
- The `UP` values need a test first. `MegamanStunTest` or a similar test can pin the 4 rows of the table.
- The fix goes in `fix/stun-bounce-direction`, and it removes the `TODO`.
