---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor-031d-devices play test (2026-10-30)
related-items: []
---

# `RobotMasterIntroScreen.dispose` logs an error at shutdown after an uncaught exception

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/other/RobotMasterIntroScreen.kt:639` (`dispose`)

(`master` `1006260c9`)

## What happens

After an uncaught exception ended the game loop, `MegamanMaverickGame.dispose()` logged this error:
`Failed to dispose screen: RobotMasterIntroScreen | Cannot invoke "com.badlogic.gdx.utils.IntMap.remove(int)" because
"this.sourceToSoundId" is null`. The stack goes through `MegaAudioManager.stopMusic` to
`OpenALLwjgl3Audio.freeSource`. `dispose()` catches the exception, so the shutdown goes on.

## What should happen

The screen stops its music with no error at shutdown.

## How to reproduce

Seen once, in the log of the `031d` crash in check `D3`. That crash is fixed, so another uncaught exception in the
game loop is necessary to see this again.

## Notes

The cause is not confirmed. The message suggests that the audio backend is already gone when the screen disposes. Only
the shutdown after the crash showed it.
