---
status: open
verified: yes
area: content
gameplay-change: no
found-by: mega-refactor-004a-entity-smoke-test (2026-11-01)
related-items: [004]
---

# `ColtonJoe` `init()` throws: the atlas has no `ColtonJoe/frozen` region

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/ColtonJoe.kt:213`, and the atlas
`assets/sprites/sprite_sheets/Enemies2.txt`.

## What happens

`init()` throws `NullPointerException: get(...) must not be null`, because the region `ColtonJoe/frozen` is not in the
atlas. Of the live maps, only `Test1.tmx` places a `ColtonJoe`.

## What should happen

A `ColtonJoe` spawns and plays its animations. Either add the region, or use an existing region for the frozen state.

## How to reproduce

Remove `enemies.ColtonJoe` from `core/src/smokeTest/resources/known-entity-failures.txt` and run
`./gradlew core:smokeTest`.

## Notes

This is the same fault as `2026-10-23-gaping-fish-missing-frozen-region.md`.
