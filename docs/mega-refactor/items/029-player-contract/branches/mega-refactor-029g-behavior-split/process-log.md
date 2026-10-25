# Process log: mega-refactor-029g-behavior-split

## Status

- State: planned
- Goal: split the player behaviors into 8 factory functions with no change in their bodies; tests fix the order
- Done: scan; scope approved
- Next: the owner approves `docs/CONTRACT.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-25

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `ab5f8cd31`. Findings and open questions in `docs/NOTES.md` ("Scan"). No scope yet.

- The owner accepted the scan recommendations 1-9 and the core placement of `CharacterDef`. Decisions in `docs/NOTES.md`. Draft `docs/SCOPE.md` written.
- The owner asked about the values table; decision: no table, behavior factories take their values as parameters, numbers stay in `MegamanValues` (`docs/NOTES.md`, "Decisions"). `docs/SCOPE.md` rewritten. One open question: the package of `CharacterDef`.
- Independent review of the draft scope (`docs/NOTES.md`, "Review"). The owner chose the smaller cut: split the behaviors only, with the order and two-instance tests; `CharacterDef` goes to a Proto Man item (backlog candidate in `docs/TODO.md`). `docs/SCOPE.md` rewritten. On `master`: "`CharacterDef` design" added to the item `NOTES.md`; decision added to `ITEM.md`; the Proto Man open question removed from it.
- The owner approved `docs/SCOPE.md` and the rename of the branch from `mega-refactor-029g-character-def` to `mega-refactor-029g-behavior-split` (folder, `ITEMS.md` row, `ITEM.md` branch split).
- Draft `docs/CONTRACT.md` written.
