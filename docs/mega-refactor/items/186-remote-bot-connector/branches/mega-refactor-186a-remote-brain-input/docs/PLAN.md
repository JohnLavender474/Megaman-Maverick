# Plan: mega-refactor-186a-remote-brain-input

SKETCH. Not final: written before the scope is approved. Each step gets its own check.

## Steps

### 1. `BotBrain` interface

- Changes: an interface for the call at `MegamanMaverickGame.kt:605`; `FollowBrain` implements it; the game holds a
  `BotBrain?` in place of `FollowBrain?`.
- Check: `core:test` (`FollowBrainTest`) passes unchanged; `--bot follow` plays as before.
- Done: no

### 2. `BotKind.REMOTE` and the launch option

- Changes: enum value, resolver rules, flag help in `DesktopRunner`, test cases.
- Check: resolver tests from the contract.
- Done: no

### 3. `RemoteBrain` (`core`)

- Changes: the atomic mask, the silent-client rule, the pause rule, and the interface that the server calls.
- Check: the `RemoteBrain` unit test.
- Done: no

### 4. Socket server (`desktop-common`)

- Changes: a loopback server thread, JSON lines, `hello`, `input`, `error`; start and stop with the game.
- Check: the loopback server test.
- Done: no

### 5. Wire it and write the protocol doc

- Changes: the `REMOTE` case in `MegamanMaverickGame`; protocol doc in the branch folder.
- Check: `lwjgl3:jar` builds; review before the play test (no game launch).
- Done: no

### 6. Play checks

- Changes: none (owner runs the manual playthrough with a script from `temp/`).
- Check: the manual table in `SCOPE.md`.
- Done: no
