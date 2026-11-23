# 179: Named player slots (`PlayerSlot.PLAYER_1`, `PlayerSlot.PLAYER_2`)

- Status: idea
- Type: refactor
- Objectives: `O1`, `O2`
- Depends on: none
- Related: `047`, `178`
- Branches: none yet

## Goal

Code names a player slot by the player number, not by a bare index: `PlayerSlot.PLAYER_1` in place of
`PlayerSlot(0)`, and `PlayerSlot.PLAYER_2` in place of `PlayerSlot(1)`. An index and a player number differ by 1, so a
bare `PlayerSlot(1)` has two possible meanings to a reader.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Add `PlayerSlot.PLAYER_1 = PlayerSlot(0)` and `PlayerSlot.PLAYER_2 = PlayerSlot(1)` as `val`s in the `PlayerSlot`
  companion (a value class cannot be `const`).
- Replace every `PlayerSlot(0)` and `PlayerSlot(1)` in one change: 27 and 7 sites in 19 main files, 114 in tests
  (count of 2026-11-22).
- `FollowBrain`'s private `LEADER` and `BOT` stay: they are role names, not slot names.

## Owner's input

- 2026-11-22 (branch `178a`): named player slots, with the name `PLAYER_1`. The name is the player number, not the
  index (`docs/CODE_STYLE.md`, "Long where a short name has two meanings").

## Open questions

## Sources

- `NOTES.md` in this folder.
- Backlog candidate in `items/178-npc-allies/branches/mega-refactor-178a-bot-input/docs/TODO.md`.
