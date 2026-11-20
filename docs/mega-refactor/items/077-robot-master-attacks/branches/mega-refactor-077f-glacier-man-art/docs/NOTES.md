# Notes: mega-refactor-077f-glacier-man-art

## Decisions

- 2026-11-19 (owner): art first. This branch draws the frames; `077g` codes the fight on the final art.
- 2026-11-19 (owner, brief D1): the art agent uses Claude Fable 5.2 (confirmation of the temporary rule in
  `docs/ways-of-working/art.md`). Sonnet only for mechanical steps.
- 2026-11-19 (owner, brief D2): the frame list in `SCOPE.md` is complete as listed.
- 2026-11-19 (owner, round 1 review): Icicle Rain starts with a fist pound on the ground (new strip `pound`), not
  a big ice chunk. `heave` and `GlacierIceChunk` are dropped (drafts kept in
  `temp/mega-refactor-077f-glacier-man-art/dropped-round-1/`). The other round 1 strips are accepted.

## Final frame list

Round 1 (art agent, 2026-11-19). Each strip is one column, read top to bottom. Frame indexes start at 0. The spawn
point is the center of the projectile, in px from the bottom-left of the cell, for the strip as drawn (he faces
left). For a right-facing sprite, use `64 - x`. Glacier Man source px are drawn at 2 ×.

| Strip | Atlas | Frames | Cell (px) | Layout (rows × cols) | Suggested frame duration | Release frame | Spawn point (x, y) |
|---|---|---|---|---|---|---|---|
| `GlacierMan/chunk` | `Bosses1` | 5 | 64 × 48 | 5 × 1 | 0.1 s (no loop) | 2 | `SmallIceCube` at (12, 32) |
| `GlacierMan/aim` | `Bosses1` | 4 | 64 × 48 | 4 × 1 | 0.1 s (no loop) | 2 | `SmallIceCube` at (9, 20) |
| `GlacierMan/aim_up` | `Bosses1` | 4 | 64 × 48 | 4 × 1 | 0.1 s (no loop) | 2 | `SmallIceCube` at (12, 32) |
| `GlacierMan/pound` | `Bosses1` | 5 | 64 × 48 | 5 × 1 | 0.1 s (no loop) | 2 (impact) | screen shake, icicles start |
| `GlacierMan/freeze` | `Bosses1` | 5 | 64 × 48 | 5 × 1 | 0.15 s (no loop) | none | none |
| `GlacierMan/frozen_crack` | `Bosses1` | 3 | 64 × 48 | 3 × 1 | 0.25 s, 0.25 s, 0.1 s (no loop) | none | none |
| `GlacierMan/frozen_shatter` | `Bosses1` | 3 | 64 × 48 | 3 × 1 | 0.1 s (no loop) | none | `IceShard` pieces from about (32, 22) at frame 0 |
| `FallingIcicle/form` | `Projectiles2` | 4 | 24 × 16 | 4 × 1 | 0.15 s (no loop), then `still` | none | none |

- No frame count differs from `SCOPE.md`.
- `pound` (round 6, 2026-11-19; owner chose round 6 over round 7): side view. The arm is a black upper arm 5 to 6
  px wide and a closed fist (7 × 6 with its black outline, 5 px of white wide, 2 black knuckle lines, 1 blue shade
  px). Frame 0 is `duck` with the fist cocked in front of the chest; frame 1 is `stand` with the fist up beside the
  face; in both, the farthest white px of the fist is 6.4 px from the shoulder px, as the hand of `stand`. Frames 2
  and 3 use the `sled` crouch 1 px forward and 3 px down, with the bent front knee of round 3. Their fist is down on
  the floor line (row 47) at x 8 to 14, and a plain black arm of 6 rows (36 to 41), 6 px wide, slants from under
  the chin forward and down to the fist. Frame 4 is `duck` with 4 chip dots. A light ice shatter is part of the
  strip: a crack flash on the floor rows and ice chips at frame 2, the chips higher and the crack blue at frame 3,
  4 dots at frame 4.
- `form`: the top row of the cell is the ceiling line, as in `still`. Frame 3 is `still` less 2 rows.
- The 3 death strips use the front view of `stop` and `defeated`; the 3 throw strips use the side view
  of `stand`.
- The cube in his hand uses the Glacier Man palette. `SmallIceCube` keeps its own blues, so the color changes at
  the release frame.

## Art sources

- Glacier Man frames: Glacier Man asset pack by Balloon Cart (`assets/sprites/frames/Bosses1/GlacierMan/`).
- `FallingIcicle`, `IceShard`, `SmallIceCube`: existing game art (`Projectiles2`, `Explosions1`, `Projectiles1`).

## Overlap with other branches

- `mega-refactor-078d-reactor-man-art`: other atlases; README `CREDITS` merge conflict only.
