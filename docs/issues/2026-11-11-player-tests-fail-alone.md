---
status: open
verified: yes
area: build
gameplay-change: no
found-by: mega-refactor-060c-protoman-buster review before the play test (2026-11-11)
related-items: [028, 060, 081]
---

# `ProtomanTest` and `MegamanXTest` fail when they run alone

## Where

- `core/src/test/kotlin/com/megaman/maverick/game/entities/protoman/ProtomanTest.kt`
- `core/src/test/kotlin/com/megaman/maverick/game/entities/megamanx/MegamanXTest.kt`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/MegaEntityFactory.kt` (the preload of 25 `Bullet` and 25
  `Block` in `init(services)`)
- `Bullet.init()` (the static region cache)

## What happens

With `--tests` on one of these classes, every test fails in `beforeEach` with
`ClassCastException: java.lang.Object cannot be cast to TextureAtlas`. `MegaEntityFactory.init(services)` preloads
25 `Bullet`, and `Bullet.init()` loads its regions from the relaxed assets mock. The generic atlas getter of the mock
returns an `Object`. In the full suite an earlier test has filled `Bullet`'s static region cache, so the load is
skipped and the tests pass. So the result depends on the test order.

## What should happen

Each test class passes alone and in the full suite, in any order.

## How to reproduce

- `./gradlew core:test --tests '*ProtomanTest'`
- `./gradlew core:test --tests '*MegamanXTest'`

Every test fails in `beforeEach`. `PushableBlockTest` passes alone. Found on `master` `aef725ce1` and on the `060c`
branch.

## Notes

Related: item `028` (the companion region caches). Found in the `060c` review
(`docs/mega-refactor/items/060-proto-man/branches/mega-refactor-060c-protoman-buster/docs/TODO.md`, "Bug candidates").
