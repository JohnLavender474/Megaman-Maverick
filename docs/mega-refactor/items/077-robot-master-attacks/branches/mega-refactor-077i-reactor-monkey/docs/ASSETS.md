# Assets: mega-refactor-077i-reactor-monkey

The code agent's input for the frames. Source: the `077h` drafts (`round-4/`, and `round-5/` for `chest_beat`); the
timings come from the round notes there (`round-3/notes.md` frame table, `round-5/notes.md`, `enraged/notes.md`).

## Atlas

- `TextureAsset.REACTOR_MONKEY` (`ReactorMonkey.txt`), new. It loads with every other `TextureAsset` (all entries are
  queued in `MegamanMaverickGame` and in the smoke-test harness).
- Frames: `assets/sprites/frames/ReactorMonkey/<prefix>/<name>.png`; project `texture_packer/ReactorMonkey.tps`
  (Basic, no rotation, 2 px border and shape padding, `MultiPackOff`). Page `ReactorMonkey.png` 1136 x 2042,
  packed by a script (no TexturePacker in the cloud); a laptop repack keeps the names and pixels.
- `Bosses1` and `Projectiles1` are untouched. Their old `ReactorMonkey/stand|stand_damaged|throw|throw_damaged` and
  `ReactorMonkeyBall/0..3` stay. The monkey reads **all** its regions from `REACTOR_MONKEY` (the new `stand` and
  `throw` have the mohawk). The ball keeps `ReactorMonkeyBall/0..3` from `PROJECTILES_1` for the calm ball and reads
  `ReactorMonkeyBall/mad_0..3` from `REACTOR_MONKEY` for the enraged ball.

## Scale and anchor

- Monkey cells are 160 x 160 art px, drawn at 2 game px per art px: sprite size `10f * ConstVals.PPM` (as today). One
  art px = one NES pixel = 2 game px, so every draw offset is a whole number of art px.
- Art faces **left**; flip x when facing right (as today).
- Feet on the bottom row of each cell (except `intro_drop`: he is in the air). The sprite stays at the body's bottom
  center, `translateX(0.5f * PPM * -facing.value)` as today. Body 3.5 x 5 tiles, `ABSTRACT`.
- Opaque x range of the calm cells is 29 to 129; `collapse` 3 to 7 and `dead` reach x 8 to 159 (the flat tail).
  Nothing is cut at the cell edges.
- An art point (ax, ay) from the cell's top-left, facing left, is at game offset
  `x = (ax + 0.5) * 2`, `y = (160 - ay - 0.5) * 2` from the sprite's bottom-left; facing right, `x = (160 - ax - 0.5) * 2`.
- Ball: 64 x 64 art cells on a 4-tile sprite (2 game px per art px), as today.
- Steam: `steam` cells 32 x 32 (sprite 2 tiles), `steam_jet` cells 16 x 16 (sprite 1 tile).

## Strips

All strips are vertical: `Animation(region, rows = cells, columns = 1, ...)`. "Loop" = `loop = true`.

| Region | Cells | Seconds per cell | Play | Code hooks |
|---|---|---|---|---|
| `ReactorMonkey/stand` | 2 | 1.0, 0.1 | loop | as today |
| `ReactorMonkey/stand_damaged` | 1 | held | hold | as today |
| `ReactorMonkey/throw`, `throw_damaged` | 3 | 0.1 | one-shot | release at 0.2 s (today's `throwTimer`); sync the time on a switch to or from `_damaged` (today's `onChangeKey`) |
| `ReactorMonkey/intro_drop` | 1 | held | hold | while he falls in; feet 1 px above the row in the air pose is fine |
| `ReactorMonkey/intro_land` | 3 | 0.06, 0.08, 0.12 | one-shot | on landing: `SHAKE_CAM` (and dust, if any) |
| `ReactorMonkey/intro_roar` | 6 | 0.12, 0.08, 0.3, 0.3, 0.12, 0.12 | one-shot | roar sound at cell 2; optional shake on cells 3, 4; then `stand` |
| `ReactorMonkey/chest_beat`, `chest_beat_damaged` | 10 | 0.1, 0.1, 0.3, 0.06, 0.06, 0.08, 0.06, 0.06, 0.08, 0.12 | one-shot; cells 4 to 9 repeat N passes | roar at cell 3; `SHAKE_CAM` at cells 4 and 7 of each pass; then `mad_stand` |
| `ReactorMonkey/mad_stand`, `mad_stand_damaged` | 2 | 0.08 | loop | cell 2 trembles 1 px; steam ear points below |
| `ReactorMonkey/mad_throw`, `mad_throw_damaged` | 3 | 0.1 | one-shot | as `throw` |
| `ReactorMonkey/ball_hit`, `ball_hit_damaged` | 5 | 0.06, 0.14, 0.14, 0.14, 0.14 | one-shot; cells 3 and 4 may loop while stunned | cell 1 squash and visor flash; cell 2 knocked back, X eyes; then the stand strip. A hit in `throw` starts at cell 2 |
| `ReactorMonkey/mad_ball_hit`, `mad_ball_hit_damaged` | 5 | as `ball_hit` | as `ball_hit` | phase 2 |
| `ReactorMonkey/dying` | 4 | 0.08 | loop | the first part of the death |
| `ReactorMonkey/collapse` | 7 | 0.1, 0.1, 0.12, 0.08, 0.08, 0.1, 0.14 | one-shot (0.72 s) | thud (and shake) at cell 5 |
| `ReactorMonkey/dead` | 1 | held | hold | = `collapse` 7; held until the defeat time ends |
| `ReactorMonkey/steam` | 5 (32 x 32) | 0.09 | one-shot | puff |
| `ReactorMonkey/steam_jet` | 3 (16 x 16) | 0.06 | one-shot | jet, then puff |
| `ReactorMonkeyBall/mad_0` | 1 (64 x 64) | held | hold | as `ReactorMonkeyBall/0` |
| `ReactorMonkeyBall/mad_1..3` | 2 each (64 x 128) | 0.1, 0.075, 0.05 (today's `ANIM_DURS`) | loop | as `ReactorMonkeyBall/1..3` (key = bounces) |

- Chest beat: `Animation` has no inner loop. Build it as one `Animation(frames, durations, loop = false)` from
  `region.splitAndFlatten(10, 1, ...)` with cells 4 to 9 repeated N times, N = ONE constant (owner decides at the play
  test). The art was made for 4 passes (8 hits, 2.22 s); 3 passes = 6 hits, 1.82 s. Start with 4.
- Damaged copies exist only for `stand`, `throw`, `chest_beat`, `mad_stand`, `mad_throw`, `ball_hit`,
  `mad_ball_hit`. The intro strips (no damage in `ENTRANCE`) and the death strips (defeated) have none.
- `throw` cells 1 and 2 and `mad_throw` 1 and 2 include the held ball (it is part of the monkey frame; the ball entity
  is hidden then, as today).

## Steam ear points

Art px from the cell's top-left, art facing left (left ear, right ear). Cells counted from 1.

| Strip, cell | Left ear | Right ear |
|---|---|---|
| `mad_stand` 1 | (45, 89) | (66, 89) |
| `mad_stand` 2 | (44, 89) | (65, 89) |
| `mad_throw` 1 | (65, 81) | (86, 81) |
| `mad_throw` 2 | (88, 63) | (101, 63) |
| `mad_throw` 3 | (42, 98) | (63, 98) |

- The steam art is the LEFT ear's steam; flip x for the right ear (and flip again with the facing).
- `steam` origin: its cell px (16, 31) (bottom center), placed about 8 art px outward and 8 art px up from the ear point.
- `steam_jet` origin: its cell px (15, 15) (bottom-right corner) on the ear point; it shoots up and out.
- No ear points were drawn for `chest_beat`, `ball_hit` or `mad_ball_hit`: hide the steam in those strips, or start a
  `steam_jet` at the `mad_stand` points right after `chest_beat` ends (the Mad Beat tell of `IDEAS.md`).
- `enraged/demo_mad_stand_with_steam.gif` in the `077h` folder shows one timing (a jet, then a puff, every 0.8 s).

## Sources

| Region | File |
|---|---|
| all `ReactorMonkey/*` except `chest_beat*` | `077h` `docs/drafts/round-4/<name>.png` |
| `ReactorMonkey/chest_beat`, `chest_beat_damaged` | `077h` `docs/drafts/round-5/<name>.png` |
| `ReactorMonkeyBall/mad_<i>` | `077h` `docs/drafts/round-4/ball_mad/<i>.png` |
