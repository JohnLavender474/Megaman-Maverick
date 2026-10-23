---
status: open
verified: yes
area: content
gameplay-change: no
found-by: mega-refactor-029b-enemy-players (2026-10-23)
related-items: [039]
---

# `GapingFish` crashes on spawn: the atlas has no `GapingFish/frozen` region

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/GapingFish.kt:224` (`defineAnimationsComponent`),
and the atlas `assets/sprites/sprite_sheets/Enemies1.txt` (`TextureAsset.ENEMIES_1`).

## What happens

When a `GapingFish` spawns for the first time, `init()` builds its animations. The `frozen` animation asks for the
region `GapingFish/frozen`. `Enemies1.txt` has only `GapingFish/Chomping`, `GapingFish/Gaping` and
`GapingFish/Swimming`, so `findRegion(...)` gives null, and the game stops with
`NullPointerException: findRegion(...) must not be null`.

No live level places a `GapingFish` (only old map versions such as `GlacierMan_v3.tmx`, and test maps), so players
do not see the crash today.

## What should happen

A `GapingFish` spawns and plays its animations. Either the atlas gets a `GapingFish/frozen` region, or the enemy
uses an existing region for its frozen state.

## How to reproduce

Place a `GapingFish` in the `enemies` layer of a test map (for example in water in `RefactorTest029.tmx`), and start
that room with `./run-game-alpha.sh --level REFACTOR_TEST_029 --room <room>`.

## Notes

Found while building the `enemy_contact` test room for `029b`; the room uses a `Coldier` instead.
