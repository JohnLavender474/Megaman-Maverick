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

It does **not** duplicate TMX mechanics. **Load the `level-editor` skill** in the same task for things like:
- Object/layer/property syntax
- Spawn types (`spawn_now`, `spawn_room`, `spawn_event`)
- How to look up an entity's spawn properties via its `onSpawn` function

For any **read-side** work this skill does — assessing an existing level, listing rooms and
counts, looking up canonical entity sizes across non-draft TMXs for the flesh-out phase — **load
the `tmx-analyzer` skill** as well. It owns the `utils/tmx-analyzer/run.sh` script that returns
a compact JSON of the level (rooms, per-room entities, `entity_details`, `entity_classes`); the
"Step 1 — Assess the level", "Cross-File Lookups", and "Two-Step Entity Placement Pattern"
sections below all rely on it.

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

Drafting is split into a short **planning** stage (no TMX yet) followed by a **phased TMX output** stage
where each phase produces its own file and pauses for user review. See the "Phased Creation Workflow" 
section below for the file-by-file mechanics.

### Planning (no TMX written yet)

1. **Read the four reference TMX files** before doing anything else.
2. **Confirm the concept.** Ask the user for: approximate length (short ≈ 8 rooms, medium ≈ 12–15, long ≈ 18+), any must-have mechanics, and **whether the draft is themed or free-form** (see "Theme Mode"). If themed, also ask for the theme/boss name. If the user only says "draft a level", default to free-form and confirm.
3. **Ask about tile layers.** Prompt the user:
   *"Do you want me to (a) include tile layers with content matching the block geometry, or (b) omit tile layers entirely so you can paint them manually in Tiled?"*
   - If (a): the tile art is produced as its own later phase (see Phased Creation Workflow), filling `tiles1` (and any other tile layers) with non-zero tile ids drawn from a reference level's tile data, matching every block rectangle.
   - If (b): omit the `<layer>` element(s) entirely from every phase's TMX. Do not write an empty all-zero tile layer as a placeholder — the user will add tile layers themselves in Tiled.
   - The default if the user gives no answer is **(b) omit**.
4. **Sketch the room chain** as a graph before touching XML: `intro → corridor → vertical climb → encounter → mini-boss → corridor → alt path? → pre-boss → boss`. Show this to the user and get sign-off. For any alt path, state explicitly **how it rejoins the main route** (shared edge with a downstream room, teleporter back, or ladder back).
5. **Assign coordinates.** Place rooms head-to-tail in world space; rooms should touch (or overlap by 0) at their shared edges so camera transitions work.

### TMX output

Once the planning stage is signed off, switch to the **Phased Creation Workflow** below.

Hand off the mechanical TMX writing inside each phase to the `level-editor` skill.

## Phased Creation Workflow

A new level is built up across a sequence of phases, each producing its own TMX file. The user
reviews after every phase and can ask for changes before the next phase begins.

### File naming

Each phase writes a new file in `assets/tiled_maps/tmx/` named:

```
draft_<level_name>_<phase_description>_<N>.tmx
```

- `draft_` — **always present**. Every level file produced by this skill is an unfinished draft and
  must carry this prefix. The user (not the skill) is responsible for renaming/removing the prefix
  once the level is finalized. The prefix also tells future cross-file searches to **exclude these
  files** (see "Cross-File Lookups" below) — drafts often contain placeholder markers that
  misrepresent how an entity is really configured.
- `<level_name>` — kebab- or snake-case version of the level concept (e.g. `my_new_level`,
  `factory_stage`). Pick this once at the start of phase 1 and keep it stable across all phases.
- `<phase_description>` — short snake_case label for what this phase added (e.g. `scaffold`,
  `enemy_markers`, `enemies_sized`, `hazards`, `decorations`, `tiles`). Match the phase's purpose;
  freeform within the spirit of the default phase list.
- `<N>` — sequential phase number starting at `1`. Always increments; never reused, never skipped.

Examples: `draft_factory_stage_scaffold_1.tmx`, `draft_factory_stage_specials_2.tmx`,
`draft_factory_stage_enemy_markers_3.tmx`, `draft_factory_stage_enemies_sized_4.tmx`.

It is expected that **many files will accumulate** by the end. They serve as a visible history of the 
draft and let the user roll back to any prior phase. Do not delete prior phase files when producing a new one.

### Cross-File Lookups

When this skill (or the `level-editor` skill, during a phase handoff) needs to **look up how an
entity is configured in other TMX files** — to find the right size, the right property set, a
canonical placement pattern — restrict the search to **non-draft files only**. Skip any TMX whose
filename begins with `draft_`. Drafts may contain 1×1 placeholder markers (see "Two-Step Entity
Placement" below) and other unfinished structures that would mislead the lookup.

Concretely, prefer search commands that exclude drafts:

```bash
grep -l 'name="SniperJoe"' assets/tiled_maps/tmx/*.tmx | grep -v '/draft_'
# or
ls assets/tiled_maps/tmx/ | grep -v '^draft_' | xargs -I{} grep -l ...
```

The reference levels listed in "Reference Levels" above are all non-draft and are the highest-trust
sources for these lookups.

### How each phase works

For phase `N`:

1. **Identify the source file.** Phase 1 starts from a blank `<map>` skeleton (matching the
   structure of `Test8.tmx`). Phase 2+ starts from phase `N-1`'s file by copying it.
2. **Copy, don't edit in place.** Always `cp` the previous phase's TMX to the new phase's filename
   first; apply all edits to the copy. The previous phase's file must remain untouched.
3. **Apply only this phase's additions.** Stay scoped — don't sneak content from a later phase in.
   If you spot something that needs fixing in an earlier phase's scope (e.g. a missing wall), call
   it out to the user rather than silently patching across phases.
4. **Announce the new file path and a one-paragraph summary** of what was added. Keep it short —
   the user is about to open the file in Tiled.
5. **Stop and wait for review.** Do not begin the next phase until the user signs off (or asks
   for changes to this phase). If they ask for changes, apply them to the **current phase's
   file** rather than creating yet another file, unless the change is large enough to warrant
   its own phase.

### Two-Step Entity Placement Pattern

Any phase that places spawner objects (enemies, hazards, items, specials, decorations) should be
done as **two phases**: a placement phase followed by a flesh-out phase. The user may also
explicitly request this split for a single layer or single room.

**Step 1 — placement phase (`*_markers_N.tmx`):**
- Drop a **1-tile × 1-tile (32×32 px) placeholder rectangle** at every intended spawn location.
- Name the object after the intended entity class (e.g. `name="SniperJoe"`) so the next phase
  can find it, but **do not** add the real `width`/`height` for that entity and **do not** add
  any properties yet.
- The point of this phase is to lock in **where** things go before getting bogged down in
  per-entity specifics. The placement should be reviewable on a single look in Tiled.

**Step 2 — flesh-out phase (`*_sized_N.tmx`):**
- For each placeholder, resolve the real entity:
  1. Look up the entity's spawn footprint and properties via the `level-editor` skill's workflow
     — read the entity's `onSpawn` function (and its parent class's) to find every property it
     reads and the size its sprite/collider expects.
  2. Cross-reference how the entity is configured in **non-draft TMX files** (see "Cross-File
     Lookups" above) — copy the size and any common property pattern from a finalized level.
- Resize the placeholder rectangle to the real footprint and attach the discovered properties
  (`spawn_room`, `spawn_type`, entity-specific keys like `type=fire`, `target_1`, etc.).
- Do **not** add new placements in the flesh-out phase. If the placement turns out to need
  adjustment (e.g. the real size overlaps a wall), call it out and fix in this same phase rather
  than silently relocating.

The same two-step split applies to hazards (`hazard_markers` → `hazards_sized`), items, and other
spawner layers. For trivially uniform layers (e.g. a level with only one enemy type at a fixed
size), the user may choose to fold the two steps into a single phase — only do this when they
explicitly opt in.

### Default phase sequence

This is the default order. Adapt to the concept — skip phases that don't apply (e.g. no
`items` phase if the level has no pickups) and merge trivially small phases (e.g. fold one
ladder into the scaffold phase rather than spinning up a `specials` phase for it). When merging
or skipping, call it out so the user knows what to expect. Spawner-layer phases (rows 3+) follow
the two-step placement → flesh-out pattern above by default.

| #  | Phase                | What it adds                                                                                            |
|----|----------------------|---------------------------------------------------------------------------------------------------------|
| 1  | `scaffold`           | `game_rooms`, baseline `blocks` geometry (floors/walls/ceilings/platforms), `player` spawn(s)           |
| 2  | `specials`           | `specials` (ladders, water, teleporters), `sensors` (gates), room `event` properties (`boss`/`success`) |
| 3a | `hazard_markers`     | 1×1 placeholder rectangles in `hazards` at intended spawn locations                                     |
| 3b | `hazards_sized`      | Resize each placeholder to the real entity footprint and attach properties                              |
| 4a | `enemy_markers`      | 1×1 placeholder rectangles in `enemies` at intended spawn locations (incl. mini-boss/boss spots)        |
| 4b | `enemies_sized`      | Resize each placeholder to the real entity footprint and attach properties                              |
| 5a | `item_markers`       | 1×1 placeholder rectangles in `items` at intended pickup locations                                      |
| 5b | `items_sized`        | Resize each placeholder to the real entity footprint and attach properties                              |
| 6  | `decorations`        | `decorations`, `backgrounds`, `foregrounds`                                                             |
| 7  | `tiles` *(opt.)*     | Only if the user opted into tile layers in planning step 3                                              |

After phase 1 (scaffold), the level should already be **walkable end to end** — the player can
traverse from intro to boss room through empty geometry. Each subsequent phase enriches but does
not break that walkability.

### Phased workflow for expansions

The same file-per-phase pattern applies when **expanding** an existing level (see "Expanding an
Existing Level"). The first phase copies the existing TMX into a `draft_` file
(e.g. `draft_InfernoMan_v2_scaffold_1.tmx`) and applies only the new structural geometry; later
phases add hazards/enemies/etc. for the new section using the same two-step placement →
flesh-out split. The original (non-`draft_`) level file stays untouched and remains valid as a
cross-file lookup source.

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

Keep suggestions concrete (name the room, name the entity, name the change).
