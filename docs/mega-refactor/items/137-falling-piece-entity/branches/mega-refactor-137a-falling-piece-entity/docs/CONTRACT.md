# Contract: mega-refactor-137a-falling-piece-entity

The behavior that this branch changes on purpose, the behavior that it must not change, and how to check them. The
owner approves the brief, not this file. Do the checks before the branch merges, and write the result in the branch
process log.

- Status: approved
- Approved: 2026-11-13 (owner, at the brief)

This draft assumes the recommended answers to D1, D2 and D3 in `SCOPE.md`.

## Intended changes

- (D3) A wood crate piece starts its spin at 0° on each spawn. Before, a reused piece started at the angle of its
  last life. The spin rate stays +90° each 0.05 s.
- A wood crate piece no longer spins while the game is paused or the world is held (a boss intro hold). Before, it
  stepped its spin in the sprite pre-process, which runs in a pause; now the spin steps with the other kinds, in the
  updatable. Found by the review before the play test.
- A spawn property of the wrong type throws at spawn with the key in the message (before the override path
  existed, no such property was read).
- A missing or unloaded block or wood crate region throws at once. Before, the piece drew with a `null` region. No
  live data has a missing region, so play does not change.
- `BlockPiece`, `WoodCratePiece` and `DebrisPiece` are gone. Their tags are gone from `entity-tags.txt`, and
  `FallingPiece` is added. No game code looks up these tags.
- The X6 debris throw draws its 2 table indices before the 2 jitter values (before: jitter first, in `onSpawn`).
  With the same seed, a piece gets other values; the set of start velocities and the jitter range are the same.
  Not visible in play.

## Must not change

For each kind (debris, block, wood crate), compared with `master`:

- The start point, including the ±0.2 tile jitter of debris.
- The start velocity: the X6 speed tables for the 4 X6 owners, the given velocity for D-1000 and for the blocks and
  wood crates.
- Gravity (-35 tiles/s² for debris, -37.5 for blocks and wood crates), the 12 tiles/s fall clamp of debris, the X
  friction of blocks and wood crates, and no X friction for debris.
- No collision: pieces fall through floors and walls.
- The life of 2 s, then the piece goes. No cull, no landing.
- The art: the same regions, the sprite size (debris: region size × 2; block: 1 tile; wood crate: 6 px), the draw
  priority (`FOREGROUND`, 1).
- The spin of block pieces (+45° each 0.1 s, from 0° on spawn) and the blink of debris (hidden on odd 0.033 s
  steps). Debris does not spin; blocks and wood crates do not blink.
- The number of pieces and the colors: 4 per brick and per crumbling cell, the color from the map or the owner
  (`RED`, `GOLD`, `BROWN`, `PINK`), the wood crate piece count and its random region index.
- No sound from a piece.
- A piece that is reused from the pool as another kind shows nothing of its last life (rotation, alpha, hidden,
  size, region, fall clamp, friction).

## Automated checks

- [x] `./gradlew engine:test` passes.
- [x] `./gradlew core:test` passes, with:
  - [x] `D1000DefeatTest` and `D1000Rig` moved to `FallingPiece`, with no change to what they assert.
  - [x] New `FallingPieceTest` (permanent): for each def, a spawn sets the position, the velocity, the gravity, the
    clamp and the X friction; the sprite size and region; the piece is destroyed after 2 s and not before; block
    pieces step 45° each 0.1 s and wood crate pieces 90° each 0.05 s, both from 0°; debris blinks on odd 0.033 s
    steps and never rotates. One instance spawned as block, then wood crate, then debris keeps no state from the
    last life. An unloaded region key throws. The test gives the camera a viewport, so the `ABSTRACT` body updates
    (`core/src/test/CLAUDE.md`).
  - [x] Temporary side-by-side suite `FallingPieceLegacyContractTest`, with frozen copies `LegacyDebrisPiece`,
    `LegacyBlockPiece` and `LegacyWoodCratePiece` in the test sources. For each kind and the same inputs (a fixed
    seed on `UtilMethods.random` for debris), the old and the new piece give the same body position and velocity,
    sprite size, rotation and hidden state at each step over 2.1 s, and end on the same step. The wood crate
    rotation is compared from a fresh instance only (D3). Delete the suite and the frozen copies in the last commit
    before the merge. The X6 throw is compared as the set of start velocities and the jitter range over 400 seeds, because
    the order of the random calls changed (see "Intended changes"). Last result: 9 of 9 passed (2026-11-13); deleted
    in step 7.
- [x] `./gradlew core:smokeTest` passes: `FallingPiece.init()` loads the 4 block and 10 wood crate regions from the
  real atlases; each debris owner's `init()` still loads its regions. `entity-tags.txt` lists `FallingPiece` and not
  the 3 old classes.
- [x] `./gradlew lwjgl3:jar` builds.
- [x] No reference to `DebrisPiece`, `BlockPiece` (as a class), `WoodCratePiece` (as a class) or `ConstKeys.THUMP`
  stays in `core/src`.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from the branch worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-137a-falling-piece-entity`

### Experimental

- `./run-game-alpha.sh --level REFACTOR_TEST_137 --room mixed --weapons MOON_SCYTHES`: shoot the wood crate until it
  breaks, throw a Moon Scythe at a brick, and jump into a gold block from below. Then, after all pieces of the first round are gone (about 2 s, so the
  pool reuses them), do it again with the second crate, brick and gold block (the room has 2 of each). Wood pieces
  are small and spin in quarter turns; red and gold pieces are 1 tile and spin in 1/8 turns; no piece blinks; no
  piece shows a size, angle or color of another kind. (The room is new in this branch; see `SCOPE.md`.)
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room1`: shoot a wood crate until it breaks. The small wood pieces
  spin fast from the crate, slow down in X, fall through the floor and are gone after about 2 s.

### Control

- `./run-game-alpha.sh --level RODENT_MAN --room room1 --weapons MOON_SCYTHES`: throw a Moon Scythe at the 3 linked
  bricks. Each brick breaks into 4 red pieces, up-left and up-right, spinning, slowing in X, gone after about 2 s.
- `./run-game-alpha.sh --level DESERT_MAN --room room3`: let a `TorikoPlundge` plunge into a crumbling block. Each
  1-tile cell gives 4 gold pieces and a thump sound.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --heartTanks all --healthTanks all`: go right to the boss.
  When `BigAssMaverickRobot` appears, brown pieces fall from the 6 points on the ceiling, as before.
- `./run-game-alpha.sh --level TEST_2 --room room1`: go right to `PicketJoe`. A picket that hits a block breaks into
  4 brown pieces.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room targets`: kill the `Junkroid` and slash the 2 `SabreBlock`s.
  The debris pieces fly up and out, blink, do not spin, keep their X speed, and are gone after 2 s.
- `./run-game-alpha.sh --level REFACTOR_TEST_081 --room sabre`: kill the `HoverGunner`, the `MetaWheelF` and the
  `Junkroid`, and slash the `SabreBlock`. Each throws its own debris, as above.
- `./run-game-alpha.sh --level X6_INTRO --room room2 --heartTanks all --healthTanks all`: go right and defeat the
  D-1000. At the break, 12 to 14 body chunks fly from the body box, blink, fall through the floor and are gone after
  2 s.

## Behaviors to check

- Pieces keep falling and end while the game is paused and resumed (they stop during the pause; a wood piece also stops its spin, see "Intended changes").
- Pieces that leave the view stop moving and still end after 2 s, as before (`ABSTRACT` body, no cull).
