---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor-004a-entity-smoke-test (2026-11-01)
related-items: [004, 039]
---

# `TreeStump` `init()` throws: it passes the enum name as the atlas path

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/decorations/TreeStump.kt:38`.

## What happens

The call passes `TextureAsset.ENVIRONS_1.name`, not `.source`, so `init()` throws
`GdxRuntimeException: Asset not loaded: ENVIRONS_1`. No live map places a `TreeStump`.

## What should happen

`TreeStump` uses `TextureAsset.ENVIRONS_1.source`. Also check that the region `TreeStump` is in that atlas.

## How to reproduce

Remove `decorations.TreeStump` from `core/src/smokeTest/resources/known-entity-failures.txt` and run
`./gradlew core:smokeTest`.
