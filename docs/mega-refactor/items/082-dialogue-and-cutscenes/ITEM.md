# 082: Dialogue boxes, in-level dialogue scenes and cutscenes

- Status: idea
- Type: feature
- Depends on: `012`, `031`
- Related: `007`, `013`, `015`, `026`, `029`, `038`, `047`, `050`, `051`, `057`, `060`, `081`
- Branches: none yet

## Goal

Add dialogue to the game, as in the Mega Man X games and some classic Mega Man games: characters talk in dialogue
boxes, with a portrait and a name. Dialogue occurs in two places:

1. **Inside a level:** a dialogue scene that a level trigger, a boss or an entity starts. The scene freezes the
   gameplay that it must freeze (the player controller, enemies, physics), runs its steps, and then gives control back.
2. **Outside a level:** an animated cutscene (opening, ending, story between stages) that shows actors, backgrounds,
   music and the same dialogue boxes.

The two uses share one dialogue box and one scene runtime. The rules are flexible enough for later content, but they
are set for this game now: 8-bit style, one player, the current screens and events. Facts and research are in
`NOTES.md`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

### Rules for the dialogue box

- **Text is data.** The lines are in a string table with stable keys (for example `assets/dialogue/en.txt`), not in
  Kotlin code. A scene refers to keys. This is the start of localization; one language now.
- **Inline tokens** in a line: pause (`{wait=0.5}`), speed (`{speed=2}`), color, a line break, and an event hook
  (`{event=name}`) that lets the scene act in the middle of a line. Unknown tokens are a load error.
- **Pages:** the box splits a line into pages by pixel width and a maximum line count. At the end of a page it shows a
  blinking arrow and waits for a press. A token can make a page advance by itself (for timed lines in a cutscene).
- **Typewriter:** a fixed number of characters per tick, a blip sound on each typed character (not on spaces).
- **Buttons:**
  - A press while the page types prints the rest of the page.
  - A press at the end of a page goes to the next page. A guard stops one press from doing both (as in X6).
  - Holding the button types faster (Cave Story and Megamix do this).
  - Start skips the whole scene if the scene is skippable (see "Skip").
- **Layout:** the box is at the top or the bottom of the UI viewport, set per line. A portrait (with talk frames while
  the text types) and a speaker name are optional, set per line.
- **Text style, per line:** a boxed line (the dialogue box) or a caption (no box, centred text, as in
  `OpeningScreen`). **Reveal and exit, per line:** typewriter, all at once, or a fade in; close at once or fade out.
  An auto-advance time is optional. These options let `OpeningScreen` (fade in, show, fade out, no input but Start)
  and a dialogue exchange (typewriter, wait for a press) use the same box.
- **One box component** that both the level screen and the cutscene screen draw. By the end of the item, every
  existing screen that shows sequenced or typed text uses it (decision 4).

### Rules for a scene

- **A scene is an ordered list of steps.** Each step starts, updates each tick and says when it is done. Examples:
  `say` (open the box with lines, done when the box closes), `wait` (time), `waitUntil` (a condition, for example the
  player is on the ground), `walkTo` (move an actor to a marker), `face`, `animate`, `camera` (a target or a pan, from
  `051`), `music` (play, stop, fade, restore), `sound`, `fade`, `event` (submit a game event), `spawn`, `setFlag`.
  `parallel` runs several steps and is done when all are done.
- **Scenes are built in Kotlin first** with a small builder. A data file format (JSON or YAML, as in `050`) is a later
  step, when a real need exists. Branching is a plain `if` in a step, not a language.
- **Freeze level, per scene** (the Cave Story `<KEY>`/`<PRI>` split, and the X6 talk zone):

  | Level | Player input | Player behaviors | Enemies, physics, projectiles | Use |
  |---|---|---|---|---|
  | `NONE` | runs | runs | run | chatter that plays while you play (a passive box) |
  | `PLAYER` | locked | held (stand) | run | X6 boss pre-fight talk, a talk where the world keeps moving |
  | `WORLD` | locked | held | frozen; they stay drawn | X6 navigator talk zone, most in-level scenes |

  Each level is one named freeze reason in `012` (`DIALOGUE_PLAYER`, `DIALOGUE_WORLD`). An entity that must run
  during a `WORLD` freeze (the speaker, an actor of the scene) is exempt, as with the X6 `0x08` flag.
- **Input:** the box reads its own buttons while gameplay input is locked. Today the lock also blinds
  `game.controllerPoller`; `031` must split "gameplay input locked" from "UI input" before this item.
- **Actors:** a scene moves the real player through a scripted input source (`031` lists "cutscene script" as a source)
  or a held state. Other actors are normal entities, spawned by the scene or found by map object id. Move targets are
  named markers in Tiled.
- **A scripted move can never leave the player stuck** (decision 1). Rules:
  - The level designer gives the route as named markers and actions (run to `a`, jump at `a`, run to `b`), not as a
    target that a pathfinder must solve. The scripted input presses the same buttons as a person, so the player
    moves with the normal physics.
  - During the move the player cannot take damage, and the world is frozen (`WORLD`) unless the scene says otherwise,
    so no enemy or projectile can push the player off the route.
  - Each move step has a time limit from the route length and a "no progress" check (the distance to the next marker
    does not get smaller for a set number of ticks).
  - If the limit or the check fails, the step puts the player on the marker, logs an error, and the scene goes on.
    The scene never waits forever.
  - A test checks each scripted route of the test map: from the start, the player gets to the end marker within the
    limit with no fallback.
- **All players:** a scene locks and releases every player through `game.players` (`029`, and `047` later).
- **Skip:** a scene is skippable or not. A skip stops the steps and runs the scene's `onEnd(skipped = true)`, which
  must put every actor, flag, music and the camera into the end state (the Celeste rule). A boss pre-fight talk is
  skippable; its end state is "boss ready, health fill starts".
- **End and interrupt:** a scene always releases its freeze reasons and locks in `onEnd`, also when the player dies or
  the level resets during the scene (`038`). Today `WilyFinalBoss` turns the controller off and nothing turns it on
  again on that path.
- **Play once:** a scene can have a seen flag. A seen scene does not play again. The flag scope is per scene: per level
  attempt (X6 boss talk: first time only, a retry gets a short wait), or saved (story scenes; needs `007`).
- **Per character:** a scene can have lines for each player character (X6 has a text bank per X and Zero). Needed
  when `060` or `081` adds a second character.

### In-level triggers

- **Talk zone:** a Tiled object (in a layer to decide, see "Open questions") with a scene id and a mode:
  - forced: when the player is inside and on the ground, the scene starts;
  - optional: a HUD hint shows while the player is inside, and a button starts the scene (X6 uses Select). The
    button is open question 4.
- **Boss pre-fight talk:** an optional scene between `ENTER_BOSS_ROOM` and the health fill. The boss waits until the
  scene ends before `BOSS_READY`. The boss music starts after the scene, as in X6.
- **Events:** any `EventType` (and later the typed channels of `057`) can start a scene, for example a room entry.
- **Entity code:** an entity can start a scene (for example `DrWily` after the final boss).

### Out-of-level cutscenes

- A `CutsceneScreen` runs one scene with the same runtime: backgrounds, sprite actors, the dialogue box, music and
  fades. A cutscene is a scene with no level and no world freeze.
- `AbstractStoryScreen` (`OpeningScreen`, `EndingScreen`) becomes a cutscene, if the owner agrees. Its slides become
  `say` steps with auto-advance, so the current look can stay the same.
- The screen after a cutscene comes from the scene end (like `screenOnCompletion` in `LevelDefinition`).

### Not in the first version

- Choices and branching menus in the box.
- Voice lines.
- More than one language.
- A visual editor.

### Proposed branches

`082a` starts after `012` (decision 2) and after the `031` change that lets UI code read input while gameplay input is
locked.

1. `082a`: the dialogue box, the string table and the scene runner. Two first callers (decision 1):
   - **`OpeningScreen`** moves onto the runner, with caption lines that fade in and out. It must look and time the
     same as now; Start still skips it.
   - **`RefactorTest082.tmx`**, one room for each case:
     - `forced_intro`: radio talk after Megaman spawns, before he is playable (forced, `WORLD`).
     - `optional_hint`: an optional talk zone with the HUD hint, as the X6 talk zones (`mmx-demake-tools` `docs/engine.md`, "Talk zones").
     - `boss_walk`: a test boss spawns; then Megaman runs, jumps over a block and stops on a marker (a scripted move
       with the no-stuck rules); then the boss talks (`PLAYER` freeze, the boss animates).
     - `passive_chatter`: a `NONE` box that plays while the player moves and shoots.
     - `death_during_scene`: a hazard kills the player during a `PLAYER` scene; after the respawn every lock and
       freeze is released and the scene does not leave the player stuck.
     - `skip`: a skippable scene; Start skips it, and the end state (actor positions, flags, music) is the same as
       when it plays to the end.
     - `play_once`: walking back into a seen forced zone does not play it again.
     - `mid_line_event`: an actor moves while a line types (the `{event=name}` token).
     - `pause_in_scene`: Start during a scene that cannot be skipped does nothing; pause works again after the scene.
2. `082b`: the boss pre-fight talk hook in `AbstractBoss`, with a first real boss talk.
3. `082c`: `CutsceneScreen`; `EndingScreen` moved onto it.
4. `082d`: the other screens with sequenced or typed text moved onto the box: `RobotMasterIntroScreen`,
   `GetWeaponScreen`, and `CreditsScreen` if it fits. `RobotMasterIntroScreenV2` is not registered; it is deleted or
   moved, as the owner decides.

## Owner's input

2026-10-29 (verbatim): "Is there a `mega-refactor` item yet for dialogue boxes? In X games and some Mega Man games,
there are sections both within levels as well as outside levels in cutscenes where characters have dialogue exchanges
which appear as dialogue boxes. There needs to be an item which entails the following:
- logic and rules for building dialogue scenes inside of levels (pauses enemies and player behaviors/controller, etc.)
- logic and rules for building dialogue scenes outside of levels as animated cutscenes
Research how other popular open-source games (specifically X/Megaman games, but any other games too) handle this. I'm
sure every game will be different in many ways, so let's make the rules/logic flexible and tuned to be appropriate for
our game now."

2026-10-29, answers to the first open questions (verbatim):

- Q1 (first scene): "I figure we should do two things at once as a first pass to prove the setup:
  - Refactor the existing opening screen to use it. The screen is super simplistic; it just shows a series of text
    that fades in and out. Most text won't fade in/out in dialogue exchanges, but having the setup be flexible enough
    to handle text fade ins/outs is preferable if possible. This proves one of the simplest setups is possible.
  - Create a new test map which has a series of different dialogue events:
    - a "forced" one at the beginning (e.g. radio chatter to Mega Man after his spawns in and before he becomes
      playable)
    - optional dialogue (e.g. the optional alerts that exist in X6; see @~/IdeaProjects/mmx-x6/)
    - dialogue scene that occurs after a "boss" or other entity spawns in which entails having Mega Man run and jump
      over a block to get to a predetermined spot; it must be guaranteed that Mega Man does not become stuck while
      being automated to move to the spot
    - any other cases you can think of"
- Q2 (`012` first): "Ideally, 012 will come first"
- Q3 (optional talk button): "Probably a button combo that is not already taken and is always present. Do you see any
  viable combos available?"
- Q4 (port the screens): "Ideally, yes. We can start with just the opening screen one, but ideally by the end of the
  item, all pre-existing screens that need it have been ported over"

## Decisions

1. 2026-10-29: `082a` has two first callers: `OpeningScreen` on the new runner (caption lines with fade in and out),
   and the test map `RefactorTest082.tmx` with one room per dialogue case (see "Proposed branches"). A scripted move
   must never leave the player stuck (see "Rules for a scene").
2. 2026-10-29: `012` comes before `082a`. `082` keeps `012` in "Depends on".
3. 2026-10-29: the optional talk button is a button combo that is free and always present. Not chosen yet; see open
   question 4.
4. 2026-10-29: by the end of the item, every existing screen that needs the box uses it. `082a` starts with
   `OpeningScreen`.

## Open questions

1. In a `WORLD` freeze, do enemy animations stop (X6: frozen objects stay drawn, but do not animate) or keep running?
2. The box style: portrait size, box size, font (`Megaman10Font` at 16 px or a new bitmap font), and the blip sound
   (no sound exists for it today).
3. Where do talk zones go in Tiled: the `triggers` layer (its builder does nothing today, and no TMX has the layer) or
   the `specials` layer as an entity (like `EventTrigger`)?
4. The optional talk button. The scan of 2026-10-29 (`NOTES.md`, "Free buttons for an optional talk") found no free
   two-button combo on the 8-button pad. The candidates are: Up while standing in the zone (contextual, as ladders and
   `PipePortal` use Up), an entry in the pause menu, or a "talk" binding after `070`.
5. Can the player turn off optional dialogue (a setting, as the X5 navigator-mute ROM hack shows players want)?
6. Saved seen flags need `007` (save slots). Until then, are story seen flags per session only?
7. `RobotMasterIntroScreenV2` is not registered. Delete it, or port it with the others?

## Sources

- `NOTES.md` in this folder.
- `012` `NOTES.md` (a `CUTSCENE` freeze reason), `031` `NOTES.md` (a "cutscene script" input source), `029` `NOTES.md`
  (cutscenes act on all players).
- X6: `~/IdeaProjects/mmx-demake-tools/docs/engine.md`, sections "Text", "Talk zones", "Stop flags", "Boss sequence".
