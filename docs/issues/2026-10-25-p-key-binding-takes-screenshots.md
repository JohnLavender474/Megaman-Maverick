---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor 031 bindings discussion (2026-10-25)
related-items: [031]
---

# Players cannot bind a button to the `P` key without causing screenshots

## Where

- `lwjgl3/src/main/kotlin/com/megaman/maverick/game/lwjgl3/DesktopLauncher.kt` (the `TODO` and the
  `screenshotTrigger` line in the params setup)
- `core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt` (`params.screenshotTrigger` in `render`)

(`master` `642c8eeab`)

## What happens

With `--allowScreenshots`, the launcher sets the screenshot trigger to `Gdx.input.isKeyJustPressed(Input.Keys.P)`.
The keyboard settings screen does not refuse `P`. If a player binds a game button to `P`, each press of that button
also writes a screenshot file. The game action still happens, but each press also writes a PNG to disk, which can
cause a short hitch.

`--allowScreenshots` is off by default, and no run script sets it, so normal players do not see this today.

## What should happen

Owner's decision (2026-10-25): keep `P` as the screenshot trigger for now. Later, either move the trigger to a key
that the bindings screen refuses (a system key), make it a binding, or remove it. Until then, the bindings screen
could refuse `P` while `--allowScreenshots` is on.

## How to reproduce

Start the game with `--allowScreenshots`. In the keyboard settings, bind a button (for example `A`) to `P`. Play
and press `P`: the button acts, and a `screenshot_<time>.png` file appears.

## Notes

Item `031` tracks the reserved keys and the bindings work.
