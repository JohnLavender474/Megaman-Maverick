# Contract: mega-refactor-047a-second-slot

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: yes (owner, 2026-11-01, verbatim: "yes")

## Intended changes

Without `--players 2`, this branch changes no gameplay.

With `--players 2`:

1. A second `Megaman` with a debug tint plays in the level.
2. The keyboard and all pads but the last control slot 0. The last connected pad controls slot 1.
3. Slot 1 spawns at the `player_2` reference of the current spawn. With no reference, it spawns 1 tile to the right,
   relative to the spawn's `direction`.
4. Slot 1 appears when the beam of slot 0 ends. It disappears when the level-end beam of slot 0 starts.
5. The death of slot 0 runs the current death flow, and slot 1 respawns with slot 0.
6. The death of slot 1 takes it out of play until the next reset. It removes no life, keeps the music on, and starts no
   death flow.
7. A pad that connects or disconnects during play changes the split on the next frame.
8. With no pad, slot 1 stands still, and the game logs one error.

Other intended changes:

- `--players <1|2>` is a new launch option. Any other value exits with code 2.
- `game.players.capacity` is 2. `GamePlayersFieldsTest` changes to match.

## Known wrong with `--players 2`

These are not failures of this branch. Later branches fix them.

- Shots from one player hurt the other (`047b`).
- Enemies target only slot 0 (`047b`).
- A `ChargedShot` from slot 1 can take the direction and facing of slot 0 (`047b`).
- Pickups by slot 1 go to slot 0 (`047b`).
- The death of slot 1 resets a gauntlet, and removes a `PreciousGem` shield and a `SpiderWeb` (`047c` or `047e`).
- The HUD and the pause screen show only slot 0, and only slot 0 can pause (`047d`).
- A boss intro can wait for a dead slot 1 (`047e`).
- When both players stand on one `QuickSand` area, neither player can jump out
  (`docs/issues/2026-11-01-two-players-stuck-in-quicksand.md`).

## Must not change

Without `--players 2`:

- Input from the keyboard and from every connected pad, with all pads merged into one player.
- The bindings screens, `--loadKeyboardPrefs` and `--resetBindings`.
- Spawn, beam-in, `READY` text, death, respawn, game over and level end.
- The number of `PLAYER_SPAWN` and `PLAYER_JUST_DIED` events, and their order.
- The saved data.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] A new test: `--players` accepts `1` and `2`, and refuses other values.
- [ ] A new test: the pad split for 0, 1 and 2 pads, with the flag and without it.
- [ ] A new test: the slot 1 spawn point with a `player_2` reference, and with no reference for each `direction`.
- [ ] A new test: the level screen ignores a `PLAYER_JUST_DIED` event for slot 1, if `testServices()` can reach this
      rule. If it cannot, the plan says so, and the manual death check covers it.

## Manual checks

Run all checks from this folder:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-047a-second-slot`

`RefactorTest047.tmx` is new. A plan step builds it, and the room names below are the planned names.

Keep the keyboard for player 1 and the gamepad for player 2, unless a check says otherwise.

### Experimental checks

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2`: player 2 appears with a tint, 1 tile
  to the right of player 1, when the beam ends. Each device moves, jumps and shoots with its own player only.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room linked_spawn --players 2`: player 2 appears on the `player_2`
  target. The target does not act as a checkpoint.
- `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_rotated --players 2`: this spawn uses `direction=left`.
  Player 2 appears 1 tile to the right of player 1 as the screen shows it, on the same floor.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room slot_0_death --players 2`: kill player 1. The normal death flow
  runs, player 2 disappears, one life goes, and both players spawn again.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room slot_1_death --players 2`: kill player 2. Player 1 keeps
  control, the music plays, and the life count does not change. Then kill player 1. Both players spawn again.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room transition --players 2`: player 1 walks through the room
  edge. Both players move through the transition, and both have control after it.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch --players 2`: player 1 changes the gravity. Both
  players turn, the camera turns once, and both players have control after the turn. Then player 2 changes the gravity.
- `./run-game-alpha.sh --level DESERT_MAN --room room16 --players 2`: finish the level. Player 2 disappears when the
  beam-out starts, and the next screen opens. The plan checks that `room16` is the room to use.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2`, with no pad at startup: player 2
  stands still, and the log has one error. Connect the pad: player 2 moves with no restart.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn --players 2`, with the pad connected: disconnect
  and connect it during play. Player 1 keeps the keyboard, and the game does not crash.
- `./run-game-alpha.sh --players 3`: the game exits with code 2 and prints an error.

### Control checks

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room default_spawn`: one player, no tint. The keyboard and the
  gamepad both control the player.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: the death, the life count and the respawn act as
  they do on `master`.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: the gravity change and the camera turn act as
  they do on `master`.
- `./run-game-alpha.sh --level DESERT_MAN --room room1`: a short pass of a real stage, with the beam-in, the `READY`
  text and one room transition.
- `./run-game-alpha.sh --level DESERT_MAN --room room16`: finish the level. The beam-out and the next screen act as they
  do on `master`.
- `./run-game-alpha.sh`: from the main menu, open the keyboard and gamepad settings screens, change a binding and leave.
  The binding works in a level.
