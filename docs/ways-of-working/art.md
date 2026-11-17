# Art

Read this file before you draw, fix or convert art (sprites, tiles, backgrounds). Atlases and packing are in
`assets.md`.

- **Credit the original and the update.** When new art in the game builds on existing art (a tileset, a sprite sheet
  or a background used as the premise), the `CREDITS` section of the root `README.md` names both, with the pattern
  that it already uses:

  ```markdown
  - Desert Man
    - Original by [<artist>](<link>) ([<work>](<link>))
    - Updated by <who made the update>
  ```

  - **Original:** the artist and the work that the new art builds on, with links. When the work is a rip of
    commercial art (for example a Spriters Resource sheet of a Capcom game), credit the ripper and the work as the
    README does for the other rips. When the source is unknown, ask the owner; do not guess an artist.
  - **Update by AI:** name the model with its full name and version (for example "Claude Fable 5.2, an AI model by
    Anthropic"), and say that the developer directed it. When a person and an AI made the update together, name
    both. Do not present AI-made art as the work of a human artist, and do not leave out the original artist
    because the AI changed every pixel.
  - **When:** in the branch that adds the art to the game, before its merge. The brief or the contract lists the
    credit lines. A draft in `temp/` needs no credit until it goes into the game.
  - Keep a record of the source of each new piece of art (the files or the pack it builds on) in the branch docs,
    so that the credit can be checked later.

  Example (owner, 2026-11-17, after `076a`): the new Desert Man tileset builds on the Mega 8-Bit Pixel Pack by Super
  Icon Ltd, and Claude Fable 5.2 redrew and refined it. The README credits both under the Super Icon Ltd entry.

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
