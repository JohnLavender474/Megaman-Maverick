# 078: Stage redesigns (epic)

- Status: idea
- Type: feature
- Objectives: `O4`, `O9`
- Depends on: none
- Related: `077`, `079`, `089`, `051`, `067`
- Branches: none yet

## Goal

One item for the redesign of every stage: the 8 robot master stages, the Wily stages and the other levels. All this
work changes the same TMX maps and loosely depends on each other, so one item owns it. A stage redesign can change the
layout, add sections and alt paths, change an enemy that the stage shows off, use camera sections, and give the stage
new tileset art and backgrounds.

The boss fights are not in this item. They stay in `077`. So the stages and the boss fights stay separate items.

Widened on 2026-11-17 (owner) from "Alt paths in the robot master stages". It took in these items, which are dropped
with a pointer here: `073`, `074`, `080`, `099`, `166` (with `075` and `165`, merged into `166` earlier the same day).
`076` (tileset art) also merged in; see "Tileset art and backgrounds" for how its active branch finishes.

This item changes gameplay on purpose. It is level content, not a refactor.

## Proposed scope

Not approved. The branch `SCOPE.md` of each branch decides its final scope.

The scope has one section for each stage, and some sections for work across stages. Each branch is `078<letter>`
and works on one stage or on one part of a stage. A stage with no section has no agreed work yet.

### All stages: alt paths

Each robot master stage is fully linear. That is not bad, but alt paths can make the stages better.

- A list of alt path ideas, one or more for each stage. The owner chooses which ones to make.
- Alt paths must not be dead ends (memory rule for level drafts).

### All stages: camera sections (from `099`)

Camera zones, scrolls and crush surfaces (`051`) exist, but only `RefactorTest051.tmx` uses them. Put them in real
stages, so that the variety that `051` adds reaches players.

- Choose the stages and sections with the owner. A stage section below can use them.

### All stages: tileset art and backgrounds (from `076`)

The game uses a mix of tilesets. Some are high-quality art from commissioned artists. Others come from free asset
packs: they are not bad, but they do not have the detail or the art style that the game needs. Give each level
tileset art of the same high quality, so that the game has one consistent look. This also covers the level
backgrounds where they do not match the new tiles.

- Sort the tilesets of the live levels: keep (high-quality art) or replace. The owner decides each tileset.
  `076` `NOTES.md` lists the tilesets of each live level.
- For each tileset to replace: choose or make new art, and paint the level tiles with it.
- Art-only work keeps the block objects as they are, so that the level plays the same, unless the scope says so.
- A tileset can be in more than one level (for example `Tileset1.tsx`).
- The final stage: its generic brick-like tileset needs new art.

### Desert Man (`DESERT_MAN`, `DesertMan_16x14_v4.tmx`) (from `166`)

Done in `076`: the new tileset (`076a`, merged). In progress in `076`: the new backgrounds (`076b`).

1. **A larger oasis** (from `075`). After Megaman leaves the darkness cave, there is an "oasis" section with water
   and palm tree enemies. The section is very short, so the palm trees get little use. Make it much larger, with more
   water areas and more palm tree encounters. It can include subtle callbacks to *Sonic and the Secret Rings* (Wii)
   and to the desert stages of *Super Mario Bros. 3* (NES): in the level design and mood only, with no copied levels
   or assets. A short list of callback ideas; the owner chooses.
2. **A branch in the stage.** A sandstorm starts, and Megaman has two options: (a) go into the darkness cave (it is
   already in the stage), or (b) go through the sandstorm in an outdoor section. The sandstorm is like the lava in
   *Mega Man X5*: it comes at intervals, and Megaman must stand behind blocks so that it does not push or kill him.
   The sandstorm section also has sand tornadoes. Two new hazards: the sandstorm and the sand tornado.
3. **A hatch on `Jetto`.** The aeroplane enemy `Jetto` gets a hatch that is open while it drops bombs (`DeathBomb`).
   New art in the `Jetto` atlas regions for each color. This changes `Jetto` in every level that uses it.
4. **Tiled automapping rules for `desert_man_tileset`** (from `165`). The tileset chooses some tiles by position and
   by neighbor: the rock and soil fill use a 2x2 block by the cell's x/y parity, the edge and corner tiles also by
   parity, and the rope and fence ends by the tile next to them. A hand edit of the map must follow these rules, or a
   seam or a wrong edge shows. Rule maps and `rules.txt` for the rock, the soil, the ropes and the fence. Check: a rule
   run on the current map changes no cell. It helps the map edits of parts 1 and 2, so it can go first.

### Moon Man (`MOON_MAN`, `MoonMan_16x14_v5.tmx`) (from `073` and `074`)

The owner calls it "Gravity Man's stage".

1. **More gravity-block puzzles** (from `073`). The stage has sections with `GravityBlock`s. One larger section has a
   simple puzzle: Megaman uses the gravity switcher to move a block out of his way. Add new areas that expand on the
   puzzle idea, with `GravityBlock` and `GravitySwitcharoo`. Each new puzzle adds one new idea on top of the existing
   one, for example more than one block, a block that must land on a switch or a hazard, or a timed gravity change.
2. **More `Starkner` sections** (from `074`). The stage introduces `Starkner` late, so the enemy gets little use. Add
   new sections with more `Starkner` encounters: introduce him earlier, in a safe room, and then use him in harder
   layouts, for example together with the gravity changes of the stage.

### Timber Woman (`TIMBER_WOMAN`, `TimberWoman_16x14_v4.tmx`) (from `080`)

1. **A larger `MockingBit` and `MockingByte` section.** The last section of the stage has these enemies. It is short
   and too simple, so the enemies and their behaviors get little use and give no special gameplay. Make it larger,
   and design each new room around one behavior of the enemies (for example the eggs from `MockingByteEgg` or the
   nest from `MockingByteNest`).

## Decisions

- 2026-11-17 (owner): `078` is the stage redesign epic for all stages. It took in `073`, `074`, `080`, `099`, `166`
  and `076`. The boss fights stay in `077`, so stages and boss fights stay separate items. The scope has one section
  per stage, and the branches are `078a`, `078b`, ..., one or more per stage.
- 2026-11-17 (owner): the aeroplane enemy of the Desert Man stage is `Jetto`.
- 2026-11-17 (Claude, for the owner to confirm): `076b` (Desert Man backgrounds, active) finishes under its own name
  and ID. When it merges, `076` is `done`. All later tileset and background work is a branch of `078`. The merged
  branch folders of `076` stay in `076` as history.

Decisions carried over from `076` (they apply to the tileset and background work):

- 2026-11-17 (owner): the Desert Man tileset is fully new and original art, based on the current Desert Man tilesets,
  drawn in phases (rock, steel, props, sand, oasis). Drafts: `temp/076-desert-tiles/` in the main checkout.
- 2026-11-17 (owner): keep the gap-0 vertical rock joins (a rock mass that stands on another keeps the sand lip
  between them).
- 2026-11-17 (owner): merge target of the tileset branches: each branch squash-merges directly into `master`.
- 2026-11-17 (owner): the Desert Man backgrounds: a simpler, Mega Man-like style; the round shapes are hills, not
  buildings; the sunset with heat haze only after the cave section.
- 2026-11-17 (owner, `076a` D3): the Desert Man remap script stays a temporary file, and automapping is a backlog
  candidate (now Desert Man part 4).

## Owner's input

2026-10-29, alt paths (verbatim):

> Each of the robot-master levels is completely linear, which isn't inherently bad, but it'd be good to come up with
> ideas for cool alt paths: for example, Desert Man's stage can be set up so that a sand storm spawns and Mega Man
> has two options: enter the darkness cave (the one that already exists in the stage) or else prevail through the
> sandstorm in an outdoors section. (The sandstorm can be kind of like the lava in MMX5 where the player must stand
> behind blocks in order to avoid being pushed or killed by the hazard that appears at intervals)

2026-10-29, Desert Man oasis, from `075` (verbatim):

> In Desert Man's stage, after Mega Man leaves the darkness cave, there is an "oasis" section with water and enemy
> palm trees. However, this section is very short, and the palm trees don't have much time to shine. I think it would
> be cool to greatly expand this oasis section. (One of my favorite games as a kid was Sonic and the Secret Rings on
> the Wii; this section can include some subtle callbacks to that game as well as to the desert stages in Mario 3 on
> the NES.)

2026-10-29, Moon Man puzzles, from `073` (verbatim):

> Gravity Man's stage has sections with GravityBlock instances in them. One larger section has a "puzzle" which has
> Mega Man use the gravity switcher in order to move a particular block out of his way. However, this "puzzle" is
> simplistic, and it seems like there should be new areas in the level that expand on the puzzle aspect.

2026-10-29, Moon Man `Starkner`, from `074` (verbatim):

> In the same Gravity Man stage, there is the Starkner enemy which is introduced late in the stage, but he doesn't
> get much use or time to shine. New sections should be created so that more Starkner interactions occur.

2026-10-29, Timber Woman, from `080` (verbatim):

> In Timber Woman's stage, the last section has MockingBit and MockingByte enemies, but alas the section is short and
> overly simplistic, so the enemy and its behaviors have no real time to shine and result in unique gameplay

2026-10-29, final stage tileset, from `076` (verbatim):

> The final level of the game uses a generic brick-like tileset that is lackluster for the final stage.

2026-11-17, tilesets, from `076` (verbatim):

> Right now, the game is a mishmash of tilesets, some of them high quality designed by commissioned/talented
> artists, while others are from free asset packs that while not bad aren't detailed or "artistic" enough for this
> game.

> Widen 076, similar to how 077 is widened to cover all bosses

2026-11-17, Desert Man (verbatim):

> I want to widen its scope to include the following:
> - Adding a "hatch" to the aeroplane enemy which is open when it drops bombs
> - Adding branching paths to the stage, including the option to either (a) go into the darkness cave or (b) face a
>   sandstorm with sand tornadoes

> Let's merge 075 and 078 together, along with any other items that pertain to reworking Desert Man's stage. The item
> can be an "epic" that covers all of Desert Man's stage. A separate item may exist for each level (or batch of
> levels)

2026-11-17, the epic (verbatim):

> It seems like we should consolidate into an "epic" since all this work loosely intersects. I think redoing boss
> fights should be 077 as it already is, while 078 becomes the stage redesign epic. This way, boss fights and stages
> remain separate items

2026-11-17: the owner also chose to merge `099` and `076`, to cover all stages, and one scope section per stage.

## Open questions

All stages:

- Do the paths of a stage join again before the boss, and does each path give a different reward (for example a
  heart tank or a health tank)?
- How does a stage show the player that two paths exist?
- Merge target of the map branches: directly into `master`, or into a parent branch `mega-refactor-078-master`?
  The tileset branches go directly into `master` (decision above).
- Order of work in a stage: the art first, or the layout first? New layout needs new tiles painted, and new tiles on
  an old layout can need a repaint later.

Tileset art:

- Which tilesets are high quality (keep), and which need new art (replace)?
- Is "the final level" `WILY_STAGE_3` (it holds `WilyFinalBoss`)? The owner confirms.
- Where does the new art come from: commissioned art, new art made in the repo, or an existing tileset in
  `assets/tiled_maps/tsx/`?
- Does this wait for `067` (PPM 16 with 1x pixel art)? New art made at 2x before `067` can need a second change.
- Should later tilesets get the same kind of automapping rules as Desert Man part 4?

Desert Man:

- Which enemy is the palm tree? No class has "palm" in its name. `Cokeyro` (3 objects in the live map) is the
  probable one. The owner confirms.
- Does the larger oasis come after the join point, so that both paths reach it, or is it on one path only?
- Does the sandstorm push Megaman, kill him, or do damage? The owner's text says "pushed or killed".
- What does a sand tornado do (carry Megaman up, push him, do damage), and does it move?
- Does the `Jetto` hatch need new art from the owner, or a draft by Claude first (see `docs/ways-of-working/art.md`)?
- Which callbacks to the two games does the owner want (for example a mechanic, a layout or a background)?
- The open issue `docs/issues/2026-10-27-desert-man-enemies-on-wrong-layers.md` (5 enemies on layers where they
  cannot spawn) is in the same map. Fix it in the first Desert Man map branch?

Moon Man:

- Where do the new puzzle areas go: before the existing puzzle, after it, or both? On the main path or on an alt path?
- Do the puzzles need new block types or new switch behavior, or only new layouts?
- Do the new `Starkner` sections come before or after the smarter `Starkner` behavior of `079`? A section designed
  for the current behavior can play differently after `079`.

Timber Woman:

- Do `MockingBit` and `MockingByte` also need new behaviors (`079`), or only better layouts?

## Sources

- `NOTES.md` in this folder.
- The research of the merged items stays in their folders: `073`, `074`, `075`, `076`, `080`, `099`, `165`, `166`
  `NOTES.md`. `076` `NOTES.md` lists the tilesets of each live level.
- The Desert Man automapping rules as code: `temp/076-desert-tiles/tools/make_desert_man_tileset.py` and its
  `README.md` in the main checkout (not in git).
- X6 decomp notes in `~/IdeaProjects/mmx-demake-tools/docs/` for ideas only (see `CLAUDE.md`, "Inspiration").
