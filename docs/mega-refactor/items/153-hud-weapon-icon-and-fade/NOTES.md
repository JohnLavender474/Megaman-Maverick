# Notes: 153 A current-weapon icon on every player's HUD, and a HUD that fades when a player is behind it

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## HUD scan (2026-11-13, `060c` worktree, based on `master` `a6970cd59`)

- `screens/levels/stats/PlayerStatsHandler.kt` draws the HUD for slot 0 only (a `TODO` names `047`).
- The health bar works for any `IPlayer`.
- The weapon bar shows only for a Mega Man special weapon (`weaponBarSupplier` returns `null` for a player that is
  not `Megaman`, and for `MEGA_BUSTER`). There is no weapon icon.
- The pause screen (`screens/menus/level/LevelPauseScreen.kt`) shows Mega Man's weapon grid for every player.
