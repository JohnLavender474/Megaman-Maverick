# Notes: mega-refactor-047b-player-rules

## Decisions

- 2026-11-02 (owner's approval of the code review): `isFriendlyFire` is a member of `SessionRules` and takes `IDamagerEntity` and `MegaGameEntity`. `rootPlayer` stays on `IGameEntity`, the narrowest common type of its callers. `IOwnable` has an `IGameEntity` bound, so `asPlayer` takes `IGameEntity?`. This replaces the 2026-11-01 decision that `asPlayer` keeps `Any?`.
- 2026-11-02: `IPlayers.any` and `forEach` stay extensions: they are inline, and an interface member cannot be inline. `players` is a member of `MegaGameEntity`, because no such reason applies to it.
- 2026-11-02: `anyLive` and `forEachLive` test `hasBody`, `liveNow` and `inPlay`, as `targetFor` does. With no live player they visit all players. So with 1 player the sites act as on `master`, also in a beam-in and after a death. `Lift` and `SpiderWeb` keep their `liveAfterUpdate` test.
- 2026-11-02: the `as Megaman` cast for weapon energy moved to `MegaLevelScreen.onEvent`. It is not gone: the event holds a slot, and `players.get` gives a `MegaGameEntity`.
- 2026-11-01: `rootPlayer` and `isFriendlyFire` take `IGameEntity`, not `Any?` (owner's review).
- 2026-11-01: the target distance uses `body.getCenter().dst2(...)`, not a helper of its own (owner's review).
- 2026-11-01, plan step 9: slot 1 gets the shared maximum health, because each player adds its `PlayerProgressComponent` as a game state listener. A heart tank heals slot 0 only, as `attain` submits the slot of slot 0.

## Enemy review (plan step 8)

From a code read of `entities/enemies/`: 320 `targetPlayer` lines and 25 `players` lines. Most plain lines were classed from the line alone. Nothing was run.

- No enemy writes to, locks or carries `targetPlayer`, or reads its input.
- 1 fault, fixed: `MagFly.kt:196`. A carried player who is not the nearest did not get the slow carry.
- The suspects are in `docs/TODO.md`.

## Measure (plan steps 2 and 10)

`--diagnostics`, `DESERT_MAN` `room1`, no `--players`, `--autoPerf false`, no input, last 1500 frames, `screen.render` median:

- Before the code: 2.224 ms. After step 9: 2.450 ms.
- Alternate captures after `0b675ac41`: branch 2.208 ms, `master` 2.169 ms, branch 2.308 ms, `master` 2.205 ms.

The branch is 0.04 ms to 0.10 ms slower in the paired captures. Single captures of one build differ by up to 0.25 ms. So the difference is small and not proved. The `master` captures ran with uncommitted changes of another session in the main checkout.

After the review fixes (plan step 10a), same room and settings, from the built jar, alternate captures: no `--players` 2.121 ms and 2.152 ms; `--players 2` 2.266 ms and 2.429 ms. The `--players 2` captures include a second `Megaman` in the world, so they are not a measure of `targetFor` alone. There is no `master` capture with `--players 2`.

## Overlap with other branches
