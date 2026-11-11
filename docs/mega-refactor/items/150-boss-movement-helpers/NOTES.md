# Notes: 150 Shared movement helpers for bosses and enemies

## `060j` movement-reuse research (2026-11-10, `master` `845b194ad`)

One read-only agent compared the player moves (run, body, jump, wall slide, air dash) with every boss and enemy that
moves like a player. Full report (not committed): `temp/mega-refactor-060j-mega-player/movement-reuse.md`.

- 0 boss or enemy files use a `BehaviorsComponent`; bosses use a `StateMachine` with timers.
- The boss jump is a targeted arc set once at the state start (`calculateJumpImpulse`, 19 files: 13 bosses, 6
  enemies), with no hold-to-rise and no release cut.
- Boss gravity is 2 or 3 constants in the pre-step, only for direction `UP`. No boss reads ice or water; among the
  enemies only `Met`, `SpikeBot` and `GapingFish` read water.
- Fit with the player moves: run (a) 2 lines each in `TimberWoman`, `PreciousWoman`, `BikerKibbo` (net about 0);
  jump, body, air dash (c); wall slide (b) in `TimberWoman`, `PreciousWoman`, `DesertMan`, `InfernoMan` (they differ
  in gravity `-11.25`/`0`/normal, friction y `10`/`6`, and start rule).
- Risk: a helper that fits both a player and a boss needs more lambdas than two copies.
