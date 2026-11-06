# Contract: mega-refactor-081a-player-lifecycle

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-06

The branch is a pure move: `Megaman` calls shared functions in place of its own lifecycle code. Megaman plays the
same. Each check below is a control check, except where it says "experimental": those cover the 2 places where the
order of lines can move (`onSpawn` and `takeDamageFrom`).

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] New tests for the shared functions pass, on `SecondPlayerEntity` or `Megaman`:
  - `playerInvincible`: true for each of its 5 terms (damaged, recovering, `DAMAGE` lock, not live, not in play), false
    when none holds. The existing `MegamanInvincibleTest` passes, moved or extended.
  - `playerCanMove`: false for each of its 4 terms (`MOVEMENT` lock, stunned, damaged, frozen), true when none holds.
  - Each of the 2 rules throws when a component that it reads is missing (fail fast in place of a silent default).
  - The damage edit: the base damage with no enhancement in normal mode; +1 in hard mode; the enhancement scale.
  - The damage permission: a hit from the player's own shot is refused; a hit from another player follows
    `rules.friendlyFire`; an entity that is not live is refused. `FriendlyFireTest` passes.
  - The out-of-bounds kill: a body 1.5 tiles or more past a map edge is destroyed; a body inside is not.
  - The room transition: `BEGIN_ROOM_TRANS` and `CONTINUE_ROOM_TRANS` set the body center to the event position and
    turn gravity off; `END_ROOM_TRANS` turns gravity on again, except while climbing.
  - The spawn: `PLAYER_SPAWN` is submitted for slot 0 and not for slot 1; the spawn bounds place the body on its bottom
    center.
  - The destroy: `PLAYER_JUST_DIED` is submitted with the slot, and the listener is removed.
  - `PlayerDamageNegotiator` gives the same values as before for its table entries, for an `ISizable` enemy, for an
    `IHazard`, and 0 for the player's own shot.
- [ ] A mutation check for each shared function: break one term or one line, and at least 1 test fails. Write the
  results in the process log.

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-081a-player-lifecycle`

Experimental: spawn order (gravity set before the lock clear):

- `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_rotated`: the spawn has `direction=left`. Mega beams in,
  the camera turns as before, and Mega can move after the beam-in. Die, and check the same after the respawn.

Experimental: damage order (the damage signal and the sounds):

- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room7`: go into the boss room and get hit by Precious Woman while
  she stands. She laughs, as before. The damage sound plays once, and a charge stops with its sound.

Control: damage reaction:

- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: get hit from both sides. Mega takes the same
  damage, moves away with the same knockback, flashes, and is not hit again during the flash.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room stun`: the pulser stuns Mega. Mega cannot move during the stun.
- `./run-game-alpha.sh --level GLACIER_MAN --room room6`: get frozen by `Coldier` or `IceFox`. Mega cannot move until
  the thaw, and frozen Mega can still be hit.

Control: room transitions:

- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room carry_left`: run across into `carry_right`, jump up into
  `carry_top`, and climb the ladder between them. Mega pauses on each change, keeps its speed across the side change,
  and has no gravity on the ladder during the change.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room7`: go through the boss `Gate`. Mega moves with the camera,
  and the charge sound stops when the gate opens.

Control: death and respawn:

- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room out_of_bounds`: walk into the pit. Mega dies past the bottom
  of the map, and respawns. The log has the error "The player is outside the level bounds, kill it" (the text changed in this branch, because the function serves every player).
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die from damage. The orbs ring out, Mega
  respawns, the enemies reset, and Mega cannot be hit during the beam-in.

One real stage:

- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: play through several rooms with hits, room changes and a
  death.

## Behaviors to check

- Hard mode +1 and the damage-increase enhancement: covered by the automated tests. No launch option sets the
  difficulty.
- Friendly fire with 2 players: covered by `FriendlyFireTest`. A manual check needs a second pad.

## Intended changes

None.
