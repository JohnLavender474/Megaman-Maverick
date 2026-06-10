# Room Geometry Conventions

## Standard room size
512 × 448 px = 16 × 14 tiles (1 tile = 32 px)

## Standard interior layout (14-row room)
| Rows (0-indexed within room) | Purpose          | Tile width |
|------------------------------|------------------|------------|
| 0–1                          | Ceiling          | Full width |
| 2–11                         | Interior         | Walls only |
| 12–13                        | Floor            | Full width |

Walls occupy the leftmost 2 cols and rightmost 2 cols of interior rows.

## Tile CSV coordinate system
- The `<data encoding="csv">` block has 50 rows × 100 cols.
- Row index `r` = pixel y `r * 32` (row 0 = top of map).
- Col index `c` = pixel x `c * 32`.
- Room at pixel `(x, y)` starts at row `y/32`, col `x/32`.

## nextobjectid / nextlayerid
The `<map>` tag's `nextobjectid` and `nextlayerid` attributes must be bumped when manually
adding objects or layers. Omitting this causes Tiled to conflict on next save.
