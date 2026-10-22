---
status: open
verified: no
area: core
gameplay-change: yes
found-by: item 056 research (2026-10-22)
related-items: [056]
---

# `BouncingPebble` bounces to the right when its right side hits a block

## Where

`BouncingPebble.kt:167`:

```kotlin
rightFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.RIGHT) }
```

## What happens

Both the left `SIDE` fixture (`:160`) and the right `SIDE` fixture (`:167`) call
`bounce(Direction.RIGHT)`. When the right side hits a wall, the pebble bounces to the right, into
the wall, not away from it.

`ReactorMonkeyBall.kt:160,167` has the same fixture setup, and its right fixture calls
`bounce(Direction.LEFT)`. So this is probably a copy-and-paste error.

## What should happen

The right fixture calls `bounce(Direction.LEFT)`.

## How to reproduce

Read the code above. In the game: make a `BouncingPebble` move right into a wall, and watch whether
it bounces back.

## Notes

`verified: no`: the result in the game is not checked. `bounce` may also read the current velocity,
which could hide the bug in some cases. A fix changes gameplay.
