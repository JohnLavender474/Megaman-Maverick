# Notes: 127 View edge warp: test the warp spot for solids

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## Review of 2026-11-06 (read-only design agent, `master` at `21efc38ea`)

- Query: `levelState.worldContainer` (`LevelState.kt:17`), `forEachFixture(minX, minY, maxX, maxY, action)`
  (`IWorldContainer.kt:20,35`); rebuilt after each world step (`WorldSystem.kt:136-141`). Pass a lambda held in a field
  (`O5-R1`). Example: `DynamicBodyHeuristic.containsBlock`. Test the held player's own bounds at the spot, and skip its
  own fixtures.
- Solids: every `Block` is a `STATIC` body (`Block.kt:268`), also moving platforms. Blocked when the block filters do
  not let the player through, as `MegaCollisionHandler.kt:40-47`. Not solid: `COLLIDE_DOWN_ONLY` (`LadderTop`,
  `CollideDownOnlyBlock`, `DropperLift`), `SLOPE` (`GroundSlope`).
- Hazards: a `FixtureType.DEATH` fixture (`Death`, `Spike`, `Lava`, `LavaBeam`, `CeilingCrusher`, `RisingLavaRiver`,
  `SwingingAxe`, `PreciousSpike`, `FrozenBlock`, some enemies). Water and ladders are safe.
- Enemies: a `DAMAGER` fixture of an `IDamagerEntity` that passes `playerAllowsDamageFrom` (`PlayerRules.kt:34`).
- Culling: things out of view cull after 1 s (`Block.kt:75`), so a spot must be fully in view.
- Facing: `IGameEntity.facing` (`engine/.../entities/EntityQueries.kt:23`); Megaman faces away from the wall in a wall
  slide (`PlayerWallSlide.kt:42`), and toward it when he runs into it; X faces the wall at a wall kick
  (`MegamanXMoves.kt:104`).
- Tests: in the `EdgesRig` of `SharedViewTest.kt`, with a real `SimpleGridWorldContainer`.

