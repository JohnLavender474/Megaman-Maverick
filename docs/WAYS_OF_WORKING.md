# Ways of working

How work on this repo is done: the owner's preferences for the approach to a task, as opposed to how the code looks.
Read this file before you start a task, and give it to each subagent that does one.

How Kotlin code looks and is shaped is in `docs/CODE_STYLE.md`. The process of refactor branches (planning, briefs,
reviews, merges) is in `docs/mega-refactor/CLAUDE.md`.

## Art

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
