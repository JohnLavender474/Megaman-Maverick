# Plan: mega-refactor-081g-x-crouch-ladder

Each step ends with its check. No game launch (owner rule); compile and tests only.

1. **Test rooms.** A subagent adds rooms `ladder` and `crouch` to `RefactorTest081.tmx` with the `level-builder`
   skill: a ladder from a floor to an upper floor with a one-way top, a free-hanging ladder to grab from a jump, and
   a low-shot turret room for the crouch. Painted `tiles`. Check: the analyzer lists both rooms with a player spawn.
2. **Golden test of Megaman's climb (before the move).** A test drives Megaman's current climb with fixed inputs
   (grab from the floor and from the top, climb up and down, let go with `A`, leave at both ends, gravity `DOWN`) and
   records the body positions per step. Check: the test passes on the old code.
3. **Shared climb.** `players/PlayerClimb.kt`: `playerClimbBehavior(player, ClimbParams)` from `ClimbBehavior.kt`;
   Megaman's parts (`aButtonTask`, `canMakeLandSound`, `NEEDLE_SPIN`) become hooks in his params. Delete
   `megaman/behaviors/ClimbBehavior.kt`. Check: the golden test passes unchanged; `core:test`.
4. **X climb.** `MegamanX` adds `CLIMBING` with X's params (speed 5.625 tiles/s from X6 `LADDER`, air-dash reset on
   grab); the X6 ends (D2): climb-down from the top and climb-over at the top, with a short lock; animation keys
   `climb`, `climb_shoot`, `climb_start`, `climb_finish`; shoot pose and turn on the ladder. D4: X's
   `PASS_THROUGH_ONE_WAY` reads `CLIMBING` as Megaman; the `TODO` goes. Check: tests for the 3 grabs, both ends and
   the jump let-go on X.
5. **X crouch.** `CROUCHING` behavior in `megamanx/MegamanXMoves.kt` with the X6 rules (D3); a lower body and
   damage box; `crouch`, `crouch_shoot` keys, the rise on release; the low buster origin. Check: tests for start,
   turn, jump and dash out, and the body height.
6. **Sprite offsets and buster origins** for the new frames. Check: `core:smokeTest`.
7. **Docs.** `players/README.md` (the shared climb), branch `NOTES.md` (X6 sources, `060c` overlap). Check: read.
8. **Review before the play test** (subagents), then the play checks and Diffity.
