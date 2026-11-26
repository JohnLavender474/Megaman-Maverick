# 007: JSON save slots

- Status: active
- Type: feature
- Objectives: `O9`
- Depends on: none
- Related: `008`, `025`, `029`, `032`, `034`, `047`
- Branches: `mega-refactor-007a-json-save-slots`

## Goal

Replace the positional save string in the XML prefs file with JSON save slots. The new logic is fresh: it does not read, import or keep compatible with the old save (decision 2026-11-26).

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A `SaveStorage` interface with desktop (files), Android (local) and web (Preferences) backends.
- 5 fixed slots `saves/slot-1.json` to `saves/slot-5.json` (decision 2026-11-26). A schema with a `version` field, so that a later change has a hook, and a safe write with `.tmp` and `.bak`. Refuse a `version` newer than the game.
- No legacy import, no `LegacySaveV0` parser, no migration chain and no compatibility fixtures. The old XML prefs file is not read.
- A slot picker in the UI for load and save.

## Owner's input

Answer to review item 6 (2026-10-22, verbatim):

> Yes, JSON would be preferable as opposed to the current setup which pulls an XML prefs file. Being able to support multiple save slots would be ideal too. Can we leverage versioning to maintain backwards compatibility with the XML prefs file?

## Decisions

- Owner (2026-11-26): backwards compatibility is no longer necessary. The new logic is fresh, with no baggage: no legacy import, no frozen v0 parser, no migration chain, no compatibility tests with legacy strings. This replaces the legacy-import part of the review proposal and of the 2026-10-22 answer. The `version` field stays, so a refusal of newer saves and later migrations stay possible.
- Owner (2026-11-26): the game has 5 save slots in total. This replaces the "slot count is a config value, default 10" decision of 2026-10-22. First-pass branch: `mega-refactor-007a-json-save-slots`.

- I (2026-10-22): the slot count is a config value, default 10. It must scale with no hard limit (`SaveStorage.list()` finds the slots; the UI pages).

- 2026-10-23 (from `029` and `047`): a save does not depend on the player count. Two players can play a save on one day, and one player can continue it on the next day. The save holds one session profile (what the lead player earns is shared), not one profile for each player.
- 2026-11-01: co-op in the main campaign is now optional, and it comes after the dedicated modes (`110`). The 2026-10-23 decision above applies to that item. `102` asks if the dedicated modes touch the save file.

## Open questions

- Copy and delete slots in the UI?
- Save how full each health tank is? Today the fill is lost on load. Save more (lives, weapons)?
- Desktop folder: `~/.megaman-maverick/` or the OS app-data folder?

## Sources

- `NOTES.md` in this folder.
