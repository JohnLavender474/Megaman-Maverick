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

It does **not** duplicate TMX mechanics. **Load the `level-editor` skill** in the same task for:
- Object/layer/property syntax
- Spawn types (`spawn_now`, `spawn_room`, `spawn_event`)
- How to look up an entity's spawn properties via its `onSpawn` function

## Reference Levels

Before drafting, skim 1–2 shipped levels for structural patterns:
- `assets/tiled_maps/tmx/ReactorMan_v3.tmx` — linear path with alt path + two mini-boss rooms
- `assets/tiled_maps/tmx/InfernoMan_16x14_v2.tmx` — sequence of denser rooms

Specifically look at the `game_rooms` objectgroup to see how rooms are sized and chained.

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

## Room Composition Patterns

Pick rooms from this vocabulary; chain them along a path.

| Pattern           | Size (px)               | Purpose                                                                     |
|-------------------|-------------------------|-----------------------------------------------------------------------------|
| Intro room        | 512×448                 | Player spawn, no/low threat, teach the level theme                          |
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
1. Identify the level theme (e.g. fire, reactor, ice, jungle).
2. List the contents of the relevant folder(s) with `ls core/.../entities/enemies/` (or `hazards/`, 
   `items/`, etc.).
3. For any candidate entity, open the file briefly to confirm it fits the theme and to read its 
   `onSpawn` (via the `level-editor` skill's workflow) so you know what properties it accepts.
4. Only then recommend it to the user.

Reading the actual `bosses/` folder is essential before suggesting a mini-boss — there is a fixed 
roster and the user will not appreciate fabricated names.

### Available events (for `spawn_event`)

If a spawner uses `spawn_type=spawn_event`, the event names must match values in 
`core/.../events/EventType.kt`. Check that file before proposing event-triggered spawns.

## Drafting Workflow

1. **Confirm the concept.** Ask the user for: theme/boss name, approximate length (short ≈ 8 rooms, medium ≈ 12–15, long ≈ 18+), and any must-have mechanics.
2. **Sketch the room chain** as a graph before touching XML: `intro → corridor → vertical climb → encounter → mini-boss → corridor → alt path? → pre-boss → boss`. Show this to the user and get sign-off.
3. **Assign coordinates.** Place rooms head-to-tail in world space; remember rooms should touch (or overlap by 0) at their shared edges so camera transitions work.
4. **Place the player spawn** in the intro room and **one additional spawn point** near each mini-boss and the boss (these act as checkpoints on death). See the `player` layer in a reference TMX.
5. **Stub the spawners sparsely.** For a draft, place a handful of enemy/hazard markers — don't over-pack. Leave a comment or a TODO list of suggested additions; the user will iterate.
6. **Hand off to `level-editor`** for the actual TMX writing.

## Expanding an Existing Level

### Step 1 — Assess the level

Before suggesting or writing anything, build a mental model of the level:

1. **Read the `game_rooms` objectgroup** end-to-end. Note the room chain, sizes, and any alt paths
   or mini-boss rooms. Sketch the chain (mentally or in a scratch note) the same way step 2 of the
   drafting workflow does.
2. **Identify the theme** from the existing enemy roster (`enemies` layer), hazards, decorations,
   and tile sets. Read the boss room's referenced entity to confirm.
3. **Locate the seams** — places where new rooms could attach without breaking adjacency: dead-end
   alt paths, the start, the pre-boss stretch, gaps between mini-bosses.
4. **Note the difficulty arc** — which rooms feel easy, which feel hard, and where checkpoints fall.
5. **Find the next available object id.** The `<map>` element's `nextobjectid` attribute is the
   value Tiled will use; new objects must use unique ids ≥ this value (and `nextobjectid` should be
   bumped accordingly).

Briefly summarize this assessment back to the user before proposing changes — it both confirms
you understood the level and gives them a chance to correct you.

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

Let the user pick before going further. Each option should be one sentence, name the attach
point, and state what skill or reward it adds.

### Constraints when expanding

- **Match the existing theme.** Don't introduce ice enemies into a fire level.
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
- [ ] Enemy roster fits the theme (e.g. fire level → fire/heat enemies; reactor → mechanical/electrical)
- [ ] Hazards reinforce the theme (lava, spikes-with-steam, electrified floors, etc.)
- [ ] Decorations and backgrounds match
- [ ] Difficulty roughly escalates room-to-room; the hardest non-boss content is just before the boss
- [ ] Every room teaches or tests something — no filler corridors
- [ ] The boss arena is the simplest geometry in the level (no platforming distractions)

## Offering Ideas and Feedback

After producing a draft, **proactively offer**:
- **Enemy suggestions** — list 3–6 entities from `core/.../entities/enemies/` that fit the theme, with one-line rationale each. Note which would suit which room.
- **Composition feedback** — call out rooms that feel like filler, pacing dips, or missing skill checks.
- **X-style enhancements** — suggest 1–3 optional additions: a hidden alt path, a vertical detour, a wall-jump shortcut, an air-dash gap. Frame these as opt-in, not part of the default draft.
- **Cohesion gaps** — flag any enemy/hazard that doesn't fit the theme.

Keep suggestions concrete (name the room, name the entity, name the change). Vague ideas aren't actionable.

## Common Pitfalls

- **Over-packing the draft.** Templates should be skeletal; the user fleshes them out. Three enemies per room is usually plenty for a draft.
- **Drifting into X-style by default.** If you find yourself making most rooms larger than a screen, pull back — the default is classic.
- **Forgetting room adjacency.** Rooms that the player walks between must share an edge in world coordinates; gaps cause the camera to fail to transition.
- **Inventing entity names.** Always verify an enemy/hazard/block class exists under `core/.../entities/` before referencing it in a draft.
- **Skipping the chain sketch.** Going straight to XML produces incoherent levels. The numbered room list in step 2 is the most valuable artifact in this workflow.
