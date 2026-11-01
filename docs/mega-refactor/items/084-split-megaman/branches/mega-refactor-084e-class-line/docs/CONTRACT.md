# Contract: mega-refactor-084e-class-line

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-31

The branch removes 5 interfaces and dead code from `Megaman`. Gates, carts, fans, input and sprites must act the same, in the same frames.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with these tests:
  - Gate rule: a registered player triggers a gate, and a non-player does not.
  - Cart rule: a registered player sets the owner and the sense and removes them at the end; a non-player sets neither.
  - Contact filter: a gate or cart fixture keeps a contact with a player, and it drops a contact with no player.
  - `resetBehavior`: an active behavior ends; a missing behavior or a missing component does nothing.
  - `UnderwaterFan`: a player is blown, and another body is not, if the check can run with no assets.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The interfaces are gone: `grep -rnE 'IBehaviorsEntity|IGateTrigger|ICartRider' core/src engine/src` finds nothing, and the class line of `Megaman.kt` has 10 interfaces.
- [ ] The dead code is gone: `grep -rnE 'spawningTimer|wallSlideGravity|SPAWNING_DUR|MegamanValues.WALL_SLIDE_GRAVITY|regionProcessor \*/|controllerComponent' core/src/main/kotlin/com/megaman/maverick/game/entities/megaman` finds nothing.
- [ ] The duplicates are gone: `grep -rnE 'val Megaman\.(input|[a-zA-Z]*Fixture)\b' core/src/main/kotlin` finds nothing.
- [ ] 16 casts to `Megaman` remain: `grep -rnE '\bas\??\s+Megaman\b' core/src/main/kotlin` has 16 casts.
- [ ] A short launch of the new test room starts with no exception, on the baseline and at the end of the branch.

## Frame order

No line moves. The checks at `GateRule.kt:15`, `FeetCartRule.kt:21,33`, `MegaContactFilter.kt:110-116` and `UnderwaterFan.kt:224` stay at their places, with `isPlayer` in place of the old test. Review the final diff for any other change of order. Lines are at `405cf0171`.

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084e-class-line`

Experimental: `isPlayer` in place of the old checks:

- `./run-game-alpha.sh --level DESERT_MAN --room room13`: walk into the gate to `room14`. The gate opens, Mega goes through, and the gate closes.
- `./run-game-alpha.sh --level GLACIER_MAN --room room7`: go up into `room8`, and swim into the path of an `UnderwaterFan`. The fan blows Mega as before. (`room8` has no player spawn; `room7` is the room below it.)

Experimental: the sprite helpers:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: the beam-in at the spawn draws Mega in the same layer. After a death, the beam-in comes again.
- `./run-game-alpha.sh --level DESERT_MAN --room room16`: finish the stage. The beam-out at the end of the level draws Mega with the same flip on both sides.

Control: behaviors and input:

- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room cart`: jump on the `CartV2`. It is solid, it moves to the right while Mega stands on it, and it turns back at a wall, as in `DESERT_MAN`. `CartV2` reads `feetFixture`, whose import changed. (Changed on 2026-10-31: the room held the old `Cart`, which has no play effect. Only tests cover `FeetCartRule`.)
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: run, jump, ground slide, air dash, wall slide, climb and swim, on both sides. A head bounce on a ceiling during a jump or a slide ends the behavior as before.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: wall slide and jump in each gravity direction. The animations and the speeds are the same.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: a hit on Mega during an air dash or a ground slide ends it as before.
- `./run-game-alpha.sh --level MOON_MAN --room rest_room_1`: run and ground slide. The animation speed follows the movement scalar as before.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: play through several rooms.

## Behaviors to check

Control:

- The pause menu and a menu return: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: pause and resume during a slide. Input works after the resume.

## Intended changes

None.
