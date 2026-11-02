# Sprite slicer

Cuts a tightly packed sprite sheet into fixed-size animation strips, then packs an atlas in the
format of the game atlases. A data file names the frames. The tool never guesses a name.

```bash
./run.sh sheets/MegamanX.yaml \
    --frames-dir ../../assets/sprites/frames/MegamanX \
    --atlas-dir ../../assets/sprites/sprite_sheets \
    --preview-dir /tmp/x-preview
```

Omit `--atlas-dir` to skip the atlas. Omit `--preview-dir` to skip the preview. Packing needs the
`TexturePacker` command. Tests: `./run_tests.sh`.

## Data file

```yaml
name: MegamanX
sheet: 1LMX.png

defaults:
  cell: [48, 48]
  anchor_y: bottom
  align_x: overlap
  align_to: stand

rows:
  6: {merge: [[4, 8]]}
  14: {split_at: [52]}

animations:
  - {name: stand, row: 8, frames: [0, 4], align_to: null}
  - {name: run, row: 15, frames: [1, 8]}
  - {name: run_shoot, row: 16, frames: [1, 8], twin: run}
  - {name: death_orb, row: 4, frames: [5, 8], anchor_y: center, align_x: center}
  - {name: climb_shoot, row: 12, frames: [5, 5], offsets: {0: [3, 0]}}
```

Rows are found from fully empty lines and count from 1. Frames in a row are found from fully
empty columns and count from 0, left to right.

| Key | Where | Meaning |
|---|---|---|
| `merge` | `rows` | Joins detected frames into one frame, as first and last index. Use it for detached pixels. The indexes count the frames after `split_at`. |
| `split_at` | `rows` | Cuts touching frames. The right part starts at this sheet column. |
| `frames` | animation | First and last frame in the row. |
| `anchor_y` | defaults, animation | `bottom` puts the lowest pixel on the last cell row. `center` centers the frame. |
| `align_x` | defaults, animation | `overlap` finds the position with the most equal pixels. `center` centers the frame. |
| `align_to` | defaults, animation | With `overlap`, the first frame aligns to frame 0 of this strip. |
| `twin` | animation | With `overlap`, each frame aligns to the same frame of this strip. Use it for shoot strips. |
| `offsets` | animation | A manual shift of one frame as `index: [x, y]`, after the placement. The index is the frame in the strip, not in the row. Positive `y` moves it down. |

With `overlap`, a frame with no `twin` aligns to the frame before it. A frame that shares no equal
pixels with its reference is centered.

The tool stops with an error when a frame on the sheet is in no animation, when a frame is in two
animations, when a frame does not fit the cell, and when a key in the data file is unknown.

## Check the result

Open `preview.png` and the GIF of each animation in the preview folder. The GIFs show jitter. Fix a
bad frame with `offsets` and run the tool again.
