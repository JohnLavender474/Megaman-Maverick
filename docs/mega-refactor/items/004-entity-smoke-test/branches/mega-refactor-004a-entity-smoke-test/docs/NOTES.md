# Notes: mega-refactor-004a-entity-smoke-test

## Decisions

Owner decisions at `SCOPE.md` approval (2026-11-01):

- The suite uses real textures, with no stub code. This closes the `ITEM.md` open question about stubs.
- A silent region miss fails the test. A miss through `containsRegion` does not count.
- The 18 failing classes go on the known-failures list, and their bugs are filed at the merge. This branch fixes none
  of them.

## Overlap with other branches

Checked on 2026-11-01. `ITEMS.md` lists no other active branch. The worktrees `mega-refactor-016b`, `016c`, `016d`,
`016e`, `016-master`, `031d` and `084e` hold merged branches. `compactor-level` is not a refactor branch. No overlap.

## Spike (2026-11-01)

A throwaway spike gave the numbers in `SCOPE.md`. It was not committed. The run needs `JAVA_HOME` on JDK 17, because
the default `java` on the owner's machine is JDK 25.
