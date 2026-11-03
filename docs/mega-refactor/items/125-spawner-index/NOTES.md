# Notes: 125 Spatial or room index for spawners

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

- 2026-11-03 (`master` at `5e4483993`): each frame, `SpawnsManager.update` tests every spawner of the level (about
  250-500 for each level, from the `019` review). Since `019b`, the bounds spawners share one camera rectangle for each
  frame, so a test is a predicate call and a rectangle overlap. No measure exists.
