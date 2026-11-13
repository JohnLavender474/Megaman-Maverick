# Scope: mega-refactor-138a-wreck-into-mock-sprite

What this branch changes and why. Write this file after the scan, with `CONTRACT.md` and before
`BRIEF.src.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-13 (owner, at the brief)
- Scan base: `master` at `add9d66d2` (2026-11-13)

## Goal

One entity for a still map sprite. `MockSpriteEntity` takes its art and its place from the map. The 2 `Wreck` objects
in the X6 intro map become `MockSpriteEntity` objects, and `Wreck` is removed. Each wreck looks the same as before.

## Item

`138` (`ITEMS.md`). From `ITEM.md`:

> `Wreck` (`081e`, 71 lines, `entities/decorations/`) draws one still sprite from the map, as `MockSpriteEntity` (99
> lines) does. Two classes do one job. Merge them, so that a map uses one entity for a still sprite.
>
> - Find what `Wreck` does that `MockSpriteEntity` does not do, and add it to `MockSpriteEntity` as spawn data if a
>   map needs it.
> - Change the `Wreck` spawners in the maps (the X6 intro map) to `MockSpriteEntity`, and remove `Wreck`.
> - Each wreck in the X6 intro stage looks the same as before.
>
> Decision (owner, 2026-11-13): merge target: each branch squash-merges directly into `master`.

## Current state

### The two classes

Paths are under `core/src/main/kotlin/com/megaman/maverick/game/entities/decorations/`.

| Aspect | `Wreck` (`Wreck.kt`, 71 lines) | `MockSpriteEntity` (`MockSpriteEntity.kt`, 97 lines) |
|---|---|---|
| Art | Atlas `ENEMIES_2` (`Enemies2.txt`); region `Wreck/<key>` (`Wreck.kt:46-47`) | Atlas `BOSSES_3`; region `ElecDevil/grid`, fixed (`MockSpriteEntity.kt:53-55`) |
| Region choice | Map property `key`: `wreck_a` or `wreck_b`; other values throw (`Wreck.kt:56-57`) | None; always the same region |
| Region store | Instance `ObjectMap` of animations, built in `init()` | Companion `var region` (a static, `MockSpriteEntity.kt:39`) |
| Sprite size | Region pixels x `TickFrames.SCALE` (2): 96 x 72 (`a`), 36 x 28 (`b`) | Fixed 8 x 6 tiles (`MockSpriteEntity.kt:84`) |
| Position | `TickFrames.place`: an art origin pixel (`a`: 20,12; `b`: 6,6) goes on the center of the object bounds (`Wreck.kt:58,64`, `TickFrames.kt:93-101`) | Bottom center of the object bounds, plus 0.15 tile in the facing direction (`MockSpriteEntity.kt:67,86-87`) |
| Flip | None (`facingRight = false`) | Flips each 1 s: an `UpdatablesComponent` swaps `facing` (`MockSpriteEntity.kt:49,75-81,88`) |
| Draw order | `BACKGROUND`, priority 1 (`Wreck.kt:62`) | Default `PLAYGROUND`, priority 0 (`GameSprite.kt:12`) |
| Animation | `TickFrames`: 1 frame, 1 tick, loops, so it is a still image (`Wreck.kt:34-37`) | `Animation` of 1 region (`MockSpriteEntity.kt:92-94`) |
| Debug | None | Yellow 5 x 4.75 tile debug box (`MockSpriteEntity.kt:45-48,60`) |
| Body, culling | No body, no cull: after its spawn it stays until the level ends | Same |
| Spawn props read | `bounds`, `key` | `bounds` |
| Tags | Default (class name) | Default (class name) |
| `EntityType` | `DECORATION` | `DECORATION` |

`MockSpriteEntity` is a developer mock of the `ElecDevil` grid. It is not a general still sprite yet, so the merge
rewrites it.

### Where the wrecks are

Live map of `X6_INTRO`: `assets/tiled_maps/tmx/X6Intro_16x14_v1.tmx` (`LevelDefinition.kt:234-236`). Map height 48
tiles (1536 px). Layer `decorations` (id 10), which `SpawnersLayerBuilder` builds as `EntityType.DECORATION`. No old copy
of this map has a `Wreck`.

| Object id | Room | Tiled rect (x, y, w, h) | `key` | Sprite rect on `master` (Tiled px) |
|---|---|---|---|---|
| `218` | `room1` | 24, 1089, 32, 32 | `wreck_a` | 0, 1081, 96, 72 |
| `219` | `room1` | 1864, 1010, 32, 32 | `wreck_b` | 1868, 1014, 36, 28 |

The sprite rect comes from `TickFrames.place`: left = center x - origin x x 2, bottom = center y - (height - origin y)
x 2 (game y up), then back to Tiled y down. All values are whole pixels.

Atlas regions (`assets/sprites/sprite_sheets/Enemies2.txt:1567-1580`): `Wreck/wreck_a` 48 x 36, `Wreck/wreck_b` 18 x 14,
no offset, no rotation.

### Users of `MockSpriteEntity`

None. No TMX file and no code spawns it. Only `entity-tags.txt:92` lists it. So a change to it changes no level.

### Code references to `Wreck`

| File | Reference |
|---|---|
| `Wreck.kt` | The class, and the `TODO` at line 28 (item 138) |
| `core/src/smokeTest/resources/entity-tags.txt:120` | `decorations.Wreck: Wreck` |
| `known-entity-failures.txt` | Not listed |
| Tests | None |
| `utils/`, `.claude/` | None |
| Docs on `master` (not changed on the branch) | `items/140-tick-frames-seconds/ITEM.md` (lists `Wreck` as a `TickFrames` user), `docs/x6-intro/intro-objects-plan.md:29` |

## In scope

1. Rewrite `MockSpriteEntity` as a still map sprite:
   - It reads `atlas` (`ConstKeys.ATLAS`, the atlas file name, for example `Enemies2.txt`) and `region`
     (`ConstKeys.REGION`, the full region name, for example `Wreck/wreck_a`) from the spawn props. These are the same
     names and the same value forms as the custom backgrounds (`BackgroundLayerBuilder.kt:232-233`).
   - The sprite fills the object bounds (decision D1).
   - Draw order `BACKGROUND`, priority 1, as `Wreck`.
   - No flip, no facing, no timer, no animation, no debug box, no companion region.
   - A missing `atlas` or `region`, or a region that is not in the atlas, throws at spawn with a message that names the
     map object id, the atlas and the region (fail fast, `docs/CODE_STYLE.md`).
2. In `X6Intro_16x14_v1.tmx`, change objects `218` and `219`: `name` to `MockSpriteEntity`, the rect to the sprite
   rect in the table above, the property `key` to `atlas` and `region`. Keep the object ids.
3. Remove `Wreck.kt` and its line in `entity-tags.txt`.
4. Add a `core` test for `MockSpriteEntity` (see `CONTRACT.md`).

## Out of scope

- The art. The regions keep the names `Wreck/wreck_a` and `Wreck/wreck_b` in `Enemies2`. The `081e` note says that
  wreck A "loses most of its shape at ratio 2"; a redraw is art work, not this item.
- `TickFrames` itself (item `140`). This branch only removes one of its users.
- Map properties for flip, draw section, priority, scale or animation frames: no map needs them (`O1-R5`).
- A rename of `MockSpriteEntity` (decision D2).
- Docs on `master` that name `Wreck` (`140` `ITEM.md`, `docs/x6-intro/intro-objects-plan.md`): update them at the merge.

## New API and first caller

| API | First caller in the branch |
|---|---|
| `MockSpriteEntity` reads the spawn props `atlas` and `region` | Objects `218` and `219` in `X6Intro_16x14_v1.tmx` |

No new class, function, `ConstKeys` key or exception type. The error is an `IllegalArgumentException` with a clear
message, as `Wreck` throws today (`Wreck.kt:57`).

## Affected areas and risks

| File | Change |
|---|---|
| `entities/decorations/MockSpriteEntity.kt` | Rewrite (fewer lines) |
| `entities/decorations/Wreck.kt` | Delete (71 lines) |
| `assets/tiled_maps/tmx/X6Intro_16x14_v1.tmx` | 2 objects in layer `decorations` |
| `core/src/smokeTest/resources/entity-tags.txt` | Remove 1 line |
| `core/src/test/.../entities/decorations/MockSpriteEntityTest.kt` | New |

Risks:

- **A wreck moves by some pixels.** The new rect must equal the old `TickFrames.place` result. The table above gives
  the values; the test checks that the sprite equals the object bounds; the owner looks at both wrecks in play.
- **Draw order.** The old `MockSpriteEntity` used `PLAYGROUND` 0. If that stays, the wrecks draw in front of X and the
  enemies. The new class must use `BACKGROUND` 1.
- **Spawn trigger.** The spawn shape is the object rect, so it grows from 32 x 32 to the sprite rect. Wreck A spawns at
  the level start in both cases. For wreck B the trigger moves 4 px to the right, and it is still at or left of the
  sprite's left edge, so the sprite never shows before its spawn. No visible effect is expected.
- **Merge conflict with `137a`.** `entity-tags.txt` lines 119 (`WoodCratePiece`) and 120 (`Wreck`) are next to each
  other. `137a` plans to remove `WoodCratePiece`. A simple conflict (see `NOTES.md`).

## Overlap with other branches

See `NOTES.md`. In short: no other branch edits the X6 intro map, `MockSpriteEntity`, `Wreck` or `TickFrames`. `137a`,
`139a` and `060c` can touch `entity-tags.txt`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | helps | Removes the companion `region` static of `MockSpriteEntity` (one companion texture region). The region is read per spawn. |
| `O1-R2` Components, not new inheritance | no effect | No new class layer. |
| `O1-R3` No new fragile order | no effect | No listener, no event. |
| `O1-R4` Typed state | no effect | `atlas` and `region` come from map data and are existing `ConstKeys`. `key` is no longer read for this sprite. |
| `O1-R5` Code only with a caller | helps | The 2 map objects use the new spawn data. Flip, section and animation props are left out because no map needs them. |
| `O2-R4` Stable order; `O2-R5` no one-player logic | no effect | No player logic. |
| `O2-R7` Gameplay does not depend on rendering | no effect | A decoration with no body. |
| `O3-R1` No JVM-only API on the web path | waiting | No new JVM-only API. |
| `O5-R1` No per-frame allocation | helps | No per-frame pre-process and no animation update; the sprite is set once per spawn. |
| `O6-R1` CI passes | no effect | Checked by the contract. |
| `O6-R2` Named checks | helps | New `MockSpriteEntityTest`. |
| `O7-R3` No level-identity checks | no effect | |
| `O8-R2` Capabilities, not character checks | no effect | |
| `O9-R1` NES-style pad; `O9-R2` saves | no effect | |

## Decisions

### D1: How the map places the sprite

- **A (recommended):** the sprite fills the object rect. In Tiled, the object is drawn at the sprite's place and
  size (region pixels x 2). What the map shows is what the game draws. No new property.
- **B:** keep the 32 x 32 objects at the X6 points. Add art-origin properties (`origin_x`, `origin_y`, new keys) and a
  scale, and place the sprite as `TickFrames.place` does. It keeps the X6 point in the map, but adds 3 properties and
  2 new keys for 2 objects.
- Reason for A: simplest data, no new keys. The X6 points stay in the `081` docs and in this file.

### D2: The name `MockSpriteEntity`

- **A (recommended):** keep the name, as the item and the branch name say. A rename can be its own small change.
- **B:** rename it now (for example to `MapSprite`), because it is no longer a mock. Now no map or code uses it, so
  the rename touches only the class file, `entity-tags.txt` and the 2 new map objects.

### D3: The old mock behavior and the animation

- **A (recommended):** drop the old mock behavior (the `ElecDevil` grid default, the flip each second, the debug box)
  and use a still region with no animation component. No map uses the old behavior. `Wreck`'s `TickFrames` animation
  is 1 frame, so a still region looks the same, and `TickFrames` loses one user (helps item `140`).
- **B:** keep the old behavior as the default when the map gives no `region`. It keeps a mode that nothing uses.
- **C:** support animated map sprites (frames and durations as map data). No map needs it (`O1-R5`).

### Owner answers (2026-11-13)

- D1: A, the sprite fills the object rect.
- D2: B, rename `MockSpriteEntity` to `MapSprite` (my recommendation at the brief; the scan draft had A).
- D3: A, drop the old mock behavior; a still region with no animation.

## Open questions

None beyond D1 to D3.
