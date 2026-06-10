# Ladder Setup

Three coordinated pieces are required for a functional ladder:

| Piece                  | Layer      | Size      | Notes                                                        |
|------------------------|------------|-----------|--------------------------------------------------------------|
| `specials:Ladder`      | `specials` | 32 × N px | The climbable zone; N = height in px (multiple of 32)        |
| `blocks:LadderTop`     | `blocks`   | 32 × 8 px | One-way pass-through at the top of the ladder                |
| Tile `132` in `tiles1` | tile layer | —         | Visual ladder texture; place at same column as Ladder entity |

## Placement rules
- `LadderTop` x and y match the **top** of the `Ladder` rectangle.
- Tile `132` goes in the column(s) the Ladder occupies, for every row the Ladder spans.
- For a **ceiling exit** (player climbs up out of a room): leave the ceiling tile open (no `190`)
  at the ladder column so the player can pass through. The `Ladder` entity can span across the
  room boundary into the room above.

## Example from Test8.tmx
- `specials:Ladder` id=33: x=1152, y=1408, 32×128 (4 tiles tall, rows 44–47)
- `blocks:LadderTop` id=29: x=1152, y=1408, 32×8 (top of ladder)
- Tile `132` at col 36 (x=1152), rows 43–47 in the CSV
