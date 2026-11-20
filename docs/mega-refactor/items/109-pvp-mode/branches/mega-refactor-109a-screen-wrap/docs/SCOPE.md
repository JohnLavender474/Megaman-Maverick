# Scope: mega-refactor-109a-screen-wrap

What this branch changes and why, and its contract. Write this file after the scan and before
`BRIEF.src.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-19

## Goal

Horizontal screen wrap, as in the Super Mario Bros. 3 battle mode, in a room whose `game_rooms` object has
`wrap=true`. A player or a shot that leaves through the left edge comes in through the right edge, and the
reverse. While an entity straddles an edge, the other half of its sprite draws on the far side, and its body
there touches blocks and can be hit. A new test map `REFACTOR_TEST_109` holds one wrap room. No other level
changes.

## Item

`109` (PvP modes). The branch takes these decisions of `ITEM.md` (all 2026-11-19):

- A first test PvP room is one view wide, with horizontal screen wrap as in the Super Mario Bros. 3 battle mode. A
  player who straddles an edge has the other half of the sprite and the body on the far side. No other level
  changes.
- Shots wrap at the edges of a wrap room, as the players do (TowerFall style, not SMB3).
- A wrap counter limits the shots. A shot wraps at most 1 time by default (a weapon can override it). After that it
  does not wrap, it leaves through the edge, and the camera cull removes it. Players have no limit.
- The wrap is generic for any entity with a body or a sprite. A central filter in `core` decides who wraps. Phase 1:
  players and shots. The physics side and the draw side both ask the filter.
- Phase 1 has no per-instance override. Phase 2 adds it with its first caller; until then, a `TODO` at the filter.
- Phase 1 draws a second half only for entities that the filter accepts. A body-less effect at the edge shows half.
- The first branch `109a` holds the screen wrap and the test map `REFACTOR_TEST_109`. PvP respawn and score come
  in a later branch.
- The wrap flag is a bool property `wrap` on the `game_rooms` object.
- A room with `wrap=true` has no room transitions at all. A `TODO` at the code says to revisit this in `111`.

Research: the item `NOTES.md`, "Horizontal screen wrap" and "Wrap filter and counter".

## Current state

Scan of 2026-11-19 (`master` at `bfd12356d`). Paths: `engine/` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`core/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

Physics:

- `WorldSystem.process` (`engine/world/WorldSystem.kt:147-206`) builds `bodyArray` from the entities whose body
  `doUpdate()` (`:158-165`), runs `cycle()` per fixed step, then rebuilds the container for outside queries.
- `cycle()` (`:233-278`): `beginStep` + `preProcess`, container clear, `process`, add bodies and fixtures,
  `collectContacts`, `processContacts`, `resolveCollisions`, `postProcess` + `endStep`.
- `collectContacts(fixture)` (`:307-322`) skips a fixture that is not a filter key (`shouldProceedFiltering`) and
  queries the cells over the fixture's bounding box; `collectCandidate` (`:117-128`) adds `Contact(fixture,
  candidate)` to an `OrderedSet`. A contact is the fixture pair by identity (`world/contacts/Contact.kt:12-16`).
- `resolveCollisions` (`:324-340`) skips static bodies and hands each overlapping body to the collision handler.
- `Fixture.getShape()` (`world/body/Fixture.kt`) computes an attached fixture from the body bounds at each call; a
  fixture with `attachedToBody=false` keeps its raw shape, which core code sets in a `preProcess` (the player
  `DAMAGEABLE`, `core/players/PlayerBody.kt:178-181`).
- `Body` (`world/body/Body.kt`) holds `stepStartX/Y` and `stepMoveX/Y` for interpolation; `getDrawOffset`
  (`WorldSystem.kt:208-215`) gives 0 for a step move over 1 tile, so a wrap alone draws without interpolation for
  1 frame. `restartStep()` has 1 caller (`PipePortal.kt:143`).
- The default container `SimpleGridWorldContainer` has no bounds (signed cell keys).
- `ConstKeys.PRIOR` is written in a body `preProcess` (`core/world/body/BodyComponentCreator.kt:47`) and read by
  `getPositionDelta()` (34 references). A wrap before `postProcess` would show a delta of ±W to a `postProcess`
  reader.

Drawing:

- `SpritesSystem.process` (`engine/drawables/sprites/SpritesSystem.kt`) sets each sprite's `drawOffsetX/Y` from the
  interpolation offset and hands it to the collector; `MegaLevelScreen.draw` (`core/screens/levels/MegaLevelScreen.kt:911-984`)
  polls 3 priority queues.
- `GameSprite.draw` (`engine/drawables/sprites/GameSprite.kt:68-83`) shifts its vertices by `drawOffsetX/Y`.
- The slot-1 tint (`setColor`) and the damage blink (`setAlpha`, `hidden`) are in the sprite vertex colors, so a
  second draw of the same vertices keeps them. No shaders exist.

Rooms and camera:

- A room exactly one view wide pins the camera (`CameraRoomClamp`, `CameraLimits.kt:52-57`).
- A transition starts from `CameraRoomScan` (`core/screens/levels/camera/CameraRoomScan.kt:9-12`, calls
  `CameraRoomBounds.scanForNextRoom`), from `CameraRoomExits.update` (`core/screens/levels/camera/CameraRoomExits.kt:85`)
  or from `NEXT_ROOM_REQ` (`MegaLevelScreen.kt:690-704`, sent by a `Gate`).
- Room properties are read from `roomBounds.currentGameRoom.properties` (`LevelRoomHandler.kt:58`, `fade_out_music`).
- `roomBounds.onSetToRoomNoTrans` (`LevelRoomHandler.kt:160-169`) fires when the level sets the room without a
  transition (level start, respawn).

Shots:

- `CULL_OUT_OF_BOUNDS` (`AbstractProjectile.kt:42-50`) culls a shot 0.05 s after it stops overlapping the camera
  (`IProjectileEntity.kt:32`). A shot that wraps at its center never leaves the camera, so it lives on; after its
  last wrap it leaves and the same cull removes it. No projectile code changes.

Wiring: `WorldSystem(...)` at `MegamanMaverickGame.kt:974`, `SpritesSystem(...)` at `:1022`. Test levels follow
`REFACTOR_TEST_047` (`LevelDefinition.kt:177-182`); `LevelCompletionScreensTest.kt:27` lists each one.

## In scope

- (Added 2026-11-19, owner) Move the draw offsets out of `WorldSystem`: a new engine class `BodyInterpolation`
  (draw side) implements `IDrawOffsets` and holds the `interpolation` flag and the 1-tile jump guard. `WorldSystem`
  keeps only the step loop facts it already has (`stepSerial`, `alpha`) and stops implementing `IDrawOffsets`.
  Callers: `SpritesSystem` and `CameraDrawOffset` (built in `MegamanMaverickGame.kt` and the level screen), the `I`
  key toggle (`MegamanMaverickGame.kt:639-642`). `WorldDrawOffsetsTest` moves to the new class. No change in what
  is drawn.
- Engine `WorldWrap`: the region (the bounds of the current wrap room, or none), the filter (does an entity wrap)
  and the cap (how many wraps). Engine code knows no game types.
- `WorldSystem`: with a region, for each body of a wrapping entity:
  - a second contact query, shifted by ∓W, for each active fixture whose bounds cross an edge (also non-key
    fixtures, so a shot's `DAMAGER` is found across the edge); the contact stays `Contact(realFixture, candidate)`;
  - a second collision pass with the body shifted by ∓W while its bounds cross an edge;
  - the wrap itself: after `postProcess` and `endStep`, a body whose center is past an edge moves by ±W, its
    step start moves with it, and its wrap count goes up. At the cap it does not wrap.
- `Body` / `IBody`: a wrap count (cleared by `reset()`) and a call that moves the body with its step start.
- `SpritesSystem` and `GameSprite`: a draw field on the sprite for the far half; `GameSprite.draw` draws the
  vertices a second time at that offset. `SpritesSystem` sets it each frame from the same region and filter.
- Core `WrapPolicy` (or the name the plan picks): players and `IProjectileEntity` wrap; shots wrap 1 time, players
  without a cap. `TODO`s for the override and for enemies, items and effects (phase 2).
- Core region supplier: the bounds of the current room when its `game_rooms` object has `wrap=true` (new
  `ConstKeys.WRAP`). It fails fast when a wrap room is not one view wide.
- No room transitions in a wrap room: a guard in `CameraRoomScan`, `CameraRoomExits` and the `NEXT_ROOM_REQ`
  handler, each with a `TODO` for `111`.
- Test map `REFACTOR_TEST_109` (`RefactorTest109.tmx`), its `LevelDefinition` entry and its
  `LevelCompletionScreensTest` line.
- Tests: engine tests for the wrap, the seam contacts, the seam collisions, the cap, the step shift and the draw
  field; a core test for the filter and the room width check.

## Out of scope

- PvP respawn, score, match rules (a later `109` branch).
- The per-instance override, and wrap for enemies, items, effects (phase 2, `TODO`).
- Rules that read the real fixture shape again inside the rule: ladders, sand, gravity changers, the side-contact
  end prune, the hit position of a shot (`ProjectileRules.kt:38-39`). Across an edge they see the real side.
- Queries outside `WorldSystem` (`GroundProbe`, pathfinding, `MegamanXMoves.kt:83`): they do not see across an edge.
- Aim and targeting across the edge (`MegaPlayers.targetFor` uses raw distance).
- Vertical wrap; wrap rooms wider than one view; transitions out of a wrap room (`111`).

## Affected areas and risks

- `WorldSystem.cycle` is the hot path of every level. With no region the added work is one null check per frame
  and none per step; it must stay allocation-free (`WorldAllocationMeasureTest`).
- A full-width floor reaches the collision handler twice for a straddling body; on `COLLIDE_DOWN_ONLY` blocks the
  friction add in `MegaCollisionHandler.kt:76-79` runs twice. The test room uses plain blocks.
- A non-attached fixture's raw shape is stale for the rest of the frame after a wrap (core sets it in the next
  `preProcess`), so an outside query in that frame sees the old side.

## Overlap with other branches

Active branches on 2026-11-19: `060k` (Proto Man frame edits) and `078d` (Reactor Man art). Neither touches the
engine world, the sprites system, the level screen or the camera parts. No overlap.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1` clean design (no rule number) | helps | The world step no longer holds draw logic. |
| `O1-R3` No new fragile order | no effect | The wrap has one fixed place in `cycle()` (after `postProcess`), named in the code and tested. |
| `O1-R4` Typed state | no effect | `wrap` is Tiled map data read once per room; the count is a typed field. |
| `O1-R5` Code only with a caller | no effect | Each new API has its caller in this branch; the override waits (`TODO`). |
| `O2-R4` Stable order | no effect | Seam contacts go into the same `OrderedSet` in body order. |
| `O2-R5` No single-player logic | helps | The filter acts on every player. |
| `O2-R6` No single-view logic | waiting | The region comes from the room, not the camera; one view per room is still assumed. |
| `O2-R7` Gameplay apart from presentation | no effect | The physics wrap reads the room bounds; the draw side reads the same region. |
| `O5-R1` No per-frame allocation | no effect | Scratch shapes made once per shape class. |
| `O5-R2` Measure hot paths | no effect | `WorldAllocationMeasureTest` and step timing before and after. |
| `O6-R2` Named checks | helps | Engine tests per wrap rule. |
| `O7-R3` No level-identity checks | no effect | The room property decides, not the level. |
| `O8-R2` Capabilities, not character checks | no effect | The filter tests `isPlayer`, not a class. |

## Open questions

None. Owner answers of 2026-11-19:

- D1: a wrap room must be exactly one view wide; the level load throws for any other width.
- D2: the rules that read the real shape (ladders, sand, gravity changers, the side-contact end prune, the hit
  point of a shot) stay as they are, with a `TODO` at each rule.

# Contract

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Do the checks before the branch merges, and write the result in the branch process log.

## Automated checks

- [ ] `./gradlew engine:test` passes, with the new wrap tests.
- [ ] `./gradlew core:test` passes, with the filter and room width tests and the `REFACTOR_TEST_109` line.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] `WorldAllocationMeasureTest` shows no new allocation per step.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

The room names and the control rooms are fixed in `PLAN.md` step 1 (test map) and copied here then.

| Level | What to check |
|-------|---------------|
| `REFACTOR_TEST_109`, room `arena`, 1 player | The wrap works with one player. |
| `REFACTOR_TEST_109`, room `arena`, `--players 2 --mode pvp` | Walk off each side: the player comes in on the other side on the same floor; both halves draw while straddling; the camera does not move. |
| same | Stand on the floor across an edge and jump from it: the player stays on the ground and does not fall through. |
| same | Shoot across an edge: the shot wraps 1 time, then leaves through the edge and is gone. |
| same | Hit the other player across an edge (shooter on one side, target straddling). |
| same, `--interpolation true` | No streak or pop at the wrap; `I` toggles interpolation live. |
| `REFACTOR_TEST_047`, room `coop_wide` | Side edges hold the players as before. |
| `DESERT_MAN`, room `room6` (side transition to `room7`) | The transition runs; shots leave the view and vanish as before. |

## Behaviors to check

- Death and respawn in the wrap room (slot 0 death resets the level as today).

## Intended changes

- In a room with `wrap=true`: players and shots wrap at the left and right edges; room transitions do not start.
