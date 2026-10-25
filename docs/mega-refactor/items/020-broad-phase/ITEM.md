# 020: Cheaper broad phase with stable contact order

- Status: planned
- Type: refactor
- Depends on: none
- Related: `006`, `010`, `014`, `055`
- Branches: none yet

## Goal

Stop rebuilding the physics container and fixture shapes about 3.5 times per frame, with no change in contact order.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Cache fixture shapes inside the contact-collection phase (lowest risk, first).
- Keep immobile bodies in a persistent layer, decided by an immobile flag, not by `BodyType.STATIC` (some STATIC blocks move).
- Test each pair once, but keep the current callback order and orientation.

## Owner's input

Answer to review item 19 (2026-10-22, verbatim):

> yes

## Decisions

- 2026-10-25: two test suites. A temporary suite runs a frozen copy of today's `WorldSystem` side by side with the
  new one and is deleted before the merge. A permanent suite checks a stable contact order in the new code.
- 2026-10-25: the work is necessary because of the weak-hardware goal. A measurement shows the gain; it is not a
  gate.
- 2026-10-25: Option C (small clean-ups) is agreed. The recommended first branch is Options A, B and C. Option D
  (an incremental grid) comes later, in its own branch. See `NOTES.md`, "Options" and "Owner's feedback and
  follow-up".
- 2026-10-25: not now. The owner put this item on hold to work on more pressing items. The scan and the options in
  `NOTES.md` stay valid until the code changes; check the line numbers again before the work starts.

## Open questions

- Is there a list of "moving static" blocks, or may an `immobile` flag be added in Tiled? Answered by the scan
  (2026-10-25): no list exists, and no flag is needed for Options A, B, C or D4. See `NOTES.md`.

## Sources

- `NOTES.md` in this folder.
