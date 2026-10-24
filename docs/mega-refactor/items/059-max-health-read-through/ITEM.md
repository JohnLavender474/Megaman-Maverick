# 059: Max health read through from the profile

- Status: idea
- Type: refactor
- Depends on: none
- Related: `029`, `047`
- Branches: none yet

## Goal

Read the player's max health from the session profile (`GameState`) on each access, in place of a stored copy.

Today (after `029e`) Megaman's max health is a stored `Points.max`. Megaman's heart-tank listener sets it to `START_HEALTH + count × HEALTH_BUMP` from `game.state` when a heart tank changes. A stored copy can go stale: a player that registers after progress exists keeps the base max health until a heart tank changes (late join, `047`), and any new way to change the heart tanks must also notify the player.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A max that is computed on read, for example a max supplier on the engine `Points` (or a health component that asks the profile).
- Megaman's heart-tank listener and its `TODO` go; the late-join `TODO` at the player registration shrinks to the weapons (already read through) and nothing else.
- The same pattern can serve other profile-driven player values later.

## Owner's input

2026-10-23, at the `029e` scope (verbatim):

> Ideally, the design would carry forward to also cover heart tanks as well as any other achievements.

2026-10-23, on the stored max (verbatim, about "The max stays a stored field, not a value read from GameState each time. Reading it each time would need a change to the engine Points class."):

> This should be a TODO item if not already.

## Open questions

- Change `Points` in the engine (a supplier for `max`), or keep `Points` and add a game-side health source?
- What happens to the current health when the max goes down (today `removeHeartTank` only runs on reset)?

## Sources

- `NOTES.md` in this folder.
- `029e` branch folder: `docs/TODO.md` (backlog candidate), `docs/SCOPE.md` (in scope, item 3).
- `TODO` at `Megaman.setMaxHealthFromHeartTanks`.
