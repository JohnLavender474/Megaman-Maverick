# 079a Met drafts, round 3: sources

All frames are edits of the approved Met frames in the worktree
`.worktrees/mega-refactor-079a-smarter-met/assets/sprites/frames/Enemies1/Met/` (`NoHelmetRun.png`,
`NoHelmetStand.png`, `HelmetLand.png`, `Helmet.png`). The SnowMet frames are palette copies: orange becomes
(12, 63, 186), the mouth pixels become red (225, 28, 28), at the same places as in the approved SnowMet frames.
`SnowMet/WorriedRun.png` frame 2 uses the lighter blue (50, 93, 221), as the approved `SnowMet/NoHelmetRun.png`
frame 2 does. Edits made by Claude Opus 5.5, an AI model by Anthropic, directed by the developer.
Generator: `make_round3.py`. Previews: `preview.py`. The unflipped Met art faces right (`Met.kt` flips the sprite when the Met faces left).

| New frame | Copied pixels | New pixels |
|---|---|---|
| `Met/WorriedRun.png` frames 1-2 | all of `NoHelmetRun.png` frames 1-2 (head dome, highlight, hand, mouth, run feet) | worried eyes rows 22-26 (white tops slant up to the inside, 1 px pupils); sweat drop 5x5 behind the head at x3, row 16 (frame 1) and row 17 (frame 2) |
| `Met/Pickup.png` frame 1 | `NoHelmetStand.png` (dome rows 21-24 moved 1 px right, toward the front; feet; back hand) | worried eyes looking down-right (rows 24-28); small helmet 12x7 on the floor in front (x20-31, rows 25-31), drawn after the shape of `Helmet.png`; glove on the helmet (x22-25, rows 24-27) |
| `Met/Pickup.png` frame 2 | `NoHelmetStand.png`; helmet: `Helmet.png` rows 5-15 at rows 9-19 | round eyes looking up; 2 gloves that hold the helmet sides |
| `Met/Pickup.png` frame 3 | `HelmetLand.png` frame 3, no change | none |
| `Met/Cry.png` frames 1-2 | `NoHelmetStand.png` (dome, feet, mouth) | shut `> <` eyes rows 24-28; hand removed; open mouth (2 more mouth pixels at row 28); tear streams beside the eyes (3 px in frame 1, 2 px in frame 2); tear drops 4x4 that swap height left and right between frames |
| `SnowMet/*.png` | palette copy of each Met frame above | red mouth pixels |

New color: tear and sweat light blue `#3CBCFC` (60, 188, 252), the NES palette entry `0x2C`, with a black outline
on the drops. The 1 px tear streams on the black face have no outline.
