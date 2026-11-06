# Contract: mega-refactor-081b-x-run-jump-shoot

- Status: approved
- Approved: 2026-11-06 (brief decisions D1 to D3)

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes. It builds `MegamanX` and calls `init()` with the real atlas, so a wrong
  region name fails.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] New tests, with `testServices()` and fixed input frames (as `MegamanRunTest` does):
  - X walk start, then walk speed; jump start speed; the jump cut on release of `A`; the fall limit; no air drag.
  - X buster (X6 input): a press spawns `Bullet`; a release after a half or a full charge spawns `ChargedShot` with
    the full flag; a release with no charge spawns nothing more; the wait between shots; the shot limits (3, and 2
    for a full charge).
  - The swap: slot 0 holds X after the start of an X level and Megaman after the start of another level; it throws
    with 2 players on an X level, and when the outgoing player is live.
  - `resetPlayerProgress` reads the roster (`PlayerRoster.forEach`), which the roster test covers; a test of the game
    object itself is not practical with `testServices()`.
  - Megaman's jump is the same after the `jumpBehavior` parameters (`MegamanBehaviorsTest` and a new jump-height
    test that runs before and after the change).
- [ ] Mutation checks: break each new rule once; at least 1 test fails each time.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from `.worktrees/mega-refactor-081b-x-run-jump-shoot`. Experimental checks first.

| Level | What to check |
|-------|---------------|
| `REFACTOR_TEST_081 --room flat` | X beams in and stands. Run: a short walk start, then the walk. Jump: a short hop on a tap, a full jump on a hold. Fall. The camera follows X. The sprite faces the right way. Jump up through the ladder top (1 tile up, near the spawn), land on it, hold `DOWN`: X drops at once. Walk into the weapon-energy bulb: no crash. |
| `REFACTOR_TEST_081 --room slope` | X walks up and down the slope and jumps from it. |
| `REFACTOR_TEST_081 --room targets` | A press shoots at once; hold to charge, release for the charged shot (X6 input). The 3 shots look and sound as Megaman's do. A tint flash on X at half and at full charge. |
| `X6_INTRO --room room1` | The HUD draws with no crash. Run, jump the first 1-tile step, the 2 `Junkroid`s and the `HoverGunner` near the start. X cannot pass the 2.5-tile step at tile 20 in phase 1 (owner decision, 2026-11-06). |
| `X6_INTRO --room room2` | The room before the boss. Jump into the gate. The boss room has a floor now, and `D1000` stands on it. A full charge hurts `D1000`; a normal shot does not. After the win, the beam-out runs with no crash. |
| Extras menu, `X6_INTRO`, die until game over, continue | No crash. X plays again. |
| Extras menu, `X6_INTRO`, back to the menu, then a Megaman extra level | Slot 0 is Megaman again. Megaman plays as before. |
| `GLACIER_MAN --room room1` (control) | Megaman runs, jumps (also a wall jump), shoots and charges as before. |

## Behaviors to check

- Pause screen on `X6_INTRO`: no crash.
- Death and respawn on `X6_INTRO`: X respawns, and the camera follows X.
- Megaman on every other level: no change.

## Intended changes

- New: X plays `X6_INTRO`; Megaman is dormant there.
- The camera focus is set at each level start, not once at the first level.
- X keeps Megaman's beam-in and beam-out art, with a `TODO`.
- X drops through a one-way block (a ladder top, or a one-way platform) while `DOWN` is held (temporary, `TODO`, until climb).
- `X6_INTRO` boss room: a floor block at the painted floor (y 512 px), and the `D1000` arena moved down 96 px to stand on it. Before, the room had no floor (also for Megaman).
- `--players 2` with a level that has no `MEGAMAN` is a launch error (exit code 2).
