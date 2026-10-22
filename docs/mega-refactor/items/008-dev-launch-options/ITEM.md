# 008: Dev launch options and level staging

- Status: active
- Type: tooling
- Depends on: none
- Related: `003`, `007`, `011`, `032`, `034`, `036`, `044`
- Branches: `mega-refactor-008a-dev-launch-options` (active, started 2026-10-22)

## Goal

Start the game on a chosen screen, level, room or save from the command line, with clear errors.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- A platform-neutral `LaunchOptions` that `DesktopLauncher` fills. Checks with clear errors and exit code 2.
- `--screen`, `--level`, `--tmx`, `--room`/`--checkpoint`, `--slot`/`--state`, `--difficulty`, `--logTags`, `--strictErrors`, `--autoPerf`, `--seed`.
- Staging: a non-level screen with a level stages the level until something overwrites it. `--room` applies one time only.
- Deprecate `--runType test`. Fix the `logLevels` parse bug (literal `\\s+`).
- TMX hot reload: a reload-and-restart key first, then a desktop file watcher.

## Owner's input

Answer to review item 7 (2026-10-22, verbatim):

> Yes, I want to be able to configure which screen, level, etc. are loaded. If the screen in the arg is NOT the level screen but a level arg is supplied, then the level should be "staged", i.e. next load of level screen loads that level (assuming no other process has since overwritten the staged level); from what I learned, the X6 game simply persists a staged level indefinitely, which happens to be what Maverick does too (saving the level into the `MegamanMaverickGame` class)

## Open questions

- Should `--room` also stay staged, or apply one time only?
- May a dev run with `--state` or `--slot` write to a save slot?
- Which screens are valid start points?

## Sources

- `NOTES.md` in this folder.
