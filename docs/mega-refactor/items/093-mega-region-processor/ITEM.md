# 093: Use or remove `MegaRegionProcessor`

- Status: idea
- Type: refactor
- Depends on: none
- Related: `024`, `084e`
- Branches: none yet

## Goal

Give `MegaRegionProcessor` an implementer, or remove it. `MegaRegionProcessor` and the `regionProcessor` parameter of `MegamanAnimations` have no implementer, so the code path at `MegamanAnimations.kt:121` never runs.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- If `024` uses the magic-pixel weapon spawn idea, `024` implements the interface. See the `024` `NOTES.md`.
- If `024` does not use it, remove the interface and the parameter.

## Owner's input

## Open questions

- Does `024` use the weapon spawn pixels?

## Sources

- `NOTES.md` in this folder.
- `items/084-split-megaman/branches/mega-refactor-084e-class-line/docs/TODO.md`, "Backlog candidates".
