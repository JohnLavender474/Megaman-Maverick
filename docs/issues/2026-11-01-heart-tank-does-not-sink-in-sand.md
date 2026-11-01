---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-084g-movement-state play test (2026-11-01)
related-items: []
---

# A `HeartTank` that falls on quicksand does not sink

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/items/HeartTank.kt:112-115`: the filter acts only on the `FEET` contact
- `core/src/main/kotlin/com/megaman/maverick/game/entities/items/AbstractItem.kt:123`: the item body is `DYNAMIC`
- `core/src/main/kotlin/com/megaman/maverick/game/world/collisions/MegaCollisionHandler.kt`: the collision with a `STATIC` block
- `core/src/main/kotlin/com/megaman/maverick/game/world/contacts/rules/FeetSandRule.kt:56`: the sand friction
- `assets/tiled_maps/tmx/DesertMan_16x14_v4.tmx`: `HeartTank` at 18432,512 in `room14`, `FeetRiseSinkBlock` 628 and `QuickSand` 627, with the same top

The owner saw the problem in play. The cause is from a read of the code on `master` at `c6b8cecf7`. No change in the game confirmed it.

## What happens

The tank falls on the sand, dips a small distance, jitters back up to the top of the sand, and never sinks. So it cannot be lost, and Mega Man has no need to catch it.

The tank is a `DYNAMIC` body, and `FeetRiseSinkBlock` 628 is `STATIC`. The standard collision handler pushes the tank up in each cycle. The filter in `HeartTank` stops only the `FEET` contact with that block, not the collision.

The dip and the jitter have a probable cause, with less proof. The block moves only for players and `PushableBlock`. It sinks under Mega Man, the tank goes down with it, and the block lifts the tank when he leaves. If the dip occurs with Mega Man off the sand, this explanation is wrong. `--logTags FeetRiseSinkBlock` shows `feetSet.size`.

## What should happen

The tank sinks into the sand, and Mega Man must catch it before it is gone. Mega Man still sinks and rises on the block.

## How to reproduce

`./run-game-alpha.sh --level DESERT_MAN --room room13`: go through the gate to `room14`, stay off the sand, and let the tank fall on it.

## Origin

- `926c46c55` (2025-11-14): `Body(BodyType.ABSTRACT)` became `DYNAMIC` in `AbstractItem`. An `ABSTRACT` body has no block collision, so the `FEET` filter from `cbd5a1a5c` was enough before this commit.
- `5724edc29` (2026-06-21): items got friction. After a fix of the collision, the sand friction limits the sink to about 0.46 tiles per second (a calculation at a step of 1/150). The old limit was `MIN_SAND_VEL_Y`, -4 tiles per second.

The refactor did not cause this: its first commit is from 2026-10-22, and `084g` changes no file in items, blocks or `world/`. These commits were not bisected in the game.

## Notes

Fix branch: `fix/heart-tank-sinks-in-sand`, off `master`. It shares no file with `084g`, so either can merge first.

- In `HeartTank`, override `defineBodyComponent()` and add a block filter for `FeetRiseSinkBlock`. Do not add it in `onSpawn`: filters stay on the pooled body.
- A test that fails first: a new `HeartTankSandTest`, from `CartV2ItemTest`, with the real `WorldSystem` and `MegaCollisionHandler`. After N cycles, the bottom of the tank is below the top of the block.
- Owner decision (2026-11-01): set `take_friction_sand=false` on the tank, as `DesertMan.kt:362` does, to restore the old sink speed. With friction, 6 tiles of sand take about 13 seconds.
