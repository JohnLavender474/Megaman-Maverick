---
status: closed
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-031f-menu-input scan (2026-11-01)
related-items: [031, 012, 013, 082]
---

# A `START` release at the start of a lock pauses the game, and the pause cannot be left

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt`: `isPauseButtonRequested()` and the pause test in `render()`
- `engine/src/main/kotlin/com/maverick/game/engine/input/InputFrameBuilder.kt`: a locked build gives `JUST_RELEASED` for a held button
- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/LevelRoomHandler.kt`: `TURN_CONTROLLER_OFF` and `TURN_CONTROLLER_ON` around a room transition

Confirmed by reading the code on `master` at `c6b8cecf7`. Not run in the game.

## What happens

Frame `N` is the frame where a room transition begins and submits `TURN_CONTROLLER_OFF`. `events.run()` sets `locked` at the end of frame `N`.

- **Trigger 1:** `START` is held at the build of frame `N`. In `N+1`, the locked build gives a false `JUST_RELEASED`, the pause test passes, and the pause runs in `N+2`.
- **Trigger 2:** a real `START` release in frame `N`. The pause test runs before `locked` is set, and the pause runs in `N+1`.

In both cases all later frames are `RELEASED`, so the pause menu reads nothing. The pause stops `cameraController.update`, so the transition does not end and `TURN_CONTROLLER_ON` never comes. The game cannot continue. With default flags there is no way out.

The boss-defeat lock (`MegaLevelScreen.kt:754`) and the `WilyFinalBoss.kt:311` lock follow the same path.

## Origin

- `6aebae6c2` (2025-02-15): pause changed from press to release through `runQueue`, and the dead pause menu began.
- `39f4d3629` (2025-03-01): the last default way out went.
- `903c5d145` (2025-03-12): closed the way out that `c5339961f` had opened again.

`031a` and `031c` moved this logic and changed no behavior.

## How to reproduce

`./run-mega-man-maverick-alpha.sh --level REFACTOR_TEST_051 --room basic`: wait for control, hold `Enter`, run right into `basic_right`, and keep `Enter` held as the transition begins. The pause menu opens by itself, and no input responds.

## Notes

The window is one frame per lock start, so a tap rarely hits it. A held key hits it every time.

`mega-refactor-031f-menu-input` fixes both triggers as an intended change: the pause test and the queued pause each test the lock. One case stays: a lock submitted from a listener, for example `MegaLevelScreen.kt:754`, starts 1 frame later, so a real release at that moment can still pause (inference, from reading). `Esc` leaves that pause. A pad-only player has no way out.

## Resolution

Fixed by `df7028ed3` (`mega-refactor-031f-menu-input`). `isPauseRequested` tests the lock, which covers trigger 1. `queuePause` tests the lock again when the queued pause runs, which covers trigger 2. Automated tests T19, T26, T32 and T33 prove both. T19 and T32 failed before the code, and T32 failed on a mutation that reads the lock when the runnable is added.

`verified` stays `no`, because no run of the old code exists. The manual checks L1 and L2 have no recorded result: the owner ran some keyboard-only checks, found no issue and did not name them.

One case stays open: a lock that a listener submits, for example `MegaLevelScreen.kt:753`, starts 1 frame later, so a real release at that moment can still pause. This is an inference, from reading. `Esc` leaves that pause. A pad-only player has no way out. See `2026-11-01-listener-lock-start-release-pauses.md`.
