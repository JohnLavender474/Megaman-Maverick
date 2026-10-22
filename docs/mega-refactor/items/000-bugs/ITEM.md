# 000: Bugs found during the review

- Status: idea
- Type: bugs
- Depends on: none
- Related: `012`, `018`, `019`, `036`, `007`, `008`, `032`, `034`, `040`
- Branches: none yet

## Goal

Keep the bugs that the review found, until they move to a real bug tracker. Bugs are not
refactors: a bug fix changes gameplay on purpose, so it does not belong in a branch whose
contract is "plays the same as `master`".

## Where bugs go

Not decided yet (decision A):

- Option A (the review recommends it): GitHub Issues, with labels (`bug`, `verified` or
  `unverified`, `area:*`, `gameplay-change`, `found-by:mega-refactor`). A fix goes in its own
  `fix/<issue#>-<slug>` branch. A refactor branch that must change buggy behavior lists `#N` in
  its `CONTRACT.md`. This needs `gh` installed and authenticated for Claude sessions.
- Option B: `docs/issues/YYYY-MM-DD-<slug>.md` files, changed only on `master`.

When the decision is made, move each bug below to the tracker and mark this item `done`.

## Verified by reading the code

| Bug | Where | Notes |
|-----|-------|-------|
| A non-local `return` skips culling for all remaining entities, and skips `diagnostics?.endEntry()` | `engine/.../cullables/CullablesSystem.kt:26` | Survived `CullablesSystemTest` |
| `InsertionOrderPriorityQueue.size` is always 0 | `InsertionOrderPriorityQueue.kt:20` | `size` is read once at construction. `026` deletes the class |
| `MutableOrderedSet.retainAll` has inverted logic | `MutableOrderedSet` | Fix with `018` or alone |
| `replace("\\s+", "")` treats the pattern as literal text, not a regex | `GameState.kt:293`, `DesktopLauncher.kt` (`logLevels` parse) | `007` and `008` touch these |
| Stale room read on respawn: the room-music block reads the room before the queued spawn moves Mega, and can NPE | `MegaLevelScreen.kt:570-579` | Dead today (no room has `music`). `038` removes it |
| A pause during a gate opening turns physics, motion, behaviors and pathfinding on again when the game resumes | `LevelStateHandler.kt:26`, `Gate.kt` | The gate does not lock input. See `012` |
| Saved keyboard bindings are not applied at boot | `MegamanMaverickGame.kt:261`, `ControllerUtilsExtensions.kt:17-35` | They load only through "load saved settings". Maybe on purpose. See `034` |
| `selectButtonAction` resets to `NEXT_WEAPON` at every boot, but is saved | `MegamanMaverickGame.kt:263` | See `034` |
| Volume changes in game are not saved | `GameSettingsPanel.kt:177-215` | See `034` |
| The save keeps which health tanks you have, not how full they are | `GameState.kt:273`, `:85` | See `007` |
| `runQueue` runs LIFO (last added runs first) | `MegamanMaverickGame.kt:419-421` | Probably not intended |
| `OrderedSet.random()` removes and adds the chosen item, so it reorders the set | `ObjectSetExtensions.kt:68-79` | See `011` |
| `superRandom()` makes a new `Random(currentTimeMillis)` on each call | `ArrayExtensions.kt:109-113`, `ObjectSetExtensions.kt:81-89` | See `011` |
| The frame delta is not clamped | `MegamanMaverickGame.kt:414` | See `010` |
| `Float.round` throws in comma-decimal locales | `FloatExtensions.kt:5` | No callers today. See `032` |
| `gdx-freetype:1.11.0` is pinned against `gdxVersion` 1.14.2; `gdx-tools:1.6.3` is unused | `engine/build.gradle:8-9` | See `032` |
| 12 of 86 `.tps` files point to missing source folders; 3 atlases have no `.tps` | `assets/sprites/texture_packer/` | See `040` |
| `WILY_CASTLE_SCREEN` is in `ScreenEnum` but not registered | `MegamanMaverickGame.kt:381-398` | |
| The TMX background `section` property is parsed but has no effect | `MegaLevelScreen.kt:976` | All backgrounds draw first. See `026` |

## Needs a runtime check

| Suspected bug | Where |
|---------------|-------|
| When a health pickup fill ends during a pause, all systems turn on while the game is still paused | `MegaLevelScreen.kt:894`, `PlayerStatsHandler.kt:317` |
| A `SpawnerForEvent` drops `BEGIN_ROOM_TRANS` for enemies that the room transition destroyed, so a `spawn_room` enemy may not respawn | `MegaLevelScreen.kt:296`, `SpawnerForEvent.onEvent` |
| The ReactorMan (and DesertMan) same-name "4" spawns: the far-side spawn likely wins | `ReactorMan_v3.tmx`, `PlayerSpawnsManager.kt` |

Refuted: a pause during the boss health fill. The controller poller is off then.

## Behavior questions (maybe intended)

- Resolved (decision E): `StandardDamageNegotiator.get(damager)` picks the table by the damager's
  size, so `DarkSerketClaw` takes the MEDIUM values. Keep this. See `005` and `023`.
- Explosions from Mega's own shots (through `explodeAndDie`) do not hurt an `AbstractEnemy`.
  (Decision E.)
- A checkpoint can change silently while Mega is not ready (beam-in or death animation).
- Coins and level points reset only in `start()`, not on respawn.

## Decisions

- A (2026-10-22): bugs go in `docs/issues/`, one Markdown file per bug, not in GitHub Issues. Until the files exist, this list stays the source.
- E (2026-10-22): `DarkSerketClaw` is `SMALL` on purpose. Its damage must not change. See `005` and `023`.

## Open questions

- File the verified bugs now, and the runtime-check ones with an `unverified` label?
- Do players rely on any of these bugs? Then the fix needs a play check first.

## Sources

- `NOTES.md` in this folder.
- The review notes of the related items.
