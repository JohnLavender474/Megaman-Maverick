# 007: Versioned JSON save slots with legacy import

- Status: planned
- Type: feature
- Depends on: none
- Related: `008`, `025`, `032`, `034`
- Branches: none yet

## Goal

Replace the positional save string in the XML prefs file with versioned JSON slots, and keep old saves loadable.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A `SaveStorage` interface with desktop (files), Android (local) and web (Preferences) backends.
- Fixed slots `saves/slot-N.json`, a schema with `version`, a safe write with `.tmp` and `.bak`, and unknown names kept, not dropped.
- A migration chain from v0 (the legacy prefs string, through a frozen `LegacySaveV0` parser) to v1. Refuse versions newer than the game.
- Import the legacy prefs save into slot 1 once. Never change or delete the XML file.
- Compatibility tests with fixture strings and golden JSON files.

## Owner's input

Answer to review item 6 (2026-10-22, verbatim):

> Yes, JSON would be preferable as opposed to the current setup which pulls an XML prefs file. Being able to support multiple save slots would be ideal too. Can we leverage versioning to maintain backwards compatibility with the XML prefs file?

## Open questions

- How many slots? Copy and delete slots in the UI? (Decision I.)
- Save how full each health tank is? Today the fill is lost on load. Save more (lives, weapons)?
- Desktop folder: `~/.megaman-maverick/` or the OS app-data folder?
- Keep the legacy file for ever, or offer a clean-up option later?

## Sources

- `NOTES.md` in this folder.
