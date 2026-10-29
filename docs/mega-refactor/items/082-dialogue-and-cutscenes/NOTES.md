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
- **Constraint:** `game.controllerPoller` is `FrameControllerPoller { players.inputs[slot0] }`
  (`C/MegamanMaverickGame.kt:299`), and menus read it (`S/menus/MegaMenuScreen.kt:21,31`). While the controller is
  off, nothing can read buttons, so a dialogue box could not either. `031` must split gameplay input from UI input.
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
  The sensors are not culled and not spawned again by the camera. Today the package holds `Death`, `Gate`,
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
