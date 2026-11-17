# Art

Read this file before you draw, fix or convert art (sprites, tiles, backgrounds). Atlases and packing are in
`assets.md`.

- **Fix art in the art, not in code.** When a sprite frame sits wrong (a pose a few pixels off center, a frame that
  is not aligned with the frames next to it), move the pixels in the sprite files: the frame strip in
  `assets/sprites/frames/`, the packed atlas in `assets/sprites/sprite_sheets/`, and the slicer data file in
  `utils/sprite_slicer/sheets/` when the slicer makes that strip. Do not add a draw offset in code for it. A code
  offset hides the fault in the art from the next reader, and every other user of the frame must repeat it.

  A draw offset in code stays correct where the shift comes from game state and not from the art, for example the
  rotation of a sprite with the gravity direction.

  Example (owner, 2026-11-12, branch `081g`): X sat 1 to 2 px right of the ladder while he climbed. The fix moved the
  `climb` and `climb_shoot` frames 2 px left in the frame strips and in the atlas, not a `translateX` in the sprite
  pre-process.

- **Trace X6 poses; do not design them.** When you redraw an X6 pose for a Maverick sheet, take the limb shapes from
  the X6 frame, shrunk to the sheet's scale, and change only the style: flat artist colors, a black outline, a black
  sole, the artist's cuff lines. Reuse the artist's own parts (the head, the `stand` legs) where the pose matches.
  Compare each frame next to the X6 frame at the same scale. Draw outlines so that each edge steps at most 1 px per
  row. (Hand-made leg shapes in the sabre work, 2026-11-05, came out "super obese"; legs traced from X6 were "far
  better".)
- **A small X6 sprite may be larger than X6.** Ratio 2 is the default for a converted sprite (the map and the
  bosses). When ratio 2 makes a small enemy look squished or too low on detail, propose a smaller ratio (for example
  1.6) for that object only, and scale its body, offsets and shots by the same factor (owner, 2026-11-03, Junkroid and
  Hover Gunner). In the demake tools, record the ratio in the object's `catalog/nessprite-<name>.json`.
