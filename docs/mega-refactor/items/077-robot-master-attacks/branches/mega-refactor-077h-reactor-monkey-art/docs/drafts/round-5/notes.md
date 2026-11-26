# Reactor Monkey drafts, round 5: the chest beat heaves

Made by Claude Fable 5.2, an AI model by Anthropic, directed by the developer. Built on round 4 (original artist of
`stand` and `throw`: not known to me, ask the owner).
Rebuild: `python3 work/heave.py && python3 work/finalize5.py && python3 check.py`.
Look at: `chest_beat.gif`, `chest_beat_sheet.png`, `before-after.png` (round 4 above round 5), `heave-track.png`
(lines = round-4 height of the mohawk top, the pad top and the belt top), `zoom-check.png` (waist and tail, 10x).

## What changed since round 4

Only `chest_beat` and `chest_beat_damaged`: 10 cells (were 8). Every other file is the round-4 file
(`work/notes-round-4.md` holds the round-4 notes; `contact-sheet.png` is redrawn with the new strip).
No pixel is redrawn. Each cell is a round-4 cell cut into 3 parts (tail; hips = belt, loincloth, legs; upper body =
torso, pads, arms, head, mohawk) that move by whole pixels. The feet rows never move.

## Cells and timing (one-shot; loop cells 4 to 9)

| Cell | Seconds | Shows | Code |
|---|---|---|---|
| 1 | 0.10 | `stand`, red eyes (round-4 cell 1) | |
| 2 | 0.10 | rises, red head (round-4 cell 2) | |
| 3 | 0.30 | roar: fists high, chest up, torso arched back | roar sound |
| 4 | 0.06 | LEFT fist hits the chest, body slams down, sparks | shake |
| 5 | 0.06 | rebound, fist still on the chest, no sparks | |
| 6 | 0.08 | inhale: fists high, chest and shoulders up | |
| 7 | 0.06 | RIGHT fist hits, body slams down, sparks | shake |
| 8 | 0.06 | rebound | |
| 9 | 0.08 | inhale | |
| 10 | 0.12 | down to the `mad_stand` pose (round-4 cell 8), then `mad_stand` 1 | |

Loop cells 4 to 9 four times: **8 hits, 8 shake-cam moments** (cells 4 and 7 of each pass), as in round 4. The loop
takes 0.4 s per pass (round 4: 0.28 s), so the whole strip is 2.22 s (round 4: 1.74 s). 3 passes (6 hits, 1.82 s) is
the choice if the length must stay.

## Heave track (px against the round-4 pose; + = up, back = toward his back)

| Cell | Hips, belt, loincloth, tail root | Torso, pads, arms, fists | Head, mohawk | Lean back | Tail tip | Knees |
|---|---|---|---|---|---|---|
| 3 roar | +1 | +3 | +3, mohawk leans back | 1 | 1 back | shins 1 row longer |
| 4, 7 hit | -2 | -3 | -3, mohawk shakes forward | 0 | 0 | shins 2 rows shorter |
| 5, 8 rebound | -1 | -1 | -1 | 0 | 0 | shins 1 row shorter |
| 6, 9 inhale | +1 | +2 | +2, mohawk leans back | 1 | 1 back | shins 1 row longer |

Cells 1, 2 and 10 are 0 everywhere (unchanged), so the hand-over from `stand` / `mad_stand` and back is the round-4 one.

## How the joints stay closed

- Knees: shin rows 141 and 142 (straight segment) are removed or repeated.
- Waist, chest down: the torso slides 1 px down behind the belt. Chest up: torso rows 115 and 116 are repeated.
- Lean back: the upper body moves 1 px toward his back at the belt line.
- Tail: fixed on the hips; the part above row 105 moves 1 px with the lean.
- Right hit: the forearm stays in front of the back hip piece and of the tail.

## Deviations and limits

- The head rides on the shoulders; it has no lift of its own (a separate head move would leave seams at the pads).
- The mouth is the round-4 open mouth in cells 3 to 9.
- The hit sparks show only on the hit cells; the rebound cells have them removed.
- Hit: hips -2 and torso -1 more, so the shoulders drop 3 px (the order said 1 to 2), for a clear read at game size.

## check.py result

Cell size, NES palette, allowed colours, no semi-transparent px, feet on the bottom row: all ok for all 20 strips
(`work/check-result.txt`). Each `chest_beat` cell is one connected shape (no stray pixel). Strict "3 colours per 8x8"
is not met, as in round 4 and in the current art (`chest_beat`: 484 of 1094 areas).
