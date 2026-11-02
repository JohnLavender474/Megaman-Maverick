# Notes: 082 Dialogue boxes, in-level dialogue scenes and cutscenes

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-29: first scan (`master` at `05e6c93ca`)

Short paths: `C` = `core/src/main/kotlin/com/megaman/maverick/game`, `E` = `engine/src/main/kotlin/com/maverick/game/engine`,
`S` = `C/screens`.

### What exists in this game

- **No dialogue, cutscene, text-box or portrait code or assets.** A grep for `dialog|cutscene|textbox|text_box` finds
  nothing in `core` or `engine`. The atlases have no box or portrait regions. No sound exists for a text blip; the
  nearest is `CURSOR_MOVE_BLOOP_SOUND` (`C/assets/SoundAsset.kt:34`).
- **Text primitive:** `MegaFontHandle` (`C/drawables/fonts/MegaFontHandle.kt:17`) wraps the engine
  `BitmapFontHandle` (`E/drawables/fonts/BitmapFontHandle.kt:19`). No wrapping, width limit, color markup or reveal
  state. Each instance runs a new `FreeTypeFontGenerator` (`BitmapFontHandle.kt:36-46`). Font `Megaman10Font.ttf`
  (`C/Constants.kt:21`) at size `PPM/2 = 16`. 39 `MegaFontHandle(` constructions in 21 files.
- **Typewriter text, 3 hand-written copies:** a `LinkedList<Runnable>` of prefix substrings and a
  `Timer(LETTER_DELAY = 0.2f)`. No sound, no skip.
  - `S/other/RobotMasterIntroScreen.kt:47,101-102,200-208,238-245` (live).
  - `S/other/RobotMasterIntroScreenV2.kt:49,108-109,212-219,290-296` (not registered; `C/MegamanMaverickGame.kt:435`).
  - `S/other/GetWeaponScreen.kt:68,100-101,216-223`.
- **Story screens:** `AbstractStoryScreen` (`S/other/AbstractStoryScreen.kt`), the base of `OpeningScreen` and
  `EndingScreen`. Data: `slides: OrderedMap<Array<String>, Float>` (lines to seconds, `:35`). A `Queue` of slides and a
  `FADE_IN/SHOW/FADE_OUT` enum (`:39`, `:122-135`). Centred lines, no box. START skips everything (`:110-114`). The
  text is in Kotlin (`OpeningScreen.kt:13-31`, `EndingScreen.kt:13-20`).
- **Other sequenced screens**, each with its own queue and enum: `GetWeaponScreen` (`:73-80`, `:156-176`),
  `LogoScreen` (`:45-66`), `RobotMasterIntroScreen` (all timing hard-coded, `:46-48`; text per boss in a `when`,
  `:303-581`), `CreditsScreen` (reads `assets/credits.txt`, the only text from a data file).
- **The only in-level cutscene:** `WilyFinalBoss.onDestroy` (`C/entities/bosses/WilyFinalBoss.kt:290-311`) spawns
  `DrWily`, locks each player (`DAMAGE`, `WILY_FINAL_BOSS`) and submits `TURN_CONTROLLER_OFF`. `DrWily`
  (`C/entities/special/DrWily.kt:62`, `:125-155`) runs a private `FLY_UP/FALL/LAND` enum with hand-run timers, plays
  `MM3_ALL_CLEAR_MUSIC` and goes to the ending. Nothing turns the controller on again on this path; only
  `MegaLevelScreen.reset()` does (`S/levels/MegaLevelScreen.kt:1131`).

### How gameplay freezes in a level today

- **`TURN_CONTROLLER_*`:** handler at `C/MegamanMaverickGame.kt:650-667`. It sets `slot0FrameBuilder.locked` and
  `ControllerSystem.on`. 13 submits in 5 files (6 OFF, 7 ON).
- **Constraint:** `game.controllerPoller` is gone (`031f`). Menus read `players.menuInput`, which is the frame of
  slot 0, so the lock still blinds it. While the controller is off, a dialogue box could not read buttons from that
  frame. This item builds an unlocked frame when it has a caller, with the system frame (`game.systemInput`, a builder
  that the lock does not touch) as the pattern. `031` does not do this split.
- **Direct `GameSystem.on` writes:** 18 lines in 4 files (`MegamanMaverickGame`, `MegaLevelScreen` 8,
  `PlayerStatsHandler` 6, `LevelStateHandler`). Each writes absolute values; the last writer wins. `012` replaces them
  with named reasons. Known bug: a pause during a gate opening turns physics on again
  (`docs/issues/2026-10-22-pause-during-gate-reenables-physics.md`).
- **Locks:** `MegaLockGroup` `DAMAGE, MOVEMENT, BEHAVIORS`; `MegaLockReason` has 7 reasons. 13 lock and unlock calls in
  5 files. `PlayPhase { PRE_PLAY, IN_PLAY, POST_PLAY }`.
- **Boss intro:** `ENTER_BOSS_ROOM` → `BossSpawnEventHandler` spawns the boss with `ready=false` → the boss
  `isReady(delta)` → `BOSS_READY` → health fill (all systems except Sprites and Animations off,
  `MegaLevelScreen.kt:762-794`) → `END_BOSS_SPAWN` turns all on → boss music (`AbstractBoss.kt:274-288`). No camera
  move. A pre-fight talk fits between the boss entry and `BOSS_READY`.
- **Level handlers:** a first-unfinished-wins `when` at `MegaLevelScreen.kt:954-959` (bossHealth, endLevel,
  playerSpawn, playerDeath). `013` cites `:906-913`, which is out of date.

### Overlays, audio, triggers

- **UI overlays** are hard-coded fields drawn in a fixed order in `MegaLevelScreen.draw` (`:1014-1074`): points and
  coins, boss bar, player stats, "READY", death fade, pause screen, checkpoint text. A dialogue box is one more field
  and one more `if`, unless `013` (overlay stack) or `026` gives a better place.
- **Audio:** `MegaAudioManager` has one current track: `playMusic`, `pauseMusic`, `stopMusic`, `fadeOutMusic`. No
  stack and no restore of the music before a scene. Pause pauses only sound effects (`LevelStateHandler.kt:21`).
- **Triggers:** the Tiled `triggers` layer builder does nothing (`TriggersLayerBuilder.kt:9`), and no TMX has the
  layer. `EventTrigger` (`C/entities/special/EventTrigger.kt:13-40`) submits an `EventType` on spawn (used in
  `DesertMan_16x14_v4.tmx` 15 times and `WilyStage1_v2.tmx` 3 times, in `specials`). Spawners support `spawn_room`
  and `spawn_event`. `EventType` has 48 entries, none for dialogue.

### Related items

- `012` (planned): the freeze mechanism. Its `NOTES.md:37` lists a `CUTSCENE` reason and `:44` turns
  `TURN_CONTROLLER_*` into `push`/`pop(CUTSCENE_INPUT)`. Its line refs are out of date.
- `013` (planned): a dialogue state or overlay in the level flow; pause as an overlay stack.
- `015` (planned): `Fsm<S>` with state timers. A scene step list can use it, or stay a plain list.
- `031` (active): input frames. Its `NOTES.md:993` lists "cutscene script" as an input source.
- `051` (planned): camera behaviors; a scene camera target or pan is one more behavior.
- `026` (planned): render layers; covers world draw order, not UI.
- `050` (planned): the data file format for later scene files.
- `053`, `057` (idea): no new order-dependent event chains; typed "scene started/ended" signals.
- `029` (done): cutscenes act on all players (`029/NOTES.md:455`, `:1052`).
- `038` (planned): a death during a scene must end the scene cleanly.
- `007` (planned): saved seen flags.
- `060`, `081`: per-character lines.

## 2026-10-29: X6 (sources: `~/IdeaProjects/mmx-demake-tools/docs`, `mmx-x6` at its current head)

Facts only; no code or data is copied. `D` = `~/IdeaProjects/mmx-demake-tools/docs`, `E6` = `D/engine.md`.

### The text box (`E6`, "Text", "The box")

- One engine text box, opened with `TextOpen(id, unused, mode)`. If a box is open, the new id is queued.
- Window: x 16–304, y 22–96 (top) or y 142–216 (bottom) on a 320×240 screen. No slide or grow animation; a delay of
  about 50 updates on open and on close.
- States: open delay → print (one character per 5 updates [likely]) → wait for a press → close delay → idle.
- Any face or shoulder button advances. A press while printing prints the rest of the page. A guard stops the same
  press from also skipping the next page. **No Start-to-skip** for in-stage text; Start opens pause only when the
  player is not stopped (`E6`, "Stage play").
- A typing sound on each printed glyph (not on the "no glyph" code, and not in the scene stage).
- A page has a name line and 1–2 body lines. A blinking arrow at the end of a page.
- Portraits: two (left, right) or one (centre), 80×64 px, with talk frames (X and Alia 6, High Max 4). The slot to
  character map is per stage.
- Text banks are per stage and per character (X or Zero), so a scene can have different lines for each character
  (`E6`, "Text", "Banks on disc").

### The script format (`E6`, "Text", "Bank", "Header", "Character word")

- A message is a header (portraits on, one or two portraits, slots) and a list of 16-bit character words. Each word
  carries the glyph and the control bits: speaker slot (read on the first line of a page), box at the bottom, "wait
  after this character", end of page, line break, end of message.
- No choice code and no timed-wait code. The speaker name is part of the text ("Alia: ..."); "??" hides a name before a
  reveal.
- Decoded scripts: `D/stages/intro.md`, "Texts"; `D/enemies/gate.md`, "Talk".

### Freezing (`E6`, "The game frame", "Stop flags"; "Talk zones"; "Events")

- Stop flags: one byte per object class (player, weapon, main, visual, effect, stage logic, stage objects, misc, ray,
  kind-7). While a class flag is set, only objects with flag bit `0x08` update. Frozen main, visual and effect objects
  are still drawn.
- **Talk zone (forced):** the player is held in event step 20 (stand), player shots are cleared, every class except
  the player is stopped, and the zone exempts itself. The text box update runs outside the object loops, so it never
  freezes.
- **Boss pre-fight talk and the intro Hunters' scene: no stop flags.** Only the player is held; the boss script waits
  until the box closes. So X6 has the same two levels as Cave Story `<KEY>`/`<PRI>` (see below).
- Player events: `PlayerEvent(step, facing)` sets an input lock; step 20 = hold, 29 = scripted walk at 1 px/update,
  also walk-to, jump, leap (`D/players/ladder-hurt-events.md`, "Events").

### Triggers

- **Talk zones** (`E6`, "Talk zones"): a per-stage box table; the player must be inside.
  - Forced: hold the player, wait until the player stands, then open.
  - Optional: a "!" HUD icon and a sound on entry; Select opens it.
  - Seen bits, one per text, are never cleared.
- **Boss pre-fight** (the same in each boss sheet, for example `D/enemies/blizzard-wolfang.md`): WARNING → stage music
  stops → the boss enters → text, shown the first time only (a retry gets a 60-update wait with no text) → boss music
  → HP fill → the player is released. `D/enemies/dynamo.md` also has a post-fight text, then a beam-out.
- **Event objects:** the intro Hunters' scene (hold → walk → text → beam-out), Zero after the D-1000 (an animation
  script, then text, then High Max) (`D/stages/intro.md`, "Events"; `D/enemies/high-max.md`).

### Music (`E6`, "Boss entry and exit"; `D/enemies/light-capsule.md`)

- Boss: the stage music stops before the talk; the boss music starts after the box closes.
- Light capsule: stops the stage music, plays a voice stream, opens the text, then restarts the stage music.

### Out of stage

- Movies (opening, logo) with a skip-button mask per movie (Start, or Start or a face button) (`E6`, "Front end",
  "Movies").
- Engine "scenes": a special scene stage with its own overlay per scene, a list of scenes after a stage clear, the same
  text box with a 1-update open delay and no typing sound (`D/enemies/high-max.md`, "Where the clear leads"). The
  ending is not read yet.
- Boss intro before a stage: no text box; the name is a graphic (`E6`, "Boss intro").

### `mmx-x6`

- No dialogue system. `World.showMessage(text, updates)` draws a timed string in the HUD (`World.java`, `Hud.java`).
  `World.freeze(updates)` is one global counter. `docs/gaps.md` lists the missing boss texts and talk zones.

## 2026-10-29: other games and engines (web research)

### Mega Man fan engines

- **Megamix Engine** (GameMaker 1, <https://github.com/MegamixEngine/MegamixEngine>, free for non-commercial use, no
  SPDX license). The closest match to this game.
  - `objDialogueBox` locks the player only (a lock from a lock pool: move, turn, jump, slide, shoot, climb, charge,
    pause); enemies keep running. Box at the top or bottom, speaker name and color, a 48 px mugshot, 0.5 characters
    per frame, ×4 while Jump is held, Start jumps to the end of the block, a choice menu after the last block, an end
    callback on the parent.
  - Inline codes: color, pause for N frames, inline sprite, auto-advance, "type the rest of the line now", motion
    effect (shake, wave), line break. Blocks are split by pixel width and up to 5 lines.
  - `objPassiveDialogueBox`: plays and closes by itself and locks nothing (chatter while you play).
  - `prtCutscene`: `phase` and `timer`; the timer stops while a box is open. Branches per player character. Named
    marker objects in the room are move targets. Stand-in actors. A skip menu. The end handler destroys the actors,
    frees the locks, resets the camera and records the cutscene as played. Only listed cutscenes are saved as played;
    boss pre-fight scenes are kept out so they play again.
- Open Mega Engine (<https://github.com/rafaelcp/Open-Mega-Engine>): boss intros, no dialogue.
- Mega Man X8 16-bit, Mega Man X Regenesis, Mega Man Maker, MMX: Corrupted: no public source or no documented dialogue
  system.
- SNES Mega Man X: no public disassembly of the text engine found. X5 navigator calls pause the game and cannot be
  turned off; a ROM hack adds a mute option (<https://tasvideos.org/Forum/Topics/27122>), so players want a way to
  turn optional talk off.

### Other games and tools

- **Cave Story TSC** through doukutsu-rs (<https://github.com/doukutsu-rs/doukutsu-rs>, MIT,
  `src/game/scripting/tsc/text_script.rs`). The clearest freeze model:
  - `<KEY`: the player is locked, the world runs. `<PRI`: the world stops too; only the script and the box run.
    `<FRE`/`<END` release both.
  - VM wait states: message, wait N ticks, wait for a press, wait until the player stands, wait for a fade, yes/no.
  - Holding Jump or Shoot types faster. Box position per message (bottom, top). Portrait command.
  - Save flags (`<FLJ`) are the "play once" guard. Skip flags let a retry after death jump past a scene.
- **Celeste** (Everest wiki; LuaCutscenes <https://github.com/Cruor/LuaCutscenes>, MIT; the game is closed source).
  - `CutsceneEntity`: `OnBegin` starts a coroutine; `OnEnd(level)` must put every actor, flag and the camera into the
    end state, because a skip stops the coroutine. **"Skip = jump to the end state"** is the main rule.
  - The world does not freeze. The player is put in a dummy state and walked by the script.
  - Dialog files: `KEY= text`, one file per language, fallback to English. Portrait lines `[THEO left normal]`.
  - Inline tokens: newline, pause, speed, color, wave, shake, and `{trigger N}`, which runs a coroutine in the middle
    of a line so actors can move while the text types.
- **SuperTux** (<https://github.com/SuperTux/supertux>, GPL-3.0): Squirrel scripts with a blocking `wait`.
  Out-of-level cutscenes are special levels with their own script and stand-in actors. Camera scroll, fades,
  letterbox, gettext. `start_cutscene()`/`end_cutscene()` mark the part that can be skipped.
- **TextraTypist** (<https://github.com/tommyettinger/textratypist>, Apache-2.0; successor of `typing-label`, MIT).
  libGDX scene2d typewriter label: tokens for wait, speed, shake, wave, color, event, variable; a listener with
  `event`, `end`, `onChar` (a per-letter blip); `skipToTheEnd`. A possible library, but it is a scene2d widget and
  this game does not use scene2d; a small own typewriter can also do.
- **Dialogic 2** (Godot, MIT): plain-text timelines of events (text, join, leave, portrait, condition, signal, wait),
  inline tags, CSV translations.
- **Yarn Spinner** (MIT): nodes, `<<command>>` lines bound to game handlers that can block, `#line:id` for string
  tables, `visited()` for play once.
- **ink** (MIT): branching text, tags as per-line metadata, external functions. Less suited to actor choreography.
- **Article:** Elias Daler, "How to implement action sequences and cutscenes"
  (<https://edw.is/how-to-implement-action-sequences-and-cutscenes/>): an action list is easy to reuse but awkward to
  branch; coroutines branch well but are hard to save mid-scene; parallel actions joined by "wait for all".

### Synthesis

What most sources share:

1. A scene is one sequence (a step list or a coroutine) of blocking waits: time, a press, the box closed, an actor
   arrived, a fade done, the player on the ground.
2. Text is data with inline tokens (pause, speed, color, effect, event hook). The box pages by pixel width and line
   count and waits for a press at the end of a page.
3. Holding a button types faster; a press finishes the page, then advances; a separate key skips more.
4. Portrait and speaker name per line; box at the top or bottom per message.
5. "Play once" is a flag that the trigger checks. Repeatable scenes (boss pre-fight) leave it out on purpose.
6. Actors: the real player in a scripted or held state, or a stand-in. Move targets are markers placed in the map
   editor.
7. Text in per-language files under stable keys.

Where they differ, and the choice proposed in `ITEM.md`:

- **Freeze scope:** Cave Story and X6 have two levels (player only; world too), Megamix adds a passive box, Celeste
  never freezes. Proposed: `NONE`, `PLAYER`, `WORLD`, each a `012` reason.
- **Script form:** data DSL (TSC, Yarn, Dialogic), coroutines (Celeste, SuperTux) or phase counters (Megamix).
  Proposed: a Kotlin step list first (no coroutines; this matches `015` and the fixed tick of `010`), a data file later
  with `050`.
- **Skip:** fast-forward (Cave Story), jump to the end state (Celeste, Megamix), text only (TextraTypist). Proposed:
  text-only skip on the advance button, and a whole-scene skip on Start that jumps to the end state through `onEnd`.
- **Mid-line hooks:** Celeste and TextraTypist allow them; Cave Story, Megamix and X6 act only between messages.
  Proposed: an `{event=name}` token, because it is cheap once tokens exist; drop it if no first scene needs it.
- **Out-of-level:** a special level (SuperTux) or a separate screen (most others). Proposed: a `CutsceneScreen` that
  shares the runtime and the box.

## 2026-10-29: free buttons for an optional talk (`master` at `27b9c71d7`)

The pad has 8 buttons (`C/controllers/MegaControllerButton.kt:7-15`): `UP`, `DOWN`, `LEFT`, `RIGHT`, `A` (jump), `B`
(shoot), `START`, `SELECT`. What each one does in a level:

- `B`: shoot and charge (`C/entities/megaman/components/ControllerComponent.kt`, `attack`). `B` with any direction
  shoots.
- `A`: jump. `DOWN` + `A`: slide (`GroundSlideBehavior.kt:53`) or drop through a platform (`JumpBehavior.kt:31`).
  `UP` + `A`: jetpack (`JetpackBehavior.kt:28`) and the air dash direction (`AirDashBehavior.kt:54`).
- `DOWN` alone on the ground: crouch after a delay (`CrouchBehavior.kt:21`). `DOWN` also enters a `PipePortal`
  (`PipePortal.kt:161`) and a `SpringBouncer` (`SpringBouncer.kt:214`), and moves down a ladder.
- `UP` alone: only near an object: a ladder (`ClimbBehavior.kt:116`), a `PipePortal` (`:163`), a `SpringBouncer`
  (`:213`). `UP` alone on open ground does nothing.
- `START`: pause on release (`MegaLevelScreen.kt:917`).
- `SELECT`: one of three actions by the setting `selectButtonAction` (`SelectButtonAction.kt:5`): next weapon on release
  (`ControllerComponent.kt:100`), air dash on press (`AirDashBehavior.kt:34-39`), or pause on press
  (`MegaLevelScreen.kt:918`).

Result: **no two-button combo is free in every setting.**

- Any combo with `SELECT` also does the `SELECT` action: a weapon change, an air dash or a pause.
- `START` + any button also pauses.
- `UP` + `A` and `DOWN` + `A` are taken; `B` + any button shoots; `UP` + `DOWN` and `LEFT` + `RIGHT` cannot be pressed
  together on a d-pad.

Options that are free and always present:

1. **`UP` (just pressed) while the player stands on the ground inside the zone.** `UP` on open ground does nothing
   today. Ladders, pipe portals and spring bouncers also use `UP`, so a talk zone must not overlap one of them; the
   level validator (`003`) can check this. Many games use a direction to interact (Cave Story uses Down). This is the
   recommended option.
2. **An entry in the pause menu** ("talk") that shows only while the player is in an optional zone. No button
   conflict, but slower to use and less visible.
3. **A new "talk" binding** after `070` (flexible controls). Not always present on an 8-button pad, so only as an
   extra to option 1.

## 2026-10-29: Tiled layer for talk zones (`master` at `8d564e4dc`)

The layer builders are in `C/screens/levels/tiled/layers/MegaMapLayerBuilders.kt:37-50`. The candidates:

- **`sensors` (existing, recommended).** `SensorsLayerBuilder` (`SensorsLayerBuilder.kt`) spawns every rectangle
  object once, at level load, as the entity class `entities/sensors/<name>`, with the rectangle as `ConstKeys.BOUNDS`.
  The builder spawns them directly, not through a camera spawner, so no spawner spawns them again; whether one is
  culled depends only on its own components (a `TalkZone` needs no cull component; not checked for the existing
  sensors). Today the package holds `Death`, `Gate`,
  `InstantDeath`, `InfernoDeath`, `PlayerDeath` and `FixtureTypeOverlapSpawn`: rectangles that act when the player is
  inside. A `TalkZone` is the same kind of object, and a talk zone needs exactly this: it exists for the whole level,
  keeps its state (the hint, "player inside"), and needs no new builder and no new layer key. The level designer sets
  the object name `TalkZone` and the properties (scene id, forced or optional).
- **`triggers` (existing, empty).** `ConstKeys.TRIGGERS` (`Constants.kt:447`) and `TriggersLayerBuilder`, whose
  `build` does nothing (`TriggersLayerBuilder.kt:9`). No TMX file has the layer. The name fits, but the builder must
  be written; it would do the same as `SensorsLayerBuilder`.
- **`specials` (existing).** `SpawnersLayerBuilder`: an entity like `EventTrigger`. Spawners are spawned when they come
  into the camera (or on a room or an event) and can be culled, so a zone could lose its state. A poorer fit.
- **A new layer (for example `dialogue`).** A new `ConstKeys` key and one line in `builderFactories` that reuses
  `SensorsLayerBuilder` (with the package of talk zones). The gain: a level designer can show or hide only the talk
  zones in Tiled, and the validator (`003`) finds them by layer. The cost: one more layer name for the same kind of
  object as the `sensors` layer.

Recommendation: the `sensors` layer, with the entity `entities/sensors/TalkZone`. Tiled can select the talk zones by
the object name. If the owner wants talk zones in their own layer, a `dialogue` layer that reuses
`SensorsLayerBuilder` is a one-line change in `builderFactories`.

## Agent report: Scripted control, player holds, locks and `isPlayer` (2026-11-02)

Note: The hostile half of this report belongs to `120`. `capturedBy` there is a plain value released through a `118` lease, because `Fight` is not an entity.

### Player "hold" design: scenes, locks, naming

Master at `b3e792bc4`. I only read and searched. I changed no file.

Short paths:
- `C` = `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game`
- `E` = `/home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine`

Line counts come from grep. They exclude imports and comment lines, unless I say otherwise.

**The main result:** the proposed `heldBy` is correct for a hostile takeover, but wrong for a scene. A scene needs "who drives the input". The hostile case needs "this slot is not on the players' side". These are two separate facts. The repo has a home for the first one already: `ControllerComponent.input`.

#### 1. Scripted control today

There is no scene system. `grep -il cutscene` finds nothing in `C`. No code walks a player by script. `MegaPlayers.kt:31` is the only production line that sets an `InputSource`. The "level-end walk" does not exist: the player stands and a beam plays. I also found no boss-intro walk.

| Case | Who takes control | Input | Damage | Movement | Camera | How it ends |
|---|---|---|---|---|---|---|
| Spawn beam | `PlayerSpawnEventHandler.kt:126-134`, all slots | `TURN_CONTROLLER_OFF` | `DAMAGE/SPAWN`, `PRE_PLAY` | `BEHAVIORS/SPAWN`, `gravityOn=false` | `cameraHold` when slot 0 is not in play (`MegaLevelScreen.kt:302-304`) | `:210-217`: unlock, `IN_PLAY`, `TURN_CONTROLLER_ON` |
| Death | `PlayerDeathEventHandler.kt:58-61`, slot 0 only | OFF | `POST_PLAY` | `gravityOn=false` | same | `PLAYER_DONE_DYIN` (`MegaLevelScreen.kt:604-619`), then spawn or game over. `:586` destroys the other slots. |
| Level end | `EndLevelEventHandler.kt:115-124`, `:171` | OFF | `DAMAGE/LEVEL_END` | `BEHAVIORS/LEVEL_END` | `POST_PLAY` | `END_LEVEL` (`MegaLevelScreen.kt:823-833`). No `unlock` exists. `LocksComponent.reset()` clears the locks at `engine.reset()`. |
| Room transition | `LevelRoomHandler.kt:54`, `setBehaviorsOn(false)` | OFF | none | `Megaman.kt:396` writes `body.setCenter`, `:400` turns gravity off | camera parts | `LevelRoomHandler.kt:135` ON, unless the room has an event |
| Gate | `MegaLevelScreen.kt:663-676`: World, Motion, Behaviors and Pathfinding off | not locked (known pause bug) | none | systems off | none | `GATE_INIT_CLOSING` (`:678-689`) |
| Boss intro | `:691-714`, `BossSpawnEventHandler`, `:743-745` | stays OFF from the transition | none | all systems off except Sprites and Animations | none | `END_BOSS_SPAWN` (`:750-754`) |
| Boss defeat | `:766-776` | OFF with the controller system left on | `DAMAGE/BOSS_DEFEATED` | none | none | `INTERMEDIATE_BOSS_DEAD` (`:798-804`) or `VICTORY_EVENT` |
| Wily end | `WilyFinalBoss.kt:304-310`, `DrWily` | OFF | `DAMAGE/WILY_FINAL_BOSS` | none | none | `GAME_COMPLETE` (`MegaLevelScreen.kt:835-839`) and `reset()` (`:1105`), or `PLAYER_DONE_DYIN` (`DrWily.kt:121`) |
| `PipePortal`, `PortalHopper`, `CapsuleTeleporter` | `beginTeleport` (`Teleports.kt:12`), `Megaman.kt:253-264`, `GameEntityUtils.kt:140-162` | no lock. Actuators test `teleporting` (`ControllerComponent.kt:73,186`). | `DAMAGE/TELEPORT` | velocity zero, collision off, gravity off, fixtures off, sprites hidden | smooth follow | `endTeleport` (`PipePortal.kt:228`, `PortalHopper.kt:252`, `CapsuleTeleporter.kt:258`) |
| Camera rotation | `Megaman.kt:154` setter | none | none | `MOVEMENT/CAMERA_ROTATION` | rotates | `Megaman.kt:425-428` |
| `SpiderWeb` | `SpiderWeb.kt:157-160` | stays human, mash to escape (`:191`) | none | `MOVEMENT`, `BEHAVIORS`, gravity off | none | `onDestroy` (`:125-128`). `PLAYER_JUST_DIED` destroys the web (`:138`). |
| Stat fill, pause | `PlayerStatsHandler.kt:140,175,319`, `LevelStateHandler.kt:13-27` | systems off | none | systems off | none | "all on" writes |

Two cases are not takeovers:
- `Cart` and `CartV2` take no control. `FeetCartRule` only sets an owner and a sense, and nothing outside the rule reads `TOUCHING_CART` or `ConstKeys.CART`.
- `GetWeaponScreen` moves its own `megamanSprite` (`:84`). Inference: no player entity is involved, because I did not read the file in full.

How the sites do it: a mix of 5 mechanisms.
- Global input lock: 13 `TURN_CONTROLLER_*` submits in 6 files (6 OFF, 7 ON). The last writer wins.
- Lock reasons per entity.
- `PlayPhase`.
- Direct `body.physics` writes.
- Absolute `system.on` writes.

#### 2. Does a scene player stop being a player?

No. Item `082` says that the scripted input "presses the same buttons as a person, so the player moves with the normal physics" (`082` `ITEM.md:82-83`). That rule needs every world reader to keep working.

| Group | Sites | Hostile wants | Scene wants | Same? |
|---|---|---|---|---|
| Gates | `GateRule.kt:15`, `MegaContactFilter.kt:112`, `Gate.kt:346` | no | yes | no |
| Carts, platforms | `MegaContactFilter.kt:116`, `FeetCartRule.kt:22`, `CartV2.kt:115`, `PushableBlock`, `FlipperPlatform` | no | yes | no |
| Items | `PlayerItemRule.kt:14` | no | yes (inference) | no |
| Targets | 591 `targetPlayer` lines in 142 files (imports not excluded), 37 live `players.any` lines | no | yes in a `PLAYER` freeze, where the world runs | no |
| Friend tests | `SessionRules.kt:11-12`, `Megaman.kt:435`, 38 `owner.asPlayer` lines | flip | must not flip | no |
| Boss landing gates | `ElecDevil.kt:339`, `GutsTank.kt:280,333`, `WilyFinalBoss.kt:448` | filter is useful | must count the player | no |
| Kill loops | `PlayerDeath.kt:46`, `GravityBlock.kt:214`, `CameraEdgeBlock.kt:42` | every slot | every slot | yes |
| Level flow, gravity | spawn, end, `WorldGravity.kt:14` | every slot | every slot | yes |
| `PLAYER_JUST_DIED` | `Megaman.kt:374`, `GauntletMaster.kt:168`, `SpiderWeb.kt:138`, `PreciousGem.kt:215`, `PreciousGemCluster.kt:109`, `MegaLevelScreen.kt:584` | not a player death | a normal player death | no |

Two groups agree and seven do not.

**Design errors in the field, stated plainly:**

1. **`heldBy` joins two facts.** "Who drives" and "which side" go in one value, and `isPlayer` reads that value. A scene hold then removes the player from gates, carts, items, targets and friend tests.

2. **`heldBy` does not take the input.** As specified, it does not touch `ControllerComponent.input`. `MegaPlayers.kt:31` and `ControllerSystem` still dispatch the slot frame. So it does not do what a scene needs, and it does what a scene must not do.

3. **Friendly fire during a scene.** A scene holds all players in co-op, and a shot from P1 is in flight. `source.rootPlayer` is null, so `isFriendlyFire` gives false (`SessionRules.kt:11`). `Megaman.kt:435` then returns true, so the shot hurts P2. Inference from reading the code.

4. **Vacuous `all`.** When all players are held, the filtered `players.all` is true. The `082b` boss talk runs before `BOSS_READY`, so these gates can pass before the player lands: `GutsTank.kt:280,333`, `ElecDevil.kt:339`, `WilyFinalBoss.kt:448`. Inference.

5. **Crash on a held hit.** `Megaman.kt:456` calls `this.asPlayer!!`. The first hit on a held entity throws. This happens in the hostile fight itself and in the `082a` room `death_during_scene`. `ReceiverRules.kt:109` has the same risk on 3 receivers (`SmallIceCube`, `SmallGrass`, `Togglee`).

6. **Faults that exist in the hostile case too:**
   - `Megaman.kt:435-436`: with `friendlyFire` off, P1 cannot damage a held P2. The test reads the damager's root and never looks at the target.
   - `Megaman.kt:435`: the `source != this` guard is skipped, because `rootPlayer` is null. A held P2 can be hit by its own shots if they touch it. Inference.
   - `BodyGravityChangeRule.kt:24` and `PortalHopper.kt:206`: the `else` path writes `entity.direction`. That setter can start a shared camera rotation (`Megaman.kt:145-154`).
   - `CameraEdgeBlock.kt:59`: a held entity passes through the view edge. This reader needs the raw slot.
   - `CeilingCrusher.kt:160` and `ToxicWaterfall.kt:137,151` test "player or `AbstractEnemy`". A held `Megaman` is neither.
   - `Players.kt:16` must read the raw slot. If it reads `isPlayer`, a second `register` passes and replaces the component.
   - `MegaLevelScreen.kt:584` tests slot 0. A held slot 0 that dies costs a life and resets the level.

7. **The holder can go stale.** The field does not clear in `reset()`, and holders are pooled. `030` `NOTES.md` warns about this risk for owners. If a holder forgets to release, `isPlayer` stays false for that slot. `destroyNow` always calls `onDestroy` (`GameEngine.kt:191-192`), so a release there is reliable.

8. **The name `hold` is already taken.** It collides with `held()` input masks, `cameraHold`, `heldLevelScreenKey`, and "held (stand)" in `082`.

**What is right in the proposal:**
- Read-through is correct. `collectContacts` runs before `processContacts` (`WorldSystem.kt:191-196`), and the rules read at process time. A per-frame recompute would be late.
- `PlayPhaseComponent.reset()` is also empty, so "does not clear in `reset()`" has a precedent.

#### 3. `LocksComponent`, `PlayPhase`, `inputLocked`

**Locks.** A lock says "this capability of this entity is off, for these reasons".
- 3 groups: `DAMAGE`, `MOVEMENT`, `BEHAVIORS`.
- 7 reasons, in a bit set per group (`LocksComponent.kt:15`).
- 10 `lock` lines, 7 `unlock` lines, 2 `clearLocks` lines, 3 `isLocked` lines, in 7 files.
- Readers: `Megaman.kt:89,94`, `SmallIceCube.kt:228`, and the `BEHAVIORS` hook.
- Only `Megaman` has the component (`Megaman.kt:268`).

| Reason | Groups |
|---|---|
| `SPAWN`, `LEVEL_END` | `DAMAGE`, `BEHAVIORS` |
| `TELEPORT`, `BOSS_DEFEATED`, `WILY_FINAL_BOSS` | `DAMAGE` |
| `CAMERA_ROTATION` | `MOVEMENT` |
| `SPIDER_WEB` | `MOVEMENT`, `BEHAVIORS` |

**Is a hold a lock?**
- A scene: yes, in part. Its damage and stand needs fit one new reason.
- A hostile takeover: no. It blocks nothing. The entity acts freely and takes damage.

**Can a hostile hold be a lock reason?** No, for four reasons:
- `LocksComponent.reset()` clears all bits at destroy, and `Megaman.kt:340,342` clear 2 groups at spawn. The hold must survive these.
- A bit names no holder.
- The engine `isPlayer` cannot read a game lock group.
- `lock` requires the component, and only `Megaman` has it.

**Bits or one reference?**
- Bits are better when reasons overlap.
- One reference is better for "who owns this slot".
- Two holders with one reason share a bit, so the first release frees both. `061` chose a set, and `SpiderWeb.kt:125` guards this with a list.
- One reference has no stack. If a scene writes `heldBy` over a fight holder and then releases, P2 becomes friendly in mid-fight. So keep them apart. A second capture of one slot must throw, as `Players.kt:15` does.

**One trap.** `LocksComponent.reset()` does not fire `onChange`, and `BehaviorsComponent.reset()` does not restore allowed behaviors. A cleared `BEHAVIORS` lock leaves behaviors off until the next `unlock` reaches zero. Today `PlayerSpawnEventHandler.kt:214` does that.

**`PlayPhase`.** One value per entity, 4 writer lines, 36 reader lines in 27 files.
- `!inPlay` stops behaviors, pause and camera follow, and removes the entity from `isLive`, so from `targetFor`.
- Neither a scene nor a hostile takeover should use it. It also makes the entity invincible (`Megaman.kt:89`).

**`inputLocked`.** It is `frameBuilders[0].locked`, one global flag.
- There is no slot and no reason. Any `TURN_CONTROLLER_ON` ends it.
- It blinds every slot (`MegamanMaverickGame.kt:691,700`). `isPauseRequested` reads it (`LevelPauseRules.kt:17`).
- A scripted source bypasses it. A stray ON cannot give control back to the human while a script source is on the entity.

#### 4. Design options

| Option | Scene | Hostile | Facts against |
|---|---|---|---|
| A. One `heldBy` | fails | works after the fixes above | Section 2: 7 of 9 groups disagree. |
| B. `controlledBy` plus role fields | works | works | `controlledBy` has no reader, so it fails `O1-R5`. It copies `ControllerComponent.input`, so two values can disagree. |
| C. Lock bits only | partly | fails | No input group. Input is a global frame flag. Section 3. |
| D. Scene entity, existing input source, lock reason, freeze reason, plus one narrow field for capture | works | works | Needs the fixes in section 2, error 6, before capture. |

**Recommend D.** The repo already chose this split. `031` `NOTES.md` lists "Driver, Slot binding, Side" as 3 concerns (`:921-931`), and `:993` lists "cutscene script" as an input source.

The owner's "parent entity" idea is correct, and it has precedents. `SpiderWeb` locks a player and releases in `onDestroy`. `DrWily` and `AbstractBoss` destroy themselves on a player event. `IParentEntity` already exists. The parent should own control, not change identity.

**Scene: who writes and who reads**

| Fact | Writer | Reader |
|---|---|---|
| Driver | scene entity, through `MegaPlayers` | `ControllerSystem`, `entity.input` (`EntityQueries.kt:37`). All input readers see the script with no change. |
| Damage | scene entity, `DAMAGE/SCENE` | `invincible` |
| World freeze | scene entity, `012` reason | systems |
| Side | not written | `isPlayer` stays true |

A scripted walk must not lock `BEHAVIORS` or `MOVEMENT`, because jump and run need them. A stand hold is an idle source that gives no presses (inference: a climb or other active behavior needs its own step).

**Multi-player.** The scene entity loops over every slot with `forEach`, and each slot gets its own script source. `082` already says this in its "All players" rule. Which slot advances the text stays open in `082`.

**Every exit route.** One `end(skipped)` function does all the release.

| Route | What happens |
|---|---|
| Normal end, skip | Steps finish or stop, then `end`, then `destroy()`. |
| Slot 0 death | `PLAYER_JUST_DIED` destroys the scene, as in `SpiderWeb.kt:138`. |
| Slot 1 death | Today this changes nothing until a reset. `047c` must set the rule. |
| Level exit, game over, game complete | `setCurrentScreen` calls `reset()` (`MegamanMaverickGame.kt:1016-1021`), then `engine.reset()`, then `onDestroy`. `showOverPause` does not reset, which is correct. |
| Pause | Updatables stop, so the scene stops. `LevelStateHandler.kt:26` turns all systems on at resume, which breaks a `WORLD` freeze until `012`. `pause_in_scene` needs `inputLocked` true or a new pause term. |
| Room change | `onBegin` sets OFF and destroys enemies. `onEnd` sets ON (`LevelRoomHandler.kt:54,72,135`). The scene type must not be `ENEMY`. The scene waits for `END_ROOM_TRANS`. |

**Note on `ControllerComponent`.** It has no `reset()`, so a script source survives a respawn unless the scene restores it. This is the same stale-reference risk as `heldBy`. Save the prior source, because a captured slot may have an AI source.

**New APIs.** None has a production caller today.

| API | First real caller | Status |
|---|---|---|
| `MegaLockReason.SCENE` | `082a` scene entity | no caller today |
| `MegaPlayers` set and restore input source | `082a` room `boss_walk` | no caller today |
| Scripted `InputSource` with its own `InputFrameBuilder` | `082a` | no caller today |
| `PlayerComponent.capturedBy`, `Players.capture`, `Players.release` | the hostile fight entity | no caller, and no item exists for it |
| `forEachSlot` | 17 lines, once a filter exists | real lines, no purpose before capture |

Inference: the scene track needs no engine change. `ControllerComponent.input` and `InputFrameBuilder` are public.

**Dependencies:**
- Scene track: `012`, then `082a`. This matches `082`.
- Capture track: a new item, because a grep of `docs/` finds no hostile takeover item. Later `047` rows are not reviewed. `030` is not a blocker, but it changes the friend tests.

**`forEach` default.** 17 of 20 lines change if `forEach` filters. The inverse is a raw `forEach` and a new filtered name, which changes 3 lines. But then `forEach` differs from `any` and `all`. This is a choice for the owner, not an error.

#### 5. Naming

**Readers of `isPlayer`:**
- Engine: 1 line (`Players.kt:16`).
- Core: 7 lines, 9 reads, 5 files (`PlayerExtensions.kt:9`, `GateRule.kt:15`, `MegaContactFilter.kt:112,116`, `FeetCartRule.kt:22,34`, `UnderwaterFan.kt:221`).
- Tests: 4 lines in 2 files.

**Through `asPlayer`:** 89 lines in 63 files.
- 38 owner lines (side).
- 10 `damageable` lines (reaction).
- 41 other lines (contact, device, body, input).

`rootPlayer` has 3 outside reads.

**What readers ask:** "Does the world treat this entity as one of the players now?" They do not ask who presses the buttons. No reader asks "is this a human?"

| Case | Readers need | `isPlayer` (with capture) | `isPlayable` | `isPlayerControlled` | `isPlayerSide` | `isActivePlayer` |
|---|---|---|---|---|---|---|
| Normal | true | true | true | true | true | true |
| PvP opponent | true | true | true | true | true | true |
| AI in a slot | true | true | unclear | false, wrong | true | true |
| Scene | true | true | false, wrong | false, wrong | true | true |
| Captured, human drives | false | false | unclear | true, wrong | false | true, wrong |
| Captured, AI drives | false | false | false | false | false | true, wrong |

- `isPlayable`: the docs already use "playable" in 3 other senses. Character (`O8`, `060`, `081`), phase (the owner in `082` `ITEM.md:181-182`: "before he becomes playable"), and "the game stays fully playable" (`O9`). It also reads like `inPlay`.
- `isActivePlayer`: reads like `isLive` or `PlayPhase`. A shot from a dead player still needs a true owner test.
- `isPlayerSide`: accurate, and it matches `030` `Faction`. But `030` has an open question on PvP teams.

**The answer changes after question 4.** With one hold, `isPlayable` looked close, because a scene flipped the value. After the split, a scene never flips it. Only capture does, and "P2 is not a player now" is true in plain English.

#### Direct answers

1. **Scenes:** no, not the same hold. A parent entity that manages players is a good idea, but it must take the driver, not the identity.
2. **`LocksComponent`:** it relates to scenes and it does not conflict. A scene uses one new lock reason. A hostile capture is not a lock. Keep the field and the bits apart, because they stack differently and clear at different times.
3. **`isPlayer`:** not outdated. Do not rename to `isPlayable`. Keep `isPlayer`, with a doc comment that says it is not a human test and that `playerSlot` is the raw slot. If the owner wants a rename, use `isPlayerSide` with `030`.

#### Recommendation

1. Do not build `heldBy` for scenes.
2. Build scenes under `082`: scene entity, input source swap, `DAMAGE/SCENE`, `012` reasons. Add nothing to `PlayerComponent`.
3. Build the hostile field later, in its own item, with the narrow name `capturedBy`. It means only "this slot is against the players".
4. Before capture is on, fix:
   - `Megaman.kt:435-436,456`
   - `ReceiverRules.kt:109`
   - `Players.kt:16`
   - `BodyGravityChangeRule.kt:24`
   - `PortalHopper.kt:206`
   - `CameraEdgeBlock.kt:59`
   - `IBodyExtensions.kt:22`
5. Do the release in one place. `MegaPlayers.capture` can add a `runnablesOnDestroy` entry to the holder (`MegaGameEntity.kt:23`), so no holder can forget.

#### Risks

- Read-through through `asPlayer` changes 89 lines at once. Only the 7 lines above are reviewed for capture. The 41 "other" lines need a line-by-line check.
- `targetFor` falls back to slot 0 if all slots are captured (`MegaPlayers.kt:39-40,79-82`, with a filtered `forEach`).
- A stray `TURN_CONTROLLER_ON` ends a frame lock during a scene until `012`. This affects pause blocking and the other slots.
- The holder's `EntityType` decides which sweeps destroy it (`LevelRoomHandler.kt:72`, `MegaLevelScreen.kt:544`). A capture can end at a room change with no fight result.
- A script with no mash leaves a player stuck in `SpiderWeb` (`:191`) or a freeze. The `082` limits must cover this.

#### Open questions for the owner

1. Who drives a captured slot: its human or an AI? The proposal does not say. Recommend naming this first, because it decides if capture needs an input swap.
2. Can slot 0 be captured? Recommend no for the first pass, because of `MegaLevelScreen.kt:584` and the camera focus on slot 0.
3. Does a captured death count as a player death? Recommend no: `Megaman.kt:374` keeps the event, and the 4 entity listeners check the side.
4. Do enemies and a captured slot fight each other? Today enemies hit it, and it cannot hurt them (`AbstractEnemy.kt:273`). Recommend deciding this in the new item.
5. What does a room change do to a scene: pause it or end it? Recommend pause until `END_ROOM_TRANS`, with the `082` time limit as the fallback.
6. Keep `isPlayer`, or `isPlayerSide` with `030`? Recommend keep now.
7. `forEach` raw or filtered? Recommend filtered only if capture is built, so that a new call site is safe by default.

#### Not read in full

- `Megaman.kt`, `MegaLevelScreen.kt`, `MegamanMaverickGame.kt`, `WilyFinalBoss.kt`: only the cited parts.
- `DrWily`, `Cart`, `CartV2`, `PortalHopper`, `PlayerStatsHandler`, `GetWeaponScreen`: parts or grep only.
- `NOTES.md` of `029`, `031`, `047`, `081`, `084`: parts or grep only. I read `012` and `082` notes in full, and the first part of the `030` notes.
- Pause-menu exit to the main menu: not traced. `screens/levels` has only 3 `setCurrentScreen` lines, all in `MegaLevelScreen`.
- I did not recheck which 17 of the 20 `forEach` lines mean "every slot". I confirmed the count of 20.
- I did not classify the 41 "other" `asPlayer` lines one by one.
- Nothing was run. All behavior claims come from reading the code.

##### Critical Files for Implementation

- `/home/johnlavender/IdeaProjects/Megaman-Maverick/engine/src/main/kotlin/com/maverick/game/engine/players/PlayerComponent.kt`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayers.kt`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/core/src/main/kotlin/com/megaman/maverick/game/locks/MegaLockReason.kt`
- `/home/johnlavender/IdeaProjects/Megaman-Maverick/docs/mega-refactor/items/082-dialogue-and-cutscenes/ITEM.md`
