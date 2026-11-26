# Plan: mega-refactor-007a-json-save-slots

How to do the work in `SCOPE.md`.

## New API and its first caller

| API | First production caller |
|-----|-------------------------|
| `SaveStorage.read`, `write` | `SaveSlots.read`, `SaveSlots.write` |
| `FileSaveStorage`, `PreferencesSaveStorage` | `MegamanMaverickGame.createSaveStorage()` |
| `SaveFile.of`, `applyTo` | `SaveSlots.write`, `MegamanMaverickGame.loadSavedState` |
| `SaveFileJson.write`, `read` | `SaveSlots` |
| `SaveSlots`, `SaveSlotContent` | `MegamanMaverickGame`, `SaveSlotScreen` |
| `IGameTitle.saveFolderName` | `MegamanMaverickGame.create()` |
| `MegamanMaverickGame.activeSaveSlot` | `SaveSlotScreen` (cursor start), `MainMenuScreen` (new game clears it) |

`SaveStorage` has 2 production implementations. The in-memory backend is test code.

## Steps

### 1. Branch docs

- Changes: `SCOPE.md`, `PLAN.md`, `NOTES.md`, `TODO.md`, process log.
- Check: docs-only commit.
- Done: yes

### 2. Storage and schema

- Changes: `state/saves/` package: `SaveStorage`, `FileSaveStorage`, `PreferencesSaveStorage`, `SaveFile`,
  `SaveFileJson`, `SaveSlots`, `SaveSlotContent`, package `README.md`.
- Check: compiles; tests of step 4.
- Done: yes

### 3. Game and UI

- Changes: `IGameTitle.saveFolderName` (both titles), `MegamanMaverickGame` slot calls, `SaveSlotScreen`, `ScreenEnum`,
  `SaveGameScreen`, `MainMenuScreen`; remove `GameState.fromString`, `PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE`,
  `GameStateSaveStringTest`; `--saveFile` help text; `titles/README.md`.
- Check: compiles; review of the manual checks in `SCOPE.md`.
- Done: yes

### 4. Tests

- Changes: `core/src/test/.../state/saves/`: `InMemorySaveStorage`, `SaveSlotsTest`, `SaveFileJsonTest`,
  `FileSaveStorageTest`.
- Check: `core:test` (see the process log for whether it ran).
- Done: yes
