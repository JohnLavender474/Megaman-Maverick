# Notes: 055 Record and replay for contract checks

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## From the old `BACKLOG.md` (2026-10-22)

Make it possible to check the contract with less manual work.

Approach under discussion:

- Drive a run with recorded inputs only.
- Record a full trace of each frame from the run on `master` and from the run on the branch, then
  compare the two traces frame by frame.
- Compare at levels. A strict level checks what the player sees and hears (positions, health,
  current room, sounds, drawn output). A detailed level shows events and internal state, to find
  the cause of a failure.

Requirements:

- The recorder can capture any relevant data, not only a fixed list.
- Recording must not lower the FPS. One option is an in-memory buffer that a background thread
  writes to a file in a compact binary format.

Required first (determinism):

- A seeded random source. Many files use `getRandom`, `shuffle` or `.random()`.
- A fixed timestep in replay mode. The frame delta comes from `Gdx.graphics.deltaTime` today.
- Synchronous pathfinding in replay mode. `AsyncPathfindingSystem` runs on other threads.
