---
name: level-creator
description: Use when creating new levels, level templates, or drafts from scratch — laying out rooms,
  sequencing the path, placing checkpoints, and proposing enemy/hazard ideas. Focuses on composition
  and creative direction. For the mechanics of writing TMX (layers, spawner properties, object ids),
  load the `level-editor` skill in tandem.
---

# Level Creator — Megaman Maverick

## Scope

This skill is for **drafting new levels or templates** and for **expanding existing levels**. It covers:
- Room composition and the path(s) through them
- Checkpoint (player spawn) placement
- Cohesion of theme, enemy roster, and hazards
- Generating suggestions and feedback on a draft

Drafts may be **themed** (e.g. fire level, reactor level) or **free-form / theme-less** — the latter
is common when the user wants a structural template they will later re-skin. Both modes are supported;
the "Theme Mode" section below explains how the workflow differs.

It does **not** duplicate TMX mechanics. **Load the `level-editor` skill** in the same task for:
- Object/layer/property syntax
- Spawn types (`spawn_now`, `spawn_room`, `spawn_event`)
- How to look up an entity's spawn properties via its `onSpawn` function

## Reference Levels

**Read all four of these files.** They are exemplary for how a playable level is actually constructed.

| File                                            | What to learn from it                                                                                                                                                                                                                                                                                                                                                                                               |
|-------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `assets/tiled_maps/tmx/Test8.tmx`               | **Smallest complete example.** Read end-to-end. Shows the minimum viable structure: tile layer with real wall/floor tiles (not all zeros), a `blocks` layer full of **unnamed rectangle objects** that form floors, walls, ceilings, and platforms, a `sensors` layer with a `Gate`, a `specials` layer with `Ladder` and `Water`, and a single `player` spawn. **This is what a draft must look like at minimum.** |
| `assets/tiled_maps/tmx/IntroStage_v3.tmx`       | A classic-style stage end-to-end. Reference for pacing, decoration density, and how rooms connect with vertical passages, ladders, and gates.                                                                                                                                                                                                                                                                       |
| `assets/tiled_maps/tmx/ReactorMan_v3.tmx`       | Linear path with an alt path and two mini-boss rooms. Reference for **how alt paths loop back** to the main route (they are *not* dead-ends — they share an exit edge with a downstream room or rejoin via a passage).                                                                                                                                                                                              |
| `assets/tiled_maps/tmx/InfernoMan_16x14_v2.tmx` | Sequence of denser rooms. Reference for how multiple hazards/enemies share a room without crowding.                                                                                                                                                                                                                                                                                                                 |

### What to learn from each of these example level

1. **Block geometry.** The `blocks` layer is dominated by **unnamed rectangle objects** — these are
   plain `Block` instances that act as walls, floors, ceilings, and platforms. A draft TMX must
   include floor/wall/ceiling rectangles for every room, not just spawner points. Without them the
   level has no geometry to stand on.
2. **Entity sizes.** Each named spawner object in the reference levels has a `width` and `height`
   that matches the entity's actual collider/sprite footprint (e.g. `SwitchGate` is wider than
   32×32; `Electrocutie` has a tall vertical extent for its beam). **Copy sizes from the reference
   levels** rather than defaulting every spawner to 32×32. If an entity isn't in any reference
   level, read its `onSpawn` (per the `level-editor` skill workflow) to find the expected size.
3. **Room connectivity.** Walk the rooms in the reference levels and confirm every room has both
   an entry edge and an exit edge (or loops back). An alt path that doesn't rejoin the main route
   needs an explicit "return" (a teleporter, a ladder back, or a shared edge with a downstream
   room). **Dead-end rooms are a bug, not a feature.**

Skipping this reference pass is the most common cause of unplayable drafts.

## Core Constants

- **1 tile = 32 px** (PPM)
- **1 screen = 16×14 tiles = 512×448 px**
- A "room" is a `<object>` in the `game_rooms` layer; the camera locks to it while the player is inside

These three numbers drive all room sizing decisions.

## Design Philosophy

Templates should **lean classic Mega Man**, with **Mega Man X accents** and some designs from other games:

**Classic (the default, ~70%)**
- Single-screen or short multi-screen rooms (512×448, 1024×448, 1536×448)
- Tight platforming and run-and-gun encounters
- Linear progression; one obvious way forward
- Hazards and enemies arranged to test a specific skill per room

**X-style accents (~25%)**
- Occasional vertical rooms (512×896, 512×1344) that require wall jumping
- Air-dash gaps that test the player's ability to navigate concurrent hazards/enemies
- An optional alt path or hidden nook with a reward (health, weapon energy, extra life)
- Some "open" rooms larger with verticality and multiple ways to approach and navigate

**Other classic-game accents (~5%)**
- Super Mario Bros.
- Super Mario World
- Donkey Kong Country
- Sonic

Push harder toward non-classic-Mega-Man accents only when the user explicitly asks for 
"experimental", "open", or "exploratory" level design.

## Theme Mode

Before drafting, decide (or ask the user) which mode applies:

**Themed mode** — the user names a theme, boss, or aesthetic ("fire level", "Reactor Man stage",
"jungle"). Enemies, hazards, decorations, music, and backgrounds should all reinforce that theme.
The Cohesion Checklist applies in full.

**Free-form / theme-less mode** — the user just wants a structural draft ("give me a medium-length
level template", "draft a level skeleton I can theme later"). In this mode, select enemies, hazards, 
and element based on their mechanics and impact rather than theme. Mixing of contradictory elements 
(like "fire" and "ice") is okay in this mode.

## Room Composition Patterns

Pick rooms from this vocabulary; chain them along a path.

| Pattern           | Size (px)               | Purpose                                                                     |
|-------------------|-------------------------|-----------------------------------------------------------------------------|
| Intro room        | 512×448                 | Player spawn, no/low threat, introduce the level (theme or core mechanic)   |
| Corridor          | (1024–2624) × 448       | Horizontal run-and-gun                                                      |
| Tight encounter   | 512×448                 | One focused enemy/hazard puzzle                                             |
| Vertical climb    | (512-1500) × (896–1472) | Wall jump / air dash up or down                                             |
| Hazard gauntlet   | 1024–1536 × 448         | Moving hazards, spikes, timing                                              |
| Alt path branch   | (512×448) (+rooms)      | Optional detour with reward                                                 |
| Mini-boss room    | 512×448                 | Single-screen, locks until boss dies                                        |
| Pre-boss breather | 512×448                 | Quiet room; place a checkpoint here                                         |
| Boss room         | 512×448                 | Locked arena with `fade_out_music` set                                      |
| Mixed             | variable                | Mix of horizontal, vertical climb, alt-path branching, and hazard gauntlets |

## Discovering What's Available

Before proposing rooms, enemies, or hazards, **build a vocabulary** of what actually exists in this 
codebase. Never invent class names from memory or from other Mega Man games — verify everything.

### Available layers

The set of supported object layers (and the unnamed-object defaults for each) lives in 
`core/.../screens/levels/tiled/layers/MegaMapLayerBuilders.kt`. The `level-editor` skill has a table 
summarizing the common ones (`game_rooms`, `player`, `enemies`, `blocks`, `items`, `hazards`, 
`specials`, `decorations`, `projectiles`, `backgrounds`, `foregrounds`, `sensors`), but **always 
consult `MegaMapLayerBuilders.kt` directly** when in doubt — new layers may have been added since this 
skill was last updated, and each builder may apply its own per-layer conventions.

### Available entities

Entity classes live under `core/.../entities/`, organized by category:

**Workflow when picking entities for a draft or proposal:**
1. Determine the selection criterion:
   - **Themed mode** → identify the theme (e.g. fire, reactor, ice, jungle) and pick entities that
     reinforce it.
   - **Free-form mode** → pick mechanically-distinct, theme-neutral entities (a walker, a flyer, a
     turret, a jumper, etc.) so the draft works as a skeleton for any future theme.
2. List the contents of the relevant folder(s) with `ls core/.../entities/enemies/` (or `hazards/`, 
   `items/`, etc.).
3. For any candidate entity, open the file briefly to confirm it fits the criterion and to read its 
   `onSpawn` (via the `level-editor` skill's workflow) so you know what properties it accepts.
4. Only then recommend it to the user.

Reading the actual `bosses/` folder is essential before suggesting a mini-boss — there is a fixed 
roster and the user will not appreciate fabricated names.

### Available events (for `spawn_event`)

If a spawner uses `spawn_type=spawn_event`, the event names must match values in 
`core/.../events/EventType.kt`. Check that file before proposing event-triggered spawns.

## Drafting Workflow

1. **Read the four reference TMX files** (see "Reference Levels — REQUIRED READING" above) before
   doing anything else. This step is non-negotiable for any draft.
2. **Confirm the concept.** Ask the user for: approximate length (short ≈ 8 rooms, medium ≈ 12–15, long ≈ 18+), any must-have mechanics, and **whether the draft is themed or free-form** (see "Theme Mode"). If themed, also ask for the theme/boss name. If the user only says "draft a level", default to free-form and confirm.
3. **Ask about tile layers.** Before any TMX is written, explicitly prompt the user:
   *"Do you want me to (a) include tile layers with content matching the block geometry, or (b) omit tile layers entirely so you can paint them manually in Tiled?"*
   - If (a): fill `tiles1` (and any other tile layers) with non-zero tile ids drawn from a reference level's tile data, matching every block rectangle.
   - If (b): omit the `<layer>` element(s) entirely from the TMX. Do not write an empty all-zero tile layer as a placeholder — the user will add tile layers themselves in Tiled.
   - The default if the user gives no answer is **(b) omit**.
4. **Sketch the room chain** as a graph before touching XML: `intro → corridor → vertical climb → encounter → mini-boss → corridor → alt path? → pre-boss → boss`. Show this to the user and get sign-off. For any alt path, state explicitly **how it rejoins the main route** (shared edge with a downstream room, teleporter back, or ladder back).
5. **Assign coordinates.** Place rooms head-to-tail in world space; remember rooms should touch (or overlap by 0) at their shared edges so camera transitions work.
6. **Place the player spawn** in the intro room and **one additional spawn point** near each mini-boss and the boss (these act as checkpoints on death). See the `player` layer in a reference TMX.
7. **Stub the spawners sparsely** — but with **correct sizes copied from a reference level** (not all 32×32). Place a handful of enemy/hazard markers per room.
8. **Add baseline block geometry.** Each room must have at least: a floor rectangle along the bottom edge, wall rectangles on the left/right edges, and any platforms the encounter requires. Use unnamed rectangle objects in the `blocks` layer (they become plain `Block` instances). Match the patterns in `Test8.tmx`.
9. **Hand off to `level-editor`** for the actual TMX writing.

## Expanding an Existing Level

### Step 1 — Assess the level

Before suggesting or writing anything, build a mental model of the level:

1. **Read the `game_rooms` objectgroup** end-to-end. Note the room chain, sizes, and any alt paths
   or mini-boss rooms. Sketch the chain (mentally or in a scratch note) the same way step 2 of the
   drafting workflow does.
2. **Identify the theme — or confirm it's free-form.** Scan the existing enemy roster
   (`enemies` layer), hazards, decorations, and tile sets. If they reinforce a coherent theme, note
   it (and read the boss room's referenced entity to confirm). If the roster is theme-neutral and
   decorations/backgrounds are empty, treat the level as free-form and preserve that.
3. **Locate the seams** — places where new rooms could attach without breaking adjacency: dead-end
   alt paths, the start, the pre-boss stretch, gaps between mini-bosses.
4. **Note the difficulty arc** — which rooms feel easy, which feel hard, and where checkpoints fall.
5. **Find the next available object id.** The `<map>` element's `nextobjectid` attribute is the
   value Tiled will use; new objects must use unique ids ≥ this value (and `nextobjectid` should be
   bumped accordingly).

### Step 2 — Branch on user intent

After assessment, pick one of two paths:

**(a) The user already has an idea** ("add a vertical climb after room5", "give it a hidden alt
path with an E-tank", "extend the pre-boss section by two rooms"). Translate that idea into
concrete proposals: which room(s) to add or change, where they attach in world coordinates, what
encounters fit, and how the difficulty curve and checkpoints shift. Show the proposal before
editing.

**(b) The user has no specific idea** ("expand this level", "make it longer", "add more to it").
**Do not invent a direction unilaterally.** Offer 2–4 distinct expansion options grounded in the
assessment, e.g.:
- "Add a vertical climb between room8 and room9 to introduce a wall-jump skill check."
- "Extend the alt path off room5 with a hidden room containing a heart tank."
- "Insert a second mini-boss in the corridor before the boss room to lengthen the back half."
- "Add a hazard gauntlet after room3 — the current jump from room3 to room4 is the difficulty
  dip in the arc."

Let the user pick before going further. Each option should be one sentence, name the attachment
point, and state what skill or reward it adds.

### Constraints when expanding

- **Preserve coordinate adjacency.** New rooms attach by sharing an edge with an existing room;
  if no seam exists, the user needs to approve relocating existing geometry.
- **Reuse the existing id-numbering scheme.** Bump `nextobjectid` rather than reusing freed ids.
- **Don't silently rebalance.** If your additions shift checkpoint spacing or difficulty pacing,
  call it out and propose checkpoint moves explicitly.
- **Hand off TMX edits to `level-editor`** once the plan is approved.

## Checkpoint Placement

The `player` layer holds one or more spawn points. The player respawns at the **most recently passed** one. Guidelines:
- Always one at the level start.
- One right before each mini-boss room.
- One right before the boss room (often the "pre-boss breather").
- For long levels, one extra between mini-bosses if the gap exceeds ~5 rooms.

Avoid checkpoints mid-gauntlet; the gauntlet should be retryable as a unit.

## Cohesion Checklist

Before declaring a draft done, verify:

**Always (themed or free-form):**
- [ ] Difficulty roughly escalates room-to-room; the hardest non-boss content is just before the boss
- [ ] Every room teaches or tests something — no filler corridors
- [ ] The boss arena (or `success` room) is the simplest geometry in the level (no platforming distractions)
- [ ] Every room has baseline block geometry (floor, walls, ceiling, required platforms)
- [ ] Every alt path rejoins the main route

**Themed mode only — skip in free-form mode:**
- [ ] Enemy roster fits the theme (e.g. fire level → fire/heat enemies; reactor → mechanical/electrical)
- [ ] Hazards reinforce the theme (lava, spikes-with-steam, electrified floors, etc.)
- [ ] Decorations and backgrounds match

**Free-form mode only:**
- [ ] Enemy roster is theme-neutral (no fire/ice/electric-flavored entities that would lock in a theme)
- [ ] Decorations and backgrounds are empty or minimal
- [ ] Boss spawner uses a placeholder name and this is called out to the user
- [ ] No `music` properties on rooms

## Offering Ideas and Feedback

After producing a draft, **proactively offer**:
- **Enemy suggestions** — list 3–6 entities from `core/.../entities/enemies/` with one-line rationale each, and note which would suit which room. In themed mode, justify by theme fit; in free-form mode, justify by mechanical role (pressure, zoning, vertical threat, etc.).
- **Composition feedback** — call out rooms that feel like filler, pacing dips, or missing skill checks.
- **X-style enhancements** — suggest 1–3 optional additions: a hidden alt path, a vertical detour, a wall-jump shortcut, an air-dash gap. Frame these as opt-in, not part of the default draft.
- **Cohesion gaps** — in themed mode, flag any enemy/hazard that doesn't fit the theme. In free-form mode, flag any entity that would silently lock the draft into a specific theme (e.g. a fire enemy in an otherwise neutral roster).
- **Theme suggestions (free-form mode only)** — propose 2–3 themes the structural draft would suit well, with a one-line rationale per theme (e.g. "The vertical climb in room5 and the hazard gauntlet would feel natural as a factory/conveyor stage"). Frame these as starting points for the user to choose from.

Keep suggestions concrete (name the room, name the entity, name the change). Vague ideas aren't actionable.

## Common Pitfalls

- **Skipping the reference reads.** This is the #1 source of broken drafts. The four reference TMX files exist precisely so you don't have to guess at block layout, entity sizes, or room connectivity. Read them.
- **Empty `blocks` layer.** A draft with only spawner points and no block rectangles has no floors, walls, or platforms — the player falls through the world. Every room needs baseline geometry (floor, walls, ceiling, platforms) as unnamed rectangles in `blocks`.
- **Default 32×32 spawner sizes.** Many entities (`SwitchGate`, `Electrocutie`, `LaserBeamer`, `Lift`, `ConveyorBelt`, etc.) require larger rectangles to match their collider/sprite. Copy sizes from the reference TMX files; if not present, read the entity's `onSpawn`.
- **Dead-end alt paths.** An alt path that doesn't loop back to the main route strands the player. Every alt path must rejoin via a shared edge with a downstream room, a teleporter, or a return ladder.
- **Writing a placeholder tile layer.** Don't include an empty all-zero `tiles1` "just in case." If the user opted to omit tile layers (the default), omit the `<layer>` element entirely — the user will add it manually in Tiled. Only include tile layers when the user explicitly opts in, and then fill them with real tile ids.
- **Over-packing the draft.** Templates should be skeletal; the user fleshes them out. Three enemies per room is usually plenty for a draft.
- **Drifting into X-style by default.** If you find yourself making most rooms larger than a screen, pull back — the default is classic.
- **Forgetting room adjacency.** Rooms that the player walks between must share an edge in world coordinates; gaps cause the camera to fail to transition.
- **Inventing entity names.** Always verify an enemy/hazard/block class exists under `core/.../entities/` before referencing it in a draft.
- **Skipping the chain sketch.** Going straight to XML produces incoherent levels. The numbered room list in step 2 is the most valuable artifact in this workflow.
