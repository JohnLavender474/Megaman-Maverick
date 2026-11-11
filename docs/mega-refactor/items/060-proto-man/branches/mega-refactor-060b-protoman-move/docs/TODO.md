# TODO: mega-refactor-060b-protoman-move

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- **Currency pickup sound queued in idle roster players** (review before the play test, low). `postCreate` adds the
  `PlayerProgressComponent` of each roster player as a `gameState` listener (`MegamanMaverickGame.kt:459`, `:468`,
  `:474`). On a currency pickup, `onCurrencyAdded` (`players/MegaPlayer.kt:258-260`) queues `CURRENCY_PICKUP_SOUND`
  in the `AudioComponent` of each player, also of a player that is not in the engine. The queue plays or clears only
  at that player's next spawn or destroy, so the queued sounds can play at a later spawn (for example: collect bolts
  as Megaman, then start a level as Proto Man). X already does this; Proto Man adds a second idle player. Not
  confirmed: whether `AudioSystem` skips the queue of an entity that is not in the engine.
