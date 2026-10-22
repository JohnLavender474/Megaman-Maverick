# 043: Lighter clone without a history rewrite

- Status: planned
- Type: tooling
- Depends on: none
- Related: `002`, `039`
- Branches: none yet

## Goal

Make a clone of the repo (`.git` is 1.6 GB) faster and smaller without a history rewrite.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Document a partial clone (`--filter=blob:none`) and a shallow CI checkout.
- LFS for new audio from now on. Renormalize in its own commit.
- Delete merged branches. Tag `overhaul_1` before deleting it.
- A history rewrite is out of scope. It needs a separate decision.

## Owner's input

Answer to review item 42 (2026-10-22, verbatim):

> yes, for sure

## Open questions

- Is `overhaul_1` still needed?
- Is a history rewrite ever acceptable (for example at a release boundary)?
- Is the GitHub LFS quota enough?

## Sources

- `NOTES.md` in this folder.
