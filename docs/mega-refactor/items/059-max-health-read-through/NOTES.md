# Notes: 059 Max health read through from the profile

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## From `029e` (2026-10-24, on `eca1b3571`)

- The only writer of Megaman's max health is `Megaman.setMaxHealthFromHeartTanks`, called from `onAddHeartTank` and `onRemoveHeartTank`. `Megaman.definePointsComponent` sets the start value (`START_HEALTH`).
- `Points` (`engine/.../points/Points.kt`) stores `min`, `max` and `current` as fields.
- `MegamanHeartTankTest` checks the max after heart tanks are added and removed.
