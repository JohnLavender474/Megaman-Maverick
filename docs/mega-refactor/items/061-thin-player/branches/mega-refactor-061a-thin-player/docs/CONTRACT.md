# Contract: mega-refactor-061a-thin-player

The gameplay that this branch must not change, and how to check it. Written ahead of time on `master`.

- Status: approved
- Approved: 2026-10-26

Every control, damage reaction, spawn and death sequence, boss intro and defeat, level end, teleporter, gravity
device, dark room and boss reaction must act as on `master`, except the overlap cases under "Intended changes".

## Automated checks

- [ ] `./gradlew engine:test` passes: the 489 tests on `master`, plus the new tests (`SignalTest`, the engine lock
      tests).
- [ ] `./gradlew core:test` passes: the 158 tests on `master`, minus the removed ones, plus the new and changed tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] `contact-rule-selection.txt` is unchanged.
- [ ] Lock tests (engine): a group is locked while any reason holds it; a reason is held once; removing a reason that
      does not hold the group changes nothing; clear and reset; an out-of-range reason index is refused; the change
      hook runs on each lock and when a group becomes free.
- [ ] Lock tests (core): a behaviors lock disallows behaviors on each lock (ends active ones) and allows them only when
      the group is free.
- [ ] `SignalTest`: order, disconnect, connect/disconnect during an emit, re-entrant emit refused.
- [ ] `MegaPlayersTest`: an entity with no parts registers; needed parts fail at the read; optional parts give their
      empty value; `targetFor` picks the first player with a body; the damage signal reaches listeners in order.
- [ ] Code checks (grep): `MegaPlayer.kt` has no `require` and no cast; no `PlayerKeys`, `IPlayerDamageListener`,
      `canBeDamaged` property, `allowMovement` or `setAllBehaviorsAllowed` call in `core/src/main` outside the lock
      functions; no `docs/temp-thin-player-review.md` at the root of `docs/`.

## Manual playthrough

The owner plays these once, after the cleanup, from the worktree. The owner already played the temp branch's checks;
this pass checks that the engine move kept them.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: movement, shooting, charge, weapon switch, pause;
  take damage from an enemy (knockback, blink, recovery).
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: the gravity switch.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room pipe_portal`: a pipe teleport (the teleport damage lock).
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: the boss intro lock, the fight, the defeat (no damage after
  it), the beam-out; the next level starts with working controls.
- Die once: no damage and no actions during the respawn sequence; normal after it.
- A dark room (for example `DESERT_MAN` from its start): the light while charging.
- `PreciousWoman`: she laughs when Megaman takes damage while she stands.

## Behaviors to check by review

- `SpiderWeb`: nothing spawns it; its lock calls are checked by review.
- Each old flag write maps to one lock call (the table in `docs/NOTES.md`).

## Intended changes

The three overlap cases in `docs/SCOPE.md`, "Intended changes" (damage, movement with a spider web, behaviors at the
level end). They differ from `master` only when one source unlocks while another still holds the same group.
