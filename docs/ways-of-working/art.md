# Art

Read this file before you draw, fix or convert art (sprites, tiles, backgrounds). Atlases and packing are in
`assets.md`.

- **NES pixel size and NES palette** (owner, 2026-11-22, branch `078e`: round-4 art had pixels smaller than an NES
  pixel, and no color from the NES palette). New and changed art looks like NES graphics:
  - **Pixel size.** One NES pixel is 2x2 game px: the view of 16x14 tiles of 32 px is the NES screen of 256x224,
    doubled. Art that the game draws at 1 art px per game px (tiles, backgrounds, a sprite drawn at its frame size,
    for example a 32 px frame on 1 tile) is drawn on a 2 px grid: every aligned 2x2 block is one color. Art that the
    game draws at 2 game px per art px (a 32 px frame on 2 tiles, as most entities) uses single art px. No detail is
    finer than one NES pixel: not an edge, a highlight, a glint arm or a dither.
  - **Palette.** Every color is from the NES (2C02) master palette:

    ```text
    000000 7C7C7C BCBCBC F8F8F8 FCFCFC 787878
    0000FC 0000BC 4428BC 940084 A80020 A81000 881400 503000 007800 006800 005800 004058
    0078F8 0058F8 6844FC D800CC E40058 F83800 E45C10 AC7C00 00B800 00A800 00A844 008888
    3CBCFC 6888FC 9878F8 F878F8 F85898 F87858 FCA044 F8B800 B8F818 58D854 58F898 00E8D8
    A4E4FC B8B8F8 D8B8F8 F8B8F8 F8A4C0 F0D0B0 FCE0A8 F8D878 D8F878 B8F8B8 B8F8D8 00FCFC F8D8F8
    ```

  - **Sprites** (entities): each 8x8 NES-px area of a frame uses one sprite palette of at most 3 colors plus
    transparent. An entity uses at most 2 sprite palettes.
  - **Background tiles:** each 16x16 NES-px area (one 32 px tile) uses one background palette of 3 colors plus the
    backdrop color, also where a decor layer draws over the tile. Each area of a stage (what one screen shows) uses
    at most 4 background palettes, which share one backdrop color. A room can change the palettes of the next area.
  - **Parallax backgrounds** follow the background-tile rule in each 16x16 NES-px area of each texture, and their
    colors count toward the 4 background palettes of the area.
  - **No rotated pixels** (owner, 2026-11-22, branch `078e`: the lasers and laser beamers turned their pixels, which
    NES art never does). The game turns new art only in 90-degree steps (or flips it), so that each pixel stays
    square and aligned with the screen grid. Do not plan new art on a free-angle turn of its sprite. For an object at
    another angle, draw a frame for each angle (for example the 8 directions at 45 degrees, drawn as pixel art with
    stepped edges), or build the shape from unrotated parts. Much old art breaks this rule; it keeps its look until a
    task changes it.
  - **Check by script** in each art round: the 2 px grid, the palette and the color counts. Old art keeps its look
    until a task changes it; do not redo old art only for this rule.

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
  - **Inspired by:** when the new art copies no pixels but takes its look from other art (for example AI-made art
    drawn after old art of the game), credit the maker and add an "Inspired by" line for each source. Use it where
    the new art clearly follows a source, also when the AI made 100% of the pixels:

    ```markdown
    - Desert Man Backgrounds
      - Made by Claude Fable 5.2, an AI model by Anthropic, directed by the developer
      - Inspired by [<artist>](<link>) ([<work>](<link>)): the hills
    ```

  - **When:** in the branch that adds the art to the game, before its merge. The brief or the contract lists the
    credit lines. A draft in `temp/` needs no credit until it goes into the game.
  - Keep a record of the source of each new piece of art (the files or the pack it builds on) in the branch docs,
    so that the credit can be checked later.

  Example (owner, 2026-11-17, after `076a`): the new Desert Man tileset builds on the Mega 8-Bit Pixel Pack by Super
  Icon Ltd, and Claude Fable 5.2 redrew and refined it. The README credits both under the Super Icon Ltd entry. The
  new Desert Man backgrounds of `076b` are AI-made and inspired by the old ones: the hills by Super Icon Ltd and the
  sunset by Capcom. The README credits them with "Inspired by" lines.

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
- **Match the model to the art task.** Every art task with an artistic choice goes to Claude Fable 5.2: new art,
  redraws, and also recolors and reskins (the choice of a palette and how it reads is artistic). A lighter model
  (Claude Sonnet 5.5) does only purely mechanical steps, for example applying a color map that the Fable agent planned,
  repeating a fixed edit over many frames, or running checks. The Fable art agent can plan the palette and hand its
  application to a Sonnet subagent to save Fable tokens, then review the result (owner, 2026-11-18, branch `078a`: the
  Sonnet-only enemy reskins did not look good). The credit names the model that made the artistic choices.
- **Art goes to an art agent.** The main session does not draw, recolor or redraw art itself. It writes a brief and
  hands the art to one art subagent, which can delegate parts to other agents (with the model of the rule above).
  The main session keeps the code, the packing, the maps and the checks (owner, 2026-11-18, branch `078a`).
- **Art work has its own item or branch.** Do art in an item or branch made for it, not as a side task of a code
  branch (owner, 2026-11-19).
- **Confirm Fable once per item or branch (temporary).** Before the first Fable agent of an item or branch, warn the
  owner once that the work uses Fable and ask for confirmation with `AskUserQuestion`. Later Fable agents of the same
  item or branch need no new confirmation. This rule holds while the owner is on the $20/month plan, and the owner
  lifts it after the upgrade back to the $100/month plan (owner, 2026-11-19).
- **A fresh art agent for each round.** Each round of owner feedback goes to a new art subagent, not to the agent of
  the last round. The images of each round stay in the context of an agent and make each later request more costly
  (one agent reached about 727k tokens over many rounds). Give the new agent the paths of the current frames, the
  owner's notes of the round and the short notes of the last rounds from the task's temp folder (owner accepted the
  recommendation, 2026-11-19).
