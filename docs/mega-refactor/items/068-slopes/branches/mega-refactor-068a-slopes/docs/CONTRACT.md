# Contract: mega-refactor-068a-slopes

- Status: approved
- Approved: 2026-11-03; scope change (name, slope layers) approved 2026-11-03; play-test fixes (`C8`, `C9`) approved
  2026-11-03

The behavior that this branch changes on purpose, the behavior that it must not change, and how to check them. Do the
checks before the branch merges, and write the result in the branch process log.

## Intended changes

- `C1`: a new `GroundSlope` block. A `GroundSlope` rectangle object in a `blocks_slopes` or `blocks_slopes_<n>` layer makes a straight, stepped floor run with a
  steepness of 1/4 or 1/2 and a high side of `left` or `right`. Any other ratio logs an error.
- `C2`: a body with `Direction.UP` gravity that does not rise stands on the slope surface under its bottom-centre.
  It walks and runs up and down the run with no air time. Its x speed is the same as on a flat floor.
- `C3`: a rising body passes up through a slope. A slope is never a wall or a ceiling: `BlockSideRule` and
  `HeadBlockRule` skip it.
- `C4`: all `BLOCK` contacts (feet, feet gravity, body, projectile, laser, explosion) see only the triangle under the
  surface, not the empty part of the box.
- `C5`: at the high end of a run, the flat block beside it does not push a body whose feet touch the slope under its
  bottom-centre.
- `C6`: a body with other gravity directions treats a slope as a normal rectangle block.
- `C8`: the body stays on the surface with no gravity (a ground slide) and at both run ends. A body with a feet
  fixture, `Direction.UP`, gravity on and `velocity.y <= 0`, whose feet stay in contact with a slope (`CONTINUE`),
  moves down to the surface under its bottom-centre. Just past the low end, it moves down from the first step to the
  slope base. The feet contact does this, not the body overlap, as the sticky feet of moving blocks do. It never moves
  a body up, a rising body, a body with gravity off, or a body that only starts its contact (a landing).
- `C9`: each `GroundSlope` draws its triangle as a filled shape. The test map has no slope tiles.
- `C7`: a new test map `RefactorTest068.tmx` (`LevelDefinition.REFACTOR_TEST_068`, type `TEST_LEVEL`).

No existing map has a `GroundSlope` object, so no existing level changes.

## Must not change

- `K1`: flat floors, walls and ceilings: standing, running, jumping, sliding, wall sliding, landing sound, head bumps.
- `K2`: one-way blocks (`COLLIDE_DOWN_ONLY`, `COLLIDE_UP_ONLY`, `PRESS_UP_FALL_THRU`) and block filters in
  `MegaCollisionHandler`.
- `K3`: riders on moving blocks (`Lift`, `DropperLift`, `PropellerPlatform`).
- `K4`: walking enemies on flat floors turn at walls as now.
- `K5`: the contact rule table: `contact-rule-selection.txt` does not change.

## Automated checks

- [x] `./gradlew engine:test` passes.
- [x] `./gradlew core:test` passes, with the new tests:
  - `GroundSlopeSurfaceTest`: the surface height at each step, for both steepnesses and both high sides, at the run ends
    and outside the run (`C1`).
  - `GroundSlopeCollisionTest`, with real `Body` objects: snap up from below the surface, no move down from above it
    (step 6b moved the stick to the feet contact), no snap while rising, no snap with the bottom-centre outside the run, no rectangle push-out
    against a slope, the high-end flat block skip, a flat block with no slope under the feet still pushes, and a
    `Direction.DOWN` body gets the rectangle push-out (`C2`, `C3`, `C5`, `C6`, `K1`).
  - `GroundSlopeFeetStickTest`: the `C8` gates, each case that moves the body and each case that must not.
  - `GroundSlopeWalkTest`, a real `WorldSystem` with a Megaman-sized body (feet, feet gravity, slide with no
    gravity): no frame off the ground walking and sliding across the `shallow` layout in both directions, a jump
    leaves the slope, a knockback leaves the slope, and a body with gravity off is not moved (`C2`, `C8`).
  - `ContactRuleSelectionTest` passes with no change to `contact-rule-selection.txt` (`K5`).
  - `LevelCompletionScreensTest` lists `REFACTOR_TEST_068` (`C7`).
- [x] `./gradlew core:smokeTest` passes. It builds `GroundSlope` and calls `init()`.
- [x] `./gradlew lwjgl3:jar` builds.
- [x] `git diff master --stat -- engine/` is empty.
- [x] One `--diagnostics` run in `REFACTOR_TEST_068` `shallow`, after the implementation: the `resolveCollisions`
  and `processContacts` entries show no outlier. A sanity check only, with no baseline (owner decision, 2026-11-03).

## Manual checks

Run from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-068a-slopes`

Experimental checks (new behavior) come first. A fix after a problem here means the control checks run again.

- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room shallow`: a shallow hill (up to the right, then down to the
  right). Walk and run up and down both sides. Megaman stays on the surface: no fall pose, no land sound on the way
  down, a jump works at any point. The run speed looks the same as on the flat floor (`C2`).
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room steep`: the same as `shallow`, with steep runs (`C2`).
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room chain`: a shallow run that joins a steep run, and the reverse.
  Megaman crosses each join with no jump or stop (`C2`).
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room high_end`: a run whose high end meets a flat floor. Walk
  slowly across the join in both directions and stop on it. No shake, no push back (`C5`).
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room jump_onto`: a ledge above a slope and a platform below it.
  Jump down onto the slope: Megaman lands on the surface, not in the air above it. Jump up from under the slope's low
  end: he passes up through it and lands on the surface. A jump from the slope reaches the same height as from a flat
  floor (`C2`, `C3`).
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room slide`: ground slide up and down a shallow and a steep run.
  The slide keeps its speed and stays on the surface (`C2`).
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room walls`: a wall at the low end of a run and a slope face next
  to Megaman. He wall slides on the wall, never on the slope. A head bump under a flat ceiling works; there is no head
  bump on a slope (`C3`).
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room shoot`: shoot normal and charged shots low across the slope,
  in both directions. A shot that goes over the surface flies on; a shot into the surface hits it (`C4`).
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room enemy`: two `Spiky` on a shallow and a steep run between two
  walls. They walk over the slopes and turn only at the walls (`C2`, `C3`, `K4`).

Control checks (kept behavior):

- `./run-game-alpha.sh --level INTRO_STAGE --room room1`: run, jump, slide, wall slide and head bumps on flat blocks.
  Land sounds as before (`K1`).
- `./run-game-alpha.sh --level TEST_1 --room room9`: ride the `Lift` and the `DropperLift` (`K3`).
- `./run-game-alpha.sh --level WILY_STAGE_1 --room room2`: ride the `PropellerPlatform`s (`K3`).
- `./run-game-alpha.sh --level TEST_1 --room room12`: `Spiky` turns at walls on a flat floor (`K4`).

- `./run-game-alpha.sh --level INTRO_STAGE --room room3`: climb up the ladder, stand on the `LadderTop`, drop
  down through it, and jump up through it from below (`K2`).

## Behaviors to check

- Death and respawn on a slope room: Megaman respawns and stands on the floor.
- Pause and resume while Megaman stands on a slope: he stays on the surface.
