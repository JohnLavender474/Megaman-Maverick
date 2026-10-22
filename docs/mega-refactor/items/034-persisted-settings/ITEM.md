# 034: Persisted settings file

- Status: planned
- Type: feature
- Depends on: `007`
- Related: `008`, `031`, `032`
- Branches: none yet

## Goal

Save settings that today come only from CLI flags or are lost on quit (volume, bindings, select action).

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `settings.json` with `version`, stored through the `SaveStorage` of `007`.
- Order: defaults, then the file, then CLI flags (CLI values are not saved).
- Import the legacy keyboard and controller prefs once. Leave the old files unchanged.
- Apply the saved bindings and select action at boot. Save volume changes. (Today they are not applied or not saved; see `000`.)

## Owner's input

Answer to review item 33 (2026-10-22, verbatim):

> yes

## Open questions

- Is the "load saved settings" button on purpose, or should saved bindings always apply at boot?
- Which values does the player change in game, and which are dev-only?

## Sources

- `NOTES.md` in this folder.
