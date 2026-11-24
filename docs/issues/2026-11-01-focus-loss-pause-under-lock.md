---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-031f-menu-input scan (2026-11-01)
related-items: [012, 013, 031, 070]
---

# A focus-loss or minimize pause under a lock gives a pause menu that a pad cannot use

## Where

- `lwjgl3/src/main/kotlin/com/megaman/maverick/game/lwjgl3/DesktopLauncher.kt:178` and `:186`: the pause on focus loss and on minimize, with `--pauseOnFocusLost` and `--pauseOnMinimized`, both off by default

Found by reading the code. Candidate 5 of the item `031` `NOTES.md`. Not run in the game for a pad.

## What happens

With one of the 2 flags on, a focus loss or a minimize during a lock pauses the game. The slot frame is `RELEASED` under the lock, so the pause menu reads nothing from a player's buttons. The pause stops `cameraController.update`, so a room transition does not end and the lock does not end.

With `031f`, `Esc` leaves this pause, because the system frame is not locked. A pad-only player has no way out.

## What should happen

The player has a way out on each device.

## How to reproduce

`./run-mega-man-maverick-alpha.sh --level REFACTOR_TEST_051 --room basic --pauseOnFocusLost`: run right into `basic_right`, and press `Alt`+`Tab` during the camera move. The pause menu opens, and only `Esc` responds.

## Notes

Check L6 of the `031f` contract is the keyboard form of this route. A pad back button (item `070`) would close the gap.
