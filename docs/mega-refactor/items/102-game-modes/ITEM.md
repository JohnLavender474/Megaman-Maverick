# 102: A game mode concept

- Status: idea
- Type: feature
- Objectives: `O8`, `O2`
- Depends on: none
- Related: `047`, `060`, `068`, `081`, `108`, `109`
- Branches: none yet

## Goal

The code has only `DifficultyMode` and `LevelType.EXTRA_LEVEL`. Character items refer to a classic mode and to extra levels, but no such concept exists. Decide if a mode is necessary, and what it holds.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- None yet.

## Owner's input

The objectives audit of 2026-11-01 found this gap.

2026-11-01: the owner's words on Spec Ops and PvP are in `108`, `109` and `110`.

## Decisions

- 2026-11-01: co-op and multiplayer are extra modes, not part of the main game. Players play co-op or PvP on one machine or on the net. Spec Ops (`108`) and PvP (`109`) are the two named modes. Co-op in the main campaign is optional, and it comes after them (`110`).
- 2026-11-01: `047b` goes first. It builds a small rules object, chosen by CLI flags, and this item extends it. The co-op rules are toggles with a value for single player, co-op and PvP.

## Open questions

### What a mode is

- What does a mode hold, and where does it live? `LevelType` mixes content and rules: `EXTRA_LEVEL` is a gauntlet and also the X6-style target (`068` `NOTES.md`), and `MARIO_LEVEL` changes the HUD (`MegaLevelScreen.kt:986`). Options: a field on `LevelDefinition`, an extended `LevelOverride`, a new session object.
- Are the co-op rules part of a mode? `LivesPolicy` is "by game mode" (`047` decision H), but no mode exists. The same applies to the 3 death options, friendly fire and the target rule. Who chooses them, and can they change when a player joins?
- Which characters can each mode use (`O8-R3`, `081`, `060`)? Can each slot pick its own character (`113`)?

### Menu and start

- Where is the menu entry? The main menu has `EXTRAS`, and `ExtrasScreen` lists each `EXTRA_LEVEL` with no player choice (`ExtrasScreen.kt:59`). Options: rows in `ExtrasScreen`, a new `ScreenEnum` value for each mode, one new lobby screen.
- How does a mode start? `game.startExtraLevel(level)` applies the override and calls `startLevel` (`MegamanMaverickGame.kt:833`). `047a` makes slot 1 at startup from `--players`, in `postCreate()`. A menu start must make or remove players at run time, and `Players.unregister` has only test callers.
- Do players join in a lobby before a mode starts, or during play? `O2` says that local players can join and leave during play. A lobby can replace the join flow of `047f`. A slot needs a menu device, and `MegaPlayers.menuInput` is slot 0 only.
- Who chooses a local or a net session in the menu, and does a net session use the same screens (`095`)?

### Progress

- Do the extra modes connect to the save file? Today they do not: `startExtraLevel` applies the override or calls `resetPlayerProgress()`, `BACK` in `ExtrasScreen` also resets, and an extra level never counts as defeated (`MegaLevelScreen.kt:813`, `GameState.kt:379`).
- Do the modes use the weapons of the main game? Options: the weapons in the loaded save, a fixed loadout per mission (`LoadoutOverride`), all weapons, a pick per match. `resetPlayerProgress()` clears the session weapons, so a mode that starts from a loaded save in memory loses them.
- Do the modes have progress of their own (mission results, unlocks)? `007` has no field for this, and its 2026-10-23 decision holds one session profile.

The questions about mission content are in `108`. The questions about PvP are in `109`.

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
