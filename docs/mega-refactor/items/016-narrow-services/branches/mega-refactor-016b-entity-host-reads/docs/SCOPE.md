# Scope: mega-refactor-016b-entity-host-reads

- Status: approved (revision 2: the `DrWily` design changed after the plan review; see "Revision 2")
- Approved: revision 1 on 2026-10-28 (owner, "approved", with changes to open questions 5 and 6); revision 2 on
  2026-10-28 (owner, "approved to all")
- Merge target: the parent branch `mega-refactor-016-master` (owner decision 2026-10-28, "Parent branches" in
  `docs/mega-refactor/CLAUDE.md`), not `master`.

## Goal

Each entity read of a host-only member of `MegamanMaverickGame` gets a typed home: an object that lives for the
session and that the `016c` holder (`MegaServices`) will hold. Then `016c` can change the entity constructor type
with no edit to these lines. Every caller reads the real home; the game keeps no forwarding member for a moved value.
No gameplay change.

## Item

From `016` `ITEM.md`, "Proposed scope", branch 2: "`016b` `entity-host-reads`: the 7 host-only reads in entities get a
home (`DrWily` screen switch, `Starkner` `paused`, 3 `selectButtonAction` reads, 2 debug `getShapes()` calls,
`WEAPONS_ATTAINED`)."

Owner decisions that apply (`ITEM.md`, "Decisions", 2026-10-28):

- 5: "Keep the name `game` and the old member names during the swap; rename later (`016e`)."
- 8: "Screens keep the host."
- 9 (replaced by 12): "`DrWily`'s screen switch becomes a `LevelState` callback, not an event, so it stays at the
  same point in the frame."
- 12 (2026-10-28, replaces 9, recorded on `master`): `DrWily` submits a new event `GAME_COMPLETE`, and the level
  screen switches to the ending screen when it gets it, in the same way as `END_LEVEL`. The owner (verbatim): "It
  doesn't seem like good design to tightly couple a flag for the game's ending screen to the level state. Is there a
  better approach to this?", then "I like option c since it reuses the existing global events manager" and "016b can
  remove the dead flag".
- 10: "`WEAPONS_ATTAINED` becomes a typed field that is not saved, outside `GameState` (the saved profile)."
- 11 (2026-10-28, from this scope, recorded on `master`): no forwarding members for a moved value; every caller reads
  the real home.
- From "Proposed scope": "Pooled entities outlive a level, so the holder and every field live for the session. Values
  that change at run time (performance, `paused`, the current level and room) are read when used, not copied." and "A
  field only with a caller."

Not in this branch (coordinator brief): `MegaServices` and the constructor change (`016c`); renames (`016e`).

Also in this branch (owner-approved small changes, 2026-10-28, out of the item scope):

- the drawables map is declared with its real value type, `InsertionOrderPriorityQueue`;
- the dead `endGame` flag of `VICTORY_EVENT` in `MegaLevelScreen` is removed (owner: "016b can remove the dead
  flag").

## Current state

Full scan: `docs/SCAN.md`. Base `07307f156`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `MMG` =
`C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`.

- The 7 reads of the item are all still in the code: `DrWily.kt:152` (`setCurrentScreen(ENDING_SCREEN)`, inside
  `engine.update`), `Starkner.kt:414` (`paused`, in the sprite `preProcess`, which also runs during a pause),
  `ControllerComponent.kt:99`, `AirDashBehavior.kt:33`, `:38` (`selectButtonAction`), `Bat.kt:405`,
  `SubmarineJoe.kt:281` (`getShapes()` behind `DEBUG_PATHFINDING`, which is `const false`, so it never runs).
- `WEAPONS_ATTAINED` is done: `016a` made it `game.weaponsAttained` (`MMG:224`). No `016b` change.
- Other entity reads that no planned holder object answers yet (not host-only): `getPerformance()` 8,
  `updatables` 7, `runQueue` 1, `getSystem(GroupsSystem::class)` 1, and 11 calls that pass `game` to
  `delayRun`/`delayNextPossibleSpawn`/`isNextPossibleSpawnDelayed` (`SCAN.md` section 3). Left to `016c` (open
  question 1).
- `paused` (`MMG:229`) and `selectButtonAction` (`MMG:216`) are session values that change at run time. Outside
  entities, code reads `paused` 17 times (16 in screens and the launcher, 1 in `setCurrentScreen`, plus the 2 checks
  inside `pause()`/`resume()`) and reads or writes `selectButtonAction` on 8 lines (`ControllerSettingsScreen` 7,
  `MLS:918` 1).
- The drawables map (`MMG:194`, `:881`; `MLS:195`) is declared with `java.util.Queue` values, but every value is an
  `InsertionOrderPriorityQueue`.

## In scope

Where each read goes, and which `016c` holder field answers it (the holder field names follow `ITEM.md`; decision 5
keeps the member names):

| Read | `016b` home | Line after `016b` | `016c` holder field |
|---|---|---|---|
| `DrWily.kt:152` | new event `EventType.GAME_COMPLETE`, handled by `MLS.onEvent` | `game.eventsMan.submitEvent(Event(EventType.GAME_COMPLETE))` | `eventsMan` (events) |
| `Starkner.kt:414` | new session class `GameClock` with `paused` | `game.clock.paused` | `clock` |
| `ControllerComponent.kt:99`, `AirDashBehavior.kt:33`, `:38` | new field `MegaPlayers.selectButtonAction` | `game.players.selectButtonAction` | `players` |
| `Bat.kt:405`, `SubmarineJoe.kt:281` | not in this branch (open question 5) | no change | to decide before `016c` |
| `Megaman.kt:921` (`weaponsAttained`) | done in `016a` | no change | `weaponsAttained` |

Core:

1. The game ending as an event:
   - `EventType`: new key `GAME_COMPLETE`, next to `VICTORY_EVENT` and `END_LEVEL`.
   - `DrWily.update`: `game.eventsMan.submitEvent(Event(EventType.GAME_COMPLETE))` at the same line and under the
     same condition (`LAND`, the still timer just finished, the target player live) in place of the
     `setCurrentScreen` call. It still happens once per `DrWily`.
   - `MLS`: `GAME_COMPLETE` in `eventKeyMask`, next to `END_LEVEL`; in `onEvent`, a branch next to `END_LEVEL` that
     calls `game.setCurrentScreen(ScreenEnum.ENDING_SCREEN.name)`. No other work in the branch: today the ending
     path does not call `TURN_CONTROLLER_ON`, `addLevelDefeated` or `screenOnCompletion`, so the new branch does not
     either.
   - Timing: the switch moves from inside `engine.update` (the `UpdatablesSystem`) to `eventsMan.run()` later in the
     same frame N, which is where `END_LEVEL` and game over switch today. The level still draws in frame N, and the
     ending screen still renders first in frame N+1. See "Revision 2" for the internal differences in frame N.
1a. `MLS`, `VICTORY_EVENT` branch (`MLS:852-857`): remove the dead `endGame` read
   (`event.getOrDefaultProperty(ConstKeys.END, false, Boolean::class)`) and call `endLevelEventHandler.init()` with
   no condition. No sender puts `ConstKeys.END` on `VICTORY_EVENT` (the 3 senders: `AbstractBoss` through `BOSS_DEAD`
   with `BOSS`, the room event `success` with `ROOM`, `ENTER_BOSS_ROOM` with no properties), so the read is always
   `false`. `ConstKeys.END` stays: it has other users.
2. New class `GameClock` (`C/state/GameClock.kt`): `var paused = false`. The game gets `val clock = GameClock()`,
   built with the game object (like `levelState`). The game member `paused` goes. `pause()` and `resume()` stay as
   actions: they are not forwards of a value; they also log, call the current screen's `pause()`/`resume()` (which
   turns the systems off and on and plays the pause sound) and keep their early return. They read and write
   `clock.paused` at the same lines. Every reader of `game.paused` reads `game.clock.paused`: `Starkner`, `MLS` 7,
   `RobotMasterIntroScreen`, `RobotMasterIntroScreenV2`, `temp_BossIntroScreen`, `MainMenuScreen`,
   `LevelSelectScreen`, `LevelSelectScreenV2`, `MMG.setCurrentScreen` (`clock.paused`), and `DesktopLauncher` 3.
3. `MegaPlayers`: new field `var selectButtonAction = SelectButtonAction.NEXT_WEAPON` (the default that `create()`
   sets today). The game member `selectButtonAction` goes, and so does its write in `create()` (`MMG:305`): the field
   starts with the same value, and no code reads it before `postCreate`. Every caller uses
   `game.players.selectButtonAction`: `ControllerComponent`, `AirDashBehavior` (2), `ControllerSettingsScreen` (7
   lines), `MLS:918`.
4. Drawables map: `ObjectMap<DrawingSection, InsertionOrderPriorityQueue<IComparableDrawable<Batch>>>` in the field
   `MMG:194`, the local map in `createGameEngine` (`MMG:881`), the return type of `getDrawables()` (`MMG:991`, now
   explicit), and the field `MLS:195`. No cast, same objects, same calls.

Docs: root `CLAUDE.md`, "Game object": add `clock: GameClock` (the pause flag, `clock.paused`; `pause()`/`resume()`
change it), and say that `selectButtonAction` lives in `players` (`MegaPlayers`). "Events" or "Screens": the game
ending is the event `GAME_COMPLETE` (`DrWily` submits it; the level screen switches to `ENDING_SCREEN`).

Tests:

5. Temporary `Temp016bDrWilyEquivalenceTest` with `Temp016bFrozenDrWily` (core test sources): the frozen copy is the
   old `DrWily` from the base, with its `game.setCurrentScreen` call, which the old side records. The new side is the
   real `DrWily`; its `GAME_COMPLETE` submits are recorded. Both run the same seeded sequences of spawn values and
   deltas, with the target player alive or dead. After each update: the same state, position and facing, and the
   same number of requests at the same update (each old request names `ENDING_SCREEN`; each new request is a
   `GAME_COMPLETE` event).
6. Permanent tests:
   - `DrWily`: it submits `GAME_COMPLETE` once, when the still timer finishes in `LAND` and the target lives; never
     when the target is dead.
   - `MLS` (checked practical on 2026-10-28: a `MegaLevelScreen` builds with a relaxed game mock in a unit test, and
     `onEvent` runs): `eventKeyMask` contains `GAME_COMPLETE`, and `onEvent(GAME_COMPLETE)` calls
     `game.setCurrentScreen(ENDING_SCREEN.name, ...)` once. If `MLS.init` can run in a unit test, `VICTORY_EVENT`
     with and without `ConstKeys.END = true` starts the end-level handler in the same way (else the play test covers
     it).
   - `GameClock` and the game: `clock.paused` starts `false`; `pause()` sets it, a second `pause()` changes nothing,
     `resume()` clears it (a real `MegamanMaverickGame`, as in `LevelStateTest`).
   - `Starkner`: the sprite `preProcess` keeps the rotation while `clock.paused` is `true` and turns the sprite when
     it is `false`.
   - `MegaPlayers`: `selectButtonAction` starts `NEXT_WEAPON` and keeps a value that is set.

New API and first production caller:

- `EventType.GAME_COMPLETE`: submitted by `DrWily.update`; handled by `MLS.onEvent`.
- `GameClock` (class) and `GameClock.paused`: `MegamanMaverickGame.clock`; written by `MMG.pause`/`resume`; read by
  `Starkner`, `MLS`, the menu and intro screens, `MMG.setCurrentScreen` and `DesktopLauncher`.
- `MegamanMaverickGame.clock`: the same callers.
- `MegaPlayers.selectButtonAction`: `ControllerComponent.select`, `AirDashBehavior`, `ControllerSettingsScreen`,
  `MLS.isPauseButtonRequested`.

Removed API: `MegamanMaverickGame.paused` and `MegamanMaverickGame.selectButtonAction`; the `ConstKeys.END` read on
`VICTORY_EVENT`.

## Out of scope

- `MegaServices`, the constructor type, the factory, the `IMegaGameEntity` cleanup (`016c`).
- The `016a` forwards on the game (`getCurrentLevel()`, `getCurrentRoom()`, `getRooms()`, `getTiledMapLoadResult()`,
  `isCameraRotating()`, `setFocusSnappedAway()` and the others): `016c` decides them.
- The reads in `SCAN.md` section 3 (clock performance, scheduler, groups, the `game`-taking functions): `016c`.
- The pathfinding debug shapes (`Bat.kt:405`, `SubmarineJoe.kt:281`, `DEBUG_PATHFINDING`, the `shapes` parameter of
  `MegaPathfinderResultConsumer.consume`): the owner uses this debug path now and then, so it stays as it is. A
  separate research agent looks for a better approach. Their home must be decided before `016c`.
- A per-slot `selectButtonAction` or a settings object per slot (`031`, `047`).
- Pause as an overlay, or removing the `paused` checks (`013`).
- Renames (`016e`).
- The `size` bug of `InsertionOrderPriorityQueue` (`SCAN.md` section 6): already filed as
  `docs/issues/2026-10-22-insertion-order-priority-queue-size.md`.
- A general model of level outcomes (victory, game complete, game over, quit), and whether the ending is the
  `screenOnCompletion` of `WILY_STAGE_3` with `addLevelDefeated(WILY_STAGE_3)` (a profile change): `013`.

## Affected areas and risks

- Files: `MMG`, `MLS`, new `C/state/GameClock.kt`, `C/events/EventType.kt`, `C/players/MegaPlayers.kt`, `DrWily`,
  `Starkner`, `ControllerComponent`, `AirDashBehavior`, `ControllerSettingsScreen`, `RobotMasterIntroScreen`,
  `RobotMasterIntroScreenV2`, `temp_BossIntroScreen`, `MainMenuScreen`, `LevelSelectScreen`, `LevelSelectScreenV2`,
  `lwjgl3/.../DesktopLauncher.kt`. Root `CLAUDE.md`. Tests in `core/src/test`.
- Risk: the level screen does not get `GAME_COMPLETE`. Mitigation: `MLS` is a listener from `MLS.start` until
  `MLS.reset`, so it is a listener while `DrWily` runs; the permanent `MLS` test checks the mask and the branch; the
  play test checks the whole path.
- Risk: the switch moves in the frame (owner-accepted, decision 12). It moves to `eventsMan.run()` of the same frame,
  the point that `END_LEVEL` and game over use. No visible output changes ("Revision 2"). It is an intended timing
  change in `CONTRACT.md`.
- Risk: `endGame` removal. No sender sets the flag, so `VICTORY_EVENT` always starts the end-level handler today;
  the permanent `MLS` test (if practical) and a play check of a robot master victory cover it.
- Risk: `selectButtonAction` read before `players` exists. Today no code reads it before `postCreate`; the readers are
  screens built in `postCreate` and `Megaman`. A read of `game.players` before `postCreate` fails, as the old
  `lateinit` field did before `create()`.
- Risk: `selectButtonAction` reset. `players` is built once in `postCreate` (`MMG:421`) and never replaced, so the
  setting lives for the session, as today.
- Risk: a stale `paused`. Mitigation: `GameClock` holds the only copy; every reader reads it when used.
- Risk: a missed `paused` or `selectButtonAction` caller. Mitigation: the game members are removed, so the compiler
  finds every caller in `core` and `lwjgl3`.
- Risk: the drawables type change. It is a declaration change only; the compiler proves that every value is an
  `InsertionOrderPriorityQueue`.
- Relaxed game mocks in tests: `game.clock` and `game.players` return relaxed mocks (`false`, the first enum value or
  a mock). Tests that run the changed code paths set real objects.

## Overlap with other branches

No other code branch is active. The parent `mega-refactor-016-master` is the merge target; the branch start
`07307f156` is its tip. `031` (no active branch) plans `selectButtonAction` per slot; the field in `MegaPlayers` is the
place that `047` makes per slot. `013` (planned) removes the `paused` checks later. `016c` builds on the homes in this
branch.

## Open questions

None. Resolved by the owner (2026-10-28, "approved"). Questions 1 to 4 and 7 to 9 are accepted as recommended;
questions 5 and 6 are changed by the owner.

1. The range of `016b`: only the 7 item reads. Accepted. The reads in `SCAN.md` section 3 go to `016c`.
2. The `DrWily` callback: `LevelState.showEndingScreen: (() -> Unit)?`, set once in `MLS.init`, called with
   `!!.invoke()`. Accepted in revision 1; replaced in revision 2 by the `GAME_COMPLETE` event (decision 12).
3. The home of `paused`: a new session class `GameClock` in `C/state/`, held by the game as `clock`. Accepted.
4. The home of `selectButtonAction`: a field of `MegaPlayers`. Accepted.
5. Debug shapes. Recommended: delete the debug path. Changed by the owner: the owner uses the pathfinding debug
   shapes now and then, so do not delete them. They stay as they are in this branch (see "Out of scope"); a separate
   research agent looks for a better approach.
6. Forwarding members. Recommended: keep `game.paused` and `game.selectButtonAction` as forwards. Changed by the
   owner (verbatim): "I'm not sure I agree with this. Forwarding patterns like this can obfuscate the actual
   source/target of things." So: no forwarding members; the two game members go, and every caller reads the real
   home. `pause()` and `resume()` stay as actions, because they do more than set the flag (in-scope item 2). The
   `016a` forwards do not change here. Recorded on `master` as `016` decision 11.
7. The temporary suite: only for `DrWily`. Accepted.
8. Root `CLAUDE.md`: add `clock`, the `selectButtonAction` home and the ending callback to "Game object". Accepted.
9. Manual checks: real rooms, no `RefactorTest016` map. Accepted.

## Revision 2 (2026-10-28): the game ending as an event

Reason: at the plan review the owner asked (verbatim): "It doesn't seem like good design to tightly couple a flag for
the game's ending screen to the level state. Is there a better approach to this?" A research note (copied in
`docs/NOTES.md`, "Research: the `DrWily` ending switch") gave 6 options. The owner chose option (c) (verbatim): "I
like option c since it reuses the existing global events manager" and "016b can remove the dead flag". Recorded on
`master` as `016` decision 12, which replaces decision 9.

Changed parts: "Item" (decision 12, the second small change), "In scope" items 1 and 1a, the docs line, tests 5 and 6,
"New API", "Removed API", "Out of scope", "Affected areas and risks".

What changes in frame N (the frame in which `DrWily` asks for the ending), from the research:

- Today `setCurrentScreen` runs inside `engine.update`. `MLS.reset` then removes the `MLS` listener at once,
  `engine.reset()` only sets a flag (the engine resets at the end of its own update), the other systems run on a level
  that is half reset, the rest of `MLS.render` runs on the reset level, and the level draws once more
  (`screen` is a local in `MMG.render`). The ending screen renders first in frame N+1.
- With the event, `MLS` switches in `eventsMan.run()` of frame N, after `screen.render` and `screen.draw`. So the
  systems and the rest of `MLS.render` run on the live level (the normal frame); the level draws once more, as today;
  `engine.reset()` runs at once (the engine does not update then); `MLS` and the destroyed entities still get the
  frame-N events that come after the switch (a listener removal during `run()` is deferred); `TURN_CONTROLLER_ON`
  from `MLS.reset` arrives in frame N+1 (one frame later than today). This is the same state as the `END_LEVEL` and
  game-over paths today. The ending screen still renders first in frame N+1; the music order (`unsetMusic`, then the
  ending music) does not change, because both calls are inside `setCurrentScreen`.
- No output that a player can see changes. The player is damage-locked and the controller is off during the
  `DrWily` scene.
