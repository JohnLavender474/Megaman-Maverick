---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: item 014 research (2026-10-22)
related-items: [014]
---

# `MagmaWave`'s shield receiver never fires: the filter never makes `SHIELD`-`SIDE` contacts

## Where

- `MegaContactListener.kt:236-241`: the begin rule for `SHIELD`-`SIDE`.
- `MagmaWave.kt:176,184`: `setHitByShield { _, _ -> disintegrating = true }` on the left and right `SIDE` fixtures.
- `MegaContactFilter.kt:20-95`: the `filters` map.

## What happens

`MegaContactFilter.filter` makes a contact only if one fixture type is a key of `filters` and the
other type is in that key's set. `SHIELD` is not a key, and the `SIDE` set (`SIDE`, `ICE`, `GATE`,
`BLOCK`, `BOUNCER`, `DEATH`) has no `SHIELD`. So `WorldSystem` never makes a `SHIELD`-`SIDE`
contact, the listener rule never runs, and `MagmaWave` never sets `disintegrating` from a shield.

The rule and the receiver were added together in `926c46c55` (2025-11-14), but the filter was not
changed.

## What should happen

Probably: a `MagmaWave` that touches a shield disintegrates. Then add `SHIELD` to the `SIDE` set in
`MegaContactFilter`. If that is not wanted, remove the rule and the two receivers.

## How to reproduce

Read the code above. In the game: make a `MagmaWave` touch an entity with a `SHIELD` fixture. The wave
does not disintegrate.

## Notes

- A fix changes gameplay. Play-test the rooms with `MagmaWave` and shielded enemies.
- Item `014` must keep this rule dead, or name this issue under "Intended changes".
