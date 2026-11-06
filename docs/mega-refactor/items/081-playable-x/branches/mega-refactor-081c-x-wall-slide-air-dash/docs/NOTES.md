# Notes: mega-refactor-081c-x-wall-slide-air-dash

## Decisions

- D1 (owner, 2026-11-06): X's air dash starts on Megaman's input (`A` in the air; `SELECT` when the slot setting is
  `AIR_DASH`) and on a new `DASH` button (index 8, keyboard `U`, pad `R1`). This changes the `069` plan ("one dash input:
  `R` only; `A` is only jump and wall kick; `SELECT` is always `NEXT_WEAPON` for X"). Megaman ignores `DASH`.
- D2 (owner, 2026-11-06): the X6 air dash shape (hover, fixed 0.3 s at 7.734 t/s, no end bump, 1 per air).
- D3 (owner, 2026-11-06): the X6 wall rules (grab, fixed slide, kick on any wall touch, kick lock, push-off).
- D4 (owner, 2026-11-06): no new sounds for the grab, the kick and the dash; a `TODO`.

## Overlap with other branches
