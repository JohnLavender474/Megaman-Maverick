# Scope: mega-refactor-016d-system-services

- Status: approved
- Approved: 2026-10-29 (owner, verbatim: "On 2nd thought, I see no harm in continuing with the original
  recommendation of passing in `LevelContext`. I approve the scope"; `LevelContext` is option (a) of open question 4,
  `LevelState`)
- Merge target: the parent branch `mega-refactor-016-master` (owner decision 2026-10-28), not `master`.

## Goal

The system code that still takes `MegamanMaverickGame` takes only the objects that it reads: the contact rules take
`players` and `gravity`, the pathfinder factory takes `levelState`. `players` and `gravity` exist before the engine is
built. `LevelDefinition.screenOnCompletion` takes `LevelState` in place of the game (open question 4). No gameplay
change. After this branch, only the host, its UI (screens, layer
builders, backgrounds, `ScreenController`) and the launcher name `MegamanMaverickGame`.

## Item

From `016` `ITEM.md`, "Proposed scope", branch 4: "`016d` `system-services`: contact rules, the pathfinder factory and
utilities take the services."

Also from "Proposed scope": "Pooled entities outlive a level, so the holder and every field live for the session.
Values that change at run time (performance, `paused`, the current level and room) are read when used, not copied."

Owner decisions that apply (`ITEM.md`, "Decisions"):

- 8: "Screens keep the host."
- 11: "No forwarding members. Callers read the real home (for example `game.clock.paused`,
  `game.players.selectButtonAction`)."

From the item `NOTES.md` (section 10, the `016d` row): "Non-entity code takes services: `megaContactRules` (players,
gravity), `MegaPathfinderFactory` (level), `MegaUtilMethods.delayRun` and the spawn-delay functions (scheduler),
`GameEntityUtils`, `AnimatedBlockAnimators`, drawables and backgrounds if they are entity-like". Proof: "Compile;
`ContactRuleSelectionTest` with a services object; play: sand, gravity change, pathfinding enemies (`Bat`,
`SubmarineJoe`)". `016c` already did the utilities (`SCAN.md` section 2). Backgrounds are host UI (built by a layer
builder), so they stay with the screens.

## Current state

Full scan: `docs/SCAN.md`. Base `48f7e72f5`.

- `megaContactRules(game: MegamanMaverickGame, debugFilter)` (`C/world/contacts/MegaContactRules.kt:20`) reads
  `game.players` in the sand-splash closure (`:40`) and `game.gravity` in the gravity-change closure (`:56`), when a
  contact happens.
- `MegaPathfinderFactory(game)` (`C/pathfinding/MegaPathfinderFactory.kt:16`) reads `game.levelState.worldContainer`
  (`:26`, `:28`) and `game.levelState.tiledMapLoadResult!!` (`:42`) at each `getPathfinder` call, on the main thread.
- Both are built in `create()`, inside `createGameEngine()` (`MMG:926`, `:945`, `:951`). `levelState` exists then (a
  field built with the game, `MMG:183`). `players` and `gravity` do not: they are `lateinit var`, set in `postCreate()`
  (`MMG:401-402`). Their constructors have no side effect, and nothing reads them between `create()` and the end of
  `postCreate()` (`SCAN.md` section 4).
- `MegaServices` cannot go to the contact rules: it holds `engine`, which is built from the rules.
- `LevelDefinition.screenOnCompletion: (MegamanMaverickGame) -> ScreenEnum` (`C/levels/LevelDefinition.kt:22`): 9
  values call `RobotMasterLevelOnCompletion` (reads only `levelState.weaponsAttained`), 10 are constants. Only
  `MLS:878` calls it. Because `LevelState.currentLevel` is a `LevelDefinition`, the level data in `MegaServices` still
  names the host type through this function type.
- `ContactRuleSelectionTest.kt:54` and `CartV2ItemTest.kt:84` mock the host only to build the contact rules.
- No test covers the 2 rule closures, the pathfinder factory or `screenOnCompletion`.

## In scope

1. `players` and `gravity` exist before `createGameEngine()` (open question 1, approved): `val players = MegaPlayers()` and
   `val gravity = WorldGravity(players)` become fields built with the game object, next to `levelState`, `clock`,
   `camera` and `settings`. The 2 lines in `postCreate()` go. `MegaServices`, the `Megaman` build, the `GameState`
   listener and `players.register(PlayerSlot(0), megaman)` stay in `postCreate()` in the same order.
2. Contact rules (open question 2, approved): `megaContactRules(players: MegaPlayers, gravity: WorldGravity, debugFilter)`. The
   2 closures read `players` and `gravity` (the same session objects). The `MegamanMaverickGame` import goes. Caller
   `MMG:926`: `megaContactRules(players, gravity, CONTACT_LISTENER_DEBUG_FILTER)`.
3. Pathfinder factory (open question 3, approved): `MegaPathfinderFactory(private val levelState: LevelState)`; the 3 reads
   become `levelState.worldContainer` and `levelState.tiledMapLoadResult!!`. No other line changes. Callers `MMG:945`,
   `:951`: `MegaPathfinderFactory(levelState)`.
4. `screenOnCompletion` takes the level state (open question 4, option (a)):
   `val screenOnCompletion: (LevelState) -> ScreenEnum`. `RobotMasterLevelOnCompletion : (LevelState) -> ScreenEnum`
   returns `GET_WEAPONS_SCREEN` when `levelState.weaponsAttained` is not `null`, else `SAVE_GAME_SCREEN` (the same test
   as today). The 9 robot master entries become `screenOnCompletion = RobotMasterLevelOnCompletion`; the 10 constant
   lambdas (`{ ScreenEnum.X }`) do not change (a one-parameter lambda ignores its argument). `MLS:179` gets the same
   type; `MLS:878` calls `screenOnCompletion.invoke(game.levelState)`. `MMG:1013` does not change. The
   `MegamanMaverickGame` import leaves `LevelDefinition.kt` and `RobotMasterLevelOnCompletion.kt`.
5. Tests (open question 5, approved):
   - `ContactRuleSelectionTest` and `CartV2ItemTest` build the rules with a real `MegaPlayers()` and
     `WorldGravity(players)` (in `CartV2ItemTest`, the same objects that its `testServices(...)` gets). The reference
     file `contact-rule-selection.txt` does not change.
   - New permanent `MegaContactRulesTest`: the sand rule from the table spawns a splash on `BEGIN` only when a
     registered player is in play; the gravity-change rule from the table sets the world gravity (and so every
     player's direction) for a player, and only the entity's own direction for a non-player, with the world gravity
     unchanged.
   - New permanent `MegaPathfinderFactoryTest`: the factory reads `levelState` at each call (a new world container and
     map load result set after the factory is built are used; the world width and height come from the current map
     load result).
   - A game field test (in the style of `GameCameraStateTest`): a new `MegamanMaverickGame()` has `players` and
     `gravity` before `create()`, one object each for the session, `gravity.direction` starts `UP`, and
     `gravity.set(...)` acts on an entity registered in `game.players`.
   - New permanent `LevelCompletionScreensTest`: a table of the 19 entries and their expected screen for a
     `LevelState()` with `weaponsAttained` `null` and with a non-empty set (the 9 robot master levels: `SAVE_GAME_SCREEN` and
     `GET_WEAPONS_SCREEN`; the others: their constant for both). An entry that is added later and is not in the table
     fails the test.
6. Manual check rooms (open question 6, approved): a new test map `RefactorTest016.tmx` with a `LevelDefinition` entry
   `REFACTOR_TEST_016` (type `TEST_LEVEL`), built by a subagent with the `level-builder` skill as a plan step, with one
   room for the gravity-change contact (no real level has a `GravityChange` in a room with a player spawn). Sand and
   pathfinding use real rooms (`DESERT_MAN`).
7. Root `CLAUDE.md` (open question 7, approved): "Game object": `players` and `gravity` are built with the game object;
   the contact rules take `players` and `gravity`, the pathfinder factory takes `levelState`; the list of what keeps
   the game becomes "screens, layer builders, backgrounds, `ScreenController` and the launcher"; "Levels and Tiled
   pipeline": `screenOnCompletion` takes the `LevelState`.

New API and first production caller:

| API | First production caller |
|---|---|
| `megaContactRules(players, gravity, debugFilter)` (changed signature) | `MMG.createGameEngine` |
| `MegaPathfinderFactory(levelState)` (changed constructor) | `MMG.createGameEngine` |
| `MegamanMaverickGame.players`, `gravity` as `val` built with the game (was `lateinit var`) | `MMG.createGameEngine` (the contact rules), `postCreate` |
| `LevelDefinition.screenOnCompletion: (LevelState) -> ScreenEnum` (changed type) | `MLS.onEvent` (`END_LEVEL`) |
| `REFACTOR_TEST_016` (`LevelDefinition` entry, test level) | the launcher `--level` option (test map, as `REFACTOR_TEST_029`) |

No new class, function or field. No API is removed other than the old signatures and the `lateinit` setters of
`players` and `gravity`.

## Out of scope

- Screens, screen handlers, layer builders, backgrounds, `ScreenController`, `MMG` and the launcher keep the host
  (decision 8). The list is in `SCAN.md` section 6, the input for `001b`.
- `MegaServices` does not change. No new holder field.
- `MegaPathfinderFactory` reads `worldContainer` twice in one call (`:26`, `:28`). This branch keeps both reads (a
  receiver change only). Merging them is a cleanup with no gameplay effect; not done here.
- The co-op `TODO` comments in `MegaContactRules.kt` (`:38-39`, `:49-52`) stay as they are.
- Renames (`016e`), engine host contract (`001b`).
- Commented-out code that names the host (5 entity files).

## Affected areas and risks

- Main files: `MegaContactRules.kt`, `MegaPathfinderFactory.kt`, `MMG` (2 field lines, 2 `postCreate` lines, 3
  `createGameEngine` lines), `LevelDefinition.kt` (1 type line, 9 entry lines, imports),
  `RobotMasterLevelOnCompletion.kt`, `MLS` (2 lines); with open question 6, `LevelDefinition.kt` (1 entry) and a new TMX
  file. Test files: `ContactRuleSelectionTest`, `CartV2ItemTest`, 3 or 4 new tests. Root `CLAUDE.md`.
- Risk: creation order. `players` and `gravity` exist from the game construction, not from `postCreate()`. Mitigation:
  the constructors have no side effect and nothing reads them in that span (`SCAN.md` section 4); the `Megaman`
  registration and listener stay where they are; a smoke launch and the play test.
- Risk: the rules hold a different `players` or `gravity` object than the entities. Mitigation: one field each on the
  game, passed to the rules and to `MegaServices`; the game field test checks one object each; the play test checks
  that a player in a gravity-change area turns the camera and the player, and that sand splashes.
- Risk: the factory copies a level value. Mitigation: it holds the session `LevelState` and reads each value at the
  call; `MegaPathfinderFactoryTest`; `Bat` in a second level or after a death.
- Risk: a level entry that gives a different screen after the type change. Mitigation: `LevelCompletionScreensTest`
  (all 19 entries, both cases); the play test beats one robot master level (get-weapon screen) and one other level.

## Overlap with other branches

No other code branch is active (`SCAN.md` section 8). `001b` waits for `016`; `SCAN.md` section 6 is its input.

## Open questions

None. Resolved by the owner on 2026-10-29 ("I approve the scope"). Questions 1 to 3 and 5 to 7 are accepted as
recommended. Question 4 is resolved as option (a), `LevelState`, not the recommended (b); the other options stay
listed as not chosen.


1. How do `players` and `gravity` exist before `createGameEngine()`?
   - (a) `val` fields built with the game object, like `levelState`, `clock`, `camera`, `settings`.
   - (b) Keep `lateinit var`; set them in `create()` before `engine = createGameEngine()`.
   - (c) Keep the order; pass suppliers (`{ players }`) to the rules. The rules then still capture the host through
     the closure, and the late read stays the hidden reason that the order works.

   Accepted: (a). Neither constructor has a side effect or needs `create()`; it removes the public `lateinit`
   setters; it is the same pattern as the 4 session fields that `016a` to `016c` added. Unit tests already build a
   `MegamanMaverickGame()` with no `create()`, so a test can check it.
2. What do the contact rules take?
   - (a) `megaContactRules(players, gravity, debugFilter)`.
   - (b) 2 lambdas (`sandSplashesOn`, `changeDirection`) built in `MMG`. This moves 2 game rules out of the rule table
     into the host.
   - (c) `MegaServices`. Not possible: a cycle through `engine`.

   Accepted: (a). The rule table keeps its rules; it names only the 2 objects that it reads.
3. What does the pathfinder factory take?
   - (a) `LevelState`.
   - (b) 2 suppliers (world container, map load result), which would make the factory free of `core` level types.

   Accepted: (a). It reads 2 values of one object; `LevelState` is a session object; (b) adds 2 closures for no
   caller that needs them. `001b` can decide (b) if the engine host wants a generic factory.
4. `LevelDefinition.screenOnCompletion`: which narrow parameter?

   Owner input (2026-10-29, verbatim): "I prefer the latter because it scopes the available data to only what's
   needed. Agent can weigh pros and cons of this choice". So the function no longer takes `MegamanMaverickGame`; this
   question is only about the exact parameter. Check of every level's function (`SCAN.md` section 3.3): 9 robot master
   entries read only `levelState.weaponsAttained`, and only to test it for `null`; 10 entries are constants and read
   nothing. No entry reads anything else.

   - (a) `(LevelState) -> ScreenEnum`. Call site: `screenOnCompletion.invoke(game.levelState)`.
     - Pros: a later rule that needs another level value (current level, coins, points) gets it with no type change.
       `LevelState` is already a session object in `MegaServices`.
     - Cons: wider than needed: every level value, and all of them writable, so a completion rule could change level
       state. It does not show what a rule reads. A test must build a `LevelState`.
   - (b) `(weaponsAttained: OrderedSet<MegamanWeapon>?) -> ScreenEnum`: the one value that is read. Call site:
     `screenOnCompletion.invoke(game.levelState.weaponsAttained)`.
     - Pros: the narrowest type that keeps the current rule exactly (`!= null`); the signature shows what a rule
       reads; the test passes plain values. `LevelDefinition.kt` already imports `OrderedSet` and `MegamanWeapon`.
     - Cons: a later rule that needs more data changes the parameter type. The cost is small: the 10 constant lambdas
       compile with any one parameter, so only `RobotMasterLevelOnCompletion`, the new rule, the `MLS` field type and
       the 1 call site change. The set is mutable, so a rule could still change it (today's rule does not).
   - (c) `(weaponsAttained: Boolean) -> ScreenEnum` ("did this level give weapons"). Call site:
     `invoke(game.levelState.weaponsAttained != null)`.
     - Pros: the narrowest, and read-only.
     - Cons: the `null` test moves from the rule into the level screen, so the screen holds part of the robot master
       rule; a rule that needs the weapons themselves needs a new type at once.
   - (d) A new small value type (for example `LevelCompletion(weaponsAttained)`), built by the level screen.
     - Pros: a later rule gets a new field, not a new function type.
     - Cons: a new class whose only field today is one value; it is "a type because a later need may come", which the
       rules forbid with no caller that needs it.

   How a later rule that needs more data gets it: with (b), widen the parameter at that time (to `LevelState`, or to
   a value type if the rule needs data from more than one object); the change is local, as above. With (a) it is
   already there, at the cost of the wide type now.

   Test impact: all options need the same `LevelCompletionScreensTest` (19 entries, 2 cases). (b) and (c) need no
   object; (a) needs a `LevelState()`; (d) needs the new type. No other test calls `screenOnCompletion`.

   Recommendation was (b). Resolved (owner, 2026-10-29, verbatim): "On 2nd thought, I see no harm in continuing with
   the original recommendation of passing in `LevelContext`. I approve the scope". There is no `LevelContext` class;
   the owner means the first proposal, option (a): `(LevelState) -> ScreenEnum`, and the level screen passes
   `game.levelState`. Options (b), (c) and (d) are not chosen.
5. A temporary old-versus-new suite?

   Accepted: no. No runtime path changes: the rules and the factory call the same code on the same session
   objects; only the reference that reaches them changes, and the 2 objects are built earlier with no side effect. The
   permanent tests in "In scope" item 5 cover the closures, the factory and the field objects.
6. Manual check for the gravity-change contact: no real level has a `GravityChange` in a room with a player spawn
   (`TEST_1` has it only in `room_7`, `room9`, `room12`, with no spawn). Options:
   - (a) A new test map `RefactorTest016.tmx` (`REFACTOR_TEST_016`) with one room `gravity_change`: a player spawn,
     `GravityChange` areas with a direction (for example `LEFT` and back to `UP`), and a non-player `IDirectional`
     enemy (for example `Met`, as in `TEST_1` `room_7`) that walks or falls into an area.
   - (b) Add a player spawn to `Test1.tmx` `room_7`.
   - (c) No manual check of this contact; only `MegaContactRulesTest`.

   Accepted: (a), per the "Test maps" rule (one test map per item; `016` has none yet). Sand (`DESERT_MAN`
   `room1`) and `Bat` (`DESERT_MAN` `room10`) use real rooms, and `GravitySwitcharoo` (`REFACTOR_TEST_029`
   `gravity_switch`) checks the earlier-built `gravity` from the entity side.
7. Root `CLAUDE.md`: update the "Game object" lines about `players`, `gravity` and what keeps the game?

   Accepted: yes, in the last code step. The current text says "contact rules and the pathfinder factory keep
   the game", which is false after this branch.
