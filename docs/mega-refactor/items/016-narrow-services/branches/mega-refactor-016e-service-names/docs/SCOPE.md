# Scope: mega-refactor-016e-service-names

- Status: approved
- Approved: 2026-10-29 (owner, verbatim: "Scope approved, go with your recommendations")
- Merge target: the parent branch `mega-refactor-016-master` (owner decision 2026-10-28), not `master`.

## Goal

Give the services holder and its members names that fit it. In entity and utility code, the `MegaServices` value is
named `services`, not `game`. The old host names of the holder members (`assMan`, `eventsMan`, `audioMan`, `state`)
become plain names. The change is one scripted, mechanical commit that `.git-blame-ignore-revs` lists. No logic line
changes and no gameplay change.

## Item

From `016` `ITEM.md`, "Proposed scope", branch 5: "`016e` `service-names` (optional): a mechanical rename of `game`
and the member names."

Owner decisions that apply (`ITEM.md`, "Decisions"):

- 5: "Keep the name `game` and the old member names during the swap; rename later (`016e`)."
- 8: "Screens keep the host."
- 11: "No forwarding members. Callers read the real home."

From the item `NOTES.md` (section 10, the `016e` row): "Mechanical rename: `game` → `services` in entity code,
`assMan` → `assets`, `eventsMan` → `events`, ... Add the commit to `.git-blame-ignore-revs`". Proof: "Compile only; no
logic line changes (a diff check that ignores the renamed names)".

The working names in "Proposed scope" of `ITEM.md` were: `engine`, `events`, `assets`, `audio`, `profile`
(`GameState`), `players`, `gravity`, `camera`, `level` (`LevelState`), `clock`, `scheduler` (`updatables` plus
`runQueue`), `groups`.

## Current state

Base: `mega-refactor-016-master` at `ffb8741a0` (after `016d`). `master` has no code change since the merge base, so
the scan is also true for `master` plus the parent. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. All
counts are from this scan. "Code" means Kotlin code tokens: comments, string text, `package` and `import` lines and
qualified names such as `com.maverick.game.engine` are not counted.

Baseline build (`engine:test core:test lwjgl3:jar`, JDK 17): passes; engine 575 tests, core 272 tests, 0 failures;
81 unique compiler warnings (line and column removed).

### 1. `MegaServices` members (`C/MegaServices.kt`, 39 lines)

`MegaServices` has 14 public `val` members and 1 function. Each member is the same session object as the host field
of the same name. The table gives the reads of each member in code:

- "Services reads": `x.member` where `x` is a `MegaServices` value (entities, entity utilities, Megaman parts,
  tests).
- "Host reads": `game.member` where `game` is the host `MegamanMaverickGame` (screens, screen handlers, layer
  builders, backgrounds, `ScreenController`, host tests), plus the unqualified uses inside
  `C/MegamanMaverickGame.kt` (`MMG`).

| Member | Type | Services reads | Host reads | Unqualified in `MMG` | Name fits? |
|---|---|---|---|---|---|
| `engine` | `GameEngine` | 2 in 2 files | 5 in 3 files | many | yes |
| `eventsMan` | `EventsManager` | 120 in 57 files | 18 in 8 files | 7 lines | no: abbreviation of the host |
| `assMan` | `AssetManager` | 408 in 380 files | 72 in 33 files | 8 lines | no: abbreviation of the host |
| `audioMan` | `MegaAudioManager` | 55 in 41 files | 75 in 26 files | 10 lines | no: abbreviation of the host |
| `state` | `GameState` (the saved progress) | 110 in 49 files | 31 in 8 files | 20 lines (2 functions also have a local `state`) | no: vague; many entities also have their own `state` field (an FSM state) |
| `players` | `MegaPlayers` | 9 in 7 files (most entity code uses the `players` extension) | 24 in 9 files | yes | yes |
| `gravity` | `WorldGravity` | 26 in 18 files | 7 in 2 files | yes | yes |
| `levelState` | `LevelState` | 66 in 35 files | 48 in 14 files | 11 lines | yes: it is the type name |
| `clock` | `GameClock` | 1 | 20 in 8 files | yes | yes |
| `camera` | `GameCameraState` | 46 in 37 files | 12 in 2 files | yes | partly: see open question 4 |
| `settings` | `GameSettings` | 8 in 8 files | 5 in 2 files | yes | yes |
| `updatables` | `OrderedMap<String, Updatable>` | 9 in 4 files | 0 | yes | yes |
| `runQueue` | `Queue<() -> Unit>` | 2 in 2 files | 4 in 2 files | yes | yes |
| `groups` | `GroupsSystem` | 1 | 0 (the host uses `getSystem(GroupsSystem::class)`) | no | yes |
| `getGameCamera()` | `RotatableCamera` | 76 in 57 files | 1 | yes | yes (see open question 4) |

The 4 names that do not fit are the old host names `eventsMan`, `assMan`, `audioMan` and `state`.

`MegaServices` is built in 2 places, both with positional arguments: `MMG.postCreate` (`MMG:401`) and the test helper
`testServices(...)` (`core/src/test/.../TestServices.kt:39`). `testServices` has one parameter per member with the
member's name, and 22 test files call it with named arguments: `engine =` 19, `players =` 8, `state =` 2,
`gravity =` 2, `eventsMan =` 1, `clock =` 1.

### 2. The name `game` where its type is `MegaServices`

- 480 declarations `game: MegaServices` in 474 files (453 in `core` main, 21 in `core` test; none in `engine` or
  `lwjgl3`):
  - 455 parameters: entity constructors (most of the 480), `AbstractEnemy`, `AbstractBoss`, `AbstractProjectile`,
    `AbstractHealthEntity`, `Megaman`, `MegaEntityFactory.init`, `EntityFactories.initialize`,
    `MegaUtilMethods.delayRun`, `delayNextPossibleSpawn` and `isNextPossibleSpawnDelayed` (`GameEntityUtils.kt`), and
    test entities.
  - 25 properties: `MegaGameEntity(val game: MegaServices)` (`C/entities/contracts/MegaGameEntity.kt:16`), 11
    `private val game` (the 10 `EntityFactories` factories and `ToxicWaterWaveAnimator`), `MegamanWeaponsHandler`
    (`:142`), `MegamanAnimations` (`:22`), `MegaEntityFactory` (`private lateinit var game`, `:27`), and 10 test
    `lateinit var game`.
- 5 locals that copy it: `MegamanDamageNegotiator.kt:24`, `GameEntityUtils.kt:78`, `IBodyExtensions.kt:19` and `:33`,
  plus `val game = testServices(...)` in 8 tests.
- Code tokens `game` that refer to a `MegaServices` value: 2,229 on 1,775 lines in 489 files. 469 of these files
  name `MegaServices`. 19 do not: they read `game` through `MegaGameEntity.game` (the Megaman behaviors, components,
  `MegamanJetpackHandler`, `MegamanDamageNegotiator`, `MegamanAnimationKeySupplier`, `FreezableEntityHandler`,
  `LightSourceUtils`, `IBodyExtensions`, the `IMegaGameEntity.kt` extensions, `MegaEntityFactoryTest`). 1 file
  (`GamePlayersFieldsTest.kt`) has both: line 17 (`FieldsTestPlayerEntity(game: MegaServices)`) is services, the other
  `game` tokens are a host `MegamanMaverickGame()`.
- In the same 489 files, `game` also occurs 22 times in comments (15 files; mostly commented-out code such as
  `// class SphinxMiniBoss(game: MegamanMaverickGame)`) and 11 times in string text (8 files; log text such as
  `"game.isCameraRotating=${...}"`).

Where `game` is still the host `MegamanMaverickGame` (decision 8; not renamed): 59 files name the host type. After the
rename, 632 code tokens `game` stay, in 53 files: screens and screen handlers (556), backgrounds (36),
`ScreenController` (4), host tests (`GameClockTest`, `GameSettingsTest`, `LevelStateTest`, `GameCameraStateTest`,
`GamePlayersFieldsTest`, `MegaLevelScreenEventsTest`; 27) and `DesktopLauncher` (9). `MegaMapLayerBuildersParams.game`
(`C/screens/levels/tiled/layers/MegaMapLayerBuilders.kt:18`) is the host too; `BackgroundLayerBuilder` reads it 31
times as `params.game` and passes it to the backgrounds. No host file reads `MegaGameEntity.game`.

### 3. Collisions and risks

- **Existing `services` names.** `MMG:401` (`val services = MegaServices(...)`, the host file, not renamed),
  `EntityConstructorsTest.kt:43`, `MegaEntityFactoryTest.kt:20` and `MegaContactRulesTest.kt:51`. In the 3 tests, no
  `game` declaration shares a scope with the `services` local, so the rename makes no clash; the dry run compiles.
- **Existing names equal to the proposed member names.** `events`: 46 code tokens in 8 files (for example
  `SpawnerFactory`, `SpawnerForEvent`, `SpawnersLayerBuilder`, `SpawnerReleaseTest`). `assets`: 15 tokens in 4 files
  (`MusicAsset`, `SoundAsset`, `TextureAsset`, and `MMG:873-883`). `audio`, `gameState`: none. `level`: 104 tokens in
  17 files. Outside `MMG`, every read of a member is qualified (`services.events`), and a qualified member is never
  hidden by a local, so these names cannot clash there.
- **One real clash, in the host.** `MMG.queueAssets()` (`MMG:872-884`) has a local `val assets = Array<IAsset>()`
  and reads the member `assMan` unqualified in the same function (`:878`, `:883`). With the host member renamed to
  `assets`, the local hides the member. The dry run showed this as compile errors, but a clash like this compiles
  silently if the local type has the same function. So the local is renamed first, in a separate small commit
  (`assets` → `queuedAssets`, 6 lines), and the clash scan runs again before the mechanical commit.
- **Kotlin named arguments.** No call passes `game =` to a `MegaServices` parameter. The 14 `game = game` and
  `game = params.game` arguments in screens and `BackgroundLayerBuilder` are host parameters and stay. The test calls
  `testServices(eventsMan = ...)` (1) and `testServices(state = ...)` (2) follow the parameter rename.
- **Reflection.** `MegaEntityFactory` calls `key.primaryConstructor!!` and `constructor.call(game)`
  (`C/entities/MegaEntityFactory.kt:70-72`): positional, no parameter name. `EntityConstructorsTest` uses
  `callBy(mapOf(required[0] to services))` (by the `KParameter` object) and checks the parameter type, not its name.
  No `::game`, `::assMan` or other property reference exists. So no runtime code reads the renamed names.
- **String keys and data.** `ConstKeys.GAME = "game"` is the game viewport key and is not a member name; it stays.
  Tiled maps use the property names `events` and `state` (one each) as spawn properties, read through `ConstKeys`;
  they are not member names. The `RuntimeDiagnostics` entry names `"assMan.update"`, `"eventsMan"` and `"audioMan"`
  (`MMG:499`, `:599`, `:603`) are data: the `utils/diagnostics` tests and expected files and the
  `diagnostics-analyzer` skill read them. The rename does not change string text, so they stay.
- **MockK.** Tests stub through the compiled names (`every { services.assMan.get(...) }`,
  `every { megaman.game } returns game` in `MegamanWeaponsHandlerOwnershipTest`). They are code tokens and follow the
  rename; the compiler checks them.
- **`IMegaGameEntity` (item `072`).** The interface itself has no `game` member since `016c`. The file has 3
  extension functions that read `game` (`IMegaGameEntity.kt:13`, `:18`, `:21`); those 3 lines change. The interface
  declaration does not change, so `072` is not affected except for these 3 lines.
- **Other members named the same.** Other classes have their own `state` (an FSM state in about 20 entities, for
  example `DrWily`, `Gate`, `Bat`), `assMan` (`BitsBar`, `BlinkingArrow` parameters) and `eventsMan` locals
  (`Megaman.kt:519`, `GameEntityUtils.kt:50`, `AnimatedBlock.kt:119` has a local `assMan`). A member rename must
  touch only reads on a `MegaServices` or host receiver, not every `state` token. The method below matches the
  receiver.

### 4. Host members with the same old names

The host `MegamanMaverickGame` declares the same old names: `lateinit var assMan`, `eventsMan`, `audioMan`, `state`
(`MMG:213-218`), and `levelState`. `postCreate` passes these same objects to `MegaServices`. Screens and other host
code reads them 196 times in 39 files (`eventsMan` 18, `assMan` 72, `audioMan` 75, `state` 31), plus 10 lines of
`params.game.assMan` in `BackgroundLayerBuilder` and 44 lines inside `MMG`. `lwjgl3` reads none of them.

If only the holder renames, one object has two names: for example `services.assets` in an entity and `game.assMan`
in a screen, and `services.gameState` next to `game.state`. Root `CLAUDE.md` then must explain both. If the host
renames too, the dry run shows 250 more changed lines in 41 host files (with `MMG` and `BackgroundLayerBuilder`), all inside the same mechanical commit, plus
the 6-line `queueAssets` local rename.

### 5. Dry run of the method

A dry run in a scratch copy of the worktree (not in this branch; scripts in the session scratchpad
`/tmp/claude-1000/-home-johnlavender-IdeaProjects-Megaman-Maverick/a600e326-cd21-4d95-a3bf-326f727a8247/scratchpad/016e/`:
`tok.py`, `rename.py`, `check.py`, `norm.py`) applied the recommended names (`game` → `services`, `assMan` →
`assets`, `eventsMan` → `events`, `audioMan` → `audio`, `state` → `gameState`, holder and host):

- Result: 532 files changed (511 `core` main, 21 `core` test), 2,038 lines changed, 2,038 replaced. Token
  replacements: `game` 2,229 (489 files), `assMan` 493 (417 files), `eventsMan` 149 (69 files), `audioMan` 143 (70
  files), `state` 166 (62 files).
- `check.py`: every changed line is the old line with only these token replacements. 0 other differences.
- Build: `engine:test core:test lwjgl3:jar` passes; engine 575, core 272, 0 failures (same as the baseline).
- Compiler warnings: 81, the same set as the baseline after the name map.
- Bytecode: `javap -c -p` of all 1,861 `core` main classes, before and after, with the old and new names mapped to one
  placeholder, constant-pool indexes removed and whitespace ignored: 0 differences.
- The first runs of the dry run found 3 script faults, each as a compile error: the `queueAssets` local clash
  (section 3), a member read on a receiver that is itself a member (`block.game.assMan`), and the fully qualified
  `com.maverick.game.engine...` in `CartV2ItemTest.kt:64`. The script now handles all 3.

## In scope

Names in this list are the recommendations; the owner chooses in "Open questions".

1. **Pre-step commit (not mechanical).** In `MMG.queueAssets()`, rename the local `assets` to `queuedAssets` (6
   lines). Only needed if the host members rename (open question 3).
2. **Mechanical commit (scripted).** Nothing else goes in this commit.
   - In the 489 services files: the code token `game` → `services`. In `GamePlayersFieldsTest.kt`, only line 17.
   - Member renames on `MegaServices` (`C/MegaServices.kt`, 4 lines): `eventsMan` → `events`, `assMan` → `assets`,
     `audioMan` → `audio`, `state` → `gameState`. Every read on a `MegaServices` receiver follows.
   - The same 4 renames on the host (`MMG:213-218` and the unqualified uses in `MMG`) and every read on a host
     receiver (`game.assMan` in screens, `params.game.assMan` in `BackgroundLayerBuilder`), if open question 3 is
     accepted.
   - `testServices(...)`: the parameter names follow the members, and the 3 named arguments follow.
3. **`.git-blame-ignore-revs`.** A separate commit adds the SHA of the mechanical commit, in the format of the
   existing entries. At the merge into the parent, the parent replaces it with the squash SHA (merge rule 4).
4. **Docs commit.** Root `CLAUDE.md`: the lines that name the old members or say "Inside an entity it is still named
   `game`" (`CLAUDE.md:49`, `:56`, `:61`, `:86`, `:99-101`, `:119`).

The method of the mechanical commit:

- A script (kept in the scratchpad, as in `063b`) with a small Kotlin lexer. It edits code tokens only. It does not
  edit comments, string text, `package` and `import` lines, or qualified names that start with `maverick.game`.
- File sets come from the scan: the services files (files that name `MegaServices`, plus the 19 files that read
  `MegaGameEntity.game`, minus `MMG` and `BackgroundLayerBuilder`) and the host files (files that name
  `MegamanMaverickGame`).
- A member is renamed only on the receivers `services` (after the `game` rename) and, in host files, `game`. In `MMG`
  the unqualified uses are matched by the listed patterns, because 2 functions there also have a local `state`.
- If the parent changes before the merge, the script runs again on the new base. A conflict is solved by running it
  again, not by hand.

Checks of the mechanical commit:

1. `check.py`: each changed line is the old line with only the listed token replacements, and the file and token
   counts are the scan counts.
2. Build and tests: `engine:test core:test lwjgl3:jar` pass with the baseline test counts.
3. Normalized bytecode: `javap -c -p` of every `core` main class, before and after, equal after the name map.
4. Compiler warnings: the same set as the baseline after the name map.
5. Greps: no code token `assMan`, `eventsMan` or `audioMan` stays as a member read; no code token `game` stays in the
   services files; the code tokens `game` that stay are all in host files.
6. `git blame --ignore-revs-file .git-blame-ignore-revs` on a renamed line shows a commit before the mechanical
   commit.

New API and first caller: none. The branch renames 1 parameter or property name in 480 declarations and 4 member names
of 2 existing classes. It adds no type, member, function or overload, and removes none.

## Out of scope

- The host's own name `game` in screens, screen handlers, layer builders, backgrounds, `ScreenController`, the
  launcher and host tests (decision 8). `001b` decides the host contract.
- `levelState`, `getGameCamera()` and `camera` keep their names (open questions 2 and 4).
- `scheduler` (one object for `updatables` and `runQueue`): a new type, not a rename. No caller needs it now.
- Locals and parameters of other classes that use an old name for the same object: `val eventsMan = services.events`
  (`Megaman.kt:519`, `GameEntityUtils.kt:50`), `val assMan = block.services.assets` (`AnimatedBlock.kt:119`), the
  `assMan` parameters of `BitsBar` and `BlinkingArrow` (open question 5).
- Comments and string text, including commented-out code and log text (open question 6).
- The `RuntimeDiagnostics` entry names (`"assMan.update"`, `"eventsMan"`, `"audioMan"`): data that `utils/diagnostics`
  and the `diagnostics-analyzer` skill read.
- A rename of the class `GameState` (for example to a profile type). If the owner wants that, it is a backlog
  candidate.
- Docs outside the branch folder and root `CLAUDE.md` (item notes, other branch folders, logs) that name the old
  names. They are history.

## Affected areas and risks

- 532 files, 2,038 lines in the dry run (all names chosen as recommended; without the host part: 491 files and
  1,788 lines). Kotlin resolves every renamed name at compile time, and no code reads these names at runtime, so a
  wrong rename shows as a compile error, not as a behavior change.
- Risk: a local or parameter with the new name hides a member after the rename and still compiles. Mitigation: the
  clash scan (section 3; 1 clash found, in `MMG`), the pre-step commit, and the normalized bytecode check, which
  finds a changed call target.
- Risk: the script changes a token that is not the services value or member (for example an FSM `state`).
  Mitigation: receiver-matched member renames, `check.py`, the compile and the bytecode check.
- Risk: the rename touches nearly every entity file. A branch that starts from the parent before this branch merges
  gets conflicts in many files. Today no other code branch is active (`git worktree list`: only the parent and this
  branch). `072` and `001b` wait for `016`. Mitigation: merge this branch before the next branch of `016` or of a
  related item starts; if one starts first, run the script again on its base.
- Risk: `.git-blame-ignore-revs` lists the squash commit after the merge, so `git blame` also skips the small
  non-mechanical changes that the squash holds (the 6-line local rename and the `CLAUDE.md` lines). Accepted, as in
  `063b`.
- No gameplay change. The contract can be "compile and the checks above"; the owner may still want a short smoke
  launch (decided in `CONTRACT.md`).

## Overlap with other branches

No other code branch is active. Only the parent `mega-refactor-016-master` and this branch have worktrees. `072`
(`IMegaGameEntity` removal) will touch `IMegaGameEntity.kt`, whose 3 extension lines this branch changes; `072` starts
after `016`.

## Open questions

1. **Name of the `MegaServices` value (now `game`).**
   - (a) `services`: the item note and the class name. 2,229 tokens.
   - (b) `svc` or `ctx`: shorter, but an abbreviation (the reason `assMan` goes) and `ctx` suggests a context of one
     level, which decision 3 rejected.
   - (c) Keep `game`: no change; the name then says "game" for a holder that is not the game.

   Recommendation: (a).
2. **Member names.**
   - `eventsMan` → (a) `events` (recommended), (b) `eventsManager`, (c) keep.
   - `assMan` → (a) `assets` (recommended), (b) `assetManager`, (c) keep.
   - `audioMan` → (a) `audio` (recommended), (b) `audioManager`, (c) keep.
   - `state` → (a) `gameState` (recommended): it matches the type `GameState`, and it is not the same word as the
     FSM `state` fields of about 20 entities; (b) `profile` (the item working name): it says what the object holds
     (the saved progress), but it does not match the type name until a later rename of `GameState`; (c) `progress`;
     (d) keep `state`.
   - `levelState` → (a) keep (recommended): it is the type name and is clear; (b) `level` (the item working name):
     shorter, but `services.level.currentLevel` reads twice "level", 17 files already use `level` for a
     `LevelDefinition` or a log level, and `MMG` has `level` lambda parameters in scopes near its unqualified
     `levelState` reads.
   - `engine`, `players`, `gravity`, `clock`, `camera`, `settings`, `updatables`, `runQueue`, `groups`: keep.
3. **Does the host rename the same members?**
   - (a) Yes, in the same mechanical commit (recommended): one name for one object in all code and in `CLAUDE.md`.
     It adds 250 lines in 41 host files to the mechanical commit and needs the 6-line pre-step in `MMG`. Decision 8
     is about which code takes the host, not about member names, so it does not block this.
   - (b) No: the holder and the host use different names for the same object until `001b`. Smaller diff (491 files,
     1,788 lines), but two names for one object, and `CLAUDE.md` must name both.
4. **`camera` and `getGameCamera()`.** `services.camera` is the `GameCameraState` (2 flags) and
   `services.getGameCamera()` is the camera object. The names are close but are not wrong.
   - (a) Keep both (recommended): item `051` (camera with components) will change the camera API; a rename now could
     be undone there. `getGameCamera()` → a property would change the call shape (`()` goes), which is not a
     token rename.
   - (b) `camera` → `cameraState` now (46 services reads, 12 host reads).
5. **Locals and parameters of other code that use the old names for the same object** (`val eventsMan =
   services.events` in 2 places, `val assMan = ...` in `AnimatedBlock`, the `assMan` parameters of `BitsBar` and
   `BlinkingArrow`; about 15 lines in 5 files).
   - (a) Leave them (recommended): they are not members of the holder or the host, and each is local to one file.
   - (b) Rename them in the same commit: one more token pair each in `check.py`, and a clash scan for each.
6. **Comments and string text.** 22 comment uses and 11 string uses of `game` in the services files, and comment uses
   of the old member names (mostly commented-out code).
   - (a) Leave them (recommended): the check then proves that only code changed; comments often mean "the game" in
     prose; log text is output.
   - (b) Also rename `game` in commented-out code: needs a manual list, because a word match in prose is often wrong.
7. **Is the branch still wanted?** `ITEM.md` marks `016e` as optional.
   - (a) Yes (recommended): after `016d`, the name `game` in 489 entity files is the only place that still says
     "game" for the holder, and every new entity copies it. The dry run shows that the method is mechanical and
     checkable. No other code branch is active now, so the merge conflicts are smallest now.
   - (b) Drop it: no diff; the old names stay, and `CLAUDE.md` keeps the note "Inside an entity it is still named
     `game`".
