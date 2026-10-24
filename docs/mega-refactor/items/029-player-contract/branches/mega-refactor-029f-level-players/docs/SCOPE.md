# Scope: mega-refactor-029f-level-players

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-24

## Goal

The level screen, its spawn, death and end-level handlers, the HUD, the pause screen, the get-weapon screen
and the game setup reach the player through the players service. `game.megaman` and the extension
`IMegaGameEntity.megaman` are removed. The level rules stay the same (decision 2026-10-23): with one player
the game plays the same.

## Item

`029`: `IPlayer` contract, players service and `CharacterDef`. This branch takes:

- Branch split: "`029f` `level-players`: Level screen, handlers, HUD, pause, menus, camera focus use
  `game.players` (slot 0 by name where needed); remove `game.megaman` and the `megaman` extension. Rules
  unchanged (decision 2026-10-23)".
- Decision 2026-10-23: no public `primary` or `first()` on the players service; "a session-level lead slot
  for the camera and the save (see `047`) is a policy, not a query helper".
- The `029b` to `029e` rules: call sites name their intent; write code only when production code calls it;
  unused paths are commented out with a `TODO` (`029d`).

## Current state

The full scan is in `docs/NOTES.md` ("Scan", on `5da7a6db5`). Summary, with paths relative to
`core/src/main/kotlin/com/megaman/maverick/game/`:

- `MegamanMaverickGame.megaman` (`:211`) is made in `postCreate` and registered in slot 0; `resetPlayerProgress`
  selects the buster through it (`:777`).
- `game.megaman` is read in `MegaLevelScreen` (20), `PlayerSpawnEventHandler` (14), `EndLevelEventHandler`
  (15), `PlayerDeathEventHandler` (4), `PlayerStatsHandler` (22, a `Megaman` constructor argument),
  `LevelPauseScreen` (8) and `GetWeaponScreen` (2).
- The extension `IMegaGameEntity.megaman` is read by `AbstractItem:83, 178` (world direction) and by
  `MegamanTrailSprite` (V1), which nothing fetches.
- The reads are generic player state (on `MegaPlayer` already), generic reads not yet on `MegaPlayer`
  (health, `spawn`, camera focus), Megaman-only reads (weapons, ammo, current weapon, `running`, the beam
  sprite priority and flip), and one identity check (`owner != megaman`).

## In scope

The owner accepted the scan recommendations 1-7 on 2026-10-24 (see `NOTES.md`, "Decisions").

1. **The lead slot by name.** Each class that needs "the" player reads it through one private property,
   `game.players.get(PlayerSlot(0))!!`, with a `TODO`: the lead slot is a session policy (`047`). No public
   `primary` or `lead` on `MegaPlayers`.
2. **`MegaPlayer` gets** (each with its caller here):
   - `currentHealth`, `maxHealth`, `translateHealth(value)`: `PlayerStatsHandler`, `LevelPauseScreen`;
   - `spawn(props)`: `MegaLevelScreen` (`PLAYER_SPAWN_INIT`);
   - `focus: IFocusable` (the entity must be `IFocusable`; resolved at registration): the camera focus and the
     focus snap-back check in `MegaLevelScreen`.
3. **Megaman-only reads** go through one typed accessor in the player package, `val MegaPlayer.megaman:
   Megaman` (the entity, cast), with a `TODO` that the loadout moves to `024` and the character's look to
   `029g`. Callers: `PlayerStatsHandler` and `LevelPauseScreen` (current weapon, ammo, `translateAmmo`),
   `EndLevelEventHandler` (beam color, sprite flip and priority), `PlayerSpawnEventHandler` (sprite
   priority), `GetWeaponScreen` (current weapon), `MegamanMaverickGame.resetPlayerProgress` (buster),
   `MegaLevelScreen` (`running`).
4. **Level-wide writes act on every player** (`players.forEach`): `canBeDamaged = false` on a big boss
   defeat, `canBeDamaged = true` on `INTERMEDIATE_BOSS_DEAD`, `running = false` after a room transition.
5. **Level rules use the lead slot**, with `047` `TODO`s: the checkpoint only while `ready`, the spawn on
   `PLAYER_SPAWN_INIT`, the camera focus and snap-back, the gravity aura position, `BOSS_DEFEATED` and
   `BOSS_DEAD` do nothing while dead, pause only while not dead and `ready` and the forced resume while dead,
   the death handler set inactive and drawn by `dead`, and the spawn, death and end-level handlers (each acts
   on the lead player).
6. **Identity:** the room-transition projectile cleanup keeps a projectile when `owner.asPlayer != null`.
7. **`PlayerStatsHandler`** takes the game (or the players service) in place of a `Megaman`, and reads the
   lead player.
8. **Removals:** `MegamanMaverickGame.megaman` (a local value in `postCreate` makes, initializes, registers
   and listens), `MegaLevelScreen.megaman`, `LevelPauseScreen.megaman`, the handler fields, and the extension
   `IMegaGameEntity.megaman`.
9. **`AbstractItem`** reads `game.gravity.direction` for its default and per-frame direction.
10. **`MegamanTrailSprite` (V1)** is commented out in full, with its `DecorationsFactory` pool entry, with a
    `TODO` that nothing fetches it and the code may be out of date.
11. **Tests:** the new `MegaPlayer` reads (health, `spawn`, `focus`) and the `megaman` accessor, in
    `MegaPlayersTest`.

New APIs and their first callers:

| API | First callers |
|---|---|
| `MegaPlayer.currentHealth`, `maxHealth`, `translateHealth` | `PlayerStatsHandler`, `LevelPauseScreen` |
| `MegaPlayer.spawn(props)` | `MegaLevelScreen` |
| `MegaPlayer.focus` | `MegaLevelScreen` (camera focus, snap-back) |
| `MegaPlayer.megaman` (typed accessor) | `PlayerStatsHandler`, `LevelPauseScreen`, `EndLevelEventHandler`, `PlayerSpawnEventHandler`, `GetWeaponScreen`, `MegamanMaverickGame`, `MegaLevelScreen` |

## Out of scope

- The level rules for more players: spawn split (`038`), death and respawn per player, camera with more
  players, pause and HUD per player (`047`).
- The exact-class `KClass` sets (`030` or `023`); the Tiled property names, room property, atlas region, tag
  filter and log text with "megaman"; the beam properties keyed by `Megaman.TAG`.
- The weapon, jetpack and damage-negotiator fields named `megaman` (they hold their own player).
- The loadout per character (`024`) and `CharacterDef` (`029g`).

## Affected areas and risks

- **The level screen flow.** The handlers keep their order and their checks; only the reference changes.
  The handler fields are read at construction today (`private val megaman = game.megaman`); the lead player
  property reads the slot each time. The slot always holds the same Megaman, so the result is the same.
- **Construction order.** `MegaLevelScreen` and its handlers are made in `postCreate` after the registration
  (`:404-410`), so slot 0 is filled when they first read it. The plan checks every read site for this.
- **The camera focus** is set once at screen init; `focus` is the same Megaman object.
- **Pause and HUD** read health through the view; the values are the entity's own `Points`.
- **Files:** `MegamanMaverickGame`, `MegaLevelScreen`, the three handlers, `PlayerStatsHandler`,
  `LevelPauseScreen`, `GetWeaponScreen`, `MegaPlayer`, a new accessor file in `players/`, `IMegaGameEntity`,
  `AbstractItem`, `MegamanTrailSprite`, `DecorationsFactory`.

## Overlap with other branches

- No other branch is active. `019` (planned work on the spawn pass in `MegaLevelScreen.render`) is not
  touched here.

## Open questions

None. The owner accepted the scan recommendations on 2026-10-24.
