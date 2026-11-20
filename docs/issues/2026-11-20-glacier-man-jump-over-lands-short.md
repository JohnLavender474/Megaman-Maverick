---
status: closed
verified: yes
area: core
gameplay-change: yes
found-by: `mega-refactor-077g-glacier-man` (2026-11-20)
related-items: [077]
---

# Glacier Man's jump over the player lands about 1 tile past the player, not 2

## Where

`GlacierMan.jumpOverTargetPlayer` (`entities/bosses/GlacierMan.kt`).

## What happens

The x speed is computed for an air time of `2 * 15 / 22.5` s with no drag. `Body.process` applies the default y
friction (`applyFrictionY = true`, `frictionOnSelf.y = 1`, `vy *= exp(-dt)` per step), and `GlacierMan` does not
turn it off. So the air time is shorter, and he lands about 1 tile past the player. `GlacierManRig`: 287.6 px for a
target of 320 px over a 6.75-tile jump.

## What should happen

He lands 2 tiles past the player in x (`JUMP_OVER_PAST`), clamped to the arena, or the design takes the shorter
landing as final.

## How to reproduce

`GlacierManMoveChoiceTest` (the sled chain case) checks only the launch speed; read `maxX` in the rig. In the game:
`./run-game-alpha.sh --level REFACTOR_TEST_077 --room glacier_man`, wait for a sled that stops at a wall and jumps.

## Notes

The owner play-tested the fight with this landing (077g round 1). A fix is a gameplay change.

Owner decision (2026-11-20, owner accepted the recommendation): fix the x speed for the real air time with the drag;
the jump arc stays as it is. The fix is in `fix/glacier-man-issues`, with the short contract below. The branch also
fixes `2026-11-20-glacier-man-standing-player-counts-as-above.md` and
`2026-11-20-glacier-man-queued-icicle-ownership.md`.

## Short contract

Branch: `fix/glacier-man-issues`, off `master`, in `.worktrees/fix/glacier-man-issues`. One branch for the 3 issues,
because all 3 fixes are in `GlacierMan.kt` and its tests.

Intended changes:

1. Jump-over (this issue): `jumpOverTargetPlayer` computes the air time of the jump with the y drag of the body
   (`defaultFrictionOnSelf.y`, `vy *= exp(-k dt)`) and the jump gravity, and sets the x speed for that time. He
   lands 2 tiles past the player in x (clamped to the arena), within a small tolerance of the step size. The y
   speed, the arc and the other jumps do not change.
2. "Above" rule (`...-standing-player-counts-as-above.md`): `MEGAMAN_ABOVE_OFFSET_Y` goes from 1.5 to 2 tiles, so a
   player counts as above only when the top of the player is 2 tiles or more over Glacier Man's feet. A player who
   stands on his floor is never above. This changes every reader of the rule: the duck weight, the jump weights, the
   `aim_up` throw, the jump at once after the sled, the duck chain and the snowball aim.
3. Queued icicles (`...-queued-icicle-ownership.md`): his icicle list holds only the icicles of their current life:
   an icicle leaves the list when it is destroyed. A defeat or a destroy of the boss acts only on those icicles. No
   gameplay change.

Not changed: the moveset, the timings and the damage of `077g`; the other bosses; `FallingIcicle`.

Checks:

- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule` passes, with:
  - a test that the jump-over lands 2 tiles past the player (`GlacierManRig`, the real `Body.process`);
  - the "above" boundary tests at 2 tiles (2.0 above, 1.99 not above) and a standing 1.5-tile player not above;
  - a test that a destroyed icicle of the boss that another owner spawns again is not destroyed by the boss's
    defeat or destroy;
  - a mutation check for each new rule (`docs/ways-of-working/git.md`).
- Play checks, from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/glacier-man-issues`:
  1. `./run-game-alpha.sh --level REFACTOR_TEST_077 --room glacier_man --heartTanks all --healthTanks all`: stand
     on the floor: he ducks, throws `chunk` and `aim` (not only `aim_up`); after a sled that stops at a wall and
     jumps over, he lands about 2 tiles past you.
  2. Same command: stand on a ledge or jump high: `aim_up` throws, no duck.
  3. Control: `./run-game-alpha.sh --level GLACIER_MAN --room room12 --heartTanks all --healthTanks all`: the full
     fight to the stage exit.

## Resolution

Fixed in `fix/glacier-man-issues`, squash commit `15d5eb772` (2026-11-20). `jumpOverTargetPlayer` computes the air time with the y drag of the body (Newton steps on the drag model), so he lands 2 tiles past the player; the arc is unchanged. Test: the sled chain case of `GlacierManMoveChoiceTest` checks the landing. Owner play test passed.
