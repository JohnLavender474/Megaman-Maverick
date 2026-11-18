# 079a Met drafts, round 2: sources

All frames are edits of the existing Met art in `assets/sprites/frames/Enemies1/Met/` (`Run.png`,
`GetUp.png`) and of the approved round 1 drafts (`../make_met.py`). The SnowMet frames are palette
copies of the Met frames, with the colors of `assets/sprites/frames/Enemies1/SnowMet/GetUp.png` and
`Run.png`. Edits made by Claude Opus 5.5, an AI model by Anthropic, directed by the developer.
Generator: `make_round2.py`. Previews: `preview.py`.

| New frame | Copied pixels | New pixels |
|---|---|---|
| `Met/Helmet.png` (32x16) | helmet: `Run.png` frame 1 rows 13-23, moved to rows 5-15, same x | none |
| `Met/Stunned.png` (4 frames) | head dome rows 21-23: round 1 `NoHelmetStand`; feet: `GetUp.png` rows 29-31 | black head rows 24-28, spiral eyes (turned 90 degrees each frame, right eye mirrored), 3 yellow stars on an ellipse (30 degrees each frame) |
| `Met/HelmetLand.png` frame 1 | helmet: `Run.png` frame 1 rows 13-23, at rows 15-25; feet: `GetUp.png` rows 29-31; hand: `Run.png` frame 1 row 25 | startled eyes (white with 1 px pupils), rows 26-28 |
| `Met/HelmetLand.png` frame 2 | helmet of `Run.png` frame 1, 1 row removed (row 21) and each row 2 px wider (pixel next to the outline repeated), at rows 17-26; feet: `GetUp.png` rows 29-31; hand: `Run.png` frame 1 row 25 | squint eyes and face, rows 27-28 |
| `Met/HelmetLand.png` frame 3 | `Run.png` frame 1 rows 13-26 moved down 2 px; feet: `GetUp.png` rows 29-31 | none |
| `Met/HelmetLand_alt_GetUp.png` | frames 1 and 2 as `HelmetLand`; frame 3 is `GetUp.png` | none |
| `SnowMet/*.png` | palette copy of each Met frame above | red mouth pixels, as in `SnowMet/Run.png` |

New color: star yellow `#F8B800` (248, 184, 0), the NES palette entry `0x28`, with a black outline.
