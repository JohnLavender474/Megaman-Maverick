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

| Level | What to check |
|-------|---------------|
| `TEST_1` | Air dash and ground slide, also while shooting: the trail sprites appear behind Megaman, in the same pose, weapon colors and position as before. Air dash toward a wall while facing it: blocked as before. Fire a special weapon until its ammo runs low: the weapon bar drops as before. |
| `TIMBER_WOMAN` | Boss fight: on the ground pound, Megaman bounces when he stands on the ground or wall slides, and does not bounce in the air. The room shakes as before. |
| `SMB3_1` | Hit a block from below: the head-bump sound plays as before. Enter a pipe portal: Megaman is hidden in the pipe and comes out with the pipe animation. |
| Any stage with a heart tank (or a save with heart tanks) | After a heart tank is collected, the health bar max grows as before. |

Use `./run-game-alpha.sh --level <LEVEL>` from the worktree; add `--room <name>` to jump to a room.

## Behaviors to check

- Death and respawn: Megaman dies, the respawn runs as before, and the level resets as before
  (`PLAYER_SPAWN`, `PLAYER_JUST_DIED`, `PLAYER_READY` and `PLAYER_DONE_DYIN` still reach their
  listeners).
- Pickups: health bulbs, weapon energy bulbs, screws, health tanks and heart tanks work as before.
- Boot: the game starts, the main menu works, and a level starts with no exception.

## Intended changes

None.
