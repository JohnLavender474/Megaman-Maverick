# Plan: mega-refactor-060a-shared-run-body

Ordered steps. Each step has its own check. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

## New API and its first caller

| API | First caller |
|---|---|
| `players/PlayerGravity` (component: 6 values, `applyDirection`) | `Megaman.direction` setter and the shared body pre-step |
| `players/playerMaxRunSpeed(...)` | Megaman's `RunParams.maxSpeed` and his momentum component |
| `players/RunParams`, `playerRun(...)` | `Megaman.run` (kept as a thin call, so the controller and `MegamanRunTest` stay) |
| `players/PlayerBodyParams`, `playerBody(...)` and its result | `Megaman.defineBodyComponent` |

## Steps

1. **Golden test of the current body.** Add `MegamanBodyFixturesTest`: Megaman's fixtures in order (type, shape size,
   offset) after one pre-step in each of the 4 directions, standing and sliding; the gravity value and the clamp in
   each state (ground, jump, fall, water, ice). Run it on the current code.
   Check: the test passes before any code moves.
2. **`PlayerGravity`.** The 6 fields and the sign code move into the component; the setter calls
   `applyDirection(direction)`. Add `PlayerGravityTest`.
   Check: `core:test` passes, the golden test is unchanged.
3. **The run.** `RunParams`, `playerRun`, `playerMaxRunSpeed`. `Megaman.run` becomes one call. The Moon check stays
   in Megaman's own max-speed lambda.
   Check: `MegamanRunTest` unchanged and passing; a `PlayerRunTest` on the test player of `players/`.
4. **The body.** `PlayerBodyParams` with hooks, and `playerBody`, which builds the shared fixtures in today's order and
   the shared pre-step. It returns the fixtures. Megaman adds the needle spin and axe fixtures after it, with their
   pre-step lines in his own pre-step entry.
   Check: the golden test passes unchanged; all Megaman tests pass.
5. **README.** `players/README.md`: add the run, the body and the gravity table to "The parts".
6. **Automated checks** of `CONTRACT.md`, then the review before the play test (subagents, no game launch), then the
   play checks and the Diffity review.
