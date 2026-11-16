---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: `mega-refactor-011a-game-random` (2026-11-14)
related-items: [011, 024]
---

# Moon Scythes of a new shot sort as the oldest and fade first

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/weapons/MegamanWeaponStates.kt`:
  `MoonScythesState.fadeExtraScythes`, the sort on `spawnOrder` (a `TODO` marks the line)

Before `024a`, the same code was in the old `MegamanWeaponsHandler.kt` `postShoot`. Before `011a`, the sort key was
the wall-clock `timeSpawned`.

## What happens

`fadeExtraScythes` runs right after the new shot asks to spawn its scythes. A spawn is queued until the next
`engine.update()`, so the new scythes have the `spawnOrder` of their last pooled life, or 0. They sort as the oldest,
and the loop fades them first, not the oldest live scythes. `master` before `011a` had the same fault with
`timeSpawned`.

## What should happen

With more than 4 scythes, the oldest scythes fade and the new shot stays.

## How to reproduce

`./run-game-alpha.sh --level INTRO_STAGE --room room1 --weapons MOON_SCYTHES`: fire Moon Scythes quickly so that more
than 4 are alive. Look at which scythes fade.

## Notes

Found by the review before the play test of `011a`. `011a` kept the old behavior (contract check 7). A fix can sort
after the spawn (for example on the next frame), or give a queued spawn its order number at the request. The other
Moon Scythes fault (`2026-11-14-moon-scythes-stale-array.md`) is in the same function.

## Short contract

Branch: `fix/moon-scythes`, off `master`, in `.worktrees/fix/moon-scythes`. It fixes this issue and
`2026-11-14-moon-scythes-stale-array.md` (same function).

Intended changes, all in `MoonScythesState.fadeExtraScythes`:

- The function clears its reused array before each count, so the count holds each spawned scythe once.
- It counts only the scythes that are live after the next update and do not fade yet. A scythe that already fades (it
  bounced out, or an earlier shot faded it) no longer counts, so it does not make one more live scythe fade.
- It sorts a scythe whose spawn is still queued (the scythes of the shot that calls it) as the newest, after all live
  scythes, which keep their `spawnOrder`. So with more than 4 counted scythes, the oldest fade and the new shot stays.
- The limit `4` becomes a named constant in `MegamanValues`.

Not changed:

- `GameEngine` and `MegaGameEntity.spawnOrder`: when the engine sets the order and what it means for other entities.
- `WeaponState` and the other weapon states, `fireMoonScythes`, `MoonScythesState.canFire` and the 1 s wait.
- `MoonScythe` itself: speed, bounces, fade time, trail, damage.

Checks:

- New unit tests in `core` (`MoonScythesStateTest`) fail on the old function and pass with the fix: a new shot of 4
  with 4 older live scythes fades the 4 older ones and none of the new; a shot after all scythes of the last shot died
  fades nothing; a shot with an older scythe that already fades fades only the older live ones; 5 live scythes fade
  only the oldest one.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/moon-scythes`:

- `./run-game-alpha.sh --level INTRO_STAGE --room room1 --weapons MOON_SCYTHES`: fire Moon Scythes, wait about 1 s
  while the 4 scythes fly, and fire again. The 4 older scythes fade, and the 4 new scythes fly on.
- `./run-game-alpha.sh --level TEST_1 --room room35 --weapons MOON_SCYTHES`: fire, and wait until all 4 scythes are
  gone. Fire again: no scythe of the new shot fades at once. Do this a few times.
- Control: `./run-game-alpha.sh --level DESERT_MAN --room room16 --weapons MOON_SCYTHES --heartTanks all --healthTanks all`:
  go right into the boss room and fight Desert Man with Moon Scythes. The scythes fly, bounce on the walls, leave the
  trail and hurt the boss as before.
