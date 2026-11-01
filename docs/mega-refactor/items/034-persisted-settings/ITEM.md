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
- Do not import the legacy keyboard and controller prefs (owner, 2026-10-25, from `031`): bindings start from the
  defaults, and players set them again once. Leave the old files unchanged.
- Saved bindings do not apply at boot (owner, 2026-10-30, from `031`). The saved select action applies at boot since
  `031e`.
- Input of this item: `PlayerSettingsStore` and the slot file `Megaman Maverick - Player Settings - Slot <n>` from `031e`
  hold the bindings and the select action.
- Save volume changes. (Today they are not saved; see `000`.)

## Owner's input

Answer to review item 33 (2026-10-22, verbatim):

> yes

## Decisions

- 2026-10-25 (from `031`): no migration of the legacy binding prefs. Players set their bindings again once.
- 2026-10-30 (from `031`): the "load saved settings" button is on purpose. Saved bindings do not load by default, so that
  a bad mapping cannot make a controller unusable. `--loadKeyboardPrefs` stays as a dev opt-in.

## Open questions

- Which values does the player change in game, and which are dev-only?

## Sources

- `NOTES.md` in this folder.
