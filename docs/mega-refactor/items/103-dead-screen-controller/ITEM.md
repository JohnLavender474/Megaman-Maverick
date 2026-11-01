# 103: Remove the dead `ScreenController` path or give it a real use

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `031`, `035`, `039`
- Branches: none yet

## Goal

`screenController` in `MegamanMaverickGame.kt` has no assignment, so `ScreenController` never runs. Remove the class, the field, `showScreenController` and `--showScreenController`, or give the overlay a real use.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- The `TODO` for mobile in `ScreenController` is the link to the mobile work. `035` plans touch controls in the side bars, so read `035` first.

## Owner's input

Answer to `031f` scope question 9 (2026-11-01, verbatim: "scope approved"): `031f` changed the 1 read to a frame, and this item takes the rest.

## Open questions

1. Does the overlay stay for a mobile build, or does `035` write a new one?

## Sources

- `NOTES.md` in this folder.
- `items/031-input-frames/branches/mega-refactor-031f-menu-input/docs/SCOPE.md`, question 9.
