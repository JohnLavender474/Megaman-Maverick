# Contract: mega-refactor-151a-level-completed-rename

- Status: approved
- Approved: 2026-11-11

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes (`LaunchOptionsResolverTest` covers `--saveFile`; `GameStateSaveStringTest` pins the save string
  `MOON_MAN,GLACIER_MAN;A,C;B,D;JUMP_BOOST,AIR_DASH_BOOST;42;HARD`, unchanged).
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `grep -rn "LevelDefeated\|LevelsDefeated\|levelsDefeated" core/src engine/src` gives no line.
- [ ] `grep -c "defeated" core/src/main/kotlin/com/megaman/maverick/game/Constants.kt` is unchanged (the
  `ConstKeys.DEFEATED` value stays) and `git diff master --stat` names no boss file, no `EventType.kt`, no
  `MegaLockReason.kt`, no `fights/` file and no `assets/` file.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-151a-level-completed-rename`.

- `./run-game-alpha.sh --level MOON_MAN --room room14 --heartTanks all --healthTanks all`: beat Moon Man (`room14` is
  the room before the boss room, with a spawn). The get-weapon screen gives Moon Scythes (through
  `Megaman.onLevelCompleted` and `weaponsAttained`), and then the save screen opens. The tank flags turn the save write
  off, and with no intro stage done its CONTINUE starts the intro stage, so stop at the save screen.
- Save-file scenarios with `--saveFile` (owner's request, 2026-11-11). The files are in
  `temp/mega-refactor-151a-level-completed-rename/saves/`, and `reset-saves.sh` there copies them into `~/.prefs/`
  (the game overwrites a file when it saves, so run the script again to reset). Each run: main menu, LOAD SAVE FILE.
  - `./run-game-alpha.sh --saveFile 151a-fresh` (`INTRO_STAGE` only): the stage select opens with no level marked.
    Beat Moon Man and save. `~/.prefs/151a-fresh` then holds `INTRO_STAGE,MOON_MAN;...` (the write path).
  - `./run-game-alpha.sh --saveFile 151a-no-intro` (`MOON_MAN`, no intro): LOAD starts the intro stage, because the
    main menu reads the intro stage flag, not the count.
  - `./run-game-alpha.sh --saveFile 151a-glacier` (`INTRO_STAGE,GLACIER_MAN`): Glacier Man is marked. Choose Inferno
    Man: `room1` has ice blocks, because the level is frozen when Glacier Man is done and Inferno Man is not.
  - `./run-game-alpha.sh --saveFile 151a-all-rm` (intro and all 8 robot masters): the center face is Dr. Wily and the
    second stage-select track plays. The Wily entry starts `WILY_STAGE_1`. Replay Moon Man to its boss room: the
    victory fires on entry with no fight, because the level is already completed.
  - `./run-game-alpha.sh --saveFile 151a-wily2` (all robot masters and `WILY_STAGE_1`): the Wily entry starts
    `WILY_STAGE_2`.
  - `./run-game-alpha.sh --saveFile 151a-bad-entry` (`INTRO_STAGE,MOON_MAN,BOGUS_MAN`): the save loads, Moon Man is
    marked, and the log has one `fromString(): failed to add level: BOGUS_MAN` error.
  - `./run-game-alpha.sh --saveFile 151a-none` (no file): LOAD SAVE FILE does nothing.

## Behaviors to check

Both are inside the 2 checks above: the load path of an old save file (unchanged string format), and the save screen
after a stage.

## Intended changes

None in gameplay. A new dev flag `--saveFile <name>` (a small QoL change, owner's request at the play checks, 2026-11-11). Code only: the family is renamed, `allLevelsDefeated` is deleted, and 3 wrong exception messages
in `GameState` name the right collection.
