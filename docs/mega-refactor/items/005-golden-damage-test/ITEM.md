# 005: Golden damage-table test

- Status: planned
- Type: test
- Depends on: `004`, `002`
- Related: `023`, `024`, `030`
- Branches: none yet

## Goal

Record today's damage results as a golden file, so that the damage redesign (`023`) can prove it changes nothing by accident.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- In the `smokeTest` suite: every receiver × damager variant (charge, size flag, dissipation 1..10) × difficulty × owner gives `canBeDamagedBy`, the health delta and the damage duration.
- Use only the `IDamageable` contract and health points, so the test survives the redesign.
- A sorted TSV golden file, a row diff on failure, and `-PupdateGolden=true` to regenerate.
- Commit the baseline on `master` before `023` starts. A changed row needs a contract note.

## Owner's input

Answer to review item 4 (2026-10-22, verbatim):

> X6 uses a lookup table for damage values. What are pros and cons of that approach versus the object-oriented override logic that's currently usedd in Maverick? What options do you propose? The Maverick approach already tries to use a bit of a "default lookup table" approach, but maybe the implementation can be cleaner?

## Decisions

- E (2026-10-22): `DarkSerketClaw` is `SMALL` on purpose, to tune its damage. Research: `size` is read in one place for the claw, `MegamanDamageNegotiator.kt:42`, which sets the damage the claw does to Mega (SMALL = 2, MEDIUM = 3). The damage the claw takes comes from its own `StandardDamageNegotiator()` (`DarkSerketClaw.kt:58`), which picks the table by the damager's size (`StandardDamageNegotiator.kt:152-166`), so `SMALL` has no effect there: the claw takes the MEDIUM values, and 0 from damagers that are not `ISizable`. The owner wants no change in either direction.
- B (2026-10-22): use a separate `smokeTest` JVM test suite in `core`.

## Open questions

- Which receiver states matter (shields, phases, boss "ready")?
- Add the damage duration (invincibility frames) as a column?

## Sources

- `NOTES.md` in this folder.
