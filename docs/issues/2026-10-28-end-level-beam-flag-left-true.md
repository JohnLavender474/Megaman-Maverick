---
status: open
verified: question
area: core
gameplay-change: unknown
found-by: mega-refactor-016a-typed-level-state (2026-10-28)
related-items: [016]
---

# The end-level beam sets the Megaman beam flag to `true` when the beam ends

## Where

`core/src/main/kotlin/com/megaman/maverick/game/screens/levels/events/EndLevelEventHandler.kt:227` (`beamEnd`), on
`master` at `06901895e`. The flag is the game property `"${Megaman.TAG}_${ConstKeys.BEAM}"` (item `016a` moves it to
`LevelState.megamanBeam`).

## What happens

The flag means "Megaman is in a beam". The writers:

- `PlayerSpawnEventHandler.kt:134` sets it to `true` when the spawn beam starts, and `:218` sets it to `false` when the
  spawn beam ends.
- `EndLevelEventHandler.kt:120` sets it to `true` when the end-level beam starts, and `:227` sets it to `true` again
  when the end-level beam ends, just after it submits `END_LEVEL`.

So after a level ends, the flag stays `true` until the next player spawn beam sets it again. The readers are
`DarknessV2.kt:372` and `DarknessV4.kt:556`: while the flag is `true`, they light the area at the beam center
(`..._${ConstKeys.BEAM}_${ConstKeys.CENTER}`, a reference to the handler's own `Vector2`, which keeps its last
position).

## What should happen

Probably `false` at `:227`, as the spawn handler does at `:218`: the beam is over when its end timer finishes.

## How to reproduce

No visible effect was found. A case to check: a level with `DarknessV2` or `DarknessV4` (for example `WILY_STAGE_1`)
that starts after another level ended with the beam-out, if a darkness entity updates before the next spawn beam sets
the flag. `verified: question` because `true` may be intended (the owner decides).

## Notes

Found in the `016a` scan (`docs/TODO.md` of that branch). `016a` keeps the current behavior. A fix changes the value
written at `:227` (or at the new `LevelState` field after `016a`).
