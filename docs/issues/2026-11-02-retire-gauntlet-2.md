---
status: closed
verified: yes
area: core
gameplay-change: yes
found-by: owner decision (2026-11-02)
related-items: [039]
---

# Retire Gauntlet 2 and 4 entities that only it or test maps use

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/levels/LevelDefinition.kt`: `GAUNTLET_2`
- `JetMet`, `ColtonJoe`, `GapingFish` and `TreeStump`, and their factory lines

## What happens

`JetMet`, `ColtonJoe` and `GapingFish` fail `init()` with no frozen region, and `TreeStump` fails with a wrong atlas
path. `Gauntlet2.tmx` has 19 `JetMet` wave entries in 4 rooms, so that level is expected to crash. `ColtonJoe` and
`GapingFish` are only in `Test1.tmx`, and `TreeStump` is in no live map.

## What should happen

Owner, 2026-11-02 (verbatim): "Let's go ahead and comment out the Gauntlet 2 stage as well, so these entities aren't used
anymore in live stages".

## Short contract

Branch: `fix/retire-gauntlet-2`.

Intended changes:

- The Extras screen no longer lists Gauntlet 2. `--level GAUNTLET_2` is a bad value, so the game exits with code 2.
- `GAUNTLET_2` and the 4 classes are commented out, with a `TODO` that names this file. `Gauntlet2.tmx` stays.
- `Test1.tmx` loses the `GapingFish` and `ColtonJoe` objects, so the level logs no error for them.

Not changed: `CeilingCrusher` stays active, because `Compactor.tmx` on `compactor-level` uses it. All other levels,
including Gauntlet 1, play as before.

Checks:

- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- A launch of `TEST_1` and of `GAUNTLET_1` from the built jar gives no exception.
- `--level GAUNTLET_2` exits with code 2.
- The owner opens the Extras screen: Gauntlet 2 is not listed, and Gauntlet 1 starts.

## How to reproduce

`EntityInitSmokeTest` lists the 4 classes in `known-entity-failures.txt`.

## Resolution

Done as specified. The owner checked the Extras screen and approved the merge. To restore Gauntlet 2, restore `JetMet` first. Squash commit `3747c6dd4` (`fix/retire-gauntlet-2`).
