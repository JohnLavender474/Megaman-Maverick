# Notes: 174 Move the D-1000 frames out of `Bosses3`

- 2026-11-18 (`077c`, branch commit before the `077c` squash `10e479f09`): `TexturePacker Bosses3.tps` fails with "No
  space left" for `D1000/face`, `D1000/glow`, `D1000/lamp`, `D1000/screw` and `D1000Core/beam`. The sheet came from
  the `081f` `MaxRects` recipe. After `077c` moved ElecDevil to `Bosses4`, `Basic` still fails by 1 sprite. `077c`
  wrote `Bosses3` frame changes into the sheet in place with `temp/077c-nes-2x-art/splice_bosses3.py` (main
  checkout).
