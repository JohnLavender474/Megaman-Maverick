# Notes: mega-refactor-188a-dragon-moves-art

## Decisions

## Overlap with other branches

## Art round 1 (2026-11-26, art agent: Claude Fable 5.2)

### What was drawn

All strips are in `assets/sprites/frames/Bosses1/MechaDragon/`, cells of 128x128 art px, facing right. No atlas was
repacked.

| Strip | Layout (cols x rows) | Size | `AnimationDef(rows, cols)` | Notes |
|---|---|---|---|---|
| `fly.png`, `charging.png` | 2 x 3 | 256x384 | (3, 2) | hind legs tucked; `charging` is a copy of `fly` |
| `fire1.png` | 5 x 2 | 640x256 | (2, 5) | **trimmed from 16 cells (4x4, 512x512) to the 10 that play** |
| `fire2.png` | 2 x 3 | 256x384 | (3, 2) | tucked legs |
| `turning.png` | 1 x 5 | 128x640 | (5, 1) | cell 3 (index 2) is the front view with both legs; cells 4-5 mirror cells 2-1 as before |
| `init.png`, `frozen.png` | 1 x 1 | 128x128 | () | `frozen` is now a blue recolor of the new `init` pose (the old one was another pose) |
| `defeated.png` | 2 x 2 | 256x256 | (2, 2) | legs dangle limp under the belly |
| `land.png` | 4 x 1 | 512x128 | (1, 4) | new; takeoff = reverse |
| `stand.png` | 2 x 1 | 256x128 | (1, 2) | new; cell 2 = 1 px breath + chest plates glow |
| `breath_windup.png` | 3 x 1 | 384x128 | (1, 3) | new; braced stance, wings up |
| `breath_sweep.png` | 6 x 1 | 768x128 | (1, 6) | new; +30, +15, 0, -15, -30, -45 degrees; only head + jaw + flame change |
| `breath_end.png` | 2 x 1 | 256x128 | (1, 2) | new |
| `chomp.png` | 4 x 1 | 512x128 | (1, 4) | new |
| `flame_puff.png` | 4 x 1 | 64x16 (16x16 cells) | (1, 4) | new; `MechaDragonFlame`: small, full, flicker, fade; no direction, no rotation |

Anchors (mouth pixel, head box, foot line): `ANCHORS.json` in this folder. The foot line is row 127 in every grounded
cell. Grounded cells: the tail runs off the left edge of the cell by design.

### Decisions (so that they can be reverted)

- Fixed by the Opus ideation picks, not overruled by the owner: (1) the dragon stands on its side ledge, back to the
  wall; (2) 1 health phase; (3) high charge lane only; (4) `fire1` trimmed to 10 cells (5 x 2). To revert (4), cut
  cells 11-16 from the old strip in git history and add legs with the script (same leg position in all cells).
- **Palette snap.** The old strips used an NES palette rendering that is not the master list of `art.md`. Every strip
  is now snapped to the list: `51A200->00A800`, `9AEB00->B8F818`, `FFA200->F8B800`, `E35100->E45C10`,
  `4161FB->0058F8`, `61A2FF->6888FC`, `FFFFFF->FCFCFC`, `EBD320->F8D878`, `FFF392->F8D878`, `B21030->A80020`.
  Above the belly the redrawn cells are pixel-identical to the old ones after this map (checked by script).
- **Palette rule not met (inherited look).** The dragon keeps its old color scheme: green (`000000 00A800 B8F818`),
  orange (`000000 E45C10 F8B800`), blue joints (`0058F8 6888FC`), white claws/horn/teeth (`FCFCFC`), pale yellow
  plates (`F8D878`), red only in `defeated`/`fire1` (`A80020`). That is more than 2 sprite palettes, and about 40% of
  the 8x8 areas hold more than 3 colors (the old strips: about 34%). A strict version (2 palettes: green + orange,
  3 colors per 8x8) loses the blue joints and the white claws and looks clearly worse; an automatic preview is in
  `temp/dragon-art/strict2pal/preview.png`. The owner decides whether a strict NES recolor of the whole dragon is
  wanted (a new art round). The flame puff alone is within the rule (`E45C10 F8B800 F8D878`).
- Grounded frames are built from the old upper body (shifted up 3 to 11 px), new drawn legs, a new tail on the
  ground, and for the head angles a re-sampled head and jaw (drawn frames on the pixel grid; the game rotates
  nothing). `land` cell 1 uses the half-raised wing, not the full-up wing: the full-up wing does not fit in the cell
  above a body on straight legs.
- `chomp` cells 2-3 use a drawn neck arc and are the weakest frames of the round.

### Sources

- The new art builds on the old MechaDragon frames of `assets/sprites/frames/Bosses1/MechaDragon/`
  (`init`, `fly`, `fire1`, `fire2`, `turning`, `defeated`): upper bodies kept, hands reused as hind paws.
- The root `README.md` credits the dragon as "Inferno Dragon - Ansimuz (https://ansimuz.itch.io/mega-bot)"
  (section "Other Bosses"). Confirm with the owner that this entry is the MechaDragon.
- One-off scripts and sheets: `temp/dragon-art/` (`lib.py`, `legs.py`, `rig.py`, `ground.py`, `build.py`, `check.py`,
  `sheets.py`, `strict_preview.py`; sheets in `temp/dragon-art/sheets/`).

### Credit lines for the root README `CREDITS` (add in the branch that puts the art in the game)

```markdown
- Inferno Dragon
  - Original by [Ansimuz](https://ansimuz.itch.io/mega-bot)
  - Updated by Claude Fable 5.2, an AI model by Anthropic, directed by the developer: hind legs, the grounded
    animations (land, stand, flame breath, chomp) and the flame puff
```

### Opus ideation summary (short form of `temp/dragon-moves/IDEAS.md`)

- **2A Sweeping Flame Breath (signature).** Grounded on its side ledge, back to the wall. Tell 0.75 s (hard 0.6): head
  rears, chest glows, flame in the mouth. One sweep high to low, +30 to -50 degrees in 1.2 s (hard 0.9), 0.25 s hold;
  panic (< 25%) and hard add a return sweep. The fire is a stream of round unrotated puffs (one per 0.05 s, 11 t/s,
  life 0.5 s, reach about 5.5 tiles) spawned at the mouth point of the nearest of 6 drawn angle frames. Safe spots:
  under the chin (near half of the center block) and out of reach (far ledge / far wall). Ends with jaw shut, smoke,
  0.4 s punish window.
- **2B Stomp Landing + Takeoff.** Hovers 0.5 s over its ledge with legs out, slams down, camera shake, 3 lava drips
  (at the player, leading, random). Takeoff = crouch, rise, legs tuck (`land` in reverse).
- **2C Chin Chomp.** Punishes the under-the-chin pocket after the breath: head rears 0.45 s, snaps to the floor in
  front of its feet, snout damager on cells 2-3, `MagmaExplosion` at the impact.
- **3 Fight structure.** 1 health phase (30 HP), panic under 25%. Loop: air cycle (hover + aimed volley, maybe a high
  charge) -> land (B) -> ground cycle (A first, then C at 70% when the player is in the pocket) -> takeoff. It lands
  on the side it is on; the far ledge stays the out-of-reach safe spot. No walk, no hops.

## Art round 2 (2026-11-26, art agent: Claude Fable 5.2): fly legs dangle

- **Owner feedback on round 1:** "The `fly` frames look weird because the legs are pointing in the same direction as
  the arms. It would look more natural if they were dangling downward." The owner also allowed a larger sprite and
  body, so the legs are full length and were not shortened to fit the old cell.
- **Change:** `fly.png` and `charging.png` (byte-identical) have new hind legs that hang down from the hips: blue hip
  cap, thick thigh with the knee forward, thin shin that slants back, blue ankle cuff, paw with 3 white claws that
  point down (the parts and colors of the grounded legs of round 1). The far leg is plain dark green and hangs from
  behind the belly, 17 art px ahead of the near leg. The round-1 tucked legs are gone and the belly and tail behind
  them are the original pixels again (checked by script). This replaces the row "`fly.png`, `charging.png`" of the
  round-1 table.
- **Cell size (code must follow):** the cells of `fly.png` and `charging.png` are now **128 x 144** art px (were
  128 x 128): **16 rows added at the bottom**; every old pixel keeps its place from the top of the cell. Strip
  256 x 432, 2 cols x 3 rows, `AnimationDef(rows = 3, cols = 2)`, cell order unchanged. Suggested sprite size:
  8 x 9 tiles (was 8 x 8), with the top edge where it was (the sprite grows 1 tile down).
- **Foot rows:** lowest opaque art row per cell (0-based from the cell top): **133, 135, 136, 133, 135, 136** (near
  foot; far foot 131, 133, 134, 131, 133, 134). The old art ended at row 115, so the feet reach 18 to 21 art px
  (36 to 42 game px, about 1.3 tiles) below the old belly. A body fixture that must cover the legs grows about
  1.25 tiles down; below the old belly (rows 116+) the legs span art columns 54 to 90.
- **Sway per cell:** wings up (cells 1, 4): knees bent, feet 3 px higher; mid (2, 5): 1 px; wings down (3, 6): legs
  trail straight; the feet swing back in cells 2-3 and forward in cells 5-6.
- **Not done (later round):** `fire1`, `fire2`, `turning`, `init`, `defeated`, `frozen` still have 128 x 128 cells
  and the tucked legs of round 1. Until they get the same legs and height, the code must size the `fly`/`charging`
  frames on their own, or all strips must be padded to 144 rows at the bottom. The rig for it is
  `dangle_legs()` in `temp/dragon-art/round2/dangle.py` (see `temp/dragon-art/round2/README.txt`).
- **Checks** (`temp/dragon-art/round2/check2.py`, report `check2_report.txt`): 0 hard fails; NES master palette (the
  9 colors of round 1); rows 0-83 identical to the original art (the hip cap starts at row 84, inside the body);
  every changed pixel belongs to the new legs; no border pixels; 8x8 areas with more than 3 colors: 333 of 874
  (round 1: 345 of 818; the inherited palette question of round 1 is unchanged).
- Sheets: `temp/dragon-art/round2/sheets/fly_before_after.png` (original / round 1 / round 2), `fly_new_4x.png`.

## Art round 3 (2026-11-26, art agent: Claude Fable 5.2): other airborne strips

- **Change:** `fire1`, `fire2`, `turning`, `init`, `defeated` and `frozen` have the dangling hind legs of round 2,
  built from the original legless cells (no round-1 leg pixel is left). **All airborne strips now have 128 x 144
  cells** (16 rows added at the bottom; every old pixel keeps its place from the top of the cell). **The grounded
  strips (`land`, `stand`, `breath_*`, `chomp`) stay 128 x 128** with the feet on the bottom row; `flame_puff` is
  unchanged. `fly.png` and `charging.png` are unchanged from round 2.
- **Strips** (these rows replace the round-1 table rows of the same strips):

  | Strip | Layout (cols x rows) | Size | `AnimationDef(rows, cols)` | Lowest opaque row per cell | Notes |
  |---|---|---|---|---|---|
  | `fire1.png` | 5 x 2 | 640x288 | (2, 5) | 135, 136, 133, 135, 136, 133, 135, 136, 133, 135 | wing poses = `fly` cells 2,3,4,5,6,1,2,3,4,5; the `fly` legs of that pose with the feet 1 px further back |
  | `fire2.png` | 2 x 3 | 256x432 | (3, 2) | 133, 135, 136, 133, 135, 136 | the `fly` legs, cell for cell |
  | `turning.png` | 1 x 5 | 128x720 | (5, 1) | 138 in every cell | cell 1 side, 2 three-quarter, 3 front, 4 = mirror of 2, 5 = mirror of 1 (as before) |
  | `init.png` | 1 x 1 | 128x144 | () | 133 | = `fly` cell 1 |
  | `frozen.png` | 1 x 1 | 128x144 | () | 133 | the round-1 ice color map on the new `init` |
  | `defeated.png` | 2 x 2 | 256x288 | (2, 2) | 135 in every cell | limp legs |

- **`turning`:** the body of these cells sits 3 px lower than in `fly`, so the feet are 3 px lower too. Cell 2
  (three-quarter): the near leg hangs at the left belly edge with the knee half forward; the far leg is dark and
  shows behind the right belly edge. Cell 3 (front): two legs side by side behind the belly, mirror images about the
  body axis (x = 64), thigh straight down, a symmetric paw with 3 claws. The original front cell is itself not fully
  symmetric (16 px in the head); the legs add none.
- **`defeated`:** the legs hang under the lower belly: the near leg has a bent knee and its foot 3 px higher, the far
  leg is straight, dark, and shows behind the belly on the right. The paws are a limp variant (uneven claws, the back
  claw curled in). The near shin swings by 1 px over the 4 cells and the far shin against it. The red and dead
  details of the head are the original pixels.
- **Rows unchanged above the legs** (identical to the original art after the round-1 palette snap): rows 0-83 in
  `fire1`, `fire2`, `init`, `frozen` (as `fly`); rows 0-86 in `turning` cells 1, 2, 4, 5 and rows 0-89 in cell 3;
  rows 0-85 in `defeated`. Below, every changed pixel belongs to a leg layer.
- **Checks** (`temp/dragon-art/round3/check3.py`, report `check3_report.txt`): 0 hard fails; sizes, alpha, NES master
  palette (round-1 colors; `frozen` its ice set), no border pixels, mirrors of `turning` hold, `fly`/`charging`
  untouched. New edge-step check: in the straight part of each thigh and shin the leg edge moves at most 1 px per
  row. It does not cover the joints: where the hip cap ends on the thigh the left edge steps 2 px, which is the
  round-2 rig of the approved `fly` cells and was kept so that the legs are the same. 8x8 areas with more than 3
  colors: `fire1` 579 of 1482, `fire2` 328 of 880, `turning` 320 of 711, `init` 65 of 164, `defeated` 210 of 506,
  `frozen` 64 of 164 (the inherited palette question of round 1 is unchanged).
- **Code must follow:** `MechaDragon.kt` still cuts `fire1` as `AnimationDef(4, 4, ...)`; the strip is 2 rows x 5
  cols since round 1. Every airborne region is now 9 tiles high at the old scale (was 8); the grounded regions are 8.
- Scripts and sheets: `temp/dragon-art/round3/` (`build3.py`, `check3.py`, `sheets3.py`, `README.txt`, `prev/` =
  the replaced round-1 strips; `sheets/overview_2x.png`, `sheets/<strip>_before_after.png`).
