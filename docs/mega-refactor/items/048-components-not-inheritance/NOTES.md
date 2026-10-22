# Notes: 048 Replace entity inheritance with components

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## From the old `BACKLOG.md` (2026-10-22)

Entities get health by extending `AbstractHealthEntity`. Enemies must extend `AbstractEnemy` and
bosses must extend `AbstractBoss`. Logic in the game does type checks on these classes. Replace
the inheritance layers with components (for example a health component) and remove the type
checks.
