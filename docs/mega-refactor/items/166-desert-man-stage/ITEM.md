# 166: Desert Man's stage rework (epic)

- Status: dropped (2026-11-17): merged into `078`
- Type: feature
- Objectives: `O4`, `O9`
- Depends on: none
- Related: `076`, `077`, `078`
- Branches: none yet

> **Dropped, 2026-11-17.** The owner merged this item into `078` (Stage redesigns, an epic for all stages).
> `078` `ITEM.md` holds the goal, the owner's words and the open questions now. The text below is kept as history.

## Goal

One item for all the rework of Desert Man's stage (`LevelDefinition.DESERT_MAN`, `DesertMan_16x14_v4.tmx`). It
replaces the Desert Man parts of other items, so that one item owns the live map and its branches do not edit the
same TMX file in parallel.

The parts:

1. **A larger oasis** (from `075`). After Megaman leaves the darkness cave, there is an "oasis" section with water and
   palm tree enemies. The section is very short, so the palm trees get little use. Make it much larger. It can include
   subtle callbacks to *Sonic and the Secret Rings* (Wii) and to the desert stages of *Super Mario Bros. 3* (NES): in
   the level design and mood only, with no copied levels or assets.
2. **A branch in the stage** (from the Desert Man part of `078`). A sandstorm starts, and Megaman has two options:
   (a) go into the darkness cave (it is already in the stage), or (b) go through the sandstorm in an outdoor section.
   The sandstorm is like the lava in *Mega Man X5*: it comes at intervals, and Megaman must stand behind blocks so
   that it does not push or kill him. The sandstorm section also has sand tornadoes.
3. **A hatch on `Jetto`** (new). The aeroplane enemy `Jetto` gets a hatch that is open while it drops bombs
   (`DeathBomb`). This changes `Jetto` in every level that uses it.
4. **Tiled automapping rules for `desert_man_tileset`** (from `165`). `desert_man_tileset` (from `076a`) chooses some
   tiles by position and by neighbor: the rock and soil fill use a 2x2 block by the cell's x/y parity, the edge and
   corner tiles also by parity, and the rope and fence ends by the tile next to them. A hand edit of the map must
   follow these rules, or a seam or a wrong edge shows. Automapping rules apply them while the owner paints. This
   part helps the map edits of parts 1 and 2.

This item changes gameplay on purpose. It is level content, not a refactor.

Not in this item: the Desert Man tileset and backgrounds (`076a`, `076b` of `076`, which covers every level), and the
Desert Man boss fight (`077b` of `077`, merged).

## Proposed scope

Not approved. The branch `SCOPE.md` of each branch decides its final scope.

- Oasis: more water areas and more palm tree encounters (use the `level-designer` and `level-builder` skills). A
  short list of callback ideas from the two games; the owner chooses.
- Branch: a split point before the darkness cave, a new outdoor sandstorm section next to it, and a join point. Two
  new hazards: the sandstorm (a wave at intervals that pushes or kills Megaman when no block is in front of him) and
  the sand tornado.
- `Jetto`: a hatch frame or animation, open while `dropBombDelay` runs and closed otherwise. New art in the `Jetto`
  atlas regions for each color.
- Automapping: rule maps and `rules.txt` for the rock, the soil, the ropes and the fence. Check: a rule run on the
  current map changes no cell.
- Probable branches, one per part. The automapping branch goes first if the map branches use it.

## Decisions

- 2026-11-17: the owner merged `075`, the Desert Man part of `078` and `165` into this item, as an epic for all of
  Desert Man's stage. A separate epic item can exist for each other level (or batch of levels). `075` and `165` are
  dropped with a pointer here; `078` keeps the alt paths of the other 7 robot master stages.
- 2026-11-17: the aeroplane enemy is `Jetto`.

## Owner's input

2026-10-29, for `075` (verbatim):

> In Desert Man's stage, after Mega Man leaves the darkness cave, there is an "oasis" section with water and enemy
> palm trees. However, this section is very short, and the palm trees don't have much time to shine. I think it would
> be cool to greatly expand this oasis section. (One of my favorite games as a kid was Sonic and the Secret Rings on
> the Wii; this section can include some subtle callbacks to that game as well as to the desert stages in Mario 3 on
> the NES.)

2026-10-29, for `078` (verbatim):

> Each of the robot-master levels is completely linear, which isn't inherently bad, but it'd be good to come up with
> ideas for cool alt paths: for example, Desert Man's stage can be set up so that a sand storm spawns and Mega Man
> has two options: enter the darkness cave (the one that already exists in the stage) or else prevail through the
> sandstorm in an outdoors section. (The sandstorm can be kind of like the lava in MMX5 where the player must stand
> behind blocks in order to avoid being pushed or killed by the hazard that appears at intervals)

2026-11-17, for `165`: the owner accepted the recommendation (decision D3 of `076a`): the remap script stays a
temporary file, and automapping is a backlog candidate.

2026-11-17 (verbatim):

> I want to widen its scope to include the following:
> - Adding a "hatch" to the aeroplane enemy which is open when it drops bombs
> - Adding branching paths to the stage, including the option to either (a) go into the darkness cave or (b) face a
>   sandstorm with sand tornadoes

> Let's merge 075 and 078 together, along with any other items that pertain to reworking Desert Man's stage. The item
> can be an "epic" that covers all of Desert Man's stage. A separate item may exist for each level (or batch of
> levels)

## Open questions

- Which enemy is the palm tree? No class has "palm" in its name. `Cokeyro` (3 objects in the live map) is the
  probable one. The owner confirms.
- Does the larger oasis come after the join point, so that both paths reach it, or is it on one path only?
- Does the sandstorm push Megaman, kill him, or do damage? The owner's text says "pushed or killed".
- What does a sand tornado do (carry Megaman up, push him, do damage), and does it move?
- Do the two paths join again before the boss, and does each path give a different reward (for example a heart tank
  or a health tank)?
- How does the stage show the player that two paths exist?
- Alt paths must not be dead ends (memory rule for level drafts).
- Does the `Jetto` hatch need new art from the owner, or a draft by Claude first (see `docs/ways-of-working/art.md`)?
- Which callbacks to the two games does the owner want (for example a mechanic, a layout or a background)?
- Should later tilesets of `076` (other levels) get the same kind of automapping rules?
- The open issue `docs/issues/2026-10-27-desert-man-enemies-on-wrong-layers.md` (5 enemies on layers where they
  cannot spawn) is in the same map. Fix it in the first map branch of this item?

## Sources

- `NOTES.md` in this folder.
- `items/075-desert-man-oasis/NOTES.md`, `items/078-robot-master-alt-paths/NOTES.md`,
  `items/165-desert-tileset-automapping/NOTES.md`.
- The automapping rules as code: `temp/076-desert-tiles/tools/make_desert_man_tileset.py` and its `README.md` in the
  main checkout (not in git).
- X6 decomp notes in `~/IdeaProjects/mmx-demake-tools/docs/` for ideas only (see `CLAUDE.md`, "Inspiration").
