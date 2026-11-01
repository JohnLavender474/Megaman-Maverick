# TODO: mega-refactor-031f-menu-input

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Remove the dead `ScreenController` path or give it a real use: `screenController`, `showScreenController`, `--showScreenController`
  (question 9). The `TODO` for mobile is the link.
- Remove the dead `LevelSelectScreen`.
- A gamepad back button (question 10), in `070`.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

1. **A held `START` at the start of a lock pauses the game, and the pause cannot be left.** Reading of the code, not run. A room
   transition starts in `cameraController.update` and submits `TURN_CONTROLLER_OFF` (`LevelRoomHandler.kt:54`). `events.run()` sets
   `locked` in frame `N`. In `N+1`, the builder gives `JUST_RELEASED` for a held `START` (`InputFrameBuilder.kt:18-19`). `MLS:862` reads it, and
   `player.inPlay` is still `true`, because a transition does not change the phase. So `MLS:878` queues `game.pause()`. In `N+2`, the game
   pauses. All later frames are `RELEASED`, so the pause menu reads nothing. `MLS:912` stops `cameraController.update`, so the transition
   does not end, and `TURN_CONTROLLER_ON` (`LevelRoomHandler.kt:135`) never comes. The only way out is a flag that is off by default.
   The same applies at `MLS:755` (boss defeated) and `WilyFinalBoss.kt:311`, in play. `MLS:755` submits from inside `events.run`, so its lock
   starts 1 frame later.

   Trigger 2 (research of 2026-11-01): a real `START` release in frame `N`. `MLS:876` queues the pause before `events.run()` sets `locked`, so the
   pause runs at the start of `N+1` with the same dead menu.

   Origin: `6aebae6c2` (2025-02-15) changed pause from press to release through `runQueue`, and added the dead menu. `39f4d3629` (2025-03-01)
   removed the last default way out. `903c5d145` (2025-03-12) closed the way out that `c5339961f` had opened. `031a` and `031c` moved this logic
   and changed no behavior. The older `ControllerPoller` has the same false release. Question 3 fixes it here.
2. **Candidate 5 of the item `NOTES.md` is confirmed by reading:** a focus-loss or minimize pause in a lock gives the same dead menu. `Esc`
   gives a way out with this branch. The flags are off by default.
3. **A press read carries a held button to the next screen.** Inference: `CreditsScreen:89` skips on press. `fullEndTimer` is 1.5 s (`:51`). If
   `START` is held past that, its release on `MainMenuScreen` confirms `START NEW GAME`, which calls `gameState.reset()` (`:134`). This needs a
   check in the game. Not in this scope.
4. **Candidate 4 of the item `NOTES.md` is still open:** a rebind of `START` or `A` with the key held past `0.25` s confirms the row again. Not in
   this scope.
5. **Docs:** the item `NOTES.md` says 9 live menu instances. The count at this commit is 8.
6. **Contract question 4: `LevelSelectScreenV2` back after an extra level.** `SAVE_GAME_SCREEN` reads `isInExtraLevel` in `show()` and in the `MAIN MENU`
   row. `currentLevel` stays on the extra level, and `loadSavedState()` does not clear it. Route: an extra level, `MAIN MENU`, `LOAD SAVE FILE`, level
   select, `Esc`. The save screen then has the extras rows. `CONTINUE` restarts the extra level, and `MAIN MENU` removes the loaded progress from memory.
   The save file does not change. Accepted with a `TODO` at `LevelSelectScreenV2.onBack`. Issue file at the merge:
   `2026-11-01-level-select-back-after-extra-level.md`.
7. **Plan open question 3: the listener lock.** A lock that a listener submits, for example `MegaLevelScreen.kt:754`, starts 1 frame later, so a real
   `START` release at that moment can still pause. `Esc` leaves that pause. A pad-only player has no way out. Follow-up in item `012`. A new issue file at
   the merge, with a `TODO` at `queuePause`.
