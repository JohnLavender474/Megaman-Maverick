# TODO: mega-refactor-178a-bot-input

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- **Named player slots** (owner, 2026-11-22). Add `PlayerSlot.PLAYER_1 = PlayerSlot(0)` and
  `PlayerSlot.PLAYER_2 = PlayerSlot(1)` as `val`s in the `PlayerSlot` companion (a value class cannot be `const`).
  Replace every `PlayerSlot(0)` and `PlayerSlot(1)` in one change: 27 and 7 sites in 19 main files, 114 in tests
  (count of 2026-11-22). The name is the player number, not the index (`CODE_STYLE.md`, "Long where a short name
  has two meanings"). `FollowBrain`'s private `LEADER` and `BOT` stay as role names.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
