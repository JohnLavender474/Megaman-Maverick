# Plan: mega-refactor-178a-bot-input

How to do the work in `SCOPE.md`. It needs no approval. When the plan changes, write the reason in the process log.

Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`. Read `docs/CODE_STYLE.md` before each code
step. Do not launch the game.

New API and its first caller in the branch:

| API | First caller |
|---|---|
| `LaunchOptions.bot`, `DesktopAppArgs.bot` (`--bot`) | `resolveLaunchOptions` |
| `BotKind` enum, `ResolvedLaunch.bot` | `MegamanMaverickGame.create()` (makes the brain) |
| `ResolvedLaunch.slots` | the 4 slot-count sites in `MegamanMaverickGame.kt` |
| `FollowBrain` | the input step of `MegamanMaverickGame.render()` |
| `MegaPlayers.isBot(slot)` (set in `postCreate()`) | `PlayersFocus` `excluded` in `MegaLevelScreen` |

## Steps

### 1. Test map `REFACTOR_TEST_178`

- Changes: a subagent builds `assets/tiled_maps/tmx/RefactorTest178.tmx` with the `level-builder` skill: rooms
  `follow_flat` (2 views wide, flat with a few low blocks), `follow_steps` (steps of 2 to 3 tiles up), `follow_pit`
  (a pit 3 tiles wide that drops out of the view to a floor in the same room, no death fixture), `follow_outrun`
  (2 views wide, a wall about 5 tiles high in the middle that P1 can wall-jump or slide past and a copy can miss),
  `follow_exit` and `follow_exit_next` (a side exit between 2 rooms), `follow_enemies` (2 `Met`s on a flat floor).
  Each room has a player spawn and painted `tiles`. Add `LevelDefinition.REFACTOR_TEST_178` (`TEST_LEVEL`,
  `SAVE_GAME_SCREEN` on completion) and its line in `LevelCompletionScreensTest`.
- Check: `level-analyzer` lists the 7 rooms, each with a spawn; `./gradlew core:test --tests
  'com.megaman.maverick.game.levels.LevelCompletionScreensTest'` passes.
- Done: yes

### 2. Launch option `--bot follow`

- Changes: `LaunchOptions.bot`; `DesktopAppArgs` `--bot` (description and the start print, as `--players`);
  `BotKind { FOLLOW }`; `ResolvedLaunch.bot` and `slots`. Rules in `resolveLaunchOptions`: an unknown kind, `--bot`
  with `--players 2`, `--bot` with `--mode pvp`, and a level with no `MEGAMAN` are errors; with a bot the mode is
  `COOP`, `--mode coop` and `--viewEdges` are allowed, and the default view edges are the co-op ones with `left` and
  `right` `WARP` (D2). `CLAUDE.md` launcher paragraph: one sentence for `--bot follow`.
- Check: new `--bot` cases in `LaunchOptionsResolverTest` pass.
- Done: yes

### 3. `FollowBrain`

- Changes: `players/bots/FollowBrain.kt`. Input each render frame: the leader's built frame, `delta`. It reads the
  leader (slot 0) and the bot (slot 1) through `MegaPlayers`. A ring buffer allocated once records the leader's held
  bits, center and `delta`; the output is the record from `DELAY` seconds back, with `START` cleared, `LEFT` /
  `RIGHT` replaced by a press toward the recorded leader X when the bot is more than `STEER_DISTANCE` away, and a held
  `A` for `ASSIST_JUMP_TIME` when the recorded leader is `ASSIST_HEIGHT` above, the bot stands, and its X has not
  changed by more than a small amount for `STUCK_TIME`. Nothing is recorded while `clock.paused` or while the leader
  is not live; the output is `0` while the bot is not live. Start values from the Sonic 2 CPU rules
  (info.sonicretro.org, "SPG: Player 2"; source in `NOTES.md`). A `TODO` names `178d` for a `BotBrain` interface.
  `FollowBrainTest`: the delay, the `START` mask, the steer, the jump assist, no record while paused.
- Check: `./gradlew core:test --tests 'com.megaman.maverick.game.players.bots.FollowBrainTest'` passes.
- Done: yes

### 4. Game wiring

- Changes: in `MegamanMaverickGame.kt`: the 4 slot-count sites read `launch.slots`; with a bot, slot 0 takes
  `PadShare.ALL` and slot 1 has no pad; `create()` makes the `FollowBrain`; `postCreate()` marks slot 1 as a bot
  (`MegaPlayers.isBot`, `TODO` for `178e`) and copies slot 0's `selectButtonAction` to slot 1; the input step builds
  slot 1's frame from the brain (brief code sketch). `TODO` at `SlotRespawn` for a free bot death (`178e`, D3).
- Check: `./gradlew lwjgl3:jar` builds; `core:test` passes.
- Done: yes

### 5. Camera on P1

- Changes: `MegaLevelScreen` passes `{ edges.isHeld(it) || game.players.isBot(it.slot) }` to `PlayersFocus` (D1).
- Check: `lwjgl3:jar` builds; a `PlayersFocus` test (or a case in the existing one, if any) shows that a bot slot is
  left out of the focus.
- Done: yes

### 6. Automated checks and the review before the play test

- Changes: none, unless the review finds a problem.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule` passes; one
  Sonnet review subagent checks the diff and each manual check (`docs/mega-refactor/CLAUDE.md`); play scripts in
  `temp/play/`.
- Done: yes
