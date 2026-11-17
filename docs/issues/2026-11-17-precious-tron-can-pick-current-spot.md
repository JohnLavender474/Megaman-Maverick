---
status: open
verified: question
area: core
gameplay-change: yes
found-by: `mega-refactor-045b-spawn-allocations` (2026-11-16)
related-items: [045]
---

# `PreciousTron` can pick its current spot as its next spot

## Where

`PreciousTron.setNextPosition`.

## What happens

The filter `it != currentPosition` compared a lambda with a `Vector2`, so it was always true, and the next spot can be
the current spot. `045b` removed the dead check and kept the behavior.

## What should happen

The owner decides: if the next spot must differ, filter out the current spot.

## How to reproduce

`./run-game-alpha.sh --level PRECIOUS_WOMAN --room room4`, climb to `room5`, and watch `PreciousTron` for a move that
stays in place.
