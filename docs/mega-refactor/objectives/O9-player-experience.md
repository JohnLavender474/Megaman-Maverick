# `O9`: Player experience

- Status: active

## Problem

- `P1`: The save is a positional string in an XML prefs file (`007`). A change to the format can lose a player's
  progress.
- `P2`: Some settings are lost on quit or come only from CLI flags (`034`).
- `P3`: Control settings are too restrictive for a modern pad or a keyboard (`070`).
- `P4`: The game has no dialogue or cutscenes (`082`), and some areas look unfinished, for example
  the levels whose tilesets come from free asset packs (`078`).

## Goal

The parts of the game around the levels feel complete: saves, settings, controls, presentation and story.

## Done means

- A save from an old version loads in a new version.
- A setting that a player changes is still there after a restart.
- A player can use the extra buttons of a modern pad, and the game stays fully playable with an NES-style pad, except
  as X.
- The game has dialogue scenes and cutscenes.

## Rules

### `O9-R1`: NES-style pad

- Rule: the game stays fully playable with only the d-pad, `A`, `B`, `START` and `SELECT`. Exception: playing as X
  needs a dash button, so these buttons are not enough for X.
- Problem: `P3`
- Check: the branch contract names a check with those buttons, if the branch changes controls.
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
| `O9-R1` | `partly` | The input enum has 8 buttons only (`MegaControllerButton.kt:8-15`), so no action can need another. 3 of 4 branches that change controls have a check. `fix/left-right-last-pressed` has none. On 2026-11-01 the `fix/` rule gained named checks. `031f`: `Esc` adds back to the keyboard only. Pad players keep the back rows, but they have no back on `LevelSelectScreenV2` and no way out of a pause under a lock (2 open issues of 2026-11-01). | `070` |
| `O9-R2` | `partly` (waiting) | The format is positional with no version (`GameState.kt:344-361`). Short strings load through guards (`:376-422`). `GameStateSaveStringTest.kt:12` covers only the current format. | `007` |
| `O9-R3` | `broken` (waiting) | Volume, pixel perfect, vsync and performance are not stored (`GameSettingsPanel.kt:41-45`). Keyboard bindings load at boot only with `--loadKeyboardPrefs`, default off. | `034` |

## Redo risk

- `031e`: `PlayerSettingsStore` moves to `settings.json` in `034`. This is planned.
- `134`: render interpolation is on by default. `134b` fixed the children placed on a parent outside the world steps.
  Left for `147`: blocks that a player rides and that move outside the steps, and bodies that move once per frame
  (for example `D1000`), which hitch against the view.

## Items

| Item | Status | Note |
|------|--------|------|
| `007` | planned | [JSON save slots](../items/007-json-save-slots/ITEM.md) (main) |
| `034` | planned | [Persisted settings file](../items/034-persisted-settings/ITEM.md) (main) |
| `070` | idea | [More flexible controller settings](../items/070-flexible-controls/ITEM.md) (main) |
| `076` | done | [Better tileset art for every level](../items/076-final-stage-tileset/ITEM.md) (main) |
| `165` | dropped | [Tiled automapping rules for the Desert Man tileset](../items/165-desert-tileset-automapping/ITEM.md) (main) |
| `166` | dropped | [Desert Man's stage rework (epic)](../items/166-desert-man-stage/ITEM.md) |
| `078` | idea | [Stage redesigns (epic)](../items/078-robot-master-alt-paths/ITEM.md) |
| `180` | idea | [Timber Walker: refine with an artist and put into the game](../items/180-timber-walker-integration/ITEM.md) |
| `082` | planned | [Dialogue boxes, in-level dialogue scenes and cutscenes](../items/082-dialogue-and-cutscenes/ITEM.md) (main) |
| `031` | done | [Input frames per player (`InputFrame`), devices and bindings](../items/031-input-frames/ITEM.md) |
| `113` | idea | [Character select screen](../items/113-character-select/ITEM.md) |
| `129` | idea | [View edge warp visuals, phase 2](../items/129-warp-visuals/ITEM.md) (main) |
| `132` | idea | [Spread the players after a room transition](../items/132-spread-after-transition/ITEM.md) |
| `133` | idea | [A cue that shows who waits at a room exit](../items/133-exit-wait-cue/ITEM.md) (main) |
| `134` | done | [Draw bodies and the camera between physics steps (render interpolation)](../items/134-render-interpolation/ITEM.md) (main) |
| `147` | idea | [Render interpolation for riders and for bodies that move once per frame](../items/147-interpolation-riders-frame-moved/ITEM.md) (main) |
| `152` | idea | [Record the last Wily stage as completed at the ending](../items/152-record-final-stage-completed/ITEM.md) (main) |
| `153` | idea | [A current-weapon icon on every player's HUD, and a HUD that fades when a player is behind it](../items/153-hud-weapon-icon-and-fade/ITEM.md) (main) |
| `154` | dropped | [Spawners test the camera after it moves](../items/154-spawn-after-camera-move/ITEM.md) (main) |
| `159` | done | [New hand and head art for the intro boss](../items/159-intro-boss-hand-art/ITEM.md) |
| `160` | idea | [Ladder shot height at a ladder foot](../items/160-ladder-foot-shot/ITEM.md) (main) |
| `170` | idea | [`UP` and `DOWN` gates in co-op](../items/170-coop-vertical-gates/ITEM.md) |
| `176` | idea | [One level animation clock for tiles and entities](../items/176-level-animation-clock/ITEM.md) (main) |
| `177` | idea | [Backgrounds drawn per room](../items/177-backgrounds-per-room/ITEM.md) (main) |
| `183` | active | [NES-faithful rotation: no sprite drawn at an angle that is not a multiple of 90](../items/183-nes-faithful-rotation/ITEM.md) (main) |
| `184` | dropped | [Keep button taps that are shorter than one frame](../items/184-sub-frame-taps/ITEM.md) (main) |
| `187` | active | [Intro story screen and main menu overhaul](../items/187-intro-story-and-menu/ITEM.md) (main) |
| `189` | planned | [Villain and dog boss fight (two phases)](../items/189-villain-and-dog-fight/ITEM.md) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

- 2026-11-01 (owner): `O9-R1` has an exception for X. The only dash button on an NES-style pad would be `SELECT`, and that is not feasible in real play. See `069` and `081`. `GAUNTLET_1` and `GAUNTLET_2` stay Megaman levels, so the exception affects only the new X6-based extra levels. These levels are X only for now, so an NES-style pad cannot play them. The code must not block Megaman from playing them later (`081`).

## Open questions

- `034` says that saved bindings do not apply at boot (owner, 2026-10-30). `O9-R3` says that a changed setting is still there
  after a restart. One of them must change.
