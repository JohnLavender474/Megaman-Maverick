# Notes: mega-refactor-007a-json-save-slots

## Decisions

Owner (2026-11-26): no backwards compatibility (no legacy import, no v0 parser, no migration chain, no legacy fixtures),
exactly 5 slots as a constant, keep `version` and refuse a newer version without overwriting it.

Made without the owner (2026-11-26, implementation agent):

- **Folder.** Desktop: `Gdx.files.external(".megaman-maverick/saves")`, so `~/.megaman-maverick/saves/`. Android and
  iOS: `Gdx.files.local` with the same path (private app storage). Web and any other platform: one Preferences file
  with that name, one key per slot file name. The title gives the root folder (`IGameTitle.saveFolderName`, which
  replaces `saveFileName`): `.megaman-maverick` and `.mega-man-x-lost-memories`.
- **`--saveFile <name>`** keeps its flag name. It now names a subfolder of the saves folder:
  `~/.megaman-maverick/saves/<name>/slot-N.json`. A new name starts with 5 empty slots, as before.
- **Schema v1:**

  ```json
  {
    "version": 1,
    "savedAt": "2026-11-26T18:02:11.123Z",
    "progress": {
      "levelsCompleted": ["INTRO_STAGE", "MOON_MAN"],
      "heartTanks": ["A"],
      "healthTanks": ["B"],
      "enhancements": [],
      "currency": 0,
      "difficulty": "NORMAL"
    }
  }
  ```

  Names are written in enum order, so the same state always gives the same text. `savedAt` is ISO-8601 UTC
  (`java.time.Instant`). The health tanks are names only, as in the old save: a load puts them back empty (open
  question of the item).
- **Unknown names.** A name that matches no enum entry is logged with `GameLogger.error` and skipped. It is not kept:
  the next save of that slot drops it. Simplest option; the `unknown` field of the review is not added. An unknown
  difficulty gives `NORMAL` with an error log. An `EXTRA_LEVEL` in `levelsCompleted` is skipped, as `fromString` did.
- **Read result.** A slot is `Empty` (no file), `Used` (a `SaveFile`), `NewerVersion` (version above 1) or `Unreadable`
  (not JSON, no `version`, or a field of the wrong type). Load works only on `Used`. Save refuses `NewerVersion` (the
  file is never touched) and is allowed on `Unreadable` (the old file goes to `.bak`).
- **Safe write (files only).** Write `slot-N.json.tmp`, move `slot-N.json` to `slot-N.json.bak` (the old `.bak` is
  deleted first), then move the `.tmp` to `slot-N.json`. A read falls back to `.bak` when `slot-N.json` is missing (a
  crash between the two moves). A corrupt `slot-N.json` does not fall back; the `.bak` stays for a manual restore.
  The Preferences backend writes in one `putString` + `flush`, with no `.tmp` or `.bak`.
- **`SaveStorage` has `read` and `write` only.** No production code lists or deletes slots (the UI reads the 5 fixed
  names, and slot delete is an open question), so `list` and `delete` are not added (rule `O1-R5`).
- **Active slot.** `MegamanMaverickGame.activeSaveSlot` is the slot loaded or saved last in this run; `null` at boot and
  after START NEW GAME. The save list opens with the cursor on the active slot (slot 1 when none). Saving to a used
  slot overwrites it with no confirmation. Not kept across restarts (item `034`).
- **One screen class, two screens.** `SaveSlotScreen(game, mode)` is registered as `SAVE_SLOT_SCREEN` and
  `LOAD_SLOT_SCREEN`. It reuses the `SaveGameScreen` rows (font handles with a text supplier, blinking arrows,
  `menu_screen_bkg_v2`). It does not change the music. Rows: `SLOT N  EMPTY`, `SLOT N  <DIFFICULTY>  <k> LEVELS`,
  `SLOT N  NEWER VERSION`, `SLOT N  UNREADABLE`, then `BACK`.
- **After a save** the screen stays (ping, the row text changes); BACK goes back to the save screen. **After a load** the
  flow of the old LOAD SAVE FILE row runs (level select if `INTRO_STAGE` is completed, else start `INTRO_STAGE`).
- **SAVE with `--heartTanks`, `--healthTanks` or `--difficulty`** writes nothing, as before, and now plays the error
  sound in place of the ping.
- **Removed:** `GameState.fromString()`, `GameStateSaveStringTest`, `PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE`,
  `hasSavedState()`. `GameState.toString()` stays as the debug text (`GameStateLivesTest` reads it).
- **Package README** `state/saves/README.md`: a new package of several types that work together (rule "Package
  READMEs"). `titles/README.md` is updated for `saveFolderName` (rule "Keep it true").

## Docs outside the branch folder that need a change at the merge

- Root `CLAUDE.md`, launcher flags: `--saveFile <name>` "reads and writes the save in the preferences file of that
  name (under `~/.prefs/`)" becomes "keeps the 5 save slots in the subfolder `<name>` of the saves folder
  (`~/.megaman-maverick/saves/<name>/`)". The `--heartTanks`/`--healthTanks`/`--difficulty` text "`saveState()` writes
  nothing" stays true.
- Root `CLAUDE.md`, "Core architecture": the game also owns `saveSlots` and `activeSaveSlot`; the title line "gives a
  game its save and settings files" stays true.
- `objectives/O9-player-experience.md`, `O9-R2` current state at the merge: the format has a `version` field
  (`SaveFileJson.VERSION`), and the owner dropped the old format.

## Overlap with other branches

- `185c-games-layout` (active): plans title seams and may move `MaverickTitle`, the menu screens and
  `LostMemoriesTitle`. This branch changes one property of each title and the two menu buttons. Resolve by moving the
  edits.
- `187a-intro-story-art` (active, art only): no code overlap. `187c` (later) rewrites `MainMenuScreen`; it must keep the
  LOAD SAVE FILE row opening `LOAD_SLOT_SCREEN`.
- `034` (planned): the last used slot can live in `settings.json`.
