# Process log: mega-refactor-029g-character-def

## Status

- State: planned
- Goal: `CharacterDef` with a movement values table and an ordered ability list; `Megaman` reads its values and builds its behaviors from it
- Done: scan; draft scope
- Next: the owner approves `docs/SCOPE.md`; then `docs/CONTRACT.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-25

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `ab5f8cd31`. Findings and open questions in `docs/NOTES.md` ("Scan"). No scope yet.

- The owner accepted the scan recommendations 1-9 and the core placement of `CharacterDef`. Decisions in `docs/NOTES.md`. Draft `docs/SCOPE.md` written.
- The owner asked about the values table; decision: no table, behavior factories take their values as parameters, numbers stay in `MegamanValues` (`docs/NOTES.md`, "Decisions"). `docs/SCOPE.md` rewritten. One open question: the package of `CharacterDef`.
