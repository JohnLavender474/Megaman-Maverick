---
status: closed
verified: yes
area: content
gameplay-change: no
found-by: mega-refactor-004a-entity-smoke-test (2026-11-01)
related-items: [004]
---

# `JetMet` `init()` throws: the atlas has no frozen region, and a live map uses it

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/JetMet.kt:111` and `JetMet.kt:293`, and the atlas
`assets/sprites/sprite_sheets/Enemies2.txt`.

## What happens

`JetMet.kt:111` asks for the region `frozen`. The atlas has only `JetMet/Jet`, `JetMet/Stand` and `JetMet/TakeOff`, so
the map gets a null value. `JetMet.kt:293` then throws `NullPointerException: get(...) must not be null` in `init()`.

`Gauntlet2.tmx`, the live map of a `LevelDefinition` entry, places `JetMet` objects (for example ids 129, 134 and 135).
`init()` runs on the first spawn, so the expected result is a crash there. This was seen in the smoke test, not in a play
run.

## What should happen

A `JetMet` spawns and plays its animations. A correct key alone does not fix this, because `JetMet/frozen` is also not in
the atlas. Either add the region, or use an existing region for the frozen state.

## How to reproduce

Remove `enemies.JetMet` from `core/src/smokeTest/resources/known-entity-failures.txt` and run `./gradlew core:smokeTest`.

## Notes

When this is fixed, remove the line from the list. The test fails until the line goes.

## Resolution

`JetMet` is commented out with Gauntlet 2, the only live level that used it. To play it again, restore the class and give it a frozen region. Squash commit `3747c6dd4` (`fix/retire-gauntlet-2`).
