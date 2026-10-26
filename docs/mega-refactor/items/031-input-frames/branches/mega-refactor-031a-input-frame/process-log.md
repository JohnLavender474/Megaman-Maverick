# Process log: mega-refactor-031a-input-frame

## Status

- State: planned
- Goal: one `InputFrame` per player slot, built from today's poller once per frame; all gameplay input reads move to it
- Done: `docs/SCOPE.md` (approved); draft `docs/CONTRACT.md`
- Next: the owner reviews `docs/CONTRACT.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-25

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `48c602ed4`: item `031` `NOTES.md`, "Scan: the size of the work (2026-10-25)" and "Options for the
  first branch". The owner chose Option 2 (the frame and all gameplay reads). Draft `docs/SCOPE.md` written.
- The owner approved `docs/SCOPE.md` with the proposals for the 3 open questions (value class, store in
  `MegaPlayers`, no temporary suite in `031a`). Decisions in `docs/SCOPE.md` and `docs/NOTES.md`. Draft
  `docs/CONTRACT.md` written; `docs/NOTES.md` lists where each moved reader can be checked in the game.
