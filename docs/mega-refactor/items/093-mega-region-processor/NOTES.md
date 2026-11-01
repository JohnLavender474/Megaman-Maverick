# Notes: 093 Use or remove `MegaRegionProcessor`

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## Scan

2026-11-01, `master` at `647c24b56`. `M/` = `core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/`.

- `M/sprites/MegamanAnimations.kt:23`: the parameter `regionProcessor: MegaRegionProcessor? = null`.
- `M/sprites/MegamanAnimations.kt:121-122`: the only use, which runs only if the parameter is not `null`.
- `M/sprites/MegamanAnimations.kt:224`: the interface.
- `core/src/main/kotlin` has no other match for either name.
