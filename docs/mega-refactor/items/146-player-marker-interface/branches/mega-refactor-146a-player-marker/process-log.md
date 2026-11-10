# Process log: mega-refactor-146a-player-marker

## Status

- State: active
- Goal: A marker `IPlayer` in the engine and `IMegaPlayer` in core; player call sites take the player type
- Done: branch start
- Next: scan, `SCOPE.md`, `CONTRACT.md`, brief
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-10

- Branch start. Owner decisions (in `ITEM.md`): the marker design, merge directly into `master`, start now in
  parallel with `060a` and `081f`, and `Event.isDeathOf` changes here.
