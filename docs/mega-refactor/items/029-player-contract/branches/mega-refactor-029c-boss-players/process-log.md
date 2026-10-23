# Process log: mega-refactor-029c-boss-players

## Status

- State: planned
- Goal: Bosses, `AbstractBoss` and `AbstractEnemy` reach the player through the players service; the boss damage listener uses the player view
- Done: scan; `docs/SCOPE.md` approved; draft `docs/CONTRACT.md`.
- Next: the owner reviews `docs/CONTRACT.md`.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-23

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `01d0947e7`. Draft `docs/SCOPE.md` written.
- The owner answered the open questions (shared state in the component; settable view properties, named locks recorded but not built; `players.all` for the boss intro landing checks with `TODO`s; listener renamed; old bosses commented out, not deleted). Scope updated; `047` notes the landing-check edge case.
- Scope approved by the owner. Draft `docs/CONTRACT.md` written (baseline after the `master` cleanups: 470 engine, 107 core tests).
