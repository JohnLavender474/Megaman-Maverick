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
- **One box component** that both the level screen and the cutscene screen draw. It replaces the 3 hand-written
  typewriter copies (`RobotMasterIntroScreen`, `RobotMasterIntroScreenV2`, `GetWeaponScreen`) only if the owner
  agrees (see "Open questions"); else they stay as they are.

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
  - optional: a HUD hint shows while the player is inside, and a button starts the scene (X6 uses Select).
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

1. `082a`: the dialogue box, the string table and the scene runner, with one in-level talk zone in
   `RefactorTest082.tmx` and one real caller (to decide; for example the `DrWily` end scene).
2. `082b`: the boss pre-fight talk hook in `AbstractBoss`, with a first boss talk.
3. `082c`: `CutsceneScreen`, and `OpeningScreen`/`EndingScreen` moved onto it.

## Owner's input

2026-10-29 (verbatim): "Is there a `mega-refactor` item yet for dialogue boxes? In X games and some Mega Man games,
there are sections both within levels as well as outside levels in cutscenes where characters have dialogue exchanges
which appear as dialogue boxes. There needs to be an item which entails the following:
- logic and rules for building dialogue scenes inside of levels (pauses enemies and player behaviors/controller, etc.)
- logic and rules for building dialogue scenes outside of levels as animated cutscenes
Research how other popular open-source games (specifically X/Megaman games, but any other games too) handle this. I'm
sure every game will be different in many ways, so let's make the rules/logic flexible and tuned to be appropriate for
our game now."

## Open questions

1. Which scenes does the game need first? (A navigator, a boss pre-fight talk, the final Wily scene, the opening?)
   The first real caller decides the scope of `082a`.
2. Must `012` be done first, or can `082a` use the current `TURN_CONTROLLER_*` events and `system.on` writes, and move
   to `012` later? The current writers are "last writer wins", so a scene that overlaps a gate or a pause can leave
   systems in the wrong state.
3. In a `WORLD` freeze, do enemy animations stop (X6: frozen objects stay drawn, but do not animate) or keep running?
4. The optional talk zone button: Select is the weapon switch or the pause button today (`selectButtonAction`). Which
   button opens an optional talk? Or no optional talk zones at all?
5. The box style: portrait size, box size, font (`Megaman10Font` at 16 px or a new bitmap font), and the blip sound
   (no sound exists for it today).
6. Do `OpeningScreen`, `EndingScreen` and the 3 typewriter copies move onto the new box, or stay as they are?
7. Where do talk zones go in Tiled: the `triggers` layer (its builder does nothing today, and no TMX has the layer) or
   the `specials` layer as an entity (like `EventTrigger`)?
8. Can the player turn off optional dialogue (a setting, as the X5 navigator-mute ROM hack shows players want)?
9. Saved seen flags need `007` (save slots). Until then, are story seen flags per session only?

## Sources

- `NOTES.md` in this folder.
- `012` `NOTES.md` (a `CUTSCENE` freeze reason), `031` `NOTES.md` (a "cutscene script" input source), `029` `NOTES.md`
  (cutscenes act on all players).
- X6: `~/IdeaProjects/mmx-demake-tools/docs/engine.md`, sections "Text", "Talk zones", "Stop flags", "Boss sequence".
