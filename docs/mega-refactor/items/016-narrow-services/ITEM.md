# 016: Narrow services instead of `game` everywhere

- Status: active
- Type: refactor
- Depends on: none
- Related: `001`, `011`, `012`, `013`, `021`, `028`, `029`, `031`, `047`, `049`, `053`, `057`, `064`, `072`
- Branches: parent `mega-refactor-016-master` (active, started 2026-10-28); `mega-refactor-016a-typed-level-state` (merged into the parent 2026-10-28, squash `07a4a3297`), `mega-refactor-016b-entity-host-reads` (active, started 2026-10-28, merges into the parent)

## Goal

Entities and systems take one typed services holder in `core` instead of the whole `MegamanMaverickGame`, and typed
fields replace the `game` property bag. Only screens, layer builders, backgrounds and the launcher still name the host.

## Proposed scope

Not approved. Each branch `SCOPE.md` decides its final scope. From the new scan (`NOTES.md`, 2026-10-28, `master` at
`c39eb9a55`): 1,358 `game.` reads in `core` main (875 in entities in 416 files, 457 in screens); 506 files take
`game: MegamanMaverickGame` in a constructor; `MegaEntityFactory` calls `primaryConstructor.call(game)`; 16 test files
mock the whole game; the property bag has 58 calls.

- One holder, working name `MegaServices`, passed to entities in place of the game: `engine`, `events`, `assets`,
  `audio`, `profile` (`GameState`), `players`, `gravity`, `camera` (camera plus the rotating and focus-snapped flags),
  `level` (a session-lived `LevelState`), `clock`, `scheduler` (`updatables` plus `runQueue`), `groups`
  (`GroupsSystem`). A field only with a caller.
- Pooled entities outlive a level, so the holder and every field live for the session. Values that change at run time
  (performance, `paused`, the current level and room) are read when used, not copied.
- `postCreate` builds the holder first; `MegaPlayers()` and `WorldGravity` move earlier.
- Branches:
  1. `016a` `typed-level-state`: the property bag becomes typed `LevelState` fields and camera flags.
  2. `016b` `entity-host-reads`: the 7 host-only reads in entities get a home (`DrWily` screen switch, `Starkner`
     `paused`, 3 `selectButtonAction` reads, 2 debug `getShapes()` calls, `WEAPONS_ATTAINED`).
  3. `016c` `entity-services`: the type swap in about 460 files, the factory, a test helper, and the
     `IMegaGameEntity` cleanup (`game`, `getGameCamera`, `playSoundNow`, `stopSoundNow` and the extensions leave the
     interface). A test builds every pooled class through its primary constructor. No other code branch active.
  4. `016d` `system-services`: contact rules, the pathfinder factory and utilities take the services.
  5. `016e` `service-names` (optional): a mechanical rename of `game` and the member names.
- Not in this item: random (`011`) and the texture cache (`028`) become fields when those items land; the engine host
  contract (`001b`) decides what the engine exposes.

## Owner's input

Answer to review item 15 (2026-10-22, verbatim):

> yes

2026-10-28 (verbatim): "Do a new scan for item 016 since it's out of date (e.g. refers to `PlayerView` which no longer
exists)" and, about the new scan, "Yes, commit the notes and go with your recommendations".

## Decisions

- 2026-10-28 (owner, the scan's recommendations):
  1. One holder, not one service per constructor parameter.
  2. Name: `MegaServices`.
  3. The level part is a session-lived `LevelState`, not a per-level `LevelContext`.
  4. The old `LevelContext<out P : IPlayers<out PlayerView>>` question is dropped (`PlayerView` is gone, `IPlayers` is
     now `IPlayers<out E : IGameEntity>`); no engine context in `016`; `001b` decides what the engine host exposes.
  5. Keep the name `game` and the old member names during the swap; rename later (`016e`).
  6. `getGameCamera`, `playSoundNow` and `stopSoundNow` leave `IMegaGameEntity` in `016c` (they read `game` through
     the interface); they move to `MegaGameEntity`, so no call site changes. This answers the open question of `072`.
  7. Random and the texture cache are out of scope.
  8. Screens keep the host.
  9. (replaced by decision 12) `DrWily`'s screen switch becomes a `LevelState` callback, not an event, so it stays at
     the same point in the frame.
  10. `WEAPONS_ATTAINED` becomes a typed field that is not saved, outside `GameState` (the saved profile).

- 2026-10-28 (owner, verbatim): "Since this is a major change, instead of merging each branch directly to master, can
  we merge to `016-master`?" The branches of `016` squash-merge into the parent branch `mega-refactor-016-master`
  (named by the pattern in `docs/mega-refactor/CLAUDE.md`, "Parent branches"); the parent merges into `master` with
  `--no-ff` when the owner agrees.

- 2026-10-28 (owner, about a `016b` scope proposal to keep `game.paused` and `game.selectButtonAction` as forwards,
  verbatim): "I'm not sure I agree with this. Forwarding patterns like this can obfuscate the actual source/target of
  things." Then "approved" for the rule: 11. No forwarding members. Callers read the real home (for example
  `game.clock.paused`, `game.players.selectButtonAction`). The forwards that `016a` kept under decision 4
  (`getCurrentLevel()`, `getRooms()`, `isCameraRotating()` and the other functions that now read `levelState` or private
  fields) are replaced by direct reads in `016c`, when the holder gets its real fields.
- 2026-10-28 (owner): the pathfinding debug shapes (`Bat`, `SubmarineJoe`, `DEBUG_PATHFINDING`) are used now and then,
  so `016b` keeps that path; a research agent looks for a better approach (a new item or `fix/` if there is one).

- 2026-10-28 (owner, about the `016b` plan step that added `LevelState.showEndingScreen`, verbatim): "It doesn't seem
  like good design to tightly couple a flag for the game's ending screen to the level state. Is there a better approach
  to this?" After a research pass (the `016b` branch `docs/NOTES.md`), the owner chose option (c): "I like option c
  since it reuses the existing global events manager". 12. This replaces decision 9: `DrWily` submits a new
  `EventType.GAME_COMPLETE`, and `MegaLevelScreen.onEvent` switches to `ENDING_SCREEN`, like `END_LEVEL` does. The
  switch moves from inside `engine.update` to `eventsMan.run()` later in the same frame; the other level exits already
  switch there, and the research found no visible difference. `016b` also removes the dead `VICTORY_EVENT` `END`
  ("endGame") flag (owner: "016b can remove the dead flag"). Left for `013`: one owner for level exits; whether the
  ending becomes the `screenOnCompletion` of `WILY_STAGE_3` and that level is marked defeated; the typed event (`057`).

## Open questions

The smaller questions in `NOTES.md` (for example the debug shapes, and whether the constructor test goes in `016c` or
waits for `004`) are decided in the branch scopes.

## Sources

- `NOTES.md` in this folder (new scan, 2026-10-28).
- `items/001-maverick-engine/NOTES.md`: the host-contract research.
- `items/072-remove-imegagameentity/`: depends on `016`.
