# Contract: mega-refactor-029g-behavior-split

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: approved
- Approved: 2026-10-25

This branch moves the 8 player behaviors into factory functions with no change in their bodies, and keeps the
order in which they are added. Wall slide, swim, jump, air dash, ground slide, crouch, climb and jetpack must act
as before, in all gravity directions, and so must their interactions on the same frame.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 470 tests on `master`.
- [ ] `./gradlew core:test` passes: the 142 tests on `master`, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] `contact-rule-selection.txt` is unchanged.
- [ ] A test shows that a built `Megaman`'s `BehaviorsComponent` has the keys in this order: `WALL_SLIDING`,
      `SWIMMING`, `JUMPING`, `AIR_DASHING`, `GROUND_SLIDING`, `CROUCHING`, `CLIMBING`, `JETPACKING`.
- [ ] A test shows that two `Megaman` objects get a different behavior object for each key.
- [ ] A whitespace-insensitive diff of each moved behavior body against `master` shows no change, except the
      wrapping function and the `return`.
- [ ] `MegamanValues.kt`, `Megaman.kt` and the other player components are unchanged, except for the imports
      that the split needs.
- [ ] The `TODO`s exist: the weapon checks in the factories (`024`) at `defineBehaviorsComponent`; the
      `MegaPlayer.megaman` `TODO` names `024` and a second character, not `029g`.

## Manual playthrough

The owner plays these once, after the implementation, from the worktree (no baseline play-through; see "Test
maps" in `docs/mega-refactor/CLAUDE.md`). The plan adds a room `movement` to `RefactorTest029.tmx`: a floor, two
tall walls, a gap one tile high, a ladder and a water pool. A short launch checks that it loads on the baseline
code.

| Level and room | What to check |
|-------|---------------|
| `REFACTOR_TEST_029`, `movement` | Run, jump (short and full height), and the post-action momentum. Wall slide down a wall, and wall jump between the two walls. Air dash on the ground and in the air (one air dash for each jump). Crouch, and ground slide under the low gap. Climb the ladder up and down, jump off it, and get on it from above. In the water: swim up with A, air dash and ground slide at the water speed, and jump out. A wall slide or a climb that ends lets an air dash start in the air, as before. |
| `REFACTOR_TEST_029`, `gravity_switch` | Flip the gravity, then run, jump, wall slide, air dash, crouch and ground slide in the other direction. The slide and crouch body sizes and positions look as before. |
| `GLACIER_MAN`, `room1` | A short pass of a real stage: run, jump, wall slide, ground slide and air dash. |

## Behaviors to check by review

- The jetpack (Rush Jet) and the Rush Jet air dash: `REFACTOR_TEST_029` does not give `RUSH_JET`, and the game
  gives it only after `MOON_MAN`. The bodies do not change; the diff review and the order test cover them.
- The `NEEDLE_SPIN` blocks and the `RODENT_CLAWS` wall stick: the same (not in the test level's loadout).
- The enemies and blocks that read or set behaviors by `BehaviorType` (`GroundSnow`, `RatRobot`, `MagFly`,
  `GroundSlideBlocker`, `InfernoChainPlatform`): the keys do not change.

## Intended changes

None.
