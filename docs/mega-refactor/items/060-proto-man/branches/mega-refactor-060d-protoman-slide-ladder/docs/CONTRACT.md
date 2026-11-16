# Contract: mega-refactor-060d-protoman-slide-ladder

- Status: approved
- Approved: 2026-11-14 (all decisions of `SCOPE.md`, "Decisions")

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with:
  - **`MegamanGroundSlideTest` (new), written and green on the old code first** (step 1), then green unchanged after
    the move (step 2). One case per rule of the old `GroundSlideBehavior`:
    - start: `FEET_ON_GROUND` + `DOWN` held + `A` just pressed; no start in the air, on sand, in cooldown, while
      jetpacking, while Needle Spin shoots, in a camera turn, out of play;
    - `DOWN` + `A` does not jump;
    - ends at the max time (0.5 s); after the min time (0.2 s) it ends when `DOWN` is released, and goes on only while
      `A` is held; ends off the ground; ends on damage;
    - under a low ceiling (`HEAD_TOUCHING_BLOCK`) it stays on past the max time, until the head is clear;
    - speed 10 tiles/s along the floor by `facing` (6 in water), x `movementScalar`, x1.5 with `GROUND_SLIDE_BOOST`;
      a press of the other side turns the slide;
    - body height 0.9; the start and end body moves for each of the 4 gravity directions;
    - the cooldown (0.075 s) after the end; the run-speed clamp at the end; the ice momentum is kept;
    - the slide-jump cut inside the min time (today's behavior, recorded, not changed).
  - `MegamanBehaviorsTest` (behavior order), `MegamanCrouchDelayTest`, `MegamanRunTest`, `MegamanBodyFixturesTest`,
    `MegamanClimbTest` unchanged.
  - `MegamanXClimbCrouchTest`, `MegamanXTest`, `MegamanXSabreTest` unchanged (X is not touched, except the `null`
    `slideOffset` in his `BusterParams`).
  - **`ProtomanTest`, slide** (new cases):
    - `DOWN` + `A` on the ground slides with Megaman's numbers (speed, min and max time, cooldown, water speed) and
      a 0.9 body; `A` alone still jumps; `DOWN` + `A` in the air does not slide;
    - the low-ceiling rule, the ice momentum and no slide on sand hold for him, as for Megaman;
    - the post-action momentum does not slow the slide (`groundSliding` reads the behavior);
    - the behavior order: `JUMPING` before `GROUND_SLIDING` before `CLIMBING`;
    - from the crouch guard (pull-out and hold): the shield fixture turns off at once, no put-away frames, no crouch
      box (the `BODY` box is the slide body);
    - a slide does not start while a slash is active;
    - buster slot: `B` fires one `ProtoBuster` from the slide muzzle offset (mirrored to the left) in the
      `groundslide_shoot` pose, and the slide goes on;
    - buster slot: `UP`+`B` ends the slide and does the single slash, as on the ground;
    - sabre slot: `B` (also with `UP` held) ends the slide and starts the floor slash (move 1 of the combo);
    - under a low ceiling: a `B` that would end the slide does nothing, and the slide goes on;
    - with `DOWN` held at the slide end, the full ground pull-out starts after Megaman's 0.15 s slide-to-crouch
      delay (no guard and side speed kept in the delay) and the guard comes up; with `DOWN` released, he stands with
      no guard;
    - a hit ends the slide.
  - **`ProtomanTest`, ladder** (new cases):
    - `UP` at a ladder grabs it (head grab); `DOWN` on a ladder top grabs it (feet grab); `DOWN` in the air with the
      feet on a ladder grabs it; `DOWN` in the air away from a ladder is the air guard;
    - climb speed 5 tiles/s; he climbs over the top and stands on it; `A` lets go with no jump; at the foot,
      `DOWN` does not leave the ladder (Megaman's `leavesOnFloor = false`);
    - no guard on the ladder: with `DOWN` held, no shield fixture, no pull-out, no guard pose;
    - `B` on the ladder fires one `ProtoBuster` from the climb offset, in both slots, with `UP` and with `DOWN` held;
      he holds still for the shot pose (`climb_still_shoot`); no slash starts; no crouch attack at the foot;
    - `SELECT` on the ladder changes the slot, and `B` stays the buster;
    - the poses: `climb` while moving, `climb_still` when still, `climb_finish` past the top; the climb key holds
      through a room transition;
    - a hit ends the climb.
  - The existing `ProtomanTest` cases (move, buster, sabre, guard, crouch attacks) pass unchanged.
  - Each new rule is seen to fail once with the rule removed.
- [ ] `./gradlew core:smokeTest` passes: `Protoman` finds `groundslide` (2 frames), `groundslide_shoot` (1), `climb`
  (2), `climb_still` (1), `climb_still_shoot` (1) and `climb_finish` (1), with the frame counts of the code;
  `entity-tags.txt` unchanged.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from the branch worktree:
`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-060d-protoman-slide-ladder`

The rooms `slide` and `ladder` are new in `RefactorTest060.tmx` (step 3). The other rooms exist in the live TMX files
(checked 2026-11-14), each with a player spawn.

Proto Man (new, experimental first):

- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room slide`: the room has a flat run, a low ceiling (a 1-tile gap,
  longer than one slide), a `GroundSlideBlocker`, an ice patch and a sand patch.
  - Hold `DOWN` (crouch guard), then press `A`: he slides in the 2-frame scarf-flutter pose; the shield drops at once,
    with no put-away. A press of the other side turns the slide.
  - Release `DOWN` at the slide end: he stands. Keep `DOWN` held: he moves on for a short moment (Megaman's 0.15 s
    slide-to-crouch delay), then the crouch guard pulls out again.
  - Slide into the low ceiling: he slides on to its far end and stands up there. Under the ceiling, `B` in the sabre
    slot does nothing; `B` in the buster slot fires a slide shot.
  - On the flat run: `B` in the buster slot fires from the slide pose and the slide goes on; `B` in the sabre slot ends
    the slide and starts the ground slash.
  - The `GroundSlideBlocker` lets the slide pass and blocks him on foot.
  - Start a slide just before the ice (not under the ceiling) and release `DOWN` before the slide ends: on the ice he
    keeps moving at run speed after the slide. With `DOWN` held at the end, the guard pulls out after the 0.15 s delay and stops him. On sand,
    `DOWN` + `A` does not slide; the sand pit sinks him into a `Death` after about 3 s.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room ladder`: the room has a floor ladder to a ledge with a
  `LadderTop`, and a ladder that hangs over the floor.
  - `UP` at the floor ladder: he grabs it and climbs with the large white shield over his back; he stops when no
    button is held; he climbs over the top in the `climb_finish` pose and stands on the ledge.
  - `DOWN` on the ladder top: he grabs it and climbs down to the floor. No shield in front and no guard pose while
    `DOWN` is held on the ladder.
  - `B` on the ladder, in both slots: a buster shot from the climb pose (he holds still for the shot pose); never a
    slash. `SELECT` changes the slot icon; `B` stays the buster.
  - `A` lets go with no jump.
  - On the floor at the foot of the floor ladder, `DOWN` grabs the ladder (as Megaman), so there is no crouch guard
    there.
  - Jump at the hanging ladder and hold `UP`: he grabs it in the air. Fall past it with `DOWN` held: he grabs it when
    his feet touch it. `DOWN` in the air away from the ladder: the air guard.
- `./run-game-alpha.sh --level REFACTOR_TEST_060 --room shield`: the crouch guard, the crouch slash and the crouch shot
  play as at the end of `060c`.

Control (existing behavior that must not change):

- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room ground_slide_blocker`: Megaman slides through the
  `GroundSlideBlocker` and is blocked on foot, as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_068 --room slide`: Megaman's slide on the slopes (speed, length, low
  ceiling, end) plays as before.
- `./run-game-alpha.sh --level INTRO_STAGE --room room3`: Megaman climbs, stops while he shoots, lets go with `A`,
  climbs over the top and grabs the ladder again from the top with `DOWN`, as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room ladder`: X climbs both ladders (floor ladder with a top,
  ladder 3 tiles over the floor), shoots, climbs over the top and grabs from the top, as at the end of `081g`.

## Behaviors to check

- Megaman's slide is the same in every state (water, ice, sand, low ceiling, the 4 gravities, Needle Spin, the boost):
  `MegamanGroundSlideTest`.
- Megaman's and X's ladders are the same: `MegamanClimbTest`, `MegamanXClimbCrouchTest`; `PlayerClimb.kt` does not
  change.
- Proto Man's existing moves (walk, jump, water, room transition, buster, sabre, guard, crouch attacks) are the same
  outside a slide and a ladder.

## Intended changes

- Proto Man slides on `DOWN` + `A` on the ground, with Megaman's rules and numbers. In a slide, `B` is a slide shot
  (buster slot) or ends the slide with the ground slash (sabre slot; nothing under a low ceiling).
- Proto Man climbs ladders with Megaman's rules. On a ladder there is no guard, and `B` is always the buster.
- The slide starts from the crouch guard and drops the shield at once; with `DOWN` held at the slide end, the guard
  pulls out again after Megaman's 0.15 s slide-to-crouch delay.
- `DOWN` in the air grabs a ladder that his feet touch; elsewhere it stays the air guard. `DOWN` on the floor at a
  ladder foot grabs the ladder, as for Megaman, so the crouch guard is not possible there.
- New Proto Man art: `groundslide`, `groundslide_shoot`, `climb`, `climb_still`, `climb_still_shoot`, `climb_finish`.
- Megaman's slide moves to `players/` with no change in behavior.
