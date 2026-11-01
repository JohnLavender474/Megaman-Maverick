# Process log: mega-refactor-051-fix-scroll-room-scope

## Status

- State: merged into `mega-refactor-051-master`
- Goal: scroll kept edges and the done test apply only in the rooms of the section, and edges do not act on stale limits
- Done: scope, contract, plan, fix and owner play test.
- Next: none.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## YYYY-MM-DD

- 

### 2026-10-31

- The combined play test found a death at the end of a pan down from `basic_up`. Cause: every section in the map could count as done after
  a respawn, kept edges applied in every room, and the end of a pan left limits from the last room. The fix is in `SCOPE.md`.
- A mutation that removes the stale-room guard fails `CameraScrollTest`.
- `engine:test core:test lwjgl3:jar` passes after `clean`: engine 596, core 450, 0 failures, 96 unique warnings.

### 2026-11-01

- The owner answered the 3 manual checks and the merge question with "approved". `master` changed only in docs since this branch took it
  in, so no merge of `master` was necessary.
