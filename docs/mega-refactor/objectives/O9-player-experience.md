# `O9`: Player experience

- Status: active

## Problem

- `P1`: The save is a positional string in an XML prefs file (`007`). A change to the format can lose a player's
  progress.
- `P2`: Some settings are lost on quit or come only from CLI flags (`034`).
- `P3`: Control settings are too restrictive for a modern pad or a keyboard (`070`).
- `P4`: The game has no dialogue or cutscenes (`082`), and some areas look unfinished, for example the final stage
  (`076`).

## Goal

The parts of the game around the levels feel complete: saves, settings, controls, presentation and story.

## Done means

- A save from an old version loads in a new version.
- A setting that a player changes is still there after a restart.
- A player can use the extra buttons of a modern pad, and the game stays fully playable with an NES-style pad.
- The game has dialogue scenes and cutscenes.

## Rules

### `O9-R1`: NES-style pad

- Rule: the game stays fully playable with only the d-pad, `A`, `B`, `START` and `SELECT`.
- Problem: `P3`
- Check: the branch `CONTRACT.md` names a check with those buttons, if the branch changes controls.
- Applies: `always-on`
- Active: `now`

### `O9-R2`: Saves stay loadable

- Rule: a save from an earlier version stays loadable.
- Problem: `P1`
- Check: a test loads a saved example of each old format.
- Applies: `always-on`
- Active: after `007`

### `O9-R3`: Settings persist

- Rule: a setting that a player changes in the game persists.
- Problem: `P2`
- Check: change the setting, restart and read it.
- Applies: `linked items`
- Active: after `034`

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01. Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O9-R1` | `partly` | The input enum has 8 buttons only (`MegaControllerButton.kt:8-15`), so no action can need another. 3 of 4 branches that change controls have a check. `fix/left-right-last-pressed` has none. On 2026-11-01 the `fix/` rule gained named checks. | none needed |
| `O9-R2` | `partly` (waiting) | The format is positional with no version (`GameState.kt:344-361`). Short strings load through guards (`:376-422`). `GameStateSaveStringTest.kt:12` covers only the current format. | `007` |
| `O9-R3` | `broken` (waiting) | Volume, pixel perfect, vsync and performance are not stored (`GameSettingsPanel.kt:41-45`). Keyboard bindings load at boot only with `--loadKeyboardPrefs`, default off. | `034` |

## Redo risk

- `031e`: `PlayerSettingsStore` moves to `settings.json` in `034`. This is planned.

## Items

| Item | Status | Note |
|------|--------|------|
| `007` | planned | [Versioned JSON save slots with legacy import](../items/007-json-save-slots/ITEM.md) (main) |
| `034` | planned | [Persisted settings file](../items/034-persisted-settings/ITEM.md) (main) |
| `070` | idea | [More flexible controller settings](../items/070-flexible-controls/ITEM.md) (main) |
| `076` | idea | [A new tileset for the final stage](../items/076-final-stage-tileset/ITEM.md) (main) |
| `082` | planned | [Dialogue boxes, in-level dialogue scenes and cutscenes](../items/082-dialogue-and-cutscenes/ITEM.md) (main) |
| `031` | active | [Input frames per player (`InputFrame`), devices and bindings](../items/031-input-frames/ITEM.md) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

## Open questions

- `034` says that saved bindings do not apply at boot (owner, 2026-10-30). `O9-R3` says that a changed setting is still there
  after a restart. One of them must change.
