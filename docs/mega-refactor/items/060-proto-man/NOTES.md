# Notes: 060 Proto Man as a playable character

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

The design so far is in item `029` `NOTES.md`, "`CharacterDef` design" (2026-10-25), and in the `029g` branch
`docs/NOTES.md` ("Decisions", "Review").

## Planning scan for `060a` and `060b` (2026-11-10, `master` `ca75c0d30`)

Two read-only agents. Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

### Code: what a thin `Protoman` can reuse

- Shared already: `players/PlayerJump.kt` (`playerJumpBehavior(JumpParams)`), `PlayerLifecycle`, `PlayerRules`,
  `PlayerDamageNegotiator`, `RunComponent`, `entities/state/PostActionMomentum.kt`.
- `Megaman` receiver functions (cannot be called by another class): `Megaman.run`
  (`entities/megaman/components/ControllerComponent.kt:174-216`), `getMaxRunSpeed` and `defineBodyComponent`
  (`entities/megaman/components/BodyComponent.kt:61-429`: feet fixture, gravity table, water, ice), the momentum and
  room-carry definitions (`MovementComponents.kt:13-25`), `slipSliding` and the direction setter (`Megaman.kt`).
- X copied his own body and run (`MegamanXBody.kt`, `MegamanXValues.kt`). For Proto Man this copy breaks `O8-R1`, so
  `060a` moves these functions into `players/` first.
- Wiring for a new player class (model: `081b`): `PlayerCharacter`, `postCreate` (`MegamanMaverickGame.kt:466-470`),
  `LevelDefinition.characters`, `TextureAsset`, `LevelCompletionScreensTest`, `PlayerRosterTest`. `getType()` returns
  `EntityType.MEGAMAN`.
- Still open, not for `060b`: `Megaman::class` hit masks in `JoeBall`, `Snowball`, `SwinginJoe`; `Megaman::class`
  damage keys; `DarknessV2`/`V4` lights; `EntityQueries.shooting`. Keep the `060b` test map free of enemies and
  gravity switches.
- X lacks Mega's room-transition animation cases (`MegamanAnimationKeySupplier.kt:33-40`); `081f` adds them for X.

### Sprite sheet

- Source: `~/Downloads/Custom _ Edited - Mega Man Customs - DLN-000 Proto Man _ Blues - Proto Man (Expanded) (Inti
  Creates 8-bit-Style).png`, 1050x1121. Work files: `temp/mega-refactor-060a-proto-run-jump/sheet-scan/`
  (`out/cells.json` has all 311 cell boxes).
- 48x48 cells at 1x NES scale, the same as `Megaman_v3`. Feet at cell row 40; Mega's at 47, so each frame moves down
  7 px (`anchor_y: bottom` in `sprite_slicer` does it).
- The sheet is opaque: key the cell color `(147,187,236)` everywhere, and `(38,123,218)` only in highlighted cells
  (the same blue is in the RIDE, SAKUGARNE and FLAME SWORD effects). Crop out the labels before the slicer runs.
- Baseline row: IDLE 2 (blink), STEP 1, WALK 3 (play w1 w2 w3 w2, as Mega's `run`), JUMP 2 (the second frame has the
  shield up), SLIDE 2, CLIMB 2 + 1, HURT 3, STUNNED 2, COLLAPSE 2. The PROTO SHIELD row has 15 frames.
- Unique moves on the sheet: TOP SPIN, WIRE, CHARGE KICK, BREAK DASH, ROLL, MEGA BALL KICK, PILE DRIVER, FLAME SWORD,
  TENGU BLADE, SPEEN, ABSORB, HADOKEN.
