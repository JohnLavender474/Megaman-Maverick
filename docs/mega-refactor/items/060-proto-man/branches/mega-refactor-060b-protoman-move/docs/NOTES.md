# Notes: mega-refactor-060b-protoman-move

## Decisions

## Atlas pipeline (step 2)

- `utils/sprite_slicer/sheets/prepare_protoman.py` crops the 48x48 cells at `y=12` (this drops the labels), keys
  `(147,187,236)` (and `(38,123,218)` only in a cell with no light blue), mirrors each cell to face right (D3) and puts
  them in 4 rows: idle x2, step, walk w1 w2 w3 w2, jump 1. `Protoman.yaml` slices that sheet. The tool did not change.
- The source sheet is not in the repo (D2 names the prepared sheet only). To run the pipeline again, the source is
  `~/Downloads/Custom _ Edited - Mega Man Customs - DLN-000 Proto Man _ Blues - Proto Man (Expanded) (Inti Creates
  8-bit-Style).png` (a copy: `temp/mega-refactor-060b-protoman-move/sprites/src/proto_sheet.png`). From the repo root:

  ```bash
  python3 -I -B utils/sprite_slicer/sheets/prepare_protoman.py <source sheet> utils/sprite_slicer/sheets/Protoman_prepared.png
  python3 -I -B utils/sprite_slicer/sprite_slicer.py utils/sprite_slicer/sheets/Protoman.yaml \
      --frames-dir assets/sprites/frames/Protoman --atlas-dir assets/sprites/sprite_sheets
  ```

- Result: 4 regions, 8 frames, atlas 50x392: `stand` 48x96 (2), `run_trans` 48x48 (1), `run` 48x192 (4), `jump` 48x48
  (1). The 4 frame files are byte-equal to the approved draft. `JUMP 2` (shield) waits for `060e`.
- `assets/credits.txt`: the sheet credit, split into lines of 32 characters or less, because `CreditsScreen` shows
  each line as one line of text.

## Protoman class (step 3)

- The step: Megaman builds `stand` as one composite animation (step, then the loop). `Protoman` gets the same result
  from its key function: on each entry to the stand (a stop, a landing, the spawn, the end of the damage) the key is
  `run_trans` until that 1-frame, non-looping animation ends, then `stand`. No new type or timer.
- Megaman's room-transition key rule (`stand` shows as `run`) is kept; X lacks it (`081f`).
- His land sound follows Megaman's rule (`MEGAMAN_LAND_SOUND` after a jump or a fall). The `GutsTank` side rule of
  Megaman's body is kept.
- Not added (Megaman has them, no `060b` room needs them): `SandReactionComponent` (Proto Man takes the default
  surface reaction), the spawn props `gravity_scalar` and `movement_scalar`, the Moon Man run scalar, the slot-1 tint.
- `TODO`s at the `Megaman::class` sets that miss him (and X), item `030` (faction and root owner; was `060e` before the review): `JoeBall`, `Snowball`, `SwinginJoe` (hit
  masks), `PreciousTron` and `PreciousWoman` x3 (shield shatter sets), `SmallIceCube` (instant-death set),
  `DarknessV2`, `DarknessV4` (lights). Not marked: the 3 `Megaman::class` keys in `StandardDamageNegotiator`; they
  name Megaman as a damager (his body as a weapon), and `Protoman` is no damager in this branch.

## Overlap with other branches

## Proto Man v2 frames (owner, 2026-11-11)

- After the first play check, the owner asked for Proto Man's frames in the style of `Megaman_v3` (the more toned,
  muscular physique), named "v2" (Proto Man had no v2). Drafts in 2 rounds:
  `temp/mega-refactor-060b-protoman-move/v2/` (`notes.md`, the previews, the scripts, the pixel grids).
- Owner choices: stand, step and run use the `Megaman_v3` body repainted in his colours, with his own head, visor,
  fin, scarf and shield on top; the jump keeps his own pose (arms out) with the v3 fist widening only; the v3 chest
  muscle lines are red; his helmet keeps its shape (no v3 crest bulge). Round 2 cleaned the joins by hand.
- In the game: `assets/sprites/frames/Protoman_v2/` and `assets/sprites/sprite_sheets/Protoman_v2.{txt,png}`, the
  same 4 regions and sizes as the classic atlas; `TextureAsset.PROTOMAN` reads `Protoman_v2.txt`. The classic
  `Protoman` frames, atlas and the slicer pipeline stay as the source of his head and the other frames. The v2 frames
  are hand-made sources now (as `Megaman_v3`'s are); the slicer does not make them.
- Every other Proto Man frame (shoot, slide, climb, hurt, stun, shield, teleport, and the rest) needs the same v2
  restyle when a later branch adds it (`060c` to `060e`). `TODO` at `Protoman.defineAnimationsComponent`.
