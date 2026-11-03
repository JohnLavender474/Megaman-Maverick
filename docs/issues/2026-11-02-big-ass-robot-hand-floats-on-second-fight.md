---
status: closed
verified: yes
area: core
gameplay-change: yes
found-by: owner play test of mega-refactor-118a-fight-routine (2026-11-02)
related-items: [118]
---

# On a second `BigAssMaverickRobot` fight, a hand floats in midair while the boss falls

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/BigAssMaverickRobotHand.kt`: `onSpawn` spawns the hand's platform `Block` with `WIDTH` and `HEIGHT` but no position (`:177-190`). The sprite is drawn at the block, not at the hand body (`defineSpritesComponent`, `preProcess`).
- `core/src/main/kotlin/com/megaman/maverick/game/world/body/IBodyExtensions.kt:20-31`: an `ABSTRACT` body with zero velocity updates only while it overlaps the game camera.

## What happens

1. `Block.onSpawn` sets a position only from `BOUNDS` or `POSITION`. With neither, a block from the pool keeps the position of its last use.
2. The hand moves its block in its own `body.preProcess`. The hand body is `ABSTRACT`, and in the `FOLLOW` state its velocity is always zero. So while the boss falls from above the view, the hand body is not processed, and the block does not move.
3. The hand sprite is drawn at the top of its block. So the hand shows at the block's old position until the hand body enters the view.

In the first fight, the old position of a pooled block is not in the boss room, so nothing shows. After a death in the fight, the blocks go back to the pool at their positions in the boss room. A second fight gets them again, so a hand shows in midair at its position from the first fight. The owner saw this for the left hand. Which hand shows depends on the pool order.

Found in play. The cause comes from reading the code. The old flow has the same code, so the bug existed before `118a`.

## What should happen

Both hands fall with the boss body in every fight.

## How to reproduce

1. `./run-game-alpha.sh --level INTRO_STAGE --room room12`.
2. Go through the gate, and die in the fight.
3. Go through the gate again, and watch the hands while the boss falls.

## Short contract

Branch: `fix/big-ass-robot-hand-block-position`, off `master`, in `.worktrees/fix/big-ass-robot-hand-block-position`.

Intended changes:

- At spawn, a hand puts its body at the boss center (the `FOLLOW` position), and it spawns its block with `BOUNDS` at the block target under the hand. So the block and the sprite start at the hand, not at the old position of a pooled block.

Only `BigAssMaverickRobotHand.kt` and a new test change. `Block`, `BigAssMaverickRobot`, `IBodyExtensions` and `engine/` do not change.

Not changed:

- The `FOLLOW`, `MOVE_TO_ORIGIN`, `REVEAL_ORBS`, `ROTATE`, `LAUNCH` and `RETURN` motion, and the block's per-step move in `preProcess`.
- The first fight looks the same, apart from the hands, which now start at the boss.
- The other users of `Block` with no position.

Checks:

- A test: the hand spawns its block with `BOUNDS`, and the top center of the bounds is the block target under a hand at the boss center.
- Break the code and confirm that the test can fail (`core/src/test/CLAUDE.md`).
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/big-ass-robot-hand-block-position`:

- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: go through the gate, and die in the fight. Go through the gate again. Both hands fall with the boss body, then move out and rotate.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: beat the boss in the first fight. The intro, the hand attacks, the platform on each hand, and the defeat look as before.

## Notes

The same fault can exist in other entities that spawn a pooled `Block` with no position. This fix does not check them.

## Resolution

Fixed by `fix/big-ass-robot-hand-block-position` (squash `8e7ae7af0`). At spawn, a hand puts its body at the boss center and spawns its block with `BOUNDS` under the hand, as the short contract says. The owner's play checks pass (2026-11-02).

Not checked: other entities that spawn a pooled `Block` with no position (see "Notes").
