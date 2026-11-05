# Process log: mega-refactor-118g-remove-old-flow

## Status

- State: planned
- Goal: remove the old boss flow (screen branches, route, guards, 3 events, and the parts with no caller left)
- Done: `SCOPE.md` approved, draft `CONTRACT.md`
- Next: owner approval of `CONTRACT.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-05

- No branch exists yet. Scan of `master` at `373923c8e`: every use of the 6 boss events, the guards, `FightDirector`, the hooks and their callers, `BossSpawnEventHandler`, the screen events test, the `fights` README, and the boss rooms of the 26 maps that `LevelDefinition` names (25 rooms, each boss with a plan). The owner chose to remove the hooks that lose their only caller. Draft `SCOPE.md`.
- The owner approved `SCOPE.md`. Draft `CONTRACT.md`, with no experimental check and 3 control checks.
