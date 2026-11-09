# Contract: mega-refactor-127a-warp-spot-solids

- Status: approved
- Approved: 2026-11-07

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with new tests for each case below (a real `SimpleGridWorldContainer`):
  - the facing side is free: the player lands there;
  - the facing side is in a block: the other side;
  - both sides in blocks: the partner's spot;
  - a one-way block (`COLLIDE_DOWN_ONLY`) or a slope at the spot does not block;
  - a `CameraEdgeBlock` at the spot blocks;
  - a `DEATH` fixture or a hurtful `DAMAGER` at the spot is not safe; a friendly `DAMAGER` (no friendly fire) is safe;
  - a spot that is not fully in the view is not used;
  - the partner in the air: the warp waits; it lands when the partner stands (ground, ladder, water);
  - the cap: after the delay plus the cap, the player lands on the partner's spot;
  - the guide shows the waiting state while it waits;
  - a low partner (crouch, slide): the spot lines up with the partner's feet;
  - a side spot over a pit is not used; a floor less than 1 tile below the feet, a one-way platform or water counts as
    ground; a block that collides only from below, ground only under the outer edge of the spot, or a floor with a
    `DEATH` fixture in its top does not (owner, 2026-11-09);
  - a `TELEPORTER` fixture at the spot is not safe (owner, 2026-11-09);
  - after the cap, the warp waits on while a solid blocks the partner's spot (a low crevice) (owner, 2026-11-09).
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from the branch worktree. Experimental checks first.

| Level | Room | What to check |
|-------|------|---------------|
| `REFACTOR_TEST_127` | `warp_wall` | slot 0 stands on a ledge against a wall and faces the wall; slot 1 falls below the view; slot 1 lands on the side away from the wall, never inside it |
| `REFACTOR_TEST_127` | `warp_spikes` | spikes on one side of the ledge: slot 1 never lands on them |
| `REFACTOR_TEST_127` | `warp_enemy` | slot 0 stands against the `Cactus` and faces it (step back to it after a needle hit); slot 1 does not land inside it |
| `REFACTOR_TEST_127` | `warp_air` | slot 0 jumps again and again, and is in the air when the 1 s delay ends: the guide is gray and slot 1 waits; it warps at the first landing |
| `REFACTOR_TEST_127` | `warp_pit` | slot 0 walks right to the very edge of the ledge, with its body partly over the pit, and faces the pit; slot 1 lands on the ledge to the left of slot 0, never over the pit |
| `REFACTOR_TEST_127` | `warp_crevice` | slot 0 slides left into the 1-tile crevice and stays at its dead end past the 1 s delay and the 3 s cap: the guide is gray and slot 1 waits; slot 1 warps when slot 0 slides out and stands |
| `REFACTOR_TEST_127` | `warp_teleporter` | slot 0 jumps onto the step, walks left and stops with Megaman touching the capsule frame, facing the capsule (does not walk in); slot 1 lands on the right of slot 0, never in the capsule, and is not sent to the receiver capsule on the floor |
| `REFACTOR_TEST_047` | `coop_shaft` | control: the bottom warp on an open floor works as before, on the side that slot 0 faces |

## Behaviors to check

- Single player: no change (no `CameraViewEdges`).
- PvP: no change (no `WARP` rule by default).
- The soft-lock fix of `047j` stays: a held player with no free partner dies.

## Intended changes

- The warp spot is tested for solids, `DEATH` fixtures and hurtful enemies; the order is the partner's facing side, the
  other side, then the partner's own spot (was: the side of the view center, untested).
- The warp waits while no spot is safe or the partner does not stand, up to a cap; then it lands on the partner's spot.
- The guide circle has another color while the warp waits.
- A side spot needs ground under the feet (a solid less than 1 tile below, or water); the partner's own spot does not.
- After the cap, the warp lands on the partner's spot only if no solid blocks it there; else it waits on.
- `--viewEdges` gets a new key for the cap.
