# Process log: mega-refactor-061c-entity-queries

## Status

- State: planned
- Goal: callers ask the entity, not the player view: entity-level query functions, `targetEntity` for targeting, `MegaPlayerAccess.kt` deleted
- Done: draft `docs/SCOPE.md`
- Next: the owner reviews the draft scope (4 open questions)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master` (at `10927055f`).
- Scan: every function in `players/MegaPlayerAccess.kt` and its callers by receiver: `targetPlayer` (147 files; 423
  `body`, 23 `direction`, 15 `dead`, about 10 others; 6 uses need the view: `slot` ×2, `input` ×2, `present` ×1,
  `entity` ×1 plus the `inPlay` reads that already go through `entity`), the screens' and handlers' `player` (about 106
  uses), `players` loops (`it`, about 77). Name checks: `target` is declared in 36 files (rejected); `targetEntity` in
  none; the members `body`, `facing`, `direction` are declared in 354, 148 and 108 files. Draft `docs/SCOPE.md` written.
