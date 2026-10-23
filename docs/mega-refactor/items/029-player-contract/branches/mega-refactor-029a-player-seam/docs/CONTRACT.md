# Contract: mega-refactor-029a-player-seam

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: draft
- Approved: no

This branch adds new engine and core types, registers Megaman as the only player, changes the
global `megaman` self-references inside the player package to `this`, and adds player identity to
the player events. With one player, every changed read gives the same object as before, so
Megaman, the HUD and every entity must behave the same.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 454 tests on `master`, plus the new players tests.
- [ ] `./gradlew core:test` passes: the 95 tests on `master`, plus the new core tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] The contact rule selection test still passes, and `contact-rule-selection.txt` is unchanged.
- [ ] No global `megaman` read is left in the player package:
      `grep -rnw "megaman" core/src/main/kotlin/com/megaman/maverick/game/entities/megaman` shows no
      read of the `IMegaGameEntity.megaman` extension or of `game.megaman` (constructor parameters
      and local names of the handlers, which already hold their own player, are allowed).
- [ ] The engine player types do not name a core type, a body or a position:
      `grep -rnE "megaman|BodyComponent|Body\b|Vector2" engine/src/main/kotlin/com/maverick/game/engine/players`
      finds nothing.
- [ ] No `primary` or `first()` exists in the players service API (grep).
- [ ] Every submit site in the `SCOPE.md` event table sends `ConstKeys.SLOT`, and `STUN_PLAYER`
      sends `ConstKeys.SLOTS` (grep and review).
- [ ] Outside the player package, only `MegamanMaverickGame` (registration), the
      `IMegaGameEntity.players` extension, the event submit sites and the tests reference the
      players service (grep).
- [ ] A review shows each self-reference change as `megaman` to `this` with nothing else changed,
      and the trail sprite owner set from the spawn props on every spawn.

## Manual playthrough

Run each check on the baseline (before the code changes) and again at the end. Use
`./run-game-alpha.sh --level <LEVEL> --room <ROOM>` from the worktree. The room names can change in
the plan.

| Level and room | What to check |
|-------|---------------|
| `REFACTOR_TEST_029`, `trail` | Air dash and ground slide, also while shooting: the trail sprites appear behind Megaman, with the same pose, weapon colors and position as before. Air dash toward the wall while facing it: blocked as before. Fire a special weapon until its ammo runs low: the weapon bar drops as before. |
| `REFACTOR_TEST_029`, `stun` | When the pulser fires: the room shakes, and Megaman bounces when he stands on the floor or wall slides, and does not bounce in the air or while already stunned. |
| `REFACTOR_TEST_029`, `heart` | Collect the heart tank: the health bar max grows as before. |
| `REFACTOR_TEST_029`, `pickups` | The health bulb, weapon energy bulb, screw and health tank work as before. |
| `REFACTOR_TEST_029`, `death` | Megaman dies, respawns and the room resets as before. |
| `SMB3_1` | Hit a block from below: the head-bump sound plays as before. Enter a pipe portal: Megaman is hidden in the pipe and comes out with the pipe animation. (These depend on a `MARIO_LEVEL`.) |
| `TIMBER_WOMAN` (sanity pass of a real stage) | The boss fight: the ground pound stuns as in the `stun` room. |

## Behaviors to check

- The player events still reach their listeners (`PLAYER_SPAWN`, `PLAYER_JUST_DIED`,
  `PLAYER_READY`, `PLAYER_DONE_DYIN`): covered by the `death` and `pickups` rooms.
- Boot: the game starts, the main menu works, and a level starts with no exception.

## Intended changes

None.
