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

## Open questions

- Which receiver states matter (shields, phases, boss "ready")?
- Is the size lookup in `StandardDamageNegotiator.get(damager)` intended? It gives `DarkSerketClaw` the MEDIUM values. (Decision E.)
- Add the damage duration (invincibility frames) as a column?

## Sources

- `NOTES.md` in this folder.
