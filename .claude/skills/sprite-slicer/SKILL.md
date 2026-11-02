---
name: sprite-slicer
description: Use when a sprite sheet has tightly packed frames with no constant offset or size, for example art from an artist, and the frames must become fixed-size animation strips or a game atlas. Also use to rename, align or fix frames in a strip that came from a sheet in utils/sprite_slicer/sheets/.
---

# Sprite Slicer

`utils/sprite_slicer/` cuts a sheet into strips from a YAML data file. The tool finds and places
frames. It does not know what a frame is, so names, grouping and the alignment check are your work.
`utils/sprite_slicer/README.md` gives the data format. Read it first.

## Steps

1. **Copy the sheet** to `utils/sprite_slicer/sheets/`. Files outside `assets/` stay out of the game.
2. **List what the tool detects.** `run.sh` or `run_tests.sh` makes the `.venv`. From `utils/sprite_slicer/`:

   ```bash
   .venv/bin/python3 -c "
   import sys, sprite_slicer as s
   sheet = s.load_sheet(sys.argv[1])
   for row, band in enumerate(s.find_rows(sheet), 1):
       print(row, band, [(l, t, r - l + 1, b - t + 1) for l, t, r, b in s.find_frames(sheet, band)])
   " sheets/<sheet>.png
   ```

   Each tuple is left, top, width, height. Also read each row at 3x zoom.
3. **Get the names from the owner.** Show your reading of each row and ask. Mark a pose that the
   owner cannot name as `r<row>_unknown`. Use the names in `assets/sprites/frames/Megaman_v3/` where
   the same animation exists.
4. **Fix the detection in `rows`.** Tiny or narrow spans can be detached parts of one frame, such as
   sparks on both sides of a beam: compare them with the zoomed row, and use `merge`. A span wider
   than the cell is usually touching frames: use `split_at`. Print the span as text (`#` and `.`) to
   find the column. When feet permit more than one column, use the symmetry of the effect that
   touches. `split_at` runs first, so `merge` indexes count the spans after the split.
5. **Write the data file** in `utils/sprite_slicer/sheets/`. Set `align_to` to the idle strip, and
   give that strip `align_to: null`. If the sheet has no idle strip, omit `align_to`. Use `twin` for
   each shoot strip. Use `align_x: center` for
   effects and for symmetric frames.
6. **Run to a temporary preview folder**, with no `--atlas-dir`. Then do "Check the alignment".
7. **Pack** with `--atlas-dir` when the strips are correct. Add a `TextureAsset` entry only when code
   reads the atlas.

## Check the alignment

The tool gives no error for a bad alignment. Look at `preview.png`, then measure each strip:

- **Ground line:** `anchor_y: bottom` uses the lowest pixel of any kind, so a glow, spark or burst
  below the feet lifts the body. In each frame with visible feet, find the lowest row of the outline
  color. Frames with different values have a fault. Compare with the sheet: if the feet are on one
  sheet line, keep that relation with `offsets`. For a frame with no feet, such as a beam, use the
  sheet line of the frames next to it. An offset is relative to the anchored position, so calculate
  `y` from the bottom of the frame on the sheet.
- **Sideways jump:** in all strips, and mostly with `align_x: center`, where an extended arm moves
  the box. For each neighbouring pair, shift one frame by -7 to 7 pixels in x and y, and count the
  equal pixels. Best shift (0, 0) is correct. When the pose changes, the head can win the count, so
  compare the pixels of the planted foot.
- **Duplicates:** two frames with all pixels equal are a fault in placement, or a question for the
  artist. Compare them with the sheet.
- **Between strips:** use the order of play, for example `stand` to `run_trans`, and `victory` to
  `spawn_out`. Check that the last frame of one strip meets the first frame of the next strip.
- **Direction:** compare each shoot strip with the Megaman strip of the same name. If the arm points
  the other way, report it, because the code must flip that strip.

## `offsets` rules

- The index is the frame in the strip, not in the sheet row.
- `overlap` runs before the offset. The next frame aligns to the moved frame.
- The `overlap` search stops at the cell edge and compares with one reference frame.
- A cell cannot show pixels below `y=47`, and a strip cannot have its own cell size. When an effect
  goes below the feet, keep the frame that joins another strip on `y=47`, let the effect frames rise,
  and report it. Do not cut the effect. The owner chooses the fix.

## Report to the owner

Give the strip and frame counts, each decision, each fault you did not fix, and which art is missing.
Say that the strips are not tested in the game until code reads them.
