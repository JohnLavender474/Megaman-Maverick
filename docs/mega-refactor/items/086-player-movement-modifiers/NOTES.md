# Notes: 086 Player movement modifiers as a component

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## Scan (2026-10-30, at `ba4b3ca1c`)

A research agent wrote this scan and read the code and the live TMX files. It did not run the game. The first 2 lines
under "What each field does" were checked by hand. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, and
`M/` = `C/entities/megaman/`.

### What each field does

- `movementScalar` scales the run impulse, so acceleration (`M/components/ControllerComponent.kt:183`).
- The top run speed is `getMaxRunSpeed()` (`M/components/BodyComponent.kt:449-453`). It does not read `movementScalar`. It
  has a water scalar and a hard-coded `MOON_MAN` level check.
- `movementScalar` also scales dash, slide, climb, jetpack, knockback, some weapon speeds and the animation speed. Its
  setter writes `animator.updateScalar`, and `Animator.reset()` sets that to 1.

### Writers

- `movementScalar`: 1 writer, the spawn prop (`Megaman.kt:475`). 13 read lines in 7 files.
- `gravityScalar`: 4 writers: the spawn prop (`Megaman.kt:474`), water begin and end (`BodyComponent.kt:252,268`), and
  `BodyGravityChangeRule.kt:54`. Each replaces the value.
- Live data: only `MoonMan_16x14_v5.tmx` sets player scalars (gravity `0.35`, movement `0.8`). `GravityChange` with a
  value is only in `Test1.tmx`, and a zone value stays after the player leaves.

### Risks

- Water end writes `1`, not the prior value, so it would erase a Moon or zone scalar. No live level has both.
- If Mega respawns in water and no new water `BEGIN` fires, gravity may stay at the spawn value (not verified).

### Options

- Typed move only: at most 3 fields move, and it gives no outside hook.
- Named sources with a multiply fold: it must not allocate per frame, must push to the animators on a change, and needs
  an owner for each source. A sticky gravity zone needs a slot that it replaces. A multiply does not express the X6
  `+0.5`, except on the base value.
- A term in `getMaxRunSpeed()`: the smallest form for a speed boost. Its 3 callers need no change.

Other speed effects use no scalar: friction rules, `velocityClamp`, `Force`, and `gravityOn`.

X6 (`~/IdeaProjects/mmx-demake-tools/docs/enemies/conveyor-belt.md`, "The forced scroll"): one value changes, walk speed
+0.5, with one writer pair.
