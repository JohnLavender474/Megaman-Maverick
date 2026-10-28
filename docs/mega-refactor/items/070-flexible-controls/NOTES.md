# Notes: 070 More flexible controller settings

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-28: starting facts (`master` at `1afc7d565`)

- `SelectButtonAction` (`core/.../controllers/SelectButtonAction.kt`): `NEXT_WEAPON`, `AIR_DASH`, `START`. It is
  read in Megaman's `ControllerComponent.kt:100`, `AirDashBehavior.kt:33-41` and the pause check in
  `MegaLevelScreen`. It is saved per device file now (`ControllerUtilsExtensions.kt:79-95`); `031e` moves it to the
  per-slot settings.
- The controller scan for item `069` (button list, loops over all entries, controls screen layout) is in `069`
  `NOTES.md`.
