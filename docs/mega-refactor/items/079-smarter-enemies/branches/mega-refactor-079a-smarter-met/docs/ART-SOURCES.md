# 079a Met drafts: sources

All new frames are edits of the existing Met art in `assets/sprites/frames/Enemies1/Met/`
(`Run.png`, `GetUp.png`). The SnowMet frames are palette copies of the new Met frames, with the
colors taken from `assets/sprites/frames/Enemies1/SnowMet/Run.png`. Edits made by Claude Opus 5.5,
an AI model by Anthropic, directed by the developer. Generator: `make_met.py` (this folder).

| New frame | Copied pixels | New pixels |
|---|---|---|
| `Met/HelmetUpSmall.png` | helmet: `Run.png` frame 1 rows 13-23, moved up 7 px; hand, lower face: `Run.png` frame 1 rows 25-26; feet: `GetUp.png` rows 29-31 | head dome, wide open eyes |
| `Met/HelmetUpLarge.png` | same as `HelmetUpSmall`, helmet moved up 14 px (top at row 1) | head dome, wide open eyes, 2 raised hands |
| `Met/Jump.png` | helmet and face: `Run.png` frame 1 rows 13-26; left foot: `Run.png` frame 1; right foot: `Run.png` frame 2 | joined row of the 2 raised feet |
| `Met/NoHelmetRun.png` | face and feet: `Run.png` frames 1 and 2 rows 24-31 | head dome, slanted eye tops (angry) |
| `Met/NoHelmetStand.png` | face: `Run.png` frame 1 rows 24-26; feet: `GetUp.png` rows 29-31 | head dome, angry eye tops |
| `Met/NoHelmetJump.png` | face: `Run.png` frame 1 rows 24-26; feet as `Jump.png` | head dome, angry eye tops |
| `Met/helmet_piece1..4.png` | helmet of `Run.png` frame 1 rows 13-23, cut in 4 | black outline on the cut edges |
| `SnowMet/*.png` | palette copy of each Met frame above | red mouth pixels, as in `SnowMet/Run.png` |
