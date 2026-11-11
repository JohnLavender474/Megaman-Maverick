# Scope: mega-refactor-060b-protoman-move

- Status: approved
- Approved: 2026-11-11

## Goal

A playable `Protoman` in slot 0 on a new test level: idle (with the scarf flick), step, walk, jump and fall, with an
atlas sliced from the fan sheet. He is a thin `MegaPlayer` that calls the shared moves. The MM9/MM10 baseline starts
here; buster, slide, ladder, shield and damage reaction come in `060c` to `060e`.

## Item

`060` (Proto Man), `ITEM.md`:

- Phase `060b`: "`Protoman` in slot 0 on a test level: idle, step, walk, jump, fall; sliced atlas" (needs `060j`).
- Decisions (2026-11-10, owner): a thin `Protoman` class that calls Megaman's shared behaviors and components, with his
  own values file, sprites, shield and Proto Strike; a test `LevelDefinition` entry with `characters =
  setOf(PROTOMAN)`, no launcher option; MM9/MM10 rules first; `Protoman` extends `MegaPlayer` (`060j`).
- Sprite source: the fan sheet "Proto Man (Expanded) (Inti Creates 8-bit-Style)", sprites by Hansungkee and others.
  Keep the credits.

Research: `temp/mega-refactor-060b-protoman-move/scan-code.md` (wiring, components, input, animations; before
`060j`), `scan-sprites.md` (cells, slicer, draft atlas and preview `sprites/compare_megaman_protoman_x4.png`).

## Current state

Paths under `core/src/main/kotlin/com/megaman/maverick/game/`. Base: `master` `a1be3cd59` (after `060j`).

- `players/MegaPlayer.kt` owns the shared lifecycle and components. A class gives `cameraFocusHeight`, `direction` and
  8 members: body, momentum, knockback, stun, behaviors, controller, sprites, animations.
- Shared moves: `playerRunButton` with the generic `runStep` (`entities/movement/Run.kt`), `playerBody` (needs
  `IScalableGravityEntity` and a `PlayerGravityComponent`, whose values the `direction` setter signs),
  `playerJumpBehavior`. X shows the minimal wiring of a second class (`entities/megamanx/`).
- Wiring of a player class (model: X): `PlayerCharacter`, the `postCreate` block (`MegamanMaverickGame.kt`), the
  `LevelDefinition` `characters`, `TextureAsset`, `LevelCompletionScreensTest`, `entity-tags.txt`. `PlayerRoster`,
  the launcher, the respawn package and `MegaLevelScreen` need no change.
- Megaman's animations: `stand` 2 frames (1.5 s, 0.15 s), the step `run_trans` (0.1 s) at the start of `stand` (at a
  stop, a landing or the spawn, not at the run start), `run` 4 frames at 0.125 s, `jump` 1 frame for rise and fall.
- The beam-in always uses Megaman's blue beam (`screens/levels/respawn/BeamDown.kt`).
- Code outside the player that names `Megaman` (HUD, pause screen, beam-out, pickups) has an `else` path. The
  `Megaman::class` damage masks and darkness lights miss `Protoman`, so the test map has no enemies, darkness or bosses.
- Sheet: all frames for this branch are in its first row (48x48 cells, the same as Megaman). The sheet has no fall
  frame (Megaman has none either). The 2 idle frames differ only in the scarf. The sheet faces left; Megaman's frames
  face right. The slicer has no key-color or crop option, so a prep step is needed.

## In scope

- `entities/protoman/Protoman.kt`: `Protoman : MegaPlayer, IScalableGravityEntity`, with the 8 members, the
  `direction` setter (`PlayerGravityComponent.applyDirection`), run (`playerRunButton`), body (`playerBody`) and jump
  (`playerJumpBehavior`). No wall slide, air dash, slide, ladder, buster or weapons.
- `entities/protoman/ProtomanValues.kt`: his run, jump, gravity, body and sprite values (D4).
- His animations: `stand` (scarf flick), the step, `run`, `jump`.
- The atlas: `assets/sprites/sprite_sheets/Protoman.txt` and `.png`, the frames, `TextureAsset.PROTOMAN`, the sheet
  pipeline in `utils/sprite_slicer/` (D2, D3), and a credit line in `assets/credits.txt`.
- `PlayerCharacter.PROTOMAN`, the `postCreate` block, `LevelDefinition.REFACTOR_TEST_060` (`characters =
  setOf(PROTOMAN)`), `assets/tiled_maps/tmx/RefactorTest060.tmx` (rooms `walk`, `jump`, `water`, `transition`),
  `LevelCompletionScreensTest`, `entity-tags.txt`.
- Tests: his run and jump through `InputFrame` (model: `MegamanXTest`).
- Docs: `players/README.md` (3 player classes).

Claude's calls (follow Megaman unless a decision says otherwise):

- Jump frame: the plain JUMP 1; the shield-up JUMP 2 waits for `060e`.
- Idle: Megaman's timing (1.5 s, 0.15 s) for the scarf flick.
- The step frame sits on the idle position (the sheet has it 1 px off).
- `run` is a vertical 4-frame strip (`(4, 1)`, as X), not Megaman's 2x2.
- `A` always jumps; `DOWN` + `A` has no slide until `060d` (`TODO`).
- Beam-in: Megaman's blue beam, with a `TODO` for `060e`.
- Damage, stun and knockback: Megaman's values and his `stand` frame while damaged, with `TODO`s for `060e`.

## Out of scope

- Buster and Proto Strike (`060c`), slide and ladder (`060d`), shield, damage reaction, stun art, teleport art and the
  red beam (`060e`), the Proto Sabre (`060f`), weapons (`060h`), co-op and `CharacterDef` (`060i`).
- The `Megaman::class` damage masks and lights (they miss `Protoman`; no test room needs them).

## Affected areas and risks

- New files in `entities/protoman/`, `assets/`, `utils/sprite_slicer/sheets/`; small edits in `PlayerCharacter`,
  `MegamanMaverickGame.kt`, `LevelDefinition`, `TextureAsset`, 2 test files, `entity-tags.txt`.
- `postCreate` builds a third player at start: one more `init()` and atlas load. A missing region fails the smoke
  test.
- A wrong foot line shows him floating or sunk; the preview shows the line matches Megaman's.

## Overlap with other branches

- `mega-refactor-081f-x6-intro-fixes`: changes `entity-tags.txt` (other hunks) and X files; `postCreate` and
  `LevelDefinition` may get neighbor hunks. Simple conflicts if any.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O8-R1` | helps | `Protoman` extends `MegaPlayer` and calls the shared moves; no copy of `Megaman.kt` |
| `O8-R2` | no effect | No character checks added |
| `O8-R3` | waiting | The modes: single player only until `060i` |
| `O1-R2` | no effect | No new layer |
| `O1-R4` | no effect | |
| `O1-R5` | helps | Each new API has a caller (`Protoman`, the test level) |
| `O2-R5` | no effect | Slot 0 only, through the roster |
| `O4-R2` | helps | A test room for each move |
| `O6-R2` | helps | Named checks |
| `O7-R3` | no effect | No level check; Megaman's `MOON_MAN` scalar stays his (Proto Man runs at full speed on Moon Man, `TODO` if needed later) |
| `O9-R1` | no effect | Walk and jump use the d-pad and `A` |

## Decisions

Owner answers to the brief (2026-11-11), all as recommended:

- D1: the step frame shows as Megaman's in this game: at a stop, a landing and the spawn.
- D2: a prep script (crop, key colors, mirror), the prepared sheet and the slicer YAML go in
  `utils/sprite_slicer/sheets/`; the slicer tool does not change.
- D3: the prep step mirrors the frames, so Proto Man flips by Megaman's rule.
- D4: his own `ProtomanValues`, starting with Megaman's numbers; `060g` tunes them.

Scope change after the review before the play test (owner, 2026-11-11):

- The copies with 3 callers move to `players/` (or `MegaPlayer`) and Megaman, X and Proto Man call them: the sprite
  hide and flash rule (`shouldHideSprite`) and the direction sign helper (`upSign`). The updatables list stays in each
  class with a `TODO` (its frame order differs per class, as `060j` found). The land sound and the `GutsTank`
  side-stick rule (2 callers) stay with `TODO`s.
- No swim: in water he falls slower and jumps higher from the floor; `A` in mid-water does nothing. A `TODO` names
  `060g`.
- The 10 `Megaman::class` `TODO`s point to item `030` (faction and root owner), which owns those keys.
- The `water` room pool is 3 tiles deep, so that he can jump out.

## Open questions

See the brief.
