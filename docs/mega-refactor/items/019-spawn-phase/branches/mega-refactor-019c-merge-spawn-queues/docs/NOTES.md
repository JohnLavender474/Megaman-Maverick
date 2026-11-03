# Notes: mega-refactor-019c-merge-spawn-queues

## Decisions

- (2026-11-03) `playerSpawnsMan` and `SPAWN_NOW` are not spawn queues; the queue work leaves them out (owner: yes).
- (2026-11-03) Item `019` is `done` after this branch. The spatial index for spawners becomes a backlog candidate,
  with a measure first (owner: "Sure, go for it"). The owner asked whether a spawner cost would already show. Answer
  given: a large cost would, a small one may not, because play tests run on a fast laptop and no measure of the
  spawner pass exists; `019b` already removed the largest known cost (the pooled camera rectangles).
- (2026-11-03) Rename `PlayerSpawnsManager` to `PlayerCheckpointManager` and `playerSpawnsMan` to
  `playerCheckpointMan` in this branch (owner's idea; owner: "Yes, go for it").

## Overlap with other branches

- `mega-refactor-118b-batch-1` (active, 2026-11-03): changes `MegaLevelScreen.kt:716-723` (boss spawn event). This
  branch changes other lines of the same file. Take in `master` before the second of the two merges.
- Item `018` (idea): its `NOTES.md` expected `019c` to change `GameEngine.kt:219-240`. The draft scope does not change
  `GameEngine`.
