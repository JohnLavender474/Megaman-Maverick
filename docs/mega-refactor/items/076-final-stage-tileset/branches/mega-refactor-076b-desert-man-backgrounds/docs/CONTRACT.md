# Contract: mega-refactor-076b-desert-man-backgrounds

- Status: approved
- Approved: 2026-11-17

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule` passes.
- [ ] The 12 regions that stay in `Backgrounds6` keep their names and pixels; `Backgrounds7` has exactly
  `SunriseHills`, `SunsetHills` and `Desert/SunSky`, equal to the accepted drafts; no region is rotated (script).
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check runs as
  written. No game launch.

## Manual playthrough

Experimental checks first, then control checks.

| Level | Room | What to check |
|-------|------|---------------|
| `DESERT_MAN` | `room1` | The dawn sky (`SunriseHills`): stars twinkle, cloud lines move, no sun. Run left and right: no seam between the copies. |
| `DESERT_MAN` | `room13` | Go right into the dark `room14` and on into `room15`: the darkness clears on the sunset. (The swap happens while `room14` comes into view, under the darkness, so no hill jump can show.) |
| `DESERT_MAN` | `room15` | The sunset: the sun with heat haze, the clouds, the silhouette over the sky. |
| `DESERT_MAN` | `room3` | Climb up to `room5` and on into `room6` (the triggers at x 208 and 213): `SunriseHills` stays, and the sunset does not show. |
| `MOON_MAN` | `room1` | Control: the `Space` background as before. |
| `INFERNO_MAN` | `room1` | Control: the `InfernoBKG` background as before. |
| `PRECIOUS_WOMAN` | `room1` | Control: the `CrystalBKG` background as before. |

## Behaviors to check

- No gameplay change.

## Intended changes

- The 3 Desert Man backgrounds look new; `SunriseHills` is animated (8 frames, 0.2 s); `DesertSunSky` has 8 frames at
  0.15 s in place of 3 at 0.1 s.
