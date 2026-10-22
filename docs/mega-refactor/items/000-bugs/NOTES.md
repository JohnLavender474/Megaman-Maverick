# Notes: 000 Bugs found during the review

The bug-tracking options come from the testing review (2026-10-22). The full text is below.
The bug evidence is in the review notes of the related items, for example
`items/012-named-freezes/NOTES.md` (pause and stats-fill bugs),
`items/019-spawn-phase/NOTES.md` (dropped room-transition event) and
`items/036-checkpoints-as-data/NOTES.md` (the "4" spawns and the stale room read).

## Review: Testing, CI and bug tracking (2026-10-22)

Copied as written from the review section "BUGS: how to track them".

````text
(1) Current state
- `origin` is `https://github.com/JohnLavender474/Megaman-Maverick`. It is public, issues are enabled, there is 1 open issue, and only the default labels exist (`bug`, `enhancement`, `documentation`, ...).
- `gh` is not installed on this machine. A Claude session can read public issues through the REST API, but cannot create or update them without `gh` (and auth) or a GitHub MCP server.
- A local branch `fix/pool-identity-freeset` exists, so a `fix/` branch convention is already in use.
- The refactor rule "gameplay is the contract" conflicts with bug fixes, because a fix changes gameplay on purpose.

(2) Options
- A. GitHub Issues. Pros: an ID (`#12`) that branches and commits can reference. `Fixes #12` in a PR closes it on merge to `master`. No merge conflicts between parallel branches. Labels and filters. Comments keep the history. Cons: not in the repo files. Claude needs `gh` or MCP to write. The data lives off-repo, but GitHub holds it.
- B. `docs/issues/` with one Markdown file per bug. Pros: in-repo, versioned with code, readable by any session with no tools. Cons: ID allocation collides across parallel branches unless IDs are date-slug based. A status change on a branch conflicts with `master`, unless the rule is "issues change only on `master`", the same as `BACKLOG.md`. No auto-close.
- C. One `BUGS.md`. Pros: simplest. Cons: every edit touches one file, so it conflicts the most. It grows without limit, and there is no history per bug.
- D. A section in `BACKLOG.md`. Cons: mixes bugs with refactors (the owner wants them separate), is master-only, and has no IDs.

(3) Recommendation: option A, GitHub Issues.
- Labels: `bug`, `verified` or `unverified`, `area:engine`, `area:core`, `area:content`, `gameplay-change` (the fix changes how the game plays), `found-by:mega-refactor`.
- Install `gh` and authenticate it for Claude sessions. This is an owner action.
- Add one line to `CLAUDE.md`: bugs go to GitHub Issues, refactor ideas go to `BACKLOG.md`. A branch that finds a bug writes it in its `docs/TODO.md` under "Bug candidates", and the issue is filed from there.
- Contract rule:
  - A bug fix goes in its own `fix/<issue#>-<slug>` branch off `master`, with a short contract: what changes and what must not change. Refactor branches then keep "plays the same as master".
  - If a refactor branch must change buggy code (for example item 16/17 rewrites `CullablesSystem`), its `CONTRACT.md` lists `#N` as an allowed behavior change, and the PR says `Fixes #N`.
  - Golden files (item 4) make such a change a visible diff.
- Minimal format, as `.github/ISSUE_TEMPLATE/bug.md`:
  - Title: the symptom in one line
  - Where: `path:line`
  - What happens / what should happen
  - How to reproduce: level, room, steps, or a test
  - Status: verified (how) / unverified
  - Gameplay effect of the fix: none / describe
  - Found by: branch or session
- Fallback if the owner wants everything in-repo: option B with filenames `docs/issues/YYYY-MM-DD-<slug>.md` (no number collisions), a front-matter `status:` line, files created and closed only on `master`, and branches that reference the filename.

(4) Open questions
- Is the owner fine with bug data outside the repo files, and with installing and authenticating `gh` for Claude?
- File the 5 verified bugs now (`CullablesSystem.kt:26`, `InsertionOrderPriorityQueue.size`, `MutableOrderedSet.retainAll`, `GameState.kt:293`, `MegaLevelScreen.kt:570`) and the 2 unverified ones with the `unverified` label?
- Are some "bugs" behavior that players now rely on? Then the fix needs a play check first.

(5) Wording: no backlog item. Add one short rule to `docs/mega-refactor/CLAUDE.md` and to the root `CLAUDE.md`: "Bugs: GitHub Issues with label `bug`; fix in `fix/<n>-<slug>`; a refactor branch that changes buggy behavior lists `#n` in its `CONTRACT.md`."

(6) Dependencies: none. Item 1 CI on `fix/*` branches makes fix branches safe.
````

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "Summary of the bugs from this review (for the bug-tracking discussion)".

````text
- Confirmed by reading: pause during gate opening re-enables World, Motion, Behaviors and Pathfinding on resume (`LevelStateHandler.kt:26`). The gate does not lock input, so the pause is reachable.
- New, found by reading: when a health fill ends during a pause, all systems turn on while the game is still paused (`MegaLevelScreen.kt:894`, `PlayerStatsHandler.kt:317`).
- Refuted (normal input): pause during the boss health fill. The controller poller is off.
- Possible, needs a runtime check: a `SpawnerForEvent` drops `BEGIN_ROOM_TRANS` for enemies that were destroyed by the room transition (`MegaLevelScreen.kt:296`, `SpawnerForEvent.onEvent`).
- Smaller issues:
  - `runQueue` runs LIFO (`MegamanMaverickGame.kt:420`).
  - `OrderedSet.random()` reorders the set.
  - `superRandom` reseeds from the clock on each call.
  - The frame delta is not clamped.
````

## Review: Rendering, backgrounds, assets and platforms (2026-10-22)

Copied as written from the review section "Extra findings for the bug list (verified)".

````text
- `FloatExtensions.kt:5` `Float.round` throws in comma-decimal locales. It has no callers today.
- `engine/build.gradle` pins `gdx-freetype:1.11.0` against `gdxVersion=1.14.2`, and it pulls the unused `gdx-tools:1.6.3`.
- The `section` property of a TMX background is parsed but has no effect on draw order (all backgrounds are drawn first, `MegaLevelScreen.kt:976`).
````

## Review: Tooling, saves, settings, logging, profiling and asset hygiene, new facts F1-F11 (2026-10-22)

````text
REVIEW: items 6, 7, 8, 33, 38, 39, 42, 43 (tooling, saves/settings, logging, profiling, asset hygiene)

Scope: I only read files. I edited nothing and did not run the game or the build. The only tool I ran was `TexturePacker --version` (7.9.0 is installed at /usr/bin).

New facts found during this review (verified):
- F1. Saved keyboard bindings are not applied at boot. `MegamanMaverickGame.kt:261` calls `loadControllerButtons()`, which uses only the default keys (`ControllerUtilsExtensions.kt:17-35`). The saved keys are read only when the player selects "load saved settings" (`ControllerSettingsScreen.kt:185`). Controller prefs load on connect only after that screen sets `loadPrefsOnConnect = true` (`ControllerSettingsScreen.kt:224`, `MegaControllerPoller.kt:30`).
- F2. `selectButtonAction` resets to `NEXT_WEAPON` at every boot (`MegamanMaverickGame.kt:263`), but the value is saved in prefs.
- F3. Volume can change in game (`GameSettingsPanel.kt:177-215`), but it is not saved. The CLI default is 0.8 (`DesktopLauncher.kt:38-39`) and the params default is 0.5 (`MegamanMaverickGame.kt:126-127`).
- F4. The save keeps only which health tanks you have, not how full they are (`GameState.kt:273`). Load puts them back empty (`putHealthTank` default `MIN_HEALTH`, `GameState.kt:85`).
- F5. `DesktopLauncher.kt` (the `logLevels` parse) has the same bug as `GameState.kt:293`: `replace("\\s+", "")` treats the pattern as literal text, not a regex.
- F6. `TAGS_TO_LOG` is an empty set in code (`MegamanMaverickGame.kt:166`), and `filterByTag = true` (`:253`). So no debug line prints unless you edit code, but all the debug strings are still built.
- F7. Dev runs (`./gradlew lwjgl3:run`) use the default GC (G1). Release builds use ZGC (`build-game.sh:86` `--useZgcIfSupportedOs`, `--vmargs Xms512M Xmx1024M`) on JRE 17. Dev and release measure different collectors.
- F8. Packr bundles all of `assets/*` (`build-game.sh:92`). This includes `sprites/frames` (16 MB), `piskel` (4.2 MB), `texture_packer`, all 87 TMX files (18 live, `LevelDefinition.kt`) and the text notes. The JAR leaves out music, sprites and maps (`lwjgl3/build.gradle:59-77`), so the size problem is in the packr bundle, not in the JAR.
- F9. 12 of 86 `.tps` files point to source folders that do not exist: `../Frames/...` and `../SpriteSheets/...` (wrong case), `../frames/Bosses`, and another repo (`RocketPartnersGame`). Examples: `FreezerMan.tps`, `MegamanMaverick.tps`, `AstroPlayer.tps`. 3 atlases have no `.tps`: `FridgeMan`, `MegamanChargedShot`, `MegamanMaverick_BUSTER`.
- F10. Music check: 27 files (37 MB) in `assets/music` have no name in `MusicAsset.kt`. The brief says ~83 MB. That number probably also counts enum entries that no code uses. Check before you write the number in the backlog.
- F11. `WILY_CASTLE_SCREEN` is in `ScreenEnum` but is not registered in the screen map (`MegamanMaverickGame.kt:381-398`).
- Note: the project memory says "no tests in core". That is out of date: `core/src/test` has tests, for example `MegaControllerPollerTest`.
````
