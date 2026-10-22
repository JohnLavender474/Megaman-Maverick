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

## Open questions

- Is there a list of "moving static" blocks, or may an `immobile` flag be added in Tiled?

## Sources

- `NOTES.md` in this folder.
