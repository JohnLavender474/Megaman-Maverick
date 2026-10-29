# 079: Smarter behavior for simple enemies

- Status: idea
- Type: feature
- Depends on: none
- Related: `015`, `074`
- Branches: none yet

## Goal

Some enemies have very simple behavior. Make them smarter, so that they give more interesting gameplay.

Examples from the owner:

- `Starkner`: he only runs toward Megaman, with a delay between each charge.
- `Met`: he can be smarter, for example he does not run into a bottomless pit.

This item changes gameplay on purpose. It is not a refactor.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A list of the enemies with too simple behavior, starting with `Starkner` and `Met`. The owner chooses which ones
  to change.
- A new behavior for each chosen enemy.
- For `Met`: a ledge check (for example a sensor in front of his feet) so that he stops or turns at the edge of a
  bottomless pit. Other walking enemies can use the same check if two or more need it.

## Owner's input

2026-10-29 (verbatim):

> Certain enemies are overly simplistic in behavior. Starkner is an example: all he does is run toward Mega Man with a
> delay between each charge. Met is another example: he could be a bit smarter by avoiding running into bottomless
> pits.

## Open questions

- Which other enemies are on the list?
- Does a `Met` stop at every ledge, or only at a bottomless pit (a `Death` object below)?
- A change to `Met` affects every level that has a `Met`. Do some levels depend on a `Met` that falls?
- Do the new `Starkner` sections of `074` come after this item?

## Sources

- `NOTES.md` in this folder.
