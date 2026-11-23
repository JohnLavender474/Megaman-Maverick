# Notes: mega-refactor-178a-bot-input

## Decisions

- 2026-11-22, owner (brief D1 to D4, all as recommended): the camera follows P1 only in a bot session; the side view
  edges are `WARP` in a bot session; a bot death costs a shared life until `178e` (`TODO`); the option is
  `--bot follow`.
- 2026-11-22, Claude: the bot copies every button except `START`, and slot 1 takes slot 0's `selectButtonAction` at
  start, so a copied `SELECT` does the same thing for both (also the weapon switch).
- 2026-11-22, Claude: no `BotBrain` interface while `FollowBrain` is the only brain (`O1-R5`); `178d` adds it.

## Overlap with other branches

None at the start (2026-11-22): the active branches `060k`, `078e`, `078f`, `078g`, `compactor-level` and
`fix/projectiles-ignore-death` touch art, levels and projectiles.
