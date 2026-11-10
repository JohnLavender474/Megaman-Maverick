# Contract: mega-refactor-146a-player-marker

- Status: approved
- Approved: 2026-11-10

## Automated checks

- [ ] `./gradlew engine:test` passes, with new tests: `IPlayer.slot` throws `PlayerNotRegisteredException` before
  registration and after unregistration, and gives the slot after registration.
- [ ] `./gradlew core:test` passes, with tests: `asPlayer` is `null` for the roster character that is not in a slot;
  `Event.isDeathOf` is true only for the player of the event slot.
- [ ] Engine tests: `canSpawn` sees an initialized entity; a rejected entity is not initialized again at its next
  spawn. Core tests for `nearestLiveTo`, the `TellySaucer` side choice, and the tank parse in `onSpawn`.
- [ ] `./gradlew core:smokeTest` passes, with a test of the tags of every concrete entity class: its own name (or its
  `TAG` for the 7 that differ), then the base name for the subclasses of the 4 bases.
- [ ] Core tests for `TagsComponent`: a subclass with no override gets its runtime class name; the tags survive a
  destroy and a new spawn of a pooled entity; `MegaGameEntities` indexes and removes an entity under each tag;
  `hasTag` on an entity with no component throws.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

Refactor only, so every check is a control check. Run from the branch worktree.

| Level | Room | What to check |
|-------|------|---------------|
| `REFACTOR_TEST_047` (`--players 2`) | `target_nearest` | Enemies aim at the nearest player, as before. |
| `REFACTOR_TEST_047` (`--players 2`, `--weapons all`) | `pickups` | Each item goes to the player who touches it; health and weapon energy fill that player. |
| `REFACTOR_TEST_047` (`--players 2`) | `slot_0_death` | A dead player respawns near the other player. |
| `REFACTOR_TEST_047` (`--players 2`) | `koopa_kick` | Either player kicks the shell (a side kick, and a stomp with the other player close): it rolls away from the kicker and hurts the player it hits, as before. |
| `X6_INTRO` | `room1` | X plays as before: run, shoot, take damage; the roster keeps Megaman out of slot 0. |
| `PRECIOUS_WOMAN` (`--heartTanks all --healthTanks all`) | `room7` | Boss fight: damage both ways, her laugh after she hits the player, the gems, the defeat and the end of the level. |
| `DESERT_MAN` | `room1` | `QuickSand` (widened block filters): Megaman sinks and can jump out, as before. |
| `REFACTOR_TEST_047` (`--players 2 --viewEdges top=BOUNCE,bottom=BOUNCE,damage=2`) | `group_tall` | `ViewEdgeHazard` (now initialized): a player who touches the top or bottom view edge is thrown back, loses 2 health and blinks; no crash. |
| `GAUNTLET_1` | `room1`, play on to `room3` | Map `block_filters` (`Met`, `Met,SniperJoe`): in `room3` the wave-1 Mets and the wave-3 SniperJoe drop through the ceiling blocks; Megaman cannot jump up through them. |
| `REFACTOR_TEST_031` (`--weapons MOON_SCYTHES`) | `ground_slide_blocker` | Moon Scythes fly through the `GroundSlideBlocker` wall and bounce off the plain wall and the floor. |
| `GLACIER_MAN` | `room1` | `FallingIcicle`: an icicle shatters on the floor; a buster shot breaks a hanging icicle; an icicle that hits Megaman damages him. |
| `INTRO_STAGE` | `room8` | `SpikeTeeth`: a charged shot makes the teeth of a `SpikeCopter` explode; a normal shot does not. |
| `TIMBER_WOMAN` (`--heartTanks all --healthTanks all`) | `room14` (gate room) | The boss deflects buster shots and charged shots, as before. |
| `MOON_MAN` | `room13` | `TellySaucer` (D10): walking right, the saucer spawns ahead and flies left at Megaman; walking back left from the right end, it does not spawn. |
| `MOON_MAN` (`--players 2`) | `room13` | The same two passes with 2 players: it spawns from the left and flies at the nearer player; from the right it does not spawn; no crash. |
| `REFACTOR_TEST_029` | `heart` | The heart tank shows; collecting it raises max health (the value comes from `onSpawn`). |
| `REFACTOR_TEST_029` (`--heartTanks A`) | `heart` | No heart tank in the room. |
| `REFACTOR_TEST_029` | `pickups` | The health tank shows and can be collected; tank A fills in the pause menu. |
| `REFACTOR_TEST_029` (`--healthTanks A`) | `pickups` | No health tank; the `Met` and the bulbs still spawn. |
| `WILY_STAGE_2` (`--heartTanks all --healthTanks all`) | `room9` (gate room) | `GutsTank` (its fist now runs `init()` inside `spawn()`): the fist moves and hits, the tank block is solid, the defeat completes. |
| `REACTOR_MAN` | `room4` | A busy room: each enemy appears, fires and drops items as before. |

## Behaviors to check

- Pause menu and the weapon screen with Megaman.
- Death, respawn and game over in single player.

## Intended changes

- Spawn flow (layers 1 and 2): the engine runs `init()` on the first spawn request, before `canSpawn`; a rejected
  entity is initialized once. `TellySaucer` picks its side from the live player nearest the spawn (with 2 players it
  can differ from before). `HeartTank` and `HealthTank` parse their value in `onSpawn`. Preloaded entities run
  `init()` once.
- Tags (D5-D7): the 34 subclasses of `Block`, `AbstractBlock`, `Switch` and `Death` have their own name as a tag,
  and the base name too. `MegaGameEntities.getOfTag(X)` finds an entity under each of its tags: a lookup of
  `Block.TAG` now also finds `AbstractBlock` entities, and a lookup of a subclass name finds that subclass. No game
  code looks up these names, so nothing changes in play. Debug log lines print all tags.
