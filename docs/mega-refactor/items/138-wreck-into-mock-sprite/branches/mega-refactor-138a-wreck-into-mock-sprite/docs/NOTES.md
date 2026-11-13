# Notes: mega-refactor-138a-wreck-into-mock-sprite

## Decisions

The owner decisions D1 to D3 are in `SCOPE.md` ("Owner answers").

Implementation choices (2026-11-13, no owner input needed):

- An unknown `atlas` file name also fails fast: `MapSprite` checks `assets.isLoaded(...)` first, so the error names
  the map object id, the atlas and the region, not only the raw `AssetManager` "Asset not loaded" text.
- The errors are `IllegalArgumentException`s with a clear message, as `SCOPE.md` says (no new exception type).
- The sprite is set once per spawn in `onSpawn` (region and bounds); no sprite pre-process runs per frame.

## Overlap with other branches

Checked 2026-11-13, `master` at `add9d66d2`, with `git -C <worktree> diff --name-only master...HEAD` and
`git -C <worktree> status --short` in each worktree.

| Branch | State | Overlap |
|---|---|---|
| `mega-refactor-060c-protoman-buster` | 70 files changed, plus uncommitted work | `entity-tags.txt` only (lines of `PlayerSabreSlash` and `ProtoBuster`, far from line 120). No TMX except `RefactorTest060.tmx`. No `decorations/`, `TickFrames` or `X6Intro` change. No conflict expected. |
| `mega-refactor-137a-falling-piece-entity` | Branch start only (no commits, no changes) | Plans to replace `DebrisPiece`, `BlockPiece` and `WoodCratePiece` in `entities/decorations/`. It will remove `entity-tags.txt:119` (`WoodCratePiece`), which is next to line 120 (`Wreck`) that this branch removes: an adjacent-line conflict at the second merge. Simple: drop both lines. `DebrisPiece` uses `TickFrames`; this branch does not change `TickFrames`. |
| `mega-refactor-139a-shared-enemy-shot` | Branch start only | `JunkShot` uses `TickFrames`; no shared file except a possible `entity-tags.txt` line far from line 120. |
| `mega-refactor-151a-level-completed-rename` | No change to the files of this branch | None. |
| `compactor-level` | Not a refactor branch | No change to `X6Intro_16x14_v1.tmx`, `TickFrames`, `Wreck` or `MockSpriteEntity`. |

No worktree changes `X6Intro_16x14_v1.tmx`. Its last commit on `master` is `f25897abd` (`081f`).

Item `140` (`TickFrames` to seconds) lists `Wreck` as a `TickFrames` user. If this branch merges first, `140` has one
user less; update its `ITEM.md` on `master` at the merge.

## X6 source

The wreck points in the map are the X6 object positions (`docs/x6-intro/intro-objects-plan.md`, "Record positions":
TMX x = X6 x, TMX y = X6 y). With D1 option A, the map objects move to the sprite rect, so the map no longer holds the
X6 points. The old points: object `218` at 24, 1089 and object `219` at 1864, 1010 (32 x 32 each), with the art
origins `wreck_a` (20, 12) and `wreck_b` (6, 6) from `Wreck.kt:34-37`.
