---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: owner play test of `DESERT_MAN` on `master` (2026-11-16)
related-items: []
---

# All level backgrounds stop drawing after a pixel-perfect change in game

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/drawables/backgrounds/Background.kt`: `onEvent` makes a new
  `FitViewport` or `PixelPerfectFitViewport` on `TOGGLE_PIXEL_PERFECT` and never calls `update(width, height)` on it
- `core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt`: the game handles the same event first
  (it registers as a listener before the backgrounds) and calls `resize()`, which updates the old background viewports
- `core/src/main/kotlin/com/megaman/maverick/game/screens/utils/GameSettingsPanel.kt`: the pixel-perfect row, also in
  the level pause screen

## What happens

After a pixel-perfect change in the pause menu, each `Background` has a new viewport with a 0×0 screen size.
`viewport.apply()` in `draw` sets a 0×0 GL viewport, so no background draws until the next window resize. A room that
tiles fill hides the fault; the next open room shows no background. The owner saw it at the `room14` → `room15` exit of
`DESERT_MAN`: no sunset and no sky.

## What should happen

The backgrounds draw as before the change, in the new viewport mode.

## How to reproduce

`./run-game-alpha.sh --level DESERT_MAN --room room14`: pause, change the pixel-perfect setting, resume, and walk
right into `room15`. Minimize and restore the window: the backgrounds come back.

## Short contract

Branch: `fix/background-pixel-perfect`, off `master`, in `.worktrees/fix/background-pixel-perfect`.

Intended changes:

- `Background.onEvent` calls `viewport.update(Gdx.graphics.width, Gdx.graphics.height)` on the new viewport, the same
  size that the game's `resize()` uses.

Not changed:

- The game and UI viewports, the order of the event listeners, the background draw order, parallax and rotation.
- The background viewport at level start (the screen `resize` sets its size as before).

Checks:

- A new unit test in `core` fails without the fix and passes with it: after `TOGGLE_PIXEL_PERFECT` (both values), the
  background viewport has the screen size of `Gdx.graphics`.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- Play check (experimental): `./run-game-alpha.sh --level DESERT_MAN --room room14`: pause, change pixel-perfect,
  resume, walk into `room15`: the sky and the sunset draw. Change it back and look again.
- Play check (control): `./run-game-alpha.sh --level DESERT_MAN --room room14` with no setting change: the sky and the
  sunset draw in `room15` as before.
