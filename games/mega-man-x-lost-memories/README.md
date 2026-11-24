# Mega Man X: Lost Memories

A second game in this repository. Today it is a splash screen (`MEGA MAN X`, `LOST MEMORIES`,
blinking `PRESS START`). START begins the X6 intro level (`LevelDefinition.X6_INTRO`), played as X.

It reuses the Maverick host game and the shared engine. An `IGameTitle` (`LostMemoriesTitle`)
gives the parts that differ: the save file names, the start screen and the screen redirects.
See `core/src/main/kotlin/com/megaman/maverick/game/titles/README.md`.

## Modules

| Gradle project | Folder | Purpose |
|---|---|---|
| `lost-memories-core` | `core/` | The title and its screens. Depends on `:engine` and `:core`. |
| `lost-memories-desktop` | `desktop/` | The desktop launcher `LostMemoriesLauncher`. Depends on `:desktop-common`. |

## Run

- `./run-mega-man-x-lost-memories-debug.sh` or `./run-mega-man-x-lost-memories-alpha.sh` (from the repo root)
- `./gradlew lost-memories-desktop:jar` writes `Mega-Man-X-Lost-Memories-<lostMemoriesVersion>.jar`.

The game runs from the root `assets/` folder for now. It has no assets of its own.
