# 023: Damage profiles keyed by damage tags

- Status: planned
- Type: refactor
- Depends on: `005`
- Related: `021`, `022`, `024`, `030`
- Branches: none yet

## Goal

Replace class-keyed damage overrides in many files with one table of named profiles, and handle dynamic damage (lemon, half and full charge) explicitly.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Damagers emit `DamageSource(tag, elements, magnitude, owner)`. Variants are separate tags (`buster.lemon`, `buster.half`, `buster.full`, ...), as X6 uses weapon ids 0, 9 and 18.
- Named, inheritable profiles (`small`, `medium`, `large`, `boss`, shared specials) in one Kotlin DSL file. A per-entity profile and deltas in the entity schema. A state change swaps the profile.
- An ordered modifier pipeline: enhancement, difficulty, element rules, cap.
- One optional `DamageRule` escape hatch per entity, listed in the snapshot. Reactions (stun, freeze) key on tags, not classes.
- Delete `SelfSizeDamageNegotiator`, `CustomMapDamageNegotiator` and `damageOverrides`. Export the grid to CSV or Markdown.

## Owner's input

Answer to review item 4 (2026-10-22, verbatim):

> X6 uses a lookup table for damage values. What are pros and cons of that approach versus the object-oriented override logic that's currently usedd in Maverick? What options do you propose? The Maverick approach already tries to use a bit of a "default lookup table" approach, but maybe the implementation can be cleaner?

Answer to review item 22 (2026-10-22, verbatim):

> yes, this would go a long way in reducing duplication and hard-coupling to entity classes; furthermore, some damage logic is dynamic, i.e. when a buster shot is a lemon versus charged, so the proposed design needs to account for that.

## Open questions

- Is the damager-size lookup for `DarkSerketClaw` and `BigAssMaverickRobot` intended? (Decision E.)
- X6-style "deflect" and "pass through" as table outcomes?
- Should enemy shots ever damage enemies (friendly fire)?
- Will modders edit damage? If yes, plan a JSON export.

## Sources

- `NOTES.md` in this folder.
