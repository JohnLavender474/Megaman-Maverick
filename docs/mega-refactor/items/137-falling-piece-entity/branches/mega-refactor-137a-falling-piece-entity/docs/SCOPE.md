# Scope: mega-refactor-137a-falling-piece-entity

What this branch changes and why. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-13 (owner, at the brief)
- Scan: 2026-11-13, `master` at `add9d66d2`.

## Goal

One falling-piece entity, `FallingPiece`, replaces `DebrisPiece`, `BlockPiece` and `WoodCratePiece`. The differences
between the 3 kinds are data in a small typed def. Each piece looks and moves as before. A new kind of piece is a new
def, not a new class.

## Item

Item `137` (`ITEM.md`), all of its proposed scope:

- Compare the 3 classes and list what is the same and what is different (art, size, speed, gravity, life time, sound).
- One entity class that takes the differences as spawn properties or as a small typed def.
- Move the callers of the 3 classes to the new entity. Each piece looks and moves the same as before.

Decision (owner, 2026-11-13): merge directly into `master`. Open question: "Spawn properties, or one typed def per
kind of piece?" (see D1).

## Current state

### The 3 classes, compared

All 3 are in `core/src/main/kotlin/com/megaman/maverick/game/entities/decorations/`.

| Part | `DebrisPiece` | `BlockPiece` | `WoodCratePiece` |
|---|---|---|---|
| Lines | 150 | 129 | 109 |
| Base, interfaces | `MegaGameEntity`, `IBodyEntity`, `ISpritesEntity` | same, and `IAudioEntity` | same as `DebrisPiece` |
| `EntityType` | `DECORATION` | `DECORATION` | `DECORATION` |
| Body | `ABSTRACT`, 0.25 tile, collision off (`:128-134`) | same (`:103-107`) | same (`:79-83`) |
| Fixtures | none | none | none |
| Gravity (tiles/s²) | -35 (`:41`) | -37.5 (`:40`) | -37.5 (`:36`) |
| Fall speed clamp | 12 tiles/s (`:42`, `:130`) | none | none |
| X friction | off (`:132`) | on, default 1/s (`exp(-delta)` per step, `Body.kt:132`) | on, same |
| Y friction | off | off | off |
| Start velocity | from the X6 speed tables (`:48-49`, `:117-120`), or `TRAJECTORY` as given (`:114-115`) | `IMPULSE` as given (`:80-81`) | `IMPULSE` as given (`:65-66`) |
| Start point | `POSITION` plus a random jitter of ±0.2 tile on each axis (`:110-112`) | `POSITION` (`:77-78`) | `POSITION` (`:62-63`) |
| Life | 2 s, then `destroy()` (`:43`, `:96-100`) | 2 s (`:39`, `:98-99`) | 2 s (`:37`, `:74-75`) |
| End by landing or cull | no (no collision, no `CullablesComponent`) | no | no |
| Atlas | given by the owner: `ENEMIES_2` default, `BOSSES_3` for D-1000 (`:53`) | `PLATFORMS_1` (`:59`) | `DECORATIONS_1` (`:49`) |
| Regions | owner keys, loaded by the owner with `DebrisPiece.load` (`:53-59`) | `BlockPiece/red`, `gold`, `brown`, `pink`, loaded in own `init()` (`:58-65`) | `WoodCratePiece/0` to `9`, loaded in own `init()` (`:48-51`) |
| Region chosen by | `KEY` (`:107-108`) | `COLOR`, a `BlockPieceColor`, default `RED` (`:83`) | `INDEX` (`:68`) |
| Missing region | throws (`:57`, `:108`) | silent: `findRegion` gives `null` | silent: `findRegion` gives `null` |
| Sprite size | region size × `TickFrames.SCALE` (2) (`:143`) | 1 × 1 tile (`:114`) | 0.1875 × 0.1875 tile (6 px) (`:90`) |
| Draw priority | `FOREGROUND`, 1 | same | same |
| Spin | none | +45° each 0.1 s, entity state, reset on spawn (`:53-54`, `:87-96`, `:123-124`) | +90° each 0.05 s, on the sprite in the sprite pre-process; never reset (`:92-104`) |
| Blink | hidden on odd 0.033 s steps (`:44`, `:145`) | none | none |
| Alpha | not set | set to 1 each frame (`:121`) | not set |
| Sound | none | `AudioComponent`, never plays a sound (`:67`) | none |
| Public state | `key` (only `D1000DefeatTest` reads it) | `BlockPieceColor` enum | `MAX_INDEX = 9` |
| Pool | `MegaEntityFactory`, one pool per class | same | same |

The same in all 3: the body, the life of 2 s, the end by time only, the draw priority, the entity type and the pool.
The differences are: gravity, the fall clamp, X friction, the start jitter, the art lookup, the sprite size, the spin
and the blink.

### Callers

Production code, 12 files:

| File:line | Kind | What it passes |
|---|---|---|
| `entities/enemies/MetaWheelF.kt:114`, `:199` | debris | `load(services, DEBRIS)` (3 keys); `spawnPieces(DEBRIS, center)` |
| `entities/enemies/Junkroid.kt:133`, `:207` | debris | `load` (4 keys); `spawnPieces(DEBRIS, body.getCenter())` |
| `entities/enemies/HoverGunner.kt:204`, `:347` | debris | `load` (5 keys); `spawnPieces(DEBRIS, body.getCenter())` |
| `entities/blocks/SabreBlock.kt:71`, `:108` | debris | `load` (3 keys); `spawnPieces(DEBRIS, center)` |
| `entities/bosses/D1000.kt:301` | debris | `load(services, D1000Defeat.DEBRIS, TextureAsset.BOSSES_3)` (8 keys) |
| `entities/bosses/d1000/D1000Defeat.kt:120` | debris | `spawnPiece(key, randomIn(bounds), velocity)`; velocity in px/s, 5 to 12 tiles/s at 30° to 150° |
| `entities/blocks/BreakableBlock.kt:52-66` | block | `breakApart(center, color)`: 4 pieces, `COLOR`, `POSITION` (one shared vector), `IMPULSE` (±5,3 and ±3,5 tiles/s), `THUMP = false` (unread) |
| `entities/blocks/BreakableBlock.kt:78`, `:104-109` | block | `BlockPieceColor` from the map `color` property (string), default `GOLD` for `SMB3_Gold`, else `RED` |
| `entities/blocks/CrumblingBlock.kt:117-124` | block | 4 pieces per 1-tile cell: `POSITION`, `IMPULSE`, `COLOR = GOLD` |
| `entities/blocks/PinkBlock.kt:30` | block | puts `BlockPieceColor.PINK.name` in its spawn props |
| `entities/bosses/BigAssMaverickRobot.kt:274` | block | `BreakableBlock.breakApart(center, BROWN)` for each `piece_N` map object, at spawn |
| `entities/projectiles/Picket.kt:76` | block | `BreakableBlock.breakApart(position, BROWN)` when it hits a block |
| `entities/blocks/WoodCrate.kt:190-199` | wood crate | `POSITION` (random in the crate), `IMPULSE` (a pooled vector), `INDEX` (random 0 to `MAX_INDEX`) |

Spawn sites: 8 (4 `spawnPieces`, 1 `spawnPiece`, 3 `fetch`). Load sites: 5. `breakApart` callers: 2. Files that use
`BlockPieceColor`: 5 (`BreakableBlock`, `CrumblingBlock`, `PinkBlock`, `BigAssMaverickRobot`, `Picket`). No caller
passes `parentSpeedX` to `spawnPieces`, so the `SPEED` path in `DebrisPiece.onSpawn` (`:117-118`) always adds 0.

Tests and data:

| File:line | Use |
|---|---|
| `core/src/test/.../bosses/D1000Rig.kt:36`, `:160`, `:168` | `entity is DebrisPiece` in the tick and fly lists |
| `core/src/test/.../bosses/D1000DefeatTest.kt:15`, `:93`, `:95`, `:207-228` | finds the fragments by class, reads `piece.key` and the body |
| `core/src/smokeTest/resources/entity-tags.txt:71`, `:78`, `:119` | the 3 classes and their tags |
| `core/src/smokeTest/resources/known-entity-failures.txt` | no line for the 3 classes |
| `assets/sprites/sprite_sheets/Platforms1.txt:41-62`, `Decorations1.txt:391-454` | the regions `BlockPiece/*` and `WoodCratePiece/*` |
| `assets/tiled_maps/tmx/IntroStage_v3.tmx:598-603` | 6 objects named `BlockPiece` in the `children` layer. The layer has no builder; they are only points for `piece_1` to `piece_6` of `BigAssMaverickRobot`. Nothing spawns them. |
| `assets/tiled_maps/tmx/IntroStage_v2.tmx:429-434` | the same, in a map that no `LevelDefinition` names |

No map spawns a piece directly.

### Problems

- 3 copies of the same body, sprite and life code.
- The block and wood regions fail silently when a region is missing; the debris regions fail fast.
- `BlockPiece` has an `AudioComponent` that plays nothing. `BreakableBlock` passes `THUMP`, which nothing reads.
  `ConstKeys.THUMP` has no other user (no map uses `thump`).
- `WoodCratePiece` spins in the sprite pre-process (a draw step), and its angle carries over to the next spawn of
  the pooled instance.
- `DebrisPiece.load` and `spawnPieces` take a Kotlin `Array<String>`, not a LibGDX `Array` (`docs/CODE_STYLE.md`).

## In scope

- New class `FallingPiece` in `entities/decorations/` and a typed def `FallingPieceDef` with 3 values: `DEBRIS`,
  `BLOCK` and `WOOD_CRATE` (D1). Fields: gravity, fall clamp (or none), X friction on or off, start jitter, sprite
  size (a size in tiles, or the region size × `TickFrames.SCALE`), spin (step and delay, or none), blink (step, or
  none). The body size, the life of 2 s and the draw priority stay constants of the class, because they are the same
  for all 3.
- One spawn entry: `FallingPiece.spawn(def, key, position, velocity)`. Velocity in px/s, as the callers give it
  today. The spawn props reuse existing keys (`DEF`, `KEY`, `POSITION`, `TRAJECTORY`).
- Per-value overrides (owner answer D1): `onSpawn` reads each of the 7 values from its spawn property first, then from
  the def. Keys: `GRAVITY` (tiles/s²), `CLAMP` (tiles/s, `null` = none), `FRICTION_X` (`Boolean`), `JITTER` (tiles),
  `SIZE` (a `FallingPieceSize`), `SPIN` (a `FallingPieceSpin`, `null` = none), `BLINK` (s, `null` = none). New keys:
  `ConstKeys.JITTER` and `ConstKeys.BLINK` (no existing key fits). A present key with a `null` value means "none".
  With no def, a missing key throws `IllegalArgumentException` at spawn.
- The X6 throw (the speed tables and the jitter) stays one helper for the 4 X6 owners, for example
  `FallingPiece.throwDebris(keys, position)`. The unused `parentSpeedX` parameter goes.
- Art (D2): the region keys stay. `FallingPiece.init()` loads the 4 block regions and the 10 wood crate regions, as
  `BlockPiece` and `WoodCratePiece` do today. The debris owners keep their `load` call. A missing or unloaded region
  throws at once, for all 3 kinds.
- `BlockPieceColor` moves out of `BlockPiece` to its own file and gives its region key. Its 5 users change their
  import.
- Spin (D3): one rule for both spinning kinds. The angle is entity state, reset on spawn, and steps with a timer in
  the updatable.
- Each frame, the sprite pre-process sets every sprite field that a kind can change: region, size, origin, rotation,
  alpha and hidden. A pooled instance can be a block piece in one life and debris in the next.
- Each spawn sets every body field that a kind can change: gravity, velocity clamp, X friction, velocity and position.
- Move the 8 spawn sites and the 5 load sites. Remove `DebrisPiece`, `BlockPiece`, `WoodCratePiece`, the unused
  `THUMP` prop and `ConstKeys.THUMP`.
- Tests: update `D1000Rig` and `D1000DefeatTest` to `FallingPiece`. A new `FallingPieceTest`. A temporary side-by-side
  contract suite (see `CONTRACT.md`).
- `entity-tags.txt`: remove the 3 lines, add `decorations.FallingPiece: FallingPiece`.
- Remove the `TODO` at `DebrisPiece.kt:35` (the class goes).
- A small test map `RefactorTest137.tmx` (`REFACTOR_TEST_137`) with one room that holds a wood crate, a brick block
  and an `SMB3_Gold` block, so that one session spawns more than one kind from the same pool. No real level does this
  in one room.

## Out of scope

- Renaming the atlas regions (`BlockPiece/*`, `WoodCratePiece/*`). The region prefix is no longer the class `TAG`;
  debris regions already use the owner's `TAG`.
- `TickFrames` and its `SCALE` (item `140`).
- A shared texture cache (item `028`). The region cache stays a companion map, as in the 3 classes today.
- Seeded random values (item `011`). The debris throw keeps `UtilMethods.getRandom`.
- Pieces that land, bounce or make a sound. No kind does this today.
- Other decorations that fall (for example `Wreck`, item `138`).
- The old map `IntroStage_v2.tmx` and the `BlockPiece` labels in the `children` layers (no builder reads them).
- `docs/x6-intro/intro-objects-plan.md`, which names `DebrisPiece` as history.

## New API and first callers

| API | First caller in the branch |
|---|---|
| `FallingPiece` (entity) | all 8 spawn sites |
| `FallingPieceDef` and `DEBRIS` | `D1000Defeat.kt:120` and `throwDebris` |
| `FallingPieceDef.BLOCK` | `BreakableBlock.breakApart`, `CrumblingBlock.crumble` |
| `FallingPieceDef.WOOD_CRATE` | `WoodCrate` (its destroy) |
| `FallingPiece.spawn(def, key, position, velocity)` | `D1000Defeat`, `BreakableBlock`, `CrumblingBlock`, `WoodCrate`, `throwDebris` |
| `FallingPiece.throwDebris(keys, position)` (moved from `spawnPieces`) | `MetaWheelF`, `Junkroid`, `HoverGunner`, `SabreBlock` |
| `FallingPiece.load(services, keys, asset)` (moved from `DebrisPiece.load`) | the 5 debris load sites |
| `BlockPieceColor.regionKey` | `BreakableBlock`, `CrumblingBlock` |
| `FallingPiece.WOOD_CRATE_REGIONS` | `WoodCrate` (picks a random key) |
| `FallingPieceSize`, `FallingPieceSpin` | the 3 defs |
| Spawn-property overrides, `ConstKeys.JITTER`, `ConstKeys.BLINK` | none in production; owner exception (D1); `FallingPieceTest` |

`FallingPiece.key` stays, as `DebrisPiece.key` today; only `D1000DefeatTest` reads it.

## Affected areas and risks

Files: the 3 piece classes (removed), 1 or 2 new files, the 12 caller files, `Constants.kt` (1 key removed), 2 test
files, 1 new test file, 1 temporary test suite, `entity-tags.txt`, 1 new test map and its `LevelDefinition` entry.

- **Pool reuse across kinds.** One pool now holds all 3 kinds. A field that one kind sets and another does not
  (rotation, alpha, hidden, origin, velocity clamp, friction) leaks into the next life. Check: the spawn and the
  sprite pre-process set every field; `FallingPieceTest` spawns one instance as each kind in turn; the `mixed` room.
- **A small change in motion.** Gravity, the clamp and the X friction differ per kind. A wrong def value changes the
  arc. Check: the temporary side-by-side suite compares the old and the new body and sprite each frame.
- **Art lookup.** `BigAssMaverickRobot` and `Picket` spawn block pieces with no `BreakableBlock` in the level. The
  block regions must load in `FallingPiece.init()`, not in `BreakableBlock`. Check: `core:smokeTest` runs
  `FallingPiece.init()` with the real atlases; the `INTRO_STAGE` and `TEST_2` checks.
- **Shared vectors.** `BreakableBlock` passes one `center` vector to 4 spawns, and `WoodCrate` passes a pooled
  impulse vector. The spawn is queued, so the piece reads the vector one update later. Keep this as it is: the piece
  copies the values in `onSpawn`, and the callers do not change how they make the vectors.

## Overlap with other branches

See `docs/NOTES.md`, "Overlap with other branches". Summary: `060c` changes 7 of the 12 caller files (imports and
damage tables) and `entity-tags.txt`; `139a` will likely change `D1000Rig.kt` lines 160 and 168 (`JunkShot`) and
`Junkroid` and `HoverGunner`; `138a` will likely change the `decorations` lines of `entity-tags.txt`. All are text
conflicts in separate lines, with no shared logic.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | `waiting` | After `096`. The region cache stays a companion map; 3 caches become 1. `MegaEntityFactory` stays static. |
| `O1-R2` Components, not new inheritance | `helps` | 3 classes become 1. No new abstract class. |
| `O1-R3` No new fragile order | `no effect` | No listener, no event. |
| `O1-R4` Typed state | `helps` | The differences are a typed def, not new keys. Reuses `DEF`, `KEY`, `POSITION`, `TRAJECTORY`; removes `ConstKeys.THUMP`. |
| `O1-R5` Code only with a caller | `helps` | Each new API has a caller (table above). The unused `parentSpeedX` and `THUMP` go. |
| `O2-R1` Input only through `InputFrame` | `no effect` | |
| `O2-R2` Random values from the seeded source | `waiting` | After `011`. The debris throw keeps `getRandom`, as today. |
| `O2-R3` One fixed tick | `waiting` | After `010`. Timers use `delta`, as today. |
| `O2-R4` Stable iteration order | `no effect` | |
| `O2-R5` No logic assumes one player | `no effect` | The pieces do not read players. |
| `O2-R6` No logic assumes one camera | `waiting` | After `094`. The `ABSTRACT` body still updates only while it overlaps the camera, as today. |
| `O2-R7` Gameplay does not depend on rendering | `helps` | The wood crate spin moves from the sprite pre-process to the updatable. The pieces have no gameplay effect. |
| `O2-R8` Local-only data out of the simulation | `no effect` | |
| `O2-R9` Events and progress name a slot | `no effect` | |
| `O3-R1` No JVM-only API on the web path | `waiting` | After `032`. No new JVM-only API. |
| `O5-R1` No per-frame allocation | `no effect` | The update and the pre-process allocate nothing. Spawns copy vectors, as today. |
| `O5-R2` Measure hot paths | `no effect` | Not a hot path. |
| `O6-R1` CI passes | `helps` | New tests. |
| `O6-R2` Named checks | `helps` | `CONTRACT.md` names the checks; the side-by-side suite proves the old motion. |
| `O7-R3` No level-identity checks | `no effect` | |
| `O8-R2` Capabilities, not character checks | `no effect` | |
| `O9-R1` Playable with an NES-style pad | `no effect` | |
| `O9-R2` Saves stay loadable | `no effect` | Nothing saved. |

No conflict.

## Decisions for the owner

### D1: Spawn properties, or one typed def per kind?

- **A (recommended): a typed def.** `FallingPieceDef` holds the 7 values that differ; 3 named defs, each with a real
  caller. The spawn passes one def, one region key, a position and a velocity. Reasons: the values come from code,
  not from map data, so new keys would break `O1-R4`; a def is complete by type, so a kind cannot miss a value and
  fall back to a default (today `BlockPiece` falls back to `RED`); the compiler finds each user.
- **B: spawn properties.** Each difference is its own spawn prop (about 7 new `ConstKeys`, each with a default).
  Trade-off: a map could set them, but no map spawns a piece, so this flexibility has no caller (`O1-R5`). Errors
  move from the compiler to play time.

### D2: How does a piece find its art?

- **A (recommended): region keys and one cache in the piece.** `FallingPiece.init()` loads the block and wood
  regions, as today; debris owners keep `load` (their atlas differs per owner). The smoke test checks all fixed
  regions through `FallingPiece.init()`, and each debris owner's regions through its own `init()`. The D-1000 test
  keeps reading `key`.
- **B: the caller passes a `TextureRegion`.** No cache in the piece. Trade-off: `BreakableBlock`, `CrumblingBlock`,
  `WoodCrate`, and also `BigAssMaverickRobot` and `Picket` (through `breakApart`) must each load the block regions;
  `D1000DefeatTest` must compare regions in place of keys.
- **C: every owner calls `load`.** Trade-off: the same extra load sites as B, with keys kept.

### D3: The wood crate spin

Today a wood piece's angle stays on the pooled sprite from its last life, and it steps in the draw step. With one
pool for all kinds, the old angle can come from a block piece (45° steps), so "exactly as before" is not possible
without extra state.

- **A (recommended): one spin rule.** The angle is entity state, reset to 0 on spawn, stepped by a timer in the
  updatable (+90° each 0.05 s for wood, +45° each 0.1 s for blocks). Visible change: a wood piece starts at 0°, not at
  a left-over quarter turn. At 20 quarter turns a second, this is very hard to see.
- **B: keep the carry-over.** A separate wood-only angle that survives across spawns, stepped in the sprite
  pre-process. Trade-off: extra state that exists only to copy an accident; it keeps the spin in a draw step
  (`O2-R7`).

## Owner answers (2026-11-13)

- D1: A typed def, and `onSpawn` also takes per-value spawn properties: each of the 7 values reads its property first, then the def; with no def, every value must come from properties, or the spawn fails fast (owner, 2026-11-13). The override path is an owner-requested API with no production caller yet: an owner exception to the rule "an API needs a real caller"; tests cover it.
- D2: Region keys and one cache in the piece; debris owners keep their load call.
- D3: One spin rule: the angle resets to 0 at each spawn.
