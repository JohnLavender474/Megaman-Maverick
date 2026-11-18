# 171: A shared ledge-walk helper for enemies

- Status: idea
- Type: refactor
- Objectives: `O1`, `O4`
- Depends on: none
- Related: `079`, `015`, `042`, `173`
- Branches: none yet

## Goal

`Met` (branch `079a`) keeps its ledge-walk rules in the class: `runOrJumpAhead`, `tryJumpToPlayerPlatform`,
`planJump`, the goal lambdas and the jump tables. No other enemy uses the same rules today, so they stay in `Met`
(`O1-R5`). When a second enemy gets "stop, turn or jump at a ledge", move the shared part into one helper, so that
the two enemies do not copy it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Start only when a second enemy needs the rules (likely `SpikeBot` or `FireMet` in a later `079` branch, or a `042`
  branch). That enemy is the second caller.
- Move the `GroundAhead` dispatch into a helper in `entities/utils/` that returns a result: run, jump with a
  `JumpPlan`, or blocked.
- Keep the state change, the jump tables and the goals in each enemy.
- Do it after `015` if `015` has merged, so that the helper fits the new state machine.
- It changes gameplay for the second enemy, so it needs its own scope and a test room.
- `Starkner` cannot use it: its body is `ABSTRACT` and it flies.

## Owner's input

2026-11-18 (`079a`, final review): the walk rules stay in `Met` while no other enemy uses them (`O1-R5`).

## Open questions

- Which enemy is the second caller?

## Sources

- `NOTES.md` in this folder.
- Branch `docs/TODO.md` of `079a`: `items/079-smarter-enemies/branches/mega-refactor-079a-smarter-met/docs/TODO.md`.
