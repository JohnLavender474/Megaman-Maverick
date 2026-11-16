---
status: open
verified: no
area: core
gameplay-change: yes
found-by: `mega-refactor-024a-weapon-defs` (2026-11-14)
related-items: [024]
---

# Moon Scythes can fade live scythes too soon, because the count array keeps old entries

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/weapons/MegamanWeaponStates.kt`:
  `MoonScythesState.fadeExtraScythes`, the early `return` when 4 or fewer scythes exist (a `TODO` marks the line)
- `core/src/main/kotlin/com/megaman/maverick/game/weapons/WeaponState.kt`: `getAllSpawned(out)` adds to `out` and
  does not clear it

Before `024a`, the same code was in the old `MegamanWeaponsHandler.kt:336`.

## What happens

After each shot, `fadeExtraScythes` fills the reused `outArray` with the spawned scythes. When 4 or fewer exist, it
returns before `outArray.clear()`. The next shot adds the scythes again to the entries that stay. So the array can hold
the same scythe twice and scythes that are already dead. The count then goes above 4 too early, and the loop fades live
scythes that should stay.

## What should happen

The count holds each live scythe once. Only the oldest scythes over 4 fade.

## How to reproduce

`./run-game-alpha.sh --level TEST_1 --room room35 --weapons MOON_SCYTHES`: fire Moon Scythes a few times with a pause
between shots, so that 4 or fewer stay alive after each shot. Then fire again and look at how many scythes fade.

## Notes

The fix is to clear `outArray` before the early return (or at the start of the function). `024a` kept the behavior,
because it is a refactor with the contract "Megaman plays the same". `011a` changes the sort key of the same function
(`spawnOrder` in place of `timeSpawned`).
