# 024: One `WeaponDef` per weapon; loadout per character

- Status: planned
- Type: refactor
- Depends on: `029`
- Related: `007`, `023`, `025`, `040`
- Branches: none yet

## Goal

Put the data for each weapon in one place, instead of `when` blocks in at least 8 files.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `WeaponDef` holds cooldown, costs, charge rules, fire behavior, HUD, icon and animation keys, ability blocks and damage tags.
- A registry with stable ids. The HUD, pause grid and save use the loadout.
- Remove the weapon `when`s from the handler, behaviors, controller, HUD and pause screen.
- Rewards come from the level manifest through `CharacterDef.unlocks`.

## Owner's input

Answer to review item 23 (2026-10-22, verbatim):

> yes

## Open questions

- Is progress (bosses beaten) shared across characters, as in X6?
- Can two characters share a `WeaponDef` with different sprites?
- Must saved weapon ids stay readable from the old save?

## Sources

- `NOTES.md` in this folder.
